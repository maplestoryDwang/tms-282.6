package net.swordie.ms.console.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.account.WorldDirectory;
import net.swordie.ms.console.ConsoleUiStyle;

import javax.swing.*;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 「簽到」：28 天獎勵表（唯讀，daily-gift.tsv）；查某帳號本月各世界的簽到日期（dailygift_log）。 */
final class DailyTab implements AdminTab {
    private static final WorldDirectory WORLDS = new WorldDirectory(null);
    private static final String[] WEEK = {"一", "二", "三", "四", "五", "六", "日"};

    private final AdminConsoleContext ctx;
    private final JPanel root = AdminUi.panel(new BorderLayout(8, 8));
    private final JTable rewards = AdminUi.table(new String[]{"天", "道具 ID", "道具名稱", "數量", "期限"}, new int[]{35, 75, 230, 45, 70});
    private final JLabel rewardsInfo = AdminUi.hint(" ");
    private final JTextField query = AdminUi.field(16);
    private final JPanel worlds = AdminUi.panel(null);
    private boolean rewardsLoaded;
    private String baseVersion = "";
    private List<Map<String, Object>> rewardRows = new ArrayList<>();
    private final JTextField itemId = AdminUi.field(8), count = AdminUi.field(5), period = AdminUi.field(4);
    private final JLabel editDay = AdminUi.label("選擇一天修改");
    private final JButton save = AdminUi.button("儲存這一天");

