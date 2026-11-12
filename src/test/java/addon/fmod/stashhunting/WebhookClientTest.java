package addon.fmod.stashhunting;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WebhookClientTest {
    @Test void serialisesUntrustedChatWithoutBreakingJson() {
        String message = "a quote: \" and a backslash: \\ and a newline:\nUnicode: \u2603";
        String json = WebhookClient.message("Found \"stash\"", message, "player").toString();
        var embed = JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("embeds").get(0).getAsJsonObject();
        assertEquals(message, embed.get("description").getAsString());
        assertEquals("Found \"stash\"", embed.get("title").getAsString());
        assertEquals("From: player", embed.getAsJsonObject("footer").get("text").getAsString());
    }
}
