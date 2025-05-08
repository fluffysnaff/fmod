package addon.fmod.modules;

import addon.fmod.FMod;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

public class LOPackets extends Module
{
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> worldBorder = sgGeneral.add(new BoolSetting.Builder()
        .name("World border")
        .description("Enable world border bypass.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> demoPopup = sgGeneral.add(new BoolSetting.Builder()
        .name("Demo")
        .description("Enable demo popup bypass.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> creativeMode = sgGeneral.add(new BoolSetting.Builder()
        .name("Creative")
        .description("Enable fake creative gamemode bypass.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> endCredits = sgGeneral.add(new BoolSetting.Builder()
        .name("End credits")
        .description("Enable end credits bypass.")
        .defaultValue(true)
        .build()
    );
    public LOPackets()
    {
        super(FMod.CATEGORY, "lo-packets", "Bypass Liveoverflow weird packets");
    }

    public Setting<Boolean> getWorldBorder() {
        return worldBorder;
    }

    public Setting<Boolean> getDemoPopup() {
        return demoPopup;
    }

    public Setting<Boolean> getCreativeMode() {
        return creativeMode;
    }

    public Setting<Boolean> getEndCredits() {
        return endCredits;
    }
}
