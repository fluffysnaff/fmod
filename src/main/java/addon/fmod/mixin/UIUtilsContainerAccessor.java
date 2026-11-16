package addon.fmod.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface UIUtilsContainerAccessor {
    @Accessor("leftPos") int fmod$left();
    @Accessor("topPos") int fmod$top();
    @Accessor("imageHeight") int fmod$imageHeight();
}
