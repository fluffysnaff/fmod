package addon.fmod.volytra;

import addon.fmod.volytra.autonav.WaypointProgress;
import addon.fmod.volytra.autonav.WaypointProgress.Point;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WaypointProgressTest {
    @Test void crossingAPlaneFarFromTheRouteDoesNotSkipItsCorner() {
        assertFalse(WaypointProgress.passed(new Point(12,5,0),new Point(0,0,0),new Point(10,0,0),.75));
        assertFalse(WaypointProgress.passed(new Point(4,6,0),new Point(0,0,0),new Point(0,5,0),.75));
    }
    @Test void passingAlongTheRouteOrReachingTheEndpointAdvances() {
        assertTrue(WaypointProgress.passed(new Point(12,.2,0),new Point(0,0,0),new Point(10,0,0),.75));
        assertTrue(WaypointProgress.passed(new Point(9.98,0,0),new Point(0,0,0),new Point(10,0,0),.75));
    }
    @Test void doesNotRoundOffACornerBeforeReachingIt() {
        assertFalse(WaypointProgress.passed(new Point(9.5,0,0),new Point(0,0,0),new Point(10,0,0),.75));
    }
    @Test void repeatedNodesCannotTrapProgress() {
        assertTrue(WaypointProgress.passed(new Point(1,0,0),new Point(0,0,0),new Point(0,0,0),.75));
    }
}
