package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

/** 282.4 native-reader checked rune replies; see research/tms2823/skillpoint-rune-1005. */
public final class Tms282RunePackets implements Tms282LegacyPackets.Contributor {
    public static final Tms282RunePackets INSTANCE = new Tms282RunePackets();
    private Tms282RunePackets() { }
    public static OutPacket challenge(byte[] action) {
        if (action.length != 48 || ByteBuffer.wrap(action, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() != 40)
            throw new IllegalArgumentException("Invalid rune challenge");
        var out = Tms282LoginPacketSupport.packet(0x04FE);
        out.encodeInt(9); out.encodeByte(0); out.encodeByte(1); out.encodeByte(0); out.encodeArr(action);
        return out;
    }
    public static OutPacket cooldown(int milliseconds) {
        var out = Tms282LoginPacketSupport.packet(0x04FE);
        out.encodeInt(2); out.encodeInt(Math.max(0, milliseconds)); return out;
    }
    public static OutPacket close() { return Tms282LoginPacketSupport.packet(0x0547); }
    @Override public void contribute(Tms282LegacyPackets.Builder b) {
        b.on(OutHeader.RUNE_STONE_USE_ACK, legacy -> {
            var in = Tms282LegacyPackets.Body.of(legacy); int type = in.i32();
            if (type == 9) {
                if (in.u8() != 0 || in.u8() != 1 || in.u8() != 0) return Optional.empty();
                return Optional.of(challenge(in.rest()));
            }
            if (type != 2) return Optional.empty();
            int time = in.i32(); in.end(); return Optional.of(cooldown(time));
        }).on(OutHeader.RUNE_STONE_SKILL_ACK, legacy -> {
            var in = Tms282LegacyPackets.Body.of(legacy); int type = in.i32(); in.end();
            return type >= 0 && type <= 8 ? Optional.of(close()) : Optional.empty();
        }).on(OutHeader.RUNE_STONE_DISAPPEAR, legacy -> Tms282DropFixTranslators.same(legacy, 0x07F8, 14));
    }
}
