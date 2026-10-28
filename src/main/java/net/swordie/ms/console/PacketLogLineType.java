package net.swordie.ms.console;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** 識別 GUI 封包日誌行並解析封包類型（用於封包日誌 Tab 動態過濾）。 */
public final class PacketLogLineType {
    private static final Set<String> KNOWN_HIGH_FREQ = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "MOVE_LIFE", "MOVE_PLAYER", "PING", "PONG", "NPC_ACTION", "UPDATE_STATS",
            "GIVE_BUFF", "CANCEL_BUFF", "SHOW_STATUS_INFO", "MOVE_PET", "MOVE_MONSTER",
            "MOVE_MONSTER_RESPONSE", "LP_MobCtrlAck", "CP_MobMove", "MOBMOVE", "MOB_CTRL"
    )));

    private PacketLogLineType() {
    }

    public static boolean isPacketLine(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        String line = firstLine(s);
        return line.contains("[" + PacketLogFormat.DIR_RECV + ":")
                || line.contains("[" + PacketLogFormat.DIR_SEND + ":")
                || line.contains("[未知客戶端:")
                || line.contains("[未知服務端:")
                || line.contains("[RECV")
                || line.contains("[SEND")
                || line.contains("[In ")
                || line.contains("[Out");
    }

    /** GUI 封包行必須以 [HH:mm:ss] 開頭，避免無時間戳的鏡像/遺留日誌重複入隊。 */
    public static boolean hasGuiTimestamp(String line) {
        if (line == null || line.length() < 10) {
            return false;
        }
        String s = firstLine(line);
        if (s.charAt(0) != '[') {
            return false;
        }
        int close = s.indexOf(']', 1);
        if (close != 9) {
            return false;
        }
        for (int i = 1; i < 9; i++) {
            char c = s.charAt(i);
            if (i == 3 || i == 6) {
                if (c != ':') {
                    return false;
                }
            } else if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isRecvLine(String line) {
        if (line == null) {
            return false;
        }
        return line.contains("[" + PacketLogFormat.DIR_RECV + ":")
                || line.contains("[未知客戶端:")
                || line.contains("[RECV")
                || line.contains("[In ");
    }

    public static boolean isUnknownLine(String line) {
        if (line == null) {
            return false;
        }
        return line.contains("[未知客戶端:")
                || line.contains("[未知服務端:")
                || line.contains("未處理");
    }

    public static boolean isHighFrequencyOpcode(String opcode) {
        return opcode != null && KNOWN_HIGH_FREQ.contains(opcode);
    }

    public static String parseType(String line) {
        if (line == null || line.isEmpty()) {
            return "其他";
        }
        String s = firstLine(line);
        int recvIdx = s.indexOf('[' + PacketLogFormat.DIR_RECV + ':');
        if (recvIdx >= 0) {
            return parseBracketType(s, recvIdx, PacketLogFormat.DIR_RECV);
        }
        int sendIdx = s.indexOf('[' + PacketLogFormat.DIR_SEND + ':');
        if (sendIdx >= 0) {
            return parseBracketType(s, sendIdx, PacketLogFormat.DIR_SEND);
        }
        recvIdx = s.indexOf("[未知客戶端:");
        if (recvIdx >= 0) {
            return parseBracketType(s, recvIdx, "未知客戶端");
        }
        sendIdx = s.indexOf("[未知服務端:");
        if (sendIdx >= 0) {
            return parseBracketType(s, sendIdx, "未知服務端");
        }
        recvIdx = s.indexOf("[RECV");
        if (recvIdx >= 0) {
            return parseBracketType(s, recvIdx, PacketLogFormat.DIR_RECV);
        }
        sendIdx = s.indexOf("[SEND");
        if (sendIdx >= 0) {
            return parseBracketType(s, sendIdx, PacketLogFormat.DIR_SEND);
        }
        recvIdx = s.indexOf("[In ");
        if (recvIdx >= 0) {
            return PacketLogFormat.DIR_RECV + " · 其他";
        }
        sendIdx = s.indexOf("[Out");
        if (sendIdx >= 0) {
            return PacketLogFormat.DIR_SEND + " · 其他";
        }
        return "其他";
    }

    public static String parseOpcode(String line) {
        String type = parseType(line);
        int dot = type.indexOf('·');
        if (dot >= 0 && dot + 1 < type.length()) {
            return type.substring(dot + 1).trim();
        }
        return type;
    }

    private static String parseBracketType(String line, int bracketIdx, String direction) {
        int close = line.indexOf(']', bracketIdx);
        if (close <= bracketIdx) {
            return direction;
        }
        String inner = line.substring(bracketIdx + 1, close);
        int colon = inner.indexOf(':');
        if (colon >= 0 && colon + 1 < inner.length()) {
            return direction + " · " + inner.substring(colon + 1).trim();
        }
        return direction;
    }

    private static String firstLine(String raw) {
        int idx = raw.indexOf('\n');
        return (idx >= 0 ? raw.substring(0, idx) : raw).trim();
    }
}
