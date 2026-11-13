package addon.fmod.modules;

import addon.fmod.FMod;
import addon.fmod.features.signs.SignLines;
import addon.fmod.mixin.SignEditScreenAccessor;
import addon.fmod.utils.PrivateFiles;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.FileDialogs;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.world.level.block.entity.SignTextSlot;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** FMod adaptation of the BepHax/Stardust sign-template workflow, using the native editable sign screen. */
public final class SignatureSign extends Module {
    private final SettingGroup general=settings.getDefaultGroup();
    private final Setting<Boolean> front=general.add(new BoolSetting.Builder().name("front").description("Fill the front when its editor is opened.").defaultValue(true).build());
    private final Setting<Boolean> back=general.add(new BoolSetting.Builder().name("back").description("Fill the back when its editor is opened.").defaultValue(false).build());
    private final Setting<Boolean> overwrite=general.add(new BoolSetting.Builder().name("overwrite-existing").description("Allow replacing nonempty sign text. Off preserves existing signs.").defaultValue(false).build());
    private final Setting<Boolean> confirm=general.add(new BoolSetting.Builder().name("auto-confirm").description("Close the authorized editor after filling, submitting via Minecraft's normal sign update.").defaultValue(false).build());
    private final Setting<String> date=general.add(new StringSetting.Builder().name("date-format").description("Java date pattern for Timestamp lines.").defaultValue("yyyy-MM-dd").build());
    private final Setting<String> brand=general.add(new StringSetting.Builder().name("brand").description("Text for Brand lines.").defaultValue("FMod").build());
    private final List<Line> frontLines=new ArrayList<>(),backLines=new ArrayList<>();
    private List<String> fileLines=List.of();
    private CompletableFuture<List<String>> reading;
    private AbstractSignEditScreen processed;

    public SignatureSign(){
        super(FMod.CATEGORY,"signature-sign","Autofills front/back sign editors with text, identities, dates, file lines and encodings; existing signs are preserved by default.");
        for(int side=0;side<2;side++)for(int i=0;i<4;i++){
            SettingGroup group=settings.createGroup((side==0?"Front":"Back")+" line "+(i+1),false);
            SignLines.Mode initial=side==0?switch(i){case 0->SignLines.Mode.Brand;case 1->SignLines.Mode.WasHere;case 2->SignLines.Mode.Timestamp;default->SignLines.Mode.Empty;}:SignLines.Mode.Empty;
            var mode=group.add(new EnumSetting.Builder<SignLines.Mode>().name("mode").description("How this line is generated.").defaultValue(initial).build());
            var text=group.add(new StringSetting.Builder().name("text").description("Custom or encoded source text.").defaultValue("").build());
            var index=group.add(new IntSetting.Builder().name("file-line").description("Zero-based line from the selected text file.").defaultValue(i).range(0,8191).sliderRange(0,100).build());
            (side==0?frontLines:backLines).add(new Line(mode,text,index));
        }
    }
    @Override public void onActivate(){processed=null;info("Open a sign editor to apply the template. Existing text is preserved unless overwrite-existing is on.");}
    @Override public void onDeactivate(){processed=null;}
    @EventHandler private void leave(GameLeftEvent event){processed=null;}
    @Override public WWidget getWidget(GuiTheme theme){
        var list=theme.verticalList();list.add(theme.label("Username/UUID modes write real identity to the sign. CoordHider changes displays, not submitted text.",430));
        list.add(theme.button("Load local text file for File lines")).widget().action=()->FileDialogs.openFile("txt",file->{
            if(!isActive())return;
            if(reading!=null&&!reading.isDone()){warning("A template file is already loading.");return;}
            try{reading=PrivateFiles.readText(file.toPath());}catch(RejectedExecutionException error){error("Private file worker is busy; try again shortly.");}
        });return list;
    }
    @EventHandler private void tick(TickEvent.Post event){
        if(reading!=null&&reading.isDone()){try{fileLines=List.copyOf(reading.join());info("Loaded %d local template lines.",fileLines.size());}catch(CompletionException error){error("Template file could not be read (limit 1 MiB).");}reading=null;}
        if(mc.player==null||mc.level==null||!(mc.gui.screen() instanceof AbstractSignEditScreen editor)){processed=null;return;}
        if(processed==editor)return;
        processed=editor;
        var access=(SignEditScreenAccessor)editor;
        boolean isFront=access.fmod$slot()==SignTextSlot.FRONT;
        if(isFront?!front.get():!back.get())return;
        var historian=Modules.get().get(SignHistorian.class);
        if(historian!=null&&historian.isActive()&&historian.wantsRestore(editor))return;
        if(!overwrite.get()&&Arrays.stream(access.fmod$messages()).anyMatch(line->!line.isBlank()))return;
        try{
            var context=new SignLines.Context(mc.player.getName().getString(),mc.player.getUUID(),Instant.now(),ZoneId.systemDefault(),brand.get());
            String[] text=new String[4];List<Line> lines=isFront?frontLines:backLines;
            for(int i=0;i<4;i++){
                Line line=lines.get(i);
                if(line.mode.get()==SignLines.Mode.File&&line.index.get()>=fileLines.size())throw new IllegalArgumentException("Missing template line.");
                String raw=SignLines.format(line.mode.get(),line.text.get(),date.get(),line.index.get(),fileLines,context);
                text[i]=mc.font.plainSubstrByWidth(raw.substring(0,Math.min(384,raw.length())),access.fmod$sign().getMaxTextLineWidth());
            }
            int selected=access.fmod$line();for(int i=0;i<4;i++){access.fmod$line(i);access.fmod$message(text[i]);}access.fmod$line(selected);
            if(confirm.get())mc.gui.setScreen(null);
        }catch(RuntimeException error){error("Sign template is invalid; check the date pattern and file selection.");}
    }
    private record Line(Setting<SignLines.Mode> mode,Setting<String> text,Setting<Integer> index){}
}
