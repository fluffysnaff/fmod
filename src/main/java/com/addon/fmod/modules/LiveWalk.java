package com.addon.fmod.modules;

import com.addon.fmod.FMod;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;

import java.util.HashSet;
import java.util.Objects;

public class LiveWalk extends Module
{

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> vehicle = sgGeneral.add(new BoolSetting.Builder()
        .name("vehicle")
        .description("Enable vehicle bypass.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> classicRound = sgGeneral.add(new BoolSetting.Builder()
        .name("classic-round")
        .description("Classic rounding with Math.round()")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> flyAntiKick = sgGeneral.add(new BoolSetting.Builder()
        .name("fly-antikick")
        .description("Don't get kicked for floating.")
        .defaultValue(false)
        .build()
    );

    public LiveWalk()
    {
        super(FMod.CATEGORY, "Live Walk", "Bypass Liveoverflow ASP with ease");
    }
    private Entity vehicleEntity = null;
    int flyingTimer = 0;

    public boolean classicRoundEnabled() { return classicRound.get(); }

    @EventHandler
    public void onSendMovementPackets(SendMovementPacketsEvent.Pre event)
    {
        assert mc.player != null;
        double dx = mc.player.getPos().x;
        double dz = mc.player.getPos().z;
        double y = mc.player.getPos().y;
        if(mc.player.getVehicle() != null && vehicle.get())
        {
            vehicleEntity = mc.player.getVehicle();
            dx = mc.player.getVehicle().getX();
            dz = mc.player.getVehicle().getZ();
            y = mc.player.getVehicle().getY();
        }

        // First round to the thousandths
        dx = FMod.round(dx, 3);
        dz = FMod.round(dz, 3);

        for(int i = 2; i > 0; i--)
        {
            if (FMod.isNotRoundedPos(dx, dz))
            {
                dx = FMod.round(dx, i);
                dz = FMod.round(dz, i);
            }
        }
        if (FMod.isNotRoundedPos(dx, dz))
        {
            dx = (int)(dx);
            dz = (int)(dz);
        }

        // Final test
        if (FMod.isNotRoundedPos(dx, dz))
            return;

        if (mc.player.isOnGround() || !flyAntiKick.get())
        {
            sendPosition(dx, y, dz, mc.player.getVehicle() != null);
            flyingTimer = 0;
            return;
        }
        if (++flyingTimer > 20) {
            sendPosition(dx, y - 0.05, dz, mc.player.getVehicle() != null);
            flyingTimer = 0;
        }
        else {
            sendPosition(dx, y, dz, mc.player.getVehicle() != null);
        }
    }

    private void sendPosition(double x, double y, double z, boolean v)
    {
        assert mc.player != null;
        if(v && vehicle.get())
        {
            vehicleEntity.setPos(x, y, z);
            VehicleMoveC2SPacket packet = new VehicleMoveC2SPacket(vehicleEntity);
            sendPacket(packet);
            return;
        }
        sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround()));
        mc.player.setPos(x, y, z);
    }

    private void sendPacket(PlayerMoveC2SPacket packet)
    {
        Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(packet);
    }
    private void sendPacket(VehicleMoveC2SPacket packet)
    {
        Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(packet);
    }
}
