package addon.fmod.commands.commands;

import addon.fmod.stashhunting.gui.StashMapScreen;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.gui.GuiThemes;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public class StashMapCommand extends Command {
    public StashMapCommand() {
        super("stash-map", "Opens the stash finder map, sorting and analysis screen.", "stashmap", "smap");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(context -> {
            // Defer: the chat screen closes after the command runs and would otherwise wipe our screen.
            mc.execute(() -> mc.setScreenAndShow(new StashMapScreen(GuiThemes.get())));
            return SINGLE_SUCCESS;
        });
    }
}
