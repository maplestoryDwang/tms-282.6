package net.swordie.ms.connection.packet;

import java.util.Arrays;
import java.util.Objects;

/**
 * The two TMS 282.3 HEXA matrix (6th job) requests of the HEXA window, both in the 265 layout (i32 type, then the
 * type's fields), so the body is handed to the 265 handler unchanged:
 * <ul>
 *     <li>CP0490 HEXA_MATRIX_OPREATION [推定; 265 0x464]: 17 senders in the HEXA window 2E7E17A..2EA6F03 (the code
 *     that uses SID_HEXASKILL_CORE_* / SID_HEXASTAT_*), each EncodeBuffer(4) type then the 265 fields: 0 activate
 *     (coreId), 1 enforce (coreId, levels, costs), 2 stat (coreId, 3 stats), 3 (coreId, Encode8 weight),
 *     5 (coreId, preset), 7 (Encode8 cost, i32 n, ...), 8 (str);</li>
 *     <li>CP0491 HEXA_ERDA_CONVERSION [推定; 265 0x465]: 284F56D type 0 (open), 284F670 / 2851FDA type 1
 *     (itemId, erda) - next to the LP0277 handler 284F710.</li>
 * </ul>
 * Research: research/tms2823/packet-audit/hexa-vmatrix-2823.md.
 */
public record Tms282HexaMatrixRequest(int opcode, int type, byte[] body) {
    public static final int OPERATION = 0x0490;
    public static final int ERDA_CONVERSION = 0x0491;
    static final int MAX_OPERATION_TYPE = 8;
    static final int MAX_ERDA_TYPE = 1;

    public Tms282HexaMatrixRequest {
        Objects.requireNonNull(body, "body");
        body = body.clone();
    }

    @Override
    public byte[] body() {
        return body.clone();
    }

    public static Tms282HexaMatrixRequest parseOperation(byte[] packet) {
        return parse(packet, OPERATION, MAX_OPERATION_TYPE, "HEXA matrix operation");
    }

    public static Tms282HexaMatrixRequest parseErdaConversion(byte[] packet) {
        return parse(packet, ERDA_CONVERSION, MAX_ERDA_TYPE, "HEXA erda conversion");
    }

    private static Tms282HexaMatrixRequest parse(byte[] packet, int opcode, int maxType, String name) {
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, opcode, name);
        int type = in.i32();
        if (type < 0 || type > maxType) {
            throw new IllegalArgumentException(name + " type " + type + " out of 0.." + maxType);
        }
        return new Tms282HexaMatrixRequest(opcode, type, Arrays.copyOfRange(packet, 2, packet.length));
    }

    /** The 265 body (no opcode): the same bytes, type included. */
    public byte[] toGms265Body() {
        return body.clone();
    }

    @Override
    public String toString() {
        return String.format("CP%04X type=%d len=%d", opcode, type, body.length);
    }
}
