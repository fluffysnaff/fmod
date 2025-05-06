package addon.fmod.modules;

import addon.fmod.FMod;
import addon.fmod.utils.RoundUtils;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.ClientPlayerEntityAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.util.math.Vec3d;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

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
    private final HashSet<PlayerMoveC2SPacket> packets = new HashSet<>();
    private int antikickTick = 0;

    public static boolean inSameBlock(Vec3d vector, Vec3d other) {
        return other.x >= Math.floor(vector.x) && other.x <= Math.ceil(vector.x) &&
            other.y >= Math.floor(vector.y) && other.y <= Math.ceil(vector.y) &&
            other.z >= Math.floor(vector.z) && other.z <= Math.ceil(vector.z);
    }

    @EventHandler
    public void onSendMovementPackets(SendMovementPacketsEvent.Pre event)
    {
        assert mc.player != null;
        mc.player.setVelocity(Vec3d.ZERO);
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event)
    {
        if ((event.packet instanceof PlayerMoveC2SPacket) && !packets.remove(event.packet)) event.cancel();
        if(event.packet instanceof PlayerMoveC2SPacket.Full)
            event.cancel();
        if(event.packet instanceof PlayerMoveC2SPacket.LookAndOnGround)
            event.cancel();
    }

    // https://github.com/Akarin-project/Akarin/blob/ver/1.12.2/sources/src/main/java/net/minecraft/server/PlayerConnection.java#L431
    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.player == null) return;
        if (!mc.player.isAlive()) return;

        antikickTick += 1;

        Vec3d vec = Vec3d.ZERO;
        double mySpeed = 1d / 32d + 0.01d;

        if (mc.options.jumpKey.isPressed())
            vec = vec.add(new Vec3d(0, 1, 0));
        else if (mc.options.sneakKey.isPressed())
            vec = vec.add(new Vec3d(0, -1, 0));
        else
        {
            if (mc.options.forwardKey.isPressed())
                vec = vec.add(new Vec3d(0, 0, 1));
            if (mc.options.rightKey.isPressed())
                vec = vec.add(new Vec3d(1, 0, 0));
            if (mc.options.backKey.isPressed())
                vec = vec.add(new Vec3d(0, 0, -1));
            if (mc.options.leftKey.isPressed())
                vec = vec.add(new Vec3d(-1, 0, 0));
        }

        if (vec.length() < 0)
            return;
        vec = vec.normalize();
        if (!(vec.x == 0 && vec.z == 0))
        {
            double moveAngle = Math.atan2(vec.x, vec.z) + Math.toRadians(mc.player.getYaw() + 90f);
            double x = Math.cos(moveAngle);
            double z = Math.sin(moveAngle);
            vec = new Vec3d(x, vec.y, z);
        }
        vec = vec.multiply(mySpeed);
        Vec3d newPos = new Vec3d(mc.player.getX() + vec.x, mc.player.getY() + vec.y, mc.player.getZ() + vec.z);

        // If we don't have strict enabled, check if when we move we'll be in the same block
        for(int i = 0; i < 10; i++)
        {
            if (inSameBlock(newPos.add(vec.multiply(1.5)), new Vec3d(mc.player.lastX, mc.player.lastY, mc.player.lastZ)) && !strict.get())
                newPos = newPos.add(vec);
        }

        mc.player.setPosition(newPos);
        sendPosition(newPos.x, newPos.y, newPos.z, mc.player.isOnGround());

        // Reset the check
        sendPosition(newPos.x - 420, newPos.y - 420, newPos.z - 420, mc.player.isOnGround());
    }

    private void sendPosition(double x, double y, double z, boolean isOnGround)
    {
        if(mc.player == null || mc.world == null) return;

        // Round coords if live walk is enabled
        LiveWalk liveWalk = Modules.get().get(LiveWalk.class);
        if (liveWalk != null && liveWalk.isActive()) {
            x = RoundUtils.roundCoordOnPositionPacket(x);
            z = RoundUtils.roundCoordOnPositionPacket(z);
        }

        Vec3d pos;
        if(antiFlyKick.get() && mc.world.getBlockState(mc.player.getBlockPos().down()).isAir() && antikickTick > 60) {
            pos = new Vec3d(x, y - 0.03130D, z);
            antikickTick = 0;
        }
        else {
            pos = new Vec3d(x, y, z);
        }

        sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, isOnGround, mc.player.horizontalCollision));
    }

    private void sendPacket(PlayerMoveC2SPacket packet)
    {
        if (mc.getNetworkHandler() == null) return;

        packets.add(packet);
        mc.getNetworkHandler().sendPacket(packet);
    }
}
