package addon.fmod;

import addon.fmod.commands.commands.*;
import addon.fmod.hud.DubCounterHud;
import addon.fmod.features.containers.ContainerTracker;
import addon.fmod.features.containers.ContainerWorld;
import addon.fmod.features.flight.MeteorFlightChunkGuard;
import addon.fmod.volytra.VolytraIntegration;
import addon.fmod.utils.WarpUtils;
import addon.fmod.modules.*;
import addon.fmod.modules.notexturerotations.NoTextureRotations;
import addon.fmod.stashhunting.StashHunting;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

public class FMod extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("FMod", () -> Items.AMETHYST_SHARD.getDefaultInstance());
    public static final HudGroup HUD_GROUP = new HudGroup("FMod");

    @Override
    public void onInitialize() {
        LOG.info("Initializing FMod");

        // Modules
        initModulesFMod();
        MeteorFlightChunkGuard.register();
        VolytraIntegration.register();
        Hud.get().register(DubCounterHud.INFO);
        MeteorClient.EVENT_BUS.subscribe(ContainerWorld.INSTANCE);
        MeteorClient.EVENT_BUS.subscribe(ContainerTracker.INSTANCE);
        MeteorClient.EVENT_BUS.subscribe(WarpUtils.INSTANCE);

        // Commands
        initCommandsFMod();
        StashHunting.register();

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
        Modules.get().add(new EventlessFly());
        Modules.get().add(new KnockbackTweaks());
        Modules.get().add(new SeedMap());
        Modules.get().add(new UIUtils());
        Modules.get().add(new CoordHider());
        Modules.get().add(new InstaMine());
        Modules.get().add(new InfReach());
        Modules.get().add(new SpeedBypass());
        Modules.get().add(new AntiMapCrash());
        Modules.get().add(new NoFpsThrottle());
        Modules.get().add(new StasisCourier());
        Modules.get().add(new PearlLoader());
        Modules.get().add(new ChestTrackerModule());
        Modules.get().add(new ChestIndex());
        Modules.get().add(new ShulkerOverviewModule());
        Modules.get().add(new StashBrander());
        Modules.get().add(new RoadTrip());
        Modules.get().add(new SignHistorian());
        Modules.get().add(new SignatureSign());
        Modules.get().add(new VolytraFly());
    }

    private void initCommandsFMod()
    {
        Commands.add(new TeleportCommand());
        Commands.add(new UIUtilsCommand());
        Commands.add(new AutoClipCommand());
        Commands.add(new SHClipCommand());
        Commands.add(new DClipCommand());
        Commands.add(new VClipCommand());
        Commands.add(new StashMapCommand());
        Commands.add(new ChestTrackerCommand());
        Commands.add(new PearlLoaderCommand());
        Commands.add(new StashMoverCommand());
        Commands.add(new RoadTripCommand());
        Commands.add(new SignHistoryCommand());
        Commands.add(new VolytraNavCommand());
    }

}
