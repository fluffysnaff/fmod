package addon.fmod.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BlockPosArgumentType implements ArgumentType<BlockPos> {
    private static final Collection<String> EXAMPLES = Arrays.asList("0 0 0", "~ ~ ~", "~2 ~1 ~-5");

    private static final DynamicCommandExceptionType MISSING_COORDINATE = new DynamicCommandExceptionType(object -> Component.literal("Command is incomplete missing 1 or more coordinates."));
    private static final DynamicCommandExceptionType INCOMPLETE_EXCEPTION = new DynamicCommandExceptionType(object -> Component.literal("Incomplete position argument."));

    public static BlockPosArgumentType pos() {
        return new BlockPosArgumentType();
    }

    public static BlockPos getPos(final CommandContext<?> context, final String name) {
        return context.getArgument(name, BlockPos.class);
    }

    public BlockPos parse(StringReader stringReader) throws CommandSyntaxException {
        return parsePos(stringReader);
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (!(context.getSource() instanceof SharedSuggestionProvider)) {
            return Suggestions.empty();
        } else {
            return SharedSuggestionProvider.suggestCoordinates(builder.getRemaining(), ((SharedSuggestionProvider) context.getSource()).getRelevantCoordinates(), builder, Commands.createValidator(this::parse));
        }
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    // Utils

    private BlockPos parsePos(StringReader reader) throws CommandSyntaxException {
        String argument = reader.getString();
        int cursor = reader.getCursor();

        Pair<Boolean, Integer> x = parseCoordinate(reader);

        if (reader.canRead() && reader.peek() == ' ') {
            reader.skip();

            Pair<Boolean, Integer> y = parseCoordinate(reader);

            if (reader.canRead() && reader.peek() == ' ') {
                reader.skip();

                Pair<Boolean, Integer> z = parseCoordinate(reader);

                return new BlockPos(
                    x.getFirst() ? (int)x.getSecond() + (int)mc.player.getX() : (int)x.getSecond(),
                    y.getFirst() ? (int)y.getSecond() + (int)mc.player.getY() : (int)y.getSecond(),
                    z.getFirst() ? (int)z.getSecond() + (int)mc.player.getZ() : (int)z.getSecond()
                );
            } else {
                reader.setCursor(cursor);

                throw INCOMPLETE_EXCEPTION.create(argument);
            }
        } else {
            reader.setCursor(cursor);

            throw INCOMPLETE_EXCEPTION.create(argument);
        }
    }

    private Pair<Boolean, Integer> parseCoordinate(StringReader reader) throws CommandSyntaxException {
        String argument = reader.getString();

        if (!reader.canRead()) {
            throw MISSING_COORDINATE.create(argument);
        } else {
            boolean relative = isRelative(reader);
            int cursor = reader.getCursor();
            int value = reader.canRead() && reader.peek() != ' ' ? reader.readInt() : 0;

            String string = reader.getString().substring(cursor, reader.getCursor());

            if (relative && string.isEmpty()) {
                return Pair.of(true, 0);
            } else {
                return Pair.of(relative, value);
            }
        }
    }

    private boolean isRelative(StringReader reader) {
        boolean relative;

        if (reader.peek() == '~') {
            relative = true;
            reader.skip();
        } else {
            relative = false;
        }

        return relative;
    }
}
