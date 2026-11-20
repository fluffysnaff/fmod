package addon.fmod.stashhunting.gui;

import addon.fmod.stashhunting.modules.BetterStashFinder;
import addon.fmod.stashhunting.modules.BetterStashFinder.Chunk;
import addon.fmod.stashhunting.modules.searcharea.SearchArea;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/** Stash-finder map, sorting, filtering and analysis in one screen. */
public class StashMapScreen extends WindowScreen {
    private enum SortBy { Total, Distance, Chests, Shulkers, Barrels, Ender, X, Z }

    private static final int[] MIN_TOTALS = {0, 10, 25, 50, 100};
    private static final int MAX_ROWS = 60;

    private final BetterStashFinder finder;
    private final SearchArea searchArea;

    private SortBy sort = SortBy.Total;
    private boolean shulkerOnly = false;
    private int minTotalIndex = 0;

    private WHorizontalList controls;
    private WStashMap map;
    private WTable results;
    private WTable analysis;

    public StashMapScreen(GuiTheme theme) {
        super(theme, "Stash Map");
        this.finder = Modules.get().get(BetterStashFinder.class);
        this.searchArea = Modules.get().get(SearchArea.class);
    }

    @Override
    public void initWidgets() {
        if (finder == null) {
            add(theme.label("Better Stash Finder module is not available."));
            return;
        }

        controls = add(theme.horizontalList()).expandX().widget();
        rebuildControls();

        add(theme.horizontalSeparator()).expandX();

        WHorizontalList mid = add(theme.horizontalList()).expandX().widget();
        map = mid.add(new WStashMap(finder.chunks, searchArea, this::onSelect)).expandX().widget();

        WVerticalList side = mid.add(theme.verticalList()).widget();
        side.add(theme.label("Analysis")).widget();
        analysis = side.add(theme.table()).widget();

        add(theme.horizontalSeparator()).expandX();
        results = add(theme.table()).expandX().widget();

        fillAnalysis();
        fillResults();
    }

    private void rebuildControls() {
        controls.clear();

        WButton sortBtn = controls.add(theme.button("Sort: " + sort)).widget();
        sortBtn.action = () -> {
            sort = SortBy.values()[(sort.ordinal() + 1) % SortBy.values().length];
            rebuildControls();
            fillResults();
        };

        WButton shulkerBtn = controls.add(theme.button(shulkerOnly ? "Shulkers: on" : "Shulkers: off")).widget();
        shulkerBtn.action = () -> {
            shulkerOnly = !shulkerOnly;
            rebuildControls();
            fillResults();
            fillAnalysis();
        };

        WButton minBtn = controls.add(theme.button("Min total: " + MIN_TOTALS[minTotalIndex])).widget();
        minBtn.action = () -> {
            minTotalIndex = (minTotalIndex + 1) % MIN_TOTALS.length;
            rebuildControls();
            fillResults();
            fillAnalysis();
        };

        WButton centre = controls.add(theme.button("Center on me")).widget();
        centre.action = () -> { if (map != null) map.centreOnPlayer(); };

        WButton addAll = controls.add(theme.button("Add all to Xaero")).widget();
        addAll.action = finder::addAllWaypoints;

        WButton clear = controls.add(theme.button("Clear")).widget();
        clear.action = () -> {
            finder.clearFinds();
            if (map != null) map.selected = null;
            fillResults();
            fillAnalysis();
        };
    }

    private List<Chunk> view() {
        int minTotal = MIN_TOTALS[minTotalIndex];
        List<Chunk> list = new ArrayList<>();
        for (Chunk c : finder.chunks) {
            if (c.getTotal() < minTotal) continue;
            if (shulkerOnly && c.shulkers == 0) continue;
            list.add(c);
        }
        list.sort(comparator());
        return list;
    }

