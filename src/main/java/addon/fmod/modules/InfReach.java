package addon.fmod.modules;

import addon.fmod.FMod;
import addon.fmod.utils.WarpUtils;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Teleports to the closest target near your aim, attacks it, and returns.
 *
 * <p>Vanilla 26.3 range-checks the attack (3-block buffer) against the player's
 * server-side position when the attack packet arrives, so the forward move must be
 * sent and processed before the attack. The forward and return hops are each a
 * single positioned packet in their own tick (one per tick is the vanilla limit),
 * padded to the ~22-block/tick speed allowance, so the reachable range is capped at
 * that budget rather than being truly unlimited.
 */
public class InfReach extends Module {
    private static final double MAX_STEP = 20.0;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgTargeting = settings.createGroup("Targeting");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("Maximum distance to a target. Capped by the ~22-block per-tick movement budget on vanilla.")
        .defaultValue(6)
        .min(0)
        .sliderRange(0, 22)
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

    private final Setting<Double> scale = sgRender.add(new DoubleSetting.Builder()
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

    private Entity closestEntity;
    private Vec3 returnPos;
    private boolean releaseHold;

    public InfReach() {
        super(FMod.CATEGORY, "inf-reach", "Teleports to the closest target near your aim and hits it from range.");
    }

    public Entity getClosestEntity() {
        return closestEntity;
    }

    @Override
    public void onDeactivate() {
        closestEntity = null;
        if (returnPos != null || releaseHold) {
            WarpUtils.endHold();
            returnPos = null;
            releaseHold = false;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null) {
            closestEntity = null;
            return;
        }

        // Second tick of a hit: return to the original position.
        if (returnPos != null) {
            WarpUtils.sendPos(returnPos);
            returnPos = null;
            releaseHold = true;
            return;
        }

        if (!mc.player.isAlive() || PlayerUtils.getGameMode() == GameType.SPECTATOR) {
            closestEntity = null;
            return;
        }

        closestEntity = findTarget();
        if (closestEntity == null || !mc.options.keyAttack.isDown()) return;

        Vec3 start = mc.player.position();
        Vec3 aim = closestEntity.position().subtract(
            closestEntity.position().subtract(start).normalize().scale(2)
        );
        if (start.distanceTo(aim) > MAX_STEP) return; // out of the single-tick movement budget

        WarpUtils.beginHold();
        returnPos = start;
        WarpUtils.sendPos(aim);
        mc.gameMode.attack(mc.player, closestEntity);
        SwingMode.Both.attackSwing(InteractionHand.MAIN_HAND);
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        if (releaseHold) {
            WarpUtils.endHold();
            releaseHold = false;
        }
    }

    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (closestEntity == null) return;

        AABB box = closestEntity.getBoundingBox();
        Vec3 center = box.getCenter();
        double scaleFactor = scale.get();

        double halfX = (box.getXsize() * scaleFactor) / 2.0;
        double halfY = (box.getYsize() * scaleFactor) / 2.0;
        double halfZ = (box.getZsize() * scaleFactor) / 2.0;

        AABB scaledBox = new AABB(
            center.x - halfX, center.y - halfY, center.z - halfZ,
            center.x + halfX, center.y + halfY, center.z + halfZ
        );

        event.renderer.box(scaledBox, color.get(), color.get(), ShapeMode.Both, 0);
    }

    private Entity findTarget() {
        Vec3 origin = mc.player.getEyePosition(1.0F);
        Vec3 direction = mc.player.getViewVector(1.0F).normalize();
        double maxDistance = range.get();
        double maxAngle = Math.toRadians(fov.get());

        Entity currentClosest = null;
        double closestAngle = Double.MAX_VALUE;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == null || !canAttack(entity)) continue;

            Vec3 toEntity = entity.position().add(0, entity.getBbHeight() / 2.0, 0).subtract(origin);
            if (toEntity.length() > maxDistance) continue;

            double angle = Math.acos(direction.dot(toEntity.normalize()));
            if (angle < closestAngle && angle <= maxAngle) {
                currentClosest = entity;
                closestAngle = angle;
            }
        }

        return currentClosest;
    }

    private boolean canAttack(Entity entity) {
        if (entity.equals(mc.player) || entity.equals(mc.getCameraEntity())) return false;
        if ((entity instanceof LivingEntity living && living.isDeadOrDying()) || !entity.isAlive()) return false;
        if (!PlayerUtils.isWithin(entity, range.get())) return false;
        if (!entities.get().contains(entity.getType())) return false;

        if (entity instanceof Player player) {
            if (player.isCreative()) return false;
            if (!Friends.get().shouldAttack(player)) return false;
        }
        return true;
    }
}
