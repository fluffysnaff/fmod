package addon.fmod.utils;

import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;
import java.util.function.Predicate;

/** Geometry-only filters. Passing them says nothing about terrain history or actual traffic. */
public final class BaseSitePlanner {
    public static final int WORLD_LIMIT = 29_999_984;
    private static final int ATTEMPTS = 8192;

    private BaseSitePlanner() {}

    public record Point(int x, int z) {
        public Point overworld(boolean nether) {
            return nether ? new Point(Math.multiplyExact(x, 8), Math.multiplyExact(z, 8)) : this;
        }
    }

    public record Corridor(Point start, Point end) {
        public double distance(Point point) {
            double dx = (double) end.x - start.x;
            double dz = (double) end.z - start.z;
            double lengthSquared = dx * dx + dz * dz;
            double t = lengthSquared == 0 ? 0 : Math.clamp(
                (((double) point.x - start.x) * dx + ((double) point.z - start.z) * dz) / lengthSquared, 0, 1);
            return Math.hypot(point.x - (start.x + t * dx), point.z - (start.z + t * dz));
        }
    }

    public record Bounds(double minX, double maxX, double minZ, double maxZ) {
        public Bounds {
            if (!Double.isFinite(minX) || !Double.isFinite(maxX) || !Double.isFinite(minZ)
                || !Double.isFinite(maxZ) || minX > maxX || minZ > maxZ) {
                throw new IllegalArgumentException("Invalid world border.");
            }
        }
    }

    public record Policy(int minSpawnDistance, int maxCoordinate, int borderMargin, int routeClearance,
                         int roundClearance, List<Integer> squareRings) {
        public Policy {
            if (minSpawnDistance < 0 || maxCoordinate < 1 || maxCoordinate > WORLD_LIMIT
                || borderMargin < 0 || routeClearance < 0 || roundClearance < 0 || roundClearance >= 50_000
                || squareRings.size() > 64 || squareRings.stream().anyMatch(r -> r <= 0 || r > WORLD_LIMIT)) {
                throw new IllegalArgumentException("Invalid candidate filters.");
            }
            squareRings = List.copyOf(squareRings);
        }
    }

    public static Optional<Point> find(RandomGenerator random, Policy policy, Bounds border,
                                       List<Point> avoid, List<Corridor> routes) {
        return find(random, policy, border, avoid, routes, point -> true);
    }

    public static Optional<Point> find(RandomGenerator random, Policy policy, Bounds border,
                                       List<Point> avoid, List<Corridor> routes, Predicate<Point> additionalFilter) {
        double lowX = Math.max(-policy.maxCoordinate, border.minX + policy.borderMargin);
        double highX = Math.min(policy.maxCoordinate, border.maxX - policy.borderMargin - 1);
        double lowZ = Math.max(-policy.maxCoordinate, border.minZ + policy.borderMargin);
        double highZ = Math.min(policy.maxCoordinate, border.maxZ - policy.borderMargin - 1);
        if (lowX > highX || lowZ > highZ) return Optional.empty();
        int minX = (int) Math.ceil(lowX);
        int maxX = (int) Math.floor(highX);
        int minZ = (int) Math.ceil(lowZ);
        int maxZ = (int) Math.floor(highZ);
        if (minX > maxX || minZ > maxZ) return Optional.empty();

        for (int i = 0; i < ATTEMPTS; i++) {
            Point point = new Point(random.nextInt(minX, maxX + 1), random.nextInt(minZ, maxZ + 1));
            if (accepts(point, policy, border, avoid, routes) && additionalFilter.test(point)) return Optional.of(point);
        }
        return Optional.empty();
    }

    public static boolean accepts(Point point, Policy policy, Bounds border,
                                  List<Point> avoid, List<Corridor> routes) {
        double x = point.x;
        double z = point.z;
        double ax = Math.abs(x);
        double az = Math.abs(z);
        if (ax > policy.maxCoordinate || az > policy.maxCoordinate
            || Math.hypot(x, z) < policy.minSpawnDistance
            || x < border.minX + policy.borderMargin || x + 1 > border.maxX - policy.borderMargin
            || z < border.minZ + policy.borderMargin || z + 1 > border.maxZ - policy.borderMargin) return false;

        if (Math.min(ax, az) <= policy.routeClearance
            || Math.min(Math.abs(x - z), Math.abs(x + z)) / Math.sqrt(2) <= policy.routeClearance) return false;
        if (roundDistance(point.x) <= policy.roundClearance || roundDistance(point.z) <= policy.roundClearance) return false;

        for (int radius : policy.squareRings) {
            double distance = ax <= radius && az <= radius ? radius - Math.max(ax, az)
                : Math.hypot(Math.max(0, ax - radius), Math.max(0, az - radius));
            if (distance <= policy.routeClearance) return false;
        }
        for (Point location : avoid) {
            if (Math.hypot(x - location.x, z - location.z) <= policy.routeClearance) return false;
        }
        for (Corridor route : routes) {
            if (route.distance(point) <= policy.routeClearance) return false;
        }
        return true;
    }

    private static int roundDistance(int coordinate) {
        int remainder = Math.floorMod(coordinate, 100_000);
        return Math.min(remainder, 100_000 - remainder);
    }
}
