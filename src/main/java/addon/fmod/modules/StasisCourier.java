package addon.fmod.modules;

import addon.fmod.FMod;
import addon.fmod.modules.courier.CourierCargo;
import addon.fmod.modules.courier.CourierSafety.Position;
import addon.fmod.modules.courier.CourierSafety.ReturnCheck;
import addon.fmod.modules.courier.CourierTransfer;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class StasisCourier extends Module {
    public enum Recall { Stasis, Tpa }
    public enum CargoStorage { Carried, EnderChest }
    private enum Phase {
        Load, StoreInEnder, PrepareRecall, AwaitRecall, CollectFromEnder, Unload,
        TakePearls, ThrowPearl, AwaitPearl, StoreRemainder, ReturnEmpty, AwaitDeath, AwaitRespawn
    }

    private final SettingGroup general = settings.getDefaultGroup();
    private final Setting<Recall> recall = general.add(new EnumSetting.Builder<Recall>()
        .name("recall").description("B manually triggers stasis, or manually accepts one tpa request. Changing this invalidates the empty trials.")
        .defaultValue(Recall.Stasis).onChanged(value -> invalidate()).build());
    private final Setting<CargoStorage> cargoStorage = general.add(new EnumSetting.Builder<CargoStorage>()
        .name("cargo-storage").description("Carry sealed shulkers normally, or protect the shipment in A's own Ender Chest.")
        .defaultValue(CargoStorage.EnderChest).onChanged(value -> invalidate()).build());
    private final Setting<String> operator = general.add(new StringSetting.Builder()
        .name("operator").description("B's exact Java username for Tpa mode. B accepts manually; never sends home commands.")
        .defaultValue("").visible(() -> recall.get() == Recall.Tpa).onChanged(value -> invalidate()).build());
    private final Setting<Integer> shulkers = general.add(new IntSetting.Builder()
        .name("shulkers-per-shipment").description("Only shulker boxes containing items are cargo. Other source items are left alone.")
        .defaultValue(9).range(1, 27).sliderRange(1, 27).onChanged(value -> stop("Shipment settings changed.")).build());
    private final Setting<Integer> shipments = general.add(new IntSetting.Builder()
        .name("shipments").description("Maximum deliveries in one run, followed by an empty return to the source.")
        .defaultValue(1).range(1, 1000).sliderRange(1, 100).onChanged(value -> stop("Shipment settings changed.")).build());
    private final Setting<Integer> recallTimeout = general.add(new IntSetting.Builder()
        .name("recall-timeout-seconds").description("Stop if arrival is not observed. Never treats a timeout as a successful recall or retries a teleport automatically.")
        .defaultValue(120).range(10, 600).sliderRange(10, 300).build());
    private final Setting<Integer> chamberRadius = general.add(new IntSetting.Builder()
        .name("chamber-radius").description("Radius around the marked chamber block where A's newly thrown pearl must remain visible before the empty return.")
        .defaultValue(3).range(1, 5).sliderRange(1, 5).onChanged(value -> invalidate()).build());
    private final Setting<Boolean> containerAreas = general.add(new BoolSetting.Builder()
        .name("container-areas").description("Collect cargo from and distribute cargo across the marked received chest/barrel areas. All used containers must be reachable; no travel route is created.")
        .defaultValue(false).onChanged(value -> invalidate()).build());
    private final Setting<Boolean> renderAreas = general.add(new BoolSetting.Builder()
        .name("render-areas").description("Outline marked cargo areas in the current dimension.").defaultValue(true).build());

    private BlockPos sourceBed;
    private StationBlock sourceStorage, sourceEnder, destinationStorage, destinationEnder, chamber;
    private ThrowPose destination;
    private StationBlock sourceCorner1, sourceCorner2, destinationCorner1, destinationCorner2;
    private int verifiedTrials;
    private Run run;
    private String status = "Enable the module, then mark both stations. Locations are session-only.";

    public StasisCourier() {
        super(FMod.CATEGORY, "stash-mover", "Automates A's shulker shipments and empty /kill bed returns. B recalls manually; test the route before cargo.", "stasis-courier", "stashmover");
    }
    public boolean isRunning() { return run != null; }
    public void beginTrials(){startTrials();printStatus();}
    public void beginShipments(){startShipments();printStatus();}
    public void stopAutomation(){stop("Automation stopped. Already transferred items remain in their containers; no rollback is performed.");printStatus();}
    public void printStatus(){info("%s Empty trials: %d/2. %s",run==null?"Stopped.":"Step: "+run.phase+".",verifiedTrials,status);}

    @Override public void onActivate() { clearAll(); }
    @Override public void onDeactivate() { clearAll(); }
    @EventHandler private void onJoin(GameJoinedEvent event) { clearAll(); }
    @EventHandler private void onLeave(GameLeftEvent event) { clearAll(); }

    private void clearAll() {
        stop("Session cleared. Mark the bed, storage and destination again.");
        sourceBed = null;
        sourceStorage = sourceEnder = destinationStorage = destinationEnder = chamber = null;
        destination = null;
        sourceCorner1=sourceCorner2=destinationCorner1=destinationCorner2=null;
        verifiedTrials = 0;
    }

    private void stop(String reason) { run = null; status = reason; }
    private void invalidate() { verifiedTrials = 0; stop("Route changed. Repeat the two empty trials."); }

    private boolean available() {
        if (!isActive() || mc.player == null || mc.level == null || mc.gameMode == null || !mc.player.isAlive()) {
            status = "Enable StashMover and join a world while alive first.";
            return false;
        }
        return true;
    }

    private void markBed() {
        if (!available() || !(mc.hitResult instanceof BlockHitResult hit)) return;
        var state = mc.level.getBlockState(hit.getBlockPos());
        if (!mc.level.dimension().equals(Level.OVERWORLD) || !(state.getBlock() instanceof BedBlock)) {
            status = "Look at the source Overworld bed. Set your spawn there yourself before testing.";
            return;
        }
        invalidate();
        sourceBed = state.getValue(BedBlock.PART) == BedPart.HEAD ? hit.getBlockPos()
            : hit.getBlockPos().relative(state.getValue(BedBlock.FACING));
        status = "Source bed marked. Set and test its spawn; marking does not set it.";
    }

    private void markContainer(boolean source, boolean ender) {
        if (!available() || !(mc.hitResult instanceof BlockHitResult hit)) return;
        BlockPos pos = hit.getBlockPos();
        var entity = mc.level.getBlockEntity(pos);
        boolean valid = ender ? mc.level.getBlockState(pos).is(Blocks.ENDER_CHEST)
            : entity instanceof ChestBlockEntity || entity instanceof BarrelBlockEntity;
        if (!valid || source && !mc.level.dimension().equals(Level.OVERWORLD)) {
            status = ender ? "Look at an Ender Chest at the selected station." : "Look at a chest or barrel at the selected station; source must be Overworld.";
            return;
        }
        invalidate();
        StationBlock target = new StationBlock(mc.level.dimension(), pos.immutable());
        if (source && ender) sourceEnder = target;
        else if (source) sourceStorage = target;
        else if (ender) destinationEnder = target;
        else destinationStorage = target;
        status = "Station container marked. Keep it reachable from the arrival/respawn position.";
    }

    private void markDestination() {
        if (!available()) return;
        invalidate();
        destination = new ThrowPose(mc.level.dimension(), mc.player.position(), mc.player.getYRot(), mc.player.getXRot());
        status = "Destination landing and pearl-throw pose marked. Arrange the chamber to return A to this spot.";
    }

    private void markChamber() {
        if (!available() || !(mc.hitResult instanceof BlockHitResult hit)) return;
        invalidate();
        chamber = new StationBlock(mc.level.dimension(), hit.getBlockPos().immutable());
        status = "Pearl region marked around this block. B must still verify the chamber and operate recalls.";
    }

    public void markArea(boolean source, int corner) {
        if(!available()||!(mc.hitResult instanceof BlockHitResult hit))return;
        if(source&&!mc.level.dimension().equals(Level.OVERWORLD)){status="Source areas must be in the Overworld.";return;}
        invalidate();StationBlock mark=new StationBlock(mc.level.dimension(),hit.getBlockPos().immutable());
        if(source&&corner==1)sourceCorner1=mark;else if(source)sourceCorner2=mark;else if(corner==1)destinationCorner1=mark;else destinationCorner2=mark;
        status="Cargo area corner marked. Areas are session-only; enable container-areas after marking both corners at both stations.";
    }

    private boolean validArea(StationBlock first,StationBlock second){
        if(first==null||second==null||!first.dimension.equals(second.dimension))return false;
        long x=Math.abs((long)first.pos.getX()-second.pos.getX())+1,y=Math.abs((long)first.pos.getY()-second.pos.getY())+1,z=Math.abs((long)first.pos.getZ()-second.pos.getZ())+1;
        return x<=64&&y<=64&&z<=64&&x*y*z<=32768;
    }
    private boolean inside(BlockPos pos,StationBlock first,StationBlock second){
        return pos.getX()>=Math.min(first.pos.getX(),second.pos.getX())&&pos.getX()<=Math.max(first.pos.getX(),second.pos.getX())
            &&pos.getY()>=Math.min(first.pos.getY(),second.pos.getY())&&pos.getY()<=Math.max(first.pos.getY(),second.pos.getY())
            &&pos.getZ()>=Math.min(first.pos.getZ(),second.pos.getZ())&&pos.getZ()<=Math.max(first.pos.getZ(),second.pos.getZ());
    }
    private BlockPos containerKey(BlockPos pos){
        var state=mc.level.getBlockState(pos);
        if(state.getBlock() instanceof ChestBlock&&state.getValue(ChestBlock.TYPE)!=ChestType.SINGLE){BlockPos other=pos.relative(ChestBlock.getConnectedDirection(state));if(other.compareTo(pos)<0)return other;}
        return pos;
    }
    private StationBlock nextContainer(boolean source,Set<BlockPos> unavailable){
        StationBlock fixed=source?sourceStorage:destinationStorage;
        if(!containerAreas.get())return unavailable.contains(containerKey(fixed.pos))?null:fixed;
        StationBlock first=source?sourceCorner1:destinationCorner1,second=source?sourceCorner2:destinationCorner2;
        if(!validArea(first,second)||!mc.level.dimension().equals(first.dimension))return null;
        StationBlock best=null;double distance=Double.POSITIVE_INFINITY;int considered=0;
        for(var entity:Utils.blockEntities()){
            if(!(entity instanceof ChestBlockEntity)&&!(entity instanceof BarrelBlockEntity))continue;
            BlockPos pos=entity.getBlockPos();
            if(!inside(pos,first,second)||unavailable.contains(containerKey(pos))||!mc.player.isWithinBlockInteractionRange(pos,0))continue;
            if(++considered>256)break;
            double current=mc.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos));
            if(current<distance){best=new StationBlock(mc.level.dimension(),pos.immutable());distance=current;}
        }
        return best;
    }

    private boolean setupValid() {
        if (!available()) return false;
        if (sourceBed == null || sourceStorage == null || destinationStorage == null || destination == null
            || cargoStorage.get() == CargoStorage.EnderChest && (sourceEnder == null || destinationEnder == null)
            || recall.get() == Recall.Stasis && chamber == null) {
            status = "Mark the source bed, station containers, destination pose and (for stasis) pearl region first.";
            return false;
        }
        if (!destinationStorage.dimension.equals(destination.dimension)
            || destinationEnder != null && !destinationEnder.dimension.equals(destination.dimension)
            || chamber != null && !chamber.dimension.equals(destination.dimension)
            || position(Level.OVERWORLD, Vec3.atCenterOf(sourceBed)).within(position(destination.dimension, destination.position), 32)) {
            status = "Source and destination must be distinct; destination marks must share a dimension.";
            return false;
        }
        if (sourceStorage.pos.distSqr(sourceBed) > 64 || sourceEnder != null && sourceEnder.pos.distSqr(sourceBed) > 64) {
            status = "Place the source containers within 8 blocks of the bed and within actual reach of its respawn space.";
            return false;
        }
        if (recall.get() == Recall.Tpa && !operator.get().matches("[A-Za-z0-9_]{1,16}")) {
            status = "Enter B's exact Java username for Tpa mode.";
            return false;
        }
        if(containerAreas.get()&&(!validArea(sourceCorner1,sourceCorner2)||!validArea(destinationCorner1,destinationCorner2)
            ||!sourceCorner1.dimension.equals(Level.OVERWORLD)||!destinationCorner1.dimension.equals(destination.dimension))){
            status="Mark valid source and destination cargo areas (at most 32,768 blocks each) in their station dimensions.";return false;
        }
        return true;
    }

    private void startTrials() {
        if (!setupValid()) return;
        if (!atDestination() || !emptyForDeath()) {
            status = "Start the trials empty at the captured destination pose: clear inventory, equipment, crafting and cursor.";
            return;
        }
        verifiedTrials = 0;
        run = new Run(true, mc.player.getUUID(), 0);
        beginReturn();
    }

    private void startShipments() {
        if (!setupValid()) return;
        if (verifiedTrials < 2 || !atSource() || !bedIntact() || !emptyForDeath()) {
            status = "Complete the two empty trials, then start empty beside the intact source bed.";
            return;
        }
        run = new Run(false, mc.player.getUUID(), shipments.get());
        advance(Phase.Load, "Loading sealed shulkers from source storage.");
    }

    private void advance(Phase phase, String message) {
        run.phase = phase;
        run.enteredNanos = System.nanoTime();
        run.transfer = null;
        if(phase==Phase.Load)run.sourceChoice=null;
        if(phase==Phase.Unload)run.destinationChoice=null;
        status = message;
    }

    @EventHandler private void onPacket(PacketEvent.Receive event) {
        var connection = event.connection;
        if (event.packet instanceof ClientboundRespawnPacket) {
            mc.execute(() -> {
                if (mc.getConnection() != null && mc.getConnection().getConnection() == connection && isActive() && run != null && run.deathObserved
                    && (run.phase == Phase.AwaitDeath || run.phase == Phase.AwaitRespawn)) run.respawnObserved = true;
            });
        }
        if (event.packet instanceof ClientboundPlayerCombatKillPacket death) {
            mc.execute(() -> {
                if (mc.getConnection() != null && mc.getConnection().getConnection() == connection && isActive() && run != null && run.phase == Phase.AwaitDeath
                    && run.deadPlayer != null && death.playerId() == run.deadPlayer.getId()) run.deathObserved = true;
            });
        }
        if (!(event.packet instanceof ClientboundContainerSetContentPacket packet)) return;
        mc.execute(() -> {
            if (mc.getConnection() != null && mc.getConnection().getConnection() == connection && isActive() && run != null && run.transfer != null) run.transfer.receive(packet);
        });
    }

    @EventHandler private void onTick(TickEvent.Post event) {
        if (run == null) return;
        long timeout = TimeUnit.SECONDS.toNanos(run.phase == Phase.AwaitRecall ? recallTimeout.get() : 45);
        if (System.nanoTime() - run.enteredNanos > timeout) {
            stop("Step timed out. Recall, transfer, death or bed return was not confirmed; inspect the route before restarting.");
            return;
        }
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (!run.playerId.equals(mc.player.getUUID())) { stop("Player changed. Automation stopped."); return; }
        if (!mc.player.isAlive() && run.phase != Phase.AwaitDeath && run.phase != Phase.AwaitRespawn) {
            stop("Unexpected death. Automation stopped; cargo and the chamber need review.");
            return;
        }

        switch (run.phase) {
            case Load -> load();
            case StoreInEnder -> storeInEnder();
            case PrepareRecall -> prepareRecall();
            case AwaitRecall -> awaitRecall();
            case CollectFromEnder -> collectFromEnder();
            case Unload -> unload();
            case TakePearls -> takePearls();
            case ThrowPearl -> throwPearl();
            case AwaitPearl -> awaitPearl();
            case StoreRemainder -> storeRemainder();
            case ReturnEmpty -> returnEmpty();
            case AwaitDeath -> awaitDeath();
            case AwaitRespawn -> awaitRespawn();
        }
    }

    private CourierTransfer transfer(StationBlock target, boolean ender, CourierTransfer.Mode mode,
                                     List<ItemStack> manifest, List<ItemStack> required) {
        if (target == null || !mc.level.dimension().equals(target.dimension)) {
            stop("Not in the configured container's dimension.");
            return null;
        }
        if (run.transfer == null) run.transfer = new CourierTransfer(mc, target.pos, ender, mode,
            manifest, required, shulkers.get(), TimeUnit.SECONDS.toNanos(30));
        CourierTransfer operation = run.transfer;
        CourierTransfer.State state = operation.tick();
        if (state == CourierTransfer.State.Failed) { stop(operation.error()); return null; }
        return state == CourierTransfer.State.Done ? operation : null;
    }

    private void load() {
        if (!atSource() || !bedIntact() || !equipmentEmpty() || !craftingEmpty()) { stop("Source bed or empty equipment/crafting check failed."); return; }
        if(run.sourceChoice==null)run.sourceChoice=nextContainer(true,run.emptySources);
        if(run.sourceChoice==null){finishLoading();return;}
        CourierTransfer operation = transfer(run.sourceChoice, false, CourierTransfer.Mode.LoadCargo, List.of(), null);
        if (operation == null) return;
        run.cargo = CourierCargo.nonEmpty(operation.playerAfter());
        if (run.cargo.stream().anyMatch(stack -> !CourierCargo.isCargo(stack))) { stop("Unexpected carried item. Review the shipment."); return; }
        if(operation.containerAfter().stream().noneMatch(CourierCargo::isCargo))run.emptySources.add(containerKey(run.sourceChoice.pos));
        if(containerAreas.get()&&run.cargo.size()<shulkers.get()&&run.emptySources.size()<256){
            run.transfer=null;run.sourceChoice=nextContainer(true,run.emptySources);run.enteredNanos=System.nanoTime();
            if(run.sourceChoice!=null)return;
        }
        finishLoading();
    }
    private void finishLoading(){
        if(run.cargo.isEmpty()){stop("No filled shulkers in the received, reachable source containers. Stopped at source.");return;}
        if(!CourierCargo.sameContents(mainInventory(),run.cargo)){stop("Carried shipment changed during loading.");return;}
        advance(cargoStorage.get() == CargoStorage.EnderChest ? Phase.StoreInEnder : Phase.PrepareRecall,
            "Source cargo loaded and verified from the server.");
    }

    private void storeInEnder() {
        if (!atSource()) { stop("Left the source during Ender Chest packing."); return; }
        if (run.transfer == null && !CourierCargo.sameContents(mainInventory(), run.cargo)) {
            stop("Carried shipment changed before Ender Chest packing."); return;
        }
        CourierTransfer operation = transfer(sourceEnder, true, CourierTransfer.Mode.DepositAll, List.of(), null);
        if (operation == null) return;
        if (!CourierCargo.nonEmpty(operation.playerAfter()).isEmpty()
            || !CourierCargo.sameContents(operation.containerAfter(), CourierCargo.combined(operation.containerBefore(), run.cargo))) {
            stop("Ender Chest shipment could not be verified."); return;
        }
        run.enderBaseline = operation.containerBefore();
        advance(Phase.PrepareRecall, "Shipment verified inside A's Ender Chest.");
    }

    private void prepareRecall() {
        if (!atSource() || !bedIntact() || !equipmentEmpty() || !craftingEmpty() || !cursorEmpty()) {
            stop("Source readiness check failed. B must not trigger yet."); return;
        }
        List<ItemStack> expected = cargoStorage.get() == CargoStorage.Carried && !run.testing ? run.cargo : List.of();
        if (!CourierCargo.sameContents(mainInventory(), expected)) { stop("Carried items changed before recall."); return; }
        if (recall.get() == Recall.Tpa) mc.player.connection.sendCommand("tpa " + operator.get());
        advance(Phase.AwaitRecall, run.testing ? "EMPTY TRIAL: ready for B to recall A. Waiting for actual destination arrival."
            : "CARGO READY: B may recall A now. Waiting for actual destination arrival.");
    }

    private void awaitRecall() {
        if (!atDestination()) return;
        if (!equipmentEmpty() || !craftingEmpty() || !cursorEmpty()) { stop("Unexpected equipment or cursor after recall."); return; }
        if (run.testing) {
            if (!mainInventory().isEmpty()) { stop("Empty trial arrived carrying items. Store them and repeat the trials."); return; }
            verifiedTrials++;
            beginReturn();
        } else if (cargoStorage.get() == CargoStorage.EnderChest) {
            if (!mainInventory().isEmpty()) { stop("Unexpected carried items after recall."); return; }
            advance(Phase.CollectFromEnder, "Collecting only this shipment from A's destination Ender Chest.");
        } else {
            if (!CourierCargo.sameContents(mainInventory(), run.cargo)) { stop("Carried shipment changed in transit."); return; }
            advance(Phase.Unload, "Unloading the shipment into destination storage.");
        }
    }

    private void collectFromEnder() {
        if (!atDestination()) { stop("Left the destination during collection."); return; }
        CourierTransfer operation = transfer(destinationEnder, true, CourierTransfer.Mode.WithdrawCargo, run.cargo,
            CourierCargo.combined(run.enderBaseline, run.cargo));
        if (operation == null) return;
        if (!CourierCargo.sameContents(operation.playerAfter(), run.cargo)
            || !CourierCargo.sameContents(operation.containerAfter(), run.enderBaseline)) {
            stop("Ender Chest withdrawal differs from the shipment manifest. Review both inventories."); return;
        }
        advance(Phase.Unload, "Ender Chest reserves preserved; unloading this shipment.");
    }

    private void unload() {
        if (!atDestination() || !CourierCargo.sameContents(mainInventory(), run.cargo) && run.transfer == null) {
            stop("Destination or shipment check failed before unloading."); return;
        }
        if(run.destinationChoice==null)run.destinationChoice=nextContainer(false,run.fullDestinations);
        if(run.destinationChoice==null){stop("No received, reachable destination container has confirmed capacity. Cargo remains with A; no death command sent.");return;}
        CourierTransfer operation = transfer(run.destinationChoice, false, containerAreas.get()?CourierTransfer.Mode.DepositUntilFull:CourierTransfer.Mode.DepositAll, List.of(), null);
        if (operation == null) return;
        List<ItemStack> remainder=CourierCargo.nonEmpty(operation.playerAfter());
        List<ItemStack> deposited;
        try{deposited=CourierCargo.subtract(run.cargo,remainder);}catch(IllegalArgumentException error){stop("Shipment changed during unloading; review inventory and storage.");return;}
        if (!CourierCargo.sameContents(operation.containerAfter(), CourierCargo.combined(operation.containerBefore(), deposited))) {
            stop("Destination receipt not verified. No death command will be sent."); return;
        }
        run.cargo = remainder;
        if(!remainder.isEmpty()){
            if(!containerAreas.get()||run.fullDestinations.size()>=256){stop("Destination capacity exhausted. Remaining cargo stays with A.");return;}
            run.fullDestinations.add(containerKey(run.destinationChoice.pos));run.destinationChoice=null;run.transfer=null;run.enteredNanos=System.nanoTime();return;
        }
        run.delivered++;
        beginReturn();
    }

    private void beginReturn() {
        run.pearl = null;
        run.priorPearls.clear();
        advance(recall.get() == Recall.Stasis ? Phase.TakePearls : Phase.StoreRemainder,
            "Shipment stored. Preparing the next recall before the empty bed return.");
    }

    private void takePearls() {
        if (!atDestination() || !equipmentEmpty() || !craftingEmpty()) { stop("Destination or equipment check failed before rearming."); return; }
        CourierTransfer operation = transfer(destinationStorage, false, CourierTransfer.Mode.TakePearls, List.of(), null);
        if (operation == null) return;
        List<ItemStack> carried = CourierCargo.nonEmpty(operation.playerAfter());
        if (carried.isEmpty() || carried.stream().anyMatch(stack -> !stack.is(Items.ENDER_PEARL))) {
            stop("Destination storage needs pearls, and the courier must carry nothing else before rearming."); return;
        }
        advance(Phase.ThrowPearl, "Throwing A's fresh pearl from the captured pose.");
    }

    private void throwPearl() {
        if (!atDestination() || mc.player.position().distanceToSqr(destination.position) > 2.25) {
            stop("A must land within 1.5 blocks of the captured pearl-throw position. Adjust the station and retest."); return;
        }
        int slot = -1;
        for (int i = 0; i < 9; i++) if (mc.player.getInventory().getItem(i).is(Items.ENDER_PEARL)) { slot = i; break; }
        if (slot < 0 || !equipmentEmpty() || !craftingEmpty() || !cursorEmpty()) { stop("A pearl must be in the hotbar; clear equipment, crafting and cursor."); return; }
        for (var entity : mc.level.entitiesForRendering()) {
            if (entity instanceof ThrownEnderpearl pearl && pearl.getOwner() != null
                && pearl.getOwner().getUUID().equals(mc.player.getUUID())) {
                if (run.priorPearls.size() >= 256) { stop("Too many owned pearls loaded to identify a new throw reliably."); return; }
                run.priorPearls.add(pearl.getUUID());
            }
        }
        if (mc.player.getCooldowns().isOnCooldown(mc.player.getInventory().getItem(slot))) return;
        advance(Phase.AwaitPearl, "Waiting for A's new pearl to be observed in the marked chamber region.");
        int previousSlot = mc.player.getInventory().getSelectedSlot();
        float previousYaw = mc.player.getYRot(), previousPitch = mc.player.getXRot();
        mc.player.getInventory().setSelectedSlot(slot);
        // This version's use-item packet carries yaw/pitch; no queued rotation or later callback is needed.
        mc.player.setYRot(destination.yaw);
        mc.player.setXRot(destination.pitch);
        try { mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND); }
        finally {
            mc.player.getInventory().setSelectedSlot(previousSlot);
            mc.player.setYRot(previousYaw);
            mc.player.setXRot(previousPitch);
        }
    }

    private void awaitPearl() {
        if (!atDestination()) { stop("Left the destination while checking the new pearl."); return; }
        for (var entity : mc.level.entitiesForRendering()) {
            if (entity instanceof ThrownEnderpearl pearl && !run.priorPearls.contains(pearl.getUUID()) && validPearl(pearl)
                && pearl.tickCount >= 20) {
                run.pearl = pearl.getUUID();
                advance(Phase.StoreRemainder, "New owned pearl observed in the region. Storing remaining pearls before /kill.");
                return;
            }
        }
    }

    private boolean validPearl(ThrownEnderpearl pearl) {
        return chamber != null && mc.level.dimension().equals(chamber.dimension) && pearl.isAlive()
            && pearl.getOwner() != null && pearl.getOwner().getUUID().equals(mc.player.getUUID())
            && pearl.position().distanceToSqr(Vec3.atCenterOf(chamber.pos)) <= chamberRadius.get() * chamberRadius.get();
    }

    private boolean pearlPresent() {
        if (recall.get() == Recall.Tpa) return true;
        if (run.pearl == null) return false;
        for (var entity : mc.level.entitiesForRendering()) {
            if (entity instanceof ThrownEnderpearl pearl && run.pearl.equals(pearl.getUUID())) return validPearl(pearl);
        }
        return false;
    }

    private void storeRemainder() {
        if (!atDestination() || !pearlPresent()) { stop("The rearmed pearl is no longer observed. No death command sent."); return; }
        CourierTransfer operation = transfer(destinationStorage, false, CourierTransfer.Mode.DepositAll, List.of(), null);
        if (operation == null) return;
        if (!CourierCargo.nonEmpty(operation.playerAfter()).isEmpty()) { stop("Remaining carried items were not stored."); return; }
        advance(Phase.ReturnEmpty, "Server confirmed carried inventory empty. Checking the final /kill gate.");
    }

    private ReturnCheck returnCheck() {
        return new ReturnCheck(mc.player != null && mc.player.isAlive(), atDestination(), run.cargo.isEmpty(),
            pearlPresent(), mainInventory().isEmpty(), equipmentEmpty(), craftingEmpty(), cursorEmpty(),
            mc.player.containerMenu == mc.player.inventoryMenu);
    }

    private void returnEmpty() {
        if (!returnCheck().mayDie()) { stop("Empty-return check failed. Store all items and verify the chamber; no /kill sent."); return; }
        run.deadPlayer = mc.player;
        run.deathObserved = false;
        run.respawnRequested = run.respawnObserved = false;
        advance(Phase.AwaitDeath, "Sent /kill once while empty. Waiting for observed death.");
        // Recheck at the command boundary after all transfer observations.
        if (!returnCheck().mayDie()) { stop("Return conditions changed; no /kill sent."); return; }
        mc.player.connection.sendCommand("kill");
    }

    private void awaitDeath() {
        if (run.deathObserved || mc.player == run.deadPlayer && !mc.player.isAlive()) {
            run.deathObserved = true;
            advance(Phase.AwaitRespawn, "Death observed. Respawning and checking the source bed location.");
        }
    }

    private void awaitRespawn() {
        if (!mc.player.isAlive()) {
            if (!run.respawnRequested) { run.respawnRequested = true; mc.player.respawn(); }
            return;
        }
        if (!run.respawnObserved || !atSource()) return;
        if (!bedIntact() || !emptyForDeath()) { stop("Respawn did not yield an empty courier beside the intact source bed."); return; }
        if (run.testing && verifiedTrials >= 2) {
            stop("Two empty recalls passed and A returned to source. B must wait; cargo runs may now be started.");
        } else if (!run.testing && run.delivered >= run.deliveryLimit) {
            stop("Delivery limit reached. A returned empty to the source; B must wait.");
        } else {
            advance(run.testing ? Phase.PrepareRecall : Phase.Load,
                run.testing ? "Empty respawn confirmed. Preparing the next empty recall trial." : "Empty bed respawn confirmed. Loading the next shipment.");
        }
    }

    private boolean atSource() {
        return mc.player != null && mc.level != null && sourceBed != null && mc.level.dimension().equals(Level.OVERWORLD)
            && mc.player.position().distanceToSqr(Vec3.atCenterOf(sourceBed)) <= 64;
    }

    private boolean atDestination() {
        return mc.player != null && mc.level != null && destination != null && mc.level.dimension().equals(destination.dimension)
            && mc.player.position().distanceToSqr(destination.position) <= 16;
    }

    private boolean bedIntact() {
        if (!atSource() || !mc.level.hasChunkAt(sourceBed)) return false;
        var head = mc.level.getBlockState(sourceBed);
        if (!(head.getBlock() instanceof BedBlock) || head.getValue(BedBlock.PART) != BedPart.HEAD) return false;
        var foot = mc.level.getBlockState(sourceBed.relative(head.getValue(BedBlock.FACING).getOpposite()));
        return foot.is(head.getBlock()) && foot.getValue(BedBlock.PART) == BedPart.FOOT
            && foot.getValue(BedBlock.FACING) == head.getValue(BedBlock.FACING);
    }

    private List<ItemStack> mainInventory() {
        List<ItemStack> stacks = new ArrayList<>();
        if (mc.player != null) for (int i = 0; i < 36; i++) if (!mc.player.getInventory().getItem(i).isEmpty()) stacks.add(mc.player.getInventory().getItem(i).copy());
        return stacks;
    }

    private boolean equipmentEmpty() {
        if (mc.player == null) return false;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot == EquipmentSlot.MAINHAND) continue;
            if (!mc.player.getItemBySlot(slot).isEmpty()) return false;
        }
        return true;
    }

    private boolean craftingEmpty() {
        if (mc.player == null) return false;
        for (int slot = InventoryMenu.CRAFT_SLOT_START; slot < InventoryMenu.CRAFT_SLOT_END; slot++) {
            if (mc.player.inventoryMenu.getSlot(slot).hasItem()) return false;
        }
        return true;
    }

    private boolean cursorEmpty() {
        return mc.player != null && mc.player.containerMenu.getCarried().isEmpty() && mc.player.inventoryMenu.getCarried().isEmpty();
    }

    private boolean emptyForDeath() { return mainInventory().isEmpty() && equipmentEmpty() && craftingEmpty() && cursorEmpty(); }
    private static Position position(ResourceKey<Level> dimension, Vec3 pos) { return new Position(dimension.toString(), pos.x, pos.y, pos.z); }

    @Override public String getInfoString() {
        if (run == null) return "stopped";
        if (run.phase == Phase.AwaitRecall) return run.testing ? "B: recall EMPTY trial" : "B: recall CARGO";
        return run.phase.toString();
    }
    @Override public WWidget getWidget(GuiTheme theme) { return new CourierWidget(theme); }

    private final class CourierWidget extends WVerticalList {
        private final WLabel setupLabel, progressLabel, statusLabel;

        private CourierWidget(GuiTheme theme) {
            this.theme = theme;
            setupLabel = add(theme.label("", 440)).widget();
            progressLabel = add(theme.label("", 440)).widget();
            statusLabel = add(theme.label(status, 440)).widget();
            var source = add(theme.section("Source setup (look at each block)", false)).widget();
            source.add(theme.button("Mark source Overworld bed")).widget().action = StasisCourier.this::markBed;
            source.add(theme.button("Mark source chest / barrel")).widget().action = () -> markContainer(true, false);
            source.add(theme.button("Mark source Ender Chest")).widget().action = () -> markContainer(true, true);
            source.add(theme.button("Mark source cargo area corner 1")).widget().action=()->markArea(true,1);
            source.add(theme.button("Mark source cargo area corner 2")).widget().action=()->markArea(true,2);
            var dest = add(theme.section("Destination setup", false)).widget();
            dest.add(theme.button("Capture landing position and pearl-throw aim")).widget().action = StasisCourier.this::markDestination;
            dest.add(theme.button("Mark destination chest / barrel")).widget().action = () -> markContainer(false, false);
            dest.add(theme.button("Mark destination Ender Chest")).widget().action = () -> markContainer(false, true);
            dest.add(theme.button("Mark block at the waiting pearl's region")).widget().action = StasisCourier.this::markChamber;
            dest.add(theme.button("Mark destination cargo area corner 1")).widget().action=()->markArea(false,1);
            dest.add(theme.button("Mark destination cargo area corner 2")).widget().action=()->markArea(false,2);
            add(theme.button("Start two EMPTY trials from destination")).widget().action = StasisCourier.this::startTrials;
            add(theme.button("Start cargo run from source")).widget().action = StasisCourier.this::startShipments;
            add(theme.button("STOP automation (keep session setup)")).widget().action = () -> stop("Stopped. Review both inventories and the chamber before restarting; no rollback is performed.");
            add(theme.button("Clear all session setup and test results")).widget().action = StasisCourier.this::clearAll;
            var notes = add(theme.section("Station requirements", false)).widget();
            notes.add(theme.label("Set the bed spawn yourself. Keep its respawn spaces clear. Only A runs this module; B manually recalls A when the status says ready.", 440));
            notes.add(theme.label("Use reachable chests/barrels and matching Ender Chests. Cargo is sealed shulker boxes; keep equipment, offhand and crafting empty. Put spare pearls in destination storage.", 440));
            notes.add(theme.label("Capture the destination pose while aiming a pearl into the chamber. It must return A to that throw spot. Mark the region where the pearl waits.", 440));
            notes.add(theme.label("Trials use /kill and respawn automatically. Default vanilla pearls vanish on death; a visible pearl is not proof of a usable recall. No server compatibility is assumed.", 440));
        }

        private void update(WLabel label, String text) { if (!label.get().equals(text)) label.set(text); }

        @Override public boolean render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            update(setupLabel, "Source: bed " + marked(sourceBed) + ", storage " + marked(sourceStorage)
                + (cargoStorage.get() == CargoStorage.EnderChest ? ", Ender Chest " + marked(sourceEnder) : "")
                + ". Destination: landing/aim " + marked(destination) + ", storage " + marked(destinationStorage)
                + (cargoStorage.get() == CargoStorage.EnderChest ? ", Ender Chest " + marked(destinationEnder) : "")
                + (recall.get() == Recall.Stasis ? ", pearl region " + marked(chamber) : "") + ".");
            update(progressLabel, "Empty recall trials: " + verifiedTrials + "/2; " + (run == null ? "stopped" : "step " + run.phase + ", deliveries " + run.delivered));
            update(statusLabel, status);
            return super.render(renderer, mouseX, mouseY, delta);
        }
    }

    private static String marked(Object value) { return value == null ? "missing" : "set"; }

    @EventHandler private void renderAreas(Render3DEvent event){
        if(!renderAreas.get()||mc.level==null)return;
        renderArea(event,sourceCorner1,sourceCorner2,new Color(80,150,255));renderArea(event,destinationCorner1,destinationCorner2,new Color(180,100,255));
    }
    private void renderArea(Render3DEvent event,StationBlock first,StationBlock second,Color color){
        if(!validArea(first,second)||!mc.level.dimension().equals(first.dimension))return;
        AABB area=new AABB(Math.min(first.pos.getX(),second.pos.getX()),Math.min(first.pos.getY(),second.pos.getY()),Math.min(first.pos.getZ(),second.pos.getZ()),
            Math.max(first.pos.getX(),second.pos.getX())+1,Math.max(first.pos.getY(),second.pos.getY())+1,Math.max(first.pos.getZ(),second.pos.getZ())+1);
        event.renderer.box(area,new Color(color.r,color.g,color.b,12),color,ShapeMode.Lines,0);
    }

    private record StationBlock(ResourceKey<Level> dimension, BlockPos pos) {}
    private record ThrowPose(ResourceKey<Level> dimension, Vec3 position, float yaw, float pitch) {}

    private static final class Run {
        private final boolean testing;
        private final UUID playerId;
        private final int deliveryLimit;
        private Phase phase;
        private long enteredNanos;
        private int delivered;
        private CourierTransfer transfer;
        private List<ItemStack> cargo = List.of(), enderBaseline = List.of();
        private final Set<UUID> priorPearls = new HashSet<>();
        private UUID pearl;
        private LocalPlayer deadPlayer;
        private boolean deathObserved;
        private boolean respawnRequested, respawnObserved;
        private StationBlock sourceChoice,destinationChoice;
        private final Set<BlockPos> emptySources=new HashSet<>(),fullDestinations=new HashSet<>();

        private Run(boolean testing, UUID playerId, int deliveryLimit) {
            this.testing = testing;
            this.playerId = playerId;
            this.deliveryLimit = deliveryLimit;
        }
    }
}
