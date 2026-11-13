package addon.fmod.features.containers;

import addon.fmod.hud.DubCounterHud;
import addon.fmod.modules.ChestIndex;
import addon.fmod.modules.ChestTrackerModule;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.*;

public final class ContainerWorld {
    public static final ContainerWorld INSTANCE = new ContainerWorld();
    private final Minecraft mc = Minecraft.getInstance();
    private final Map<ChunkPos, List<ContainerCounts.Block>> chunks = new HashMap<>();
    private final Set<ChunkPos> dirty = new LinkedHashSet<>();
    private final Set<ChunkPos> truncated = new HashSet<>();
    private Object level;
    private int cursor, totalChunks;
    private List<ContainerCounts.Block> blocks = List.of();
    private long revision;

    private ContainerWorld() {}
    @EventHandler private void onLeave(GameLeftEvent event) { clear(); }
    private void clear() { chunks.clear(); dirty.clear(); truncated.clear(); blocks = List.of(); cursor = totalChunks = 0; level = null; revision++; }
    @EventHandler private void onChunk(ChunkDataEvent event) {
        if (event.chunk().getLevel() == mc.level && dirty.size() < 8192) dirty.add(event.chunk().getPos());
    }
    private boolean needed() {
        Modules modules = Modules.get();
        if (modules == null) return false;
        ChestIndex index = modules.get(ChestIndex.class);
        ChestTrackerModule tracker = modules.get(ChestTrackerModule.class);
        if (index != null && index.isActive() || tracker != null && tracker.isActive() && tracker.autoOpens()) return true;
        if (Hud.get() != null && Hud.get().active) for (var element : Hud.get()) if (element instanceof DubCounterHud && element.isActive()) return true;
        return false;
    }
    @EventHandler private void onTick(TickEvent.Post event) {
        if (mc.level == null || mc.player == null || !needed()) { if (level != null) clear(); return; }
        if (level != mc.level) { clear(); level = mc.level; }
        Map<ChunkPos, LevelChunk> loaded = new LinkedHashMap<>();
        totalChunks = 0;
        for (var access : Utils.chunks()) if (access instanceof LevelChunk chunk) {
            totalChunks++;
            if (loaded.size() < 8192) loaded.put(chunk.getPos(), chunk);
        }
        boolean changed = chunks.keySet().retainAll(loaded.keySet()); dirty.retainAll(loaded.keySet()); truncated.retainAll(loaded.keySet());
        int available = 131072 - chunks.values().stream().mapToInt(List::size).sum();
        List<ChunkPos> positions = new ArrayList<>(loaded.keySet());
        if (positions.isEmpty()) { if (!blocks.isEmpty()) revision++; blocks = List.of(); return; }
        for (int i = 0; i < Math.min(4, positions.size()); i++) {
            ChunkPos pos;
            if (i % 2 == 0 && !dirty.isEmpty()) { var iterator = dirty.iterator(); pos = iterator.next(); iterator.remove(); }
            else pos = positions.get(Math.floorMod(cursor++, positions.size()));
            List<ContainerCounts.Block> found = new ArrayList<>();
            int previousSize = chunks.getOrDefault(pos, List.of()).size();
            int limit = Math.min(4096, available + previousSize);
            boolean cut = false;
            for (BlockEntity entity : loaded.get(pos).getBlockEntities().values()) {
                if (!(entity instanceof Container) && !(entity instanceof EnderChestBlockEntity)) continue;
                if (found.size() >= limit) { cut = true; break; }
                BlockPos position = entity.getBlockPos(), partner = position;
                var state = entity.getBlockState();
                boolean paired = entity instanceof ChestBlockEntity && state.hasProperty(ChestBlock.TYPE)
                    && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE;
                if (paired) partner = position.relative(ChestBlock.getConnectedDirection(state));
                String color = null;
                if (state.getBlock() instanceof ShulkerBoxBlock shulker) color = shulker.getColor() == null ? "undyed" : shulker.getColor().getName();
                int slots = entity instanceof Container container ? container.getContainerSize() : 0;
                found.add(new ContainerCounts.Block(position.getX(), position.getY(), position.getZ(),
                    BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType()).toString(), slots, paired, partner.getX(), partner.getZ(), color));
            }
            List<ContainerCounts.Block> next=List.copyOf(found);
            available += previousSize - next.size();
            if (cut) truncated.add(pos); else truncated.remove(pos);
            if(!next.equals(chunks.put(pos,next)))changed=true;
        }
        if(changed){blocks = chunks.values().stream().flatMap(Collection::stream).limit(131072).toList();revision++;}
    }
    public List<ContainerCounts.Block> blocks() { return blocks; }
    public int scannedChunks() { return chunks.size(); }
    public int receivedChunks() { return totalChunks; }
    public long revision() { return revision; }
    public boolean incomplete() { return !truncated.isEmpty() || totalChunks > 8192; }
}
