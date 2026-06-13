package com.iridium126.hextricks.util;

public final class MetadataEscaper {
    private MetadataEscaper() {
    }

    public static String sanitize(String value) {
        if (value == null) {
            return "null";
        }
        if (!needsSanitizing(value)) {
            return value;
        }
        return value.replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n")
                .replace(",", "\\u002C").replace("[", "\\u005B").replace("]", "\\u005D")
                .replace("<", "\\u003C").replace(">", "\\u003E");
    }

    public static String desanitize(String value) {
        if (value == null || value.indexOf('\\') < 0) {
            return value;
        }
        return value
                .replace("\\u002C", ",")
                .replace("\\u005B", "[")
                .replace("\\u005D", "]")
                .replace("\\u003C", "<")
                .replace("\\u003E", ">")
                .replace("\\r", "\r")
                .replace("\\n", "\n")
                .replace("\\\\", "\\");
    }

    private static boolean needsSanitizing(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' || c == '\r' || c == '\n' || c == ',' || c == '[' || c == ']' || c == '<' || c == '>') {
                return true;
            }
        }
        return false;
    }
}
