/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.ide.common.vectordrawable;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import java.util.Locale;
import java.util.Map;

import static java.util.Map.entry;

/** Methods for converting SVG color values to vector drawable format. */
public class SvgColor {
    /**
     * Color table from <a href="https://www.w3.org/TR/SVG11/types.html#ColorKeywords">Recognized
     * color keyword names</a>.
     */
    private static final Map<String, String> colorMap =
            Map.ofEntries(
                    entry("aliceblue", "#f0f8ff"),
                    entry("antiquewhite", "#faebd7"),
                    entry("aqua", "#00ffff"),
                    entry("aquamarine", "#7fffd4"),
                    entry("azure", "#f0ffff"),
                    entry("beige", "#f5f5dc"),
                    entry("bisque", "#ffe4c4"),
                    entry("black", "#000000"),
                    entry("blanchedalmond", "#ffebcd"),
                    entry("blue", "#0000ff"),
                    entry("blueviolet", "#8a2be2"),
                    entry("brown", "#a52a2a"),
                    entry("burlywood", "#deb887"),
                    entry("cadetblue", "#5f9ea0"),
                    entry("chartreuse", "#7fff00"),
                    entry("chocolate", "#d2691e"),
                    entry("coral", "#ff7f50"),
                    entry("cornflowerblue", "#6495ed"),
                    entry("cornsilk", "#fff8dc"),
                    entry("crimson", "#dc143c"),
                    entry("cyan", "#00ffff"),
                    entry("darkblue", "#00008b"),
                    entry("darkcyan", "#008b8b"),
                    entry("darkgoldenrod", "#b8860b"),
                    entry("darkgray", "#a9a9a9"),
                    entry("darkgrey", "#a9a9a9"),
                    entry("darkgreen", "#006400"),
                    entry("darkkhaki", "#bdb76b"),
                    entry("darkmagenta", "#8b008b"),
                    entry("darkolivegreen", "#556b2f"),
                    entry("darkorange", "#ff8c00"),
                    entry("darkorchid", "#9932cc"),
                    entry("darkred", "#8b0000"),
                    entry("darksalmon", "#e9967a"),
                    entry("darkseagreen", "#8fbc8f"),
                    entry("darkslateblue", "#483d8b"),
                    entry("darkslategray", "#2f4f4f"),
                    entry("darkslategrey", "#2f4f4f"),
                    entry("darkturquoise", "#00ced1"),
                    entry("darkviolet", "#9400d3"),
                    entry("deeppink", "#ff1493"),
                    entry("deepskyblue", "#00bfff"),
                    entry("dimgray", "#696969"),
                    entry("dimgrey", "#696969"),
                    entry("dodgerblue", "#1e90ff"),
                    entry("firebrick", "#b22222"),
                    entry("floralwhite", "#fffaf0"),
                    entry("forestgreen", "#228b22"),
                    entry("fuchsia", "#ff00ff"),
                    entry("gainsboro", "#dcdcdc"),
                    entry("ghostwhite", "#f8f8ff"),
                    entry("gold", "#ffd700"),
                    entry("goldenrod", "#daa520"),
                    entry("gray", "#808080"),
                    entry("grey", "#808080"),
                    entry("green", "#008000"),
                    entry("greenyellow", "#adff2f"),
                    entry("honeydew", "#f0fff0"),
                    entry("hotpink", "#ff69b4"),
                    entry("indianred", "#cd5c5c"),
                    entry("indigo", "#4b0082"),
                    entry("ivory", "#fffff0"),
                    entry("khaki", "#f0e68c"),
                    entry("lavender", "#e6e6fa"),
                    entry("lavenderblush", "#fff0f5"),
                    entry("lawngreen", "#7cfc00"),
                    entry("lemonchiffon", "#fffacd"),
                    entry("lightblue", "#add8e6"),
                    entry("lightcoral", "#f08080"),
                    entry("lightcyan", "#e0ffff"),
                    entry("lightgoldenrodyellow", "#fafad2"),
                    entry("lightgray", "#d3d3d3"),
                    entry("lightgrey", "#d3d3d3"),
                    entry("lightgreen", "#90ee90"),
                    entry("lightpink", "#ffb6c1"),
                    entry("lightsalmon", "#ffa07a"),
                    entry("lightseagreen", "#20b2aa"),
                    entry("lightskyblue", "#87cefa"),
                    entry("lightslategray", "#778899"),
                    entry("lightslategrey", "#778899"),
                    entry("lightsteelblue", "#b0c4de"),
                    entry("lightyellow", "#ffffe0"),
                    entry("lime", "#00ff00"),
                    entry("limegreen", "#32cd32"),
                    entry("linen", "#faf0e6"),
                    entry("magenta", "#ff00ff"),
                    entry("maroon", "#800000"),
                    entry("mediumaquamarine", "#66cdaa"),
                    entry("mediumblue", "#0000cd"),
                    entry("mediumorchid", "#ba55d3"),
                    entry("mediumpurple", "#9370db"),
                    entry("mediumseagreen", "#3cb371"),
                    entry("mediumslateblue", "#7b68ee"),
                    entry("mediumspringgreen", "#00fa9a"),
                    entry("mediumturquoise", "#48d1cc"),
                    entry("mediumvioletred", "#c71585"),
                    entry("midnightblue", "#191970"),
                    entry("mintcream", "#f5fffa"),
                    entry("mistyrose", "#ffe4e1"),
                    entry("moccasin", "#ffe4b5"),
                    entry("navajowhite", "#ffdead"),
                    entry("navy", "#000080"),
                    entry("oldlace", "#fdf5e6"),
                    entry("olive", "#808000"),
                    entry("olivedrab", "#6b8e23"),
                    entry("orange", "#ffa500"),
                    entry("orangered", "#ff4500"),
                    entry("orchid", "#da70d6"),
                    entry("palegoldenrod", "#eee8aa"),
                    entry("palegreen", "#98fb98"),
                    entry("paleturquoise", "#afeeee"),
                    entry("palevioletred", "#db7093"),
                    entry("papayawhip", "#ffefd5"),
                    entry("peachpuff", "#ffdab9"),
                    entry("peru", "#cd853f"),
                    entry("pink", "#ffc0cb"),
                    entry("plum", "#dda0dd"),
                    entry("powderblue", "#b0e0e6"),
                    entry("purple", "#800080"),
                    entry("rebeccapurple", "#663399"),
                    entry("red", "#ff0000"),
                    entry("rosybrown", "#bc8f8f"),
                    entry("royalblue", "#4169e1"),
                    entry("saddlebrown", "#8b4513"),
                    entry("salmon", "#fa8072"),
                    entry("sandybrown", "#f4a460"),
                    entry("seagreen", "#2e8b57"),
                    entry("seashell", "#fff5ee"),
                    entry("sienna", "#a0522d"),
                    entry("silver", "#c0c0c0"),
                    entry("skyblue", "#87ceeb"),
                    entry("slateblue", "#6a5acd"),
                    entry("slategray", "#708090"),
                    entry("slategrey", "#708090"),
                    entry("snow", "#fffafa"),
                    entry("springgreen", "#00ff7f"),
                    entry("steelblue", "#4682b4"),
                    entry("tan", "#d2b48c"),
                    entry("teal", "#008080"),
                    entry("thistle", "#d8bfd8"),
                    entry("tomato", "#ff6347"),
                    entry("turquoise", "#40e0d0"),
                    entry("violet", "#ee82ee"),
                    entry("wheat", "#f5deb3"),
                    entry("white", "#ffffff"),
                    entry("whitesmoke", "#f5f5f5"),
                    entry("yellow", "#ffff00"),
                    entry("yellowgreen", "#9acd32"));

