package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.Optional;

/**
 * Character-interaction phase (2026-09-24): 265 sit / emotion / chair packets in their 282 form, traced against the
 * client (research/tms2823/interact-trace, tools/Tms282InteractionCheck).
 * <ul>
 *     <li>EMOTION -> LP04BE {@code i32 emotion, i32 duration, u8 byItemOption}: same layout as 265; CUserLocal handler
 *     545EB30 (shared with REMOTE_EMOTION LP0480) reads it CLEAN [證實]. The 8-byte 265 android variant (no byte) is
 *     declined: 282 would read past it.</li>
 *     <li>SIT_RESULT -> LP0401 {@code i32 charId, u8 seated, [u16 seat]}: CUserPool::OnUserCommonPacket case 5695C50
 *     reads {@code D1, [D2]} after the id, the 265 body [layout 證實 by disassembly; opcode Δ+54 like SET_DAMAGE_SKIN
 *     0x3FF: 推定].</li>
 *     <li>REMOTE_SET_ACTIVE_PORTABLE_CHAIR -> LP048C: 282 5ADA9A0 reads {@code D4 itemId, D4 (new, meaning unknown),
 *     D1 hasChair} and, when set, lets the chair object built from the item's WZ data decode the rest (vtable+0x18),
 *     which the emulator cannot follow. The stand-up form (hasChair 0) is translated, with the new i32 as 0
 *     (traced CLEAN), and a normal chair (Tms282ChairView kind "normal": decoder 35BDD60 reads nothing) as
 *     {@code itemId, 0, 1} without a body (09-29); any other sitting chair is declined.</li>
 * </ul>
 * REMOTE_EMOTION -> LP0480 and SET_DAMAGE_SKIN -> LP03FF keep their 265 bodies (copied; 545EB30 / 54C5BD0 read them
 * CLEAN). Their static SAME_AS_265 table rows alone do not give the profile an opcode (static evidence is not loaded),
 * so they are translated like the others.
 */
public final class Tms282InteractionTranslators implements Tms282LegacyPackets.Contributor {
    public static final Tms282InteractionTranslators INSTANCE = new Tms282InteractionTranslators();

    public static final int SIT_RESULT = 0x0401;
    public static final int REMOTE_SET_ACTIVE_PORTABLE_CHAIR = 0x048C;
    public static final int EMOTION = 0x04BE;
    public static final int SET_DAMAGE_SKIN = 0x03FF;
    public static final int REMOTE_EMOTION = 0x0480;

    private Tms282InteractionTranslators() {
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        builder.on(OutHeader.EMOTION, Tms282InteractionTranslators::emotion)
                .on(OutHeader.SIT_RESULT, Tms282InteractionTranslators::sitResult)
                .on(OutHeader.REMOTE_SET_ACTIVE_PORTABLE_CHAIR, Tms282InteractionTranslators::remoteChair)
                .on(OutHeader.REMOTE_EMOTION, p -> copy(p, REMOTE_EMOTION))
                .on(OutHeader.REMOTE_SET_DEFAULT_WING_ITEM, p -> remoteWing(p, false))
                .on(OutHeader.REMOTE_SET_KAISER_TRANSFORM_ITEM, p -> remoteWing(p, true))
                .on(OutHeader.SET_DAMAGE_SKIN, p -> copy(p, SET_DAMAGE_SKIN));
    }

    /** LP0487/0488: after the pool's character id, 5ADA8B0/5ADA8E0 read D4 / D4 D1.
     * Native read traces: interact-trace/out/{wing-*,kaiser-*}; appearance setters are not emulated.
     */
    static Optional<OutPacket> remoteWing(OutPacket legacy, boolean kaiser) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int charId = body.i32();
        int itemId = body.i32();
        int type = kaiser ? body.u8() : 0;
        body.end();
        if (kaiser && type > 1) {
            return Optional.empty();
        }
        OutPacket out = Tms282LoginPacketSupport.packet(kaiser ? 0x0488 : 0x0487);
        out.encodeInt(charId);
        out.encodeInt(itemId);
        if (kaiser) {
            out.encodeByte(type);
        }
        return Optional.of(out);
    }

    static Optional<OutPacket> copy(OutPacket legacy, int op) {
        OutPacket out = Tms282LoginPacketSupport.packet(op);
        out.encodeArr(Tms282LegacyPackets.Body.of(legacy).rest());
        return Optional.of(out);
    }

    static Optional<OutPacket> emotion(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int emotion = body.i32();
        int duration = body.i32();
        int byItemOption = body.u8();
        body.end();
        OutPacket out = Tms282LoginPacketSupport.packet(EMOTION);
        out.encodeInt(emotion);
        out.encodeInt(duration);
        out.encodeByte(byItemOption);
        return Optional.of(out);
    }

    static Optional<OutPacket> sitResult(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int charId = body.i32();
        int seated = body.u8();
        OutPacket out = Tms282LoginPacketSupport.packet(SIT_RESULT);
        out.encodeInt(charId);
        out.encodeByte(seated);
        if (seated != 0) {
            out.encodeShort(body.u16());
        }
        body.end();
        return Optional.of(out);
    }

    static Optional<OutPacket> remoteChair(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int charId = body.i32();
        int itemId = body.i32();
        int hasChair = body.u8();
        if (hasChair != 0 && Tms282ChairView.kindOf(itemId).equals("normal")) {
            // normal chair class body (35BDD60) reads nothing: the 265 body is not sent (packet-audit/chair-miniroom-282.md)
            OutPacket out = Tms282LoginPacketSupport.packet(REMOTE_SET_ACTIVE_PORTABLE_CHAIR);
            out.encodeInt(charId);
            out.encodeInt(itemId);
            out.encodeInt(0);
            out.encodeByte(1);
            return Optional.of(out);
        }
        if (hasChair != 0) {
            System.out.printf("[TMS282] chair %d of character %d not shown to others: 282 chair body untraced%n",
                    itemId, charId);
            return Optional.empty();
        }
        body.end();
        OutPacket out = Tms282LoginPacketSupport.packet(REMOTE_SET_ACTIVE_PORTABLE_CHAIR);
        out.encodeInt(charId);
        out.encodeInt(itemId);
        out.encodeInt(0);
        out.encodeByte(0);
        return Optional.of(out);
    }
}
