package addon.fmod.mixin;

import addon.fmod.modules.LiveWalk;
import addon.fmod.utils.RoundUtils;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerboundMovePlayerPacket.class)
public abstract class PlayerPositionPacketMixin {
    @Shadow @Final @Mutable protected double x;
    @Shadow @Final @Mutable protected double z;

    // Both Pos and PosRot pass through this constructor, including primitive-coordinate callers.
    @Inject(method = "<init>", at = @At("RETURN"))
    private void roundPosition(CallbackInfo ci) {
        if (Modules.get() == null) return;
        LiveWalk liveWalk = Modules.get().get(LiveWalk.class);
        if (liveWalk != null && liveWalk.isActive()) {
            x = RoundUtils.roundCoordOnPositionPacket(x);
            z = RoundUtils.roundCoordOnPositionPacket(z);
        }
    }
}
