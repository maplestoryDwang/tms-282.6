package net.swordie.ms.console.admin;

import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 「連線安全」：檢視／修改 net.allow、net.maxPerIp、net.login.idleSec、api.http.allow（伺服器驗證、留備份、只改這幾行、
 * 熱重載生效；新名單必須包含 127.0.0.1）；最近被擋的來源 IP 與「加入 net.allow」；各網卡與監聽埠；防火牆規則唯讀顯示。
 */
final class NetSecurityTab implements AdminTab {
    private static final String[][] KEYS = {
            {"net.allow", "遊戲埠來源白名單（8484／頻道）", "本機限定版固定使用 127.0.0.1"},
            {"net.maxPerIp", "每個 IP 最多同時連線數", "0＝不限；一台電腦開一個遊戲約佔 2 條"},
            {"net.login.idleSec", "登入畫面閒置逾時（秒）", "0＝關閉；預設 60"},
            {"api.http.allow", "8600 登入器 API 來源白名單", "本機限定版固定使用 127.0.0.1"},
    };

    private final AdminConsoleContext ctx;
    private final JPanel root = AdminUi.panel(new BorderLayout(8, 8));
    private final Map<String, JTextField> fields = new LinkedHashMap<>();
    private final Map<String, JLabel> effective = new LinkedHashMap<>();
    private final Map<String, String> loaded = new LinkedHashMap<>();
    private final JLabel saveState = AdminUi.hint(" ");
    private final JTable refused = AdminUi.table(new String[]{"來源 IP", "次數", "原因", "埠", "最後被擋", "現在允許"}, new int[]{125, 45, 105, 45, 85, 60});
    private final JLabel refusedInfo = AdminUi.hint(" ");
    private final JTable interfaces = AdminUi.table(new String[]{"網卡", "類型", "位址", "遊戲埠", "8600"}, new int[]{165, 80, 110, 55, 50});
    private final JTable listeners = AdminUi.table(new String[]{"用途", "監聽位址", "埠"}, new int[]{120, 75, 45});
    private final JTable firewall = AdminUi.table(new String[]{"規則名稱", "啟用", "動作", "設定檔", "通訊協定", "本機埠", "遠端位址", "程式"},
            new int[]{170, 40, 50, 90, 60, 70, 120, 200});
    private final JLabel firewallInfo = AdminUi.hint("按「讀取防火牆」顯示（只讀取，不會新增、修改或刪除任何規則）");
    private JButton readFw;
    private String baseSha = "";

    NetSecurityTab(AdminConsoleContext ctx) {
        this.ctx = ctx;
        JPanel form = AdminUi.panel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 4, 2, 4);
        g.anchor = GridBagConstraints.WEST;
        int y = 0;
        for (String[] k : KEYS) {
            g.gridy = y++;
            g.gridx = 0;
            g.weightx = 0;
            g.fill = GridBagConstraints.NONE;
            JLabel name = AdminUi.label(k[0]);
            name.setFont(AdminUi.font(Font.BOLD, 13));
            name.setToolTipText(k[2]);
            form.add(name, g);
            g.gridx = 1;
            form.add(AdminUi.label(k[1]), g);
            g.gridx = 2;
            g.weightx = 1;
            g.fill = GridBagConstraints.HORIZONTAL;
            JTextField f = AdminUi.field(26);
            f.setToolTipText(k[2]);
            if (k[0].equals("net.allow") || k[0].equals("api.http.allow")) {
                f.setEditable(false);
            }
            fields.put(k[0], f);
            form.add(f, g);
            g.gridx = 3;
            g.weightx = 0;
            g.fill = GridBagConstraints.NONE;
            JLabel e = AdminUi.hint("生效中：-");
            effective.put(k[0], e);
            form.add(e, g);
        }
        JButton save = AdminUi.button("儲存設定…");
        save.addActionListener(e -> save());
        JButton reload = AdminUi.button("重新載入");
        reload.addActionListener(e -> reloadClicked());
        g.gridy = y;
        g.gridx = 0;
        g.gridwidth = 4;
        form.add(AdminUi.row(save, reload, saveState), g);
        root.add(AdminUi.section("連線設定（config\\server.properties；名單必須包含 127.0.0.1）", form), BorderLayout.NORTH);

