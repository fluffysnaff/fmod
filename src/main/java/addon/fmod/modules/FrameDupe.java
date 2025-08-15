package addon.fmod.modules;

import addon.fmod.FMod;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
// import meteordevelopment.meteorclient.utils.player.FindItemResult; // Not directly used but good to keep if InvUtils.findInHotbar is used elsewhere
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FrameDupe extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDelay = settings.createGroup("Delays");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The maximum range to search for item frames.")
        .defaultValue(4.5)
        .min(1)
        .sliderMax(10)
        .build()
    );
    private final Setting<Boolean> sortFrames = sgGeneral.add(new BoolSetting.Builder()
        .name("sort-frames")
        .description("Sorts item frames by distance before processing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> delayAfterSwitch = sgDelay.add(new IntSetting.Builder()
        .name("delay-after-switch")
        .description("Ticks to wait after switching items (if needed).")
        .defaultValue(2)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Integer> delayBeforeRightClick = sgDelay.add(new IntSetting.Builder()
        .name("delay-before-right-click")
        .description("Ticks to wait before right-clicking the item frame.")
        .defaultValue(3)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Integer> delayBeforeLeftClick = sgDelay.add(new IntSetting.Builder()
        .name("delay-before-left-click")
        .description("Ticks to wait between right-click and left-click.")
        .defaultValue(3)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Integer> delayNextFrame = sgDelay.add(new IntSetting.Builder()
        .name("delay-next-frame")
        .description("Ticks to wait before processing the next item frame.")
        .defaultValue(5)
        .min(0)
        .sliderMax(40)
        .build()
    );

    private ItemStack savedInitialStack;
    private boolean savedItemWasNonStackable;

    private final List<ItemFrameEntity> itemFrames = new ArrayList<>();
    private int currentFrameIndex;
    private ItemFrameEntity currentTargetFrame;

    private enum State {
        IDLE,
        SEARCHING_FRAMES,
        CHECK_ITEM_SWITCH,
        POST_SWITCH_DELAY,
        PRE_RIGHT_CLICK_DELAY,
        RIGHT_CLICKING,
        PRE_LEFT_CLICK_DELAY,
        LEFT_CLICKING,
        DELAY_BEFORE_NEXT_OPERATION
    }

    private State currentState = State.IDLE;
    private int timer = 0;

    public FrameDupe() {
        super(FMod.CATEGORY, "frame-dupe", "Automatically interacts with nearby item frames.");
    }

    @Override
    public void onActivate() {
        if (mc.player == null || mc.interactionManager == null) {
            error("Player or InteractionManager not available.");
            toggle();
            return;
        }
        savedInitialStack = mc.player.getMainHandStack().copy(); // Make a copy
        if (savedInitialStack.isEmpty()) {
            error("Cannot activate with an empty hand.");
            toggle();
            return;
        }
        savedItemWasNonStackable = savedInitialStack.getMaxCount() == 1;

        currentState = State.SEARCHING_FRAMES;
        itemFrames.clear();
        currentFrameIndex = -1;
        currentTargetFrame = null;
        timer = 0;
        info("Activated. Initial item: %s (Non-stackable: %b)", savedInitialStack.getName().getString(), savedItemWasNonStackable);
    }

    @Override
    public void onDeactivate() {
        currentState = State.IDLE;
        itemFrames.clear();
        savedInitialStack = null;
        currentTargetFrame = null;
        if (InvUtils.previousSlot != -1) {
            InvUtils.swapBack();
        }
        info("Deactivated.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || savedInitialStack == null) {
            if (isActive()) toggle();
            return;
        }

        if (timer > 0) {
            timer--;
            return;
        }

        switch (currentState) {
            case SEARCHING_FRAMES:
                itemFrames.clear();
                for (Entity entity : mc.world.getEntities()) {
                    if (entity instanceof ItemFrameEntity itemFrameEntity && isTargetValid(itemFrameEntity)) {
                        itemFrames.add(itemFrameEntity);
                    }
                }

                if (sortFrames.get()) {
                    itemFrames.sort(Comparator.comparingDouble(e -> mc.player.squaredDistanceTo(e)));
                }
                currentFrameIndex = 0;

                setupNextTargetFrame();
                if (currentTargetFrame != null) {
                    currentState = State.CHECK_ITEM_SWITCH;
                } else {
                    info("No item frames found in range. Will retry.");
                    timer = delayNextFrame.get() * 2;
                    currentState = State.DELAY_BEFORE_NEXT_OPERATION;
                }
                break;

            case CHECK_ITEM_SWITCH:
                if (currentTargetFrame == null || !isTargetValid(currentTargetFrame)) {
                    advanceToNextFrameOrRescan();
                    break;
                }

                if (savedItemWasNonStackable) {
                    ItemStack currentMainHand = mc.player.getMainHandStack();
                    // Use areItemsAndComponentsEqual for proper comparison in 1.20.2+
                    boolean holdingSavedItemInMainHand = ItemStack.areItemsAndComponentsEqual(currentMainHand, savedInitialStack);

                    if (!holdingSavedItemInMainHand) {
                        int hotbarSlotToSwitch = -1;
                        for (int i = 0; i < 9; i++) {
                            ItemStack hotbarStack = mc.player.getInventory().getStack(i);
                            // Use areItemsAndComponentsEqual here as well
                            if (ItemStack.areItemsAndComponentsEqual(hotbarStack, savedInitialStack)) {
                                hotbarSlotToSwitch = i;
                                break;
                            }
                        }

                        if (hotbarSlotToSwitch != -1) {
                            if (mc.player.getInventory().getSelectedSlot() != hotbarSlotToSwitch) {
                                InvUtils.swap(hotbarSlotToSwitch, false);
                                currentState = State.POST_SWITCH_DELAY;
                                timer = delayAfterSwitch.get();
                            } else {
                                currentState = State.PRE_RIGHT_CLICK_DELAY;
                                timer = delayBeforeRightClick.get();
                            }
                        } else {
                            error("Saved non-stackable item '%s' not found in hotbar. Skipping frame.", savedInitialStack.getName().getString());
                            advanceToNextFrameOrRescan();
                        }
                        break;
                    }
                }
                currentState = State.PRE_RIGHT_CLICK_DELAY;
                timer = delayBeforeRightClick.get();
                break;

            case POST_SWITCH_DELAY:
                if (currentTargetFrame == null || !isTargetValid(currentTargetFrame)) {
                    advanceToNextFrameOrRescan();
                    break;
                }
                currentState = State.PRE_RIGHT_CLICK_DELAY;
                timer = delayBeforeRightClick.get();
                break;

            case PRE_RIGHT_CLICK_DELAY:
                if (currentTargetFrame == null || !isTargetValid(currentTargetFrame)) {
                    advanceToNextFrameOrRescan();
                    break;
                }
                currentState = State.RIGHT_CLICKING;
                break;

            case RIGHT_CLICKING:
                if (currentTargetFrame == null || !isTargetValid(currentTargetFrame)) {
                    advanceToNextFrameOrRescan();
                    break;
                }
                mc.interactionManager.interactEntity(mc.player, currentTargetFrame, Hand.MAIN_HAND);
                currentState = State.PRE_LEFT_CLICK_DELAY;
                timer = delayBeforeLeftClick.get();
                break;

            case PRE_LEFT_CLICK_DELAY:
                if (currentTargetFrame == null || !isTargetValid(currentTargetFrame)) {
                    advanceToNextFrameOrRescan();
                    break;
                }
                currentState = State.LEFT_CLICKING;
                break;

            case LEFT_CLICKING:
                if (currentTargetFrame == null || !isTargetValid(currentTargetFrame)) {
                    advanceToNextFrameOrRescan();
                    break;
                }
                mc.interactionManager.attackEntity(mc.player, currentTargetFrame);
                advanceToNextFrameOrRescan();
                break;

            case DELAY_BEFORE_NEXT_OPERATION:
                if (currentTargetFrame != null) {
                    currentState = State.CHECK_ITEM_SWITCH;
                } else {
                    currentState = State.SEARCHING_FRAMES;
                }
                break;

            case IDLE:
                // Do nothing
                break;
        }
    }

    private boolean isTargetValid(ItemFrameEntity entity) {
        if (entity == null || !entity.isAlive()) return false;
        return mc.player.getEyePos().distanceTo(entity.getPos()) <= range.get();
    }

    private void setupNextTargetFrame() {
        currentTargetFrame = null;
        while (currentFrameIndex < itemFrames.size()) {
            ItemFrameEntity frame = itemFrames.get(currentFrameIndex);
            if (isTargetValid(frame)) {
                currentTargetFrame = frame;
                return;
            }
            currentFrameIndex++;
        }
    }

    private void advanceToNextFrameOrRescan() {
        currentFrameIndex++;
        setupNextTargetFrame();

        currentState = State.DELAY_BEFORE_NEXT_OPERATION;
        timer = delayNextFrame.get();
    }
}
