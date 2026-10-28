package net.swordie.ms.loaders.Etc.VCore;

import net.swordie.ms.constants.JobConstants;

/** TMS skill-node eligibility from stable job tags, including explicit IDs beside a class tag. */
public final class Tms282VCoreEligibility {
    private Tms282VCoreEligibility() {}

    public static boolean isLearnableSkill(VCoreData core, short job) {
        if (core == null || core.getType() != VCore.SKILL) return false;
        var jobs = core.getJobs();
        // E.g. Venom Burst lists [rogue, 3612]; the generic class tag must not hide Xenon's explicit ID.
        if (jobs.contains("all") || jobs.contains(Short.toString(job))) return true;
        if (core.isClassSkill(job)) return true;
        return jobs.contains("warrior") && (JobConstants.isAdele(job) || JobConstants.isRen(job))
                || jobs.contains("magician") && (JobConstants.isIllium(job) || JobConstants.isLara(job)
                    || JobConstants.isLynn(job) || JobConstants.isSiaAstelle(job))
                || jobs.contains("archer") && JobConstants.isKain(job)
                || jobs.contains("rogue") && (JobConstants.isCadena(job) || JobConstants.isHoYoung(job)
                    || JobConstants.isKhali(job))
                || jobs.contains("pirate") && (JobConstants.isArk(job) || JobConstants.isMoXuan(job));
    }
}
