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
        if (!Vars.bypassLo)
        {
            return;
        }

        if (packet instanceof PlayerMoveC2SPacket movePacket)
        {
            var movePacketAccessor = (PlayerMoveC2SPacketAccessor) movePacket;

            BigDecimal x = Util.roundToDirection(BigDecimal.valueOf(movePacketAccessor.getX()), 2);
            BigDecimal z = Util.roundToDirection(BigDecimal.valueOf(movePacketAccessor.getZ()), 2);

            if ((x.longValue() * 1000) % 10 != 0 && (z.longValue() * 1000) % 10 != 0)
            {
                x = Util.roundToDirection(x, 1);
                z = Util.roundToDirection(z, 1);
            }
            if ((x.longValue() * 1000) % 10 != 0 && (z.longValue() * 1000) % 10 != 0)
            {
                x = Util.roundToDirection(x, 0);
                z = Util.roundToDirection(z, 0);
            }
            if ((x.longValue() * 1000) % 10 != 0 && (z.longValue() * 1000) % 10 != 0)
            {
                ci.cancel();
                return;
            }
            movePacketAccessor.setX(x.doubleValue());
            movePacketAccessor.setZ(z.doubleValue());

            BigDecimal pitch = Util.roundToDirection(BigDecimal.valueOf(movePacketAccessor.getPitch()), 2);
            BigDecimal yaw = Util.roundToDirection(BigDecimal.valueOf(movePacketAccessor.getYaw()), 2);
            if ((pitch.longValue() * 1000) % 10 != 0 && (yaw.longValue() * 1000) % 10 != 0)
            {
                pitch = Util.roundToDirection(pitch, 1);
                yaw = Util.roundToDirection(yaw, 1);
            }
            FMod.LOGGER.info(String.format("1PY: %f, %f", pitch, yaw));
            if ((pitch.longValue() * 1000) % 10 != 0 && (yaw.longValue() * 1000) % 10 != 0)
            {
                pitch = Util.roundToDirection(pitch, 0);
                yaw = Util.roundToDirection(yaw, 0);
            }
            if ((pitch.longValue() * 1000) % 10 != 0 && (yaw.longValue() * 1000) % 10 != 0)
            {
                ci.cancel();
                return;
            }
            movePacketAccessor.setPitch(pitch.floatValue());
            movePacketAccessor.setYaw(yaw.floatValue());
            FMod.LOGGER.info(String.format("2PY: %f, %f", pitch, yaw));
        }

        if (packet instanceof VehicleMoveC2SPacket movePacket)
        {
            var vehiclePacketAccessor = (VehicleMoveC2SPacketAccessor) movePacket;

            BigDecimal x = Util.roundToDirection(BigDecimal.valueOf(vehiclePacketAccessor.getX()), 2);
            BigDecimal z = Util.roundToDirection(BigDecimal.valueOf(vehiclePacketAccessor.getZ()), 2);

            if ((x.longValue() * 1000) % 10 != 0 && (z.longValue() * 1000) % 10 != 0)
            {
                x = Util.roundToDirection(x, 1);
                z = Util.roundToDirection(z, 1);
            }

            if ((x.longValue() * 1000) % 10 != 0 && (z.longValue() * 1000) % 10 != 0)
            {
                x = Util.roundToDirection(x, 0);
                z = Util.roundToDirection(z, 0);
            }
            vehiclePacketAccessor.setX(x.doubleValue());
            vehiclePacketAccessor.setZ(z.doubleValue());

            BigDecimal pitch = Util.roundToDirection(BigDecimal.valueOf(vehiclePacketAccessor.getPitch()), 2);
            BigDecimal yaw = Util.roundToDirection(BigDecimal.valueOf(vehiclePacketAccessor.getYaw()), 2);
            if ((pitch.longValue() * 1000) % 10 != 0 && (yaw.longValue() * 1000) % 10 != 0)
            {
                pitch = Util.roundToDirection(pitch, 1);
                yaw = Util.roundToDirection(yaw, 1);
            }

            if ((pitch.longValue() * 1000) % 10 != 0 &&  (yaw.longValue() * 1000) % 10 != 0)
            {
                pitch = Util.roundToDirection(pitch, 0);
                yaw = Util.roundToDirection(yaw, 0);
            }
            vehiclePacketAccessor.setPitch(pitch.floatValue());
            vehiclePacketAccessor.setYaw(yaw.floatValue());
            FMod.LOGGER.info(String.format("XZPY: %f, %f, %f, %f", x, z, pitch, yaw));
        }
    }
}
