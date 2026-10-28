package net.swordie.ms.connection.packet;

import java.io.ByteArrayOutputStream;

/**
 * CP02EB summoned create-shoot-object request (282 sender 4551BE0, built at 4552888; e.g. the Battle Mage Altar of
 * Annihilation curses). Layout from a live capture (recv-5598-02eb, 802 bytes, 16 objects) and the static encode order:
 * <pre>
 * i32 summon, u8 action,
 * i32 skill, i32 slv, i32 action, i32 actionSpeed, i32 item, i16 x, i16 y, 6 x i32, u8, i32, u8, 8 bytes, i32, i32,
 * u8 (282 only), i32 n,
 * n x { 4 x i32, i16 direction, 2 x (i16, i16), i32 delay, u8 flip, i32, u8 crystal [i32, i32], u8 arrow [i32, i32],
 *       i32, i32 (282 only) }
 * </pre>
 * 265 SUMMONED_CREATE_SHOOT_OBJ (733) is the same without the two 282-only fields; {@link #toGms265Body()} drops them.
 * Parsed completely so a malformed body is refused.
 */
public final class Tms282SummonShootRequest {
    public static final int OPCODE = 0x02EB;
    private static final int MAX_OBJECTS = 64;
    private static final int HEAD = 4 + 1 + 5 * 4 + 2 * 2 + 6 * 4 + 1 + 4 + 1 + 8 + 4 + 4;

    private final int summonId, skillId, count;
    private final byte[] body;

    private Tms282SummonShootRequest(int summonId, int skillId, int count, byte[] body) {
        this.summonId = summonId;
        this.skillId = skillId;
        this.count = count;
        this.body = body;
    }

    public static Tms282SummonShootRequest parse(byte[] packet) {
        var in = Tms282FieldRequestReader.open(packet, OPCODE, "summon shoot request");
        ByteArrayOutputStream out = new ByteArrayOutputStream(packet.length);
        byte[] head = in.bytes(HEAD);
        out.writeBytes(head);
        int summon = le32(head, 0), skill = le32(head, 5);
        in.u8(); // 282 only
        byte[] n4 = in.bytes(4);
        int n = le32(n4, 0);
        if (n < 0 || n > MAX_OBJECTS) throw new IllegalArgumentException("Bad shoot object count " + n);
        out.writeBytes(n4);
        for (int i = 0; i < n; i++) {
            out.writeBytes(in.bytes(4 * 4 + 2 + 2 * 4 + 4 + 1 + 4));
            for (int flag = 0; flag < 2; flag++) { // crystal, arrow
                int on = in.u8();
                out.write(on);
                if (on != 0) out.writeBytes(in.bytes(8));
            }
            out.writeBytes(in.bytes(4));
            in.i32(); // 282 only
        }
        in.end();
        return new Tms282SummonShootRequest(summon, skill, n, out.toByteArray());
    }

    private static int le32(byte[] b, int at) {
        return (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8 | (b[at + 2] & 0xFF) << 16 | (b[at + 3] & 0xFF) << 24;
    }

    public int summonId() { return summonId; }
    public int skillId() { return skillId; }
    public int count() { return count; }

    /** The 265 SUMMONED_CREATE_SHOOT_OBJ body (the two 282-only fields removed). */
    public byte[] toGms265Body() { return body.clone(); }
}
