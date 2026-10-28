package net.swordie.ms.enums;

public enum BossPartyType {

    BALROG(0, "Balrog", 65, BossPartyDifficultyType.Easy, 0, BossPartyEnterCountType.SevenTimesADay),

    ZAKUM_EASY(1, "Zakum", 50, BossPartyDifficultyType.Easy, 0, BossPartyEnterCountType.OnceADay),
    ZAKUM_NORMAL(1, "Zakum", 90, BossPartyDifficultyType.Normal, 0, BossPartyEnterCountType.OnceADay),
    ZAKUM_CHAOS(1, "Zakum", 90, BossPartyDifficultyType.Chaos, 0, BossPartyEnterCountType.OnceInSevendays),

    HORNTAIL_EASY(2, "Horntail", 130, BossPartyDifficultyType.Easy, 0, BossPartyEnterCountType.OnceADay),
    HORNTAIL_NORMAL(2, "Horntail", 130, BossPartyDifficultyType.Normal, 0, BossPartyEnterCountType.OnceADay),
    HORNTAIL_CHAOS(2, "Horntail", 135, BossPartyDifficultyType.Chaos, 0, BossPartyEnterCountType.OnceADay),

    HILLA_NORMAL(3, "Hilla", 85, BossPartyDifficultyType.Normal, 0, BossPartyEnterCountType.OnceADay),
    HILLA_HARD(3, "Hilla", 170, BossPartyDifficultyType.Hard, 0, BossPartyEnterCountType.OnceInSevendays),

    PIERRE_NORMAL(4, "Pierre", 125, BossPartyDifficultyType.Normal, 30007, BossPartyEnterCountType.OnceADay),
    PIERRE_CHAOS(4, "Pierre", 180, BossPartyDifficultyType.Chaos, 30007, BossPartyEnterCountType.OnceInSevendays),

    VON_BON_NORMAL(5, "Von Bon", 125, BossPartyDifficultyType.Normal, 30007, BossPartyEnterCountType.OnceADay),
    VON_BON_CHAOS(5, "Von Bon", 180, BossPartyDifficultyType.Chaos, 30007, BossPartyEnterCountType.OnceInSevendays),

    QUEEN_NORMAL(6, "Crimson Queen", 125, BossPartyDifficultyType.Normal, 30007, BossPartyEnterCountType.OnceADay),
    QUEEN_CHAOS(6, "Crimson Queen", 180, BossPartyDifficultyType.Chaos, 30007, BossPartyEnterCountType.OnceInSevendays),

    VELLUM_NORMAL(7, "Vellum", 125, BossPartyDifficultyType.Normal, 30007, BossPartyEnterCountType.OnceADay),
    VELLUM_CHAOS(7, "Vellum", 180, BossPartyDifficultyType.Chaos, 30007, BossPartyEnterCountType.OnceInSevendays),

    VON_LEON_EASY(8, "Von Leon", 125, BossPartyDifficultyType.Easy, 1562, BossPartyEnterCountType.OnceADay),
    VON_LEON_NORMAL(8, "Von Leon", 125, BossPartyDifficultyType.Normal, 1562, BossPartyEnterCountType.OnceADay),
    VON_LEON_HARD(8, "Von Leon", 125, BossPartyDifficultyType.Hard, 1562, BossPartyEnterCountType.OnceADay),

    ARKARIUM_EASY(9, "Arkarium", 140, BossPartyDifficultyType.Easy, 31179, BossPartyEnterCountType.OnceADay),
    ARKARIUM_NORMAL(9, "Arkarium", 140, BossPartyDifficultyType.Normal, 31179, BossPartyEnterCountType.OnceADay),

    MAGNUS_EASY(10, "Magnus", 115, BossPartyDifficultyType.Easy, 31833, BossPartyEnterCountType.OnceADay),
    MAGNUS_NORMAL(10, "Magnus", 155, BossPartyDifficultyType.Normal, 31833, BossPartyEnterCountType.OnceADay),
    MAGNUS_HARD(10, "Magnus", 175, BossPartyDifficultyType.Hard, 31833, BossPartyEnterCountType.OnceInSevendays),

    PINK_BEAN_NORMAL(11, "Pink Bean", 140, BossPartyDifficultyType.Normal, 3521, BossPartyEnterCountType.OnceADay),
    PINK_BEAN_CHAOS(11, "Pink Bean", 170, BossPartyDifficultyType.Chaos, 3521, BossPartyEnterCountType.OnceInSevendays),

