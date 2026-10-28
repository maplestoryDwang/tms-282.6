package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.Optional;

/**
 * Evan's dragon, Kanna's Haku (fox man) and skill pets on 282 (research/tms2823/pet-dragon-fox-trace/README.md).
 * They were dropped by the guard (never shown). 282 opcode = 265 + 61 in all three groups (user dispatcher 5AA7B10):
 * <ul>
 *     <li>DRAGON_CREATED -> LP044B: i32 owner, i32 x, i32 y, u8 move action, u16 foothold, u16 job (= 265);
 *     DRAGON_REMOVE(_ROLE_PLAYING_CHAR) -> LP044D / LP044E (owner only).</li>
 *     <li>FOX_MAN_ENTER_FIELD -> LP0454: i32 owner, u16 form, i16 x, i16 y, u8 move action, i16 foothold, i32 upgrade,
 *     i32 fan (= 265, whose foothold is a hard-coded 0); EXCL_RESULT / SHOW_CHANGE_EFFECT / LEAVE_FIELD -> LP0456 /
 *     LP0457 / LP0459 (owner only).</li>
 *     <li>SKILL_PET_TRANSFER_FIELD -> LP0460: i32 owner, i32 object, i32 template, u8 state, i16 x, i16 y, <b>u8</b> move
 *     action (265 writes u16), i16 foothold; SKILL_PET_STATE -> LP045D (= 265); SKILL_PET_ACTION -> LP045C: i32 owner,
 *     <b>i32 object</b>, u8, u8, string.</li>
 * </ul>
 * Every create gets the owner's current position and the foothold below it (a stale / 0 foothold - the same family
 * as the android create). Not translated: the three MOVE packets (282 move paths; the owner's client moves its own
 * companion), FOX_MAN_MODIFIED (282 reads a trailing u8 for the owner only, unknown per recipient here).
 * {@code tms282.companions.visible=false} (server.properties / -D) sends none of them again.
 */
public final class Tms282CompanionTranslators implements Tms282LegacyPackets.Contributor {
    public static final Tms282CompanionTranslators INSTANCE = new Tms282CompanionTranslators();

    public static final int DRAGON_CREATED = 0x044B;
    public static final int DRAGON_REMOVE = 0x044D;
    public static final int DRAGON_REMOVE_ROLE_PLAYING_CHAR = 0x044E;
    public static final int FOX_MAN_ENTER_FIELD = 0x0454;
    public static final int FOX_MAN_EXCL_RESULT = 0x0456;
    public static final int FOX_MAN_SHOW_CHANGE_EFFECT = 0x0457;
    public static final int FOX_MAN_LEAVE_FIELD = 0x0459;
    public static final int SKILL_PET_ACTION = 0x045C;
    public static final int SKILL_PET_STATE = 0x045D;
    public static final int SKILL_PET_TRANSFER_FIELD = 0x0460;

    public static final boolean VISIBLE = !"false".equalsIgnoreCase(System.getProperty("tms282.companions.visible",
            net.swordie.ms.console.config.ServerRuntimeConfig.stringValue("tms282.companions.visible")).trim());

    private Tms282CompanionTranslators() {
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        java.nio.charset.Charset charset = builder.charset();
        builder.on(OutHeader.DRAGON_CREATED, p -> VISIBLE ? dragon(p) : Optional.empty())
                .on(OutHeader.DRAGON_REMOVE, p -> VISIBLE ? copy(p, DRAGON_REMOVE) : Optional.empty())
                .on(OutHeader.DRAGON_REMOVE_ROLE_PLAYING_CHAR, p -> VISIBLE ? copy(p, DRAGON_REMOVE_ROLE_PLAYING_CHAR) : Optional.empty())
                .on(OutHeader.FOX_MAN_ENTER_FIELD, p -> VISIBLE ? fox(p) : Optional.empty())
                .on(OutHeader.FOX_MAN_EXCL_RESULT, p -> VISIBLE ? copy(p, FOX_MAN_EXCL_RESULT) : Optional.empty())
                .on(OutHeader.FOX_MAN_SHOW_CHANGE_EFFECT, p -> VISIBLE ? copy(p, FOX_MAN_SHOW_CHANGE_EFFECT) : Optional.empty())
                .on(OutHeader.FOX_MAN_LEAVE_FIELD, p -> VISIBLE ? copy(p, FOX_MAN_LEAVE_FIELD) : Optional.empty())
                .on(OutHeader.SKILL_PET_TRANSFER_FIELD, p -> VISIBLE ? skillPet(p) : Optional.empty())
                .on(OutHeader.SKILL_PET_STATE, p -> VISIBLE ? copy(p, SKILL_PET_STATE) : Optional.empty())
                .on(OutHeader.SKILL_PET_ACTION, p -> VISIBLE ? skillPetAction(p, charset) : Optional.empty());
    }

