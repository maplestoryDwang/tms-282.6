package net.swordie.ms.client.jobs.resistance;

import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;

import java.util.Set;
import java.util.function.IntPredicate;

/**
 * TMS 282.3 Wild Hunter (狂豹獵人 3300/3310/3311/3312, skill audit research/tms2823/kanna-wh-282/README.md).
 * <p>
 * 282 reworked the job: one jaguar (阿希莉) that is always ridden ({@code 33001001} 美洲豹騎乘; "若未搭乘阿希莉，除移動技能外
 * 無法使用技能"), 狂野機關砲 crossbow attacks, 支援 / 指令 skills cast by a spirit jaguar and the 另一個咬擊 debuff. The 265
 * jaguar summons (33001007..33001015), 美洲豹捕捉, 爆走形態, 火眼晶晶 and the V jaguar skills are gone and several of
 * their ids now name other skills (e.g. 33001007 = 捕食者之躍：助跑, 33121004 = 指令：阿爾法浪潮, 33111007 = 粉碎線 hidden
 * attack). On 282 {@link WildHunter} runs none of its 265 skill / attack / debuff logic and only the pieces below,
 * which use CTS rows {@code cts-local.tsv} supports (IndiePADR, IndieMADR, CriticalBuff, IndieCD, IndieNotDamaged,
 * RideVehicle two-state). 捕食者之躍 / 另一個咬擊 / 原始綻放 / 誘餌信標 / 陷阱散佈器 need client RE (282-only CTS
 * PredatorsLeap 1026 and WHOverbite 1027, new summons); see the README.
 */
public final class Tms282WildHunter {
    /** 美洲豹騎乘 (type 13, fixLevel): RideVehicle, same as 265 but every skill needs it on 282. */
    public static final int RIDE_JAGUAR = 33001001;
    /** The only jaguar vehicle of the 282 client (isJaguarVehicle AECB00 compares with 0x1D7EB6). */
    public static final int JAGUAR_VEHICLE = 1932982;
    /** 咆哮 (type 11): party {@code x}% attack and magic attack for {@code time} s. */
    public static final int HOWLING = 33101004;
    /** 會心之眼 (type 10): party crit rate {@code x}% / crit damage {@code y}% for {@code time} s. */
    public static final int CRITICAL_EYE = 33121006;
    /** 狂獸附體 (type 10): {@code u2} s invincible. */
    public static final int BEAST_POSSESSION = 33111008;
    /** 滑流 (passive): every {@code y} ms recover {@code x}% max HP. */
    public static final int GLIDE = 33110012;
    /** 終極攻擊 / 進階終極攻擊 / 終極攻擊VI: the {@code finalAttack} node of every 282 crossbow skill names 33100005. */
    public static final int FINAL_ATTACK = 33100005;
    public static final int ADVANCED_FINAL_ATTACK = 33120011;
    public static final int FINAL_ATTACK_VI = 33140006;
    /** 楓葉淨化 (Hero's Will), same meaning as 265. */
    public static final int HEROS_WILL = 33121008;
    /** 祕密廣場的緊急會議 (Resistance), same meaning as 265. */
    public static final int SECRET_ASSEMBLY = 30001281;

    /** 265 {@link WildHunter} constants whose id is another skill on 282 or is gone from 282 Skill.wz. */
    public static final Set<Integer> CHANGED_265_IDS = Set.of(
            WildHunter.SUMMON_JAGUAR_GREY,      // 33001007 282 捕食者之躍：助跑 (265 summons a jaguar, drops the ride)
            WildHunter.FELINE_BERSERK,          // 33111007 282 狂野機關砲：粉碎線 hidden attack (265 IndieBooster/DamR/Speed)
            WildHunter.SHARP_EYES,              // 33121004 282 指令：阿爾法浪潮 (265 SharpEyes buff)
            WildHunter.FINAL_ATTACK_WH,         // 33100009 282 原始感知 passive (265 final attack)
            WildHunter.HEXA_FINAL_ATTACK_WH,    // 21140012 is Aran's 終極攻擊VI, never Wild Hunter
            WildHunter.CAPTURE,                 // 30001061 not in 282 Skill.wz
            WildHunter.PRIMAL_GRENADE,          // 400031032 not in 282 Skill.wz
            WildHunter.JAGUAR_STORM,            // 400031005 not in 282 Skill.wz
            WildHunter.PRIMAL_FURY              // 400031012 not in 282 Skill.wz
    );

