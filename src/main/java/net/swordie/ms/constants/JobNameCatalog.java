package net.swordie.ms.constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traditional Chinese job names, shared by the server console and the launcher API. Plain data: no Protocol, WZ or
 * database access, so the launcher CLI can use it.
 * <p>
 * Two tables, kept apart on purpose:
 * <ul>
 *     <li><b>Launcher</b> ({@link #lineName}, {@link #branch}, {@link #collab}): the job <i>line</i> a character
 *     belongs to, in TMS spelling (String.wz Skill.img book names of 282): beginners and first jobs show their own
 *     name (初心者, 劍士, 法師...), from the second job on the final job of the line (英雄, 煉獄巫師, 凱內西斯...).
 *     Each line names its branch explicitly instead of using {@code JobConstants.is*EquipJob}: those overlap (Ark and
 *     Xenon are two branches) and miss Kain. Unknown ids answer {@code "職業 <id>"} and {@link JobBranch#NON}.</li>
 *     <li><b>Console</b> ({@link #consoleRootName}, {@link #consoleFifthRootName}, {@link #consoleLoginJobName}): the
 *     strings the console's JobCatalog always showed, moved here verbatim (coarse groups such as 騎士團 / 反抗者, and
 *     the "（職業ID n）" suffix). The console keeps its wording until its launcher panel decides otherwise.</li>
 * </ul>
 */
public final class JobNameCatalog {

    /** One job line of the launcher table. */
    private record Line(String name, JobBranch branch, boolean collab) {
    }

    private static final Map<Integer, Line> LINES = lines();

    private JobNameCatalog() {
    }

    // ------------------------------------------------------------------ launcher

    /** The line name of {@code jobId} (e.g. 112 -> 英雄, 3212 -> 煉獄巫師), or {@code "職業 <id>"} when unknown. */
    public static String lineName(int jobId) {
        Line line = LINES.get(jobId);
        return line == null ? "職業 " + jobId : line.name();
    }

    /** The branch of {@code jobId}; {@link JobBranch#NON} for beginners, GMs and unknown ids. */
    public static JobBranch branch(int jobId) {
        Line line = LINES.get(jobId);
        return line == null ? JobBranch.NON : line.branch();
    }

    /** A collaboration job (Tanjiro 12005 / 12100, Saitama 12006 / 12200). */
    public static boolean collab(int jobId) {
        Line line = LINES.get(jobId);
        return line != null && line.collab();
    }

    /** {@code jobId} is in the launcher table. */
    public static boolean known(int jobId) {
        return LINES.containsKey(jobId);
    }

    /** Every job id of the launcher table (checks). */
    public static java.util.Set<Integer> knownJobIds() {
        return Collections.unmodifiableSet(LINES.keySet());
    }

    private static Map<Integer, Line> lines() {
        Map<Integer, Line> m = new HashMap<>();
        // Explorers
        line(m, "初心者", JobBranch.NON, 0);
        line(m, "劍士", JobBranch.WAR, 100);
        line(m, "英雄", JobBranch.WAR, 110, 111, 112);
        line(m, "聖騎士", JobBranch.WAR, 120, 121, 122);
        line(m, "黑騎士", JobBranch.WAR, 130, 131, 132);
        line(m, "法師", JobBranch.MAG, 200);
        line(m, "大魔導士（火、毒）", JobBranch.MAG, 210, 211, 212);
        line(m, "大魔導士（冰、雷）", JobBranch.MAG, 220, 221, 222);
        line(m, "主教", JobBranch.MAG, 230, 231, 232);
        line(m, "弓箭手", JobBranch.ARC, 300);
        line(m, "箭神", JobBranch.ARC, 310, 311, 312);
        line(m, "神射手", JobBranch.ARC, 320, 321, 322);
        line(m, "開拓者", JobBranch.ARC, 301, 330, 331, 332);
        line(m, "盜賊", JobBranch.THF, 400);
        line(m, "夜使者", JobBranch.THF, 410, 411, 412);
        line(m, "暗影神偷", JobBranch.THF, 420, 421, 422);
        line(m, "影武者", JobBranch.THF, 430, 431, 432, 433, 434);
        line(m, "海盜", JobBranch.PIR, 500);
        line(m, "拳霸", JobBranch.PIR, 510, 511, 512);
        line(m, "槍神", JobBranch.PIR, 520, 521, 522);
        line(m, "重砲指揮官", JobBranch.PIR, 501, 530, 531, 532);
        line(m, "蒼龍俠客", JobBranch.PIR, 508, 570, 571, 572);
        line(m, "管理員", JobBranch.NON, 800, 900, 910);
        // Cygnus Knights
        line(m, "貴族", JobBranch.NON, 1000);
        line(m, "聖魂劍士", JobBranch.WAR, 1100, 1110, 1111, 1112);
        line(m, "烈焰巫師", JobBranch.MAG, 1200, 1210, 1211, 1212);
        line(m, "破風使者", JobBranch.ARC, 1300, 1310, 1311, 1312);
        line(m, "暗夜行者", JobBranch.THF, 1400, 1410, 1411, 1412);
        line(m, "閃雷悍將", JobBranch.PIR, 1500, 1510, 1511, 1512);
        line(m, "米哈逸", JobBranch.WAR, 5000, 5100, 5110, 5111, 5112);
        // Heroes
        line(m, "狂狼勇士", JobBranch.WAR, 2000, 2100, 2110, 2111, 2112);
        line(m, "龍魔導士", JobBranch.MAG, 2001, 2200, 2210, 2211, 2212, 2213, 2214, 2215, 2216, 2217, 2218);
        line(m, "精靈遊俠", JobBranch.ARC, 2002, 2300, 2310, 2311, 2312);
        line(m, "幻影俠盜", JobBranch.THF, 2003, 2400, 2410, 2411, 2412);
        line(m, "夜光", JobBranch.MAG, 2004, 2700, 2710, 2711, 2712);
        line(m, "隱月", JobBranch.PIR, 2005, 2500, 2510, 2511, 2512);
        // Resistance
        line(m, "市民", JobBranch.NON, 3000);
        line(m, "惡魔", JobBranch.WAR, 3001);
        line(m, "惡魔殺手", JobBranch.WAR, 3100, 3110, 3111, 3112);
        line(m, "惡魔復仇者", JobBranch.WAR, 3101, 3120, 3121, 3122);
        line(m, "煉獄巫師", JobBranch.MAG, 3200, 3210, 3211, 3212);
        line(m, "狂豹獵人", JobBranch.ARC, 3300, 3310, 3311, 3312);
        line(m, "機甲戰神", JobBranch.PIR, 3500, 3510, 3511, 3512);
        // Xenon equips thief and pirate gear; one branch is shown (thief).
        line(m, "傑諾", JobBranch.THF, 3002, 3600, 3610, 3611, 3612);
        line(m, "爆拳槍神", JobBranch.WAR, 3700, 3710, 3711, 3712);
        // Sengoku
        line(m, "劍豪", JobBranch.WAR, 4001, 4100, 4110, 4111, 4112);
        line(m, "陰陽師", JobBranch.MAG, 4002, 4200, 4210, 4211, 4212);
        // Nova
        line(m, "凱撒", JobBranch.WAR, 6000, 6100, 6110, 6111, 6112);
        line(m, "天使破壞者", JobBranch.PIR, 6001, 6500, 6510, 6511, 6512);
        line(m, "卡蒂娜", JobBranch.THF, 6002, 6400, 6410, 6411, 6412);
        line(m, "凱殷", JobBranch.ARC, 6003, 6300, 6310, 6311, 6312);
        // Others
        line(m, "神之子", JobBranch.WAR, 10000, 10100, 10110, 10111, 10112);
        line(m, "幻獸師", JobBranch.MAG, 11000, 11200, 11210, 11211, 11212);
        line(m, "品克繽", JobBranch.WAR, 13000, 13100);
        line(m, "雪吉拉", JobBranch.PIR, 13001, 13500);
        line(m, "凱內西斯", JobBranch.MAG, 14000, 14200, 14210, 14211, 14212);
        line(m, "伊利恩", JobBranch.MAG, 15000, 15200, 15210, 15211, 15212);
        // Ark equips warrior and pirate gear; one branch is shown (pirate).
        line(m, "亞克", JobBranch.PIR, 15001, 15500, 15510, 15511, 15512);
        line(m, "阿戴爾", JobBranch.WAR, 15002, 15100, 15110, 15111, 15112);
        line(m, "卡莉", JobBranch.THF, 15003, 15400, 15410, 15411, 15412);
        line(m, "虎影", JobBranch.THF, 16000, 16400, 16410, 16411, 16412);
        line(m, "菈菈", JobBranch.MAG, 16001, 16200, 16210, 16211, 16212);
        line(m, "蓮", JobBranch.WAR, 16002, 16100, 16110, 16111, 16112);
        line(m, "墨玄", JobBranch.PIR, 17000, 17500, 17510, 17511, 17512);
        line(m, "琳恩", JobBranch.MAG, 17001, 17200, 17210, 17211, 17212);
        line(m, "希婭", JobBranch.MAG, 18000, 18200, 18210, 18211, 18212);
        // Collaboration jobs
        collabLine(m, "炭治郎", JobBranch.WAR, 12005, 12100);
        collabLine(m, "一拳超人", JobBranch.PIR, 12006, 12200);
        return Map.copyOf(m);
    }

    private static void line(Map<Integer, Line> m, String name, JobBranch branch, int... ids) {
        add(m, new Line(name, branch, false), ids);
    }

    private static void collabLine(Map<Integer, Line> m, String name, JobBranch branch, int... ids) {
        add(m, new Line(name, branch, true), ids);
    }

    private static void add(Map<Integer, Line> m, Line line, int... ids) {
        for (int id : ids) {
            if (m.put(id, line) != null) {
                throw new IllegalStateException("job " + id + " listed twice");
            }
        }
    }

    // ------------------------------------------------------------------ console (moved verbatim from JobCatalog)

    /** The console's name of a login job (character creation settings). */
    public static String consoleLoginJobName(JobConstants.LoginJob job) {
        return switch (job) {
            case RESISTANCE -> "反抗者";
            case EXPLORER -> "冒險家";
            case CYGNUS -> "騎士團";
            case ARAN -> "戰神";
            case EVAN -> "龍神";
            case MERCEDES -> "雙弩精靈";
            case DEMON -> "惡魔";
            case PHANTOM -> "幻影";
            case DUAL_BLADE -> "雙刀";
            case MIHILE -> "米哈爾";
            case LUMINOUS -> "夜光法師";
            case KAISER -> "凱撒";
            case ANGELIC -> "爆莉萌天使";
            case CANNONER -> "神炮王";
            case XENON -> "尖兵";
            case ZERO -> "神之子";
            case SHADE -> "隱月";
            case PINK_BEAN -> "品克繽";
            case KINESIS -> "超能力者";
            case CADENA -> "卡蒂娜";
            case ILLIUM -> "伊利恩";
            case ARK -> "亞克";
            case PATHFINDER -> "古蹟獵人";
            case HOYOUNG -> "虎影";
            case ADELE -> "阿戴爾";
            case KAIN -> "凱殷";
            case YETI -> "雪人";
            case LARA -> "菈菈";
            case KHALI -> "卡莉";
            case TANJIRO -> "竈門炭治郎";
            case SAITAMA -> "埼玉";
            case REN -> "蓮";
            case MOXUAN -> "墨玄";
            case HAYATO -> "劍豪";
            case KANNA -> "陰陽師";
            case LYNN -> "琳恩";
            case SIA -> "希婭";
        };
    }

    /** The console's skill-root name: "名稱（職業ID n）", or "未知職業（職業ID n）" (never null). */
    public static String consoleRootName(int rootId) {
        return formatSkillNodeName(ROOT_NAMES.get(rootId), rootId);
    }

    /** The console's fifth-job skill-root name, or null when the root has none. */
    public static String consoleFifthRootName(int rootId) {
        String explicit = FIFTH_ROOT_NAMES.get(rootId);
        if (explicit != null) {
            return formatSkillNodeName(explicit, rootId);
        }
        String fourthJob = ROOT_NAMES.get(rootId);
        if (fourthJob != null && fourthJob.contains("·四轉")) {
            return formatSkillNodeName(fourthJob.replace("·四轉", "·五轉"), rootId);
        }
        return null;
    }

    private static String formatSkillNodeName(String name, int rootId) {
        if (name != null) {
            return name + "（職業ID " + rootId + "）";
        }
        return "未知職業（職業ID " + rootId + "）";
    }

    private static final Map<Integer, String> ROOT_NAMES = rootNames();
    private static final Map<Integer, String> FIFTH_ROOT_NAMES = fifthRootNames();

    private static Map<Integer, String> fifthRootNames() {
        Map<Integer, String> names = new LinkedHashMap<>();
        put(names, "英雄·五轉", 114); put(names, "聖騎士·五轉", 124); put(names, "黑騎士·五轉", 134);
        put(names, "火毒·五轉", 214); put(names, "冰雷·五轉", 224); put(names, "主教·五轉", 234);
        put(names, "箭神·五轉", 314); put(names, "神射手·五轉", 324); put(names, "古蹟獵人·五轉", 334);
        put(names, "隱士·五轉", 414); put(names, "俠盜·五轉", 424); put(names, "雙刀·五轉", 434);
        put(names, "衝鋒隊長·五轉", 514); put(names, "船長·五轉", 524); put(names, "神炮王·五轉", 534);
        return Map.copyOf(names);
    }

    private static Map<Integer, String> rootNames() {
        Map<Integer, String> names = new LinkedHashMap<>();
        put(names, "初心者", 0);
        put(names, "戰士·一轉", 100); put(names, "英雄系·二轉", 110); put(names, "英雄系·三轉", 111); put(names, "英雄·四轉", 112);
        put(names, "聖騎士系·二轉", 120); put(names, "聖騎士系·三轉", 121); put(names, "聖騎士·四轉", 122);
        put(names, "黑騎士系·二轉", 130); put(names, "黑騎士系·三轉", 131); put(names, "黑騎士·四轉", 132);
        put(names, "魔法師·一轉", 200); put(names, "火毒系·二轉", 210); put(names, "火毒系·三轉", 211); put(names, "火毒·四轉", 212);
        put(names, "冰雷系·二轉", 220); put(names, "冰雷系·三轉", 221); put(names, "冰雷·四轉", 222); put(names, "主教系·二轉", 230); put(names, "主教系·三轉", 231); put(names, "主教·四轉", 232);
        put(names, "弓箭手·一轉", 300); put(names, "箭神系·二轉", 310); put(names, "箭神系·三轉", 311); put(names, "箭神·四轉", 312); put(names, "神射手系·二轉", 320); put(names, "神射手系·三轉", 321); put(names, "神射手·四轉", 322); put(names, "古蹟獵人系", 301, 330, 331, 332);
        put(names, "飛俠·一轉", 400); put(names, "隱士系·二轉", 410); put(names, "隱士系·三轉", 411); put(names, "隱士·四轉", 412); put(names, "俠盜系·二轉", 420); put(names, "俠盜系·三轉", 421); put(names, "俠盜·四轉", 422); put(names, "雙刀系", 430, 431, 432, 433, 434);
        put(names, "海盜·一轉", 500); put(names, "衝鋒隊長系", 510, 511, 512); put(names, "船長系", 520, 521, 522); put(names, "神炮王系", 501, 530, 531, 532);
        put(names, "騎士團", 1000, 1100, 1110, 1111, 1112, 1200, 1210, 1211, 1212, 1300, 1310, 1311, 1312, 1400, 1410, 1411, 1412, 1500, 1510, 1511, 1512);
        put(names, "戰神系", 2000, 2100, 2110, 2111, 2112); put(names, "龍神系", 2001, 2210, 2212, 2214, 2217); put(names, "雙弩精靈系", 2002, 2300, 2310, 2311, 2312); put(names, "幻影系", 2003, 2400, 2410, 2411, 2412); put(names, "夜光法師系", 2004, 2700, 2710, 2711, 2712); put(names, "隱月系", 2005, 2500, 2510, 2511, 2512);
        put(names, "反抗者", 3000, 3001, 3002, 3100, 3110, 3111, 3112, 3101, 3120, 3121, 3122, 3200, 3210, 3211, 3212, 3300, 3310, 3311, 3312, 3500, 3510, 3511, 3512, 3600, 3610, 3611, 3612, 3700, 3710, 3711, 3712);
        put(names, "劍豪系", 4001, 4100, 4110, 4111, 4112); put(names, "陰陽師系", 4002, 4200, 4210, 4211, 4212); put(names, "米哈爾系", 5000, 5100, 5110, 5111, 5112);
        put(names, "凱撒系", 6000, 6100, 6110, 6111, 6112); put(names, "爆莉萌天使系", 6001, 6500, 6510, 6511, 6512); put(names, "卡蒂娜系", 6002, 6400, 6410, 6411, 6412); put(names, "凱殷系", 6003, 6300, 6310, 6311, 6312);
        put(names, "神之子系", 10000, 10100, 10110, 10111, 10112); put(names, "林之靈系", 11000, 11200, 11210, 11211, 11212); put(names, "品克繽", 13000, 13100); put(names, "超能力者系", 14000, 14200, 14210, 14211, 14212);
        put(names, "伊利恩系", 15000, 15200, 15210, 15211, 15212); put(names, "亞克系", 15001, 15500, 15510, 15511, 15512); put(names, "阿戴爾系", 15002, 15100, 15110, 15111, 15112); put(names, "卡莉系", 15003, 15400, 15410, 15411, 15412);
        put(names, "蓮", 16002, 16100, 16110, 16111, 16112); put(names, "菈菈系", 16001, 16200, 16210, 16211, 16212); put(names, "虎影系", 16000, 16400, 16410, 16411, 16412); put(names, "墨玄系", 17000, 17500, 17510, 17511, 17512); put(names, "琳恩系", 17001, 17200, 17210, 17211, 17212); put(names, "希婭系", 18000, 18200, 18210, 18211, 18212);
        return Map.copyOf(names);
    }

    private static void put(Map<Integer, String> names, String name, int... ids) {
        for (int id : ids) {
            names.put(id, name);
        }
    }
}
