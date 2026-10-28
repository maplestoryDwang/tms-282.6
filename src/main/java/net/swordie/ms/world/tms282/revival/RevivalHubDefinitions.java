package net.swordie.ms.world.tms282.revival;

import net.swordie.ms.world.tms282.revival.hub.HubCatalog;
import net.swordie.ms.world.tms282.revival.hub.HubDefinition;
import net.swordie.ms.world.tms282.revival.hub.HubDefinition.Mission;
import net.swordie.ms.world.tms282.revival.hub.HubDefinition.Offer;
import net.swordie.ms.world.tms282.revival.hub.HubDefinition.Period;
import net.swordie.ms.world.tms282.revival.hub.HubDefinition.Reward;

import java.util.List;
import java.util.Set;

/**
 * Data of the revived hub events (missions, rewards, coin shops). Official reward tables were not available for these
 * legacy events, so the amounts are this server's choice (docs/revival/*.md list them); item ids are checked against
 * the 282 data by Tms282RevivalHubCheck data.
 */
public final class RevivalHubDefinitions {
    /** 追加經驗值 50% 券 / 70% 券 / 經驗值 2 倍券 (mushroom pass items in the 282 data). */
    static final int EXP50 = 2023999, EXP70 = 2024303, EXP2X = 2450180;

    /** "event:mission" ids removed from the code hubs; revival-rewards.json overrides for them are skipped, not rejected. */
    static final Set<String> RETIRED_MISSIONS = Set.of(
            "mushroom:lv30", "mushroom:lv60", "mushroom:lv90", "mushroom:lv100", "mushroom:lv120", "mushroom:lv130",
            "mushroom:lv150", "mushroom:lv170", "mushroom:lv190", "mushroom:lv200", "mushroom:lv210", "mushroom:lv220",
            "mushroom:lv230", "mushroom:lv240", "mushroom:lv250", "mushroom:lv260");

    private RevivalHubDefinitions() {
    }

    /** True for a mission id that existed in launcherapi8's revival hubs and was removed on purpose. */
    public static boolean retired(String eventKey, String missionId) {
        return RETIRED_MISSIONS.contains(eventKey + ":" + missionId);
    }

    static Reward r(int id, int q) {
        return new Reward(id, q);
    }

    static void registerAll() {
        HubCatalog.register(mushroom());
        HubCatalog.register(lacheln());
        HubCatalog.register(mapleTree());
        HubCatalog.register(circus());
        HubCatalog.register(restaurant());
        HubCatalog.register(edentisk());
    }

    /** Final forms of the weekly bosses counted by the "defeat a boss" missions (282 names checked, docs/revival). */
    static final Set<Integer> BOSSES = Set.of(8800102, 8930000, 8930100, 8500022, 8240098, 8240099, 8240104, 8880711,
            8880150, 8880151, 8880153, 8880155, 8880301, 8880302, 8880303, 8645009, 8645066, 8880100, 8880101, 8644650, 8644655);
    static final Set<Integer> LUCID = Set.of(8880150, 8880151, 8880153, 8880155);

    static Offer o(int item, int qty, int coin, int cost) {
        return new Offer(item, qty, coin, cost);
    }

    /** 復刻 5：夢中的拉契爾恩／午夜派對 (2024/6/26～9/10). Coins 4310343 夜之碎片 / 4310344 舞之碎片 / 4310342 夢之碎片. */
    static HubDefinition lacheln() {
        int night = 4310343, dance = 4310344, dream = 4310342;
        return new HubDefinition("lacheln", "夢中的拉契爾恩", 9063135, List.of(
                Mission.killRange("d1", "擊敗等級範圍怪物 1000 隻", 1000, 101, Period.DAILY, r(night, 30)),
                Mission.killRange("d2", "擊敗等級範圍怪物 3000 隻", 3000, 101, Period.DAILY, r(night, 50), r(dream, 10)),
                Mission.killMobs("w1", "擊敗露希妲", 1, LUCID, 220, Period.WEEKLY, r(dance, 50)),
                Mission.killRange("w2", "擊敗等級範圍怪物 20000 隻", 20000, 101, Period.WEEKLY, r(dream, 40), r(dance, 20))),
                List.of(o(EXP2X, 1, night, 60), o(2436024, 1, night, 300), o(3019151, 1, night, 500),
                        o(1143344, 1, dance, 200), o(3018965, 1, dance, 300), o(3018966, 1, dance, 300), o(1702715, 1, dance, 400),
                        o(1001117, 1, dream, 150), o(1007058, 1, dream, 200), o(1054678, 1, dream, 300), o(3015633, 1, dream, 400)));
    }

