package addon.fmod.features.containers;

import addon.fmod.modules.ChestIndex;
import addon.fmod.modules.ChestTrackerModule;
import addon.fmod.modules.StasisCourier;
import addon.fmod.utils.PrivateFiles;
import addon.fmod.utils.WorldScope;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/** One passive recorder and one optional auto-opener serve both ChestTracker and ChestIndex. */
public final class ContainerTracker {
    public static final ContainerTracker INSTANCE = new ContainerTracker();
    private static final Set<String> SAFE_MENUS = Set.of("minecraft:chest", "minecraft:trapped_chest", "minecraft:barrel",
        "minecraft:shulker_box", "minecraft:hopper", "minecraft:dispenser", "minecraft:dropper", "minecraft:furnace",
        "minecraft:blast_furnace", "minecraft:smoker", "minecraft:crafter", "minecraft:brewing_stand");
    public record Saved(int version, List<ContainerRecord> containers) {
        public Saved { if (version != 1 || containers == null || containers.size() > 10000) throw new IllegalArgumentException("Invalid catalog."); containers = List.copyOf(containers); }
    }
    private final Minecraft mc = Minecraft.getInstance();
    private final Map<String, ContainerRecord> records = new LinkedHashMap<>();
    private final Set<String> scanned = new HashSet<>();
    private final AtomicReference<String> ioError = new AtomicReference<>();
    private String scope, status = "Enable ChestTracker or ChestIndex in a world.";
    private Path file;
    private CompletableFuture<Saved> loading;
    private boolean ready, dirty;
    private int tick, ownedMenu = -1;
    private Pending pending;
    private BlockPos scanTarget;
    private long scanStarted;
    private String query = "";
    private List<ContainerRecord> view = List.of();

    private ContainerTracker() {}
    private boolean enabled() {
        Modules modules = Modules.get();
        return modules != null && (modules.isActive(ChestTrackerModule.class) || modules.isActive(ChestIndex.class));
    }
    @EventHandler private void onLeave(GameLeftEvent event) { flush(); reset(); }
    private void reset() {
        records.clear(); scanned.clear(); view = List.of(); scope = null; file = null; loading = null; ready = dirty = false;
        pending = null; scanTarget = null; ownedMenu = -1; tick = 0;
    }
    public void flush() {
        if (ready && dirty && file != null) { PrivateFiles.save(file, new Saved(1, List.copyOf(records.values())), ioError::set); dirty = false; }
    }
    public void rescan() { scanned.clear(); status = "Scan history cleared; reachable containers can be revisited."; }
    public String status() { return status; }
    public List<ContainerRecord> records() { return view; }
    public String query() { return query; }
    public void search(String value) { query = value.strip().substring(0, Math.min(value.strip().length(), 256)); }
    private boolean moverOwnsContainers() {
        StasisCourier mover = Modules.get().get(StasisCourier.class);
        return mover != null && mover.isActive();
    }

