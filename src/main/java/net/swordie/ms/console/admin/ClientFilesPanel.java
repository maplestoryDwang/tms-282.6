package net.swordie.ms.console.admin;

import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 「發佈」→「客戶端檔案」（cat-launcher 1.2.3）：release-sign.mjs client-sign 的輸出（dist-release\client\&lt;版本&gt;\）匯入成
 * 伺服器發佈的客戶端遊戲檔（data\launcher\client-files\&lt;版本&gt;\，逐檔比對大小與 SHA-256 後設成目前版本）；停止發佈。
 * 匯入在伺服器的背景執行緒跑（檔案可能好幾 GB），這裡每 1.5 秒問一次進度。伺服器不簽章、沒有私鑰。
 */
final class ClientFilesPanel {
    private final AdminConsoleContext ctx;
    private final JPanel root = AdminUi.panel(new BorderLayout(8, 8));
    private final JLabel live = AdminUi.value();
    private final JLabel liveFiles = AdminUi.label("-");
    private final JLabel jobLine = AdminUi.label(" ");
    private final JLabel downloads = AdminUi.label("-");
    private final JTable candidates = AdminUi.table(new String[]{"版本", "檔案數", "大小", "金鑰", "狀態"}, new int[]{55, 55, 75, 140, 0});
    private final JTable imported = AdminUi.table(new String[]{"版本", "檔案數", "大小", "狀態"}, new int[]{55, 55, 75, 0});
    private final List<String> candidateVersions = new ArrayList<>();
    private final Timer poll;
    private String activeVersion = "";

    ClientFilesPanel(AdminConsoleContext ctx) {
        this.ctx = ctx;
        poll = new Timer(1500, e -> refresh());
        poll.setRepeats(false);
        JPanel info = AdminUi.panel(new GridLayout(4, 1, 0, 2));
        info.add(live);
        info.add(liveFiles);
        info.add(downloads);
        info.add(jobLine);
        JButton importButton = AdminUi.button("匯入選取的版本");
        importButton.addActionListener(e -> importSelected());
        JButton disable = AdminUi.button("停止發佈");
        disable.addActionListener(e -> disable());
        JPanel cand = AdminUi.panel(new BorderLayout(0, 4));
        cand.add(AdminUi.scroll(candidates), BorderLayout.CENTER);
        cand.add(AdminUi.row(importButton, disable, AdminUi.hint("逐檔比對大小與 SHA-256 後才設成目前版本；不能降版")), BorderLayout.SOUTH);
        JPanel lists = AdminUi.panel(new GridLayout(1, 2, 8, 0));
        lists.add(AdminUi.section("可匯入（client-sign 的輸出：dist-release\\client\\<版本>\\）", cand));
        lists.add(AdminUi.section("已匯入（data\\launcher\\client-files\\）", AdminUi.scroll(imported)));
        root.add(info, BorderLayout.NORTH);
        root.add(lists, BorderLayout.CENTER);
    }

    JComponent component() {
        return root;
    }

    void refresh() {
        ctx.call("clientfiles.get", this::show);
    }

    private void show(JsonNode r) {
        if (!r.path("ok").asBoolean()) {
            AdminUi.colored(live, "讀不到客戶端檔案的狀態：" + AdminText.failure(r), AdminUi.BAD);
            return;
        }
        JsonNode l = r.path("live");
        String state = AdminUi.text(l, "state");
        activeVersion = AdminUi.text(l, "activeVersion");
        JsonNode sum = l.path("summary");
        switch (state) {
            case "ok" -> AdminUi.colored(live, "正在發佈 " + AdminUi.text(sum, "version") + "（" + sum.path("count").asInt() + " 個檔，"
                    + AdminText.bytes(sum.path("totalSize").asLong()) + (l.path("test").asBoolean() ? "，測試金鑰" : "") + "）：登入器會看到「下載並覆蓋」", AdminUi.OK);
            case "none" -> AdminUi.colored(live, "沒有發佈客戶端檔案（登入器不會顯示遊戲檔那一列）", AdminUi.MUTED);
            default -> AdminUi.colored(live, activeVersion + " 沒有提供：" + problemText(state + (l.path("problem").isTextual() ? "." + AdminUi.text(l, "problem") : "")), AdminUi.BAD);
        }
        List<String> names = new ArrayList<>();
        l.path("files").forEach(f -> names.add(AdminUi.text(f, "path")));
        liveFiles.setText(names.isEmpty() ? " " : "檔案：" + String.join("、", names.size() > 6 ? names.subList(0, 6) : names) + (names.size() > 6 ? "…（共 " + names.size() + " 個）" : ""));
        JsonNode d = r.path("downloads");
        downloads.setText("下載中 " + d.path("active").size() + " 個（每台 1 個、全體 4 個）・啟動後共 " + d.path("started").asLong() + " 次・被擋 " + d.path("refused").asLong() + " 次");
        JsonNode job = r.path("job");
        String js = AdminUi.text(job, "state");
        switch (js) {
            case "running" -> {
                AdminUi.colored(jobLine, "匯入 " + AdminUi.text(job, "version") + " 中：" + job.path("done").asInt() + " / " + job.path("total").asInt() + " 個（"
                        + AdminText.bytes(job.path("bytes").asLong()) + "）…", AdminUi.MUTED);
                poll.restart();
            }
            case "done" -> AdminUi.colored(jobLine, "已匯入 " + AdminUi.text(job, "version") + "（" + job.path("total").asInt() + " 個、" + AdminText.bytes(job.path("bytes").asLong())
                    + "），伺服器 5 秒內提供。", AdminUi.OK);
            case "failed" -> AdminUi.colored(jobLine, "匯入 " + AdminUi.text(job, "version") + " 失敗：" + problemText(AdminUi.text(job, "problem")), AdminUi.BAD);
            default -> jobLine.setText(" ");
        }
        candidateVersions.clear();
        boolean allowTest = r.path("allowTestKey").asBoolean();
        List<Object[]> rows = new ArrayList<>();
        for (JsonNode n : r.path("candidates")) {
            candidateVersions.add(AdminUi.text(n, "version"));
            boolean test = n.path("test").asBoolean();
            String key = test ? "測試金鑰" + (allowTest ? "" : "（不能匯入）") : AdminUi.text(n, "keyId").isEmpty() ? "-" : AdminUi.text(n, "keyId");
            String problem = AdminUi.text(n, "problem");
            rows.add(new Object[]{AdminUi.text(n, "version"), n.path("count").asInt(), AdminText.bytes(n.path("totalSize").asLong()), key,
                    problem.isEmpty() ? "可以匯入" : problemText(problem)});
        }
        AdminUi.rows(candidates, rows);
        if (!rows.isEmpty() && candidates.getSelectedRow() < 0) {
            candidates.setRowSelectionInterval(0, 0);
        }
        List<Object[]> imp = new ArrayList<>();
        for (JsonNode n : r.path("imported")) {
            String v = AdminUi.text(n, "version");
            String problem = AdminUi.text(n, "problem");
            imp.add(new Object[]{v, n.path("count").asInt(), AdminText.bytes(n.path("totalSize").asLong()),
                    !problem.isEmpty() ? problemText(problem) : v.equals(activeVersion) && "ok".equals(state) ? "發佈中" : "未發佈"});
        }
        AdminUi.rows(imported, imp);
    }

