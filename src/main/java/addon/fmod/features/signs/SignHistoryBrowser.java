package addon.fmod.features.signs;

import addon.fmod.modules.CoordHider;
import addon.fmod.modules.SignHistorian;
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

public final class SignHistoryBrowser extends WindowScreen {
    private static final DateTimeFormatter OBSERVED = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private final SignHistorian owner;
    private WVerticalList rows;
    private WLabel status;
    private String query="";
    private List<SignHistory> displayed;
    private final Map<WLabel,SignHistory> labels=new HashMap<>();
    private final Map<WLabel,String> textLabels=new HashMap<>();
    private int page,lastPage=-1;
    private SignHistoryBrowser(SignHistorian owner){super(GuiThemes.get(),"Sign Historian");this.owner=owner;}
    public static void open(){var owner=Modules.get().get(SignHistorian.class);if(!owner.isActive())owner.toggle();Minecraft.getInstance().setScreenAndShow(new SignHistoryBrowser(owner));}
    @Override public void initWidgets(){
        status=add(theme.label("Loading private sign history...",600)).widget();var search=add(theme.textBox("")).minWidth(400).widget();search.action=()->{query=search.get().toLowerCase(Locale.ROOT);displayed=null;page=0;};
        var controls=add(theme.horizontalList()).widget();controls.add(theme.button("Previous")).widget().action=()->{if(page>0)page--;};controls.add(theme.button("Next")).widget().action=()->page++;
        rows=add(theme.verticalList()).widget();add(theme.label("History contains client observations, not creation dates or proof of authorship. Restoring text requires a server-authorized editor.",600));
    }
    @Override protected void onRenderBefore(GuiGraphicsExtractor graphics,int x,int y,float delta){
        var records=owner.histories();
        if(displayed!=records||page!=lastPage){
            displayed=records;List<SignHistory> found=records.stream().filter(record->record.versions().stream().anyMatch(version->String.join(" ",version.front()).toLowerCase(Locale.ROOT).contains(query)||String.join(" ",version.back()).toLowerCase(Locale.ROOT).contains(query)))
                .sorted(Comparator.comparingLong(SignHistory::observedAt).reversed()).toList();
            int pages=Math.max(1,(found.size()+9)/10);page=Math.min(page,pages-1);lastPage=page;rows.clear();labels.clear();textLabels.clear();status.set(found.size()+" signs; page "+(page+1)+"/"+pages);
            for(var record:found.subList(Math.min(page*10,found.size()),Math.min((page+1)*10,found.size()))){
                WLabel label=rows.add(theme.label(location(record),600)).widget();labels.put(label,record);
                var details=rows.add(theme.section(record.destroyed()?"Removed sign":record.changed()?"Changed sign":"Observed sign",false)).widget();
                details.add(theme.label("Last observed "+OBSERVED.format(Instant.ofEpochMilli(record.observedAt()))+"; "+record.versions().size()+" retained versions"));
                textLabels.put(details.add(theme.label("",600)).widget(),"Original front: "+String.join(" | ",record.original().front()));
                textLabels.put(details.add(theme.label("",600)).widget(),"Original back: "+String.join(" | ",record.original().back()));
                if(record.changed())textLabels.put(details.add(theme.label("",600)).widget(),"Latest: "+String.join(" | ",record.latest().front()));
                details.add(theme.label("Front "+record.original().frontColor()+", glow "+record.original().frontGlow()+"; back "+record.original().backColor()+", glow "+record.original().backGlow()+"; waxed "+record.original().waxed()));
            }
        }
        for(var entry:labels.entrySet())entry.getKey().set(location(entry.getValue()));
        for(var entry:textLabels.entrySet()){String text=CoordHider.mask(entry.getValue());if(!entry.getKey().get().equals(text))entry.getKey().set(text);}
        super.onRenderBefore(graphics,x,y,delta);
    }
    private String location(SignHistory record){return record.dimension().replace("minecraft:","")+" "+(CoordHider.active()!=null?"[coordinates hidden]":record.x()+" "+record.y()+" "+record.z());}
}
