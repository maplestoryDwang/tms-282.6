package net.swordie.ms.console.datamanage;

import javax.swing.*;
import java.awt.*;

public final class DataManagePanel extends JDialog {
    private DataManagePanel(Frame owner) {
        super(owner, "數據管理", false);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("玩家角色", new PlayerManagePane());
        tabs.addTab("怪物爆率", new DropManagePane());
        tabs.addTab("商店管理", new ShopManagePane());
        tabs.addTab("角色預覽", new net.swordie.ms.console.preview.CharacterPreviewPane());
        tabs.addTab("首領預覽", new net.swordie.ms.console.preview.BossPreviewPane());
        getContentPane().add(tabs, BorderLayout.CENTER);
        setSize(1024, 720);
        setLocationRelativeTo(owner);
    }

    public static void open(Frame owner) {
        try {
            ConsoleDatabaseService.prepare();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(owner,
                    "數據庫連接失敗，數據管理功能不可用。\n" + ConsoleDatabaseService.formatSqlError(e),
                    "數據管理", JOptionPane.ERROR_MESSAGE);
            return;
        }
        DataManagePanel panel = new DataManagePanel(owner);
        panel.setVisible(true);
    }

    private static JComponent placeholder(String text) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setForeground(Color.DARK_GRAY);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }
}