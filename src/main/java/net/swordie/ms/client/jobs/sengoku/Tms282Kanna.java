package net.swordie.ms.client.jobs.sengoku;

import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;

import java.util.Set;

/**
 * TMS 282.3 Kanna (陰陽師 4200/4210/4211/4212, skill audit research/tms2823/kanna-wh-282/README.md).
 * <p>
 * 282 reworked the job around three shikigami (鬼 / 天狗 / 大蛇, {@code ShikigamiType} in Skill.wz) and 花狐; most 265
 * {@link Kanna} constants now name other skills (e.g. 42111003 265 式神炎舞 summon = 282 魂之葉舞 jump,
 * 42101002 265 小白 transform = 282 [式令]車輪擊, 42121002 265 party heal = 282 碎心符). On 282 {@link Kanna} runs none of
 * its 265 skill / attack / mob-debuff logic and only the pieces below, which need nothing but server logic and CTS
 * rows that {@code data/protocol/tms282.3/cts-local.tsv} supports. Shikigami summons, 感應 / 黑雨 / 狂亂 / 猛毒 stacks,
 * 羈絆的波動 and 真名解放 need client RE (see the README); nothing here guesses a packet layout.
 */
public final class Tms282Kanna {
    // 282 ids (282 Skill.wz / String.wz)
    /** 紫扇傳授 (invisible, Lv 1..3): after {@code x} attack skills the next one gives {@code indieDamR} for {@code time} s. */
    public static final int PURPLE_FAN_TRANSMISSION = 40020002;
    /** 結界：咒靈護身 (type 10): {@code time} s invincible (265: 櫻花結界 affected area). */
    public static final int SPIRIT_WARD_BARRIER = 42111004;
    /** 曉之意志 (Hero's Will), same meaning as 265. */
    public static final int DAWN_WILL = 42121007;
    /** 曉之庇護 (indieDamR + time): the CP04B4 buff resolver already sends it. */
    public static final int DAWN_PROTECTION = 42121053;
    /** 紋櫻公主的祝福 (indiePMdR + time on 282; 265 read {@code q}, which 282 lacks, so 265 sent value 0). */
    public static final int PRINCESS_BLESSING = 400001057;

