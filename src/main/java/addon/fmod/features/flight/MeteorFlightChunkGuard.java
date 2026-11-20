package addon.fmod.features.flight;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Flight;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.Minecraft;

/** Extends Meteor Flight through its settings API and final player-move event. */
public final class MeteorFlightChunkGuard {
    private final Flight flight;
    private final Setting<Boolean> noUnloadedChunks;

    private MeteorFlightChunkGuard(Flight flight) {
        this.flight = flight;
        noUnloadedChunks = flight.settings.getDefaultGroup().add(new BoolSetting.Builder()
            .name("no-unloaded-chunks")
            .description("FMod: pause horizontal flight before your body enters an unloaded chunk; resume when it loads.")
            .defaultValue(true)
            .build());
    }

    public static void register() {
        Flight flight = Modules.get().get(Flight.class);
        if (flight != null) MeteorClient.EVENT_BUS.subscribe(new MeteorFlightChunkGuard(flight));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void move(PlayerMoveEvent event) {
        if (!flight.isActive() || !noUnloadedChunks.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.player.isPassenger()) return;
        var movement = event.movement;
        if (movement.x == 0 && movement.z == 0) return;
        var box = mc.player.getBoundingBox();
        double fraction = LoadedChunkPath.maxFraction(box.minX, box.minZ, box.maxX, box.maxZ, movement.x, movement.z, mc.level.getChunkSource()::hasChunk);
        if (fraction >= 1.0) return;
        // Glide to the loaded frontier instead of hard-stopping a chunk early.
        ((IVec3) movement).meteor$setXZ(movement.x * fraction, movement.z * fraction);
        var velocity = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(velocity.x * fraction, velocity.y, velocity.z * fraction);
    }
}
