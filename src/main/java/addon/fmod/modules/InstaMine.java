package addon.fmod.modules;

import addon.fmod.FMod;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

public class InstaMine extends Module
{
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public InstaMine()
    {
        super(FMod.CATEGORY, "insta-mine", "Attempts to stop mining at 70% progress");
    }


}
