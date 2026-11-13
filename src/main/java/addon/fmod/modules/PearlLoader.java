package addon.fmod.modules;

import addon.fmod.FMod;
import addon.fmod.utils.PrivateFiles;
import addon.fmod.utils.WorldScope;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/** Operator companion. A door receipt confirms only the door state, never a courier's arrival. */
public final class PearlLoader extends Module {
    private enum Stage { OpenBefore, Close, Hold, OpenAfter }
    public record Chamber(String name,String dimension,int x,int y,int z){
        public Chamber{if(name==null||!name.matches("[A-Za-z0-9_-]{1,32}")||dimension==null||dimension.length()>128||Math.abs((long)x)>30000000||Math.abs((long)z)>30000000||Math.abs((long)y)>4096)throw new IllegalArgumentException();}
    }
    public record Saved(int version,List<Chamber> chambers){public Saved{if(version!=1||chambers==null||chambers.size()>32)throw new IllegalArgumentException();chambers=List.copyOf(chambers);}}
    private final SettingGroup general=settings.getDefaultGroup();
    private final Setting<String> route=general.add(new StringSetting.Builder().name("chamber-name").description("Route name for marking and manual triggering.").defaultValue("courier").build());
    private final Setting<Double> hold=general.add(new DoubleSetting.Builder().name("closed-seconds").description("Desired closed pulse duration after server-confirmed closing.").defaultValue(1).range(0.1,10).sliderRange(0.1,5).build());
    private final Setting<Boolean> listen=general.add(new BoolSetting.Builder().name("signed-chat-triggers").description("Opt-in: exact signed player-chat triggers from trusted UUIDs. Unsigned/system/whisper text is ignored.").defaultValue(false).build());
    private final Setting<String> keyword=general.add(new StringSetting.Builder().name("trigger-keyword").description("Exact body: keyword followed by a chamber name.").defaultValue("!pearl").visible(listen::get).build());
    private final Setting<List<String>> trusted=general.add(new StringListSetting.Builder().name("trusted-uuids").description("Up to 128 player UUIDs. Names and formatted chat text are never authentication.").defaultValue(List.of()).visible(listen::get).build());
    private final Map<String,Chamber> chambers=new LinkedHashMap<>();
    private final AtomicReference<String> ioError=new AtomicReference<>();
    private String scope;
    private Path file;
    private CompletableFuture<Saved> loading;
    private boolean ready;
    private Operation operation;
    public PearlLoader(){super(FMod.CATEGORY,"pearl-loader","B's stasis trapdoor operator: local triggers or opt-in signed UUID-whitelisted chat. Does not assume a successful teleport.");}
    @Override public void onActivate(){reset();}
    @Override public void onDeactivate(){reset();}
    @EventHandler private void leave(GameLeftEvent event){reset();}
    private void reset(){chambers.clear();scope=null;loading=null;ready=false;operation=null;}
    public void mark(){
        if(mc.player==null||mc.level==null||!ready||!(mc.hitResult instanceof BlockHitResult hit)||!(mc.level.getBlockState(hit.getBlockPos()).getBlock() instanceof TrapDoorBlock)){warning("Wait for chamber data, then look at a trapdoor to mark it.");return;}
        if(!route.get().matches("[A-Za-z0-9_-]{1,32}")){warning("Chamber names use 1-32 letters, numbers, underscores or hyphens.");return;}
        if(chambers.size()>=32&&!chambers.containsKey(route.get())){warning("Chamber limit reached (32).");return;}
        operation=null;BlockPos pos=hit.getBlockPos();var chamber=new Chamber(route.get(),mc.level.dimension().identifier().toString(),pos.getX(),pos.getY(),pos.getZ());chambers.put(chamber.name,chamber);
        PrivateFiles.save(file,new Saved(1,List.copyOf(chambers.values())),ioError::set);info("Chamber marked; saving privately. Test recall with empty cargo.");
    }
    public void trigger(String name){
        if(!isActive()){warning("Enable PearlLoader and mark a chamber first.");return;}
        if(!ready||operation!=null){warning("Chamber data is loading or a door cycle is already running.");return;}
        Chamber chamber=chambers.get(name);if(chamber==null){warning("No chamber with that name is marked in this server.");return;}
        if(!canAct(chamber)){warning("Stand within reach, keep your main hand/cursor empty, and close inventory screens.");return;}
        operation=new Operation(chamber,System.nanoTime());
        info("Door cycle requested. Confirm that the courier actually arrives; a closed trapdoor is not proof of recall.");
    }
    private boolean canAct(Chamber chamber){
        if(mc.player==null||mc.level==null||mc.gameMode==null||!mc.player.isAlive()||!chamber.dimension.equals(mc.level.dimension().identifier().toString()))return false;
        BlockPos pos=new BlockPos(chamber.x,chamber.y,chamber.z);
        return mc.level.hasChunkAt(pos)&&mc.level.getBlockState(pos).getBlock() instanceof TrapDoorBlock&&mc.player.isWithinBlockInteractionRange(pos,0)
            &&mc.player.getMainHandItem().isEmpty()&&mc.player.containerMenu==mc.player.inventoryMenu&&mc.player.containerMenu.getCarried().isEmpty()&&!mc.player.isShiftKeyDown();
    }
    @Override public WWidget getWidget(GuiTheme theme){
        var list=theme.verticalList();list.add(theme.label("Manual B operation is the 6b6t default. Signed triggers are optional; unsupported unsigned whispers stay ignored.",430));
        list.add(theme.button("Mark looked-at trapdoor as current chamber")).widget().action=this::mark;
        list.add(theme.button("Trigger current chamber")).widget().action=()->trigger(route.get());
        list.add(theme.button("Cancel cycle (check door state manually)")).widget().action=()->{operation=null;info("Cycle cancelled. No further interactions will be sent.");};
        return list;
    }
    @EventHandler private void receive(PacketEvent.Receive event){
        var connection=event.connection;
        if(event.packet instanceof ClientboundBlockUpdatePacket packet)mc.execute(()->{
            if(!isActive()||operation==null||mc.getConnection()==null||mc.getConnection().getConnection()!=connection)return;
            Chamber chamber=operation.chamber;
            if(packet.getPos().equals(new BlockPos(chamber.x,chamber.y,chamber.z))&&packet.getBlockState().getBlock() instanceof TrapDoorBlock)operation.confirmed=packet.getBlockState().getValue(TrapDoorBlock.OPEN);
        });
        if(event.packet instanceof ClientboundPlayerChatPacket packet&&listen.get()&&packet.signature().isPresent()
            &&trusted.get().stream().limit(128).anyMatch(value->value.equalsIgnoreCase(packet.sender().toString()))){
            if(!keyword.get().matches("[A-Za-z0-9_!.-]{1,32}")||operation!=null)return;
            String body=packet.body().content();String prefix=keyword.get()+" ";
            if(body.startsWith(prefix)){
                String name=body.substring(prefix.length());
                if(name.matches("[A-Za-z0-9_-]{1,32}"))mc.execute(()->{
                    if(isActive()&&listen.get()&&body.equals(keyword.get()+" "+name)&&operation==null
                        &&trusted.get().stream().limit(128).anyMatch(value->value.equalsIgnoreCase(packet.sender().toString()))
                        &&mc.getConnection()!=null&&mc.getConnection().getConnection()==connection)trigger(name);
                });
            }
        }
    }
    @EventHandler private void tick(TickEvent.Post event){
        if(mc.level==null||mc.player==null)return;
        String current=WorldScope.current();if(current==null)return;
        if(!current.equals(scope)){reset();scope=current;file=WorldScope.file("pearl-loader",scope);try{loading=PrivateFiles.read(file,Saved.class,new Saved(1,List.of()));}catch(RejectedExecutionException error){error("Private file worker is busy; restart PearlLoader shortly.");}}
        if(loading!=null&&loading.isDone()){try{for(var chamber:loading.join().chambers)chambers.put(chamber.name,chamber);ready=true;info("Loaded %d private chambers. Use .pl mark / trigger <name>.",chambers.size());}catch(CompletionException error){error("Chamber data could not be read; existing files remain untouched.");}loading=null;}
        String failure=ioError.getAndSet(null);if(failure!=null)error("%s",failure);
        if(operation==null)return;
        if(!canAct(operation.chamber)||System.nanoTime()-operation.started>TimeUnit.SECONDS.toNanos(30)){operation=null;warning("Door cycle stopped: reach/context changed or receipt timed out. Check its state manually.");return;}
        Operation currentOperation=operation;
        if(currentOperation.stage==Stage.Hold){
            if(Boolean.TRUE.equals(currentOperation.confirmed)){operation=null;warning("Trapdoor changed while held; cycle cancelled.");return;}
            if(System.nanoTime()-currentOperation.closedAt>=hold.get()*1_000_000_000.0){currentOperation.stage=Stage.OpenAfter;currentOperation.sent=false;currentOperation.confirmed=null;}
            return;
        }
        boolean desired=currentOperation.stage!=Stage.Close;
        if(!currentOperation.sent){
            BlockPos pos=new BlockPos(currentOperation.chamber.x,currentOperation.chamber.y,currentOperation.chamber.z);
            if(mc.level.getBlockState(pos).getValue(TrapDoorBlock.OPEN)==desired){currentOperation.confirmed=desired;currentOperation.sent=true;}
            else{currentOperation.sent=true;currentOperation.confirmed=null;mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));}
        }
        if(currentOperation.confirmed==null||currentOperation.confirmed!=desired)return;
        if(currentOperation.stage==Stage.OpenBefore){currentOperation.stage=Stage.Close;currentOperation.sent=false;currentOperation.confirmed=null;}
        else if(currentOperation.stage==Stage.Close){currentOperation.stage=Stage.Hold;currentOperation.closedAt=System.nanoTime();}
        else{operation=null;info("Door cycle completed with an open trapdoor. Confirm courier arrival before the next shipment.");}
    }
    public String currentRoute(){return route.get();}
    @Override public String getInfoString(){return operation==null?chambers.size()+" chambers":operation.stage.toString();}
    private static final class Operation{
        private final Chamber chamber;private final long started;private Stage stage=Stage.OpenBefore;private boolean sent;private Boolean confirmed;private long closedAt;
        private Operation(Chamber chamber,long started){this.chamber=chamber;this.started=started;}
    }
}