    /**
     * 265 {@link Kanna} constants whose id is another skill on 282 (or does not exist in 282 Skill.wz). Only
     * documentation / checks use this; {@link Kanna} skips all 265 logic on 282.
     */
    public static final Set<Integer> CHANGED_265_IDS = Set.of(
            Kanna.SHIKIGAMI_HAUNTING_1_1,        // 42001000 282 破魂符-千- (no slow / burn)
            Kanna.SHIKIGAMI_HAUNTING_1_2,        // 42001005 282 飛身下擊 (鬼)
            Kanna.MANA_WARP,                     // 42001002 282 符越 (no stun)
            Kanna.RADIANT_PEACOCK,               // 42101003 282 [式令]車輪擊 hidden attack (265 Booster)
            Kanna.HAKU_REBORN,                   // 42101002 282 [式令]車輪擊 command (265 ChangeFoxMan)
            Kanna.SHIKIGAMI_ACTIVATION_SKILL,    // 42110013 282 穩固的信賴 passive (265 burn on haunting)
            Kanna.BLOSSOM_BARRIER,               // 42111004 282 invincibility (265 affected area)
            Kanna.KISHIN_SHOUKAN,                // 42111003 282 魂之葉舞 jump (265 式神炎舞 summon)
            Kanna.BELLFLOWER_BARRIER,            // 42121005 282 撕裂斬 (大蛇 attack)
            Kanna.BELLFLOWER_BARRIER_PERSIST_H,  // 42120049 282 處刑-強化傷害
            Kanna.BINDING_TEMPEST,               // 42121004 282 猛毒之舞 (265 Stun with time 0)
            Kanna.FALLING_SAKURA,                // 42121002 282 碎心符 (265 party heal + mana vein)
            Kanna.FALLING_SAKURA_VITALITY_H,     // 42120048 282 影羽穿殺-BOSS殺手 (265 +20% heal)
            Kanna.VERITABLE_PANDEMONIUM,         // 42121052 282 [式令]鬼魂粉碎 (265 Stun)
            Kanna.BLACKHEARTED_CURSE,            // 42121054 282 真名解放：真顯 (265 AntiEvilShield, no 282 row)
            Kanna.YUKI_MUSUME_SHOUKAN,           // 400021017 282 十尾妖狐的羈絆 (265 summon 400021018 = 282 attack id)
            Kanna.SPIRITS_DOMAIN,                // 400021054 282 部分解放：燃炎光刃 type 1 (265 summon + AA)
            Kanna.LIBERATED_SPIRIT_CIRCLE_SUMMON,// 400021079 282 黑翼破天 hidden attack (265 summons)
            Kanna.HEXA_KISHIN_SHOUKAN,           // 42141011 282 [式令]鬼魂粉碎VI hidden attack
            Kanna.HEXA_YOSUZUME,                 // 42141007 282 [式令]車輪擊VI command
            Kanna.HEXA_TENGU_STRIKE,             // 42141012 282 [式令]鬼魂粉碎VI resonance
            Kanna.HEXA_TENGU_STRIKE_SUMMON_L,    // 42141013 282 召喚：天狗VI
            Kanna.HEXA_TENGU_STRIKE_SUMMON_R,    // 42141014 282 貫通射擊VI
            Kanna.HEXA_SHIKIGAMI_DOPPLEGANGER,   // 42141005 282 飛身下擊VI (265 KannaSiksinAutoAttack charge)
            Kanna.HEXA_SHIKIGAMI_DOPPLEGANGER_ACTIVATED, // 42141006 282 鬼暴陣VI
            Kanna.HEXA_NINE_TAILED_FURY,         // 42141009 282 [式令]車輪擊VI resonance (265 IndieDamR)
            Kanna.HEXA_NINE_TAILED_FURY_PASSIVE, // 42141010 282 [式令]鬼魂粉碎VI command
            Kanna.HEXA_GHOST_YAKSHA_BOSS,        // 42141015 282 [式令]影羽穿殺VI (265 鬼夜叉 summon)
            Kanna.MANA_FLOW,                     // 40020001 282 奇妙之手 mastery (265 50 MP / 6 s timer)
            Kanna.PRINCESS_SAKUNO_BLESSING       // 400001057 282 indiePMdR (265 q -> 0)
    );

    private Tms282Kanna() {
    }

    public static boolean active() {
        return Protocol.active().version() == ProtocolVersion.TMS_282_3;
    }

    /**
     * MP a 282 Kanna attack costs. 265 {@code Char.applyMpCon} charges Kanna {@code epCon}, which no 282 Kanna skill
     * has, so the attack path charged 0; 282 skills use {@code mpCon} ({@code mpconReduce} % applied like
     * {@code applyMpCon}).
     */
    public static int attackMpCost(SkillInfo si, int slv, int mpconReducePercent) {
        if (si == null || slv <= 0) {
            return 0;
        }
        int mpCon = Math.max(0, si.getValue(SkillStat.mpCon, slv));
        mpCon -= (int) (mpCon * (Math.max(0, mpconReducePercent) / 100D));
        return Math.max(0, mpCon);
    }

    /** Whether an attack counts for 紫扇傳授: a skill attack the player casts (hidden shikigami attacks do not). */
    public static boolean countsAsAttackSkill(int skillId, SkillInfo si) {
        return skillId != 0 && si != null && !si.isInvisible();
    }

    /**
     * 紫扇傳授 counter: {@code x} attack skills make it ready; the next attack skill applies the buff and restarts the
     * count. One per Kanna job handler (not saved: 282 has no gauge CTS for it).
     */
    public static final class FanTransmission {
        private int count;
        private boolean ready;

        /** Returns true when this attack skill should get the 紫扇傳授 buff. */
        public synchronized boolean onAttackSkill(int needed) {
            if (needed <= 0) {
                return false;
            }
            if (ready) {
                ready = false;
                count = 0;
                return true;
            }
            if (++count >= needed) {
                ready = true;
            }
            return false;
        }

        public synchronized int count() {
            return count;
        }

        public synchronized boolean ready() {
            return ready;
        }
    }
}
