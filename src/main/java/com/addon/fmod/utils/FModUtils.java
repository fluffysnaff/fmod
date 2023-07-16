package com.addon.fmod.utils;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;
import java.util.Random;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class FModUtils {
    public static void moveTo(Vec3d pos)
    {
        if (mc.player == null) return;

        if (mc.player.getVehicle() != null) {
            mc.player.getVehicle().setPosition(pos);
            Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new VehicleMoveC2SPacket(mc.player.getVehicle()));
        } else {
            mc.player.setPosition(pos);
            Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, true));
        }
    }
    public static void moveTo(Vec3d pos, float yaw, float pitch) {
        if (mc.player == null) return;

        if (mc.player.getVehicle() != null) {
            mc.player.getVehicle().setPosition(pos);
            Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new VehicleMoveC2SPacket(mc.player.getVehicle()));
        }
        else {
            mc.player.setPosition(pos);
            Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new PlayerMoveC2SPacket.Full(
                pos.x, pos.y, pos.z,
                yaw, pitch,
                mc.player.isOnGround()));
        }
    }

    public static void clipStraight(Vec3d targetPos) {
        if (mc.player == null) return;

        Vec3d pos = mc.player.getPos();

        for (int i = 0; i < 18; i++) {  // Send a lot of the same movement packets to increase max travel distance
            moveTo(pos);
        }
        // Send one big movement packet to actually move the player
        moveTo(targetPos);
    }

    // TODO: maybe refactor this to be the same as ClipReach#hitEntity
    public static void clipUpDown(Vec3d targetPos) {
        if (mc.player == null) return;

        Vec3d pos = mc.player.getPos();

        for (int i = 0; i < 15; i++) {  // Send a lot of the same movement packets to increase max travel distance
            moveTo(pos);
        }

        pos = pos.add(0, 100, 0);  // Up
        moveTo(pos);

        pos = new Vec3d(targetPos.x, pos.y, targetPos.z);  // Horizontal
        moveTo(pos);

        moveTo(targetPos);  // Down
    }

    /**
     * Automatically go through the nearest blocks in the given direction.
     * Credits to @EnderKill98 for the original code.
     */
    public static Vec3d getAutoClipPos(int direction) {
        if (mc.player == null || mc.world == null) return null;

        boolean inside = false;
        for (float i = 0; i < 150; i += 0.25) {
            Vec3d pos = mc.player.getPos();
            Vec3d targetPos = pos.add(0, direction * i, 0);

            boolean collides = !mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().offset(targetPos.subtract(pos)));

            if (!inside && collides) {  // Step 1: Into the blocks
                inside = true;
            } else if (inside && !collides) {  // Step 2: Out of the blocks
                return targetPos;
            }
        }

        return null;  // Nothing found
    }

}
