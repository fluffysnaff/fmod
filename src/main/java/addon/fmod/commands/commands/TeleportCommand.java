package addon.fmod.commands.commands;

import addon.fmod.commands.arguments.PositionArgumentType;
import addon.fmod.utils.WarpUtils;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.world.phys.Vec3;

public class TeleportCommand extends Command {
    public TeleportCommand() {
        super("teleport", "Allows to teleport small distances.", "tp");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {

        builder.then(argument("pos", PositionArgumentType.pos()).then(argument("ticks", IntegerArgumentType.integer(0)).executes(context -> {
            if (mc.player == null) return 0;

            Vec3 pos = PositionArgumentType.getPos(context, "pos");
            int ticks =  IntegerArgumentType.getInteger(context, "ticks");

            mc.player.noPhysics = true;
            WarpUtils.warpTo(pos, (ticks <= 0 ? 1 : ticks));

            return SINGLE_SUCCESS;
        })));

    }
}
