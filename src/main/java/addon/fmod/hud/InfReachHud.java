package addon.fmod.hud;

import addon.fmod.FMod;
import addon.fmod.modules.InfReach;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;

public class InfReachHud extends HudElement {

    public static final HudElementInfo<InfReachHud> INFO = new HudElementInfo<>(
        FMod.HUD_GROUP,
        "InfReach Target",
        "Displays the current target of inf reach.",
        InfReachHud::new
    );

    public InfReachHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        setSize(renderer.textWidth("InfReach Target", true), renderer.textHeight(true));

        String text = "Target: ";
        InfReach infReach = Modules.get().get(InfReach.class);

        if (infReach != null && infReach.isActive()) {
            Entity targetEntity = infReach.getClosestEntity();
            if (targetEntity != null) {
                Text name = targetEntity.getDisplayName();
                if (name != null) {
                    text += name.getString();
                }
            }
        }
        renderer.text(text, x, y, Color.WHITE, true);
    }
}
