package net.swordie.ms.console.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 「部署」：列出 {@code <伺服器>\bin\formal-snapshot-*}（有 Deploy.ps1 與 jar 的資料夾），標出目前執行中的那一包，
 * 一鍵部署或回復上一版。部署腳本本身不變（Deploy.ps1：驗雜湊、改 formal-runtime.json、存檔停機、重啟）；這個分頁只是
 * 代替手動開 PowerShell：有人在線時先廣播、把在線角色踢下線（會存檔），再以獨立程序執行腳本並顯示它的輸出。
 * <p>
 * 腳本以 WMI（Win32_Process.Create）啟動，不是主控台的子程序：關掉主控台不會影響正在進行的部署或新的伺服器。
 */
final class DeployTab implements AdminTab {
    /** Set by AdminConsoleMain to its --home (the server folder); the working directory when absent. */
    static final String HOME_KEY = "tms.admin.home";
    private static final String PREFIX = "formal-snapshot-";
    private static final String JAR = "maplestory-282.3-jar-with-dependencies.jar";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final AdminConsoleContext ctx;
    private final Path home;
    private final JPanel root = AdminUi.panel(new BorderLayout(8, 8));
    private final JLabel running = AdminUi.value();
    private final JLabel online = AdminUi.value();
    private final JTable table = AdminUi.table(new String[]{"包", "建立時間", "狀態", "jar 大小"}, new int[]{260, 130, 110, 90});
    private final JTextArea output = new JTextArea(9, 80);
    private final JTextField noticeMinutes = AdminUi.field(4);
    private final JTextField noticeText = AdminUi.field(34);
    private final List<Path> folders = new ArrayList<>();
    private final Timer tail;
    private Path watching;
    private int onlineCharacters;
    private String current = "";

    DeployTab(AdminConsoleContext ctx, Path home) {
        this.ctx = ctx;
        this.home = home;
        JPanel stats = AdminUi.panel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        running.setText("（讀不到 formal-runtime.json）000000"); // the stat box keeps the width it has when it is made
        stats.add(AdminUi.stat("目前執行的包", running));
        stats.add(AdminUi.stat("在線角色", online));
        root.add(stats, BorderLayout.NORTH);

        JButton deploy = AdminUi.dangerButton("部署選取的包");
        deploy.addActionListener(e -> start(false));
        JButton rollback = AdminUi.button("回復到選取這包部署前的版本");
        rollback.addActionListener(e -> start(true));
        JButton reload = AdminUi.button("重新整理");
        reload.addActionListener(e -> refresh());
        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JPanel center = AdminUi.panel(new BorderLayout(0, 6));
        center.add(AdminUi.scroll(table), BorderLayout.CENTER);
        // launcher 1.2.8: the planned restart shows in every launcher (and in the game as a notice) before the deploy
        JButton announce = AdminUi.button("預告維護重啟");
        announce.addActionListener(e -> announce());
        JButton withdraw = AdminUi.button("取消預告");
        withdraw.addActionListener(e -> ctx.call("ops.restartNotice", Map.of("clear", true),
                r -> ctx.status(r.path("ok").asBoolean() ? "已取消維護預告。" : "取消預告：" + AdminText.failure(r))));
        noticeMinutes.setText("5");
        noticeText.setText("伺服器即將維護更新，請先存檔下線。");
        JPanel south = AdminUi.panel(new GridLayout(2, 1, 0, 4));
        south.add(AdminUi.row(deploy, rollback, reload));
        south.add(AdminUi.row(AdminUi.label("幾分鐘後："), noticeMinutes, AdminUi.label("說明："), noticeText, announce, withdraw));
        center.add(south, BorderLayout.SOUTH);
        JPanel body = AdminUi.panel(new GridLayout(2, 1, 0, 8));
        body.add(AdminUi.section("可部署的包（最新的在最上面）", center));
        body.add(AdminUi.section("部署腳本的輸出（deploy-run.log）", AdminUi.scroll(output)));
        root.add(body, BorderLayout.CENTER);
        root.add(AdminUi.hint("部署會重啟伺服器，所有人都會斷線。有人在線時會先廣播並把在線角色踢下線（先存檔）。"), BorderLayout.SOUTH);
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        tail = new Timer(1500, e -> showLog());
        tail.setRepeats(true);
    }

