package addon.fmod.utils;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Client-side position-warp helper for vanilla 26.3 servers.
 *
 * <p>Vanilla disconnects a client that sends more than one positioned move packet
 * ({@code Pos}/{@code PosRot}) per client tick ({@code receivedPositionThisTick}),
 * so every warp sends at most one {@code Pos} per tick and this class cancels the
 * client's own end-of-tick position packet while a warp is running.
 *
 * <p>Position-less {@code StatusOnly} packets carry no position and only raise the
 * server's per-tick speed allowance (~100 blocks&sup2; per move packet). The server
 * resets the packet count to 1 once it passes 5, so the usable ceiling is five move
 * packets per tick ({@value #PAD} {@code StatusOnly} + 1 {@code Pos}) &rarr; ~22
 * blocks per tick. Longer warps are split into {@value #MAX_STEP}-block steps, one
 * per tick.
 *
 * <p>Server-side vertical movement is not collision-checked (the vclip quirk), so
 * vertical hops pass through blocks; horizontal movement is collision-checked and
 * snapped back, so horizontal clipping must route up, across open air, then down.
 */
public final class WarpUtils {
    public static final WarpUtils INSTANCE = new WarpUtils();

    private static final int PAD = 4;
    private static final double MAX_STEP = 20.0;

    private final Queue<Vec3> waypoints = new ArrayDeque<>();
    private final Set<ServerboundMovePlayerPacket> owned = Collections.newSetFromMap(new IdentityHashMap<>());

    private boolean queueActive;
    private boolean externalHold;
    private boolean releasePending;

    private WarpUtils() {}

    private boolean active() {
        return queueActive || externalHold;
    }

    public static boolean isWarping() {
        return INSTANCE.active();
    }

    // Low-level packet helpers: every positioned move packet must go through here so
    // it is whitelisted against the suppression in onSendPacket.

    private static void sendOwned(ServerboundMovePlayerPacket packet) {
        if (mc.player == null) return;
        INSTANCE.owned.add(packet);
        mc.player.connection.send(packet);
    }

    public static void sendStatusOnly(int count) {
        if (mc.player == null) return;
        boolean onGround = mc.player.onGround();
        boolean hCollision = mc.player.horizontalCollision;
        for (int i = 0; i < count; i++) {
            sendOwned(new ServerboundMovePlayerPacket.StatusOnly(onGround, hCollision));
        }
    }

    /** Pads to the 5-packet allowance, sends one positioned packet, and mirrors it client-side. */
    public static void sendPos(Vec3 pos) {
        if (mc.player == null) return;
        sendStatusOnly(PAD);
        sendOwned(new ServerboundMovePlayerPacket.Pos(pos.x, pos.y, pos.z, mc.player.onGround(), mc.player.horizontalCollision));
        mc.player.setPos(pos);
    }

    // External hold: for callers that drive their own per-tick packets (e.g. reach),
    // keeping vanilla's position packets suppressed across a multi-tick sequence.

    public static void beginHold() {
        INSTANCE.externalHold = true;
    }

    public static void endHold() {
        INSTANCE.externalHold = false;
    }

    // Queued multi-tick warp: one positioned packet per client tick.

    public static void warp(Vec3 target) {
        if (mc.player == null) return;
        warpPath(List.of(target));
    }

    /** Clips up by {@code clearance} above the higher of start/target, across, then down. */
    public static void warpOver(Vec3 target, double clearance) {
        if (mc.player == null) return;
        Vec3 start = mc.player.position();
        double y = Math.max(start.y, target.y) + clearance;
        warpPath(List.of(
            new Vec3(start.x, y, start.z),
            new Vec3(target.x, y, target.z),
            target
        ));
    }

    private static void warpPath(List<Vec3> legs) {
        if (mc.player == null) return;
        List<Vec3> steps = new ArrayList<>();
        Vec3 from = mc.player.position();
        for (Vec3 leg : legs) {
            int n = Math.max(1, (int) Math.ceil(from.distanceTo(leg) / MAX_STEP));
            for (int i = 1; i <= n; i++) steps.add(from.lerp(leg, (double) i / n));
            from = leg;
        }
        INSTANCE.waypoints.clear();
        INSTANCE.waypoints.addAll(steps);
        INSTANCE.queueActive = true;
        INSTANCE.releasePending = false;
    }

    public static void cancel() {
        INSTANCE.waypoints.clear();
        INSTANCE.queueActive = false;
        INSTANCE.releasePending = false;
    }

    /** Scans vertically for the first empty space past the nearest blocks (autoclip). */
    public static Vec3 getAutoClipPos(int direction) {
        if (mc.player == null || mc.level == null) return null;

        boolean inside = false;
        for (float i = 0; i < 150; i += 0.25f) {
            Vec3 pos = mc.player.position();
            Vec3 targetPos = pos.add(0, direction * i, 0);
            boolean collides = !mc.level.noCollision(mc.player, mc.player.getBoundingBox().move(targetPos.subtract(pos)));

            if (!inside && collides) inside = true;
            else if (inside && !collides) return targetPos;
        }

        return null;
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        if (!queueActive || waypoints.isEmpty()) return;
        sendPos(waypoints.poll());
        if (waypoints.isEmpty()) releasePending = true;
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        if (releasePending) {
            queueActive = false;
            releasePending = false;
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof ServerboundMovePlayerPacket packet)) return;
        if (owned.remove(packet)) return;   // our packet: let it through
        if (active()) event.cancel();        // suppress vanilla's position packet while warping
    }
}
