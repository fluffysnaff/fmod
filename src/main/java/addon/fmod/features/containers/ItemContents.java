package addon.fmod.features.containers;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.*;

public final class ItemContents {
    public record Summary(Map<String, Long> items, Set<String> names, Item mostCommon, boolean mixed) {
        public Summary { items = Map.copyOf(items); names = Set.copyOf(names); }
    }
    private ItemContents() {}

    public static Summary summarize(List<ItemStack> stacks, boolean nested) {
        Map<String, Long> items = new TreeMap<>();
        Set<String> names = new TreeSet<>();
        for (ItemStack stack : stacks) add(stack, nested, 0, items, names);
        Item mostCommon = null;
        long largest = -1;
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            long count = items.getOrDefault(id, 0L);
            if (count > largest) { mostCommon = stack.getItem(); largest = count; }
        }
        return new Summary(items, names, mostCommon, items.size() > 1);
    }

    public static Summary shulker(ItemContainerContents contents) {
        return summarize(contents.nonEmptyItemCopyStream().limit(27).toList(), false);
    }

    private static void add(ItemStack stack, boolean nested, int depth, Map<String, Long> items, Set<String> names) {
        if (stack.isEmpty() || items.size() >= 512) return;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        items.merge(id, (long) stack.getCount(), Math::addExact);
        if (stack.has(DataComponents.CUSTOM_NAME) && names.size() < 128) {
            String name = stack.getHoverName().getString();
            names.add(name.substring(0, Math.min(name.length(), 256)));
        }
        ItemContainerContents inner = stack.get(DataComponents.CONTAINER);
        if (nested && depth < 3 && inner != null) {
            inner.nonEmptyItemCopyStream().limit(54).forEach(child -> add(child, true, depth + 1, items, names));
        }
    }
}
