package addon.fmod.commands.commands;

import addon.fmod.modules.StasisCourier;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public final class StashMoverCommand extends Command {
    public StashMoverCommand(){super("stashmover","Control FMod's bed-return stash mover.","sm");}
    private StasisCourier module(){return Modules.get().get(StasisCourier.class);}
    @Override public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder){
        builder.executes(context->{module().printStatus();return SINGLE_SUCCESS;});
        builder.then(literal("status").executes(context->{module().printStatus();return SINGLE_SUCCESS;}));
        builder.then(literal("test").executes(context->{module().beginTrials();return SINGLE_SUCCESS;}));
        builder.then(literal("start").executes(context->{module().beginShipments();return SINGLE_SUCCESS;}));
        builder.then(literal("stop").executes(context->{module().stopAutomation();return SINGLE_SUCCESS;}));
        for(boolean source:new boolean[]{true,false})for(int corner=1;corner<=2;corner++){
            boolean atSource=source;int number=corner;
            builder.then(literal((source?"input":"output")+corner).executes(context->{module().markArea(atSource,number);module().printStatus();return SINGLE_SUCCESS;}));
        }
    }
}
