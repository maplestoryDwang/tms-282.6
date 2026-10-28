package net.swordie.ms.console.admin;

import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 「營運狀態」：伺服器健康（記憶體、執行緒、GC、已啟動時間）與在線角色列表；可以把選取的角色踢下線。 */
final class OpsTab implements AdminTab {
    private final AdminConsoleContext ctx;
    private final JPanel root = AdminUi.panel(new BorderLayout(8, 8));
    private final JLabel uptime = AdminUi.value();
    private final JLabel heap = AdminUi.value();
    private final JLabel threads = AdminUi.value();
    private final JLabel gc = AdminUi.value();
    private final JLabel online = AdminUi.value();
    private final JLabel connections = AdminUi.value();
    private final JTable players = AdminUi.table(new String[]{"角色 ID", "角色名稱", "職業", "等級", "地圖", "頻道", "帳號 ID", "來源 IP"},
            new int[]{70, 150, 70, 50, 100, 50, 70, 130});
    private final JLabel hint = AdminUi.hint("每幾秒自動更新。踢下線會先存檔再中斷那個角色的連線。");

    OpsTab(AdminConsoleContext ctx) {
        this.ctx = ctx;
        JPanel stats = AdminUi.panel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        heap.setText("00000 MB／00000 MB"); // the stat box keeps the width it has when it is made
        gc.setText("000000 次／00000 秒");
        uptime.setText("00 天 00 小時 00 分");
        stats.add(AdminUi.stat("已啟動", uptime));
        stats.add(AdminUi.stat("記憶體（已用／上限）", heap));
        stats.add(AdminUi.stat("執行緒（目前／最高）", threads));
        stats.add(AdminUi.stat("GC（次數／總耗時）", gc));
        stats.add(AdminUi.stat("在線（角色／帳號）", online));
        stats.add(AdminUi.stat("連線數", connections));
        root.add(stats, BorderLayout.NORTH);

        JButton kick = AdminUi.dangerButton("把選取的角色踢下線");
        kick.addActionListener(e -> kick());
        JPanel south = AdminUi.panel(new BorderLayout(8, 0));
        south.add(hint, BorderLayout.CENTER);
        south.add(kick, BorderLayout.EAST);
        JPanel center = AdminUi.panel(new BorderLayout(0, 6));
        center.add(AdminUi.scroll(players), BorderLayout.CENTER);
        center.add(south, BorderLayout.SOUTH);
        root.add(AdminUi.section("在線角色", center), BorderLayout.CENTER);
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    @Override
    public String title() {
        return "營運狀態";
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
        ctx.call("ops.health", this::showHealth);
        ctx.call("ops.online", this::showOnline);
    }

    void showHealth(JsonNode r) {
        if (!r.path("ok").asBoolean()) {
            ctx.status("營運狀態：" + AdminText.failure(r));
            return;
        }
        uptime.setText(AdminUi.duration(r.path("uptimeSec").asLong()));
        long used = r.path("heapUsedMb").asLong();
        long max = r.path("heapMaxMb").asLong();
        String text = used + " MB／" + (max > 0 ? max + " MB" : "不限");
        AdminUi.colored(heap, text, max > 0 && used * 100 / max >= 85 ? AdminUi.WARN : AdminUi.OK);
        threads.setText(r.path("threads").asInt() + "／" + r.path("peakThreads").asInt());
        gc.setText(r.path("gcCount").asLong() + " 次／" + r.path("gcMs").asLong() / 1000 + " 秒");
        online.setText(r.path("onlineCharacters").asInt() + "／" + r.path("onlineUsers").asInt());
        connections.setText(r.path("connections").asText());
    }

    void showOnline(JsonNode r) {
        if (!r.path("ok").asBoolean()) {
            ctx.status("在線角色：" + AdminText.failure(r));
            return;
        }
        int selected = selectedId();
        List<Object[]> rows = new ArrayList<>();
        for (JsonNode c : r.path("characters")) {
            rows.add(new Object[]{c.path("id").asInt(), c.path("name").asText(), c.path("job").asInt(), c.path("level").asInt(),
                    c.path("field").asInt(), c.path("channel").asInt(), c.path("user").asInt(), c.path("ip").asText()});
        }
        AdminUi.rows(players, rows);
        for (int i = 0; i < players.getRowCount(); i++) {
            if (selected != 0 && selected == (Integer) players.getValueAt(i, 0)) {
                players.setRowSelectionInterval(i, i);
            }
        }
    }

    private int selectedId() {
        int row = players.getSelectedRow();
        return row < 0 ? 0 : (Integer) players.getValueAt(row, 0);
    }

    private void kick() {
        int id = selectedId();
        if (id == 0) {
            ctx.info("踢下線", "請先在列表選一個角色。");
            return;
        }
        String name = String.valueOf(players.getValueAt(players.getSelectedRow(), 1));
        if (!ctx.confirm("踢下線", "要把角色 " + name + "（" + id + "）踢下線嗎？會先存檔。")) {
            return;
        }
        ctx.call("ops.kick", Map.of("character", String.valueOf(id)), r -> {
            ctx.status(r.path("ok").asBoolean() ? "已把角色 " + name + " 踢下線。" : "踢下線：" + AdminText.failure(r));
            refresh();
        });
    }
}
