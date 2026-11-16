package addon.fmod.uiutils;

import addon.fmod.modules.UIUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public final class UiChatBox extends EditBox {
    private final UIUtils owner;

    public UiChatBox(Font font, int x, int y, int width, UIUtils owner) {
        super(font, x, y, width, 20, Component.literal("Chat or /command"));
        this.owner = owner;
        setMaxLength(256);
        setHint(Component.literal(width < 180 ? "Chat" : "Chat or /command; Enter to send"));
        setTooltip(Tooltip.create(Component.literal("Chat or /command; Enter to send, Escape to release focus.")));
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (isFocused() && event.isConfirmation()) {
            if (owner.isActive() && !getValue().isBlank()) { owner.chat(getValue()); setValue(""); }
            return true;
        }
        return super.keyPressed(event);
    }
}
