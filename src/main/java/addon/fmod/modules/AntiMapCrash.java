package addon.fmod.modules;

import addon.fmod.FMod;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;

/**
 * Defends against map "lag machine" crashes (many filled maps in item frames).
 *
 * <p>Each unique filled map the client receives becomes a 128x128 dynamic GPU
 * texture; rendering hundreds or thousands at once exhausts memory and crashes the
 * client. This cancels incoming {@link ClientboundMapItemDataPacket}s before the
 * colours are stored, so no texture is ever built. Affected maps render blank. Enable
 * it before approaching the area (or while away, then reconnect, if it crashes you on
 * login). It is client-side only and cannot affect the server.
 */
public class AntiMapCrash extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> notify = sgGeneral.add(new BoolSetting.Builder()
        .name("notify")
        .description("Log a chat message the first time map data is blocked after enabling.")
        .defaultValue(false)
        .build()
    );

    private int blocked;
    private boolean announced;

    public AntiMapCrash() {
        super(FMod.CATEGORY, "anti-map-crash", "Cancels incoming map-image packets so map lag machines can't build thousands of textures and crash your client.");
    }

    @Override
    public void onActivate() {
        blocked = 0;
        announced = false;
    }

    @EventHandler
    private void onReceive(PacketEvent.Receive event) {
        if (!(event.packet instanceof ClientboundMapItemDataPacket)) return;
        event.cancel();
        blocked++;
        if (notify.get() && !announced) {
            announced = true;
            info("Blocking map data from loading. Affected maps will appear blank.");
        }
    }

    @Override
    public String getInfoString() {
        return blocked + " blocked";
    }
}
