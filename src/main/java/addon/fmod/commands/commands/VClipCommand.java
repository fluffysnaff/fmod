package addon.fmod.commands.commands;

import addon.fmod.utils.WarpUtils;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.world.phys.Vec3;

public class VClipCommand extends Command {
    public VClipCommand() {
        super("fvclip", "Clips straight up or down, chunked across ticks for long distances.", "vc");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.then(argument("blocks", DoubleArgumentType.doubleArg()).executes(context -> {
            if (mc.player == null) return 0;

            double blocks = context.getArgument("blocks", Double.class);
            WarpUtils.warp(mc.player.position().add(0, blocks, 0));

            return SINGLE_SUCCESS;
        }));
    }
}
