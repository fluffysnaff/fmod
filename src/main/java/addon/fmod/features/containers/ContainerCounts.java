package addon.fmod.features.containers;

import java.util.*;

public final class ContainerCounts {
    public record Block(int x, int y, int z, String type, int slots, boolean paired, int partnerX, int partnerZ, String color) {
        public String key() { return x + ":" + y + ":" + z; }
        public String partnerKey() { return partnerX + ":" + y + ":" + partnerZ; }
    }
    public record Result(Map<String, Integer> counts, int singles, int doubles, int partialDoubles,
                         Map<String, Integer> shulkers, long slots) {}
    private ContainerCounts() {}

    public static Result count(List<Block> blocks, Set<String> selected) {
        Map<String, Block> byPosition = new HashMap<>();
        for (Block block : blocks) if (selected.contains(block.type)) byPosition.put(block.key(), block);
        Set<String> seen = new HashSet<>();
        Map<String, Integer> counts = new TreeMap<>(), colors = new TreeMap<>();
        int singles = 0, doubles = 0, partial = 0;
        long slots = 0;
        for (Block block : byPosition.values()) {
            if (!seen.add(block.key())) continue;
            counts.merge(block.type, 1, Integer::sum);
            if (block.paired) {
                Block partner = byPosition.get(block.partnerKey());
                if (partner != null && partner.paired && partner.partnerKey().equals(block.key()) && partner.type.equals(block.type)) {
                    seen.add(partner.key()); doubles++; slots += block.slots + partner.slots;
                } else { partial++; slots += block.slots; }
            } else {
                slots += block.slots;
                if (block.type.equals("minecraft:chest") || block.type.equals("minecraft:trapped_chest")) singles++;
            }
            if (block.color != null) colors.merge(block.color, 1, Integer::sum);
        }
        return new Result(Map.copyOf(counts), singles, doubles, partial, Map.copyOf(colors), slots);
    }
}
