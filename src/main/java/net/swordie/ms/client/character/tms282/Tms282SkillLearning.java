package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.loaders.Tms282SkillRequirements;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

/** 282 learning and free fixed-level skill requirements. Normal SP levels and special-event progression are separate. */
public final class Tms282SkillLearning {
    private Tms282SkillLearning() { }
    public static boolean enabled() { return Protocol.active().version()==ProtocolVersion.TMS_282_3; }

    /** Evan's actual jobs 2210/2212 use native skill books 2200/2211. Do not alter global legacy job predicates. */
    public static int bookStage(int root) {
        return switch(root) {
            case 2200,2210 -> 1;
            case 2211,2212,2213 -> 2;
            case 2214,2215,2216 -> 3;
            case 2217,2218 -> 4;
            default -> JobConstants.getJobLevel((short)root);
        };
    }

    public static boolean belongs(Char chr, int root) {
        var job=JobConstants.JobEnum.getJobById(chr.getJob());
        if(job==null) return false;
        if(JobConstants.isBeginnerJob((short)root)) return root==job.getBeginnerJobId();
        return SkillConstants.isMatching(root,chr.getJob()) && bookStage(root)>0
                && bookStage(root)<=JobConstants.getJobLevel(chr.getJob());
    }

    public static boolean requirementsMet(Char chr, SkillInfo info) {
        if(chr.getLevel()<info.getReqLevel()) return false;
        return info.getReqSkills().entrySet().stream().allMatch(e -> chr.getSkillLevel(e.getKey())>=e.getValue());
    }

    public static String pointRefusal(Char chr, SkillInfo info) {
        if(!enabled()) return null;
        if(!Tms282SkillRequirements.hasOriginalSkill(info.getSkillId())) return "skill is absent from the original 282 skill book";
        if(!belongs(chr,info.getRootId())) return "skill book is not available to this job stage";
        if(info.isInvisible()) return "hidden skill is derived, not learned with SP";
        if(info.getFixLevel()>0) return "fixed-level skill is granted automatically when its requirements are met";
        if(info.getHyper()!=0 || info.getVSkill()!=0 || info.isOriginSkill() || info.isAscentSkill())
            return "skill uses its dedicated progression, not ordinary SP";
        if(!requirementsMet(chr,info)) return "skill level or prerequisite skill requirement is not met";
        return null;
    }

    private static boolean managedFixed(SkillInfo info) {
        int root=info.getRootId();
        return info.getFixLevel()>0 && !info.isInvisible() && root<20000 && bookStage(root)>0
                && Tms282SkillRequirements.hasOriginalSkill(info.getSkillId())
                && info.getHyper()==0 && info.getVSkill()==0
                && !JobConstants.isZero((short)root) && !JobConstants.isSpecialEventJob((short)root);
    }

    /** Used by setJob before it installs the ordinary visible skill book. */
    public static void prepareInitialLevel(Char chr, Skill skill) {
        if(!enabled()) return;
        SkillInfo info=SkillData.getSkillInfoById(skill.getSkillId());
        if(info!=null && managedFixed(info) && (!belongs(chr,info.getRootId()) || !requirementsMet(chr,info)))
            skill.setCurrentLevel(0);
    }

    /** Preserve independent sources such as Phantom's stolen skills; only this character's native fixed books apply. */
    public static boolean fixedUseAllowed(Char chr, int skillId) {
        SkillInfo info=SkillData.getSkillInfoById(skillId);
        return !enabled() || info==null || !managedFixed(info) || !belongs(chr,info.getRootId()) || requirementsMet(chr,info);
    }

    /** Copy/replace in memory, keeping the row identity and passive cache correct; caller owns persistence. */
    public static void putWithStats(Char chr, Skill record) {
        Skill old=chr.getSkill(record.getSkillId());
        if(old!=null && SkillConstants.isPassiveSkill(old.getSkillId())) chr.removeFromBaseStatCache(old);
        if(old!=null) record.setId(old.getId());
        chr.putSkill(record);
        chr.addToBaseStatCache(record);
    }

    /** Reconcile free native fixed skills only. An ineligible old fixed row becomes level 0, never deleted;
     * normal SP skills are untouched and the fixed level is restored when the prerequisite/level is reached. */
    public static List<Skill> reconcileFixed(Char chr, Consumer<Skill> apply) {
        if(chr==null || chr.getId()==0 || !enabled() || JobConstants.isZero(chr.getJob())
                || JobConstants.isSpecialEventJob(chr.getJob())) return List.of();
        var job=JobConstants.JobEnum.getJobById(chr.getJob());
        if(job==null) return List.of();
        synchronized(chr) {
            Set<Integer> books=new TreeSet<>();books.add((int)job.getBeginnerJobId());
            for(int root=chr.getJob()/100*100;root<=chr.getJob();root++)
                if(belongs(chr,root) && (JobConstants.JobEnum.getJobById((short)root)!=null || root==2200 || root==2211)) books.add(root);
            List<Skill> candidates=new ArrayList<>();
            for(int root:books) candidates.addAll(SkillData.getSkillsByJob((short)root));
            candidates.sort(Comparator.comparingInt(Skill::getSkillId));
            List<Skill> changed=new ArrayList<>();
            for(Skill skill:candidates) {
                SkillInfo info=SkillData.getSkillInfoById(skill.getSkillId());
                if(info==null || !managedFixed(info)) continue;
                int level=requirementsMet(chr,info)?info.getFixLevel():0;
                if(chr.getSkillLevel(skill.getSkillId())==level) continue;
                skill.setCurrentLevel(level);skill.setCharId(chr.getId());
                Skill old=chr.getSkill(skill.getSkillId());if(old!=null) skill.setId(old.getId());
                apply.accept(skill);
                changed.add(chr.getSkill(skill.getSkillId()));
            }
            return changed;
        }
    }
}
