package addon.fmod.utils;

import addon.fmod.utils.BaseScoutExclusions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BaseScoutExclusionsTest {
    @TempDir Path temporary;

    @Test void importsQuotedDiscordCsvWithoutSavingPeopleOrHistory() throws IOException {
        Path input = temporary.resolve("export.csv");
        Files.writeString(input, "AuthorID,Author,Date,Content,Attachments\r\n"
            + "123456789012345678,PrivatePerson,2026-01-02,\"156 chest(s) at -123456, 789123 in overworld\",\r\n"
            + "123456789012345678,PrivatePerson,2026-01-02,\"0 chest(s) at -123456, 789123 in overworld\",\r\n"
            + "2,OtherPerson,2026-01-02,\"Found a kit\n0 chest(s) at -1234, -5678 in the_nether\",\r\n");
        ImportResult result = BaseScoutExclusions.importFile(input, 10000);
        assertEquals(3, result.records());
        assertEquals(2, result.entries().size());
        assertEquals(Dimension.nether, result.entries().get(1).dimension());
        Path saved = temporary.resolve("private.json");
        BaseScoutExclusions.save(saved, result.entries());
        String contents = Files.readString(saved);
        assertFalse(contents.contains("PrivatePerson"));
        assertFalse(contents.contains("123456789012345678"));
        assertFalse(contents.contains("chest(s)"));
        assertFalse(contents.contains("Author"));
        assertEquals(result.entries(), BaseScoutExclusions.load(saved));
    }

    @Test void keepsAmbiguousDimensionsForReviewAndExcludesBothInterpretations() throws IOException {
        Path input = temporary.resolve("unknown.txt");
        Files.writeString(input, "coords: -12345, 56789\ncoords: 23456, 67891 in overworld and nether\n");
        ImportResult result = BaseScoutExclusions.importFile(input, 10);
        assertEquals(2, result.reviewCount());
        Index index = new Index(result.entries(), 0);
        assertTrue(index.hit(-12345, 56789).orElseThrow().ambiguous());
        assertTrue(index.hit(-98760, 454312).isPresent());
        assertTrue(index.hit(23456L * 8, 67891L * 8).isPresent());
    }

    @Test void respectsDimensionScalingAndOriginalRadiusAndEndIsolation() {
        Index index = new Index(List.of(new Entry(-100, -200, Dimension.nether, 100),
            new Entry(10000, 20000, Dimension.overworld, 10), new Entry(30000, 40000, Dimension.end, 1000)), 20);
        assertTrue(index.hit(-740, -1520).isPresent());
        assertFalse(index.hit(-739, -1520).isPresent());
        assertFalse(index.hit(-100, -200).isPresent());
        assertTrue(index.hit(10020, 20000).isPresent());
        assertFalse(index.hit(10021, 20000).isPresent());
        assertFalse(index.hit(30000, 40000).isPresent());
    }

    @Test void handlesNegativeCellBoundariesAndLargeProjectedCoordinates() {
        Index index = new Index(List.of(new Entry(-16385, -16385, Dimension.overworld, 10000),
            new Entry(-29_999_984, 29_999_984, Dimension.nether, 250000)), 0);
        assertTrue(index.hit(-16384, -16384).isPresent());
        assertTrue(index.hit(-29_999_984L * 8, 29_999_984L * 8).isPresent());
        assertFalse(index.hit(0, 0).isPresent());
    }

    @Test void importsExplicitJsonAndStripsMetadataWhilePreservingTheLargerDuplicateRadius() throws IOException {
        Path input = temporary.resolve("export.json");
        Files.writeString(input, "{\"locations\":[{\"x\":1000,\"z\":2000,\"dimension\":\"nether\",\"radius\":50,\"author\":\"PrivatePerson\"},{\"x\":1000,\"z\":2000,\"dimension\":\"nether\",\"radius\":100}]}");
        ImportResult result = BaseScoutExclusions.importFile(input, 10);
        assertEquals(List.of(new Entry(1000, 2000, Dimension.nether, 100)), result.entries());
        assertEquals(result.entries(), BaseScoutExclusions.merge(result.entries(), result.entries()));
    }

    @Test void understandsNamedAxesAndTriplesWithoutImportingMessageIdsOrDates() throws IOException {
        Path input = temporary.resolve("coordinates.txt");
        Files.writeString(input, "Message 123456789012345678 on 2026-01-02\nX: -1,234,567 Y: 64 Z: 2,345,678 in overworld\ncoords: -765432, 70, 123456 in the_end\n");
        ImportResult result = BaseScoutExclusions.importFile(input, 500);
        assertEquals(2, result.entries().size());
        assertEquals(new Entry(-1234567, 2345678, Dimension.overworld, 500), result.entries().getFirst());
        assertEquals(new Entry(-765432, 123456, Dimension.end, 500), result.entries().getLast());
    }

    @Test void invalidFilesDoNotOverwriteAnExistingSavedList() throws IOException {
        Path saved = temporary.resolve("saved.json");
        BaseScoutExclusions.save(saved, List.of(new Entry(1000, 2000, Dimension.overworld, 100)));
        String before = Files.readString(saved);
        Path invalid = temporary.resolve("invalid.json");
        Files.writeString(invalid, "{\"locations\":[{\"x\":30000000,\"z\":0}]}");
        assertThrows(IOException.class, () -> BaseScoutExclusions.importFile(invalid, 100));
        Files.writeString(invalid, "[".repeat(33) + "]".repeat(33));
        assertThrows(IOException.class, () -> BaseScoutExclusions.importFile(invalid, 100));
        assertEquals(before, Files.readString(saved));
    }

    @Test void serverKeysArePrivateStableAndSeparatedFromOtherServersAndWorlds() {
        String first = BaseScoutExclusions.serverIdentity("6B6T.ORG:25565");
        assertEquals(first, BaseScoutExclusions.serverIdentity("6b6t.org."));
        Path path = BaseScoutExclusions.serverFile(temporary, first);
        assertEquals(path, BaseScoutExclusions.serverFile(temporary, first));
        assertFalse(path.getFileName().toString().contains("6b6t"));
        assertNotEquals(path, BaseScoutExclusions.serverFile(temporary, "world:private-world"));
        assertNotEquals(path, BaseScoutExclusions.serverFile(temporary, "server:another.example"));
        assertTrue(path.normalize().startsWith(temporary));
    }
}
