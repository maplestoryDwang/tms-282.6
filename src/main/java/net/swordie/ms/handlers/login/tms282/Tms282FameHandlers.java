package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.social.tms282.FameService;
import net.swordie.ms.handlers.header.InHeader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * CP0198 USER_GIVE_POPULARITY_REQUEST of TMS 282.3 on the channel socket after LP02EF ({@code when(IN_FIELD)}):
 * sender 5EE13A0, body i32 target character id, u8 1 = fame / 0 = defame (ingame-cp-map-2823 證實, sequence "4 1"
 * exact = 265). Handled by {@link FameService} (persisted fame history, daily + per-target limits); the 265 replies
 * (GIVE_POPULARITY_RESULT) go out as LP00AC through
 * {@link net.swordie.ms.connection.packet.Tms282PopularityTranslators}, the fame stat as LP009D.
 * Opcode status: CP0198 UNVERIFIED (static only) until a live fame.
 */
final class Tms282FameHandlers {
    static final int USER_GIVE_POPULARITY_REQUEST = 0x0198;

    void register(Tms282Routes routes) {
        routes.channel(USER_GIVE_POPULARITY_REQUEST, InHeader.USER_GIVE_POPULARITY_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("fame -> FameService (LP00AC replies)", this::givePopularity);
    }

    private void givePopularity(Tms282LoginState session, int op, byte[] packet) {
        if (packet.length < 2 + 5) {
            Tms282SocialHandlers.rejected(op, packet, new IllegalArgumentException("fame request too short"));
            return;
        }
        ByteBuffer b = ByteBuffer.wrap(packet, 2, packet.length - 2).order(ByteOrder.LITTLE_ENDIAN);
        int targetId = b.getInt();
        boolean increase = b.get() != 0;
        Char chr = session.fieldCharacter();
        System.out.printf("[TMS282] CP0198 fame %s target=%d%s%n", increase ? "+1" : "-1", targetId,
                packet.length != 7 ? " (len " + packet.length + ")" : "");
        Tms282SocialHandlers.guarded(chr, op, () -> FameService.get().give(chr, targetId, increase));
    }
}
