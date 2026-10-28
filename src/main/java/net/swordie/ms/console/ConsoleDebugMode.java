package net.swordie.ms.console;

import net.swordie.ms.ServerConfig;

import java.util.Properties;

/** 控制檯調試模式與封包詳情開關，狀態寫入 config/console.properties。 */
public final class ConsoleDebugMode {
    public static final String KEY_DEBUG_MODE = "console.debug.mode.enabled";
    public static final String KEY_PACKET_DETAIL = "console.packet.detail.enabled";

    private static volatile boolean active;
    private static volatile boolean packetDetailEnabled;

    private ConsoleDebugMode() {
    }

    public static void load() {
        Properties p = ConsoleProperties.load();
        active = ConsoleProperties.parseBool(p.getProperty(KEY_DEBUG_MODE), false);
        packetDetailEnabled = ConsoleProperties.parseBool(p.getProperty(KEY_PACKET_DETAIL), true);
        applyPacketDetail();
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isPacketDetailEnabled() {
        return packetDetailEnabled;
    }

    public static void setActive(boolean on) {
        active = on;
        persistFlags();
        System.out.println("[Console] 調試模式: " + (on ? "開啓" : "關閉")
                + "；封包詳情=" + (packetDetailEnabled ? "開" : "關"));
    }

    public static void setPacketDetail(boolean on) {
        packetDetailEnabled = on;
        applyPacketDetail();
        persistFlags();
        System.out.println("[Console] 封包詳情: " + (on ? "開啓（輸出至封包日誌 Tab）" : "關閉"));
    }

    private static void applyPacketDetail() {
        ServerConfig.PACKET_LOG = packetDetailEnabled;
    }

    private static void persistFlags() {
        try {
            Properties p = ConsoleProperties.load();
            p.setProperty(KEY_DEBUG_MODE, Boolean.toString(active));
            p.setProperty(KEY_PACKET_DETAIL, Boolean.toString(packetDetailEnabled));
            ConsoleProperties.save(p);
        } catch (Exception e) {
            System.err.println("[Console] 保存調試配置失敗: " + e.getMessage());
        }
    }

    public static void mirrorLine(String tag, String message) {
        if (!active || message == null || message.isEmpty()) {
            return;
        }
        System.out.println("[" + tag + "] " + message);
    }

    public static void mirrorUnknownRecv(String who, short opcode) {
        if (!active) {
            return;
        }
        mirrorLine("未知Recv", who + " 未處理 " + opcode);
    }

    public static void mirrorUnknownSend(String who, int opcode) {
        if (!active) {
            return;
        }
        mirrorLine("未知Send", who + " 未處理 " + opcode);
    }
}
