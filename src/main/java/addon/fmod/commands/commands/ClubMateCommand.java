package addon.fmod.commands.commands;

import addon.fmod.utils.WarpUtils;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;

// This will only work on the LO server, it's otherwise useless.

public class ClubMateCommand extends Command {
    public ClubMateCommand() {
        super("clubmate", "Get clubmate!", "cm", "clubm", "cmate");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder)
    {
        builder.then(literal("clubmate")).executes(context ->
            {
                if (mc.player == null || mc.gameMode == null) return 0;

                // Move `direction` blocks into viewing direction
                WarpUtils.warpTo(new Vec3(1331, 89, 1330), 18);  // Next to chest

                BlockPos chest = new BlockPos(1331, 89, 1331); // chest

                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(
                    new Vec3(chest.getX(), chest.getY(), chest.getZ()),
                    Direction.DOWN,
                    chest,
                    false
                ));
                return SINGLE_SUCCESS;
            }
        );
    }

}
