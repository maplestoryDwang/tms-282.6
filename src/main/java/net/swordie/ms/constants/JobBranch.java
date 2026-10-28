package net.swordie.ms.constants;

/**
 * The five job branches (plus "other") a launcher shows next to a character: one per job line, see
 * {@link JobNameCatalog#branch(int)}. {@link #code()} is the stable wire value, {@link #displayName()} the Traditional
 * Chinese label.
 */
public enum JobBranch {
    WAR("war", "劍士"),
    MAG("mag", "法師"),
    ARC("arc", "弓箭手"),
    THF("thf", "盜賊"),
    PIR("pir", "海盜"),
    NON("non", "其他");

    private final String code;
    private final String displayName;

    JobBranch(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /** war / mag / arc / thf / pir / non. */
    public String code() {
        return code;
    }

    /** 劍士 / 法師 / 弓箭手 / 盜賊 / 海盜 / 其他. */
    public String displayName() {
        return displayName;
    }

    /** The branch of a job id; {@link #NON} for beginners, GMs and unknown ids. */
    public static JobBranch of(int jobId) {
        return JobNameCatalog.branch(jobId);
    }
}
