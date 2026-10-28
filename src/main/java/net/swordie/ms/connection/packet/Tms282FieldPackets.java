package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.util.Position;

import java.nio.charset.Charset;
import java.util.Objects;

import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.checkByte;
import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.str;

/**
 * TMS 282.3 field-transfer replies of the local user.
 * <ul>
 * <li>LP04CC TELEPORT [verified] (CUserLocal::OnPacket 5788060, inline at 578810E): the 265
 * {@code FieldPacket.teleport} fields {@code u8 exclRequest, u8 callingType, i32 characterId, i16 x, i16 y}, then
 * 282 reads {@code u8, i32} more (meaning unknown; 0, 0).</li>
 * <li>LP02F4 TRANSFER_FIELD_REQ_IGNORED [verified] (3753970, outside CField's jump table): {@code u8 reason}; the
 * handler first releases the request lock (5E04DA0, like an exclRequest) and then shows a notice by reason
 * (jump table 3753B14): 1 SP[0xB1C], 2 SP[0x1F80], 4 SP[0x1F81] (other dialog), 6 SP[0x1F8C], 7 SP[0x1F8D],
 * 8 SP[0x1F8E], 9 SP[0x1F8F]; 3 and 5 read one more {@code str} and show it; 0 and &gt; 9 show nothing.</li>
 * </ul>
 */
public final class Tms282FieldPackets {
    public static final int TELEPORT = 0x04CC;
    public static final int TRANSFER_FIELD_REQ_IGNORED = 0x02F4;
    public static final int MAP_TRANSFER_RESULT = 0x00B0;
    /** Empty teleport-rock slot (265 HyperTPRock). */
    public static final int EMPTY_ROCK_FIELD = 999999999;
    /** 265 FieldPacket.teleport nUserCallingType. */
    public static final int CALLING_TYPE_SERVER = 6;
    /** LP02F4 reason that reads a server message (3753A05). */
    public static final int REASON_MESSAGE = 3;

    private Tms282FieldPackets() {
    }

    /** LP02F4 reasons with a fixed client string. */
    public enum Reason {
        /** 0: no notice, only the request lock is released. */
        SILENT(0),
        /** SP[0xB1C] "現在關閉入口。" */
        PORTAL_CLOSED(1),
        /** SP[0x1F80] "無法移動到該區。" */
        CANNOT_MOVE(2),
        /** SP[0x1F81] "無法進行瞬間移動的地區。" (5BABF80 dialog) */
        CANNOT_TELEPORT(4),
        /** SP[0x1F8C] "只有隊員能入場的地圖。" */
        PARTY_ONLY(6);

        private final int value;

        Reason(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    /** LP04CC, as 265 FieldPacket.teleport (exclRequest 1, calling type 6) plus the two 282 fields. */
    public static OutPacket teleport(int characterId, Position position) {
        return teleport(true, CALLING_TYPE_SERVER, characterId, position);
    }

    public static OutPacket teleport(boolean exclRequest, int callingType, int characterId, Position position) {
        Objects.requireNonNull(position, "position");
        checkByte(callingType, "Calling type");
        OutPacket out = Tms282LoginPacketSupport.packet(TELEPORT);
        out.encodeByte(exclRequest);
        out.encodeByte(callingType);
        out.encodeInt((long) characterId);
        out.encodePosition(position);
        out.encodeByte(0);                                 // 282 new
        out.encodeInt(0L);                                 // 282 new
        return out;
    }

    /**
     * LP00B0 MAP_TRANSFER_RESULT [opcode presumed: 265 177 with the CWvsContext -1 shift; reads verified statically]
     * (handler 5F2EA60): {@code u8 type, u8 itemType}; types 2 (DeleteListSend) and 3 (RegisterListSend) then read
     * the rock list whose length 831BD0 takes from the item type - 5 (itemType 1), 10 (2), 13 (3), none otherwise -
     * where 265 always wrote its 13 hyper-rock fields. The list is cut or padded ({@link #EMPTY_ROCK_FIELD}) to it.
     */
    public static OutPacket mapTransferResult(int type, int itemType, int[] fields) {
        checkByte(type, "Map transfer type");
        checkByte(itemType, "Map transfer item type");
        OutPacket out = Tms282LoginPacketSupport.packet(MAP_TRANSFER_RESULT);
        out.encodeByte(type);
        out.encodeByte(itemType);
        if (type == 2 || type == 3) {
            int count = rockListLength(itemType);
            for (int i = 0; i < count; i++) {
                out.encodeInt(fields != null && i < fields.length ? (long) fields[i] : EMPTY_ROCK_FIELD);
            }
        }
        return out;
    }

    /** 831BD0: rock list length per item type. */
    public static int rockListLength(int itemType) {
        return switch (itemType) {
            case 1 -> 5;
            case 2 -> 10;
            case 3 -> 13;
            default -> 0;
        };
    }

    /** LP02F4 {@code u8 reason}. */
    public static OutPacket transferFieldReqIgnored(Reason reason) {
        OutPacket out = Tms282LoginPacketSupport.packet(TRANSFER_FIELD_REQ_IGNORED);
        out.encodeByte(Objects.requireNonNull(reason, "reason").value());
        return out;
    }

    /** LP02F4 reason 3 with the server's own notice ({@code str}). */
    public static OutPacket transferFieldReqIgnored(String message, Charset charset) {
        Objects.requireNonNull(message, "message");
        OutPacket out = Tms282LoginPacketSupport.packet(TRANSFER_FIELD_REQ_IGNORED);
        out.encodeByte(REASON_MESSAGE);
        str(out, message, charset);
        return out;
    }
}
