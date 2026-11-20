package addon.fmod.modules;

import addon.fmod.FMod;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Horizontal speed with a vanilla-26.3 packet bypass.
 *
 * <p>The server allows a per-tick move distance of ~{@code 100 * movePacketCount}
 * (on distance&sup2;) but resets that count to 1 once it passes 5, so the usable
 * ceiling is five move packets per tick: four position-less {@code StatusOnly}
 * packets plus the client's own position packet &rarr; ~22 blocks/tick. This module
 * pads up to {@value #MAX_PAD} {@code StatusOnly} packets so externally-sourced speed
 * (or the optional boost) is accepted, and clamps the boost below that ceiling. It
 * does not touch timer, onGround or collision state.
 */
public class SpeedBypass extends Module {
    private static final int MAX_PAD = 4;         // 4 StatusOnly + 1 vanilla Pos = 5 (the allowance sweet spot)
    private static final int MAX_PACKETS = MAX_PAD + 1;
    private static final double SAFETY = 0.92;    // stay just under the computed ceiling
    // Server per-packet speed allowance (distance^2): 300 while fall-flying (elytra), else 100.
    // With 5 packets the ceiling is ~38 blocks/tick on elytra, ~22 otherwise.

    public enum Mode { Static, Dynamic }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Dynamic pads packets from your actual speed; Static always pads a fixed amount.")
        .defaultValue(Mode.Dynamic)
        .build()
    );

    private final Setting<Integer> packets = sgGeneral.add(new IntSetting.Builder()
        .name("packets")
        .description("StatusOnly packets to pad per tick (Static mode).")
        .defaultValue(MAX_PAD)
        .range(1, MAX_PAD)
        .sliderRange(1, MAX_PAD)
        .visible(() -> mode.get() == Mode.Static)
        .build()
    );

    private final Setting<Double> boost = sgGeneral.add(new DoubleSetting.Builder()
        .name("boost")
        .description("Extra horizontal blocks/tick added in your movement direction, clamped under the server ceiling (~22 walking, ~38 elytra). 0 = bypass only.")
        .defaultValue(0)
        .min(0)
        .sliderRange(0, 38)
        .build()
    );

    private final Setting<Boolean> onlyWhenMoving = sgGeneral.add(new BoolSetting.Builder()
        .name("only-when-moving")
        .description("Only act when you are moving horizontally.")
        .defaultValue(true)
        .build()
    );

    public SpeedBypass() {
        super(FMod.CATEGORY, "speed-bypass", "Bypasses the vanilla per-tick speed check (capped ~22 blocks/tick on 26.3).");
    }

    // LOWEST so it reads the final movement after flight modules (e.g. VolytraFly) and the
    // chunk guard have set it, and pads the packet allowance to match the real per-tick speed.
    @EventHandler(priority = EventPriority.LOWEST)
    private void onMove(PlayerMoveEvent event) {
        if (mc.player == null || mc.player.isPassenger()) return;

        double perPacket = mc.player.isFallFlying() ? 300.0 : 100.0;
        double maxSpeed = Math.sqrt(perPacket * MAX_PACKETS) * SAFETY;

        double x = event.movement.x;
        double z = event.movement.z;
        double horizontal = Math.sqrt(x * x + z * z);

        if (horizontal == 0) {
            if (onlyWhenMoving.get()) return;
        } else if (boost.get() > 0) {
            double target = Math.min(horizontal + boost.get(), maxSpeed);
            double scale = target / horizontal;
            x *= scale;
            z *= scale;
            ((IVec3) event.movement).meteor$setXZ(x, z);
            horizontal = target;
        }

        int pad = mode.get() == Mode.Static
            ? packets.get()
            : clampPad((int) Math.ceil(horizontal * horizontal / perPacket) - 1);

        boolean onGround = mc.player.onGround();
        boolean hCollision = mc.player.horizontalCollision;
        for (int i = 0; i < pad; i++) {
            mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(onGround, hCollision));
        }
    }

    private static int clampPad(int n) {
        return Math.max(0, Math.min(MAX_PAD, n));
    }
}
