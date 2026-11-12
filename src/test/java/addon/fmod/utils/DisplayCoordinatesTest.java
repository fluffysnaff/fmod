package addon.fmod.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DisplayCoordinatesTest {
    @Test void generatesBoundedSyntheticLocationsWithoutPlayerInput() {
        for (int i = 0; i < 64; i++) {
            DisplayCoordinates coordinates = DisplayCoordinates.generate();
            assertTrue(Math.abs(coordinates.x()) <= 3_000_000);
            assertTrue(Math.abs(coordinates.z()) <= 3_000_000);
            assertTrue(coordinates.y() >= -64 && coordinates.y() < 320);
            assertEquals(0, coordinates.x() % 8);
            assertEquals(0, coordinates.z() % 8);
        }
    }

    @Test void derivesNegativeChunksAndOppositeDimensionsConsistently() {
        DisplayCoordinates coordinates = new DisplayCoordinates(-808, 72, 1608);
        assertEquals(new DisplayCoordinates(-51, 4, 100), coordinates.chunk());
        assertEquals(new DisplayCoordinates(-101, 72, 201), coordinates.opposite(false));
        assertEquals(new DisplayCoordinates(-6464, 72, 12864), coordinates.opposite(true));
        assertEquals(-808, coordinates.axis('X'));
        assertTrue(coordinates.debugLines().contains("Chunk: -51 4 100"));
    }
}
