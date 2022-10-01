package com.addon.fmod.modules;

import com.addon.fmod.FMod;
import com.addon.fmod.mixin.PlayerPositionLookS2CPacketAccessor;
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
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

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
    private final Setting<Boolean> noRotate = sgGeneral.add(new BoolSetting.Builder()
        .name("no-rotate")
        .description("Stops sending/receiving pitch/yaw")
        .defaultValue(true)
        .build()
    );

    public LiveWalk()
    {
        super(FMod.CATEGORY, "Live Walk", "Bypass Liveoverflow ASP with ease");
    }

    private final HashSet<PlayerMoveC2SPacket> packets = new HashSet<>();
    private final HashSet<VehicleMoveC2SPacket> packetsVehicle = new HashSet<>();
    private Entity vehicleEntity = null;

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

        // Final test
        if (FMod.isNotRoundedPos(dx, dz))
            return;

        sendPosition(dx, y, dz, mc.player.getVehicle() != null);
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event)
    {
        if ((event.packet instanceof PlayerMoveC2SPacket) && !packets.remove(event.packet)) event.cancel();
        if ((event.packet instanceof VehicleMoveC2SPacket) && vehicle.get() && !packetsVehicle.remove(event.packet)) event.cancel();
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
        sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, mc.player.isOnGround()));
        mc.player.setPos(x, y, z);
    }

    private void sendPacket(PlayerMoveC2SPacket packet)
    {
        packets.add(packet);
        Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(packet);
    }
    private void sendPacket(VehicleMoveC2SPacket packet)
    {
        packetsVehicle.add(packet);
        Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(packet);
    }
}
