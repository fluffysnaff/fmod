package addon.fmod.volytra;

/** VolytraFly's tapering speed envelope. Speeds are blocks per client tick. */
public final class FlightEnvelope {
    private FlightEnvelope() {}

    public static double next(double speed, double step, double plateau, double cap) {
        if (!Double.isFinite(speed) || !Double.isFinite(step) || !Double.isFinite(plateau)
            || !Double.isFinite(cap) || speed < 0 || step < 0 || plateau <= 0 || cap < 0) {
            throw new IllegalArgumentException("Invalid flight envelope.");
        }
        return Math.min(speed + step * Math.max(0, plateau - speed) / plateau, cap);
    }
}
