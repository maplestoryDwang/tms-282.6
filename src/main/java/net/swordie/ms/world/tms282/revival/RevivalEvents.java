package net.swordie.ms.world.tms282.revival;

import java.time.LocalDate;
import java.util.*;

import static net.swordie.ms.ServerConfig.ZONE;

/**
 * Registry of the revived legacy events (handler registry: one {@link RevivalEvent} per key, in panel order) and the
 * facade scripts and commands use. Whether an event is open comes from {@link RevivalConfigSource}.
 */
public final class RevivalEvents {
    private static final Map<String, RevivalEvent> EVENTS = new LinkedHashMap<>();

    static {
        register(new SimpleRevivalEvent(new RevivalEvent.Spec("sunday", "星期天楓之谷",
                "每週日的楓之谷優惠（怪物公園經驗值加成），依星期天日曆自動顯示", 0, 0, 0)));
        register(new SimpleRevivalEvent(new RevivalEvent.Spec("punch", "世界最強拳擊王",
                "125 秒內攻擊拳擊王機器，比總傷害；每天第一次挑戰依擊破台數發獎勵，NPC 可看本週排行", 993222000, 0, 101),
                List.of(993222000, 993222001, 993222100, 993222200), List.of(9062764, 9062765)));
        register(new SimpleRevivalEvent(new RevivalEvent.Spec("mushroom", "究極菇菇通行證",
                "升級到指定等級、單人擊敗指定頭目都可以向茱妮領獎（每個帳號各一次）", 100000000, 0, 0),
                List.of(100000000, 240000000), List.of(9401434)));
        register(hubEvent("lacheln", "夢中的拉契爾恩", "露希妲主題活動：每日、每週任務收集夜之、舞之、夢之碎片，到三間碎片商店兌換",
                993236200, List.of(9063135, 9063164, 9063141, 9063162, 9063145), 993238000, 993238001, 993238100, 993238200));
        register(hubEvent("mapletree", "楓之樹", "收集回憶憑證、挑戰憑證，在海爾曼與凱尼爾的商店兌換楓之樹系列",
                993200000, List.of(9062645, 9062653, 9062657)));
        register(hubEvent("circus", "星夜馬戲團", "武陵篇：狩獵與頭目任務收集幻想硬幣、夢想硬幣，向韓泰秀兌換慶典獎勵",
                867168000, List.of(9401996, 9402010, 9402009), 867168300));
        register(hubEvent("restaurant", "溫餐廳", "完成溫莉的今日菜單與 BOSS 菜單，用溫餐廳硬幣兌換料理與家具",
                993223000, List.of(9062797, 9062813, 9062815)));
        register(hubEvent("edentisk", "伊甸提斯克", "準備慶典的每日狩獵與每週任務，收集陽光硬幣、風之硬幣兌換",
                993228000, List.of(9062962, 9062968, 9062972)));
        RevivalHubDefinitions.registerAll();
    }

    private RevivalEvents() {
    }

    private static RevivalEvent hubEvent(String key, String title, String summary, int map, List<Integer> npcs, int... extraMaps) {
        List<Integer> maps = new java.util.ArrayList<>(List.of(map));
        for (int m : extraMaps) maps.add(m);
        return new SimpleRevivalEvent(new RevivalEvent.Spec(key, title, summary, map, 0, 101), maps, npcs);
    }

    static synchronized void register(RevivalEvent event) {
        String key = event.spec().key();
        if (EVENTS.putIfAbsent(key, event) != null) throw new IllegalStateException("duplicate revival event " + key);
    }

    public static synchronized List<RevivalEvent> all() {
        return List.copyOf(EVENTS.values());
    }

    public static synchronized Optional<RevivalEvent> find(String key) {
        return Optional.ofNullable(EVENTS.get(key));
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public static boolean isOpen(String key) {
        return isOpen(key, today());
    }

    public static boolean isOpen(String key, LocalDate day) {
        return find(key).isPresent() && RevivalConfigSource.shared().current().period(key).openOn(day);
    }

    /** Open events, in panel order. */
    public static List<RevivalEvent> open() {
        LocalDate day = today();
        List<RevivalEvent> out = new ArrayList<>();
        for (RevivalEvent e : all()) if (isOpen(e.spec().key(), day)) out.add(e);
        return out;
    }

    public static RevivalEvent.EntryResult enter(String key, RevivalPlayer player) {
        RevivalEvent e = find(key).orElse(null);
        if (e == null || !isOpen(key)) return RevivalEvent.EntryResult.CLOSED;
        return e.enter(player);
    }

    public static String describe(RevivalEvent.EntryResult r) {
        return switch (r) {
            case WARPED -> "已傳送到活動地圖。";
            case NO_ENTRY_MAP -> "這個活動不需要傳送，請直接找活動 NPC。";
            case CLOSED -> "這個活動目前沒有開放。";
            case LEVEL -> "等級不足，無法參加這個活動。";
            case MAP_MISSING -> "活動地圖不存在，請回報管理員。";
            case BUSY -> "請先離開目前的副本或活動再進入。";
        };
    }
}
