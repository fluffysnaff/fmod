package addon.fmod.modules;

import addon.fmod.FMod;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class InstaMine extends Module {
    public InstaMine() {
        super(FMod.CATEGORY, "insta-mine", "Attempts to immediately finish blocks you can mine in less than two ticks.");
    }

    public boolean canInstaMine(BlockPos pos) {
        if (!isActive() || mc.level == null || mc.player == null || mc.gameMode == null
            || mc.player.getAbilities().instabuild
            || mc.player.blockActionRestricted(mc.level, pos, mc.gameMode.getPlayerMode())
            || !mc.level.getWorldBorder().isWithinBounds(pos)) return false;

        BlockState state = mc.level.getBlockState(pos);
        return !state.isAir()
            && (!(state.getBlock() instanceof GameMasterBlock) || mc.player.canUseGameMasterBlocks())
            && mc.player.getMainHandItem().canDestroyBlock(state, mc.level, pos, mc.player)
            && state.getDestroyProgress(mc.player, mc.level, pos) > 0.5F;
    }
}
