package addon.fmod.mixin;

import addon.fmod.modules.ShulkerOverviewModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public abstract class ShulkerOverviewMixin {
    @Inject(method="item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V",at=@At("RETURN"))
    private void overview(LivingEntity owner,Level level,ItemStack stack,int x,int y,int seed,CallbackInfo info){
        ShulkerOverviewModule.overlay((GuiGraphicsExtractor)(Object)this,stack,x,y);
    }
}
