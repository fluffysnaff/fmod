package addon.fmod.utils;

import java.security.SecureRandom;
import java.util.List;

/** Synthetic display values, generated without reading the player's location. */
public record DisplayCoordinates(int x, int y, int z) {
    private static final SecureRandom RANDOM = new SecureRandom();

    public static DisplayCoordinates generate() {
        // Multiples of eight keep opposite-dimension displays exact and within world bounds.
        return new DisplayCoordinates(RANDOM.nextInt(-375_000, 375_001) * 8,
            RANDOM.nextInt(-64, 320), RANDOM.nextInt(-375_000, 375_001) * 8);
    }

    public DisplayCoordinates opposite(boolean inNether) {
        return inNether ? new DisplayCoordinates(x * 8, y, z * 8)
            : new DisplayCoordinates(x / 8, y, z / 8);
    }

    public DisplayCoordinates chunk() {
        return new DisplayCoordinates(Math.floorDiv(x, 16), Math.floorDiv(y, 16), Math.floorDiv(z, 16));
    }

    public int axis(char axis) {
        return switch (Character.toLowerCase(axis)) {
            case 'x' -> x;
            case 'y' -> y;
            case 'z' -> z;
            default -> throw new IllegalArgumentException("Unknown coordinate axis");
        };
    }

    public String xyz() { return x + ", " + y + ", " + z; }
    public String spaced() { return x + " " + y + " " + z; }

    public List<String> debugLines() {
        return List.of("XYZ: " + x + " / " + y + " / " + z,
            "Block: " + spaced(), "Chunk: " + chunk().spaced());
    }

    public boolean isSyntheticTriple(double a, double b, double c) {
        return matches(a, b, c) || opposite(false).matches(a, b, c)
            || opposite(true).matches(a, b, c) || chunk().matches(a, b, c);
    }

    public boolean isSyntheticPair(double a, double b) {
        return (a == x && b == z) || (a == x / 8 && b == z / 8)
            || (a == x * 8 && b == z * 8);
    }

    public boolean isSyntheticAxis(char axis, double value) {
        return value == axis(axis) || value == opposite(false).axis(axis)
            || value == opposite(true).axis(axis);
    }

    private boolean matches(double a, double b, double c) { return a == x && b == y && c == z; }
}
