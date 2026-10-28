package net.swordie.ms.console.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.account.WorldDirectory;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 「禮包序號」(P7): create codes (items by name / ID through gift.itemSearch, uses, per-account limit, world, level,
 * period, how many codes); the new codes are shown here once and never again (the server keeps only their HMAC);
 * the code list with 停用; redemptions; the reward queue (pending or all). Everything through the admin API.
 */
final class GiftCodeTab implements AdminTab {
    private static final WorldDirectory WORLDS = new WorldDirectory(null);
    private static final DateTimeFormatter INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int[] WORLD_IDS = {-1, 0, 47, 55, 56};

    private final AdminConsoleContext ctx;
    private final JPanel root = AdminUi.panel(new BorderLayout(8, 8));

    // create form
    private final JTextField title = AdminUi.field(16);
    private final JTextField note = AdminUi.field(16);
    private final JTextField search = AdminUi.field(10);
    private final JTable found = AdminUi.table(new String[]{"ID", "名稱", "類型"}, new int[]{70, 230, 60});
    private final JSpinner qty = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
    private final JTable chosen = AdminUi.table(new String[]{"ID", "道具", "數量"}, new int[]{70, 250, 40});
    private final List<int[]> items = new ArrayList<>();
    private final List<String> itemNames = new ArrayList<>();
    private final List<JsonNode> foundRows = new ArrayList<>();
    private final JSpinner maxUses = new JSpinner(new SpinnerNumberModel(1, 1, 100_000, 1));
    private final JSpinner perUser = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
    private final JComboBox<String> world = new JComboBox<>();
    private final JSpinner minLevel = new JSpinner(new SpinnerNumberModel(0, 0, 300, 1));
    private final JTextField startsAt = AdminUi.field(11);
    private final JTextField expiresAt = AdminUi.field(11);
    private final JSpinner count = new JSpinner(new SpinnerNumberModel(1, 1, 200, 1));
    private final JTextArea created = new JTextArea(4, 20);
    private final JLabel createdInfo = AdminUi.hint("建立後序號只會在這裡顯示這一次（伺服器只存雜湊）。");

    // lists
    private final JTable codes = AdminUi.table(new String[]{"ID", "名稱", "道具", "已用/上限", "每人", "世界", "等級", "期限", "狀態"},
            new int[]{36, 110, 170, 62, 36, 90, 36, 120, 54});
    private final List<Integer> codeIds = new ArrayList<>();
    private final JTable redemptions = AdminUi.table(new String[]{"時間", "序號 ID", "名稱", "帳號", "角色", "來源 IP", "已送達"},
            new int[]{95, 55, 120, 100, 100, 95, 50});
    private final JTable queue = AdminUi.table(new String[]{"建立", "帳號", "角色", "道具", "數量", "來源", "送達"},
            new int[]{95, 100, 100, 170, 40, 70, 95});
    private final JCheckBox queueAll = new JCheckBox("含已送達");
    private final JLabel listInfo = AdminUi.hint(" ");

