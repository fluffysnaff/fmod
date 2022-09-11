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
        // FMod.LOGGER.info(packet.getClass().getName());
        if(!Vars.bypassLo) {
            return;
        }

        if (packet instanceof PlayerMoveC2SPacket movePacket)
        {
            double z = movePacket.getZ(Double.MAX_VALUE);
            double x = movePacket.getX(Double.MAX_VALUE);
            if(x == Double.MAX_VALUE || z == Double.MAX_VALUE)
            {
                // FMod.LOGGER.info("XZ was undef");
                ci.cancel();
            }

            float pitch = movePacket.getPitch(Float.MAX_VALUE);
            float yaw = movePacket.getYaw(Float.MAX_VALUE);
            if(pitch == Float.MAX_VALUE || yaw == Float.MAX_VALUE)
            {
                // FMod.LOGGER.info("XZ was undef py");
                ci.cancel();
            }

            if((x * 1000) % 10 != 0 || (z * 1000) % 10 != 0)
            {
                ci.cancel();
            }

            x = Util.roundToDirection(x);
            z = Util.roundToDirection(z);

            pitch = Util.roundToDirection(pitch);
            yaw = Util.roundToDirection(yaw);

            if((x * 1000) % 10 != 0 || (z * 1000) % 10 != 0) {
                ci.cancel();
            }

            ((PlayerMoveC2SPacketAccessor) packet).setX(Util.roundToDirection(x));
            ((PlayerMoveC2SPacketAccessor) packet).setZ(Util.roundToDirection(z));

            ((PlayerMoveC2SPacketAccessor) packet).setPitch(Util.roundToDirection(pitch));
            ((PlayerMoveC2SPacketAccessor) packet).setYaw(Util.roundToDirection(yaw));
            FMod.LOGGER.info(String.format("Fuck you: %f, %f, %f, %f", Util.roundToDirection(x), Util.roundToDirection(z), Util.roundToDirection(pitch), Util.roundToDirection(yaw)));
        }

        if (packet instanceof VehicleMoveC2SPacket movePacket)
        {
            double z = movePacket.getZ();
            double x = movePacket.getX();
            float pitch = movePacket.getPitch();
            float yaw = movePacket.getYaw();

            if((x * 1000) % 10 != 0 || (z * 1000) % 10 != 0) {
                ci.cancel();
            }

            x = Util.roundToDirection(x);
            z = Util.roundToDirection(z);

            pitch = Util.roundToDirection(pitch);
            yaw = Util.roundToDirection(yaw);

            if((x * 1000) % 10 != 0 || (z * 1000) % 10 != 0) {
                ci.cancel();
            }

            ((VehicleMoveC2SPacketAccessor) packet).setX(Util.roundToDirection(x));
            ((VehicleMoveC2SPacketAccessor) packet).setZ(Util.roundToDirection(z));

            ((VehicleMoveC2SPacketAccessor) packet).setPitch(Util.roundToDirection(pitch));
            ((VehicleMoveC2SPacketAccessor) packet).setYaw(Util.roundToDirection(yaw));
            FMod.LOGGER.info(String.format("Fuck you on a vehicle: %f, %f, %f, %f", Util.roundToDirection(x), Util.roundToDirection(z), Util.roundToDirection(pitch), Util.roundToDirection(yaw)));
        }
    }
}
