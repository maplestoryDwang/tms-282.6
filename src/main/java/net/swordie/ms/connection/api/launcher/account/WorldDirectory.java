package net.swordie.ms.connection.api.launcher.account;

import net.swordie.ms.handlers.login.tms282.Tms282LoginSettings;
import net.swordie.ms.handlers.login.tms282.Tms282WorldCatalog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The worlds as the launcher names them. {@link Tms282WorldCatalog} stays the source of the advertised worlds and
 * their LP0013 types (its wire names are ASCII until the client's charset is verified, and world 0's type is
 * configurable, so neither can tell a "collaboration world"); the Traditional Chinese display names and the
 * collaboration flag live here.
 */
public final class WorldDirectory {
    private static final Map<Integer, String> NAMES = Map.of(
            0, "一般世界",
            47, "雪吉拉×品克繽世界",
            55, "合作職業世界",
            56, "合作職業世界 2");
    /** The Saitama / Tanjiro worlds. */
    private static final Set<Integer> COLLAB = Set.of(55, 56);

    private final Tms282WorldCatalog catalog;

    public WorldDirectory(Tms282WorldCatalog catalog) {
        this.catalog = catalog;
    }

    /** The catalog the login server advertises with this configuration (config/server.properties). */
    public static WorldDirectory current() {
        return new WorldDirectory(Tms282WorldCatalog.standard(Tms282LoginSettings.load().world0Type()));
    }

    /** Display name; {@code "世界 <id>"} for a world without one. */
    public String name(int worldId) {
        String name = NAMES.get(worldId);
        return name == null ? "世界 " + worldId : name;
    }

    public boolean collab(int worldId) {
        return COLLAB.contains(worldId);
    }

    /** The login server lists this world. */
    public boolean advertised(int worldId) {
        return catalog.contains(worldId);
    }

    /**
     * Every advertised world in id order, then any other world in {@code alsoShow} (worlds the caller's characters
     * live in that are no longer advertised, e.g. older data): {@code {id, name, type, collab, advertised}}, where
     * {@code type} is the LP0013 world type or null.
     */
    public List<Map<String, Object>> describe(Collection<Integer> alsoShow) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Tms282WorldCatalog.World w : catalog.worlds()) {
            out.add(entry(w.id(), w.type(), true));
        }
        for (int id : new TreeSet<>(alsoShow)) {
            if (!catalog.contains(id)) {
                out.add(entry(id, null, false));
            }
        }
        return out;
    }

    private Map<String, Object> entry(int id, Integer type, boolean advertised) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name(id));
        m.put("type", type);
        m.put("collab", collab(id));
        m.put("advertised", advertised);
        return m;
    }
}
