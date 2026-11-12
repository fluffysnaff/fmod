package addon.fmod.mixin;

import addon.fmod.modules.CoordHider;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiGraphicsExtractor.class)
public abstract class VanillaCoordinateTextMixin {
    @ModifyVariable(method = "text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V",
        at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String maskString(String value) {
        return CoordHider.mask(value);
    }

    @ModifyVariable(method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",
        at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private FormattedCharSequence maskFormatted(FormattedCharSequence value) {
        if (CoordHider.active() == null) return value;
        StringBuilder text = new StringBuilder();
        value.accept((index, style, codePoint) -> { text.appendCodePoint(codePoint); return true; });
        String original = text.toString();
        String masked = CoordHider.mask(original);
        return original.equals(masked) ? value : Component.literal(masked).getVisualOrderText();
    }
}
