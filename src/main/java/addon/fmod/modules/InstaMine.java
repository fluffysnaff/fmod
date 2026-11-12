package addon.fmod.modules;

import addon.fmod.FMod;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class InstaMine extends Module {
    public InstaMine() {
        super(FMod.CATEGORY, "insta-mine", "Attempts to break blocks very quickly if breakable fast enough.");
    }

    public boolean canInstaMine(BlockPos pos) {
        if (!isActive() || mc.level == null || mc.player == null) return false;
        BlockState state = mc.level.getBlockState(pos);
        return !state.isAir() && state.getDestroyProgress(mc.player, mc.level, pos) > 0.5F;
    }
}