    CYGNUS_EASY(12, "Cygnus", 165, BossPartyDifficultyType.Easy, 31152, BossPartyEnterCountType.OnceInSevendays),
    CYGNUS_NORMAL(12, "Cygnus", 165, BossPartyDifficultyType.Normal, 31152, BossPartyEnterCountType.OnceInSevendays),

    LOTUS_NORMAL(13, "Lotus", 190, BossPartyDifficultyType.Normal, 0, BossPartyEnterCountType.OnceInSevendays),
    LOTUS_HARD(13, "Lotus", 190, BossPartyDifficultyType.Hard, 0, BossPartyEnterCountType.OnceInSevendays),
    LOTUS_EXTREME(13, "Lotus", 190, BossPartyDifficultyType.Extreme, 0, BossPartyEnterCountType.OnceInSevendays),

    URSUS(14, "Ursus", 100, BossPartyDifficultyType.Normal, 33565, BossPartyEnterCountType.ThreeTimesAday),

    DAMIEN_NORMAL(15, "Damien", 190, BossPartyDifficultyType.Normal, 0, BossPartyEnterCountType.OnceInSevendays),
    DAMIEN_HARD(15, "Damien", 190, BossPartyDifficultyType.Hard, 0, BossPartyEnterCountType.OnceInSevendays),

    GOLLUX(16, "Gollux", 180, BossPartyDifficultyType.Normal, 17523, BossPartyEnterCountType.OnceADay),

    RANMARU_NORMAL(17, "Ranmaru", 120, BossPartyDifficultyType.Normal, 65851, BossPartyEnterCountType.OnceADay),
    RANMARU_HARD(17, "Ranmaru", 180, BossPartyDifficultyType.Hard, 65851, BossPartyEnterCountType.OnceADay),

    PRINCESS_NO(18, "Princess No", 180, BossPartyDifficultyType.Normal, 0, BossPartyEnterCountType.OnceInSevendays),

    LUCID_EASY(19, "Lucid", 220, BossPartyDifficultyType.Easy, 34330, BossPartyEnterCountType.OnceInSevendays),
    LUCID_NORMAL(19, "Lucid", 220, BossPartyDifficultyType.Normal, 34330, BossPartyEnterCountType.OnceInSevendays),
    LUCID_HARD(19, "Lucid", 220, BossPartyDifficultyType.Hard, 34330, BossPartyEnterCountType.OnceInSevendays),

    OMNI_CLN_NORMAL(21, "OMNI-CLN", 180, BossPartyDifficultyType.Normal, 3496, BossPartyEnterCountType.OnceADay),

    PAPULATUS_EASY(22, "Papulatus", 115, BossPartyDifficultyType.Easy, 3470, BossPartyEnterCountType.OnceADay),
    PAPULATUS_NORMAL(22, "Papulatus", 155, BossPartyDifficultyType.Normal, 3470, BossPartyEnterCountType.OnceADay),
    PAPULATUS_CHAOS(22, "Papulatus", 190, BossPartyDifficultyType.Chaos, 3470, BossPartyEnterCountType.OnceInSevendays),

    WILL_EASY(23, "Will", 235, BossPartyDifficultyType.Easy, 37871, BossPartyEnterCountType.OnceInSevendays),
    WILL_NORMAL(23, "Will", 235, BossPartyDifficultyType.Normal, 37871, BossPartyEnterCountType.OnceInSevendays),
    WILL_HARD(23, "Will", 235, BossPartyDifficultyType.Hard, 37871, BossPartyEnterCountType.OnceInSevendays),

    VERUS_HILLA_NORMAL(24, "Verus Hilla", 250, BossPartyDifficultyType.Normal, 36772, BossPartyEnterCountType.OnceInSevendays),
    VERUS_HILLA_HARD(24, "Verus Hilla", 250, BossPartyDifficultyType.Hard, 36772, BossPartyEnterCountType.OnceInSevendays),

    BLACK_MAGE_HARD(25, "Black Mage", 255, BossPartyDifficultyType.Hard, 35815, BossPartyEnterCountType.OnceInAMonth),
    BLACK_MAGE_EXTREME(25, "Black Mage", 255, BossPartyDifficultyType.Extreme, 35815, BossPartyEnterCountType.OnceInAMonth),

