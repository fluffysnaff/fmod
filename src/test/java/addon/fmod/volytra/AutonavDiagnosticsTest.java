package addon.fmod.volytra;

import addon.fmod.volytra.autonav.AutonavDiagnostics;
import addon.fmod.volytra.autonav.AutonavDiagnostics.Coordinates;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class AutonavDiagnosticsTest {
    @TempDir Path directory;
    private AutonavDiagnostics logger(long bytes, int files) {
        return new AutonavDiagnostics(directory, bytes, files, 128, () -> Map.of("build_identity", "test-build"));
    }

    @Test void relativeLogsKeepTheOriginOutOfTheFileAndPreserveEventOrder() throws Exception {
        try (var log = logger(8192, 4)) {
            var session = log.begin(new Coordinates(12345678.25, -42.5, -9876543.75), false, Map.of("dimension", "nether"));
            log.record(session, "manual_snapshot", Map.of("position", session.position(12345679.5, -42.5, -9876541.75)), true);
            Path file = log.flush(session).get(5, TimeUnit.SECONDS);
            var lines = Files.readAllLines(file);
            assertEquals("header", JsonParser.parseString(lines.getFirst()).getAsJsonObject().get("event").getAsString());
            assertEquals("route_start", JsonParser.parseString(lines.get(1)).getAsJsonObject().get("event").getAsString());
            var point = JsonParser.parseString(lines.getLast()).getAsJsonObject().getAsJsonObject("data").getAsJsonObject("position");
            assertEquals(1.25, point.get("x").getAsDouble());
            assertEquals(2, point.get("z").getAsDouble());
            String text = Files.readString(file);
            assertFalse(text.contains("12345678")); assertFalse(text.contains("9876543")); assertFalse(text.contains("-42.5"));
            assertTrue(text.contains("relative_to_route_start")); assertTrue(text.contains("test-build"));
            var headerOrigin=JsonParser.parseString(lines.getFirst()).getAsJsonObject().getAsJsonObject("data").getAsJsonObject("origin");
            assertEquals(0,headerOrigin.get("x").getAsDouble());assertEquals(0,headerOrigin.get("y").getAsDouble());assertEquals(0,headerOrigin.get("z").getAsDouble());
        }
    }

    @Test void absolutePositionsRequireTheExplicitSessionOption() throws Exception {
        try (var log = logger(8192, 4)) {
            var session = log.begin(new Coordinates(12345678.25, 80, -9876543.75), true, Map.of());
            log.record(session, "sample", Map.of("position", session.position(12345679.5, 81, -9876541.75)), false);
            var lines=Files.readAllLines(log.flush(session).get(5, TimeUnit.SECONDS));
            var event=JsonParser.parseString(lines.getLast()).getAsJsonObject();
            assertEquals(12345679.5,event.getAsJsonObject("data").getAsJsonObject("position").get("x").getAsDouble());
            assertEquals("absolute",JsonParser.parseString(lines.getFirst()).getAsJsonObject().getAsJsonObject("data").get("coordinate_mode").getAsString());
        }
    }

    @Test void rollingFilesStayBoundedAndEachChunkKeepsItsHeader() throws Exception {
        try (var log = logger(4096, 3)) {
            var session = log.begin(new Coordinates(0, 0, 0), false, Map.of());
            for (int i = 0; i < 12; i++) log.record(session, "sample", Map.of("index", i, "payload", "x".repeat(2000)), false);
            Path current = log.flush(session).get(5, TimeUnit.SECONDS);
            try (var entries = Files.list(directory)) {
                var files = entries.toList(); assertTrue(files.size() <= 3);
                for (Path file : files) {
                    assertTrue(Files.size(file) <= 4096);
                    assertEquals("header", JsonParser.parseString(Files.readAllLines(file).getFirst()).getAsJsonObject().get("event").getAsString());
                }
            }
            assertTrue(Files.readString(current).contains("\"index\":11"));
        }
    }

    @Test void retentionDoesNotTouchUnrelatedFiles() throws Exception {
        Path note = directory.resolve("notes.txt"); Files.writeString(note, "keep");
        try (var log = logger(4096, 2)) {
            for (int i = 0; i < 6; i++) {
                var session = log.begin(new Coordinates(0, 0, 0), false, Map.of());
                log.flush(session).get(5, TimeUnit.SECONDS);
            }
            try (var entries = Files.list(directory)) { assertEquals(3, entries.count()); }
            assertEquals("keep", Files.readString(note));
        }
    }

    @Test void diskFailureIsReportedWithoutThrowingOnTheProducer() throws Exception {
        Path notDirectory = directory.resolve("not-a-folder"); Files.writeString(notDirectory, "keep");
        try (var log = new AutonavDiagnostics(notDirectory, Map::of)) {
            var session = assertDoesNotThrow(() -> log.begin(new Coordinates(0, 0, 0), false, Map.of()));
            assertThrows(ExecutionException.class, () -> log.flush(session).get(5, TimeUnit.SECONDS));
            assertNotNull(log.takeFailure()); assertEquals("keep", Files.readString(notDirectory));
        }
    }

    @Test void anOversizedEventDoesNotCorruptTheExistingLog() throws Exception {
        try (var log = logger(4096, 4)) {
            var session = log.begin(new Coordinates(0, 0, 0), false, Map.of());
            log.record(session, "oversized", Map.of("data", "x".repeat(10000)), false);
            Path file = log.flush(session).get(5, TimeUnit.SECONDS);
            assertEquals(1, log.droppedRecords()); assertNotNull(log.takeFailure());
            for (String line : Files.readAllLines(file)) assertDoesNotThrow(() -> JsonParser.parseString(line));
        }
    }

    @Test void aPreviousSessionRemainsDiscoverableAfterRestart() throws Exception {
        Path file;
        try(var log=logger(4096,4)){
            var session=log.begin(new Coordinates(0,0,0),false,Map.of());
            file=log.flush(session).get(5,TimeUnit.SECONDS);
        }
        try(var restarted=logger(4096,4)){assertEquals(file,restarted.latest().get(5,TimeUnit.SECONDS));}
    }

    @Test void snapshotRemainsStableWhileRollingLogsContinue() throws Exception {
        try (var log = logger(4096, 2)) {
            var session = log.begin(new Coordinates(0, 0, 0), false, Map.of());
            log.record(session, "manual_snapshot", Map.of("terrain", "test"), true);
            Path snapshot = log.snapshot(session).get(5, TimeUnit.SECONDS);
            String captured = Files.readString(snapshot);
            assertTrue(captured.contains("manual_snapshot"));
            for (int i = 0; i < 12; i++) log.record(session, "sample", Map.of("index", i, "payload", "x".repeat(2000)), false);
            log.flush(session).get(5, TimeUnit.SECONDS);
            assertEquals(captured, Files.readString(snapshot));
            try (var entries = Files.list(directory)) { assertEquals(3, entries.count()); }
            assertEquals(snapshot, log.snapshot(session).get(5, TimeUnit.SECONDS));
            assertNotEquals(captured, Files.readString(snapshot));
            assertTrue(Files.size(snapshot) <= 4096);
        }
    }

    @Test void missingSessionDoesNotDestroyTheSavedSnapshotAndRestartFindsIt() throws Exception {
        Path snapshot;
        String captured;
        try (var log = logger(4096, 1)) {
            var incident = log.begin(new Coordinates(0, 0, 0), false, Map.of());
            snapshot = log.snapshot(incident).get(5, TimeUnit.SECONDS);
            captured = Files.readString(snapshot);
            var later = log.begin(new Coordinates(0, 0, 0), false, Map.of());
            log.flush(later).get(5, TimeUnit.SECONDS);
            assertThrows(ExecutionException.class, () -> log.snapshot(incident).get(5, TimeUnit.SECONDS));
            assertEquals(captured, Files.readString(snapshot));
            try (var entries = Files.list(directory)) { assertEquals(2, entries.count()); }
        }
        try (var restarted = logger(4096, 1)) {
            assertEquals(snapshot, restarted.snapshot(null).get(5, TimeUnit.SECONDS));
            assertEquals(captured, Files.readString(snapshot));
        }
    }

    @Test void saturatedQueueDropsSamplesAndAdmitsAnIncidentWithoutBlockingTheProducer() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var log = new AutonavDiagnostics(directory, 8192, 4, 2, () -> {
            entered.countDown();
            try { if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Test worker timed out."); }
            catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IllegalStateException(error); }
            return Map.of();
        })) {
            try {
                var session = log.begin(new Coordinates(0, 0, 0), false, Map.of());
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                assertTrue(log.record(session, "sample", Map.of("index", 1), false));
                assertTrue(log.record(session, "sample", Map.of("index", 2), false));
                assertFalse(log.record(session, "sample", Map.of("index", 3), false));
                assertTrue(log.record(session, "route_paused", Map.of(), true));
                release.countDown();
                log.close();
                assertEquals(2, log.droppedRecords());
                assertTrue(Files.readString(session.file()).contains("route_paused"));
            } finally { release.countDown(); }
        }
    }
}
