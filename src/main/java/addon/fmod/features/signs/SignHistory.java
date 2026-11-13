package addon.fmod.features.signs;

import java.util.*;

public record SignHistory(String dimension,int x,int y,int z,String block,List<Snapshot> versions,long observedAt,boolean destroyed) {
    public record Snapshot(List<String> front,List<String> back,String frontColor,String backColor,boolean frontGlow,boolean backGlow,boolean waxed){
        public Snapshot{
            if(front==null||back==null||front.size()!=4||back.size()!=4||front.stream().anyMatch(line->line==null||line.length()>384)||back.stream().anyMatch(line->line==null||line.length()>384)
                ||frontColor==null||backColor==null||frontColor.length()>32||backColor.length()>32)throw new IllegalArgumentException("Invalid sign snapshot.");
            front=List.copyOf(front);back=List.copyOf(back);
        }
    }
    public SignHistory{
        if(dimension==null||dimension.length()>128||block==null||block.length()>128||Math.abs((long)x)>30000000||Math.abs((long)z)>30000000||Math.abs((long)y)>4096
            ||versions==null||versions.isEmpty()||versions.size()>8)throw new IllegalArgumentException("Invalid sign history.");
        versions=List.copyOf(versions);
    }
    public String key(){return dimension+":"+x+":"+y+":"+z;}
    public Snapshot original(){return versions.getFirst();}
    public Snapshot latest(){return versions.getLast();}
    public boolean changed(){return versions.size()>1;}
    public SignHistory observe(Snapshot next,String block,long now){
        if(latest().equals(next)&&!destroyed)return this;
        List<Snapshot> history=new ArrayList<>(versions);
        if(!latest().equals(next)){if(history.size()==8)history.remove(1);history.add(next);}
        return new SignHistory(dimension,x,y,z,block,history,now,false);
    }
    public SignHistory removed(long now){return destroyed?this:new SignHistory(dimension,x,y,z,block,versions,now,true);}
}
