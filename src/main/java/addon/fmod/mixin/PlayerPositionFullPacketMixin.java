package addon.fmod.mixin;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import meteordevelopment.meteorclient.systems.modules.Modules;
import addon.fmod.modules.LiveWalk;
import addon.fmod.utils.RoundUtils;

@Mixin(PlayerMoveC2SPacket.Full.class)
public abstract class PlayerPositionFullPacketMixin
{
    // Target the CORRECT constructor signature found by Mixin
    @Inject(method = "<init>(Lnet/minecraft/util/math/Vec3d;FFZZ)V",
        at = @At("RETURN"))
    // Update injector parameters to MATCH the target signature
    private void onInitReturn_Full(Vec3d position, float yaw, float pitch, boolean onGround, boolean changesPosition, CallbackInfo ci)
    {
        LiveWalk liveWalk = Modules.get().get(LiveWalk.class);
        if (liveWalk != null && liveWalk.isActive()) {
            // Cast 'this' to the PARENT packet accessor
            PlayerMoveC2SPacketAccessor accessor = (PlayerMoveC2SPacketAccessor) this;

            // Get the underlying primitive fields after constructor ran
            double currentX = accessor.getPacketX();
            double currentZ = accessor.getPacketZ();

            // Calculate rounded values
            double roundedX = RoundUtils.roundCoordOnPositionPacket(currentX);
            double roundedZ = RoundUtils.roundCoordOnPositionPacket(currentZ);

            // Set modified values back into the fields
            accessor.setPacketX(roundedX);
            accessor.setPacketZ(roundedZ);

        }
    }
}
