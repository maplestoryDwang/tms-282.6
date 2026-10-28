package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.Optional;

/**
 * 265 combat packets the other users of a field see, as TMS 282.3 packets ({@link Tms282LegacyPackets.Contributor};
 * traced with research/tms2823/skill2-trace/skill2_trace.py, skills phase 2):
 * <ul>
 *     <li>REMOTE_MELEE/SHOOT/MAGIC/BODY_ATTACK -> LP0478..LP047B (265 + 49; CUserRemote attack decoder 5ACD8E0 for all
 *     four). Header and mob blocks equal 265 [traced]; each hit is one i64 with the critical flag in bit 63, which
 *     {@link net.swordie.ms.connection.protocol.codec.RemoteAttackHitCodec} already writes for this profile. After the
 *     mob loop the decoder enters the Themida VM (jmp at 5ACF80A), so the 265 trailer is sent as is [presumed] and
 *     {@link #ATTACK_PAD} zero bytes follow (an optional field read as 0 / false instead of a short read; the client
 *     ignores unread bytes).</li>
 *     <li>SUMMONED_ATTACK -> LP0671 (265 + 48, summoned pool jump table 45AC1B4 -> 5482010 -> CSummoned 4522060):
 *     body equal to 265 [traced CLEAN].</li>
 *     <li>REMOTE_SKILL_PREPARE -> LP047C (5AD7E40: i32 skill, u8 slv, i16, u8 read as 265; the trailing position was
 *     not read on the traced path) and REMOTE_SKILL_CANCEL -> LP047E (5AD88A0: i32, i32 as 265 [traced CLEAN]).</li>
 *     <li>AFFECTED_AREA_CREATED -> LP0752 (168F9E0): 265 writes elemAttr twice, 282 reads it once, and after the
 *     special-area list 282 reads two more i32 (168FCCD / 168FCD5) before the Vestige of Divinity i32 [traced]: the
 *     second elemAttr is cut and two zero i32 are inserted. INSTALLED_AREA_FIRE -> LP0753 (map 證實, body as 265)
 *     and AFFECTED_AREA_REMOVED -> LP0754 (16A5070: i32 object id, then skill-dependent i32s only for the areas it knows; 265 body kept [opcode
 *     presumed]).</li>
 * </ul>
 */
public final class Tms282CombatTranslators implements Tms282LegacyPackets.Contributor {
    public static final Tms282CombatTranslators INSTANCE = new Tms282CombatTranslators();

    public static final int REMOTE_MELEE_ATTACK = 0x0478;
    public static final int REMOTE_SHOOT_ATTACK = 0x0479;
    public static final int REMOTE_MAGIC_ATTACK = 0x047A;
    public static final int REMOTE_BODY_ATTACK = 0x047B;
    public static final int REMOTE_SKILL_PREPARE = 0x047C;
    public static final int REMOTE_SKILL_CANCEL = 0x047E;
    public static final int REMOTE_MOVING_SHOOT_ATTACK_PREPARE = 0x047D;
    public static final int SUMMONED_ATTACK = 0x0671;
    public static final int AFFECTED_AREA_CREATED = 0x0752;
    public static final int INSTALLED_AREA_FIRE = 0x0753;
    public static final int AFFECTED_AREA_REMOVED = 0x0754;
    /** Zero bytes after a remote attack's 265 trailer (see the class comment). */
    static final int ATTACK_PAD = 16;
    /** Manifestation: Wind Swing, the one skill with a second rectangle in AFFECTED_AREA_CREATED. */
    static final int WIND_SWING = 162111000;
    static final int VESTIGE_OF_DIVINITY = 400001098;
    static final int HOLY_WATER = 2321015;

    private Tms282CombatTranslators() {
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        builder.on(OutHeader.REMOTE_MELEE_ATTACK, p -> attack(p, REMOTE_MELEE_ATTACK))
                .on(OutHeader.REMOTE_SHOOT_ATTACK, p -> attack(p, REMOTE_SHOOT_ATTACK))
                .on(OutHeader.REMOTE_MAGIC_ATTACK, p -> attack(p, REMOTE_MAGIC_ATTACK))
                .on(OutHeader.REMOTE_BODY_ATTACK, p -> attack(p, REMOTE_BODY_ATTACK))
                .on(OutHeader.REMOTE_SKILL_PREPARE, p -> copy(p, REMOTE_SKILL_PREPARE))
                .on(OutHeader.REMOTE_SKILL_CANCEL, p -> copy(p, REMOTE_SKILL_CANCEL))
                .on(OutHeader.REMOTE_MOVING_SHOOT_ATTACK_PREPARE, Tms282CombatTranslators::movingPrepare)
                .on(OutHeader.SUMMONED_ATTACK, p -> copy(p, SUMMONED_ATTACK))
                .on(OutHeader.AFFECTED_AREA_CREATED, Tms282CombatTranslators::affectedAreaCreated)
                .on(OutHeader.INSTALLED_AREA_FIRE, p -> copy(p, INSTALLED_AREA_FIRE))
                .on(OutHeader.AFFECTED_AREA_REMOVED, p -> copy(p, AFFECTED_AREA_REMOVED));
    }

