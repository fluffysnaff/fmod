package addon.fmod.mixin.stashhunting;

import addon.fmod.stashhunting.modules.ElytraFlyPlusPlus;
import addon.fmod.stashhunting.modules.NoJumpDelay;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity
{
    @Shadow
    private int noJumpDelay;

    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Shadow
    public abstract Brain<?> getBrain();

    @Inject(at = @At("HEAD"), method = "aiStep()V")
    private void tickMovement(CallbackInfo ci)
    {
        if ((Object) this != mc.player || Modules.get() == null) return;
        NoJumpDelay module = Modules.get().get(NoJumpDelay.class);
        ElytraFlyPlusPlus efly = Modules.get().get(ElytraFlyPlusPlus.class);
        if ((efly != null && efly.enabled()) || (module != null && module.isActive()))
        {
            this.noJumpDelay = 0;
        }
    }

    @Inject(at = @At("HEAD"), method = "isFallFlying", cancellable = true)
    private void isGliding(CallbackInfoReturnable<Boolean> cir)
    {
        if ((Object) this != mc.player || Modules.get() == null) return;
        ElytraFlyPlusPlus efly = Modules.get().get(ElytraFlyPlusPlus.class);
        if (efly != null && efly.enabled())
        {
            cir.setReturnValue(true);
        }
    }
}
