package addon.fmod.modules;

import addon.fmod.FMod;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class SeedMap extends Module {
    private static final String ORIGIN = "https://www.chunkbase.com";
    private static final String MAP_URL = ORIGIN + "/apps/seed-map";
    private static final String SCRIPT = "/assets/fmod/seedmap-script.js";

    private HttpServer server;
    private String endpoint;
    private volatile MapPosition position;

    public SeedMap() {
        super(FMod.CATEGORY, "seed-map", "Follows your position in the browser's Chunkbase Seed Map.");
    }

    @Override
    public void onActivate() {
        String path = "/" + UUID.randomUUID();
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 4);
            int port = server.getAddress().getPort();
            String host = "127.0.0.1:" + port;
            server.createContext(path, exchange -> respond(exchange, host, path));
            server.start();
            endpoint = "http://" + host + path;
        } catch (IOException exception) {
            error("Could not open the local Seed Map connection.");
            toggle();
        }
    }

    @Override
    public void onDeactivate() {
        position = null;
        endpoint = null;
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null || CoordHider.active() != null) {
            position = null;
            return;
        }
        var dimension = mc.level.dimension();
        String name = dimension.equals(Level.OVERWORLD) ? "overworld"
            : dimension.equals(Level.NETHER) ? "nether" : dimension.equals(Level.END) ? "end" : null;
        var block = mc.player.blockPosition();
        position = name == null ? null : new MapPosition(block.getX(), block.getZ(), name);
    }

    @EventHandler
    private void onLeave(GameLeftEvent event) {
        position = null;
    }

    private void respond(HttpExchange request, String host, String path) throws IOException {
        try (request) {
            var headers = request.getRequestHeaders();
            if (!ORIGIN.equals(headers.getFirst("Origin")) || !host.equals(headers.getFirst("Host"))
                || !path.equals(request.getRequestURI().getRawPath())) {
                request.sendResponseHeaders(404, -1);
                return;
            }
            var response = request.getResponseHeaders();
            response.set("Access-Control-Allow-Origin", ORIGIN);
            response.set("Cache-Control", "no-store");
            if (request.getRequestMethod().equals("OPTIONS")) {
                response.set("Access-Control-Allow-Methods", "GET");
                response.set("Access-Control-Allow-Private-Network", "true");
                request.sendResponseHeaders(204, -1);
                return;
            }
            if (!request.getRequestMethod().equals("GET")) {
                request.sendResponseHeaders(405, -1);
                return;
            }
            MapPosition snapshot = position;
            if (snapshot == null) {
                request.sendResponseHeaders(204, -1);
                return;
            }
            byte[] body = snapshot.json().getBytes(StandardCharsets.UTF_8);
            response.set("Content-Type", "application/json");
            request.sendResponseHeaders(200, body.length);
            request.getResponseBody().write(body);
        }
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        var layout = theme.verticalList();
        layout.add(theme.button("Open Chunkbase Seed Map")).widget().action = () -> ConfirmLinkScreen.confirmLinkNow(mc.gui.screen(), URI.create(MAP_URL));
        layout.add(theme.button("Copy browser script")).widget().action = this::copyScript;
        return layout;
    }

    private void copyScript() {
        if (endpoint == null) {
            error("Enable Seed Map before copying its browser script.");
            return;
        }
        try (var input = SeedMap.class.getResourceAsStream(SCRIPT)) {
            if (input == null) {
                error("The Seed Map browser script is missing.");
                return;
            }
            String script = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            mc.keyboardHandler.setClipboard(script.replace("__FMOD_ENDPOINT__", endpoint));
        } catch (IOException exception) {
            error("Could not read the Seed Map browser script.");
        }
    }

    private record MapPosition(int x, int z, String dimension) {
        private String json() {
            return "{\"x\":" + x + ",\"z\":" + z + ",\"dimension\":\"" + dimension + "\"}";
        }
    }
}
