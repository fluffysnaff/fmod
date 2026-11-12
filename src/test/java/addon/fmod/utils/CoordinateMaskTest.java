package addon.fmod.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CoordinateMaskTest {
    private final DisplayCoordinates fake = new DisplayCoordinates(-800, 72, 1600);
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

    @Test void replacesCoordinateTextWithOneCoherentSyntheticLocation() {
        assertEquals("XYZ: -800, 72, 1600", CoordinateMask.randomize("XYZ: 123.5 / 64 / -321.25", fake));
        assertEquals("Chunk: -50 4 100", CoordinateMask.randomize("Chunk: 123 4 999 [3 4 in r.2.3.mca]", fake));
        assertEquals("X: -800 Y: 72 Z: 1600", CoordinateMask.randomize("X: 123 Y: 64 Z: -321", fake));
        assertEquals("Found stash at -800, 1600.", CoordinateMask.randomize("Found stash at 123, -321.", fake));
        assertEquals("At -800 72 1600", CoordinateMask.randomize("At 123 64 -321", fake));
    }

    @Test void repeatedRendererLayersKeepFakeAndOppositeCoordinatesStable() {
        for (String value : new String[]{"XYZ: 123 / 64 / -321", "At 123 64 -321", "X: 123 Y: 64 Z: -321",
            "Found stash at 123, -321.", "-100 72 200", "-6400 / 72 / 12800", "Chunk: -50 4 100"}) {
            String once = CoordinateMask.randomize(value, fake);
            assertEquals(once, CoordinateMask.randomize(once, fake), value);
        }
        assertEquals("-100 72 200", CoordinateMask.randomize("-100 72 200", fake));
        assertEquals("Minecraft 26.3 | FPS: 144", CoordinateMask.randomize("Minecraft 26.3 | FPS: 144", fake));
    }

    @Test void replacesHudPositionReadsBeforeEvaluationIncludingSplitAxes() {
        assertEquals("X: {(-800)} Y: {(72)} Z: {(1600)}", CoordinateMask.randomizeTemplate(
            "X: {player.pos.x} Y: {player.pos.y} Z: {player.pos.z}", fake, false));
        assertEquals("{round((-100), 2)} | {\"-800, 72, 1600\"}", CoordinateMask.randomizeTemplate(
            "{round(camera.opposite_dim_pos.x, 2)} | {crosshair_target.value.pos}", fake, false));
        assertEquals("{(-6400)}", CoordinateMask.randomizeTemplate("{player.opposite_dim_pos.x}", fake, true));
        assertEquals("FPS: {fps}", CoordinateMask.randomizeTemplate("FPS: {fps}", fake, false));
        assertTrue(CoordinateMask.containsCoordinateTemplate(CoordinateMask.randomizeTemplate("{player['pos'].x}", fake, false)));
    }

    @Test void protectsScientificNotationAndLeadingDecimals() {
        assertEquals("([hidden])", CoordinateMask.redact("(1.2345E7, 64.0, -2.5e6)"));
        assertEquals("(-800, 72, 1600)", CoordinateMask.randomize("(1.2345E7, 64.0, -2.5e6)", fake));
        assertEquals("Coordinates: -800, 72, 1600", CoordinateMask.randomize("Coordinates: .5 -.25", fake));
        assertEquals("X: -800", CoordinateMask.randomize("X: 1e7", fake));
    }
}
