package net.swordie.ms.console.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import javax.swing.*;
import java.awt.*;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 「發佈」：登入器更新（P8／P8b）。顯示 data\launcher\release.json（原始內容與欄位）、伺服器現在給登入器的 launcherVersion
 * （download 有沒有提供、沒有的原因）、data\launcher\releases\ 的檔案、manifestMeta 的值、自伺服器啟動以來看到的登入器版本；
 * 編輯 latest／minSupported／notes／publishedAt（伺服器驗證、留 release.json.bak、原子寫入）；匯入 release-sign.mjs 的輸出
 * （dist-release\&lt;版本&gt;\：release-entry.json＋exe）。提高 minSupported 由伺服器擋下要求確認，主控台再問一次「朋友都換成新版了嗎」。
 * 主控台不產生、不讀取、不保存任何簽章金鑰：簽章原樣來自 release-entry.json。
 */
final class ReleaseTab implements AdminTab {
    private final AdminConsoleContext ctx;
    private final JPanel root = AdminUi.panel(new BorderLayout(8, 8));
    private final JTextField latest = AdminUi.field(10);
    private final JTextField minSupported = AdminUi.field(10);
    private final JTextField publishedAt = AdminUi.field(22);
    private final JTextArea notes = new JTextArea(6, 30);
    private final JLabel downloadInfo = AdminUi.label("-");
    private final JLabel fileState = AdminUi.label(" ");
    private final JLabel check = AdminUi.label(" ");
    private final JLabel liveStatus = AdminUi.value();
    private final JLabel liveVersions = AdminUi.label("-");
    private final JLabel liveDownload = AdminUi.label("-");
    private final JLabel manifest = AdminUi.label("-");
    private final JLabel manifestExe = AdminUi.hint(" ");
    private final JLabel seen = AdminUi.label("-");
    private final JLabel downloads = AdminUi.label("-");
    private final JTextArea raw = new JTextArea(5, 30);
    private final JTable releases = AdminUi.table(new String[]{"檔名", "大小", "修改時間", "狀態"}, new int[]{200, 65, 85, 0});
    private final JTable candidates = AdminUi.table(new String[]{"版本", "檔名", "大小", "金鑰", "狀態"}, new int[]{50, 195, 65, 140, 0});
    private final List<String> candidateVersions = new ArrayList<>();
    private String baseSha = "";
    private String loadedForm = "";
    private String currentMin = "";
    private JsonNode versionsSeen;
    /** 1.2.3：下方的「客戶端檔案」分頁（client-sign 的輸出匯入、停止發佈） */
    private final ClientFilesPanel clientFiles;

