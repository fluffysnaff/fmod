package addon.fmod.stashhunting;

import addon.fmod.FMod;
import addon.fmod.stashhunting.hud.EntityList;
import addon.fmod.stashhunting.hud.Weather;
import addon.fmod.stashhunting.modules.*;
import addon.fmod.stashhunting.modules.searcharea.SearchArea;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.fabricmc.loader.api.FabricLoader;

public final class StashHunting {
    private StashHunting() {}

    public static void register() {
        Modules modules = Modules.get();
        modules.add(new SearchArea());
        modules.add(new AutoLogPlus());
        modules.add(new GotoPosition());
        modules.add(new ChestIndex());
        modules.add(new HighlightOldLava());
        modules.add(new VanityESP());
        modules.add(new AFKVanillaFly());
        modules.add(new AutoPortal());
        modules.add(new Pitch40Util());
        modules.add(new NoJumpDelay());
        modules.add(new GrimAirPlace());
        modules.add(new DiscordNotifs());
        modules.add(new AutoEXPPlus());

        boolean baritone = loaded("baritone") || loaded("baritone-meteor");
        boolean maps = loaded("xaeroworldmap") && loaded("xaeroplus");
        if (maps && loaded("xaerominimap")) {
            modules.add(new BetterStashFinder());
            modules.add(new OldChunkNotifier());
            modules.add(new TrailMaker());
        }
        if (maps && baritone) modules.add(new TrailFollower());
        if (baritone) modules.add(new ElytraFlyPlusPlus());
        if (!maps) FMod.LOG.info("Install XaeroPlus and Xaero's maps to enable the map integrations.");
        if (!baritone) FMod.LOG.info("Install Meteor Baritone to enable the trail pathfinding and ElytraFly++ integrations.");

        Hud.get().register(Weather.INFO);
        Hud.get().register(EntityList.INFO);
    }

    private static boolean loaded(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }
}
