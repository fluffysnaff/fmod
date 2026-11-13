package addon.fmod.utils;

import com.google.gson.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Private coordinate-only imports. Raw messages and author fields are never part of the saved model. */
public final class BaseScoutExclusions {
    public static final int MAX_ENTRIES = 100_000;
    private static final long MAX_BYTES = 20 * 1024 * 1024;
    private static final Pattern PAIR = Pattern.compile("(?<![\\w/])([+-]?\\d{1,9})\\s*[,;]\\s*([+-]?\\d{1,9})(?:\\s*[,;]\\s*([+-]?\\d{1,9}))?(?![\\w\\d])");
    private static final Pattern LABELLED = Pattern.compile("(?i)\\bx\\s*[:=]\\s*([+-]?[\\d,]+)\\s*(?:[,; ]+y\\s*[:=]\\s*[+-]?[\\d,]+\\s*)?[,; ]+z\\s*[:=]\\s*([+-]?[\\d,]+)");
    private static final Pattern DIMENSION = Pattern.compile("(?i)(?<![a-z])(?:minecraft:)?(the_nether|nether|overworld|the_end|end)(?![a-z])");
    private static final Pattern CONTEXT = Pattern.compile("(?i)\\b(at|coords?|coordinates?|position|xyz|stash|base|kit)\\b");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public enum Dimension { overworld, nether, end, unknown }
    public record Entry(int x, int z, Dimension dimension, int radius) {
        public Entry {
            if (Math.abs((long) x) > BaseSitePlanner.WORLD_LIMIT || Math.abs((long) z) > BaseSitePlanner.WORLD_LIMIT
                || dimension == null || radius < 0 || radius > BaseSitePlanner.WORLD_LIMIT) throw new IllegalArgumentException("Invalid exclusion entry.");
        }
    }
    public record ImportResult(List<Entry> entries, int records, int skipped) {
        public ImportResult { entries = deduplicate(entries); }
        public long reviewCount() { return entries.stream().filter(entry -> entry.dimension == Dimension.unknown).count(); }
    }
    public record Hit(double distance, int clearance, boolean ambiguous) {}

    private BaseScoutExclusions() {}

    public static ImportResult importFile(Path path, int radius) throws IOException {
        String text = readBounded(path);
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        Collector collector = new Collector(radius);
        if (name.endsWith(".json")) parseJson(text, collector);
        else if (name.endsWith(".csv")) parseCsv(text, collector);
        else {
            if (name.endsWith(".html") || name.endsWith(".htm")) {
                text = text.replaceAll("(?is)<(script|style)\\b.*?</\\1\\s*>", " ")
                    .replaceAll("(?i)<br\\s*/?>|</(?:div|p|li|tr|article|section)\\s*>", "\n")
                    .replaceAll("(?s)<[^>]*>", " ").replace("&nbsp;", " ").replace("&minus;", "-")
                    .replace("&#45;", "-").replace("&quot;", "\"").replace("&amp;", "&");
            }
            for (String line : text.split("\\R")) collector.message(line);
        }
        return new ImportResult(collector.entries, collector.records, collector.skipped);
    }

    public static List<Entry> load(Path path) throws IOException {
        if (!Files.exists(path)) return List.of();
        Collector collector = new Collector(0);
        parseJson(readBounded(path), collector);
        return deduplicate(collector.entries);
    }

    public static List<Entry> merge(List<Entry> existing, List<Entry> incoming) {
        List<Entry> combined = new ArrayList<>(existing);
        combined.addAll(incoming);
        return deduplicate(combined);
    }

