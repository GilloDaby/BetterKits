package com.gillodaby.betterkits.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class JsonUtil {

    private JsonUtil() {
    }

    public static Object parse(String raw) {
        return SimpleJson.parse(raw);
    }

    public static Object readFile(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return null;
        }
        String raw = Files.readString(path, StandardCharsets.UTF_8);
        return parse(raw);
    }

    public static void writeFile(Path path, Object value) throws IOException {
        if (path == null) {
            return;
        }
        String json = toJson(value);
        Files.createDirectories(path.getParent());
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    public static String toJson(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value, 0);
        sb.append("\n");
        return sb.toString();
    }

    public static Map<String, Object> asObjectMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    public static List<Object> asList(Object value) {
        if (!(value instanceof List<?> list)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(list);
    }

    public static String asString(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? fallback : text;
    }

    public static boolean asBoolean(Object value, boolean fallback) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            if ("true".equalsIgnoreCase(s)) {
                return true;
            }
            if ("false".equalsIgnoreCase(s)) {
                return false;
            }
        }
        return fallback;
    }

    public static long asLong(Object value, long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public static int asInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public static double asDouble(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public static List<String> asStringList(Object value) {
        List<Object> list = asList(value);
        List<String> result = new ArrayList<>();
        for (Object entry : list) {
            if (entry == null) {
                continue;
            }
            String text = String.valueOf(entry).trim();
            if (!text.isEmpty()) {
                result.add(text);
            }
        }
        return result;
    }

    public static Map<String, Object> sortedMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return new TreeMap<>();
        }
        return new TreeMap<>(map);
    }

    private static void writeValue(StringBuilder sb, Object value, int indent) {
        if (value == null) {
            sb.append("null");
            return;
        }
        if (value instanceof Map<?, ?> map) {
            writeObject(sb, map, indent);
            return;
        }
        if (value instanceof List<?> list) {
            writeArray(sb, list, indent);
            return;
        }
        if (value instanceof String text) {
            sb.append('"').append(escape(text)).append('"');
            return;
        }
        if (value instanceof Boolean || value instanceof Number) {
            sb.append(String.valueOf(value));
            return;
        }
        sb.append('"').append(escape(String.valueOf(value))).append('"');
    }

    private static void writeObject(StringBuilder sb, Map<?, ?> map, int indent) {
        sb.append("{");
        if (map.isEmpty()) {
            sb.append("}");
            return;
        }
        sb.append("\n");
        int index = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = entry.getKey() == null ? "" : String.valueOf(entry.getKey());
            indent(sb, indent + 2);
            sb.append('"').append(escape(key)).append('"').append(": ");
            writeValue(sb, entry.getValue(), indent + 2);
            index++;
            if (index < map.size()) {
                sb.append(',');
            }
            sb.append("\n");
        }
        indent(sb, indent);
        sb.append("}");
    }

    private static void writeArray(StringBuilder sb, List<?> list, int indent) {
        sb.append("[");
        if (list.isEmpty()) {
            sb.append("]");
            return;
        }
        sb.append("\n");
        for (int i = 0; i < list.size(); i++) {
            indent(sb, indent + 2);
            writeValue(sb, list.get(i), indent + 2);
            if (i < list.size() - 1) {
                sb.append(',');
            }
            sb.append("\n");
        }
        indent(sb, indent);
        sb.append("]");
    }

    private static void indent(StringBuilder sb, int indent) {
        sb.append(" ".repeat(Math.max(0, indent)));
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    private static final class SimpleJson {
        private final String input;
        private int index;

        private SimpleJson(String input) {
            this.input = input;
        }

        static Object parse(String raw) {
            if (raw == null) {
                return null;
            }
            SimpleJson parser = new SimpleJson(raw);
            Object value = parser.readValue();
            parser.skipWhitespace();
            return value;
        }

        private Object readValue() {
            skipWhitespace();
            if (index >= input.length()) {
                return null;
            }
            char c = input.charAt(index);
            return switch (c) {
                case '{' -> readObject();
                case '[' -> readArray();
                case '"' -> readString();
                case 't', 'f' -> readBoolean();
                case 'n' -> readNull();
                default -> readNumber();
            };
        }

        private Map<String, Object> readObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            index++;
            skipWhitespace();
            if (index < input.length() && input.charAt(index) == '}') {
                index++;
                return map;
            }
            while (index < input.length()) {
                skipWhitespace();
                String key = readString();
                skipWhitespace();
                if (index < input.length() && input.charAt(index) == ':') {
                    index++;
                }
                Object value = readValue();
                map.put(key, value);
                skipWhitespace();
                if (index < input.length() && input.charAt(index) == ',') {
                    index++;
                    continue;
                }
                if (index < input.length() && input.charAt(index) == '}') {
                    index++;
                    break;
                }
            }
            return map;
        }

        private List<Object> readArray() {
            List<Object> list = new ArrayList<>();
            index++;
            skipWhitespace();
            if (index < input.length() && input.charAt(index) == ']') {
                index++;
                return list;
            }
            while (index < input.length()) {
                Object value = readValue();
                list.add(value);
                skipWhitespace();
                if (index < input.length() && input.charAt(index) == ',') {
                    index++;
                    continue;
                }
                if (index < input.length() && input.charAt(index) == ']') {
                    index++;
                    break;
                }
            }
            return list;
        }

        private String readString() {
            if (input.charAt(index) != '"') {
                return "";
            }
            index++;
            StringBuilder sb = new StringBuilder();
            while (index < input.length()) {
                char c = input.charAt(index++);
                if (c == '"') {
                    break;
                }
                if (c == '\\' && index < input.length()) {
                    char esc = input.charAt(index++);
                    switch (esc) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (index + 3 < input.length()) {
                                String hex = input.substring(index, index + 4);
                                index += 4;
                                try {
                                    sb.append((char) Integer.parseInt(hex, 16));
                                } catch (NumberFormatException ignored) {
                                }
                            }
                        }
                        default -> sb.append(esc);
                    }
                    continue;
                }
                sb.append(c);
            }
            return sb.toString();
        }

        private Boolean readBoolean() {
            if (input.startsWith("true", index)) {
                index += 4;
                return Boolean.TRUE;
            }
            if (input.startsWith("false", index)) {
                index += 5;
                return Boolean.FALSE;
            }
            return Boolean.FALSE;
        }

        private Object readNull() {
            if (input.startsWith("null", index)) {
                index += 4;
            }
            return null;
        }

        private Double readNumber() {
            int start = index;
            while (index < input.length()) {
                char c = input.charAt(index);
                if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                    index++;
                    continue;
                }
                break;
            }
            String raw = input.substring(start, index);
            try {
                return Double.parseDouble(raw);
            } catch (NumberFormatException ignored) {
                return 0.0;
            }
        }

        private void skipWhitespace() {
            while (index < input.length()) {
                char c = input.charAt(index);
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                    index++;
                } else {
                    break;
                }
            }
        }
    }
}
