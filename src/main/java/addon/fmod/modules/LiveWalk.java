package addon.fmod.modules;

import addon.fmod.FMod;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

public class LiveWalk extends Module
{
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> vehicle = sgGeneral.add(new BoolSetting.Builder()
        .name("vehicle")
        .description("Enable vehicle bypass.")
        .defaultValue(true)
        .build()
    );

    public LiveWalk()
    {
        super(FMod.CATEGORY, "Live Walk", "Bypass Liveoverflow skiddy plugin with ease.");
    }

    public boolean vehicleEnabled() { return vehicle.get(); }
}
