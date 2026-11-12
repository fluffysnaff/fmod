package addon.fmod.mixin;

import addon.fmod.modules.notexturerotations.NoTextureRotations;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public class AbstractBlockBlockStateMixin {
    @Inject(method = "getOffset", at = @At("HEAD"), cancellable = true)
    public void disableOffsetBasedOnPos(CallbackInfoReturnable<Vec3> cir, @Local(argsOnly = true) LocalRef<BlockPos> posRef) {
        if (Modules.get() == null) return;
        NoTextureRotations noTextureRotations = Modules.get().get(NoTextureRotations.class);
        if(noTextureRotations != null && noTextureRotations.isActive()) {
            switch(noTextureRotations.getCurrentMode()) {
                case NO_ROTATIONS -> cir.setReturnValue(Vec3.ZERO);
                case SECURE_RANDOM -> posRef.set(BlockPos.of(NoTextureRotations.secureRandom.nextLong()));
                case RANDOM_OFFSET -> posRef.set(
                    new BlockPos(
                        posRef.get().getX() + NoTextureRotations.offsetsRandomOffsetX,
                        posRef.get().getY() + NoTextureRotations.offsetsRandomOffsetY,
                        posRef.get().getZ() + NoTextureRotations.offsetsRandomOffsetZ
                    )
                );
            }
        }
    }
}
