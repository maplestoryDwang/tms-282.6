package net.swordie.ms.client.jobs;

import net.swordie.ms.constants.JobConstants;

import java.util.List;
import java.util.function.Predicate;

/**
 * The starter weapon of each job family, shared by the new-character equipment box (item script consume_2433509)
 * and the control panel's 萌新武器 (content expand/menu/萌新專區/萌新武器), which used to keep two copies of these
 * tables. The weapons are the 282 "黑色" starter series; every id is checked against the 282 item data and the job's
 * weapon types by Tms282ControlPanelCheck.
 * <p>
 * Order matters and follows the old script tables: jobs that choose between several weapons first, then fixed
 * weapons; a novice (the box is opened at creation) then gets {@link #DEFAULT_WEAPON}; then per-branch fallbacks. {@link #options} is empty for jobs without a matching starter weapon (Zero
 * keeps its own Lapis/Lazuli; Sia is disabled in 282 and has no weapon in the 282 data).
 */
public final class StarterWeapons {
    /** 黑色帽子, 黑色的披肩, 黑色套裝: the armour of the equipment box. */
    public static final List<Integer> ARMOR_SET = List.of(1004404, 1102799, 1052893);
    /** Given when nothing else matches (novices of the 265 tables' default). */
    public static final int DEFAULT_WEAPON = 1302334;

    private record Rule(Predicate<Short> job, List<Integer> weapons) {
    }

    private static final List<Rule> CHOICES = List.of(
            rule(JobConstants::isHero, 1302334, 1312200, 1402252, 1412178),
            rule(JobConstants::isPaladin, 1302334, 1322251, 1402252, 1422185),
            rule(JobConstants::isDarkKnight, 1432215, 1442269),
            rule(JobConstants::isDawnWarrior, 1302334, 1402252),
            rule(JobConstants::isDemonSlayer, 1312200, 1322251),
            rule(j -> JobConstants.isAdventurerMage(j) || JobConstants.isBlazeWizard(j) || JobConstants.isEvan(j), 1372223, 1382260)
    );

    private static final List<Rule> FIXED = List.of(
            rule(JobConstants::isZero),                         // Lapis/Lazuli are Zero's own weapons
            rule(JobConstants::isSiaAstelle),                   // disabled in 282, no 1253xxx weapon in the 282 data
            rule(JobConstants::isTanjiro, 1402252),             // 282 MakeCharInfo 12100: two-handed sword 1402119
            rule(JobConstants::isSaitama, 1482217),             // 282 MakeCharInfo 12200: knuckle 1482110
            rule(JobConstants::isPinkBean, 1302334),            // 282 MakeCharInfo 13100: one-handed sword 1302000
            rule(JobConstants::isYeti, 1482217),                // knuckle 1482191
            rule(JobConstants::isBeastTamer, 1252094),          // 282 MakeCharInfo 11000: 1252000 (memory staff type)
            rule(JobConstants::isJett, 1492232),
            rule(JobConstants::isLuminous, 1212116),
            rule(JobConstants::isAdele, 1213023),
            rule(JobConstants::isKain, 1214023),
            rule(JobConstants::isRen, 1215033),
            rule(j -> JobConstants.isAngelicBuster(j), 1222110),
            rule(JobConstants::isDemonAvenger, 1232110),
            rule(JobConstants::isXenon, 1242117),
            rule(JobConstants::isLynn, 1252094),                // 黑色記憶長杖 (1252095 does not exist)
            rule(JobConstants::isKinesis, 1262027),
            rule(JobConstants::isCadena, 1272031),
            rule(JobConstants::isIllium, 1282019),
            rule(JobConstants::isHoYoung, 1292023),
            rule(JobConstants::isShadower, 1332275),
            rule(JobConstants::isDualBlade, 1332275),
            rule(JobConstants::isPhantom, 1362136),
            rule(JobConstants::isLara, 1372223),
            rule(JobConstants::isBattleMage, 1382260),
            rule(JobConstants::isMihile, 1302334),
            rule(JobConstants::isKaiser, 1402252),
            rule(JobConstants::isMoXuan, 1403023),
            rule(JobConstants::isKhali, 1404023),
            rule(JobConstants::isAran, 1442269),
            rule(JobConstants::isBowMaster, 1452253),
            rule(JobConstants::isWindArcher, 1452253),
            rule(JobConstants::isMarksman, 1462240),
            rule(JobConstants::isWildHunter, 1462240),
            rule(JobConstants::isNightLord, 1472262),
            rule(JobConstants::isNightWalker, 1472262),
            rule(JobConstants::isBuccaneer, 1482217),
            rule(JobConstants::isThunderBreaker, 1482217),
            rule(JobConstants::isShade, 1482217),
            rule(JobConstants::isArk, 1482217),
            rule(JobConstants::isMechanic, 1492232),
            rule(JobConstants::isCorsair, 1492232),
            rule(JobConstants::isMercedes, 1522139),
            rule(JobConstants::isCannoneer, 1532145),
            rule(JobConstants::isHayato, 1542113),              // 黑色之鐵刀 (1542114 does not exist)
            rule(JobConstants::isKanna, 1254011),               // 282 Kanna uses 陰陽扇 1254xxx (MakeCharInfo 4002: 1254015)
            rule(JobConstants::isBlaster, 1582021),
            rule(JobConstants::isPathFinder, 1592008)
    );

