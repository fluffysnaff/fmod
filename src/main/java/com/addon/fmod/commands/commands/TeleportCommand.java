package com.addon.fmod.commands.commands;

import com.addon.fmod.commands.arguments.PositionArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.systems.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class TeleportCommand extends Command {
    public TeleportCommand() {
        super("teleport", "Allows to teleport small distances.", "tp");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {

        builder.then(argument("pos", PositionArgumentType.pos()).then(argument("ticks", IntegerArgumentType.integer(0)).executes(context -> {
            Vec3d pos = PositionArgumentType.getPos(context, "pos");
            int ticks =  IntegerArgumentType.getInteger(context, "ticks");

            assert mc.player != null;
            mc.player.noClip = true;
            for (int i = 0; i < (ticks <= 0 ? 1 : ticks); i++)
            {
                if (mc.player.hasVehicle()) {
                    Entity vehicle = mc.player.getVehicle();

                    assert vehicle != null;
                    vehicle.setPosition(vehicle.getPos().getX(), vehicle.getPos().getY(), vehicle.getPos().getZ());
                    Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new VehicleMoveC2SPacket(vehicle));
                }

                Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getPos().getX(), mc.player.getPos().getY(), mc.player.getPos().getZ(), mc.player.isOnGround()));
                mc.player.updatePosition(mc.player.getPos().getX(), mc.player.getPos().getY(), mc.player.getPos().getZ());
            }

            if (mc.player.hasVehicle()) {
                Entity vehicle = mc.player.getVehicle();

                assert vehicle != null;
                vehicle.setPosition(pos.getX(), pos.getY(), pos.getZ());
                Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new VehicleMoveC2SPacket(vehicle));
            }

            Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.getX(), pos.getY(), pos.getZ(), mc.player.isOnGround()));
            mc.player.updatePosition(pos.getX(), pos.getY(), pos.getZ());

            return SINGLE_SUCCESS;
        })));

    }
}
