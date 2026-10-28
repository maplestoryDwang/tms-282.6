package net.swordie.ms.connection.packet;

import java.util.OptionalInt;

/**
 * CP01A3 USER_MAP_TRANSFER_REQUEST [verified] (265 0x19B; sender 5EE2FF0; ingame-cp-map-2823.md 3.4): the teleport
 * rock list edit, {@code u8 type, u8 itemType, [u32 fieldId]} - the sequence 265 MigrationHandler reads (type 0
 * DeleteListRecv carries the field id; type 1 RegisterListRecv registers the current field and has none).
 */
public record Tms282MapTransferRequest(int type, int itemType, OptionalInt fieldId) {
    public static final int OPCODE = 0x01A3;

    /** Whole plaintext packet, read exactly. */
    public static Tms282MapTransferRequest parse(byte[] packet) {
        Tms282FieldRequestReader r = Tms282FieldRequestReader.open(packet, OPCODE, "CP01A3");
        int type = r.u8();
        int itemType = r.u8();
        OptionalInt fieldId = r.remaining() >= 4 ? OptionalInt.of(r.i32()) : OptionalInt.empty();
        r.end();
        return new Tms282MapTransferRequest(type, itemType, fieldId);
    }
}
