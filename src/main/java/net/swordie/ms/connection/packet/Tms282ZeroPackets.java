package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.info.ZeroInfo;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;
import java.util.Optional;

/** Native zero-followup-1005: LP0119 resource mask, LP011A i32, LP011B u8+i32, LP04A4 pool id only. */
public final class Tms282ZeroPackets implements Tms282LegacyPackets.Contributor {
    public static final int INFO = 0x0119, WEAPON_POINTS = 0x011A, SUB_HP = 0x011B, ASSIST_END = 0x04A4;
    public static final int RESOURCE_MASK = 0xC7;
    public static final Tms282ZeroPackets INSTANCE = new Tms282ZeroPackets();
    private Tms282ZeroPackets() { }

    /** Only proven state/HP/TP/MHP/MTP fields; no claim of Zero cosmetic support. */
    public static OutPacket info(ZeroInfo z) {
        OutPacket out = Tms282LoginPacketSupport.packet(INFO);
        out.encodeShort(RESOURCE_MASK); out.encodeByte(z.isZeroBetaState());
        out.encodeInt(z.getSubHP()); out.encodeInt(z.getSubMP());
        out.encodeInt(z.getSubMHP()); out.encodeInt(z.getSubMMP());
        return out;
    }
    public static OutPacket weaponPoints(int points) {
        OutPacket out = Tms282LoginPacketSupport.packet(WEAPON_POINTS); out.encodeInt(points); return out;
    }
    public static OutPacket subHp(boolean beta, int hp) {
        OutPacket out = Tms282LoginPacketSupport.packet(SUB_HP); out.encodeByte(beta); out.encodeInt(hp); return out;
    }
    public static OutPacket assistEnd(int characterId) {
        OutPacket out = Tms282LoginPacketSupport.packet(ASSIST_END);
        out.encodeInt(characterId); // Pool5AA6E40 consumes id; leaf5ADF700 reads no body.
        return out;
    }
    @Override public void contribute(Tms282LegacyPackets.Builder builder) {
        builder.on(OutHeader.ZERO_INFO, Tms282ZeroPackets::infoLegacy)
                .on(OutHeader.ZERO_WP, p -> {var b = Tms282LegacyPackets.Body.of(p); int n = b.i32(); b.end(); return Optional.of(weaponPoints(n));})
                .on(OutHeader.ZERO_INFO_SUB_HP, p -> {var b = Tms282LegacyPackets.Body.of(p); int state = b.u8(), hp = b.i32(); b.end();
                    return state > 1 || hp < 0 ? Optional.empty() : Optional.of(subHp(state == 1, hp));})
                .on(OutHeader.REMOTE_ZERO_LAST_ASSIST_STATE, p -> {var b = Tms282LegacyPackets.Body.of(p); int id = b.i32(); b.end(); return Optional.of(assistEnd(id));});
    }
    static Optional<OutPacket> infoLegacy(OutPacket legacy) {
        var b = Tms282LegacyPackets.Body.of(legacy);
        int mask = b.u16();
        if ((mask & ~0x3FF) != 0) return Optional.empty();
        OutPacket out = Tms282LoginPacketSupport.packet(INFO);
        out.encodeShort(mask & RESOURCE_MASK);
        if ((mask & 1) != 0) {int state = b.u8(); if (state > 1) return Optional.empty(); out.encodeByte(state);}
        if ((mask & 2) != 0) out.encodeInt(b.i32());
        if ((mask & 4) != 0) out.encodeInt(b.i32());
        // 282 bit8 reads u8+i32; appearance correspondence is unproved. Consume265 fields, omit appearance bits.
        if ((mask & 8) != 0) b.u8();
        if ((mask & 0x10) != 0) b.i32();
        if ((mask & 0x20) != 0) b.i32();
        if ((mask & 0x40) != 0) out.encodeInt(b.i32());
        if ((mask & 0x80) != 0) out.encodeInt(b.i32());
        if ((mask & 0x100) != 0) b.i32(); // native bit8 is i64, not this265 cash-link field
        if ((mask & 0x200) != 0) {b.i32(); b.i32(); b.i32();}
        b.end(); return Optional.of(out);
    }
}
