package net.swordie.ms.handlers.login.tms282;

import java.util.function.LongSupplier;
import net.swordie.ms.client.jobs.flora.Illium;
import net.swordie.ms.connection.packet.Tms282IlliumTimerRequest;
import net.swordie.ms.handlers.header.InHeader;

/** CP0409 is shared with Adele; the common start route dispatches this exact Illium key separately. */
final class Tms282IlliumTimerHandlers {
    private final LongSupplier clock;
    Tms282IlliumTimerHandlers(Tms282LoginContext context) { clock = context.fieldServices().skills().clock(); }

    void register(Tms282Routes routes) {
        routes.channel(Tms282IlliumTimerRequest.PULSE_OPCODE, InHeader.CP0408).when(Tms282Guards.IN_FIELD)
                .handle("owned crystal timer -> server charge and native pending rearm", this::handle);
    }

    private void handle(Tms282LoginState session, int op, byte[] packet) {
        try {
            var character = session.fieldCharacter();
            if (character != null && character.getJobHandler() instanceof net.swordie.ms.client.jobs.legend.Aran aran) {
                var request = net.swordie.ms.connection.packet.Tms282AranTimerRequest.parse(packet);
                aran.getPeriodicCombo().tick(request.skillId(), clock.getAsLong());
                return;
            }
            var request = Tms282IlliumTimerRequest.parse(packet);
            if (character == null || !(character.getJobHandler() instanceof Illium illium))
                throw new IllegalArgumentException("Non-Illium crystal timer owner");
            illium.pulseTms282PassiveCrystalCharge(request, clock.getAsLong());
        } catch (IllegalArgumentException e) { Tms282SocialHandlers.rejected(op, packet, e); }
    }
}
