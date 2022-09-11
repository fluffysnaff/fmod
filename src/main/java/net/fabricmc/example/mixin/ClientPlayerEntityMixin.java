package net.fabricmc.example.mixin;

import net.fabricmc.example.FMod;
import net.fabricmc.example.Util;
import net.fabricmc.example.Vars;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {
    @Shadow public abstract void sendMessage(Text message);

    @Inject(at = @At("TAIL"), method = "tick")
    public void tick(CallbackInfo ci) {
        FMod.tick();
    }

    @Inject(at = @At("HEAD"), method = "sendChatMessage", cancellable = true)
    public void sendChatMessage(String message, Text preview, CallbackInfo ci) {
        if (message.equalsIgnoreCase(Util.commandPrefix + "toggle boatfly"))
        {
            String udi = Vars.boatFly ? "Disabled" : "Toggled";
            Util.log(udi + " boatfly");
            Vars.boatFly = !Vars.boatFly;
            ci.cancel();
        }

        if (message.equalsIgnoreCase(Util.commandPrefix + "toggle bypass"))
        {
            String udi = Vars.bypassLo ? "Disabled" : "Toggled";
            Util.log(udi + " bypass");
            Vars.bypassLo = !Vars.bypassLo;
            ci.cancel();
        }
    }
}