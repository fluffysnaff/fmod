package addon.fmod.mixin;

import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(VehicleMoveC2SPacket.class)
public interface VehicleMoveC2SPacketAccessor {

    // Accessor for the 'position' Vec3d field
    @Accessor("position") // <-- Target the 'position' field
    Vec3d getPosition(); // <-- Return type is Vec3d

    // Setter for the 'position' Vec3d field
    @Mutable // Records have final fields, so mutable is needed
    @Accessor("position") // <-- Target the 'position' field
    void setPosition(Vec3d position); // <-- Parameter type is Vec3d
}
