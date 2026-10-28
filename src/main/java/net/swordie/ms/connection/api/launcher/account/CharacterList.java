package net.swordie.ms.connection.api.launcher.account;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The caller's characters (ordered by world, slot, id) and the one the launcher's hero banner shows.
 *
 * @param lastPlayed an online character if there is one; otherwise the latest {@code lastLogout} among the characters
 *                   not scheduled for deletion; null when none qualifies (no characters, or none ever logged out)
 */
public record CharacterList(List<CharacterView> characters, CharacterView lastPlayed) {

    public CharacterList {
        characters = List.copyOf(characters);
    }

    public static CharacterList of(List<CharacterView> characters) {
        return new CharacterList(characters, pickLastPlayed(characters));
    }

    static CharacterView pickLastPlayed(List<CharacterView> characters) {
        for (CharacterView c : characters) {
            if (c.online()) {
                return c;
            }
        }
        CharacterView best = null;
        for (CharacterView c : characters) {
            if (c.deleting() || c.lastLogout() == null) {
                continue;
            }
            if (best == null || c.lastLogout() > best.lastLogout()) {
                best = c;
            }
        }
        return best;
    }

    public List<Map<String, Object>> characterMaps() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (CharacterView c : characters) {
            out.add(c.toMap());
        }
        return out;
    }

    /** The banner character as a map, or null. */
    public Map<String, Object> lastPlayedMap() {
        return lastPlayed == null ? null : lastPlayed.toMap();
    }

    /** Worlds the characters live in, ascending. */
    public Set<Integer> worlds() {
        Set<Integer> out = new LinkedHashSet<>();
        characters.stream().map(CharacterView::world).sorted(Comparator.naturalOrder()).forEach(out::add);
        return out;
    }
}
