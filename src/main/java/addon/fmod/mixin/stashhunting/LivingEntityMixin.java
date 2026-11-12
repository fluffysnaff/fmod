package addon.fmod.mixin.stashhunting;

import addon.fmod.stashhunting.modules.NoJumpDelay;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow private int noJumpDelay;

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void resetJumpDelay(CallbackInfo ci) {
        if ((Object) this != mc.player || Modules.get() == null) return;
        NoJumpDelay module = Modules.get().get(NoJumpDelay.class);
        if (module != null && module.isActive()) noJumpDelay = 0;
    }
}
