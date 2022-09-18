package com.addon.fmod.modules;

import com.addon.fmod.FMod;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Objects;

public class LiveWalk extends Module
{

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> spoofVelocity = sgGeneral.add(new BoolSetting.Builder()
        .name("spoof-velocity")
        .description("Spoofs your velocity to get rid of external retardation.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> spoofRotate = sgGeneral.add(new BoolSetting.Builder()
        .name("spoof-rotate")
        .description("Spoofs your rotation.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> cancelBad = sgGeneral.add(new BoolSetting.Builder()
        .name("cancel-bad")
        .description("Cancels bad position packets.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onMoveOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("on-move-only")
        .description("Only sends packets when moving.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> lockYP = sgGeneral.add(new BoolSetting.Builder()
        .name("lock-yaw-pitch")
        .description("Blocks base yaw and base pitch.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> vehicle = sgGeneral.add(new BoolSetting.Builder()
        .name("vehicle")
        .description("Enable vehicle bypass")
        .defaultValue(false)
        .build()
    );

    public LiveWalk()
    {
        super(FMod.CATEGORY, "Live Walk", "Bypass Liveoverflow ASP with ease");
    }

    private final HashSet<PlayerMoveC2SPacket> packets = new HashSet<>();
    private final HashSet<VehicleMoveC2SPacket> packetsVehicle = new HashSet<>();
    private boolean shouldCancel = false;

    private double round(double val, int dec)
    {
        int decimals = (int)Math.pow(10, dec);
        long subX = ((long)(val * decimals)) % 10;
        return ((val * decimals) - subX) / decimals;
    }

    private boolean testPosition(double x, double z)
    {
        return ((long) (x * 1000)) % 10 == 0 || ((long) (z * 1000)) % 10 == 0;
    }

    @EventHandler
    public void onSendMovementPackets(SendMovementPacketsEvent.Pre event)
    {
        if (spoofVelocity.get())
        {
            sendVelocity(0, 0, 0);
        }
        assert mc.player != null;
        double dx = mc.player.getPos().x;
        double dz = mc.player.getPos().z;
        if(mc.player.getVehicle() != null && vehicle.get())
        {
            dx = mc.player.getVehicle().getX();
            dz = mc.player.getVehicle().getZ();
        }

        dx = round(dx, 3);
        dz = round(dz, 3);

        if (!testPosition(dx, dz))
        {
            dx = round(dx, 2);
            dz = round(dz, 2);
        }

        if (!testPosition(dx, dz))
        {
            dx = round(dx, 1);
            dz = round(dz, 1);
        }

        if (!testPosition(dx, dz))
        {
            dx = round(dx, 0);
            dz = round(dz, 0);
        }
        if (!testPosition(dx, dz) && cancelBad.get())
        {
            shouldCancel = true;
            return;
        }
        if(lockYP.get())
        {
            mc.player.bodyYaw = mc.player.getYaw();
            mc.player.headYaw = mc.player.getYaw();
        }
        sendPosition(dx, mc.player.getPos().y, dz, mc.player.getVehicle() != null);
        // MeteorClient.LOG.info(String.format("%f, %f, %f, %f", mc.player.prevX, mc.player.prevZ, mc.player.getX(), mc.player.getZ()));
        // mc.player.prevX = mc.player.getX();
        // mc.player.prevY = mc.player.getY();
        // mc.player.prevZ = mc.player.getZ();
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event)
    {
        if ((event.packet instanceof PlayerMoveC2SPacket) && !packets.remove(event.packet)) event.cancel();
        if ((event.packet instanceof VehicleMoveC2SPacket) && vehicle.get() && !packetsVehicle.remove(event.packet)) event.cancel();

        if(shouldCancel)
        {
            event.cancel();
            shouldCancel = false;
        }
        if (onMoveOnly.get())
        {
            assert mc.player != null;
            if (mc.player.input.movementForward == 0)
            {
                event.cancel();
            }
        }
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event)
    {
        if (event.packet instanceof PlayerPositionLookS2CPacket packet && spoofRotate.get())
        {
            assert mc.player != null;
            // mc.player.setYaw(packet.getYaw());
            // mc.player.setPitch(packet.getPitch());
        }
    }

    private void sendPosition(double x, double y, double z, boolean v)
    {
        Vec3d pos = new Vec3d(x, y, z);
        assert mc.player != null;
        if(v && vehicle.get())
        {
            VehicleMoveC2SPacket packet = new VehicleMoveC2SPacket(mc.player);
            sendPacket(packet);
        }
        sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, mc.player.isOnGround()));
        mc.player.setPosition(pos);
    }

    private void sendVelocity(double x, double y, double z)
    {
        assert mc.player != null;
        Vec3d pos = mc.player.getPos().add(x, y, z);
        sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, mc.player.isOnGround()));
        mc.player.setPos(pos.x, pos.y, pos.z);
    }

    private void sendPacket(PlayerMoveC2SPacket packet)
    {
        packets.add(packet);
        Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(packet);
    }
    private void sendPacket(VehicleMoveC2SPacket packet)
    {
        if(!vehicle.get())
            return;
        packetsVehicle.add(packet);
        Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(packet);
    }
}
