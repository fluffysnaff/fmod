package addon.fmod.modules.courier;

import addon.fmod.modules.courier.CourierSafety.Position;
import addon.fmod.modules.courier.CourierSafety.ReturnCheck;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CourierSafetyTest {
    @Test void allowsOnlyTheFullyVerifiedEmptyReturn() {
        assertTrue(new ReturnCheck(true, true, true, true, true, true, true, true, true).mayDie());
        for (int missing = 0; missing < 9; missing++) {
            boolean[] facts = {true, true, true, true, true, true, true, true, true};
            facts[missing] = false;
            assertFalse(new ReturnCheck(facts[0], facts[1], facts[2], facts[3], facts[4],
                facts[5], facts[6], facts[7], facts[8]).mayDie(), "Missing return prerequisite " + missing);
        }
    }

    @Test void distinguishesDimensionsAndIncludesVerticalSeparation() {
        Position source = new Position("overworld", -20.5, 64, -10.5);
        assertTrue(source.within(new Position("overworld", -17.5, 64, -6.5), 5));
        assertFalse(source.within(new Position("nether", -20.5, 64, -10.5), 5));
        assertFalse(source.within(new Position("overworld", -20.5, 70, -10.5), 5));
        assertFalse(source.within(new Position("overworld", 20.5, 64, 10.5), 8));
    }
}