    @Override
    public String title() {
        return "部署";
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public void refresh() {
        current = currentJar();
        List<Object[]> rows = new ArrayList<>();
        folders.clear();
        folders.addAll(snapshots(home.resolve("bin")));
        for (Path f : folders) {
            String name = f.getFileName().toString();
            long size = 0;
            String time = "";
            try {
                size = Files.size(f.resolve(JAR));
                time = AdminUi.dateTime(JSON.getNodeFactory().numberNode(Files.getLastModifiedTime(f.resolve(JAR)).toMillis()));
            } catch (IOException e) {
                // listed anyway; the deploy script verifies the jar
            }
            rows.add(new Object[]{name.substring(PREFIX.length()), time, current.contains(name) ? "執行中" : "", (size >> 20) + " MB"});
        }
        AdminUi.rows(table, rows);
        String shown = folders.stream().map(f -> f.getFileName().toString()).filter(current::contains).findFirst()
                .map(n -> n.substring(PREFIX.length())).orElse(current.isEmpty() ? "（讀不到 formal-runtime.json）" : current);
        running.setText(shown);
        ctx.call("ops.health", r -> {
            onlineCharacters = r.path("ok").asBoolean() ? r.path("onlineCharacters").asInt() : 0;
            AdminUi.colored(online, r.path("ok").asBoolean() ? String.valueOf(onlineCharacters) : "伺服器未回應",
                    onlineCharacters > 0 ? AdminUi.WARN : AdminUi.OK);
        });
    }

    /** {@code <home>\..\..\launcher\formal-runtime.json} "jar" (bin/formal-snapshot-x/…jar), "" when unreadable. */
    private String currentJar() {
        Path runtime = home.resolve("..").resolve("..").resolve("launcher").resolve("formal-runtime.json").normalize();
        try {
            return JSON.readTree(Files.readString(runtime)).path("jar").asText("");
        } catch (IOException | RuntimeException e) {
            return "";
        }
    }

    /** Snapshot folders holding both the deploy script and the jar, newest jar first. */
    static List<Path> snapshots(Path bin) {
        if (!Files.isDirectory(bin)) {
            return List.of();
        }
        try (Stream<Path> s = Files.list(bin)) {
            return s.filter(p -> p.getFileName().toString().startsWith(PREFIX) && Files.isRegularFile(p.resolve("Deploy.ps1"))
                            && Files.isRegularFile(p.resolve(JAR)))
                    .sorted(Comparator.comparing(DeployTab::modified).reversed()).limit(40).toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private static FileTime modified(Path folder) {
        try {
            return Files.getLastModifiedTime(folder.resolve(JAR));
        } catch (IOException e) {
            return FileTime.fromMillis(0);
        }
    }

    private void start(boolean rollback) {
        int row = table.getSelectedRow();
        if (row < 0 || row >= folders.size()) {
            ctx.info("部署", "請先在列表選一個包。");
            return;
        }
        Path folder = folders.get(row);
        String name = folder.getFileName().toString().substring(PREFIX.length());
        String what = rollback ? "回復到部署「" + name + "」之前的版本" : "部署「" + name + "」";
        String warn = onlineCharacters > 0 ? "\n\n目前有 " + onlineCharacters + " 個角色在線：會先廣播、把他們踢下線（先存檔）。" : "";
        if (!ctx.typed(rollback ? "回復" : "部署", "要" + what + "嗎？伺服器會重啟，所有人都會斷線。" + warn + "\n\n請輸入包的名稱確認：", name)) {
            return;
        }
        output.setText("準備中…\n");
        if (onlineCharacters > 0) {
            ctx.call("notice.send", Map.of("text", "伺服器即將重啟更新，請稍後重新登入。"), n ->
                    ctx.call("ops.online", o -> {
                        List<String> ids = new ArrayList<>();
                        o.path("characters").forEach(c -> ids.add(c.path("id").asText()));
                        kickThenRun(ids, 0, folder, rollback);
                    }));
        } else {
            run(folder, rollback);
        }
    }

    private void announce() {
        long minutes;
        try {
            minutes = Long.parseLong(noticeMinutes.getText().trim());
        } catch (NumberFormatException e) {
            minutes = -1;
        }
        if (minutes < 0 || minutes > 600) {
            ctx.info("預告維護重啟", "「幾分鐘後」請填 0 到 600 的數字。");
            return;
        }
        String text = noticeText.getText().trim();
        long m = minutes;
        ctx.call("ops.restartNotice", Map.of("minutes", m, "text", text), r -> {
            if (!r.path("ok").asBoolean()) {
                ctx.status("預告維護重啟：" + AdminText.failure(r));
                return;
            }
            String line = (m == 0 ? "伺服器即將維護重啟。" : "伺服器將在 " + m + " 分鐘後維護重啟。") + text;
            ctx.call("notice.send", Map.of("text", line), n -> ctx.status("已預告：" + line + (n.path("ok").asBoolean() ? "（遊戲內也已廣播）" : "（遊戲內廣播失敗）")));
        });
    }

    private void kickThenRun(List<String> ids, int index, Path folder, boolean rollback) {
        if (index >= ids.size()) {
            Timer wait = new Timer(4000, e -> run(folder, rollback)); // let the saves and disconnects finish
            wait.setRepeats(false);
            wait.start();
            return;
        }
        ctx.call("ops.kick", Map.of("character", ids.get(index)), r -> kickThenRun(ids, index + 1, folder, rollback));
    }

    private void run(Path folder, boolean rollback) {
        Path script = folder.resolve("Deploy.ps1");
        Path log = folder.resolve("deploy-run.log");
        String inner = "cmd.exe /c powershell -NoProfile -ExecutionPolicy Bypass -File \"" + script + "\"" + (rollback ? " -Rollback" : "")
                + " > \"" + log + "\" 2>&1";
        String command = "Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{CommandLine='"
                + inner.replace("'", "''") + "';CurrentDirectory='" + folder.toString().replace("'", "''") + "'} | Out-Null";
        ctx.run(() -> {
            Files.deleteIfExists(log);
            Process p = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", command)
                    .redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            return p.waitFor();
        }, code -> {
            if (code != 0) {
                output.append("無法啟動部署腳本（PowerShell 結束代碼 " + code + "）。\n");
                return;
            }
            watching = log;
            output.setText("已啟動部署腳本，等待輸出…\n");
            tail.start();
            ctx.status((rollback ? "回復" : "部署") + "已開始：" + folder.getFileName());
        });
    }

    private void showLog() {
        if (watching == null || !Files.isRegularFile(watching)) {
            return;
        }
        try {
            String text = new String(Files.readAllBytes(watching), Charset.defaultCharset());
            if (text.length() > 20000) {
                text = text.substring(text.length() - 20000);
            }
            output.setText(text);
            output.setCaretPosition(output.getDocument().getLength());
            if (text.contains("新伺服器 PID") || text.contains("FullyQualifiedErrorId") || text.contains("請先關閉遊戲")) {
                tail.stop();
                refresh();
            }
        } catch (IOException e) {
            // the script still holds the file; try again on the next tick
        }
    }
}
