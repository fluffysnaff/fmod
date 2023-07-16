package com.addon.fmod.mixin;

import com.addon.fmod.modules.LOPackets;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import meteordevelopment.meteorclient.systems.modules.Modules;

import java.util.Objects;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    // Normalize WorldBorder
    @ModifyArgs(method = "onWorldBorderInitialize", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/border/WorldBorder;setSize(D)V"))
    private void setSize(Args args) {  // Set radius to default 30 million
        if ((Modules.get().get(LOPackets.class)).getWorldBorder().get()){
            args.set(0, 30000000.0D);  // radius
        }
    }
    @ModifyArgs(method = "onWorldBorderInitialize", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/border/WorldBorder;setCenter(DD)V"))
    private void setCenter(Args args) {  // Set center to 0 0
        if ((Modules.get().get(LOPackets.class)).getWorldBorder().get()) {
            args.set(0, 0.0D);  // x
            args.set(1, 0.0D);  // z
        }
    }

    // Disable demo popup, end credits and creative mode
    @Redirect(method = "onGameStateChange", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/GameStateChangeS2CPacket;getReason()Lnet/minecraft/network/packet/s2c/play/GameStateChangeS2CPacket$Reason;"))
    private GameStateChangeS2CPacket.Reason getReason(GameStateChangeS2CPacket instance) {
        GameStateChangeS2CPacket.Reason reason = instance.getReason();
        // Demo popup
        if (reason.equals(GameStateChangeS2CPacket.DEMO_MESSAGE_SHOWN) && (Modules.get().get(LOPackets.class)).getDemoPopup().get())
            return null;

        // Creative mode
        if (reason.equals(GameStateChangeS2CPacket.GAME_MODE_CHANGED) && (Modules.get().get(LOPackets.class)).getCreativeMode().get())
            return null;

        // End credits (still send respawn packet)
        if (reason.equals(GameStateChangeS2CPacket.GAME_WON) && (Modules.get().get(LOPackets.class)).getEndCredits().get()) {
            Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.PERFORM_RESPAWN));
            return null;
        }
        return reason;
    }
}
