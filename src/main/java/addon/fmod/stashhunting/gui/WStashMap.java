package addon.fmod.stashhunting.gui;

import addon.fmod.stashhunting.modules.BetterStashFinder.Chunk;
import addon.fmod.stashhunting.modules.searcharea.SearchArea;
import addon.fmod.stashhunting.modules.searcharea.SearchAreaModes;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.function.Consumer;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/** Top-down pannable/zoomable canvas of stash finds, the player and the scan area. */
public class WStashMap extends WWidget {
    private static final Color BG = new Color(18, 20, 26, 255);
    private static final Color GRID = new Color(255, 255, 255, 18);
    private static final Color AXIS = new Color(120, 170, 255, 90);
    private static final Color BORDER = new Color(255, 255, 255, 40);
    private static final Color SCAN_FILL = new Color(80, 160, 255, 30);
    private static final Color SCAN_LINE = new Color(80, 160, 255, 120);
    private static final Color PLAYER = new Color(90, 220, 120, 255);
    private static final Color SPAWN = new Color(255, 210, 70, 220);
    private static final Color SEL = new Color(255, 255, 255, 255);

    private final List<Chunk> chunks;
    private final SearchArea searchArea;
    private final Consumer<Chunk> onSelect;

    private double originX, originZ; // world coordinates at the widget centre
    private double scale = 0.06;      // pixels per block
    private boolean centred;
    private boolean dragging;
    private double mouseX, mouseY;

    public Chunk selected;

    public WStashMap(List<Chunk> chunks, SearchArea searchArea, Consumer<Chunk> onSelect) {
        this.chunks = chunks;
        this.searchArea = searchArea;
        this.onSelect = onSelect;
    }

    public void centreOnPlayer() {
        if (mc.player != null) {
            originX = mc.player.getX();
            originZ = mc.player.getZ();
            centred = true;
        }
    }

    @Override
    protected void onCalculateSize() {
        width = 520;
        height = 360;
    }

    private double sx(double wx) { return x + width / 2 + (wx - originX) * scale; }
    private double sz(double wz) { return y + height / 2 + (wz - originZ) * scale; }

    @Override
    protected void onRender(GuiRenderer renderer, double mx, double my, double delta) {
        if (!centred) centreOnPlayer();
        mouseX = mx;
        mouseY = my;

        renderer.scissorStart(x, y, width, height);
        renderer.quad(x, y, width, height, BG);

        drawGrid(renderer);
        drawScanArea(renderer);
        drawMarker(renderer, 0, 0, SPAWN, 3);

        Chunk hover = null;
        for (Chunk c : chunks) {
            double px = sx(c.x), pz = sz(c.z);
            if (px < x - 8 || px > x + width + 8 || pz < y - 8 || pz > y + height + 8) continue;

            double r = 2.5 + Math.min(6, c.getTotal() / 12.0);
            Color col = colorFor(c.getTotal());
            renderer.quad(px - r, pz - r, r * 2, r * 2, col);
            if (c == selected) drawRing(renderer, px, pz, r + 2, SEL);

            if (mx >= px - r - 1 && mx <= px + r + 1 && my >= pz - r - 1 && my <= pz + r + 1) hover = c;
        }

        drawPlayer(renderer);
        renderer.scissorEnd();

        renderer.quad(x, y, width, 1, BORDER);
        renderer.quad(x, y + height - 1, width, 1, BORDER);
        renderer.quad(x, y, 1, height, BORDER);
        renderer.quad(x + width - 1, y, 1, height, BORDER);

        tooltip = hover != null ? tip(hover) : null;
    }

    private void drawGrid(GuiRenderer renderer) {
        double target = 110.0 / scale;
        long step = 16;
        while (step < target) step *= 2;

        double left = originX - (width / 2) / scale;
        double right = originX + (width / 2) / scale;
        double top = originZ - (height / 2) / scale;
        double bot = originZ + (height / 2) / scale;

        for (long gx = (long) (Math.floor(left / step) * step); gx <= right; gx += step) {
            double px = sx(gx);
            renderer.quad(px, y, 1, height, gx == 0 ? AXIS : GRID);
        }
        for (long gz = (long) (Math.floor(top / step) * step); gz <= bot; gz += step) {
            double pz = sz(gz);
            renderer.quad(x, pz, width, 1, gz == 0 ? AXIS : GRID);
        }
    }

