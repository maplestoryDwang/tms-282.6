package net.swordie.ms.console.config;

import net.swordie.ms.console.ConsoleFrame;
import net.swordie.ms.connection.hikariCP.DatabaseManager;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ConfigPanel extends JDialog {
    private final ServerConfigGroup serverGroup = new ServerConfigGroup();
    private final JobCreationConfigGroup jobCreationGroup = new JobCreationConfigGroup();
    private final JobConfigGroup skillGroup = new JobConfigGroup();
    private final BossConfigGroup bossGroup = new BossConfigGroup();
    private final RateConfigGroup rateGroup = new RateConfigGroup();
    private final JButton applyButton = new JButton("應用基礎配置");

    public ConfigPanel(Frame owner) {
        super(owner, "功能配置", false);
        buildUi(owner);
        setSize(1120, 720);
        setMinimumSize(new Dimension(980, 620));
        setLocationRelativeTo(owner);
    }

    public static void open(Frame owner) {
        new ConfigPanel(owner).setVisible(true);
    }

    private void buildUi(Frame owner) {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("服務端基礎配置", serverGroup.buildPanel(() -> applyButton.setEnabled(true)));
        tabs.addTab("倍率設定", rateGroup.buildPanel());
        tabs.addTab("職業管理", jobCreationGroup.buildPanel());
        tabs.addTab("技能管理", skillGroup.buildPanel());
        tabs.addTab("首領BOSS", bossGroup.buildPanel());

        applyButton.setEnabled(false);
        applyButton.addActionListener(e -> applyServerConfig(owner));
        JButton close = new JButton("關閉");
        close.addActionListener(e -> dispose());

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(close);
        south.add(applyButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(tabs, BorderLayout.CENTER);
        getContentPane().add(south, BorderLayout.SOUTH);
    }

    private void applyServerConfig(Frame owner) {
        try {
            ServerRuntimeConfig.save(serverGroup.values());
            ServerRuntimeConfig.reload();
            ServerRuntimeConfig.applyToRuntime();
            boolean databaseSettingsApplied = DatabaseManager.reloadRuntimeSettingsIfIdle();
            applyButton.setEnabled(false);
            if (owner instanceof ConsoleFrame frame) {
                frame.reloadChannelStatsFromConfig();
            }
            JOptionPane.showMessageDialog(this,
                    "基礎配置已保存到 config/server.properties。\n"
                            + "· 服務器名稱：已同步到當前 JVM。\n"
                            + "· 服務端頻道數：主界面在線人數面板已按新配置重建；實際頻道對象需重啓服務端。\n"
                            + "· 登錄端口 / API端口：已寫入當前 JVM；如果端口已綁定，需重啓服務端後重新監聽。\n"
                            + "· 數據庫配置：" + (databaseSettingsApplied ? "連接池尚未初始化，已同步到當前 JVM。" : "連接池已初始化，需重啓服務端後生效。") + "\n"
                            + "· 本機限定版：服務端 IP 與數據庫主機固定為 127.0.0.1。",
                    "保存成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "保存失敗: " + ex.getMessage(), "錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static final class ServerConfigGroup {
        private final Map<String, JTextField> fields = new LinkedHashMap<>();

        JComponent buildPanel(Runnable dirty) {
            JPanel form = ConfigUi.formPanel();
            GridBagConstraints c = ConfigUi.gbc();
            Map<String, String> values = ServerRuntimeConfig.defaultFormValues();
            add(form, c, "服務器名稱", "server.name", values, dirty);
            add(form, c, "服務端IP", "server.host", values, dirty);
            add(form, c, "登錄端口", "login.port", values, dirty);
            add(form, c, "API端口", "api.port", values, dirty);
            add(form, c, "服務端頻道數", "channel.count", values, dirty);
            add(form, c, "數據庫主機", "db.host", values, dirty);
            add(form, c, "數據庫端口", "db.port", values, dirty);
            add(form, c, "數據庫庫名", "db.name", values, dirty);
            add(form, c, "數據庫賬號", "db.user", values, dirty);
            add(form, c, "數據庫密碼", "db.password", values, dirty);
            c.gridx = 0;
            c.gridy++;
            c.gridwidth = 2;
            c.weightx = 1;
            JLabel tip = new JLabel("說明：網絡端口、數據庫連接池和實際頻道對象在服務端啓動時創建，保存後需重新啓動服務端。技能參數在右側單獨保存，可實時生效。");
            tip.setForeground(Color.DARK_GRAY);
            form.add(tip, c);
            JPanel wrap = new JPanel(new BorderLayout());
            wrap.add(form, BorderLayout.NORTH);
            return ConfigUi.verticalScroll(wrap);
        }

        private void add(JPanel form, GridBagConstraints c, String label, String key, Map<String, String> values, Runnable dirty) {
            JTextField field = ConfigUi.addText(form, c, label, values.get(key), dirty);
            if (key.equals("server.host") || key.equals("db.host")) {
                field.setEditable(false);
                field.setToolTipText("本機限定版固定使用 127.0.0.1");
            }
            fields.put(key, field);
        }

        Map<String, String> values() {
            Map<String, String> map = new LinkedHashMap<>();
            for (Map.Entry<String, JTextField> entry : fields.entrySet()) {
                map.put(entry.getKey(), entry.getValue().getText());
            }
            return map;
        }
    }
}
