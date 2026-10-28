package net.swordie.ms.connection.api.launcher.account;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The caller's own account as the launcher shows it. Deliberately without email, pic, password, ban or recovery data.
 *
 * @param createdAt users.creationdate in epoch milliseconds, or null when unknown
 */
public record AccountView(String username, Long createdAt, int vipGrade, int characterSlots, boolean online) {

    /** Wire order: username, createdAt, vipGrade, characterSlots, online. */
    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("username", username);
        m.put("createdAt", createdAt);
        m.put("vipGrade", vipGrade);
        m.put("characterSlots", characterSlots);
        m.put("online", online);
        return m;
    }
}