    GiftCodeTab(AdminConsoleContext ctx) {
        this.ctx = ctx;
        for (int w : WORLD_IDS) {
            world.addItem(w < 0 ? "全部世界" : WORLDS.name(w) + "（" + w + "）");
        }
        world.setFont(AdminUi.font(Font.PLAIN, 13));
        for (JSpinner s : new JSpinner[]{qty, maxUses, perUser, minLevel, count}) {
            s.setFont(AdminUi.font(Font.PLAIN, 13));
        }
        queueAll.setFont(AdminUi.font(Font.PLAIN, 13));
        queueAll.setOpaque(false);
        created.setEditable(false);
        created.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        expiresAt.setText(LocalDateTime.now().plusDays(30).withHour(23).withMinute(59).format(INPUT));

        root.add(createPanel(), BorderLayout.WEST);
        root.add(listsPanel(), BorderLayout.CENTER);
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    private JComponent createPanel() {
        JPanel form = AdminUi.panel(null);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(line(AdminUi.label("名稱（玩家看得到）："), title));
        form.add(line(AdminUi.label("備註（只有主控台）："), note));

        JButton find = AdminUi.button("查道具");
        find.addActionListener(e -> searchItems());
        search.addActionListener(e -> searchItems());
        form.add(line(AdminUi.label("道具名稱或 ID："), search, find));
        JScrollPane fs = AdminUi.scroll(found);
        fs.setPreferredSize(new Dimension(400, 110));
        form.add(wrap(fs));
        JButton add = AdminUi.button("加入");
        add.addActionListener(e -> addChosen());
        JButton remove = AdminUi.button("移除選取");
        remove.addActionListener(e -> removeChosen());
        form.add(line(AdminUi.label("數量："), qty, add, remove));
        JScrollPane cs = AdminUi.scroll(chosen);
        cs.setPreferredSize(new Dimension(400, 80));
        form.add(wrap(cs));

        form.add(line(AdminUi.label("使用次數上限："), maxUses, AdminUi.label("每帳號："), perUser));
        form.add(line(AdminUi.label("世界："), world, AdminUi.label("最低等級："), minLevel));
        form.add(line(AdminUi.label("開始："), startsAt, AdminUi.label("到期："), expiresAt));
        form.add(line(AdminUi.hint("格式 2026-10-31 23:59；開始空白＝立即，到期空白＝不到期")));
        JButton create = AdminUi.button("建立序號");
        create.addActionListener(e -> create());
        form.add(line(AdminUi.label("產生幾組："), count, create));

        JPanel out = AdminUi.panel(new BorderLayout(0, 4));
        out.add(createdInfo, BorderLayout.NORTH);
        JScrollPane os = new JScrollPane(created);
        os.setPreferredSize(new Dimension(400, 86));
        out.add(os, BorderLayout.CENTER);
        JButton copy = AdminUi.button("複製全部");
        copy.addActionListener(e -> copyCodes());
        JButton clear = AdminUi.button("清除畫面");
        clear.addActionListener(e -> showCreated(null));
        out.add(AdminUi.row(copy, clear), BorderLayout.SOUTH);
        form.add(wrap(out));

        JPanel section = AdminUi.section("建立禮包序號", form);
        section.setPreferredSize(new Dimension(440, 100));
        return section;
    }

    private JComponent listsPanel() {
        JPanel top = AdminUi.panel(new BorderLayout(0, 4));
        JButton reload = AdminUi.button("重新載入");
        reload.addActionListener(e -> refresh());
        JButton disable = AdminUi.dangerButton("停用選取的序號");
        disable.addActionListener(e -> disableSelected());
        JButton byCode = AdminUi.button("只看這組的兌換紀錄");
        byCode.addActionListener(e -> loadRedemptions(selectedCode()));
        top.add(AdminUi.row(reload, disable, byCode, listInfo), BorderLayout.NORTH);
        top.add(AdminUi.scroll(codes), BorderLayout.CENTER);

        JTabbedPane lower = new JTabbedPane();
        lower.setFont(AdminUi.font(Font.PLAIN, 13));
        lower.addTab("兌換紀錄", AdminUi.scroll(redemptions));
        JPanel q = AdminUi.panel(new BorderLayout(0, 4));
        queueAll.addActionListener(e -> loadQueue());
        q.add(AdminUi.row(queueAll, AdminUi.hint("待發放：角色下次進場（登入或換頻）時自動放進背包；背包滿會保留到下次。")), BorderLayout.NORTH);
        q.add(AdminUi.scroll(queue), BorderLayout.CENTER);
        lower.addTab("待發放佇列", q);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, AdminUi.section("序號列表", top), lower);
        split.setResizeWeight(0.55);
        split.setBorder(null);
        return split;
    }

