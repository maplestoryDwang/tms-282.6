package net.swordie.ms.console.admin;

import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 「問題回報」：玩家在登入器按「回報問題」送來的內容（登入器 1.2.8）。上面是列表，選一列看全文：玩家寫的話、登入器版本與模式、
 * 遊戲最後一次關閉的原因、當時在線的角色、伺服器那 30 分鐘記下的技能問題，以及登入器紀錄的最後一段。
 */
final class ReportsTab implements AdminTab {
    private final AdminConsoleContext ctx;
    private final JPanel root = AdminUi.panel(new BorderLayout(8, 8));
    private final JTable table = AdminUi.table(new String[]{"時間", "帳號", "模式", "版本", "遊戲關閉原因", "技能問題", "內容"},
            new int[]{110, 110, 60, 60, 110, 70, 420});
    private final JTextArea detail = new JTextArea(14, 80);
    private final List<String> ids = new ArrayList<>();
    private String shown = "";

    ReportsTab(AdminConsoleContext ctx) {
        this.ctx = ctx;
        JButton reload = AdminUi.button("重新整理");
        reload.addActionListener(e -> refresh());
        root.add(AdminUi.row(reload, AdminUi.hint("玩家在登入器的「回報問題」送出的內容；最多保留最近 300 筆（logs\\launcher-reports）。")), BorderLayout.NORTH);
        detail.setEditable(false);
        detail.setLineWrap(true);
        detail.setFont(AdminUi.font(Font.PLAIN, 12));
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (!e.getValueIsAdjusting() && row >= 0 && row < ids.size() && !ids.get(row).equals(shown)) {
                open(ids.get(row));
            }
        });
        JPanel body = AdminUi.panel(new GridLayout(2, 1, 0, 8));
        body.add(AdminUi.section("回報列表（最新的在最上面）", AdminUi.scroll(table)));
        body.add(AdminUi.section("選取那一筆的全文", AdminUi.scroll(detail)));
        root.add(body, BorderLayout.CENTER);
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    @Override
    public String title() {
        return "問題回報";
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public void refresh() {
        ctx.call("ops.reports", Map.of("limit", 100), this::show);
    }

    void show(JsonNode r) {
        if (!r.path("ok").asBoolean()) {
            ctx.status("問題回報：" + AdminText.failure(r));
            return;
        }
        List<Object[]> rows = new ArrayList<>();
        ids.clear();
        for (JsonNode n : r.path("reports")) {
            ids.add(n.path("id").asText());
            String who = n.path("username").asText("");
            rows.add(new Object[]{AdminUi.time(n.path("time")), who.isEmpty() ? "#" + n.path("user").asInt() : who,
                    mode(n.path("mode").asText()), n.path("version").asText(), n.path("exit").asText(),
                    n.path("issues").asInt() == 0 ? "" : n.path("issues").asText(), n.path("text").asText().replace('\n', ' ')});
        }
        AdminUi.rows(table, rows);
        ctx.status("問題回報：" + rows.size() + " 筆。");
    }

    private static String mode(String m) {
        return "local".equals(m) ? "本機" : "remote".equals(m) ? "遠端" : m;
    }

    private void open(String id) {
        ctx.call("ops.report", Map.of("id", id), r -> {
            if (!r.path("ok").asBoolean()) {
                detail.setText("讀不到這筆回報：" + AdminText.failure(r));
                return;
            }
            shown = id;
            detail.setText(format(r.path("report")));
            detail.setCaretPosition(0);
        });
    }

    /** The stored report as readable text. */
    static String format(JsonNode n) {
        StringBuilder b = new StringBuilder();
        b.append("時間：").append(AdminUi.dateTime(n.path("time"))).append("　帳號：").append(n.path("username").asText("")).append("（#")
                .append(n.path("user").asInt()).append("）　來源：").append(n.path("ip").asText()).append('\n');
        b.append("玩家寫的：\n").append(n.path("text").asText()).append("\n\n");
        JsonNode c = n.path("client");
        b.append("登入器：版本 ").append(c.path("version").asText("?")).append("、").append(mode(c.path("mode").asText("?")))
                .append("模式、遊戲").append(c.path("gameRunning").asBoolean() ? "開著" : "沒開").append('\n');
        JsonNode exit = c.path("lastExit");
        if (exit.isObject()) {
            b.append("遊戲最後一次關閉：").append(AdminUi.dateTime(exit.path("at"))).append("　原因 ").append(exit.path("reason").asText())
                    .append(exit.path("playedSec").isNumber() ? "　玩了 " + AdminUi.duration(exit.path("playedSec").asLong()) : "").append('\n');
        }
        b.append("\n當時在線的角色：");
        if (n.path("characters").isEmpty()) {
            b.append("（沒有）");
        }
        for (JsonNode ch : n.path("characters")) {
            b.append("\n  ").append(ch.path("name").asText()).append("（").append(ch.path("id").asInt()).append("）職業 ")
                    .append(ch.path("job").asInt()).append("・").append(ch.path("level").asInt()).append(" 等・地圖 ").append(ch.path("field").asInt());
        }
        b.append("\n\n伺服器前 30 分鐘記下的技能問題：");
        if (n.path("issues").isEmpty()) {
            b.append("（沒有）");
        }
        for (JsonNode i : n.path("issues")) {
            b.append("\n  [").append(i.path("kind").asText()).append("] 技能 ").append(i.path("skill").asInt()).append(' ')
                    .append(i.path("subject").asText()).append("：").append(i.path("reason").asText()).append(" ×").append(i.path("count").asInt());
        }
        b.append("\n\n登入器紀錄（最後一段）：\n").append(n.path("log").asText());
        return b.toString();
    }
}
