package net.swordie.ms.console.admin;

import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 「上線獎勵」(fixes10): the 累積上線獎勵 settings in use (onlinereward.get: state, file, the six values), one day's
 * per-account minutes and points with totals, and the newest receipts (online_reward_log). The settings can be edited
 * here: 「儲存設定」 sends onlinereward.save (the server checks the values, writes online-reward.json atomically and
 * hot-reloads it; a file changed meanwhile is a conflict). Edits not saved yet keep the tab from reloading by itself.
 */
final class OnlineRewardTab implements AdminTab {
    private final AdminConsoleContext ctx;
    private final JPanel root = AdminUi.panel(new BorderLayout(8, 8));
    private final JLabel state = AdminUi.value();
    private final JLabel accountsToday = AdminUi.value();
    private final JLabel minutesToday = AdminUi.value();
    private final JLabel pointsToday = AdminUi.value();
    private final JLabel file = AdminUi.hint(" ");
    private final JLabel problem = AdminUi.hint(" ");
    private final JCheckBox enabled = new JCheckBox("啟用（關閉 = 不累計、不發放）");
    private final JTextField interval = AdminUi.field(5);
    private final JTextField points = AdminUi.field(5);
    private final JTextField cap = AdminUi.field(6);
    private final JTextField afk = AdminUi.field(5);
    private final JTextField notice = AdminUi.field(48);
    private final JButton save = AdminUi.button("儲存設定");
    private final JLabel saveInfo = AdminUi.hint(" ");
    private final JTextField day = AdminUi.field(10);
    private final JTextField query = AdminUi.field(12);
    private final JTable accounts = AdminUi.table(new String[]{"帳號 ID", "帳號", "今日分鐘", "今日樂豆點", "達上限", "在線", "最後計入"},
            new int[]{70, 150, 80, 90, 60, 50, 120});
    private final JLabel accountsInfo = AdminUi.hint(" ");
    private final JTable log = AdminUi.table(new String[]{"#", "時間", "帳號 ID", "帳號", "日期", "發放", "今日累計", "分鐘", "發放後餘額"},
            new int[]{60, 120, 70, 140, 90, 60, 80, 60, 100});
    private static final String UNSAVED = "有還沒儲存的變更";
    private String baseVersion = "";
    private JsonNode loaded;
    private boolean dirty;
    private boolean filling;