    /** Do not instantiate. All methods are static. */
    private SvgColor() {}

    /**
     * Converts an SVG color value to "#RRGGBB" or "#AARRGGBB" format used by vector drawables.
     * The input color value can be "none" and RGB value, e.g. "rgb(255, 0, 0)",
     * "rgba(255, 0, 0, 127)", or a color name defined in
     * <a href="https://www.w3.org/TR/SVG11/types.html#ColorKeywords">Recognized color keyword names
     * </a>.
     *
     * @param svgColorValue the SVG color value to convert
     * @return the converted value, or null if the given value cannot be interpreted as color
     * @throws IllegalArgumentException if the supplied SVG color value has invalid or unsupported
     *     format
     */
    @Nullable
    protected static String colorSvg2Vd(@NonNull String svgColorValue) {
        String color = svgColorValue.trim();

        if (color.startsWith("#")) {
            // Convert RGBA to ARGB.
            if (color.length() == 5) {
                return '#' + color.substring(4) + color.substring(1, 4);
            } else if (color.length() == 9) {
                return '#' + color.substring(7) + color.substring(1, 7);
            }
            return color;
        }

        if ("none".equals(color)) {
            return "#00000000";
        }

        if (color.startsWith("rgb(") && color.endsWith(")")) {
            String rgb = color.substring(4, color.length() - 1);
            String[] numbers = rgb.split(",");
            if (numbers.length != 3) {
                throw new IllegalArgumentException(svgColorValue);
            }
            StringBuilder builder = new StringBuilder(7);
            builder.append("#");
            for (int i = 0; i < 3; i++) {
                int component = getColorComponent(numbers[i].trim(), svgColorValue);
                builder.append(String.format("%02X", component));
            }
            assert builder.length() == 7;
            return builder.toString();
        }

        if (color.startsWith("rgba(") && color.endsWith(")")) {
            String rgb = color.substring(5, color.length() - 1);
            String[] numbers = rgb.split(",");
            if (numbers.length != 4) {
                throw new IllegalArgumentException(svgColorValue);
            }
            StringBuilder builder = new StringBuilder(9);
            builder.append("#");
            for (int i = 0; i < 4; i++) {
                int component = getColorComponent(numbers[(i + 3) % 4].trim(), svgColorValue);
                builder.append(String.format("%02X", component));
            }
            assert builder.length() == 9;
            return builder.toString();
        }

        return colorMap.get(color.toLowerCase(Locale.ENGLISH));
    }

    private static int getColorComponent(
            @NonNull String colorComponent, @NonNull String svgColorValue) {
        try {
            if (colorComponent.endsWith("%")) {
                float value =
                        Float.parseFloat(colorComponent.substring(0, colorComponent.length() - 1));
                return clampColor(Math.round(value * 255.f / 100.f));
            }

            return clampColor(Integer.parseInt(colorComponent));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(svgColorValue);
        }
    }

    private static int clampColor(int val) {
        return Math.max(0, Math.min(255, val));
    }
}
