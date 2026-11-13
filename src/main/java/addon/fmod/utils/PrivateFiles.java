package addon.fmod.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Coordinate-bearing feature data stays local. Writes retain only the latest immutable snapshot per file. */
public final class PrivateFiles {
    private static final int MAX_BYTES = 32 * 1024 * 1024;
    private static final Gson GSON = new GsonBuilder().create();
    private static final ThreadPoolExecutor WORKER = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(8), task -> { Thread thread = new Thread(task, "fmod-private-files"); thread.setDaemon(true); return thread; });
    private static final Map<Path, WriteSlot> WRITES = new HashMap<>();

    private PrivateFiles() {}

    public static <T> CompletableFuture<T> read(Path path, Type type, T empty) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!Files.exists(path)) return empty;
                if (!Files.isRegularFile(path) || Files.size(path) > MAX_BYTES) throw new IOException("Private file exceeds its size limit.");
                try (var input = Files.newInputStream(path)) {
                    byte[] bytes = input.readNBytes(MAX_BYTES + 1);
                    if (bytes.length > MAX_BYTES) throw new IOException("Private file exceeds its size limit.");
                    T value = GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), type);
                    if (value == null) throw new IOException("Empty private data file.");
                    return value;
                }
            } catch (Exception exception) { throw new CompletionException(new IOException("Private data could not be read.")); }
        }, WORKER);
    }

    public static CompletableFuture<java.util.List<String>> readText(Path path) {
        return CompletableFuture.supplyAsync(()->{
            try(var input=Files.newInputStream(path)){
                byte[] bytes=input.readNBytes(1024*1024+1);if(bytes.length>1024*1024)throw new IOException("Text file exceeds 1 MiB.");
                return new String(bytes,StandardCharsets.UTF_8).lines().limit(8192).map(line->line.substring(0,Math.min(384,line.length()))).toList();
            }catch(Exception error){throw new CompletionException(new IOException("Template file could not be read."));}
        },WORKER);
    }

    public static void save(Path path, Object immutableSnapshot, Consumer<String> failure) {
        synchronized (WRITES) {
            WriteSlot existing = WRITES.get(path);
            if (existing != null) { existing.latest = immutableSnapshot; return; }
            if (WRITES.size() >= 8) { failure.accept("Private save worker is busy; the latest data remains in memory."); return; }
            WriteSlot slot = new WriteSlot(path, immutableSnapshot, failure);
            WRITES.put(path, slot);
            try { WORKER.execute(() -> drain(slot)); }
            catch (RejectedExecutionException exception) { WRITES.remove(path); failure.accept("Private save worker is busy; the latest data remains in memory."); }
        }
    }

    private static void drain(WriteSlot slot) {
        while (true) {
            Object snapshot;
            synchronized (WRITES) {
                snapshot = slot.latest;
                slot.latest = null;
                if (snapshot == null) { WRITES.remove(slot.path); return; }
            }
            try { atomicWrite(slot.path, GSON.toJson(snapshot)); }
            catch (Exception exception) { slot.failure.accept("Private data could not be saved. Check local disk access."); }
        }
    }

    private static void atomicWrite(Path path, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_BYTES) throw new IOException("Private snapshot exceeds 32 MiB.");
        Path directory = path.toAbsolutePath().getParent();
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, "fmod-", ".tmp");
        try {
            Files.write(temporary, bytes);
            try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException exception) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }

    private static final class WriteSlot {
        private final Path path;
        private Object latest;
        private final Consumer<String> failure;
        private WriteSlot(Path path, Object latest, Consumer<String> failure) { this.path = path; this.latest = latest; this.failure = failure; }
    }
}
