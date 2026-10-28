package net.swordie.ms.world.tms282.dojo;

/** Best weekly dojo run of one character. {@code timeSec} is the elapsed dojo clock at the time of the clear. */
public record DojoRecord(int charId, int weekKey, int stage, int timeSec, int job, int level, String name) {

    public boolean isBetterThan(DojoRecord other) {
        return stage > other.stage || (stage == other.stage && timeSec < other.timeSec);
    }

    /** Job line used for "本職" ranking: the job id without its advancement digit (e.g. 112 -> 11, 1412 -> 141). */
    public int jobLine() {
        return jobLineOf(job);
    }

    public static int jobLineOf(int job) {
        return job / 10;
    }
}
