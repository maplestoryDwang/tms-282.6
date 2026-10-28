package net.swordie.ms.client.jobs.adventurer.magician;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.*;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.jobs.adventurer.Beginner;
import net.swordie.ms.client.jobs.adventurer.archer.Pathfinder;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.FieldConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.*;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.util.*;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class Magician extends Beginner {

    //Common
    public static final int MAPLE_RETURN = 1281;

    public static final int MAGIC_GUARD = 2001002;
    public static final int MAGIC_GUARD_282 = 2000002; // type11, switchDamtoMP; learned effect without a CTS.
    public static final int TELEPORT = 2001009;

    // V skills
    public static final int UNRELIABLE_MEMORY = 400001021;
    public static boolean isHexaArcana(int skillId) {
        return skillId >= 500001021 && skillId <= 500001023;
    }

    public static final List<Integer> unreliableMemFP = new ArrayList<>() {{
        add(2001008);
        add(2101004);
        add(2101005);
        add(2111002);
        add(2111003);
        add(2111010);
        add(2121006);
        add(2121003);
        add(2121007);
        add(2121011);
        add(2121004);
        add(2121005);
        add(2121052);
        add(2121053);
    }};

    public static final List<Integer> unreliableMemIL = new ArrayList<Integer>() {{
        add(2001008);
        add(2201008);
        add(2201005);
        add(2211002);
        add(2211010);
        add(2211011);
        add(2221006);
        add(2221007);
        add(2221012);
        add(2221004);
        add(2221005);
        add(2221052);
        add(2221053);
    }};

    public static final List<Integer> unreliableMemBishop = new ArrayList<Integer>() {{
        add(2001008);
        add(2301005);
        add(2301002);
        add(2311004);
        add(2311011);
        add(2311001);
        add(2311012);
        add(2321007);
        add(2321008);
        add(2321006);
        add(2321004);
        add(2321003);
        add(2321052);
        add(2321053);
    }};

    private final int[] addedSkills = new int[]{
            MAPLE_RETURN
    };

    public Magician(Char chr) {
        super(chr);
        if (chr.getId() != 0 && isHandlerOfJob(chr.getJob())) {
            for (int id : addedSkills) {
                if (!chr.hasSkill(id)) {
                    Skill skill = SkillData.getSkillDeepCopyById(id);
                    if (skill != null) {
                        skill.setCurrentLevel(skill.getMasterLevel());
                        chr.addSkill(skill);
                    }
                }
            }
        }
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isAdventurerMage(id);
    }

    // Skill related methods -------------------------------------------------------------------------------------------
    @Override
    public void handleSkill(Client c, InPacket inPacket, SkillUseInfo skillUseInfo) {
        if (!JobConstants.isPhantom(chr.getJob())) {
            super.handleSkill(c, inPacket, skillUseInfo);
        }
        Char chr = c.getChr();
        SkillInfo si = skillUseInfo.skillInfo;
        int slv = skillUseInfo.slv;
        int skillID = skillUseInfo.skillID;
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        switch (skillID) {
            case UNRELIABLE_MEMORY:
            case 500001021:
            case 500001022:
            case 500001023:
                if (Tms282RemoteBuffs.active()) {
                    // TMS 282 repurposes this id as Arcana (three declining stages), not a random skill cast.
                    o1.nOption = o1.xOption = si.getValue(x, slv);
                    o1.yOption = si.getValue(y, slv) * 1000;
                    o1.zOption = si.getValue(z, slv);
                    o1.wOption = si.getValue(u, slv);
                    o1.uOption = si.getValue(u2, slv);
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    Option previous = tsm.getOption(Arcana);
                    if (previous.rOption != 0 && previous.rOption != skillID) tsm.removeStatsBySkill(previous.rOption);
                    EnumMap<CharacterTemporaryStat, Option> stats = new EnumMap<>(CharacterTemporaryStat.class);
                    stats.put(Arcana, o1);
                    if (isHexaArcana(skillID)) {
                        Option magic = new Option();
                        magic.nReason = skillID;
                        magic.nValue = si.getValue(indieMadR, slv);
                        magic.tTerm = o1.tOption;
                        stats.put(IndieMADR, magic);
                    }
                    tsm.sendStat(stats);
                    break;
                }
                if (skillID != UNRELIABLE_MEMORY) break;
                chr.write(UserLocal.unreliableMemory(Util.getRandomFromCollection(
                        JobConstants.isFirePoison(chr.getJob()) ? unreliableMemFP :
                                JobConstants.isIceLightning(chr.getJob()) ? unreliableMemIL :
                                        JobConstants.isBishop(chr.getJob()) ? unreliableMemBishop :
                                                Collections.singleton(1))));
                break;
            case MAGIC_GUARD:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                tsm.sendStat(MagicGuard, o1);
                break;
            case MAPLE_RETURN:
                Field toField = chr.getOrCreateFieldByCurrentInstanceType(si.getValue(x, slv));
                chr.warp(toField);
                break;
        }
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleRemoveCTS(CharacterTemporaryStat cts, List<Option> options) {
        if (cts == Arcana) for (Option option : options) {
            if (isHexaArcana(option.rOption)) {
                TemporaryStatManager tsm = chr.getTemporaryStatManager();
                Option magic = tsm.getOptByCTSAndSkill(IndieMADR, option.rOption);
                if (magic != null) tsm.removeIndieStat(IndieMADR, magic);
            }
        }
        super.handleRemoveCTS(cts, options);
    }

    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        boolean tms282 = net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
        boolean phantom282 = tms282 && chr.getJobHandler() instanceof net.swordie.ms.client.jobs.legend.Phantom;
        int guardSkillId = tms282 && chr.hasSkill(MAGIC_GUARD_282) ? MAGIC_GUARD_282 : MAGIC_GUARD;
        if (!phantom282 && (guardSkillId == MAGIC_GUARD_282 || tsm.hasStat(MagicGuard))) {
            Skill skill = chr.getSkill(guardSkillId);
            SkillInfo si = SkillData.getSkillInfoById(guardSkillId);
            int dmgPerc = si.getValue(x, skill.getCurrentLevel());
            int dmg = hitInfo.hpDamage;
            int mpDmg = (int) (dmg * (dmgPerc / 100D));
            mpDmg = chr.getStat(Stat.mp) - mpDmg < 0 ? chr.getStat(Stat.mp) : mpDmg;
            hitInfo.hpDamage = dmg - mpDmg;
            hitInfo.mpDamage = mpDmg;
        }
        super.handleHit(c, inPacket, hitInfo);
    }

    @Override
    public void handleLevelUp(short level) {
        super.handleLevelUp(level);
        // hacks to bypass the quest glitch (accept but no packet)
        var sm = chr.getScriptManager();
        if (level == 60 || level == 100) {
            final short jobID = chr.getJob();
            if (!JobConstants.canJobAdvance(jobID)) {
                return;
            }
            final short next = JobConstants.nextJob(jobID);
            sm.setJob(next);
            sm.completeQuestNoRewards(level == 60 ? 1434 : 1452);
            if (level == 60) {
                if (jobID == JobConstants.JobEnum.FP_WIZARD.getJobId()) {
                    sm.completeQuestNoRewards(1435);
                } else if (jobID == JobConstants.JobEnum.IL_WIZARD.getJobId()) {
                    sm.completeQuestNoRewards(1436);
                } else {
                    sm.completeQuestNoRewards(1437);
                }
            } else {
                sm.completeQuestNoRewards(1453);
            }
            sm.addSPJobAdv(jobID, 5);
            sm.addSPJobAdv(next, 3);
        }
    }
}
