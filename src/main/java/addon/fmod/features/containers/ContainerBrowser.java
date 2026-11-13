package addon.fmod.features.containers;

import addon.fmod.modules.ChestTrackerModule;
import addon.fmod.modules.CoordHider;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class ContainerBrowser extends WindowScreen {
    private static final DateTimeFormatter OBSERVED = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private int page;
    private WLabel summary;
    private WVerticalList rows;
    private List<ContainerRecord> shown;
    private String shownQuery;
    private int shownPage=-1;
    private int matchCount,pageCount=1;
    private final Map<WLabel,ContainerRecord> positions=new HashMap<>();
    private ContainerBrowser(){super(GuiThemes.get(),"Chest Tracker");}
    public static void open(){
        var tracker=Modules.get().get(ChestTrackerModule.class);if(!tracker.isActive())tracker.toggle();
        Minecraft.getInstance().setScreenAndShow(new ContainerBrowser());
    }
    @Override public void initWidgets(){
        summary=add(theme.label("Loading private catalog...",600)).widget();
        var search=add(theme.textBox(ContainerTracker.INSTANCE.query())).minWidth(450).widget();
        search.action=()->{ContainerTracker.INSTANCE.search(search.get());page=0;};
        var controls=add(theme.horizontalList()).widget();
        controls.add(theme.button("Previous")).widget().action=()->{if(page>0)page--;};
        controls.add(theme.button("Next")).widget().action=()->page++;
        rows=add(theme.verticalList()).widget();
        add(theme.label("Last observed contents; predicted changes are marked unverified. Only opening a container reveals contents.",600));
    }
    @Override protected void onRenderBefore(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta){
        var store=ContainerTracker.INSTANCE;List<ContainerRecord> records=store.records();String query=store.query();
        if(shown!=records||!Objects.equals(shownQuery,query)||shownPage!=page){
            shown=records;shownQuery=query;
            List<ContainerRecord> matches=records.stream().filter(record->record.matches(query)).sorted(Comparator.comparingLong(ContainerRecord::observedAt).reversed()).toList();
            int pages=Math.max(1,(matches.size()+14)/15);page=Math.min(page,pages-1);shownPage=page;
            matchCount=matches.size();pageCount=pages;
            rows.clear();positions.clear();
            for(ContainerRecord record:matches.subList(Math.min(page*15,matches.size()),Math.min((page+1)*15,matches.size()))){
                var line=rows.add(theme.horizontalList()).widget();
                WLabel position=line.add(theme.label(position(record))).widget();positions.put(position,record);
                line.add(theme.label(record.kind().replace("minecraft:","")+" | "+record.totalItems()+" items"+(record.verified()?"":" (unverified)")));
                var details=rows.add(theme.section("Contents: "+record.items().size()+" item types",false)).widget();
                details.add(theme.label("Observed "+OBSERVED.format(Instant.ofEpochMilli(record.observedAt()))+"; "+(record.verified()?"server snapshot":"unverified prediction")));
                record.items().entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed()).limit(100)
                    .forEach(entry->details.add(theme.label(entry.getKey()+": "+entry.getValue())));
            }
        }
        String text=matchCount+" matches; page "+(page+1)+"/"+pageCount+". "+store.status();if(!summary.get().equals(text))summary.set(text);
        for(var entry:positions.entrySet()){String location=position(entry.getValue());if(!entry.getKey().get().equals(location))entry.getKey().set(location);}
        super.onRenderBefore(graphics,mouseX,mouseY,delta);
    }
    private String position(ContainerRecord record){return record.dimension().replace("minecraft:","")+" "+(CoordHider.active()!=null?"[coordinates hidden]":record.x()+" "+record.y()+" "+record.z());}
}
