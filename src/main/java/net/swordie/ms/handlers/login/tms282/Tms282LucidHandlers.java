package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.user.UserHandler;

import java.util.Arrays;

/**
 * Lucid boss client requests of TMS 282.3 (research/tms2823/lucid-re-0930). None of the three senders is in the
 * SendPacket 346A070 tail-u32 list (predicate 347ECF0 emulated: 0x0616 / 0x0617 / 0x0618 -> no tail).
 * <ul>
 *     <li>CP0616 (sender 38EC900, called from both Lucid field update loops 38F1360 / 38FA960): empty body, sent
 *     when the local user's hit box overlaps a fairy-dust object (576BC40 count &gt; 0) = 265
 *     LUCID_FAIRY_DUST_CHECK (推定). The 265 server has no handler: acknowledged and ignored (the client applies
 *     the hit itself).</li>
 *     <li>CP0617 (senders 3D82380 / 3D83860, statue "horn" casting bar): {@code u8 1} = 265
 *     LUCID_ACTIVATE_STATUE_REQUEST (推定強; UNVERIFIED row 2026-09-23) -> 265
 *     {@link UserHandler#handleLucidActivateStatueRequest} (reads nothing).</li>
 *     <li>CP0618 (senders 3901B80 and 3904F40 on LP0825 type 6): empty body = 265 LUCIDE_WELCOME_BARRAGE_END
 *     (推定). No 265 handler: ignored.</li>
 * </ul>
 * Registration: {@link Tms282BossMechHandlers} (bossmech1-0930), itself registered once in Tms282LoginOverlay.
 */
final class Tms282LucidHandlers {
    static final int FAIRY_DUST_CHECK = 0x0616;
    static final int ACTIVATE_STATUE = 0x0617;
    static final int WELCOME_BARRAGE_END = 0x0618;

    void register(Tms282Routes routes) {
        routes.channel(FAIRY_DUST_CHECK, InHeader.LUCID_FAIRY_DUST_CHECK).when(Tms282Guards.IN_FIELD)
                .handle("Lucid fairy dust hit (no 265 handler, ignored)", (s, op, p) -> expect(op, p, 2));
        routes.channel(ACTIVATE_STATUE, InHeader.LUCID_ACTIVATE_STATUE_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("Lucid statue activation -> 265 UserHandler.handleLucidActivateStatueRequest", (s, op, p) -> {
                    if (!expect(op, p, 3)) {
                        return;
                    }
                    Char chr = s.fieldCharacter();
                    if (chr.getField() == null || chr.getField().getId() / 1000 != 450004) {
                        return; // only the Lucid battle maps 450004xxx have statues
                    }
                    Tms282SocialHandlers.guarded(chr, op,
                            () -> UserHandler.handleLucidActivateStatueRequest(chr, new InPacket(new byte[0])));
                });
        routes.channel(WELCOME_BARRAGE_END, InHeader.LUCIDE_WELCOME_BARRAGE_END).when(Tms282Guards.IN_FIELD)
                .handle("Lucid welcome barrage end (no 265 handler, ignored)", (s, op, p) -> expect(op, p, 2));
    }

    private static boolean expect(int op, byte[] packet, int length) {
        if (packet.length != length) {
            System.out.printf("[TMS282] REJECTED CP%04X len=%d (expected %d): %s%n", op, packet.length, length,
                    packet.length <= 32 ? Arrays.toString(packet) : "");
            return false;
        }
        return true;
    }
}
