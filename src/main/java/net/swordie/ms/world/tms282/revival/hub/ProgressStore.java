package net.swordie.ms.world.tms282.revival.hub;

import java.util.Map;

/** Persistence of per-character mission progress (one row per character, event, mission, period). */
public interface ProgressStore {
    record Row(int count, boolean claimed) {
    }

    /** Rows of {@code charId} for {@code event}, keyed {@code mission + "@" + period}. */
    Map<String, Row> load(int charId, String event) throws Exception;

    /** Upsert; {@code count} never goes down in the store. */
    void save(int charId, String event, String mission, String period, int count, boolean claimed) throws Exception;

    static String key(String mission, String period) {
        return mission + "@" + period;
    }
}