    private void importSelected() {
        int row = candidates.getSelectedRow();
        if (row < 0 || row >= candidateVersions.size()) {
            ctx.error("匯入客戶端檔案", "請先選一個版本（dist-release\\client 裡沒有東西時，請先執行 node scripts\\release-sign.mjs client-sign --version x.y.z）。");
            return;
        }
        String v = candidateVersions.get(row);
        if (!ctx.confirm("匯入客戶端檔案 " + v, "要匯入客戶端檔案 " + v + " 嗎？\n\n會把檔案複製到 data\\launcher\\client-files\\" + v + "\\（逐檔比對大小與 SHA-256），\n"
                + "再設成目前發佈的版本。朋友按「下載並覆蓋」時，登入器會先驗簽章、確認遊戲沒在跑、備份原檔再替換。")) {
            return;
        }
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("version", v);
        ctx.call("clientfiles.import", p, r -> {
            if (r.path("ok").asBoolean()) {
                AdminUi.colored(jobLine, "開始匯入 " + v + "（" + r.path("count").asInt() + " 個、" + AdminText.bytes(r.path("totalSize").asLong()) + "）…", AdminUi.MUTED);
                poll.restart();
            } else {
                AdminUi.colored(jobLine, "沒有匯入：" + problemText(AdminUi.text(r, "problem")) + (r.has("active") ? "（目前發佈 " + AdminUi.text(r, "active") + "）" : ""), AdminUi.BAD);
            }
        });
    }

    private void disable() {
        if (!ctx.confirm("停止發佈客戶端檔案", "要停止發佈客戶端檔案嗎？\n登入器 5 秒內不再看到「下載並覆蓋」；已匯入的資料夾會留著，之後可以重新匯入同一版。")) {
            return;
        }
        ctx.call("clientfiles.disable", r -> {
            if (r.path("ok").asBoolean()) {
                AdminUi.colored(jobLine, "已停止發佈。", AdminUi.OK);
                refresh();
            } else {
                AdminUi.colored(jobLine, "沒有停止：" + AdminText.failure(r), AdminUi.BAD);
            }
        });
    }

    static String problemText(String p) {
        if (p == null || p.isEmpty()) {
            return "不明原因";
        }
        if (p.startsWith("list.testKey")) return "測試金鑰簽的清單只能匯入測試伺服器";
        if (p.startsWith("list.missing")) return "沒有 client-files.json";
        if (p.startsWith("list.version.folder") || p.startsWith("invalid.version.folder")) return "清單的版本跟資料夾名稱不同";
        if (p.startsWith("list.")) return "client-files.json 格式不對（" + p.substring(5) + "）";
        if (p.startsWith("version.older")) return "不能降版：比目前發佈的版本舊";
        if (p.startsWith("target.exists")) return "伺服器上已經有同版本但內容不同的資料夾（請換一個版本號）";
        if (p.startsWith("target.incomplete")) return "伺服器上的同版本資料夾缺檔";
        if (p.startsWith("file.missing")) return "少了檔案";
        if (p.startsWith("file.size")) return "檔案大小跟清單不同";
        if (p.startsWith("file.sha256")) return "檔案內容（SHA-256）跟清單不同";
        if (p.startsWith("import.running")) return "另一個匯入還在進行";
        if (p.startsWith("dist.missing")) return "dist-release\\client 裡沒有這個版本";
        if (p.startsWith("fileMissing")) return "缺檔（" + p.substring(Math.min(p.length(), 12)) + "）";
        if (p.startsWith("sizeMismatch")) return "檔案大小不對（" + p.substring(Math.min(p.length(), 13)) + "）";
        if (p.startsWith("invalid")) return "清單有問題（" + p + "）";
        return p;
    }
}
