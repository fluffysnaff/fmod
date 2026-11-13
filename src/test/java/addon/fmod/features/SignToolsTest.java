package addon.fmod.features;

import addon.fmod.features.signs.SignHistory;
import addon.fmod.features.signs.SignLines;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SignToolsTest {
    private final SignLines.Context context=new SignLines.Context("Nova",UUID.fromString("00000000-0000-0000-0000-000000000001"),Instant.parse("2026-11-13T00:00:00Z"),ZoneOffset.UTC,"FMod");
    private String line(SignLines.Mode mode,String value){return SignLines.format(mode,value,"yyyy-MM-dd",0,List.of("file text"),context);}
    private SignHistory.Snapshot snapshot(String text){return new SignHistory.Snapshot(List.of(text,"","",""),List.of("","","",""),"black","black",false,false,false);}
    @Test void formatsIdentityDatesAndEncodingsWithoutMutatingTheSource(){
        assertEquals("Nova was here",line(SignLines.Mode.WasHere,""));assertEquals("2026-11-13",line(SignLines.Mode.Timestamp,""));
        assertEquals("SGVsbG8=",line(SignLines.Mode.Base64,"Hello"));assertEquals("0x4869",line(SignLines.Mode.PrefixedHex,"Hi"));assertEquals("Uryyb!",line(SignLines.Mode.Rot13,"Hello!"));
        assertEquals("file text",line(SignLines.Mode.File,""));assertEquals(64,line(SignLines.Mode.HashedUUID,"").length());
    }
    @Test void preservesOriginalAndBoundsObservedVersions(){
        var original=snapshot("original");var record=new SignHistory("minecraft:overworld",0,64,0,"minecraft:oak_sign",List.of(original),1,false);
        for(int i=0;i<20;i++)record=record.observe(snapshot("edit "+i),"minecraft:oak_sign",i+2);
        assertEquals(8,record.versions().size());assertEquals(original,record.original());assertEquals("edit 19",record.latest().front().getFirst());assertTrue(record.changed());
        var removed=record.removed(100);assertTrue(removed.destroyed());assertFalse(removed.observe(removed.latest(),removed.block(),101).destroyed());
    }
}