        JPanel refusedBox = AdminUi.panel(new BorderLayout(0, 4));
        refusedBox.add(AdminUi.scroll(refused), BorderLayout.CENTER);
        JButton addIp = AdminUi.button("把選取的 IP 加入 net.allow…");
        addIp.setEnabled(false);
        addIp.setToolTipText("本機限定版不接受其他 IP");
        addIp.addActionListener(e -> addSelected());
        refusedBox.add(AdminUi.row(addIp, refusedInfo), BorderLayout.SOUTH);

        JPanel netBox = AdminUi.panel(new BorderLayout(6, 0));
        netBox.add(AdminUi.scroll(interfaces), BorderLayout.CENTER);
        JScrollPane ls = AdminUi.scroll(listeners);
        ls.setPreferredSize(new Dimension(250, 100));
        netBox.add(ls, BorderLayout.EAST);

        JPanel fwBox = AdminUi.panel(new BorderLayout(0, 4));
        fwBox.add(AdminUi.scroll(firewall), BorderLayout.CENTER);
        readFw = AdminUi.button("讀取防火牆（唯讀）");
        readFw.addActionListener(e -> readFirewall());
        fwBox.add(AdminUi.row(readFw, firewallInfo), BorderLayout.SOUTH);

        JPanel upper = AdminUi.panel(new BorderLayout(8, 0));
        upper.add(AdminUi.section("最近被 net.allow／net.maxPerIp 擋下的來源（最多 64 個）", refusedBox), BorderLayout.CENTER);
        JPanel netSection = AdminUi.section("網卡位址與監聽埠", netBox);
        netSection.setPreferredSize(new Dimension(700, 100));
        upper.add(netSection, BorderLayout.EAST);
        JPanel grid = AdminUi.panel(new GridLayout(2, 1, 0, 8));
        grid.add(upper);
        grid.add(AdminUi.section("Windows 防火牆輸入規則（唯讀：java.exe 與名稱含 TMS／Maple／8484／8600／Radmin 的規則）", fwBox));
        root.add(grid, BorderLayout.CENTER);
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    @Override
    public String title() {
        return "連線安全";
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public void refresh() {
        ctx.call("net.get", this::show);
    }

    @Override
    public boolean hasUnsavedChanges() {
        return !changes().isEmpty();
    }

    private void reloadClicked() {
        if (hasUnsavedChanges() && !ctx.confirm("重新載入連線設定", "連線設定有還沒儲存的變更（" + String.join("、", changes().keySet())
                + "）。\n重新載入會丟掉這些變更，改用 server.properties 目前的內容。要繼續嗎？")) {
            return;
        }
        refresh();
    }

    void show(JsonNode r) {
        if (!r.path("ok").asBoolean()) {
            ctx.status("連線安全：" + AdminText.failure(r));
            return;
        }
        baseSha = r.path("sha256").asText();
        for (JsonNode s : r.path("settings")) {
            String key = s.path("key").asText();
            JTextField f = fields.get(key);
            if (f == null) {
                continue;
            }
            String file = AdminUi.text(s, "fileValue");
            String value = file.isEmpty() ? s.path("default").asText() : file;
            f.setText(value);
            loaded.put(key, value);
            boolean forced = s.path("jvmOverride").asBoolean();
            f.setEnabled(!forced);
            JLabel e = effective.get(key);
            e.setText((forced ? "被 -D 指定：" : file.isEmpty() ? "未設定（預設）生效中：" : "生效中：") + s.path("effective").asText());
            e.setForeground(forced ? AdminUi.WARN : AdminUi.MUTED);
        }
        List<Object[]> rows = new ArrayList<>();
        for (JsonNode n : r.path("refused")) {
            rows.add(new Object[]{n.path("ip").asText(), n.path("count").asLong(), AdminText.refusal(n.path("reason").asText()),
                    n.path("port").asInt(), AdminUi.shortTime(n.path("lastAt")), n.path("allowedNow").asBoolean() ? "是" : "否"});
        }
        AdminUi.rows(refused, rows);
        refusedInfo.setText("啟動以來共擋下 " + r.path("refusedTotal").asLong() + " 次");
        List<Object[]> nic = new ArrayList<>();
        for (JsonNode n : r.path("interfaces")) {
            for (JsonNode a : n.path("addresses")) {
                if (!a.path("v4").asBoolean()) {
                    continue;
                }
                nic.add(new Object[]{n.path("displayName").asText(), AdminText.category(n.path("category").asText()), a.path("ip").asText(),
                        a.path("netAllow").asBoolean() ? "允許" : "擋", a.path("httpAllow").asBoolean() ? "允許" : "擋"});
            }
        }
        AdminUi.rows(interfaces, nic);
        List<Object[]> ls = new ArrayList<>();
        for (JsonNode n : r.path("listeners")) {
            String addr = n.path("address").asText();
            ls.add(new Object[]{AdminText.purpose(n.path("purpose").asText()), "0.0.0.0".equals(addr) ? "全部網卡" : addr, n.path("port").asInt()});
        }
        AdminUi.rows(listeners, ls);
        saveState.setText("設定檔 " + r.path("configFile").asText());
    }

    private Map<String, Object> changes() {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, JTextField> e : fields.entrySet()) {
            String v = e.getValue().getText().strip();
            if (e.getValue().isEnabled() && !v.equals(loaded.getOrDefault(e.getKey(), ""))) {
                out.put(e.getKey(), v);
            }
        }
        return out;
    }

