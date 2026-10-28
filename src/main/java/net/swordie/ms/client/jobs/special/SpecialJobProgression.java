package net.swordie.ms.client.jobs.special;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.FieldConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.loaders.SkillData;

import java.util.ArrayList;
import java.util.List;

/** Visible fixed-level skills and the explicit TMS link-manager utility; no inferred hidden skills. */
public final class SpecialJobProgression {
    private SpecialJobProgression() { }

    public static boolean isSpecialSkill(int skillId) {
        int book = skillId / 10000;
        return skillId > 0 && book >= 12000 && book <= 13999
                && JobConstants.isSpecialEventJob((short) book);
    }

    /** Visible learned skills, plus the one explicitly verified active Beast Breathing follow-up on 282. */
    public static int requestLevel(Char chr, int skillId) {
        if (chr == null || !isFamilySkill(chr.getJob(), skillId)) return 0;
        int saitamaParent = saitamaFollowupParent(skillId);
        if (saitamaParent != 0) return learnedLevel(chr, saitamaParent);
        if (isTanjiroSwordFollowup(skillId)) return tanjiroSwordFollowupLevel(chr);
        if (skillId == 121001007) return tanjiroJumpLevel(chr);
        if (skillId == 121002003) return beastBreathingAttackLevel(chr);
        SkillInfo info = SkillData.getSkillInfoById(skillId);
        if (info == null || info.isInvisible()) return 0;
        return learnedLevel(chr, skillId);
    }

    public static boolean isFamilySkill(short job, int skillId) {
        if (!JobConstants.isSpecialEventJob(job)) return false;
        int book = skillId / 10000;
        int beginner = JobConstants.getJobEnumById(job).getBeginnerJobId();
        return book == beginner || book == JobConstants.getSpecialEventMainJob(job);
    }

    public static List<Skill> getLearnableSkills(short job, int level) {
        if (!JobConstants.isSpecialEventJob(job)) throw new IllegalArgumentException("Not a special event job");
        List<Skill> result = new ArrayList<>();
        short beginner = JobConstants.getJobEnumById(job).getBeginnerJobId();
        short[] books = job == beginner ? new short[] { beginner } : new short[] { beginner, job };
        for (short book : books) {
            for (Skill skill : SkillData.getSkillsByJob(book)) {
                SkillInfo info = SkillData.getSkillInfoById(skill.getSkillId());
                boolean linkManager = skill.getSkillId() == 120051251
                        && net.swordie.ms.connection.protocol.Protocol.active().version()
                        == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
                if (info == null || info.isInvisible() || (!linkManager && info.getFixLevel() <= 0) || info.getMaxLevel() <= 0
                        || info.getReqLevel() > level || !info.getReqSkills().isEmpty()) continue;
                skill.setCurrentLevel(linkManager ? 1 : Math.min(info.getFixLevel(), info.getMaxLevel()));
                result.add(skill);
            }
        }
        return result;
    }

    public static void learnAvailableSkills(Char chr, boolean notify) {
        List<Skill> changed = new ArrayList<>();
        for (Skill skill : getLearnableSkills(chr.getJob(), chr.getLevel())) {
            if (chr.getSkillLevel(skill.getSkillId()) >= skill.getCurrentLevel()) continue;
            if (chr.getId() == 0) chr.putSkill(skill);
            else chr.addSkill(skill);
            changed.add(skill);
        }
        if (notify && !changed.isEmpty()) chr.write(WvsContext.changeSkillRecordResult(changed, true, false, false));
    }

