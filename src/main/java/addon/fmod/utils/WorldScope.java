package addon.fmod.utils;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixin.MinecraftServerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

public final class WorldScope {
    private WorldScope() {}

    public static String current() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        Object local = mc.getSingleplayerServer();
        if (local != null) return "world:" + ((MinecraftServerAccessor) local).meteor$getStorageSource()
            .getLevelPath(LevelResource.ROOT).toAbsolutePath().normalize();
        var server = mc.getCurrentServer();
        return server == null ? null : serverIdentity(server.ip);
    }

    public static Path file(String feature, String scope) {
        return serverFile(MeteorClient.FOLDER.toPath().resolve("fmod-private").resolve(feature), scope);
    }

    private static Path serverFile(Path directory, String identity) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8));
            return directory.resolve(HexFormat.of().formatHex(hash) + ".json");
        } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 unavailable.", exception); }
    }

    private static String serverIdentity(String address) {
        String normalized = address.strip().toLowerCase(Locale.ROOT);
        if (normalized.endsWith(":25565")) normalized = normalized.substring(0, normalized.length() - 6);
        if (normalized.endsWith(".")) normalized = normalized.substring(0, normalized.length() - 1);
        return "server:" + normalized;
    }
}
