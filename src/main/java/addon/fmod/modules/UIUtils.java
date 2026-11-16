package addon.fmod.modules;

import addon.fmod.FMod;
import addon.fmod.uiutils.DelayedUiPackets;
import addon.fmod.uiutils.UIUtilsScreen;
import addon.fmod.uiutils.UiPacketFields;
import com.google.gson.JsonElement;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.meteor.KeyInputEvent;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.resources.RegistryOps;
import net.minecraft.network.Connection;
import net.minecraft.network.HashedStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.inventory.ContainerInput;

/** Native implementation of the UI-Utils workflow; see THIRD_PARTY_NOTICES.md. */
public final class UIUtils extends Module {
    public static final int MAX_DELAYED_PACKETS = 512;
    private final SettingGroup general = settings.getDefaultGroup();
    private final Setting<Boolean> showControls = general.add(new BoolSetting.Builder()
        .name("show-controls").description("Show UI Utils and chat controls in inventories, books and signs.").defaultValue(true).build());
    private final Setting<Keybind> restoreKey = general.add(new KeybindSetting.Builder()
        .name("restore-screen").description("Reopen the saved container screen in this connection. May be out of sync with the server.")
        .defaultValue(Keybind.fromKey(InputConstants.KEY_V)).build());
    private final Setting<Boolean> bypassPacks = general.add(new BoolSetting.Builder()
        .name("bypass-resource-pack").description("Acknowledge required server resource packs without downloading them.").defaultValue(false).build());
    private final Setting<Boolean> forceDeny = general.add(new BoolSetting.Builder()
        .name("force-deny").description("Also bypass optional packs when bypass-resource-pack is on.").defaultValue(false).build());
    private final DelayedUiPackets<Packet<?>> packets = new DelayedUiPackets<>(MAX_DELAYED_PACKETS);
    // Only the packet currently being replayed bypasses our interceptor; other modules still see it.
    private final ThreadLocal<Packet<?>> replaying = new ThreadLocal<>();
    private AbstractContainerScreen<?> savedScreen;
    private Connection savedConnection;
    private Screen closingWithoutUpdate;
    private boolean overflowReported;

    public UIUtils() {
        super(FMod.CATEGORY, "ui-utils", "Container debugging, delayed clicks, saved screens and an in-game packet editor.");
        runInMainMenu = true;
    }

    public static UIUtils get() { return Modules.get() == null ? null : Modules.get().get(UIUtils.class); }
    public boolean controlsVisible() { return isActive() && showControls.get(); }
    public boolean sending() { return packets.sending(); }
    public boolean delaying() { return packets.delaying(); }
    public int queued() { return packets.size(); }
    public boolean bypassesPack(boolean required) { return isActive() && bypassPacks.get() && (required || forceDeny.get()); }
    public boolean bypassPacks() { return bypassPacks.get(); }
    public boolean forceDeny() { return forceDeny.get(); }
    public void toggleBypassPacks() { bypassPacks.set(!bypassPacks.get()); }
    public void toggleForceDeny() { forceDeny.set(!forceDeny.get()); }
    public boolean suppressSignUpdate(Screen screen) { return closingWithoutUpdate == screen; }

    private Connection connection() { return mc.getConnection() == null ? null : mc.getConnection().getConnection(); }
    private void reset(Connection connection) {
        packets.reset(connection);
        savedScreen = null;
        savedConnection = null;
        overflowReported = false;
    }
    @Override public void onActivate() { reset(connection()); refreshControls(); }
    @Override public void onDeactivate() {
        reset(null);
        if (mc.gui.screen() instanceof UIUtilsScreen screen) screen.returnToContainer();
        // Meteor marks the module inactive after onDeactivate returns.
        mc.schedule(this::refreshControls);
    }
    @EventHandler private void join(GameJoinedEvent event) { reset(connection()); }
    @EventHandler private void leave(GameLeftEvent event) { reset(null); }

    private void refreshControls() {
        Screen screen = mc.gui.screen();
        if (screen != null) screen.resize(screen.width, screen.height);
    }

    public void toggleControls() { showControls.set(!showControls.get()); refreshControls(); }

    @Override public WWidget getWidget(GuiTheme theme) {
        var list = theme.verticalList();
        list.add(theme.label("Open a container and select UI Utils. Saved screens and queued packets stay in this connection only.", 400));
        list.add(theme.button("Discard delayed packets")).widget().action = this::discard;
        list.add(theme.button("Restore saved GUI")).widget().action = this::restore;
        return list;
    }

    @EventHandler private void key(KeyInputEvent event) {
        if (mc.gui.screen() == null && event.action == KeyAction.Press && restoreKey.get().matches(event.input)) {
            restore();
            event.cancel();
        }
    }

    @EventHandler private void mouse(MouseClickEvent event) {
        if (mc.gui.screen() == null && event.action == KeyAction.Press && restoreKey.get().matches(event.input)) {
            restore();
            event.cancel();
        }
    }

