package addon.fmod.mixin;

import addon.fmod.modules.UIUtils;
import addon.fmod.uiutils.UiChatBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.PreeditEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractSignEditScreen.class)
public abstract class UIUtilsSignMixin {
    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void fmod$chatCharacters(CharacterEvent event, CallbackInfoReturnable<Boolean> info) {
        if (((Screen) (Object) this).getFocused() instanceof UiChatBox chat && chat.isFocused()) info.setReturnValue(chat.charTyped(event));
    }

    @Inject(method = "preeditUpdated", at = @At("HEAD"), cancellable = true)
    private void fmod$chatPreedit(PreeditEvent event, CallbackInfoReturnable<Boolean> info) {
        if (((Screen) (Object) this).getFocused() instanceof UiChatBox chat && chat.isFocused()) info.setReturnValue(chat.preeditUpdated(event));
    }

    @Inject(method = "removed", at = @At("HEAD"), cancellable = true)
    private void fmod$skipSignSubmission(CallbackInfo info) {
        UIUtils owner = UIUtils.get();
        if (owner != null && owner.suppressSignUpdate((Screen) (Object) this)) info.cancel();
    }
}
