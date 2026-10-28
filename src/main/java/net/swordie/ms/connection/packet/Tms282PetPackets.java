package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.codec.MovePathCodec;
import net.swordie.ms.connection.protocol.codec.Tms282MovePathCodec;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.life.movement.MovementInfo;
import net.swordie.ms.util.Position;

import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Optional;

/**
 * TMS 282.3 pet packets of CUserPool::OnUserCommonPacket (5AA7B10; the dispatcher reads {@code i32 characterId}
 * first). The pet range is 0x441..0x44A [證實]: the default branch 5AA823D sends 0x441..0x44A to 5480E40 -> 5480E60,
 * which calls the user's virtual +0xD0 for 0x441 (CUserLocal 5790C50 / CUserRemote 5ADD000) and, for 0x442..0x44A,
 * reads {@code i32 petIdx}, finds the pet and jumps through 5481054. The 265 pet headers 1028..1037 map with +0x3D:
 * <ul>
 *     <li>LP0441 PET_ACTIVATED: {@code i32 idx, u8 active}; inactive {@code u8 reason}; when active {@code u8 init} + the pet (419D7E0, the
 *     LP03D0 #58 pet: {@link Tms282UserPoolPackets#encodePet}, 265 Pet.encode + one i32 after the template id);</li>
 *     <li>LP0442 PET_MOVE (41A43E0): 3FDD1D0(path, pkt, keyPad 0), the broadcast path
 *     ({@link MovePathCodec#encodeBroadcast}) as LP046D;</li>
 *     <li>LP0443 PET_ACTION (41A4490): {@code u8, u8, str}; LP0444 PET_ACTION_SPEAK (41A4540): {@code str};
 *     LP0445 PET_NAME_CHANGED (41A4BE0): {@code str}; LP0446 PET_HUE_CHANGED (41A57F0): {@code i32};
 *     LP044A PET_ACTION_COMMAND (41A4D90): {@code u8, u8, ...} - the 265 bodies after the pet index.</li>
 * </ul>
 * The 265 pet packets reach the client through {@link #contribute} (LegacyPacketTranslator): the bodies are read back
 * and written again with the 282 pet encoding and string charset. PET_MOVE is not translated (a 265 path re-read from
 * 265 bytes loses the 282 element tails): the 282 pet move route broadcasts {@link #move} with the path it parsed. Layouts traced with
 * research/tms2823/social-trace (pet-* fixtures of Tms282SocialCheck).
 */
public final class Tms282PetPackets implements Tms282LegacyPackets.Contributor {
    public static final int ACTIVATED = 0x0441;
    public static final int MOVE = 0x0442;
    public static final int ACTION = 0x0443;
    public static final int ACTION_SPEAK = 0x0444;
    public static final int NAME_CHANGED = 0x0445;
    public static final int HUE_CHANGED = 0x0446;
    public static final int ACTION_COMMAND = 0x044A;

    public static final Tms282PetPackets INSTANCE = new Tms282PetPackets();

    private Tms282PetPackets() {
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        Charset cs = builder.charset();
        builder.on(OutHeader.PET_ACTIVATED, p -> activated(p, cs))
                .on(OutHeader.PET_ACTION, p -> action(p, cs))
                .on(OutHeader.PET_ACTION_SPEAK, p -> text(p, ACTION_SPEAK, cs))
                .on(OutHeader.PET_NAME_CHANGED, p -> text(p, NAME_CHANGED, cs))
                .on(OutHeader.PET_HUE_CHANGED, Tms282PetPackets::hue)
                .on(OutHeader.PET_ACTION_COMMAND, Tms282PetPackets::actionCommand);
    }

    // ---- builders ------------------------------------------------------------------------------------------------

