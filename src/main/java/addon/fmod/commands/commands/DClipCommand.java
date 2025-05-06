package addon.fmod.commands.commands;

import addon.fmod.utils.WarpUtils;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.Vec3d;

public class DClipCommand extends Command {
    public DClipCommand() {
        super("dclip", "Allows to clip in your view direction.", "dc");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder)
    {
        builder.then(argument("distance", DoubleArgumentType.doubleArg()).executes(context -> {
            if (mc.player == null) return 0;

            double distance = context.getArgument("distance", Double.class);
            // Move `direction` blocks into viewing direction
            Vec3d pos = mc.player.getPos();
            // Move into players viewing direction
            Vec3d targetPos = pos.add(mc.player.getRotationVector().normalize().multiply(distance));
            WarpUtils.warpTo(targetPos, 18);

            return SINGLE_SUCCESS;
        }));
    }

}
