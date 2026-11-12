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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PositionArgumentType implements ArgumentType<Vec3> {
    private static final Collection<String> EXAMPLES = Arrays.asList("0 0 0", "~ ~ ~", "^ ^ ^", "^1 ^ ^-5", "0.1 -0.5 .9", "~0.5 ~1 ~-5");

    private static final DynamicCommandExceptionType MISSING_COORDINATE = new DynamicCommandExceptionType(object -> Component.literal("Command is incomplete missing 1 or more coordinates."));
    private static final DynamicCommandExceptionType INCOMPLETE_EXCEPTION = new DynamicCommandExceptionType(object -> Component.literal("Incomplete position argument."));
    private static final DynamicCommandExceptionType MIXED_COORDINATE_EXCEPTION = new DynamicCommandExceptionType(object -> Component.literal("Can't mix two or more position argument types."));

    private final boolean centerIntegers;

    public PositionArgumentType(boolean centerIntegers) {
        this.centerIntegers = centerIntegers;
    }

    public static PositionArgumentType pos() {
        return new PositionArgumentType(true);
    }

    public static PositionArgumentType pos(boolean centerIntegers) {
        return new PositionArgumentType(centerIntegers);
    }

    public static Vec3 getPos(final CommandContext<?> context, final String name) {
        return context.getArgument(name, Vec3.class);
    }

    public Vec3 parse(StringReader stringReader) throws CommandSyntaxException {
        return stringReader.canRead() && stringReader.peek() == '^' ? parseLookingPos(stringReader) : parsePos(stringReader, this.centerIntegers);
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (!(context.getSource() instanceof SharedSuggestionProvider)) {
            return Suggestions.empty();
        } else {
            String string = builder.getRemaining();

            if (!string.isEmpty() && string.charAt(0) == '^') {
                Set<SharedSuggestionProvider.TextCoordinates> collection = Collections.singleton(SharedSuggestionProvider.TextCoordinates.DEFAULT_LOCAL);
                return SharedSuggestionProvider.suggestCoordinates(string, collection, builder, Commands.createValidator(this::parse));
            } else {
                Collection<SharedSuggestionProvider.TextCoordinates> collection = ((SharedSuggestionProvider) context.getSource()).getAbsoluteCoordinates();
                return SharedSuggestionProvider.suggestCoordinates(string, collection, builder, Commands.createValidator(this::parse));
            }
        }
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    // Looking Pos
    private Vec3 parseLookingPos(StringReader reader) throws CommandSyntaxException {
        String argument = reader.getString();
        int cursor = reader.getCursor();

        double x = readCoordinate(reader, cursor);

        if (reader.canRead() && reader.peek() == ' ') {
            reader.skip();

            double y = readCoordinate(reader, cursor);

            if (reader.canRead() && reader.peek() == ' ') {
                reader.skip();

                double z = readCoordinate(reader, cursor);

                return toAbsolutePos(new Vec3(x, y, z));
            } else {
                reader.setCursor(cursor);

                throw INCOMPLETE_EXCEPTION.create(argument);
            }
        } else {
            reader.setCursor(cursor);

            throw INCOMPLETE_EXCEPTION.create(argument);
        }
    }

    public Vec3 toAbsolutePos(Vec3 original) {
        if(mc.player == null) return original;
        Vec2 rotation = mc.player.getRotationVector();
        Vec3 position = mc.player.position();

        float cos1 = Mth.cos((rotation.y + 90.0F) * 0.017453292F);
        float sin1 = Mth.sin((rotation.y + 90.0F) * 0.017453292F);
        float cos2 = Mth.cos(-rotation.x * 0.017453292F);
        float sin2 = Mth.sin(-rotation.x * 0.017453292F);
        float cos3 = Mth.cos((-rotation.x + 90.0F) * 0.017453292F);
        float sin3 = Mth.sin((-rotation.x + 90.0F) * 0.017453292F);

        Vec3 vector1 = new Vec3(cos1 * cos2, sin2, sin1 * cos2);
        Vec3 vector2 = new Vec3(cos1 * cos3, sin3, sin1 * cos3);
        Vec3 product = vector1.cross(vector2).scale(-1.0D);

        double x = vector1.x * original.z + vector2.x * original.y + product.x * original.x;
        double y = vector1.y * original.z + vector2.y * original.y + product.y * original.x;
        double z = vector1.z * original.z + vector2.z * original.y + product.z * original.x;

        return new Vec3(position.x + x, position.y + y, position.z + z);
    }

    private double readCoordinate(StringReader reader, int start) throws CommandSyntaxException {
        String argument = reader.getString();

        if (!reader.canRead()) {
            throw MISSING_COORDINATE.create(argument);
        } else if (reader.peek() != '^') {
            reader.setCursor(start);

            throw MIXED_COORDINATE_EXCEPTION.create(argument);
        } else {
            reader.skip();

            return reader.canRead() && reader.peek() != ' ' ? reader.readDouble() : 0.0;
        }
    }

    // Default Pos

    private Vec3 parsePos(StringReader reader, boolean centerIntegers) throws CommandSyntaxException {
        String argument = reader.getString();
        int cursor = reader.getCursor();

        Pair<Boolean, Double> x = parseCoordinate(reader, centerIntegers);

        if (reader.canRead() && reader.peek() == ' ') {
            reader.skip();

            Pair<Boolean, Double> y = parseCoordinate(reader, false);

            if (reader.canRead() && reader.peek() == ' ') {
                reader.skip();

                Pair<Boolean, Double> z = parseCoordinate(reader, centerIntegers);

                return new Vec3(
                    x.getFirst() ? x.getSecond() + mc.player.getX() : x.getSecond(),
                    y.getFirst() ? y.getSecond() + mc.player.getY() : y.getSecond(),
                    z.getFirst() ? z.getSecond() + mc.player.getZ() : z.getSecond()
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

    private Pair<Boolean, Double> parseCoordinate(StringReader reader, boolean center) throws CommandSyntaxException {
        String argument = reader.getString();

        if (reader.canRead() && reader.peek() == '^') {
            throw MIXED_COORDINATE_EXCEPTION.create(argument);
        } else if (!reader.canRead()) {
            throw MISSING_COORDINATE.create(argument);
        } else {
            boolean relative = isRelative(reader);
            int cursor = reader.getCursor();
            double value = reader.canRead() && reader.peek() != ' ' ? reader.readDouble() : 0.0;

            String string = reader.getString().substring(cursor, reader.getCursor());

            if (relative && string.isEmpty()) {
                return Pair.of(true, 0.0);
            } else {
                if (!string.contains(".") && !relative && center) {
                    value += 0.5;
                }

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
