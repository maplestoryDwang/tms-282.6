package net.swordie.ms.world.tms282.mesomarket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.swordie.ms.ServerConstants;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Meso market (Maple Point exchange) rules, loaded from data/resources/tms282/mesomarket/mesomarket.json.
 * Defaults mirror TMS279 config.toml [game.meso-market] (daily-limit 100, tax-rate 10).
 */
public record MesoMarketConfig(boolean enabled, int taxRate, int dailyOrderLimit, int maxOpenOrdersPerAccount,
                               long minPrice, long maxPrice, long minCount, long maxCount, int listDepth) {

    public static final MesoMarketConfig DEFAULT = new MesoMarketConfig(true, 10, 100, 10,
            1, 100_000_000L, 1, 1_000_000L, 10);

    private static volatile MesoMarketConfig current;

    public static Path defaultPath() {
        return Path.of(System.getProperty("tms282.mesoMarket.config",
                ServerConstants.RESOURCES_DIR + "/tms282/mesomarket/mesomarket.json"));
    }

    public static MesoMarketConfig get() {
        MesoMarketConfig c = current;
        if (c == null) {
            synchronized (MesoMarketConfig.class) {
                if (current == null) {
                    current = loadOrDefault(defaultPath());
                }
                c = current;
            }
        }
        return c;
    }

    public static void reload() {
        current = loadOrDefault(defaultPath());
    }

    static MesoMarketConfig loadOrDefault(Path path) {
        try {
            return parse(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            System.out.println("[MesoMarket] config " + path + " not loaded (" + e + "), using defaults");
            return DEFAULT;
        }
    }

    public static MesoMarketConfig parse(String json) {
        JsonNode o;
        try {
            o = new ObjectMapper().readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("meso market config JSON: " + e.getOriginalMessage(), e);
        }
        if (o == null || !o.isObject()) throw new IllegalArgumentException("meso market config is not a JSON object");
        MesoMarketConfig d = DEFAULT;
        MesoMarketConfig c = new MesoMarketConfig(
                o.has("enabled") ? o.get("enabled").asBoolean() : d.enabled,
                o.has("taxRate") ? o.get("taxRate").asInt() : d.taxRate,
                o.has("dailyOrderLimit") ? o.get("dailyOrderLimit").asInt() : d.dailyOrderLimit,
                o.has("maxOpenOrdersPerAccount") ? o.get("maxOpenOrdersPerAccount").asInt() : d.maxOpenOrdersPerAccount,
                o.has("minPrice") ? o.get("minPrice").asLong() : d.minPrice,
                o.has("maxPrice") ? o.get("maxPrice").asLong() : d.maxPrice,
                o.has("minCount") ? o.get("minCount").asLong() : d.minCount,
                o.has("maxCount") ? o.get("maxCount").asLong() : d.maxCount,
                o.has("listDepth") ? o.get("listDepth").asInt() : d.listDepth);
        if (c.taxRate < 0 || c.taxRate > 100 || c.minPrice < 1 || c.maxPrice < c.minPrice
                || c.minCount < 1 || c.maxCount < c.minCount || c.maxCount > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("invalid meso market config " + c);
        }
        return c;
    }

    /** Meso tax on a sale of {@code gross} meso, floored. */
    public long tax(long gross) {
        return gross / 100 * taxRate + (gross % 100) * taxRate / 100;
    }
}
