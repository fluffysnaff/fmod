package addon.fmod.utils;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class WarpUtils {
    public static void warp(double x1, double y1, double z1, double x2, double y2, double z2) {
        if (mc.player == null)
            return;
        double distance = calculateDistance(x1, y1, z1, x2, y2, z2);
        int packetsRequired = (int) Math.ceil(Math.abs(distance / 10));

        Vec3d startPos = new Vec3d(x1, y1, z1);
        for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
            moveTo(startPos);
        }

        Vec3d endPos = new Vec3d(x2, y2, z2);
        moveTo(endPos);
    }

    private static double calculateDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static void moveTo(Vec3d pos)
    {
        if (mc.player == null) return;

        if (mc.player.getVehicle() != null) {
            mc.player.networkHandler.sendPacket(new VehicleMoveC2SPacket(pos, mc.player.getVehicle().getYaw(), mc.player.getVehicle().getPitch(), true));
        } else {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, true, false));
        }
    }

}
