package addon.fmod.uiutils;

import addon.fmod.modules.UIUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;

public class UIUtilsScreen extends Screen {
    protected final UIUtils owner;
    protected final AbstractContainerScreen<?> container;
    protected final Connection connection;
    protected int left, top, panelWidth, columnWidth;
    private String message = "";
    private Button sending, delaying;

    public UIUtilsScreen(UIUtils owner, AbstractContainerScreen<?> container) {
        super(Component.literal("FMod UI Utils"));
        this.owner = owner;
        this.container = container;
        this.connection = Minecraft.getInstance().getConnection().getConnection();
    }

    protected void layout(int height) {
        panelWidth = Math.min(440, width - 16);
        columnWidth = (panelWidth - 4) / 2;
        left = (width - panelWidth) / 2;
        top = Math.max(4, (this.height - height) / 2);
    }

    protected Button button(String title, String hint, int column, int y, Runnable action) {
        var button = Button.builder(Component.literal(title), pressed -> action.run())
            .bounds(left + column * (columnWidth + 4), y, columnWidth, 20).build();
        button.setTooltip(Tooltip.create(Component.literal(hint)));
        return addRenderableWidget(button);
    }

    @Override protected void init() {
        layout(230);
        int row = top + 42;
        button("Close without packet", "Hide the screen while keeping the client menu and server container open.", 0, row, () -> owner.closeWithoutPacket(this));
        button("De-sync", "Send a container-close packet but keep this menu open on the client.", 1, row, () -> { owner.desync(container); message = "Close packet submitted; client menu retained."; });
        row += 24;
        sending = button("", "Turning sending off discards pending clicks and blocks slot/button packets.", 0, row, () -> { owner.toggleSending(); message = owner.sending() ? "Sending enabled." : "Sending blocked; delayed packets discarded."; });
        delaying = button("", "Buffer slot/button packets. Turning delay off flushes the queue in order.", 1, row, () -> message = owner.toggleDelaying());
        row += 24;
        button("Save GUI", "Save this container screen for the current connection only.", 0, row, () -> { owner.save(container); message = "GUI saved. Default restore key: V."; });
        button("Restore saved GUI", "Reattach the saved client menu. Its server state may be stale.", 1, row, owner::restore);
        row += 24;
        button("Flush queued packets", "Submit pending packets in FIFO order to this connection.", 0, row, () -> message = owner.flushStatus());
        button("Discard queued packets", "Clear the queue without submitting any packets.", 1, row, () -> { owner.discard(); message = "Delayed packets discarded."; });
        row += 24;
        button("Fabricate packet", "Edit a 26.3 container click or button packet without predicting inventory changes.", 0, row, () -> minecraft.gui.setScreen(new UiPacketScreen(owner, container, this)));
        button("Copy GUI title JSON", "Copy the original container title, including its component formatting.", 1, row, () -> {
            try { message = owner.copyTitle(container); }
            catch (RuntimeException error) { message = "Could not encode the GUI title."; }
        });
        row += 24;
        button("Send queued + disconnect", "Submit pending packets, then disconnect immediately. Server processing is not guaranteed.", 0, row, () -> owner.disconnect(true));
        button("Back to container", "Return without sending a container-close packet.", 1, row, this::onClose);
        addRenderableWidget(new UiChatBox(font, left, top + 208, panelWidth, owner));
    }

    protected boolean valid() {
        return owner.isActive() && minecraft.player != null && minecraft.getConnection() != null
            && minecraft.getConnection().getConnection() == connection && minecraft.player.containerMenu == container.getMenu();
    }

    @Override public void tick() {
        if (!valid()) minecraft.gui.setScreen(null);
    }

    @Override public void onClose() {
        returnToContainer();
    }

    public final void returnToContainer() {
        boolean sameMenu = minecraft.player != null && minecraft.getConnection() != null
            && minecraft.getConnection().getConnection() == connection && minecraft.player.containerMenu == container.getMenu();
        minecraft.gui.setScreen(sameMenu ? container : null);
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }

    protected void heading(GuiGraphicsExtractor graphics, String title) {
        graphics.fill(left - 6, top - 4, left + panelWidth + 6, top + 232, 0xED141820);
        graphics.centeredText(font, title, width / 2, top + 3, 0xFFFFFFFF);
    }

    protected void extractWidgets(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        heading(graphics, "FMod UI Utils");
        graphics.text(font, "Sync ID: " + container.getMenu().containerId + "   Revision: " + container.getMenu().getStateId()
            + "   Queued: " + owner.queued() + "/" + UIUtils.MAX_DELAYED_PACKETS, left, top + 20, 0xFFB8C5D6);
        sending.setMessage(Component.literal("Send packets: " + owner.sending()));
        delaying.setMessage(Component.literal("Delay packets: " + owner.delaying()));
        graphics.text(font, font.plainSubstrByWidth(message, panelWidth), left, top + 190, 0xFFCEE1A7);
        extractWidgets(graphics, mouseX, mouseY, delta);
    }
}
