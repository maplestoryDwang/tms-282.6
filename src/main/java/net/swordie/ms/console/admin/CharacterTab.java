package net.swordie.ms.console.admin;

import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 「角色」：查一個在線角色的狀態（職業、等級、地圖、HP/MP、技能、冷卻中的技能、帳號的連結技能），並提供常用修補：
 * 送回村莊（卡在活動地圖）、清除冷卻。只對在線角色有效。
 */
final class CharacterTab implements AdminTab {
    private final AdminConsoleContext ctx;
    private final JPanel root = AdminUi.panel(new BorderLayout(8, 8));
    private final JTextField query = AdminUi.field(16);
    private final JLabel who = AdminUi.value();
    private final JLabel where = AdminUi.value();
    private final JLabel vitals = AdminUi.value();
    private final JTable skills = AdminUi.table(new String[]{"技能 ID", "等級", "上限"}, new int[]{110, 50, 50});
    private final JTable cooldowns = AdminUi.table(new String[]{"冷卻中的技能", "剩餘秒數"}, new int[]{130, 80});
    private final JTable links = AdminUi.table(new String[]{"來源角色", "連結技能", "等級", "傳授給"}, new int[]{80, 110, 50, 80});
    private final JTextField kitName = AdminUi.field(14);
    private final JCheckBox kitLevel = new JCheckBox("同時設定等級、職業、技能全滿", true);
    private String shown = "";

