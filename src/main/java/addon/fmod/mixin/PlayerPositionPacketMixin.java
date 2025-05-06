package addon.fmod.mixin;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d; // <-- Import Vec3d
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import meteordevelopment.meteorclient.systems.modules.Modules;
import addon.fmod.modules.LiveWalk;
import addon.fmod.utils.RoundUtils;

import static addon.fmod.FMod.LOG;

@Mixin(PlayerMoveC2SPacket.PositionAndOnGround.class)
public class PlayerPositionPacketMixin {

    // Correct the target method signature in the annotation
    @Inject(method = "<init>(Lnet/minecraft/util/math/Vec3d;ZZ)V", // <-- MATCHES EXPECTED (Vec3d, boolean, boolean)
        at = @At("RETURN"))
    // Correct the parameters of the injector method to match the target signature
    private void onInitReturn_PosGround(Vec3d pos, boolean onGround, boolean changePosition, CallbackInfo ci) { // <-- MATCHES (Vec3d, boolean, boolean)
        // Check if the module is active
        if ((Modules.get().get(LiveWalk.class)).isActive()) {
            // Cast 'this' to our accessor (still valid)
            PlayerMoveC2SPacketAccessor accessor = (PlayerMoveC2SPacketAccessor) this;

            // Get current values using the accessor (which reads the fields AFTER construction)
            double currentX = accessor.getPacketX();
            double currentZ = accessor.getPacketZ();

            // Calculate rounded values (logic remains the same)
            double roundedX = RoundUtils.roundCoordOnPositionPacket(currentX);
            double roundedZ = RoundUtils.roundCoordOnPositionPacket(currentZ);

            // Set modified values using the accessor
            accessor.setPacketX(roundedX);
            accessor.setPacketZ(roundedZ);
        }
    }
}
