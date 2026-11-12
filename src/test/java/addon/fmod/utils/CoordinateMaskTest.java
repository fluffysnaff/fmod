package addon.fmod.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CoordinateMaskTest {
    @Test void masksSignedTriplesAndTargetLocations() {
        assertEquals("Found stash at [hidden]", CoordinateMask.redact("Found stash at -12345, 63, 90876"));
        assertEquals("[hidden]", CoordinateMask.redact("-12.25 / 64.0 / 900.75"));
        assertEquals("Targeted Block: [hidden]", CoordinateMask.redact("Targeted Block: -20, 64, 123"));
        assertEquals("XYZ: [hidden]", CoordinateMask.redact("XYZ: -1,234.00 / 64.00 / 9,876.00"));
    }

    @Test void masksPairsWithLabelsAndIndividualAxes() {
        assertEquals("Coordinates: [hidden]", CoordinateMask.redact("Coordinates: 123 -456"));
        assertEquals("Found stash at [hidden].", CoordinateMask.redact("Found stash at -12345, 90876."));
        assertEquals("X: [hidden] Z: [hidden]", CoordinateMask.redact("X: 123 Z: -456"));
        assertEquals("Chunk: [hidden]", CoordinateMask.redact("Chunk: 12 4 -8 in 3 2 1"));
    }

    @Test void keepsNonLocationTextAndHandlesEmptyText() {
        String stats = "Minecraft 26.3 | FPS: 144 | TPS: 20.0 | Entities: 7";
        assertEquals(stats, CoordinateMask.redact(stats));
        assertEquals("minecraft:stone", CoordinateMask.redact("minecraft:stone"));
        assertEquals("", CoordinateMask.redact(""));
        assertNull(CoordinateMask.redact(null));
    }

    @Test void recognisesWholeAndSplitMeteorPositionTemplates() {
        for (String template : new String[] {"{player.pos}", "Y: {player.pos.y}",
            "{round(camera.opposite_dim_pos.x, 2)}", "{crosshair_target.value.pos}"}) {
            assertTrue(CoordinateMask.containsCoordinateTemplate(template), template);
        }
        assertFalse(CoordinateMask.containsCoordinateTemplate("FPS: {fps} | {player.main_hand}"));
        assertFalse(CoordinateMask.containsCoordinateTemplate(null));
    }
}
