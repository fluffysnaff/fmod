package addon.fmod.features.travel;

import java.util.ArrayDeque;

public final class TravelProgress {
    private record Sample(double seconds,double distance){}
    private final ArrayDeque<Sample> samples=new ArrayDeque<>();
    private long lastTime;
    private double lastX,lastZ,seconds,distance,total;
    private boolean started;
    public void reset(){samples.clear();started=false;seconds=distance=total=0;}
    public void sample(long now,double x,double z,int windowSeconds){
        if(!Double.isFinite(x)||!Double.isFinite(z)||windowSeconds<1||windowSeconds>1800)throw new IllegalArgumentException();
        if(!started){started=true;lastTime=now;lastX=x;lastZ=z;return;}
        double elapsed=(now-lastTime)/1_000_000_000.0;if(elapsed<=0)return;
        double moved=Math.hypot(x-lastX,z-lastZ);lastTime=now;lastX=x;lastZ=z;
        if(moved>Math.max(512,elapsed*1000)){samples.clear();seconds=distance=0;return;}
        samples.addLast(new Sample(elapsed,moved));seconds+=elapsed;distance+=moved;total+=moved;
        while(samples.size()>1&&seconds-samples.getFirst().seconds>=windowSeconds){Sample old=samples.removeFirst();seconds-=old.seconds;distance-=old.distance;}
        while(samples.size()>1800){Sample old=samples.removeFirst();seconds-=old.seconds;distance-=old.distance;}
    }
    public double speed(){return seconds>0?distance/seconds:0;}
    public double totalDistance(){return total;}
    public double eta(double remaining){return speed()>0.01?remaining/speed():Double.POSITIVE_INFINITY;}
}
