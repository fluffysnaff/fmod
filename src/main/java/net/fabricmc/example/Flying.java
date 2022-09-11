package net.fabricmc.example;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import static net.fabricmc.example.Util.CLIENT;

public class Flying
{
    public static void tick()
    {
        if(CLIENT.player != null && CLIENT.player.hasVehicle() && Vars.boatFly)
        {
            Entity vehicle = CLIENT.player.getVehicle();
            if(Vars.bypassLo)
            {
                double z = vehicle.getZ();
                double x = vehicle.getX();
                vehicle.setPos(Util.roundToDirection(x), vehicle.getY(), Util.roundToDirection(z));
            }
            Vec3d velocity = vehicle.getVelocity();
            double motionY = CLIENT.options.sprintKey.isPressed() ? -0.5 : (CLIENT.options.jumpKey.isPressed() ? 0.5 : 0);
            vehicle.setVelocity(new Vec3d(velocity.x, motionY, velocity.z));
        }
    }
}
