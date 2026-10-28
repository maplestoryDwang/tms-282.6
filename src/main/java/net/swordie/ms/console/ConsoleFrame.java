package net.swordie.ms.console;

import net.swordie.ms.Server;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.tms282report.JsonlClientReportSink;
import net.swordie.ms.connection.packet.tms282report.JsonlClientReportSource;
import net.swordie.ms.connection.packet.tms282report.JsonlPacketTraceSink;
import net.swordie.ms.connection.packet.tms282report.JsonlPacketTraceSource;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.console.config.ConfigPanel;
import net.swordie.ms.console.config.ServerRuntimeConfig;
import net.swordie.ms.console.datamanage.DataManagePanel;
import net.swordie.ms.util.FastStartup;
import net.swordie.ms.world.Channel;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.PrintStream;
import java.time.Duration;

public final class ConsoleFrame extends JFrame {
    private final JTextPane logArea = new JTextPane();
    private final JLabel totalOnlineLabel = new JLabel("-");
    private final JLabel uptimeLabel = new JLabel("運行時間: -");
    private final JLabel packetStatusLabel = new JLabel();
    private final JProgressBar memoryBar = new JProgressBar(0, 100);
    private JLabel[] channelValueLabels = new JLabel[0];
    private JPanel announceAndStatsBlock;
    private Component onlineStatsWrap;
    private final JButton serverToggleButton = ConsoleUiStyle.serverToggleButton();
    private final JToggleButton packetDetailToggle = ConsoleUiStyle.sidebarToggleButton("封包詳情");
    private final JToggleButton debugModeToggle = ConsoleUiStyle.sidebarToggleButton("調試模式");
    private final JToggleButton fastStartToggle = ConsoleUiStyle.sidebarToggleButton("快速啓動");
    private final JTextField announceField = new JTextField(48);
    private final JComboBox<String> announceTypeCombo = new JComboBox<>(new String[]{"頂部黃字公告", "彈窗公告", "聊天窗口公告", "NPC對話", "詳細提示"});
    private final Timer uiTimer;

    private ConsoleLogAppender logAppender;
    private ConsolePacketLogPane packetLogPane;
    private ConsoleClientReportPane clientReportPane;
    private ConsolePacketTracePane packetTracePane;
    private ConsoleServerThread serverThread;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private boolean serverRunning;
    private boolean serverStarting;
    private boolean serverStopping;
    private long serverStartRequestedAt;
    private long serverStartedAt;
    private int lastMemoryBarPct = -1;
    private int channelCountOverride = -1;

