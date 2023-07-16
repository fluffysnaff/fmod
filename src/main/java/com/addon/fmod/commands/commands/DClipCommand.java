package com.addon.fmod.commands.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.Vec3d;

import static com.addon.fmod.utils.FModUtils.clipStraight;
import static com.addon.fmod.utils.FModUtils.clipUpDown;
import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class DClipCommand extends Command {
    public DClipCommand() {
        super("dclip", "Allows to clip in your view direction.", "dc");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder)
    {
        builder.then(argument("distance", DoubleArgumentType.doubleArg()).executes(context -> {
            double distance = context.getArgument("distance", Double.class);
            // Move `direction` blocks into viewing direction
            assert mc.player != null;
            Vec3d pos = mc.player.getPos();
            // Move into players viewing direction
            Vec3d targetPos = pos.add(mc.player.getRotationVector().normalize().multiply(distance));
            clipStraight(targetPos);

            return SINGLE_SUCCESS;
        }));
    }

}
