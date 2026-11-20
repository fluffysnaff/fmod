/*
 * Adapted from Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */
package addon.fmod.modules;

import addon.fmod.FMod;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

public final class KnockbackTweaks extends Module {
    public enum Mode { RelativeToPlayer, RelativeToView, Absolute }

    private final SettingGroup general = settings.getDefaultGroup();
    private final Setting<Mode> mode = general.add(new EnumSetting.Builder<Mode>()
        .name("mode").description("Measure the knockback angle from the target, the view, or world yaw.")
        .defaultValue(Mode.RelativeToPlayer).build());
    private final Setting<Integer> direction = general.add(new IntSetting.Builder()
        .name("direction").description("Knockback direction in degrees.")
        .defaultValue(0).range(-180, 180).sliderRange(-180, 180).build());
    private final Setting<Integer> spread = general.add(new IntSetting.Builder()
        .name("random").description("Maximum random offset in either direction, in degrees.")
        .defaultValue(0).range(0, 180).sliderMax(90).build());
    private final Setting<Boolean> fakeSprint = general.add(new BoolSetting.Builder()
        .name("fake-sprint").description("Send a sprint state around the attack to enable sprint knockback.")
        .defaultValue(true).build());
    private final Setting<Boolean> renderDirection = general.add(new BoolSetting.Builder()
        .name("render-direction").description("Draw the configured knockback direction on the target.")
        .defaultValue(false).build());

    public KnockbackTweaks() {
        super(FMod.CATEGORY, "knockback-tweaks", "Steers the knockback from melee attacks that use the attacker's yaw.");
    }

    public void sendAttack(LocalPlayer player, Entity target, Runnable attack) {
        int range = spread.get();
        float yaw = Mth.wrapDegrees(yaw(player, target) + ThreadLocalRandom.current().nextInt(-range, range + 1));
        boolean sprint = fakeSprint.get() && !player.isSprinting();
        if (sprint) sendSprint(player, ServerboundPlayerCommandPacket.Action.START_SPRINTING);
        sendLook(player, yaw);
        try {
            attack.run();
        } finally {
            sendLook(player, player.getYRot());
            if (sprint) sendSprint(player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING);
        }
    }

    private float yaw(LocalPlayer player, Entity target) {
        Vec3 offset = target.position().subtract(player.position());
        float base = switch (mode.get()) {
            case Absolute -> 0;
            case RelativeToView -> player.getYRot();
            case RelativeToPlayer -> (float) Math.toDegrees(Math.atan2(offset.z, offset.x)) - 90;
        };
        return base + direction.get();
    }

    private void sendLook(LocalPlayer player, float yaw) {
        player.connection.send(new ServerboundMovePlayerPacket.Rot(
            yaw, player.getXRot(), player.onGround(), player.horizontalCollision));
    }

    private void sendSprint(LocalPlayer player, ServerboundPlayerCommandPacket.Action action) {
        player.connection.send(new ServerboundPlayerCommandPacket(player, action));
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!renderDirection.get() || mc.player == null || !(mc.hitResult instanceof EntityHitResult hit)
            || !(hit.getEntity() instanceof LivingEntity target)) return;
        Vec3 start = target.getBoundingBox().getCenter();
        float yaw = yaw(mc.player, target);
        int range = spread.get();
        drawRay(event, start, yaw - range);
        if (range != 0) drawRay(event, start, yaw + range);
    }

    private void drawRay(Render3DEvent event, Vec3 start, float yaw) {
        Vec3 end = start.add(Vec3.directionFromRotation(0, yaw).scale(2));
        event.renderer.line(start.x, start.y, start.z, end.x, end.y, end.z, new SettingColor(255, 128, 0, 255));
    }
}
