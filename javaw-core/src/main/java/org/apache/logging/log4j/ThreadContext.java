package org.apache.logging.log4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Standard Log4j2 ThreadContext (Mapped Diagnostic Context) for WebAssembly.
 */
public class ThreadContext {

    private static final Map<String, String> contextMap = new HashMap<>();

    public static void put(String key, String value) {
        if (key != null) {
            contextMap.put(key, value != null ? value : "");
        }
    }

    public static String get(String key) {
        if (key == null) return null;
        return contextMap.get(key);
    }

    public static void remove(String key) {
        if (key != null) {
            contextMap.remove(key);
        }
    }

    public static boolean containsKey(String key) {
        if (key == null) return false;
        return contextMap.containsKey(key);
    }

    public static void clearMap() {
        contextMap.clear();
    }

    public static void clearAll() {
        contextMap.clear();
    }

    public static Map<String, String> getContext() {
        return contextMap;
    }

    public static String formatContext() {
        if (contextMap.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : contextMap.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}
