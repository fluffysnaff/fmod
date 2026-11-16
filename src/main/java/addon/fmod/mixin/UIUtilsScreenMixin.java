package addon.fmod.mixin;

import addon.fmod.uiutils.UIUtilsWidgets;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class UIUtilsScreenMixin {
    @Shadow protected abstract <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T widget);
    @Shadow protected abstract void removeWidget(GuiEventListener widget);
    @Unique private final java.util.List<AbstractWidget> fmod$uiUtilsWidgets = new java.util.ArrayList<>();

    @Inject(method = { "init(II)V", "rebuildWidgets", "resize" }, at = @At("TAIL"))
    private void fmod$uiUtilsWidgets(CallbackInfo info) {
        fmod$uiUtilsWidgets.forEach(this::removeWidget);
        fmod$uiUtilsWidgets.clear();
        UIUtilsWidgets.attach((Screen) (Object) this, widget -> {
            addRenderableWidget(widget);
            fmod$uiUtilsWidgets.add(widget);
        });
    }
}
