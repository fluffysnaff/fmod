package addon.fmod.volytra;

import addon.fmod.volytra.autonav.NavigationProgress;
import addon.fmod.volytra.autonav.WaypointProgress.Point;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NavigationProgressTest {
    @Test void wallJitterDoesNotKeepResettingTheStallClock() {
        var progress=new NavigationProgress();
        progress.sample(0,new Point(10,80,10));
        double stalled=0;
        for(int second=1;second<=30;second++)stalled=progress.sample(second*1_000_000_000L,new Point(10+(second%2==0?.1:-.1),80,10));
        assertEquals(30,stalled);
    }
    @Test void meaningfulMovementRestartsTheMovementWindowButNotTheActiveClock() {
        var progress=new NavigationProgress();
        progress.sample(0,new Point(0,80,0));
        assertEquals(4,progress.sample(4_000_000_000L,new Point(0,80,0)));
        assertEquals(0,progress.sample(5_000_000_000L,new Point(2,80,0)));
        assertEquals(5_000_000_000L,progress.activeNanos());
    }
    @Test void intentionalGamePauseDoesNotConsumeTheRecoveryBudget() {
        var progress=new NavigationProgress();
        progress.sample(0,new Point(0,80,0));
        progress.sample(1_000_000_000L,new Point(0,80,0));
        progress.pause(61_000_000_000L);
        assertEquals(2,progress.sample(62_000_000_000L,new Point(0,80,0)));
        assertEquals(2_000_000_000L,progress.activeNanos());
        progress.reset();
        assertEquals(0,progress.sample(70_000_000_000L,new Point(100,80,100)));
    }
}
