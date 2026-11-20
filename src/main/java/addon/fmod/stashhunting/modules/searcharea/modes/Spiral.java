package addon.fmod.stashhunting.modules.searcharea.modes;

import addon.fmod.stashhunting.modules.searcharea.SearchAreaMode;
import addon.fmod.stashhunting.modules.searcharea.SearchArea;
import addon.fmod.stashhunting.modules.searcharea.SearchAreaModes;
import addon.fmod.stashhunting.modules.searcharea.VolytraRoutePilot;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.core.BlockPos;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static addon.fmod.stashhunting.Utils.*;
import static meteordevelopment.meteorclient.utils.player.ChatUtils.info;

public class Spiral extends SearchAreaMode
{
    private PathingDataSpiral pd;
    private boolean goingToStart = true;
    private long startTime;

    private boolean useVolytra;
    private VolytraRoutePilot pilot;
    private int spiralCenterX, spiralCenterZ;
    private int lastSavedIndex = -1;
    private boolean routeComplete;

    public Spiral(SearchArea owner)
    {
        super(owner, SearchAreaModes.Spiral);
    }

    @Override
    public void onActivate()
    {
            startTime = System.nanoTime();
            goingToStart = true;
            pd = new PathingDataSpiral(mc.player.blockPosition(), mc.player.blockPosition(), -90.0f, true, 0, 0);
            File file = getJsonFile(super.toString());
            if (file == null || !file.exists())
            {
                pd = new PathingDataSpiral(mc.player.blockPosition(), mc.player.blockPosition(), -90.0f, true, 0, 0);
            }
            else
            {
                try {
                    FileReader reader = new FileReader(file);
                    PathingDataSpiral loaded = GSON.fromJson(reader, PathingDataSpiral.class);
                    if (loaded != null && loaded.initialPos != null && loaded.currPos != null) pd = loaded;
                    else searchArea.warning("Saved spiral was incomplete; starting a new path.");
                    reader.close();
                    info("Loaded previously saved path, heading to where you left off.");
                } catch (Exception e) {
                    searchArea.warning("Could not read saved spiral; starting a new path.");
                }
            }

        useVolytra = searchArea.flightController.get() == SearchArea.FlightController.VolytraFly;
        if (useVolytra) {
            VolytraProgress vp = loadVolytraProgress();
            if (vp != null) {
                spiralCenterX = vp.originX;
                spiralCenterZ = vp.originZ;
            } else {
                spiralCenterX = mc.player.blockPosition().getX();
                spiralCenterZ = mc.player.blockPosition().getZ();
            }
            pilot = new VolytraRoutePilot(buildCorners(spiralCenterX, spiralCenterZ));
            if (!pilot.available()) {
                searchArea.error("VolytraFly module not found; falling back to keyboard control.");
                useVolytra = false;
            } else {
                pilot.start(vp != null ? vp.index : 0);
                lastSavedIndex = pilot.index();
                routeComplete = false;
                if (!pilot.ready()) searchArea.warning("Enable VolytraFly and start gliding; Search Area will fly the spiral once it is on.");
            }
        }
    }

    @Override
    public void onDeactivate()
    {
        if (useVolytra && pilot != null) {
            if (routeComplete) clearVolytraProgress();
            else saveVolytraProgress(spiralCenterX, spiralCenterZ, pilot.index());
        }
        if (pilot != null) pilot.stop();
        super.onDeactivate();
        super.saveToJson(goingToStart, pd);

    }

    private List<int[]> buildCorners(int x, int z) {
        int gap = 16 * searchArea.rowGap.get();
        int[][] dirs = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}}; // +X, +Z, -X, -Z, matching the keyboard spiral

        List<int[]> corners = new ArrayList<>();
        corners.add(new int[]{x, z}); // centre
        for (int n = 1; n <= 40000; n++) {
            int len = ((n + 1) / 2) * gap; // leg lengths: g, g, 2g, 2g, 3g, 3g, ...
            int[] d = dirs[(n - 1) % 4];
            x += d[0] * len;
            z += d[1] * len;
            corners.add(new int[]{x, z});
            if (Math.abs((long) x) > 29000000 || Math.abs((long) z) > 29000000) break;
        }
        return corners;
    }

    @Override
    public void onTick()
    {
        if (useVolytra) {
            if (pilot != null) {
                boolean done = pilot.tick(mc);
                int idx = pilot.index();
                if (idx != lastSavedIndex) {
                    lastSavedIndex = idx;
                    saveVolytraProgress(spiralCenterX, spiralCenterZ, idx);
                }
                if (done) { routeComplete = true; searchArea.toggle(); }
            }
            return;
        }

        // autosave every 10 minutes in case of crashes
        if (System.nanoTime() - startTime > 6e11)
        {
            startTime = System.nanoTime();
            super.saveToJson(goingToStart, pd);
        }

        if (System.nanoTime() < paused)
        {
            setPressed(mc.options.keyUp, false);
            return;
        }


        if (goingToStart)
        {

            if (Math.sqrt(mc.player.blockPosition().distToLowCornerSqr(pd.currPos.getX(), mc.player.getY(), pd.currPos.getZ())) < 5)
            {
                goingToStart = false;
                mc.player.setDeltaMovement(0, 0, 0);
            }
            else
            {
                mc.player.setYRot((float) Rotations.getYaw(net.minecraft.world.phys.Vec3.atCenterOf(pd.currPos)));
                setPressed(mc.options.keyUp, true);
            }
            return;
        }

        setPressed(mc.options.keyUp, true);
        mc.player.setYRot(pd.yawDirection);
        int blockGap = 16 * searchArea.rowGap.get();
        if (pd.mainPath && Math.abs(mc.player.getX() - pd.initialPos.getX()) >= (blockGap + pd.spiralWidth))
        {
            pd.yawDirection += 90.0f;
            pd.initialPos = new BlockPos((int)mc.player.getX(), pd.initialPos.getY(), pd.initialPos.getZ());
            pd.spiralWidth += blockGap;
            pd.mainPath = false;
            mc.player.setDeltaMovement(0, 0, 0);
        }
        else if (!pd.mainPath && Math.abs(mc.player.getZ() - pd.initialPos.getZ()) >= (blockGap + pd.spiralHeight))
        {
            pd.yawDirection += 90.0f;
            pd.initialPos = new BlockPos(pd.initialPos.getX(), pd.initialPos.getY(), (int)mc.player.getZ());
            pd.spiralHeight += blockGap;
            pd.mainPath = true;
            mc.player.setDeltaMovement(0, 0, 0);
        }
    }

    public static class PathingDataSpiral extends PathingData
    {
        public int spiralWidth = 0;
        public int spiralHeight = 0;

        public PathingDataSpiral(BlockPos initialPos, BlockPos currPos, float yawDirection, boolean mainPath, int spiralWidth, int spiralHeight)
        {
            this.initialPos = initialPos;
            this.currPos = currPos;
            this.yawDirection = yawDirection;
            this.mainPath = mainPath;
            this.spiralWidth = spiralWidth;
            this.spiralHeight = spiralHeight;
        }
    }
}
