package addon.fmod.mixin;

import addon.fmod.modules.EventlessFly;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(Entity.class)
public abstract class EventlessFlyMixin {
    @ModifyVariable(method = "move", at = @At(value = "FIELD",
        target = "Lnet/minecraft/world/entity/Entity;noPhysics:Z", ordinal = 0), argsOnly = true)
    private Vec3 fmod$flightMovement(Vec3 movement, MoverType type) {
        if (type != MoverType.SELF || (Object) this != mc.player || mc.player == null) return movement;
        var modules = Modules.get();
        EventlessFly fly = modules == null ? null : modules.get(EventlessFly.class);
        return fly != null && fly.isActive() ? fly.movement(mc.player, movement) : movement;
    }
}
