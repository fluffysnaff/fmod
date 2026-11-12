package addon.fmod.mixin.stashhunting;

import addon.fmod.stashhunting.modules.ElytraFlyPlusPlus;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(Entity.class)
public class EntityMixin
{
    @Shadow
    protected UUID uuid;

    @Unique
    private boolean fmod$fakeFly() {
        if ((Object) this != mc.player || Modules.get() == null) return false;
        ElytraFlyPlusPlus module = Modules.get().get(ElytraFlyPlusPlus.class);
        return module != null && module.enabled();
    }

    @Inject(at = @At("HEAD"), method = "getPose()Lnet/minecraft/world/entity/Pose;", cancellable = true)
    private void getPose(CallbackInfoReturnable<Pose> cir)
    {
        if (fmod$fakeFly())
        {
            cir.setReturnValue(Pose.STANDING);
        }
    }

    @Inject(at = @At("HEAD"), method = "isSprinting()Z", cancellable = true)
    private void isSprinting(CallbackInfoReturnable<Boolean> cir)
    {
        if (fmod$fakeFly())
        {
            cir.setReturnValue(true);
        }
    }

    @Inject(at = @At("HEAD"), method = "push(Lnet/minecraft/world/entity/Entity;)V", cancellable = true)
    private void pushAwayFrom(Entity entity, CallbackInfo ci)
    {
        if (fmod$fakeFly() && entity != mc.player)
        {
            ci.cancel();
        }
    }
}
