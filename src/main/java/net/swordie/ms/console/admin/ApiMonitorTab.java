package net.swordie.ms.console.admin;

import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** 「API 監看」：8600 狀態、launcher session、各版本登入器請求、限速、api.http.allow、最近錯誤；撤銷某帳號全部 session。 */
final class ApiMonitorTab implements AdminTab {
    private final AdminConsoleContext ctx;
    private final JPanel root = AdminUi.panel(new BorderLayout(8, 8));
    private final JLabel state = AdminUi.value();
    private final JLabel sessions = AdminUi.value();
    private final JLabel requests = AdminUi.value();
    private final JLabel limited = AdminUi.value();
    private final JLabel launcherLocks = AdminUi.value();
    private final JLabel gameLocks = AdminUi.value();
    private final JLabel adminRefused = AdminUi.value();
    private final JTable versions = AdminUi.table(new String[]{"登入器版本（X-Cat-Launcher）", "請求數"}, new int[]{220, 70});
    private final JTable codes = AdminUi.table(new String[]{"回應代碼", "意思", "次數"}, new int[]{105, 230, 50});
    private final JTable buckets = AdminUi.table(new String[]{"限速桶", "容量", "補 1 次", "擋下", "追蹤 IP"}, new int[]{70, 50, 70, 50, 60});
    private final JTable recent = AdminUi.table(new String[]{"時間", "來源 IP", "路徑", "HTTP", "代碼", "意思", "登入器版本"},
            new int[]{110, 110, 130, 50, 120, 190, 190});
    private final JLabel allow = AdminUi.label("-");
    private final JLabel allowSource = AdminUi.hint("");
    private final JTextField revokeQuery = AdminUi.field(14);

