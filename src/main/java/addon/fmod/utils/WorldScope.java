package addon.fmod.utils;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixin.MinecraftServerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;

public final class WorldScope {
    private WorldScope() {}

    public static String current() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        Object local = mc.getSingleplayerServer();
        if (local != null) return "world:" + ((MinecraftServerAccessor) local).meteor$getStorageSource()
            .getLevelPath(LevelResource.ROOT).toAbsolutePath().normalize();
        var server = mc.getCurrentServer();
        return server == null ? null : BaseScoutExclusions.serverIdentity(server.ip);
    }

    public static Path file(String feature, String scope) {
        return BaseScoutExclusions.serverFile(MeteorClient.FOLDER.toPath().resolve("fmod-private").resolve(feature), scope);
    }
}
