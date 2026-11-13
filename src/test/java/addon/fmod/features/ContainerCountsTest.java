package addon.fmod.features;

import addon.fmod.features.containers.ContainerCounts;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ContainerCountsTest {
    @Test void countsADoubleOnceEvenAcrossChunkBoundaries(){
        var left=new ContainerCounts.Block(15,64,0,"minecraft:chest",27,true,16,0,null);
        var right=new ContainerCounts.Block(16,64,0,"minecraft:chest",27,true,15,0,null);
        var result=ContainerCounts.count(List.of(left,right,left),Set.of("minecraft:chest"));
        assertEquals(1,result.doubles());assertEquals(0,result.singles());assertEquals(54,result.slots());assertEquals(1,result.counts().get("minecraft:chest"));
    }
    @Test void doesNotInventTheCapacityOfAnUnreceivedPartner(){
        var half=new ContainerCounts.Block(-17,64,0,"minecraft:chest",27,true,-16,0,null);
        var result=ContainerCounts.count(List.of(half),Set.of("minecraft:chest"));
        assertEquals(0,result.doubles());assertEquals(1,result.partialDoubles());assertEquals(27,result.slots());
    }
    @Test void respectsFiltersAndDoesNotMultiplySharedEnderStorage(){
        var box=new ContainerCounts.Block(0,64,0,"minecraft:shulker_box",27,false,0,0,"blue");
        var ender=new ContainerCounts.Block(1,64,0,"minecraft:ender_chest",0,false,1,0,null);
        var result=ContainerCounts.count(List.of(box,ender),Set.of("minecraft:shulker_box","minecraft:ender_chest"));
        assertEquals(27,result.slots());assertEquals(1,result.shulkers().get("blue"));
        assertTrue(ContainerCounts.count(List.of(box),Set.of("minecraft:barrel")).counts().isEmpty());
    }
}
