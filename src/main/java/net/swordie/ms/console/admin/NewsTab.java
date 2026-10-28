package net.swordie.ms.console.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.swordie.ms.console.ConsoleUiStyle;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 「公告」：編輯 data\launcher\news.json - 新增、編輯、排序、置頂、上下架（startAt／endAt）、預覽；存檔前由伺服器用同一套規則
 * 驗證，寫入前留 news.json.bak，伺服器 5 秒內熱重載。還沒存到伺服器的編輯（清單或右邊表單）不會被切換分頁或重新連線
 * 自動蓋掉；按「重新載入」時會先確認。
 */
final class NewsTab implements AdminTab {
    private static final String[] KINDS = {"ev", "up", "mt"};

    private final AdminConsoleContext ctx;
    private final JPanel root = AdminUi.panel(new BorderLayout(8, 8));
    private final JTable list = AdminUi.table(new String[]{"#", "ID", "類型", "標題", "開始", "結束", "置頂"},
            new int[]{30, 110, 45, 220, 110, 110, 40});
    private final JTextField id = AdminUi.field(16);
    private final JComboBox<String> kind = new JComboBox<>(new String[]{"活動（ev）", "更新（up）", "維修（mt）"});
    private final JTextField title = AdminUi.field(28);
    private final JTextArea body = new JTextArea(6, 28);
    private final JTextField startAt = AdminUi.field(14);
    private final JTextField endAt = AdminUi.field(14);
    private final JCheckBox pinned = new JCheckBox("置頂");
    private final JPanel preview = AdminUi.panel(null);
    private final JLabel fileState = AdminUi.label("-");
    private final JLabel check = AdminUi.label(" ");
    private final List<ObjectNode> items = new ArrayList<>();
    /** The list as last loaded from / saved to the server (to tell unsaved edits). */
    private String savedItems = "[]";
    /** The form as {@link #load} filled it (to tell edits not yet applied to the list). */
    private String loadedForm = "";
    /** Items the launchers get that are not in news.json (generated events / maintenance / slides). */
    private final List<JsonNode> generated = new ArrayList<>();
    private String baseSha = "";
    private boolean loading;

