package addon.fmod.stashhunting.modules.searcharea.modes;

import addon.fmod.stashhunting.modules.searcharea.SearchAreaMode;
import addon.fmod.stashhunting.modules.searcharea.SearchArea;
import addon.fmod.stashhunting.modules.searcharea.SearchAreaModes;
import addon.fmod.stashhunting.modules.searcharea.VolytraRoutePilot;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoReconnect;
import meteordevelopment.meteorclient.systems.modules.movement.EntityControl;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import java.io.*;

import static meteordevelopment.meteorclient.utils.player.ChatUtils.info;
import static addon.fmod.stashhunting.Utils.*;

public class Rectangle extends SearchAreaMode
{

    private PathingDataRectangle pd;
    private boolean goingToStart = true;
    private long startTime;

    private boolean useVolytra;
    private VolytraRoutePilot pilot;
    private int lastSavedIndex = -1;
    private boolean routeComplete;

    public Rectangle(SearchArea owner) {
        super(owner, SearchAreaModes.Rectangle);
    }

    @Override
    public void onActivate()
    {
        goingToStart = true;
        startTime = System.nanoTime();
        pd = new PathingDataRectangle(searchArea.startPos.get(), searchArea.targetPos.get(), searchArea.startPos.get(), 90, true, (int) mc.player.getZ());
        File file = getJsonFile(super.toString());
        if (file == null || !file.exists())
        {
            // set currPos to startpos if it is not read from file, so that the bot travels to the startpoint and not where the player currently is
            pd = new PathingDataRectangle(searchArea.startPos.get(), searchArea.targetPos.get(), searchArea.startPos.get(), 90, true, (int)mc.player.getZ());
        }
        else
        {
            try {
                FileReader reader = new FileReader(file);
                PathingDataRectangle loaded = GSON.fromJson(reader, PathingDataRectangle.class);
                if (loaded != null && loaded.initialPos != null && loaded.targetPos != null && loaded.currPos != null) pd = loaded;
                else searchArea.warning("Saved rectangle was incomplete; starting a new path.");
                reader.close();
            } catch (Exception e) {
                searchArea.warning("Could not read saved rectangle; starting a new path.");
            }
        }

        useVolytra = searchArea.flightController.get() == SearchArea.FlightController.VolytraFly;
        if (useVolytra) {
            pilot = new VolytraRoutePilot(buildCorners());
            if (!pilot.available()) {
                searchArea.error("VolytraFly module not found; falling back to keyboard control.");
                useVolytra = false;
            } else {
                VolytraProgress vp = loadVolytraProgress();
                pilot.start(vp != null ? vp.index : 0);
                lastSavedIndex = pilot.index();
                routeComplete = false;
                if (!pilot.ready()) searchArea.warning("Enable VolytraFly and start gliding; Search Area will fly the route once it is on.");
            }
        }
    }

    @Override
    public void onDeactivate()
    {
        if (useVolytra && pilot != null) {
            if (routeComplete) clearVolytraProgress();
            else saveVolytraProgress(searchArea.startPos.get().getX(), searchArea.startPos.get().getZ(), pilot.index());
        }
        if (pilot != null) pilot.stop();
        super.onDeactivate();
        super.saveToJson(goingToStart, pd);
    }

    private List<int[]> buildCorners() {
        int x0 = searchArea.startPos.get().getX();
        int z0 = searchArea.startPos.get().getZ();
        int x1 = searchArea.targetPos.get().getX();
        int z1 = searchArea.targetPos.get().getZ();
        int step = searchArea.rowGap.get() * 16;
        if (z1 < z0) step = -step;

        List<int[]> corners = new ArrayList<>();
        int z = z0;
        boolean toTarget = true;
        int guard = 0;
        while ((step > 0 ? z <= z1 : z >= z1) && guard++ < 20000) {
            int a = toTarget ? x0 : x1;
            int b = toTarget ? x1 : x0;
            corners.add(new int[]{a, z});
            corners.add(new int[]{b, z});
            toTarget = !toTarget;
            z += step;
        }
        return corners;
    }

