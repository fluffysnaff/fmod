package addon.fmod.mixin;

import addon.fmod.modules.notexturerotations.NoTextureRotations;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.block.AbstractBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.class)
public class AbstractBlockMixin {
    @Inject(method = "getRenderingSeed", at = @At("HEAD"), cancellable = true)
    public void disableSeedBasedOnPosition(CallbackInfoReturnable<Long> cir) {
        if (Modules.get() == null) return;
        NoTextureRotations noTextureRotations = Modules.get().get(NoTextureRotations.class);
        if(noTextureRotations != null && noTextureRotations.isActive()) {
            switch(noTextureRotations.getCurrentMode()) {
                case NO_ROTATIONS -> cir.setReturnValue(42L);
                case SECURE_RANDOM -> cir.setReturnValue(NoTextureRotations.secureRandom.nextLong());
            }
        }
    }
}
