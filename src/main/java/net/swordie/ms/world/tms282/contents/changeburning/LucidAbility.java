package net.swordie.ms.world.tms282.contents.changeburning;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.ProtocolVersion;

import java.util.ArrayList;
import java.util.List;

/**
 * 變身燃燒：露希妲 — Lucid ability values (bossmech1-0930; research/tms2823/lucid-re-0930/ability/README.md §1, §2).
 * Static RE on the 282.3 client (layouts identical to 282.4); not live-tested.
 *
 * <h3>StatBlock (LP02CC, reader 1015020, 20 x i32) 【證實】</h3>
 * Field order = the names 100FCE0 reads from a WZ node (and the #RPStatByLevel...S&lt;name&gt;# resolver 1B50886..1B50AC4):
 * maxHp, maxHpR, maxMp, atk, atkR, atkSpd, def, hpRecovery, mpRecovery, cri, criDamR, moveSpd, jump, stance, fs,
 * maxExp, mastery, reduceCoolTime, atkMinRange, atkMaxRange.
 *
 * <h3>RPEventStat explicit ints (CTS bit 978, 14FC274, 19 x i32) 【證實】</h3>
 * explicit[k] -> getter index (129EC10) -> meaning (debug viewer 4607010 labels, combat use):
 * <pre>
 *  0 idx2  LEVEL        level-difference penalty vs mob level (7B7BA4, only when r != 80004200)
 *  1 idx3  EXP          viewer only
 *  2 idx4  MAX_HP       11A81D6 (HP recovery base), 57ABD9B (damage taken)
 *  3 idx6  MAX_MP       viewer only (Lucid skills have no MP cost)
 *  4 idx7  ATK          damage multiplier; C8A870 returns ATK x 100 while r == 80004200
 *  5 idx9  ATK_SPD      replaces the attack speed of RP skills (11569F1)
 *  6 idx10 DEF          57ABDC1 (damage taken; viewer clamps 0..100)
 *  7 idx11 HP_RECOVERY  viewer label
 *  8 idx12 MP_RECOVERY  viewer label
 *  9 idx13 CRI          critical rate (7B4B26)
 * 10 idx14 CRI_DAMR     critical damage (7B75B6)
 * 11 idx15 MOVE_SPD     movement (13EF942, 54DAB27)
 * 12 idx16 JUMP         movement (13EF951)
 * 13 idx17 STANCE       knock-back resistance %, rand%100 &lt; STANCE (576B064)
 * 14 idx18 FS           movement double (13EF961); name from StatBlock field 14 "fs" (推定)
 * 15 idx19 MASTERY      min damage = minR + MASTERY*(1-minR) (7B751C)
 * 16 idx20 REDUCE_COOL  seconds, x1000 ms off RP skill cooldowns, 10 s / 5 s rule (116F666)
 * 17 idx21 ATK_MIN_RANGE base min damage % (7B74E8)
 * 18 idx22 ATK_MAX_RANGE max damage %; max = max(this/100, min+0.01) (7B7502)
 * </pre>
 * idx5 (+0x7814) and idx8 (+0x7838) = StatBlock maxHpR / atkR positions; not in the packet: the server sends final
 * (percent-applied) values. All zero (the server before bossmech1) => Lucid skills hit for ~1% of 0 ATK.
 *
 * <h3>The level -> ability table is 自訂／推定 (custom)</h3>
 * The official RPStatByLevel T80004228 table is sent by the official server and is NOT in the client (no
 * Etc/RPEventSystemSvr.img). {@link #base(int)} is the table PROPOSED in ability/README §2.3 — every value there is a
 * 自訂／推定 guess, to be replaced when official values are known.
 * <p>
 * 夢幻的裝備／結晶 (LP02C8 / CP02D3, ability/patch/DreamEquipment*) is NOT part of bossmech1: the equipment part of
 * {@link #finalBlock} is all zero until that patch lands (next step).
 */
public final class LucidAbility {
    /** rpStatViewer info/jobCode and the LP02CC stat table key (#RPStatByLevelT80004228...#). */
    public static final int JOB_CODE = 80004228;
    public static final int STAT_BLOCK_INTS = 20;
    public static final int RP_EVENT_STAT_INTS = 19;
    /** The 9 EventItem stats (685C40 order: maxHP, maxHPR, atk, atkR, cri, criDamR, speed, mastery, reduceCoolTime). */
    public static final int EQUIP_STATS = 9;

