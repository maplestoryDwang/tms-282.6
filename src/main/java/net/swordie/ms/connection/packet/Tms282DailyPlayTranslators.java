package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.Optional;

/**
 * {@link Tms282LegacyPackets.Contributor} of the daily-play phase (shop, storage, channel change, return scrolls,
 * death): the 265 packets those flows build directly and whose 282 body is unchanged.
 * <ul>
 *     <li>OPEN_UI_DEAD -> LP051E {@code i32 flag, i32 reviveType, i32, u8, i32, i32, u8, u8} (265 {@code
 *     UserLocal.openUIOnDead}, sent by {@code Char.openUIOnDead} when HP reaches 0). CUserLocal handler 58B05B0 reads
 *     Decode4 x3 (58B05E7 / 58B05F2 / 58B05FD), Decode1 (58B0607), Decode4 x2 (58B0619 / 58B0625), Decode1 x2
 *     (58B0631 / 58B0643); traced CLEAN with the 265 bodies (research/tms2823/daily-trace, dead-ui-*.tsv) [layout
 *     證實]. Opcode: 265 1255 + 55, the shift of the verified neighbours DO_LOTTERY_UI (1257 -> 0x520) and
 *     FINAL_ATTACK_REQUEST (1261 -> 0x524), with SET_DEAD 1253 -> 0x51C and the one-slot handler 0x51D between
 *     [推定]. The in-game map had guessed SET_HIT_MISS for 0x51E with a +56 shift.</li>
 * </ul>
 */
public final class Tms282DailyPlayTranslators implements Tms282LegacyPackets.Contributor {
    public static final int OPEN_UI_DEAD = 0x051E;

    public static final Tms282DailyPlayTranslators INSTANCE = new Tms282DailyPlayTranslators();

    private Tms282DailyPlayTranslators() {
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        builder.on(OutHeader.OPEN_UI_DEAD, Tms282DailyPlayTranslators::openUiOnDead);
    }

    /** The 265 body as it is, strictly checked, under LP051E. */
    public static Optional<OutPacket> openUiOnDead(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int flag = body.i32();
        int reviveType = body.i32();
        int unknown = body.i32();
        int countdown = body.u8();
        int a = body.i32();
        int b = body.i32();
        int c = body.u8();
        int d = body.u8();
        body.end();
        OutPacket out = Tms282LoginPacketSupport.packet(OPEN_UI_DEAD);
        out.encodeInt(flag);                                        // 58B05E7
        out.encodeInt(reviveType);                                  // 58B05F2
        out.encodeInt(unknown);                                     // 58B05FD
        out.encodeByte(countdown);                                  // 58B0607
        out.encodeInt(a);                                           // 58B0619
        out.encodeInt(b);                                           // 58B0625
        out.encodeByte(c);                                          // 58B0631
        out.encodeByte(d);                                          // 58B0643
        return Optional.of(out);
    }
}
