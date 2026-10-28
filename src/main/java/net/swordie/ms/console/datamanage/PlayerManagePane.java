package net.swordie.ms.console.datamanage;

import net.swordie.ms.Server;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.enums.AccountType;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Set;

public final class PlayerManagePane extends JPanel {
    private static final int COL_ID = 0;
    private static final int COL_NAME = 1;
    private static final int COL_LEVEL = 2;
    private static final int COL_JOB = 3;
    private static final int COL_CHANNEL = 4;
    private static final int COL_MAP = 5;
    private static final int COL_ONLINE = 6;

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "角色名", "等級", "職業", "頻道", "地圖", "在線"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JTextField searchField = new JTextField(16);
    private final JTextArea infoArea = new JTextArea(8, 28);
    private final PlayerStatsEditPanel statsEditPanel = new PlayerStatsEditPanel();

    public PlayerManagePane() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(buildToolbar(), BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setComponentPopupMenu(buildContextMenu());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onTableSelectionChanged();
            }
        });
        statsEditPanel.addPropertyChangeListener("statsSaved", evt -> {
            if (Boolean.TRUE.equals(evt.getNewValue())) {
                refreshSelectedRowInTable();
                showSelectedInfo();
            }
        });

        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setMargin(new Insets(4, 6, 4, 6));

        JScrollPane tableScroll = new JScrollPane(table);
        JScrollPane infoScroll = new JScrollPane(infoArea);
        infoScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("角色狀態"));
        infoPanel.add(infoScroll, BorderLayout.CENTER);

        JSplitPane detailSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, statsEditPanel, infoPanel);
        detailSplit.setResizeWeight(0.38);
        detailSplit.setDividerLocation(400);
        detailSplit.setBorder(null);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, detailSplit);
        mainSplit.setResizeWeight(0.62);
        mainSplit.setDividerLocation(380);
        mainSplit.setBorder(null);
        add(mainSplit, BorderLayout.CENTER);

        refresh(false);
    }

    private JComponent buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        bar.add(new JLabel("搜索:"));
        bar.add(searchField);
        JButton search = new JButton("搜索");
        search.addActionListener(e -> refresh(false));
        JButton online = new JButton("顯示在線");
        online.addActionListener(e -> showOnlineOnly());
        JButton all = new JButton("刷新全部");
        all.addActionListener(e -> refresh(false));
        JButton giveItem = new JButton("發送物品");
        giveItem.addActionListener(e -> openGiveItemDialog());
        JButton message = new JButton("發送提示");
        message.addActionListener(e -> sendMessageToSelected());
        bar.add(search);
        bar.add(online);
        bar.add(all);
        bar.add(giveItem);
        bar.add(message);
        return bar;
    }

    private JPopupMenu buildContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(menuItem("角色預覽", this::openPreview));
        menu.addSeparator();
        menu.add(menuItem("發送物品", this::openGiveItemDialog));
        menu.add(menuItem("發送金幣", this::giveMeso));
        menu.add(menuItem("發送樂豆點", this::giveNxPrepaid));
        menu.add(menuItem("發送楓點", this::giveMaplePoints));
        menu.add(menuItem("贊助積分", this::giveDonationPoints));
        menu.add(menuItem("投票積分", this::giveVotePoints));
        menu.addSeparator();
        JMenu gmMenu = new JMenu("設置賬號權限");
        for (AccountType type : AccountType.values()) {
            JMenuItem gmItem = new JMenuItem(type.getVal() + " - " + ConsoleCharacterService.accountTypeLabel(type));
            gmItem.addActionListener(e -> setAccountType(type));
            gmMenu.add(gmItem);
        }
        menu.add(gmMenu);
        menu.addSeparator();
        menu.add(menuItem("發送提示", this::sendMessageToSelected));
        menu.add(menuItem("踢下線", this::kickSelected));
        menu.add(menuItem("傳送到自由市場", this::warpTown));
        menu.add(menuItem("傳送到地圖...", this::warpToMap));
        return menu;
    }

    private JMenuItem menuItem(String text, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(e -> action.run());
        return item;
    }

    private void onTableSelectionChanged() {
        showSelectedInfo();
        statsEditPanel.onPlayerSelected(getSelectedTarget());
    }

    private void refreshSelectedRowInTable() {
        ConsoleCharacterService.PlayerTarget target = getSelectedTarget();
        if (target == null) {
            return;
        }
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        CharacterStatsSnapshot snap = ConsoleCharacterService.loadCharacterStats(target);
        if (snap != null) {
            model.setValueAt(snap.level, row, COL_LEVEL);
        }
        if (target.isOnline()) {
            Char chr = target.online;
            model.setValueAt((int) chr.getJob(), row, COL_JOB);
            int channel = chr.getClient() == null ? -1 : chr.getClient().getChannel();
            model.setValueAt(channel > 0 ? channel : "-", row, COL_CHANNEL);
            model.setValueAt(chr.getFieldID(), row, COL_MAP);
            model.setValueAt("是", row, COL_ONLINE);
        }
    }

    private ConsoleCharacterService.PlayerTarget getSelectedTarget() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return null;
        }
        Object idObj = model.getValueAt(row, COL_ID);
        Object nameObj = model.getValueAt(row, COL_NAME);
        if (!(idObj instanceof Number id) || nameObj == null) {
            return null;
        }
        return ConsoleCharacterService.resolve(id.intValue(), String.valueOf(nameObj));
    }

    private void refresh(boolean onlineOnly) {
        int sel = table.getSelectedRow();
        String selName = sel >= 0 ? String.valueOf(model.getValueAt(sel, COL_NAME)) : null;
        model.setRowCount(0);
        if (onlineOnly) {
            showOnlineOnly();
            restoreSelection(selName);
            return;
        }
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim();
        String sql = keyword.isEmpty()
                ? "SELECT c.id, cs.name, cs.level, cs.job, COALESCE(c.fieldid, CAST(NULLIF(cs.posmap, '') AS UNSIGNED)) AS map "
                + "FROM characters c LEFT JOIN characterstats cs ON cs.characterid = c.id ORDER BY c.id DESC LIMIT ?"
                : "SELECT c.id, cs.name, cs.level, cs.job, COALESCE(c.fieldid, CAST(NULLIF(cs.posmap, '') AS UNSIGNED)) AS map "
                + "FROM characters c LEFT JOIN characterstats cs ON cs.characterid = c.id WHERE cs.name LIKE ? ORDER BY c.id DESC LIMIT ?";
        Set<String> onlineNames = ConsoleCharacterService.onlineNames();
        int count = 0;
        try {
            ConsoleDatabaseService.prepare();
            try (Connection con = DatabaseManager.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (keyword.isEmpty()) {
                ps.setInt(1, ConsoleCharacterService.listLimit());
            } else {
                ps.setString(1, "%" + keyword + "%");
                ps.setInt(2, ConsoleCharacterService.listLimit());
            }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        addRowFromResult(rs, onlineNames);
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "讀取角色列表失敗: " + ConsoleDatabaseService.formatSqlError(e),
                    "玩家角色", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (keyword.isEmpty() && count >= ConsoleCharacterService.listLimit()) {
            infoArea.setText("已加載前 " + ConsoleCharacterService.listLimit() + " 名角色。可使用搜索縮小範圍。");
        } else {
            infoArea.setText("已加載 " + count + " 名角色。");
        }
        restoreSelection(selName);
    }

    private void showOnlineOnly() {
        model.setRowCount(0);
        int count = 0;
        for (Char chr : Server.get().getChars().values()) {
            int channel = chr.getClient() == null ? -1 : chr.getClient().getChannel();
            model.addRow(new Object[]{
                    chr.getId(),
                    chr.getName(),
                    chr.getLevel(),
                    (int) chr.getJob(),
                    channel > 0 ? channel : "-",
                    chr.getFieldID(),
                    "是"
            });
            count++;
        }
        infoArea.setText("當前在線 " + count + " 人。清空搜索後點「刷新全部」可恢復全量列表。");
        onTableSelectionChanged();
    }

    private void addRowFromResult(ResultSet rs, Set<String> onlineNames) throws java.sql.SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        Char online = Server.get().getChars().get(id);
        boolean isOnline = online != null || onlineNames.contains(name);
        if (online != null) {
            int channel = online.getClient() == null ? -1 : online.getClient().getChannel();
            model.addRow(new Object[]{
                    id,
                    online.getName(),
                    online.getLevel(),
                    (int) online.getJob(),
                    channel > 0 ? channel : "-",
                    online.getFieldID(),
                    "是"
            });
            return;
        }
        model.addRow(new Object[]{
                id,
                name,
                rs.getInt("level"),
                rs.getInt("job"),
                "-",
                readMapId(rs),
                isOnline ? "是" : "否"
        });
    }

    private static int readMapId(ResultSet rs) throws java.sql.SQLException {
        Object map = rs.getObject("map");
        if (map == null) {
            return 0;
        }
        if (map instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(map).trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void restoreSelection(String selName) {
        if (selName == null) {
            return;
        }
        for (int i = 0; i < model.getRowCount(); i++) {
            if (selName.equals(String.valueOf(model.getValueAt(i, COL_NAME)))) {
                table.setRowSelectionInterval(i, i);
                break;
            }
        }
    }

    private void showSelectedInfo() {
        ConsoleCharacterService.PlayerTarget target = getSelectedTarget();
        if (target == null) {
            infoArea.setText("");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("角色: ").append(target.name).append('\n');
        sb.append("ID: ").append(target.characterId).append('\n');
        if (target.isOnline()) {
            Char chr = target.online;
            sb.append("狀態: 在線\n");
            AccountType type = chr.getUser() == null ? AccountType.Player : chr.getUser().getAccountType();
            sb.append("賬號權限: ").append(type.getVal()).append(" (")
                    .append(ConsoleCharacterService.accountTypeLabel(type)).append(")\n");
            sb.append("金幣: ").append(chr.getMoney()).append('\n');
            if (chr.getUser() != null) {
                sb.append("楓點: ").append(chr.getUser().getMaplePoints()).append('\n');
                sb.append("樂豆點: ").append(chr.getUser().getNxPrepaid()).append('\n');
                sb.append("贊助積分: ").append(chr.getUser().getDonationPoints()).append('\n');
                sb.append("投票積分: ").append(chr.getUser().getVotePoints()).append('\n');
                sb.append("賬號: ").append(chr.getUser().getName()).append('\n');
            }
            if (chr.getClient() != null) {
                sb.append("頻道: ").append(chr.getClient().getChannel()).append('\n');
            }
        } else {
            loadOfflineInfo(target, sb);
        }
        infoArea.setText(sb.toString());
    }

    private void loadOfflineInfo(ConsoleCharacterService.PlayerTarget target, StringBuilder sb) {
        sb.append("狀態: 離線\n");
        AccountType type = ConsoleCharacterService.readAccountType(target.characterId);
        sb.append("賬號權限: ").append(type.getVal()).append(" (")
                .append(ConsoleCharacterService.accountTypeLabel(type)).append(")\n");
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT cs.level, cs.job, cs.money, u.name AS acc, u.maplePoints, u.nxPrepaid, u.donationpoints, u.votepoints "
                             + "FROM characters c "
                             + "JOIN characterstats cs ON cs.characterid = c.id "
                             + "JOIN accounts a ON a.id = c.accid "
                             + "JOIN users u ON u.id = a.userid "
                             + "WHERE c.id = ?")) {
            ps.setInt(1, target.characterId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    sb.append("賬號: ").append(rs.getString("acc")).append('\n');
                    sb.append("等級: ").append(rs.getInt("level")).append('\n');
                    sb.append("職業: ").append(rs.getInt("job")).append('\n');
                    sb.append("金幣: ").append(rs.getLong("money")).append('\n');
                    sb.append("楓點: ").append(rs.getInt("maplePoints")).append('\n');
                    sb.append("樂豆點: ").append(rs.getInt("nxPrepaid")).append('\n');
                    sb.append("贊助積分: ").append(rs.getInt("donationpoints")).append('\n');
                    sb.append("投票積分: ").append(rs.getInt("votepoints")).append('\n');
                }
            }
        } catch (Exception ex) {
            sb.append("查詢失敗: ").append(ex.getMessage());
        }
    }

    private void openPreview() {
        ConsoleCharacterService.PlayerTarget target = getSelectedTarget();
        if (target == null) {
            JOptionPane.showMessageDialog(this, "請先在列表中選中一名玩家。", "角色預覽", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "角色預覽 - " + target.name);
        net.swordie.ms.console.preview.CharacterPreviewPane pane = new net.swordie.ms.console.preview.CharacterPreviewPane();
        dialog.getContentPane().add(pane);
        dialog.setSize(760, 560);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        pane.show(target.name);
    }

    private void openGiveItemDialog() {
        ConsoleCharacterService.PlayerTarget target = getSelectedTarget();
        if (target == null) {
            JOptionPane.showMessageDialog(this, "請先在列表中選中一名玩家。", "發送物品", JOptionPane.WARNING_MESSAGE);
            return;
        }
        GiveItemDialog dialog = new GiveItemDialog(SwingUtilities.getWindowAncestor(this), target);
        dialog.setVisible(true);
    }

    private void giveMeso() {
        promptAmount("金幣數量（可爲負）", "1000000", amount -> {
            ConsoleCharacterService.OpResult result = ConsoleCharacterService.giveMeso(getSelectedTarget(), amount);
            showOpResult(result, "發送金幣");
        });
    }

    private void giveNxPrepaid() {
        promptIntAmount("樂豆點數量（可爲負）", "1000", amount ->
                showOpResult(ConsoleCharacterService.giveNxPrepaid(getSelectedTarget(), amount), "發送樂豆點"));
    }

    private void giveMaplePoints() {
        promptIntAmount("楓點數量（可爲負）", "1000", amount ->
                showOpResult(ConsoleCharacterService.giveMaplePoints(getSelectedTarget(), amount), "發送楓點"));
    }

    private void giveDonationPoints() {
        promptIntAmount("贊助積分", "100", amount ->
                showOpResult(ConsoleCharacterService.giveDonationPoints(getSelectedTarget(), amount), "贊助積分"));
    }

    private void giveVotePoints() {
        promptIntAmount("投票積分", "100", amount ->
                showOpResult(ConsoleCharacterService.giveVotePoints(getSelectedTarget(), amount), "投票積分"));
    }

    private void setAccountType(AccountType type) {
        ConsoleCharacterService.PlayerTarget target = getSelectedTarget();
        if (target == null) {
            JOptionPane.showMessageDialog(this, "請先選擇一名玩家。", "設置賬號權限", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ConsoleCharacterService.OpResult result = ConsoleCharacterService.setAccountType(target, type);
        showOpResult(result, "設置賬號權限");
        showSelectedInfo();
    }

    private void sendMessageToSelected() {
        ConsoleCharacterService.PlayerTarget target = getSelectedTarget();
        if (target == null || !target.isOnline()) {
            JOptionPane.showMessageDialog(this, "請選擇在線角色。", "發送提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String text = JOptionPane.showInputDialog(this, "提示內容", "[TMS282.3] 管理員消息");
        if (text == null || text.isBlank()) {
            return;
        }
        target.online.chatMessage(text.trim());
    }

    private void kickSelected() {
        ConsoleCharacterService.OpResult result = ConsoleCharacterService.kick(getSelectedTarget());
        showOpResult(result, "踢下線");
    }

    private void warpTown() {
        ConsoleCharacterService.OpResult result = ConsoleCharacterService.warpToFreeMarket(getSelectedTarget());
        showOpResult(result, "傳送");
    }

    private void warpToMap() {
        ConsoleCharacterService.PlayerTarget target = getSelectedTarget();
        if (target == null) {
            JOptionPane.showMessageDialog(this, "請先選擇玩家。", "傳送", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!target.isOnline()) {
            JOptionPane.showMessageDialog(this, "請選擇在線角色。", "傳送", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String s = JOptionPane.showInputDialog(this, "輸入地圖 ID", String.valueOf(target.online.getFieldID()));
        if (s == null || s.isBlank()) {
            return;
        }
        try {
            int mapId = Integer.parseInt(s.trim());
            ConsoleCharacterService.OpResult result = ConsoleCharacterService.warpToMap(target, mapId);
            showOpResult(result, "傳送");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "無效地圖 ID。", "傳送", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void promptAmount(String message, String defaultValue, java.util.function.LongConsumer action) {
        ConsoleCharacterService.PlayerTarget target = getSelectedTarget();
        if (target == null) {
            JOptionPane.showMessageDialog(this, "請先選擇玩家。", "操作", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String s = JOptionPane.showInputDialog(this, message, defaultValue);
        if (s == null || s.isBlank()) {
            return;
        }
        try {
            long amount = Long.parseLong(s.trim());
            action.accept(amount);
            showSelectedInfo();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "無效數字", "操作", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void promptIntAmount(String message, String defaultValue, java.util.function.IntConsumer action) {
        promptAmount(message, defaultValue, v -> action.accept((int) v));
    }

    private void showOpResult(ConsoleCharacterService.OpResult result, String title) {
        JOptionPane.showMessageDialog(this, result.message, title,
                result.success ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
        if (result.success) {
            refreshSelectedRowInTable();
            showSelectedInfo();
        }
    }
}
