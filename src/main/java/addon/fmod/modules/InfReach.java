package addon.fmod.modules;

import addon.fmod.FMod;
import addon.fmod.utils.WarpUtils;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.SwingMode;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameType;

import java.util.HashSet;
import java.util.Set;

import static addon.fmod.FMod.LOG;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class InfReach extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgTargeting = settings.createGroup("Targeting");

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The size of the marker.")
        .defaultValue(1.0d)
        .range(0.5d, 10.0d)
        .build()
    );

    private final Setting<SettingColor> color = sgRender.add(new ColorSetting.Builder()
        .name("color")
        .description("The color of the marker.")
        .defaultValue(new Color(255, 0, 255, 100))
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The maximum range the entity can be to attack it.")
        .defaultValue(99)
        .min(0)
        .sliderMax(99)
        .build()
    );

    private final Setting<Double> fov = sgGeneral.add(new DoubleSetting.Builder()
        .name("fov")
        .description("The maximum fov the entity can be in to attack it.")
        .defaultValue(10)
        .min(0)
        .sliderMax(50)
        .build()
    );

    private final Setting<Set<EntityType<?>>> entities = sgTargeting.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Entities to attack.")
        .onlyAttackable()
        .build()
    );

    private Entity closestEntity;

    public InfReach() {
        super(FMod.CATEGORY, "inf-reach", "Tries to teleport to the closest target near your aim and hit him from far away.");
    }

    public Entity getClosestEntity() {
        return closestEntity;
    }

    private final HashSet<ServerboundMovePlayerPacket> packets = new HashSet<>();
    @EventHandler
    public void onPacketSend(PacketEvent.Send event)
    {
        if(packets.isEmpty()) return;
        if ((event.packet instanceof ServerboundMovePlayerPacket) && !packets.remove(event.packet)) event.cancel();
    }

    @Override
    public void onDeactivate() {
        closestEntity = null;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null) {
            closestEntity = null;
            return;
        }

        if (!mc.player.isAlive() || PlayerUtils.getGameMode() == GameType.SPECTATOR) return;

        Vec3 origin = mc.player.getEyePosition(1.0F);
        Vec3 direction = mc.player.getViewVector(1.0F).normalize();
        double maxDistance = range.get();
        double maxAngleThreshold = Math.toRadians(fov.get());

        Entity currentClosest = null;
        double closestAngle = Double.MAX_VALUE;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == null || !canAttack(entity)) continue;

            Vec3 toEntity = entity.position().add(0, entity.getBbHeight() / 2.0, 0).subtract(origin);
            double distance = toEntity.length();
            if (distance > maxDistance) continue;

            double angle = Math.acos(direction.dot(toEntity.normalize()));
            if (angle < closestAngle && angle <= maxAngleThreshold) {
                currentClosest = entity;
                closestAngle = angle;
            }
        }
        closestEntity = currentClosest;

        if (closestEntity == null) return;
        if (!mc.options.keyAttack.isDown()) return;

        hitEntity(closestEntity);
    }

    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (closestEntity == null) return;

        AABB box = closestEntity.getBoundingBox();
        Vec3 center = box.getCenter();
        double scaleFactor = scale.get();

        double halfLengthX = (box.getXsize() * scaleFactor) / 2.0;
        double halfLengthY = (box.getYsize() * scaleFactor) / 2.0;
        double halfLengthZ = (box.getZsize() * scaleFactor) / 2.0;

        AABB scaledBox = new AABB(
            center.x - halfLengthX, center.y - halfLengthY, center.z - halfLengthZ,
            center.x + halfLengthX, center.y + halfLengthY, center.z + halfLengthZ
        );

        event.renderer.box(scaledBox, color.get(), color.get(), ShapeMode.Both, 0);
    }

    private boolean canAttack(Entity entity) {
        if (entity.equals(mc.player) || entity.equals(mc.getCameraEntity())) return false;
        if ((entity instanceof LivingEntity && ((LivingEntity) entity).isDeadOrDying()) || !entity.isAlive()) return false;
        if (!PlayerUtils.isWithin(entity, range.get())) return false;
        if (!entities.get().contains(entity.getType())) return false;

        if (entity instanceof Player player) {
            if (player.isCreative()) return false;
            if (!Friends.get().shouldAttack(player)) return false;
        }
        return true;
    }

    private void hitEntity(Entity target) {
        if (mc.gameMode == null || mc.player == null || target == null) return;

        Vec3 startPos = mc.player.position();
        Vec3 targetPos = target.position().subtract(  // Subtract a bit from the end
            target.position().subtract(startPos).normalize().scale(2)
        );

        mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));

        // Exploit
        warpTo(targetPos);

        // Attack
        mc.gameMode.attack(mc.player, target);
        SwingMode.Both.attackSwing(InteractionHand.MAIN_HAND);

        // Go back
        ServerboundMovePlayerPacket movePacket = new ServerboundMovePlayerPacket.Pos(startPos.x, startPos.y, startPos.z, true, mc.player.horizontalCollision);
        packets.add(movePacket);
        mc.player.connection.send(movePacket);
        mc.player.setPos(startPos);
    }

    public void warpTo(Vec3 pos) {
        if (mc.player == null) return;

        Vec3 newPos = new Vec3(pos.x + 0.5, pos.y + 1, pos.z + 0.5);
        int packetsRequired = (int) Math.ceil(mc.player.position().distanceTo(newPos) / 10) - 1;

        // Exploit
        for (int packetNumber = 0; packetNumber < (packetsRequired); packetNumber++) {
            ServerboundMovePlayerPacket movePacket = new ServerboundMovePlayerPacket.StatusOnly(true, mc.player.horizontalCollision);
            packets.add(movePacket);
            mc.player.connection.send(movePacket);
        }

        // Tp to pos
        ServerboundMovePlayerPacket movePacket = new ServerboundMovePlayerPacket.Pos(newPos.x, newPos.y, newPos.z, true, mc.player.horizontalCollision);
        packets.add(movePacket);
        mc.player.connection.send(movePacket);
        mc.player.setPos(newPos);
    }
}
