package net.swordie.ms.console;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

public final class ConsoleUiStyle {
    public static final Color PANEL_BG = new Color(0xFAFAFA);
    public static final Color BG = PANEL_BG;
    public static final Color SIDEBAR_BG = new Color(0xE8E8E8);
    public static final Color LOG_BG = new Color(0xFFFFFF);
    public static final Color LOG_FG = new Color(0x222222);
    public static final Color BORDER = new Color(0xB0B0B0);
    public static final Color ACCENT = new Color(0x4A90D9);
    public static final Color SERVER_STOP = new Color(0xC75050);
    /** 工具列提示文字與分隔符的顏色。 */
    public static final Color HINT_FG = new Color(0x888888);

    private ConsoleUiStyle() {
    }

    public static void applyFrame(JFrame frame) {
        ConsoleIconLoader.applyFrameIcon(frame);
        frame.getContentPane().setBackground(BG);
        frame.getRootPane().putClientProperty("JRootPane.titleBarBackground", PANEL_BG);
        frame.getRootPane().putClientProperty("JRootPane.titleBarForeground", Color.DARK_GRAY);
    }

    public static Border lineBorder() {
        return BorderFactory.createLineBorder(BORDER);
    }

    public static TitledBorder logSectionBorder() {
        TitledBorder border = BorderFactory.createTitledBorder(lineBorder(), "服務端運行日誌");
        border.setTitleFont(border.getTitleFont().deriveFont(Font.BOLD, 12f));
        return border;
    }

    public static JTabbedPane logTabs() {
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(tabs.getFont().deriveFont(12f));
        tabs.setBackground(PANEL_BG);
        tabs.setOpaque(true);
        return tabs;
    }

    /** 工具列上的小字提示（封包日誌、客戶端回報共用）。 */
    public static JLabel hintLabel(String text) {
        JLabel hint = new JLabel(text);
        hint.setForeground(HINT_FG);
        hint.setFont(hint.getFont().deriveFont(11f));
        return hint;
    }