    GLOOM_NORMAL(26, "Gloom", 245, BossPartyDifficultyType.Normal, 35632, BossPartyEnterCountType.OnceInSevendays),
    GLOOM_CHAOS(26, "Gloom", 245, BossPartyDifficultyType.Chaos, 35632, BossPartyEnterCountType.OnceInSevendays),

    DARKNELL_NORMAL(27, "Darknell", 255, BossPartyDifficultyType.Normal, 35815, BossPartyEnterCountType.OnceInSevendays),
    DARKNELL_HARD(27, "Darknell", 255, BossPartyDifficultyType.Hard, 35815, BossPartyEnterCountType.OnceInSevendays),

    CHOSEN_SEREN_NORMAL(28, "Chosen Seren", 260, BossPartyDifficultyType.Normal, 39921, BossPartyEnterCountType.OnceInSevendays),
    CHOSEN_SEREN_HARD(28, "Chosen Seren", 260, BossPartyDifficultyType.Hard, 39921, BossPartyEnterCountType.OnceInSevendays),
    CHOSEN_SEREN_EXTREME(28, "Chosen Seren", 260, BossPartyDifficultyType.Extreme, 39921, BossPartyEnterCountType.OnceInSevendays),

    SLIME_NORMAL(29, "Guardian Angel Slime", 210, BossPartyDifficultyType.Normal, 36013, BossPartyEnterCountType.OnceInSevendays),
    SLIME_HARD(29, "Guardian Angel Slime", 210, BossPartyDifficultyType.Hard, 36013, BossPartyEnterCountType.OnceInSevendays),

    KALOS_EASY(30, "Kalos the Guardian", 265, BossPartyDifficultyType.Easy, 38214, BossPartyEnterCountType.OnceInSevendays),
    KALOS_NORMAL(30, "Kalos the Guardian", 265, BossPartyDifficultyType.Normal, 38214, BossPartyEnterCountType.OnceInSevendays),
    KALOS_HARD(30, "Kalos the Guardian", 265, BossPartyDifficultyType.Hard, 38214, BossPartyEnterCountType.OnceInSevendays),
    KALOS_EXTREME(30, "Kalos the Guardian", 265, BossPartyDifficultyType.Extreme, 38214, BossPartyEnterCountType.OnceInSevendays),

    KALING_EASY(31, "Kaling", 275, BossPartyDifficultyType.Easy, 38401, BossPartyEnterCountType.OnceInSevendays),
    KALING_NORMAL(31, "Kaling", 275, BossPartyDifficultyType.Normal, 38401, BossPartyEnterCountType.OnceInSevendays),
    KALING_HARD(31, "Kaling", 275, BossPartyDifficultyType.Hard, 38401, BossPartyEnterCountType.OnceInSevendays),
    KALING_EXTREME(31, "Kaling", 275, BossPartyDifficultyType.Extreme, 38401, BossPartyEnterCountType.OnceInSevendays),

    MONSTER_PARK_EXTREME(32, "Monster Park Extreme", 260, BossPartyDifficultyType.Normal, 0, BossPartyEnterCountType.OnceInSevendays),

    LIMBO_NORMAL(33, "Limbo", 285, BossPartyDifficultyType.Normal, 38702, BossPartyEnterCountType.OnceInSevendays),
    LIMBO_HARD(33, "Limbo", 285, BossPartyDifficultyType.Hard, 38702, BossPartyEnterCountType.OnceInSevendays),

    BALDRIX_NORMAL(34, "Baldrix", 290, BossPartyDifficultyType.Normal, 36996, BossPartyEnterCountType.OnceInSevendays),
    BALDRIX_HARD(34, "Baldrix", 290, BossPartyDifficultyType.Hard, 36996, BossPartyEnterCountType.OnceInSevendays),

    FIRST_ADVERSARY_EASY(35, "First Adversary", 270, BossPartyDifficultyType.Easy, 38319, BossPartyEnterCountType.OnceInSevendays),
    FIRST_ADVERSARY_NORMAL(35, "First Adversary", 270, BossPartyDifficultyType.Normal, 38319, BossPartyEnterCountType.OnceInSevendays),
    FIRST_ADVERSARY_HARD(35, "First Adversary", 270, BossPartyDifficultyType.Hard, 38319, BossPartyEnterCountType.OnceInSevendays),
    FIRST_ADVERSARY_EXTREME(35, "First Adversary", 270, BossPartyDifficultyType.Extreme, 38319, BossPartyEnterCountType.OnceInSevendays),

