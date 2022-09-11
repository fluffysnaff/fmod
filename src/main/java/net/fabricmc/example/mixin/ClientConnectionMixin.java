package net.fabricmc.example.mixin;
import net.fabricmc.example.FMod;
import net.fabricmc.example.Util;
import net.fabricmc.example.Vars;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.text.Text;
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
            var z = Util.roundToDirection(movePacket.getZ(Double.MAX_VALUE));
            var x = Util.roundToDirection(movePacket.getX(Double.MAX_VALUE));

            if((x * 1000.0) % 10 != 0 || (z * 1000.0) % 10 != 0)
            {
                ci.cancel();
            }
            ((PlayerMoveC2SPacketAccessor) packet).setX(x);
            ((PlayerMoveC2SPacketAccessor) packet).setZ(z);

            var pitch = Util.roundToDirection(movePacket.getPitch(Float.MAX_VALUE));
            var yaw = Util.roundToDirection(movePacket.getYaw(Float.MAX_VALUE));
            if((pitch * 1000.f) % 10 != 0 || (yaw * 1000.f) % 10 != 0)
            {
                ci.cancel();
            }

            ((PlayerMoveC2SPacketAccessor) packet).setPitch(pitch);
            ((PlayerMoveC2SPacketAccessor) packet).setYaw(yaw);
            FMod.LOGGER.info(String.format("XZPY: %f, %f, %f, %f", x, z, pitch, yaw));
        }

        if (packet instanceof VehicleMoveC2SPacket movePacket)
        {
            var z = Util.roundToDirection(movePacket.getZ());
            var x = Util.roundToDirection(movePacket.getX());

            if((x * 1000.0) % 10 != 0 || (z * 1000.0) % 10 != 0)
            {
                ci.cancel();
            }
            ((VehicleMoveC2SPacketAccessor) packet).setX(x);
            ((VehicleMoveC2SPacketAccessor) packet).setZ(z);

            var pitch = Util.roundToDirection(movePacket.getPitch());
            var yaw =Util.roundToDirection( movePacket.getYaw());
            if((x * 1000) % 10 != 0 || (z * 1000) % 10 != 0)
            {
                ci.cancel();
            }

            ((VehicleMoveC2SPacketAccessor) packet).setPitch(pitch);
            ((VehicleMoveC2SPacketAccessor) packet).setYaw(yaw);
            FMod.LOGGER.info(String.format("XZPY: %f, %f, %f, %f", x, z, pitch, yaw));
        }
    }
}
