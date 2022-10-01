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
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
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
    private final HashSet<PlayerMoveC2SPacket> packets = new HashSet<>();

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

    @EventHandler
    public void onTick(TickEvent.Post event) {
        assert mc.player != null;
        if (!mc.player.isAlive())
            return;

        // https://github.com/Akarin-project/Akarin/blob/ver/1.12.2/sources/src/main/java/net/minecraft/server/PlayerConnection.java#L431
        Vec3d moveVec = Vec3d.ZERO;
        double mySpeed = 1.0 / 256.0;

        double x = mc.player.getVelocity().x;
        double z = mc.player.getVelocity().z;
        double den = (x*x + z*z) != 0 ? (x*x + z*z) : 1;
        double n = Math.sqrt((mySpeed * mySpeed) / den);

        moveVec = moveVec.add(x * n, 0, z * n);
        if (mc.player.input.jumping)
        {
            moveVec = moveVec.add(0, mySpeed, 0);
        }
        else if (mc.player.input.sneaking)
        {
            moveVec = moveVec.add(0, -mySpeed, 0);
        }
        Vec3d newPos = new Vec3d(mc.player.getX() + moveVec.x, mc.player.getY(), mc.player.getZ() + moveVec.z);
        mc.player.setPosition(newPos);

        sendPosition(newPos.x, newPos.y + moveVec.y, newPos.z, false);
        sendPosition(newPos.x, newPos.y - 500, newPos.z, true);
    }

    private void sendPosition(double x, double y, double z, boolean isOnGround)
    {
        assert mc.player != null;

        // If we figure out a better bypass, i.e. so that server thinks we are not moving at all, then this is
        // not necessary
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