    private String type(BlockEntity entity) { return BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType()).toString(); }
    private boolean allowed(BlockPos pos) {
        if (mc.level == null || !mc.level.hasChunkAt(pos)) return false;
        BlockEntity entity = mc.level.getBlockEntity(pos);
        if (entity == null) return false;
        if (entity instanceof EnderChestBlockEntity) {
            ChestTrackerModule tracker = Modules.get().get(ChestTrackerModule.class);
            return tracker != null && tracker.recordsEnderChests();
        }
        return SAFE_MENUS.contains(type(entity));
    }
    private BlockPos canonical(BlockPos position) {
        var state = mc.level.getBlockState(position);
        if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos other = position.relative(ChestBlock.getConnectedDirection(state));
            return other.compareTo(position) < 0 ? other : position;
        }
        return position;
    }
    private String key(BlockPos pos) { return mc.level.dimension().identifier() + ":" + pos.getX() + ":" + pos.getY() + ":" + pos.getZ(); }

    @EventHandler private void send(PacketEvent.Send event) {
        if (!enabled() || mc.player == null || mc.level == null) return;
        if (event.packet instanceof ServerboundUseItemOnPacket packet) {
            if (moverOwnsContainers()) { scanTarget = null; ownedMenu = -1; }
            BlockPos pos = packet.hitResult().getBlockPos();
            if (!allowed(pos)) { pending=null; scanTarget=null; ownedMenu=-1; return; }
            if (scanTarget != null && !scanTarget.equals(pos)) { scanTarget = null; ownedMenu = -1; }
            BlockEntity entity = mc.level.getBlockEntity(pos);
            pending = new Pending(mc.level, canonical(pos), type(entity), System.nanoTime());
        } else if (event.packet instanceof ServerboundContainerClickPacket) {
            ownedMenu = -1; scanTarget = null;
            if (ready && pending != null && mc.player.containerMenu.containerId == pending.menuId) capture(mc.player.containerMenu.getItems(), false);
        } else if (event.packet instanceof ServerboundContainerClosePacket) {
            pending=null;
        }
    }
    @EventHandler private void receive(PacketEvent.Receive event) {
        if (!enabled()) return;
        var connection=event.connection;
        if (event.packet instanceof ClientboundOpenScreenPacket packet) mc.execute(() -> {
            if(mc.getConnection()==null||mc.getConnection().getConnection()!=connection)return;
            if (pending != null && pending.level == mc.level && System.nanoTime() - pending.started < TimeUnit.SECONDS.toNanos(5)) {
                pending.menuId = packet.getContainerId();
                if (scanTarget != null && !moverOwnsContainers()) ownedMenu = packet.getContainerId();
            }
        });
        if (event.packet instanceof ClientboundContainerSetContentPacket packet) mc.execute(() -> {
            if(mc.getConnection()==null||mc.getConnection().getConnection()!=connection)return;
            if (pending != null && pending.level == mc.level && pending.menuId == packet.containerId()) pending.contents = packet.items().stream().map(ItemStack::copy).toList();
        });
    }
    @EventHandler private void onTick(TickEvent.Post event) {
        if (!enabled() || mc.player == null || mc.level == null) { if (scope != null) { flush(); reset(); } return; }
        String current = WorldScope.current();
        if (current == null) return;
        current += ":account:" + mc.player.getUUID();
        if (!current.equals(scope)) {
            flush(); reset(); scope = current; file = WorldScope.file("containers", scope);
            try { loading = PrivateFiles.read(file, Saved.class, new Saved(1, List.of())); status = "Loading private container catalog..."; }
            catch (RejectedExecutionException exception) { status = "Private file worker is busy; restart tracking shortly."; }
        }
        if (loading != null && loading.isDone()) {
            try {
                for (ContainerRecord record : loading.join().containers) records.put(record.key(), record);
                ready = true; refreshView(); status = "Container catalog ready: " + records.size() + " recorded locations.";
            } catch (CompletionException exception) { status = "Private container catalog could not be read; repair it before recording."; }
            loading = null;
        }
        String error = ioError.getAndSet(null); if (error != null) {status = error;Modules.get().get(ChestTrackerModule.class).warning("%s",error);}
        if (!ready) return;
        if (pending != null && pending.level != mc.level) { pending = null; scanTarget = null; ownedMenu = -1; }
        if (pending != null && pending.contents != null && mc.player.containerMenu.containerId == pending.menuId) {
            capture(pending.contents, true); pending.contents = null;
            if (scanTarget != null) scanned.add(key(canonical(scanTarget)));
            if (!moverOwnsContainers() && ownedMenu == mc.player.containerMenu.containerId && mc.player.containerMenu.getCarried().isEmpty()) {
                mc.player.closeContainer(); ownedMenu = -1; scanTarget = null;
            }
        }
        if (++tick % 100 == 0) flush();
        scan();
    }
    private void capture(List<ItemStack> contents, boolean verified) {
        AbstractContainerMenu menu = mc.player.containerMenu;
        if (pending == null || pending.level != mc.level || menu == mc.player.inventoryMenu || contents.size() != menu.slots.size()) return;
        List<ItemStack> external = new ArrayList<>();
        for (int i = 0; i < menu.slots.size(); i++) if (menu.getSlot(i).container != mc.player.getInventory()) external.add(contents.get(i));
        if (external.isEmpty() || external.size() > 256) return;
        var totals = ItemContents.summarize(external, true);
        BlockPos pos = pending.position;
        var record = new ContainerRecord(mc.level.dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ(), pending.type,
            external.size(), totals.items(), totals.names(), System.currentTimeMillis(), verified);
        if (records.size() >= 10000 && !records.containsKey(record.key())) { status = "Catalog limit reached (10,000). Existing records remain available."; return; }
        records.put(record.key(), record); dirty = true; refreshView();
        status = verified ? "Recorded server container contents." : "Recorded predicted contents; reopen the container to verify.";
    }
    private void refreshView() { view = List.copyOf(records.values()); }
    private void scan() {
        ChestIndex index = Modules.get().get(ChestIndex.class);
        ChestTrackerModule tracker = Modules.get().get(ChestTrackerModule.class);
        boolean scanning = index != null && index.isActive() || tracker != null && tracker.isActive() && tracker.autoOpens();
        // Yield even between courier runs: a scanner's outstanding open cannot be recalled.
        if (moverOwnsContainers()) { scanTarget = null; ownedMenu = -1; status = "Auto-opening paused while StashMover is enabled; passive recording continues."; return; }
        if (!scanning) {
            if (ownedMenu >= 0 && mc.player.containerMenu.containerId == ownedMenu && mc.player.containerMenu.getCarried().isEmpty()) mc.player.closeContainer();
            scanTarget = null; ownedMenu = -1; return;
        }
        if (scanTarget != null) {
            if (System.nanoTime() - scanStarted > TimeUnit.SECONDS.toNanos(5)) {
                scanned.add(key(canonical(scanTarget))); scanTarget = null; ownedMenu = -1; status = "Container did not open or provide contents; skipped without retries.";
            }
            return;
        }
        if (mc.gui.screen() != null || mc.player.containerMenu != mc.player.inventoryMenu || !mc.player.containerMenu.getCarried().isEmpty()
            || mc.player.isShiftKeyDown() || mc.player.isSpectator() || scanned.size() >= 10000) return;
        double range = index != null && index.isActive() ? index.range() : tracker.openRange();
        ContainerCounts.Block closest = null; double best = range * range;
        for (var block : ContainerWorld.INSTANCE.blocks()) {
            BlockPos pos = new BlockPos(block.x(),block.y(),block.z());
            double distance = mc.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos));
            if (distance > best || !mc.player.isWithinBlockInteractionRange(pos,0) || scanned.contains(key(canonical(pos))) || !allowed(pos)) continue;
            closest = block; best = distance;
        }
        if (closest != null) {
            scanTarget = new BlockPos(closest.x(),closest.y(),closest.z()); scanStarted = System.nanoTime();
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(scanTarget),Direction.UP,scanTarget,false));
        }
    }
    private static final class Pending {
        private final Object level;
        private final BlockPos position;
        private final String type;
        private final long started;
        private int menuId = -1;
        private List<ItemStack> contents;
        private Pending(Object level, BlockPos position, String type, long started) { this.level=level;this.position=position;this.type=type;this.started=started; }
    }
}