    private void save() {
        Map<String, Object> values = changes();
        if (values.isEmpty()) {
            ctx.info("儲存設定", "沒有變更。");
            return;
        }
        StringBuilder sb = new StringBuilder("要寫入 config\\server.properties 的這幾行嗎？（會先備份，其他內容不動）\n\n");
        values.forEach((k, v) -> sb.append(k).append("\n  原本：").append(loaded.getOrDefault(k, "")).append("\n  改成：").append(v).append("\n"));
        sb.append("\n伺服器幾秒內套用，不必重啟。");
        if (!ctx.confirm("儲存連線設定", sb.toString())) {
            return;
        }
        send(values);
    }

    private void send(Map<String, Object> values) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("baseSha256", baseSha);
        p.put("values", values);
        ctx.call("net.set", p, r -> {
            if (r.path("ok").asBoolean()) {
                AdminUi.colored(saveState, "已儲存並套用；備份：" + r.path("backup").asText(), AdminUi.OK);
                refresh();
            } else {
                AdminUi.colored(saveState, "沒有儲存：" + AdminText.failure(r), AdminUi.BAD);
                ctx.error("儲存連線設定", AdminText.failure(r));
            }
        });
    }

    private void addSelected() {
        int row = refused.getSelectedRow();
        if (row < 0) {
            ctx.error("加入名單", "請先在表格裡選一個 IP。");
            return;
        }
        String ip = String.valueOf(refused.getValueAt(row, 0));
        if (hasUnsavedChanges()) {
            // the save reloads every field afterwards: never let it drop edits typed above
            ctx.error("加入名單", "上方連線設定有還沒儲存的變更（" + String.join("、", changes().keySet())
                    + "）。\n請先按「儲存設定…」或「重新載入」，再加入名單。");
            return;
        }
        String current = loaded.getOrDefault("net.allow", "");
        if (List.of(current.split(",")).stream().map(String::strip).anyMatch(ip::equals)) {
            ctx.info("加入名單", ip + " 已經在 net.allow 裡。");
            return;
        }
        String next = current.isEmpty() ? "127.0.0.1," + ip : current + "," + ip;
        if (!ctx.confirm("加入 net.allow", "要讓 " + ip + " 可以連遊戲埠嗎？\n\nnet.allow 會改成：\n" + next
                + "\n\n只加這一個位址（不是整段網段）。請確認這是你認識的人。")) {
            return;
        }
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("net.allow", next);
        send(values);
    }

    private void readFirewall() {
        readFw.setEnabled(false); // one PowerShell at a time
        firewallInfo.setText("讀取中…（最多 " + FirewallReader.TIMEOUT_SECONDS + " 秒）");
        ctx.run(FirewallReader::read, rules -> {
            readFw.setEnabled(true);
            showFirewall(rules);
        }, failure -> {
            readFw.setEnabled(true);
            AdminUi.colored(firewallInfo, "讀取失敗：" + failure.getMessage(), AdminUi.BAD);
        });
    }

    void showFirewall(List<FirewallReader.Rule> rules) {
        List<Object[]> rows = new ArrayList<>();
        for (FirewallReader.Rule r : rules) {
            rows.add(new Object[]{r.name(), "True".equalsIgnoreCase(r.enabled()) ? "是" : "否", "Allow".equalsIgnoreCase(r.action()) ? "允許" : "封鎖",
                    r.profile(), r.protocol(), r.localPort(), r.remote(), r.program()});
        }
        AdminUi.rows(firewall, rows);
        firewallInfo.setText("共 " + rules.size() + " 條（唯讀；要改請自己到 Windows 防火牆設定，主控台不會動它）");
    }
}
