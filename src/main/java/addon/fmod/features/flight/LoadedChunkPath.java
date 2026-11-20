package addon.fmod.features.flight;

/** Tests a swept horizontal player box without loading chunks or sampling over gaps. */
public final class LoadedChunkPath {
    private static final int MAX_CANDIDATE_CHUNKS = 256;

    @FunctionalInterface
    public interface Chunks { boolean isLoaded(int x, int z); }

    private LoadedChunkPath() {}

    /**
     * Largest fraction of the move (0..1) that keeps the swept box inside loaded chunks.
     * Returns 1 when the whole move is clear, letting flight glide up to the loaded frontier
     * instead of stopping a chunk early or stuttering.
     */
    public static double maxFraction(double minX, double minZ, double maxX, double maxZ, double dx, double dz, Chunks chunks) {
        if (canMove(minX, minZ, maxX, maxZ, dx, dz, chunks)) return 1.0;
        double lo = 0.0, hi = 1.0;
        for (int i = 0; i < 12; i++) {
            double mid = (lo + hi) * 0.5;
            if (canMove(minX, minZ, maxX, maxZ, dx * mid, dz * mid, chunks)) lo = mid;
            else hi = mid;
        }
        return lo;
    }

    public static boolean canMove(double minX, double minZ, double maxX, double maxZ, double dx, double dz, Chunks chunks) {
        if (!Double.isFinite(minX) || !Double.isFinite(minZ) || !Double.isFinite(maxX) || !Double.isFinite(maxZ)
            || !Double.isFinite(dx) || !Double.isFinite(dz) || minX >= maxX || minZ >= maxZ) return false;
        if (dx == 0 && dz == 0) return true;

        double firstX = Math.floor(Math.min(minX, minX + dx) / 16);
        double firstZ = Math.floor(Math.min(minZ, minZ + dz) / 16);
        double lastX = Math.floor(Math.nextDown(Math.max(maxX, maxX + dx) / 16));
        double lastZ = Math.floor(Math.nextDown(Math.max(maxZ, maxZ + dz) / 16));
        if (firstX < Integer.MIN_VALUE || firstZ < Integer.MIN_VALUE || lastX > Integer.MAX_VALUE || lastZ > Integer.MAX_VALUE) return false;
        double countX = lastX - firstX + 1, countZ = lastZ - firstZ + 1;
        // Reject pathological speeds before entering the per-move loop.
        if (!(countX > 0 && countZ > 0 && countX * countZ <= MAX_CANDIDATE_CHUNKS)) return false;

        for (long x = (long) firstX; x <= (long) lastX; x++) {
            for (long z = (long) firstZ; z <= (long) lastZ; z++) {
                double tileX = x * 16.0, tileZ = z * 16.0;
                // A chunk can unload around the player. Allow backing out of that existing overlap.
                if (minX < tileX + 16 && maxX > tileX && minZ < tileZ + 16 && maxZ > tileZ) continue;
                if (intersects(minX, minZ, maxX, maxZ, dx, dz, tileX, tileZ) && !chunks.isLoaded((int) x, (int) z)) return false;
            }
        }
        return true;
    }

    private static boolean intersects(double minX, double minZ, double maxX, double maxZ, double dx, double dz, double tileX, double tileZ) {
        if (dx == 0 && (maxX <= tileX || minX >= tileX + 16)) return false;
        if (dz == 0 && (maxZ <= tileZ || minZ >= tileZ + 16)) return false;
        double enterX = dx == 0 ? Double.NEGATIVE_INFINITY : (dx > 0 ? tileX - maxX : tileX + 16 - minX) / dx;
        double exitX = dx == 0 ? Double.POSITIVE_INFINITY : (dx > 0 ? tileX + 16 - minX : tileX - maxX) / dx;
        double enterZ = dz == 0 ? Double.NEGATIVE_INFINITY : (dz > 0 ? tileZ - maxZ : tileZ + 16 - minZ) / dz;
        double exitZ = dz == 0 ? Double.POSITIVE_INFINITY : (dz > 0 ? tileZ + 16 - minZ : tileZ - maxZ) / dz;
        return Math.max(0, Math.max(enterX, enterZ)) < Math.min(1, Math.min(exitX, exitZ));
    }
}
