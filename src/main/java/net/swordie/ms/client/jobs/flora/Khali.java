package net.swordie.ms.client.jobs.flora;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.ExtraSkill;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.JobConstants;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class Khali extends Job {

    /** 環刃推進器 (type 10): attack speed {@code x} for {@code time} s. */
    public static final int CHAKRAM_THRUST = 154101005;
    /** 召喚查克里 (type 50, fixLevel): on / off; 282 CTS SummonChakri. */
    public static final int SUMMON_CHAKRI = 154111000;
    /** 沙漠面紗 (type 10): {@code time} (3) s invincible. */
    public static final int DESERT_VEIL = 154121004;
    /** 雷普勇士的意志 (type 35). */
    public static final int HEROS_WILL = 154121006;
    /** 忘卻 (type 36, hyper): {@code time} (30) s; 282 CTS Oblivion. */
    public static final int OBLIVION = 154121043;

    /**
     * 影刃 / VI (type 53, {@code time} 20 s, {@code cooltimeMS} 500, extraSkillInfo skill = itself, delay 240): a 虛空
     * skill ({@code skillList}) arms it for {@code time} seconds, a hit of a 藝術 / 妖術 skill ({@code skillList2}) then
     * calls the blade on the first mob hit. Which list arms and which fires is 推定 from the 282 data; live untested.
     */
    public static final int SHADOW_BLADE = 154110005;
    public static final int SHADOW_BLADE_VI = 154141014;
    private static final java.util.Set<Integer> SHADOW_ARM = java.util.Set.of(
            154101004, 154111004, 154111011, 154121003, 154121009, 154121011, 154121012, 400041084, 400041085,
            400041086, 154141003, 154141004, 154141008);
    private static final java.util.Set<Integer> SHADOW_FIRE = java.util.Set.of(
            154001000, 154101000, 154101001, 154101002, 154111002, 154111006, 154121000, 154121001, 154121002,
            400041082, 400041083, 154141000, 154141500, 154141501, 154141503, 154141001, 154141002, 154141009,
            154141010, 154141011);
    private long shadowArmedUntil, shadowReadyAt;

    /**
     * 召喚查克里 installed chakri (hidden, fixLevel, summon node; {@code time} 30 s, at most {@code x} 6, {@code prop} 30 %,
     * skillList = the 虛空 skills). Only the installation is done here; its SecondAtom attack 154110001 is not.
     */
    public static final int CHAKRI = 154110010;

    /** 282 Skill.wz 154121043 skillList: the skills 忘卻 speeds up. */
    private static final java.util.Set<Integer> OBLIVION_SKILLS = java.util.Set.of(
            154001003, 154101004, 154111004, 154111011, 154121003, 154121009, 154121011, 154121012, 154141003,
            154141004, 154141007, 154141008);

    /**
     * Cooldown seconds of {@code skillId} while 忘卻 is active: {@code u} (50) % shorter, at least 1 s. That {@code u} is
     * the cooldown cut is 推定 from the 282 data ({@code v} 30 is not applied).
     */
    public static int oblivionCooldownSeconds(Char chr, int skillId, int seconds) {
        if (seconds <= 1 || chr == null || !OBLIVION_SKILLS.contains(skillId)
                || !chr.getTemporaryStatManager().hasStatBySkillId(OBLIVION)) {
            return seconds;
        }
        SkillInfo si = net.swordie.ms.loaders.SkillData.getSkillInfoById(OBLIVION);
        int cut = si == null ? 0 : Math.min(90, Math.max(0, si.getValue(u, 1)));
        return Math.max(1, (int) Math.ceil(seconds * (100 - cut) / 100.0));
    }

    // V Skills
    public static final int HEX_PANDEMONIUM = 400041082;
    public static final int VOID_BURST = 400041084;
    public static final int ARTS_ASTRA = 400041087;
    public static final int RESONATE_ULTIMATUM = 400041089;

    public Khali(Char chr) {
        super(chr);
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isKhali(id);
    }

    @Override
    public void handleAttack(Client c, AttackInfo attackInfo, SkillInfo si, long now) {
        super.handleAttack(c, attackInfo, si, now);
        if (Protocol.active().version() != ProtocolVersion.TMS_282_3) {
            return;
        }
        installChakri282(attackInfo);
        int blade = chr.getSkillLevel(SHADOW_BLADE_VI) > 0 ? SHADOW_BLADE_VI : SHADOW_BLADE;
        int slv = chr.getSkillLevel(blade);
        SkillInfo bi = net.swordie.ms.loaders.SkillData.getSkillInfoById(blade);
        if (slv <= 0 || bi == null) {
            return;
        }
        if (SHADOW_ARM.contains(attackInfo.skillId)) {
            shadowArmedUntil = now + Math.max(1, bi.getValue(time, slv)) * 1000L;
            return;
        }
        if (!SHADOW_FIRE.contains(attackInfo.skillId) || attackInfo.mobAttackInfo.isEmpty()
                || now > shadowArmedUntil || now < shadowReadyAt) {
            return;
        }
        int mobId = attackInfo.mobAttackInfo.get(0).mobId;
        Life life = chr.getField().getLifeByObjectID(mobId);
        if (!(life instanceof Mob)) {
            return;
        }
        shadowReadyAt = now + 500;
        ExtraSkill extra = new ExtraSkill(blade, life.getPosition());
        extra.TriggerSkillID = attackInfo.skillId;
        extra.FaceLeft = attackInfo.left ? 1 : 0;
        extra.Delay = 240;
        extra.Value = 1;
        extra.MobOIDs.add(mobId);
        chr.write(UserLocal.registerExtraSkill(blade, java.util.List.of(extra)));
    }

    private void installChakri282(AttackInfo attackInfo) {
        SkillInfo ci = net.swordie.ms.loaders.SkillData.getSkillInfoById(CHAKRI);
        if (ci == null || chr.getField() == null || !(SHADOW_ARM.contains(attackInfo.skillId) || attackInfo.skillId == 154141007)
                || !chr.getTemporaryStatManager().hasStatBySkillId(SUMMON_CHAKRI)
                || !net.swordie.ms.util.Util.succeedProp(Math.max(1, ci.getValue(prop, 1)))) {
            return;
        }
        long mine = chr.getField().getSummons().stream()
                .filter(x -> x.getOwnerId() == chr.getId() && x.getSkillID() == CHAKRI).count();
        if (mine >= Math.max(1, ci.getValue(x, 1))) {
            return;
        }
        net.swordie.ms.life.Summon chakri = net.swordie.ms.life.Summon.getSummonBy(chr, CHAKRI, 1);
        chakri.setMoveAction((byte) 4);
        chakri.setMoveAbility(net.swordie.ms.enums.MoveAbility.Stop);
        chakri.setAssistType(net.swordie.ms.enums.AssistType.None);
        chakri.setAttackActive(false);
        chr.getField().spawnAddSummon(chakri);
    }

    /**
     * 282.3 only (265 had no Khali logic): the buffs of the uses the live log showed as accepted with no effect.
     * 死亡綻放 is spawned by {@code Tms282InstallSummons} in {@link Job#handleSkill}. Option meanings of SummonChakri /
     * Oblivion are 推定 (flag 1); live untested.
     */
    @Override
    public void handleSkill(Client c, InPacket inPacket, SkillUseInfo skillUseInfo) {
        super.handleSkill(c, inPacket, skillUseInfo);
        SkillInfo si = skillUseInfo.skillInfo;
        if (si == null || inPacket == null || Protocol.active().version() != ProtocolVersion.TMS_282_3) {
            return;
        }
        int slv = skillUseInfo.slv;
        int skillID = skillUseInfo.skillID;
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        switch (skillID) {
            case CHAKRAM_THRUST -> {
                o.nOption = si.getValue(x, slv);
                o.rOption = skillID;
                o.tOption = si.getValue(time, slv);
                tsm.sendStat(Booster, o);
            }
            case SUMMON_CHAKRI -> {
                if (tsm.hasStatBySkillId(skillID)) {
                    tsm.removeStatsBySkill(skillID);
                } else {
                    o.nOption = 1;
                    o.rOption = skillID;
                    tsm.sendStat(SummonChakri, o);
                }
            }
            case DESERT_VEIL -> {
                o.nReason = skillID;
                o.nValue = 1;
                o.tTerm = Math.max(1, si.getValue(time, slv));
                tsm.sendStat(IndieNotDamaged, o);
            }
            case HEROS_WILL -> tsm.removeAllDebuffs();
            case OBLIVION -> {
                o.nOption = 1;
                o.rOption = skillID;
                o.tOption = si.getValue(time, slv);
                tsm.sendStat(Oblivion, o);
            }
            default -> {
            }
        }
    }
}