    // StatBlock field indexes (1015020 / 100FCE0)
    public static final int MAX_HP = 0, MAX_HP_R = 1, MAX_MP = 2, ATK = 3, ATK_R = 4, ATK_SPD = 5, DEF = 6,
            HP_RECOVERY = 7, MP_RECOVERY = 8, CRI = 9, CRI_DAM_R = 10, MOVE_SPD = 11, JUMP = 12, STANCE = 13, FS = 14,
            MAX_EXP = 15, MASTERY = 16, REDUCE_COOL_TIME = 17, ATK_MIN_RANGE = 18, ATK_MAX_RANGE = 19;

    /** StatBlock index of each explicit int 2..18 (explicit 0 = LEVEL, 1 = EXP are not StatBlock fields). */
    static final int[] EXPLICIT_FROM_BLOCK = {
            -1, -1, MAX_HP, MAX_MP, ATK, ATK_SPD, DEF, HP_RECOVERY, MP_RECOVERY, CRI, CRI_DAM_R, MOVE_SPD, JUMP, STANCE,
            FS, MASTERY, REDUCE_COOL_TIME, ATK_MIN_RANGE, ATK_MAX_RANGE,
    };

    /**
     * LEVEL policy. In 80004228 mode the client compares explicit LEVEL with the mob level (7B7B99): the nightmare mobs
     * 9834752..54 are Lv200, so Lucid Lv1..100 always takes the 10+ row of 81E74A0 = 50 %. The official value is
     * unknown (猜測); {@code -Dtms282.lucid.levelPlus200=true} sends {@code lucidLevel + 200} instead. 80004200 (the
     * mode the server uses now) ignores LEVEL (own-level formula 7F90E0).
     */
    public static volatile boolean LEVEL_PLUS_200 = Boolean.getBoolean("tms282.lucid.levelPlus200");

    private LucidAbility() {
    }

    /**
     * 自訂／推定 base StatBlock of Lucid level {@code lv} (1..100) — ability/README §2.3 proposal, NOT official values.
     */
    public static int[] base(int lv) {
        lv = Math.max(1, Math.min(ChangeBurning.MAX_LUCID_LEVEL, lv));
        int[] b = new int[STAT_BLOCK_INTS];
        b[MAX_HP] = 3000 + 300 * (lv - 1);       // 自訂: Lv100 32,700 (legend +12 pendant is 22,350)
        b[MAX_MP] = 100;                          // 自訂: unused (no MP cost on 800042xx)
        b[ATK] = 50 + 10 * (lv - 1);              // 自訂: Lv100 1,040 (legend +12 weapon is 1,845)
        b[ATK_SPD] = 4;                           // 自訂: skill attack-speed stage
        b[CRI] = 5 + (lv - 1) / 10;               // 自訂: Lv100 14 (legend +12 ring is 38)
        b[CRI_DAM_R] = 20 + (lv - 1) / 5;         // 自訂: Lv100 39
        b[MOVE_SPD] = 100;                        // 自訂
        b[JUMP] = 100;                            // 自訂
        b[STANCE] = 100;                          // 自訂: Lucid floats, never knocked back
        b[MASTERY] = 15 + (lv - 1) / 5;           // 自訂: Lv100 34 (gems up to +16 each, cap 100)
        b[ATK_MIN_RANGE] = 0;                     // 自訂: min damage = mastery %
        b[ATK_MAX_RANGE] = 100;                   // 自訂: max damage = 100 %
        return b;
    }

    /** Level -> StatBlock rows for LP02CC (step table: the client uses the greatest level <= lv, 1820BE0). 自訂 values. */
    public static List<int[]> levelTable() {
        List<int[]> rows = new ArrayList<>();
        for (int lv = 1; lv <= ChangeBurning.MAX_LUCID_LEVEL; lv++) {
            int[] row = new int[1 + STAT_BLOCK_INTS];
            row[0] = lv;
            System.arraycopy(base(lv), 0, row, 1, STAT_BLOCK_INTS);
            rows.add(row);
        }
        return rows;
    }

