package addon.fmod.uiutils;

/** Validates the editable numeric fields against the 26.3 packet wire types. */
public record UiPacketFields(int container, int revision, short slot, int button, int repeats) {
    public static final int MAX_REPEATS = 100;

    public static UiPacketFields parse(boolean click, String container, String revision, String slot, String button, String repeats) {
        return new UiPacketFields(
            number("Sync ID", container, 0, Integer.MAX_VALUE),
            click ? number("Revision", revision, 0, Integer.MAX_VALUE) : 0,
            click ? (short) number("Slot", slot, Short.MIN_VALUE, Short.MAX_VALUE) : 0,
            number(click ? "Button" : "Button ID", button, click ? Byte.MIN_VALUE : 0, click ? Byte.MAX_VALUE : Integer.MAX_VALUE),
            number("Times to send", repeats, 1, MAX_REPEATS)
        );
    }

    private static int number(String name, String value, int min, int max) {
        try {
            int result = Integer.parseInt(value.strip());
            if (result >= min && result <= max) return result;
        } catch (NumberFormatException ignored) {
        }
        throw new IllegalArgumentException(name + " must be an integer from " + min + " to " + max + ".");
    }
}
