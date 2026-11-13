package addon.fmod.modules;

import addon.fmod.FMod;
import addon.fmod.features.containers.ItemContents;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import java.util.IdentityHashMap;
import java.util.Map;

/** BepHax-inspired overlay, keyed by immutable contents instead of mutable ItemStack identities. */
public final class ShulkerOverviewModule extends Module {
    public enum Position { BottomRight, BottomLeft, TopRight, TopLeft, Center }
    private final SettingGroup general=settings.getDefaultGroup();
    private final Setting<Integer> size=general.add(new IntSetting.Builder().name("icon-size").description("Common-item overlay size.").defaultValue(10).range(4,16).sliderRange(4,16).build());
    private final Setting<Position> position=general.add(new EnumSetting.Builder<Position>().name("position").description("Common-item icon position.").defaultValue(Position.BottomRight).build());
    private final Setting<String> mixed=general.add(new StringSetting.Builder().name("mixed-indicator").description("Up to four characters when multiple item types are present.").defaultValue("+").build());
    private final Map<ItemContainerContents,ItemContents.Summary> cache=new IdentityHashMap<>();
    private boolean drawing;
    public ShulkerOverviewModule(){super(FMod.CATEGORY,"shulker-overview","Overlays the most common received item on shulker icons, with a mixed-content indicator.");}
    @Override public void onDeactivate(){cache.clear();}
    @EventHandler private void leave(GameLeftEvent event){cache.clear();}
    public static void overlay(GuiGraphicsExtractor graphics,ItemStack stack,int x,int y){
        if(Modules.get()==null)return;var module=Modules.get().get(ShulkerOverviewModule.class);
        if(module!=null&&module.isActive())module.draw(graphics,stack,x,y);
    }
    private void draw(GuiGraphicsExtractor graphics,ItemStack stack,int x,int y){
        if(drawing||stack.isEmpty()||!(stack.getItem() instanceof BlockItem item)||!(item.getBlock() instanceof ShulkerBoxBlock))return;
        ItemContainerContents contents=stack.get(DataComponents.CONTAINER);if(contents==null)return;
        var summary=cache.get(contents);
        if(summary==null){if(cache.size()>=128)cache.clear();summary=ItemContents.shulker(contents);cache.put(contents,summary);}
        if(summary.mostCommon()==null)return;
        int pixels=size.get(),iconX=x,iconY=y;
        switch(position.get()){
            case BottomRight->{iconX+=16-pixels;iconY+=16-pixels;}
            case BottomLeft->iconY+=16-pixels;
            case TopRight->iconX+=16-pixels;
            case Center->{iconX+=(16-pixels)/2;iconY+=(16-pixels)/2;}
            case TopLeft->{}
        }
        drawing=true;graphics.pose().pushMatrix();
        try{
            graphics.pose().translate(iconX,iconY);float factor=pixels/16f;graphics.pose().scale(factor,factor);
            graphics.item(summary.mostCommon().getDefaultInstance(),0,0);
        }finally{graphics.pose().popMatrix();drawing=false;}
        if(summary.mixed()&&!mixed.get().isEmpty())graphics.text(mc.font,mixed.get().substring(0,Math.min(4,mixed.get().length())),x+10,y,0xFFFFFF00,true);
    }
}
