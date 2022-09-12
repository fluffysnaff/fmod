package net.fabricmc.example;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class Util
{
    public static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    public static String commandPrefix = "^";

    public static double roundToDirection(double val, double decimals)
    {
        var round = Math.pow(10.0, decimals);
        var rounded = Math.round(val * round) / round;
        var dir = Math.nextAfter(rounded, rounded  + Math.signum(val));
        return Math.round(dir * round) / round;
    }

    public static void log(String msg)
    {
        if(CLIENT != null)
        {
            CLIENT.player.sendMessage(Text.of("§5[FMod]: " + msg), false);
        }
    }
}
