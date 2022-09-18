package net.fabricmc.example;

import net.fabricmc.example.mixin.ClientPlayerEntityAccessor;
import net.fabricmc.example.mixin.PlayerMoveC2SPacketAccessor;
import net.fabricmc.example.mixin.VehicleMoveC2SPacketAccessor;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class Bypass
{
    public static void MovementBypass(Packet<?> packet, CallbackInfo ci)
    {
        if(!Vars.bypassLo)
            return;
        if (packet instanceof PlayerMoveC2SPacket movePacket)
        {
            var movePacketAccessor = (PlayerMoveC2SPacketAccessor) movePacket;

            double dx = movePacketAccessor.getX();
            double dz = movePacketAccessor.getZ();

            dx = Util.round(dx, 3);
            dz = Util.round(dz, 3);

            if ((((long) (dx * 1000)) % 10 != 0 && ((long) (dz * 1000)) % 10 != 0))
            {
                dx = Util.round(dx, 2);
                dz = Util.round(dz, 2);
                ci.cancel();
            }

            if ((((long) (dx * 1000)) % 10 != 0 && ((long) (dz * 1000)) % 10 != 0))
            {
                dx = Util.round(dx, 1);
                dz = Util.round(dz, 1);
                ci.cancel();
            }

            if ((((long) (dx * 1000)) % 10 != 0 && ((long) (dz * 1000)) % 10 != 0))
            {
                dx = Util.round(dx, 0);
                dz = Util.round(dz, 0);
                ci.cancel();
            }

            // What does this do?
            movePacketAccessor.setChangeLook(false);
            movePacketAccessor.setChangePosition(false);

            movePacketAccessor.setX(dx);
            movePacketAccessor.setZ(dz);
            FMod.LOGGER.info(String.format("X: %f, Z: %f | DX: %f, DZ: %f", movePacketAccessor.getX(), movePacketAccessor.getZ(), dx, dz));
        }

        if (packet instanceof VehicleMoveC2SPacket movePacket)
        {
            var movePacketAccessor = (VehicleMoveC2SPacketAccessor) movePacket;

            double dx = movePacketAccessor.getX();
            double dz = movePacketAccessor.getZ();

            dx = Util.round(dx, 3);
            dz = Util.round(dz, 3);

            if ((((long) (dx * 1000)) % 10 != 0 && ((long) (dz * 1000)) % 10 != 0))
            {
                dx = Util.round(dx, 2);
                dz = Util.round(dz, 2);
                ci.cancel();
            }

            if ((((long) (dx * 1000)) % 10 != 0 && ((long) (dz * 1000)) % 10 != 0))
            {
                dx = Util.round(dx, 1);
                dz = Util.round(dz, 1);
                ci.cancel();
            }

            if ((((long) (dx * 1000)) % 10 != 0 && ((long) (dz * 1000)) % 10 != 0))
            {
                dx = Util.round(dx, 0);
                dz = Util.round(dz, 0);
                ci.cancel();
            }

            movePacketAccessor.setX(dx);
            movePacketAccessor.setZ(dz);
            FMod.LOGGER.info(String.format("X: %f, Z: %f | DX: %f, DZ: %f", movePacketAccessor.getX(), movePacketAccessor.getZ(), dx, dz));
        }
    }

    public static void WGBypass(Packet<?> packet, CallbackInfo ci)
    {
        if(!Vars.bypassWg)
            return;
        if (packet instanceof ClientPlayerEntity movePacket)
        {
            var packetAccessor = (ClientPlayerEntityAccessor) movePacket;
            assert Util.CLIENT.player != null;
            packetAccessor.setLastX(Util.CLIENT.player.getX());
            packetAccessor.setLastBaseY(Util.CLIENT.player.getY());
            packetAccessor.setLastZ(Util.CLIENT.player.getZ());
            packetAccessor.setLastPitch(Util.CLIENT.player.getPitch());
            packetAccessor.setLastYaw(Util.CLIENT.player.getYaw());
        }
    }
}
