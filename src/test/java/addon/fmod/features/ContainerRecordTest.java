package addon.fmod.features;

import addon.fmod.features.containers.ContainerRecord;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ContainerRecordTest {
    @Test void searchesNestedItemIdsAndCustomNamesAndKeepsAnImmutableSnapshot(){
        Map<String,Long> items=new HashMap<>(Map.of("minecraft:diamond",1728L,"minecraft:shulker_box",1L));
        var record=new ContainerRecord("minecraft:overworld",-20,64,30,"minecraft:barrel",27,items,Set.of("Recovery Kit"),100,true);
        items.clear();assertEquals(1729,record.totalItems());assertTrue(record.matches("DIAMOND"));assertTrue(record.matches("recovery"));assertFalse(record.matches("elytra"));
        assertThrows(UnsupportedOperationException.class,()->record.items().clear());
    }
    @Test void validatesPersistedBoundaries(){
        assertThrows(IllegalArgumentException.class,()->new ContainerRecord("minecraft:overworld",Integer.MIN_VALUE,64,0,"barrel",27,Map.of(),Set.of(),1,true));
        assertThrows(IllegalArgumentException.class,()->new ContainerRecord("minecraft:overworld",0,64,0,"barrel",27,Map.of("minecraft:diamond",Long.MAX_VALUE),Set.of(),1,true));
    }
}
