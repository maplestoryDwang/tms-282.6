package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.world.tms282.contents.changeburning.dream.DreamPackets;
import net.swordie.ms.world.tms282.contents.changeburning.dream.DreamService;

/**
 * CP02D3 露希妲物品欄 request (senders 683C50..685600, static only) on the channel socket, in field ->
 * {@link DreamService#onRequest}, which always answers LP02C8 with exclusiveReset = 1 (every sender locks the client
 * until then). A body that does not parse is answered the same way so the client is unlocked.
 * Registration: {@code new Tms282DreamEquipmentHandlers().register(collector);} in {@code Tms282LoginOverlay}.
 */
final class Tms282DreamEquipmentHandlers {

    void register(Tms282Routes routes) {
        routes.channel(DreamPackets.REQUEST, InHeader.CP02D3).when(Tms282Guards.IN_FIELD)
                .handle("露希妲物品欄 request (夢幻的裝備)", this::request);
    }

    private void request(Tms282LoginState session, int op, byte[] packet) {
        var chr = session.fieldCharacter();
        DreamPackets.Request request;
        try {
            request = DreamPackets.parse(packet);
        } catch (IllegalArgumentException e) {
            Tms282SocialHandlers.rejected(op, packet, e);
            if (chr != null) {
                chr.write(DreamPackets.inventoryOperation(true, 0, 0, 0, java.util.List.of()));
            }
            return;
        }
        DreamService.get().onRequest(chr, request);
        System.out.printf("[TMS282] CP02D3 dream equipment sub=%d character=%s%n", request.sub(),
                chr == null ? "-" : String.valueOf(chr.getId()));
    }
}
