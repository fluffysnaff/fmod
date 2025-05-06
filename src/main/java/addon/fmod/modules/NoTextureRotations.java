package addon.fmod.modules;

import addon.fmod.FMod;
import meteordevelopment.meteorclient.systems.modules.Module;

public class NoTextureRotations extends Module {
    public NoTextureRotations() {
        super(FMod.CATEGORY, "no-texture-rotations", "Disables rotating textures for blocks like(grass, bedrock, ...) REQUIRES YOU TO REJOIN!");
    }
}
