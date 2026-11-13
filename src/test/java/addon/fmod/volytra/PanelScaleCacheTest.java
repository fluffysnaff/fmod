package addon.fmod.volytra;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class PanelScaleCacheTest {
    private record Style(String font, double scale) {}

    @Test void repeatedFramesWithTheSameStyleDoNotFitFontsAgain() {
        var cache = new PanelScaleCache();
        var fits = new AtomicInteger();
        for (int frame = 0; frame < 100; frame++) {
            assertEquals(1.5, cache.get(new Style("font", 1), () -> { fits.incrementAndGet(); return 1.5; }));
        }
        assertEquals(1, fits.get());
    }

    @Test void fontAndDialScaleChangesInvalidateTheLayout() {
        var cache = new PanelScaleCache();
        var fits = new AtomicInteger();
        for (var style : new Style[]{new Style("font-a", 1), new Style("font-b", 1), new Style("font-b", 2)}) {
            cache.get(style, () -> { fits.incrementAndGet(); return 1; });
        }
        assertEquals(3, fits.get());
    }

    @Test void failedFittingDoesNotReplaceTheLastValidLayout() {
        var cache = new PanelScaleCache();
        cache.get("valid", () -> 1);
        assertThrows(IllegalArgumentException.class, () -> cache.get("invalid", () -> Double.NaN));
        assertEquals(1, cache.get("valid", () -> { fail("Should retain the valid layout"); return 2; }));
    }
}