    AKECHI_MITSUHIDE(107, "Akechi Mitsuhide", 200, BossPartyDifficultyType.Normal, 0, BossPartyEnterCountType.OnceInSevendays),
    // IDs, levels and difficulties read from the original TMS282 Etc/BossParty.img.
    KAI_NORMAL(36, "Kai", 270, BossPartyDifficultyType.Normal, 0, BossPartyEnterCountType.OnceInSevendays),
    KAI_HARD(36, "Kai", 280, BossPartyDifficultyType.Hard, 0, BossPartyEnterCountType.OnceInSevendays),
    YMIR_NORMAL(37, "Ymir", 280, BossPartyDifficultyType.Normal, 0, BossPartyEnterCountType.OnceInSevendays),
    YMIR_HARD(37, "Ymir", 280, BossPartyDifficultyType.Hard, 0, BossPartyEnterCountType.OnceInSevendays),
    JUPITER_NORMAL(38, "Jupiter", 295, BossPartyDifficultyType.Normal, 0, BossPartyEnterCountType.OnceInSevendays),
    JUPITER_HARD(38, "Jupiter", 295, BossPartyDifficultyType.Hard, 0, BossPartyEnterCountType.OnceInSevendays),
    MALITIA_NORMAL(110, "Malitia", 250, BossPartyDifficultyType.Normal, 0, BossPartyEnterCountType.OnceInSevendays),
    MALITIA_EXTREME(110, "Malitia", 250, BossPartyDifficultyType.Extreme, 0, BossPartyEnterCountType.OnceInSevendays),
    ;

    private final int orderId;
    private final String bossName;
    private final int levelMin;
    private final BossPartyDifficultyType difficulty;
    private final int preQuest;
    private final BossPartyEnterCountType enterCount;

    BossPartyType(int orderId, String bossName, int levelMin, BossPartyDifficultyType difficulty, int preQuest, BossPartyEnterCountType enterCount) {
        this.orderId = orderId;
        this.bossName = bossName;
        this.levelMin = levelMin;
        this.difficulty = difficulty;
        this.preQuest = preQuest;
        this.enterCount = enterCount;
    }

    public int getOrderId() {
        return orderId;
    }

    public String getBossName() {
        return bossName;
    }

    public int getLevelMin() {
        return levelMin;
    }

    public BossPartyDifficultyType getDifficulty() {
        return difficulty;
    }

    public int getPreQuest() {
        return preQuest;
    }

    public BossPartyEnterCountType getEnterCount() {
        return enterCount;
    }

    public static BossPartyType getByOrderIdAndDifficulty(int orderId, int difficulty) {
        for (BossPartyType bossPartyType : BossPartyType.values()) {
            if (bossPartyType.getOrderId() == orderId && bossPartyType.getDifficulty().getVal() == difficulty) {
                return bossPartyType;
            }
        }
        return null;
    }

    public static int getOrderIdByBossName(String bossName) {
        for (BossPartyType bossPartyType : BossPartyType.values()) {
            if (bossPartyType.getBossName().equalsIgnoreCase(bossName)) {
                return bossPartyType.orderId;
            }
        }
        return -1;
    }

    private static final int[][] DIFF_IDS_BY_ORDER;

    static {
        int max = 0;
        for (BossPartyType e : values()) max = Math.max(max, e.orderId);

        int[][] tmp = new int[max + 1][];
        for (int oid = 0; oid <= max; oid++) {
            int count = 0;
            for (BossPartyType e : values()) if (e.orderId == oid) count++;

            int[] diffs = new int[count];
            int idx = 0;
            for (BossPartyType e : values()) {
                if (e.orderId == oid) diffs[idx++] = e.difficulty.getVal();
            }
            java.util.Arrays.sort(diffs);
            tmp[oid] = diffs;
        }
        DIFF_IDS_BY_ORDER = tmp;
    }

    public static int[] getDifficultyIdsByOrderId(int orderId) {
        return (orderId >= 0 && orderId < DIFF_IDS_BY_ORDER.length)
                ? DIFF_IDS_BY_ORDER[orderId]
                : new int[0];
    }
}
