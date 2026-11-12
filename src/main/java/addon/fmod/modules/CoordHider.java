package addon.fmod.modules;

import addon.fmod.FMod;
import addon.fmod.utils.CoordinateMask;
import addon.fmod.utils.DisplayCoordinates;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.Level;

public final class CoordHider extends Module {
    public enum Mode { Hide, Randomize }

    private volatile DisplayCoordinates coordinates = DisplayCoordinates.generate();
    private final SettingGroup general = settings.getDefaultGroup();
    public final Setting<Mode> mode = general.add(new EnumSetting.Builder<Mode>()
        .name("mode").description("Hide coordinates or display securely generated synthetic coordinates.")
        .defaultValue(Mode.Hide).onChanged(value -> { if (value == Mode.Randomize) regenerate(); }).build());
    private final Setting<Boolean> f3 = general.add(new BoolSetting.Builder()
        .name("hide-f3-location").description("Protects F3 location entries. Randomize mode shows synthetic XYZ, block and chunk coordinates; other sensitive entries stay hidden.")
        .defaultValue(true).build());
    private final Setting<Boolean> meteor = general.add(new BoolSetting.Builder()
        .name("hide-meteor-coordinate-hud").description("Protects Meteor position HUD expressions, including individual axes and opposite dimension coordinates.")
        .defaultValue(true).build());
    private final Setting<Boolean> text = general.add(new BoolSetting.Builder()
        .name("mask-coordinate-text").description("Protects recognised coordinate text in vanilla and Meteor displays. Maps and world geometry remain visible.")
        .defaultValue(true).build());

    public CoordHider() {
        super(FMod.CATEGORY, "coord-hider", "Hides location displays or replaces them with securely generated synthetic coordinates.");
    }

    @Override public void onActivate() { regenerate(); }
    @EventHandler private void onGameJoined(GameJoinedEvent event) { regenerate(); }

    public void regenerate() { coordinates = DisplayCoordinates.generate(); }
    public DisplayCoordinates displayCoordinates() { return coordinates; }

    @Override public WWidget getWidget(GuiTheme theme) {
        WButton button = theme.button("Regenerate fake coordinates");
        button.action = this::regenerate;
        return button;
    }

    public static CoordHider active() {
        if (Modules.get() == null) return null;
        CoordHider module = Modules.get().get(CoordHider.class);
        return module != null && module.isActive() ? module : null;
    }

    public static boolean hideF3() {
        CoordHider module = active();
        return module != null && module.f3.get();
    }

    /** Null means suppress this HUD, including coordinate syntax that cannot be safely replaced. */
    public static String hudTemplate(String source) {
        CoordHider module = active();
        if (module == null || !module.meteor.get() || !CoordinateMask.containsCoordinateTemplate(source)) return source;
        if (module.mode.get() == Mode.Hide) return null;
        String result = CoordinateMask.randomizeTemplate(source, module.coordinates,
            module.mc.level != null && module.mc.level.dimension().equals(Level.NETHER));
        return CoordinateMask.containsCoordinateTemplate(result) ? null : result;
    }

    public static boolean hideHud(String template) { return hudTemplate(template) == null; }

    public static String mask(String value) {
        CoordHider module = active();
        if (module == null || !module.text.get()) return value;
        return module.mode.get() == Mode.Hide ? CoordinateMask.redact(value)
            : CoordinateMask.randomize(value, module.coordinates);
    }
}
