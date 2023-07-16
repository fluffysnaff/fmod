package com.addon.fmod;

import com.addon.fmod.commands.commands.*;
import com.addon.fmod.hud.HudExample;
import com.addon.fmod.modules.FrozenWalk;
import com.addon.fmod.modules.LOPackets;
import com.addon.fmod.modules.LiveWalk;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.commands.Commands;
import net.minecraft.item.Items;
import org.slf4j.Logger;

public class FMod extends MeteorAddon
{
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("FMod", Items.DAMAGED_ANVIL.getDefaultStack());
    public static final HudGroup HUD_GROUP = new HudGroup("FMod");

    public static double round(double val, int dec)
    {
        int decimals = (int) Math.pow(10, dec);
        LiveWalk liveWalkModule = Modules.get().get(LiveWalk.class);
        if(liveWalkModule != null && liveWalkModule.classicRoundEnabled())
        {
            double n = Math.round(val * decimals) / (double)decimals;
            return Math.nextAfter(n, n + Math.signum(n));
        }
        long subX = ((long) (val * decimals)) % 10;
        return ((val * decimals) - subX) / decimals;
    }
    public static boolean isNotRoundedPos(double x, double z)
    {
        return ((long) (x * 1000)) % 10 != 0 && ((long) (z * 1000)) % 10 != 0;
    }

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor FMod Addon");

        // Modules
        initModulesFMod();

        // Commands
        initCommandsFMod();

        // HUD
        Hud.get().register(HudExample.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.addon.fmod";
    }

    private void initModulesFMod()
    {
        Modules.get().add(new LiveWalk());
        Modules.get().add(new FrozenWalk());
        Modules.get().add(new LOPackets());
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
