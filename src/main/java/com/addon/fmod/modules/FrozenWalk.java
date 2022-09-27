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
import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
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
    private int timer = 0;
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
        if(event.packet instanceof PlayerMoveC2SPacket.PositionAndOnGround)
            event.cancel();
        if(event.packet instanceof PlayerMoveC2SPacket.LookAndOnGround)
            event.cancel();
    }

    @EventHandler
    public void onRecievePacket(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket)
        {
            PlayerPositionLookS2CPacketAccessor p = (PlayerPositionLookS2CPacketAccessor) event.packet;

            p.setPitch(mc.player.getPitch());
            p.setYaw(mc.player.getYaw());
        }
    }
    private boolean testPosition(double x, double z)
    {
        return ((long) (x * 1000)) % 10 == 0 || ((long) (z * 1000)) % 10 == 0;
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        assert mc.player != null;
        if (!mc.player.isAlive())
            return;

        double hspeed = 0.03130D;
        double vspeed = 0.03130D;
        timer++;

        Vec3d forward = new Vec3d(0, 0, hspeed).rotateY(-(float) Math.toRadians(Math.round(mc.player.getYaw() / 90) * 90));
        Vec3d moveVec = Vec3d.ZERO;
        if (mc.player.input.pressingForward) {
            if (!mc.player.input.pressingBack){
                moveVec = moveVec.add(forward);
            }
        } else if (mc.player.input.pressingBack){
            moveVec = moveVec.add(forward.negate());
        } else if (mc.player.input.jumping) {
            if (!mc.player.input.sneaking){
                moveVec = moveVec.add(0, vspeed, 0);
            }
        } else if (mc.player.input.sneaking){
            moveVec = moveVec.add(0, -vspeed, 0);
        } else if (mc.player.input.pressingLeft) {
            if (!mc.player.input.pressingRight){
                moveVec = moveVec.add(forward.rotateY((float) Math.toRadians(90)));
            }
        } else if (mc.player.input.pressingRight){
            moveVec = moveVec.add(forward.rotateY((float) -Math.toRadians(90)));
        }

        if(timer > 1) {
            moveVec.add(0, -2 * vspeed, 0);
            timer = 0;
        }


        double x = FMod.round(mc.player.getX() + moveVec.x, 3);
        double z = FMod.round(mc.player.getZ() + moveVec.z, 3);
        if(!testPosition(x, z))
        {
            x = FMod.round(x, 2);
            z = FMod.round(z, 2);
        }
        if(!testPosition(x, z))
        {
            x = FMod.round(x, 1);
            z = FMod.round(z, 1);
        }
        Vec3d newX = new Vec3d(x, mc.player.getY(), z);
        mc.player.setPosition(newX);

        sendPosition(newX.x, newX.y + moveVec.y, newX.z, false);
        sendPosition(newX.x, newX.y - 500, newX.z, true);
    }

    private void sendPosition(double x, double y, double z, boolean isOnGround)
    {
        assert mc.player != null;
        x = FMod.round(x, 3);
        z = FMod.round(z, 3);
        if(!testPosition(x, z))
        {
            x = FMod.round(x, 2);
            z = FMod.round(z, 2);
        }
        Vec3d pos;
        if(antiFlyKick.get() && mc.world.getBlockState(mc.player.getBlockPos().down()).isAir())
        {
            pos = new Vec3d(x, y - 0.03130D, z);
        }
        else
        {
            pos = new Vec3d(x, y, z);
        }
        sendPacket(new PlayerMoveC2SPacket.Full(pos.x, pos.y, pos.z, mc.player.getYaw(), mc.player.getPitch(), isOnGround));
    }

    private void sendPacket(PlayerMoveC2SPacket packet)
    {
        packets.add(packet);
        Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(packet);
    }
}
