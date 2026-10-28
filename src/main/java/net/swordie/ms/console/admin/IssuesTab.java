package net.swordie.ms.console.admin;

import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 「技能問題」：伺服器自己記下的問題（技能被拒絕、封包被擋、判定未過、狀態未送出、沒有效果…），依種類／角色／技能
 * 分組。測試前按「清空」，測完直接看這裡，不用翻日誌。
 */
final class IssuesTab implements AdminTab {
    private final AdminConsoleContext ctx;
    private final JPanel root = AdminUi.panel(new BorderLayout(8, 8));
    private final JTextField minutes = AdminUi.field(5);
    private final JTextField character = AdminUi.field(8);
    private final JLabel summary = AdminUi.value();
    private final JTable table = AdminUi.table(new String[]{"最後發生", "種類", "角色", "技能", "對象", "原因", "次數"},
            new int[]{110, 100, 60, 90, 190, 330, 50});
    private final JTextArea sample = new JTextArea(4, 60);
    private final List<String> samples = new ArrayList<>();

    IssuesTab(AdminConsoleContext ctx) {
        this.ctx = ctx;
        JButton reload = AdminUi.button("重新整理");
        reload.addActionListener(e -> refresh());
        JButton clear = AdminUi.dangerButton("清空（開始新的測試）");
        clear.addActionListener(e -> clear());
        root.add(AdminUi.row(AdminUi.label("最近幾分鐘（空白＝全部）："), minutes, AdminUi.label("角色 ID（空白＝全部）："), character,
                reload, clear, summary), BorderLayout.NORTH);
        sample.setEditable(false);
        sample.setLineWrap(true);
        sample.setFont(AdminUi.font(Font.PLAIN, 12));
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            sample.setText(row >= 0 && row < samples.size() ? samples.get(row) : "");
            sample.setCaretPosition(0);
        });
        JPanel center = AdminUi.panel(new BorderLayout(0, 6));
        center.add(AdminUi.scroll(table), BorderLayout.CENTER);
        center.add(AdminUi.section("選取那一列最後一次的原始紀錄", AdminUi.scroll(sample)), BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);
        root.add(AdminUi.hint("「沒有效果」＝伺服器接受了技能但沒有送出狀態，攻擊技能出現在這裡是正常的；「封包被擋」＝282 還沒有對應格式。"),
                BorderLayout.SOUTH);
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    @Override
    public String title() {
        return "技能問題";
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public boolean autoRefresh() {
        return true;
    }

    @Override
    public void refresh() {
        Map<String, Object> params = new HashMap<>();
        Long m = number(minutes.getText());
        Long c = number(character.getText());
        if (m != null) {
            params.put("minutes", m);
        }
        if (c != null) {
            params.put("character", c);
        }
        ctx.call("ops.issues", params, this::show);
    }

    private static Long number(String text) {
        try {
            return text == null || text.isBlank() ? null : Long.valueOf(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    void show(JsonNode r) {
        if (!r.path("ok").asBoolean()) {
            ctx.status("技能問題：" + AdminText.failure(r));
            return;
        }
        int keep = table.getSelectedRow();
        List<Object[]> rows = new ArrayList<>();
        samples.clear();
        long total = 0;
        for (JsonNode i : r.path("issues")) {
            rows.add(new Object[]{AdminUi.time(i.path("last")), i.path("kind").asText(),
                    i.path("character").asInt() == 0 ? "" : i.path("character").asText(),
                    i.path("skill").asInt() == 0 ? "" : i.path("skill").asText(),
                    i.path("subject").asText(), i.path("reason").asText(), i.path("count").asInt()});
            samples.add(i.path("sample").asText());
            total += i.path("count").asLong();
        }
        AdminUi.rows(table, rows);
        if (keep >= 0 && keep < table.getRowCount()) {
            table.setRowSelectionInterval(keep, keep);
        }
        summary.setText(rows.size() + " 組／" + total + " 次（上次清空：" + AdminUi.time(r.path("clearedAt")) + "）");
    }

    private void clear() {
        if (!ctx.confirm("清空技能問題", "要清空目前累積的問題紀錄嗎？（只清主控台看到的統計，不影響日誌檔）")) {
            return;
        }
        ctx.call("ops.issuesClear", r -> {
            ctx.status(r.path("ok").asBoolean() ? "已清空技能問題。" : "清空：" + AdminText.failure(r));
            refresh();
        });
    }
}
