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
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.Set;

public class InfReach extends Module
{
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgRender = this.settings.createGroup("Render");
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


    public InfReach()
    {
        super(FMod.CATEGORY, "inf-reach", "Tries to teleport to the closest target near your aim and hit him from far away.");
    }

    private Entity closestEntity;
    public Entity getClosestEntity() {
        return closestEntity;
    }

    private Vec3d startPos = null;
    private Vec3d targetPos = null;

    @Override
    public void onDeactivate() {
        closestEntity = null;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) {
            closestEntity = null;
            return;
        }
        if (!mc.player.isAlive() || PlayerUtils.getGameMode() == GameMode.SPECTATOR) return;

        Vec3d origin = mc.player.getCameraPosVec(1.0F);
        Vec3d direction = mc.player.getRotationVec(1.0F).normalize();
        double maxDistance = range.get();
        double maxAngleThreshold = Math.toRadians(fov.get()); // 10 degrees

        Entity closest = null;
        double closestAngle = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (entity == null) continue;
            if (!canAttack(entity)) continue;

            Vec3d toEntity = entity.getPos().add(0, entity.getHeight() / 2.0, 0).subtract(origin);
            double distance = toEntity.length();
            if (distance > maxDistance) continue;

            double angle = Math.acos(direction.dotProduct(toEntity.normalize()));
            if (angle < closestAngle && angle <= maxAngleThreshold) {
                closest = entity;
                closestAngle = angle;
            }
        }

        closestEntity = closest;

        if (closestEntity == null) return;
        if (!canAttack(closestEntity)) return;
        if (!mc.options.attackKey.isPressed()) return;

        hitEntity(closestEntity);
    }

    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (closestEntity == null) return;

        Box box = closestEntity.getBoundingBox();
        Vec3d center = box.getCenter();
        double scaleFactor = scale.get();
        double halfLengthX = (box.getLengthX() * scaleFactor) / 2.0;
        double halfLengthY = (box.getLengthY() * scaleFactor) / 2.0;
        double halfLengthZ = (box.getLengthZ() * scaleFactor) / 2.0;

        Box scaledBox = new Box(
            center.x - halfLengthX, center.y - halfLengthY, center.z - halfLengthZ,
            center.x + halfLengthX, center.y + halfLengthY, center.z + halfLengthZ
        );

        event.renderer.box(scaledBox, color.get(), color.get(), ShapeMode.Both, 0);
    }

    private boolean canAttack(Entity entity) {
        if (entity.equals(mc.player) || entity.equals(mc.cameraEntity)) return false;
        if ((entity instanceof LivingEntity && ((LivingEntity) entity).isDead()) || !entity.isAlive()) return false;
        if (!PlayerUtils.isWithin(entity, range.get())) return false;
        if (!entities.get().contains(entity.getType())) return false;

        if (entity instanceof PlayerEntity player) {
            if (player.isCreative()) return false;
            if (!Friends.get().shouldAttack(player)) return false;
        }
        return true;
    }

    private void hitEntity(Entity target) {
        if (mc.interactionManager == null || mc.player == null || target == null) return;

        startPos = mc.player.getPos();
        targetPos = target.getPos();

        // Try detecting failed warp
        Vec3d currentPos = mc.player.getPos();
        if (currentPos.squaredDistanceTo(targetPos) < 0.5 && canAttack(target)) {
            // Too close to target (warp failed or glitch), go back
            WarpUtils.moveTo(startPos);
            mc.player.setPosition(startPos);
        }

        // Exploit
        WarpUtils.warpTo(targetPos);

        // Attack
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);

        // Move back
        WarpUtils.moveTo(startPos);
        mc.player.setPosition(startPos);
    }


}
