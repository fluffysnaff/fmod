package addon.fmod.mixin.stashhunting;

import addon.fmod.stashhunting.modules.TrailMaker;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaeroplus.Globals;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "xaeroplus.module.impl.Drawing", remap = false)
public class XaeroDrawingMixin
{
    @Inject(method= "addHighlight(II)V", at = @At("HEAD"), remap = false)
    public void addHighlight(int chunkX, int chunkZ, CallbackInfo ci)
    {
        TrailMaker trailMaker = Modules.get().get(TrailMaker.class);
        if (trailMaker == null || !trailMaker.isRecording()) return;
        trailMaker.dimension = Globals.getCurrentDimensionId();

        trailMaker.points.add(new ChunkPos(chunkX, chunkZ));
    }
}
