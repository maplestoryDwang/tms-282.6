package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.world.tms282.contents.punchking.PunchKingHudPackets;

/**
 * CP0685 "field ready" of the punch-king field class (fieldType 236; static: 39D03F0 -> 782A80, body i32 0, no
 * exclusive lock) on the channel socket, in field: logged and acknowledged by doing nothing, so it no longer shows as
 * an unhandled packet. Opcode status: CP0685 UNVERIFIED (static only) until a live entry into 993222200.
 * Registration: {@code new Tms282PunchKingHandlers().register(collector);} in {@code Tms282LoginOverlay}.
 */
final class Tms282PunchKingHandlers {

    void register(Tms282Routes routes) {
        routes.channel(PunchKingHudPackets.FIELD_READY_REQUEST, InHeader.CP0685).when(Tms282Guards.IN_FIELD)
                .handle("punch-king field ready (fieldType 236 HUD)", this::fieldReady);
    }

    private void fieldReady(Tms282LoginState session, int op, byte[] packet) {
        PunchKingHudPackets.FieldReadyRequest request;
        try {
            request = PunchKingHudPackets.FieldReadyRequest.parse(packet);
        } catch (IllegalArgumentException e) {
            Tms282SocialHandlers.rejected(op, packet, e);
            return;
        }
        Char chr = session.fieldCharacter();
        int field = chr == null || chr.getField() == null ? -1 : chr.getField().getId();
        System.out.printf("[TMS282] CP0685 punch-king field ready value=%d field=%d%s%n", request.value(), field,
                PunchKingHudPackets.isHudField(field) ? "" : " (not a fieldType-236 map)");
    }
}
