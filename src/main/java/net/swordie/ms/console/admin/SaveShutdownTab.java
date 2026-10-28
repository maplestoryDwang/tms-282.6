package net.swordie.ms.console.admin;

import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.LongFunction;

/**
 * 「存檔／停機」：定期存檔狀態、最近存檔、失敗次數、在線人數；「優雅停機」（寫 stop 檔，二次確認並顯示在線人數，之後等到
 * 伺服器程序消失）。沒有「重新啟動」：只顯示啟動指令。
 */
final class SaveShutdownTab implements AdminTab {
    private final AdminConsoleContext ctx;
    private final JPanel root = AdminUi.panel(new BorderLayout(8, 8));
    private final JLabel periodic = AdminUi.value();
    private final JLabel interval = AdminUi.value();
    private final JLabel saved = AdminUi.value();
    private final JLabel failed = AdminUi.value();
    private final JLabel users = AdminUi.value();
    private final JLabel chars = AdminUi.value();
    private final JLabel pending = AdminUi.value();
    private final JTable recent = AdminUi.table(new String[]{"時間", "角色 ID", "角色名稱", "耗時", "結果"}, new int[]{120, 70, 160, 80, 70});
    private final JLabel serverInfo = AdminUi.label("-");
    private final JLabel stopState = AdminUi.label(" ");
    private final JButton stop = AdminUi.dangerButton("優雅停機…");
    /** 維修倒數 (2026-10-05): 0 = stop at once; otherwise the server announces and refuses new logins first. */
    static final int[] COUNTDOWN_MINUTES = {0, 1, 3, 5, 10};
    private final JComboBox<String> countdown = new JComboBox<>(new String[]{"立即停機", "倒數 1 分鐘", "倒數 3 分鐘", "倒數 5 分鐘", "倒數 10 分鐘"});
    private final JButton cancelCountdown = AdminUi.button("取消倒數");
    private int countdownSec;
    private final JTextField startCommand = AdminUi.field(60);
    /** Tells whether a process is alive (ProcessHandle; replaced in the screenshot mode). */
    private final LongFunction<Optional<Boolean>> alive;
    private long pid;
    private int onlineUsers = -1;
    private Timer waiter;

