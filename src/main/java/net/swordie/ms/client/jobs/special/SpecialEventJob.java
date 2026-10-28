package net.swordie.ms.client.jobs.special;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.life.Summon;
import net.swordie.ms.enums.MoveAbility;

import java.util.EnumMap;

/** Tanjiro, Saitama and Yeti share only metadata-based behavior; no invented gauge mechanics. */
public final class SpecialEventJob extends Job {
    public static final int TANJIRO_BASIC_SWORD = 121001000;
    public static final int SAITAMA_NORMAL_PUNCH = 122001001;
    public static final int SAITAMA_INCINERATION = 122001011;
    public static final int SAITAMA_KING_ENGINE = 122001012;
    public static final int YETI_SNACK_ATTACK = 135001000;
    public static final int TANJIRO_WILL = 121001014;
    public static final int TANJIRO_WARRIOR = 121001013;
    public static final int SAITAMA_WILL = 122001016;
    private final short family;
    private final SaitamaActionProtection actionProtection = new SaitamaActionProtection();

    public SaitamaActionProtection getActionProtection() { return actionProtection; }

    public SpecialEventJob(Char chr) {
        super(chr);
        if (JobConstants.isPinkBean(chr.getJob())) throw new IllegalArgumentException("Use PinkBean's handler");
        family = JobConstants.getSpecialEventMainJob(chr.getJob());
    }

    @Override public boolean isHandlerOfJob(short id) {
        return JobConstants.isSpecialEventJob(id) && JobConstants.getSpecialEventMainJob(id) == family;
    }
    @Override public void setCharCreationStats(Char chr) { SpecialJobProgression.setLocalCreationStats(chr); }
    @Override public void handleJobAdvance() { SpecialJobProgression.learnAvailableSkills(chr, true); }
    @Override public void handleInitAfterField() {
        actionProtection.clear(chr);
        super.handleInitAfterField();
        SpecialJobProgression.learnAvailableSkills(chr, true);
    }

    @Override public void handleAttack(Client c, AttackInfo attack, SkillInfo info, long now) {
        if (JobConstants.isSpecialEventJob((short) (attack.skillId / 10000))) {
            int level = SpecialJobProgression.learnedLevel(chr, attack.skillId);
            if (level == 0) return;
            attack.slv = level;
            info = SkillData.getSkillInfoById(attack.skillId);
        }
        // Normal damage, MP costs and cooldowns use the existing attack pipeline and real SkillInfo.
        // Character-specific follow-up attacks, gauges and summons are deliberately not synthesized.
        super.handleAttack(c, attack, info, now);
    }

    @Override public void handleSkill(Client c, InPacket packet, SkillUseInfo use) {
        if (!JobConstants.isSpecialEventJob((short) (use.skillID / 10000))) {
            super.handleSkill(c, packet, use);
            return;
        }
        int level = SpecialJobProgression.learnedLevel(chr, use.skillID);
        if (level == 0) return;
        use.slv = level;
        use.skillInfo = SkillData.getSkillInfoById(use.skillID);
        super.handleSkill(c, packet, use);
        if (use.skillID == SaitamaPsychicPower.SKILL && net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) SaitamaPsychicPower.cast(chr);
        if (use.skillID == SaitamaActionProtection.SERIOUS_PUNCH)
            actionProtection.startSeriousPunch(chr, System.currentTimeMillis());
        if ((use.skillID == SAITAMA_INCINERATION || use.skillID == SAITAMA_KING_ENGINE)
                && net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            // 282 XML: footholdInstallSummoned, stand/attack animations, common/time in seconds.
            // Use the server's known character position; the 282 request placement body is not decoded yet.
            Summon summon = Summon.getSummonByAndSetStat(chr, use.skillID, level);
            summon.setMoveAbility(MoveAbility.Stop);
            summon.setFlyMob(false);
            chr.getField().spawnSummon(summon);
        }
        if (use.skillID == TANJIRO_WARRIOR && net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            chr.getTemporaryStatManager().sendStat(CharacterTemporaryStat.BasicStatUp, tanjiroWarriorBuff(chr.getLevel()));
        }
        if (use.skillID == TANJIRO_WILL || use.skillID == SAITAMA_WILL)
            chr.getTemporaryStatManager().removeAllDebuffs();
        if ((use.skillID == TANJIRO_WILL || use.skillID == SAITAMA_WILL) && net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            chr.getTemporaryStatManager().sendStat(CharacterTemporaryStat.AsrR,
                    use.skillID == SAITAMA_WILL ? saitamaWillBuff() : tanjiroWillBuff());
        }
        EnumMap<CharacterTemporaryStat, Option> buffs = getExplicitBuffs(use.skillInfo, level);
        if (!buffs.isEmpty()) chr.getTemporaryStatManager().sendStat(buffs);
    }

    public static Option tanjiroWillBuff() {
        Option option = new Option();
        option.nOption = 100;
        option.rOption = TANJIRO_WILL;
        // String.wz explicitly specifies 3 seconds; common/time=1 is not the immunity duration.
        option.tOption = 3;
        return option;
    }

    /** Saitama's localized description also specifies exactly three seconds. */
    public static Option saitamaWillBuff() {
        Option option = tanjiroWillBuff();
        option.rOption = SAITAMA_WILL;
        return option;
    }

    public static EnumMap<CharacterTemporaryStat, Option> getExplicitBuffs(SkillInfo info, int level) {
        EnumMap<CharacterTemporaryStat, Option> buffs = new EnumMap<>(CharacterTemporaryStat.class);
        if (info == null || info.getValue(SkillStat.time, level) <= 0) return buffs;
        // These three properties have direct, named server temporary-stat equivalents in the Yeti XML.
        addBuff(buffs, info, level, SkillStat.indieSpeed, CharacterTemporaryStat.IndieSpeed);
        addBuff(buffs, info, level, SkillStat.indiePadR, CharacterTemporaryStat.IndiePADR);
        addBuff(buffs, info, level, SkillStat.indieExp, CharacterTemporaryStat.IndieEXP);
        return buffs;
    }

    /** Visible skill:180s; hidden growth121001113 x=u(characterLevel/5), as the localized description states. */
    public static Option tanjiroWarriorBuff(int characterLevel) {
        Option option=new Option();
        int value=SkillData.getSkillInfoById(121001113).getValue(SkillStat.x, characterLevel);
        option.nOption=value;
        option.nValue=value; // BaseStat's BasicStatUp mapping reads nValue for STR/DEX/INT/LUK percentages.
        option.rOption=TANJIRO_WARRIOR;
        option.tOption=SkillData.getSkillInfoById(TANJIRO_WARRIOR).getValue(SkillStat.time,1);
        return option;
    }

    private static void addBuff(EnumMap<CharacterTemporaryStat, Option> buffs, SkillInfo info, int level,
                                SkillStat property, CharacterTemporaryStat temporary) {
        int value = info.getValue(property, level);
        if (value == 0) return;
        Option option = new Option();
        option.nReason = info.getSkillId();
        option.nValue = value;
        option.tTerm = info.getValue(SkillStat.time, level);
        buffs.put(temporary, option);
    }
}