    OnlineRewardTab(AdminConsoleContext ctx) {
        this.ctx = ctx;
        JPanel stats = AdminUi.row(AdminUi.stat("設定", state), AdminUi.stat("今日帳號數", accountsToday), AdminUi.stat("今日總分鐘", minutesToday),
                AdminUi.stat("今日已發樂豆點", pointsToday));
        JButton refresh = AdminUi.button("重新整理");
        refresh.addActionListener(e -> reload());
        save.addActionListener(e -> saveConfig());
        save.setEnabled(false);
        enabled.setFont(AdminUi.font(Font.PLAIN, 13));
        enabled.setOpaque(false);
        for (JTextField f : new JTextField[]{interval, points, cap, afk, notice}) {
            f.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e) { edited(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { edited(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { edited(); }
            });
        }
        enabled.addActionListener(e -> edited());

        JPanel form = AdminUi.panel(new BorderLayout());
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(stats);
        form.add(AdminUi.row(enabled));
        form.add(AdminUi.row(AdminUi.label("每累積"), interval, AdminUi.label("分鐘發"), points, AdminUi.label("樂豆點；每帳號每天上限"), cap,
                AdminUi.label("點；"), afk, AdminUi.label("分鐘沒移動視為掛機（0 = 不判斷）")));
        form.add(AdminUi.row(AdminUi.label("通知文字："), notice, AdminUi.hint("{minutes} {points} {today} {cap}")));
        form.add(AdminUi.row(save, refresh, saveInfo));
        form.add(file);
        form.add(problem);
        for (Component child : form.getComponents()) ((JComponent) child).setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton find = AdminUi.button("查詢");
        find.addActionListener(e -> load());
        query.addActionListener(e -> load());
        day.addActionListener(e -> load());
        JPanel list = AdminUi.panel(new BorderLayout(0, 4));
        list.add(AdminUi.row(AdminUi.label("日期（yyyy-MM-dd，空白 = 今天）："), day, AdminUi.label("帳號 ID 或名稱："), query, find), BorderLayout.NORTH);
        JTabbedPane views = new JTabbedPane();
        JPanel acc = AdminUi.panel(new BorderLayout(0, 4));
        acc.add(AdminUi.scroll(accounts), BorderLayout.CENTER);
        acc.add(accountsInfo, BorderLayout.SOUTH);
        views.addTab("各帳號（online_reward_day）", acc);
        views.addTab("最近發放紀錄（online_reward_log）", AdminUi.scroll(log));
        list.add(views, BorderLayout.CENTER);

        root.add(AdminUi.section("目前設定（data\\tms282-online-reward\\online-reward.json，熱載入）", form), BorderLayout.NORTH);
        root.add(AdminUi.section("累積上線", list), BorderLayout.CENTER);
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    @Override
    public String title() {
        return "上線獎勵";
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public boolean hasUnsavedChanges() {
        return dirty;
    }

    @Override
    public void refresh() {
        load();
    }

    /** 「重新整理」: confirms first when edits would be lost. */
    private void reload() {
        if (dirty && !ctx.confirm("重新整理", "設定有還沒儲存的變更，重新整理會改回伺服器上的設定。確定嗎？")) {
            return;
        }
        dirty = false;
        load();
    }

    void query(String q) {
        query.setText(q);
    }

    private void load() {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("query", query.getText().strip());
        p.put("day", day.getText().strip());
        p.put("limit", 200);
        p.put("logLimit", 100);
        ctx.call("onlinereward.get", p, this::show);
    }

    static String stateText(String s) {
        return switch (s) {
            case "file" -> "使用設定檔";
            case "default" -> "內建預設（沒有設定檔）";
            case "invalid" -> "設定檔有誤，沿用上一份";
            default -> s;
        };
    }

    void show(JsonNode r) {
        if (!r.path("ok").asBoolean()) {
            AdminUi.colored(state, "讀取失敗", AdminUi.BAD);
            problem.setText(AdminText.failure(r));
            problem.setForeground(AdminUi.BAD);
            return;
        }
        loaded = r;
        showState(r);
        if (!dirty) {
            fill(r.path("config"));
            baseVersion = r.path("version").asText();
        }
        JsonNode t = r.path("totals");
        accountsToday.setText(String.valueOf(t.path("accounts").asLong()));
        minutesToday.setText(String.valueOf(t.path("minutes").asLong()));
        pointsToday.setText(String.valueOf(t.path("points").asLong()));
        int capPoints = r.path("config").path("dailyCapPoints").asInt();
        List<Object[]> rows = new ArrayList<>();
        for (JsonNode n : r.path("accounts")) {
            rows.add(new Object[]{n.path("userId").asInt(), n.path("name").asText(), n.path("minutes").asInt(), n.path("points").asInt(),
                    AdminUi.yesNo(capPoints > 0 && n.path("points").asInt() >= capPoints), n.path("online").asBoolean() ? "●" : "",
                    AdminUi.shortTime(n.path("updatedAt"))});
        }
        AdminUi.rows(accounts, rows);
        accountsInfo.setText(r.path("tables").asBoolean(true)
                ? r.path("day").asText() + "：" + rows.size() + " 個帳號" + (rows.size() >= 200 ? "（只顯示前 200 個）" : "") + "；時區 " + r.path("zone").asText()
                : "伺服器還沒累計過（資料表在第一次計入時建立）。");
        List<Object[]> logs = new ArrayList<>();
        for (JsonNode n : r.path("log")) {
            logs.add(new Object[]{n.path("id").asLong(), AdminUi.shortTime(n.path("grantedAt")), n.path("userId").asInt(), n.path("name").asText(),
                    n.path("day").asText(), "+" + n.path("amount").asInt(), n.path("pointsAfter").asInt(), n.path("minutes").asInt(), n.path("balanceAfter").asLong()});
        }
        AdminUi.rows(log, logs);
    }

    private void showState(JsonNode r) {
        String s = r.path("state").asText();
        boolean on = r.path("config").path("enabled").asBoolean();
        AdminUi.colored(state, stateText(s) + (on ? "" : "（停用）"), "invalid".equals(s) ? AdminUi.BAD : on ? AdminUi.OK : AdminUi.WARN);
        // the stat box grows with its text (as 新手流程 does), so 「內建預設（沒有設定檔）」 is not clipped
        if (state.getParent() instanceof JComponent box) {
            box.setPreferredSize(null);
            Dimension d = box.getPreferredSize();
            box.setPreferredSize(new Dimension(Math.max(118, d.width), d.height));
            box.revalidate();
        }
        file.setText("設定檔：" + r.path("file").asText());
        String p = r.path("problem").asText();
        problem.setText(p.isEmpty() ? " " : "問題：" + p);
        problem.setForeground(p.isEmpty() ? AdminUi.MUTED : AdminUi.BAD);
    }

    private void fill(JsonNode c) {
        filling = true;
        try {
            enabled.setSelected(c.path("enabled").asBoolean());
            interval.setText(String.valueOf(c.path("intervalMinutes").asInt()));
            points.setText(String.valueOf(c.path("pointsPerInterval").asInt()));
            cap.setText(String.valueOf(c.path("dailyCapPoints").asInt()));
            afk.setText(String.valueOf(c.path("afkMinutes").asInt()));
            notice.setText(c.path("notice").asText());
            notice.setCaretPosition(0);
        } finally {
            filling = false;
        }
        dirty = false;
        save.setEnabled(false);
        if (UNSAVED.equals(saveInfo.getText())) {
            saveInfo.setText(" ");
        }
    }

    private void edited() {
        if (filling) {
            return;
        }
        dirty = true;
        save.setEnabled(true);
        saveInfo.setText(UNSAVED);
    }

    /** The form as onlinereward.save's {@code config}; null (and a hint) when a number is not an integer. */
    Map<String, Object> form() {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("enabled", enabled.isSelected());
        try {
            c.put("intervalMinutes", Integer.parseInt(interval.getText().strip()));
            c.put("pointsPerInterval", Integer.parseInt(points.getText().strip()));
            c.put("dailyCapPoints", Integer.parseInt(cap.getText().strip()));
            c.put("afkMinutes", Integer.parseInt(afk.getText().strip()));
        } catch (NumberFormatException e) {
            saveInfo.setText("分鐘、點數、上限、掛機分鐘請輸入整數。");
            return null;
        }
        c.put("notice", notice.getText().strip());
        return c;
    }

    void saveConfig() {
        Map<String, Object> c = form();
        if (c == null) {
            return;
        }
        if (!ctx.confirm("儲存上線獎勵設定", "每 " + c.get("intervalMinutes") + " 分鐘 " + c.get("pointsPerInterval") + " 樂豆點、每天上限 " + c.get("dailyCapPoints")
                + " 點、" + (Boolean.TRUE.equals(c.get("enabled")) ? "啟用" : "停用") + "。\n伺服器會立刻改用新設定（已累計的分鐘與已發的點數不變）。確定嗎？")) {
            return;
        }
        send(c);
    }

    /** onlinereward.save of {@code c} (after the confirmation). */
    void send(Map<String, Object> c) {
        save.setEnabled(false);
        ctx.call("onlinereward.save", Map.of("baseVersion", baseVersion, "config", c), r -> {
            if (r.path("ok").asBoolean()) {
                dirty = false;
                fill(r.path("config"));
                baseVersion = r.path("version").asText();
                showState(r);
                saveInfo.setText("已儲存，伺服器已改用新設定。");
                ctx.status("上線獎勵設定已儲存（online-reward.json）。");
                load();
            } else {
                save.setEnabled(true);
                saveInfo.setText("儲存失敗：" + (r.path("code").asText().equals("conflict")
                        ? "設定檔在別處被修改過，請按「重新整理」後再改。" : r.path("problem").asText(AdminText.failure(r))));
            }
        });
    }

    // for the console check
    JTable accountsTable() { return accounts; }
    JTable logTable() { return log; }
    JLabel stateLabel() { return state; }
    JLabel saveInfo() { return saveInfo; }
    JTextField intervalField() { return interval; }
    JButton saveButton() { return save; }
    JsonNode loaded() { return loaded; }
}
