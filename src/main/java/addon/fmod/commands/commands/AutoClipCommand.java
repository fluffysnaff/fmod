package addon.fmod.commands.commands;

import addon.fmod.utils.WarpUtils;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;


public class AutoClipCommand extends Command {
    public AutoClipCommand() {
        super("autoclip", "Allows to clip through ground up/down.", "ac", "aclip");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder)
    {
        builder.then(argument("direction", DoubleArgumentType.doubleArg()).executes(context -> {
            if (mc.player == null) return 0;
            double direction = context.getArgument("direction", Double.class);
            int dir = direction == 1 ? 1 : -1;
            Vec3d pos = WarpUtils.getAutoClipPos(dir);
            if (pos == null) {
                MutableText errOut = Text.literal("");
                errOut.append(Text.literal("No valid position found within").formatted(Formatting.GRAY));
                errOut.append(Text.literal(" 150 blocks").formatted(Formatting.DARK_PURPLE));
                ChatUtils.sendMsg(errOut);
                return 0;
            }
            MutableText errOut = Text.literal("");
            errOut.append(Text.literal("Clipping ").formatted(Formatting.GRAY));
            errOut.append(Text.literal(String.format(" %d blocks", (int)pos.y - (int)mc.player.getPos().y)).formatted(Formatting.DARK_PURPLE));
            ChatUtils.sendMsg(errOut);
            WarpUtils.warpTo(pos, 18);
            return SINGLE_SUCCESS;
        }));
    }

}
