package addon.fmod.commands.commands;

import addon.fmod.modules.PearlLoader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public final class PearlLoaderCommand extends Command {
    public PearlLoaderCommand(){super("pearl-loader","Mark and trigger the operator's local stasis trapdoors.","pl","pearlloader");}
    private PearlLoader module(){return Modules.get().get(PearlLoader.class);}
    @Override public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder){
        builder.executes(context->{module().info("Use .pl mark while looking at the current named chamber; .pl trigger [name] operates it. Empty-hand, reachable trapdoors only.");return SINGLE_SUCCESS;});
        builder.then(literal("mark").executes(context->{if(!module().isActive())module().toggle();module().mark();return SINGLE_SUCCESS;}));
        builder.then(literal("trigger").executes(context->{module().trigger(module().currentRoute());return SINGLE_SUCCESS;})
            .then(argument("name",StringArgumentType.word()).executes(context->{module().trigger(StringArgumentType.getString(context,"name"));return SINGLE_SUCCESS;})));
    }
}