    public ConsoleFrame() {
        super("TMS282.3 管理控制檯");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                onClose();
            }
        });
        setupLogArea();
        ConsoleDebugMode.load();
        PacketLogFilter.load();
        FastStartup.load();
        ConsoleUiStyle.applyFrame(this);
        buildUi();
        updateServerButton();
        uiTimer = new Timer(1000, e -> refreshStatus());
        uiTimer.start();
        setSize(1180, 760);
        setMinimumSize(new Dimension(980, 640));
        setLocationRelativeTo(null);
    }

    private void setupLogArea() {
        logArea.setEditable(false);
        DefaultCaret caret = (DefaultCaret) logArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        logArea.setBackground(ConsoleUiStyle.LOG_BG);
        logArea.setForeground(ConsoleUiStyle.LOG_FG);
        logArea.setOpaque(true);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(ConsoleUiStyle.BG);
        root.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel centerRow = new JPanel(new BorderLayout(6, 0));
        centerRow.setOpaque(true);
        centerRow.setBackground(ConsoleUiStyle.PANEL_BG);
        centerRow.add(buildLogSection(), BorderLayout.CENTER);
        centerRow.add(buildSidebar(), BorderLayout.EAST);

        root.add(centerRow, BorderLayout.CENTER);
        root.add(buildBottom(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JComponent buildLogSection() {
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBackground(ConsoleUiStyle.PANEL_BG);
        logScroll.getViewport().setBackground(ConsoleUiStyle.LOG_BG);
        logScroll.setBorder(BorderFactory.createEmptyBorder());
        logScroll.getVerticalScrollBar().setUnitIncrement(16);
        logAppender = new ConsoleLogAppender(logArea);
        initLogStreams();
        installLogPopup(logScroll);

        JTabbedPane tabs = ConsoleUiStyle.logTabs();
        JPanel runLogTab = new JPanel(new BorderLayout());
        runLogTab.setOpaque(false);
        runLogTab.add(logScroll, BorderLayout.CENTER);
        tabs.addTab("運行日誌", runLogTab);
        packetLogPane = new ConsolePacketLogPane();
        tabs.addTab("封包日誌", packetLogPane);
        clientReportPane = new ConsoleClientReportPane(new JsonlClientReportSource(JsonlClientReportSink.defaultFile()));
        tabs.addTab("客戶端回報", clientReportPane);
        packetTracePane = new ConsolePacketTracePane(new JsonlPacketTraceSource(JsonlPacketTraceSink.defaultFile()));
        tabs.addTab("282 封包", packetTracePane);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(true);
        wrap.setBackground(ConsoleUiStyle.PANEL_BG);
        wrap.setBorder(ConsoleUiStyle.logSectionBorder());
        wrap.add(tabs, BorderLayout.CENTER);
        return wrap;
    }

    private JComponent buildSidebar() {
        JPanel sidebar = ConsoleUiStyle.sidebar();

        JButton config = ConsoleUiStyle.sidebarActionButton("功能配置");
        config.addActionListener(e -> ConfigPanel.open(this));
        sidebar.add(config);
        sidebar.add(Box.createVerticalStrut(6));

        JButton dataManage = ConsoleUiStyle.sidebarActionButton("數據管理");
        dataManage.addActionListener(e -> DataManagePanel.open(this));
        sidebar.add(dataManage);
        sidebar.add(Box.createVerticalStrut(6));

        JButton logSettings = ConsoleUiStyle.sidebarActionButton("日誌設置");
        logSettings.addActionListener(e -> showNotImplemented("日誌設置"));
        sidebar.add(logSettings);
        sidebar.add(Box.createVerticalStrut(10));

        packetDetailToggle.setSelected(ConsoleDebugMode.isPacketDetailEnabled());
        packetDetailToggle.setToolTipText("勾選後客戶端/服務端封包顯示在「封包日誌」Tab");
        packetDetailToggle.addActionListener(e -> {
            ConsoleDebugMode.setPacketDetail(packetDetailToggle.isSelected());
            updatePacketStatusLabel();
        });
        sidebar.add(packetDetailToggle);
        sidebar.add(Box.createVerticalStrut(6));

        debugModeToggle.setSelected(ConsoleDebugMode.isActive());
        debugModeToggle.setToolTipText("未知封包摘要、登錄/掉線診斷信息輸出到運行日誌");
        debugModeToggle.addActionListener(e -> {
            ConsoleDebugMode.setActive(debugModeToggle.isSelected());
            updatePacketStatusLabel();
        });
        sidebar.add(debugModeToggle);
        sidebar.add(Box.createVerticalStrut(6));

        fastStartToggle.setSelected(FastStartup.isEnabled());
        fastStartToggle.setToolTipText("啓用時數據串行加載，同時重疊綁定網絡端口，縮短總啓動時間");
        fastStartToggle.addActionListener(e -> {
            FastStartup.setEnabled(fastStartToggle.isSelected());
            updatePacketStatusLabel();
        });
        sidebar.add(fastStartToggle);

        sidebar.add(Box.createVerticalGlue());
        serverToggleButton.addActionListener(e -> onServerToggle());
        sidebar.add(serverToggleButton);
        return sidebar;
    }

    private JComponent buildBottom() {
        JPanel bottom = new JPanel(new BorderLayout(0, 4));
        bottom.setOpaque(true);
        bottom.setBackground(ConsoleUiStyle.PANEL_BG);
        bottom.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        bottom.add(buildAnnounceAndStatsBlock(), BorderLayout.NORTH);
        bottom.add(buildFooterBar(), BorderLayout.SOUTH);
        return bottom;
    }

    private JPanel buildAnnounceAndStatsBlock() {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(true);
        block.setBackground(ConsoleUiStyle.PANEL_BG);

        JPanel announceInner = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        announceInner.setOpaque(true);
        announceInner.setBackground(ConsoleUiStyle.PANEL_BG);
        JButton send = new JButton("發送消息");
        send.addActionListener(e -> sendAnnouncement());
        announceInner.add(new JLabel("類型"));
        announceInner.add(announceTypeCombo);
        announceInner.add(announceField);
        announceInner.add(send);
        JComponent announce = ConsoleUiStyle.wrapSection(announceInner, "系統公告", 2, 8, 2, 8);
        ConsoleUiStyle.stretchBoxWidth(announce);
        block.add(announce);
        block.add(Box.createVerticalStrut(4));

        announceAndStatsBlock = block;
        rebuildOnlineStatsSection();
        return block;
    }

    private JPanel buildFooterBar() {
        setupMemoryBar();
        uptimeLabel.setFont(uptimeLabel.getFont().deriveFont(12f));
        packetStatusLabel.setFont(packetStatusLabel.getFont().deriveFont(12f));
        packetStatusLabel.setForeground(new Color(0x666666));
        updatePacketStatusLabel();

        JPanel footerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 4));
        footerRight.setOpaque(false);
        footerRight.add(uptimeLabel);
        footerRight.add(memoryBar);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(ConsoleUiStyle.PANEL_BG);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ConsoleUiStyle.BORDER),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        footer.add(packetStatusLabel, BorderLayout.WEST);
        footer.add(footerRight, BorderLayout.EAST);
        return footer;
    }

    private void updatePacketStatusLabel() {
        packetStatusLabel.setText("封包詳情:" + (ConsoleDebugMode.isPacketDetailEnabled() ? "開" : "關")
                + "  調試:" + (ConsoleDebugMode.isActive() ? "開" : "關")
                + "  快速啓動:" + (FastStartup.isEnabled() ? "開" : "關"));
    }

    private static JScrollPane findOnlineStatsScroll(Container root) {
        if (root == null) {
            return null;
        }
        for (Component child : root.getComponents()) {
            if (child instanceof JScrollPane scroll) {
                return scroll;
            }
            if (child instanceof Container container) {
                JScrollPane nested = findOnlineStatsScroll(container);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private void disposeOnlineStatsWrap() {
        if (onlineStatsWrap == null) {
            return;
        }
        if (onlineStatsWrap instanceof Container container) {
            ConsoleUiStyle.disposeWrappingStatScroll(findOnlineStatsScroll(container));
            container.removeAll();
        }
        announceAndStatsBlock.remove(onlineStatsWrap);
        onlineStatsWrap = null;
    }

    private int configuredChannelCount() {
        if (channelCountOverride > 0) {
            return channelCountOverride;
        }
        try {
            if (Server.get().getWorld() != null && !Server.get().getWorld().getChannels().isEmpty()) {
                return Server.get().getWorld().getChannels().size();
            }
        } catch (Exception ignored) {
        }
        return ServerRuntimeConfig.intValue("channel.count", GameConstants.CHANNELS_PER_WORLD);
    }

    public void reloadChannelStatsFromConfig() {
        channelCountOverride = ServerRuntimeConfig.intValue("channel.count", GameConstants.CHANNELS_PER_WORLD);
        rebuildOnlineStatsSection(true);
    }

    private void rebuildOnlineStatsSection() {
        rebuildOnlineStatsSection(false);
    }

    private void rebuildOnlineStatsSection(boolean force) {
        if (announceAndStatsBlock == null) {
            return;
        }
        int count = configuredChannelCount();
        if (!force && channelValueLabels.length == count && onlineStatsWrap != null) {
            return;
        }
        disposeOnlineStatsWrap();
        channelValueLabels = new JLabel[count];
        for (int i = 0; i < count; i++) {
            channelValueLabels[i] = new JLabel("-");
        }
        JPanel statsBoxes = new JPanel(new WrapFlowLayout(FlowLayout.LEFT, 10, 6));
        statsBoxes.setOpaque(true);
        statsBoxes.setBackground(ConsoleUiStyle.PANEL_BG);
        statsBoxes.add(ConsoleUiStyle.borderedStatBox("總在線人數", totalOnlineLabel));
        for (int i = 0; i < channelValueLabels.length; i++) {
            statsBoxes.add(ConsoleUiStyle.borderedStatBox("頻道" + (i + 1), channelValueLabels[i]));
        }
        JScrollPane statsScroll = ConsoleUiStyle.wrappingStatScrollPane(statsBoxes, 1, 6);
        JComponent newWrap = ConsoleUiStyle.wrapSection(statsScroll, "在線人數", 2, 8, 2, 8);
        ConsoleUiStyle.stretchBoxWidth(newWrap);
        onlineStatsWrap = newWrap;
        announceAndStatsBlock.add(onlineStatsWrap);
        announceAndStatsBlock.revalidate();
        announceAndStatsBlock.repaint();
        if (!serverRunning) {
            totalOnlineLabel.setText("-");
            for (JLabel label : channelValueLabels) {
                label.setText("-");
            }
        }
    }

    private void initLogStreams() {
        originalOut = System.out;
        originalErr = System.err;
        logAppender.setMirrorStream(originalOut, originalErr);
        System.setOut(new PrintStream(new ConsoleLogStream(logAppender, false), true));
        System.setErr(new PrintStream(new ConsoleLogStream(logAppender, true), true));
    }

    private void installLogPopup(JScrollPane logScroll) {
        JPopupMenu popup = ConsoleUiStyle.createLogContextMenu(this::copyLogText, () -> logAppender.saveToFile(this), logAppender::clear);
        logArea.setComponentPopupMenu(popup);
        logScroll.setComponentPopupMenu(popup);
    }

    private void copyLogText() {
        String selected = logArea.getSelectedText();
        String text = selected != null && !selected.isEmpty() ? selected : logAppender.getText();
        if (text == null || text.isEmpty()) {
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }

    private void onServerToggle() {
        if (serverRunning) {
            stopServer();
            return;
        }
        if (!serverStarting) {
            startServer();
        }
    }

    private void startServer() {
        if (loginPortTaken()) { // 正式伺服器（或另一個控制檯）已在執行：不在 GUI 內再開第二台共用同一個資料庫
            logAppender.append("無法啟動：登入連接埠 " + net.swordie.ms.ServerConstants.LOGIN_PORT
                    + " 已被其他程式占用（正式伺服器已在執行）。控制檯的預覽、資料管理可照常使用；要由控制檯啟動請先停止正式伺服器。", true);
            return;
        }
        serverStarting = true;
        serverStartRequestedAt = System.currentTimeMillis();
        updateServerButton();
        FastStartup.setEnabled(fastStartToggle.isSelected());
        channelCountOverride = -1;
        rebuildOnlineStatsSection();
        logAppender.append("服務端啓動中：正在初始化 TMS282.3 服務端，請等待...", false);
        serverThread = new ConsoleServerThread(() -> SwingUtilities.invokeLater(() -> {
            long elapsed = System.currentTimeMillis() - serverStartRequestedAt;
            serverStarting = false;
            serverRunning = true;
            serverStartedAt = System.currentTimeMillis();
            updateServerButton();
            logAppender.appendSuccessLast("服務端已成功啓動，耗時 " + formatDuration(elapsed) + "。");
        }), () -> SwingUtilities.invokeLater(() -> {
            long elapsed = System.currentTimeMillis() - serverStartRequestedAt;
            serverStarting = false;
            serverRunning = false;
            updateServerButton();
            logAppender.append("服務端啓動失敗，已耗時 " + formatDuration(elapsed) + "，請查看上方錯誤日誌。", true);
        }));
        serverThread.start();
    }

    private static boolean loginPortTaken() {
        try (java.net.ServerSocket probe = new java.net.ServerSocket()) {
            probe.setReuseAddress(false);
            probe.bind(new java.net.InetSocketAddress("127.0.0.1", net.swordie.ms.ServerConstants.LOGIN_PORT));
            return false;
        } catch (java.io.IOException e) {
            return true;
        }
    }

    private void stopServer() {
        if (serverStopping) {
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "確定結束服務端？結束前將安全踢下在線玩家並保存數據庫。", "確認", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            serverStopping = true;
            updateServerButton();
            logAppender.append("正在結束服務端：正在安全保存在線玩家並斷開連接...", false);
            Thread shutdownThread = new Thread(this::shutdownServerAndExit, "TMS282.3-ConsoleShutdown");
            shutdownThread.setDaemon(false);
            shutdownThread.start();
        }
    }

    private void shutdownServerAndExit() {
        try {
            Server.get().shutdownOnlineClients();
            SwingUtilities.invokeLater(() -> {
                logAppender.appendSuccess("在線玩家已安全保存並下線，程序正在關閉。");
                exitNow();
            });
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> {
                serverStopping = false;
                updateServerButton();
                logAppender.append("結束服務端失敗: " + e.getMessage(), true);
                JOptionPane.showMessageDialog(this, "結束服務端失敗: " + e.getMessage(), "關閉服務端", JOptionPane.ERROR_MESSAGE);
            });
        }
    }

    private void updateServerButton() {
        if (serverStopping) {
            serverToggleButton.setText("正在結束…");
            serverToggleButton.setEnabled(false);
            return;
        }
        ConsoleUiStyle.styleServerButton(serverToggleButton, serverRunning, serverStarting);
    }

    private void refreshStatus() {
        refreshMemory();
        refreshUptime();
        refreshOnline();
        updatePacketStatusLabel();
    }

    private void refreshOnline() {
        if (channelValueLabels.length != configuredChannelCount()) {
            rebuildOnlineStatsSection();
        }
        if (!serverRunning) {
            if (!"-".equals(totalOnlineLabel.getText())) {
                totalOnlineLabel.setText("-");
                for (JLabel label : channelValueLabels) {
                    label.setText("-");
                }
            }
            return;
        }
        try {
            int total = Server.get().getChars().size();
            totalOnlineLabel.setText(String.valueOf(total));
            int[] counts = new int[channelValueLabels.length];
            if (Server.get().getWorld() != null) {
                for (Channel channel : Server.get().getWorld().getChannels()) {
                    int ch = channel.getChannelId();
                    if (ch >= 1 && ch <= counts.length) {
                        counts[ch - 1] = channel.getChars().size();
                    }
                }
            }
            for (int i = 0; i < channelValueLabels.length; i++) {
                channelValueLabels[i].setText(String.valueOf(counts[i]));
            }
        } catch (Exception e) {
            totalOnlineLabel.setText("-");
            for (JLabel label : channelValueLabels) {
                label.setText("-");
            }
        }
    }

    private void setupMemoryBar() {
        memoryBar.setStringPainted(true);
        memoryBar.setValue(0);
        memoryBar.setString("內存: -");
        memoryBar.setPreferredSize(new Dimension(200, 18));
        memoryBar.setMinimumSize(new Dimension(160, 18));
        memoryBar.setToolTipText("JVM 堆內存（已用 / 已提交）；啓服完成後會自動嘗試回收空閒堆");
    }

    private void refreshMemory() {
        StartupMemoryOptimizer.HeapSnapshot heap = StartupMemoryOptimizer.snapshot();
        int pct = heap.usagePercentOfCommitted();
        if (pct == lastMemoryBarPct) {
            return;
        }
        lastMemoryBarPct = pct;
        memoryBar.setValue(pct);
        memoryBar.setString(String.format("%.2f / %.2f GB", heap.usedGb(), heap.committedGb()));
        memoryBar.setToolTipText(String.format(
                "<html>堆已用: %.2f GB<br>堆已提交: %.2f GB<br>堆上限: %.2f GB<br>佔用率(已用/已提交): %d%%",
                heap.usedGb(), heap.committedGb(), heap.maxGb(), pct));
    }

    private void refreshUptime() {
        if (!serverRunning || serverStartedAt <= 0L) {
            uptimeLabel.setText("運行時間: -");
            return;
        }
        Duration d = Duration.ofMillis(System.currentTimeMillis() - serverStartedAt);
        long seconds = d.toSeconds();
        long days = seconds / 86400;
        seconds %= 86400;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;
        uptimeLabel.setText(String.format("運行時間: %02d天%02d:%02d:%02d", days, hours, minutes, seconds));
    }

    private static String formatDuration(long millis) {
        long safeMillis = Math.max(0L, millis);
        long minutes = safeMillis / 60_000L;
        long seconds = (safeMillis % 60_000L) / 1_000L;
        long ms = safeMillis % 1_000L;
        if (minutes > 0) {
            return String.format("%d分%02d秒%03d毫秒", minutes, seconds, ms);
        }
        return String.format("%d秒%03d毫秒", seconds, ms);
    }

    private void sendAnnouncement() {
        String text = announceField.getText();
        if (text == null || text.isBlank()) {
            JOptionPane.showMessageDialog(this, "請輸入公告內容。", "系統公告", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int count = 0;
        for (Char chr : Server.get().getChars().values()) {
            if (chr != null) {
                chr.chatMessage(text.trim());
                count++;
            }
        }
        System.out.println("[Console] 已發送公告給 " + count + " 名在線玩家: " + text.trim());
        announceField.setText("");
    }

    private void showNotImplemented(String module) {
        JOptionPane.showMessageDialog(this, module + " 已保留入口，後續按 TMS282.3 配置結構繼續適配。", module, JOptionPane.INFORMATION_MESSAGE);
    }

    private void onClose() {
        int ok = JOptionPane.showConfirmDialog(this, "確認關閉控制檯？", "關閉控制檯", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) {
            return;
        }
        if (serverRunning) {
            stopServer();
            return;
        }
        exitNow();
    }

    private void exitNow() {
        disposeOnlineStatsWrap();
        if (packetLogPane != null) {
            packetLogPane.shutdown();
        }
        if (clientReportPane != null) {
            clientReportPane.shutdown();
        }
        if (packetTracePane != null) {
            packetTracePane.shutdown();
        }
        restoreStreams();
        dispose();
        System.exit(0);
    }

    private void restoreStreams() {
        uiTimer.stop();
        if (originalOut != null) {
            System.setOut(originalOut);
        }
        if (originalErr != null) {
            System.setErr(originalErr);
        }
    }
}