    public static OutPacket activated(int characterId, Tms282UserPoolPackets.RemotePet pet, Charset charset) {
        OutPacket out = head(ACTIVATED, characterId, pet.idx());
        out.encodeByte(1);
        out.encodeByte(1);
        Tms282UserPoolPackets.encodePet(out, pet, charset);
        // The owner's handler (CUserLocal 5790C50) then reads two i32 from the AUX region (5790DAA / 5790DDB, stream
        // 939890 in throw mode; locals preset to 0 and -1). Without AUX the owner's client dies on summoning a pet
        // (09-29, packet-audit/aux-readers-0929.md); CUserRemote 5ADD000 reads no AUX, so other users ignore it.
        out.setAuxiliaryData(new byte[]{0, 0, 0, 0, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
        return out;
    }

    public static OutPacket deactivated(int characterId, int idx) {
        return deactivated(characterId, idx, 0);
    }

    /**
     * {@code u8 0, u8 reason}: when the pet is on screen the owner's handler (5790C50 -> 57911D8) reads the removal
     * reason (57912E5; 1..5 and 0x63 have own messages, 0 takes the plain 5791863 path, no further reads). Without it
     * the owner's client threw ZException 0x26 on unsummoning a pet (09-29, CP00AE report of LP0441, 11 bytes).
     * CUserRemote 5ADD000 stops after the active flag; the extra byte is left unread there.
     */
    public static OutPacket deactivated(int characterId, int idx, int reason) {
        OutPacket out = head(ACTIVATED, characterId, idx);
        out.encodeByte(0);
        out.encodeByte(reason);
        return out;
    }

    public static OutPacket move(int characterId, int idx, MovementInfo path) {
        OutPacket out = head(MOVE, characterId, idx);
        Tms282MovePathCodec.INSTANCE.encodeBroadcast(out, Objects.requireNonNull(path, "path"));
        return out;
    }

    public static OutPacket action(int characterId, int idx, int type, int action, String message, Charset charset) {
        OutPacket out = head(ACTION, characterId, idx);
        out.encodeByte(type);
        out.encodeByte(action);
        Tms282LoginPacketSupport.str(out, message, charset);
        return out;
    }

    public static OutPacket text(int opcode, int characterId, int idx, String text, Charset charset) {
        OutPacket out = head(opcode, characterId, idx);
        Tms282LoginPacketSupport.str(out, text, charset);
        return out;
    }

    private static OutPacket head(int opcode, int characterId, int idx) {
        OutPacket out = Tms282LoginPacketSupport.packet(opcode);
        out.encodeInt((long) characterId);
        out.encodeInt((long) idx);
        return out;
    }

    // ---- 265 -> 282 --------------------------------------------------------------------------------------------

    /** 265 PetPacket.activated / deactivated: i32 owner, i32 idx, u8 active, u8 init, [Pet.encode]. */
    static Optional<OutPacket> activated(OutPacket legacy, Charset cs) {
        Tms282LegacyPackets.Body b = Tms282LegacyPackets.Body.of(legacy);
        int owner = b.i32();
        int idx = b.i32();
        boolean active = b.u8() != 0;
        int initOrReason = b.u8();
        if (!active) {
            b.end();
            return Optional.of(deactivated(owner, idx, initOrReason));
        }
        int template = b.i32();
        String name = b.str();
        long sn = b.i64();
        Position pos = new Position((short) b.u16(), (short) b.u16());
        int moveAction = b.u8();
        int fh = (short) b.u16();
        int hue = b.i32();
        b.i32();
        int wonderGrade = (short) b.u16();
        int giantRate = b.i32();
        b.end();
        return Optional.of(activated(owner, new Tms282UserPoolPackets.RemotePet(idx, template, name, sn, pos, moveAction,
                fh, hue, wonderGrade, giantRate), cs));
    }

    static Optional<OutPacket> action(OutPacket legacy, Charset cs) {
        Tms282LegacyPackets.Body b = Tms282LegacyPackets.Body.of(legacy);
        int owner = b.i32();
        int idx = b.i32();
        int type = b.u8();
        int act = b.u8();
        String msg = b.str();
        b.end();
        return Optional.of(action(owner, idx, type, act, msg, cs));
    }

    static Optional<OutPacket> text(OutPacket legacy, int opcode, Charset cs) {
        Tms282LegacyPackets.Body b = Tms282LegacyPackets.Body.of(legacy);
        int owner = b.i32();
        int idx = b.i32();
        String s = b.str();
        b.end();
        return Optional.of(text(opcode, owner, idx, s, cs));
    }

    /** 265 PetPacket.actionCommand / actionCommand_GiveFood: the bytes after the pet index go out unchanged. */
    static Optional<OutPacket> actionCommand(OutPacket legacy) {
        Tms282LegacyPackets.Body b = Tms282LegacyPackets.Body.of(legacy);
        int owner = b.i32();
        int idx = b.i32();
        OutPacket out = head(ACTION_COMMAND, owner, idx);
        out.encodeArr(b.rest());
        return Optional.of(out);
    }

    static Optional<OutPacket> hue(OutPacket legacy) {
        Tms282LegacyPackets.Body b = Tms282LegacyPackets.Body.of(legacy);
        int owner = b.i32();
        int idx = b.i32();
        int color = b.i32();
        b.end();
        OutPacket out = head(HUE_CHANGED, owner, idx);
        out.encodeInt((long) color);
        return Optional.of(out);
    }
}