    ReleaseTab(AdminConsoleContext ctx) {
        this.ctx = ctx;
        this.clientFiles = new ClientFilesPanel(ctx);
        notes.setFont(AdminUi.font(Font.PLAIN, 13));
        notes.setLineWrap(false);
        raw.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        raw.setEditable(false);

        JPanel form = AdminUi.panel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 4, 3, 4);
        g.anchor = GridBagConstraints.WEST;
        int y = 0;
        y = row(form, g, y, "最新版 latest", latest, "x.y.z，例如 1.2.0");
        y = row(form, g, y, "最低支援 minSupported", minSupported, "低於這版的登入器會被擋在登入頁");
        JButton now = AdminUi.button("現在");
        now.addActionListener(e -> publishedAt.setText(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
        g.gridx = 0;
        g.gridy = y;
        form.add(AdminUi.label("發佈時間 publishedAt"), g);
        g.gridx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        form.add(publishedAt, g);
        g.fill = GridBagConstraints.NONE;
        g.weightx = 0;
        g.gridx = 2;
        form.add(now, g);
        y++;
        g.gridx = 0;
        g.gridy = y;
        g.anchor = GridBagConstraints.NORTHWEST;
        form.add(AdminUi.label("更新說明 notes"), g);
        g.gridx = 1;
        g.gridwidth = 2;
        g.fill = GridBagConstraints.BOTH;
        g.weightx = 1;
        g.weighty = 1;
        JScrollPane ns = AdminUi.scroll(notes);
        ns.setPreferredSize(new Dimension(320, 120));
        form.add(ns, g);
        g.gridwidth = 1;
        g.fill = GridBagConstraints.NONE;
        g.weightx = 0;
        g.weighty = 0;
        g.anchor = GridBagConstraints.WEST;
        y++;
        g.gridx = 1;
        g.gridy = y;
        g.gridwidth = 2;
        form.add(AdminUi.hint("一行一則，最多 10 行、每行 100 字以內；純文字（網址不能點）"), g);
        y++;
        g.gridx = 0;
        g.gridy = y;
        g.gridwidth = 1;
        form.add(AdminUi.label("下載 download"), g);
        g.gridx = 1;
        g.gridwidth = 2;
        form.add(downloadInfo, g);
        g.gridwidth = 1;

        JButton validate = AdminUi.button("驗證");
        JButton save = AdminUi.button("儲存到伺服器");
        JButton reload = AdminUi.button("重新載入");
        validate.addActionListener(e -> validateForm());
        save.addActionListener(e -> save());
        reload.addActionListener(e -> reloadClicked());
        JPanel left = AdminUi.panel(new BorderLayout(0, 4));
        left.add(form, BorderLayout.CENTER);
        JPanel actions = AdminUi.panel(new GridLayout(3, 1));
        actions.add(AdminUi.row(validate, save, reload));
        actions.add(AdminUi.row(check));
        actions.add(AdminUi.row(fileState));
        left.add(actions, BorderLayout.SOUTH);

        JPanel live = AdminUi.panel(new GridBagLayout());
        GridBagConstraints l = new GridBagConstraints();
        l.insets = new Insets(2, 4, 2, 4);
        l.anchor = GridBagConstraints.WEST;
        int ly = 0;
        ly = liveRow(live, l, ly, "launcherVersion", liveStatus);
        ly = liveRow(live, l, ly, "版本", liveVersions);
        ly = liveRow(live, l, ly, "下載", liveDownload);
        ly = liveRow(live, l, ly, "manifestMeta", manifest);
        ly = liveRow(live, l, ly, "", manifestExe);
        ly = liveRow(live, l, ly, "登入器版本", seen);
        ly = liveRow(live, l, ly, "下載中", downloads);
        l.gridx = 0;
        l.gridy = ly;
        l.gridwidth = 2;
        l.fill = GridBagConstraints.BOTH;
        l.weightx = 1;
        l.weighty = 1;
        JScrollPane rs = AdminUi.scroll(raw);
        rs.setPreferredSize(new Dimension(300, 90));
        live.add(rs, l);

        JPanel top = AdminUi.panel(new GridLayout(1, 2, 8, 0));
        top.add(AdminUi.section("編輯 release.json（登入器看到的版本資訊）", left));
        top.add(AdminUi.section("伺服器現在提供的內容（下面是 release.json 原始內容）", live));

        JButton importButton = AdminUi.button("匯入選取的版本");
        importButton.addActionListener(e -> importSelected());
        JPanel cand = AdminUi.panel(new BorderLayout(0, 4));
        cand.add(AdminUi.scroll(candidates), BorderLayout.CENTER);
        cand.add(AdminUi.row(importButton, AdminUi.hint("複製 exe，改 latest 與 download（minSupported 不變）")),
                BorderLayout.SOUTH);
        JPanel bottom = AdminUi.panel(new GridLayout(1, 2, 8, 0));
        bottom.add(AdminUi.section("data\\launcher\\releases\\（只會提供 release.json 登記的那一個）", AdminUi.scroll(releases)));
        bottom.add(AdminUi.section("可匯入（release-sign.mjs 的輸出：dist-release\\<版本>\\）", cand));
        JTabbedPane lower = new JTabbedPane();
        lower.setFont(AdminUi.font(Font.PLAIN, 13));
        lower.addTab("登入器 exe", bottom);
        lower.addTab("客戶端檔案", clientFiles.component());
        lower.setPreferredSize(new Dimension(100, 262));

        root.add(top, BorderLayout.CENTER);
        root.add(lower, BorderLayout.SOUTH);
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        loadedForm = formState();
    }

    private static int row(JPanel form, GridBagConstraints g, int y, String label, JComponent field, String hint) {
        g.gridx = 0;
        g.gridy = y;
        form.add(AdminUi.label(label), g);
        g.gridx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        field.setMinimumSize(new Dimension(110, field.getPreferredSize().height));
        form.add(field, g);
        g.fill = GridBagConstraints.NONE;
        g.weightx = 0;
        g.gridx = 2;
        form.add(AdminUi.hint(hint), g);
        return y + 1;
    }

    private static int liveRow(JPanel p, GridBagConstraints g, int y, String label, JComponent value) {
        g.gridx = 0;
        g.gridy = y;
        g.gridwidth = 1;
        g.fill = GridBagConstraints.NONE;
        g.weightx = 0;
        g.weighty = 0;
        p.add(AdminUi.label(label), g);
        g.gridx = 1;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        p.add(value, g);
        return y + 1;
    }

    @Override
    public String title() {
        return "發佈";
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public void refresh() {
        ctx.call("release.get", this::show);
        clientFiles.refresh();
    }

    @Override
    public boolean hasUnsavedChanges() {
        return !loadedForm.equals(formState());
    }

    private String formState() {
        return String.join("\u0000", latest.getText(), minSupported.getText(), publishedAt.getText(), notes.getText());
    }

    private void reloadClicked() {
        if (hasUnsavedChanges() && !ctx.confirm("重新載入", "release.json 有還沒存到伺服器的變更。\n重新載入會丟掉這些變更，改用伺服器上的檔案。要繼續嗎？")) {
            return;
        }
        refresh();
    }

    void show(JsonNode r) {
        if (!r.path("ok").asBoolean()) {
            ctx.status("發佈：" + AdminText.failure(r));
            return;
        }
        baseSha = r.path("sha256").asText();
        JsonNode f = r.path("fields");
        latest.setText(AdminUi.text(f, "latest"));
        minSupported.setText(AdminUi.text(f, "minSupported"));
        currentMin = AdminUi.text(f, "minSupported");
        publishedAt.setText(AdminUi.text(f, "publishedAt"));
        List<String> lines = new ArrayList<>();
        f.path("notes").forEach(n -> lines.add(n.asText()));
        notes.setText(String.join("\n", lines));
        notes.setCaretPosition(0);
        JsonNode d = f.get("download");
        downloadInfo.setText(d == null || d.isNull() ? "（沒有：登入器不會出現「下載並更新」）"
                : AdminUi.text(d, "version") + "　" + AdminUi.text(d, "file") + "　" + AdminText.bytes(d.path("size").asLong()));
        downloadInfo.setToolTipText(d == null || d.isNull() ? null : "sha256 " + AdminUi.text(d, "sha256"));
        loadedForm = formState();
        String problem = AdminUi.text(r, "problem");
        String status = !r.path("exists").asBoolean() ? "還沒有 release.json（登入器顯示「無法辨識」；儲存或匯入後建立）"
                : problem.isEmpty() ? "release.json 格式正確" : "release.json 目前有錯：" + AdminText.releaseProblem(problem) + "（伺服器沿用上一個正確版本）";
        AdminUi.colored(fileState, status, problem.isEmpty() ? AdminUi.MUTED : AdminUi.BAD);
        check.setText(" ");
        raw.setText(AdminUi.text(r, "text").isEmpty() ? "（沒有檔案）" : AdminUi.text(r, "text"));
        raw.setCaretPosition(0);
        showLive(r.path("live"));
        showManifest(r.path("manifest"));
        versionsSeen = r.path("launcherVersions");
        seen.setText(AdminText.versions(versionsSeen));
        JsonNode dl = r.path("downloads");
        downloads.setText(dl.path("active").size() + " 個（上限每 IP " + dl.path("perIp").asInt() + "、全體 " + dl.path("total").asInt()
                + "）；啟動以來 " + dl.path("started").asLong() + " 次、因同時下載數拒絕 " + dl.path("refused").asLong() + " 次");
        showReleases(r.path("releases"), r.path("live"));
        showCandidates(r.path("candidates"), r.path("allowTestKey").asBoolean());
    }

    private void showLive(JsonNode live) {
        String status = AdminUi.text(live, "status");
        JsonNode info = live.path("info");
        if (info.isMissingNode() || info.isNull()) {
            AdminUi.colored(liveStatus, "unavailable（沒有有效的 release.json）", AdminUi.BAD);
            liveVersions.setText("-");
        } else {
            AdminUi.colored(liveStatus, "ok" + ("invalid".equals(status) ? "（檔案有錯，沿用上一個正確版本）" : ""),
                    "invalid".equals(status) ? AdminUi.WARN : AdminUi.OK);
            liveVersions.setText("latest " + AdminUi.text(info, "latest") + "、minSupported " + AdminUi.text(info, "minSupported")
                    + "、說明 " + info.path("notes").size() + " 行");
        }
        JsonNode d = live.path("download");
        String state = AdminUi.text(live, "downloadState");
        if (d.isObject()) {
            AdminUi.colored(liveDownload, "提供 " + AdminUi.text(d, "file") + "（" + AdminText.bytes(d.path("size").asLong()) + "，已核對）", AdminUi.OK);
            liveDownload.setToolTipText("版本 " + AdminUi.text(d, "version") + "，大小與 SHA-256 已核對：" + AdminUi.text(d, "sha256"));
        } else {
            AdminUi.colored(liveDownload, AdminText.downloadState(state), "none".equals(state) ? AdminUi.MUTED : AdminUi.BAD);
        }
    }

    private void showManifest(JsonNode m) {
        if (m.path("ok").asBoolean()) {
            AdminUi.colored(manifest, "參考檔 " + AdminText.shortHash(AdminUi.text(m, "referenceSha256")) + "、exe "
                    + AdminText.shortHash(AdminUi.text(m, "exeSha256")) + "、" + m.path("count").asInt() + " 個檔案", AdminUi.OK);
        } else {
            AdminUi.colored(manifest, "讀不到（" + AdminText.manifestProblem(AdminUi.text(m, "problem")) + "）：manifestMeta 回 unavailable", AdminUi.BAD);
        }
        manifestExe.setText("客戶端 " + AdminUi.text(m, "clientExe"));
    }

    private void showReleases(JsonNode arr, JsonNode live) {
        List<Object[]> rows = new ArrayList<>();
        String state = AdminUi.text(live, "downloadState");
        for (JsonNode n : arr) {
            String s;
            if (n.path("registered").asBoolean()) {
                s = "ok".equals(state) ? "登記中・提供下載" : "登記中・不提供";
            } else {
                s = n.path("validName").asBoolean() ? "未登記" : "未登記・檔名不合規則";
            }
            rows.add(new Object[]{AdminUi.text(n, "name"), AdminText.bytes(n.path("size").asLong()), AdminUi.shortTime(n.path("modified")), s});
        }
        AdminUi.rows(releases, rows);
    }

    private void showCandidates(JsonNode arr, boolean allowTest) {
        candidateVersions.clear();
        List<Object[]> rows = new ArrayList<>();
        for (JsonNode n : arr) {
            candidateVersions.add(AdminUi.text(n, "version"));
            boolean test = n.path("test").asBoolean();
            String key = test ? "測試金鑰" + (allowTest ? "" : "（不能匯入）") : AdminUi.text(n, "keyId").isEmpty() ? "-" : AdminUi.text(n, "keyId");
            String problem = AdminUi.text(n, "problem");
            rows.add(new Object[]{AdminUi.text(n, "version"), AdminUi.text(n, "file"), AdminText.bytes(n.path("size").asLong()), key,
                    problem.isEmpty() ? "可以匯入" : AdminText.releaseProblem(problem)});
        }
        AdminUi.rows(candidates, rows);
        if (!rows.isEmpty()) {
            candidates.setRowSelectionInterval(0, 0);
        }
    }

    private Map<String, Object> edit() {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("latest", latest.getText().strip());
        p.put("minSupported", minSupported.getText().strip());
        ArrayNode arr = HttpAdminGateway.JSON.createArrayNode();
        for (String line : notes.getText().split("\\R")) {
            if (!line.isBlank()) {
                arr.add(line.strip());
            }
        }
        p.put("notes", arr);
        p.put("publishedAt", publishedAt.getText().strip());
        return p;
    }

    private void validateForm() {
        ctx.call("release.validate", edit(), r -> {
            if (r.path("ok").asBoolean()) {
                AdminUi.colored(check, "驗證通過" + (r.path("raise").asBoolean() ? "（注意：minSupported 會從 " + AdminUi.text(r, "fromMin")
                        + " 提高，儲存時要再確認）" : ""), r.path("raise").asBoolean() ? AdminUi.WARN : AdminUi.OK);
            } else {
                AdminUi.colored(check, "驗證失敗：" + AdminText.releaseProblem(AdminUi.text(r, "problem")), AdminUi.BAD);
            }
        });
    }

    private void save() {
        if (!ctx.confirm("儲存 release.json", "要把 latest " + latest.getText().strip() + "、minSupported " + minSupported.getText().strip()
                + " 寫入伺服器的 release.json 嗎？\n舊檔會留成 release.json.bak，登入器 5 秒內看到。download 不會改。")) {
            return;
        }
        Map<String, Object> p = edit();
        p.put("baseSha256", baseSha);
        save(p, false);
    }

    private void save(Map<String, Object> p, boolean confirmRaise) {
        Map<String, Object> q = new LinkedHashMap<>(p);
        q.put("confirmRaise", confirmRaise);
        ctx.call("release.save", q, r -> {
            if (r.path("ok").asBoolean()) {
                baseSha = r.path("sha256").asText();
                loadedForm = formState();
                currentMin = AdminUi.text(r.path("info"), "minSupported");
                AdminUi.colored(check, "已儲存；伺服器 " + r.path("reloadSec").asInt() + " 秒內重新讀取"
                        + (r.path("backup").isNull() ? "" : "，舊檔備份：" + r.path("backup").asText()), AdminUi.OK);
                refresh();
            } else if ("confirmRequired".equals(AdminUi.text(r, "code"))) {
                if (confirmRaise(AdminUi.text(r, "fromMin"), AdminUi.text(r, "minSupported"))) {
                    save(p, true);
                } else {
                    AdminUi.colored(check, "沒有儲存：minSupported 沒有提高（還有朋友用舊版的話，先讓他們更新）", AdminUi.WARN);
                }
            } else {
                AdminUi.colored(check, "沒有儲存：" + AdminText.failure(r) + releaseHint(r), AdminUi.BAD);
            }
        });
    }

    private static String releaseHint(JsonNode r) {
        String problem = AdminUi.text(r, "problem");
        return problem.isEmpty() ? "" : "（" + AdminText.releaseProblem(problem) + "）";
    }

    /** The second question before raising minSupported. */
    boolean confirmRaise(String from, String to) {
        return ctx.confirm("提高 minSupported", "要把 minSupported 從 " + from + " 提高到 " + to + " 嗎？\n\n"
                + "朋友都換成新版了嗎？\n低於 " + to + " 的登入器（遠端模式）會停用登入按鈕，只能向你拿新版。\n"
                + "自伺服器啟動以來看到的登入器版本：" + AdminText.versions(versionsSeen) + "\n\n確定每個朋友都已經更新了，再按「確定」。");
    }

    private void importSelected() {
        int row = candidates.getSelectedRow();
        if (row < 0 || row >= candidateVersions.size()) {
            ctx.error("匯入", "請先在右下的清單選一個版本（dist-release 裡沒有東西時，請先執行 release-sign.mjs sign）。");
            return;
        }
        if (hasUnsavedChanges()) {
            ctx.error("匯入", "release.json 有還沒儲存的變更：請先儲存或重新載入，再匯入。");
            return;
        }
        String v = candidateVersions.get(row);
        if (!ctx.confirm("匯入 " + v, "要匯入 " + v + " 嗎？\n\n會把 exe 複製到 data\\launcher\\releases\\（先核對大小與 SHA-256），\n"
                + "release.json 的 latest 改成 " + v + "（比現在新的話）、download 換成這一版、publishedAt 改成現在；\nminSupported 不變。"
                + "\n舊檔留成 release.json.bak。朋友按「下載並更新」時，登入器會先驗簽章再下載。")) {
            return;
        }
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("version", v);
        p.put("baseSha256", baseSha);
        ctx.call("release.import", p, r -> {
            if (r.path("ok").asBoolean()) {
                AdminUi.colored(check, "已匯入 " + v + "（" + AdminUi.text(r, "file") + (r.path("copied").asBoolean() ? "，已複製" : "，releases\\ 已有同一個檔案")
                        + "）；伺服器 5 秒內提供下載。朋友都更新後，再提高 minSupported。", AdminUi.OK);
                refresh();
            } else {
                AdminUi.colored(check, "沒有匯入：" + AdminText.failure(r) + releaseHint(r), AdminUi.BAD);
            }
        });
    }

    /** Checks: the field values as the form holds them. */
    Map<String, String> form() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("latest", latest.getText());
        m.put("minSupported", minSupported.getText());
        m.put("publishedAt", publishedAt.getText());
        m.put("notes", notes.getText());
        m.put("currentMin", currentMin);
        return m;
    }

    /** Checks: sets the form (as if typed). */
    void type(String latestText, String minText, String notesText) {
        latest.setText(latestText);
        minSupported.setText(minText);
        notes.setText(notesText);
    }
}
