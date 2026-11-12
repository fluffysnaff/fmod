package addon.fmod.mixin;

import addon.fmod.modules.CoordHider;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.hud.elements.TextHud;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TextHud.class, remap = false)
public abstract class MeteorCoordinateHudMixin {
    @Shadow @Final public Setting<String> text;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void hideCoordinates(CallbackInfo ci) {
        if (CoordHider.hideHud(text.get())) ci.cancel();
    }
}