    /**
     * 支援：撕裂爪 (type 53, {@code cooltimeMS} 2500): the spirit jaguar attack "在弩弓技能命中時". The 282 client never
     * starts it by itself: LP057E (5A2F650) with an assist id (isAssist 117E640) looks up the rotation root (10A6E40, always
     * 33100002), replaces it by the current skill of the 撕裂爪 → 獠牙撕咬 33110006 → 碎顱猛擊 33120002 rotation (10A6FB0)
     * and queues the attack, so the server only names the root.
     */
    public static final int ASSIST = 33100002;
    public static final long ASSIST_GUARD_MS = 2500;

    /** 282 Skill.wz 3310/33100002 {@code skillList}: the crossbow attacks whose hit calls the assist. */
    private static final Set<Integer> ASSIST_TRIGGERS = Set.of(
            33001000, 33101000, 33101001, 33111000, 33111007, 33121000, 33121003, 33141000, 33141002,
            400031072, 400031073, 33141500, 33141501);

    public static boolean assistTrigger(int skillId) {
        return ASSIST_TRIGGERS.contains(skillId);
    }

    /**
     * 捕食者之獵 mark (type 99, mobCount 1, {@code time} 600 s): CP04B4 33001005 names the target mob in ProcessType 19.
     * The 282 client leaps (hidden 33001006) only while it owns a 33001005 mark object, which LP0607 creates
     * (5A50C68: map [user+0xB4B8] lookup; factory 829954).
     */
    public static final int PREDATORS_LEAP = 33001005;
    public static final int PREDATORS_LEAP_JUMP = 33001006;
    /**
     * 另一個咬擊 / VI (passive, {@code time} 90 s, {@code x} 2): mark object 33110010 (factory 8298C5) on the mobs a
     * skillList crossbow attack hits; stack meaning 推定 (hits, at most {@code x}).
     */
    public static final int ANOTHER_BITE = 33110010;
    public static final int ANOTHER_BITE_VI = 33140011;

    /**
     * 原始綻放 (V, processtype 6, skillIconOnOff): {@code x}% final damage for {@code time} s. The 282 client starts and
     * stops its screen effects from the IndiePMdR (indie bit 44) entry whose reason is this skill (5FE0C45, 5FE3EF3).
     */
    public static final int PRIMAL_BLOOM = 400031074;

    private Tms282WildHunter() {
    }

    public static boolean active() {
        return Protocol.active().version() == ProtocolVersion.TMS_282_3;
    }

    /**
     * The final attack a 282 crossbow attack asks for: 終極攻擊VI when learned, else 進階終極攻擊 when learned, else
     * 終極攻擊 ({@code 0} = keep {@code faSkill}).
     */
    public static int finalAttackSkill(int faSkill, IntPredicate hasSkill) {
        if (faSkill != FINAL_ATTACK) {
            return 0;
        }
        if (hasSkill.test(FINAL_ATTACK_VI)) {
            return FINAL_ATTACK_VI;
        }
        if (hasSkill.test(ADVANCED_FINAL_ATTACK)) {
            return ADVANCED_FINAL_ATTACK;
        }
        return 0;
    }

    /**
     * The jaguar mount index for 美洲豹騎乘. 282 has no 美洲豹捕捉 (30001061) and every skill needs the ride, so an unset
     * 265 choice (-1) falls back to the first mount instead of refusing the ride.
     */
    public static int mountIndex(WildHunterInfo info, int mounts) {
        int idx = info == null ? -1 : info.getIdx();
        return idx >= 0 && idx < mounts ? idx : 0;
    }

    /** 滑流 heal: {@code percent}% of max HP, at least 1 when the skill gives anything. */
    public static int glideHeal(int maxHp, int percent) {
        if (maxHp <= 0 || percent <= 0) {
            return 0;
        }
        return Math.max(1, (int) ((long) maxHp * percent / 100));
    }
}