    public static int learnedLevel(Char chr, int skillId) {
        if (!isFamilySkill(chr.getJob(), skillId)) return 0;
        int saitamaParent = saitamaFollowupParent(skillId);
        if (saitamaParent != 0) return learnedLevel(chr, saitamaParent);
        if (isTanjiroSwordFollowup(skillId)) return tanjiroSwordFollowupLevel(chr);
        if (skillId == 121001007) return tanjiroJumpLevel(chr);
        if (skillId == 121002003) return beastBreathingAttackLevel(chr);
        SkillInfo info = SkillData.getSkillInfoById(skillId);
        if (info == null || info.getReqLevel() > chr.getLevel()) return 0;
        Skill owned = chr.getSkill(skillId);
        if (owned != null && !info.isInvisible()) return Math.min(owned.getCurrentLevel(), info.getMaxLevel());
        // Hidden variants are recognized only through explicit relationships of an owned parent.
        if (!info.isInvisible()) return 0;
        for (Skill parent : chr.getSkills()) {
            if (parent.getCurrentLevel() <= 0 || !isFamilySkill(chr.getJob(), parent.getSkillId())) continue;
            SkillInfo source = SkillData.getSkillInfoById(parent.getSkillId());
            if (source == null || source.isInvisible() || source.getReqLevel() > chr.getLevel()) continue;
            if (source.getPsdSkills().contains(skillId) || source.getAddAttackSkills().contains(skillId)
                    || source.getExtraSkillInfo().containsKey(skillId))
                return Math.min(Math.min(parent.getCurrentLevel(), source.getMaxLevel()), info.getMaxLevel());
        }
        return 0;
    }

    private static boolean isTanjiroSwordFollowup(int skillId) {
        return skillId >= 121001001 && skillId <= 121001003;
    }

    private static int saitamaFollowupParent(int skillId) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                != net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) return 0;
        return switch (skillId) {
            case 122001002 -> 122001001; // Explicit WZ psdSkill + live follow-up.
            case 122001004 -> 122001003; // WZ keydownFinish; native56D5321..56D5335 remaps to parent.
            case 122001009, 122001010 -> 122001008; // Native10FE003..10FE01D ->10FE2E7 ->10FBFE0.
            case 122001015 -> 122001000; // Native10FE026 ->10FE2B9 ->10FBFE0.
            default -> 0;
        };
    }

    /** Native10FDEC1 ->10FDFA7: upward jump inherits the visible jump skill. */
    private static int tanjiroJumpLevel(Char chr) {
        return net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3 && chr.getJob() == 12100
                ? learnedLevel(chr, 121001006) : 0;
    }

    /** Native10FDD2E/10FDDCC branches map these three IDs to parent121001000 at10FDDF5. */
    private static int tanjiroSwordFollowupLevel(Char chr) {
        return net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3 && chr.getJob() == 12100
                ? learnedLevel(chr, 121001000) : 0;
    }

    /** Only the verified active Beast Breathing follow-up is allowed. */
    private static int beastBreathingAttackLevel(Char chr) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                != net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                || chr.getJob() != 12100 || chr.getLevel() < 50 || chr.getSkillLevel(121002002) <= 0) return 0;
        var options = net.swordie.ms.client.character.tms282.Tms282BuffDirectory.INSTANCE.activeStats(chr.getId())
                .get(net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.TanjiroBeastBreathingType5);
        return options != null && options.stream().anyMatch(o -> o.rOption == 121002002 && o.nOption > 0) ? 1 : 0;
    }

    public static boolean claimBeastBreathingAttack(Char chr,
            net.swordie.ms.client.character.skills.info.AttackInfo attack, long nowMillis) {
        if (requestLevel(chr, attack.skillId) <= 0 || attack.skillId != 121002003
                || Byte.toUnsignedInt(attack.hits) > 8 || attack.mobCount > 1 || attack.mobAttackInfo.size() > 1) return false;
        int interval = SkillData.getSkillInfoById(121002002).getValue(
                net.swordie.ms.client.character.skills.SkillStat.subTime, 1);
        return interval > 0 && net.swordie.ms.client.character.tms282.Tms282BuffDirectory.INSTANCE
                .tryStartCooldown(chr.getId(), attack.skillId, nowMillis, interval);
    }

    /** Private-server defaults, not a claim about official event creation maps or starting levels. */
    public static void setLocalCreationStats(Char chr) {
        if (chr.getId() != 0) throw new IllegalArgumentException("Creation defaults apply only to a new character");
        short main = JobConstants.getSpecialEventMainJob(chr.getJob());
        CharacterStat cs = chr.getAvatarData().getCharacterStat();
        cs.setJob(main);
        chr.getAvatarData().getAvatarLook().setJob(main);
        cs.setPosMap(FieldConstants.START_MAP);
        cs.setLevel(10);
        cs.setStr(4); cs.setDex(4); cs.setInt(4); cs.setLuk(4);
        cs.setAp(40);
        cs.setHp(1000); cs.setMaxHp(1000);
        cs.setMp(500); cs.setMaxMp(500);
        learnAvailableSkills(chr, false);
    }
}
