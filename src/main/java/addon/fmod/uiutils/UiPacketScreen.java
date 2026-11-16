package addon.fmod.uiutils;

import addon.fmod.modules.UIUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;

public final class UiPacketScreen extends UIUtilsScreen {
    private final UIUtilsScreen parent;
    private boolean click = true, respectDelay = true;
    private ContainerInput action = ContainerInput.PICKUP;
    private EditBox sync, revision, slot, buttonId, repeats;
    private Button actionButton;
    private String syncValue, revisionValue, slotValue = "0", buttonValue = "0", repeatValue = "1";
    private String result = "Packets use an empty changed-slot map and the current cursor hash.";

    public UiPacketScreen(UIUtils owner, AbstractContainerScreen<?> container, UIUtilsScreen parent) {
        super(owner, container);
        this.parent = parent;
        syncValue = Integer.toString(container.getMenu().containerId);
        revisionValue = Integer.toString(container.getMenu().getStateId());
    }

    private EditBox field(String name, String value, int column, int y, java.util.function.Consumer<String> remember) {
        var box = new EditBox(font, left + column * (columnWidth + 4), y, columnWidth, 20, Component.literal(name));
        box.setMaxLength(12);
        box.setValue(value);
        box.setResponder(remember);
        return addRenderableWidget(box);
    }

    @Override protected void init() {
        layout(230);
        button("Click slot", "Fabricate ServerboundContainerClickPacket.", 0, top + 22, () -> { click = true; rebuildWidgets(); });
        button("Button click", "Fabricate ServerboundContainerButtonClickPacket.", 1, top + 22, () -> { click = false; rebuildWidgets(); });
        sync = field("Sync ID", syncValue, 0, top + 58, value -> syncValue = value);
        revision = field("Revision", revisionValue, 1, top + 58, value -> revisionValue = value);
        slot = field("Slot", slotValue, 0, top + 94, value -> slotValue = value);
        buttonId = field("Button", buttonValue, 1, top + 94, value -> buttonValue = value);
        repeats = field("Times to send", repeatValue, 0, top + 130, value -> repeatValue = value);
        actionButton = button(action.name(), "Cycle the slot action. The numeric button is separate from this action.", 1, top + 130, () -> {
            var values = ContainerInput.values();
            action = values[(action.ordinal() + 1) % values.length];
            actionButton.setMessage(Component.literal(action.name()));
        });
        revision.active = slot.active = actionButton.active = click;
        button("Respect delay: " + respectDelay, "When off, send immediately. Send packets: false still blocks submission.", 0, top + 158, () -> { respectDelay = !respectDelay; rebuildWidgets(); });
        button("Send packet", "Submit up to 100 repeats. This does not confirm server acceptance.", 1, top + 158, this::submit);
        button("Use current IDs", "Copy the current menu's sync ID and revision into the fields.", 0, top + 208, () -> {
            sync.setValue(Integer.toString(container.getMenu().containerId));
            revision.setValue(Integer.toString(container.getMenu().getStateId()));
        });
        button("Back", "Return to UI Utils.", 1, top + 208, this::onClose);
    }

    private void submit() {
        if (!valid()) { result = "Connection or container changed."; return; }
        try {
            var fields = UiPacketFields.parse(click, sync.getValue(), revision.getValue(), slot.getValue(), buttonId.getValue(), repeats.getValue());
            result = owner.fabricate(click, fields, action, respectDelay);
        } catch (IllegalArgumentException error) {
            result = error.getMessage();
        } catch (RuntimeException error) {
            result = "Submission stopped. Some packets may already have been sent.";
        }
    }

    @Override public void onClose() { if (valid()) minecraft.gui.setScreen(parent); else super.onClose(); }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        heading(graphics, click ? "Fabricate: click slot" : "Fabricate: button click");
        String[] labels = { "Sync ID", "Revision", "Slot (signed short)", click ? "Button (signed byte)" : "Button ID", "Times to send (1-100)", "Action" };
        for (int i = 0; i < labels.length; i++) graphics.text(font, labels[i], left + (i % 2) * (columnWidth + 4), top + 47 + (i / 2) * 36, 0xFFB8C5D6);
        int y = top + 184;
        for (var line : font.split(Component.literal(result), panelWidth)) {
            graphics.text(font, line, left, y, 0xFFCEE1A7);
            y += 10;
            if (y >= top + 204) break;
        }
        extractWidgets(graphics, mouseX, mouseY, delta);
    }
}
