package com.addon.fmod.commands.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.Vec3d;

import static com.addon.fmod.utils.FModUtils.clipStraight;
import static com.addon.fmod.utils.FModUtils.getAutoClipPos;
import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class AutoClipCommand extends Command {
    public AutoClipCommand() {
        super("autoclip", "Allows to clip through ground up/down.", "ac");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder)
    {
        builder.then(argument("direction", DoubleArgumentType.doubleArg()).executes(context -> {
            double direction = context.getArgument("direction", Double.class);
            int dir = direction == 1 ? 1 : -1;
            Vec3d pos = getAutoClipPos(dir);
            if (pos == null) {
                //context.getSource().sendFeedback(Text.of("§cNo valid position found within 150 blocks"));
                return 0;
            }
            clipStraight(pos);
            return SINGLE_SUCCESS;
        }));
    }

}
