package addon.fmod.modules;

import addon.fmod.FMod;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

public class InstaMine extends Module {

    public InstaMine() {
        super(FMod.CATEGORY, "insta-mine", "Attempts to break blocks very quickly if breakable fast enough.");
        // Original description: "Attempts to stop mining at 70% progress"
        // The code's logic (speed > 0.5F) suggests instant breaking if already fast, rather than stopping progress.
        // Clarified description to better match behavior.
    }

    public void doInstaMine(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (mc.world == null || mc.player == null) {
            cir.setReturnValue(false);
            return;
        }

        // Check if this module (InstaMine) is active.
        if (this.isActive()) {
            BlockState blockState = mc.world.getBlockState(pos);
            // calcBlockBreakingDelta returns progress per tick. > 0.5 means breakable in < 2 ticks.
            double speed = blockState.calcBlockBreakingDelta(mc.player, mc.world, pos);

            if (!blockState.isAir() && speed > 0.5F) {
                // Simulate breaking the block client-side
                mc.world.breakBlock(pos, true, mc.player);

                // Send packets to server to start and immediately stop mining
                // This sequence can sometimes trick servers into insta-breaking.
                Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, direction));
                Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, direction));

                // Set cir to true to indicate the block attack was handled (and potentially cancel further vanilla processing)
                cir.setReturnValue(true);
            }
        }
        else{
            cir.setReturnValue(false);
        }
    }
}
