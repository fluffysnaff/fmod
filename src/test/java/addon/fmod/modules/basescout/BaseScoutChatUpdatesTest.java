package addon.fmod.modules.basescout;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BaseScoutChatUpdatesTest {
    @Test void publishesTheFirstReportAndKeepsAnUnchangedAreaQuiet() {
        var updates = new BaseScoutChatUpdates();
        assertTrue(updates.shouldPublish("clues", 100, 10));
        assertFalse(updates.shouldPublish("clues", 1000, 10));
    }
    @Test void coalescesChangesWithoutDeliveringAnObsoleteIntermediateReport() {
        var updates = new BaseScoutChatUpdates();
        assertTrue(updates.shouldPublish("clear", 100, 10));
        assertFalse(updates.shouldPublish("clues", 105, 10));
        assertFalse(updates.shouldPublish("clear", 111, 10));
        assertTrue(updates.shouldPublish("near known", 112, 10));
        assertFalse(updates.shouldPublish("near known", 200, 10));
    }
    @Test void resetsBetweenSessionsAndUsesElapsedTimeAcrossNanoClockWrap() {
        var updates = new BaseScoutChatUpdates();
        assertTrue(updates.shouldPublish("first", Long.MAX_VALUE - 5, 10));
        assertFalse(updates.shouldPublish("second", Long.MIN_VALUE, 10));
        assertTrue(updates.shouldPublish("second", Long.MIN_VALUE + 6, 10));
        updates.reset();
        assertTrue(updates.shouldPublish("second", -100, 10));
        assertThrows(IllegalArgumentException.class, () -> updates.shouldPublish("other", 0, -1));
    }
}
