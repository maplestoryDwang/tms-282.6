package net.swordie.ms.console.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.console.ConsoleUiStyle;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Shared look of the admin console: the server console's colours and borders, CJK-safe fonts, tables, formats. */
final class AdminUi {
    static final String FONT = "Microsoft JhengHei UI";
    static final Color OK = new Color(0x2E7D32);
    static final Color WARN = new Color(0xB26A00);
    static final Color BAD = ConsoleUiStyle.SERVER_STOP;
    static final Color MUTED = new Color(0x666666);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private AdminUi() {
    }

    static Font font(int style, float size) {
        return new Font(FONT, style, Math.round(size));
    }

    static JPanel section(String title, JComponent content) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(ConsoleUiStyle.PANEL_BG);
        TitledBorder border = BorderFactory.createTitledBorder(ConsoleUiStyle.lineBorder(), title);
        border.setTitleFont(font(Font.BOLD, 13));
        p.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(4, 6, 6, 6)));
        p.add(content, BorderLayout.CENTER);
        return p;
    }

    static JPanel panel(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setBackground(ConsoleUiStyle.PANEL_BG);
        return p;
    }

    static JPanel row(Component... items) {
        JPanel p = panel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        for (Component c : items) {
            p.add(c);
        }
        return p;
    }

    static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(font(Font.PLAIN, 13));
        return l;
    }

    static JLabel hint(String text) {
        JLabel l = new JLabel(text);
        l.setFont(font(Font.PLAIN, 12));
        l.setForeground(MUTED);
        return l;
    }

    static JLabel value() {
        JLabel l = new JLabel("-");
        l.setFont(font(Font.BOLD, 13));
        return l;
    }

    /** A plain button whose width follows its text (the server console's pill buttons are fixed to 136 px). */
    static JButton button(String text) {
        JButton b = new JButton(text);
        b.setFont(font(Font.PLAIN, 13));
        b.setFocusPainted(false);
        return b;
    }

    static JButton dangerButton(String text) {
        JButton b = button(text);
        b.setBackground(BAD);
        b.setForeground(Color.WHITE);
        b.setFont(font(Font.BOLD, 13));
        return b;
    }

    static JTextField field(int columns) {
        JTextField f = new JTextField(columns);
        f.setFont(font(Font.PLAIN, 13));
        return f;
    }

    /** A stat box like the server console's (caption over a bold value). */
    static JPanel stat(String caption, JLabel value) {
        JPanel box = ConsoleUiStyle.borderedStatBox(caption, value);
        for (Component c : box.getComponents()) {
            if (c instanceof JLabel l && l != value) {
                l.setFont(font(Font.PLAIN, 12));
            }
        }
        value.setFont(font(Font.BOLD, 15));
        box.setMinimumSize(null);
        box.setPreferredSize(null);
        Dimension d = box.getPreferredSize();
        box.setPreferredSize(new Dimension(Math.max(118, d.width), d.height));
        return box;
    }

    /** Read-only table with the given column headers and preferred widths (0 = flexible). */
    static JTable table(String[] headers, int[] widths) {
        DefaultTableModel model = new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable t = new JTable(model);
        t.setFont(font(Font.PLAIN, 13));
        t.getTableHeader().setFont(font(Font.BOLD, 13));
        t.setRowHeight(22);
        t.setFillsViewportHeight(true);
        t.setAutoCreateRowSorter(false);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer());
        for (int i = 0; i < widths.length && i < headers.length; i++) {
            if (widths[i] > 0) {
                TableColumn c = t.getColumnModel().getColumn(i);
                c.setPreferredWidth(widths[i]);
                c.setMinWidth(Math.min(widths[i], 40));
            }
        }
        return t;
    }

    static JScrollPane scroll(JComponent c) {
        JScrollPane s = new JScrollPane(c);
        s.getViewport().setBackground(Color.WHITE);
        s.setBorder(ConsoleUiStyle.lineBorder());
        return s;
    }

    static void rows(JTable table, List<Object[]> rows) {
        DefaultTableModel m = (DefaultTableModel) table.getModel();
        m.setRowCount(0);
        for (Object[] r : rows) {
            m.addRow(r);
        }
    }

    static String time(JsonNode epochMs) {
        if (epochMs == null || epochMs.isNull() || !epochMs.canConvertToLong() || epochMs.asLong() <= 0) {
            return "-";
        }
        return TIME.format(Instant.ofEpochMilli(epochMs.asLong()));
    }

    /** MM-dd HH:mm (narrow table columns). */
    static String shortTime(JsonNode epochMs) {
        if (epochMs == null || epochMs.isNull() || !epochMs.canConvertToLong() || epochMs.asLong() <= 0) {
            return "-";
        }
        return SHORT.format(Instant.ofEpochMilli(epochMs.asLong()));
    }

    static String dateTime(JsonNode epochMs) {
        if (epochMs == null || epochMs.isNull() || !epochMs.canConvertToLong() || epochMs.asLong() <= 0) {
            return "-";
        }
        return DATE_TIME.format(Instant.ofEpochMilli(epochMs.asLong()));
    }

    static String duration(long sec) {
        if (sec < 60) {
            return sec + " 秒";
        }
        if (sec < 3600) {
            return sec / 60 + " 分 " + sec % 60 + " 秒";
        }
        if (sec < 86_400) {
            return sec / 3600 + " 小時 " + sec % 3600 / 60 + " 分";
        }
        return sec / 86_400 + " 天 " + sec % 86_400 / 3600 + " 小時";
    }

    static String yesNo(boolean b) {
        return b ? "是" : "否";
    }

    static String text(JsonNode n, String key) {
        JsonNode v = n == null ? null : n.get(key);
        return v == null || v.isNull() ? "" : v.asText();
    }

    static void colored(JLabel l, String text, Color c) {
        l.setText(text);
        l.setForeground(c);
    }
}
