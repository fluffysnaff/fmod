package addon.fmod.mixin;

import addon.fmod.uiutils.UiChatBox;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ AbstractContainerScreen.class, AbstractSignEditScreen.class, BookViewScreen.class, BookEditScreen.class })
public abstract class UIUtilsInputMixin {
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void fmod$chatKeys(KeyEvent event, CallbackInfoReturnable<Boolean> info) {
        Screen screen = (Screen) (Object) this;
        if (!(screen.getFocused() instanceof UiChatBox chat) || !chat.isFocused()) return;
        if (event.isEscape() || event.key() == InputConstants.KEY_TAB) screen.clearFocus();
        else chat.keyPressed(event);
        // Inventory hotkeys and sign editing must not consume chat input, even for printable keys.
        info.setReturnValue(true);
    }
}
