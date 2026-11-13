package addon.fmod.commands.commands;

import addon.fmod.features.containers.ContainerBrowser;
import addon.fmod.features.containers.ContainerTracker;
import addon.fmod.modules.ChestTrackerModule;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public final class ChestTrackerCommand extends Command {
    public ChestTrackerCommand(){super("chest-tracker","Search and browse privately recorded containers.","ct","chestindex");}
    @Override public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder){
        builder.executes(context->{ContainerBrowser.open();return SINGLE_SUCCESS;});
        builder.then(literal("open").executes(context->{ContainerBrowser.open();return SINGLE_SUCCESS;}));
        builder.then(literal("search").then(argument("query",StringArgumentType.greedyString()).executes(context->{ContainerTracker.INSTANCE.search(StringArgumentType.getString(context,"query"));ContainerBrowser.open();return SINGLE_SUCCESS;})));
        builder.then(literal("status").executes(context->{Modules.get().get(ChestTrackerModule.class).info("%s",ContainerTracker.INSTANCE.status());return SINGLE_SUCCESS;}));
        builder.then(literal("rescan").executes(context->{ContainerTracker.INSTANCE.rescan();info("Reachable containers may be indexed again.");return SINGLE_SUCCESS;}));
    }
}
