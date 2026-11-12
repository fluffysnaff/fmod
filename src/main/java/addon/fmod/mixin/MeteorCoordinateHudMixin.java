package addon.fmod.mixin;

import addon.fmod.modules.CoordHider;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.elements.TextHud;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TextHud.class, remap = false)
public abstract class MeteorCoordinateHudMixin {
    @Shadow @Final public Setting<String> text;
    @Shadow private boolean needsCompile;
    @Shadow protected abstract void runTick(HudRenderer renderer);
    @Unique private String fmod$compiledTemplate;

    @Inject(method = "runTick", at = @At("HEAD"), cancellable = true)
    private void protectCompilation(HudRenderer renderer, CallbackInfo ci) {
        String effective = CoordHider.hudTemplate(text.get());
        if (effective == null) { ci.cancel(); return; }
        if (!effective.equals(fmod$compiledTemplate)) {
            needsCompile = true;
            fmod$compiledTemplate = effective;
        }
    }

    @ModifyArg(method = "runTick", at = @At(value = "INVOKE",
        target = "Lorg/meteordev/starscript/compiler/Parser;parse(Ljava/lang/String;)Lorg/meteordev/starscript/compiler/Parser$Result;"), index = 0)
    private String protectExpression(String source) {
        String effective = CoordHider.hudTemplate(source);
        return effective == null ? "false" : effective;
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void hideCoordinates(HudRenderer renderer, CallbackInfo ci) {
        String effective = CoordHider.hudTemplate(text.get());
        if (effective == null) { ci.cancel(); return; }
        // Invalidate immediately on enable, disable or regeneration, regardless of HUD update delay.
        if (fmod$compiledTemplate != null && !effective.equals(fmod$compiledTemplate)) runTick(renderer);
    }
}
