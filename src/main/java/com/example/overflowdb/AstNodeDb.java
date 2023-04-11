package com.example.overflowdb;

import com.example.AstNodeKind;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Contract;
import overflowdb.NodeDb;
import overflowdb.NodeLayoutInformation;
import overflowdb.NodeRef;

import java.awt.Color;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static com.example.overflowdb.AstNodeRef.BLUE;
import static com.example.overflowdb.AstNodeRef.COLOR;
import static com.example.overflowdb.AstNodeRef.KIND;
import static com.example.overflowdb.AstNodeRef.GREEN;
import static com.example.overflowdb.AstNodeRef.LABEL;
import static com.example.overflowdb.AstNodeRef.LABEL_V;
import static com.example.overflowdb.AstNodeRef.RED;
import static java.lang.String.format;
import static java.util.regex.Pattern.compile;

public final class AstNodeDb extends NodeDb {
    @Language("RegExp")
    private static final String HEX_CHARS = "[A-Fa-f0-9]";

    /**
     * Either RGB or RGBA.
     */
    private static final Pattern COLOR_PATTERN = compile(format("^#(?:%s{8}|%s{6})$", HEX_CHARS, HEX_CHARS));

    private @Nullable String label;

    private @Nullable String kind;

    /**
     * Either the symbolic name of a color, or {@code #rrggbb}.
     */
    private @Nullable String color;

    AstNodeDb(final NodeRef<AstNodeDb> nodeRef) {
        super(nodeRef);
    }

    public @Nullable String getLabel() {
        return label;
    }

    public @Nullable String getKind() {
        return kind;
    }

    public @Nullable String getColor() {
        return color;
    }

    public int getRed() {
        final @Nullable Integer red = getRedOrNull();
        return red == null ? 0 : red;
    }

    private @Nullable Integer getRedOrNull() {
        final Color parsedColor = parseColorSafe(color);
        return parsedColor == null ? null : parsedColor.getRed();
    }

    public int getGreen() {
        final @Nullable Integer green = getGreenOrNull();
        return green == null ? 0 : green;
    }

    private @Nullable Integer getGreenOrNull() {
        final Color parsedColor = parseColorSafe(color);
        return parsedColor == null ? null : parsedColor.getGreen();
    }

    public int getBlue() {
        final @Nullable Integer blue = getBlueOrNull();
        return blue == null ? 0 : blue;
    }

    private @Nullable Integer getBlueOrNull() {
        final Color parsedColor = parseColorSafe(color);
        return parsedColor == null ? null : parsedColor.getBlue();
    }

    @Override
    public NodeLayoutInformation layoutInformation() {
        return new NodeLayoutInformation(
                LABEL_V,
                Set.of(LABEL, KIND, COLOR, RED, GREEN, BLUE),
                List.of(AstChildEdge.LAYOUT_INFORMATION, AstNextSiblingEdge.LAYOUT_INFORMATION),
                List.of(AstChildEdge.LAYOUT_INFORMATION, AstNextSiblingEdge.LAYOUT_INFORMATION));
    }

    @Override
    public @Nullable Object property(final @NonNull String key) {
        return switch (key) {
            case LABEL -> label;
            case KIND -> kind;
            case COLOR -> color;
            case RED -> getRedOrNull();
            case GREEN -> getGreenOrNull();
            case BLUE -> getBlueOrNull();
            default -> null;
        };
    }

    @Override
    protected void updateSpecificProperty(final @NonNull String key, final @Nullable Object value) {
        switch (key) {
        case LABEL -> label = (String) value;
        case KIND -> {
            if (value instanceof String newKind) {
                kind = newKind;
            } else if (value instanceof AstNodeKind newKind) {
                kind = newKind.toString();
            }
        }
        case COLOR -> {
            if (value instanceof String newColor) {
                color = newColor;
            } else if (value instanceof Color newColor) {
                color = formatColor(newColor);
            }
        }
        case RED -> {
            final int red = value == null ? 0 : (int) value;
            final int green = getGreen();
            final int blue = getBlue();
            color = formatColor(new Color(red, green, blue));
        }
        case GREEN -> {
            final int red = getRed();
            final int green = value == null ? 0 : (int) value;
            final int blue = getBlue();
            color = formatColor(new Color(red, green, blue));
        }
        case BLUE -> {
            final int red = getRed();
            final int green = getGreen();
            final int blue = value == null ? 0 : (int) value;
            color = formatColor(new Color(red, green, blue));
        }
        }
    }

    @Override
    protected void removeSpecificProperty(final @NonNull String key) {
        updateSpecificProperty(key, null);
    }

    private static @NonNull Color parseColor(final @NonNull String color) {
        if (!COLOR_PATTERN.matcher(color).matches()) {
            throw new IllegalArgumentException("Not a color string: " + color);
        }

        final int red = Integer.parseInt(color.substring(1, 3), 16);
        final int green = Integer.parseInt(color.substring(3, 5), 16);
        final int blue = Integer.parseInt(color.substring(5, 7), 16);

        final int alpha = color.length() > 7
                          ? Integer.parseInt(color.substring(7, 9), 16)
                          : 255;

        return new Color(red, green, blue, alpha);
    }

    @Contract("null -> null")
    private static @Nullable Color parseColorSafe(final @Nullable String color) {
        if (color == null) {
            return null;
        }

        try {
            return parseColor(color);
        } catch (final IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * Ignores the <em>alpha</em>.
     */
    private static @NonNull String formatColor(final @NonNull Color color) {
        return format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
