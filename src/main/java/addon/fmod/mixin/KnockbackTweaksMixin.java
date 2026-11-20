package addon.fmod.mixin;

import addon.fmod.modules.KnockbackTweaks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(MultiPlayerGameMode.class)
public abstract class KnockbackTweaksMixin {
    @WrapOperation(method = "attack", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void fmod$sendAttack(ClientPacketListener connection, Packet<?> packet, Operation<Void> original,
                                  @Local(argsOnly = true, ordinal = 0) Entity target) {
        var modules = Modules.get();
        KnockbackTweaks tweaks = modules == null ? null : modules.get(KnockbackTweaks.class);
        if (tweaks != null && tweaks.isActive() && mc.player != null && target instanceof LivingEntity) {
            tweaks.sendAttack(mc.player, target, () -> original.call(connection, packet));
        } else {
            original.call(connection, packet);
        }
    }
}
