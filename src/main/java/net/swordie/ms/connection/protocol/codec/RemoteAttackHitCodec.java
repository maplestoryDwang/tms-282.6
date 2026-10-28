package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.connection.OutPacket;

/**
 * One hit of a mob block in the remote attack packets (265 REMOTE_*_ATTACK, written by
 * {@link net.swordie.ms.connection.packet.UserRemote#attack}). GMS 265 writes {@code u8 critical, i64 damage}; the
 * TMS 282.3 client reads a single i64 whose bit 63 is the critical flag (CUserRemote attack decoder 5ACD8E0, loop at
 * 5ACE931: Decode8, {@code shr 63} -> critical, {@code and 0x7FFF...} -> damage).
 * <p>
 * Codec family of {@link net.swordie.ms.connection.protocol.ProfileCodecs}; the GMS 265 profile registers none and
 * gets {@link #GMS_265}, so its bytes do not change.
 */
public interface RemoteAttackHitCodec {

    void writeHit(OutPacket out, boolean critical, long damage);

    /** 265: {@code u8 critical, i64 damage}. */
    RemoteAttackHitCodec GMS_265 = (out, critical, damage) -> {
        out.encodeByte(critical);
        out.encodeLong(damage);
    };

    /** 282: {@code i64 damage | critical << 63}. */
    RemoteAttackHitCodec TMS_282 = (out, critical, damage) ->
            out.encodeLong((damage & Long.MAX_VALUE) | (critical ? Long.MIN_VALUE : 0L));

    /** The active profile's codec, else the GMS 265 one. */
    static RemoteAttackHitCodec active() {
        return ProfileEncoders.find(RemoteAttackHitCodec.class).orElse(GMS_265);
    }
}
