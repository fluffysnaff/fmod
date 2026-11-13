package addon.fmod.modules;

import addon.fmod.FMod;
import addon.fmod.features.signs.SignHistory;
import addon.fmod.features.signs.SignHistoryBrowser;
import addon.fmod.mixin.SignEditScreenAccessor;
import addon.fmod.utils.PrivateFiles;
import addon.fmod.utils.WorldScope;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/** Observed history is rendered separately; historical signs never replace the live client world. */
public final class SignHistorian extends Module {
    public record Saved(int version,List<SignHistory> signs){public Saved{if(version!=1||signs==null||signs.size()>5000)throw new IllegalArgumentException();signs=List.copyOf(signs);}}
    private final SettingGroup general=settings.getDefaultGroup();
    private final Setting<Boolean> persist=general.add(new BoolSetting.Builder().name("persistence").description("Save observed sign text and real coordinates privately per server.").defaultValue(true).build());
    private final Setting<Boolean> restore=general.add(new BoolSetting.Builder().name("restore-on-edit").description("Fill an authorized sign editor with the earliest observed text. Does not replace blocks or infer authors/dates.").defaultValue(false).build());
    private final Setting<Boolean> ignoreEmpty=general.add(new BoolSetting.Builder().name("ignore-empty").description("Do not create a new history from a blank sign. Clearing previously recorded text is still tracked.").defaultValue(true).build());
    private final Setting<Boolean> esp=general.add(new BoolSetting.Builder().name("highlight-changes").description("Outline changed and removed sign locations without modifying the world.").defaultValue(true).build());
    private final Setting<Integer> range=general.add(new IntSetting.Builder().name("render-range").description("Maximum marker range.").defaultValue(128).range(8,512).sliderRange(8,256).build());
    private final Map<String,SignHistory> histories=new LinkedHashMap<>();
    private final Set<BlockPos> unconfirmed=new HashSet<>();
    private final Set<BlockPos> updated=new LinkedHashSet<>();
    private final AtomicReference<String> ioError=new AtomicReference<>();
    private String scope;
    private Path file;
    private CompletableFuture<Saved> reading;
    private boolean ready,dirty;
    private int cursor,ticks;
    private AbstractSignEditScreen restored;
    private List<SignHistory> view=List.of();
    private Object level;
    public SignHistorian(){super(FMod.CATEGORY,"sign-historian","Privately records received sign text/style, detects changes/removal and can restore text through an authorized editor.");}
    @Override public void onActivate(){scope=null;histories.clear();view=List.of();ready=false;}
    @Override public void onDeactivate(){flush();clear();}
    @EventHandler private void leave(GameLeftEvent event){flush();clear();}
    private void clear(){histories.clear();view=List.of();unconfirmed.clear();updated.clear();scope=null;reading=null;ready=dirty=false;restored=null;level=null;cursor=ticks=0;}
    private void flush(){if(ready&&dirty&&persist.get()&&file!=null){PrivateFiles.save(file,new Saved(1,List.copyOf(histories.values())),ioError::set);dirty=false;}}
    public List<SignHistory> histories(){return view;}
    private String key(BlockPos pos){return mc.level.dimension().identifier()+":"+pos.getX()+":"+pos.getY()+":"+pos.getZ();}
    @Override public String getInfoString(){return histories.size()+" signs";}
    @Override public WWidget getWidget(GuiTheme theme){var button=theme.button("Browse original text and changes");button.action=SignHistoryBrowser::open;return button;}
    @EventHandler private void send(PacketEvent.Send event){if(event.packet instanceof ServerboundSignUpdatePacket packet&&unconfirmed.size()<100)unconfirmed.add(packet.pos());}
    @EventHandler private void receive(PacketEvent.Receive event){
        if(!(event.packet instanceof ClientboundBlockEntityDataPacket packet))return;
        var connection=event.connection;mc.execute(()->{
            if(!isActive()||mc.getConnection()==null||mc.getConnection().getConnection()!=connection)return;
            unconfirmed.remove(packet.getPos());if(updated.size()<8192)updated.add(packet.getPos());
        });
    }
    @EventHandler private void chunks(ChunkDataEvent event){unconfirmed.removeIf(pos->new net.minecraft.world.level.ChunkPos(pos.getX()>>4,pos.getZ()>>4).equals(event.chunk().getPos()));}
    public boolean wantsRestore(AbstractSignEditScreen editor){
        if(!restore.get()||mc.level==null)return false;
        // Native editor setters also update the preview, so retain ownership after applying it.
        if(restored==editor)return true;
        var access=(SignEditScreenAccessor)editor;var previous=histories.get(key(access.fmod$sign().getBlockPos()));
        if(previous==null)return false;
        List<String> original=access.fmod$slot()==SignTextSlot.FRONT?previous.original().front():previous.original().back();
        return !original.equals(access.fmod$sign().getText(access.fmod$slot()).getMessages(false).stream().map(text->text.getString()).toList());
    }
    @EventHandler private void tick(TickEvent.Post event){
        if(mc.player==null||mc.level==null)return;
        String current=WorldScope.current();if(current==null)return;
        if(!current.equals(scope)){
            flush();clear();scope=current;file=WorldScope.file("sign-history",scope);
            if(!persist.get())ready=true;
            else try{reading=PrivateFiles.read(file,Saved.class,new Saved(1,List.of()));}catch(RejectedExecutionException error){error("Private file worker is busy; restart SignHistorian shortly.");}
        }
        if(level!=mc.level){unconfirmed.clear();updated.clear();restored=null;level=mc.level;cursor=0;}
        if(reading!=null&&reading.isDone()){
            try{for(SignHistory record:reading.join().signs)histories.put(record.key(),record);ready=true;view=List.copyOf(histories.values());}
            catch(CompletionException error){error("Sign history could not be read. Existing files were left untouched.");}reading=null;
        }
        String failure=ioError.getAndSet(null);if(failure!=null)error("%s",failure);
        if(!ready)return;
        if(mc.gui.screen() instanceof AbstractSignEditScreen editor){
            if(restored!=editor&&wantsRestore(editor)){
                var access=(SignEditScreenAccessor)editor;var record=histories.get(key(access.fmod$sign().getBlockPos()));
                List<String> lines=access.fmod$slot()==SignTextSlot.FRONT?record.original().front():record.original().back();
                int selected=access.fmod$line();for(int i=0;i<4;i++){access.fmod$line(i);access.fmod$message(mc.font.plainSubstrByWidth(lines.get(i),access.fmod$sign().getMaxTextLineWidth()));}access.fmod$line(selected);restored=editor;
            }
        }else restored=null;
        if(!updated.isEmpty()){var iterator=updated.iterator();BlockPos pos=iterator.next();iterator.remove();observe(pos);}
        else{
            List<LevelChunk> loaded=new ArrayList<>();for(var chunk:Utils.chunks())if(chunk instanceof LevelChunk full&&loaded.size()<8192)loaded.add(full);
            if(!loaded.isEmpty())for(var entity:loaded.get(Math.floorMod(cursor++,loaded.size())).getBlockEntities().values())if(entity instanceof SignBlockEntity sign)observe(sign.getBlockPos());
        }
        if(++ticks%20==0){
            String dimension=mc.level.dimension().identifier().toString();
            for(var record:new ArrayList<>(histories.values())){
                if(!record.dimension().equals(dimension)||record.destroyed())continue;
                BlockPos pos=new BlockPos(record.x(),record.y(),record.z());
                if(mc.level.hasChunkAt(pos)&&!(mc.level.getBlockState(pos).getBlock() instanceof SignBlock)){histories.put(record.key(),record.removed(System.currentTimeMillis()));dirty=true;}
            }
            if(dirty)view=List.copyOf(histories.values());
        }
        if(ticks%100==0)flush();
    }
    private void observe(BlockPos pos){
        if(unconfirmed.contains(pos)||!mc.level.hasChunkAt(pos)||!(mc.level.getBlockEntity(pos) instanceof SignBlockEntity sign))return;
        if(mc.gui.screen() instanceof AbstractSignEditScreen editor&&((SignEditScreenAccessor)editor).fmod$sign().getBlockPos().equals(pos))return;
        var front=sign.getText(SignTextSlot.FRONT);var back=sign.getText(SignTextSlot.BACK);
        var snapshot=new SignHistory.Snapshot(front.getMessages(false).stream().map(text->text.getString()).toList(),back.getMessages(false).stream().map(text->text.getString()).toList(),
            front.getColor().getName(),back.getColor().getName(),front.hasGlowingText(),back.hasGlowingText(),sign.isWaxed());
        String key=key(pos);var old=histories.get(key);
        if(old==null&&ignoreEmpty.get()&&!front.hasMessage(false)&&!back.hasMessage(false))return;
        if(old==null&&histories.size()>=5000)return;
        String block=BuiltInRegistries.BLOCK.getKey(sign.getBlockState().getBlock()).toString();
        var next=old==null?new SignHistory(mc.level.dimension().identifier().toString(),pos.getX(),pos.getY(),pos.getZ(),block,List.of(snapshot),System.currentTimeMillis(),false):old.observe(snapshot,block,System.currentTimeMillis());
        if(next!=old){histories.put(key,next);view=List.copyOf(histories.values());dirty=true;}
    }
    @EventHandler private void render(Render3DEvent event){
        if(!esp.get()||mc.player==null||mc.level==null)return;int drawn=0;
        for(var record:view){
            if(!record.dimension().equals(mc.level.dimension().identifier().toString())||!record.changed()&&!record.destroyed())continue;
            BlockPos pos=new BlockPos(record.x(),record.y(),record.z());if(mc.player.position().distanceToSqr(Vec3.atCenterOf(pos))>range.get()*range.get())continue;
            Color color=record.destroyed()?new Color(255,80,80):new Color(255,200,50);event.renderer.box(pos,new Color(color.r,color.g,color.b,25),color,ShapeMode.Both,0);if(++drawn>=256)break;
        }
    }
}
