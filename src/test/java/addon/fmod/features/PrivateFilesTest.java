package addon.fmod.features;

import addon.fmod.utils.PrivateFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class PrivateFilesTest {
    record Data(int version,String value){}
    @TempDir Path directory;
    @Test void savesTheLatestSnapshotAndReadsTheActualFile()throws Exception{
        Path file=directory.resolve("data.json");PrivateFiles.save(file,new Data(1,"first"),message->fail(message));PrivateFiles.save(file,new Data(1,"latest"),message->fail(message));
        var loaded=PrivateFiles.<Data>read(file,Data.class,new Data(1,"empty")).get(5,TimeUnit.SECONDS);
        assertEquals("latest",loaded.value());assertTrue(Files.isRegularFile(file));
    }
    @Test void rejectsMalformedDataWithoutChangingTheFile()throws Exception{
        Path file=directory.resolve("data.json");Files.writeString(file,"invalid json");
        assertThrows(ExecutionException.class,()->PrivateFiles.<Data>read(file,Data.class,new Data(1,"empty")).get(5,TimeUnit.SECONDS));assertEquals("invalid json",Files.readString(file));
    }
}
