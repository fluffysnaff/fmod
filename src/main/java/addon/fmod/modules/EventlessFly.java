/*
 * Based on Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */
package addon.fmod.modules;

import addon.fmod.FMod;
import addon.fmod.utils.WarpUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Flight;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFly;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class EventlessFly extends Module {
    private final SettingGroup general = settings.getDefaultGroup();
    private final Setting<Double> speed = general.add(new DoubleSetting.Builder()
        .name("speed").description("Flight speed in blocks per tick.")
        .defaultValue(0.062).range(0.01, 1).build());
    private final Setting<Boolean> flightDip = general.add(new BoolSetting.Builder()
        .name("anti-kick").description("Periodically descend while airborne.")
        .defaultValue(true).build());
    private final Setting<Integer> dipInterval = general.add(new IntSetting.Builder()
        .name("delay").description("Airborne ticks between descents.")
        .defaultValue(20).range(5, 200).visible(flightDip::get).build());
    private int airborneTicks;

    public EventlessFly() {
        super(FMod.CATEGORY, "eventless-fly", "Applies flight after Meteor's move event and uses vanilla position updates.");
    }

    @Override
    public void onActivate() {
        airborneTicks = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        LocalPlayer player = mc.player;
        if (player == null || player.onGround() || mc.options.keyShift.isDown() || !canFly(player)) airborneTicks = 0;
        else airborneTicks = (airborneTicks + 1) % (dipInterval.get() + 4);
    }

    public Vec3 movement(LocalPlayer player, Vec3 original) {
        if (!canFly(player) || !Double.isFinite(speed.get())) return original;

        double forward = (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0);
        double strafe = (mc.options.keyLeft.isDown() ? 1 : 0) - (mc.options.keyRight.isDown() ? 1 : 0);
        double vertical = (mc.options.keyJump.isDown() ? 1 : 0) - (mc.options.keyShift.isDown() ? 1 : 0);
        Vec3 direction = Vec3.directionFromRotation(0, player.getYRot());
        Vec3 motion = new Vec3(direction.x * forward + direction.z * strafe, vertical,
            direction.z * forward - direction.x * strafe).normalize().scale(speed.get());
        if (flightDip.get() && airborneTicks >= dipInterval.get()) motion = new Vec3(motion.x, -0.08, motion.z);
        player.setDeltaMovement(Vec3.ZERO);
        return motion;
    }

    private boolean canFly(LocalPlayer player) {
        var modules = Modules.get();
        var courier = modules.get(StasisCourier.class);
        return mc.gui.screen() == null && player.isAlive() && !player.isSpectator() && !player.isPassenger()
            && !player.isFallFlying() && !player.isSwimming() && !player.getAbilities().flying && !WarpUtils.isWarping()
            && !modules.isActive(Flight.class) && !modules.isActive(ElytraFly.class) && !modules.isActive(VolytraFly.class)
            && (courier == null || !courier.isRunning());
    }
}
