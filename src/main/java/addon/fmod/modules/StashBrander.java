package addon.fmod.modules;

import addon.fmod.FMod;
import addon.fmod.mixin.AnvilScreenAccessor;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.concurrent.TimeUnit;

/** Native bulk anvil branding; server result/cost updates authorize each output pickup. */
public final class StashBrander extends Module {
    private enum Stage { Idle, Input, Rename, Take }
    private final SettingGroup general=settings.getDefaultGroup();
    private final Setting<List<Item>> items=general.add(new ItemListSetting.Builder().name("items").description("Items to brand, or exclusions in blacklist mode. Empty whitelist renames nothing.").build());
    private final Setting<String> name=general.add(new StringSetting.Builder().name("custom-name").description("Desired name; blank removes a name when rename-named is enabled.").defaultValue("").build());
    private final Setting<Boolean> blacklist=general.add(new BoolSetting.Builder().name("blacklist").description("Brand all items except the item list.").defaultValue(false).build());
    private final Setting<Boolean> renameNamed=general.add(new BoolSetting.Builder().name("rename-named").description("Allow replacing existing custom names.").defaultValue(false).build());
    private final Setting<Integer> maxCost=general.add(new IntSetting.Builder().name("maximum-level-cost").description("Maximum experience levels to spend per rename.").defaultValue(1).range(1,39).sliderRange(1,10).build());
    private final Setting<Integer> delay=general.add(new IntSetting.Builder().name("click-interval-ticks").description("Minimum spacing between inventory actions; receipts still determine completion.").defaultValue(2).range(1,20).sliderRange(1,10).build());
    private final Setting<Boolean> close=general.add(new BoolSetting.Builder().name("close-on-done").description("Close only after all owned input/output slots are empty.").defaultValue(false).build());
    private Stage stage=Stage.Idle;
    private AnvilMenu owned;
    private ItemStack original=ItemStack.EMPTY,serverOutput=ItemStack.EMPTY;
    private boolean costReset,notified;
    private boolean acting;
    private long started;
    private int cooldown,renamed;
    private int serverCost=-1;