    @EventHandler private void send(PacketEvent.Send event) {
        if (event.isCancelled() || event.packet == replaying.get()) return;
        if (!(event.packet instanceof ServerboundContainerClickPacket) && !(event.packet instanceof ServerboundContainerButtonClickPacket)) return;
        var decision = packets.accept(event.connection, event.packet, true);
        if (decision != DelayedUiPackets.Decision.Send) event.cancel();
        if (decision == DelayedUiPackets.Decision.Full && !overflowReported) {
            overflowReported = true;
            mc.execute(() -> { if (isActive()) warning("Delayed packet limit reached (%d). Further clicks are discarded until flushed or cleared.", MAX_DELAYED_PACKETS); });
        }
    }

    public void toggleSending() {
        packets.setSending(!sending());
        overflowReported = false;
    }
    public String toggleDelaying() {
        boolean delay = !delaying();
        packets.setDelaying(delay);
        return delay ? "New clicks will be queued." : flushStatus();
    }
    public void discard() { packets.discard(); overflowReported = false; }

    private void sendDirect(Connection connection, Packet<?> packet) {
        if (!isActive() || connection != connection() || !connection.isConnected()) throw new IllegalStateException("Connection ended.");
        Packet<?> previous = replaying.get();
        replaying.set(packet);
        try { connection.send(packet); }
        finally { if (previous == null) replaying.remove(); else replaying.set(previous); }
    }

    public int flush() {
        Connection current = connection();
        try {
            int count = packets.flush(current, packet -> sendDirect(current, packet));
            overflowReported = false;
            return count;
        } catch (RuntimeException error) {
            error("Packet replay stopped; remaining packets were discarded. Some packets may already have been sent.");
            return -1;
        }
    }

    public String flushStatus() {
        int count = flush();
        return count < 0 ? "Replay failed; some packets may already have been sent." : "Submitted " + count + " delayed packets.";
    }

    public void open(AbstractContainerScreen<?> parent) {
        if (isActive() && mc.player != null && parent.getMenu() == mc.player.containerMenu) mc.gui.setScreen(new UIUtilsScreen(this, parent));
    }

    public void save(AbstractContainerScreen<?> screen) {
        if (!isActive() || connection() == null) return;
        savedScreen = screen;
        savedConnection = connection();
        info("Saved GUI for this connection. Restore with %s; the server may no longer have this menu open.", restoreKey.get());
    }

    public void restore() {
        if (!isActive() || mc.player == null || savedScreen == null || savedConnection != connection()) {
            warning("No saved GUI in this connection.");
            return;
        }
        mc.player.containerMenu = savedScreen.getMenu();
        mc.gui.setScreen(savedScreen);
    }

    public void closeWithoutPacket(Screen screen) {
        closingWithoutUpdate = screen;
        try { mc.gui.setScreen(null); }
        finally { closingWithoutUpdate = null; }
    }

    public void desync(AbstractContainerScreen<?> screen) {
        if (isActive() && mc.player != null && connection() != null) mc.getConnection().send(new ServerboundContainerClosePacket(screen.getMenu().containerId));
    }

    public void disconnect(boolean flush) {
        if (flush) flush();
        Connection current = connection();
        if (mc.gui.screen() instanceof AbstractSignEditScreen screen) closeWithoutPacket(screen);
        reset(null);
        if (current != null) current.disconnect(Component.literal("Disconnected by FMod UI Utils"));
    }

    public String copyTitle(Screen screen) {
        if (mc.getConnection() == null) throw new IllegalStateException("Connection ended.");
        var ops = RegistryOps.create(JsonOps.INSTANCE, mc.getConnection().registryAccess());
        JsonElement json = ComponentSerialization.CODEC.encodeStart(ops, screen.getTitle()).getOrThrow();
        mc.keyboardHandler.setClipboard(json.toString());
        return "GUI title JSON copied.";
    }

    public void chat(String text) {
        if (mc.getConnection() == null || text.isBlank()) return;
        if (text.startsWith("/")) mc.getConnection().sendCommand(text.substring(1));
        else if (text.equals("^toggleuiutils")) toggleControls();
        else mc.getConnection().sendChat(text);
    }

    /** Fabricated clicks deliberately omit client slot prediction and use the current carried stack hash. */
    public String fabricate(boolean click, UiPacketFields fields, ContainerInput action, boolean respectDelay) {
        Connection current = connection();
        if (!isActive() || mc.player == null || current == null) return "No active connection.";
        Packet<?> packet = click
            ? new ServerboundContainerClickPacket(fields.container(), fields.revision(), fields.slot(), (byte) fields.button(), action,
                new Int2ObjectOpenHashMap<>(), HashedStack.create(mc.player.containerMenu.getCarried(), mc.getConnection().decoratedHashOpsGenenerator()))
            : new ServerboundContainerButtonClickPacket(fields.container(), fields.button());
        int sent = 0, queued = 0;
        for (int i = 0; i < fields.repeats(); i++) {
            switch (packets.accept(current, packet, respectDelay)) {
                case Send -> { sendDirect(current, packet); sent++; }
                case Queued -> queued++;
                case Block -> { return "Sending is off. No packets submitted."; }
                case Full -> { return "Queue full: " + sent + " sent, " + queued + " queued; remaining repeats discarded."; }
                case Stale -> { return "Connection changed. Remaining repeats discarded."; }
            }
        }
        return sent + " sent, " + queued + " queued. Server acceptance is not confirmed.";
    }
}
