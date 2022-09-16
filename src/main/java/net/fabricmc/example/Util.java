package net.fabricmc.example;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Util
{
    public static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    public static String commandPrefix = "^";
    public static void log(String msg)
    {
        assert CLIENT.player != null;
        CLIENT.player.sendMessage(Text.of("§5[FMod]: " + msg), false);
    }
}
