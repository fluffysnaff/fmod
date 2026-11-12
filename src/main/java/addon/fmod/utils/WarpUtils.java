package addon.fmod.utils;

import net.minecraft.core.PositionAndRotation;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.world.phys.Vec3;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class WarpUtils {
    public static void warpTo(Vec3 pos, int ticks) {
        if (mc.player == null) return;

        // Exploit
        for (int i = 0; i < ticks; i++) {
            moveTo(mc.player.position());
        }

        // Tp to pos
        moveTo(pos);
    }

    public static void moveTo(Vec3 pos)
    {
        if (mc.player == null) return;

        if (mc.player.getVehicle() != null) {
            mc.player.getVehicle().setPos(pos);
            mc.player.connection.send(new ServerboundMoveVehiclePacket(PositionAndRotation.of(pos, mc.player.getVehicle().getYRot(), mc.player.getVehicle().getXRot()), mc.player.horizontalCollision));
        } else {
            mc.player.setPos(pos);
            mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(pos.x, pos.y, pos.z, true,  mc.player.horizontalCollision));
        }
    }

    // Todo: this doesn't seem to work anymore on paper 1.21.5
    public static void clipUpDown(Vec3 targetPos) {
        if (mc.player == null) return;

        Vec3 pos = mc.player.position();

        for (int i = 0; i < 15; i++) {  // Send a lot of the same movement packets to increase max travel distance
            moveTo(pos);
        }

        pos = pos.add(0, 100, 0);  // Up
        moveTo(pos);

        pos = new Vec3(targetPos.x, pos.y, targetPos.z);  // Horizontal
        moveTo(pos);

        moveTo(targetPos);  // Down
    }

    /**
     * Automatically go through the nearest blocks in the given direction.
     * Credits to @EnderKill98 for the original code.
     */
    public static Vec3 getAutoClipPos(int direction) {
        if (mc.player == null || mc.level == null) return null;

        boolean inside = false;
        for (float i = 0; i < 150; i += 0.25) {
            Vec3 pos = mc.player.position();
            Vec3 targetPos = pos.add(0, direction * i, 0);

            boolean collides = !mc.level.noCollision(mc.player, mc.player.getBoundingBox().move(targetPos.subtract(pos)));

            if (!inside && collides) {  // Step 1: Into the blocks
                inside = true;
            } else if (inside && !collides) {  // Step 2: Out of the blocks
                return targetPos;
            }
        }

        return null;  // Nothing found
    }
}
