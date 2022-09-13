package net.fabricmc.example.mixin;
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

@Mixin(ClientConnection.class)
public class ClientConnectionMixin
{
    @Inject(at = @At("TAIL"), method = "send(Lnet/minecraft/network/Packet;)V", cancellable = true)
    public void send(Packet<?> packet, CallbackInfo ci)
    {
        if(!Vars.bypassLo)
        {
            return;
        }

        if (packet instanceof PlayerMoveC2SPacket movePacket)
        {
            var movePacketAccessor = (PlayerMoveC2SPacketAccessor) movePacket;

            double x = Util.roundToDirection(movePacketAccessor.getX(), 2.0);
            double z = Util.roundToDirection(movePacketAccessor.getZ(), 2.0);

            if((long)((x * 1000)) % 10 != 0 && (long)((z * 1000)) % 10 != 0)
            {
                x = Util.roundToDirection(movePacketAccessor.getX(), 1.0);
                z = Util.roundToDirection(movePacketAccessor.getZ(), 1.0);
            }

            if((long)((x * 1000)) % 10 != 0 && (long)((z * 1000)) % 10 != 0)
            {
                x = Util.roundToDirection(movePacketAccessor.getX(), 0);
                z = Util.roundToDirection(movePacketAccessor.getZ(), 0);
            }
            movePacketAccessor.setX(x);
            movePacketAccessor.setZ(z);

            double pitch = Util.roundToDirection(movePacketAccessor.getPitch(), 2.0);
            double yaw = Util.roundToDirection(movePacketAccessor.getYaw(), 2.0);
            if((long)((pitch * 1000)) % 10 != 0 && (long)((yaw * 1000)) % 10 != 0)
            {
                pitch = Util.roundToDirection(movePacketAccessor.getPitch(), 1.0);
                yaw = Util.roundToDirection(movePacketAccessor.getYaw(), 1.0);
            }

            if((long)((pitch * 1000)) % 10 != 0 && (long)((yaw * 1000)) % 10 != 0)
            {
                pitch = Util.roundToDirection(movePacketAccessor.getPitch(), 0);
                yaw = Util.roundToDirection(movePacketAccessor.getYaw(), 0);
            }
            movePacketAccessor.setPitch((float)pitch);
            movePacketAccessor.setYaw((float)yaw);
            FMod.LOGGER.info(String.format("XZPY: %f, %f, %f, %f", x, z, pitch, yaw));
        }

        if (packet instanceof VehicleMoveC2SPacket movePacket)
        {
            var vehiclePacketAccessor = (VehicleMoveC2SPacketAccessor) movePacket;

            double x = Util.roundToDirection(vehiclePacketAccessor.getX(), 2.0);
            double z = Util.roundToDirection(vehiclePacketAccessor.getZ(), 2.0);

            if((long)(x * 1000) % 10 != 0 && (long)(z * 1000) % 10 != 0)
            {
                x = Util.roundToDirection(vehiclePacketAccessor.getX(), 1.0);
                z = Util.roundToDirection(vehiclePacketAccessor.getZ(), 1.0);
            }
            vehiclePacketAccessor.setX(x);
            vehiclePacketAccessor.setZ(z);

            double pitch = Util.roundToDirection(vehiclePacketAccessor.getPitch(), 2.0);
            double yaw = Util.roundToDirection(vehiclePacketAccessor.getYaw(), 2.0);
            if(((long)(pitch * 1000)) % 10 != 0 && ((long)(yaw * 1000)) % 10 != 0)
            {
                pitch = Util.roundToDirection(vehiclePacketAccessor.getPitch(), 1.0);
                yaw = Util.roundToDirection(vehiclePacketAccessor.getYaw(), 1.0);
            }

            vehiclePacketAccessor.setPitch((float)pitch);
            vehiclePacketAccessor.setYaw((float)yaw);
            FMod.LOGGER.info(String.format("XZPY: %f, %f, %f, %f", x, z, pitch, yaw));
        }
    }
}
