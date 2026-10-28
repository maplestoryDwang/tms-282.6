package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;

/** Request field access for admin actions: absent or mistyped fields read as the given default. */
final class AdminParams {
    private AdminParams() {
    }

    static String text(JsonNode request, String key) {
        JsonNode v = request == null ? null : request.get(key);
        return v != null && v.isTextual() ? v.textValue() : "";
    }

    /** An integral number (or a numeric string) in [min, max], else null. */
    static Long number(JsonNode request, String key, long min, long max) {
        JsonNode v = request == null ? null : request.get(key);
        if (v == null || v.isNull()) {
            return null;
        }
        long n;
        if (v.isIntegralNumber() && v.canConvertToLong()) {
            n = v.longValue();
        } else if (v.isTextual() && v.textValue().trim().matches("-?\\d{1,12}")) {
            n = Long.parseLong(v.textValue().trim());
        } else {
            return null;
        }
        return n >= min && n <= max ? n : null;
    }

    static boolean has(JsonNode request, String key) {
        return request != null && request.has(key) && !request.get(key).isNull();
    }
}