    private void drawScanArea(GuiRenderer renderer) {
        if (searchArea == null || searchArea.chunkLoadMode.get() != SearchAreaModes.Rectangle) return;
        BlockPos a = searchArea.startPos.get();
        BlockPos b = searchArea.targetPos.get();
        if (a.getX() == 0 && a.getZ() == 0 && b.getX() == 0 && b.getZ() == 0) return;

        double x1 = sx(Math.min(a.getX(), b.getX())), x2 = sx(Math.max(a.getX(), b.getX()));
        double z1 = sz(Math.min(a.getZ(), b.getZ())), z2 = sz(Math.max(a.getZ(), b.getZ()));
        renderer.quad(x1, z1, x2 - x1, z2 - z1, SCAN_FILL);
        renderer.quad(x1, z1, x2 - x1, 1, SCAN_LINE);
        renderer.quad(x1, z2, x2 - x1, 1, SCAN_LINE);
        renderer.quad(x1, z1, 1, z2 - z1, SCAN_LINE);
        renderer.quad(x2, z1, 1, z2 - z1, SCAN_LINE);
    }

    private void drawPlayer(GuiRenderer renderer) {
        if (mc.player == null) return;
        double px = sx(mc.player.getX()), pz = sz(mc.player.getZ());
        if (px < x || px > x + width || pz < y || pz > y + height) return;
        double s = 5;
        float yaw = (float) Math.toRadians(mc.player.getYRot());
        double dx = -Math.sin(yaw), dz = Math.cos(yaw);
        double nx = -dz, nz = dx;
        renderer.triangle(
            px + dx * s, pz + dz * s,
            px - dx * s + nx * s * 0.8, pz - dz * s + nz * s * 0.8,
            px - dx * s - nx * s * 0.8, pz - dz * s - nz * s * 0.8,
            PLAYER
        );
    }

    private void drawMarker(GuiRenderer renderer, double wx, double wz, Color col, double r) {
        double px = sx(wx), pz = sz(wz);
        if (px < x || px > x + width || pz < y || pz > y + height) return;
        renderer.quad(px - r, pz - r, r * 2, r * 2, col);
    }

    private void drawRing(GuiRenderer renderer, double px, double pz, double r, Color col) {
        renderer.quad(px - r, pz - r, r * 2, 1, col);
        renderer.quad(px - r, pz + r, r * 2, 1, col);
        renderer.quad(px - r, pz - r, 1, r * 2, col);
        renderer.quad(px + r, pz - r, 1, r * 2 + 1, col);
    }

    private static Color colorFor(int total) {
        if (total < 15) return new Color(90, 220, 120, 230);
        if (total < 50) return new Color(230, 220, 90, 235);
        if (total < 100) return new Color(240, 160, 60, 240);
        return new Color(230, 70, 70, 245);
    }

    private static String tip(Chunk c) {
        StringBuilder sb = new StringBuilder();
        sb.append(c.x).append(", ").append(c.z).append("  (total ").append(c.getTotal()).append(")");
        append(sb, "chests", c.chests);
        append(sb, "barrels", c.barrels);
        append(sb, "shulkers", c.shulkers);
        append(sb, "ender", c.enderChests);
        append(sb, "hoppers", c.hoppers);
        append(sb, "disp/drop", c.dispensersDroppers);
        append(sb, "furnaces", c.furnaces);
        append(sb, "crafters", c.crafters);
        return sb.toString();
    }

    private static void append(StringBuilder sb, String name, int v) {
        if (v > 0) sb.append('\n').append(name).append(": ").append(v);
    }

    @Override
    public boolean onMouseClicked(MouseButtonEvent event, boolean used) {
        if (!mouseOver || used) return false;
        Chunk hit = chunkAt(mouseX, mouseY);
        if (hit != null) {
            selected = hit;
            if (onSelect != null) onSelect.accept(hit);
            return true;
        }
        dragging = true;
        return true;
    }

    @Override
    public boolean onMouseReleased(MouseButtonEvent event) {
        dragging = false;
        return false;
    }

    @Override
    public void onMouseMoved(double x, double y, double dx, double dy) {
        mouseX = x;
        mouseY = y;
        if (dragging) {
            originX -= dx / scale;
            originZ -= dy / scale;
        }
    }

    @Override
    public boolean onMouseScrolled(double amount) {
        if (!mouseOver) return false;
        double factor = amount > 0 ? 1.18 : 1 / 1.18;
        scale = Math.max(0.004, Math.min(3.0, scale * factor));
        return true;
    }

    private Chunk chunkAt(double mx, double my) {
        Chunk best = null;
        double bestD = Double.MAX_VALUE;
        for (Chunk c : chunks) {
            double px = sx(c.x), pz = sz(c.z);
            double r = 3 + Math.min(6, c.getTotal() / 12.0);
            double d = (mx - px) * (mx - px) + (my - pz) * (my - pz);
            if (d <= r * r && d < bestD) { best = c; bestD = d; }
        }
        return best;
    }
}
