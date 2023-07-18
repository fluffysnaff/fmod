package com.addon.fmod.modules;

import com.addon.fmod.FMod;
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
    private final Setting<Boolean> classicRound = sgGeneral.add(new BoolSetting.Builder()
        .name("classic-round")
        .description("Classic rounding with Math.round()")
        .defaultValue(false)
        .build()
    );

    public LiveWalk()
    {
        super(FMod.CATEGORY, "Live Walk", "Bypass Liveoverflow skiddy plugin with ease.");
    }

    public boolean classicRoundEnabled() { return classicRound.get(); }

    public boolean vehicleEnabled() { return vehicle.get(); }
}
