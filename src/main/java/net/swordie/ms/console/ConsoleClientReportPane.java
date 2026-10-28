package net.swordie.ms.console;

import net.swordie.ms.connection.packet.tms282report.JsonlClientReportSource;
import net.swordie.ms.connection.packet.tms282report.Tms282ClientReport;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 客戶端回報 Tab：顯示客戶端送出、伺服器不回覆的回報類封包（版本字串、ELog、機器資訊、硬體…）。
 * <p>
 * 資料來自 {@link JsonlClientReportSource}（登入 probe 與 GUI 是不同程序，由 JSONL 檔案銜接）；
 * 背景執行緒每秒輪詢新行，再交給 EDT 更新表格。輪詢丟出的 RuntimeException 會被接住並顯示在摘要，
 * 排程不會因此停止；初次載入失敗時，來源下次仍以 readLast 的上限重試。
 * 明細（欄位與 hex dump）也在背景執行緒組好，EDT 只負責 setText。
 * 之後 282 登入搬進 server 本體時，改由 in-memory sink 直接呼叫 {@link #addReports(List)} 即可，
 * 本類別與解析器都不用改。
 */
public final class ConsoleClientReportPane extends JPanel {
    public static final int INITIAL_REPORTS = 500;
    private static final int MAX_ROWS = 5000;
    private static final long POLL_MILLIS = 1000;
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private final JsonlClientReportSource source;
    private final ReportTableModel model = new ReportTableModel();
    private final JTable table = new JTable(model);
    private final TableRowSorter<ReportTableModel> sorter = new TableRowSorter<>(model);
    private final JTextArea detail = new JTextArea();
    private final JComboBox<OpcodeFilter> filterCombo = new JComboBox<>();
    private final JLabel summaryLabel = new JLabel("共 0 筆");
    private final TreeMap<Integer, OpcodeFilter> knownOpcodes = new TreeMap<>();
    private final ScheduledExecutorService poller = daemonExecutor("client-report-tail");
    private final ScheduledExecutorService detailWorker = daemonExecutor("client-report-detail");
    /** Poller thread only: false until the first readLast has been issued. */
    private boolean initialReadIssued;
    /** EDT only. */
    private long skippedLines;
    private String pollError;
    private int detailGeneration;

    public ConsoleClientReportPane(JsonlClientReportSource source) {
        super(new BorderLayout());
        this.source = Objects.requireNonNull(source, "source");
        setOpaque(false);
        setupTable();
        setupDetail();
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildSplit(), BorderLayout.CENTER);
        poller.scheduleWithFixedDelay(this::pollOnce, 0, POLL_MILLIS, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        poller.shutdownNow();
        detailWorker.shutdownNow();
    }

    private static ScheduledExecutorService daemonExecutor(String name) {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, name);
            thread.setDaemon(true);
            return thread;
        });
    }

    /** Poller thread. Never throws: an escaping exception would cancel the fixed-delay schedule. */
    private void pollOnce() {
        try {
            List<Tms282ClientReport> reports;
            if (initialReadIssued) {
                reports = source.poll();
            } else {
                initialReadIssued = true;
                reports = source.readLast(INITIAL_REPORTS);
            }
            long skipped = source.skippedLines();
            SwingUtilities.invokeLater(() -> {
                pollError = null;
                skippedLines = skipped;
                addReports(reports);
                updateSummary();
            });
        } catch (RuntimeException e) {
            String message = e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage());
            SwingUtilities.invokeLater(() -> {
                pollError = message;
                updateSummary();
            });
        }
    }

    /** EDT only. Also the entry point for a future in-process sink. */
    public void addReports(List<Tms282ClientReport> reports) {
        if (reports.isEmpty()) {
            return;
        }
        for (Tms282ClientReport report : reports) {
            knownOpcodes.computeIfAbsent(report.opcode(), op -> {
                OpcodeFilter filter = new OpcodeFilter(op, report.opcodeHex() + " " + report.name());
                insertFilter(filter);
                return filter;
            });
        }
        model.append(reports);
        updateSummary();
    }

    private void setupTable() {
        table.setRowSorter(sorter);
        table.setFillsViewportHeight(true);
        table.setBackground(ConsoleUiStyle.LOG_BG);
        table.setForeground(ConsoleUiStyle.LOG_FG);
        table.setGridColor(new Color(0xE4E4E4));
        table.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        table.setRowHeight(table.getFontMetrics(table.getFont()).getHeight() + 4);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        int[] widths = {140, 190, 60, 150, 600};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelected();
            }
        });
    }

    private void setupDetail() {
        detail.setEditable(false);
        detail.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        detail.setBackground(ConsoleUiStyle.LOG_BG);
        detail.setForeground(ConsoleUiStyle.LOG_FG);
        detail.setText("點選上方任一列以顯示完整欄位與 hex。");
    }

    private JComponent buildToolbar() {
        JPanel bar = ConsoleUiStyle.logToolbar();
        bar.add(new JLabel("Opcode 篩選"));
        filterCombo.addItem(OpcodeFilter.ALL);
        filterCombo.addActionListener(e -> applyFilter());
        bar.add(filterCombo);
        bar.add(summaryLabel);
        JButton clearBtn = new JButton("清空");
        clearBtn.setToolTipText("只清空畫面，不刪除 " + source.file());
        clearBtn.addActionListener(e -> clearAll());
        bar.add(clearBtn);
        bar.add(ConsoleUiStyle.hintLabel("來源：" + source.file()));
        return bar;
    }

    private JComponent buildSplit() {
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(ConsoleUiStyle.LOG_BG);
        JScrollPane detailScroll = new JScrollPane(detail);
        detailScroll.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ConsoleUiStyle.BORDER));
        detailScroll.getViewport().setBackground(ConsoleUiStyle.LOG_BG);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, detailScroll);
        split.setResizeWeight(0.6);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setBackground(ConsoleUiStyle.PANEL_BG);
        return split;
    }

    private void insertFilter(OpcodeFilter filter) {
        int index = 1;
        while (index < filterCombo.getItemCount() && filterCombo.getItemAt(index).opcode() < filter.opcode()) {
            index++;
        }
        filterCombo.insertItemAt(filter, index);
    }

    private void applyFilter() {
        OpcodeFilter selected = (OpcodeFilter) filterCombo.getSelectedItem();
        if (selected == null || selected == OpcodeFilter.ALL) {
            sorter.setRowFilter(null);
        } else {
            int opcode = selected.opcode();
            sorter.setRowFilter(new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends ReportTableModel, ? extends Integer> entry) {
                    return entry.getModel().get(entry.getIdentifier()).opcode() == opcode;
                }
            });
        }
        updateSummary();
    }

    /** Builds the detail text off the EDT (a hex dump covers up to 64 KB); only the newest request is shown. */
    private void showSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return;
        }
        Tms282ClientReport report = model.get(table.convertRowIndexToModel(viewRow));
        int generation = ++detailGeneration;
        detail.setText("載入中…");
        detailWorker.execute(() -> {
            String text;
            try {
                text = describe(report);
            } catch (RuntimeException e) {
                text = "無法顯示此回報：" + e;
            }
            String shown = text;
            SwingUtilities.invokeLater(() -> {
                if (generation == detailGeneration) {
                    detail.setText(shown);
                    detail.setCaretPosition(0);
                }
            });
        });
    }

    private void clearAll() {
        detailGeneration++;
        model.clear();
        knownOpcodes.clear();
        filterCombo.removeAllItems();
        filterCombo.addItem(OpcodeFilter.ALL);
        detail.setText("");
        updateSummary();
    }

    private void updateSummary() {
        StringBuilder text = new StringBuilder("共 ").append(model.getRowCount()).append(" 筆，顯示 ")
                .append(table.getRowCount()).append(" 筆");
        if (skippedLines > 0) {
            text.append("，略過 ").append(skippedLines).append(" 行無效資料");
        }
        if (pollError != null) {
            text.append("，讀取失敗：").append(pollError);
        }
        summaryLabel.setText(text.toString());
    }

    static String describe(Tms282ClientReport report) {
        StringBuilder text = new StringBuilder(512);
        text.append("時間    ").append(report.time()).append('\n');
        text.append("來源    ").append(report.source().remoteAddress()).append('\n');
        text.append("帳號    ").append(report.source().hasAccount() ? report.source().account() : "(未登入)").append('\n');
        text.append("Opcode  CP").append(report.opcodeHex()).append("  ").append(report.name()).append("\n\n");
        int width = report.fields().stream().mapToInt(f -> f.name().length()).max().orElse(0);
        for (Tms282ClientReport.Field field : report.fields()) {
            text.append("  ").append(field.name()).append(" ".repeat(width - field.name().length()))
                    .append("  ").append(field.value()).append('\n');
        }
        byte[] raw = report.raw();
        text.append("\n原始封包（").append(report.rawLength()).append(" bytes，含 opcode");
        if (report.truncated()) {
            text.append("；只保存前 ").append(raw.length).append(" bytes");
        }
        text.append("；遮蔽欄位已清零）\n");
        ConsoleHexDump.append(text, raw);
        return text.toString();
    }

    /** Combo item: one opcode, or ALL (opcode -1). */
    private record OpcodeFilter(int opcode, String label) {
        static final OpcodeFilter ALL = new OpcodeFilter(-1, "全部");

        @Override
        public String toString() {
            return label;
        }
    }

    private static final class ReportTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"時間", "來源", "Opcode", "名稱", "摘要"};
        private final List<Tms282ClientReport> rows = new ArrayList<>();

        Tms282ClientReport get(int row) {
            return rows.get(row);
        }

        void append(List<Tms282ClientReport> reports) {
            int overflow = rows.size() + reports.size() - MAX_ROWS;
            if (overflow > 0) {
                int removed = Math.min(overflow, rows.size());
                rows.subList(0, removed).clear();
                if (removed > 0) {
                    fireTableRowsDeleted(0, removed - 1);
                }
            }
            List<Tms282ClientReport> added = reports.size() > MAX_ROWS
                    ? reports.subList(reports.size() - MAX_ROWS, reports.size()) : reports;
            int first = rows.size();
            rows.addAll(added);
            fireTableRowsInserted(first, rows.size() - 1);
        }

        void clear() {
            rows.clear();
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            Tms282ClientReport report = rows.get(row);
            return switch (column) {
                case 0 -> TIME_FORMAT.format(report.time());
                case 1 -> report.source().display();
                case 2 -> report.opcodeHex();
                case 3 -> report.name();
                default -> report.summary();
            };
        }
    }
}
