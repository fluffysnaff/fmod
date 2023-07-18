package com.addon.fmod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import com.addon.fmod.FMod;
import com.addon.fmod.modules.LiveWalk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import meteordevelopment.meteorclient.systems.modules.Modules;


@Mixin(VehicleMoveC2SPacket.class)
public class VehicleMovePacketMixin {
    // Livewalk for X
    @Redirect(method = "<init>(Lnet/minecraft/entity/Entity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getX()D"))
    public double getX(Entity instance){
        if ((Modules.get().get(LiveWalk.class)).vehicleEnabled()) {
            return FMod.roundCoordOnPositionPacket(instance.getX());
        }
        else {
            return instance.getX();
        }
    }
    // Livewalk for Z
    @Redirect(method = "<init>(Lnet/minecraft/entity/Entity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getZ()D"))
    public double getZ(Entity instance) {
        if ((Modules.get().get(LiveWalk.class)).vehicleEnabled()) {
            return FMod.roundCoordOnPositionPacket(instance.getZ());
        } else {
            return instance.getZ(); // This is X in LO mod
        }
    }
}
