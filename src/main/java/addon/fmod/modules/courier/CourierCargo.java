package addon.fmod.modules.courier;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import java.util.ArrayList;
import java.util.List;

public final class CourierCargo {
    private CourierCargo() {}

    public static boolean isCargo(ItemStack stack) {
        return stack.getCount() == 1 && stack.getItem() instanceof BlockItem item
            && item.getBlock() instanceof ShulkerBoxBlock && stack.get(DataComponents.CONTAINER) != null
            && stack.get(DataComponents.CONTAINER).nonEmptyItems().iterator().hasNext();
    }

    public static List<ItemStack> copy(List<ItemStack> stacks) {
        return stacks.stream().map(ItemStack::copy).toList();
    }

    public static List<ItemStack> nonEmpty(List<ItemStack> stacks) {
        return copy(stacks.stream().filter(stack -> !stack.isEmpty()).toList());
    }

    public static int count(List<ItemStack> stacks, ItemStack kind) {
        int total = 0;
        for (ItemStack stack : stacks) {
            if (ItemStack.isSameItemSameComponents(stack, kind)) total += stack.getCount();
        }
        return total;
    }

    public static boolean sameContents(List<ItemStack> left, List<ItemStack> right) {
        List<ItemStack> remaining = new ArrayList<>(copy(right));
        for (ItemStack stack : left) {
            if (stack.isEmpty()) continue;
            int needed = stack.getCount();
            for (ItemStack other : remaining) {
                if (!ItemStack.isSameItemSameComponents(stack, other)) continue;
                int taken = Math.min(needed, other.getCount());
                needed -= taken;
                other.shrink(taken);
            }
            if (needed != 0) return false;
        }
        return remaining.stream().allMatch(ItemStack::isEmpty);
    }

    public static List<ItemStack> combined(List<ItemStack> first, List<ItemStack> second) {
        List<ItemStack> result = new ArrayList<>(copy(first));
        result.addAll(copy(second));
        return result;
    }

    public static List<ItemStack> subtract(List<ItemStack> whole, List<ItemStack> remainder) {
        List<ItemStack> difference = new ArrayList<>(copy(whole));
        for (ItemStack stack : remainder) {
            int needed = stack.getCount();
            for (ItemStack candidate : difference) {
                if (!ItemStack.isSameItemSameComponents(stack, candidate)) continue;
                int taken = Math.min(needed, candidate.getCount()); candidate.shrink(taken); needed -= taken;
            }
            if (needed != 0) throw new IllegalArgumentException("Shipment remainder differs from the manifest.");
        }
        return nonEmpty(difference);
    }
}
