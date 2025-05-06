package addon.fmod.utils;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class WarpUtils {
    public static void warpTo(Vec3d pos, int ticks) {
        if (mc.player == null) return;

        // Exploit
        for (int i = 0; i < ticks; i++) {
            moveTo(mc.player.getPos());
        }

        // Tp to pos
        moveTo(pos);
    }

    public static void moveTo(Vec3d pos)
    {
        if (mc.player == null) return;

        if (mc.player.getVehicle() != null) {
            mc.player.getVehicle().setPosition(pos);
            mc.player.networkHandler.sendPacket(new VehicleMoveC2SPacket(pos, mc.player.getVehicle().getYaw(), mc.player.getVehicle().getPitch(),  mc.player.horizontalCollision));
        } else {
            mc.player.setPosition(pos);
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, true,  mc.player.horizontalCollision));
        }
    }

    // Todo: this doesn't seem to work anymore on paper 1.21.5
    public static void clipUpDown(Vec3d targetPos) {
        if (mc.player == null) return;

        Vec3d pos = mc.player.getPos();

        for (int i = 0; i < 15; i++) {  // Send a lot of the same movement packets to increase max travel distance
            moveTo(pos);
        }

        pos = pos.add(0, 100, 0);  // Up
        moveTo(pos);

        pos = new Vec3d(targetPos.x, pos.y, targetPos.z);  // Horizontal
        moveTo(pos);

        moveTo(targetPos);  // Down
    }

    /**
     * Automatically go through the nearest blocks in the given direction.
     * Credits to @EnderKill98 for the original code.
     */
    public static Vec3d getAutoClipPos(int direction) {
        if (mc.player == null || mc.world == null) return null;

        boolean inside = false;
        for (float i = 0; i < 150; i += 0.25) {
            Vec3d pos = mc.player.getPos();
            Vec3d targetPos = pos.add(0, direction * i, 0);

            boolean collides = !mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().offset(targetPos.subtract(pos)));

            if (!inside && collides) {  // Step 1: Into the blocks
                inside = true;
            } else if (inside && !collides) {  // Step 2: Out of the blocks
                return targetPos;
            }
        }

        return null;  // Nothing found
    }
}
