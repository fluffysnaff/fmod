package addon.fmod.volytra;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightEnvelopeTest {
    @Test void rampsTowardThreeHundredWithoutExceedingTheConfiguredCap() {
        double speed = 2.999;
        for (int tick = 0; tick < 300; tick++) {
            double next = FlightEnvelope.next(speed, 0.3, 14.999, 14.999);
            assertTrue(next >= speed && next <= 14.999);
            speed = next;
        }
        assertTrue(speed * 20 > 299 && speed * 20 < 300);
    }

    @Test void honorsReducedCapsAndDoesNotAccelerateAboveThePlateau() {
        assertEquals(2.9, FlightEnvelope.next(12, 0.3, 15, 2.9));
        assertEquals(12, FlightEnvelope.next(12, 0.3, 10, 15));
    }

    @Test void rejectsNonFiniteAndInvalidEnvelopeInputs() {
        assertThrows(IllegalArgumentException.class, () -> FlightEnvelope.next(Double.NaN, 0.3, 15, 15));
        assertThrows(IllegalArgumentException.class, () -> FlightEnvelope.next(3, 0.3, 0, 15));
        assertThrows(IllegalArgumentException.class, () -> FlightEnvelope.next(3, Double.POSITIVE_INFINITY, 15, 15));
    }
}
