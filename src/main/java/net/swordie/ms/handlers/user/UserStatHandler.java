package net.swordie.ms.handlers.user;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.ExtendSP;
import net.swordie.ms.client.character.skills.HyperStat;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.client.jobs.cygnus.DawnWarrior;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.*;
import net.swordie.ms.enums.InstanceTableType;
import net.swordie.ms.enums.Stat;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.loaders.containerclasses.MakingSkillRecipe;
import net.swordie.ms.scripts.ScriptType;
import net.swordie.ms.util.DataPrinter;

import java.util.*;

public class UserStatHandler {

    @Handler(op = InHeader.USER_SKILL_UP_REQUEST)
    public static void handleUserSkillUpRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        int skillID = inPacket.decodeInt();
        int amount = inPacket.decodeInt();
        if (amount < 1) {
            chr.chatPopup("發生未知錯誤。");
            chr.dispose();
            return;
        }
        // seperate skill/current skills for adding stuff to the base cache if everything is succesful
        Skill skill = SkillData.getSkillDeepCopyById(skillID);
        Skill curSkill = chr.getSkill(skillID);
        byte jobLevel = (byte) JobConstants.getJobLevel((short) skill.getRootId());
        if (JobConstants.isZero((short) skill.getRootId())) {
            jobLevel = JobConstants.getJobLevelByZeroSkillID(skillID);
        }
        Map<Stat, Object> stats;
        int rootId = skill.getRootId();
        if ((!JobConstants.isBeginnerJob((short) rootId) && !SkillConstants.isMatching(rootId, chr.getJob())) || SkillConstants.isSkillFromItem(skillID)) {
            DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("Character %d tried adding an invalid skill (job %d, skill id %d)", chr.getId(), rootId, skillID));
            chr.chatPopup("發生未知錯誤。");
            chr.dispose();
            return;
        }
        if (JobConstants.isBeginnerJob((short) rootId)) {
            stats = new HashMap<>();
            int spentSp = 0;
            for (Skill s : chr.getSkills()) {
                if (SkillConstants.isBeginnerSpAddableSkill(s.getSkillId())) {
                    int currentLevel = s.getCurrentLevel();
                    spentSp += currentLevel;
                }
            }
            int totalSp;
            if (JobConstants.isResistance((short) skill.getRootId())) {
                totalSp = Math.min(chr.getLevel(), GameConstants.RESISTANCE_SP_MAX_LV) - 1; // sp gained from 2~10
            } else {
                totalSp = Math.min(chr.getLevel(), GameConstants.BEGINNER_SP_MAX_LV) - 1; // sp gained from 2~7
            }
            if (totalSp - spentSp >= amount) {
                int curLevel = curSkill == null ? 0 : curSkill.getCurrentLevel();
                int max = curSkill == null ? skill.getMasterLevel() : curSkill.getMasterLevel();
                if (max == 0) {
                    // some beginner skills have no max level, default is 3
                    max = 3;
                }
                int newLevel = curLevel + amount > max ? max : curLevel + amount;
                skill.setCurrentLevel(newLevel);
                chr.addSkill(skill);
                chr.write(WvsContext.changeSkillRecordResult(skill));
                return;
            }
        } else if (JobConstants.isExtendSpJob(chr.getJob())) {
            ExtendSP esp = chr.getAvatarData().getCharacterStat().getExtendSP();
            int currentSp = esp.getSpByJobLevel(jobLevel);
            if (currentSp >= amount) {
                int curLevel = curSkill == null ? 0 : curSkill.getCurrentLevel();
                int max = 0;
                //Some skill don't need mastery Book but have mastery Level = 10.
                switch (skillID) {
                    case 1120012:
                    case 1320011:
                    case 21120011:
                        max = skill.getMaxLevel();
                        break;
                    default:
                        max = curSkill == null ? skill.getMasterLevel() : curSkill.getMasterLevel();
                        break;
                }
                int newLevel;
                if (curLevel + amount >= max) {
                    newLevel = max;
                    amount = max - curLevel;
                } else {
                    newLevel = curLevel + amount;
                }
                if (curSkill != null) {
                    if (curSkill.getMasterLevel() > skill.getMasterLevel()) {
                        skill.setMasterLevel(curSkill.getMasterLevel());
                    }
                }
                skill.setCurrentLevel(newLevel);
                esp.setSpToJobLevel(jobLevel, currentSp - amount);
                chr.addSkill(skill);
                chr.write(WvsContext.changeSkillRecordResult(skill));
                stats = new HashMap<>();
                stats.put(Stat.sp, esp);
            } else {
                DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("Character %d tried adding a skill without having the required amount of sp (required %d, has %d)", chr.getId(), currentSp, amount));
                return;
            }
        } else {
            int currentSp = chr.getAvatarData().getCharacterStat().getSp();
            if (currentSp >= amount) {
                int curLevel = curSkill == null ? 0 : curSkill.getCurrentLevel();
                int max = curSkill == null ? skill.getMasterLevel() : curSkill.getMasterLevel();
                int newLevel;
                if (curLevel + amount > max) {
                    newLevel = max;
                    amount = max - curLevel;
                } else {
                    newLevel = curLevel + amount;
                }
                if (curSkill != null) {
                    if (curSkill.getMasterLevel() > skill.getMasterLevel()) {
                        skill.setMasterLevel(curSkill.getMasterLevel());
                    }
                }
                skill.setCurrentLevel(newLevel);
                chr.getAvatarData().getCharacterStat().setSp(currentSp - amount);
                chr.addSkill(skill);
                chr.write(WvsContext.changeSkillRecordResult(skill));
                stats = new HashMap<>();
                stats.put(Stat.sp, chr.getAvatarData().getCharacterStat().getSp());
            } else {
                DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("Character %d tried adding a skill without having the required amount of sp (required %d, has %d)", chr.getId(), currentSp, amount));
                return;
            }
        }
        if (stats != null) {
            chr.sendStatsPacket(stats);
            if (EventConstants.HYPER_BURNING_MAX && chr.hasQuest(102430)
                    && !"done".equals(chr.getQRValueByKey(102430, "step"))) {
                chr.getScriptManager().startScript(chr, 102430, "q102430s_1", ScriptType.Quest);
            }
        } else {
            DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("skill stats are null (%d)", skillID));
            chr.chatPopup("發生未知錯誤。");
            chr.dispose();
        }
    }

    @Handler(op = InHeader.USER_ABILITY_UP_REQUEST)
    public static void handleUserAbilityUpRequest(Char chr, InPacket inPacket) {
        if (chr.getStat(Stat.ap) <= 0) {
            chr.chatPopup("發生未知錯誤。");
            chr.dispose();
            return;
        }
        inPacket.decodeInt(); // tick
        short stat = inPacket.decodeShort();
        Stat charStat = Stat.getByVal(stat);
        short amount = 1;
        boolean isHpOrMp = false;
        if (charStat == Stat.mmp || charStat == Stat.mhp) {
            isHpOrMp = true;
            amount = 20;
        }
        chr.addStat(charStat, amount);
        chr.addStat(Stat.ap, (short) -1);
        Map<Stat, Object> stats = new HashMap<>();
        if (isHpOrMp) {
            stats.put(charStat, chr.getStat(charStat));
        } else {
            stats.put(charStat, (short) chr.getStat(charStat));
        }
        stats.put(Stat.ap, (short) chr.getStat(Stat.ap));
        chr.sendStatsPacket(stats);
        chr.dispose();
    }

    @Handler(op = InHeader.USER_ABILITY_MASS_UP_REQUEST)
    public static void handleUserAbilityMassUpRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        int autoSpSize = inPacket.decodeInt();
        if (inPacket.getUnreadAmount() < autoSpSize * 12L) {
            chr.chatPopup("發生未知錯誤。");
            chr.dispose();
            return;
        }
        Stat charStat = null;
        Stat charStat2 = null;
        int primaryStat = (int) inPacket.decodeLong();
        int amount1 = inPacket.decodeInt();
        int SecondaryStat = autoSpSize > 1 ? (int) inPacket.decodeLong() : 0;
        int amount2 = autoSpSize > 1 ? inPacket.decodeInt() : 0;
        if (amount1 < 0 || amount2 < 0) {
            chr.chatPopup("發生未知錯誤。");
            chr.dispose();
            return;
        }
        if (amount1 + amount2 > chr.getStat(Stat.ap)) {
            chr.chatPopup("發生未知錯誤。");
            chr.dispose();
            return;
        }
        charStat = Stat.getByVal(primaryStat);
        charStat2 = Stat.getByVal(SecondaryStat);
        if (charStat != null) {
            boolean isHpOrMp = false;
            if (charStat == Stat.mmp || charStat == Stat.mhp) {
                isHpOrMp = true;
            }
            chr.addStat(charStat, amount1 * 20);
            chr.addStat(Stat.ap, (short) -amount1);
            Map<Stat, Object> stats = new HashMap<>();
            if (isHpOrMp) {
                stats.put(charStat, chr.getStat(charStat));
            } else {
                stats.put(charStat, (short) chr.getStat(charStat));
            }
            stats.put(Stat.ap, (short) chr.getStat(Stat.ap));
            chr.sendStatsPacket(stats);
        }
        if (charStat2 != null && amount2 != 0) {
            boolean isHpOrMp = false;
            if (charStat2 == Stat.mmp || charStat2 == Stat.mhp) {
                isHpOrMp = true;
            }
            chr.addStat(charStat2, amount2 * 20);
            chr.addStat(Stat.ap, (short) -amount2);
            Map<Stat, Object> stats = new HashMap<>();
            if (isHpOrMp) {
                stats.put(charStat2, chr.getStat(charStat2));
            } else {
                stats.put(charStat2, (short) chr.getStat(charStat2));
            }
            stats.put(Stat.ap, (short) chr.getStat(Stat.ap));
            chr.sendStatsPacket(stats);
        }
        chr.dispose();
    }

    @Handler(op = InHeader.USER_DOT_HEAL)
    public static void handleUserDotHeal(Char chr, InPacket inPacket) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int mask = inPacket.decodeInt();

        int heal = inPacket.decodeInt();
        if (tsm.hasStat(CharacterTemporaryStat.DotHealHPPerSecond)) {
            heal = (int) (tsm.getTotalNOptionOfStat(CharacterTemporaryStat.DotHealHPPerSecond) * chr.getMaxHP() / 100.0D);
        } else if (tsm.hasStat(CharacterTemporaryStat.DotHealMPPerSecond)) {
            heal = (int) (tsm.getTotalNOptionOfStat(CharacterTemporaryStat.DotHealMPPerSecond) * chr.getMaxMP() / 100.0D);
        }
        if (mask == Stat.hp.getVal()) {
            chr.heal(heal);
        } else if (mask == Stat.mp.getVal()) {
            chr.healMP(heal);
        }
    }

    @Handler(op = InHeader.USER_CHANGE_STAT_REQUEST)
    public static void handleUserChangeStatRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        int mask = inPacket.decodeInt();
        List<Stat> stats = Stat.getStatsByFlag(mask);
        inPacket.decodeInt();
        short hpVal = 0;
        short mpVal = 0;
        boolean hasHp = false;
        boolean hasMp = false;
        for (Stat stat : stats) {
            short val = inPacket.decodeShort();
            if (stat == Stat.hp) {
                hpVal = val;
                hasHp = true;
            } else if (stat == Stat.mp) {
                mpVal = val;
                hasMp = true;
            }
            // các stat khác decode bỏ qua
        }
        byte option = inPacket.decodeByte();
        if (hasHp) {
            chr.heal(hpVal);
        }
        if (hasMp) {
            chr.healMP(mpVal);
        }
    }

    @Handler(op = InHeader.USER_CHANGE_STAT_REQUEST_BY_ITEM_OPTION)
    public static void handleUserChangeStatByItemOptionRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        int mask = inPacket.decodeInt();
        int hp = inPacket.decodeInt();
        int mp = inPacket.decodeInt();
        if (hp > 0) {
            chr.heal(hp);
        }
        if (mp > 0) {
            chr.healMP(mp);
        }
    }

    @Handler(op = InHeader.USER_REQUEST_INSTANCE_TABLE)
    public static void handleUserRequestInstanceTable(Char chr, InPacket inPacket) {
        String requestStr = inPacket.decodeString();
        int type = inPacket.decodeInt();
        int subType = inPacket.decodeInt();

        InstanceTableType itt = InstanceTableType.getByStr(requestStr);
        if (itt == null) {
            System.out.printf("Unknown instance table type request %s, type %d, subType %d%n", requestStr, type, subType);
            chr.dispose();
            return;
        }
        int value;
        switch (itt) {
            // HyperSkills: both have the same requestStr. level = type * 5
            case HyperActiveSkill:
            case HyperPassiveSkill:
                if (subType == InstanceTableType.HyperActiveSkill.getSubType()) {
                    value = SkillConstants.getHyperActiveSkillSpByLv(type * 5);
                } else {
                    value = SkillConstants.getHyperPassiveSkillSpByLv(type * 5);
                }
                break;
            case HyperStatIncAmount:
                // type == level
                value = SkillConstants.getHyperStatSpByLv((short) type);
                break;
            case NeedHyperStatLv:
                // type == skill lv
                value = SkillConstants.getNeededSpForHyperStatSkill(type);
                break;
            case Skill_9200:
            case Skill_9201:
            case Skill_9202:
            case Skill_9203:
            case Skill_9204:
                // type == recommendSkillLevel - 1
                // subType == making skill level -1
                value = MakingSkillRecipe.getSuccessProb(Integer.parseInt(requestStr), type + 1, chr.getMakingSkillLevel(Integer.parseInt(requestStr)));
                break;
            default:
                System.out.printf("Unhandled instance table type request %s, type %d, subType %d%n", itt, type, subType);
                return;
        }

        chr.write(WvsContext.resultInstanceTable(requestStr, type, subType, true, value));
    }

    @Handler(ops = {InHeader.USER_HYPER_STAT_RESET_REQUEST})
    public static void handleUserHyperStatResetRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        int preset = inPacket.decodeInt();
        if (preset > 2 && preset < 0) {
            chr.chatPopup("發生未知錯誤。");
            chr.dispose();
            return;
        }
        if (chr.getMoney() < GameConstants.HYPER_STAT_RESET_COST) {
            chr.chatPopup(String.format("楓幣不足，無法執行該操作。（需要：%d 楓幣）", GameConstants.HYPER_STAT_RESET_COST));
            chr.dispose();
        } else if (chr.getLevel() < 140) {
            chr.chatPopup("發生未知錯誤。");
            chr.dispose();
        } else {
            Map<Stat, Object> stats = new HashMap<>();
            chr.deductMoney(GameConstants.HYPER_STAT_RESET_COST);
            List<Skill> skills = new ArrayList<>();
            for (HyperStat hyperStats : chr.getHyperStats(preset)) {
                hyperStats.setSkillLevel(0);
                Skill skill = chr.getSkill(hyperStats.getSkillID(), true);
                if (skill != null) {
                    skill.setCurrentLevel(0);
                    skills.add(skill);
                    chr.addSkill(skill);
                }
            }
            stats.put(Stat.sp, chr.getAvatarData().getCharacterStat().getExtendSP());
            chr.write(WvsContext.changeSkillRecordResult(skills, true, false, false));
            chr.write(WvsContext.updateHyperPresets(chr, preset, (byte) 1));
            chr.write(WvsContext.receiveHyperStatResetResult(chr.getId(), true, true));
            chr.sendStatsPacket(stats);
        }
        chr.dispose();
    }

    @Handler(ops = {InHeader.USER_HYPER_STAT_CHANGE_PRESET_REQUEST})
    public static void handleUserHyperStatChangePresetRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        int preset = inPacket.decodeInt();
        if (preset > 2 && preset < 0) {
            chr.chatPopup("發生未知錯誤。");
            chr.dispose();
            return;
        }
        handleChangeHyperStatPreset(chr, preset);
    }

    public static void handleChangeHyperStatPreset(Char chr, int preset) {
        Map<Stat, Object> stats = new HashMap<>();
        List<Skill> skills = new ArrayList<>();
        for (HyperStat hyperStat : chr.getHyperStats(preset)) {
            Skill skill = chr.getSkill(hyperStat.getSkillID(), true);
            skill.setCurrentLevel(hyperStat.getSkillLevel());
            chr.addSkill(skill);
            skills.add(skill);
        }
        chr.createQuestWithQRValue(QuestConstants.HYPER_STATS_PRESET, "hyperstats=" + preset);
        stats.put(Stat.sp, chr.getAvatarData().getCharacterStat().getExtendSP());
        chr.write(WvsContext.changeSkillRecordResult(skills, true, false, false));
        chr.write(WvsContext.updateHyperPresets(chr, preset, (byte) 0));
        chr.sendStatsPacket(stats);
        chr.dispose();
    }

    @Handler(op = InHeader.USER_HYPER_SKILL_RESET_REQUEST)
    public static void handleUserHyperSkillsResetRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick        
        boolean all = inPacket.decodeByte() != 0;
        int type = inPacket.decodeByte();
        int availableHyperSkillSP = chr.getSpentActiveHyperSkillSp() + chr.getSpentPassiveHyperSkillSp();
        if (chr.getMoney() < GameConstants.HYPER_SKILL_RESET_COST) {
            chr.chatPopup(String.format("楓幣不足，無法執行該操作。（需要：%d 楓幣）", GameConstants.HYPER_SKILL_RESET_COST));
            chr.dispose();
        } else if (availableHyperSkillSP <= 0 || chr.getLevel() < 140) {
            chr.chatPopup("發生未知錯誤。");
            chr.dispose();
        } else {
            Map<Stat, Object> stats = new HashMap<>();
            List<Integer> currentHyperSkills = SkillConstants.getHyperSkill(chr.getJob());
            chr.deductMoney(GameConstants.HYPER_SKILL_RESET_COST);
            List<Skill> newHyperSkills = new ArrayList<>();
            for (int id : currentHyperSkills) {
                Skill skill = chr.getSkill(id);
                if (skill != null) {
                    skill.setCurrentLevel(0);
                    newHyperSkills.add(skill);
                    chr.removeFromBaseStatCache(skill);
                    chr.addSkill(skill);
                }
            }
            chr.addSpToJobByCurrentLevel(availableHyperSkillSP);
            stats.put(Stat.sp, chr.getAvatarData().getCharacterStat().getExtendSP());
            chr.write(WvsContext.changeSkillRecordResult(newHyperSkills, true, false, false));
            chr.write(WvsContext.receiveHyperSkillResetResult(chr.getId(), true, true));
        }
        chr.dispose();
    }

    @Handler(op = InHeader.USER_HYPER_SKILL_UP_REQUEST)
    public static void handleUserHyperSkillUpRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        int skillID = inPacket.decodeInt();
        SkillInfo skillInfo = SkillData.getSkillInfoById(skillID);
        if (skillInfo == null) {
            System.out.printf("Character %d attempted assigning hyper SP to a skill with null skill info (%d).%n", chr.getId(), skillID);
            chr.dispose();
            return;
        }
        if (skillInfo.getHyper() != 0 && !SkillConstants.isMatching(skillInfo.getRootId(), chr.getJob()) && !SkillConstants.isHyperSkill(chr.getJob(), skillID)) {
            System.out.printf("Character %d attempted assigning hyper SP to a wrong hyper skill (skill id %d, player job %d)%n", chr.getId(), skillID, chr.getJob());
            chr.dispose();
            return;
        }
        Skill skill = chr.getSkill(skillID, true);
        if (skillInfo.getHyper() != 0) { // Passive/Active hyper
            chr.write(UserPacket.effect(Effect.avatarOriented("Effect/CharacterEff.img/LevelUpHyper")));
            chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.avatarOriented("Effect/CharacterEff.img/LevelUpHyper")));
        } else { // not hyper skill
            System.out.printf("Character %d attempted assigning hyper stat to an improper skill. (%d, job %d)%n", chr.getId(), skillID, chr.getJob());
            chr.dispose();
            return;
        }
        chr.removeFromBaseStatCache(skill);
        skill.setCurrentLevel(skill.getCurrentLevel() + 1);
        chr.addToBaseStatCache(skill);
        chr.addSkill(skill);
        chr.write(WvsContext.changeSkillRecordResult(skill));
        chr.dispose();
        if (EventConstants.HYPER_BURNING_MAX
                && chr.hasQuest(102446)
                && !"done".equals(chr.getQRValueByKey(102446, "step"))) {
            chr.getScriptManager().startScript(chr, 102446, "q102446s_1", ScriptType.Quest);
        }
    }

    @Handler(op = InHeader.USER_HYPER_STAT_UP_REQUEST)
    public static void handleUserHyperStatUpRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        int preset = inPacket.decodeInt(); // preset
        if (preset > 2 && preset < 0) {
            chr.chatPopup("發生未知錯誤。");
            chr.dispose();
            return;
        }
        int skillID = inPacket.decodeInt();
        int amount = inPacket.decodeInt();
        HyperStat hyperStat = chr.getHyperStats(preset, skillID);
        SkillInfo skillInfo = SkillData.getSkillInfoById(skillID);
        if (skillInfo == null || hyperStat == null) {
            System.out.printf("Character %d attempted assigning hyper SP to a skill with null skill info (%d).%n", chr.getId(), skillID);
            chr.dispose();
            return;
        }
        Skill skill = chr.getSkill(skillID, true);
        short level = chr.getLevel();
        if (skillInfo.getHyperStat() != 0) {
            int totalHyperSp = SkillConstants.getTotalHyperStatSpByLevel(level);
            int spentSp = chr.getSpentHyperSp(preset);
            int availableSp = totalHyperSp - spentSp;
            int neededSp = SkillConstants.getNeededSpForHyperStatSkill(hyperStat.getSkillLevel() + amount);
            if (hyperStat.getSkillLevel() >= skill.getMaxLevel() || availableSp < neededSp) {
                System.out.printf("Character %d attempted assigning too many hyper stat levels. Available SP %d, needed %d, current %d (%d, job %d)%n", chr.getId(), availableSp, neededSp, skill.getCurrentLevel(), skillID, chr.getJob());
                chr.dispose();
                return;
            }
        } else {
            System.out.printf("Character %d attempted assigning hyper stat to an improper skill. (%d, job %d)%n", chr.getId(), skillID, chr.getJob());
            chr.dispose();
            return;
        }
        chr.removeFromBaseStatCache(skill);
        skill.setCurrentLevel(skill.getCurrentLevel() + amount);
        chr.addToBaseStatCache(skill);
        chr.addSkill(skill);
        hyperStat.setSkillLevel(skill.getCurrentLevel());
        chr.write(WvsContext.updateHyperPresets(chr, preset, (byte) skill.getCurrentLevel()));
        chr.write(WvsContext.changeSkillRecordResult(skill));
        if (skillID == Job.HYPER_STAT_ARCANE_FORCE) {
            chr.getJobHandler().giveHyperArcaneForceBuff();
        }
        chr.dispose();
    }
}
