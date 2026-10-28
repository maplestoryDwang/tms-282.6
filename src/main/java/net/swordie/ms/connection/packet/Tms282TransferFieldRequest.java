package net.swordie.ms.connection.packet;

import java.nio.charset.Charset;
import java.util.HexFormat;
import java.util.Objects;

/**
 * USER_TRANSFER_FIELD_REQUEST (portal walk / revive field change) - the 282.3 opcode is NOT KNOWN. CP 0xEE..0xFA
 * have no static COutPacket construction site; the request is built inside the Themida VM (presumed 0xFA from the
 * +2 shift of 265 0xF8; ingame-cp-map-2823.md 3.4). Its handler is registered only when
 * {@code tms282.transferFieldOp} is configured, after a capture shows the opcode.
 * <p>
 * The parser follows the 265 layout (MigrationHandler.handleUserTransferFieldRequest):
 * <pre>
 * u8  unknown         265 skips it
 * u8  fieldKey
 * i32 targetField     -1 for a portal walk, the return field when reviving
 * str portalName      "" when reviving
 * ... rest            kept raw ({@link #tail}); 265 ignores whatever follows the name
 * </pre>
 * Possible 282 differences to check against the first capture (only the head is parsed strictly):
 * <ol>
 * <li>an added u32 before or after fieldKey (282 added one before skillId in CP04B4 and in the attack headers);</li>
 * <li>a u32 tick first (many 282 CPs start with the update time);</li>
 * <li>the user position as i16 x, i16 y after the name (CP019E/01A1 carry it), or more flags (premium, chase);</li>
 * <li>a trailing u32 appended by the socket layer (346A070, the CPs marked "補尾端 u32");</li>
 * <li>the portal name in the client charset rather than ASCII - it is decoded with the configured name charset.</li>
 * </ol>
 * Enabling: set the opcode, walk through one plain portal, compare {@link #describe()} in the server log with the
 * portal that was used (name and field), then keep or adjust this layout.
 */
public record Tms282TransferFieldRequest(int opcode, int unknown, int fieldKey, int targetField, String portalName,
                                         byte[] tail) {
    public Tms282TransferFieldRequest {
        Objects.requireNonNull(portalName, "portalName");
        tail = tail.clone();
    }

    @Override
    public byte[] tail() {
        return tail.clone();
    }

    public boolean hasPortal() {
        return !portalName.isEmpty();
    }

    /** Whole plaintext packet with the configured opcode; the head must be complete, the rest is kept raw. */
    public static Tms282TransferFieldRequest parse(byte[] packet, int opcode, Charset charset) {
        Tms282FieldRequestReader r = Tms282FieldRequestReader.open(packet, opcode,
                String.format("CP%04X transfer field", opcode));
        int unknown = r.u8();
        int fieldKey = r.u8();
        int target = r.i32();
        String portal = r.str(charset);
        byte[] tail = r.rest();
        return new Tms282TransferFieldRequest(opcode, unknown, fieldKey, target, portal, tail);
    }

    /** One log line: every parsed field and the raw tail. */
    public String describe() {
        return String.format("CP%04X transfer field: u8=%d fieldKey=%d target=%d portal=\"%s\" tail[%d]=%s", opcode,
                unknown, fieldKey, targetField, portalName, tail.length, HexFormat.of().formatHex(tail));
    }
}
