package addon.fmod.mixin;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerMoveC2SPacket.class)
public interface PlayerMoveC2SPacketAccessor {
    // Accessor for the 'x' field
    @Accessor("x")
    double getPacketX(); // Method to get the value

    @Mutable // Allows modification of a final field
    @Accessor("x")
    void setPacketX(double x); // Method to set the value

    // Accessor for the 'z' field
    @Accessor("z")
    double getPacketZ();

    @Mutable
    @Accessor("z")
    void setPacketZ(double z);
}
