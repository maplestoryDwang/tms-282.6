package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.world.tms282.contents.mushroompass.MushroomPassPackets;
import net.swordie.ms.world.tms282.contents.mushroompass.MushroomPassService;

/**
 * CP04FC 菇菇通行證 claim (window CUIHyperMushPass, sender 3269C5E; static only) on the channel socket, in field ->
 * {@link MushroomPassService#claim}. Opcode status: CP04FC UNVERIFIED until a live claim.
 * Registration: {@code new Tms282MushroomPassHandlers().register(collector);} in {@code Tms282LoginOverlay}.
 */
final class Tms282MushroomPassHandlers {

    void register(Tms282Routes routes) {
        routes.channel(MushroomPassPackets.CLAIM_REQUEST, InHeader.CP04FC).when(Tms282Guards.IN_FIELD)
                .handle("mushroom pass claim (CUIHyperMushPass)", this::claim);
    }

    private void claim(Tms282LoginState session, int op, byte[] packet) {
        MushroomPassPackets.ClaimRequest request;
        try {
            request = MushroomPassPackets.ClaimRequest.parse(packet);
        } catch (IllegalArgumentException e) {
            Tms282SocialHandlers.rejected(op, packet, e);
            return;
        }
        MushroomPassService.ClaimResult r = MushroomPassService.get().claim(session.fieldCharacter(), request);
        System.out.printf("[TMS282] CP04FC mushroom pass idx=%d basic=%s premium=%s -> %s%n", request.index(),
                request.basic(), request.premium(), r);
    }
}
