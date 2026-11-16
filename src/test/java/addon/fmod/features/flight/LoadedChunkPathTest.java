package addon.fmod.features.flight;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class LoadedChunkPathTest {
    @Test void allowsLoadedMovementAndDoesNotQueryChunksForZeroHorizontalMovement() {
        assertTrue(LoadedChunkPath.canMove(7.7, 7.7, 8.3, 8.3, 32, 0, (x, z) -> true));
        assertTrue(LoadedChunkPath.canMove(7.7, 7.7, 8.3, 8.3, 0, 0, (x, z) -> { fail("No movement needs no chunk lookup"); return false; }));
    }

    @Test void stopsTheBodyBeforeTheCenterEntersAnUnloadedChunk() {
        assertFalse(LoadedChunkPath.canMove(15.2, 7.7, 15.8, 8.3, .3, 0, (x, z) -> x == 0));
        assertTrue(LoadedChunkPath.canMove(15.2, 7.7, 15.8, 8.3, .2, 0, (x, z) -> x == 0));
    }

    @Test void usesFloorDivisionAtNegativeChunkBoundaries() {
        assertFalse(LoadedChunkPath.canMove(.2, .2, .8, .8, -.5, 0, (x, z) -> x == 0));
        assertFalse(LoadedChunkPath.canMove(-15.8, 7.7, -15.2, 8.3, -.3, 0, (x, z) -> x == -1));
        assertTrue(LoadedChunkPath.canMove(-15.8, 7.7, -15.2, 8.3, -.2, 0, (x, z) -> x == -1));
    }

    @Test void cannotSkipAnUnloadedChunkEvenWhenTheDestinationIsLoaded() {
        assertFalse(LoadedChunkPath.canMove(7.7, 7.7, 8.3, 8.3, 32, 0, (x, z) -> x != 1));
        assertFalse(LoadedChunkPath.canMove(39.7, 7.7, 40.3, 8.3, -32, 0, (x, z) -> x != 1));
    }

    @Test void ignoresUnloadedChunksOutsideTheActualDiagonalSweep() {
        assertTrue(LoadedChunkPath.canMove(7.7, 7.7, 8.3, 8.3, 32, 32, (x, z) -> !(x == 0 && z == 2)));
        assertFalse(LoadedChunkPath.canMove(7.7, 7.7, 8.3, 8.3, 32, 32, (x, z) -> !(x == 1 && z == 0)));
    }

    @Test void checksTheFullWidthAcrossChunkCorners() {
        assertFalse(LoadedChunkPath.canMove(15.2, 15.2, 15.8, 15.8, .4, .4, (x, z) -> !(x == 1 && z == 0)));
        assertFalse(LoadedChunkPath.canMove(7.7, 15.2, 8.3, 15.8, 0, .4, (x, z) -> z == 0));
    }

    @Test void canBackOutOfAChunkThatUnloadedAroundThePlayer() {
        assertTrue(LoadedChunkPath.canMove(16.2, 7.7, 16.8, 8.3, -1, 0, (x, z) -> x == 0));
        assertFalse(LoadedChunkPath.canMove(16.2, 7.7, 16.8, 8.3, 32, 0, (x, z) -> x == 0));
    }

    @Test void boundsWorkAndRejectsInvalidGeometryAndMovement() {
        var calls = new AtomicInteger();
        assertFalse(LoadedChunkPath.canMove(0, 0, .6, .6, 1e100, 1e100, (x, z) -> { calls.incrementAndGet(); return true; }));
        assertEquals(0, calls.get());
        for (double invalid : new double[] { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY })
            assertFalse(LoadedChunkPath.canMove(0, 0, .6, .6, invalid, 0, (x, z) -> true));
        assertFalse(LoadedChunkPath.canMove(1, 0, 0, .6, 1, 0, (x, z) -> true));
    }
}
