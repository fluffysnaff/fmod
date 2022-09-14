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
            Vec3d velocity = vehicle.getVelocity();
            double motionX = CLIENT.options.backKey.isPressed() ? 0.5 : (CLIENT.options.forwardKey.isPressed() ? -0.5 : 0);
            double motionY = CLIENT.options.sprintKey.isPressed() ? -0.5 : (CLIENT.options.jumpKey.isPressed() ? 0.5 : 0);
            double motionZ = CLIENT.options.rightKey.isPressed() ? -0.5 : (CLIENT.options.leftKey.isPressed() ? 0.5 : 0);
            vehicle.setVelocity(new Vec3d(motionX, motionY, velocity.z + motionZ));
        }
    }
}