    private static final List<Rule> FALLBACKS = List.of(
            rule(j -> JobConstants.isAdventurerWarrior(j) || JobConstants.isCygnusKnight(j) || JobConstants.isDemon(j)
                    || JobConstants.isKaiser(j), 1302334),
            rule(j -> JobConstants.isAdventurerMage(j) || JobConstants.isEvan(j) || JobConstants.isLuminous(j)
                    || JobConstants.isKanna(j) || JobConstants.isLara(j), 1372223),
            rule(j -> JobConstants.isAdventurerArcher(j) || JobConstants.isMercedes(j) || JobConstants.isKain(j), 1452253),
            rule(j -> JobConstants.isAdventurerThief(j) || JobConstants.isPhantom(j) || JobConstants.isCadena(j)
                    || JobConstants.isHoYoung(j) || JobConstants.isKhali(j), 1472262),
            rule(j -> JobConstants.isAdventurerPirate(j) || JobConstants.isResistance(j) || JobConstants.isAngelicBuster(j)
                    || JobConstants.isArk(j) || JobConstants.isXenon(j), 1482217)
    );

    /**
     * Weapons below the Lv.100 黑色 starter series, per weapon type (item id / 1000): Lv.10 / 30 / 60. Picked from
     * the 282 item data (2026-10-06, scratchpad tiers.py): the strongest ordinary weapon in [tier-20, tier], event and
     * novelty items excluded. Lv.100 and above get the job's own starter weapon from {@link #options}.
     */
    private static final java.util.Map<Integer, int[]> WEAPON_TIERS = java.util.Map.ofEntries(
            java.util.Map.entry(1212, new int[]{1212001, 1212002, 1212030}), // 普通克魯 / 金色閃耀 / 會心瑟雷弗
            java.util.Map.entry(1213, new int[]{1213000, 1213001, 1213003}), // 耐力 / 榮譽 / 奉獻
            java.util.Map.entry(1214, new int[]{1214000, 1214001, 1214003}), // 黑暗凝視 / 暗刺 / 反諷攻擊
            java.util.Map.entry(1215, new int[]{1215001, 1215002, 1215004}), // 長劍 / 研磨長劍 / 紅風劍
            java.util.Map.entry(1222, new int[]{1222001, 1222076, 1222004}), // 紫色煙霧 / 紫色煙霧蓓塔 / 杰德日出環
            java.util.Map.entry(1232, new int[]{1232001, 1232002, 1232030}), // 藍色復仇者 / 深處記憶體 / 卡迪納奧門
            java.util.Map.entry(1242, new int[]{1242001, 1242002, 1242029}), // 不對稱之魂 / 山脊切割者 / 梅克洛短刀
            java.util.Map.entry(1252, new int[]{1252001, 1252002, 1252004}), // 露西奧萊記憶長杖 / 海紋石記憶長杖 / 勿忘草記憶長杖
            java.util.Map.entry(1254, new int[]{1254001, 1254003, 1254006}), // 鐵扇 / 三紋陰陽扇 / 爽倉扇
            java.util.Map.entry(1262, new int[]{1262000, 1262001, 1262003}), // 原型ESP限制器 / 測試用ESP限制器 / 衛波輪ESP限制器
            java.util.Map.entry(1272, new int[]{1272000, 1272001, 1272003}), // 德梅勒魁里歐 / 德被奴斯 / 德馬爾提
            java.util.Map.entry(1282, new int[]{1282000, 1282001, 1282003}), // 銀月魔法護腕 / 馬提斯魔法護腕 / 羅比斯魔法護腕
            java.util.Map.entry(1292, new int[]{1292000, 1292001, 1292003}), // 黑雲扇 / 小扇 / 暴風扇
            java.util.Map.entry(1302, new int[]{1302077, 1302134, 1302041}), // 新手劍士之劍 / 冒險家的黃沙之劍 / 火焰刀
            java.util.Map.entry(1312, new int[]{1312049, 1312048, 1312020}), // 挑戰的單手斧頭 - 等級10 / 冒險家的青光斧 / 黃金斧
            java.util.Map.entry(1322, new int[]{1322032, 1322076, 1322038}), // 鋼鐵鎚子 / 冒險家的古代錘 / 暴風錘
            java.util.Map.entry(1332, new int[]{1332102, 1332103, 1332036}), // 盜賊之飛影刃 / 冒險家的刺客短刀 / 鳳凰刃
            java.util.Map.entry(1362, new int[]{1362001, 1362005, 1362035}), // 初學者手杖 / 盜賊手杖 / 魔棒
            java.util.Map.entry(1372, new int[]{1372013, 1372061, 1372056}), // 竹杖 / 冒險家的水晶短杖 / 楓葉靈魂短杖
            java.util.Map.entry(1382, new int[]{1382100, 1382109, 1382110}), // 新手魔法師的長杖 / 迅猛的弩法杖 / 狂暴的弩法杖
            java.util.Map.entry(1402, new int[]{1402076, 1402077, 1402022}), // 劍士之雙手劍 / 冒險家的半月巨刀 / 霸王劍
            java.util.Map.entry(1403, new int[]{1403000, 1403001, 1403003}), // 老舊武拳 / 修行者武拳 / 武俠武拳
            java.util.Map.entry(1404, new int[]{1404000, 1404001, 1404003}), // 埃內特環刃 / 阿奎拉環刃 / 拉切爾塔
            java.util.Map.entry(1412, new int[]{1412048, 1412047, 1412015}), // 挑戰的雙手斧頭 - 等級10 / 冒險家的格鬥斧 / 雷電斧
            java.util.Map.entry(1422, new int[]{1422050, 1422049, 1422017}), // 挑戰的雙手鈍器 - 等級10 / 冒險家的巨人錘 / 封魂之錘
            java.util.Map.entry(1432, new int[]{1432068, 1432067, 1432022}), // 挑戰的長槍 - 等級10 / 冒險家的雙天戟 / 銀龍槍
            java.util.Map.entry(1442, new int[]{1442096, 1442080, 1442033}), // 挑戰的矛 - 等級10 / 鋼鐵矛 / 黃龍刀
            java.util.Map.entry(1452, new int[]{1452091, 1452092, 1452030}), // 弓箭手之弓 / 冒險家的暴風弓 / 黃神弓
            java.util.Map.entry(1462, new int[]{1462078, 1462102, 1462026}), // 挑戰的弩弓 - 等級10 / 迅猛的雷本弩 / 破天弩弓
            java.util.Map.entry(1472, new int[]{1472104, 1472103, 1472037}), // 挑戰的拳套 - 等級10 / 冒險家的黑護腕 / 黑閃電甲
            java.util.Map.entry(1482, new int[]{1482065, 1482064, 1482056}), // 虎爪指虎 / 冒險家的尼奧札德毀滅拳套 / 拜爾毀滅拳套
            java.util.Map.entry(1492, new int[]{1492065, 1492064, 1492056}), // 沙漠之鷹 / 冒險家的銀月神射 / 深淵射手
            java.util.Map.entry(1522, new int[]{1522000, 1522004, 1522042}), // 雙重貝西斯 / 雙刃風 / 艾奧羅
            java.util.Map.entry(1532, new int[]{1532000, 1532004, 1532027}), // 無聲加農砲 / 火箭筒 / 火紋火箭炮
            java.util.Map.entry(1542, new int[]{1542000, 1542002, 1542024}), // 鐵刀 / 虎徹 / 紫電
            java.util.Map.entry(1582, new int[]{1582000, 1582001, 1582003}), // 鐵製重拳槍 / 超能不速之客重拳槍 / 巨型重拳槍
            java.util.Map.entry(1592, new int[]{1592000, 1592001, 1592003}) // 馬拉德古代之弓 / 阿修爾古代之弓 / 阿瓦里斯古代之弓
    );

