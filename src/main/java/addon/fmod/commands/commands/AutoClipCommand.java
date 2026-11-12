package addon.fmod.commands.commands;

import addon.fmod.utils.WarpUtils;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.Vec3;


public class AutoClipCommand extends Command {
    public AutoClipCommand() {
        super("autoclip", "Allows to clip through ground up/down.", "ac", "aclip");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder)
    {
        builder.then(argument("direction", DoubleArgumentType.doubleArg()).executes(context -> {
            if (mc.player == null) return 0;
            double direction = context.getArgument("direction", Double.class);
            int dir = direction == 1 ? 1 : -1;
            Vec3 pos = WarpUtils.getAutoClipPos(dir);
            if (pos == null) {
                MutableComponent errOut = Component.literal("");
                errOut.append(Component.literal("No valid position found within").withStyle(ChatFormatting.GRAY));
                errOut.append(Component.literal(" 150 blocks").withStyle(ChatFormatting.DARK_PURPLE));
                ChatUtils.sendMsg(errOut);
                return 0;
            }
            MutableComponent errOut = Component.literal("");
            errOut.append(Component.literal("Clipping ").withStyle(ChatFormatting.GRAY));
            errOut.append(Component.literal(String.format(" %d blocks", (int)pos.y - (int)mc.player.position().y)).withStyle(ChatFormatting.DARK_PURPLE));
            ChatUtils.sendMsg(errOut);
            WarpUtils.warpTo(pos, 18);
            return SINGLE_SUCCESS;
        }));
    }

}
