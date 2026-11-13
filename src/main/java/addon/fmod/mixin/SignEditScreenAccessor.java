package addon.fmod.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractSignEditScreen.class)
public interface SignEditScreenAccessor {
    @Accessor("sign") SignBlockEntity fmod$sign();
    @Accessor("slot") SignTextSlot fmod$slot();
    @Accessor("line") int fmod$line();
    @Accessor("line") void fmod$line(int value);
    @Accessor("messages") String[] fmod$messages();
    @Invoker("setMessage") void fmod$message(String value);
}
