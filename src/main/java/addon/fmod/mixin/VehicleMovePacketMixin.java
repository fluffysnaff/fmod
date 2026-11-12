package addon.fmod.mixin;

import addon.fmod.modules.LiveWalk;
import addon.fmod.utils.RoundUtils;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.core.PositionAndRotation;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerboundMoveVehiclePacket.class)
public abstract class VehicleMovePacketMixin {
    // Modify the constructor input instead of trying to mutate a record's final fields.
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static PositionAndRotation roundPosition(PositionAndRotation movingTo) {
        if (Modules.get() == null) return movingTo;
        LiveWalk liveWalk = Modules.get().get(LiveWalk.class);
        if (liveWalk == null || !liveWalk.isActive() || !liveWalk.vehicleEnabled()) return movingTo;
        Vec3 position = movingTo.position();
        return PositionAndRotation.of(new Vec3(
            RoundUtils.roundCoordOnPositionPacket(position.x),
            position.y,
            RoundUtils.roundCoordOnPositionPacket(position.z)
        ), movingTo.yRot(), movingTo.xRot());
    }
}
