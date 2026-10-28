package net.swordie.ms.client.character.skills.matrix;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.connection.packet.Effect;
import net.swordie.ms.connection.packet.UserPacket;
import net.swordie.ms.connection.packet.UserRemote;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.Util;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;
import static net.swordie.ms.client.jobs.Job.*;

public class SpecialNodeSkill {

    private final Char chr;
    private boolean isRune;
    private long lastUseTime;

    public SpecialNodeSkill(Char chr) {
        this.chr = chr;
    }

    public void activate() {
        Option o1 = new Option();
        boolean success = false;
        boolean resetRune = true;
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        boolean succeedProp = Util.succeedProp(10);
        if (chr.hasSkill(FRENZIED_STRENGTH_I)) {
            SkillInfo si1 = SkillData.getSkillInfoById(FRENZIED_STRENGTH_I);
            int slv1 = chr.getSkillLevel(FRENZIED_STRENGTH_I);
            if (!isRune() && succeedProp && System.currentTimeMillis() - getLastUseTime() >= 30000) {
                o1.nValue = si1.getValue(indieDamR, slv1);
                o1.nReason = FRENZIED_STRENGTH_I;
                o1.tTerm = si1.getValue(time, slv1);
                tsm.sendStat(IndieDamR, o1);
                success = true;
            }
        } else if (chr.hasSkill(FRENZIED_STRENGTH_II)) {
            SkillInfo si1 = SkillData.getSkillInfoById(FRENZIED_STRENGTH_II);
            int slv1 = chr.getSkillLevel(FRENZIED_STRENGTH_II);
            if (!isRune() && succeedProp && System.currentTimeMillis() - getLastUseTime() >= 30000) {
                o1.nValue = si1.getValue(indieDamR, slv1);
                o1.nReason = FRENZIED_STRENGTH_II;
                o1.tTerm = si1.getValue(time, slv1);
                tsm.sendStat(IndieDamR, o1);
                success = true;
            }
        } else if (chr.hasSkill(FRENZIED_STRENGTH_III)) {
            SkillInfo si1 = SkillData.getSkillInfoById(FRENZIED_STRENGTH_III);
            int slv1 = chr.getSkillLevel(FRENZIED_STRENGTH_III);
            if (!isRune() && succeedProp && System.currentTimeMillis() - getLastUseTime() >= 30000) {
                o1.nValue = si1.getValue(indieDamR, slv1);
                o1.nReason = FRENZIED_STRENGTH_III;
                o1.tTerm = si1.getValue(time, slv1);
                tsm.sendStat(IndieDamR, o1);
                success = true;
            }
        } else if (chr.hasSkill(FRENZIED_STRENGTH_V)) {
            SkillInfo si1 = SkillData.getSkillInfoById(FRENZIED_STRENGTH_V);
            int slv1 = chr.getSkillLevel(FRENZIED_STRENGTH_V);
            if (!isRune() && succeedProp && System.currentTimeMillis() - getLastUseTime() >= 30000) {
                o1.nValue = si1.getValue(indieDamR, slv1);
                o1.nReason = FRENZIED_STRENGTH_V;
                o1.tTerm = si1.getValue(time, slv1);
                tsm.sendStat(IndieDamR, o1);
                success = true;
            }
        } else if (chr.hasSkill(FATAL_STRIKE_I)) {
            SkillInfo si1 = SkillData.getSkillInfoById(FATAL_STRIKE_I);
            int slv1 = chr.getSkillLevel(FATAL_STRIKE_I);
            if (!isRune() && succeedProp && System.currentTimeMillis() - getLastUseTime() >= 30000) {
                o1.nValue = si1.getValue(indieDamR, slv1);
                o1.nReason = FATAL_STRIKE_I;
                o1.tTerm = si1.getValue(time, slv1);
                tsm.sendStat(IndieDamR, o1);
                success = true;
            }
        } else if (chr.hasSkill(FRENZIED_STRENGTH_V)) {
            SkillInfo si1 = SkillData.getSkillInfoById(FRENZIED_STRENGTH_V);
            int slv1 = chr.getSkillLevel(FRENZIED_STRENGTH_V);
            if (!isRune() && succeedProp && System.currentTimeMillis() - getLastUseTime() >= 30000) {
                o1.nValue = si1.getValue(indieDamR, slv1);
                o1.nReason = FRENZIED_STRENGTH_V;
                o1.tTerm = si1.getValue(time, slv1);
                tsm.sendStat(IndiePMdR, o1);
                success = true;
            }
        } else if (chr.hasSkill(KEEN_ATTACk_I)) {
            SkillInfo si2 = SkillData.getSkillInfoById(KEEN_ATTACk_I);
            int slv2 = chr.getSkillLevel(KEEN_ATTACk_I);
            if (!isRune() && succeedProp && System.currentTimeMillis() - getLastUseTime() >= 30000) {
                o1.nValue = si2.getValue(indieCr, slv2);
                o1.nReason = KEEN_ATTACk_I;
                o1.tTerm = si2.getValue(time, slv2);
                tsm.sendStat(IndieCrR, o1);
                success = true;
            }
        } else if (chr.hasSkill(KEEN_STRIKE_I)) {
            SkillInfo si3 = SkillData.getSkillInfoById(KEEN_STRIKE_I);
            int slv3 = chr.getSkillLevel(KEEN_STRIKE_I);
            if (!isRune() && succeedProp && System.currentTimeMillis() - getLastUseTime() >= 30000) {
                o1.nValue = si3.getValue(indieCD, slv3);
                o1.nReason = KEEN_STRIKE_I;
                o1.tTerm = si3.getValue(time, slv3);
                tsm.sendStat(IndieCD, o1);
                success = true;
            }
        } else if (chr.hasSkill(DEFENSE_SMASH_I)) {
            SkillInfo si4 = SkillData.getSkillInfoById(DEFENSE_SMASH_I);
            int slv4 = chr.getSkillLevel(DEFENSE_SMASH_I);
            if (!isRune() && succeedProp && System.currentTimeMillis() - getLastUseTime() >= 30000) {
                o1.nValue = si4.getValue(indieIgnoreMobpdpR, slv4);
                o1.nReason = DEFENSE_SMASH_I;
                o1.tTerm = si4.getValue(time, slv4);
                tsm.sendStat(IndieIgnoreMobpdpR, o1);
                success = true;
            }
        } else if (chr.hasSkill(RUNE_BLESSED)) {
            SkillInfo si5 = SkillData.getSkillInfoById(RUNE_BLESSED);
            int slv5 = chr.getSkillLevel(RUNE_BLESSED);
            if (isRune() && System.currentTimeMillis() - getLastUseTime() >= 30000) {
                o1.nOption = si5.getValue(indieNotDamaged, slv5);
                o1.rOption = RUNE_BLESSED;
                o1.tOption = si5.getValue(time, slv5);
                tsm.sendStat(NotDamaged, o1);
                if (isRune()) {
                    resetRune = false;
                }
                success = true;
            }
        } else if (chr.hasSkill(RUNE_EXP) || chr.hasSkill(CHARACTER_BUILDING)) {
            SkillInfo si6 = SkillData.getSkillInfoById(RUNE_EXP);
            int slv6 = chr.getSkillLevel(RUNE_EXP);
            if ((isRune() || succeedProp) && System.currentTimeMillis() - getLastUseTime() >= 30000) {
                o1.nValue = si6.getValue(indieExp, slv6);
                o1.nReason = RUNE_EXP;
                o1.tTerm = si6.getValue(time, slv6);
                tsm.sendStat(IndieEXP, o1);
                if (isRune()) {
                    resetRune = false;
                }
                success = true;
            }
        } else if (chr.hasSkill(BOSS_SLAYER)) {
            SkillInfo si7 = SkillData.getSkillInfoById(BOSS_SLAYER);
            int slv7 = chr.getSkillLevel(BOSS_SLAYER);
            if (!isRune() && succeedProp && System.currentTimeMillis() - getLastUseTime() >= 30000) {
                o1.nValue = si7.getValue(indieBDR, slv7);
                o1.nReason = BOSS_SLAYER;
                o1.tTerm = si7.getValue(time, slv7);
                tsm.sendStat(IndieBDR, o1);
                success = true;
            }
        }
        if (success) {
            if (resetRune) this.isRune = false;
            setLastUseTime(System.currentTimeMillis());
            chr.write(UserPacket.effect(Effect.avatarOriented("Effect/CharacterEff.img/VMatrixSP")));
            chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.avatarOriented("Effect/CharacterEff.img/VMatrixSP")), chr);
            return;
        }
        if (isRune()) {
            this.isRune = false;
        }
    }

    public boolean isRune() {
        return isRune;
    }

    public void setRune(boolean rune) {
        isRune = rune;
    }

    public long getLastUseTime() {
        return lastUseTime;
    }

    public void setLastUseTime(long lastUseTime) {
        this.lastUseTime = lastUseTime;
    }
}
