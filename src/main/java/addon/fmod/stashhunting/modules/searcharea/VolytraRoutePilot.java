package addon.fmod.stashhunting.modules.searcharea;

import addon.fmod.modules.VolytraFly;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * Flies a precomputed list of {x, z} waypoints by handing each to {@link VolytraFly}'s
 * Autonav, advancing to the next once the player is within VolytraFly's arrival radius.
 * VolytraFly does the actual flying (with its terrain avoidance); this only sequences the route.
 *
 * <p>Recovers on its own: if Autonav stops (disconnect reset, VolytraFly toggled, or a stall
 * where VolytraFly pauses and holds), it re-issues the current leg, so the route survives
 * reconnects and chunk-loading stalls instead of getting stuck.
 */
public final class VolytraRoutePilot {
    private static final int STALL_TICKS = 100; // ~5s of no progress before re-issuing the leg

    private final List<int[]> corners;
    private final VolytraFly volytra;
    private int index = -1;

    private double lastDist = Double.MAX_VALUE;
    private int stallTicks;

    public VolytraRoutePilot(List<int[]> corners) {
        this.corners = corners;
        this.volytra = Modules.get().get(VolytraFly.class);
    }

    public boolean available() {
        return volytra != null;
    }

    /** True if VolytraFly is enabled so Autonav can run. */
    public boolean ready() {
        return volytra != null && volytra.isActive();
    }

    public int index() {
        return index;
    }

    public int size() {
        return corners.size();
    }

    public boolean start() {
        return start(0);
    }

    /** Begins (or resumes) the route at {@code fromIndex}; returns false if it could not issue a route. */
    public boolean start(int fromIndex) {
        if (corners.isEmpty()) { index = -1; return false; }
        index = Math.max(0, Math.min(fromIndex, corners.size() - 1));
        lastDist = Double.MAX_VALUE;
        stallTicks = 0;
        return volytra != null && volytra.navigateTo(corners.get(index)[0], corners.get(index)[1]);
    }

    /** Advances the route; returns true when the whole route is complete. */
    public boolean tick(Minecraft mc) {
        if (volytra == null || mc.player == null) return false;
        if (index < 0 || index >= corners.size()) return true;

        int[] c = corners.get(index);
        double arrive = Math.max(6.0, volytra.autonavArrival());
        double dist = Math.hypot(c[0] + 0.5 - mc.player.getX(), c[1] + 0.5 - mc.player.getZ());

        if (dist <= arrive) {
            index++;
            lastDist = Double.MAX_VALUE;
            stallTicks = 0;
            if (index >= corners.size()) return true;
            volytra.navigateTo(corners.get(index)[0], corners.get(index)[1]);
            return false;
        }

        if (dist < lastDist - 0.5) {
            lastDist = dist;
            stallTicks = 0;
        } else {
            stallTicks++;
        }

        // Re-issue the current leg if Autonav stopped (reconnect/toggle) or has stalled (VolytraFly
        // paused on unloaded chunks or a wall). Harmless to repeat; acts like pressing Go again.
        if (!volytra.isAutonavActive() || stallTicks > STALL_TICKS) {
            volytra.navigateTo(c[0], c[1]);
            stallTicks = 0;
        }
        return false;
    }

    public void stop() {
        if (volytra != null) volytra.stopAutonav();
        index = -1;
    }
}