    /** The weapon a {@code job} character of {@code level} should hold: the tier of its (first) starter weapon type. */
    public static int weaponFor(int job, int level) {
        List<Integer> weapons = options(job);
        if (weapons.isEmpty()) {
            return 0;
        }
        int starter = weapons.get(0);
        if (level >= 100) {
            return starter;
        }
        int[] tiers = WEAPON_TIERS.get(starter / 1000);
        if (tiers == null) {
            return starter;
        }
        return level >= 60 ? tiers[2] : level >= 30 ? tiers[1] : tiers[0];
    }
    /** A job's secondary (副手) series by required level 10 / 30 / 60 / 100; 0 = the series has no such tier. */
    private record Secondary(Predicate<Short> job, int lv10, int lv30, int lv60, int lv100) {
    }

    /**
     * The basic secondary series of the 282 item data (names and required levels read from String.wz / the equip
     * data, 2026-10-06). Not listed: Zero and the collab jobs (none), Dual Blade (its 134xxxx blades are bought per
     * level), Beast Tamer and Lynn (series not identified). [presumed from the item names, not from a job check]:
     * Jett 拳, MoXuan 拳環, Ren 如意寶珠, Kanna 靈符.
     */
    private static final List<Secondary> SECONDARIES = List.of(
            sub3(JobConstants::isHero, 1352200),                // 獎牌
            sub3(JobConstants::isPaladin, 1352210),             // 念珠
            sub3(JobConstants::isDarkKnight, 1352220),          // 鎖鏈
            sub3(JobConstants::isFirePoison, 1352230),          // 赤綠之書
            sub3(JobConstants::isIceLightning, 1352240),        // 青銀之書
            sub3(JobConstants::isBishop, 1352250),              // 白金之書
            sub3(JobConstants::isBowMaster, 1352260),           // 羽毛
            sub3(JobConstants::isMarksman, 1352270),            // 一發必中
            sub4(JobConstants::isPathFinder, 1353700),          // 遺跡
            sub3(JobConstants::isShadower, 1352280),            // 暗影
            sub3(JobConstants::isNightLord, 1352290),           // 符
            sub3(JobConstants::isBuccaneer, 1352900),           // 盾牌護腕
            sub3(JobConstants::isCorsair, 1352910),             // 鷹眼
            sub3(JobConstants::isCannoneer, 1352920),           // 轟擊底火
            sub4(JobConstants::isJett, 1352820),                // 拳
            sub4(JobConstants::isMihile, 1098000),              // 靈魂盾牌
            sub3(JobConstants::isCygnusKnight, 1352970),        // 耶雷弗的碎片
            sub3(JobConstants::isAran, 1352930),                // 錘
            sub3(JobConstants::isEvan, 1352940),                // 龍使者的記錄
            sub4(JobConstants::isMercedes, 1352000),            // 魔法箭
            sub4(JobConstants::isPhantom, 1352100),             // 卡牌
            sub4(JobConstants::isLuminous, 1352400),            // 彈
            sub4(JobConstants::isShade, 1353100),               // 狐狸珠子
            new Secondary(j -> JobConstants.isDemonSlayer(j) || JobConstants.isDemonAvenger(j), 1099000, 1099002, 1099003, 1099004), // 力量之盾
            sub3(JobConstants::isBattleMage, 1352950),          // 球
            sub3(JobConstants::isWildHunter, 1352960),          // 狂野鳥嘴
            sub4(JobConstants::isMechanic, 1352700),            // 連發槍
            sub4(JobConstants::isXenon, 1353001),               // 操縱桿
            sub4(JobConstants::isBlaster, 1353400),             // 爆發能力
            sub4(JobConstants::isKaiser, 1352500),              // 超新星的精華
            sub4(JobConstants::isAngelicBuster, 1352601),       // 靈魂之環
            sub4(JobConstants::isCadena, 1353300),              // 發信器
            sub4(JobConstants::isKain, 1354010),                // 武器腰封
            new Secondary(JobConstants::isHayato, 1352800, 1352801, 1352834, 1352835), // 刀 / 小太刀 (1352802 is Lv.70)
            sub4(JobConstants::isKanna, 1354300),               // 靈符
            sub4(JobConstants::isKinesis, 1353200),             // 西洋棋 (士兵 is Lv.1)
            sub4(JobConstants::isIllium, 1353500),              // 魔法翅膀 (基本 is Lv.1)
            sub4(JobConstants::isArk, 1353600),                 // 通行券
            sub4(JobConstants::isAdele, 1354000),               // 手鐲
            sub4(JobConstants::isHoYoung, 1353800),             // 扇墜
            sub4(JobConstants::isLara, 1354020),                // 四玉飾品
            sub4(JobConstants::isKhali, 1354030),               // 能量手環
            sub4(JobConstants::isRen, 1354040),                 // 如意寶珠
            sub4(JobConstants::isMoXuan, 1352860)               // 拳環
    );

