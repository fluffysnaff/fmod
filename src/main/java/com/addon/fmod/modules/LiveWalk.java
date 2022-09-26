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
import net.minecraft.util.math.Vec3d;

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

    private final Setting<Boolean> lockYaw = sgGeneral.add(new BoolSetting.Builder()
        .name("lock-yaw")
        .description("Locks yaw & head yaw")
        .defaultValue(false)
        .build()
    );

    public LiveWalk()
    {
        super(FMod.CATEGORY, "Live Walk", "Bypass Liveoverflow ASP with ease");
    }

    private final HashSet<PlayerMoveC2SPacket> packets = new HashSet<>();
    private final HashSet<VehicleMoveC2SPacket> packetsVehicle = new HashSet<>();
    private Entity vehicleEntity = null;

    private boolean isBadPosition(double x, double z)
    {
        return ((long) (x * 1000)) % 10 != 0 && ((long) (z * 1000)) % 10 != 0;
    }

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
            if (isBadPosition(dx, dz))
            {
                dx = FMod.round(dx, i);
                dz = FMod.round(dz, i);
            }
        }

        // Final test
        if (isBadPosition(dx, dz))
            return;

        sendPosition(dx, y, dz, mc.player.getVehicle() != null);
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event)
    {
        if ((event.packet instanceof PlayerMoveC2SPacket) && !packets.remove(event.packet)) event.cancel();
        if ((event.packet instanceof VehicleMoveC2SPacket) && vehicle.get() && !packetsVehicle.remove(event.packet)) event.cancel();
    }

    private void sendPosition(double x, double y, double z, boolean v)
    {
        assert mc.player != null;
        float yaw = mc.player.getYaw();
        if(lockYaw.get())
        {
            mc.player.bodyYaw = 0.f;
            mc.player.headYaw = 0.f;
        }
        if(v && vehicle.get())
        {
            vehicleEntity.setPos(x, y, z);
            VehicleMoveC2SPacket packet = new VehicleMoveC2SPacket(vehicleEntity);
            sendPacket(packet);
            return;
        }
        sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, mc.player.getPitch(), mc.player.isOnGround()));
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
