package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.nio.charset.Charset;
import java.util.Optional;

/**
 * 282 戰地聯盟 (research/tms2823/union-282/REPORT.md). CUnion::Decode 135CEA0 reads i32 rank, i32 n + members,
 * i32 m + members, <b>3</b> x (u8 flag [+member]), then a varint32 count of {i32, u8 f [member]} entries; 265 writes a
 * 4th optional member and five zero bytes. Member (135C0E0) = 265: i32 type, charId, level, job, subJob, rotation,
 * grid, chuc, i64 combat power, str name, [type 2: str mobile name]; names re-encoded in the 282 charset.
 * <ul>
 *     <li>UNION_RESULT -> LP0AAC (48B31E0): i32 coin, u8 open, CUnion; unlocks the client, open != 0 opens the
 *     window (265 sends 0: its client opened the window itself), so 1 here - 265 sends it only on a union request.</li>
 *     <li>UNION_ASSIGN_RESULT -> LP021A (5E66500): CUnion.</li>
 *     <li>UNION_COIN -> LP0ABB (370D760): i32, i32.</li>
 * </ul>
 */
public final class Tms282UnionPackets implements Tms282LegacyPackets.Contributor {
    public static final Tms282UnionPackets INSTANCE = new Tms282UnionPackets();

    public static final int UNION_RESULT = 0x0AAC;
    public static final int UNION_ASSIGN_RESULT = 0x021A;
    public static final int UNION_COIN = 0x0ABB;

    private Tms282UnionPackets() {
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        Charset charset = builder.charset();
        builder.on(OutHeader.UNION_RESULT, p -> unionResult(p, charset))
                .on(OutHeader.UNION_ASSIGN_RESULT, p -> assignResult(p, charset))
                .on(OutHeader.UNION_COIN, p -> Tms282SkillLegacyTranslators.copy(p, UNION_COIN));
    }

    static Optional<OutPacket> unionResult(OutPacket legacy, Charset charset) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        OutPacket out = Tms282LoginPacketSupport.packet(UNION_RESULT);
        out.encodeInt(body.i32()); // coin
        body.u8();                 // 265 open flag 0
        out.encodeByte(1);
        cunion(body, out, charset);
        return Optional.of(out);
    }

    static Optional<OutPacket> assignResult(OutPacket legacy, Charset charset) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        OutPacket out = Tms282LoginPacketSupport.packet(UNION_ASSIGN_RESULT);
        cunion(body, out, charset);
        return Optional.of(out);
    }

    /** 265 CUnion -> 282 CUnion (3 optional members, varint 0 entries). */
    static void cunion(Tms282LegacyPackets.Body body, OutPacket out, Charset charset) {
        out.encodeInt(body.i32()); // rank
        for (int list = 0; list < 2; list++) {
            int n = body.i32();
            out.encodeInt(n);
            for (int i = 0; i < n; i++) {
                member(body, out, charset);
            }
        }
        for (int i = 0; i < 4; i++) {
            int present = body.u8();
            if (i < 3) {
                out.encodeByte(present);
            }
            if (present != 0) {
                if (i < 3) {
                    member(body, out, charset);
                } else {
                    member(body, new OutPacket((short) 0), charset); // 282 has no 4th member: skipped
                }
            }
        }
        out.encodeByte(0); // varint32 0 entries (265's five zero bytes)
    }

    static void member(Tms282LegacyPackets.Body body, OutPacket out, Charset charset) {
        int type = body.i32();
        out.encodeInt(type);
        for (int i = 0; i < 7; i++) {
            out.encodeInt(body.i32()); // charId, level, job, subJob, rotation, grid, chuc
        }
        out.encodeLong(body.i64());    // combat power
        Tms282LoginPacketSupport.str(out, body.str(), charset);
        if (type == 2) {
            Tms282LoginPacketSupport.str(out, body.str(), charset);
        }
    }
}
