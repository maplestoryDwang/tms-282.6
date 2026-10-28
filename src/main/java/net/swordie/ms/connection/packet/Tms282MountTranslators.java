package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.Optional;

/**
 * {@link Tms282LegacyPackets.Contributor} for the taming mob (mount) info (social phase 3): 265 SET_TAMING_MOB_INFO
 * (0x00BE, {@code FieldPacket.setTamingMobInfo}) goes out as LP00BD with the 265 body, i32 character, i32 level,
 * i32 exp, i32 fatigue, u8 show effect (CWvsContext handler 5F27940 reads I I I I B, ingame-lp-map 證實, Δ=-1).
 */
public final class Tms282MountTranslators implements Tms282LegacyPackets.Contributor {
    public static final int SET_TAMING_MOB_INFO = 0x00BD;
    public static final Tms282MountTranslators INSTANCE = new Tms282MountTranslators();

    private Tms282MountTranslators() {
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        builder.on(OutHeader.SET_TAMING_MOB_INFO, Tms282MountTranslators::tamingMobInfo);
    }

    static Optional<OutPacket> tamingMobInfo(OutPacket legacy) {
        Tms282LegacyPackets.Body b = Tms282LegacyPackets.Body.of(legacy);
        OutPacket out = Tms282LoginPacketSupport.packet(SET_TAMING_MOB_INFO);
        out.encodeInt(b.i32());
        out.encodeInt(b.i32());
        out.encodeInt(b.i32());
        out.encodeInt(b.i32());
        out.encodeByte(b.u8());
        b.end();
        return Optional.of(out);
    }
}
