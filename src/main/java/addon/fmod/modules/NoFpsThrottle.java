package addon.fmod.modules;

import addon.fmod.FMod;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;

/**
 * Replaces vanilla's minimized/AFK frame-rate throttle (10-30 FPS) with a configurable cap.
 *
 * <p>The client runs at most 10 game ticks per rendered frame, so once load pushes a throttled
 * client to a few FPS the game itself slows below 20 TPS. Automation such as Baritone sends no
 * input, so vanilla's AFK mode treats it as idle.
 */
public class NoFpsThrottle extends Module {
    /** Vanilla treats a limit at or above this value as unlimited. */
    private static final int UNLIMITED = 260;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> limit = sgGeneral.add(new IntSetting.Builder()
        .name("limit")
        .description("Frame-rate cap while minimized or idle. Never exceeds the Max Framerate video setting; 260 uses it unchanged.")
        .defaultValue(UNLIMITED)
        .range(1, UNLIMITED)
        .sliderRange(5, UNLIMITED)
        .build()
    );

    public NoFpsThrottle() {
        super(FMod.CATEGORY, "no-fps-throttle", "Stops Minecraft lowering the frame rate while the window is minimized or idle, so background clients keep ticking at full speed.");
    }

    /** Returns the frame-rate limit to use instead of vanilla's throttle, or -1 to keep vanilla's. */
    public static int overrideLimit(int framerateLimit) {
        Modules modules = Modules.get();
        if (modules == null) return -1;
        NoFpsThrottle module = modules.get(NoFpsThrottle.class);
        if (module == null || !module.isActive()) return -1;
        return Math.min(framerateLimit, module.limit.get());
    }
}
