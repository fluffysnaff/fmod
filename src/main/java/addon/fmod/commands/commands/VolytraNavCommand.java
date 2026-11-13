package addon.fmod.commands.commands;

import addon.fmod.modules.VolytraFly;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public final class VolytraNavCommand extends Command {
    public VolytraNavCommand() { super("volytra-nav", "Local Autonav diagnostic logs and snapshots.", "vnav"); }
    private VolytraFly module() { return Modules.get().get(VolytraFly.class); }

    @Override public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(context -> { module().printAutonavLog(); return SINGLE_SUCCESS; });
        builder.then(literal("snapshot").executes(context -> { module().saveAutonavSnapshot(); return SINGLE_SUCCESS; }));
        builder.then(literal("log")
            .executes(context -> { module().printAutonavLog(); return SINGLE_SUCCESS; })
            .then(literal("copy").executes(context -> { module().copyAutonavLogPath(); return SINGLE_SUCCESS; }))
            .then(literal("on").executes(context -> { module().setAutonavDiagnostics(true); module().printAutonavLog(); return SINGLE_SUCCESS; }))
            .then(literal("off").executes(context -> { module().setAutonavDiagnostics(false); module().printAutonavLog(); return SINGLE_SUCCESS; })));
    }
}
