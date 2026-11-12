package addon.fmod.stashhunting.modules.searcharea;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import static addon.fmod.stashhunting.Utils.*;
import java.io.*;

import static addon.fmod.stashhunting.Utils.sendWebhook;

public class SearchAreaMode
{

    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    protected final SearchArea searchArea;
    protected final Minecraft mc;
    private final SearchAreaModes type;
    protected long paused = 0;

    public SearchAreaMode(SearchArea searchArea, SearchAreaModes type) {
        this.searchArea = searchArea;
        this.mc = Minecraft.getInstance();
        this.type = type;
    }

    public void onTick()
    {

    }

    public void onActivate()
    {

    }

    public void onDeactivate()
    {
        setPressed(mc.options.keyUp, false);
    }

    public void disable()
    {
        if (searchArea.isActive()) searchArea.toggle();
    }

    protected File getJsonFile(String fileName) {
        String name = searchArea.saveLocation.get().trim();
        if (name.isEmpty()) return null;
        if (!name.matches("[a-zA-Z0-9_. -]{1,100}") || name.equals(".") || name.equals("..")) {
            searchArea.error("Save name must be a folder name, using letters, numbers, spaces, dots, underscores, or hyphens.");
            return null;
        }
        return new File(new File(new File(MeteorClient.FOLDER, "fmod-search-area"), name), fileName + ".json");
    }

    protected void saveToJson(boolean goingToStart, PathingData pd)
    {
        // Fix issue where "null" gets saved to the file creating crashes when it gets loaded next time.
        if (pd == null) return;
        // last pos doesn't matter if disconnecting while going to start
        if (!goingToStart && mc.player != null) pd.currPos = mc.player.blockPosition();
        try {
            File file = getJsonFile(type.toString());
            if (file == null) return;
            file.getParentFile().mkdirs();
            Writer writer = new FileWriter(file);
            GSON.toJson(pd, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static class PathingData
    {
        public BlockPos initialPos;
        public BlockPos currPos;
        public float yawDirection;
        public boolean mainPath;
    }

    public void clear()
    {
        File file = getJsonFile(type.toString());
        if (file != null && file.exists() && !file.delete()) searchArea.error("Could not delete saved path.");
    }

    public void clear(String mode)
    {
        File file = getJsonFile(mode);
        if (file != null && file.exists() && !file.delete()) searchArea.error("Could not delete saved path.");
    }

    public void clearAll()
    {
        for (SearchAreaModes mode : SearchAreaModes.values())
        {
            clear(mode.toString());
        }
    }

    public String toString()
    {
        return type.toString();
    }

}