    private static JPanel line(Component... c) {
        JPanel p = AdminUi.row(c);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height));
        return p;
    }

    private static JPanel wrap(JComponent c) {
        JPanel p = AdminUi.panel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(2, 6, 4, 6));
        p.add(c, BorderLayout.CENTER);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    @Override
    public String title() {
        return "禮包序號";
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public boolean hasUnsavedChanges() {
        return !created.getText().isBlank();
    }

    @Override
    public void refresh() {
        ctx.call("gift.list", Map.of("includeDisabled", true), this::showCodes);
        loadRedemptions(null);
        loadQueue();
    }

    // ------------------------------------------------------------------ items

    void search(String q) {
        search.setText(q);
        searchItems();
    }

    private void searchItems() {
        String q = search.getText().strip();
        if (q.isEmpty()) {
            return;
        }
        ctx.call("gift.itemSearch", Map.of("query", q), this::showFound);
    }

    void showFound(JsonNode r) {
        foundRows.clear();
        List<Object[]> rows = new ArrayList<>();
        if (!r.path("ok").asBoolean()) {
            ctx.status("禮包序號：" + AdminText.failure(r));
        } else {
            for (JsonNode n : r.path("items")) {
                foundRows.add(n);
                String type = n.path("type").asText() + (n.path("grantable").asBoolean() ? "" : "（不可發）");
                rows.add(new Object[]{n.path("itemId").asInt(), n.path("name").asText(), type});
            }
        }
        AdminUi.rows(found, rows);
        if (!foundRows.isEmpty()) {
            found.setRowSelectionInterval(0, 0);
        }
    }

    private void addChosen() {
        int i = found.getSelectedRow();
        if (i < 0 || i >= foundRows.size()) {
            ctx.error("加入道具", "請先查詢並在上方清單選一個道具。");
            return;
        }
        JsonNode n = foundRows.get(i);
        if (!n.path("grantable").asBoolean()) {
            ctx.error("加入道具", "這個道具在 282 客戶端不能發放（已移除或沒有資料）。");
            return;
        }
        int q = (Integer) qty.getValue();
        if (n.path("unique").asBoolean() && q != 1) {
            ctx.error("加入道具", "裝備與寵物每一行只能 1 個；要多個請分成多行。");
            return;
        }
        if (items.size() >= 10) {
            ctx.error("加入道具", "一組序號最多 10 行道具。");
            return;
        }
        addItem(n.path("itemId").asInt(), n.path("name").asText(), q);
    }

    void addItem(int itemId, String name, int quantity) {
        items.add(new int[]{itemId, quantity});
        itemNames.add(name);
        showChosen();
    }

    private void removeChosen() {
        int i = chosen.getSelectedRow();
        if (i >= 0 && i < items.size()) {
            items.remove(i);
            itemNames.remove(i);
            showChosen();
        }
    }

    private void showChosen() {
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            rows.add(new Object[]{items.get(i)[0], itemNames.get(i), items.get(i)[1]});
        }
        AdminUi.rows(chosen, rows);
    }

    // ------------------------------------------------------------------ create

    void fill(String t, String n, int uses, int each, int worldIndex, int level, int howMany) {
        title.setText(t);
        note.setText(n);
        maxUses.setValue(uses);
        perUser.setValue(each);
        world.setSelectedIndex(worldIndex);
        minLevel.setValue(level);
        count.setValue(howMany);
    }

    private Long time(JTextField f, String what) {
        String s = f.getText().strip();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(s, INPUT).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(what + "的格式要像 2026-10-31 23:59");
        }
    }

    private void create() {
        if (title.getText().isBlank()) {
            ctx.error("建立序號", "請填名稱（玩家在登入器的兌換紀錄看得到）。");
            return;
        }
        if (items.isEmpty()) {
            ctx.error("建立序號", "請至少加入一個道具。");
            return;
        }
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("title", title.getText().strip());
        p.put("note", note.getText().strip());
        List<Map<String, Object>> list = new ArrayList<>();
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            list.add(Map.of("itemId", items.get(i)[0], "quantity", items.get(i)[1]));
            summary.append("\n　").append(itemNames.get(i)).append(" ×").append(items.get(i)[1]);
        }
        p.put("items", list);
        p.put("maxUses", maxUses.getValue());
        p.put("perUserLimit", perUser.getValue());
        p.put("worldId", WORLD_IDS[Math.max(0, world.getSelectedIndex())]);
        p.put("minLevel", minLevel.getValue());
        p.put("count", count.getValue());
        try {
            Long s = time(startsAt, "開始時間"), e = time(expiresAt, "到期時間");
            if (s != null) p.put("startsAt", s);
            if (e != null) p.put("expiresAt", e);
        } catch (IllegalArgumentException ex) {
            ctx.error("建立序號", ex.getMessage());
            return;
        }
        if (!ctx.confirm("建立序號", "要建立 " + count.getValue() + " 組「" + title.getText().strip() + "」嗎？" + summary
                + "\n\n每組可用 " + maxUses.getValue() + " 次、每帳號 " + perUser.getValue() + " 次。\n序號只會顯示這一次，請馬上複製保存。")) {
            return;
        }
        ctx.call("gift.create", p, this::showCreated);
    }

    void showCreated(JsonNode r) {
        if (r == null) {
            created.setText("");
            createdInfo.setText("建立後序號只會在這裡顯示這一次（伺服器只存雜湊）。");
            createdInfo.setForeground(AdminUi.MUTED);
            return;
        }
        if (!r.path("ok").asBoolean()) {
            ctx.error("建立序號", AdminText.failure(r));
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode c : r.path("codes")) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(c.asText());
        }
        created.setText(sb.toString());
        created.setCaretPosition(0);
        AdminUi.colored(createdInfo, "已建立 " + r.path("codes").size() + " 組：請現在複製，離開這個畫面後就看不到了。", AdminUi.WARN);
        items.clear();
        itemNames.clear();
        showChosen();
        ctx.call("gift.list", Map.of("includeDisabled", true), this::showCodes);
    }

    private void copyCodes() {
        if (created.getText().isBlank()) {
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(created.getText()), null);
        ctx.status("禮包序號：已複製 " + created.getText().lines().count() + " 組序號");
    }

    // ------------------------------------------------------------------ lists

    void showCodes(JsonNode r) {
        codeIds.clear();
        List<Object[]> rows = new ArrayList<>();
        if (!r.path("ok").asBoolean()) {
            listInfo.setText(AdminText.failure(r));
            AdminUi.rows(codes, rows);
            return;
        }
        for (JsonNode n : r.path("codes")) {
            codeIds.add(n.path("id").asInt());
            StringBuilder it = new StringBuilder();
            for (JsonNode i : n.path("items")) {
                if (it.length() > 0) it.append("、");
                it.append(i.path("name").asText()).append("×").append(i.path("quantity").asInt());
            }
            int w = n.path("worldId").asInt();
            String period = AdminUi.dateTime(n.get("startsAt")) + " ~ " + AdminUi.dateTime(n.get("expiresAt"));
            if (n.path("startsAt").isNull() && n.path("expiresAt").isNull()) period = "不限";
            else if (n.path("startsAt").isNull()) period = "至 " + AdminUi.dateTime(n.get("expiresAt"));
            rows.add(new Object[]{n.path("id").asInt(), n.path("title").asText(), it.toString(),
                    n.path("usedCount").asInt() + "/" + n.path("maxUses").asInt(), n.path("perUserLimit").asInt(),
                    w < 0 ? "全部" : WORLDS.name(w), n.path("minLevel").asInt(), period, AdminText.giftState(n.path("state").asText())});
        }
        AdminUi.rows(codes, rows);
        listInfo.setText(codeIds.size() + " 組；金鑰 " + AdminUi.text(r, "keyId"));
    }

    private Integer selectedCode() {
        int i = codes.getSelectedRow();
        return i >= 0 && i < codeIds.size() ? codeIds.get(i) : null;
    }

    private void disableSelected() {
        Integer id = selectedCode();
        if (id == null) {
            ctx.error("停用序號", "請先在列表選一組序號。");
            return;
        }
        if (!ctx.confirm("停用序號", "要停用序號 #" + id + " 嗎？停用後不能再兌換（已兌換、待發放的道具照常送達）。")) {
            return;
        }
        ctx.call("gift.disable", Map.of("id", id), r -> {
            if (r.path("ok").asBoolean()) {
                ctx.status("禮包序號：#" + id + (r.path("changed").asBoolean() ? " 已停用" : " 原本就是停用"));
                refresh();
            } else {
                ctx.error("停用序號", AdminText.failure(r));
            }
        });
    }

    private void loadRedemptions(Integer codeId) {
        ctx.call("gift.redemptions", codeId == null ? Map.of() : Map.of("codeId", codeId), this::showRedemptions);
    }

    void showRedemptions(JsonNode r) {
        List<Object[]> rows = new ArrayList<>();
        for (JsonNode n : r.path("redemptions")) {
            rows.add(new Object[]{AdminUi.time(n.get("redeemedAt")), n.path("codeId").asInt(), n.path("title").asText(),
                    n.path("userName").asText() + "（" + n.path("userId").asInt() + "）", n.path("characterName").asText(),
                    n.path("sourceIp").asText(), AdminUi.yesNo(n.path("delivered").asBoolean())});
        }
        AdminUi.rows(redemptions, rows);
        if (!r.path("ok").asBoolean()) {
            ctx.status("兌換紀錄：" + AdminText.failure(r));
        }
    }

    private void loadQueue() {
        ctx.call("gift.queue", Map.of("includeDelivered", queueAll.isSelected()), this::showQueue);
    }

    void showQueue(JsonNode r) {
        List<Object[]> rows = new ArrayList<>();
        for (JsonNode n : r.path("queue")) {
            rows.add(new Object[]{AdminUi.time(n.get("createdAt")), n.path("userName").asText(), n.path("characterName").asText(),
                    n.path("itemName").asText() + "（" + n.path("itemId").asInt() + "）", n.path("quantity").asInt(),
                    "gift".equals(n.path("source").asText()) ? "序號" : n.path("source").asText(),
                    n.path("deliveredAt").isNull() ? "等待中" : AdminUi.time(n.get("deliveredAt"))});
        }
        AdminUi.rows(queue, rows);
        if (!r.path("ok").asBoolean()) {
            ctx.status("待發放佇列：" + AdminText.failure(r));
        }
    }
}
