package addon.fmod.volytra.autonav;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/** Optional local diagnostics. Captured values are immutable; serialization and all disk work stay off the client thread. */
public final class AutonavDiagnostics implements AutoCloseable {
    public record Coordinates(double x, double y, double z) {}
    public static final class Session {
        private final AutonavDiagnostics owner;
        private final String id;
        private final Path file;
        private final Coordinates origin;
        private final boolean absolute;
        private final long started = System.nanoTime();
        private final Map<String, Object> header;

        private Session(AutonavDiagnostics owner, String id, Coordinates origin, boolean absolute, Map<String, Object> metadata) {
            this.owner = owner;
            this.id = id;
            this.file = owner.directory.resolve(id + "-0.jsonl");
            this.origin = origin;
            this.absolute = absolute;
            var header = new LinkedHashMap<String, Object>(metadata);
            header.put("format_version", 1);
            header.put("coordinate_mode", absolute ? "absolute" : "relative_to_route_start");
            header.put("origin", position(origin.x, origin.y, origin.z));
            header.put("origin_block_fraction", new Coordinates(fraction(origin.x), fraction(origin.y), fraction(origin.z)));
            header.put("position_unit", "blocks");
            header.put("velocity_unit", "blocks_per_client_tick");
            this.header = Map.copyOf(header);
        }
        public Coordinates position(double x, double y, double z) {
            return absolute ? new Coordinates(x, y, z) : new Coordinates(x - origin.x, y - origin.y, z - origin.z);
        }
        public Path file() { return file; }
        public boolean absolute() { return absolute; }
        private static double fraction(double value) { return value - Math.floor(value); }
    }

    private static final Gson GSON = new Gson();
    private static final DateTimeFormatter NAME_TIME = DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final String FILE_PATTERN = "autonav-[0-9]{8}T[0-9]{6}Z-[0-9a-f]{8}-[0-9]+\\.jsonl";
    private final Path directory;
    private final long maxBytes;
    private final int maxFiles;
    private final ThreadPoolExecutor worker;
    private final Supplier<Map<String, Object>> buildMetadata;
    private Map<String, Object> build;
    private final AtomicLong dropped = new AtomicLong();
    private final AtomicReference<String> failure = new AtomicReference<>();

