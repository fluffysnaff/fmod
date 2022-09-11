package net.fabricmc.example;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class Util
{
    public static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    public static String commandPrefix = "^";

    public static double roundToDirection(double val)
    {
        var rounded = Math.round(val * 100.0) / 100.0;
        var dir = Math.nextAfter(rounded, rounded + Math.signum(val));
        return Math.round(dir * 1000.0) / 1000.0;
    }

    public static float roundToDirection(float val)
    {
        var rounded = Math.round(val * 100.0) / 100.0;
        var dir = Math.nextAfter(rounded, rounded + Math.signum(val));
        return (float)(Math.round(dir * 1000.0) / 1000.0);
    }

    public static void log(String msg)
    {
        if(CLIENT != null)
        {
            CLIENT.player.sendMessage(Text.of("§5[FMod]: " + msg), false);
        }
    }
}
