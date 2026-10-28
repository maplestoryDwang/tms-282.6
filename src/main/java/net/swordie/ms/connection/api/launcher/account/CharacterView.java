package net.swordie.ms.connection.api.launcher.account;

import net.swordie.ms.constants.JobBranch;
import net.swordie.ms.constants.JobNameCatalog;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One character of the caller, as the launcher lists it. Names come from {@link JobNameCatalog} (job line, branch,
 * collaboration flag), so the launcher no longer keeps its own job table.
 *
 * @param world      accounts.worldid (the per-world account the character belongs to)
 * @param lastLogout characterstats.lastlogout in epoch milliseconds; null when never logged out (NULL or the
 *                   FileTime-zero placeholder 1601-01-01)
 * @param deleting   characters.deletionStartTime is set (deletion scheduled); false where the column does not exist
 * @param online     the server holds the character right now (level / job are then the live values)
 * @param extras     1.2.8 ({@link CharacterExtras}): link / fairy / empress, appended after {@code online}; empty
 *                   when not computed
 */
public record CharacterView(int id, String name, int level, int job, int subJob, int world, int orderId,
                            Long lastLogout, boolean deleting, boolean online, Map<String, Object> extras) {

    public CharacterView {
        extras = extras == null ? Map.of() : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(extras));
    }

    public CharacterView(int id, String name, int level, int job, int subJob, int world, int orderId,
                         Long lastLogout, boolean deleting, boolean online) {
        this(id, name, level, job, subJob, world, orderId, lastLogout, deleting, online, Map.of());
    }

    public CharacterView withExtras(Map<String, Object> extras) {
        return new CharacterView(id, name, level, job, subJob, world, orderId, lastLogout, deleting, online, extras);
    }

    public String jobName() {
        return JobNameCatalog.lineName(job);
    }

    public JobBranch branch() {
        return JobNameCatalog.branch(job);
    }

    public boolean collab() {
        return JobNameCatalog.collab(job);
    }

    /**
     * Wire order: id, name, level, job, subjob, jobName, branch, branchName, collab, world, orderId, lastLogout,
     * deleting, online.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("level", level);
        m.put("job", job);
        m.put("subjob", subJob);
        m.put("jobName", jobName());
        m.put("branch", branch().code());
        m.put("branchName", branch().displayName());
        m.put("collab", collab());
        m.put("world", world);
        m.put("orderId", orderId);
        m.put("lastLogout", lastLogout);
        m.put("deleting", deleting);
        m.put("online", online);
        m.putAll(extras);
        return m;
    }
}