    public StashBrander(){super(FMod.CATEGORY,"stash-brander","Bulk-renames selected inventory items through an open anvil, with cost and server-receipt checks.");}
    private void reset(){owned=null;stage=Stage.Idle;original=serverOutput=ItemStack.EMPTY;costReset=false;cooldown=0;notified=false;serverCost=-1;}
    @Override public void onActivate(){reset();renamed=0;}
    @Override public void onDeactivate(){reset();}
    @EventHandler private void leave(GameLeftEvent event){reset();}
    private boolean wanted(ItemStack stack){
        if(stack.isEmpty()||(!blacklist.get()&&!items.get().contains(stack.getItem()))||(blacklist.get()&&items.get().contains(stack.getItem())))return false;
        if(stack.has(DataComponents.CUSTOM_NAME)&&!renameNamed.get())return false;
        return name.get().isBlank()?stack.has(DataComponents.CUSTOM_NAME):!stack.getHoverName().getString().equals(name.get());
    }
    private boolean named(ItemStack stack){return !stack.isEmpty()&&(name.get().isBlank()?!stack.has(DataComponents.CUSTOM_NAME):stack.getHoverName().getString().equals(name.get()));}
    private boolean preserved(ItemStack result){
        ItemStack before=original.copy(),after=result.copy();
        before.remove(DataComponents.CUSTOM_NAME);after.remove(DataComponents.CUSTOM_NAME);
        before.remove(DataComponents.REPAIR_COST);after.remove(DataComponents.REPAIR_COST);
        return ItemStack.isSameItemSameComponents(before,after)&&result.getCount()==original.getCount();
    }
    private void click(int slot){acting=true;try{mc.gameMode.handleContainerInput(owned.containerId,slot,0,ContainerInput.QUICK_MOVE,mc.player);}finally{acting=false;}}
    @EventHandler private void send(PacketEvent.Send event){
        if(owned!=null&&!acting&&(event.packet instanceof ServerboundContainerClickPacket||event.packet instanceof ServerboundRenameItemPacket)){
            warning("Manual anvil input detected; branding stopped. Existing inputs remain in place.");toggle();
        }
    }
    @EventHandler private void receive(PacketEvent.Receive event){
        var connection=event.connection;
        mc.execute(()->{
            if(!isActive()||owned==null||mc.getConnection()==null||mc.getConnection().getConnection()!=connection)return;
            if(event.packet instanceof ClientboundContainerSetSlotPacket packet&&packet.getContainerId()==owned.containerId){
                if(packet.getSlot()==2)serverOutput=packet.getItem().copy();
            }else if(event.packet instanceof ClientboundContainerSetContentPacket packet&&packet.containerId()==owned.containerId&&packet.items().size()>2){serverOutput=packet.items().get(2).copy();}
            else if(event.packet instanceof ClientboundContainerSetDataPacket packet&&packet.getContainerId()==owned.containerId&&packet.getId()==0){serverCost=packet.getValue();if(serverCost==0)costReset=true;}
        });
    }
    @EventHandler private void tick(TickEvent.Post event){
        if(mc.player==null||mc.level==null||mc.gameMode==null||!(mc.gui.screen() instanceof AnvilScreen screen)||!(mc.player.containerMenu instanceof AnvilMenu menu)){reset();return;}
        if(name.get().length()>AnvilMenu.MAX_NAME_LENGTH){if(!notified)error("Name exceeds the anvil's maximum length.");notified=true;return;}
        if(!menu.getCarried().isEmpty()||!menu.getSlot(1).getItem().isEmpty())return;
        if(owned!=null&&owned!=menu){reset();return;}
        if(cooldown>0){cooldown--;return;}
        if(stage!=Stage.Idle&&System.nanoTime()-started>TimeUnit.SECONDS.toNanos(8)){error("Rename was not confirmed. Recover the anvil inputs manually; automation stopped.");toggle();return;}
        if(stage==Stage.Idle){
            if(menu.getSlot(0).hasItem()||menu.getSlot(2).hasItem())return;
            int chosen=-1;for(int i=3;i<menu.slots.size();i++)if(wanted(menu.getSlot(i).getItem())){chosen=i;break;}
            if(chosen<0){if(!notified)info("Branding complete: %d renamed. No more matching items.",renamed);notified=true;if(close.get())mc.player.closeContainer();return;}
            notified=false;owned=menu;original=menu.getSlot(chosen).getItem().copy();serverOutput=ItemStack.EMPTY;started=System.nanoTime();stage=Stage.Input;
            click(chosen);cooldown=delay.get();
        }else if(stage==Stage.Input){
            if(menu.getSlot(0).getItem().isEmpty()||!ItemStack.isSameItemSameComponents(menu.getSlot(0).getItem(),original))return;
            original=menu.getSlot(0).getItem().copy();serverOutput=ItemStack.EMPTY;serverCost=-1;
            acting=true;try{((AnvilScreenAccessor)screen).fmod$name().setValue(name.get());}finally{acting=false;}stage=Stage.Rename;
        }else if(stage==Stage.Rename){
            if(!named(serverOutput)||!preserved(serverOutput)||!named(menu.getSlot(2).getItem()))return;
            int cost=serverCost;
            if(cost<1)return;
            if(cost>maxCost.get()||!menu.getSlot(2).mayPickup(mc.player)){warning("Rename needs %d levels or exceeds the configured cost. Inputs left in the anvil.",cost);toggle();return;}
            costReset=false;stage=Stage.Take;
            click(2);cooldown=delay.get();
        }else if(stage==Stage.Take&&costReset&&!menu.getSlot(0).hasItem()&&!menu.getSlot(2).hasItem()){
            renamed++;stage=Stage.Idle;owned=null;original=ItemStack.EMPTY;
        }
    }
    @Override public String getInfoString(){return renamed+" renamed";}
}
