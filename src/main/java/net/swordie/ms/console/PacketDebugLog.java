package net.swordie.ms.console;

import net.swordie.ms.ServerConfig;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** 封包調試輸出：調用線程上快速格式化後入隊，由 PacketLogAppender 異步刷新 GUI。 */
public final class PacketDebugLog {
    private static final byte[] EMPTY = new byte[0];
    private static final int MAX_LINES_PER_SECOND = 800;

    private static final Set<String> HIGH_FREQUENCY_SEND = Set.of(
            "MOVE_MONSTER", "MOVE_MONSTER_RESPONSE", "UPDATE_STATS", "NPC_ACTION",
            "GIVE_BUFF", "CANCEL_BUFF", "SHOW_STATUS_INFO", "INGAME_PING", "PONG");

    private static final Set<String> HIGH_FREQUENCY_RECV = Set.of(
            "MOVE_LIFE", "MOVE_PLAYER", "MOVE_PET", "PONG", "INGAME_PONG", "HEAL_OVER_TIME");

    private static final AtomicInteger linesThisSecond = new AtomicInteger();
    private static volatile long secondWindowStart = System.currentTimeMillis();

    private PacketDebugLog() {
    }

    public static void logRecv(String ip, InHeader header, short opcode, byte[] remain, String charName) {
        if (!ServerConfig.PACKET_LOG) {
            return;
        }
        String opcodeName = header != null ? header.name() : "UNKNOWN";
        if (shouldDropRecv(opcodeName)) {
            return;
        }
        if (!acquireLineBudget()) {
            return;
        }
        enqueue(PacketLogFormat.formatRecv(ip, opcodeName, opcode, copyPayload(remain), charName));
    }

    public static void logSend(String ip, OutHeader header, int opcode, byte[] msg, String charName) {
        if (!ServerConfig.PACKET_LOG) {
            return;
        }
        String opcodeName = header != null ? header.name() : "UNKNOWN";
        if (shouldDropSend(opcodeName)) {
            return;
        }
        if (!acquireLineBudget()) {
            return;
        }
        enqueue(PacketLogFormat.formatSend(ip, opcodeName, opcode, copyPayload(msg), charName));
    }

    public static void logUnknownRecv(String ip, short opcode, byte[] data, String who) {
        if (!ServerConfig.PACKET_LOG) {
            return;
        }
        enqueue(PacketLogFormat.formatUnknownRecv(ip, opcode, copyPayload(data), who));
    }

    public static void logUnknownSend(String ip, int opcode, byte[] data, String who) {
        if (!ServerConfig.PACKET_LOG) {
            return;
        }
        enqueue(PacketLogFormat.formatUnknownSend(ip, opcode, copyPayload(data), who));
    }

    private static boolean shouldDropRecv(String opcodeName) {
        return PacketLogFilter.isIgnoreHighFrequencySendEnabled()
                && PacketLogFilter.isSimpleLogEnabled()
                && HIGH_FREQUENCY_RECV.contains(opcodeName);
    }

    private static boolean shouldDropSend(String opcodeName) {
        return PacketLogFilter.isIgnoreHighFrequencySendEnabled()
                && PacketLogFilter.isSimpleLogEnabled()
                && HIGH_FREQUENCY_SEND.contains(opcodeName);
    }

    private static void enqueue(String line) {
        PacketLogAppender appender = PacketLogAppender.getInstance();
        if (appender != null) {
            appender.enqueue(line);
        }
    }

    private static byte[] copyPayload(byte[] source) {
        if (source == null || source.length == 0) {
            return EMPTY;
        }
        int max = PacketLogFormat.guiHexMaxBytes();
        int len = Math.min(source.length, Math.max(0, max));
        return Arrays.copyOf(source, len);
    }

    private static boolean acquireLineBudget() {
        long now = System.currentTimeMillis();
        if (now - secondWindowStart >= 1000L) {
            secondWindowStart = now;
            linesThisSecond.set(0);
        }
        return linesThisSecond.incrementAndGet() <= MAX_LINES_PER_SECOND;
    }
}
