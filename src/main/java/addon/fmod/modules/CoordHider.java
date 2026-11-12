package addon.fmod.modules;

import addon.fmod.FMod;
import addon.fmod.utils.CoordinateMask;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;

public final class CoordHider extends Module {
    private final SettingGroup general = settings.getDefaultGroup();
    private final Setting<Boolean> f3 = general.add(new BoolSetting.Builder()
        .name("hide-f3-location").description("Hides F3 position, section, terrain height, and targeted block/fluid/entity entries.")
        .defaultValue(true).build());
    private final Setting<Boolean> meteor = general.add(new BoolSetting.Builder()
        .name("hide-meteor-coordinate-hud").description("Hides Meteor text HUD elements that use position Starscript variables, including opposite dimension coordinates.")
        .defaultValue(true).build());
    private final Setting<Boolean> text = general.add(new BoolSetting.Builder()
        .name("mask-coordinate-text").description("Redacts coordinate triples and labelled coordinates in vanilla and Meteor text. Does not hide maps or world geometry.")
        .defaultValue(true).build());

    public CoordHider() {
        super(FMod.CATEGORY, "coord-hider", "Hides location displays for recording and streaming, without changing your actual position.");
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

    public static boolean hideHud(String template) {
        CoordHider module = active();
        return module != null && module.meteor.get() && CoordinateMask.containsCoordinateTemplate(template);
    }

    public static String mask(String value) {
        CoordHider module = active();
        return module != null && module.text.get() ? CoordinateMask.redact(value) : value;
    }
}