    public static void save(Path path, List<Entry> entries) throws IOException {
        List<Entry> unique = deduplicate(entries);
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        JsonArray locations = new JsonArray();
        for (Entry entry : unique) {
            JsonObject item = new JsonObject();
            item.addProperty("x", entry.x);
            item.addProperty("z", entry.z);
            item.addProperty("dimension", entry.dimension.name());
            item.addProperty("radius", entry.radius);
            locations.add(item);
        }
        root.add("locations", locations);
        Path parent = path.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, "exclusions-", ".tmp");
        try {
            Files.writeString(temporary, GSON.toJson(root), StandardCharsets.UTF_8);
            try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException exception) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }

    public static Path serverFile(Path directory, String identity) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8));
            return directory.resolve(HexFormat.of().formatHex(hash) + ".json");
        } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 unavailable.", exception); }
    }

    public static String serverIdentity(String address) {
        String normalized = address.strip().toLowerCase(Locale.ROOT);
        if (normalized.endsWith(":25565")) normalized = normalized.substring(0, normalized.length() - 6);
        if (normalized.endsWith(".")) normalized = normalized.substring(0, normalized.length() - 1);
        return "server:" + normalized;
    }

    private static String readBounded(Path path) throws IOException {
        if (!Files.isRegularFile(path) || Files.size(path) > MAX_BYTES) throw new IOException("Choose a regular file no larger than 20 MiB.");
        try (var input = Files.newInputStream(path)) {
            byte[] bytes = input.readNBytes((int) MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) throw new IOException("Import exceeds 20 MiB.");
            String text = new String(bytes, StandardCharsets.UTF_8);
            return text.startsWith("\uFEFF") ? text.substring(1) : text;
        }
    }

    private static List<Entry> deduplicate(List<Entry> entries) {
        record Key(int x, int z, Dimension dimension) {}
        Map<Key, Entry> unique = new LinkedHashMap<>();
        for (Entry entry : entries) {
            Key key = new Key(entry.x, entry.z, entry.dimension);
            unique.merge(key, entry, (first, second) -> first.radius >= second.radius ? first : second);
            if (unique.size() > MAX_ENTRIES) throw new IllegalArgumentException("At most 100,000 unique exclusions are supported.");
        }
        return List.copyOf(unique.values());
    }

    private static void parseJson(String text, Collector collector) throws IOException {
        // Reject excessive nesting before the parser sees it, including in unused author/message metadata.
        int depth = 0;
        boolean quoted = false, escaped = false;
        for (char ch : text.toCharArray()) {
            if (quoted) {
                if (escaped) escaped = false;
                else if (ch == '\\') escaped = true;
                else if (ch == '"') quoted = false;
            } else if (ch == '"') quoted = true;
            else if (ch == '{' || ch == '[') { if (++depth > 32) throw new IOException("JSON nesting exceeds 32 levels."); }
            else if (ch == '}' || ch == ']') depth--;
        }
        try {
            JsonElement value = JsonParser.parseString(text);
            JsonArray rows;
            boolean messages = false;
            if (value.isJsonArray()) rows = value.getAsJsonArray();
            else {
                JsonObject root = value.getAsJsonObject();
                if (root.has("version") && root.get("version").getAsInt() != 1) throw new IOException("Unsupported exclusion file version.");
                messages = !root.has("locations") && root.has("messages");
                rows = root.getAsJsonArray(messages ? "messages" : "locations");
            }
            if (rows == null || rows.size() > MAX_ENTRIES) throw new IOException("JSON needs locations or messages, with at most 100,000 rows.");
            for (JsonElement row : rows) {
                JsonObject object = row.getAsJsonObject();
                if (messages) {
                    if (object.has("content")) collector.message(object.get("content").getAsString());
                } else {
                    int x = object.get("x").getAsBigDecimal().intValueExact();
                    int z = object.get("z").getAsBigDecimal().intValueExact();
                    int radius = object.has("radius") ? object.get("radius").getAsBigDecimal().intValueExact() : collector.radius;
                    Dimension dimension = object.has("dimension") ? dimension(object.get("dimension").getAsString()) : Dimension.unknown;
                    collector.add(new Entry(x, z, dimension, radius));
                }
            }
        } catch (RuntimeException exception) { throw new IOException("Invalid coordinate JSON; existing exclusions were preserved."); }
    }

    private static Dimension dimension(String value) {
        return switch (value.strip().toLowerCase(Locale.ROOT).replace("minecraft:", "")) {
            case "overworld", "ow" -> Dimension.overworld;
            case "the_nether", "nether" -> Dimension.nether;
            case "the_end", "end" -> Dimension.end;
            case "unknown", "ambiguous", "" -> Dimension.unknown;
            default -> throw new IllegalArgumentException("Unknown dimension.");
        };
    }

    private static void parseCsv(String text, Collector collector) throws IOException {
        List<String> row = new ArrayList<>();
        List<String> headers = null;
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        int records = 0;
        for (int i = 0; i <= text.length(); i++) {
            char ch = i == text.length() ? '\n' : text.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') { field.append('"'); i++; }
                    else quoted = false;
                } else field.append(ch);
            } else if (ch == '"' && field.isEmpty()) quoted = true;
            else if (ch == ',' || ch == '\r' || ch == '\n') {
                row.add(field.toString()); field.setLength(0);
                if (row.size() > 128) throw new IOException("CSV has too many columns.");
                if (ch != ',') {
                    if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                    if (headers == null) headers = row.stream().map(value -> value.strip().toLowerCase(Locale.ROOT)).toList();
                    else if (!(row.size() == 1 && row.getFirst().isBlank())) {
                        if (++records > MAX_ENTRIES) throw new IOException("CSV exceeds 100,000 rows.");
                        int content = headers.indexOf("content");
                        if (content < 0) content = headers.indexOf("message");
                        if (content >= 0 && content < row.size()) collector.message(row.get(content));
                        else if (headers.contains("x") && headers.contains("z")) {
                            try {
                                String dim = headers.contains("dimension") ? row.get(headers.indexOf("dimension")) : "unknown";
                                int radius = headers.contains("radius") ? Integer.parseInt(row.get(headers.indexOf("radius"))) : collector.radius;
                                collector.add(new Entry(Integer.parseInt(row.get(headers.indexOf("x"))), Integer.parseInt(row.get(headers.indexOf("z"))), dimension(dim), radius));
                            } catch (RuntimeException exception) { throw new IOException("Invalid coordinate CSV row; existing exclusions were preserved."); }
                        } else throw new IOException("CSV needs a Content column or X and Z columns.");
                    }
                    row.clear();
                }
            } else field.append(ch);
            if (field.length() > 262_144) throw new IOException("CSV field exceeds 256 KiB.");
        }
        if (quoted) throw new IOException("Unclosed CSV quote.");
    }

    private static final class Collector {
        private final int radius;
        private final List<Entry> entries = new ArrayList<>();
        private int records, skipped;
        private Collector(int radius) { this.radius = radius; }
        private void add(Entry entry) {
            if (entries.size() >= MAX_ENTRIES) throw new IllegalArgumentException("Import exceeds 100,000 coordinate records.");
            entries.add(entry); records++;
        }
        private void message(String content) {
            if (content.length() > 262_144) throw new IllegalArgumentException("Message exceeds 256 KiB.");
            Set<Dimension> dimensions = new HashSet<>();
            Matcher labels = DIMENSION.matcher(content);
            while (labels.find()) dimensions.add(dimension(labels.group(1)));
            Dimension dim = dimensions.size() == 1 ? dimensions.iterator().next() : Dimension.unknown;
            Matcher named = LABELLED.matcher(content);
            boolean found = false;
            while (named.find()) {
                found |= coordinate(named.group(1).replace(",", ""), named.group(2).replace(",", ""), dim);
            }
            if (found) return;
            Matcher pair = PAIR.matcher(content);
            while (pair.find()) {
                String before = content.substring(Math.max(0, pair.start() - 50), pair.start());
                boolean standalone = content.strip().equals(pair.group()) || content.strip().equals("(" + pair.group() + ")");
                if (!standalone && !CONTEXT.matcher(before).find()) continue;
                if (pair.group().matches("[+-]?\\d{1,3}(?:,\\d{3}){1,2}")) continue;
                if (pair.group(3) != null && Math.abs(Long.parseLong(pair.group(2))) > 4096) continue;
                found |= coordinate(pair.group(1), pair.group(3) == null ? pair.group(2) : pair.group(3), dim);
            }
            if (!found) skipped++;
        }
        private boolean coordinate(String x, String z, Dimension dimension) {
            try { add(new Entry(Integer.parseInt(x), Integer.parseInt(z), dimension, radius)); return true; }
            catch (NumberFormatException exception) { return false; }
            catch (IllegalArgumentException exception) {
                if (entries.size() >= MAX_ENTRIES) throw exception;
                return false;
            }
        }
    }

    /** Radius-tiered buckets keep lookups bounded even for large private lists and varied radii. */
    public static final class Index {
        private record Circle(long x, long z, int radius, boolean ambiguous) {}
        private final Map<Long, Map<Long, List<Circle>>> levels = new HashMap<>();

        public Index(List<Entry> entries, int minimumRadius) {
            if (entries.size() > MAX_ENTRIES || minimumRadius < 0 || minimumRadius > BaseSitePlanner.WORLD_LIMIT) throw new IllegalArgumentException("Invalid exclusion index.");
            for (Entry entry : entries) {
                int radius = Math.max(minimumRadius, entry.radius);
                if (entry.dimension == Dimension.overworld || entry.dimension == Dimension.unknown) add(entry.x, entry.z, radius, entry.dimension == Dimension.unknown);
                if (entry.dimension == Dimension.nether || entry.dimension == Dimension.unknown) add(entry.x * 8L, entry.z * 8L, radius, entry.dimension == Dimension.unknown);
            }
        }
        private void add(long x, long z, int radius, boolean ambiguous) {
            long size = 1;
            while (size < radius) size <<= 1;
            levels.computeIfAbsent(size, ignored -> new HashMap<>()).computeIfAbsent(key(Math.floorDiv(x, size), Math.floorDiv(z, size)), ignored -> new ArrayList<>())
                .add(new Circle(x, z, radius, ambiguous));
        }
        public Optional<Hit> hit(long x, long z) {
            Hit best = null;
            for (var level : levels.entrySet()) {
                long size = level.getKey(), cellX = Math.floorDiv(x, size), cellZ = Math.floorDiv(z, size);
                for (long dx = -1; dx <= 1; dx++) for (long dz = -1; dz <= 1; dz++) {
                    for (Circle circle : level.getValue().getOrDefault(key(cellX + dx, cellZ + dz), List.of())) {
                        double distance = Math.hypot((double) x - circle.x, (double) z - circle.z);
                        if (distance <= circle.radius && (best == null || distance < best.distance)) best = new Hit(distance, circle.radius, circle.ambiguous);
                    }
                }
            }
            return Optional.ofNullable(best);
        }
        private static long key(long x, long z) { return (x << 32) ^ (z & 0xffffffffL); }
    }
}
