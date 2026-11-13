package addon.fmod.hud;

import addon.fmod.FMod;
import addon.fmod.features.containers.ContainerCounts;
import addon.fmod.features.containers.ContainerWorld;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.*;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;

import java.util.*;
import java.util.stream.Collectors;

/** FMod adaptation of BepHax's DubCounter HUD design (GPL-3.0); see THIRD_PARTY_NOTICES.md. */
public final class DubCounterHud extends HudElement {
    public static final HudElementInfo<DubCounterHud> INFO = new HudElementInfo<>(FMod.HUD_GROUP, "dub-counter", "Counts received storage blocks, full/partial doubles and physical capacity.", DubCounterHud::new);
    private final SettingGroup general = settings.getDefaultGroup();
    private final Setting<List<BlockEntityType<?>>> types = general.add(new StorageBlockListSetting.Builder()
        .name("containers").description("Received container types to count. Ender Chests add no independent physical capacity.")
        .defaultValue(BlockEntityTypes.CHEST, BlockEntityTypes.TRAPPED_CHEST, BlockEntityTypes.BARREL, BlockEntityTypes.SHULKER_BOX).build());
    private final Setting<Boolean> colors = general.add(new BoolSetting.Builder().name("shulker-colors").description("Break down shulkers by color.").defaultValue(false).build());
    private final Setting<Boolean> capacity = general.add(new BoolSetting.Builder().name("capacity").description("Show estimated physical slots; contents and nested boxes are not inferred.").defaultValue(true).build());
    private final Setting<Double> scale = general.add(new DoubleSetting.Builder().name("scale").description("Text scale.").defaultValue(1).range(0.5,3).sliderRange(0.5,2).build());
    private long countedRevision=-1;
    private Set<String> countedTypes=Set.of();
    private ContainerCounts.Result result;
    public DubCounterHud() { super(INFO); }

    public List<String> lines() {
        Set<String> selected = types.get().stream().map(type -> BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type).toString()).collect(Collectors.toSet());
        var world = ContainerWorld.INSTANCE;
        if(result==null||countedRevision!=world.revision()||!countedTypes.equals(selected)){
            result=ContainerCounts.count(world.blocks(),selected);countedRevision=world.revision();countedTypes=Set.copyOf(selected);
        }
        List<String> lines = new ArrayList<>();
        lines.add("Dub Counter | single " + result.singles() + ", double " + result.doubles() + ", partial " + result.partialDoubles());
        result.counts().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> lines.add(entry.getKey().replace("minecraft:", "") + ": " + entry.getValue()));
        if (colors.get()) result.shulkers().entrySet().stream().sorted(Map.Entry.<String,Integer>comparingByValue().reversed()).forEach(entry -> lines.add("  " + entry.getKey() + ": " + entry.getValue()));
        if (capacity.get()) lines.add("Physical slots: " + result.slots());
        lines.add("Received chunks: " + world.scannedChunks() + "/" + world.receivedChunks());
        if (world.incomplete()) lines.add("Storage limit reached; counts incomplete");
        return lines;
    }
    @Override public void render(HudRenderer renderer) {
        List<String> lines = lines();
        double width = 0, height = renderer.textHeight(true,scale.get()), top = y;
        for (String line : lines) {
            width = Math.max(width,renderer.textWidth(line,true,scale.get()));
            renderer.text(line,x,top,Color.WHITE,true,scale.get()); top += height;
        }
        setSize(width,lines.size()*height);
    }
}
