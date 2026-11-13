package addon.fmod.modules;

import addon.fmod.FMod;
import addon.fmod.features.travel.TravelProgress;
import addon.fmod.utils.WorldScope;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoReconnect;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** Native travel monitor adapted from the BepHax/Stardust RoadTrip feature set. */
public final class RoadTrip extends Module {
    private final SettingGroup general=settings.getDefaultGroup(),guards=settings.createGroup("Optional logout guards",false);
    private final Setting<Integer> targetX=general.add(new IntSetting.Builder().name("target-x").description("Apply this target with the button below or .rt target.").defaultValue(0).range(-29999984,29999984).sliderRange(-100000,100000).build());
    private final Setting<Integer> targetZ=general.add(new IntSetting.Builder().name("target-z").description("Target in the current dimension; not automatically resumed on another server.").defaultValue(0).range(-29999984,29999984).sliderRange(-100000,100000).build());
    private final Setting<Integer> averaging=general.add(new IntSetting.Builder().name("average-speed-seconds").description("Real-time averaging window; distance is horizontal blocks.").defaultValue(60).range(1,1800).sliderRange(5,300).build());
    private final Setting<Integer> arrivalRange=general.add(new IntSetting.Builder().name("arrival-distance").description("Horizontal target distance considered arrived.").defaultValue(10).range(1,100).sliderRange(1,50).build());
    private final Setting<Boolean> logoutArrival=guards.add(new BoolSetting.Builder().name("logout-on-arrival").description("Disconnect normally when the applied target is reached.").defaultValue(false).build());
    private final Setting<Integer> logoutSeconds=guards.add(new IntSetting.Builder().name("logout-after-seconds").description("Session timer; zero disables.").defaultValue(0).range(0,86400).sliderRange(0,3600).build());
    private final Setting<Integer> minY=guards.add(new IntSetting.Builder().name("minimum-y").description("Disconnect below this Y; -4096 disables.").defaultValue(-4096).range(-4096,4096).sliderRange(-100,200).build());
    private final Setting<Integer> rockets=guards.add(new IntSetting.Builder().name("minimum-rockets").description("Disconnect below this loose rocket count; zero disables.").defaultValue(0).range(0,4096).sliderRange(0,128).build());
    private final Setting<Integer> food=guards.add(new IntSetting.Builder().name("minimum-food").description("Disconnect below this loose edible-item count; zero disables.").defaultValue(0).range(0,4096).sliderRange(0,128).build());
    private final Setting<Integer> elytras=guards.add(new IntSetting.Builder().name("minimum-spare-elytras").description("Healthy spare elytras in inventory; zero disables this guard.").defaultValue(0).range(0,36).sliderRange(0,5).build());
    private final Setting<Integer> stallLogout=guards.add(new IntSetting.Builder().name("stalled-logout-seconds").description("Disconnect after no horizontal progress; zero disables.").defaultValue(0).range(0,600).sliderRange(0,120).build());
    private final TravelProgress progress=new TravelProgress();
    private String scope,dimension;
    private boolean targetApplied,arrivalReported;
    private int ticks;
    private long started,lastMoved,lastWarning;
    private volatile long lastPacket;
    private double previousX,previousZ;
    public RoadTrip(){super(FMod.CATEGORY,"road-trip","Travel speed/ETA, durability and lag alerts, with opt-in normal-disconnect guards. Does not steer or send malformed packets.");}
    @Override public void onActivate(){reset();}
    @Override public void onDeactivate(){targetApplied=false;progress.reset();}
    @EventHandler private void join(GameJoinedEvent event){reset();}
    private void reset(){progress.reset();scope=null;dimension=null;targetApplied=arrivalReported=false;ticks=0;started=lastMoved=lastPacket=System.nanoTime();lastWarning=started-TimeUnit.SECONDS.toNanos(30);}
    public void target(int x,int z){
        if(Math.abs((long)x)>29999984||Math.abs((long)z)>29999984){warning("Target is outside the supported world bounds.");return;}
        if(mc.player==null||mc.level==null)return;if(!isActive())toggle();targetX.set(x);targetZ.set(z);scope=WorldScope.current();dimension=mc.level.dimension().identifier().toString();targetApplied=true;arrivalReported=false;info("Target applied in the current server and dimension. Coordinates remain in the settings, not chat.");
    }
    public void printStatus(){info("Average %.1f blocks/s; %.0f horizontal blocks travelled; %s",progress.speed(),progress.totalDistance(),etaText());}
    @Override public WWidget getWidget(GuiTheme theme){var list=theme.verticalList();list.add(theme.button("Apply configured target here")).widget().action=()->target(targetX.get(),targetZ.get());list.add(theme.button("Print speed and ETA")).widget().action=this::printStatus;list.add(theme.button("Reset trip statistics")).widget().action=this::reset;return list;}
    @EventHandler private void packet(PacketEvent.Receive event){lastPacket=System.nanoTime();}
    @EventHandler private void tick(TickEvent.Post event){
        if(mc.player==null||mc.level==null)return;
        String current=WorldScope.current(),dim=mc.level.dimension().identifier().toString();
        if(scope==null){scope=current;dimension=dim;previousX=mc.player.getX();previousZ=mc.player.getZ();}
        if(!Objects.equals(scope,current)||!Objects.equals(dimension,dim)){reset();scope=current;dimension=dim;warning("Trip context changed; target cleared. Apply a target in this server/dimension.");return;}
        long now=System.nanoTime();
        if(Math.hypot(mc.player.getX()-previousX,mc.player.getZ()-previousZ)>0.1){lastMoved=now;previousX=mc.player.getX();previousZ=mc.player.getZ();}
        if(++ticks%20!=0)return;
        progress.sample(now,mc.player.getX(),mc.player.getZ(),averaging.get());
        int rocketCount=0,foodCount=0,spares=0;
        for(int i=0;i<36;i++){
            ItemStack stack=mc.player.getInventory().getItem(i);if(stack.is(Items.FIREWORK_ROCKET))rocketCount+=stack.getCount();if(stack.has(DataComponents.FOOD))foodCount+=stack.getCount();if(healthyElytra(stack))spares++;
        }
        String reason=null;
        if(logoutSeconds.get()>0&&now-started>=TimeUnit.SECONDS.toNanos(logoutSeconds.get()))reason="configured trip timer elapsed";
        else if(minY.get()>-4096&&mc.player.getY()<minY.get())reason="below the configured minimum Y";
        else if(rockets.get()>0&&rocketCount<rockets.get())reason="low rocket supply";
        else if(food.get()>0&&foodCount<food.get())reason="low food supply";
        else if(elytras.get()>0&&spares<elytras.get())reason="low healthy spare elytra supply";
        else if(stallLogout.get()>0&&now-lastMoved>=TimeUnit.SECONDS.toNanos(stallLogout.get()))reason="no horizontal progress";
        if(reason!=null){logout(reason);return;}
        if(targetApplied&&remaining()<=arrivalRange.get()&&!arrivalReported){arrivalReported=true;info("Arrived at the applied target.");if(logoutArrival.get()){logout("target reached");return;}}
        ItemStack worn=mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if(now-lastWarning>=TimeUnit.SECONDS.toNanos(30)){
            if(worn.is(Items.ELYTRA)&&!healthyElytra(worn)){warning("Equipped elytra is at or below 5%% durability.");lastWarning=now;}
            else if(now-lastPacket>TimeUnit.SECONDS.toNanos(10)){warning("No server packets received for over 10 seconds.");lastWarning=now;}
            else if(targetApplied&&now-lastMoved>TimeUnit.SECONDS.toNanos(15)){warning("No horizontal progress toward the target.");lastWarning=now;}
        }
    }
    private boolean healthyElytra(ItemStack stack){return stack.is(Items.ELYTRA)&&stack.getMaxDamage()>0&&(stack.getMaxDamage()-stack.getDamageValue())/(double)stack.getMaxDamage()>0.05;}
    private double remaining(){return mc.player==null?Double.POSITIVE_INFINITY:Math.hypot(targetX.get()-mc.player.getX(),targetZ.get()-mc.player.getZ());}
    private String etaText(){if(!targetApplied)return "no target applied";double eta=progress.eta(remaining());return Double.isFinite(eta)?String.format(Locale.ROOT,"ETA %dm %02ds",(long)eta/60,(long)eta%60):"ETA unknown while stationary";}
    private void logout(String reason){
        info("Disconnecting: %s. AutoReconnect is disabled for this logout.",reason);
        var reconnect=Modules.get().get(AutoReconnect.class);if(reconnect!=null&&reconnect.isActive())reconnect.toggle();
        var connection=mc.getConnection()==null?null:mc.getConnection().getConnection();toggle();if(connection!=null)connection.disconnect(Component.literal("RoadTrip: "+reason));
    }
    @Override public String getInfoString(){return String.format(Locale.ROOT,"%.1f b/s | %s",progress.speed(),etaText());}
}
