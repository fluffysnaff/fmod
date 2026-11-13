package addon.fmod.modules.basescout;

import java.util.Objects;

/** Coalesces changing observations; a suppressed message is never queued for later delivery. */
public final class BaseScoutChatUpdates {
    private String lastState;
    private long lastSentNanos;

    public boolean shouldPublish(String state, long nowNanos, long intervalNanos) {
        Objects.requireNonNull(state);
        if (intervalNanos < 0) throw new IllegalArgumentException("Negative notification interval.");
        if (state.equals(lastState) || lastState != null && nowNanos - lastSentNanos < intervalNanos) return false;
        lastState = state;
        lastSentNanos = nowNanos;
        return true;
    }

    public void reset() { lastState = null; lastSentNanos = 0; }
}
