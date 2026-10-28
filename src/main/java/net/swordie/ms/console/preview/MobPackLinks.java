package net.swordie.ms.console.preview;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * The 282 client keeps most mob definitions in {@code Data/Packs/Mob_*.ms}; the main Mob.wz is a stub. A pack entry's
 * frames are {@code _outlink}s into {@code Mob/_Canvas}. This resolves, for given mob ids, the outlink of the first
 * standing frame and caches it in {@code config/preview-cache/mob-frame-links.tsv} (packs decrypt to ~1 GB each,
 * so they are opened once, one at a time, and released).
 */
public final class MobPackLinks {
    static final Path CACHE = Path.of("config", "preview-cache", "mob-frame-links.tsv");
    static final String[] ACTIONS = {"stand", "fly", "move", "regen", "attack1"};
    private static final String NONE = "-";

    private static final Map<Integer, String> links = new HashMap<>();
    private static boolean loaded;

    private MobPackLinks() { }

    private static synchronized void loadCache() {
        if (loaded) return;
        loaded = true;
        try {
            if (!Files.isRegularFile(CACHE)) return;
            for (String row : Files.readAllLines(CACHE, StandardCharsets.UTF_8)) {
                String[] c = row.split("\t", 2);
                if (c.length == 2 && c[0].matches("\\d+")) links.put(Integer.parseInt(c[0]), c[1]);
            }
        } catch (Exception ignored) {
            // A broken cache is rebuilt on demand.
        }
    }

    /** Cached link for a mob: an outlink path, or null (unknown / no art in any pack). */
    public static synchronized String link(int mobId) {
        loadCache();
        String v = links.get(mobId);
        return v == null || v.equals(NONE) ? null : v;
    }

    /** Scans the mob packs once for every id not cached yet; returns how many new links were found. */
    public static int resolve(WzClientData wz, Collection<Integer> mobIds) {
        Set<Integer> wanted = new TreeSet<>();
        synchronized (MobPackLinks.class) {
            loadCache();
            for (int id : mobIds) if (id > 0 && !links.containsKey(id)) wanted.add(id);
        }
        if (wanted.isEmpty()) return 0;
        Map<Integer, String> found = new HashMap<>();
        List<String> failed = new ArrayList<>();
        for (Path pack : wz.packs("Mob_")) {
            Map<String, Object> entries;
            try {
                entries = wz.packEntries(pack, name -> {
                    String id = name.startsWith("Mob/") && name.endsWith(".img") ? name.substring(4, name.length() - 4) : "";
                    return id.matches("\\d+") && wanted.contains(Integer.parseInt(id));
                });
            } catch (RuntimeException e) {
                failed.add(pack.getFileName().toString()); // e.g. a pack format the parser does not support
                continue;
            }
            for (Map.Entry<String, Object> e : entries.entrySet()) {
                int id = Integer.parseInt(e.getKey().substring(4, e.getKey().length() - 4));
                String link = firstFrameLink(wz, e.getValue());
                if (link != null) found.put(id, link);
            }
            if (found.keySet().containsAll(wanted)) break;
        }
        synchronized (MobPackLinks.class) {
            // Ids missing only because a pack was unreadable stay uncached so they are retried later.
            for (int id : wanted) {
                if (found.containsKey(id)) links.put(id, found.get(id));
                else if (failed.isEmpty()) links.put(id, NONE);
            }
            save();
            lastUnreadable = List.copyOf(failed);
        }
        return found.size();
    }

    private static List<String> lastUnreadable = List.of();

    /** Pack files the last scan could not read. */
    public static synchronized List<String> unreadablePacks() {
        return lastUnreadable;
    }

    private static String firstFrameLink(WzClientData wz, Object img) {
        for (String action : ACTIONS) {
            String link = wz.outlinkOf(wz.child(img, action + "/0"));
            if (link != null) return link;
        }
        return null;
    }

    private static void save() {
        try {
            Files.createDirectories(CACHE.getParent());
            StringBuilder sb = new StringBuilder("mob_id\tframe_outlink\n");
            new TreeMap<>(links).forEach((id, link) -> sb.append(id).append('\t').append(link).append('\n'));
            Files.writeString(CACHE, sb.toString(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            // Cache only; previews still work, they just rescan next time.
        }
    }
}
