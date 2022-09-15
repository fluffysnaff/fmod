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

        ci.cancel();
        if (packet instanceof PlayerMoveC2SPacket movePacket)
        {
            var movePacketAccessor = (PlayerMoveC2SPacketAccessor) movePacket;

            double dx = movePacketAccessor.getX();
            double dz = movePacketAccessor.getZ();

            for(int i = 5; i > 0; i--)
            {
                if ((((long) (dx * 1000)) % 10 != 0 && ((long) (dz * 1000)) % 10 != 0))
                {
                    dx = Util.round(dx, i);
                    dz = Util.round(dz, i);
                }
            }
            movePacketAccessor.setX(dx);
            movePacketAccessor.setZ(dz);

            movePacketAccessor.setPitch(0.f);
            movePacketAccessor.setYaw(0.f);
            FMod.LOGGER.info(String.format("XZPY: %f, %f, %f, %f", dx, dz, ((double)((long)dx)), ((double)((long)dz))));
        }

        // (packet instanceof VehicleMoveC2SPacket movePacket)
        //
        //  var vehiclePacketAccessor = (VehicleMoveC2SPacketAccessor) movePacket;

        //  BigDecimal x = Util.roundToDirPos(BigDecimal.valueOf(vehiclePacketAccessor.getX()), 2);
        //  BigDecimal z = Util.roundToDirPos(BigDecimal.valueOf(vehiclePacketAccessor.getZ()), 2);

        //  if ((x.longValue() * 1000) % 10 != 0 && (z.longValue() * 1000) % 10 != 0)
        //  {
        //      x = Util.roundToDirPos(x, 1);
        //      z = Util.roundToDirPos(z, 1);
        //  }

        //  if ((x.longValue() * 1000) % 10 != 0 && (z.longValue() * 1000) % 10 != 0)
        //  {
        //      x = Util.roundToDirPos(x, 0);
        //      z = Util.roundToDirPos(z, 0);
        //  }
        //  vehiclePacketAccessor.setX(x.doubleValue());
        //  vehiclePacketAccessor.setZ(z.doubleValue());

        //  BigDecimal pitch = Util.roundToDirection(BigDecimal.valueOf(vehiclePacketAccessor.getPitch()), 2);
        //  BigDecimal yaw = Util.roundToDirection(BigDecimal.valueOf(vehiclePacketAccessor.getYaw()), 2);
        //  if ((pitch.longValue() * 1000) % 10 != 0 && (yaw.longValue() * 1000) % 10 != 0)
        //  {
        //      pitch = Util.roundToDirection(pitch, 1);
        //      yaw = Util.roundToDirection(yaw, 1);
        //  }

        //  if ((pitch.longValue() * 1000) % 10 != 0 &&  (yaw.longValue() * 1000) % 10 != 0)
        //  {
        //      pitch = Util.roundToDirection(pitch, 0);
        //      yaw = Util.roundToDirection(yaw, 0);
        //  }
        //  vehiclePacketAccessor.setPitch(pitch.floatValue());
        //  vehiclePacketAccessor.setYaw(yaw.floatValue());
        //  FMod.LOGGER.info(String.format("XZPY: %f, %f, %f, %f", x.doubleValue(), z.doubleValue(), pitch.floatValue(), yaw.floatValue()));
    }
}
