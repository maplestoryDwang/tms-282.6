package net.swordie.ms.world.tms282.contents.changeburning;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.life.mob.Mob;

import java.util.ArrayList;
import java.util.Map;

/**
 * 變身燃燒：露希妲 progression (research/tms2823/lucid-program-0930, item 3/6): Lucid level / exp (QR 600384
 * {@code lv}, {@code exp} = progress inside the level, demand {@link ChangeBurning#demandExp}), 夢境通行證 (QR 102930
 * {@code passCnt}), the per-kill counters of QR 102883 and the LucidBoss missions.
 * <ul>
 *     <li>{@link #addExp}: the one gain path (nightmare kills below, mission / forest scripts via Jython
 *     {@code LucidDream.addExp(chr, n)}). Level ups re-grant the Lucid skills of the new level
 *     ({@link ChangeBurning#grantLucidSkills}); the client gets the QR update and the LP02C7 sub 3 exp effect
 *     (19BB570, static). The main character gets {@link #MAIN_EXP_PER_LUCID_EXP} of a level per Lucid exp
 *     (policy: 10,000,000 Lucid exp = one character level; official ratio unknown).</li>
 *     <li>{@link #onMobKilled} ({@code Job.handleMobKilled}, 282 only): only nightmare mobs give Lucid exp and only
 *     while transformed (official: 露希妲打一般怪物沒有經驗); 食夢者 +1 {@code eaterKillCnt}, 夢幻的根源 +1
 *     {@code bossCnt}. The LucidBoss bosses complete their mission quest (102931..102934, 102961) for every
 *     designated, transformed character in the field (UI LucidBoss: queststate 2 shows the clear).</li>
 * </ul>
 * Per-kill amounts are policy (the official values are not in the client): override with the system properties
 * {@code tms282.lucid.exp.<mobId>}.
 */
public final class LucidDream {
    public static final int QR_LUCID = ChangeBurning.QR_LUCID;          // 600384 lv, exp
    public static final int QR_PASS = ChangeBurning.QR_PASS;            // 102930 passCnt, ui
    public static final int QR_COUNTS = 102883;                         // eaterMaxCnt, eaterKillCnt, bossCnt
    public static final int QR_EQUIP = 102882;                          // epic/unique/legend/gem*
    public static final int QR_TICKET = 102959;                         // curTicket, burnTicket, wUse, totalUse
    public static final int QR_SHOP = 600314;                           // point, r1..r25
    public static final int QR_SPECIAL = 102960;                        // r1..r19
    public static final int QR_BOSS = 102963;                           // r00..r40 (LucidBoss rewards)
    public static final int QR_BOSS_2 = 600379;                         // r00..r40 (LucidBoss rewards)
    /** Season mission 「達成露希妲70級」 (infoNumber 600384). */
    public static final int QUEST_LUCID_70 = 102941;

    public static final int DREAM_EATER = 9834752;   // 食夢者
    public static final int DREAM_ROOT = 9834753;    // 夢幻的根源
    public static final int NIGHTMARE_DOG = 9834754; // 惡夢的走狗

    /** Main-character exp per Lucid exp, as a fraction of the character's exp to the next level. */
    public static final double MAIN_EXP_PER_LUCID_EXP = 1.0 / 10_000_000;

    /** LucidBoss missions: boss template -> quest (UI LucidBoss boss1..boss5 queststate 2). */
    static final Map<Integer, Integer> BOSS_QUESTS = Map.of(
            8800102, 102931,   // 殘暴炎魔(混沌) final body
            8930000, 102932,   // 貝倫(混沌)
            8880153, 102933,   // 露希妲(困難) last phase (BossHelper: Hard Lucid)
            9834755, 102934,   // 赫麗娜(普通) (推定: String 赫麗娜 Lv75)
            9834770, 102961);  // 赫麗娜(夢魘) (推定: String 赫麗娜 Lv100)

