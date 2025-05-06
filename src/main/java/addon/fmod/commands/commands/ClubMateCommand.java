package addon.fmod.commands.commands;

import addon.fmod.utils.WarpUtils;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.Hand;

// This will only work on the LO server, it's otherwise useless.

public class ClubMateCommand extends Command {
    public ClubMateCommand() {
        super("clubmate", "Get clubmate!", "cm", "clubm", "cmate");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder)
    {
        builder.then(literal("clubmate")).executes(context ->
            {
                if (mc.player == null || mc.interactionManager == null) return 0;

                // Move `direction` blocks into viewing direction
                WarpUtils.warpTo(new Vec3d(1331, 89, 1330), 18);  // Next to chest

                BlockPos chest = new BlockPos(1331, 89, 1331); // chest

                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(
                    new Vec3d(chest.getX(), chest.getY(), chest.getZ()),
                    Direction.DOWN,
                    chest,
                    false
                ));
                return SINGLE_SUCCESS;
            }
        );
    }

}
