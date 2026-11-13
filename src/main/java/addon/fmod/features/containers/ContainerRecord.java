package addon.fmod.features.containers;

import java.util.*;

public record ContainerRecord(String dimension, int x, int y, int z, String kind, int slots,
                              Map<String, Long> items, Set<String> names, long observedAt, boolean verified) {
    public ContainerRecord {
        if (dimension == null || dimension.length() > 128 || kind == null || kind.length() > 128
            || Math.abs((long)x) > 30_000_000 || Math.abs((long)z) > 30_000_000 || Math.abs((long)y) > 4096
            || slots < 0 || slots > 256 || items == null || items.size() > 512 || names == null || names.size() > 128
            || items.entrySet().stream().anyMatch(entry -> entry.getKey() == null || entry.getKey().length() > 256 || entry.getValue() == null || entry.getValue() < 0 || entry.getValue() > 1_000_000_000_000L)
            || names.stream().anyMatch(name -> name == null || name.length() > 256)) throw new IllegalArgumentException("Invalid container record.");
        items = Map.copyOf(items); names = Set.copyOf(names);
    }
    public String key() { return dimension + ":" + x + ":" + y + ":" + z; }
    public long totalItems() { return items.values().stream().mapToLong(Long::longValue).sum(); }
    public boolean matches(String query) {
        String term = query.strip().toLowerCase(Locale.ROOT);
        return term.isEmpty() || items.keySet().stream().anyMatch(id -> id.toLowerCase(Locale.ROOT).contains(term))
            || names.stream().anyMatch(name -> name.toLowerCase(Locale.ROOT).contains(term));
    }
}