    static Optional<OutPacket> copy(OutPacket legacy, int op) {
        OutPacket out = Tms282LoginPacketSupport.packet(op);
        out.encodeArr(Tms282LegacyPackets.Body.of(legacy).rest());
        return Optional.of(out);
    }

    /** 5AD84B0: the native flag means "has skill"; 265 sends "normal attack" and always includes skillId. */
    static Optional<OutPacket> movingPrepare(OutPacket legacy) {
        if (legacy.getData().length != 15) return Optional.empty();
        var body = Tms282LegacyPackets.Body.of(legacy);
        int owner = body.i32(), level = body.u8(), normal = body.u8(), skill = body.i32();
        if (normal != (skill == 0 ? 1 : 0)) return Optional.empty();
        OutPacket out = Tms282LoginPacketSupport.packet(REMOTE_MOVING_SHOOT_ATTACK_PREPARE);
        out.encodeInt(owner); out.encodeByte(level); out.encodeByte(skill != 0);
        if (skill != 0) out.encodeInt(skill);
        out.encodeArr(body.bytes(3)); body.end();
        return Optional.of(out);
    }

    static Optional<OutPacket> attack(OutPacket legacy, int op) {
        OutPacket out = Tms282LoginPacketSupport.packet(op);
        out.encodeArr(Tms282LegacyPackets.Body.of(legacy).rest());
        out.encodeArr(new byte[ATTACK_PAD]);
        // 5ACD8E0 reads an AUX flag byte (5AD1BE5 via stream 939890 in throw mode; non-zero adds an i32 at 5AD1C22)
        // after the skill classification every attack path joins. Without AUX the read throws (ZException 0x26 ->
        // fatal, the android LP044F crash of 09-29) on every client that sees another player attack.
        out.setAuxiliaryData(new byte[]{0});
        return Optional.of(out);
    }

    /** 265 body without its second elemAttr i32 and with the two 282 i32 after the special-area list. */
    static Optional<OutPacket> affectedAreaCreated(OutPacket legacy) {
        byte[] body = Tms282LegacyPackets.Body.of(legacy).rest();
        // i32 objectId, u8 mobOrigin, i32 owner, i32 skillId, i16 slv, i16 delay, rect (16), [rect (16)], i32 elem, i32 elem
        int skillAt = 4 + 1 + 4;
        if (body.length < skillAt + 4) {
            return Optional.empty();
        }
        int skillId = (body[skillAt] & 0xFF) | (body[skillAt + 1] & 0xFF) << 8 | (body[skillAt + 2] & 0xFF) << 16
                | (body[skillAt + 3] & 0xFF) << 24;
        int secondElem = skillAt + 4 + 2 + 2 + 16 + (skillId == WIND_SWING ? 16 : 0) + 4;
        if (body.length < secondElem + 4) {
            return Optional.empty();
        }
        // 265 ends with i32 (Vestige of Divinity) / i32 (Holy Water) after the special-area list; 282 reads two
        // unconditional i32 there first (168FCCD, 168FCD5), then the Vestige i32 only (168FDC5)
        int tail = (skillId == VESTIGE_OF_DIVINITY ? 4 : 0) + (skillId == HOLY_WATER ? 4 : 0);
        byte[] out282 = new byte[body.length - 4 + 8];
        int w = 0;
        System.arraycopy(body, 0, out282, w, secondElem);
        w += secondElem;
        int listEnd = body.length - tail;
        System.arraycopy(body, secondElem + 4, out282, w, listEnd - secondElem - 4);
        w += listEnd - secondElem - 4 + 8;
        System.arraycopy(body, listEnd, out282, w, tail);
        OutPacket out = Tms282LoginPacketSupport.packet(AFFECTED_AREA_CREATED);
        out.encodeArr(out282);
        return Optional.of(out);
    }
}
