package addon.fmod.stashhunting;

import addon.fmod.FMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** One bounded worker owns outbound notifications; no network I/O runs on the client tick. */
public final class WebhookClient {
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final ThreadPoolExecutor WORKER = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(128), task -> {
            Thread thread = new Thread(task, "FMod webhooks");
            thread.setDaemon(true);
            return thread;
        });

    private WebhookClient() {}

    public static JsonObject message(String title, String description, String playerName) {
        JsonObject embed = new JsonObject();
        embed.addProperty("title", title);
        embed.addProperty("description", description);
        embed.addProperty("color", 15258703);
        JsonObject footer = new JsonObject();
        footer.addProperty("text", "From: " + playerName);
        embed.add("footer", footer);
        JsonArray embeds = new JsonArray();
        embeds.add(embed);
        JsonObject payload = new JsonObject();
        payload.add("embeds", embeds);
        return payload;
    }

    public static void submit(String endpoint, String json, String pingId) {
        if (endpoint == null || endpoint.isBlank()) return;
        try {
            URI uri = URI.create(endpoint);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
                FMod.LOG.warn("Webhook URL must be an HTTPS URL without embedded credentials.");
                return;
            }
            JsonObject payload = JsonParser.parseString(json).getAsJsonObject();
            JsonObject mentions = new JsonObject();
            mentions.add("parse", new JsonArray());
            if (pingId != null && pingId.matches("[0-9]{1,22}")) {
                payload.addProperty("content", "<@" + pingId + ">");
                JsonArray users = new JsonArray();
                users.add(pingId);
                mentions.add("users", users);
            }
            payload.add("allowed_mentions", mentions);
            // Discord requires embed field values to be strings, including item counts.
            if (payload.has("embeds")) for (var element : payload.getAsJsonArray("embeds")) {
                JsonObject embed = element.getAsJsonObject();
                if (embed.has("fields")) for (var field : embed.getAsJsonArray("fields")) {
                    JsonObject value = field.getAsJsonObject();
                    if (value.has("value")) value.addProperty("value", value.get("value").getAsString());
                }
            }
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(payload.toString())).build();
            WORKER.execute(() -> send(request));
        } catch (RejectedExecutionException e) {
            FMod.LOG.warn("Webhook queue is full; notification was not sent.");
        } catch (RuntimeException e) {
            FMod.LOG.warn("Invalid webhook URL or payload; notification was not sent.");
        }
    }

    private static void send(HttpRequest request) {
        try {
            int status = CLIENT.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (status < 200 || status >= 300) FMod.LOG.warn("Webhook notification failed with HTTP {}.", status);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // Exceptions can include the webhook token in the URL; log only the failure category.
            FMod.LOG.warn("Webhook notification failed ({}).", e.getClass().getSimpleName());
        }
    }
}
