package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.jobs.flora.Illium;
import net.swordie.ms.connection.packet.Tms282ForceAtomUpdateRequest;
import net.swordie.ms.handlers.header.InHeader;

/** Only the captured kind 2 is implemented; the other native atom subclasses have different payloads. */
final class Tms282AtomStateHandlers {
    void register(Tms282Routes routes) {
        routes.channel(Tms282ForceAtomUpdateRequest.OPCODE, InHeader.CP03EE).when(Tms282Guards.IN_FIELD)
                .handle("owned Illium javelin target state -> LP032E", (s, op, p) -> {
                    var chr = s.fieldCharacter();
                    try {
                        var r = Tms282ForceAtomUpdateRequest.parse(p);
                        if (!(chr.getJobHandler() instanceof Illium illium))
                            throw new IllegalArgumentException("Non-Illium javelin update");
                        illium.updateTms282Javelin(r);
                    } catch (IllegalArgumentException e) {
                        Tms282SocialHandlers.rejected(op, p, e);
                    }
                });
    }
}
