package net.fabricmc.example.mixin;

import net.fabricmc.example.Bypass;

import net.fabricmc.example.Util;
import net.fabricmc.example.Vars;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.math.BigDecimal;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin
{
    @Inject(at = @At("TAIL"), method = "send(Lnet/minecraft/network/Packet;)V", cancellable = true)
    public void send(Packet<?> packet, CallbackInfo ci)
    {
        Bypass.MovementBypass(packet, ci);
    }
}
