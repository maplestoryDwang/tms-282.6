package net.swordie.ms.world.tms282.contents.changeburning.dream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * The client's own 夢幻的裝備 table {@code Etc/EventItem.img} (282 Etc.wz, exported to
 * {@code data/tms282-changeburning/eventitem.tsv}, research/tms2823/lucid-re-0930/ability §3.1). Immutable.
 * <ul>
 *     <li>{@code ItemInfo/1..19}: type 1 equip / 2 gem, bodyPart 1 傳令 (Weapon) / 2 根源 (Ring) / 3 邊境 (Pendant),
 *     grade 0 普通 .. 4 傳說, slotCount; item ids 2831585..2831603 = ItemInfo 1..19;</li>
 *     <li>{@code ItemStat/<Weapon|Ring|Pendant>/<grade>/<+n>/<atk|cri|maxHP>};</li>
 *     <li>{@code EnhanceEquip/<grade 1..4>/<n 0..11>/success|dust} (per mille / dust for +n -> +n+1);</li>
 *     <li>{@code DisassembleEquip/<grade>/<n>/dust}.</li>
 * </ul>
 * The 9 stats in the order the client sums them (688FE6..68912D) = {@code LucidAbility.finalBlock}'s equip vector:
 * maxHP, maxHPR, atk, atkR, cri, criDamR, speed, mastery, reduceCoolTime.
 */
public final class DreamCatalog {
    public static final int FIRST_ITEM = 2831585, LAST_ITEM = 2831603;
    public static final int MAX_ENHANCE = 12; // 17B0754: cmp enhance, 0xC
    public static final int TYPE_EQUIP = 1, TYPE_GEM = 2;
    public static final int STATS = 9, MAX_HP = 0, ATK = 2, CRI = 4;
    public static final String[] GRADE_NAMES = {"普通", "特殊", "稀有", "罕見", "傳說"};
    public static final String[] PART_NAMES = {"", "傳令", "根源", "邊境"};

    public record Info(int index, String name, int type, int bodyPart, int grade, int slotCount) {
        public int itemId() {
            return FIRST_ITEM + index - 1;
        }
    }

    private final Map<String, String> data;

    private DreamCatalog(Map<String, String> data) {
        this.data = Map.copyOf(data);
    }

    public static DreamCatalog load(Path tsv) {
        Map<String, String> m = new HashMap<>();
        try {
            for (String line : Files.readAllLines(tsv, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                int tab = line.indexOf('\t');
                if (tab > 0) {
                    m.put(line.substring(0, tab), line.substring(tab + 1).trim());
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + tsv, e);
        }
        DreamCatalog c = new DreamCatalog(m);
        for (int i = 1; i <= 19; i++) {
            if (c.info(i) == null) {
                throw new IllegalStateException(tsv + ": ItemInfo/" + i + " missing");
            }
        }
        return c;
    }

    private int num(String key, int fallback) {
        String v = data.get(key);
        if (v == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** ItemInfo 1..19, or null. */
    public Info info(int index) {
        if (index < 1 || index > 19 || !data.containsKey("ItemInfo/" + index + "/type")) {
            return null;
        }
        String p = "ItemInfo/" + index + "/";
        return new Info(index, data.getOrDefault(p + "name", ""), num(p + "type", 0), num(p + "bodyPart", 0),
                num(p + "grade", 0), num(p + "slotCount", 0));
    }

    /** The equip ItemInfo of a body part (1..3) and grade (0..4), or null. */
    public Info equip(int bodyPart, int grade) {
        for (int i = 1; i <= 19; i++) {
            Info in = info(i);
            if (in.type() == TYPE_EQUIP && in.bodyPart() == bodyPart && in.grade() == grade) {
                return in;
            }
        }
        return null;
    }

    public static Info indexOfItem(DreamCatalog c, int itemId) {
        return itemId < FIRST_ITEM || itemId > LAST_ITEM ? null : c.info(itemId - FIRST_ITEM + 1);
    }

    private static String part(int bodyPart) {
        return switch (bodyPart) {
            case 1 -> "Weapon";
            case 2 -> "Ring";
            case 3 -> "Pendant";
            default -> throw new IllegalArgumentException("bodyPart " + bodyPart);
        };
    }

    /** The 9-stat vector of one equip at {@code enhance}. */
    public int[] equipStats(int bodyPart, int grade, int enhance) {
        int[] v = new int[STATS];
        String p = "ItemStat/" + part(bodyPart) + "/" + grade + "/" + enhance + "/";
        switch (bodyPart) {
            case 1 -> v[ATK] = num(p + "atk", 0);
            case 2 -> v[CRI] = num(p + "cri", 0);
            case 3 -> v[MAX_HP] = num(p + "maxHP", 0);
            default -> {
            }
        }
        return v;
    }

    /** Highest enhance level with data for the grade (0 for 普通). */
    public int maxEnhance(int grade) {
        int n = 0;
        while (n < MAX_ENHANCE && data.containsKey("EnhanceEquip/" + grade + "/" + n + "/success")) {
            n++;
        }
        return n;
    }

    /** Success per mille of +n -> +n+1, -1 when it cannot be enhanced. */
    public int enhanceSuccess(int grade, int n) {
        return num("EnhanceEquip/" + grade + "/" + n + "/success", -1);
    }

    public int enhanceDust(int grade, int n) {
        return num("EnhanceEquip/" + grade + "/" + n + "/dust", -1);
    }

    public int disassembleDust(int grade, int enhance) {
        return num("DisassembleEquip/" + grade + "/" + enhance + "/dust", 0);
    }
}
