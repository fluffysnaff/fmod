package addon.fmod.features;

import addon.fmod.features.travel.TravelProgress;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TravelProgressTest {
    @Test void measuresEuclideanBlocksPerRealSecondAndAnHonestUnknownEta(){
        var trip=new TravelProgress();trip.sample(0,0,0,60);assertFalse(Double.isFinite(trip.eta(100)));
        trip.sample(1_000_000_000L,3,4,60);assertEquals(5,trip.speed(),1e-9);assertEquals(20,trip.eta(100),1e-9);
        trip.sample(2_000_000_000L,3,4,60);assertEquals(2.5,trip.speed(),1e-9);
    }
    @Test void excludesTeleportsAndResetsSessionHistory(){
        var trip=new TravelProgress();trip.sample(0,0,0,60);trip.sample(1_000_000_000L,1000000,1000000,60);assertEquals(0,trip.totalDistance());assertEquals(0,trip.speed());
        trip.sample(2_000_000_000L,1000003,1000004,60);assertEquals(5,trip.speed(),1e-9);trip.reset();assertEquals(0,trip.totalDistance());
    }
}