    static Optional<OutPacket> copy(OutPacket legacy, int op) {
        return Tms282SkillLegacyTranslators.copy(legacy, op);
    }

    /** The online owner, or null (tests / offline). */
    static Char owner(int characterId) {
        try {
            return net.swordie.ms.Server.get().getChars().get(characterId);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    /** x, y, foothold of the owner now (null when unknown). */
    static int[] place(int ownerId) {
        Char owner = owner(ownerId);
        if (owner == null || owner.getPosition() == null || owner.getField() == null) {
            return null;
        }
        var below = owner.getField().findFootHoldBelow(owner.getPosition());
        return new int[]{owner.getPosition().getX(), owner.getPosition().getY(), below == null ? owner.getFoothold() : below.getId()};
    }

    static Optional<OutPacket> dragon(OutPacket legacy) {
        Tms282LegacyPackets.Body b = Tms282LegacyPackets.Body.of(legacy);
        int owner = b.i32();
        int x = b.i32(), y = b.i32();
        int action = b.u8();
        int fh = b.u16();
        int job = b.u16();
        int[] at = place(owner);
        if (at != null) {
            x = at[0];
            y = at[1];
            fh = at[2];
        }
        OutPacket out = Tms282LoginPacketSupport.packet(DRAGON_CREATED);
        out.encodeInt(owner);
        out.encodeInt(x);
        out.encodeInt(y);
        out.encodeByte(action);
        out.encodeShort(fh);
        out.encodeShort(job);
        return Optional.of(out);
    }

    static Optional<OutPacket> fox(OutPacket legacy) {
        Tms282LegacyPackets.Body b = Tms282LegacyPackets.Body.of(legacy);
        int owner = b.i32();
        int form = b.u16();
        int x = (short) b.u16(), y = (short) b.u16();
        int action = b.u8();
        int fh = (short) b.u16();
        int upgrade = b.i32();
        int fan = b.i32();
        int[] at = place(owner);
        if (at != null) {
            x = at[0];
            y = at[1];
            fh = at[2];
        }
        OutPacket out = Tms282LoginPacketSupport.packet(FOX_MAN_ENTER_FIELD);
        out.encodeInt(owner);
        out.encodeShort(form);
        out.encodeShort(x);
        out.encodeShort(y);
        out.encodeByte(action);
        out.encodeShort(fh);
        out.encodeInt(upgrade);
        out.encodeInt(fan);
        return Optional.of(out);
    }

    static Optional<OutPacket> skillPet(OutPacket legacy) {
        Tms282LegacyPackets.Body b = Tms282LegacyPackets.Body.of(legacy);
        int owner = b.i32();
        int object = b.i32();
        int template = b.i32();
        int state = b.u8();
        int x = (short) b.u16(), y = (short) b.u16();
        int action = b.u16(); // 265 u16, 282 u8
        int fh = (short) b.u16();
        if (fh == 0) {
            int[] at = place(owner);
            if (at != null) {
                x = at[0];
                y = at[1];
                fh = at[2];
            }
        }
        OutPacket out = Tms282LoginPacketSupport.packet(SKILL_PET_TRANSFER_FIELD);
        out.encodeInt(owner);
        out.encodeInt(object);
        out.encodeInt(template);
        out.encodeByte(state);
        out.encodeShort(x);
        out.encodeShort(y);
        out.encodeByte(action);
        out.encodeShort(fh);
        return Optional.of(out);
    }

    static Optional<OutPacket> skillPetAction(OutPacket legacy, java.nio.charset.Charset charset) {
        Tms282LegacyPackets.Body b = Tms282LegacyPackets.Body.of(legacy);
        int owner = b.i32();
        Char chr = owner(owner);
        if (chr == null || chr.getSkillPet() == null) {
            return Optional.empty(); // the object id 282 needs is unknown
        }
        OutPacket out = Tms282LoginPacketSupport.packet(SKILL_PET_ACTION);
        out.encodeInt(owner);
        out.encodeInt(chr.getSkillPet().getObjectId());
        out.encodeByte(b.u8());
        out.encodeByte(b.u8());
        Tms282LoginPacketSupport.str(out, b.str(), charset); // 282 name charset
        return Optional.of(out);
    }
}
