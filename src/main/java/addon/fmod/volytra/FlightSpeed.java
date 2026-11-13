package addon.fmod.volytra;

import java.util.ArrayDeque;

/** Net horizontal displacement over a real-time window, including any intervening backtracking. */
public final class FlightSpeed {
    private record Sample(long time, double x, double z) {}
    private final ArrayDeque<Sample> samples = new ArrayDeque<>();

    public void reset() { samples.clear(); }

    public double sample(long now, double x, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(z)) throw new IllegalArgumentException("Invalid position.");
        if (!samples.isEmpty() && now <= samples.getLast().time) return speed();
        samples.addLast(new Sample(now, x, z));
        while (samples.size() > 2 && now - samples.getFirst().time > 1_000_000_000L) samples.removeFirst();
        while (samples.size() > 256) samples.removeFirst();
        return speed();
    }

    private double speed() {
        if (samples.size() < 2) return 0;
        Sample first = samples.getFirst(), last = samples.getLast();
        double seconds = (last.time - first.time) / 1_000_000_000.0;
        return seconds > 0 ? Math.hypot(last.x - first.x, last.z - first.z) / seconds : 0;
    }
}