    /** Series base, base+1, base+2 = Lv.30 / 60 / 100 (no Lv.10 item). */
    private static Secondary sub3(Predicate<Short> job, int base) {
        return new Secondary(job, 0, base, base + 1, base + 2);
    }

    /** Series base .. base+3 = Lv.10 / 30 / 60 / 100. */
    private static Secondary sub4(Predicate<Short> job, int base) {
        return new Secondary(job, base, base + 1, base + 2, base + 3);
    }

    /** The best secondary of {@code job}'s series a character of {@code level} can wear; 0 = none. */
    public static int secondary(int job, int level) {
        short j = (short) job;
        for (Secondary s : SECONDARIES) {
            if (s.job.test(j)) {
                int[] tiers = {level >= 100 ? s.lv100 : 0, level >= 60 ? s.lv60 : 0, level >= 30 ? s.lv30 : 0, level >= 10 ? s.lv10 : 0};
                for (int id : tiers) {
                    if (id != 0) {
                        return id;
                    }
                }
                return 0;
            }
        }
        return 0;
    }

    private StarterWeapons() {
    }

    private static Rule rule(Predicate<Short> job, Integer... weapons) {
        return new Rule(job, List.of(weapons));
    }

    /** The weapons {@code job} may pick from (one entry: no choice; empty: no starter weapon). */
    public static List<Integer> options(int job) {
        short j = (short) job;
        for (List<Rule> rules : List.of(CHOICES, FIXED)) {
            for (Rule r : rules) {
                if (r.job.test(j)) {
                    return r.weapons;
                }
            }
        }
        if (isNovice(j)) {
            // the box is opened at creation, before the branch is known: a weapon every novice can hold
            return List.of(DEFAULT_WEAPON);
        }
        for (Rule r : FALLBACKS) {
            if (r.job.test(j)) {
                return r.weapons;
            }
        }
        return List.of(DEFAULT_WEAPON);
    }

