package addon.fmod.uiutils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UiPacketFieldsTest {
    @Test void preservesWireTypeBoundariesAndOutsideClickSlot() {
        var fields = UiPacketFields.parse(true, " 2147483647 ", "0", "-999", "-128", "100");
        assertEquals(Integer.MAX_VALUE, fields.container());
        assertEquals(-999, fields.slot());
        assertEquals(-128, fields.button());
        assertEquals(100, fields.repeats());
        assertEquals(Short.MAX_VALUE, UiPacketFields.parse(true, "0", "0", "32767", "127", "1").slot());
    }

    @Test void rejectsOverflowInvalidNumbersAndUnboundedBatches() {
        for (String value : new String[] { "", "no", "1.5", "2147483648", "-1" })
            assertThrows(IllegalArgumentException.class, () -> UiPacketFields.parse(true, value, "0", "0", "0", "1"));
        assertThrows(IllegalArgumentException.class, () -> UiPacketFields.parse(true, "0", "0", "32768", "0", "1"));
        assertThrows(IllegalArgumentException.class, () -> UiPacketFields.parse(true, "0", "0", "0", "128", "1"));
        for (String value : new String[] { "0", "101", "-1", "999999999999" })
            assertThrows(IllegalArgumentException.class, () -> UiPacketFields.parse(true, "0", "0", "0", "0", value));
    }

    @Test void buttonPacketsIgnoreInapplicableFieldsAndAllowFullWidthButtonIds() {
        var fields = UiPacketFields.parse(false, "5", "invalid", "invalid", "2147483647", "1");
        assertEquals(Integer.MAX_VALUE, fields.button());
        assertEquals(0, fields.revision());
        assertEquals(0, fields.slot());
    }
}
