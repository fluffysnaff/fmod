package addon.fmod.utils;

import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

public class RoundUtils {
    public static double round(double val, int dec)
    {
        int decimals = (int) Math.pow(10, dec);
        long subX = ((long) (val * decimals)) % 10;
        return ((val * decimals) - subX) / decimals;
    }

    public static double roundCoordOnPositionPacket(double val)
    {
        // First round to the thousandths
        double newVal = RoundUtils.round(val, 3);

        // Then check if it rounded correctly - if no round again
        for(int i = 2; i > 0; i--)
        {
            if (RoundUtils.isNotRoundedPos(newVal, newVal))
            {
                newVal = RoundUtils.round(newVal, i);
            }
        }

        // If it for some reason couldn't round, just truncate it completely
        if (RoundUtils.isNotRoundedPos(newVal, newVal))
        {
            newVal = (int)(newVal);
        }

        return newVal;
    }

    public static boolean isNotRoundedPos(double x, double z)
    {
        return ((((long) (x * 1000)) % 10) != 0) && ((((long) (z * 1000)) % 10) != 0);
    }

}
