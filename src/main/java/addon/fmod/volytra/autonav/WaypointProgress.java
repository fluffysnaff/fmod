package addon.fmod.volytra.autonav;

public final class WaypointProgress {
    public record Point(double x, double y, double z) {}
    private WaypointProgress() {}

    /** Passing a waypoint's plane only counts when the player is also near that route segment. */
    public static boolean passed(Point position, Point from, Point to, double corridor) {
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        double lengthSquared = dx * dx + dy * dy + dz * dz;
        if (lengthSquared < 1e-12) return true;
        double ex = position.x - to.x, ey = position.y - to.y, ez = position.z - to.z;
        if (ex * ex + ey * ey + ez * ez <= 0.0025) return true;
        double px = position.x - from.x, py = position.y - from.y, pz = position.z - from.z;
        double projection = (px * dx + py * dy + pz * dz) / lengthSquared;
        if (projection < 1) return false;
        double ox = px - projection * dx, oy = py - projection * dy, oz = pz - projection * dz;
        return ox * ox + oy * oy + oz * oz <= corridor * corridor;
    }
}
