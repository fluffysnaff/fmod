package addon.fmod.mixin;

import addon.fmod.modules.CoordHider;
import meteordevelopment.meteorclient.renderer.text.CustomTextRenderer;
import meteordevelopment.meteorclient.renderer.text.VanillaTextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = {CustomTextRenderer.class, VanillaTextRenderer.class}, remap = false)
public abstract class MeteorCoordinateTextMixin {
    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String maskCoordinates(String text) {
        return CoordHider.mask(text);
    }
}
