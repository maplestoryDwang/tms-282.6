package net.swordie.ms.connection.packet;

import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Set;

/**
 * CP019E / CP019F / CP01A0 USER_PORTAL_SCRIPT_REQUEST (_2, _3) [verified] (265 0x196..0x198; senders 57B6100,
 * 57BA9C0, 597E420, 1FF7260; ingame-cp-map-2823.md 3.4): {@code u8 fieldKey, str portalName, i16 x, i16 y}. 265
 * reads {@code u8, str} only; the position is new in 282 (where the user stood when it entered the portal).
 */
public record Tms282PortalScriptRequest(int opcode, int fieldKey, String portalName, short x, short y) {
    public static final int OPCODE = 0x019E;
    public static final int OPCODE_2 = 0x019F;
    public static final int OPCODE_3 = 0x01A0;
    public static final Set<Integer> OPCODES = Set.of(OPCODE, OPCODE_2, OPCODE_3);

    public Tms282PortalScriptRequest {
        Objects.requireNonNull(portalName, "portalName");
        if (!OPCODES.contains(opcode)) {
            throw new IllegalArgumentException(String.format("CP%04X is not a portal script request", opcode));
        }
    }

    /** Whole plaintext packet (CP019E, CP019F or CP01A0), read exactly. */
    public static Tms282PortalScriptRequest parse(byte[] packet, Charset charset) {
        Objects.requireNonNull(packet, "packet");
        int op = packet.length < 2 ? -1 : (packet[0] & 0xFF) | (packet[1] & 0xFF) << 8;
        Tms282FieldRequestReader r = Tms282FieldRequestReader.open(packet, OPCODES.contains(op) ? op : OPCODE,
                "portal script request");
        int fieldKey = r.u8();
        String portal = r.str(charset);
        short x = r.i16();
        short y = r.i16();
        r.end();
        return new Tms282PortalScriptRequest(op, fieldKey, portal, x, y);
    }
}
