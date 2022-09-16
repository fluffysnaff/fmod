package net.fabricmc.example.mixin;

import net.fabricmc.example.Bypass;

import net.fabricmc.example.FMod;
import net.fabricmc.example.Util;
import net.fabricmc.example.Vars;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
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
        if (!Vars.bypassLo)
        {
            return;
        }
        if (packet instanceof PlayerMoveC2SPacket movePacket)
        {
            var movePacketAccessor = (PlayerMoveC2SPacketAccessor) movePacket;

            double dx = movePacketAccessor.getX();
            double dz = movePacketAccessor.getZ();

            long delX = ((long)(dx * 1000)) % 10;
            long delZ = ((long)(dz * 1000)) % 10;

            dx = ((dx * 1000) - delX) / 1000;
            dz = ((dz * 1000) - delZ) / 1000;

            if ((((long) (dx * 1000)) % 10 != 0 && ((long) (dz * 1000)) % 10 != 0))
            {
                delX = ((long)(dx * 100)) % 10;
                delZ = ((long)(dz * 100)) % 10;
                dx = ((dx * 100) - delX) / 100;
                dz = ((dz * 100) - delZ) / 100;
                ci.cancel();
            }
            // What does this do?
            movePacketAccessor.setChangeLook(false);
            movePacketAccessor.setChangePosition(false);

            movePacketAccessor.setX(dx);
            movePacketAccessor.setZ(dz);

            float dp = movePacketAccessor.getPitch();
            float dy = movePacketAccessor.getYaw();

            long delP = ((long)(dp * 1000)) % 10;
            long delY = ((long)(dy * 1000)) % 10;

            dp = ((dp * 1000) - delP) / 1000;
            dy = ((dy * 1000) - delY) / 1000;

            if ((((long) (dp * 1000)) % 10 != 0 && ((long) (dy * 1000)) % 10 != 0))
            {
                delP = ((long)(dp * 100)) % 10;
                delY = ((long)(dy * 100)) % 10;
                dp = ((dp * 100) - delP) / 100;
                dy = ((dy * 100) - delY) / 100;
                ci.cancel();
            }

            movePacketAccessor.setPitch(dp);
            movePacketAccessor.setYaw(dy);
            FMod.LOGGER.info(String.format("X: %f, Z: %f, P: %f, Y: %f | DX: %f, DZ: %f, DP: %f, DY: %f", dx, dz, dp, dy, dx, dz, dp, dy));
        }

        if (packet instanceof VehicleMoveC2SPacket movePacket)
        {
            var movePacketAccessor = (VehicleMoveC2SPacketAccessor) movePacket;
            // movePacketAccessor.setChangeLook(false);
            // movePacketAccessor.setChangePosition(false);

            double dx = movePacketAccessor.getX();
            double dz = movePacketAccessor.getZ();

            long delX = ((long)(dx * 1000)) % 10;
            long delZ = ((long)(dz * 1000)) % 10;

            dx = ((dx * 1000) - delX) / 1000;
            dz = ((dz * 1000) - delZ) / 1000;

            if ((((long) (dx * 1000)) % 10 != 0 && ((long) (dz * 1000)) % 10 != 0))
            {
                delX = ((long)(dx * 100)) % 10;
                delZ = ((long)(dz * 100)) % 10;
                dx = ((dx * 100) - delX) / 100;
                dz = ((dz * 100) - delZ) / 100;
                ci.cancel();
            }
            // movePacketAccessor.setChangeLook(false);
            // movePacketAccessor.setChangePosition(false);
            movePacketAccessor.setX(dx);
            movePacketAccessor.setZ(dz);

            float dp = movePacketAccessor.getPitch();
            float dy = movePacketAccessor.getYaw();

            long delP = ((long)(dp * 1000)) % 10;
            long delY = ((long)(dy * 1000)) % 10;

            dp = ((dp * 1000) - delP) / 1000;
            dy = ((dy * 1000) - delY) / 1000;

            if ((((long) (dp * 1000)) % 10 != 0 && ((long) (dy * 1000)) % 10 != 0))
            {
                delP = ((long)(dp * 100)) % 10;
                delY = ((long)(dy * 100)) % 10;
                dp = ((dp * 100) - delP) / 100;
                dy = ((dy * 100) - delY) / 100;
                ci.cancel();
            }

            movePacketAccessor.setPitch(dp);
            movePacketAccessor.setYaw(dy);
            FMod.LOGGER.info(String.format("X: %f, Z: %f, P: %f, Y: %f | DX: %f, DZ: %f, DP: %f, DY: %f", dx, dz, dp, dy, dx, dz, dp, dy));
        }
    }
}
