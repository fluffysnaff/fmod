package addon.fmod.uiutils;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static addon.fmod.uiutils.DelayedUiPackets.Decision.*;

class DelayedUiPacketsTest {
    @Test void passesNormallyAndReplaysDelayedPacketsInOrderWithBoundedAdmission() {
        Object connection = new Object();
        var queue = new DelayedUiPackets<Integer>(2);
        queue.reset(connection);
        assertEquals(Send, queue.accept(connection, 0, true));
        queue.setDelaying(true);
        assertEquals(Queued, queue.accept(connection, 1, true));
        assertEquals(Queued, queue.accept(connection, 2, true));
        assertEquals(Full, queue.accept(connection, 3, true));
        assertEquals(Send, queue.accept(connection, 4, false));
        var sent = new ArrayList<Integer>();
        assertEquals(2, queue.flush(connection, sent::add));
        assertEquals(List.of(1, 2), sent);
        assertEquals(0, queue.size());
    }

    @Test void sendingOffDiscardsTheQueueAndBlocksEvenImmediateFabricatedPackets() {
        Object connection = new Object();
        var queue = new DelayedUiPackets<Integer>(3);
        queue.reset(connection);
        queue.setDelaying(true);
        queue.accept(connection, 1, true);
        queue.setSending(false);
        assertEquals(0, queue.size());
        assertEquals(Block, queue.accept(connection, 2, false));
        assertEquals(0, queue.flush(connection, value -> fail("Output after stop")));
        queue.setSending(true);
        assertEquals(0, queue.flush(connection, value -> fail("Old output resurrected")));
    }

    @Test void neitherReconnectNorStaleCallbacksCanReplayAnotherConnectionsPackets() {
        Object old = new Object(), current = new Object();
        var queue = new DelayedUiPackets<Integer>(3);
        queue.reset(old);
        queue.setDelaying(true);
        queue.accept(old, 1, true);
        queue.reset(current);
        assertEquals(Stale, queue.accept(old, 2, true));
        assertEquals(0, queue.flush(old, value -> fail("Stale session")));
        assertTrue(queue.sending());
        assertFalse(queue.delaying());
        queue.reset(null);
        assertEquals(Stale, queue.accept(current, 3, true));
    }

    @Test void resetDuringReplayStopsRemainingOutputAndDoesNotResurrectIt() {
        Object connection = new Object();
        var queue = new DelayedUiPackets<Integer>(3);
        queue.reset(connection);
        queue.setDelaying(true);
        queue.accept(connection, 1, true);
        queue.accept(connection, 2, true);
        var sent = new ArrayList<Integer>();
        assertEquals(1, queue.flush(connection, value -> { sent.add(value); queue.reset(null); }));
        assertEquals(List.of(1), sent);
        assertEquals(0, queue.size());
    }

    @Test void failedReplayDiscardsRemainingPacketsRatherThanRetryingUnknownEffects() {
        Object connection = new Object();
        var queue = new DelayedUiPackets<Integer>(3);
        queue.reset(connection);
        queue.setDelaying(true);
        queue.accept(connection, 1, true);
        queue.accept(connection, 2, true);
        assertThrows(IllegalStateException.class, () -> queue.flush(connection, value -> { throw new IllegalStateException("Send failed"); }));
        assertEquals(0, queue.size());
    }
}
