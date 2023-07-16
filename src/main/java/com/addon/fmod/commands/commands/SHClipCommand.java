package com.addon.fmod.commands.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.Vec3d;

import static com.addon.fmod.utils.FModUtils.*;
import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class SHClipCommand extends Command {
    public SHClipCommand() {
        super("shclip", "Allows to clip horizontally(even thru walls).", "hc", "shc");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder)
    {
        builder.then(argument("distance", DoubleArgumentType.doubleArg()).executes(context -> {
            double distance = context.getArgument("distance", Double.class);
            // Move `direction` blocks into viewing direction
            assert mc.player != null;
            Vec3d targetPos = mc.player.getPos().add(
                mc.player.getRotationVector().multiply(1, 0, 1).normalize().multiply(distance)
            );

            clipUpDown(targetPos);

            return SINGLE_SUCCESS;
        }));
    }

}
