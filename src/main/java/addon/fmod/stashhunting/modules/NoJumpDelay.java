package addon.fmod.stashhunting.modules;

import addon.fmod.FMod;
import meteordevelopment.meteorclient.systems.modules.Module;

public class NoJumpDelay extends Module
{
    public NoJumpDelay()
    {
        super(
            FMod.CATEGORY,
            "no-jump-delay",
            "Removes the delay between jumps."
        );
    }
}
