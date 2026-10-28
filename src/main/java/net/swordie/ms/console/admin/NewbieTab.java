package net.swordie.ms.console.admin;

import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 「新手流程」(step 7): the configuration in use (newbie.get: state, file, lobby, steps), 「重新載入設定」 (newbie.reload:
 * the server reads data\newbie\newbie-flow.json again now; a wrong file keeps the previous valid version) and the
 * characters' progress (newbie.progress: step, branch, gift / first job / completion times). Read-only otherwise:
 * the file is edited with a text editor.
 */
final class NewbieTab implements AdminTab {
    private final AdminConsoleContext ctx;
    private final JPanel root = AdminUi.panel(new BorderLayout(8, 8));
    private final JLabel state = AdminUi.value();
    private final JLabel version = AdminUi.value();
    private final JLabel lobby = AdminUi.value();
    private final JLabel options = AdminUi.hint(" ");
    private final JLabel file = AdminUi.hint(" ");
    private final JLabel problem = AdminUi.hint(" ");
    private final JTable steps = AdminUi.table(new String[]{"順序", "步驟 ID", "類型", "內容"}, new int[]{40, 90, 100, 0});
    private final JTable progress = AdminUi.table(new String[]{"角色 ID", "名稱", "等級", "職業", "所在地圖", "種族", "下一步", "分支",
            "禮包", "一轉", "完成", "更新"}, new int[]{60, 110, 45, 55, 85, 90, 80, 70, 95, 95, 95, 95});
    private final JLabel progressInfo = AdminUi.hint(" ");
    private final JTextField query = AdminUi.field(14);
    private final JTextArea fileText = new JTextArea(8, 60);
    private final JTable jobs = AdminUi.table(new String[]{"職業 ID", "職業", "角色數"}, new int[]{90,250,90});
    private final JTabbedPane config = new JTabbedPane();

