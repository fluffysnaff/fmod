package com.addon.fmod.mixin;

import com.addon.fmod.FMod;
import com.addon.fmod.modules.LiveWalk;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import meteordevelopment.meteorclient.systems.modules.Modules;

@Mixin(PlayerMoveC2SPacket.PositionAndOnGround.class)
public class PlayerPositionPacketMixin {
    // Livewalk
    @ModifyArgs(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/c2s/play/PlayerMoveC2SPacket;<init>(DDDFFZZZ)V"))
    private static void init(Args args) {
        if ((Modules.get().get(LiveWalk.class)).isActive()) {
            FMod.roundOnPositionPacket(args);
        }
    }

}