    SaveShutdownTab(AdminConsoleContext ctx, LongFunction<Optional<Boolean>> alive) {
        this.ctx = ctx;
        this.alive = alive;
        JPanel stats = AdminUi.panel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        stats.add(AdminUi.stat("定期存檔", periodic));
        stats.add(AdminUi.stat("每個角色間隔", interval));
        stats.add(AdminUi.stat("成功次數", saved));
        stats.add(AdminUi.stat("失敗次數", failed));
        stats.add(AdminUi.stat("在線帳號", users));
        stats.add(AdminUi.stat("在線角色", chars));
        stats.add(AdminUi.stat("佇列中存檔", pending));
        root.add(stats, BorderLayout.NORTH);

        JScrollPane rs = AdminUi.scroll(recent);
        JPanel center = AdminUi.panel(new BorderLayout(0, 8));
        center.add(AdminUi.section("最近的定期存檔（最新 50 筆；登出、換頻道時的存檔不在這裡）", rs), BorderLayout.CENTER);

        JPanel shutdown = AdminUi.panel(new GridLayout(0, 1, 0, 4));
        shutdown.add(AdminUi.label("優雅停機會：拒絕新連線 → 存檔並斷開所有玩家 → 等存檔寫完 → 伺服器程序自己結束。"));
        shutdown.add(AdminUi.label("有人在線時可以選倒數：開始、每分鐘、最後 30 秒與 10 秒公告維修，倒數期間不接受新登入，時間到才停機。"));
        shutdown.add(AdminUi.row(countdown, stop, cancelCountdown, stopState));
        cancelCountdown.setEnabled(false);
        cancelCountdown.addActionListener(e -> requestCancel());
        shutdown.add(serverInfo);
        stop.addActionListener(e -> requestStop());
        startCommand.setEditable(false);
        startCommand.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JButton copy = AdminUi.button("複製");
        copy.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(startCommand.getText()), null));
        JPanel cmd = AdminUi.panel(new BorderLayout(6, 0));
        cmd.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        cmd.add(AdminUi.label("啟動指令："), BorderLayout.WEST);
        cmd.add(startCommand, BorderLayout.CENTER);
        cmd.add(copy, BorderLayout.EAST);
        shutdown.add(cmd);
        shutdown.add(AdminUi.hint("主控台不提供「重新啟動」：停機後要再開，請執行上面的指令（由既有的 Start-CompatibleTest.ps1 負責）。"));
        center.add(AdminUi.section("停機", shutdown), BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    @Override
    public String title() {
        return "存檔／停機";
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public boolean autoRefresh() {
        return waiter == null;
    }

    @Override
    public void refresh() {
        if (waiter != null) {
            return;
        }
        ctx.call("save.status", this::show);
    }

    void show(JsonNode r) {
        if (!r.path("ok").asBoolean()) {
            ctx.status("存檔：" + AdminText.failure(r));
            return;
        }
        JsonNode p = r.path("periodic");
        if (!p.path("running").asBoolean()) {
            AdminUi.colored(periodic, "未執行", AdminUi.BAD);
            interval.setText("-");
            saved.setText("-");
            failed.setText("-");
        } else {
            boolean on = p.path("enabled").asBoolean();
            AdminUi.colored(periodic, on ? "啟用" : "已暫停", on ? AdminUi.OK : AdminUi.WARN);
            interval.setText(p.path("minSec").asInt() / 60 + "–" + p.path("maxSec").asInt() / 60 + " 分");
            saved.setText(p.path("saved").asText());
            long f = p.path("failed").asLong();
            AdminUi.colored(failed, String.valueOf(f), f > 0 ? AdminUi.BAD : Color.DARK_GRAY);
        }
        onlineUsers = r.path("onlineUsers").asInt();
        users.setText(String.valueOf(onlineUsers));
        chars.setText(r.path("onlineCharacters").asText());
        pending.setText(r.path("pendingSaves").asText());
        List<Object[]> rows = new ArrayList<>();
        for (JsonNode n : r.path("recent")) {
            rows.add(new Object[]{AdminUi.time(n.path("at")), n.path("characterId").asInt(), n.path("name").asText(),
                    n.path("millis").asLong() + " ms", n.path("ok").asBoolean() ? "成功" : "失敗"});
        }
        AdminUi.rows(recent, rows);
        pid = r.path("pid").asLong();
        serverInfo.setText("伺服器 PID " + pid + "，已執行 " + AdminUi.duration(r.path("uptimeSec").asLong()) + "；stop 檔：" + r.path("stopFile").asText());
        startCommand.setText(r.path("startCommand").asText());
        startCommand.setCaretPosition(0);
        countdownSec = r.path("countdownSec").asInt();
        boolean counting = countdownSec > 0 && !r.path("shuttingDown").asBoolean();
        boolean stopping = r.path("shuttingDown").asBoolean() || (r.path("stopRequested").asBoolean() && !counting);
        boolean watcher = r.path("stopWatcher").asBoolean();
        stop.setEnabled(watcher && !stopping && !counting);
        countdown.setEnabled(watcher && !stopping && !counting);
        cancelCountdown.setEnabled(counting);
        if (counting) {
            AdminUi.colored(stopState, String.format("維修倒數中：剩 %d:%02d，時間到會存檔並停機（倒數期間不接受新登入）",
                    countdownSec / 60, countdownSec % 60), AdminUi.WARN);
        } else if (stopping) {
            AdminUi.colored(stopState, "已有停機要求，伺服器正在停機…", AdminUi.WARN);
        } else if (!watcher) {
            AdminUi.colored(stopState, "這個伺服器程序沒有監看 stop 檔，無法從這裡停機", AdminUi.WARN);
        } else {
            AdminUi.colored(stopState, " ", Color.DARK_GRAY);
        }
    }

    private void requestStop() {
        if (onlineUsers < 0 || pid <= 0) {
            return;
        }
        int n = onlineUsers;
        int sec = COUNTDOWN_MINUTES[Math.max(0, countdown.getSelectedIndex())] * 60;
        String how = sec > 0 ? "先在遊戲內公告維修倒數 " + sec / 60 + " 分鐘（期間不接受新登入），時間到才" : "";
        if (!ctx.confirm("優雅停機（第 1 次確認）", "目前在線 " + n + " 個帳號。\n\n停機會" + how + "先把所有人存檔並斷線，然後伺服器結束。\n"
                + "停機後要再開請執行啟動指令（主控台不會自動重開）。\n\n要繼續嗎？")) {
            return;
        }
        if (!ctx.typed("優雅停機（第 2 次確認）", "目前在線 " + n + " 個帳號。請輸入「停機」兩個字確認：", "停機")) {
            ctx.info("優雅停機", "沒有輸入「停機」，已取消。");
            return;
        }
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("confirm", "shutdown");
        p.put("expectOnline", n);
        if (sec > 0) {
            p.put("countdownSec", sec);
        }
        ctx.call("server.shutdown", p, r -> {
            if (r.path("ok").asBoolean() && sec > 0) {
                ctx.status("維修倒數已開始：" + sec / 60 + " 分鐘");
                refresh();
            } else if (r.path("ok").asBoolean()) {
                startWaiting(r.path("pid").asLong());
            } else if ("onlineChanged".equals(r.path("code").asText())) {
                ctx.error("優雅停機", "在線人數已變成 " + r.path("onlineUsers").asInt() + "，請重新確認。");
                refresh();
            } else {
                ctx.error("優雅停機", AdminText.failure(r));
                refresh();
            }
        });
    }

    private void requestCancel() {
        if (!ctx.confirm("取消倒數", "要取消維修倒數嗎？\n伺服器會公告「維修取消」並繼續運作。")) {
            return;
        }
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("confirm", "cancel");
        ctx.call("server.cancelShutdown", p, r -> {
            if (r.path("ok").asBoolean()) {
                ctx.status("維修倒數已取消");
            } else {
                ctx.error("取消倒數", AdminText.failure(r));
            }
            refresh();
        });
    }

    /** Polls the server's process every second until it is gone. */
    void startWaiting(long serverPid) {
        stop.setEnabled(false);
        long t0 = System.currentTimeMillis();
        AdminUi.colored(stopState, "已送出停機要求，等待伺服器存檔並結束…", AdminUi.WARN);
        waiter = new Timer(1000, e -> {
            long sec = (System.currentTimeMillis() - t0) / 1000;
            Optional<Boolean> a = alive.apply(serverPid);
            if (a.isEmpty() || !a.get()) {
                ((Timer) e.getSource()).stop();
                AdminUi.colored(stopState, "伺服器已結束（PID " + serverPid + "，等了 " + sec + " 秒）。要再開請執行啟動指令。", AdminUi.OK);
                waiter = null;
                ctx.status("伺服器已停機");
            } else {
                AdminUi.colored(stopState, "等待伺服器存檔並結束…（PID " + serverPid + "，已等 " + sec + " 秒"
                        + (sec > 150 ? "，比平常久，請看 formal-server.log" : "") + "）", AdminUi.WARN);
            }
        });
        waiter.start();
    }

    /** For the screenshot: the waiting line as it looks after {@code sec} seconds. */
    void showWaiting(long serverPid, long sec) {
        stop.setEnabled(false);
        AdminUi.colored(stopState, "等待伺服器存檔並結束…（PID " + serverPid + "，已等 " + sec + " 秒）", AdminUi.WARN);
    }

    static Optional<Boolean> processAlive(long pid) {
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive);
    }
}
