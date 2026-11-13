package addon.fmod.commands.commands;

import addon.fmod.modules.BaseScout;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public final class BaseScoutCommand extends Command {
    public BaseScoutCommand() { super("base-scout", "Control BaseScout and print its local report.", "bs"); }

    @Override public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(context -> { scout().printStatus(); return SINGLE_SUCCESS; });
        builder.then(literal("status").executes(context -> { scout().printStatus(); return SINGLE_SUCCESS; }));
        builder.then(literal("start").executes(context -> { scout().startScouting(); return SINGLE_SUCCESS; }));
        builder.then(literal("stop").executes(context -> { scout().stopScouting(); return SINGLE_SUCCESS; }));
        builder.then(literal("next").executes(context -> { scout().findAnotherCandidate(); return SINGLE_SUCCESS; }));
        builder.then(literal("import").executes(context -> { scout().chooseImport(); return SINGLE_SUCCESS; }));
        builder.then(literal("help").executes(context -> { scout().printHelp(); return SINGLE_SUCCESS; }));
        builder.then(literal("hud").executes(context -> { scout().enableHud(); return SINGLE_SUCCESS; })
            .then(literal("on").executes(context -> { scout().enableHud(); return SINGLE_SUCCESS; }))
            .then(literal("off").executes(context -> { scout().hideHud(); return SINGLE_SUCCESS; })));
    }

    private BaseScout scout() { return Modules.get().get(BaseScout.class); }
}
