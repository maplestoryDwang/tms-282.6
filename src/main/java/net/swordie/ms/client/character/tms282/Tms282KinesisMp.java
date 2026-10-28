package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.JobConstants;

/**
 * 282 Kinesis has a real MP pool (no PP). Characters that levelled under the 265 rules kept base max MP at the PP cap 30
 * (live 2026-09-28: character 37, level 300, "base 32000/30", potions capped at 656). On login the base max MP is raised
 * to the level's minimum of the INT jobs (ScriptManagerImpl.getLevelDirectMinMaxMP: 42 000 x level / 300); a higher
 * value is kept. Kanna (fixed 100 under 265) is repaired the same way (2026-10-06), and Angelic Buster (max MP 0 under
 * 265, 2026-10-07) with the DEX-job minimum 30 000 x level / 300. Other jobs and GMS 265 are untouched.
 */
public final class Tms282KinesisMp {
    static final int INT_JOB_MAX_MP_AT_300 = 42_000;

    private Tms282KinesisMp() {
    }

    public static int minimumMaxMp(int level) {
        return (int) (INT_JOB_MAX_MP_AT_300 * (Math.min(level, 300) / 300D));
    }

    /** ScriptManagerImpl.getLevelDirectMinMaxMP for a job whose main stat is neither INT nor STR. */
    static final int DEX_JOB_MAX_MP_AT_300 = 30_000;

    public static int minimumMaxMp(short job, int level) {
        return JobConstants.isAngelicBuster(job)
                ? (int) (DEX_JOB_MAX_MP_AT_300 * (Math.min(level, 300) / 300D)) : minimumMaxMp(level);
    }

    /** Returns the new base max MP when it was raised, else -1. */
    public static int repair(Char chr) {
        if (chr == null || Protocol.active().version() != ProtocolVersion.TMS_282_3
                || !(JobConstants.isKinesis(chr.getJob()) || JobConstants.isKanna(chr.getJob())
                || JobConstants.isAngelicBuster(chr.getJob())) || JobConstants.isNoManaJob(chr.getJob())) {
            return -1;
        }
        CharacterStat stat = chr.getAvatarData().getCharacterStat();
        int min = Math.max(1, minimumMaxMp(chr.getJob(), stat.getLevel()));
        if (stat.getMaxMp() >= min) {
            return -1;
        }
        stat.setMaxMp(min);
        if (JobConstants.isAngelicBuster(chr.getJob()) && stat.getMp() < min) {
            stat.setMp(min); // she never had MP: start full instead of at the leftover 0..4
        }
        System.out.printf("[TMS282] Kinesis/Kanna base max MP of character %d raised to %d (level %d; was the 265 PP cap / fixed 100)%n",
                chr.getId(), min, stat.getLevel());
        return min;
    }
}
