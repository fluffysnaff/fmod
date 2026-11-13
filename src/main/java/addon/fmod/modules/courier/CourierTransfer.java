package addon.fmod.modules.courier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** One bounded inventory operation. Completion requires a fresh server snapshot after reopening. */
public final class CourierTransfer {
    public enum Mode { LoadCargo, DepositAll, DepositUntilFull, WithdrawCargo, TakePearls, Inspect }
    public enum State { Running, Done, Failed }
    private enum Stage { Opening, Moving, Reopening }

    private final Minecraft mc;
    private final ClientLevel level;
    private final BlockPos target;
    private final boolean enderChest;
    private final Mode mode;
    private final List<ItemStack> manifest;
    private final List<ItemStack> requiredBefore;
    private final int limit;
    private final long deadline;
    private Stage stage = Stage.Opening;
    private State state = State.Running;
    private ChestMenu menu;
    private ClientboundContainerSetContentPacket received;
    private List<ItemStack> expected;
    private List<ItemStack> containerBefore = List.of();
    private List<ItemStack> containerAfter = List.of();
    private List<ItemStack> playerAfter = List.of();
    private String error = "";
    private boolean openSent;

    public CourierTransfer(Minecraft mc, BlockPos target, boolean enderChest, Mode mode,
                           List<ItemStack> manifest, List<ItemStack> requiredBefore, int limit, long timeoutNanos) {
        this.mc = mc;
        level = mc.level;
        this.target = target;
        this.enderChest = enderChest;
        this.mode = mode;
        this.manifest = CourierCargo.copy(manifest);
        this.requiredBefore = requiredBefore == null ? null : CourierCargo.copy(requiredBefore);
        this.limit = limit;
        deadline = System.nanoTime() + timeoutNanos;
    }

    public void receive(ClientboundContainerSetContentPacket packet) {
        if (state == State.Running && openSent && (stage == Stage.Opening || stage == Stage.Reopening)) received = packet;
    }

    public State tick() {
        if (state != State.Running) return state;
        if (mc.player == null || mc.gameMode == null || mc.level != level || !mc.player.isAlive()) return fail("World or player changed during transfer.");
        if (System.nanoTime() >= deadline) return fail("Container operation timed out. Cargo was not confirmed transferred.");
        if (!mc.player.containerMenu.getCarried().isEmpty()) return fail("Clear the cursor stack manually before continuing.");
        if (!mc.player.getOffhandItem().isEmpty() || mc.player.isShiftKeyDown()) return fail("Keep the offhand empty and release sneak while transferring.");
        if (!validTarget()) return fail("Configured container is missing or outside interaction range.");

        if (stage == Stage.Opening || stage == Stage.Reopening) {
            if (!openSent) {
                if (mc.player.containerMenu != mc.player.inventoryMenu) return fail("Close the current container before starting.");
                received = null;
                openSent = true;
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false));
                return state;
            }
            if (!(mc.player.containerMenu instanceof ChestMenu opened) || received == null
                || received.containerId() != opened.containerId) return state;
            if (!received.carriedItem().isEmpty()) return fail("Server reports a cursor stack; transfer stopped.");
            if (received.items().size() != opened.slots.size()) return fail("Unsupported container layout.");
            for (int i = 0; i < opened.slots.size(); i++) {
                if (!ItemStack.matches(opened.getSlot(i).getItem(), received.items().get(i))) return state;
            }
            menu = opened;
            if (stage == Stage.Reopening) {
                if (!sameSlots(expected, received.items())) return fail("Server contents differ from the predicted transfer. Review both inventories.");
                finish(received.items());
                return state;
            }
            containerBefore = CourierCargo.copy(received.items().subList(0, containerSlots()));
            if (requiredBefore != null && !CourierCargo.sameContents(requiredBefore, containerBefore)) {
                return fail("Container cargo changed unexpectedly. No further items were moved.");
            }
            if (mode == Mode.Inspect) {
                finish(received.items());
                return state;
            }
            stage = Stage.Moving;
        }

        if (mc.player.containerMenu != menu) return fail("Container changed during transfer.");
        int slot = nextSlot();
        if (slot < 0) {
            verifyAgain();
            return state;
        }
        List<ItemStack> before = CourierCargo.copy(menu.getItems());
        mc.gameMode.handleContainerInput(menu.containerId, slot, 0, ContainerInput.QUICK_MOVE, mc.player);
        if (sameSlots(before, menu.getItems())) {
            if (mode == Mode.DepositUntilFull) verifyAgain();
            else return fail("No inventory space or transfer refused. Cargo remains where it is.");
        }
        return state;
    }

    private int nextSlot() {
        int size = containerSlots();
        List<ItemStack> player = menu.getItems().subList(size, menu.slots.size());
        if (mode == Mode.DepositAll || mode == Mode.DepositUntilFull) {
            for (int i = size; i < menu.slots.size(); i++) if (menu.getSlot(i).hasItem()) return i;
        } else if (mode == Mode.LoadCargo) {
            if (CourierCargo.nonEmpty(player).size() >= limit) return -1;
            for (int i = 0; i < size; i++) if (CourierCargo.isCargo(menu.getSlot(i).getItem())) return i;
        } else if (mode == Mode.TakePearls) {
            if (player.stream().anyMatch(stack -> stack.is(Items.ENDER_PEARL))) return -1;
            for (int i = 0; i < size; i++) if (menu.getSlot(i).getItem().is(Items.ENDER_PEARL)) return i;
        } else if (mode == Mode.WithdrawCargo) {
            for (int i = 0; i < size; i++) {
                ItemStack stack = menu.getSlot(i).getItem();
                if (!stack.isEmpty() && CourierCargo.count(player, stack) < CourierCargo.count(manifest, stack)) return i;
            }
        }
        return -1;
    }

    private boolean validTarget() {
        if (!level.hasChunkAt(target) || !mc.player.isWithinBlockInteractionRange(target, 0)) return false;
        if (enderChest) return level.getBlockState(target).is(Blocks.ENDER_CHEST);
        var blockEntity = level.getBlockEntity(target);
        return blockEntity instanceof ChestBlockEntity || blockEntity instanceof BarrelBlockEntity;
    }

    private int containerSlots() { return menu.getRowCount() * 9; }
    private void verifyAgain() {
        expected = CourierCargo.copy(menu.getItems()); mc.player.closeContainer(); menu = null;
        received = null; openSent = false; stage = Stage.Reopening;
    }

    private void finish(List<ItemStack> contents) {
        containerAfter = CourierCargo.copy(contents.subList(0, containerSlots()));
        playerAfter = CourierCargo.copy(contents.subList(containerSlots(), contents.size()));
        mc.player.closeContainer();
        state = State.Done;
    }

    private State fail(String reason) { error = reason; return state = State.Failed; }

    private static boolean sameSlots(List<ItemStack> left, List<ItemStack> right) {
        if (left.size() != right.size()) return false;
        for (int i = 0; i < left.size(); i++) if (!ItemStack.matches(left.get(i), right.get(i))) return false;
        return true;
    }

    public String error() { return error; }
    public List<ItemStack> containerBefore() { return containerBefore; }
    public List<ItemStack> containerAfter() { return containerAfter; }
    public List<ItemStack> playerAfter() { return playerAfter; }
}