    CharacterTab(AdminConsoleContext ctx) {
        this.ctx = ctx;
        JButton find = AdminUi.button("查詢");
        find.addActionListener(e -> inspect(query.getText()));
        query.addActionListener(e -> inspect(query.getText()));
        JButton home = AdminUi.button("送回村莊");
        home.addActionListener(e -> repair("char.home", "送回村莊", "要把這個角色送回弓箭手村嗎？"));
        JButton cool = AdminUi.button("清除冷卻");
        cool.addActionListener(e -> repair("char.cooldowns", "清除冷卻", "要清除這個角色所有冷卻中的技能嗎？"));
        JPanel top = AdminUi.panel(new GridLayout(3, 1, 0, 4));
        top.add(AdminUi.row(AdminUi.label("在線角色的 ID 或名稱："), query, find, home, cool));
        // 複製官方角色（MapleKit 的角色資料 → 外觀、裝備含星力／卷軸／火花／潛能、時裝）到上面查到的在線角色
        JButton kit = AdminUi.dangerButton("複製到這個角色");
        kit.addActionListener(e -> kit());
        kitLevel.setOpaque(false);
        kitLevel.setFont(AdminUi.font(Font.PLAIN, 12));
        top.add(AdminUi.row(AdminUi.label("複製官方角色（輸入官方角色名稱）："), kitName, kitLevel, kit));
        JPanel stats = AdminUi.panel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        stats.add(AdminUi.stat("角色", who));
        stats.add(AdminUi.stat("位置", where));
        stats.add(AdminUi.stat("HP／MP", vitals));
        top.add(stats);
        root.add(top, BorderLayout.NORTH);

        JPanel right = AdminUi.panel(new GridLayout(2, 1, 0, 8));
        right.add(AdminUi.section("冷卻中的技能", AdminUi.scroll(cooldowns)));
        right.add(AdminUi.section("帳號的連結技能紀錄", AdminUi.scroll(links)));
        JPanel center = AdminUi.panel(new GridLayout(1, 2, 8, 0));
        center.add(AdminUi.section("技能", AdminUi.scroll(skills)));
        center.add(right);
        root.add(center, BorderLayout.CENTER);
        root.add(AdminUi.hint("只查得到在線角色（角色下線後請用「帳號」分頁）。修補動作會寫進管理紀錄。"), BorderLayout.SOUTH);
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    @Override
    public String title() {
        return "角色";
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public void refresh() {
        if (!shown.isEmpty()) {
            inspect(shown);
        }
    }

    private void inspect(String text) {
        String q = text == null ? "" : text.trim();
        if (q.isEmpty()) {
            return;
        }
        ctx.call("char.inspect", Map.of("character", q), r -> show(q, r));
    }

    void show(String q, JsonNode r) {
        if (!r.path("ok").asBoolean()) {
            shown = "";
            who.setText("-");
            where.setText("-");
            vitals.setText("-");
            AdminUi.rows(skills, List.of());
            AdminUi.rows(cooldowns, List.of());
            AdminUi.rows(links, List.of());
            ctx.status("角色「" + q + "」：" + ("notFound".equals(r.path("code").asText()) ? "不在線上或不存在" : AdminText.failure(r)));
            return;
        }
        JsonNode c = r.path("character");
        shown = c.path("id").asText();
        who.setText(c.path("name").asText() + "（" + c.path("id").asInt() + "）職業 " + c.path("job").asInt() + "・" + c.path("level").asInt() + " 等");
        where.setText("地圖 " + c.path("field").asInt() + "・頻道 " + c.path("channel").asInt());
        vitals.setText(r.path("hp").asInt() + "/" + r.path("maxHp").asInt() + "・" + r.path("mp").asInt() + "/" + r.path("maxMp").asInt());
        List<Object[]> s = new ArrayList<>();
        for (JsonNode k : r.path("skills")) {
            s.add(new Object[]{k.path("id").asInt(), k.path("level").asInt(), k.path("master").asInt()});
        }
        AdminUi.rows(skills, s);
        List<Object[]> cd = new ArrayList<>();
        for (JsonNode k : r.path("cooldowns")) {
            cd.add(new Object[]{k.path("skill").asInt(), k.path("leftSec").asLong()});
        }
        AdminUi.rows(cooldowns, cd);
        List<Object[]> l = new ArrayList<>();
        for (JsonNode k : r.path("linkSkills")) {
            l.add(new Object[]{k.path("owner").asInt(), k.path("skill").asInt(), k.path("level").asInt(),
                    k.path("linkedTo").asInt() == 0 ? "" : k.path("linkedTo").asText()});
        }
        AdminUi.rows(links, l);
        ctx.status("角色 " + c.path("name").asText() + "：" + s.size() + " 個技能、" + cd.size() + " 個冷卻中。");
    }

    /** Downloads the official character from MapleKit into data\kit-sources, shows what it holds, then applies it. */
    private void kit() {
        String official = kitName.getText().trim();
        if (shown.isEmpty()) {
            ctx.info("複製官方角色", "請先在上面查詢一個在線角色（要套用的對象）。");
            return;
        }
        if (official.isEmpty() || official.length() > 24) {
            ctx.info("複製官方角色", "請輸入官方角色的名稱。");
            return;
        }
        String target = shown;
        String file = "kit_" + Long.toString(System.currentTimeMillis(), 36);
        ctx.status("正在下載「" + official + "」的資料…");
        ctx.run(() -> KitDownload.fetch(official, java.nio.file.Path.of(System.getProperty(DeployTab.HOME_KEY, "")).toAbsolutePath(), file), bytes ->
                ctx.call("char.kit", Map.of("file", file, "preview", true), p -> {
                    if (!p.path("ok").asBoolean()) {
                        ctx.error("複製官方角色", "讀不懂下載到的資料：" + AdminText.failure(p));
                        return;
                    }
                    JsonNode k = p.path("kit");
                    String question = "來源：" + k.path("source").asText() + "（" + k.path("level").asInt() + " 等" + (k.path("job").asInt() == 0 ? "，職業對不到、不會改職業" : "，職業 " + k.path("job").asInt()) + "，" + k.path("gender").asText("?") + "性）\n"
                            + "（請用同性別的角色：性別不同時，選角畫面和快速登入不會顯示髮型和臉型）\n"
                            + "一般裝備 " + k.path("equips").asInt() + " 件、時裝 " + k.path("cash").asInt() + " 件"
                            + (k.path("skipped").isEmpty() ? "" : "；這個版本沒有資料、會略過 " + k.path("skipped").size() + " 件")
                            + (k.path("unmappedPotentials").asInt() == 0 ? "" : "；有 " + k.path("unmappedPotentials").asInt() + " 條潛能對不到、會留空") + "\n\n"
                            + "要套用到在線角色 " + target + " 嗎？他身上的裝備會被換下來放回背包。\n請輸入這個角色的 ID 確認：";
                    if (!ctx.typed("複製官方角色", question, target)) {
                        ctx.status("已取消複製。");
                        return;
                    }
                    ctx.call("char.kit", Map.of("file", file, "character", target, "levelAndJob", kitLevel.isSelected()), r -> {
                        if (!r.path("ok").asBoolean()) {
                            ctx.error("複製官方角色", "沒有成功：" + AdminText.failure(r));
                            return;
                        }
                        StringBuilder b = new StringBuilder("完成：\n");
                        r.path("done").forEach(d -> b.append("  ").append(d.asText()).append('\n'));
                        if (!r.path("failed").isEmpty()) {
                            b.append("\n沒有成功的：\n");
                            r.path("failed").forEach(d -> b.append("  ").append(d.asText()).append('\n'));
                        }
                        ctx.info("複製官方角色", b.toString());
                        inspect(target);
                    });
                }), e -> ctx.error("複製官方角色", "下載失敗：" + e.getMessage()));
    }

    private void repair(String action, String title, String question) {
        if (shown.isEmpty()) {
            ctx.info(title, "請先查詢一個在線角色。");
            return;
        }
        if (!ctx.confirm(title, question)) {
            return;
        }
        String id = shown;
        ctx.call(action, Map.of("character", id), r -> {
            ctx.status(r.path("ok").asBoolean() ? title + "：完成（角色 " + id + "）。" : title + "：" + AdminText.failure(r));
            inspect(id);
        });
    }
}
