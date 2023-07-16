package com.addon.fmod.modules;

import com.addon.fmod.FMod;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;

import java.util.HashSet;
import java.util.Objects;

public class LOPackets extends Module
{

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> worldBorder = sgGeneral.add(new BoolSetting.Builder()
        .name("World border")
        .description("Enable world border bypass.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> demoPopup = sgGeneral.add(new BoolSetting.Builder()
        .name("Demo")
        .description("Enable demo popup bypass.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> creativeMode = sgGeneral.add(new BoolSetting.Builder()
        .name("Creative")
        .description("Enable fake creative gamemode bypass.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> endCredits = sgGeneral.add(new BoolSetting.Builder()
        .name("End credits")
        .description("Enable end credits bypass.")
        .defaultValue(true)
        .build()
    );
    public LOPackets()
    {
        super(FMod.CATEGORY, "LOPackets", "Bypass Liveoverflow weird packets");
    }

    public Setting<Boolean> getWorldBorder() {
        return worldBorder;
    }

    public Setting<Boolean> getDemoPopup() {
        return demoPopup;
    }

    public Setting<Boolean> getCreativeMode() {
        return creativeMode;
    }

    public Setting<Boolean> getEndCredits() {
        return endCredits;
    }
}
