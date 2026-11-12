package addon.fmod.utils;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/** Text-only redaction. Never use masked values in movement, pathfinding, or persistence. */
public final class CoordinateMask {
    private static final String NUMBER = "[-+]?(?:\\d+(?:\\.\\d+)?|\\.\\d+)(?:[eE][-+]?\\d+)?";
    private static final Pattern LABELLED = Pattern.compile(
        "(?i)(\\b(?:xyz|coordinates?|coords|position|block(?: position)?|chunk(?: position)?|section|targeted (?:block|fluid|entity))\\s*[:=]\\s*)(?=[(\\[]?[-+]?(?:\\d|\\.\\d))[^\\n]*");
    private static final Pattern TRIPLE = Pattern.compile(
        "(?<![\\w.])" + NUMBER + "(?:\\s*,\\s*|\\s+/\\s+|\\s+)" + NUMBER
            + "(?:\\s*,\\s*|\\s+/\\s+|\\s+)" + NUMBER + "(?!\\w|\\.\\d)");
    private static final Pattern AXIS = Pattern.compile("(?i)(\\b[xyz]\\s*[:=]\\s*)" + NUMBER);
    private static final Pattern PAIR = Pattern.compile("(?i)(\\b(?:at|near)\\s*:?\\s*)" + NUMBER
        + "(?:\\s*,\\s*|\\s+)" + NUMBER + "(?!\\w|\\.\\d)");
    private static final Pattern TEMPLATE = Pattern.compile(
        "(?i)\\{[^}]*\\b(?:pos|opposite_dim_pos|block_pos|chunk_pos|coordinates|coords)\\b[^}]*}");
    private static final String SEPARATOR = "(?:\\s*,\\s*|\\s+/\\s+|\\s+)";
    private static final Pattern VALUES = Pattern.compile("(?<![\\w.])(" + NUMBER + ")(" + SEPARATOR
        + ")(" + NUMBER + ")(" + SEPARATOR + ")(" + NUMBER + ")(?!\\w|\\.\\d)");
    private static final Pattern LABEL_VALUES = Pattern.compile(
        "(?i)(\\b(?:xyz|coordinates?|coords|position|block(?: position)?|chunk(?: position)?|section(?:-relative)?|targeted (?:block|fluid|entity))\\s*[:=]\\s*)(?=[(\\[]?[-+]?(?:\\d|\\.\\d))([^\\n]*)");
    private static final Pattern PAIR_VALUES = Pattern.compile("(?i)(\\b(?:at|near)\\s*:?\\s*)(" + NUMBER
        + ")(" + SEPARATOR + ")(" + NUMBER + ")(?!\\w|\\.\\d|" + SEPARATOR + NUMBER + ")");
    private static final Pattern AXIS_VALUES = Pattern.compile("(?i)(\\b([xyz])\\s*[:=]\\s*)(" + NUMBER + ")");
    private static final Pattern EXPRESSION = Pattern.compile("\\{[^{}]*}");
    private static final Pattern POSITION_REFERENCE = Pattern.compile(
        "(?<![\\w.])(?:player|camera|crosshair_target(?:\\.value)?)\\.(pos|opposite_dim_pos)(?:\\.([xyz]))?(?![\\w.])");

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

    public static String randomize(String text, DisplayCoordinates coordinates) {
        if (text == null || text.isEmpty()) return text;
        String result = LABEL_VALUES.matcher(text).replaceAll(match -> {
            String label = match.group(1);
            Matcher tuple = VALUES.matcher(match.group(2));
            if (tuple.matches() && coordinates.isSyntheticTriple(number(tuple.group(1)), number(tuple.group(3)), number(tuple.group(5)))) {
                return Matcher.quoteReplacement(match.group());
            }
            String lowerLabel = label.toLowerCase(java.util.Locale.ROOT);
            String values = lowerLabel.contains("chunk") || lowerLabel.contains("section")
                ? coordinates.chunk().spaced() : coordinates.xyz();
            return Matcher.quoteReplacement(label + values);
        });
        result = VALUES.matcher(result).replaceAll(match -> coordinates.isSyntheticTriple(
            number(match.group(1)), number(match.group(3)), number(match.group(5)))
            ? Matcher.quoteReplacement(match.group()) : Matcher.quoteReplacement(coordinates.x() + match.group(2)
                + coordinates.y() + match.group(4) + coordinates.z()));
        result = PAIR_VALUES.matcher(result).replaceAll(match -> coordinates.isSyntheticPair(number(match.group(2)), number(match.group(4)))
            ? Matcher.quoteReplacement(match.group()) : Matcher.quoteReplacement(match.group(1) + coordinates.x() + match.group(3) + coordinates.z()));
        return AXIS_VALUES.matcher(result).replaceAll(match -> coordinates.isSyntheticAxis(match.group(2).charAt(0), number(match.group(3)))
            ? Matcher.quoteReplacement(match.group()) : Matcher.quoteReplacement(match.group(1) + coordinates.axis(match.group(2).charAt(0))));
    }

    /** Replace position reads before a HUD script runs, including expressions split into colour sections. */
    public static String randomizeTemplate(String template, DisplayCoordinates coordinates, boolean inNether) {
        if (template == null || template.isEmpty()) return template;
        return EXPRESSION.matcher(template).replaceAll(expression -> {
            String transformed = POSITION_REFERENCE.matcher(expression.group()).replaceAll(reference -> {
                DisplayCoordinates values = reference.group(1).equals("opposite_dim_pos") ? coordinates.opposite(inNether) : coordinates;
                String replacement = reference.group(2) == null ? "\"" + values.xyz() + "\""
                    : "(" + values.axis(reference.group(2).charAt(0)) + ")";
                return Matcher.quoteReplacement(replacement);
            });
            return Matcher.quoteReplacement(transformed);
        });
    }

    private static double number(String value) { return Double.parseDouble(value); }
}
