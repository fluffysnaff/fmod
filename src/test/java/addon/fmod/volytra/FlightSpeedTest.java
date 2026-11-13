package addon.fmod.volytra;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightSpeedTest {
    @Test void measuresRealElapsedTimeInsteadOfAssumingTwentyTicksPerSecond() {
        FlightSpeed speed = new FlightSpeed();
        assertEquals(0, speed.sample(0, 0, 0));
        assertEquals(150, speed.sample(100_000_000, 15, 0));
        assertEquals(100, speed.sample(300_000_000, 30, 0));
    }

    @Test void returningToTheStartDoesNotCountAsNetProgress() {
        FlightSpeed speed = new FlightSpeed();
        speed.sample(0, 0, 0);
        speed.sample(100_000_000, 20, 0);
        assertEquals(0, speed.sample(200_000_000, 0, 0));
    }

    @Test void resettingDoesNotBridgeAWorldChangeOrTeleport() {
        FlightSpeed speed = new FlightSpeed();
        speed.sample(0, 0, 0);
        speed.sample(100_000_000, 20, 0);
        speed.reset();
        assertEquals(0, speed.sample(200_000_000, 10000, 10000));
        assertEquals(20, speed.sample(300_000_000, 10002, 10000));
    }
}
