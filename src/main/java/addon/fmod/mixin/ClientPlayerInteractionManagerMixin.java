package addon.fmod.mixin;

import addon.fmod.modules.InstaMine;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(MultiPlayerGameMode.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Shadow public abstract boolean destroyBlock(BlockPos pos);
    @Shadow public abstract void stopDestroyBlock();
    @Shadow private void ensureHasSentCarriedItem() { throw new AssertionError(); }
    @Shadow private void startPrediction(ClientLevel level, PredictiveAction action) { throw new AssertionError(); }

    @Inject(method = "startDestroyBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z",
        at = @At("HEAD"), cancellable = true)
    private void attackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (Modules.get() == null) return;
        InstaMine instaMine = Modules.get().get(InstaMine.class);
        if (instaMine == null || !instaMine.canInstaMine(pos)) return;

        ensureHasSentCarriedItem();
        stopDestroyBlock();
        startPrediction(mc.level, sequence -> new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, direction, sequence));
        // Track the local removal under the STOP sequence so a rejected break can be corrected.
        startPrediction(mc.level, sequence -> {
            destroyBlock(pos);
            return new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, direction, sequence);
        });
        cir.setReturnValue(true);
    }
}
