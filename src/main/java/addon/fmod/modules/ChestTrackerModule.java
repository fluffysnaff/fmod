package addon.fmod.modules;

import addon.fmod.FMod;
import addon.fmod.features.containers.ContainerBrowser;
import addon.fmod.features.containers.ContainerTracker;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;

public final class ChestTrackerModule extends Module {
    private final SettingGroup general=settings.getDefaultGroup();
    private final Setting<Boolean> autoOpen=general.add(new BoolSetting.Builder().name("auto-open").description("Inspect reachable containers automatically; off by default.").defaultValue(false).build());
    private final Setting<Double> range=general.add(new DoubleSetting.Builder().name("open-range").description("Normal interaction range only.").defaultValue(4).range(1,6).sliderRange(1,6).build());
    private final Setting<Boolean> ender=general.add(new BoolSetting.Builder().name("record-ender-chests").description("Record your own Ender Chest contents at visited sites. Data is separated by account.").defaultValue(false).build());
    private final Setting<Boolean> highlight=general.add(new BoolSetting.Builder().name("highlight-results").description("Highlight matching recorded containers in the current dimension.").defaultValue(true).build());
    private final Setting<Integer> renderRange=general.add(new IntSetting.Builder().name("render-range").description("Maximum distance for matching recorded locations.").defaultValue(128).range(8,512).sliderRange(8,256).build());
    public ChestTrackerModule(){super(FMod.CATEGORY,"chest-tracker","Records opened containers, searches nested contents and remembers private per-server/account locations.");}
    public boolean autoOpens(){return autoOpen.get();}
    public double openRange(){return range.get();}
    public boolean recordsEnderChests(){return ender.get();}
    @Override public void onDeactivate(){ContainerTracker.INSTANCE.flush();}
    @Override public String getInfoString(){return ContainerTracker.INSTANCE.records().size()+" recorded";}
    @Override public WWidget getWidget(GuiTheme theme){
        var list=theme.verticalList();list.add(theme.label("Records only contents the server sends after opening. Local files contain real coordinates.",420));
        list.add(theme.button("Browse / search containers")).widget().action=ContainerBrowser::open;
        list.add(theme.button("Rescan reachable containers")).widget().action=ContainerTracker.INSTANCE::rescan;
        return list;
    }
    @EventHandler private void render(Render3DEvent event){
        if(!highlight.get()||mc.player==null||mc.level==null)return;
        var tracker=ContainerTracker.INSTANCE;
        if(tracker.query().isBlank())return;
        int drawn=0;
        for(var record:tracker.records()){
            if(!record.dimension().equals(mc.level.dimension().identifier().toString())||!record.matches(tracker.query()))continue;
            BlockPos pos=new BlockPos(record.x(),record.y(),record.z());
            if(mc.player.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))>renderRange.get()*renderRange.get())continue;
            event.renderer.box(pos,new Color(160,80,255,30),new Color(190,130,255),ShapeMode.Both,0);
            if(++drawn>=512)break;
        }
    }
}