    ApiMonitorTab(AdminConsoleContext ctx) {
        this.ctx = ctx;
        JPanel stats = AdminUi.panel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        stats.add(AdminUi.stat("登入器 API", state));
        stats.add(AdminUi.stat("Launcher session", sessions));
        stats.add(AdminUi.stat("請求總數", requests));
        stats.add(AdminUi.stat("限速擋下", limited));
        stats.add(AdminUi.stat("登入器密碼鎖定", launcherLocks));
        stats.add(AdminUi.stat("遊戲登入密碼鎖定", gameLocks));
        stats.add(AdminUi.stat("管理 API 被拒", adminRefused));
        root.add(stats, BorderLayout.NORTH);

        JPanel middle = AdminUi.panel(new GridLayout(1, 3, 8, 0));
        middle.add(AdminUi.section("各版本登入器請求數", AdminUi.scroll(versions)));
        middle.add(AdminUi.section("各回應代碼次數", AdminUi.scroll(codes)));
        JPanel right = AdminUi.panel(new BorderLayout(0, 6));
        right.add(AdminUi.scroll(buckets), BorderLayout.CENTER);
        JPanel allowBox = AdminUi.panel(new GridLayout(2, 1));
        allowBox.add(allow);
        allowBox.add(allowSource);
        right.add(AdminUi.section("api.http.allow（8600 來源白名單，修改請到「連線安全」）", allowBox), BorderLayout.SOUTH);
        middle.add(AdminUi.section("限速（token bucket）", right));

        JPanel bottom = AdminUi.panel(new BorderLayout(0, 6));
        JScrollPane rs = AdminUi.scroll(recent);
        rs.setPreferredSize(new Dimension(100, 210));
        bottom.add(AdminUi.section("最近錯誤／拒絕（最新 50 筆，不含成功）", rs), BorderLayout.CENTER);
        JButton revoke = AdminUi.button("撤銷這個帳號的全部 session");
        revoke.addActionListener(e -> revoke());
        bottom.add(AdminUi.section("撤銷 launcher session（登入器要重新登入；不影響遊戲連線）",
                AdminUi.row(AdminUi.label("帳號 ID／名稱／角色名："), revokeQuery, revoke)), BorderLayout.SOUTH);

        JPanel center = AdminUi.panel(new BorderLayout(0, 8));
        center.add(middle, BorderLayout.CENTER);
        center.add(bottom, BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    @Override
    public String title() {
        return "API 監看";
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
        ctx.call("api.status", this::show);
    }

    void show(JsonNode r) {
        if (!r.path("ok").asBoolean()) {
            ctx.status("API 監看：" + AdminText.failure(r));
            return;
        }
        AdminUi.colored(state, "執行中", AdminUi.OK);
        sessions.setText(r.path("sessions").asText());
        requests.setText(r.path("requests").asText());
        limited.setText(r.path("rateLimitedTotal").asText());
        launcherLocks.setText(r.path("launcherBackoff").path("locked").asInt() + " 組");
        gameLocks.setText(r.path("gameLoginLocked").asInt() + " 組");
        adminRefused.setText(r.path("adminRefused").asText());
        List<Object[]> v = new ArrayList<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = r.path("byVersion").fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> e = it.next();
            v.add(new Object[]{AdminText.version(e.getKey()), e.getValue().asLong()});
        }
        AdminUi.rows(versions, v);
        List<Object[]> c = new ArrayList<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = r.path("byCode").fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> e = it.next();
            c.add(new Object[]{e.getKey(), AdminText.launcherCode(e.getKey()), e.getValue().asLong()});
        }
        AdminUi.rows(codes, c);
        List<Object[]> b = new ArrayList<>();
        for (JsonNode n : r.path("rateLimits")) {
            b.add(new Object[]{n.path("bucket").asText(), n.path("capacity").asInt(), n.path("refillMs").asLong() / 1000.0 + " 秒",
                    n.path("refused").asLong(), n.path("tracked").asInt()});
        }
        AdminUi.rows(buckets, b);
        JsonNode a = r.path("httpAllow");
        List<String> rules = new ArrayList<>();
        a.path("rules").forEach(x -> rules.add(x.asText()));
        allow.setText(String.join("，", rules));
        String src = switch (a.path("source").asText()) {
            case "jvm" -> "由啟動參數 -Dapi.http.allow 指定";
            case "file" -> "來自 config\\server.properties";
            default -> "未設定，使用預設 " + a.path("default").asText();
        };
        int bad = a.path("rejected").size();
        allowSource.setText(src + (bad > 0 ? "；有 " + bad + " 個看不懂的項目被忽略" : ""));
        List<Object[]> rec = new ArrayList<>();
        for (JsonNode n : r.path("recent")) {
            String code = n.path("code").asText();
            rec.add(new Object[]{AdminUi.time(n.path("at")), n.path("ip").asText(), n.path("path").asText(),
                    n.path("status").asInt(), code, AdminText.launcherCode(code), AdminText.version(n.path("version").asText())});
        }
        AdminUi.rows(recent, rec);
    }

    void serverDown() {
        AdminUi.colored(state, "未啟動", AdminUi.BAD);
    }

    private void revoke() {
        String q = revokeQuery.getText().strip();
        if (q.isEmpty()) {
            ctx.error("撤銷 session", "請先輸入帳號 ID、帳號名稱或角色名稱。");
            return;
        }
        if (!ctx.confirm("撤銷 session", "要撤銷「" + q + "」的全部 launcher session 嗎？\n該帳號的登入器會被登出，需要重新登入（遊戲連線不受影響）。")) {
            return;
        }
        ctx.call("api.revokeSessions", Map.of("query", q), r -> {
            if (r.path("ok").asBoolean()) {
                ctx.info("撤銷 session", "已撤銷 " + r.path("name").asText() + "（ID " + r.path("userId").asInt() + "）的 "
                        + r.path("revoked").asInt() + " 個 session。");
                refresh();
            } else {
                ctx.error("撤銷 session", AdminText.failure(r));
            }
        });
    }
}
