package net.swordie.ms.console.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.account.WorldDirectory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 「帳號」：依帳號 ID／名稱／角色名查詢角色清單、在線狀態、最後遊玩、密碼錯誤鎖定與解除鎖定、封鎖紀錄與封鎖／解封
 * （伺服器的 BanService，與遊戲內 !tempban／!untempban 同一條路）。
 */
final class AccountTab implements AdminTab {
    private static final WorldDirectory WORLDS = new WorldDirectory(null);

    private final AdminConsoleContext ctx;
    private final JPanel root = AdminUi.panel(new BorderLayout(8, 8));
    private final JTextField query = AdminUi.field(18);
    private final Map<String, JLabel> info = new LinkedHashMap<>();
    private final JTable characters = AdminUi.table(new String[]{"角色 ID", "名稱", "等級", "職業", "世界", "最後登出", "在線", "刪除中"},
            new int[]{55, 105, 40, 160, 100, 85, 36, 48});
    private final JTable locks = AdminUi.table(new String[]{"來源", "IP", "失敗次數", "剩餘鎖定", "最後失敗"}, new int[]{90, 110, 60, 80, 110});
    private final JTable bans = AdminUi.table(new String[]{"類型", "開始", "結束", "狀態", "原因"}, new int[]{125, 85, 85, 55, 200});
    private final JComboBox<String> banType = new JComboBox<>();
    private final JSpinner banHours = new JSpinner(new SpinnerNumberModel(24, 0, 87600, 1));
    private final JTextField banReason = AdminUi.field(18);
    private final JButton unlock = AdminUi.button("解除鎖定");
    private final JButton ban = AdminUi.dangerButton("封鎖");
    private final JButton unban = AdminUi.button("解除封鎖");
    private final List<String> banTypeNames = new ArrayList<>();
    private int userId;
    private String userName = "";

    AccountTab(AdminConsoleContext ctx) {
        this.ctx = ctx;
        JButton find = AdminUi.button("查詢");
        find.addActionListener(e -> refresh());
        query.addActionListener(e -> refresh());
        root.add(AdminUi.row(AdminUi.label("帳號 ID／帳號名稱／角色名稱："), query, find,
                AdminUi.hint("（依序比對：數字 ID → 帳號名稱 → 角色名稱）")), BorderLayout.NORTH);

        JPanel infoGrid = AdminUi.panel(new GridLayout(0, 4, 8, 4));
        for (String k : List.of("帳號 ID", "帳號名稱", "建立日期", "在線", "Launcher session", "VIP 等級", "角色欄位", "帳號類型",
                "最後遊玩", "符合方式", "舊封鎖欄位", "")) {
            JLabel v = AdminUi.value();
            if (!k.isEmpty()) {
                infoGrid.add(AdminUi.label(k));
                infoGrid.add(v);
                info.put(k, v);
            }
        }
        JPanel left = AdminUi.panel(new BorderLayout(0, 8));
        left.add(AdminUi.section("帳號", infoGrid), BorderLayout.NORTH);
        JScrollPane cs = AdminUi.scroll(characters);
        cs.setPreferredSize(new Dimension(100, 200));
        left.add(AdminUi.section("角色清單（在線角色顯示即時等級／職業）", cs), BorderLayout.CENTER);

        JPanel right = AdminUi.panel(new GridLayout(2, 1, 0, 8));
        JPanel lockBox = AdminUi.panel(new BorderLayout(0, 4));
        lockBox.add(AdminUi.scroll(locks), BorderLayout.CENTER);
        unlock.addActionListener(e -> unlock());
        lockBox.add(AdminUi.row(unlock, AdminUi.hint("清掉登入器與遊戲登入兩邊、所有來源 IP 的失敗紀錄")), BorderLayout.SOUTH);
        right.add(AdminUi.section("密碼錯誤鎖定（連錯 5 次鎖 30 秒起，加倍到 15 分鐘）", lockBox));
        JPanel banBox = AdminUi.panel(new BorderLayout(0, 4));
        banBox.add(AdminUi.scroll(bans), BorderLayout.CENTER);
        banType.setFont(AdminUi.font(Font.PLAIN, 13));
        banHours.setFont(AdminUi.font(Font.PLAIN, 13));
        ban.addActionListener(e -> ban());
        unban.addActionListener(e -> unban());
        JPanel form = AdminUi.panel(new GridLayout(2, 1));
        form.add(AdminUi.row(AdminUi.label("類型"), banType, AdminUi.label("小時（0＝永久）"), banHours));
        form.add(AdminUi.row(AdminUi.label("原因"), banReason, ban, unban));
        banBox.add(form, BorderLayout.SOUTH);
        right.add(AdminUi.section("封鎖（伺服器 BanService，與遊戲內 !tempban 相同）", banBox));

        JPanel center = AdminUi.panel(new GridLayout(1, 2, 8, 0));
        center.add(left);
        center.add(right);
        root.add(center, BorderLayout.CENTER);
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        enableActions(false);
    }