    NewsTab(AdminConsoleContext ctx) {
        this.ctx = ctx;
        preview.setLayout(new BoxLayout(preview, BoxLayout.Y_AXIS));
        list.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                load(list.getSelectedRow());
            }
        });
        JButton add = AdminUi.button("新增");
        JButton remove = AdminUi.button("刪除");
        JButton up = AdminUi.button("上移");
        JButton down = AdminUi.button("下移");
        add.addActionListener(e -> add());
        remove.addActionListener(e -> remove());
        up.addActionListener(e -> move(-1));
        down.addActionListener(e -> move(1));
        JPanel left = AdminUi.panel(new BorderLayout(0, 4));
        left.add(AdminUi.scroll(list), BorderLayout.CENTER);
        JPanel listActions = AdminUi.panel(new GridLayout(2, 1));
        listActions.add(AdminUi.row(add, remove, up, down));
        listActions.add(AdminUi.row(AdminUi.hint("這是檔案裡的順序；登入器另依置頂、維修、開始時間排序（見下方預覽）")));
        left.add(listActions, BorderLayout.SOUTH);

        body.setFont(AdminUi.font(Font.PLAIN, 13));
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        kind.setFont(AdminUi.font(Font.PLAIN, 13));
        pinned.setFont(AdminUi.font(Font.PLAIN, 13));
        pinned.setBackground(ConsoleUiStyle.PANEL_BG);
        JPanel form = AdminUi.panel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 4, 3, 4);
        g.anchor = GridBagConstraints.WEST;
        int y = 0;
        y = formRow(form, g, y, "ID", id, "英數、_、-，1–40 字，不可重複");
        y = formRow(form, g, y, "類型", kind, null);
        y = formRow(form, g, y, "標題", title, "1–80 字");
        g.gridx = 0;
        g.gridy = y;
        g.anchor = GridBagConstraints.NORTHWEST;
        form.add(AdminUi.label("內容"), g);
        g.gridx = 1;
        g.gridwidth = 2;
        g.fill = GridBagConstraints.BOTH;
        g.weightx = 1;
        g.weighty = 1;
        JScrollPane bs = AdminUi.scroll(body);
        bs.setPreferredSize(new Dimension(300, 110));
        form.add(bs, g);
        g.gridwidth = 1;
        g.fill = GridBagConstraints.NONE;
        g.weightx = 0;
        g.weighty = 0;
        g.anchor = GridBagConstraints.WEST;
        y++;
        y = formRow(form, g, y, "開始", startAt, "yyyy-MM-dd 或 yyyy-MM-ddTHH:mm，空白＝立即");
        y = formRow(form, g, y, "結束", endAt, "空白＝不下架；只寫日期＝到當天結束");
        g.gridx = 1;
        g.gridy = y;
        form.add(pinned, g);
        JButton apply = AdminUi.button("套用到清單");
        apply.addActionListener(e -> apply());
        g.gridx = 2;
        form.add(apply, g);

        JPanel top = AdminUi.panel(new GridLayout(1, 2, 8, 0));
        top.add(AdminUi.section("公告清單（news.json）", left));
        top.add(AdminUi.section("編輯選取的公告", form));

        JScrollPane ps = AdminUi.scroll(preview);
        ps.setPreferredSize(new Dimension(100, 190));
        ps.getVerticalScrollBar().setUnitIncrement(16);
        JPanel bottom = AdminUi.panel(new BorderLayout(0, 4));
        bottom.add(AdminUi.section("預覽（news.json 的公告在登入器的排序與狀態；自動產生的活動／維修另列在最後，灰字）", ps), BorderLayout.CENTER);
        JButton validate = AdminUi.button("驗證");
        JButton save = AdminUi.button("儲存到伺服器");
        JButton reload = AdminUi.button("重新載入");
        validate.addActionListener(e -> validateItems());
        save.addActionListener(e -> save());
        reload.addActionListener(e -> reloadClicked());
        JPanel actions = AdminUi.panel(new BorderLayout());
        actions.add(AdminUi.row(validate, save, reload, fileState), BorderLayout.WEST);
        actions.add(AdminUi.row(check), BorderLayout.SOUTH);
        bottom.add(actions, BorderLayout.SOUTH);

        root.add(top, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        loadedForm = formState();
    }

    private static int formRow(JPanel form, GridBagConstraints g, int y, String label, JComponent field, String hint) {
        g.gridx = 0;
        g.gridy = y;
        form.add(AdminUi.label(label), g);
        g.gridx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        field.setMinimumSize(new Dimension(150, field.getPreferredSize().height));
        form.add(field, g);
        g.fill = GridBagConstraints.NONE;
        g.weightx = 0;
        if (hint != null) {
            g.gridx = 2;
            form.add(AdminUi.hint(hint), g);
        }
        return y + 1;
    }

    @Override
    public String title() {
        return "公告";
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public void refresh() {
        ctx.call("news.get", this::show);
    }

    @Override
    public boolean hasUnsavedChanges() {
        return !savedItems.equals(itemsJson().toString()) || !loadedForm.equals(formState());
    }

    private void reloadClicked() {
        if (hasUnsavedChanges() && !ctx.confirm("重新載入公告", "公告有還沒存到伺服器的變更。\n重新載入會丟掉這些變更，改用伺服器上的 news.json。要繼續嗎？")) {
            return;
        }
        refresh();
    }

    private String formState() {
        return String.join("\u0000", id.getText(), String.valueOf(kind.getSelectedIndex()), title.getText(), body.getText(),
                startAt.getText(), endAt.getText(), String.valueOf(pinned.isSelected()));
    }

    void show(JsonNode r) {
        if (!r.path("ok").asBoolean()) {
            ctx.status("公告：" + AdminText.failure(r));
            return;
        }
        baseSha = r.path("sha256").asText();
        items.clear();
        JsonNode arr = r.get("items");
        if (arr != null && arr.isArray()) {
            for (JsonNode n : arr) {
                if (n.isObject()) {
                    items.add(((ObjectNode) n).deepCopy());
                }
            }
        }
        savedItems = itemsJson().toString();
        generated.clear();
        java.util.Set<String> fileIds = new java.util.HashSet<>();
        items.forEach(i -> fileIds.add(AdminUi.text(i, "id")));
        for (JsonNode n : r.path("live")) {
            if (!fileIds.contains(AdminUi.text(n, "id"))) {
                generated.add(n);
            }
        }
        String problem = AdminUi.text(r, "problem");
        String status = !r.path("exists").asBoolean() ? "還沒有 news.json（儲存後建立）"
                : problem.isEmpty() ? "news.json 格式正確" : "news.json 目前有錯：" + AdminText.problem("", problem) + "（伺服器沿用上一個正確版本）";
        AdminUi.colored(fileState, status + "；登入器目前顯示 " + r.path("live").size() + " 則（news.json 以外自動產生 " + generated.size() + " 則）",
                problem.isEmpty() ? AdminUi.MUTED : AdminUi.BAD);
        fillList(0);
        showPreview(r.path("preview"));
        check.setText(" ");
    }

    private void fillList(int select) {
        List<Object[]> rows = new ArrayList<>();
        int n = 1;
        for (ObjectNode i : items) {
            rows.add(new Object[]{n++, AdminUi.text(i, "id"), AdminText.kind(AdminUi.text(i, "kind")), AdminUi.text(i, "title"),
                    blank(AdminUi.text(i, "startAt"), "立即"), blank(AdminUi.text(i, "endAt"), "不下架"),
                    i.path("pinned").asBoolean() ? "★" : ""});
        }
        loading = true;
        AdminUi.rows(list, rows);
        loading = false;
        if (!items.isEmpty()) {
            int s = Math.max(0, Math.min(select, items.size() - 1));
            list.setRowSelectionInterval(s, s);
            load(s);
        } else {
            load(-1);
        }
    }

    private static String blank(String s, String dflt) {
        return s.isEmpty() ? dflt : s;
    }

    private void load(int row) {
        if (loading) {
            return;
        }
        boolean has = row >= 0 && row < items.size();
        ObjectNode i = has ? items.get(row) : null;
        id.setText(has ? AdminUi.text(i, "id") : "");
        String k = has ? AdminUi.text(i, "kind") : "up";
        kind.setSelectedIndex(Math.max(0, List.of(KINDS).indexOf(k)));
        title.setText(has ? AdminUi.text(i, "title") : "");
        body.setText(has ? AdminUi.text(i, "body") : "");
        body.setCaretPosition(0);
        startAt.setText(has ? AdminUi.text(i, "startAt") : "");
        endAt.setText(has ? AdminUi.text(i, "endAt") : "");
        pinned.setSelected(has && i.path("pinned").asBoolean());
        loadedForm = formState();
    }

    private void apply() {
        int row = list.getSelectedRow();
        if (row < 0 || row >= items.size()) {
            ctx.error("公告", "請先在左邊選一則公告（或按「新增」）。");
            return;
        }
        ObjectNode i = HttpAdminGateway.JSON.createObjectNode();
        i.put("id", id.getText().strip());
        i.put("kind", KINDS[Math.max(0, kind.getSelectedIndex())]);
        i.put("title", title.getText().strip());
        i.put("body", body.getText());
        putTime(i, "startAt", startAt.getText());
        putTime(i, "endAt", endAt.getText());
        i.put("pinned", pinned.isSelected());
        items.set(row, i);
        fillList(row);
        check.setText("已套用到清單（還沒存到伺服器）");
        check.setForeground(AdminUi.WARN);
    }

    private static void putTime(ObjectNode i, String key, String text) {
        String t = text.strip();
        if (t.isEmpty()) {
            i.putNull(key);
        } else {
            i.put(key, t);
        }
    }

    private void add() {
        ObjectNode i = HttpAdminGateway.JSON.createObjectNode();
        int n = items.size() + 1;
        String newId = "news-" + n;
        while (exists(newId)) {
            newId = "news-" + ++n;
        }
        i.put("id", newId);
        i.put("kind", "up");
        i.put("title", "新公告");
        i.put("body", "");
        i.putNull("startAt");
        i.putNull("endAt");
        i.put("pinned", false);
        items.add(i);
        fillList(items.size() - 1);
    }

    private boolean exists(String newId) {
        return items.stream().anyMatch(i -> newId.equals(AdminUi.text(i, "id")));
    }

    private void remove() {
        int row = list.getSelectedRow();
        if (row < 0 || row >= items.size()) {
            return;
        }
        if (!ctx.confirm("刪除公告", "要從清單刪除「" + AdminUi.text(items.get(row), "title") + "」嗎？（按「儲存到伺服器」後才會生效）")) {
            return;
        }
        items.remove(row);
        fillList(row);
    }

    private void move(int delta) {
        int row = list.getSelectedRow();
        int to = row + delta;
        if (row < 0 || to < 0 || to >= items.size()) {
            return;
        }
        ObjectNode a = items.get(row);
        items.set(row, items.get(to));
        items.set(to, a);
        fillList(to);
    }

    private ArrayNode itemsJson() {
        ArrayNode arr = HttpAdminGateway.JSON.createArrayNode();
        items.forEach(arr::add);
        return arr;
    }

    private void validateItems() {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("items", itemsJson());
        ctx.call("news.validate", p, r -> {
            if (r.path("ok").asBoolean()) {
                AdminUi.colored(check, "驗證通過（" + r.path("bytes").asInt() + " bytes）", AdminUi.OK);
                showPreview(r.path("preview"));
            } else {
                AdminUi.colored(check, "驗證失敗：" + AdminText.failure(r), AdminUi.BAD);
            }
        });
    }

    private void save() {
        if (!ctx.confirm("儲存公告", "要把 " + items.size() + " 則公告寫入伺服器的 news.json 嗎？\n舊檔會留成 news.json.bak，登入器 5 秒內更新。")) {
            return;
        }
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("items", itemsJson());
        p.put("baseSha256", baseSha);
        ctx.call("news.save", p, r -> {
            if (r.path("ok").asBoolean()) {
                baseSha = r.path("sha256").asText();
                savedItems = p.get("items").toString();
                AdminUi.colored(check, "已儲存；伺服器 " + r.path("reloadSec").asInt() + " 秒內熱重載"
                        + (r.path("backup").isNull() ? "" : "，舊檔備份：" + r.path("backup").asText()), AdminUi.OK);
                showPreview(r.path("preview"));
            } else {
                AdminUi.colored(check, "沒有儲存：" + AdminText.failure(r), AdminUi.BAD);
            }
        });
    }

    private void showPreview(JsonNode arr) {
        preview.removeAll();
        if (arr == null || !arr.isArray() || arr.isEmpty()) {
            JLabel none = AdminUi.hint("（news.json 沒有公告）");
            none.setAlignmentX(Component.LEFT_ALIGNMENT);
            preview.add(none);
        } else {
            for (JsonNode n : arr) {
                preview.add(card(n));
                preview.add(Box.createVerticalStrut(4));
            }
        }
        if (!generated.isEmpty()) {
            JLabel head = AdminUi.hint("登入器另外顯示（伺服器自動產生，不在 news.json，這裡不能編輯）：");
            head.setAlignmentX(Component.LEFT_ALIGNMENT);
            head.setBorder(BorderFactory.createEmptyBorder(6, 0, 2, 0));
            preview.add(head);
            for (JsonNode n : generated) {
                JLabel g = AdminUi.hint("　［" + AdminText.kind(AdminUi.text(n, "kind")) + "］" + AdminUi.text(n, "title") + "　"
                        + AdminUi.dateTime(n.path("startAt")) + " ～ " + (n.path("endAt").isNull() ? "不下架" : AdminUi.dateTime(n.path("endAt"))));
                g.setAlignmentX(Component.LEFT_ALIGNMENT);
                preview.add(g);
            }
        }
        preview.revalidate();
        preview.repaint();
    }

    /** Screenshots: shows the end of the preview (the generated items listed after the file's cards). */
    void scrollPreviewToEnd() {
        preview.doLayout();
        preview.scrollRectToVisible(new Rectangle(0, Math.max(0, preview.getPreferredSize().height - 1), 1, 1));
        if (preview.getParent() instanceof JViewport v && v.getParent() instanceof JScrollPane sp) {
            sp.validate();
            JScrollBar bar = sp.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        }
    }

    private static JComponent card(JsonNode n) {
        JPanel c = AdminUi.panel(new BorderLayout(8, 2));
        c.setBackground(Color.WHITE);
        c.setBorder(BorderFactory.createCompoundBorder(ConsoleUiStyle.lineBorder(), BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        String state = AdminUi.text(n, "state");
        JLabel badge = AdminUi.label((n.path("pinned").asBoolean() ? "★ " : "") + "［" + AdminText.kind(AdminUi.text(n, "kind")) + "］");
        badge.setFont(AdminUi.font(Font.BOLD, 13));
        JLabel t = AdminUi.label(AdminUi.text(n, "title"));
        t.setFont(AdminUi.font(Font.BOLD, 13));
        JLabel s = AdminUi.hint(AdminText.newsState(state) + "　" + AdminUi.dateTime(n.path("startAt")) + " ～ "
                + (n.path("endAt").isNull() ? "不下架" : AdminUi.dateTime(n.path("endAt"))));
        s.setForeground("shown".equals(state) ? AdminUi.OK : AdminUi.MUTED);
        JPanel head = AdminUi.panel(new BorderLayout(6, 0));
        head.setBackground(Color.WHITE);
        head.add(badge, BorderLayout.WEST);
        head.add(t, BorderLayout.CENTER);
        head.add(s, BorderLayout.EAST);
        c.add(head, BorderLayout.NORTH);
        String b = AdminUi.text(n, "body");
        if (!b.isEmpty()) {
            JTextArea area = new JTextArea(b);
            area.setFont(AdminUi.font(Font.PLAIN, 12));
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setEditable(false);
            area.setBackground(Color.WHITE);
            area.setRows(Math.min(3, (int) b.chars().filter(ch -> ch == '\n').count() + 1));
            c.add(area, BorderLayout.CENTER);
        }
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return c;
    }
}