    /** 復刻 6：楓之樹 (2023/9/6～10/17). Coins 4310325 回憶憑證 / 4310326 挑戰憑證. */
    static HubDefinition mapleTree() {
        int memory = 4310325, challenge = 4310326;
        return new HubDefinition("mapletree", "楓之樹", 9062645, List.of(
                Mission.killRange("d1", "擊敗等級範圍怪物 1000 隻", 1000, 101, Period.DAILY, r(memory, 100)),
                Mission.killRange("d2", "擊敗等級範圍怪物 3000 隻", 3000, 101, Period.DAILY, r(memory, 200)),
                Mission.killMobs("w1", "擊敗任一週頭目 3 次", 3, BOSSES, 200, Period.WEEKLY, r(challenge, 30)),
                Mission.killRange("w2", "擊敗等級範圍怪物 20000 隻", 20000, 101, Period.WEEKLY, r(memory, 1000), r(challenge, 10))),
                List.of(o(EXP2X, 1, memory, 300), o(1000120, 1, memory, 1500), o(1050644, 1, memory, 1500), o(1051717, 1, memory, 1500),
                        o(1070146, 1, memory, 1000), o(1071162, 1, memory, 1000), o(1103461, 1, memory, 2000), o(3018854, 1, memory, 2500),
                        o(1143266, 1, challenge, 30), o(EXP70, 1, challenge, 10)));
    }

    /** 復刻 8：星夜馬戲團（2024 武陵篇，V274 2024/10/30～11/26）. Coins 4310331 幻想硬幣 / 4310297 夢想硬幣. */
    static HubDefinition circus() {
        int fantasy = 4310331, dream = 4310297;
        return new HubDefinition("circus", "星夜馬戲團", 9401996, List.of(
                Mission.killRange("d1", "擊敗等級範圍怪物 1500 隻", 1500, 101, Period.DAILY, r(fantasy, 20)),
                Mission.killRange("d2", "擊敗等級範圍怪物 4000 隻", 4000, 101, Period.DAILY, r(fantasy, 30), r(dream, 5)),
                Mission.killMobs("w1", "擊敗任一週頭目 5 次", 5, BOSSES, 200, Period.WEEKLY, r(dream, 30)),
                Mission.level("lv200", "達到 200 級", 200, r(dream, 20)),
                Mission.level("lv250", "達到 250 級", 250, r(dream, 40))),
                List.of(o(EXP2X, 1, fantasy, 40), o(1006930, 1, fantasy, 200), o(1050841, 1, fantasy, 300), o(1051921, 1, fantasy, 300),
                        o(1074249, 1, fantasy, 200), o(1103929, 1, fantasy, 300), o(2635018, 1, dream, 60), o(2638385, 1, dream, 80),
                        o(2672323, 1, dream, 100)));
    }

    /** 復刻 9：溫餐廳 (2023/6/20～9/5). Coins 4310332 溫餐廳硬幣 / 4310333 溫餐廳 BOSS 硬幣. */
    static HubDefinition restaurant() {
        int coin = 4310332, boss = 4310333;
        return new HubDefinition("restaurant", "溫餐廳", 9062797, List.of(
                Mission.killRange("d1", "今日菜單：擊敗等級範圍怪物 1000 隻", 1000, 101, Period.DAILY, r(coin, 10)),
                Mission.killRange("d2", "今日菜單：擊敗等級範圍怪物 2000 隻", 2000, 101, Period.DAILY, r(coin, 15)),
                Mission.killRange("d3", "今日菜單：擊敗等級範圍怪物 3000 隻", 3000, 101, Period.DAILY, r(coin, 20)),
                Mission.killMobs("w1", "BOSS 菜單：擊敗任一週頭目", 1, BOSSES, 160, Period.WEEKLY, r(boss, 10)),
                Mission.killMobs("w2", "BOSS 菜單：擊敗任一週頭目 5 次", 5, BOSSES, 160, Period.WEEKLY, r(boss, 30))),
                List.of(o(2024152, 1, coin, 20), o(2024153, 1, coin, 20), o(EXP2X, 1, coin, 30), o(1005629, 1, coin, 150),
                        o(1053689, 1, coin, 200), o(1103380, 1, coin, 250), o(3018999, 1, coin, 300), o(3019018, 1, coin, 400),
                        o(3700729, 1, boss, 60), o(1662168, 1, boss, 120)));
    }