    /**
     * Final StatBlock: tooltip rule of UI rpStatViewer ("基本數值總和與 % 數相乘", 推定): maxHp / atk = (base + equip) x
     * (100 + R) / 100; cri / mastery capped at 100; reduceCoolTime capped at 9 s ("最多可減少9秒").
     *
     * @param equip the {@link #EQUIP_STATS} EventItem stats (all zero until 夢幻的裝備 is implemented)
     */
    public static int[] finalBlock(int[] base, int[] equip) {
        int[] f = base.clone();
        f[MAX_HP] = (int) Math.min(Integer.MAX_VALUE,
                (long) (base[MAX_HP] + equip[0]) * (100 + base[MAX_HP_R] + equip[1]) / 100);
        f[ATK] = (int) Math.min(Integer.MAX_VALUE,
                (long) (base[ATK] + equip[2]) * (100 + base[ATK_R] + equip[3]) / 100);
        f[MAX_HP_R] = 0;
        f[ATK_R] = 0;
        f[CRI] = Math.min(100, base[CRI] + equip[4]);
        f[CRI_DAM_R] = base[CRI_DAM_R] + equip[5];
        f[MOVE_SPD] = base[MOVE_SPD] + equip[6];
        f[MASTERY] = Math.min(100, base[MASTERY] + equip[7]);
        f[REDUCE_COOL_TIME] = Math.min(9, base[REDUCE_COOL_TIME] + equip[8]);
        return f;
    }

    /** The 19 explicit ints of RPEventStat for this Lucid. */
    public static int[] rpEventStatInts(int lucidLevel, long exp, int[] finalBlock) {
        int[] v = new int[RP_EVENT_STAT_INTS];
        v[0] = LEVEL_PLUS_200 ? lucidLevel + 200 : lucidLevel;
        v[1] = (int) Math.max(0, Math.min(Integer.MAX_VALUE, exp));
        for (int k = 2; k < RP_EVENT_STAT_INTS; k++) {
            v[k] = finalBlock[EXPLICIT_FROM_BLOCK[k]];
        }
        return v;
    }

    /** From the character: Lucid level / exp (QR 600384, {@link LucidDream}); no 夢幻的裝備 yet (equip = 0). */
    public static int[] rpEventStatInts(net.swordie.ms.client.character.Char chr) {
        int lv = ChangeBurning.lucidLevel(chr);
        long exp = LucidDream.exp(chr);
        int[] equip;
        try {
            equip = net.swordie.ms.world.tms282.contents.changeburning.dream.DreamService.get().equippedStats(chr); // content-1005 夢幻的裝備
        } catch (RuntimeException | LinkageError e) {
            equip = new int[EQUIP_STATS];
        }
        return rpEventStatInts(lv, exp, finalBlock(base(lv), equip));
    }

    /**
     * LP02CC with the stat table 80004228 (level -> StatBlock, {@link #levelTable()} 自訂 values) and the exp table 4
     * (traced CLEAN: ability/out/lp02cc-proposed.bin and java-lp02cc.bin, trace_maps.py --seed-map 9E40EF8 9E40F08).
     * The other stat-table parts are sent empty: nA / nB {i32,i32} (unknown), two StatBlocks after nA and two at the
     * end (推定 Min / Max), nC = the Avatar override table (1014720 -> 1010630, 推定; Lucid look = later work).
     * Replaces {@link ChangeBurningPackets#expTable} (same exp part, plus one stat table).
     */
    public static OutPacket rpTables(List<int[]> levelStatRows, List<int[]> levelDemandExp) {
        OutPacket out = OutPacket.forProfile(ProtocolVersion.TMS_282_3, ChangeBurningPackets.RP_TABLES);
        out.encodeInt(1);                         // nStatTables
        out.encodeInt(JOB_CODE);                  // key 80004228
        out.encodeString("Lucid");                // name (ASCII; 推定 characterName)
        out.encodeInt(levelStatRows.size());      // 1015DE0: level -> StatBlock
        for (int[] row : levelStatRows) {
            out.encodeInt(row[0]);
            for (int k = 1; k <= STAT_BLOCK_INTS; k++) {
                out.encodeInt(row[k]);
            }
        }
        out.encodeInt(0);                         // nA
        zeroBlock(out);
        zeroBlock(out);
        out.encodeInt(0);                         // nB
        out.encodeInt(0);                         // nC (Avatar)
        zeroBlock(out);
        zeroBlock(out);
        out.encodeInt(1);                         // nExpTables
        out.encodeInt(ChangeBurningPackets.EXP_TABLE_KEY);
        out.encodeInt(0);                         // n1 (i32, i32, str)
        out.encodeInt(levelDemandExp.size());
        for (int[] row : levelDemandExp) {
            out.encodeInt(row[0]);
            out.encodeInt(row[1]);
        }
        out.encodeInt(0);                         // tail
        return out;
    }

    private static void zeroBlock(OutPacket out) {
        for (int k = 0; k < STAT_BLOCK_INTS; k++) {
            out.encodeInt(0);
        }
    }
}