    @Override
    public String title() {
        return "帳號";
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
        String q = query.getText().strip();
        if (q.isEmpty()) {
            return;
        }
        ctx.call("account.find", Map.of("query", q), this::show);
    }

    void show(JsonNode r) {
        if (!r.path("ok").asBoolean()) {
            clear();
            ctx.status("帳號：" + AdminText.failure(r));
            return;
        }
        JsonNode a = r.path("account");
        userId = a.path("id").asInt();
        userName = a.path("name").asText();
        set("帳號 ID", String.valueOf(userId));
        set("帳號名稱", userName);
        set("建立日期", AdminUi.dateTime(a.path("createdAt")));
        boolean online = r.path("online").asBoolean();
        info.get("在線").setText(online ? "在線" : "離線");
        info.get("在線").setForeground(online ? AdminUi.OK : AdminUi.MUTED);
        set("Launcher session", r.path("sessions").asText());
        set("VIP 等級", a.path("vipGrade").asText());
        set("角色欄位", a.path("characterSlots").asText());
        set("帳號類型", a.path("accountType").asText());
        JsonNode lp = r.path("lastPlayed");
        set("最後遊玩", lp.isMissingNode() || lp.isNull() ? "-" : lp.path("name").asText()
                + (lp.path("online").asBoolean() ? "（在線）" : "（" + AdminUi.time(lp.path("lastLogout")) + "）"));
        set("符合方式", switch (a.path("matchedBy").asText()) {
            case "id" -> "帳號 ID";
            case "name" -> "帳號名稱";
            case "character" -> "角色名稱";
            default -> "-";
        });
        set("舊封鎖欄位", r.path("legacyBanActive").asBoolean() ? "封鎖到 " + AdminUi.dateTime(a.path("banExpireAt")) : "無");
        List<Object[]> cs = new ArrayList<>();
        for (JsonNode c : r.path("characters")) {
            cs.add(new Object[]{c.path("id").asInt(), c.path("name").asText(), c.path("level").asInt(), c.path("jobName").asText(),
                    WORLDS.name(c.path("world").asInt()), AdminUi.shortTime(c.path("lastLogout")),
                    c.path("online").asBoolean() ? "●" : "", c.path("deleting").asBoolean() ? "是" : ""});
        }
        AdminUi.rows(characters, cs);
        List<Object[]> ls = new ArrayList<>();
        for (JsonNode l : r.path("locks")) {
            long sec = l.path("lockedForSec").asLong();
            ls.add(new Object[]{"launcher".equals(l.path("kind").asText()) ? "登入器" : "遊戲登入", l.path("ip").asText(),
                    l.path("failures").asInt(), sec > 0 ? AdminUi.duration(sec) : "未鎖定", AdminUi.time(l.path("lastFailure"))});
        }
        AdminUi.rows(locks, ls);
        List<Object[]> bs = new ArrayList<>();
        for (JsonNode b : r.path("bans")) {
            bs.add(new Object[]{b.path("typeLabel").asText(), AdminUi.shortTime(b.path("createdAt")),
                    b.path("permanent").asBoolean() ? "永久" : AdminUi.shortTime(b.path("endTime")),
                    b.path("active").asBoolean() ? "生效中" : "已結束", b.path("reason").asText()});
        }
        AdminUi.rows(bans, bs);
        if (banType.getItemCount() == 0) {
            for (JsonNode t : r.path("banTypes")) {
                banTypeNames.add(t.path("name").asText());
                banType.addItem(t.path("label").asText() + (t.path("blocksLogin").asBoolean() ? "" : "（不擋登入）"));
            }
        }
        enableActions(true);
        ctx.status("帳號：已載入 " + userName + "（ID " + userId + "）");
    }

