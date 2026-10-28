package net.swordie.ms.handlers.login.tms282;

import java.util.function.LongSupplier;
import net.swordie.ms.client.jobs.flora.Adele;
import net.swordie.ms.connection.packet.Tms282AdeleTimerRequest;
import net.swordie.ms.handlers.header.InHeader;

/** The captured Aether Weaving timer; resource values and elapsed-time credit stay on the server. */
final class Tms282AdeleTimerHandlers {
    private final LongSupplier clock;
    Tms282AdeleTimerHandlers(Tms282LoginContext context) { clock = context.fieldServices().skills().clock(); }

    void register(Tms282Routes routes) {
        routes.channel(Tms282AdeleTimerRequest.START_OPCODE, InHeader.CP0409).when(Tms282Guards.IN_FIELD)
                .handle("owned Adele / Illium timer start", this::handle);
        routes.channel(Tms282AdeleTimerRequest.PULSE_OPCODE, InHeader.CP0407).when(Tms282Guards.IN_FIELD)
                .handle("owned Aether Weaving timer -> server aether and native pending rearm", this::handle);
    }

    private void handle(Tms282LoginState session, int op, byte[] packet) {
        try {
            var character = session.fieldCharacter();
            if (character != null && character.getJobHandler() instanceof net.swordie.ms.client.jobs.resistance.Xenon xenon) {
                xenon.supplyTimer().pulse(net.swordie.ms.connection.packet.Tms282XenonSupplyTimerRequest.parse(packet),
                        clock.getAsLong());
                return;
            }
            if (op == 0x0407 && character != null
                    && character.getJobHandler() instanceof net.swordie.ms.client.jobs.legend.Aran aran) {
                var request = net.swordie.ms.connection.packet.Tms282AranTimerRequest.parse(packet);
                aran.getPeriodicCombo().tick(request.skillId(), clock.getAsLong());
                return;
            }
            if (op == net.swordie.ms.connection.packet.Tms282IlliumTimerRequest.START_OPCODE
                    && character != null && character.getJobHandler() instanceof net.swordie.ms.client.jobs.flora.Illium illium) {
                illium.pulseTms282PassiveCrystalCharge(
                        net.swordie.ms.connection.packet.Tms282IlliumTimerRequest.parse(packet), clock.getAsLong());
                return;
            }
            var request = Tms282AdeleTimerRequest.parse(packet);
            if (character == null || !(character.getJobHandler() instanceof Adele adele))
                throw new IllegalArgumentException("Non-Adele timer owner");
            adele.pulseTms282PassiveAether(request, clock.getAsLong());
        } catch (IllegalArgumentException e) { Tms282SocialHandlers.rejected(op, packet, e); }
    }
}