    private Comparator<Chunk> comparator() {
        return switch (sort) {
            case Total -> Comparator.comparingInt((Chunk c) -> -c.getTotal());
            case Distance -> Comparator.comparingDouble(this::distance);
            case Chests -> Comparator.comparingInt((Chunk c) -> -c.chests);
            case Shulkers -> Comparator.comparingInt((Chunk c) -> -c.shulkers);
            case Barrels -> Comparator.comparingInt((Chunk c) -> -c.barrels);
            case Ender -> Comparator.comparingInt((Chunk c) -> -c.enderChests);
            case X -> Comparator.comparingInt((Chunk c) -> c.x);
            case Z -> Comparator.comparingInt((Chunk c) -> c.z);
        };
    }

    private double distance(Chunk c) {
        if (mc.player == null) return 0;
        double dx = c.x - mc.player.getX(), dz = c.z - mc.player.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private void onSelect(Chunk c) {
        fillAnalysis();
    }

    private void fillResults() {
        results.clear();
        List<Chunk> list = view();

        results.add(theme.label("Pos"));
        results.add(theme.label("Total"));
        results.add(theme.label("C/B/S/E"));
        results.add(theme.label("Dist"));
        results.add(theme.label(""));
        results.add(theme.label(""));
        results.add(theme.label(""));
        results.row();

        int shown = 0;
        for (Chunk c : list) {
            if (shown++ >= MAX_ROWS) break;
            results.add(theme.label(c.x + ", " + c.z));
            results.add(theme.label(String.valueOf(c.getTotal())));
            results.add(theme.label(c.chests + "/" + c.barrels + "/" + c.shulkers + "/" + c.enderChests));
            results.add(theme.label(mc.player == null ? "-" : (int) distance(c) + "m"));

            WButton goBtn = results.add(theme.button("Goto")).widget();
            goBtn.action = () -> PathManagers.get().moveTo(new BlockPos(c.x, 0, c.z), true);

            WButton wpBtn = results.add(theme.button("WP")).widget();
            wpBtn.action = () -> finder.addWaypointFor(c);

            WMinus delBtn = results.add(theme.minus()).widget();
            delBtn.action = () -> {
                finder.deleteFind(c);
                if (map != null && map.selected == c) map.selected = null;
                fillResults();
                fillAnalysis();
            };

            results.row();
        }

        if (list.size() > MAX_ROWS) {
            results.add(theme.label("... " + (list.size() - MAX_ROWS) + " more (narrow the filter)"));
            results.row();
        }
    }

    private void fillAnalysis() {
        analysis.clear();
        List<Chunk> list = view();

        int chests = 0, barrels = 0, shulkers = 0, ender = 0, hoppers = 0, disp = 0, furnaces = 0, crafters = 0;
        Chunk richest = null;
        Chunk nearest = null;
        double nearestD = Double.MAX_VALUE;
        for (Chunk c : list) {
            chests += c.chests; barrels += c.barrels; shulkers += c.shulkers; ender += c.enderChests;
            hoppers += c.hoppers; disp += c.dispensersDroppers; furnaces += c.furnaces; crafters += c.crafters;
            if (richest == null || c.getTotal() > richest.getTotal()) richest = c;
            double d = distance(c);
            if (mc.player != null && d < nearestD) { nearestD = d; nearest = c; }
        }

        row("Finds", String.valueOf(list.size()));
        row("Chests", String.valueOf(chests));
        row("Barrels", String.valueOf(barrels));
        row("Shulkers", String.valueOf(shulkers));
        row("Ender", String.valueOf(ender));
        row("Hoppers", String.valueOf(hoppers));
        row("Disp/Drop", String.valueOf(disp));
        row("Furnaces", String.valueOf(furnaces));
        row("Crafters", String.valueOf(crafters));
        if (richest != null) row("Richest", richest.x + "," + richest.z + " (" + richest.getTotal() + ")");
        if (nearest != null) row("Nearest", nearest.x + "," + nearest.z + " (" + (int) nearestD + "m)");

        Chunk sel = map == null ? null : map.selected;
        if (sel != null) row("Selected", sel.x + "," + sel.z + " (" + sel.getTotal() + ")");
    }

    private void row(String key, String value) {
        analysis.add(theme.label(key + ":"));
        analysis.add(theme.label(value));
        analysis.row();
    }
}
