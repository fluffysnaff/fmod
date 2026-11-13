package addon.fmod.utils;

import addon.fmod.utils.BaseSitePlanner.Bounds;
import addon.fmod.utils.BaseSitePlanner.Corridor;
import addon.fmod.utils.BaseSitePlanner.Point;
import addon.fmod.utils.BaseSitePlanner.Policy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class BaseSitePlannerTest {
    private static final Bounds BORDER = new Bounds(-29_999_984, 29_999_984, -29_999_984, 29_999_984);
    private static final Policy POLICY = new Policy(1_000_000, 25_000_000, 250_000, 10_000, 64, List.of(2_000_000));

    private boolean accepts(int x, int z) {
        return BaseSitePlanner.accepts(new Point(x, z), POLICY, BORDER, List.of(), List.of());
    }

    @Test void rejectsTrafficGeometryInEveryQuadrant() {
        for (int signX : new int[]{-1, 1}) {
            for (int signZ : new int[]{-1, 1}) {
                assertFalse(accepts(signX * 1_234_567, signZ * 9999), "axis");
                assertFalse(accepts(signX * 1_234_567, signZ * 1_240_000), "diagonal");
                assertFalse(accepts(signX * 1_999_001, signZ * 1_234_567), "square ring");
                assertFalse(accepts(signX * 3_000_010, signZ * 1_234_567), "round coordinate");
                assertFalse(accepts(signX * 234_567, signZ * 345_678), "spawn distance");
                assertFalse(accepts(signX * 25_000_001, signZ * 1_234_567), "sample extent");
                assertTrue(accepts(signX * 3_456_789, signZ * 1_234_567));
            }
        }
    }

    @Test void usesPerpendicularDiagonalDistanceAndSquareRingEdges() {
        assertFalse(accepts(3_012_345, 3_026_345));
        Policy ring = new Policy(0, 25_000_000, 0, 10_000, 0, List.of(2_000_000));
        assertFalse(BaseSitePlanner.accepts(new Point(2_001_001, 1_984_999), ring, BORDER, List.of(), List.of()));
        assertTrue(BaseSitePlanner.accepts(new Point(2_011_001, 1_984_999), ring, BORDER, List.of(), List.of()));
    }

    @Test void clipsToOffsetWorldBorderWithMargin() {
        Bounds offset = new Bounds(1_000_000, 6_000_000, -4_000_000, -500_000);
        assertFalse(BaseSitePlanner.accepts(new Point(1_249_999, -2_345_678), POLICY, offset, List.of(), List.of()));
        assertFalse(BaseSitePlanner.accepts(new Point(5_750_001, -2_345_678), POLICY, offset, List.of(), List.of()));
        assertTrue(BaseSitePlanner.accepts(new Point(3_456_789, -1_234_567), POLICY, offset, List.of(), List.of()));
        Random random = new Random(11);
        for (int i = 0; i < 50; i++) {
            Point candidate = BaseSitePlanner.find(random, POLICY, offset, List.of(), List.of()).orElseThrow();
            assertTrue(candidate.x() >= 1_250_000 && candidate.x() <= 5_750_000);
            assertTrue(candidate.z() >= -3_750_000 && candidate.z() <= -750_000);
        }
    }

    @Test void measuresSegmentsWithoutExtendingThemOrOverflowing() {
        Corridor segment = new Corridor(new Point(10, 10), new Point(30, 10));
        assertEquals(5, segment.distance(new Point(20, 15)));
        assertEquals(5, segment.distance(new Point(7, 14)));
        assertEquals(5, segment.distance(new Point(33, 14)));
        assertEquals(5, new Corridor(new Point(10, 10), new Point(10, 10)).distance(new Point(13, 14)));
        assertEquals(3, new Corridor(new Point(-239_999_872, 10), new Point(239_999_872, 10)).distance(new Point(0, 13)));
    }

    @Test void keepsTheWholeCandidateBlockInsideFractionalBorders() {
        Policy policy = new Policy(0, 1000, 0, 0, 0, List.of());
        Bounds border = new Bounds(-100.5, 100.5, -100.5, 100.5);
        assertFalse(BaseSitePlanner.accepts(new Point(100, 21), policy, border, List.of(), List.of()));
        assertFalse(BaseSitePlanner.accepts(new Point(21, 100), policy, border, List.of(), List.of()));
        assertFalse(BaseSitePlanner.accepts(new Point(-101, 21), policy, border, List.of(), List.of()));
        assertTrue(BaseSitePlanner.accepts(new Point(99, 21), policy, border, List.of(), List.of()));
        assertTrue(BaseSitePlanner.accepts(new Point(-100, 21), policy, border, List.of(), List.of()));
    }

    @Test void projectsNetherPointsAndExcludesTheirCorridorsAndPortals() {
        Point nether = new Point(-432_098, 154_321);
        Point projected = nether.overworld(true);
        assertEquals(new Point(-3_456_784, 1_234_568), projected);
        assertEquals(nether, nether.overworld(false));
        assertFalse(BaseSitePlanner.accepts(projected, POLICY, BORDER, List.of(projected), List.of()));
        Corridor corridor = new Corridor(new Point(-450_001, 154_321).overworld(true),
            new Point(-400_001, 154_321).overworld(true));
        assertFalse(BaseSitePlanner.accepts(projected, POLICY, BORDER, List.of(), List.of(corridor)));
        assertTrue(BaseSitePlanner.accepts(new Point(projected.x(), projected.z() + 10_001), POLICY, BORDER, List.of(), List.of(corridor)));
    }

    @Test void generatesIndependentSignsAndAxesWithinAllConstraints() {
        Random random = new Random(35);
        List<Point> avoid = List.of(new Point(3_456_789, 1_234_567));
        List<Corridor> routes = List.of(new Corridor(new Point(-10_000_001, 4_567_891), new Point(10_000_001, 4_567_891)));
        boolean[] quadrants = new boolean[4];
        for (int i = 0; i < 200; i++) {
            Point point = BaseSitePlanner.find(random, POLICY, BORDER, avoid, routes).orElseThrow();
            assertTrue(BaseSitePlanner.accepts(point, POLICY, BORDER, avoid, routes));
            quadrants[(point.x() < 0 ? 2 : 0) + (point.z() < 0 ? 1 : 0)] = true;
        }
        for (boolean visited : quadrants) assertTrue(visited);
    }

    @Test void returnsNoResultForImpossibleOrExhaustedSearches() {
        assertTrue(BaseSitePlanner.find(new Random(1), POLICY, new Bounds(-100, 100, -100, 100), List.of(), List.of()).isEmpty());
        Policy impossible = new Policy(5_000_000, 100, 0, 0, 0, List.of());
        assertTrue(BaseSitePlanner.find(new Random(1), impossible, BORDER, List.of(), List.of()).isEmpty());
        assertTrue(BaseSitePlanner.find(new Random(1), POLICY, new Bounds(100_000_000, 100_000_100, 0, 100), List.of(), List.of()).isEmpty());
    }

    @Test void neverFallsBackToAnExcludedCandidateWhenThePrivateListCoversTheSearchArea() {
        Policy small = new Policy(0, 1000, 0, 0, 0, List.of());
        Bounds area = new Bounds(11, 15, 21, 25);
        var index = new BaseScoutExclusions.Index(List.of(new BaseScoutExclusions.Entry(12, 22,
            BaseScoutExclusions.Dimension.overworld, 100)), 0);
        assertTrue(BaseSitePlanner.find(new Random(9), small, area, List.of(), List.of(),
            point -> index.hit(point.x(), point.z()).isEmpty()).isEmpty());
        assertEquals(new Point(12, 22), BaseSitePlanner.find(new Random(9), small, area, List.of(), List.of(),
            point -> point.x() == 12 && point.z() == 22).orElseThrow());
    }

    @Test void validatesPoliciesAndCopiesRingInput() {
        List<Integer> rings = new ArrayList<>(List.of(1000));
        Policy policy = new Policy(0, 100, 0, 0, 0, rings);
        rings.clear();
        assertEquals(List.of(1000), policy.squareRings());
        assertThrows(IllegalArgumentException.class, () -> new Policy(0, 0, 0, 0, 0, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Policy(0, 100, 0, 0, 50_000, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Policy(0, 100, 0, 0, 0, List.of(-1)));
        assertThrows(IllegalArgumentException.class, () -> new Bounds(Double.NaN, 10, 0, 10));
    }
}
