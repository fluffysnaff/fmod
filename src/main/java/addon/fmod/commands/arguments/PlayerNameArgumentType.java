package addon.fmod.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PlayerNameArgumentType implements ArgumentType<String> {
    private static Collection<String> EXAMPLES;

    static {
        if (mc.level != null && mc.getConnection() != null) {
            EXAMPLES = mc.level.players()
                .stream()
                .map(Player::getScoreboardName)
                .collect(Collectors.toList()
                );

            EXAMPLES.addAll(mc.getConnection().getOnlinePlayers()
                .stream()
                .limit(3)
                .map(entry -> entry.getProfile().name())
                .toList()
            );
        }
    }

    public static PlayerNameArgumentType player() {
        return new PlayerNameArgumentType();
    }

    public static String getPlayer(CommandContext<?> context, final String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();

        while (reader.canRead() && isValidChar(reader.peek())) {
            reader.skip();
        }

        return reader.getString().substring(start, reader.getCursor());
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        Collection<String> suggestions = new ArrayList<>(
            mc.level.players()
                .stream()
                .map(Player::getScoreboardName)
                .toList()
        );

        suggestions.addAll(mc.getConnection().getOnlinePlayers()
            .stream()
            .limit(3)
            .map(entry -> entry.getProfile().name())
            .toList()
        );

        return SharedSuggestionProvider.suggest(suggestions, builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    private boolean isValidChar(char character) {
        return character != '\\' && (character >= '0' && character <= '9' || character >= 'A' && character <= 'Z' || character >= 'a' && character <= 'z'
            || character >= '!' && character <= '/' || character >= ':' && character <= '@' || character >= '[' && character <= '`' || character >= '{' && character <= '~');
    }
}
