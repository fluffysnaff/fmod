package net.fabricmc.example.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPlayerEntity.class)
public interface ClientPlayerEntityAccessor
{
    // Set
    @Mutable
    @Accessor("lastX")
    void setLastX(double lastX);

    @Mutable
    @Accessor("lastBaseY")
    void setLastBaseY(double lastBaseY);

    @Mutable
    @Accessor("lastZ")
    void setLastZ(double lastZ);

    @Mutable
    @Accessor("lastYaw")
    void setLastYaw(float lastYaw);

    @Mutable
    @Accessor("lastPitch")
    void setLastPitch(float lastPitch);
}
