package util;

import java.lang.reflect.Method;
import java.util.*;

/**
 * JsonUtil - Pure Java JSON Serializer and Parser
 * 
 * Complies strictly with the constraint: NO external JSON libraries (no Jackson, Gson, org.json).
 * Suitable for college viva explainability.
 */
public class JsonUtil {

    // =========================================================================
    // SERIALIZATION (Object -> JSON String)
    // =========================================================================
    public static String toJson(Object obj) {
        if (obj == null) return "null";

        if (obj instanceof String) {
            return "\"" + escape((String) obj) + "\"";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(escape(String.valueOf(entry.getKey()))).append("\":");
                sb.append(toJson(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof Collection) {
            Collection<?> col = (Collection<?>) obj;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : col) {
                if (!first) sb.append(",");
                sb.append(toJson(item));
                first = false;
            }
            sb.append("]");
            return sb.toString();
        }
        if (obj.getClass().isArray()) {
            Object[] arr = (Object[]) obj;
            return toJson(Arrays.asList(arr));
        }

        // Java POJO via Getters
        StringBuilder sb = new StringBuilder("{");
        Method[] methods = obj.getClass().getMethods();
        boolean first = true;
        for (Method m : methods) {
            if (m.getParameterCount() == 0 && m.getName().startsWith("get") && !m.getName().equals("getClass")) {
                String propName = Character.toLowerCase(m.getName().charAt(3)) + m.getName().substring(4);
                try {
                    Object val = m.invoke(obj);
                    if (!first) sb.append(",");
                    sb.append("\"").append(propName).append("\":").append(toJson(val));
                    first = false;
                } catch (Exception ignored) {}
            } else if (m.getParameterCount() == 0 && m.getName().startsWith("is")) {
                String propName = Character.toLowerCase(m.getName().charAt(2)) + m.getName().substring(3);
                try {
                    Object val = m.invoke(obj);
                    if (!first) sb.append(",");
                    sb.append("\"").append(propName).append("\":").append(toJson(val));
                    first = false;
                } catch (Exception ignored) {}
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // =========================================================================
    // PARSING (JSON String -> Map / Object)
    // =========================================================================
    public static Map<String, Object> parseObject(String json) {
        if (json == null) return new HashMap<>();
        String trimmed = json.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        Map<String, Object> map = new HashMap<>();
        if (trimmed.isEmpty()) return map;

        int len = trimmed.length();
        int i = 0;
        while (i < len) {
            // Find key
            while (i < len && (Character.isWhitespace(trimmed.charAt(i)) || trimmed.charAt(i) == ',')) i++;
            if (i >= len) break;

            if (trimmed.charAt(i) != '"') {
                i++;
                continue;
            }
            i++; // skip opening quote
            int keyStart = i;
            while (i < len && trimmed.charAt(i) != '"') i++;
            String key = trimmed.substring(keyStart, i);
            i++; // skip closing quote

            // Find colon
            while (i < len && trimmed.charAt(i) != ':') i++;
            i++; // skip colon

            // Skip whitespace
            while (i < len && Character.isWhitespace(trimmed.charAt(i))) i++;
            if (i >= len) break;

            // Value extraction
            char c = trimmed.charAt(i);
            if (c == '"') {
                i++; // skip quote
                StringBuilder sb = new StringBuilder();
                while (i < len) {
                    if (trimmed.charAt(i) == '\\' && i + 1 < len) {
                        char next = trimmed.charAt(i + 1);
                        if (next == '"') sb.append('"');
                        else if (next == 'n') sb.append('\n');
                        else if (next == 'r') sb.append('\r');
                        else if (next == 't') sb.append('\t');
                        else sb.append(next);
                        i += 2;
                    } else if (trimmed.charAt(i) == '"') {
                        break;
                    } else {
                        sb.append(trimmed.charAt(i));
                        i++;
                    }
                }
                i++; // skip closing quote
                map.put(key, sb.toString());
            } else if (c == '[') {
                int depth = 1;
                int arrStart = i;
                i++;
                while (i < len && depth > 0) {
                    if (trimmed.charAt(i) == '[') depth++;
                    else if (trimmed.charAt(i) == ']') depth--;
                    i++;
                }
                String arrStr = trimmed.substring(arrStart, i);
                map.put(key, parseArray(arrStr));
            } else if (c == '{') {
                int depth = 1;
                int objStart = i;
                i++;
                while (i < len && depth > 0) {
                    if (trimmed.charAt(i) == '{') depth++;
                    else if (trimmed.charAt(i) == '}') depth--;
                    i++;
                }
                String nestedObj = trimmed.substring(objStart, i);
                map.put(key, parseObject(nestedObj));
            } else {
                // Primitive Number or Boolean
                int valStart = i;
                while (i < len && trimmed.charAt(i) != ',' && trimmed.charAt(i) != '}') i++;
                String raw = trimmed.substring(valStart, i).trim();
                if ("true".equalsIgnoreCase(raw)) map.put(key, Boolean.TRUE);
                else if ("false".equalsIgnoreCase(raw)) map.put(key, Boolean.FALSE);
                else if ("null".equalsIgnoreCase(raw)) map.put(key, null);
                else {
                    try {
                        if (raw.contains(".")) {
                            map.put(key, Double.parseDouble(raw));
                        } else {
                            map.put(key, Long.parseLong(raw));
                        }
                    } catch (NumberFormatException e) {
                        map.put(key, raw);
                    }
                }
            }
        }
        return map;
    }

    public static List<Object> parseArray(String json) {
        List<Object> list = new ArrayList<>();
        if (json == null) return list;
        String trimmed = json.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        if (trimmed.isEmpty()) return list;

        String[] tokens = trimmed.split(",");
        for (String token : tokens) {
            token = token.trim();
            if (token.startsWith("\"") && token.endsWith("\"")) {
                list.add(token.substring(1, token.length() - 1));
            } else if (!token.isEmpty()) {
                try {
                    list.add(Integer.parseInt(token));
                } catch (NumberFormatException e) {
                    try {
                        list.add(Double.parseDouble(token));
                    } catch (NumberFormatException e2) {
                        list.add(token);
                    }
                }
            }
        }
        return list;
    }
}
