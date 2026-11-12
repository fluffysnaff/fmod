package addon.fmod.mixin;

import addon.fmod.modules.InstaMine;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(MultiPlayerGameMode.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Shadow public abstract boolean destroyBlock(BlockPos pos);
    @Shadow protected abstract void ensureHasSentCarriedItem();
    @Shadow protected abstract void startPrediction(ClientLevel level, PredictiveAction action);

    @Inject(method = "startDestroyBlock", at = @At(value = "HEAD"), cancellable = true)
    private void attackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        InstaMine instaMine = Modules.get().get(InstaMine.class);
        if (instaMine == null || !instaMine.canInstaMine(pos)) return;
        ensureHasSentCarriedItem();
        destroyBlock(pos);
        startPrediction(mc.level, sequence -> new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, direction, sequence));
        startPrediction(mc.level, sequence -> new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, direction, sequence));
        cir.setReturnValue(true);
    }
}
