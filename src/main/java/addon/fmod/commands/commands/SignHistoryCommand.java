package addon.fmod.commands.commands;

import addon.fmod.features.signs.SignHistoryBrowser;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public final class SignHistoryCommand extends Command {
    public SignHistoryCommand(){super("signhistory","Browse privately observed original and changed sign text.","shistory");}
    @Override public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder){builder.executes(context->{SignHistoryBrowser.open();return SINGLE_SUCCESS;});}
}
