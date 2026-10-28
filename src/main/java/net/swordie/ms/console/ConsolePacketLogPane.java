package net.swordie.ms.console;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseWheelListener;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** 封包日誌 Tab：單行不換行、橫向滾動、客戶端/服務端/未知封包分色顯示。 */
public final class ConsolePacketLogPane extends JPanel {
    private final NoWrapTextPane area = new NoWrapTextPane();
    private final PacketLogAppender appender;
    private final JLabel filterSummaryLabel = new JLabel("已出現 0 種封包");
    private final JPanel filterCheckboxPanel = new JPanel();
    private final Map<String, JCheckBox> typeCheckboxes = new LinkedHashMap<>();
    private JPopupMenu filterPopup;
    private final Timer statusTimer;

    public ConsolePacketLogPane() {
        super(new BorderLayout());
        setOpaque(false);
        PacketLogFilter.load();
        setupArea();
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBackground(ConsoleUiStyle.PANEL_BG);
        scroll.getViewport().setBackground(ConsoleUiStyle.LOG_BG);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        configureLogScrollSpeed(scroll);
        appender = new PacketLogAppender(area, scroll);
        appender.setOnNewType(this::onNewPacketType);
        appender.setOnTypesPruned(this::onPacketTypesPruned);
        PacketLogBridge.setAppender(appender);
        setupContextMenu(scroll);
        add(buildToolbar(), BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        statusTimer = new Timer(250, e -> {
            if (appender.getPendingLineCount() > 0 || appender.getDroppedLineCount() > 0) {
                updateFilterSummary();
            }
        });
        statusTimer.setRepeats(true);
        statusTimer.setCoalesce(true);
        statusTimer.start();
    }

    public PacketLogAppender getAppender() {
        return appender;
    }

    public void shutdown() {
        statusTimer.stop();
        appender.shutdown();
    }

    public boolean isPacketLogEnabled() {
        return appender.isEnabled();
    }

    private void setupArea() {
        area.setEditable(false);
        area.setCaret(new DefaultCaret() {
            @Override
            public void setVisible(boolean visible) {
                super.setVisible(false);
            }
        });
        area.setBackground(ConsoleUiStyle.LOG_BG);
        area.setOpaque(true);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    }

    private void configureLogScrollSpeed(JScrollPane scroll) {
        int lineHeight = area.getFontMetrics(area.getFont()).getHeight();
        int verticalUnit = Math.max(1, lineHeight * 3);
        int horizontalUnit = 48;
        JScrollBar vBar = scroll.getVerticalScrollBar();
        if (vBar != null) {
            vBar.setUnitIncrement(verticalUnit);
            vBar.setBlockIncrement(Math.max(320, verticalUnit * 8));
        }
        JScrollBar hBar = scroll.getHorizontalScrollBar();
        if (hBar != null) {
            hBar.setUnitIncrement(horizontalUnit);
            hBar.setBlockIncrement(240);
        }
        MouseWheelListener wheelHandler = e -> {
            boolean horizontal = e.isShiftDown();
            JScrollBar bar = horizontal ? hBar : vBar;
            if (bar == null || !bar.isVisible()) {
                return;
            }
            int step = horizontal ? horizontalUnit : verticalUnit;
            int next = bar.getValue() + e.getWheelRotation() * step;
            bar.setValue(Math.max(bar.getMinimum(), Math.min(bar.getMaximum(), next)));
            e.consume();
        };
        scroll.addMouseWheelListener(wheelHandler);
        area.addMouseWheelListener(wheelHandler);
    }

    private JPanel buildToolbar() {
        JPanel bar = ConsoleUiStyle.logToolbar();

        JButton filterBtn = new JButton("封包過濾 ▼");
        filterBtn.setToolTipText("勾選要顯示的封包類型，僅列出日誌中已出現的類型");
        filterBtn.addActionListener(e -> showFilterPopup(filterBtn));
        bar.add(filterBtn);
        bar.add(filterSummaryLabel);

        JButton selectAllBtn = new JButton("全選");
        selectAllBtn.addActionListener(e -> {
            setAllCheckboxes(true);
            appender.setAllTypesVisible(true);
            updateFilterSummary();
        });
        bar.add(selectAllBtn);

        JButton deselectAllBtn = new JButton("取消全選");
        deselectAllBtn.addActionListener(e -> {
            setAllCheckboxes(false);
            appender.setAllTypesVisible(false);
            updateFilterSummary();
        });
        bar.add(deselectAllBtn);

        JToggleButton enabledBtn = ConsoleUiStyle.packetLogToolbarToggle("封包日誌", appender.isEnabled());
        enabledBtn.addActionListener(e -> appender.setEnabled(enabledBtn.isSelected()));
        bar.add(enabledBtn);

        JToggleButton simpleLogBtn = ConsoleUiStyle.packetLogToolbarToggle("簡單日誌", PacketLogFilter.isSimpleLogEnabled());
        simpleLogBtn.setToolTipText("開啓後僅顯示封包頭和前 " + PacketLogFilter.SIMPLE_LOG_HEX_MAX_CHARS
                + " 個十六進制字符（寫入時截斷，切換後需新封包生效）");
        simpleLogBtn.addActionListener(e -> {
            PacketLogFilter.setSimpleLogEnabled(simpleLogBtn.isSelected());
            appender.reapplySimpleLogMode();
        });
        bar.add(simpleLogBtn);

        JToggleButton ignoreHighFreqBtn = ConsoleUiStyle.packetLogToolbarToggle("忽略高頻",
                PacketLogFilter.isIgnoreHighFrequencySendEnabled());
        ignoreHighFreqBtn.setToolTipText("開啓後跳過移動、心跳等高頻封包，避免 GUI 日誌刷屏");
        ignoreHighFreqBtn.addActionListener(e -> PacketLogFilter.setIgnoreHighFrequencySendEnabled(ignoreHighFreqBtn.isSelected()));
        bar.add(ignoreHighFreqBtn);

        JButton clearBtn = new JButton("清空");
        clearBtn.addActionListener(e -> clearAll());
        bar.add(clearBtn);

        bar.add(ConsoleUiStyle.hintLabel(" "));

        JLabel clientLegend = new JLabel("客戶端");
        clientLegend.setForeground(PacketLogAppender.COLOR_CLIENT);
        bar.add(clientLegend);
        JLabel sep = new JLabel("/");
        sep.setForeground(ConsoleUiStyle.HINT_FG);
        bar.add(sep);
        JLabel serverLegend = new JLabel("服務端");
        serverLegend.setForeground(PacketLogAppender.COLOR_SERVER);
        bar.add(serverLegend);
        JLabel unkSep = new JLabel("/");
        unkSep.setForeground(ConsoleUiStyle.HINT_FG);
        bar.add(unkSep);
        JLabel unknownLegend = new JLabel("未知");
        unknownLegend.setForeground(PacketLogAppender.COLOR_UNKNOWN);
        bar.add(unknownLegend);
        return bar;
    }

    private void showFilterPopup(Component anchor) {
        if (filterPopup == null) {
            filterPopup = new JPopupMenu();
            filterCheckboxPanel.setLayout(new BoxLayout(filterCheckboxPanel, BoxLayout.Y_AXIS));
            filterCheckboxPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            JScrollPane scroll = new JScrollPane(filterCheckboxPanel);
            scroll.setPreferredSize(new Dimension(320, 280));
            scroll.setBorder(BorderFactory.createEmptyBorder());
            filterPopup.add(scroll);
        }
        syncCheckboxesFromAppender();
        filterPopup.show(anchor, 0, anchor.getHeight());
    }

    private void onNewPacketType(String type) {
        runOnEdt(() -> {
            addTypeCheckbox(type, true);
            updateFilterSummary();
        });
    }

    private void onPacketTypesPruned(Set<String> removed) {
        if (removed == null || removed.isEmpty()) {
            return;
        }
        runOnEdt(() -> {
            for (String type : removed) {
                JCheckBox box = typeCheckboxes.remove(type);
                if (box != null) {
                    filterCheckboxPanel.remove(box);
                }
            }
            filterCheckboxPanel.revalidate();
            filterCheckboxPanel.repaint();
            updateFilterSummary();
        });
    }

    private static void runOnEdt(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    private void addTypeCheckbox(String type, boolean selected) {
        if (typeCheckboxes.containsKey(type)) {
            return;
        }
        JCheckBox box = new JCheckBox(type, selected);
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.addActionListener(e -> {
            appender.setTypeVisible(type, box.isSelected());
            updateFilterSummary();
        });
        typeCheckboxes.put(type, box);
        filterCheckboxPanel.add(box);
        filterCheckboxPanel.revalidate();
        filterCheckboxPanel.repaint();
    }

    private void syncCheckboxesFromAppender() {
        for (String type : appender.getKnownTypes()) {
            addTypeCheckbox(type, appender.isTypeVisible(type));
        }
        for (Map.Entry<String, JCheckBox> e : typeCheckboxes.entrySet()) {
            e.getValue().setSelected(appender.isTypeVisible(e.getKey()));
        }
        updateFilterSummary();
    }

    private void setAllCheckboxes(boolean selected) {
        for (JCheckBox box : typeCheckboxes.values()) {
            box.setSelected(selected);
        }
    }

    private void updateFilterSummary() {
        int total = typeCheckboxes.size();
        long shown = typeCheckboxes.values().stream().filter(AbstractButton::isSelected).count();
        int pending = appender.getPendingLineCount();
        long dropped = appender.getDroppedLineCount();
        StringBuilder summary = new StringBuilder(96);
        summary.append("已出現 ").append(total).append(" 種封包，顯示 ").append(shown).append(" 種");
        if (pending > 0) {
            summary.append("，待刷新 ").append(pending).append(" 行");
        }
        if (dropped > 0) {
            summary.append("，已丟棄 ").append(dropped).append(" 行");
        }
        filterSummaryLabel.setText(summary.toString());
    }

    private void setupContextMenu(JScrollPane scroll) {
        JPopupMenu popup = ConsoleUiStyle.createLogContextMenu(this::copyText, () -> appender.saveToFile(this), this::clearAll);
        area.setComponentPopupMenu(popup);
        scroll.setComponentPopupMenu(popup);
    }

    private void copyText() {
        String selected = appender.getSelectedText();
        String text = selected != null && !selected.isEmpty() ? selected : appender.getText();
        if (text == null || text.isEmpty()) {
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }

    private void clearAll() {
        appender.clear();
        typeCheckboxes.clear();
        filterCheckboxPanel.removeAll();
        updateFilterSummary();
    }

    /** 單行不換行：寬度隨內容增長，超長行由橫向滾動條查看。 */
    private static final class NoWrapTextPane extends JTextPane {
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return false;
        }
    }
}