    private void completePath() {
        setPressed(mc.options.keyUp, false);
        if (pilot != null) pilot.stop();
        searchArea.toggle();
        if (searchArea.disconnectOnCompletion.get()) {
            var autoReconnect = Modules.get().get(AutoReconnect.class);
            if (autoReconnect.isActive()) autoReconnect.toggle();
            mc.player.connection.handleDisconnect(new ClientboundDisconnectPacket(Component.literal("[Search Area] Path is complete")));
        }
    }

    private void printRectangleEstimate()
    {
        Class<? extends Module> entityControl = EntityControl.class;
        Module module = Modules.get().get(entityControl);
        double speedBPS = (double)module.settings.get("horizontal-speed").get();
        double rowDistance = Math.abs(pd.initialPos.getX() - pd.targetPos.getX());
        int rowCount = Math.abs(pd.currPos.getZ() - pd.targetPos.getZ()) / 16 / searchArea.rowGap.get();
        double totalBlocks = rowCount * (rowDistance + (searchArea.rowGap.get() * 16));
        long totalSeconds = (long)(totalBlocks / speedBPS);
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        info("Completion will take an estimated %02d hours %02d minutes %02d seconds with boatfly at a speed of %.2f and a gap of %d chunks between paths.", hours, minutes, seconds, speedBPS, searchArea.rowGap.get());
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
                    saveVolytraProgress(searchArea.startPos.get().getX(), searchArea.startPos.get().getZ(), idx);
                }
                if (done) { routeComplete = true; completePath(); }
            }
            return;
        }

        // autosave every 10 minutes in case of crashes
        if (System.nanoTime() - startTime > 6e11)
        {
            startTime = System.nanoTime();
            super.saveToJson(goingToStart, pd);
        }

        if (goingToStart)
        {
            if (Math.sqrt(mc.player.blockPosition().distToLowCornerSqr(pd.currPos.getX(), mc.player.getY(), pd.currPos.getZ())) < 5)
            {
                goingToStart = false;
                mc.player.setDeltaMovement(0, 0, 0);
                printRectangleEstimate();
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
        if (Math.sqrt(mc.player.blockPosition().distToLowCornerSqr(pd.targetPos.getX(), mc.player.getY(), pd.targetPos.getZ())) < 20) // end of rectangle
        {
            completePath();
        }
        else if (pd.mainPath && (
            (pd.yawDirection == -90.0f && mc.player.getX() >= Math.max(pd.initialPos.getX(), pd.targetPos.getX())) ||
            (pd.yawDirection == 90.0f && mc.player.getX() <= Math.min(pd.initialPos.getX(), pd.targetPos.getX()))))
        {
            pd.yawDirection = (mc.player.getZ() < pd.targetPos.getZ()) ? 0.0f : 180.0f;
            pd.mainPath = false;
            mc.player.setDeltaMovement(0, 0, 0);
        }
        else if (!pd.mainPath && Math.abs(mc.player.getZ() - pd.lastCompleteRowZ) >= (16 * searchArea.rowGap.get())) // if the path to go past loaded chunks is done
        {
            pd.lastCompleteRowZ = (int)mc.player.getZ();
            pd.yawDirection = (pd.initialPos.getX() > mc.player.getX() ? -90.0f : 90.0f);
            pd.mainPath = true;
            mc.player.setDeltaMovement(0, 0, 0);
        }
    }

    public static class PathingDataRectangle extends PathingData
    {
        public BlockPos targetPos;
        public int lastCompleteRowZ;

        public PathingDataRectangle(BlockPos initialPos, BlockPos targetPos, BlockPos currPos, float yawDirection, boolean mainPath, int lastCompleteRowZ)
        {
            this.initialPos = initialPos;
            this.targetPos = targetPos;
            this.currPos = currPos;
            this.yawDirection = yawDirection;
            this.mainPath = mainPath;
            this.lastCompleteRowZ = lastCompleteRowZ;
        }
    }
}
