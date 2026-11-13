package addon.fmod.modules.basescout;

import addon.fmod.modules.BaseScout.HistorySignals;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.OldChunks;
import xaeroplus.module.impl.PaletteNewChunks;

/** Loaded only after XaeroPlus and World Map are present. Never changes their detector settings. */
public final class XaeroChunkHistory {
    private XaeroChunkHistory() {}
    public static HistorySignals read(ChunkPos pos, ResourceKey<Level> dimension) {
        OldChunks old = ModuleManager.getModule(OldChunks.class);
        PaletteNewChunks fresh = ModuleManager.getModule(PaletteNewChunks.class);
        boolean oldEnabled = old != null && old.isEnabled(), newEnabled = fresh != null && fresh.isEnabled();
        return new HistorySignals(oldEnabled, newEnabled, oldEnabled && old.isOldChunk(pos.x(), pos.z(), dimension),
            newEnabled && fresh.isNewChunk(pos.x(), pos.z(), dimension), newEnabled && fresh.isInverseNewChunk(pos.x(), pos.z(), dimension));
    }
}
