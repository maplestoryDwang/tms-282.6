package net.swordie.ms.world.tms282.revival;

import java.util.List;

/**
 * One revived legacy event (docs/revival). Implementations are registered in {@link RevivalEvents}; the panel script
 * {@code expand/menu/經典活動} and the GM command list them, {@link #enter} is the entry command.
 */
public interface RevivalEvent {
    /** Static description; {@code entryMap} 0 = no warp (the event is entered through an NPC already on a map). */
    record Spec(String key, String title, String summary, int entryMap, int entryPortal, int minLevel) {
        public Spec {
            if (key == null || !key.matches("[a-z][a-z0-9_]{0,23}")) throw new IllegalArgumentException("event key " + key);
            if (title == null || title.isBlank()) throw new IllegalArgumentException("title");
            if (entryPortal < 0 || entryPortal > 255 || minLevel < 0) throw new IllegalArgumentException("entry");
        }
    }

    enum EntryResult {WARPED, NO_ENTRY_MAP, CLOSED, LEVEL, MAP_MISSING, BUSY}

    Spec spec();

    /** Maps that must exist in the 282 data for the event to work (checked by the revival data check). */
    default List<Integer> requiredMaps() {
        return spec().entryMap() == 0 ? List.of() : List.of(spec().entryMap());
    }

    /** NPCs whose scripts the event provides (checked: the script file exists). */
    default List<Integer> scriptedNpcs() {
        return List.of();
    }

    default EntryResult enter(RevivalPlayer player) {
        Spec s = spec();
        if (s.entryMap() == 0) return EntryResult.NO_ENTRY_MAP;
        if (player.level() < s.minLevel()) return EntryResult.LEVEL;
        if (!player.mapExists(s.entryMap())) return EntryResult.MAP_MISSING;
        if (player.busy()) return EntryResult.BUSY;
        player.warp(s.entryMap(), s.entryPortal());
        return EntryResult.WARPED;
    }
}
