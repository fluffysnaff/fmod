package addon.fmod.mixin;

import addon.fmod.modules.notexturerotations.NoTextureRotations;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.block.AbstractBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.AbstractBlockState.class)
public class AbstractBlockBlockStateMixin {
    @Inject(method = "getModelOffset", at = @At("HEAD"), cancellable = true)
    public void disableOffsetBasedOnPos(CallbackInfoReturnable<Vec3d> cir, @Local(argsOnly = true) LocalRef<BlockPos> posRef) {
        if (Modules.get() == null) return;
        NoTextureRotations noTextureRotations = Modules.get().get(NoTextureRotations.class);
        if(noTextureRotations != null && noTextureRotations.isActive()) {
            switch(noTextureRotations.getCurrentMode()) {
                case NO_ROTATIONS -> cir.setReturnValue(Vec3d.ZERO);
                case SECURE_RANDOM -> posRef.set(BlockPos.fromLong(NoTextureRotations.secureRandom.nextLong()));
            }
        }
    }
}
