package addon.fmod.modules;

import addon.fmod.FMod;
import addon.fmod.utils.RoundUtils;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.world.phys.Vec3;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import java.util.HashSet;

public class FrozenWalk extends Module
{

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public FrozenWalk()
    {
        super(FMod.CATEGORY, "frozen-walk", "Get into protected regions without being teleported");
    }
    private final Setting<Boolean> antiFlyKick = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-fly-kick")
        .description("Stops you from getting kicked when flying")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> noRotate = sgGeneral.add(new BoolSetting.Builder()
        .name("no-rotate")
        .description("Stops sending/receiving pitch/yaw")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> strict = sgGeneral.add(new BoolSetting.Builder()
        .name("strict")
        .description("Strict mode, uses low speed")
        .defaultValue(true)
        .build()
    );
    private final HashSet<ServerboundMovePlayerPacket> packets = new HashSet<>();
    private int antikickTick = 0;

    public static boolean inSameBlock(Vec3 vector, Vec3 other) {
        return other.x >= Math.floor(vector.x) && other.x <= Math.ceil(vector.x) &&
            other.y >= Math.floor(vector.y) && other.y <= Math.ceil(vector.y) &&
            other.z >= Math.floor(vector.z) && other.z <= Math.ceil(vector.z);
    }

    @EventHandler
    public void onSendMovementPackets(SendMovementPacketsEvent.Pre event)
    {
        assert mc.player != null;
        mc.player.setDeltaMovement(Vec3.ZERO);
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event)
    {
        if ((event.packet instanceof ServerboundMovePlayerPacket) && !packets.remove(event.packet)) event.cancel();
        if(event.packet instanceof ServerboundMovePlayerPacket.PosRot)
            event.cancel();
        if(event.packet instanceof ServerboundMovePlayerPacket.Rot)
            event.cancel();
    }

    // https://github.com/Akarin-project/Akarin/blob/ver/1.12.2/sources/src/main/java/net/minecraft/server/PlayerConnection.java#L431
    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.player == null) return;
        if (!mc.player.isAlive()) return;

        antikickTick += 1;

        Vec3 vec = Vec3.ZERO;
        double mySpeed = 1d / 32d + 0.01d;

        if (mc.options.keyJump.isDown())
            vec = vec.add(new Vec3(0, 1, 0));
        else if (mc.options.keyShift.isDown())
            vec = vec.add(new Vec3(0, -1, 0));
        else
        {
            if (mc.options.keyUp.isDown())
                vec = vec.add(new Vec3(0, 0, 1));
            if (mc.options.keyRight.isDown())
                vec = vec.add(new Vec3(1, 0, 0));
            if (mc.options.keyDown.isDown())
                vec = vec.add(new Vec3(0, 0, -1));
            if (mc.options.keyLeft.isDown())
                vec = vec.add(new Vec3(-1, 0, 0));
        }

        if (vec.length() < 0)
            return;
        vec = vec.normalize();
        if (!(vec.x == 0 && vec.z == 0))
        {
            double moveAngle = Math.atan2(vec.x, vec.z) + Math.toRadians(mc.player.getYRot() + 90f);
            double x = Math.cos(moveAngle);
            double z = Math.sin(moveAngle);
            vec = new Vec3(x, vec.y, z);
        }
        vec = vec.scale(mySpeed);
        Vec3 newPos = new Vec3(mc.player.getX() + vec.x, mc.player.getY() + vec.y, mc.player.getZ() + vec.z);

        // If we don't have strict enabled, check if when we move we'll be in the same block
        for(int i = 0; i < 10; i++)
        {
            if (inSameBlock(newPos.add(vec.scale(1.5)), new Vec3(mc.player.xo, mc.player.yo, mc.player.zo)) && !strict.get())
                newPos = newPos.add(vec);
        }

        mc.player.setPos(newPos);
        sendPosition(newPos.x, newPos.y, newPos.z, mc.player.onGround());

        // Reset the check
        sendPosition(newPos.x - 420, newPos.y - 420, newPos.z - 420, mc.player.onGround());
    }

    private void sendPosition(double x, double y, double z, boolean isOnGround)
    {
        if(mc.player == null || mc.level == null) return;

        // Round coords if live walk is enabled
        LiveWalk liveWalk = Modules.get().get(LiveWalk.class);
        if (liveWalk != null && liveWalk.isActive()) {
            x = RoundUtils.roundCoordOnPositionPacket(x);
            z = RoundUtils.roundCoordOnPositionPacket(z);
        }

        Vec3 pos;
        if(antiFlyKick.get() && mc.level.getBlockState(mc.player.blockPosition().below()).isAir() && antikickTick > 60) {
            pos = new Vec3(x, y - 0.03130D, z);
            antikickTick = 0;
        }
        else {
            pos = new Vec3(x, y, z);
        }

        sendPacket(new ServerboundMovePlayerPacket.Pos(pos.x, pos.y, pos.z, isOnGround, mc.player.horizontalCollision));
    }

    private void sendPacket(ServerboundMovePlayerPacket packet)
    {
        if (mc.getConnection() == null) return;

        packets.add(packet);
        mc.getConnection().send(packet);
    }
}
