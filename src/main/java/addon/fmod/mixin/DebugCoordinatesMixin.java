package addon.fmod.mixin;

import addon.fmod.modules.CoordHider;
import net.minecraft.client.gui.components.debug.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({DebugEntryPosition.class, DebugEntrySectionPosition.class, DebugEntryLookingAt.class,
    DebugEntryLookingAtEntity.class, DebugEntryHeightmap.class, DebugEntryChunkGeneration.class})
public abstract class DebugCoordinatesMixin {
    @Inject(method = "display", at = @At("HEAD"), cancellable = true)
    private void hideLocation(CallbackInfo ci) {
        if (CoordHider.hideF3()) ci.cancel();
    }
}
