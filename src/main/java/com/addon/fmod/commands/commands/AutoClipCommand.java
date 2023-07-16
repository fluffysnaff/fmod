package com.addon.fmod.commands.commands;

import com.addon.fmod.commands.arguments.PositionArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ClipCommand extends Command {
    public ClipCommand() {
        super("autoclip", "Allows to clip through ground up/down.", "ac");
    }

    {
    public static void moveTo(Vec3d pos)
        {
        if (client.player == null) return;

        if (client.player.getVehicle() != null) {
            client.player.getVehicle().setPosition(pos);
            networkHandler.sendPacket(new VehicleMoveC2SPacket(client.player.getVehicle()));
        } else {
            client.player.setPosition(pos);
            networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, true));
        }
    }
    public static void moveTo(Vec3d pos, float yaw, float pitch) {
        if (client.player == null) return;

        if (client.player.getVehicle() != null) {
            client.player.getVehicle().setPosition(pos);
            networkHandler.sendPacket(new VehicleMoveC2SPacket(client.player.getVehicle()));
        } else {
            client.player.setPosition(pos);
            networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                pos.x, pos.y, pos.z,
                yaw, pitch,
                client.player.isOnGround()));
        }
    }

    public static void clipStraight(Vec3d targetPos) {
        if (client.player == null) return;

        Vec3d pos = client.player.getPos();

        for (int i = 0; i < 18; i++) {  // Send a lot of the same movement packets to increase max travel distance
            moveTo(pos);
        }
        // Send one big movement packet to actually move the player
        moveTo(targetPos);
    }

    // TODO: maybe refactor this to be the same as ClipReach#hitEntity
    public static void clipUpDown(Vec3d targetPos) {
        if (client.player == null) return;

        Vec3d pos = client.player.getPos();

        for (int i = 0; i < 15; i++) {  // Send a lot of the same movement packets to increase max travel distance
            moveTo(pos);
        }

        pos = pos.add(0, 100, 0);  // Up
        moveTo(pos);

        pos = new Vec3d(targetPos.x, pos.y, targetPos.z);  // Horizontal
        moveTo(pos);

        moveTo(targetPos);  // Down
    }

    public static int executeAutoClip(CommandContext<FabricClientCommandSource> context, int direction) {
        if (client.player == null) return 0;

        Vec3d pos = getAutoClipPos(direction);
        if (pos == null) {
            context.getSource().sendFeedback(Text.of("§cNo valid position found within 150 blocks"));
            return 0;
        } else {
            context.getSource().sendFeedback(Text.of(String.format("§7Clipping §a%.0f§7 blocks", pos.y - (int) client.player.getPos().y)));
            clipStraight(pos);
            return 1;
        }
    }

    /**
     * Automatically go through the nearest blocks in the given direction.
     * Credits to @EnderKill98 for the original code.
     */
    public static Vec3d getAutoClipPos(int direction) {
        if (client.player == null || client.world == null) return null;

        boolean inside = false;
        for (float i = 0; i < 150; i += 0.25) {
            Vec3d pos = client.player.getPos();
            Vec3d targetPos = pos.add(0, direction * i, 0);

            boolean collides = !client.world.isSpaceEmpty(client.player, client.player.getBoundingBox().offset(targetPos.subtract(pos)));

            if (!inside && collides) {  // Step 1: Into the blocks
                inside = true;
            } else if (inside && !collides) {  // Step 2: Out of the blocks
                return targetPos;
            }
        }

        return null;  // Nothing found
    }
}