    private LucidDream() {
    }

    private static boolean tms282() {
        return Protocol.active().version() == ProtocolVersion.TMS_282_3;
    }

    /** Lucid exp for one kill of {@code mobId} while transformed (0 = none). */
    public static long killExp(int mobId) {
        long def = switch (mobId) {
            case DREAM_EATER -> 40;
            case NIGHTMARE_DOG -> 25;
            case DREAM_ROOT -> 400;
            default -> 0;
        };
        if (def == 0) {
            return 0;
        }
        try {
            return Long.parseLong(System.getProperty("tms282.lucid.exp." + mobId, String.valueOf(def)).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static int level(Char chr) {
        return ChangeBurning.lucidLevel(chr);
    }

    public static long exp(Char chr) {
        return parseLong(chr.getQuestById(QR_LUCID) == null ? null : chr.getQRValueByKey(QR_LUCID, "exp"));
    }

    static long parseLong(String v) {
        try {
            return v == null ? 0 : Math.max(0, Long.parseLong(v.trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Result of applying {@code gain} to (lv, exp): the new level and exp. Pure, for checks. */
    static long[] apply(int lv, long exp, long gain) {
        lv = Math.max(1, Math.min(ChangeBurning.MAX_LUCID_LEVEL, lv));
        exp = Math.max(0, exp) + Math.max(0, gain);
        while (lv < ChangeBurning.MAX_LUCID_LEVEL && exp >= ChangeBurning.demandExp(lv)) {
            exp -= ChangeBurning.demandExp(lv);
            lv++;
        }
        if (lv >= ChangeBurning.MAX_LUCID_LEVEL) {
            exp = 0; // max level: the gauge stays empty
        }
        return new long[]{lv, exp};
    }

    /**
     * Adds Lucid exp to the designated 變身燃燒 character; returns the number of levels gained (0 also when nothing
     * was added: not designated, event over, max level).
     */
    public static int addExp(Char chr, long amount) {
        if (chr == null || amount <= 0 || !tms282()) {
            return 0;
        }
        ChangeBurning cb = ChangeBurning.get();
        if (!cb.eventActive() || !cb.isDesignated(chr)) {
            return 0;
        }
        int lv = level(chr);
        if (lv >= ChangeBurning.MAX_LUCID_LEVEL) {
            return 0;
        }
        long[] r = apply(lv, exp(chr), amount);
        int newLv = (int) r[0];
        if (chr.getQuestById(QR_LUCID) == null) {
            chr.createQuestWithQRValue(QR_LUCID, "lv=" + newLv + ";exp=" + r[1]);
        } else {
            chr.setQRValueByKey(QR_LUCID, "lv", String.valueOf(newLv));
            chr.setQRValueByKey(QR_LUCID, "exp", String.valueOf(r[1]));
        }
        if (chr.getClient() != null) {
            chr.getClient().write(ChangeBurningPackets.expGain(amount));
        }
        giveMainExp(chr, amount);
        int gained = newLv - lv;
        if (gained > 0) {
            onLevelUp(chr, lv, newLv);
        }
        return gained;
    }

    private static void giveMainExp(Char chr, long lucidExp) {
        int level = chr.getLevel();
        if (level <= 0 || level >= GameConstants.charExp.length) {
            return;
        }
        long need = GameConstants.charExp[level];
        long main = (long) Math.floor(need * MAIN_EXP_PER_LUCID_EXP * lucidExp);
        if (main > 0) {
            try {
                chr.addExp(main, false);
            } catch (RuntimeException e) {
                System.out.println("[LucidDream] main exp failed for character " + chr.getId() + ": " + e);
            }
        }
    }

    private static void onLevelUp(Char chr, int from, int to) {
        try {
            ChangeBurning.grantLucidSkills(chr);
        } catch (RuntimeException e) {
            System.out.println("[LucidDream] Lucid skills failed for character " + chr.getId() + ": " + e);
        }
        try {
            ChangeBurning.get().refreshAbility(chr); // bossmech1-0930: RPEventStat ints of the new level
        } catch (RuntimeException e) {
            System.out.println("[LucidDream] Lucid ability refresh failed for character " + chr.getId() + ": " + e);
        }
        chr.chatMessage("[變身燃燒] 露希妲等級提升到 Lv." + to + "！");
        if (to >= 70 && from < 70 && !chr.hasQuestCompleted(QUEST_LUCID_70)) {
            chr.completeQuest(QUEST_LUCID_70);
        }
        System.out.printf("[LucidDream] character %d Lucid Lv.%d -> Lv.%d%n", chr.getId(), from, to);
    }

    /** 夢境通行證: QR 102930 passCnt += n (the UI shows passCnt x 10,000). Returns the new count. */
    public static long addPass(Char chr, int n) {
        if (chr == null || n == 0) {
            return 0;
        }
        long cnt = Math.max(0, (chr.getQuestById(QR_PASS) == null ? 0 : parseLong(chr.getQRValueByKey(QR_PASS, "passCnt"))) + n);
        if (chr.getQuestById(QR_PASS) == null) {
            chr.createQuestWithQRValue(QR_PASS, "passCnt=" + cnt + ";ui=max");
        } else {
            chr.setQRValueByKey(QR_PASS, "passCnt", String.valueOf(cnt));
        }
        return cnt;
    }

    /** Adds {@code n} to the QR {@code questId} counter {@code key} (created when missing); returns the new value. */
    public static long addCount(Char chr, int questId, String key, long n) {
        long v = Math.max(0, (chr.getQuestById(questId) == null ? 0 : parseLong(chr.getQRValueByKey(questId, key))) + n);
        if (chr.getQuestById(questId) == null) {
            chr.createQuestWithQRValue(questId, key + "=" + v);
        } else {
            chr.setQRValueByKey(questId, key, String.valueOf(v));
        }
        return v;
    }

    /** {@code Job.handleMobKilled} (282): nightmare kill exp / counters and the LucidBoss missions. */
    public static void onMobKilled(Char chr, Mob mob) {
        if (chr == null || mob == null || !tms282()) {
            return;
        }
        try {
            mobKilled(chr, mob);
        } catch (RuntimeException e) {
            System.out.println("[LucidDream] kill hook failed for character " + chr.getId() + ": " + e);
        }
    }

    private static void mobKilled(Char chr, Mob mob) {
        ChangeBurning cb = ChangeBurning.get();
        if (!cb.eventActive()) {
            return;
        }
        int id = mob.getTemplateId();
        Integer bossQuest = BOSS_QUESTS.get(id);
        if (bossQuest != null) {
            java.util.List<Char> chars = mob.getField() == null ? java.util.List.of(chr) : new ArrayList<>(mob.getField().getChars());
            for (Char c : chars) {
                try {
                    if (cb.isDesignated(c) && cb.isTransformed(c) && !c.hasQuestCompleted(bossQuest)) {
                        c.completeQuest(bossQuest);
                        c.chatMessage("[變身燃燒] 已用露希妲完成首領任務！");
                        System.out.printf("[LucidDream] character %d LucidBoss quest %d (mob %d)%n", c.getId(), bossQuest, id);
                    }
                } catch (RuntimeException e) {
                    System.out.println("[LucidDream] boss quest failed for character " + c.getId() + ": " + e);
                }
            }
            return;
        }
        long exp = killExp(id);
        if (exp <= 0 || !cb.isDesignated(chr) || !cb.isTransformed(chr)) {
            return;
        }
        if (id == DREAM_EATER) {
            addCount(chr, QR_COUNTS, "eaterKillCnt", 1);
        } else if (id == DREAM_ROOT) {
            addCount(chr, QR_COUNTS, "bossCnt", 1);
        }
        addExp(chr, exp);
    }
}
