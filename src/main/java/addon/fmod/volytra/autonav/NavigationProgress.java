package addon.fmod.volytra.autonav;

import addon.fmod.volytra.autonav.WaypointProgress.Point;

/** Active real time without leaving a small position neighbourhood; route replans do not count as movement. */
public final class NavigationProgress {
    private Point anchor;
    private long sampledAt, activeNanos, stalledNanos;

    public void reset() { anchor = null; sampledAt = activeNanos = stalledNanos = 0; }
    public void pause(long now) { sampledAt = now; }
    public long activeNanos() { return activeNanos; }

    public double sample(long now, Point position) {
        if (anchor == null) { anchor = position; sampledAt = now; return 0; }
        long elapsed = Math.max(0, now - sampledAt);
        sampledAt = now;
        activeNanos += elapsed;
        double dx = position.x() - anchor.x(), dy = position.y() - anchor.y(), dz = position.z() - anchor.z();
        if (dx * dx + dy * dy + dz * dz >= 2.25) { anchor = position; stalledNanos = 0; }
        else stalledNanos += elapsed;
        return stalledNanos / 1_000_000_000.0;
    }
}
