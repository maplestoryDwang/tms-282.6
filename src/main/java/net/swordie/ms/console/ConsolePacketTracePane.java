package net.swordie.ms.console;

import net.swordie.ms.connection.packet.tms282report.JsonlPacketTraceSource;
import net.swordie.ms.connection.packet.tms282report.Tms282PacketTrace;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.nio.file.Files;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 282 封包 Tab：即時顯示 282 研究 probe 或正式伺服器（{@code capture.trace=true}，登入端與頻道端）的每一個收送封包；
 * 方向 DROPPED 是正式伺服器 OutboundGuard 擋下、沒有送出的封包。
 * <p>
 * 資料來自 {@link JsonlPacketTraceSource}（probe／伺服器與 GUI 是不同程序，由 {@code logs/tms282-packets.jsonl} 銜接）。
 * 背景執行緒每 {@value #POLL_MILLIS} ms 輪詢一次，整批新封包只用一次 invokeLater 交給 EDT；上一批還沒套用前
 * 不會再讀，所以封包再多也不會塞爆 EDT 佇列。一次輪詢最多取最新 {@value #MAX_ROWS} 筆，表格同樣只留
 * {@value #MAX_ROWS} 列。暫停時不讀檔（檔案位置不動），恢復後一次補上暫停期間的封包（仍受上限限制）。
 * 敏感 opcode 的內容在寫入端就已清零，本 Tab 只看得到 opcode 和長度。
 * hex dump 在背景執行緒組好，EDT 只負責 setText；只有最新一次點選的結果會顯示。
 */
public final class ConsolePacketTracePane extends JPanel {
    public static final int INITIAL_TRACES = 1000;
    static final int MAX_ROWS = 5000;
    static final long POLL_MILLIS = 250;
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());
    private static final Color RECV_FG = new Color(0x1F5FA8);
    private static final Color SEND_FG = new Color(0x2E7D32);
    private static final Color DROPPED_FG = new Color(0xC62828);

    private final JsonlPacketTraceSource source;
    private final TraceTableModel model = new TraceTableModel();
    private final JTable table = new JTable(model);
    private final TableRowSorter<TraceTableModel> sorter = new TableRowSorter<>(model);
    private final JScrollPane tableScroll = new JScrollPane(table);
    private final JTextArea detail = new JTextArea();
    private final JComboBox<Choice<Tms282PacketTrace.Direction>> directionCombo = new JComboBox<>();
    private final JComboBox<Choice<Tms282PacketTrace.SocketKind>> socketCombo = new JComboBox<>();
    private final JTextField keywordField = new JTextField(14);
    private final JToggleButton pauseToggle = ConsoleUiStyle.packetLogToolbarToggle("暫停", false);
    private final JLabel summaryLabel = new JLabel("共 0 筆");
    private final ScheduledExecutorService poller = daemonExecutor("tms282-packet-tail");
    private final ScheduledExecutorService detailWorker = daemonExecutor("tms282-packet-detail");
    /** True while a polled batch waits for the EDT; the poller does not read again until it is applied. */
    private final AtomicBoolean batchPending = new AtomicBoolean();
    private volatile boolean paused;
    /** Poller thread only: false until the first readLast has been issued. */
    private boolean initialReadIssued;
    /** EDT only. */
    private long skippedLines;
    private long droppedLines;
    private boolean fileMissing = true;
    private String pollError;
    private int detailGeneration;

    public ConsolePacketTracePane(JsonlPacketTraceSource source) {
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
        if (paused || !batchPending.compareAndSet(false, true)) {
            return;
        }
        try {
            List<Tms282PacketTrace> traces;
            if (initialReadIssued) {
                traces = source.poll(MAX_ROWS);
            } else {
                initialReadIssued = true;
                traces = source.readLast(INITIAL_TRACES);
            }
            long skipped = source.skippedLines();
            long dropped = source.droppedLines();
            boolean missing = !Files.isRegularFile(source.file());
            SwingUtilities.invokeLater(() -> {
                try {
                    pollError = null;
                    skippedLines = skipped;
                    droppedLines = dropped;
                    fileMissing = missing;
                    addTraces(traces);
                    updateSummary();
                } finally {
                    batchPending.set(false);
                }
            });
        } catch (RuntimeException e) {
            String message = e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage());
            SwingUtilities.invokeLater(() -> {
                try {
                    pollError = message;
                    updateSummary();
                } finally {
                    batchPending.set(false);
                }
            });
        }
    }

    /** EDT only. Also the entry point for a future in-process sink. */
    public void addTraces(List<Tms282PacketTrace> traces) {
        if (traces.isEmpty()) {
            return;
        }
        boolean follow = atBottom();
        model.append(traces);
        updateSummary();
        if (follow && table.getRowCount() > 0) {
            SwingUtilities.invokeLater(() -> {
                int last = table.getRowCount() - 1;
                if (last >= 0) {
                    table.scrollRectToVisible(table.getCellRect(last, 0, true));
                }
            });
        }
    }

    private boolean atBottom() {
        JScrollBar bar = tableScroll.getVerticalScrollBar();
        return bar.getValue() + bar.getVisibleAmount() >= bar.getMaximum() - table.getRowHeight();
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
        int[] widths = {140, 60, 80, 80, 320, 120};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getColumnModel().getColumn(1).setCellRenderer(new DirectionRenderer());
        sorter.setComparator(5, Comparator.comparingInt((String text) -> Integer.parseInt(text.split(" ", 2)[0])));
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
        detail.setText("點選上方任一列以顯示 hex dump。");
    }

    private JComponent buildToolbar() {
        JPanel bar = ConsoleUiStyle.logToolbar();
        bar.add(new JLabel("方向"));
        directionCombo.addItem(Choice.all());
        for (Tms282PacketTrace.Direction direction : Tms282PacketTrace.Direction.values()) {
            directionCombo.addItem(new Choice<>(direction, direction.name()));
        }
        directionCombo.addActionListener(e -> applyFilter());
        bar.add(directionCombo);
        bar.add(new JLabel("Socket"));
        socketCombo.addItem(Choice.all());
        for (Tms282PacketTrace.SocketKind socket : Tms282PacketTrace.SocketKind.values()) {
            socketCombo.addItem(new Choice<>(socket, socket.name()));
        }
        socketCombo.addActionListener(e -> applyFilter());
        bar.add(socketCombo);
        bar.add(new JLabel("Opcode／名稱"));
        keywordField.setToolTipText("例：0074、CP0074、LP02EF、SET_FIELD（不分大小寫，部分符合即可）");
        keywordField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });
        bar.add(keywordField);
        pauseToggle.setToolTipText("暫停讀取新封包；恢復後補上暫停期間的封包（最多 " + MAX_ROWS + " 筆）");
        pauseToggle.addActionListener(e -> {
            paused = pauseToggle.isSelected();
            updateSummary();
        });
        bar.add(pauseToggle);
        JButton clearBtn = new JButton("清空畫面");
        clearBtn.setToolTipText("只清空畫面，不刪除 " + source.file());
        clearBtn.addActionListener(e -> clearAll());
        bar.add(clearBtn);
        bar.add(summaryLabel);
        bar.add(ConsoleUiStyle.hintLabel("來源：" + source.file()));
        return bar;
    }

    private JComponent buildSplit() {
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

    @SuppressWarnings("unchecked")
    private void applyFilter() {
        TraceFilter filter = new TraceFilter(((Choice<Tms282PacketTrace.Direction>) directionCombo.getSelectedItem()).value(),
                ((Choice<Tms282PacketTrace.SocketKind>) socketCombo.getSelectedItem()).value(), keywordField.getText());
        sorter.setRowFilter(filter.matchesAll() ? null : new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends TraceTableModel, ? extends Integer> entry) {
                return filter.test(entry.getModel().get(entry.getIdentifier()));
            }
        });
        updateSummary();
    }

    /** Builds the detail text off the EDT (up to 64 KB of hex); only the newest request is shown. */
    private void showSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return;
        }
        Tms282PacketTrace trace = model.get(table.convertRowIndexToModel(viewRow));
        int generation = ++detailGeneration;
        detail.setText("載入中…");
        detailWorker.execute(() -> {
            String text;
            try {
                text = describe(trace);
            } catch (RuntimeException e) {
                text = "無法顯示此封包：" + e;
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
        detail.setText("");
        updateSummary();
    }

    private void updateSummary() {
        StringBuilder text = new StringBuilder("共 ").append(model.getRowCount()).append(" 筆，顯示 ")
                .append(table.getRowCount()).append(" 筆");
        if (paused) {
            text.append("（已暫停）");
        }
        if (fileMissing && model.getRowCount() == 0) {
            text.append("，等待 probe／伺服器寫入");
        }
        if (droppedLines > 0) {
            text.append("，爆量略過 ").append(droppedLines).append(" 筆");
        }
        if (skippedLines > 0) {
            text.append("，略過 ").append(skippedLines).append(" 行無效資料");
        }
        if (pollError != null) {
            text.append("，讀取失敗：").append(pollError);
        }
        summaryLabel.setText(text.toString());
    }

    static String describe(Tms282PacketTrace trace) {
        byte[] data = trace.data();
        StringBuilder text = new StringBuilder(256 + data.length * 5);
        text.append("時間    ").append(trace.time()).append('\n');
        text.append("方向    ").append(trace.direction()).append("  (").append(trace.socket()).append(")\n");
        text.append("Session ").append(trace.session()).append("  #").append(trace.sequence()).append('\n');
        text.append("Opcode  ").append(trace.opcodeLabel()).append("  ").append(trace.name()).append('\n');
        text.append("長度    ").append(trace.length()).append(" bytes（含 opcode）\n\n");
        if (trace.redacted()) {
            text.append("敏感 opcode（帳密／第二組密碼）：opcode 以外的內容已全部清零，只保留長度。\n");
        }
        if (trace.truncated()) {
            text.append("只保存前 ").append(data.length).append(" bytes。\n");
        }
        if (trace.direction() == Tms282PacketTrace.Direction.DROPPED) {
            text.append("OutboundGuard 擋下：此封包沒有送到客戶端（不寫 send-*.bin）。\n");
        }
        ConsoleHexDump.append(text, data);
        return text.toString();
    }

    private static String lengthText(Tms282PacketTrace trace) {
        if (trace.redacted()) {
            return trace.length() + " (遮蔽)";
        }
        return trace.truncated() ? trace.length() + " (截斷)" : Integer.toString(trace.length());
    }

    /** Filter of the toolbar: null direction/socket means any; the keyword matches the opcode label, hex or name. */
    record TraceFilter(Tms282PacketTrace.Direction direction, Tms282PacketTrace.SocketKind socket, String keyword) {
        TraceFilter {
            String k = keyword == null ? "" : keyword.strip().toUpperCase(Locale.ROOT);
            keyword = k.startsWith("0X") ? k.substring(2) : k;
        }

        boolean matchesAll() {
            return direction == null && socket == null && keyword.isEmpty();
        }

        boolean test(Tms282PacketTrace trace) {
            if (direction != null && trace.direction() != direction || socket != null && trace.socket() != socket) {
                return false;
            }
            return keyword.isEmpty() || trace.opcodeLabel().contains(keyword)
                    || trace.name().toUpperCase(Locale.ROOT).contains(keyword);
        }
    }

    /** Combo item: one value, or "全部" (null). */
    private record Choice<T>(T value, String label) {
        static <T> Choice<T> all() {
            return new Choice<>(null, "全部");
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final class DirectionRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                c.setForeground("RECV".equals(value) ? RECV_FG : "DROPPED".equals(value) ? DROPPED_FG : SEND_FG);
            }
            return c;
        }
    }

    private static final class TraceTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"時間", "方向", "Socket", "Opcode", "名稱", "長度"};
        private final List<Tms282PacketTrace> rows = new ArrayList<>();

        Tms282PacketTrace get(int row) {
            return rows.get(row);
        }

        void append(List<Tms282PacketTrace> traces) {
            List<Tms282PacketTrace> added = traces.size() > MAX_ROWS
                    ? traces.subList(traces.size() - MAX_ROWS, traces.size()) : traces;
            int overflow = rows.size() + added.size() - MAX_ROWS;
            if (overflow > 0) {
                int removed = Math.min(overflow, rows.size());
                rows.subList(0, removed).clear();
                fireTableRowsDeleted(0, removed - 1);
            }
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
            Tms282PacketTrace trace = rows.get(row);
            return switch (column) {
                case 0 -> TIME_FORMAT.format(trace.time());
                case 1 -> trace.direction().name();
                case 2 -> trace.socket().name();
                case 3 -> trace.opcodeLabel();
                case 4 -> trace.name();
                default -> lengthText(trace);
            };
        }
    }
}
