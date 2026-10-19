package com.lavrel.platform.adapter.out.steam;

import java.util.List;
import java.util.Map;

/** Null-safe readers for the loosely typed maps that come out of the JSON parser. */
final class SteamJson {

    private SteamJson() {
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    @SuppressWarnings("unchecked")
    static List<Object> list(Object value) {
        return value instanceof List<?> l ? (List<Object>) l : List.of();
    }

    static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    static long longValue(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String s) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L;
    }
}