    DailyTab(AdminConsoleContext ctx) {
        this.ctx = ctx;
        JPanel left = AdminUi.panel(new BorderLayout(0, 4));
        left.add(AdminUi.scroll(rewards), BorderLayout.CENTER);
        left.add(rewardsInfo, BorderLayout.SOUTH);
        JPanel editor = AdminUi.panel(new GridLayout(3, 1, 0, 4));
        editor.add(AdminUi.row(editDay, AdminUi.label("道具 ID"), itemId));
        editor.add(AdminUi.row(AdminUi.label("數量"), count, AdminUi.label("期限（天；0 永久）"), period));
        editor.add(AdminUi.row(save, AdminUi.hint("儲存後同步遊戲與登入器")));
        left.add(editor, BorderLayout.NORTH);
        save.setEnabled(false);
        rewards.getSelectionModel().addListSelectionListener(e -> {
            int selected = rewards.getSelectedRow();
            if (e.getValueIsAdjusting() || selected < 0 || selected >= rewardRows.size()) return;
            var row = rewardRows.get(rewards.convertRowIndexToModel(selected));
            editDay.setText("第 " + row.get("day") + " 天");
            itemId.setText(String.valueOf(row.get("itemId")));
            count.setText(String.valueOf(row.get("count")));
            period.setText(String.valueOf(row.get("periodDays")));
            save.setEnabled(true);
        });
        save.addActionListener(e -> saveReward());
        JPanel leftWrap = AdminUi.section("28 天獎勵表", left);
        leftWrap.setPreferredSize(new Dimension(510, 100));

        worlds.setLayout(new BoxLayout(worlds, BoxLayout.Y_AXIS));
        JButton find = AdminUi.button("查詢本月簽到");
        find.addActionListener(e -> lookup());
        query.addActionListener(e -> lookup());
        JPanel right = AdminUi.panel(new BorderLayout(0, 6));
        right.add(AdminUi.row(AdminUi.label("帳號 ID／名稱／角色名："), query, find), BorderLayout.NORTH);
        JScrollPane ws = AdminUi.scroll(worlds);
        ws.getVerticalScrollBar().setUnitIncrement(16);
        right.add(ws, BorderLayout.CENTER);

        root.add(leftWrap, BorderLayout.WEST);
        root.add(AdminUi.section("帳號本月簽到（dailygift_log）", right), BorderLayout.CENTER);
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    @Override
    public String title() {
        return "簽到";
    }

    @Override
    public JComponent component() {
        return root;
    }

    void query(String q) {
        query.setText(q);
    }

    @Override
    public void refresh() {
        ctx.call("daily.rewards", this::showRewards);
        if (!query.getText().isBlank()) {
            lookup();
        }
    }

    void showRewards(JsonNode r) {
        if (!r.path("ok").asBoolean()) {
            rewardsInfo.setText("讀不到獎勵表：" + AdminText.failure(r));
            return;
        }
        List<Object[]> rows = new ArrayList<>();
        rewardRows = new ArrayList<>();
        for (JsonNode n : r.path("rewards")) {
            int days = n.path("periodDays").asInt();
            rows.add(new Object[]{n.path("day").asInt(), n.path("itemId").asInt(), n.path("name").asText(), n.path("count").asInt(),
                    days == 0 ? "永久" : days + " 天"});
            rewardRows.add(new java.util.LinkedHashMap<>(Map.of("day", n.path("day").asInt(), "itemId", n.path("itemId").asInt(),
                    "count", n.path("count").asInt(), "periodDays", days, "name", n.path("name").asText())));
        }
        AdminUi.rows(rewards, rows);
        rewardsInfo.setText("版本 " + r.path("version").asText() + "（daily-gift.tsv）");
        rewardsInfo.setToolTipText(r.path("file").asText());
        rewardsLoaded = true;
        baseVersion = r.path("version").asText();
        save.setEnabled(false);
    }

    private void saveReward() {
        int selected = rewards.getSelectedRow();
        if (selected < 0) return;
        try {
            var proposed = rewardRows.stream().map(java.util.LinkedHashMap::new).toList();
            var row = proposed.get(rewards.convertRowIndexToModel(selected));
            row.put("itemId", Integer.parseInt(itemId.getText().strip()));
            row.put("count", Integer.parseInt(count.getText().strip()));
            row.put("periodDays", Integer.parseInt(period.getText().strip()));
            save.setEnabled(false);
            ctx.call("daily.save", Map.of("baseVersion", baseVersion, "rewards", proposed), r -> {
                if (r.path("ok").asBoolean()) {
                    ctx.call("daily.rewards", this::showRewards);
                    ctx.status("每日禮物已儲存，遊戲畫面已更新。");
                } else {
                    rewardsInfo.setText("儲存失敗：" + (r.path("code").asText().equals("conflict") ? "設定已被修改，請重新整理。" : r.path("problem").asText(AdminText.failure(r))));
                    save.setEnabled(true);
                }
            });
        } catch (NumberFormatException e) { rewardsInfo.setText("道具 ID、數量、期限請輸入整數。"); }
    }

    private void lookup() {
        String q = query.getText().strip();
        if (q.isEmpty()) {
            return;
        }
        ctx.call("daily.status", Map.of("query", q), this::showStatus);
    }

    void showStatus(JsonNode r) {
        worlds.removeAll();
        if (!r.path("ok").asBoolean()) {
            worlds.add(AdminUi.label(AdminText.failure(r)));
        } else {
            JsonNode a = r.path("account");
            worlds.add(AdminUi.label(a.path("name").asText() + "（帳號 ID " + a.path("id").asInt() + "）"));
            if (r.path("worlds").isEmpty()) {
                worlds.add(AdminUi.hint("這個帳號還沒有任何世界帳號（沒建過角色）。"));
            }
            for (JsonNode w : r.path("worlds")) {
                worlds.add(Box.createVerticalStrut(6));
                worlds.add(world(w));
            }
        }
        worlds.revalidate();
        worlds.repaint();
    }

    private static JComponent world(JsonNode w) {
        JPanel p = AdminUi.panel(new BorderLayout(8, 4));
        p.setBorder(BorderFactory.createCompoundBorder(ConsoleUiStyle.lineBorder(), BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        int id = w.path("world").asInt();
        int claimed = w.path("claimedCount").asInt();
        String src = switch (w.path("source").asText()) {
            case "memory" -> "在線，遊戲記憶體";
            case "database" -> "資料庫";
            default -> "尚無紀錄";
        };
        JsonNode next = w.path("nextReward");
        String nextText = next.isMissingNode() || next.isNull() ? (w.path("monthComplete").asBoolean() ? "本月已全部簽完" : "-")
                : "第 " + next.path("day").asInt() + " 天：" + next.path("name").asText() + " ×" + next.path("count").asInt();
        JPanel text = AdminUi.panel(new GridLayout(0, 1));
        JLabel title = AdminUi.label(WORLDS.name(id) + "（世界 " + id + "）　" + w.path("month").asText());
        title.setFont(AdminUi.font(Font.BOLD, 13));
        text.add(title);
        text.add(AdminUi.label("已簽 " + claimed + " / 28 天　今天：" + (w.path("claimedToday").asBoolean() ? "已簽到" : "還沒簽") + "　（" + src + "）"));
        text.add(AdminUi.label("下一個獎勵：" + nextText));
        int undated = w.path("undatedCount").asInt();
        JsonNode logAvailable = w.path("logAvailable");
        if (logAvailable.isBoolean() && !logAvailable.asBoolean()) {
            text.add(AdminUi.hint("dailygift_log 資料表不存在，無法顯示日期"));
        } else if (undated > 0) {
            text.add(AdminUi.hint("有 " + undated + " 次簽到沒有日期紀錄（dailygift_log 建立之前）"));
        }
        p.add(text, BorderLayout.NORTH);
        Set<LocalDate> dates = new HashSet<>();
        for (JsonNode d : w.path("claimDates")) {
            dates.add(LocalDate.parse(d.asText()));
        }
        p.add(calendar(YearMonth.parse(w.path("month").asText()), dates, LocalDate.parse(w.path("today").asText())), BorderLayout.CENTER);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height));
        return p;
    }

    /** Month grid, Monday first; claimed days filled, today outlined. */
    static JComponent calendar(YearMonth month, Set<LocalDate> claimed, LocalDate today) {
        JPanel grid = AdminUi.panel(new GridLayout(0, 7, 3, 3));
        for (String d : WEEK) {
            JLabel l = AdminUi.hint(d);
            l.setHorizontalAlignment(SwingConstants.CENTER);
            grid.add(l);
        }
        int offset = month.atDay(1).getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
        for (int i = 0; i < offset; i++) {
            grid.add(new JLabel(""));
        }
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate d = month.atDay(day);
            JLabel l = new JLabel(String.valueOf(day), SwingConstants.CENTER);
            l.setFont(AdminUi.font(Font.PLAIN, 12));
            l.setOpaque(true);
            boolean on = claimed.contains(d);
            l.setBackground(on ? new Color(0x4A90D9) : Color.WHITE);
            l.setForeground(on ? Color.WHITE : Color.DARK_GRAY);
            l.setBorder(BorderFactory.createLineBorder(d.equals(today) ? AdminUi.WARN : new Color(0xDDDDDD), d.equals(today) ? 2 : 1));
            l.setPreferredSize(new Dimension(34, 24));
            grid.add(l);
        }
        JPanel wrap = AdminUi.panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrap.add(grid);
        return wrap;
    }
}
