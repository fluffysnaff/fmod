package addon.fmod.mixin;

import addon.fmod.modules.StasisCourier;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Meteor's saved-module loader uses exact names, not the module's search aliases. */
@Mixin(value=Modules.class,remap=false)
public abstract class LegacyStashMoverNameMixin {
    @Shadow public abstract <T extends Module> T get(Class<T> type);

    @Inject(method="get(Ljava/lang/String;)Lmeteordevelopment/meteorclient/systems/modules/Module;",at=@At("RETURN"),cancellable=true)
    private void legacyName(String name,CallbackInfoReturnable<Module> result){
        if(result.getReturnValue()==null&&(name.equalsIgnoreCase("stasis-courier")||name.equalsIgnoreCase("stashmover")))result.setReturnValue(get(StasisCourier.class));
    }
}