    private void set(String key, String value) {
        JLabel l = info.get(key);
        l.setText(value == null || value.isEmpty() ? "-" : value);
        l.setForeground(Color.DARK_GRAY);
    }

    private void clear() {
        userId = 0;
        info.values().forEach(l -> l.setText("-"));
        AdminUi.rows(characters, List.of());
        AdminUi.rows(locks, List.of());
        AdminUi.rows(bans, List.of());
        enableActions(false);
    }

    private void enableActions(boolean on) {
        unlock.setEnabled(on);
        ban.setEnabled(on);
        unban.setEnabled(on);
    }

    private void unlock() {
        if (userId <= 0 || !ctx.confirm("解除鎖定", "要清掉「" + userName + "」在登入器與遊戲登入的所有密碼錯誤紀錄嗎？")) {
            return;
        }
        ctx.call("account.unlock", Map.of("userId", userId), r -> {
            if (r.path("ok").asBoolean()) {
                ctx.info("解除鎖定", "已解除：登入器 " + r.path("launcherCleared").asInt() + " 組、遊戲登入 " + r.path("gameCleared").asInt() + " 組。");
                refresh();
            } else {
                ctx.error("解除鎖定", AdminText.failure(r));
            }
        });
    }

    private void ban() {
        int i = banType.getSelectedIndex();
        String reason = banReason.getText().strip();
        if (userId <= 0 || i < 0) {
            return;
        }
        if (reason.isEmpty()) {
            ctx.error("封鎖", "請填寫封鎖原因。");
            return;
        }
        int hours = (Integer) banHours.getValue();
        String what = banType.getItemAt(i) + "，" + (hours == 0 ? "永久" : hours + " 小時");
        if (!ctx.confirm("封鎖", "確定要封鎖「" + userName + "」（ID " + userId + "）？\n" + what + "\n原因：" + reason
                + "\n會擋登入的類型會同時把他踢下線（會先存檔）並登出登入器。")) {
            return;
        }
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("userId", userId);
        p.put("type", banTypeNames.get(i));
        p.put("hours", hours);
        p.put("reason", reason);
        ctx.call("account.ban", p, r -> {
            if (r.path("ok").asBoolean()) {
                JsonNode w = r.get("databaseWritten");
                ctx.info("封鎖", "已封鎖。斷線 " + r.path("disconnected").asInt() + " 個連線，撤銷 " + r.path("revokedSessions").asInt()
                        + " 個 session。" + (w == null || w.isNull() ? "（資料庫寫入還在進行）" : w.asBoolean() ? "" : "（資料庫寫入失敗，只在快取生效到重啟前）"));
                refresh();
            } else {
                ctx.error("封鎖", AdminText.failure(r));
            }
        });
    }

    private void unban() {
        if (userId <= 0 || !ctx.confirm("解除封鎖", "要解除「" + userName + "」所有生效中的封鎖嗎？")) {
            return;
        }
        ctx.call("account.unban", Map.of("userId", userId), r -> {
            if (r.path("ok").asBoolean()) {
                JsonNode n = r.get("lifted");
                ctx.info("解除封鎖", n == null || n.isNull() ? "已從快取解除，資料庫寫入還在進行。"
                        : n.asInt() < 0 ? "已從快取解除，但資料庫寫入失敗。" : "已解除 " + n.asInt() + " 筆封鎖。");
                refresh();
            } else {
                ctx.error("解除封鎖", AdminText.failure(r));
            }
        });
    }
}
