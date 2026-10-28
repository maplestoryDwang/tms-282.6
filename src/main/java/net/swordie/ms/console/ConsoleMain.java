package net.swordie.ms.console;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.net.ServerSocket;

public final class ConsoleMain {
    private ConsoleMain() {
    }

    public static void main(String[] args) {
        if (!trySingleInstance()) {
            JOptionPane.showMessageDialog(null, "TMS282.3 管理控制檯已在運行中。", "TMS282.3", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }
        UIManager.put("defaultFont", new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        SwingUtilities.invokeLater(() -> {
            ConsoleFrame frame = new ConsoleFrame();
            ConsoleIconLoader.applyFrameIcon(frame);
            TaiwanText.install(frame); // 介面文字統一台灣繁體
            frame.setVisible(true);
        });
    }

    private static boolean trySingleInstance() {
        try {
            ServerSocket socket = new ServerSocket(26265, 1, java.net.InetAddress.getByAddress(new byte[]{127, 0, 0, 1}));
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
