package addon.fmod;

import addon.fmod.commands.commands.*;
import addon.fmod.hud.InfReachHud;
import addon.fmod.modules.*;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.Items;
import org.slf4j.Logger;

public class FMod extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("FMod", Items.AMETHYST_SHARD.getDefaultStack());
    public static final HudGroup HUD_GROUP = new HudGroup("FMod");

    @Override
    public void onInitialize() {
        LOG.info("Initializing FMod");

        // Modules
        initModulesFMod();

        // Commands
        initCommandsFMod();

        // HUD
        initHudFMod();
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
        Modules.get().add(new InstaMine());
        Modules.get().add(new LiveWalk());
        Modules.get().add(new TpMine());
        Modules.get().add(new InfReach());
        Modules.get().add(new FrozenWalk());
        // Modules.get().add(new LOPackets());
    }

    private void initCommandsFMod()
    {
        Commands.add(new TeleportCommand());
        Commands.add(new AutoClipCommand());
        Commands.add(new SHClipCommand());
        Commands.add(new DClipCommand());
        Commands.add(new ClubMateCommand());
    }

    private void initHudFMod()
    {
        Hud.get().register(InfReachHud.INFO);
    }
}
