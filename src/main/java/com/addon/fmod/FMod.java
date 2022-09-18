package com.addon.fmod;

import com.addon.fmod.commands.CommandExample;
import com.addon.fmod.hud.HudExample;
import com.addon.fmod.modules.LiveWalk;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.Items;
import org.slf4j.Logger;

public class FMod extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("FMod", Items.DAMAGED_ANVIL.getDefaultStack());
    public static final HudGroup HUD_GROUP = new HudGroup("FMod");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor FMod Addon");

        // Modules
        initModulesFMod();

        // Commands
        Commands.get().add(new CommandExample());

        // HUD
        Hud.get().register(HudExample.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.example.addon";
    }

    private void initModulesFMod()
    {
        Modules.get().add(new LiveWalk());
    }
}
