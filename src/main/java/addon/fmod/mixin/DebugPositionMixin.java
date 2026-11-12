package addon.fmod.mixin;

import addon.fmod.modules.CoordHider;
import net.minecraft.client.gui.components.debug.DebugEntryPosition;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugEntryPosition.class)
public abstract class DebugPositionMixin {
    @Inject(method = "display", at = @At("HEAD"), cancellable = true)
    private void protectPosition(DebugScreenDisplayer displayer, Level level, LevelChunk clientChunk,
                                   LevelChunk serverChunk, CallbackInfo ci) {
        if (!CoordHider.hideF3()) return;
        CoordHider hider = CoordHider.active();
        if (hider.mode.get() == CoordHider.Mode.Randomize) {
            displayer.addToGroup(DebugEntryPosition.GROUP, hider.displayCoordinates().debugLines());
        }
        ci.cancel();
    }
}
