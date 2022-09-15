package net.fabricmc.example;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Util
{
    public static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    public static String commandPrefix = "^";

    public static double round(double val, int decimals)
    {
        int multiplier = (int) Math.pow(10, decimals);
        return ((double) ((long) ((val) * multiplier)) / multiplier);
    }

    public static void log(String msg)
    {
        assert CLIENT.player != null;
        CLIENT.player.sendMessage(Text.of("§5[FMod]: " + msg), false);
    }
}