    /** 復刻 10：伊甸提斯克 (2024/1/10～4/1). Coins 4310339 陽光硬幣 / 4310340 風之硬幣. */
    static HubDefinition edentisk() {
        int sun = 4310339, wind = 4310340;
        return new HubDefinition("edentisk", "伊甸提斯克", 9062962, List.of(
                Mission.killRange("d1", "慶典準備：擊敗等級範圍怪物 1000 隻", 1000, 101, Period.DAILY, r(sun, 30)),
                Mission.killRange("d2", "慶典準備：擊敗等級範圍怪物 3000 隻", 3000, 101, Period.DAILY, r(sun, 50)),
                Mission.killRange("w1", "風之硬幣：擊敗等級範圍怪物 15000 隻", 15000, 101, Period.WEEKLY, r(wind, 30)),
                Mission.killMobs("w2", "風之硬幣：擊敗任一週頭目 3 次", 3, BOSSES, 200, Period.WEEKLY, r(wind, 20))),
                List.of(o(EXP2X, 1, sun, 60), o(3019088, 1, sun, 400), o(3019096, 1, sun, 500), o(2636300, 1, sun, 300),
                        o(1143314, 1, wind, 30), o(1143315, 1, wind, 30), o(1143316, 1, wind, 30), o(1143317, 1, wind, 30),
                        o(EXP70, 2, wind, 10)));
    }

    /**
     * 復刻 4：究極菇菇通行證 — solo boss kills only (QI 68393..68403). The level checkpoints (30..260, receipt
     * {@code rv_mushroom:once:lv<n>}) were retired at the launcherapi9 merge: content-1005's client window
     * (MushroomPassService, data/tms282-mushroompass/season.json, receipt account_questex claimQuest) is the one level
     * milestone grant path, so a level is never rewarded twice. Their ids stay accepted in revival-rewards.json
     * ({@link #RETIRED_MISSIONS}) so an existing file written by launcherapi8 keeps loading.
     */
    static HubDefinition mushroom() {
        java.util.ArrayList<Mission> m = new java.util.ArrayList<>();
        Object[][] bosses = {
                {"b_zakum", "單人擊敗混沌炎魔", Set.of(8800102)},
                {"b_vellum", "單人擊敗混沌貝倫", Set.of(8930000)},
                {"b_papu", "單人擊敗混沌拉圖斯", Set.of(8500022)},
                {"b_lotus", "單人擊敗史烏", Set.of(8240098, 8240099, 8240104)},
                {"b_slime", "單人擊敗守護天使綠水靈", Set.of(8880711)},
                {"b_lucid", "單人擊敗露希妲", Set.of(8880150, 8880151, 8880153, 8880155)},
                {"b_will", "單人擊敗威爾", Set.of(8880301, 8880302, 8880303)},
                {"b_darknl", "單人擊敗頓凱爾", Set.of(8645009, 8645066)},
                {"b_damien", "單人擊敗困難戴米安", Set.of(8880101)},
                {"b_dusk", "單人擊敗戴斯克", Set.of(8644650, 8644655)},
        };
        for (Object[] b : bosses) {
            @SuppressWarnings("unchecked") Set<Integer> mobs = (Set<Integer>) b[2];
            m.add(Mission.soloBoss((String) b[0], (String) b[1], mobs, 200, Period.ONCE, r(EXP2X, 2), r(EXP70, 2)));
        }
        return new HubDefinition("mushroom", "究極菇菇通行證", 9401434, m, List.<Offer>of());
    }
}
