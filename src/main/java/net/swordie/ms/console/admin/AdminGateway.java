package net.swordie.ms.console.admin;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * How the console reaches the server's admin API ({@code POST /api/v2/admin}). The real one is
 * {@link HttpAdminGateway}; the screenshot mode uses {@link FakeAdminGateway}. Every call blocks (the console runs it
 * off the event thread) and answers the result object ({@code ok}, {@code code}, fields) or throws.
 */
public interface AdminGateway {

    JsonNode call(String action, Map<String, Object> params) throws AdminCallException;

    default JsonNode call(String action) throws AdminCallException {
        return call(action, Map.of());
    }

    /** What the header shows ({@code 127.0.0.1:8600}). */
    String describe();
}
