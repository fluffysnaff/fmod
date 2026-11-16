package addon.fmod.uiutils;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.function.Consumer;

/** Session-owned FIFO. Disabling sending or ending a session discards pending output. */
public final class DelayedUiPackets<T> {
    public enum Decision { Send, Block, Queued, Full, Stale }

    private final int capacity;
    private final ArrayDeque<T> pending = new ArrayDeque<>();
    private Object session;
    private boolean sending = true;
    private boolean delaying;

    public DelayedUiPackets(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("Capacity must be positive.");
        this.capacity = capacity;
    }

    public synchronized void reset(Object session) {
        this.session = session;
        pending.clear();
        sending = true;
        delaying = false;
    }

    public synchronized Decision accept(Object session, T packet, boolean respectDelay) {
        Objects.requireNonNull(packet);
        if (session == null || session != this.session) return Decision.Stale;
        if (!sending) return Decision.Block;
        if (!delaying || !respectDelay) return Decision.Send;
        if (pending.size() == capacity) return Decision.Full;
        pending.addLast(packet);
        return Decision.Queued;
    }

    public synchronized void setSending(boolean sending) {
        this.sending = sending;
        if (!sending) pending.clear();
    }

    public synchronized void setDelaying(boolean delaying) { this.delaying = delaying; }
    public synchronized boolean sending() { return sending; }
    public synchronized boolean delaying() { return delaying; }
    public synchronized int size() { return pending.size(); }
    public synchronized void discard() { pending.clear(); }

    public synchronized int flush(Object session, Consumer<T> sender) {
        if (session == null || session != this.session || !sending) return 0;
        int sent = 0;
        try {
            while (session == this.session && sending && !pending.isEmpty()) {
                sender.accept(pending.removeFirst());
                sent++;
            }
        } catch (RuntimeException error) {
            pending.clear();
            throw error;
        }
        return sent;
    }
}
