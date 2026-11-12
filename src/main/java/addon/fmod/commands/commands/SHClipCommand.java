package addon.fmod.commands.commands;

import addon.fmod.utils.WarpUtils;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.world.phys.Vec3;

public class SHClipCommand extends Command {
    public SHClipCommand() {
        super("shclip", "Allows to clip horizontally(even thru walls).", "hc", "shc");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder)
    {
        builder.then(argument("distance", DoubleArgumentType.doubleArg()).executes(context -> {
            if (mc.player == null) return 0;

            double distance = context.getArgument("distance", Double.class);
            // Move `direction` blocks into viewing direction
            Vec3 targetPos = mc.player.position().add(
                mc.player.getLookAngle().multiply(1, 0, 1).normalize().scale(distance)
            );

            WarpUtils.clipUpDown(targetPos);

            return SINGLE_SUCCESS;
        }));
    }

}