    /** 日誌 Tab 頂部工具列（封包日誌、客戶端回報共用）。 */
    public static JPanel logToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setOpaque(true);
        bar.setBackground(PANEL_BG);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        return bar;
    }

    public static JPanel section(String title, JComponent content) {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(PANEL_BG);
        section.setBorder(BorderFactory.createTitledBorder(lineBorder(), title));
        section.add(content, BorderLayout.CENTER);
        return section;
    }

    public static JPanel wrapSection(JComponent content, String title) {
        return wrapSection(content, title, 4, 8, 6, 8);
    }

    public static JPanel wrapSection(JComponent content, String title, int top, int left, int bottom, int right) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(PANEL_BG);
        wrap.setBorder(BorderFactory.createTitledBorder(lineBorder(), title));
        content.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
        wrap.add(content, BorderLayout.CENTER);
        return wrap;
    }

    public static JPanel sidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setBorder(BorderFactory.createCompoundBorder(lineBorder(), BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        sidebar.setPreferredSize(new Dimension(152, 0));
        return sidebar;
    }

    public static JButton sidebarActionButton(String text) {
        JButton button = new JButton(text);
        stylePillButton(button);
        return button;
    }

    public static JToggleButton sidebarToggleButton(String text) {
        JToggleButton button = new JToggleButton(text);
        stylePillButton(button);
        button.setForeground(new Color(0x333333));
        return button;
    }

    public static JToggleButton packetLogToolbarToggle(String text, boolean selected) {
        JToggleButton button = new JToggleButton(text, selected);
        button.setUI(new PacketLogToolbarToggleUI());
        button.setFocusPainted(false);
        button.setFont(button.getFont().deriveFont(12f));
        button.setForeground(new Color(0x333333));
        return button;
    }

    private static void stylePillButton(AbstractButton button) {
        button.setUI(new PillButtonUI());
        button.setFocusPainted(false);
        button.setFont(button.getFont().deriveFont(12f));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(136, 34));
        button.setPreferredSize(new Dimension(136, 34));
        button.setMinimumSize(new Dimension(136, 34));
        button.setBackground(new Color(0xF2F2F2));
        button.setForeground(new Color(0x333333));
    }

    public static JButton serverToggleButton() {
        JButton button = sidebarActionButton("啓動服務端");
        button.setFont(button.getFont().deriveFont(Font.BOLD, 12f));
        button.setMaximumSize(new Dimension(136, 38));
        button.setPreferredSize(new Dimension(136, 38));
        button.setMinimumSize(new Dimension(136, 38));
        return button;
    }

    public static void styleServerButton(JButton button, boolean running, boolean starting) {
        if (starting) {
            button.setText("正在啓動…");
            button.setEnabled(false);
            button.setBackground(new Color(0x9E9E9E));
            button.setForeground(Color.WHITE);
            return;
        }
        button.setEnabled(true);
        button.setText(running ? "結束服務端" : "啓動服務端");
        button.setBackground(running ? SERVER_STOP : ACCENT);
        button.setForeground(Color.WHITE);
    }

    /** 視口比內容略高，避免統計塊邊框/字體被裁切。 */
    private static final int STAT_ROW_VIEW_SLACK = 10;
    private static final String KEY_RESIZE_LISTENER = "console.stat.resizeListener";
    private static final String KEY_HIER_LISTENER = "console.stat.hierListener";
    private static final String KEY_LAST_SCROLL_H = "console.stat.lastScrollH";
    private static final String KEY_LAST_VIEW_W = "console.stat.lastViewW";
    private static final String KEY_LAST_NEED_SCROLL = "console.stat.lastNeedScroll";
    private static final String KEY_SCROLL_UPDATING = "console.stat.updating";
    private static final String KEY_SCROLL_PENDING = "console.stat.pending";
    private static final String KEY_SCROLL_ALIVE = "console.stat.alive";

    public static int statBoxRowHeight() {
        JLabel probe = new JLabel("0");
        return borderedStatBox("頻道", probe).getPreferredSize().height;
    }

    public static JScrollPane wrappingStatScrollPane(JPanel boxesPanel, int maxRows, int vgap) {
        JScrollPane scroll = new JScrollPane(boxesPanel);
        scroll.putClientProperty(KEY_SCROLL_ALIVE, Boolean.TRUE);
        scroll.setBorder(null);
        scroll.setOpaque(true);
        scroll.setBackground(PANEL_BG);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(PANEL_BG);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        int rowH = statBoxRowHeight();
        int maxH = rowH * maxRows + vgap * Math.max(0, maxRows - 1);
        scroll.getVerticalScrollBar().setUnitIncrement(rowH + vgap);

        ComponentAdapter listener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (isStatScrollAlive(scroll)) {
                    scheduleStatScrollHeightUpdate(scroll, boxesPanel, maxH, vgap);
                }
            }
        };
        scroll.putClientProperty(KEY_RESIZE_LISTENER, listener);
        scroll.getViewport().addComponentListener(listener);
        HierarchyListener hierListener = e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && scroll.isShowing()
                    && isStatScrollAlive(scroll)) {
                scheduleStatScrollHeightUpdate(scroll, boxesPanel, maxH, vgap);
            }
        };
        scroll.putClientProperty(KEY_HIER_LISTENER, hierListener);
        scroll.addHierarchyListener(hierListener);
        scheduleStatScrollHeightUpdate(scroll, boxesPanel, maxH, vgap);
        return scroll;
    }

    private static boolean isStatScrollAlive(JScrollPane scroll) {
        return scroll != null && Boolean.TRUE.equals(scroll.getClientProperty(KEY_SCROLL_ALIVE));
    }

    private static void scheduleStatScrollHeightUpdate(JScrollPane scroll, JPanel boxesPanel, int maxH, int vgap) {
        if (!isStatScrollAlive(scroll)) {
            return;
        }
        if (Boolean.TRUE.equals(scroll.getClientProperty(KEY_SCROLL_PENDING))) {
            return;
        }
        scroll.putClientProperty(KEY_SCROLL_PENDING, Boolean.TRUE);
        SwingUtilities.invokeLater(() -> {
            scroll.putClientProperty(KEY_SCROLL_PENDING, null);
            if (isStatScrollAlive(scroll)) {
                updateWrappingStatScrollHeight(scroll, boxesPanel, maxH, vgap);
            }
        });
    }

    public static void disposeWrappingStatScroll(JScrollPane scroll) {
        if (scroll == null) {
            return;
        }
        scroll.putClientProperty(KEY_SCROLL_ALIVE, Boolean.FALSE);
        scroll.putClientProperty(KEY_SCROLL_PENDING, null);
        ComponentAdapter listener = (ComponentAdapter) scroll.getClientProperty(KEY_RESIZE_LISTENER);
        if (listener != null) {
            scroll.removeComponentListener(listener);
            scroll.getViewport().removeComponentListener(listener);
        }
        HierarchyListener hierListener = (HierarchyListener) scroll.getClientProperty(KEY_HIER_LISTENER);
        if (hierListener != null) {
            scroll.removeHierarchyListener(hierListener);
        }
        scroll.putClientProperty(KEY_RESIZE_LISTENER, null);
        scroll.putClientProperty(KEY_HIER_LISTENER, null);
        scroll.putClientProperty(KEY_LAST_SCROLL_H, null);
        scroll.putClientProperty(KEY_LAST_VIEW_W, null);
        scroll.putClientProperty(KEY_LAST_NEED_SCROLL, null);
        scroll.putClientProperty(KEY_SCROLL_UPDATING, null);
        scroll.putClientProperty(KEY_SCROLL_PENDING, null);
        scroll.putClientProperty(KEY_SCROLL_ALIVE, null);
    }

    /** 佈局寬度：始終預留縱向滾動條寬度，避免 AS_NEEDED 顯隱引起寬度抖動與反覆 revalidate。 */
    private static int layoutWidthForStatScroll(JScrollPane scroll) {
        int w = scroll.getViewport().getExtentSize().width;
        if (w <= 0) {
            w = scroll.getViewport().getWidth();
        }
        if (w <= 0) {
            w = scroll.getWidth();
        }
        if (w <= 0) {
            return 0;
        }
        int barW = scroll.getVerticalScrollBar().getPreferredSize().width;
        return Math.max(1, w - barW);
    }

    private static void updateWrappingStatScrollHeight(JScrollPane scroll, JPanel boxesPanel, int maxH, int vgap) {
        if (!isStatScrollAlive(scroll) || !scroll.isDisplayable()
                || Boolean.TRUE.equals(scroll.getClientProperty(KEY_SCROLL_UPDATING))) {
            return;
        }
        int viewW = layoutWidthForStatScroll(scroll);
        if (viewW <= 0) {
            return;
        }
        int contentH = measureBoxesPanelHeightAtWidth(boxesPanel, viewW, vgap);
        boolean needScroll = contentH > maxH;
        int viewH = (needScroll ? maxH : contentH) + STAT_ROW_VIEW_SLACK;
        Insets scrollPad = scroll.getInsets();
        int h = viewH + scrollPad.top + scrollPad.bottom;
        Integer lastH = (Integer) scroll.getClientProperty(KEY_LAST_SCROLL_H);
        Integer lastW = (Integer) scroll.getClientProperty(KEY_LAST_VIEW_W);
        Boolean lastNeedScroll = (Boolean) scroll.getClientProperty(KEY_LAST_NEED_SCROLL);
        if (lastH != null && lastH == h && lastW != null && lastW == viewW
                && lastNeedScroll != null && lastNeedScroll == needScroll) {
            return;
        }
        scroll.putClientProperty(KEY_SCROLL_UPDATING, Boolean.TRUE);
        try {
            boxesPanel.setSize(viewW, Math.max(contentH, 1));
            boxesPanel.setPreferredSize(new Dimension(viewW, contentH));
            boxesPanel.invalidate();
            boxesPanel.validate();

            scroll.putClientProperty(KEY_LAST_SCROLL_H, h);
            scroll.putClientProperty(KEY_LAST_VIEW_W, viewW);
            scroll.putClientProperty(KEY_LAST_NEED_SCROLL, needScroll);
            scroll.setVerticalScrollBarPolicy(needScroll
                    ? ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
                    : ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            if (!needScroll) {
                scroll.getViewport().setViewPosition(new Point(0, 0));
            }
            Dimension scrollSize = new Dimension(Integer.MAX_VALUE, h);
            if (!scrollSize.equals(scroll.getPreferredSize())) {
                scroll.setPreferredSize(scrollSize);
                scroll.setMaximumSize(scrollSize);
            }
            Container parent = scroll.getParent();
            if (parent instanceof JComponent sectionWrap) {
                stretchBoxWidth(sectionWrap);
                sectionWrap.revalidate();
            }
        } finally {
            scroll.putClientProperty(KEY_SCROLL_UPDATING, null);
        }
    }

    private static int measureBoxesPanelHeightAtWidth(JPanel boxesPanel, int viewW, int vgap) {
        boxesPanel.setSize(viewW, 1);
        return measureBoxesPanelHeight(boxesPanel, vgap);
    }

    /** 內容區實際高度（與 WrapFlowLayout 單行佈局一致，不額外留白）。 */
    private static int measureBoxesPanelHeight(JPanel boxesPanel, int vgap) {
        Insets insets = boxesPanel.getInsets();
        int maxBoxH = 0;
        for (Component child : boxesPanel.getComponents()) {
            if (child.isVisible()) {
                maxBoxH = Math.max(maxBoxH, child.getPreferredSize().height);
            }
        }
        int oneRowH = insets.top + vgap + maxBoxH + insets.bottom;
        int layoutH = boxesPanel.getPreferredSize().height;
        return Math.max(oneRowH, layoutH);
    }

    public static JPanel borderedStatBox(String title, JLabel value) {
        JPanel box = new JPanel(new GridBagLayout());
        box.setBackground(PANEL_BG);
        box.setBorder(BorderFactory.createCompoundBorder(
                lineBorder(),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)));
        JLabel cap = new JLabel(title, SwingConstants.CENTER);
        cap.setFont(cap.getFont().deriveFont(11f));
        cap.setForeground(new Color(0x666666));
        value.setFont(value.getFont().deriveFont(Font.BOLD, 14f));
        value.setHorizontalAlignment(SwingConstants.CENTER);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 4, 0);
        box.add(cap, gbc);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        box.add(value, gbc);
        Dimension pref = box.getPreferredSize();
        int minW = Math.max(92, pref.width);
        box.setMinimumSize(new Dimension(minW, pref.height));
        box.setPreferredSize(new Dimension(minW, pref.height));
        return box;
    }

    public static JPanel statBox(String title, JLabel value) {
        return borderedStatBox(title, value);
    }

    public static JPopupMenu createLogContextMenu(Runnable copy, Runnable save, Runnable clear) {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem copyItem = new JMenuItem("複製");
        copyItem.addActionListener(e -> copy.run());
        JMenuItem saveItem = new JMenuItem("保存日誌");
        saveItem.addActionListener(e -> save.run());
        JMenuItem clearItem = new JMenuItem("清空");
        clearItem.addActionListener(e -> clear.run());
        popup.add(copyItem);
        popup.add(saveItem);
        popup.addSeparator();
        popup.add(clearItem);
        return popup;
    }

    public static void stretchBoxWidth(JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension pref = component.getPreferredSize();
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));
    }
}