package addon.fmod.modules;

import addon.fmod.FMod;
import addon.fmod.hud.BaseScoutHud;
import addon.fmod.modules.basescout.BaseScoutChatUpdates;
import addon.fmod.modules.basescout.XaeroChunkHistory;
import addon.fmod.utils.BaseScoutExclusions;
import addon.fmod.utils.BaseScoutExclusions.Entry;
import addon.fmod.utils.BaseScoutExclusions.Index;
import addon.fmod.utils.BaseSitePlanner;
import addon.fmod.utils.BaseSitePlanner.*;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.mixin.MinecraftServerAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.XAnchor;
import meteordevelopment.meteorclient.systems.hud.YAnchor;
import meteordevelopment.meteorclient.utils.misc.FileDialogs;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;

import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

public final class BaseScout extends Module {
    private static final ThreadPoolExecutor FILE_WORKER = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(1), task -> { Thread thread = new Thread(task, "fmod-base-scout-files"); thread.setDaemon(true); return thread; });
    private final SettingGroup general = settings.getDefaultGroup();
    private final SettingGroup filters = settings.createGroup("Candidate filters", false);
    private final Setting<Boolean> showHud = general.add(new BoolSetting.Builder()
        .name("show-hud").description("Show the BaseScout HUD while scouting. A missing element is placed automatically; existing positions and HUD preferences are preserved.")
        .defaultValue(true).onChanged(value -> { if (value && isActive() && mc.level != null) ensureHud(); }).build());
    private final Setting<Boolean> chatUpdates = general.add(new BoolSetting.Builder()
        .name("chat-updates").description("Local Meteor module messages for significant report changes and operation results. Never sends server chat or candidate coordinates.")
        .defaultValue(true).build());
    private final Setting<Integer> chatInterval = general.add(new IntSetting.Builder()
        .name("chat-update-interval-seconds").description("Minimum interval between automatic area reports. Changes are coalesced into the latest report.")
        .defaultValue(10).range(5, 120).sliderRange(5, 60).visible(chatUpdates::get).build());
    private final Setting<Integer> knownClearance = general.add(new IntSetting.Builder()
        .name("known-location-clearance").description("Minimum Overworld-block distance from private locations, including Nether projections. Saved radii can be larger.")
        .defaultValue(10_000).range(0, BaseSitePlanner.WORLD_LIMIT).sliderRange(0, 100_000).onChanged(value -> rebuildIndex()).build());
    private final Setting<Integer> surveyRadius = general.add(new IntSetting.Builder()
        .name("scouting-radius-chunks").description("Continuously inspect this radius around you, one received chunk per tick. Missing chunks are never requested.")
        .defaultValue(4).range(1, 8).sliderRange(1, 8).onChanged(value -> clearObservations()).build());
    private final Setting<Integer> minSpawnDistance = filters.add(new IntSetting.Builder()
        .name("minimum-spawn-distance").description("Minimum horizontal distance from 0,0 in Overworld blocks. Not a safe distance.")
        .defaultValue(1_000_000).range(0, BaseSitePlanner.WORLD_LIMIT).sliderRange(0, 10_000_000).onChanged(value -> invalidateCandidate()).build());
    private final Setting<Integer> maxCoordinate = filters.add(new IntSetting.Builder()
        .name("maximum-coordinate").description("Sample X and Z independently across this positive/negative Overworld extent.")
        .defaultValue(25_000_000).range(1, BaseSitePlanner.WORLD_LIMIT).sliderRange(1_000_000, BaseSitePlanner.WORLD_LIMIT).onChanged(value -> invalidateCandidate()).build());
    private final Setting<Integer> borderMargin = filters.add(new IntSetting.Builder()
        .name("border-margin").description("Keep candidates this far inside the actual Overworld border.")
        .defaultValue(250_000).range(0, BaseSitePlanner.WORLD_LIMIT).sliderRange(0, 1_000_000).onChanged(value -> invalidateCandidate()).build());
    private final Setting<Integer> routeClearance = filters.add(new IntSetting.Builder()
        .name("route-clearance").description("Avoid axes, diagonals, configured square rings and session routes by this many Overworld blocks.")
        .defaultValue(10_000).range(0, BaseSitePlanner.WORLD_LIMIT).sliderRange(0, 100_000).onChanged(value -> invalidateCandidate()).build());
    private final Setting<Integer> roundClearance = filters.add(new IntSetting.Builder()
        .name("round-coordinate-clearance").description("Avoid either coordinate near a multiple of 100,000.")
        .defaultValue(64).range(0, 49_999).sliderRange(0, 1024).onChanged(value -> invalidateCandidate()).build());
    private final Setting<String> squareRings = filters.add(new StringSetting.Builder()
        .name("square-ring-radii").description("Known square ring roads, comma-separated in Overworld blocks. Multiply Nether radii by eight.")
        .defaultValue("").onChanged(value -> invalidateCandidate()).build());

    private final SecureRandom random = new SecureRandom();
    private final BaseScoutChatUpdates reportAnnouncements = new BaseScoutChatUpdates();
    private final Map<ChunkPos, Clues> observations = new HashMap<>();
    private final Set<ChunkPos> dirty = new LinkedHashSet<>();
    private final List<Corridor> avoidedRoutes = new ArrayList<>();
    private final boolean historyInstalled = FabricLoader.getInstance().isModLoaded("xaeroplus") && FabricLoader.getInstance().isModLoaded("xaeroworldmap");
    private boolean historyCompatible = true;
    private ClientLevel observedLevel;
    private ChunkPos centre;
    private int cursor, ticks, entities;
    private Point routeStart, candidate;
    private ResourceKey<Level> routeDimension;
    private List<Entry> knownEntries = List.of();
    private Index knownIndex = new Index(List.of(), 0);
    private String identity;
    private boolean exclusionsReady;
    private IoTask io;
    private HistorySignals history = HistorySignals.NONE;
    private Report report = Report.EMPTY;
    private String status = "Start scouting in a world, then import your private exclusions.";

    public BaseScout() { super(FMod.CATEGORY, "base-scout", "Continuously scouts received chunks and suggests candidates away from known locations and routes."); }
    @Override public void onActivate() { resetSession(); syncServer(); }
    @Override public void onDeactivate() { resetSession(); }
    @EventHandler private void onJoin(GameJoinedEvent event) { resetSession(); syncServer(); }
    @EventHandler private void onLeave(GameLeftEvent event) { resetSession(); }

    private void resetSession() {
        clearObservations();
        observedLevel = null;
        candidate = routeStart = null; routeDimension = null;
        avoidedRoutes.clear(); knownEntries = List.of(); knownIndex = new Index(List.of(), 0);
        identity = null; io = null; exclusionsReady = false;
        reportAnnouncements.reset();
        status = "Join a world to start scouting. Private exclusions reload for each server.";
    }
    private void clearObservations() {
        if (observations != null) observations.clear();
        if (dirty != null) dirty.clear();
        centre = null; cursor = ticks = entities = 0;
        history = HistorySignals.NONE; report = Report.EMPTY;
    }
    private void invalidateCandidate() { candidate = null; status = "Filters changed. Find another candidate."; }
    private boolean ready(boolean overworld) {
        if (mc.level == null || mc.player == null) { status = "Join a world first."; return false; }
        if (!isActive()) toggle();
        if (overworld && !mc.level.dimension().equals(Level.OVERWORLD)) { status = "Find candidates in the Overworld to check its actual border."; return false; }
        syncServer();
        if (identity == null) { status = "The connected server/world identity is unavailable; private imports and candidates are paused."; return false; }
        return true;
    }
    private String currentIdentity() {
        if (mc.level == null) return null;
        Object local = mc.getSingleplayerServer();
        if (local != null) return "world:" + ((MinecraftServerAccessor) local).meteor$getStorageSource()
            .getLevelPath(LevelResource.ROOT).toAbsolutePath().normalize();
        var server = mc.getCurrentServer();
        return server != null ? BaseScoutExclusions.serverIdentity(server.ip) : null;
    }
    private Path privateFile(String key) { return BaseScoutExclusions.serverFile(MeteorClient.FOLDER.toPath().resolve("base-scout-private"), key); }
    private void syncServer() {
        String current = currentIdentity();
        if (current == null || current.equals(identity)) return;
        resetSession(); identity = current;
        int radius = knownClearance.get(); Path saved = privateFile(current);
        submitIo(false, () -> {
            try {
                List<Entry> entries = BaseScoutExclusions.load(saved);
                return new IoResult(entries, new Index(entries, radius), radius, "Loaded " + entries.size() + " private locations.");
            } catch (Exception exception) { throw new CompletionException(exception); }
        });
    }
    private void submitIo(boolean importing, Supplier<IoResult> operation) {
        try {
            var future = CompletableFuture.supplyAsync(operation, FILE_WORKER);
            io = new IoTask(identity, importing, future); exclusionsReady = false; candidate = null;
            status = importing ? "Importing coordinates and saving a private, coordinate-only list..." : "Loading private exclusions...";
        } catch (RejectedExecutionException exception) { status = "A previous file operation is finishing. Try again shortly."; }
    }
    private void completeIo() {
        if (io == null || !io.future.isDone()) return;
        IoTask completed = io; io = null;
        if (!Objects.equals(identity, completed.identity)) return;
        try {
            IoResult result = completed.future.join();
            knownEntries = result.entries; knownIndex = result.index; exclusionsReady = true; status = result.message;
            announce(status, false);
            if (result.radius != knownClearance.get()) rebuildIndex();
        } catch (CompletionException exception) {
            exclusionsReady = false;
            status = "Private list could not be read or saved. Check file access and use coordinate JSON or Discord CSV.";
            announce(status, true);
        }
    }
    private void rebuildIndex() {
        invalidateCandidate();
        if (!isActive() || identity == null || !exclusionsReady) return;
        List<Entry> entries = knownEntries; int radius = knownClearance.get();
        submitIo(false, () -> new IoResult(entries, new Index(entries, radius), radius, "Exclusion distance updated."));
    }
    public void chooseImport() {
        if (!ready(false)) { localInfo(status); return; }
        if (io != null && io.importing) { status = "An import is already in progress."; localInfo(status); return; }
        String selectedServer = identity;
        localInfo("Choose a local coordinate file or chat export. Only coordinates, dimensions and exclusion distances are saved.");
        FileDialogs.openFile("json;csv;txt;html;htm", file -> {
            if (isActive() && Objects.equals(identity, selectedServer) && Objects.equals(currentIdentity(), selectedServer)) importExclusions(file.toPath());
        });
    }
    public void importExclusions(Path source) {
        if (!ready(false)) return;
        if (io != null && io.importing) { status = "An import is already in progress."; return; }
        Path saved = privateFile(identity); int radius = knownClearance.get();
        submitIo(true, () -> {
            try {
                var imported = BaseScoutExclusions.importFile(source, radius);
                if (imported.entries().isEmpty()) throw new IllegalArgumentException("No coordinates found.");
                List<Entry> entries = BaseScoutExclusions.merge(BaseScoutExclusions.load(saved), imported.entries());
                Index index = new Index(entries, radius); BaseScoutExclusions.save(saved, entries);
                return new IoResult(entries, index, radius, "Imported " + imported.entries().size() + " unique locations; "
                    + imported.reviewCount() + " need dimension review. Saved privately; " + imported.skipped() + " messages had no usable coordinates.");
            } catch (Exception exception) { throw new CompletionException(exception); }
        });
    }
    private void resolveDimension(Entry entry, BaseScoutExclusions.Dimension dimension) {
        if (!ready(false)) return;
        if (dimension == BaseScoutExclusions.Dimension.unknown) { status = "Choose the confirmed dimension first."; return; }
        if (io != null) { status = "Wait for the current file operation before reviewing locations."; return; }
        Path saved = privateFile(identity); int radius = knownClearance.get();
        submitIo(true, () -> {
            try {
                List<Entry> existing = BaseScoutExclusions.load(saved);
                if (!existing.contains(entry)) throw new IllegalArgumentException("Entry changed.");
                List<Entry> updated = existing.stream().map(value -> value.equals(entry) ? new Entry(value.x(), value.z(), dimension, value.radius()) : value).toList();
                updated = BaseScoutExclusions.merge(List.of(), updated);
                BaseScoutExclusions.save(saved, updated);
                return new IoResult(updated, new Index(updated, radius), radius, "Confirmed dimension saved privately.");
            } catch (Exception exception) { throw new CompletionException(exception); }
        });
    }
    private Bounds border() {
        var border = mc.level.getWorldBorder(); return new Bounds(border.getMinX(), border.getMaxX(), border.getMinZ(), border.getMaxZ());
    }
    private Policy policy() {
        String input = squareRings.get().strip();
        if (input.length() > 1024) throw new IllegalArgumentException();
        List<Integer> rings = input.isEmpty() ? List.of() : Arrays.stream(input.split(",", -1)).map(String::strip).map(Integer::parseInt).toList();
        return new Policy(minSpawnDistance.get(), maxCoordinate.get(), borderMargin.get(), routeClearance.get(), roundClearance.get(), rings);
    }
    private void generateCandidate() {
        if (!ready(true)) return;
        if (!exclusionsReady) { status = "Wait for the private list to load, or repair/import it if loading failed."; return; }
        candidate = null;
        try {
            candidate = BaseSitePlanner.find(random, policy(), border(), List.of(), avoidedRoutes,
                point -> knownIndex.hit(point.x(), point.z()).isEmpty()).orElse(null);
            status = candidate == null ? "No candidate found in 8192 attempts. Review exclusions, extent and border."
                : "Candidate passes known-location and route filters. Distant terrain remains uninspected.";
        } catch (IllegalArgumentException exception) { status = "Check candidate filters and comma-separated positive ring radii (at most 64)."; }
    }
    public void startScouting() {
        boolean alreadyActive = isActive();
        if (!ready(false)) { localInfo(status); return; }
        if (alreadyActive || !chatUpdates.get()) localInfo("Scouting is active. " + commandHint());
    }
    public void stopScouting() {
        if (isActive()) toggle();
        localInfo("Scouting stopped. Private exclusions remain saved.");
    }
    public void findAnotherCandidate() {
        generateCandidate();
        localInfo(status + (candidate == null ? "" : " Candidate status and coordinates are on the HUD."));
    }
    public void printHelp() { localInfo(commandHint() + " Import opens a local file picker. Hud enables Meteor's HUD, including your other enabled elements. Candidate coordinates stay on the HUD."); }
    public void printStatus() {
        if (!isActive()) { localInfo("Scouting is stopped. " + commandHint()); return; }
        localInfo(report.summary + ". " + report.reason + " " + coverageText());
        localInfo(cluesText());
        localInfo(historyText() + " " + exclusionsText());
        localInfo(status + (candidate == null ? "" : " Check the HUD for candidate status."));
    }
    private String commandHint() {
        String command = Config.get().prefix.get() + "bs";
        return command + " start / stop / next / status / import / hud / help.";
    }
    private void localInfo(String message) { if (mc.player != null) info("%s", message); }
    private void announce(String message, boolean failure) {
        if (!chatUpdates.get() || mc.player == null) return;
        if (failure) error("%s", message); else info("%s", message);
    }
    public boolean showsHud() { return showHud.get(); }
    public void enableHud() {
        if (!ready(false)) { localInfo(status); return; }
        showHud.set(true);
        ensureHud();
        boolean active = false;
        BaseScoutHud first = null;
        for (var element : Hud.get()) if (element instanceof BaseScoutHud scoutHud) {
            if (first == null) first = scoutHud;
            active |= scoutHud.isActive();
        }
        if (!active && first != null) first.toggle();
        Hud.get().active = true;
        localInfo("BaseScout HUD enabled. Meteor's HUD system is on, including your other enabled HUD elements.");
    }
    public void hideHud() { showHud.set(false); localInfo("BaseScout HUD hidden. Other HUD elements are unchanged."); }
    private void ensureHud() {
        if (!showHud.get() || Hud.get() == null) return;
        for (var element : Hud.get()) if (element instanceof BaseScoutHud) return;
        Hud.get().add(BaseScoutHud.INFO, -8, 90, XAnchor.Right, YAnchor.Top);
    }
    private Point currentOverworldPoint() {
        var pos = mc.player.blockPosition(); return new Point(pos.getX(), pos.getZ()).overworld(mc.level.dimension().equals(Level.NETHER));
    }
    private void startRoute() {
        if (!ready(false)) return;
        if (!mc.level.dimension().equals(Level.OVERWORLD) && !mc.level.dimension().equals(Level.NETHER)) { status = "Routes support Overworld and Nether positions."; return; }
        routeStart = currentOverworldPoint(); routeDimension = mc.level.dimension(); status = "Route start remembered. Mark its end in the same dimension.";
    }
    private void endRoute() {
        if (!ready(false)) return;
        if (routeStart == null || !mc.level.dimension().equals(routeDimension)) { status = "Mark a route start in this dimension first."; return; }
        if (avoidedRoutes.size() >= 128) { status = "Session route limit reached (128). Restart scouting to clear routes."; return; }
        avoidedRoutes.add(new Corridor(routeStart, currentOverworldPoint())); routeStart = null; routeDimension = null; invalidateCandidate();
        status = "Session corridor excluded, projected at 8:1 if marked in the Nether.";
    }
    @EventHandler private void onChunk(ChunkDataEvent event) {
        if (mc.player == null || event.chunk().getLevel() != mc.level) return;
        ChunkPos pos = event.chunk().getPos(); if (near(pos) && dirty.size() < totalChunks()) dirty.add(pos);
    }
    @EventHandler private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;
        syncServer(); completeIo();
        if (observedLevel != mc.level) {
            clearObservations(); observedLevel = mc.level; reportAnnouncements.reset(); ensureHud();
            announce("Scouting active. " + commandHint() + (showHud.get() && Hud.get().active ? ""
                : " Use " + Config.get().prefix.get() + "bs hud to enable Meteor's HUD."), false);
        }
        ChunkPos nextCentre = mc.player.chunkPosition();
        boolean moved = !nextCentre.equals(centre);
        centre = nextCentre;
        observations.keySet().removeIf(pos -> !near(pos) || loaded(pos) == null);
        dirty.removeIf(pos -> !near(pos));
        int width = surveyRadius.get() * 2 + 1; ChunkPos selected;
        if (!dirty.isEmpty() && (ticks & 1) == 0) { var iterator = dirty.iterator(); selected = iterator.next(); iterator.remove(); }
        else {
            int index = cursor++ % totalChunks();
            int dx = (index % width + surveyRadius.get()) % width - surveyRadius.get();
            int dz = (index / width + surveyRadius.get()) % width - surveyRadius.get();
            selected = new ChunkPos(centre.x() + dx, centre.z() + dz);
        }
        LevelChunk chunk = loaded(selected);
        if (chunk != null) observations.put(selected, inspect(chunk)); else observations.remove(selected);
        if (++ticks % 20 == 0 || moved) sampleEntities();
        updateReport();
        announceReport();
    }
    private boolean near(ChunkPos pos) {
        if (mc.player == null) return false;
        ChunkPos origin = mc.player.chunkPosition();
        return Math.abs(pos.x() - origin.x()) <= surveyRadius.get() && Math.abs(pos.z() - origin.z()) <= surveyRadius.get();
    }
    private int totalChunks() { int width = surveyRadius.get() * 2 + 1; return width * width; }
    private LevelChunk loaded(ChunkPos pos) { return mc.level.getChunkSource().getChunk(pos.x(), pos.z(), ChunkStatus.FULL, false); }
    private Clues inspect(LevelChunk chunk) {
        int storage = 0, portals = 0, obsidian = 0, construction = 0;
        for (var entity : chunk.getBlockEntities().values()) for (var type : StorageBlockListSetting.STORAGE_BLOCKS) if (entity.getType() == type) { storage++; break; }
        for (var section : chunk.getSections()) {
            if (section.hasOnlyAir()) continue;
            if (section.maybeHas(state -> state.is(Blocks.NETHER_PORTAL))) portals++;
            if (section.maybeHas(state -> state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN))) obsidian++;
            if (section.maybeHas(state -> state.getBlock() instanceof SignBlock || state.getBlock() instanceof BaseTorchBlock
                || state.getBlock() instanceof BaseRailBlock || state.is(Blocks.SCAFFOLDING) || state.is(Blocks.LADDER))) construction++;
        }
        history = HistorySignals.NONE;
        if (historyInstalled && historyCompatible) {
            try { history = XaeroChunkHistory.read(chunk.getPos(), mc.level.dimension()); }
            catch (LinkageError exception) { historyCompatible = false; }
        }
        return new Clues(storage, portals, obsidian, construction, history);
    }
    private void sampleEntities() {
        double minX = (centre.x() - surveyRadius.get()) * 16.0, minZ = (centre.z() - surveyRadius.get()) * 16.0;
        int width = (surveyRadius.get() * 2 + 1) * 16;
        entities = mc.level.getEntitiesOfClass(Entity.class, new AABB(minX, mc.level.getMinY(), minZ, minX + width, mc.level.getMaxY(), minZ + width),
            entity -> entity != mc.player && (entity instanceof Player || entity instanceof ItemFrame || entity instanceof ArmorStand || entity instanceof Villager || entity instanceof AbstractMinecart)).size();
    }
    private void updateReport() {
        int received = 0, storage = 0, portals = 0, obsidian = 0, construction = 0, old = 0, fresh = 0, previous = 0;
        int radius = surveyRadius.get();
        for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) if (loaded(new ChunkPos(centre.x() + dx, centre.z() + dz)) != null) received++;
        for (Clues clues : observations.values()) {
            storage += clues.storage; portals += clues.portals; obsidian += clues.obsidian; construction += clues.construction;
            if (history.oldEnabled && clues.history.oldGeneration) old++;
            if (history.newEnabled && clues.history.newGeneration) fresh++;
            if (history.newEnabled && clues.history.previouslyGenerated) previous++;
        }
        Point point = currentOverworldPoint();
        var known = mc.level.dimension().equals(Level.OVERWORLD) || mc.level.dimension().equals(Level.NETHER)
            ? knownIndex.hit(point.x(), point.z()) : Optional.<BaseScoutExclusions.Hit>empty();
        String summary = known.isPresent() ? "Near known coordinates" : storage + portals + obsidian + construction + entities > 0 ? "Clues detected" : "No clues detected in inspected chunks";
        if (received < totalChunks() || observations.size() < received) summary += "; incomplete coverage";
        String reason = known.map(hit -> "Inside a private location's exclusion radius" + (hit.ambiguous() ? " (dimension needs review)." : "."))
            .orElse("Only received chunks are inspected. No result proves the area is unvisited.");
        report = new Report(summary, reason, received, observations.size(), totalChunks(), storage, portals, obsidian, construction, old, fresh, previous);
    }
    private String candidateText() {
        if (candidate == null) return "Candidate: find a new location to scout";
        if (CoordHider.active() != null) return "Candidate: hidden by Coord Hider";
        if (mc.level != null && mc.level.dimension().equals(Level.OVERWORLD)) {
            Bounds bounds = border();
            if (candidate.x() < bounds.minX() + borderMargin.get() || candidate.x() + 1 > bounds.maxX() - borderMargin.get()
                || candidate.z() < bounds.minZ() + borderMargin.get() || candidate.z() + 1 > bounds.maxZ() - borderMargin.get()) return "Candidate: border changed; find another";
        }
        return "Overworld candidate: X: " + candidate.x() + " Z: " + candidate.z();
    }
    private String coverageText() { return "Coverage: " + report.scanned + " inspected, " + report.received + "/" + report.total + " chunks received."; }
    private String cluesText() {
        return "Clues: storage " + report.storage + "; portal sections " + report.portals + "; obsidian sections "
            + report.obsidian + "; construction sections " + report.construction + "; entities " + entities + ".";
    }
    private String historyText() {
        if (!historyInstalled) return "XaeroPlus history: optional mods absent.";
        if (!historyCompatible) return "XaeroPlus history: incompatible API; unavailable.";
        if (!history.oldEnabled && !history.newEnabled) return "XaeroPlus history: enable its old/new chunk detectors.";
        return "XaeroPlus flags: old generation " + report.old + "; new generation " + report.fresh + "; previously generated " + report.previous + ". Unmarked means unknown.";
    }
    private String exclusionsText() {
        long review = knownEntries.stream().filter(entry -> entry.dimension() == BaseScoutExclusions.Dimension.unknown).count();
        return "Private list: " + knownEntries.size() + " locations; " + review + " need review; "
            + (exclusionsReady ? "loaded for this server." : io != null ? "loading." : "unavailable.");
    }
    public List<String> hudLines() {
        if (mc.player == null || mc.level == null) return List.of("Base Scout: waiting for a world");
        return List.of("Base Scout: " + report.summary, coverageText(), cluesText(), historyText(), exclusionsText(), candidateText(), status);
    }
    private void announceReport() {
        if (!chatUpdates.get() || !exclusionsReady || report.scanned == 0) return;
        String key = report.summary + ":" + report.storage + ":" + report.portals + ":" + report.obsidian + ":"
            + report.construction + ":" + entities + ":" + report.old + ":" + report.fresh + ":" + report.previous;
        if (reportAnnouncements.shouldPublish(key, System.nanoTime(), TimeUnit.SECONDS.toNanos(chatInterval.get()))) {
            localInfo(report.summary + ". " + coverageText() + " " + cluesText());
        }
    }
    @Override public String getInfoString() { return report.summary; }
    @Override public WWidget getWidget(GuiTheme theme) { return new ScoutWidget(theme); }

    private final class ScoutWidget extends WVerticalList {
        private final WLabel summary, reason, coverage, clues, historyLabel, exclusions, candidateLabel, statusLabel;
        private final WButton start;
        private final WSection reviewSection;
        private final WVerticalList reviewRows;
        private List<Entry> shownEntries;
        private ScoutWidget(GuiTheme theme) {
            this.theme = theme;
            start = add(theme.button(isActive() ? "Stop scouting" : "Start scouting")).widget();
            start.action = () -> { if (isActive()) stopScouting(); else startScouting(); };
            add(theme.button("Find another candidate")).widget().action = BaseScout.this::findAnotherCandidate;
            add(theme.button("Import exclusions")).widget().action = BaseScout.this::chooseImport;
            summary = add(theme.label("", 440)).widget(); reason = add(theme.label("", 440)).widget();
            coverage = add(theme.label("", 440)).widget(); clues = add(theme.label("", 440)).widget();
            historyLabel = add(theme.label("", 440)).widget(); exclusions = add(theme.label("", 440)).widget();
            candidateLabel = add(theme.label(candidateText(), 440)).widget(); statusLabel = add(theme.label(status, 440)).widget();
            reviewSection = add(theme.section("Review ambiguous dimensions", false)).widget();
            reviewRows = reviewSection.add(theme.verticalList()).widget();
            reviewRows.minWidth = 440;
            var advanced = add(theme.section("Session route exclusions", false)).widget();
            advanced.add(theme.button("Mark route start here")).widget().action = BaseScout.this::startRoute;
            advanced.add(theme.button("Exclude straight route to here")).widget().action = BaseScout.this::endRoute;
            advanced.add(theme.label("Nether routes project at 8:1. Session route segments are not saved.", 440));
        }
        private void update(WLabel label, String value) { if (!label.get().equals(value)) label.set(value); }
        @Override public boolean render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            reviewSection.visible = CoordHider.active() == null;
            if (shownEntries != knownEntries) {
                shownEntries = knownEntries;
                reviewRows.clear();
                reviewRows.add(theme.label("Unknown dimensions exclude both Overworld and Nether interpretations until you confirm them. Showing up to 20 at a time.", 440));
                knownEntries.stream().filter(entry -> entry.dimension() == BaseScoutExclusions.Dimension.unknown).limit(20).forEach(entry -> {
                    var row = reviewRows.add(theme.horizontalList()).widget();
                    row.add(theme.label("X: " + entry.x() + " Z: " + entry.z()));
                    var dimension = row.add(theme.dropdown(BaseScoutExclusions.Dimension.unknown)).widget();
                    row.add(theme.button("Confirm")).widget().action = () -> resolveDimension(entry, dimension.get());
                });
            }
            if (!start.getText().equals(isActive() ? "Stop scouting" : "Start scouting")) start.set(isActive() ? "Stop scouting" : "Start scouting");
            update(summary, "Nearby area: " + report.summary); update(reason, report.reason);
            update(coverage, coverageText()); update(clues, cluesText()); update(historyLabel, historyText()); update(exclusions, exclusionsText());
            update(candidateLabel, candidateText()); update(statusLabel, status);
            return super.render(renderer, mouseX, mouseY, delta);
        }
    }
    public record HistorySignals(boolean oldEnabled, boolean newEnabled, boolean oldGeneration, boolean newGeneration, boolean previouslyGenerated) {
        public static final HistorySignals NONE = new HistorySignals(false, false, false, false, false);
    }
    private record Clues(int storage, int portals, int obsidian, int construction, HistorySignals history) {}
    private record Report(String summary, String reason, int received, int scanned, int total, int storage, int portals, int obsidian, int construction, int old, int fresh, int previous) {
        private static final Report EMPTY = new Report("Start scouting", "Waiting for received chunks.", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
    private record IoResult(List<Entry> entries, Index index, int radius, String message) {}
    private record IoTask(String identity, boolean importing, CompletableFuture<IoResult> future) {}
}
