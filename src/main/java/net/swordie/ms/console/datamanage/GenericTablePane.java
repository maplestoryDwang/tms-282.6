package net.swordie.ms.console.datamanage;

import net.swordie.ms.connection.hikariCP.DatabaseManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.util.Arrays;

class GenericTablePane extends JPanel {
    private final JComboBox<String> tableCombo;
    private final JTextField keywordField = new JTextField(18);
    private final DefaultTableModel model = new DefaultTableModel();
    private final JTable table = new JTable(model);

    GenericTablePane(String[] tables) {
        super(new BorderLayout(8, 8));
        this.tableCombo = new JComboBox<>(tables);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(buildToolbar(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        refresh();
    }

    private JComponent buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JButton refresh = new JButton("查詢");
        refresh.addActionListener(e -> refresh());
        bar.add(new JLabel("表"));
        bar.add(tableCombo);
        bar.add(new JLabel("關鍵字"));
        bar.add(keywordField);
        bar.add(refresh);
        return bar;
    }

    private void refresh() {
        String tableName = String.valueOf(tableCombo.getSelectedItem());
        String keyword = keywordField.getText() == null ? "" : keywordField.getText().trim();
        model.setRowCount(0);
        model.setColumnCount(0);
        String sql = "SELECT * FROM `" + tableName + "` LIMIT 500";
        try {
            ConsoleDatabaseService.prepare();
            try (Connection con = DatabaseManager.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int count = meta.getColumnCount();
                for (int i = 1; i <= count; i++) {
                    model.addColumn(meta.getColumnLabel(i));
                }
                while (rs.next()) {
                    Object[] row = new Object[count];
                    boolean matched = keyword.isEmpty();
                    for (int i = 1; i <= count; i++) {
                        Object value = formatCellValue(rs.getObject(i));
                        row[i - 1] = value;
                        if (!matched && value != null && String.valueOf(value).contains(keyword)) {
                            matched = true;
                        }
                    }
                    if (matched) {
                        model.addRow(row);
                    }
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "查詢 " + tableName + " 失敗: " + ConsoleDatabaseService.formatSqlError(e),
                    "數據管理", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static Object formatCellValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof byte[] bytes) {
            if (bytes.length == 1) {
                return bytes[0] & 0xFF;
            }
            return Arrays.toString(bytes);
        }
        if (value instanceof Boolean bool) {
            return bool ? 1 : 0;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toString();
        }
        return value;
    }
}