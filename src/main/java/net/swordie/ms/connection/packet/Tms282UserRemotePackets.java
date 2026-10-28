package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.codec.MovePathCodec;
import net.swordie.ms.connection.protocol.codec.Tms282MovePathCodec;
import net.swordie.ms.life.movement.MovementInfo;

import java.util.Objects;

/**
 * TMS 282.3 CUserRemote packets sent to the other users of a field.
 * <p>
 * LP046D REMOTE_MOVE [verified]: CUserPool::OnUserRemotePacket (5AA6E40) reads {@code i32 characterId} and finds
 * the remote user, then CUserRemote::OnMove 5ACD840 calls 3FDD1D0(path, pkt, keyPad = 0): the CMovePath header and
 * elements ({@link MovePathCodec#encodeBroadcast}), no key pad. The path is the one the mover sent in CP0106, so it
 * is re-encoded with the 282 element table (5-byte element tail).
 * <p>
 * The dispatcher switches on the opcode with the jump table 5AA7908 / 5AA78A0 (0x46D..0x4B6): 0x48F -> 5AA764A ->
 * 5ADDAD0, 0x490 -> 5AA766A -> 5ADDD30; REMOTE_EFFECT 0x48E is in the first switch (5AA7413 -> 545EC40). Layouts
 * traced with research/tms2823/skill-trace/remote_trace.py (packets without the leading characterId):
 * <ul>
 *     <li>LP048F REMOTE_SET_TEMPORARY_STAT (5ADDAD0): the remote CTS block 14FCC50
 *     ({@link Tms282RemoteStatEncoder#encodeForRemote}), then {@code i16 delay} (5ADDB4F), {@code u8} (5ADDB5A);
 *     265 writes the same tail;</li>
 *     <li>LP0490 REMOTE_RESET_TEMPORARY_STAT (5ADDD30): {@link Tms282RemoteStatEncoder#encodeReset};</li>
 *     <li>LP048E REMOTE_EFFECT (545EC40, CUser::OnEffect): {@link Tms282EffectPackets.UserEffect}.</li>
 * </ul>
 */
public final class Tms282UserRemotePackets {
    public static final int REMOTE_MOVE = 0x046D;
    public static final int REMOTE_EFFECT = 0x048E;
    public static final int REMOTE_SET_TEMPORARY_STAT = 0x048F;
    public static final int REMOTE_RESET_TEMPORARY_STAT = 0x0490;

    private Tms282UserRemotePackets() {
    }

    public static OutPacket move(int characterId, MovementInfo path) {
        return move(characterId, path, Tms282MovePathCodec.INSTANCE);
    }

    public static OutPacket move(int characterId, MovementInfo path, MovePathCodec paths) {
        Objects.requireNonNull(path, "path");
        OutPacket out = Tms282LoginPacketSupport.packet(REMOTE_MOVE);
        out.encodeInt((long) characterId);
        paths.encodeBroadcast(out, path);
        return out;
    }

    /** LP048F for the stats of {@code selection} (from {@link Tms282RemoteStatEncoder#select}, not empty). */
    public static OutPacket setTemporaryStat(int characterId, Tms282RemoteStatEncoder encoder,
                                             TemporaryStatEncoder.Selection selection, long nowMillis) {
        Objects.requireNonNull(encoder, "encoder");
        Objects.requireNonNull(selection, "selection");
        if (selection.isEmpty()) {
            throw new IllegalArgumentException("REMOTE_SET_TEMPORARY_STAT without stats");
        }
        OutPacket out = Tms282LoginPacketSupport.packet(REMOTE_SET_TEMPORARY_STAT);
        out.encodeInt((long) characterId);
        encoder.encodeForRemote(out, selection, nowMillis, activeSwords(characterId));
        out.encodeShort(0);    // delay (5ADDB4F)
        out.encodeByte(0);     // 5ADDB5A
        return out;
    }

    private static net.swordie.ms.client.character.skills.StopForceAtom activeSwords(int characterId) {
        if (!Tms282RemoteBuffs.active()) return null;
        // Resolve the live character on every packet; a reconnect with the same ID cannot inherit a cached snapshot.
        var chr = net.swordie.ms.Server.get().getChars().get(characterId);
        if (chr == null) return null;
        var tsm = chr.getTemporaryStatManager();
        var stat = net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.StopForceAtomInfo;
        var swords = tsm.hasStat(stat) ? tsm.getOption(stat).stopForceAtom : null;
        return swords == null ? null : swords.deepCopy();
    }

    /** LP0490 for {@code reset.reset()} (from {@link Tms282RemoteStatEncoder#selectReset}, not empty). */
    public static OutPacket resetTemporaryStat(int characterId, Tms282RemoteStatEncoder encoder,
                                               Tms282RemoteStatEncoder.Reset reset, long nowMillis) {
        Objects.requireNonNull(encoder, "encoder");
        Objects.requireNonNull(reset, "reset");
        if (reset.reset().isEmpty()) {
            throw new IllegalArgumentException("REMOTE_RESET_TEMPORARY_STAT without stats");
        }
        OutPacket out = Tms282LoginPacketSupport.packet(REMOTE_RESET_TEMPORARY_STAT);
        out.encodeInt((long) characterId);
        encoder.encodeReset(out, reset, nowMillis);
        return out;
    }

    /** LP048E: {@code effect} shown on character {@code characterId} to the other users. */
    public static OutPacket effect(int characterId, Tms282EffectPackets.UserEffect effect) {
        Objects.requireNonNull(effect, "effect");
        OutPacket out = Tms282LoginPacketSupport.packet(REMOTE_EFFECT);
        out.encodeInt((long) characterId);
        effect.encode(out);
        return out;
    }
}
