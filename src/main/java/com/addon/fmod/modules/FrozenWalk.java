package com.addon.fmod.modules;

import com.addon.fmod.FMod;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import com.addon.fmod.mixin.PlayerPositionLookS2CPacketAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Objects;

public class FrozenWalk extends Module
{

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public FrozenWalk()
    {
        super(FMod.CATEGORY, "Frozen Walk", "Get into protected regions without being teleported");
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

    @EventHandler
    public void onRecievePacket(PacketEvent.Receive event)
    {
        if (event.packet instanceof PlayerPositionLookS2CPacket && noRotate.get())
        {
            PlayerPositionLookS2CPacketAccessor p = (PlayerPositionLookS2CPacketAccessor) event.packet;
            assert mc.player != null;
            p.setPitch(mc.player.getPitch());
            p.setYaw(mc.player.getYaw());
        }
    }

    // https://github.com/Akarin-project/Akarin/blob/ver/1.12.2/sources/src/main/java/net/minecraft/server/PlayerConnection.java#L431
    @EventHandler
    public void onTick(TickEvent.Post event) {
        assert mc.player != null;
        if (!mc.player.isAlive())
            return;

        Vec3d vec = Vec3d.ZERO;
        double mySpeed = 0.045d;

        if (mc.player.input.jumping)
            vec = vec.add(new Vec3d(0, 1, 0));
        else if (mc.player.input.sneaking)
            vec = vec.add(new Vec3d(0, -1, 0));
        else
        {
            if (mc.player.input.pressingForward)
                vec = vec.add(new Vec3d(0, 0, 1));
            if (mc.player.input.pressingRight)
                vec = vec.add(new Vec3d(1, 0, 0));
            if (mc.player.input.pressingBack)
                vec = vec.add(new Vec3d(0, 0, -1));
            if (mc.player.input.pressingLeft)
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

        for(int i = 0; i < 5; i++)
        {
            if (inSameBlock(newPos.add(vec.multiply(1.5)), new Vec3d(mc.player.prevX, mc.player.prevY, mc.player.prevZ)) && !strict.get())
                newPos = newPos.add(vec);
        }

        mc.player.setPosition(newPos);
        sendPosition(newPos.x, newPos.y, newPos.z, mc.player.isOnGround());
        sendPosition(newPos.x - 420, newPos.y - 420, newPos.z - 420, mc.player.isOnGround());
    }

    private void sendPosition(double x, double y, double z, boolean isOnGround)
    {
        assert mc.player != null;
        x = FMod.round(x, 3);
        z = FMod.round(z, 3);
        for(int i = 2; i > 0; i--)
        {
            if (FMod.isNotRoundedPos(x, z))
            {
                x = FMod.round(x, i);
                z = FMod.round(z, i);
            }
        }

        Vec3d pos;
        if(antiFlyKick.get() && Objects.requireNonNull(mc.world).getBlockState(mc.player.getBlockPos().down()).isAir())
            pos = new Vec3d(x, y - 0.03130D, z);
        else
            pos = new Vec3d(x, y, z);

        sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, isOnGround));
    }

    private void sendPacket(PlayerMoveC2SPacket packet)
    {
        packets.add(packet);
        Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(packet);
    }
}
