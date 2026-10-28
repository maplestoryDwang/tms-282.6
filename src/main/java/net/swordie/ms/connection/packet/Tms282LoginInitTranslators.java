package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.Optional;

/**
 * Login-time / field-entry 265 packets that the fail-closed guard dropped in the live capture
 * (formal-channel-1790176449461-3, WORK-STATE "DROPPED analysis"), in their 282 form. Each rule is admitted only
 * after the live 265 body was read CLEAN by the 282 handler (research/tms2823/interact-trace, progression phase
 * 2026-09-24; tools/Tms282InteractionCheck).
 * <ul>
 *     <li>SET_QUICK_MOVE_INFO -> LP0331 (CField handler 37806B0): the live 1608-byte 265 body (quick-move NPC list)
 *     is read CLEAN, 132 reads, nothing left [layout 證實; opcode = the map's +27 guess, confirmed by the unique
 *     exact read]. 2026-10-05: a CLEAN read is not enough: the 265 codes are outside the 282 EasyMove buttons and
 *     the strings were UTF-8, so the window opened empty. Both LP0331 and LP0296 are now rebuilt by
 *     {@link Tms282QuickMove} (282 button codes, strings in the 282 charset).</li>
 * </ul>
 * Not admitted (traced NOT CLEAN at the mapped guess, left dropped): USER_REG_DATE_RESULT (0x1EA reads nothing),
 * ACHIEVEMENT_UPDATE (0x23E reads a u32 after the u8), BUFF_FAVORITES_RESULT (0xB62 reads nothing),
 * UNION_ARTIFACT_INFORMATION (0x258 stops after 8 bytes), ENEMINES_DEFEATED_UPDATE (0xB4B), SET_MAPLE_POINT (0x14A
 * reads u8 first), SKILL_SEQUENCE_* (0x277..0x27A).
 */
public final class Tms282LoginInitTranslators implements Tms282LegacyPackets.Contributor {
    public static final Tms282LoginInitTranslators INSTANCE = new Tms282LoginInitTranslators();

    public static final int SET_QUICK_MOVE_INFO = Tms282QuickMove.SET_QUICK_MOVE_INFO;
    /** CWvsContext 5DFA4BA -> 4E52A00: u8 refresh, optional 37806B0 list, then open the window. */
    public static final int USER_OPEN_QUICK_MOVE = Tms282QuickMove.USER_OPEN_QUICK_MOVE;

    private Tms282LoginInitTranslators() {
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        java.nio.charset.Charset charset = builder.charset();
        builder.on(OutHeader.SET_QUICK_MOVE_INFO, p -> Tms282QuickMove.setQuickMoveInfo(p, charset));
        builder.on(OutHeader.USER_OPEN_QUICK_MOVE, p -> Tms282QuickMove.openQuickMove(p, charset));
    }

    static Optional<OutPacket> copy(OutPacket legacy, int op) {
        OutPacket out = Tms282LoginPacketSupport.packet(op);
        out.encodeArr(Tms282LegacyPackets.Body.of(legacy).rest());
        return Optional.of(out);
    }
}
