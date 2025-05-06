package addon.fmod.mixin;

import addon.fmod.FMod;
import addon.fmod.modules.NoTextureRotations;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.block.AbstractBlock;
import net.minecraft.util.math.BlockPos;
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
            cir.setReturnValue(42L);
        }
    }
}
