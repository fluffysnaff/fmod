package addon.fmod.utils;

import java.util.regex.Pattern;

/** Text-only redaction. Never use masked values in movement, pathfinding, or persistence. */
public final class CoordinateMask {
    private static final String NUMBER = "[-+]?\\d+(?:\\.\\d+)?";
    private static final Pattern LABELLED = Pattern.compile(
        "(?i)(\\b(?:xyz|coordinates?|coords|position|block(?: position)?|chunk(?: position)?|section|targeted (?:block|fluid|entity))\\s*[:=]\\s*)(?=[(\\[]?[-+]?\\d)[^\\n]*");
    private static final Pattern TRIPLE = Pattern.compile(
        "(?<![\\w.])" + NUMBER + "(?:\\s*,\\s*|\\s+/\\s+|\\s+)" + NUMBER
            + "(?:\\s*,\\s*|\\s+/\\s+|\\s+)" + NUMBER + "(?!\\w|\\.\\d)");
    private static final Pattern AXIS = Pattern.compile("(?i)(\\b[xyz]\\s*[:=]\\s*)" + NUMBER);
    private static final Pattern PAIR = Pattern.compile("(?i)(\\b(?:at|near)\\s*:?\\s*)" + NUMBER
        + "(?:\\s*,\\s*|\\s+)" + NUMBER + "(?!\\w|\\.\\d)");
    private static final Pattern TEMPLATE = Pattern.compile(
        "(?i)\\{[^}]*\\b(?:pos|opposite_dim_pos|block_pos|chunk_pos|coordinates|coords)\\b[^}]*}");

    private CoordinateMask() {}

    public static String redact(String text) {
        if (text == null || text.isEmpty()) return text;
        String masked = LABELLED.matcher(text).replaceAll("$1[hidden]");
        masked = TRIPLE.matcher(masked).replaceAll("[hidden]");
        masked = PAIR.matcher(masked).replaceAll("$1[hidden]");
        return AXIS.matcher(masked).replaceAll("$1[hidden]");
    }

    public static boolean containsCoordinateTemplate(String text) {
        return text != null && TEMPLATE.matcher(text).find();
    }
}
