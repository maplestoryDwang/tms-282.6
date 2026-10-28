package net.swordie.ms.world.tms282.bossrun;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.swordie.ms.ServerConstants;

import java.io.IOException;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Account clear limits per boss key ({@code data/resources/tms282/bossrun/limits.json}) and the day / week windows.
 * A limit of 0 means unlimited.
 */
public final class BossRunLimits {
    public record Limit(int daily, int weekly) {
    }

    private final Limit defaults;
    private final Map<String, Limit> overrides;
    private final int aufhebenLogKey;
    private final int aufhebenDaily;

    public BossRunLimits(Limit defaults, Map<String, Limit> overrides, int aufhebenLogKey, int aufhebenDaily) {
        this.defaults = defaults;
        this.overrides = Map.copyOf(overrides);
        this.aufhebenLogKey = aufhebenLogKey;
        this.aufhebenDaily = aufhebenDaily;
    }

    public static Path defaultPath() {
        return Path.of(ServerConstants.RESOURCES_DIR, "tms282", "bossrun", "limits.json");
    }

    public static BossRunLimits load(Path path) {
        try {
            JsonNode root = new ObjectMapper().readTree(path.toFile());
            Limit def = new Limit(root.path("defaults").path("daily").asInt(1), root.path("defaults").path("weekly").asInt(4));
            Map<String, Limit> map = new HashMap<>();
            for (Iterator<Map.Entry<String, JsonNode>> it = root.path("overrides").fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> e = it.next();
                map.put(e.getKey(), new Limit(e.getValue().path("daily").asInt(def.daily()),
                        e.getValue().path("weekly").asInt(def.weekly())));
            }
            JsonNode auf = root.path("aufheben");
            return new BossRunLimits(def, map, auf.path("logKey").asInt(100936), auf.path("daily").asInt(5));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load " + path, e);
        }
    }

    public Limit limitOf(String bossKey) {
        return overrides.getOrDefault(bossKey, defaults);
    }

    public int aufhebenLogKey() {
        return aufhebenLogKey;
    }

    public int aufhebenDaily() {
        return aufhebenDaily;
    }

    public static LocalDateTime dayStart(LocalDateTime now) {
        return now.toLocalDate().atStartOfDay();
    }

    public static LocalDateTime weekStart(LocalDateTime now, DayOfWeek resetDay) {
        LocalDate d = now.toLocalDate().with(TemporalAdjusters.previousOrSame(resetDay));
        return d.atStartOfDay();
    }
}
