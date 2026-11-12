package addon.fmod.modules;

import addon.fmod.FMod;
import addon.fmod.utils.WarpUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.AABB;

public class TpMine extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Double> maxDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-distance")
        .description("The maximum distance you can teleport.")
        .defaultValue(30)
        .min(0)
        .build()
    );

    private final Setting<Boolean> isRenderBlock = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders the target block.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> targetColor = sgRender.add(new ColorSetting.Builder()
        .name("render-color")
        .description("Set target block render color.")
        .defaultValue(new SettingColor(255, 0, 255, 120))
        .visible(isRenderBlock::get)
        .build()
    );

    private HitResult hitResult;
    private BlockPos pos;

    public TpMine() {
        super(FMod.CATEGORY, "tp-mine", "Teleports you to the block silently and breaks it.");
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;

        hitResult = mc.player.pick(maxDistance.get(), 1f / 20f, false); // 1f/20f is tick delta, typically 1.0f for full tick
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
            pos = null; // Clear position if not looking at a block
            return;
        }

        pos = ((BlockHitResult) hitResult).getBlockPos();

        if (mc.options.keyAttack.isDown()) {
            Direction side = ((BlockHitResult) hitResult).getDirection();
            BlockState state = mc.level.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(mc.level, pos);
            if (shape.isEmpty()) {
                shape = state.getShape(mc.level, pos);
            }

            double height = shape.isEmpty() ? 1 : shape.max(Direction.Axis.Y);
            double tx = pos.getX() + 0.5 + side.getStepX() * 0.5; // Target center of the side, or slightly offset
            double ty = pos.getY() + height; // Target top of the block or its shape
            double tz = pos.getZ() + 0.5 + side.getStepZ() * 0.5;

            Vec3 startPos = mc.player.position();
            Vec3 endPos = new Vec3(tx, ty, tz); // This target position might be inside a block, adjust as needed

            // Exploit
            WarpUtils.warpTo(endPos, 9);

            // Break block
            BlockUtils.breakBlock(pos, true);

            // Go black
            WarpUtils.moveTo(startPos);
            mc.player.setPos(startPos); // Sync client position
        }
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (!isRenderBlock.get() || pos == null || hitResult == null || hitResult.getType() != HitResult.Type.BLOCK || mc.level == null) {
            return;
        }
        VoxelShape shape = mc.level.getBlockState(pos).getShape(mc.level, pos);
        if (shape.isEmpty()) return;
        AABB box = shape.bounds();
        render(event, pos, box);
    }

    private void render(Render3DEvent event, BlockPos bp, AABB box) {
        event.renderer.box(bp.getX() + box.minX, bp.getY() + box.minY, bp.getZ() + box.minZ, bp.getX() + box.maxX, bp.getY() + box.maxY, bp.getZ() + box.maxZ, targetColor.get(), targetColor.get(), ShapeMode.Both, 0);
    }
}
