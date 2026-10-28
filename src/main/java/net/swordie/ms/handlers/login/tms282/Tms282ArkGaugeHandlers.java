package net.swordie.ms.handlers.login.tms282;

import java.util.function.LongSupplier;
import net.swordie.ms.client.jobs.flora.Ark;
import net.swordie.ms.connection.packet.Tms282ArkGaugeRequest;
import net.swordie.ms.handlers.header.InHeader;

/** The native one-second Ark resource pulse; the server's gauge and job state remain authoritative. */
final class Tms282ArkGaugeHandlers {
    private final LongSupplier clock;
    Tms282ArkGaugeHandlers(Tms282LoginContext context) { clock=context.fieldServices().skills().clock(); }
    void register(Tms282Routes routes) {
        routes.channel(Tms282ArkGaugeRequest.OPCODE,InHeader.UPDATE_ARK_SPECTRA_ENERGY).when(Tms282Guards.IN_FIELD)
                .handle("owned Ark resource pulse -> authoritative SpecterGauge",(s,op,p) -> {
                    var chr=s.fieldCharacter();
                    try {
                        Tms282ArkGaugeRequest.parse(p);
                        if(!(chr.getJobHandler() instanceof Ark ark))throw new IllegalArgumentException("Non-Ark gauge pulse");
                        ark.pulseTms282SpectraEnergy(clock.getAsLong());
                    } catch(IllegalArgumentException e) { Tms282SocialHandlers.rejected(op,p,e); }
                });
    }
}