    NewbieTab(AdminConsoleContext ctx) {
        this.ctx = ctx;
        JPanel stats = AdminUi.row(AdminUi.stat("狀態", state), AdminUi.stat("設定版本", version), AdminUi.stat("新手大廳", lobby));
        JButton reload = AdminUi.button("重新載入設定");
        reload.addActionListener(e -> ctx.call("newbie.reload", this::showFlow));
        JButton refresh = AdminUi.button("重新整理");
        refresh.addActionListener(e -> refresh());
        JPanel head = AdminUi.panel(new BorderLayout());
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.add(stats);
        head.add(AdminUi.row(reload, refresh, AdminUi.hint("要改內容請用文字編輯器改 data\\newbie\\newbie-flow.json，存檔後按「重新載入設定」或等 2 秒。")));
        head.add(options);
        head.add(file);
        head.add(problem);
        for (Component child : head.getComponents()) ((JComponent) child).setAlignmentX(Component.LEFT_ALIGNMENT);

        fileText.setEditable(false);
        fileText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        config.addTab("步驟", AdminUi.scroll(steps));
        config.addTab("設定檔內容（唯讀）", new JScrollPane(fileText));
        config.addTab("職業統計（已存檔）", AdminUi.scroll(jobs));
        JPanel top = AdminUi.panel(new BorderLayout(0, 6));
        top.add(head, BorderLayout.NORTH);
        top.add(config, BorderLayout.CENTER);
        top.setPreferredSize(new Dimension(100, 330));

        JButton find = AdminUi.button("查詢");
        find.addActionListener(e -> loadProgress());
        query.addActionListener(e -> loadProgress());
        JPanel bottom = AdminUi.panel(new BorderLayout(0, 4));
        bottom.add(AdminUi.row(AdminUi.label("角色 ID 或名稱（空白 = 最近 200 筆）："), query, find), BorderLayout.NORTH);
        bottom.add(AdminUi.scroll(progress), BorderLayout.CENTER);
        bottom.add(progressInfo, BorderLayout.SOUTH);

        root.add(AdminUi.section("目前設定", top), BorderLayout.NORTH);
        root.add(AdminUi.section("各角色進度（newbie_flow）", bottom), BorderLayout.CENTER);
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    @Override
    public String title() {
        return "新手流程";
    }

    @Override
    public JComponent component() {
        return root;
    }

    void query(String q) {
        query.setText(q);
    }

    void selectJobs() { config.setSelectedIndex(2); }

    @Override
    public void refresh() {
        ctx.call("newbie.get", this::showFlow);
        loadProgress();
        ctx.call("newbie.jobs", r -> {
            if (!r.path("ok").asBoolean()) return;
            List<Object[]> rows = new ArrayList<>();
            r.path("jobs").forEach(j -> rows.add(new Object[]{j.path("job").asInt(),j.path("name").asText(),j.path("count").asInt()}));
            AdminUi.rows(jobs, rows);
        });
    }

    static String stateText(String s) {
        return switch (s) {
            case "ok" -> "使用設定檔";
            case "default" -> "內建預設（沒有設定檔）";
            case "invalid" -> "設定檔有誤，沿用上一份";
            default -> s;
        };
    }

    void showFlow(JsonNode r) {
        if (!r.path("ok").asBoolean()) {
            AdminUi.colored(state, "讀取失敗", AdminUi.BAD);
            problem.setText(AdminText.failure(r));
            return;
        }
        JsonNode f = r.path("flow");
        String s = f.path("state").asText();
        AdminUi.colored(state, stateText(s), "invalid".equals(s) ? AdminUi.BAD : f.path("enabled").asBoolean() ? AdminUi.OK : AdminUi.WARN);
        version.setText(f.path("version").asText() + (f.path("enabled").asBoolean() ? "" : "（停用）"));
        lobby.setText(String.valueOf(f.path("lobbyMap").asInt()));
        for (JLabel label : new JLabel[]{state, version, lobby}) {
            JComponent box = (JComponent) label.getParent();
            box.setPreferredSize(null);
            Dimension d = box.getPreferredSize();
            box.setPreferredSize(new Dimension(Math.max(118, d.width), d.height));
            box.revalidate();
        }
        options.setText("對話 NPC " + f.path("speakerNpc").asInt() + "　進場後 " + f.path("popupDelayMs").asInt() + " ms 彈出　登入時在大廳外也接續："
                + AdminUi.yesNo(f.path("resumeOnLogin").asBoolean()) + "　載入時間 " + AdminUi.time(f.path("loadedAt")));
        file.setText("設定檔：" + f.path("file").asText());
        String p = f.path("problem").asText();
        List<String> warn = new ArrayList<>();
        f.path("warnings").forEach(w -> warn.add(w.asText()));
        problem.setText(p.isEmpty() ? (warn.isEmpty() ? " " : "資料警告：" + String.join("；", warn)) : "問題：" + p);
        problem.setForeground(p.isEmpty() && warn.isEmpty() ? AdminUi.MUTED : AdminUi.BAD);
        List<Object[]> rows = new ArrayList<>();
        int i = 1;
        for (JsonNode st : f.path("steps")) {
            rows.add(new Object[]{i++, st.path("id").asText(), st.path("type").asText(), st.path("detail").asText()});
        }
        AdminUi.rows(steps, rows);
        String text = r.path("fileText").asText();
        fileText.setText(text.isEmpty() ? "（沒有設定檔：使用內建預設。部署時 Deploy.ps1 會放一份 newbie-flow.json 到 data\\newbie\\。）" : text);
        fileText.setCaretPosition(0);
    }

    private void loadProgress() {
        ctx.call("newbie.progress", Map.of("query", query.getText().strip(), "limit", 200), this::showProgress);
    }

    void showProgress(JsonNode r) {
        if (!r.path("ok").asBoolean()) {
            progressInfo.setText("讀不到進度：" + AdminText.failure(r));
            return;
        }
        List<Object[]> rows = new ArrayList<>();
        for (JsonNode n : r.path("rows")) {
            boolean done = n.path("completedAt").asLong() > 0;
            rows.add(new Object[]{n.path("characterId").asInt(), n.path("name").asText(), n.path("level").asInt(), n.path("job").asInt(),
                    n.path("posMap").asLong(), n.path("loginJob").asText(), done ? "（已完成）" : n.path("stepId").asText(),
                    n.path("branch").asText().isEmpty() ? "-" : n.path("branch").asText(), AdminUi.shortTime(n.path("giftClaimedAt")),
                    AdminUi.shortTime(n.path("jobDoneAt")), AdminUi.shortTime(n.path("completedAt")), AdminUi.shortTime(n.path("updatedAt"))});
        }
        AdminUi.rows(progress, rows);
        progressInfo.setText(rows.size() + " 筆" + (rows.size() >= 200 ? "（只顯示最近 200 筆）" : ""));
    }
}
