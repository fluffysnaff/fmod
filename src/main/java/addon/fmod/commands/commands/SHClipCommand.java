package addon.fmod.commands.commands;

import addon.fmod.utils.WarpUtils;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.Vec3d;

public class SHClipCommand extends Command {
    public SHClipCommand() {
        super("shclip", "Allows to clip horizontally(even thru walls).", "hc", "shc");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder)
    {
        builder.then(argument("distance", DoubleArgumentType.doubleArg()).executes(context -> {
            if (mc.player == null) return 0;

            double distance = context.getArgument("distance", Double.class);
            // Move `direction` blocks into viewing direction
            Vec3d targetPos = mc.player.getPos().add(
                mc.player.getRotationVector().multiply(1, 0, 1).normalize().multiply(distance)
            );

            WarpUtils.clipUpDown(targetPos);

            return SINGLE_SUCCESS;
        }));
    }

}