    public AutonavDiagnostics(Path directory, Supplier<Map<String, Object>> buildMetadata) {
        this(directory, 2 * 1024 * 1024, 4, 128, buildMetadata);
    }
    public AutonavDiagnostics(Path directory, long maxBytes, int maxFiles, int queueCapacity, Supplier<Map<String, Object>> buildMetadata) {
        if (maxBytes < 512 || maxFiles < 1 || queueCapacity < 1) throw new IllegalArgumentException("Invalid diagnostic bounds.");
        this.directory = directory.toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
        this.maxFiles = maxFiles;
        this.buildMetadata = buildMetadata;
        this.worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(queueCapacity), task -> {
            Thread thread = new Thread(task, "FMod-Autonav-Diagnostics");
            thread.setDaemon(true);
            return thread;
        });
    }
    public Path directory() { return directory; }
    public long droppedRecords() { return dropped.get(); }
    public String takeFailure() { return failure.getAndSet(null); }

    public Session begin(Coordinates origin, boolean absolute, Map<String, Object> metadata) {
        String id = "autonav-" + NAME_TIME.format(Instant.now()) + "-" + UUID.randomUUID().toString().substring(0, 8);
        Session session = new Session(this, id, origin, absolute, metadata);
        record(session, "route_start", Map.of(), true);
        return session;
    }

    public boolean record(Session session, String type, Map<String, Object> immutableData, boolean critical) {
        if (session == null || session.owner != this) return false;
        try {
            Write task = new Write(session, type, Map.copyOf(immutableData), critical, Instant.now(), (System.nanoTime() - session.started) / 1_000_000);
            submit(task, critical);
            return true;
        } catch (RuntimeException error) {
            dropped.incrementAndGet();
            failure.compareAndSet(null, "Autonav diagnostics could not accept a record; flight continues normally.");
            return false;
        }
    }

    private void submit(Runnable task, boolean critical) {
        try { worker.execute(task); }
        catch (RejectedExecutionException full) {
            if (critical && !worker.isShutdown()) {
                for (Runnable pending : worker.getQueue()) {
                    if (pending instanceof Write write && !write.critical && worker.getQueue().remove(pending)) {
                        dropped.incrementAndGet();
                        worker.execute(task);
                        return;
                    }
                }
            }
            throw full;
        }
    }

    public CompletableFuture<Path> flush(Session session) {
        var result = new CompletableFuture<Path>();
        if (session == null || session.owner != this) { result.completeExceptionally(new IOException("No diagnostic session is available.")); return result; }
        try { submit(() -> {
            if (Files.isRegularFile(session.file)) result.complete(session.file);
            else result.completeExceptionally(new IOException("No diagnostic file could be written."));
        }, true); }
        catch (RejectedExecutionException error) { result.completeExceptionally(new IOException("Diagnostic worker is busy.")); }
        return result;
    }

    public CompletableFuture<Path> latest() {
        var result = new CompletableFuture<Path>();
        try { submit(() -> {
            try { result.complete(latestRollingFile()); }
            catch (IOException error) { result.completeExceptionally(new IOException("Diagnostic directory could not be read.")); }
        }, true); }
        catch (RejectedExecutionException error) { result.completeExceptionally(new IOException("Diagnostic worker is busy.")); }
        return result;
    }

    /** A stable export, replaced only by another explicit snapshot request. */
    public CompletableFuture<Path> snapshot(Session session) {
        var result = new CompletableFuture<Path>();
        if (session != null && session.owner != this) {
            result.completeExceptionally(new IOException("Invalid diagnostic session.")); return result;
        }
        try { submit(() -> {
            Path temporary = null;
            try {
                Path destination = directory.resolve("autonav-snapshot.jsonl");
                if (session == null && Files.isRegularFile(destination)) { result.complete(destination); return; }
                Path source = session == null ? latestRollingFile() : session.file;
                if (source == null) { result.complete(null); return; }
                temporary = Files.createTempFile(directory, "snapshot-", ".tmp");
                Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
                try { Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                catch (AtomicMoveNotSupportedException error) { Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING); }
                result.complete(destination);
            } catch (IOException error) { result.completeExceptionally(new IOException("Diagnostic snapshot could not be saved.")); }
            finally {
                if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) {}
            }
        }, true); }
        catch (RejectedExecutionException error) { result.completeExceptionally(new IOException("Diagnostic worker is busy.")); }
        return result;
    }

    private Path latestRollingFile() throws IOException {
        if (!Files.isDirectory(directory)) return null;
        try (var files = Files.list(directory)) {
            return files.filter(path -> path.getFileName().toString().matches(FILE_PATTERN) && Files.isRegularFile(path))
                .max(Comparator.comparing(this::modified).thenComparing(path -> path.getFileName().toString().endsWith("-0.jsonl"))).orElse(null);
        }
    }

    private final class Write implements Runnable {
        private final Session session;
        private final String type;
        private final Map<String, Object> data;
        private final boolean critical;
        private final Instant time;
        private final long elapsed;
        private Write(Session session, String type, Map<String, Object> data, boolean critical, Instant time, long elapsed) {
            this.session = session; this.type = type; this.data = data; this.critical = critical; this.time = time; this.elapsed = elapsed;
        }
        @Override public void run() {
            try {
                if (build == null) {
                    try { build = Map.copyOf(buildMetadata.get()); }
                    catch (RuntimeException error) { build = Map.of("build_identity", "unavailable"); }
                }
                byte[] line = line(session, type, time, elapsed, data);
                var metadata = new LinkedHashMap<String, Object>(session.header);
                metadata.putAll(build);
                byte[] header = line(session, "header", time, elapsed, Map.copyOf(metadata));
                if (line.length > 64 * 1024 || header.length + line.length > maxBytes) {
                    dropped.incrementAndGet();
                    failure.compareAndSet(null, "An Autonav diagnostic record exceeded its size limit.");
                    return;
                }
                Files.createDirectories(directory);
                if (Files.exists(session.file) && Files.size(session.file) + line.length > maxBytes) rotate(session);
                if (!Files.exists(session.file)) Files.write(session.file, header, StandardOpenOption.CREATE_NEW);
                Files.write(session.file, line, StandardOpenOption.APPEND);
                retain(session.file);
            } catch (Exception error) {
                dropped.incrementAndGet();
                failure.compareAndSet(null, "Autonav diagnostics could not be written. Check local disk access; flight continues normally.");
            }
        }
    }

    private byte[] line(Session session, String type, Instant time, long elapsed, Map<String, Object> data) {
        return (GSON.toJson(Map.of("session", session.id, "event", type, "time_utc", time.toString(), "elapsed_ms", elapsed,
            "dropped_records", dropped.get(), "data", data)) + "\n").getBytes(StandardCharsets.UTF_8);
    }
    private void rotate(Session session) throws IOException {
        Files.deleteIfExists(directory.resolve(session.id + "-" + (maxFiles - 1) + ".jsonl"));
        for (int index = maxFiles - 2; index >= 0; index--) {
            Path from = directory.resolve(session.id + "-" + index + ".jsonl");
            if (Files.exists(from)) Files.move(from, directory.resolve(session.id + "-" + (index + 1) + ".jsonl"), StandardCopyOption.REPLACE_EXISTING);
        }
    }
    private void retain(Path current) throws IOException {
        List<Path> files;
        try (var entries = Files.list(directory)) {
            files = entries.filter(path -> path.getFileName().toString().matches(FILE_PATTERN) && Files.isRegularFile(path))
                .sorted(Comparator.comparing(this::modified).reversed()).toList();
        }
        int kept = 1;
        for (Path file : files) if (!file.equals(current) && kept++ >= maxFiles) Files.deleteIfExists(file);
    }
    private java.nio.file.attribute.FileTime modified(Path path) {
        try { return Files.getLastModifiedTime(path); }
        catch (IOException error) { return java.nio.file.attribute.FileTime.fromMillis(0); }
    }
    @Override public void close() {
        worker.shutdown();
        try { if (!worker.awaitTermination(5, TimeUnit.SECONDS)) worker.shutdownNow(); }
        catch (InterruptedException error) { worker.shutdownNow(); Thread.currentThread().interrupt(); }
    }
}
