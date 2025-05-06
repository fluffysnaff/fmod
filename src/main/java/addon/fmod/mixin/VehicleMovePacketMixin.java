package addon.fmod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import addon.fmod.modules.LiveWalk;
import addon.fmod.utils.RoundUtils;

import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(VehicleMoveC2SPacket.class)
public class VehicleMovePacketMixin {
    // Inject into the CANONICAL RECORD CONSTRUCTOR, right before it returns
    @Inject(method = "<init>(Lnet/minecraft/util/math/Vec3d;FFZ)V", at = @At("RETURN"))

    // Parameters MUST match the target constructor signature
    private void onInitReturn_Vehicle(Vec3d positionArg, float yawArg, float pitchArg, boolean onGroundArg, CallbackInfo ci) {
        LiveWalk liveWalk = Modules.get().get(LiveWalk.class);
        if (liveWalk != null && liveWalk.isActive() && liveWalk.vehicleEnabled()) {
            // Cast 'this' (the record instance) to our accessor
            VehicleMoveC2SPacketAccessor accessor = (VehicleMoveC2SPacketAccessor) this;
            // Get the Vec3d position object that the constructor set
            Vec3d currentPosition = accessor.getPosition();
            // Extract original coordinates
            double currentX = currentPosition.getX();
            double currentY = currentPosition.getY(); // Keep the original Y
            double currentZ = currentPosition.getZ();
            // Calculate rounded X and Z
            double roundedX = RoundUtils.roundCoordOnPositionPacket(currentX);
            double roundedZ = RoundUtils.roundCoordOnPositionPacket(currentZ);
            // Create a NEW Vec3d with modified X/Z and original Y
            Vec3d newPosition = new Vec3d(roundedX, currentY, roundedZ);
            // Use the accessor to replace the record's position field with the new Vec3d
            accessor.setPosition(newPosition);
        }
    }
}
