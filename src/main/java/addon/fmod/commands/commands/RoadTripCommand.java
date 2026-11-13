package addon.fmod.commands.commands;

import addon.fmod.modules.RoadTrip;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public final class RoadTripCommand extends Command {
    public RoadTripCommand(){super("roadtrip","Travel statistics and current-dimension targets.","rt");}
    private RoadTrip module(){return Modules.get().get(RoadTrip.class);}
    @Override public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder){
        builder.executes(context->{module().printStatus();return SINGLE_SUCCESS;});
        builder.then(literal("status").executes(context->{module().printStatus();return SINGLE_SUCCESS;}));
        builder.then(literal("start").executes(context->{if(!module().isActive())module().toggle();module().info("Travel monitoring started. Use .rt target <x> <z> to apply an ETA target.");return SINGLE_SUCCESS;}));
        builder.then(literal("stop").executes(context->{if(module().isActive())module().toggle();return SINGLE_SUCCESS;}));
        builder.then(literal("target").then(argument("x",IntegerArgumentType.integer(-29999984,29999984)).then(argument("z",IntegerArgumentType.integer(-29999984,29999984))
            .executes(context->{module().target(IntegerArgumentType.getInteger(context,"x"),IntegerArgumentType.getInteger(context,"z"));return SINGLE_SUCCESS;}))));
    }
}
