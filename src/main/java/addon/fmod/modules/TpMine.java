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
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.math.Box;

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
        if (mc.player == null || mc.world == null) return;

        hitResult = mc.player.raycast(maxDistance.get(), 1f / 20f, false); // 1f/20f is tick delta, typically 1.0f for full tick
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
            pos = null; // Clear position if not looking at a block
            return;
        }

        pos = ((BlockHitResult) hitResult).getBlockPos();

        if (mc.options.attackKey.isPressed()) {
            Direction side = ((BlockHitResult) hitResult).getSide();
            BlockState state = mc.world.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(mc.world, pos);
            if (shape.isEmpty()) {
                shape = state.getOutlineShape(mc.world, pos);
            }

            double height = shape.isEmpty() ? 1 : shape.getMax(Direction.Axis.Y);
            double tx = pos.getX() + 0.5 + side.getOffsetX() * 0.5; // Target center of the side, or slightly offset
            double ty = pos.getY() + height; // Target top of the block or its shape
            double tz = pos.getZ() + 0.5 + side.getOffsetZ() * 0.5;

            Vec3d startPos = mc.player.getPos();
            Vec3d endPos = new Vec3d(tx, ty, tz); // This target position might be inside a block, adjust as needed

            // Exploit
            WarpUtils.warpTo(endPos, 9);

            // Break block
            BlockUtils.breakBlock(pos, true);

            // Go black
            WarpUtils.moveTo(startPos);
            mc.player.setPosition(startPos); // Sync client position
        }
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (!isRenderBlock.get() || pos == null || hitResult == null || hitResult.getType() != HitResult.Type.BLOCK || mc.world == null) {
            return;
        }
        VoxelShape shape = mc.world.getBlockState(pos).getOutlineShape(mc.world, pos);
        if (shape.isEmpty()) return;
        Box box = shape.getBoundingBox();
        render(event, pos, box);
    }

    private void render(Render3DEvent event, BlockPos bp, Box box) {
        event.renderer.box(bp.getX() + box.minX, bp.getY() + box.minY, bp.getZ() + box.minZ, bp.getX() + box.maxX, bp.getY() + box.maxY, bp.getZ() + box.maxZ, targetColor.get(), targetColor.get(), ShapeMode.Both, 0);
    }
}
