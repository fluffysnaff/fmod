package addon.fmod.hud;

import addon.fmod.FMod;
import addon.fmod.modules.BaseScout;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;

import java.util.ArrayList;
import java.util.List;

public final class BaseScoutHud extends HudElement {
    public static final HudElementInfo<BaseScoutHud> INFO = new HudElementInfo<>(FMod.HUD_GROUP,
        "base-scout", "Live BaseScout report and candidate. Added automatically when scouting starts.", BaseScoutHud::new);
    private final SettingGroup general = settings.getDefaultGroup();
    private final Setting<Double> scale = general.add(new DoubleSetting.Builder()
        .name("scale").description("Text scale.").defaultValue(1).range(0.5, 3).sliderRange(0.5, 2).build());
    private final Setting<Integer> maximumWidth = general.add(new IntSetting.Builder()
        .name("maximum-width").description("Wrap text at this width before scaling.").defaultValue(400).range(160, 1000).sliderRange(200, 700).build());
    private final Setting<Boolean> background = general.add(new BoolSetting.Builder()
        .name("background").description("Draw a translucent background for readability.").defaultValue(true).build());

    public BaseScoutHud() { super(INFO); }

    @Override public void render(HudRenderer renderer) {
        BaseScout scout = Modules.get() == null ? null : Modules.get().get(BaseScout.class);
        List<String> source;
        if (scout == null || !scout.isActive() || !scout.showsHud()) {
            if (!isInEditor()) { setSize(0, 0); return; }
            source = List.of("Base Scout: " + (scout != null && scout.isActive() ? "HUD hidden" : "stopped"),
                "Enable scouting and show-hud to display the live report.");
        } else source = scout.hudLines();

        double textScale = scale.get(), padding = 4 * textScale;
        List<String> lines = wrap(renderer, source, maximumWidth.get() * textScale, textScale);
        double lineHeight = renderer.textHeight(true, textScale) + 2 * textScale;
        double width = 0;
        for (String line : lines) width = Math.max(width, renderer.textWidth(line, true, textScale));
        setSize(width + padding * 2, lines.size() * lineHeight + padding * 2);
        if (background.get()) renderer.quad(x, y, getWidth(), getHeight(), new Color(15, 15, 20, 170));
        double lineY = y + padding;
        for (int i = 0; i < lines.size(); i++) {
            renderer.text(lines.get(i), x + padding, lineY, i == 0 ? new Color(190, 130, 255) : Color.WHITE, true, textScale);
            lineY += lineHeight;
        }
    }

    private static List<String> wrap(HudRenderer renderer, List<String> source, double width, double scale) {
        List<String> lines = new ArrayList<>();
        for (String text : source) {
            String line = "";
            for (String word : text.split("\\s+")) {
                String next = line.isEmpty() ? word : line + " " + word;
                if (!line.isEmpty() && renderer.textWidth(next, true, scale) > width) {
                    lines.add(line);
                    line = word;
                } else line = next;
            }
            if (!line.isEmpty()) lines.add(line);
        }
        return lines;
    }
}
