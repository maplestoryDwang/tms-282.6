package net.swordie.ms.console.config;

import net.swordie.ms.constants.JobNameCatalog;

/** Public access to the console's Chinese job names for other console panels (shared table: {@link JobNameCatalog}). */
public final class JobNames {
    private JobNames() {
    }

    /** Chinese name of a job id, or null when the catalog has none. */
    public static String of(int jobId) {
        String name = JobNameCatalog.consoleRootName(jobId);
        return name == null || name.isBlank() ? null : name;
    }
}
