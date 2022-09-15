package net.fabricmc.example;

import net.fabricmc.example.mixin.PlayerMoveC2SPacketAccessor;
import net.fabricmc.example.mixin.VehicleMoveC2SPacketAccessor;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.math.BigDecimal;

public class Bypass
{
    public static void MovementBypass(Packet<?> packet, CallbackInfo ci)
    {
        return;
    }
}
