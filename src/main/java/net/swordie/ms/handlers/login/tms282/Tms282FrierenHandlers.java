package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.world.tms282.collab.frieren.FrierenCollab;
import net.swordie.ms.world.tms282.collab.frieren.FrierenPackets;

/**
 * CP03BF skill ON/OFF (skill window right-click; static: senders 4F1C9A0 / 4F49150 / 4F62710, body i32 skillId,
 * no exclusive lock) on the channel socket, in field -> {@link FrierenCollab#onToggleRequest}. Other skills that
 * use the same request (the older hunting-event skills with additional_process 90) are logged and ignored.
 * Opcode status: CP03BF UNVERIFIED (static only) until a live right-click.
 * Registration: {@code new Tms282FrierenHandlers().register(collector);} in {@code Tms282LoginOverlay}.
 */
final class Tms282FrierenHandlers {

    void register(Tms282Routes routes) {
        routes.channel(FrierenPackets.SKILL_ON_OFF_REQUEST, InHeader.CP03BF).when(Tms282Guards.IN_FIELD)
                .handle("skill ON/OFF (Frieren collab 80012691 / 80012711)", this::skillOnOff);
    }

    private void skillOnOff(Tms282LoginState session, int op, byte[] packet) {
        FrierenPackets.SkillOnOffRequest request;
        try {
            request = FrierenPackets.SkillOnOffRequest.parse(packet);
        } catch (IllegalArgumentException e) {
            Tms282SocialHandlers.rejected(op, packet, e);
            return;
        }
        Char chr = session.fieldCharacter();
        boolean handled = FrierenCollab.get().onToggleRequest(chr, request.skillId());
        System.out.printf("[TMS282] CP03BF skill ON/OFF %d%s%n", request.skillId(), handled ? "" : " (ignored)");
    }
}
