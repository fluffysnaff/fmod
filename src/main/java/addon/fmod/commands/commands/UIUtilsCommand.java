package addon.fmod.commands.commands;

import addon.fmod.modules.UIUtils;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public final class UIUtilsCommand extends Command {
    public UIUtilsCommand() { super("ui-utils", "Control UI Utils and its session packet queue.", "uiutils"); }

    @Override public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(context -> {
            UIUtils owner = UIUtils.get();
            if (owner == null || !owner.isActive()) error("Enable ui-utils first.");
            else if (mc.gui.screen() instanceof AbstractContainerScreen<?> container) owner.open(container);
            else info("Open an inventory and select UI Utils. Actions: restore, clear, flush, controls, status.");
            return SINGLE_SUCCESS;
        });
        builder.then(literal("restore").executes(context -> { mc.schedule(() -> UIUtils.get().restore()); return SINGLE_SUCCESS; }));
        builder.then(literal("clear").executes(context -> { UIUtils.get().discard(); return SINGLE_SUCCESS; }));
        builder.then(literal("flush").executes(context -> { info("%s", UIUtils.get().flushStatus()); return SINGLE_SUCCESS; }));
        builder.then(literal("controls").executes(context -> { UIUtils.get().toggleControls(); return SINGLE_SUCCESS; }));
        builder.then(literal("status").executes(context -> {
            UIUtils owner = UIUtils.get();
            info("Enabled: %s, send: %s, delay: %s, queued: %d/%d.", owner.isActive(), owner.sending(), owner.delaying(), owner.queued(), UIUtils.MAX_DELAYED_PACKETS);
            return SINGLE_SUCCESS;
        }));
    }
}