    /** A first job of a branch (Beginner, Noblesse, Citizen, Legend ...): its own beginner job. */
    private static boolean isNovice(short job) {
        JobConstants.JobEnum e = JobConstants.JobEnum.getJobById(job);
        return JobConstants.isBeginnerJob(job) || (e != null && e.getBeginnerJobId() == job);
    }

    /**
     * After a job advancement: gives the weapon of the new job and level ({@link #weaponFor}) and the secondary
     * ({@link #secondary}) unless the character already holds that exact item. Jobs with a choice get the first
     * weapon type; the others stay available from the panel's 新手武器. Never throws: an advancement must not fail
     * over a weapon.
     *
     * @return the item given, or 0
     */
    public static int grantFor(net.swordie.ms.client.character.Char chr) {
        try {
            int sub = secondary(chr.getJob(), chr.getLevel());
            if (sub != 0 && !chr.hasItem(sub) && chr.canHold(sub)) {
                chr.getScriptManager().giveItem(sub);
                chr.chatMessage("已發放本職業的副手裝備，請到裝備欄查看。");
            }
            int weapon = weaponFor(chr.getJob(), chr.getLevel());
            if (weapon == 0 || chr.hasItem(weapon)) {
                return 0;
            }
            if (!chr.canHold(weapon)) {
                chr.chatMessage("裝備欄已滿，未發放轉職武器；空出欄位後可到萬能面板的新手武器領取。");
                return 0;
            }
            chr.getScriptManager().giveItem(weapon);
            chr.chatMessage("已發放本職業的武器，請到裝備欄查看。");
            return weapon;
        } catch (RuntimeException e) {
            System.out.println("[TMS282] job weapon not granted character=" + chr.getId() + " job=" + chr.getJob() + ": " + e);
            return 0;
        }
    }

    /** Whether {@code job} chooses between several weapons (the scripts then show a menu). */
    public static boolean isChoice(int job) {
        return options(job).size() > 1;
    }
}
