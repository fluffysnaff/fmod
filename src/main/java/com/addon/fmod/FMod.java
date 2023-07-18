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
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

public class FMod extends MeteorAddon
{
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("FMod", Items.AMETHYST_SHARD.getDefaultStack());
    public static final HudGroup HUD_GROUP = new HudGroup("FMod");

    public static double round(double val, int dec)
    {
        int decimals = (int) Math.pow(10, dec);
        if((Modules.get().get(LiveWalk.class)).classicRoundEnabled())
        {
            double n = Math.round(val * decimals) / (double)decimals;
            return Math.nextAfter(n, n + Math.signum(n)); // this tries to fix floating point errors
        }
        //  This is our own floating point fix
        long subX = ((long) (val * decimals)) % 10;
        return ((val * decimals) - subX) / decimals;
    }

    public static double roundCoordOnPositionPacket(double val)
    {
        // First round to the thousandths
        double newVal = FMod.round(val, 3);

        // Then check if it rounded correctly - if no round again
        for(int i = 2; i > 0; i--)
        {
            if (FMod.isNotRoundedPos(newVal, newVal))
            {
                newVal = FMod.round(newVal, i);
            }
        }

        // If it for some reason couldn't round, just truncate it completely
        if (FMod.isNotRoundedPos(newVal, newVal))
        {
            newVal = (int)(newVal);
        }

        return newVal;
    }

    public static void roundOnPositionPacket(Args args)
    {
        double dx = args.get(0);
        double dz = args.get(2);
        double y = args.get(1);

        // First round to the thousandths
        dx = FMod.round(dx, 3);
        dz = FMod.round(dz, 3);

        // Then check if it rounded correctly - if no round again
        for(int i = 2; i > 0; i--)
        {
            if (FMod.isNotRoundedPos(dx, dz))
            {
                dx = FMod.round(dx, i);
                dz = FMod.round(dz, i);
            }
        }
        if (FMod.isNotRoundedPos(dx, dz))
        {
            dx = (int)(dx);
            dz = (int)(dz);
        }

        args.set(0, dx);
        args.set(2, dz);
        args.set(1, y);
    }

    public static boolean isNotRoundedPos(double x, double z)
    {
        return ((((long) (x * 1000)) % 10) != 0) && ((((long) (z * 1000)) % 10) != 0);
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
