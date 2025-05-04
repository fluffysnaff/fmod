package addon.fmod.utils;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class WarpUtils {
    public static void moveTo(Vec3d pos)
    {
        if (mc.player == null) return;

        if (mc.player.getVehicle() != null) {
            mc.player.getVehicle().setPosition(pos);
            mc.player.networkHandler.sendPacket(new VehicleMoveC2SPacket(pos, mc.player.getVehicle().getYaw(), mc.player.getVehicle().getPitch(), true));
        } else {
            mc.player.setPosition(pos);
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, true, true));
        }
    }

}
