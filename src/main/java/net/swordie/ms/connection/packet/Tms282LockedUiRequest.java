package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;

/**
 * CP02C5 (TMS 282.3; 265 USER_CHARACTER_INFO_SELF_REQUEST): self character information request
 * sent by 5FA7FC0(ctx, type) from about a dozen UI callers
 * (party window "partyName" 246B900, "JobChanged" 5EEA1A0, "MenuUp"/"MenuDown" 5DD5D70, 3701CE0 after CP0132,
 * 2DBEFD0 with type 3 and 5; type 0 elsewhere). Type 4 is handled locally and never sent. Before sending, 5E04C30
 * checks the request lock with a 500 ms throttle; after 346A070 the sender calls 5E04DA0(ctx, 1), so the client
 * stays locked until a reply. LP00CA (5FA81C0) both unlocks and opens the information window, forwarding
 * the requested type/tab from its four-byte field to 5FA9970. LP009D alone leaves the window closed.
 * <pre>
 * u32 tick    5B4FC50 (update time)
 * u32 type    EncodeBuffer(&amp;type, 4)
 * </pre>
 * Samples: formal-channel-1790176449461-3 recv-135/253/255-02c5 (type 0).
 */
public record Tms282LockedUiRequest(int tick, int type) {
    public static final int OPCODE = 0x02C5;

    /** LP00CA / 5FA81C0: u8, i32, requested tab (Buf4), u8; same body as 265, profile-owned opcode. */
    public OutPacket reply() {
        OutPacket out = Tms282LoginPacketSupport.packet(0x00CA);
        out.encodeByte(0);
        out.encodeInt(0);
        out.encodeInt(type);
        out.encodeByte(1);
        return out;
    }

    public static Tms282LockedUiRequest parse(byte[] packet) {
        Tms282FieldRequestReader r = Tms282FieldRequestReader.open(packet, OPCODE, "CP02C5 locked UI request");
        int tick = r.i32();
        int type = r.i32();
        r.end();
        return new Tms282LockedUiRequest(tick, type);
    }
}
