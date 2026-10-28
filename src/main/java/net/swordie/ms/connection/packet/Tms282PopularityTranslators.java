package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.nio.charset.Charset;
import java.util.Optional;

/**
 * {@link Tms282LegacyPackets.Contributor} for the 265 fame reply (WvsContext.givePopularityResult,
 * 265 GIVE_POPULARITY_RESULT 173) -> LP00AC, CWvsContext handler 5EEDEA0 (context-dispatch.tsv case 0x00AC;
 * ingame-lp-map-2823 證實 Δ=-1, status UNVERIFIED live).
 * <p>
 * Body = 265: u8 type; 0 Success: Str name, u8 inc, i32 fame; 5 Notify: Str name, u8 inc; 1/2/3/4: nothing. Traced
 * CLEAN 2026-09-28 with research/tms2823/social-trace/social_trace.py --entry 5EEDEA0 --this-is-ctx on the 265 bodies
 * of types 0 (11 bytes, 4 reads), 1, 2, 3, 4 and 5 (7 bytes, 3 reads). Only the string charset changes (265 UTF-8 ->
 * the 282 name charset). Other types are declined.
 */
public final class Tms282PopularityTranslators implements Tms282LegacyPackets.Contributor {
    public static final int GIVE_POPULARITY_RESULT = 0x00AC;

    public static final Tms282PopularityTranslators INSTANCE = new Tms282PopularityTranslators();

    private Tms282PopularityTranslators() {
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        Charset charset = builder.charset();
        builder.on(OutHeader.GIVE_POPULARITY_RESULT, p -> popularityResult(p, charset));
    }

    static Optional<OutPacket> popularityResult(OutPacket legacy, Charset cs) {
        Tms282LegacyPackets.Body b = Tms282LegacyPackets.Body.of(legacy);
        int type = b.u8();
        OutPacket out = Tms282LoginPacketSupport.packet(GIVE_POPULARITY_RESULT);
        out.encodeByte(type);
        switch (type) {
            case 0 -> {
                Tms282LoginPacketSupport.str(out, b.str(), cs);
                out.encodeByte(b.u8());
                out.encodeInt(b.i32());
            }
            case 5 -> {
                Tms282LoginPacketSupport.str(out, b.str(), cs);
                out.encodeByte(b.u8());
            }
            case 1, 2, 3, 4 -> {
            }
            default -> {
                return Optional.empty();
            }
        }
        b.end();
        return Optional.of(out);
    }
}
