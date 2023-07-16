package com.addon.fmod.commands.commands;

import com.addon.fmod.FMod;
import com.addon.fmod.utils.FModUtils;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import static com.addon.fmod.utils.FModUtils.clipStraight;
import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ClubMateCommand extends Command {
    public ClubMateCommand() {
        super("clubmate", "Get clubmate!", "cm", "clubm", "cmate");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder)
    {
        builder.then(literal("clubmate")).executes(context ->
        {
            // Move `direction` blocks into viewing direction
            assert mc.player != null;
            clipStraight(new Vec3d(1331, 89, 1330));  // Next to chest
            FModUtils.interactAt(new BlockPos(1331, 89, 1331));  // Chest
            return SINGLE_SUCCESS;
        }
        );
    }

}
