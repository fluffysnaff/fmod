package addon.fmod.volytra;

import addon.fmod.volytra.autonav.AutonavPlanner;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CancellationException;
import static org.junit.jupiter.api.Assertions.*;

class AutonavPlannerTest {
    private static class Grid implements AutonavPlanner.Grid {
        public boolean isChunkUnloaded(int x, int z) { return false; }
        public boolean isPassable(int x, int y, int z, int clearance) { return x != 5 || Math.abs(z) > 2; }
        public boolean lineClear(int x, int y, int z, int x1, int y1, int z1) {
            if (Math.abs(x1-x)+Math.abs(y1-y)+Math.abs(z1-z)!=1) return false;
            return isPassable(x,y,z,0)&&isPassable(x1,y1,z1,0);
        }
        public int escapeCost(int x, int y, int z) { return isPassable(x, y, z, 0) ? 0 : 1; }
        public int minY() { return 0; }
        public int maxY() { return 0; }
    }

    @Test void routesAroundBlockedCellsToTheDestination() {
        Grid grid = new Grid();
        var result = AutonavPlanner.plan(grid, new int[]{0, 0, 0}, 12.5, 0.5, 0.1, 0, 0, 1000, 0);
        assertEquals(AutonavPlanner.Status.REACHED_DESTINATION, result.status);
        assertTrue(result.cells.stream().allMatch(cell -> grid.isPassable(cell[0], cell[1], cell[2], 0)));
        assertArrayEquals(new int[]{12, 0, 0}, result.cells.getLast());
    }

    @Test void stopsAtTheLoadedFrontierWithoutRoutingThroughUnknownChunks() {
        Grid grid = new Grid() {
            public boolean isChunkUnloaded(int x, int z) { return x != 0 || z != 0; }
            public boolean isPassable(int x, int y, int z, int clearance) { return true; }
        };
        var result = AutonavPlanner.plan(grid, new int[]{8, 0, 8}, 100, 8.5, 0.1, 0, 0, 1000, 0);
        assertEquals(AutonavPlanner.Status.REACHED_FRONTIER, result.status);
        assertEquals(15, result.cells.getLast()[0]);
        assertTrue(result.cells.stream().noneMatch(cell -> grid.isChunkUnloaded(cell[0] >> 4, cell[2] >> 4)));
    }

    @Test void cancelledSearchStopsWithoutPublishingARoute() {
        Thread.currentThread().interrupt();
        try {
            assertThrows(CancellationException.class, () -> AutonavPlanner.plan(new Grid(), new int[]{0, 0, 0}, 12, 0, 1, 0, 0, 1000, 0));
        } finally { Thread.interrupted(); }
    }

    @Test void doesNotPlanThroughABlockedTransitionBetweenClearCells() {
        Grid grid = new Grid() {
            public boolean isPassable(int x,int y,int z,int clearance) { return true; }
            public boolean lineClear(int x,int y,int z,int x1,int y1,int z1) {
                return !(z==0&&z1==0&&Math.min(x,x1)==4&&Math.max(x,x1)==5);
            }
        };
        var result=AutonavPlanner.plan(grid,new int[]{0,0,0},12.5,.5,.1,0,0,1000,0);
        assertEquals(AutonavPlanner.Status.REACHED_DESTINATION,result.status);
        for(int i=1;i<result.cells.size();i++){
            int[] a=result.cells.get(i-1),b=result.cells.get(i);
            assertTrue(grid.lineClear(a[0],a[1],a[2],b[0],b[1],b[2]),"Every planned transition must fit the moving body");
        }
    }

    @Test void escapeDoesNotRequireFlyingThroughAnotherSolidBlock() {
        Grid grid=new Grid(){
            public boolean isPassable(int x,int y,int z,int clearance){return x>=2;}
            public int escapeCost(int x,int y,int z){return x>=2?0:10;}
            public boolean lineClear(int x,int y,int z,int x1,int y1,int z1){return x1>=2;}
        };
        var result=AutonavPlanner.escape(grid,new int[]{0,0,0},5,500);
        assertEquals(AutonavPlanner.Status.SEALED,result.status,"A route requiring a new solid-block collision is unusable");
    }
}
