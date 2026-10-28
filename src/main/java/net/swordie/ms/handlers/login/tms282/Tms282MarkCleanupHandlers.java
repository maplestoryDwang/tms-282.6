package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.jobs.resistance.WildHunter;
import net.swordie.ms.connection.packet.Tms282MarkCleanupRequest;
import net.swordie.ms.handlers.header.InHeader;

/** Expired/missing target notifications can prune the owner's book, never unexpired live targets. */
final class Tms282MarkCleanupHandlers {
    void register(Tms282Routes routes) {
        routes.channel(0x0466,InHeader.CP0466).when(Tms282Guards.IN_FIELD)
                .handle("Wild Hunter stale marked-target cleanup",(s,op,p)->{
                    try {
                        var r=Tms282MarkCleanupRequest.parse(p);var chr=s.fieldCharacter();
                        if(!(chr.getJobHandler() instanceof WildHunter hunter))throw new IllegalArgumentException("Non-Wild Hunter cleanup");
                        hunter.cleanupMarkedTargets282(r.skillId(),r.targets(),System.currentTimeMillis());
                    } catch(IllegalArgumentException e){Tms282SocialHandlers.rejected(op,p,e);}
                });
    }
}
