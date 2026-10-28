package net.swordie.ms.console.config;

import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.constants.JobNameCatalog;

/**
 * The console's job names. Since 2026-10-01 (launcher P5b) the table lives in the shared
 * {@link JobNameCatalog} ("console" methods, moved verbatim), so the console shows exactly the same text as before.
 */
final class JobCatalog {
    private JobCatalog() {
    }

    static String loginJobName(JobConstants.LoginJob job) {
        return JobNameCatalog.consoleLoginJobName(job);
    }

    static String skillRootName(int rootId) {
        return JobNameCatalog.consoleRootName(rootId);
    }

    static String fifthSkillRootName(int rootId) {
        return JobNameCatalog.consoleFifthRootName(rootId);
    }
}
