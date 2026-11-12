package addon.fmod.commands.commands;

import addon.fmod.utils.WarpUtils;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.world.phys.Vec3;

public class DClipCommand extends Command {
    public DClipCommand() {
        super("dclip", "Allows to clip in your view direction.", "dc");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder)
    {
        builder.then(argument("distance", DoubleArgumentType.doubleArg()).executes(context -> {
            if (mc.player == null) return 0;

            double distance = context.getArgument("distance", Double.class);
            // Move `direction` blocks into viewing direction
            Vec3 pos = mc.player.position();
            // Move into players viewing direction
            Vec3 targetPos = pos.add(mc.player.getLookAngle().normalize().scale(distance));
            WarpUtils.warpTo(targetPos, 18);

            return SINGLE_SUCCESS;
        }));
    }

}
