package addon.fmod.modules.notexturerotations;

import addon.fmod.FMod;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

import java.security.SecureRandom;

import static addon.fmod.modules.notexturerotations.NoTextureRotationsModes.NO_ROTATIONS;

public class NoTextureRotations extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<NoTextureRotationsModes> flightMode = sgGeneral.add(new EnumSetting.Builder<NoTextureRotationsModes>()
        .name("mode")
        .description("The mode of flying.")
        .defaultValue(NO_ROTATIONS)
        .onModuleActivated(flightModesSetting -> onModeChanged(flightModesSetting.get()))
        .onChanged(this::onModeChanged)
        .build()
    );

    public NoTextureRotations() {
        super(FMod.CATEGORY, "no-texture-rotations", "Disables rotating textures for blocks like(grass, bedrock, ...) REQUIRES YOU TO REJOIN!");
    }

    private NoTextureRotationsModes currentMode = NO_ROTATIONS;

    public NoTextureRotationsModes getCurrentMode() {
        return currentMode;
    }

    public static final SecureRandom secureRandom = new SecureRandom();

    private void onModeChanged(NoTextureRotationsModes mode) {
        currentMode = mode;
    }
}
