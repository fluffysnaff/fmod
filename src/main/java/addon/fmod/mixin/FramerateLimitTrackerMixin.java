package addon.fmod.mixin;

import addon.fmod.modules.NoFpsThrottle;
import com.mojang.blaze3d.platform.FramerateLimitTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FramerateLimitTracker.class)
public abstract class FramerateLimitTrackerMixin {
    @Shadow
    private int framerateLimit;

    @Shadow
    public abstract FramerateLimitTracker.FramerateThrottleReason getThrottleReason();

    // The out-of-level menu cap only applies on title/loading screens, so it is left alone.
    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
    private void fmod$overrideThrottle(CallbackInfoReturnable<Integer> info) {
        FramerateLimitTracker.FramerateThrottleReason reason = getThrottleReason();
        if (reason == FramerateLimitTracker.FramerateThrottleReason.NONE
            || reason == FramerateLimitTracker.FramerateThrottleReason.OUT_OF_LEVEL_MENU) return;

        int limit = NoFpsThrottle.overrideLimit(framerateLimit);
        if (limit > 0) info.setReturnValue(limit);
    }
}
