package addon.fmod.uiutils;

import addon.fmod.modules.UIUtils;
import addon.fmod.mixin.UIUtilsContainerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class UIUtilsWidgets {
    private UIUtilsWidgets() {}

    private static Button button(String text, String hint, int x, int y, int width, Runnable action) {
        var button = Button.builder(Component.literal(text), pressed -> action.run()).bounds(x, y, width, 20).build();
        button.setTooltip(Tooltip.create(Component.literal(hint)));
        return button;
    }

    public static void attach(Screen screen, Consumer<AbstractWidget> add) {
        UIUtils owner = UIUtils.get();
        if (owner == null || !owner.controlsVisible()) return;
        if (screen instanceof JoinMultiplayerScreen) {
            add.accept(button("Bypass packs: " + owner.bypassPacks(), "Acknowledge required packs without downloading them.", 4, 4, 150, () -> { owner.toggleBypassPacks(); screen.resize(screen.width, screen.height); }));
            add.accept(button("Force deny: " + owner.forceDeny(), "Also bypass optional packs when bypass is enabled.", 158, 4, 150, () -> { owner.toggleForceDeny(); screen.resize(screen.width, screen.height); }));
            return;
        }
        int chatWidth = Math.min(230, screen.width - 10);
        if (screen instanceof AbstractContainerScreen<?> container) {
            var bounds = (UIUtilsContainerAccessor) container;
            int margin = Math.max(50, bounds.fmod$left() - 10);
            add.accept(button("UI Utils", "Container controls, sync ID/revision, packet queue and fabrication.", 4, 4, Math.min(76, margin), () -> owner.open(container)));
            if (screen.height - bounds.fmod$top() - bounds.fmod$imageHeight() < 28) chatWidth = Math.min(chatWidth, margin);
        } else if (screen instanceof AbstractSignEditScreen) {
            add.accept(button("Close without packet", "Close the editor without submitting a sign update.", 4, 4, 150, () -> owner.closeWithoutPacket(screen)));
            add.accept(button("Disconnect", "Disconnect without submitting the current sign editor.", 158, 4, 100, () -> owner.disconnect(false)));
        } else if (screen instanceof InBedChatScreen) {
            add.accept(button("Client wake up", "Close the sleep screen and wake only the local player without a wake packet.", 4, 4, 120, () -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) mc.player.stopSleepInBed(true, false);
                owner.closeWithoutPacket(screen);
            }));
            return;
        } else if (screen instanceof BookViewScreen || screen instanceof BookEditScreen) {
            if (screen.height < 280) chatWidth = Math.min(chatWidth, Math.max(50, (screen.width - 192) / 2 - 10));
        } else return;
        add.accept(new UiChatBox(screen.getFont(), 5, screen.height - 24, chatWidth, owner));
    }
}
