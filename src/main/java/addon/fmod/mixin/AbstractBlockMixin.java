package addon.fmod.mixin;

import addon.fmod.modules.notexturerotations.NoTextureRotations;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public class AbstractBlockMixin {
    @Inject(method = "getSeed", at = @At("HEAD"), cancellable = true)
    public void disableSeedBasedOnPosition(BlockState state, BlockPos pos, CallbackInfoReturnable<Long> cir) {
        if (Modules.get() == null) return;
        NoTextureRotations noTextureRotations = Modules.get().get(NoTextureRotations.class);
        if(noTextureRotations != null && noTextureRotations.isActive()) {
            switch(noTextureRotations.getCurrentMode()) {
                case NO_ROTATIONS -> cir.setReturnValue(42L);
                case SECURE_RANDOM -> cir.setReturnValue(NoTextureRotations.secureRandom.nextLong());
                case RANDOM_OFFSET -> cir.setReturnValue(Mth.getSeed(pos) + NoTextureRotations.rotationsRandomOffset);
            }
        }
    }
}
