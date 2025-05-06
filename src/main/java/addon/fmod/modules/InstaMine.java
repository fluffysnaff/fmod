package addon.fmod.modules;

import addon.fmod.FMod;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

public class InstaMine extends Module
{
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public InstaMine()
    {
        super(FMod.CATEGORY, "insta-mine", "Attempts to stop mining at 70% progress");
    }

    // Called in mixin/ClientPlayerInteractionManagerMixin.java
    public void doInstaMine(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (mc.world == null || mc.player == null) return;
        InstaMine instaMine = Modules.get().get(InstaMine.class);
        if (instaMine != null && instaMine.isActive()) {
            BlockState blockState = mc.world.getBlockState(pos);
            double speed = blockState.calcBlockBreakingDelta(mc.player, mc.world, pos);

            if (!blockState.isAir() && speed > 0.5F) {
                mc.world.breakBlock(pos, true, mc.player);
                Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, direction));
                Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, direction));
                cir.setReturnValue(true);
            }
        }
    }
}
