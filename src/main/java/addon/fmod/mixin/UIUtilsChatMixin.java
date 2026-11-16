package addon.fmod.mixin;

import addon.fmod.modules.UIUtils;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class UIUtilsChatMixin {
    @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
    private void fmod$toggleControls(String text, boolean addToHistory, CallbackInfo info) {
        UIUtils owner = UIUtils.get();
        if (owner != null && owner.isActive() && text.strip().equals("^toggleuiutils")) {
            owner.toggleControls();
            info.cancel();
        }
    }
}
