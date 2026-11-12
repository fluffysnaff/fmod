package addon.fmod;

import addon.fmod.commands.commands.*;
import addon.fmod.modules.*;
import addon.fmod.modules.notexturerotations.NoTextureRotations;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

public class FMod extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("FMod", () -> Items.AMETHYST_SHARD.getDefaultInstance());

    @Override
    public void onInitialize() {
        LOG.info("Initializing FMod");

        // Modules
        initModulesFMod();

        // Commands
        initCommandsFMod();

    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "addon.fmod";
    }

    private void initModulesFMod()
    {
        Modules.get().add(new NoTextureRotations());
        Modules.get().add(new FrameDupe());
    }

    private void initCommandsFMod()
    {
        Commands.add(new TeleportCommand());
        Commands.add(new AutoClipCommand());
        Commands.add(new SHClipCommand());
        Commands.add(new DClipCommand());
        Commands.add(new ClubMateCommand());
    }

}
