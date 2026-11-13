package addon.fmod.volytra;

import java.util.Objects;
import java.util.function.DoubleSupplier;

/** One fitted HUD scale, invalidated by the caller's font, label and scale identity. Client thread only. */
public final class PanelScaleCache {
    private Object style;
    private double scale;

    public double get(Object currentStyle, DoubleSupplier fit) {
        Objects.requireNonNull(currentStyle);
        if (!currentStyle.equals(style)) {
            double fitted = fit.getAsDouble();
            if (!Double.isFinite(fitted) || fitted <= 0) throw new IllegalArgumentException("Invalid panel scale.");
            scale = fitted;
            style = currentStyle;
        }
        return scale;
    }
}
