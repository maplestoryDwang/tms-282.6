package net.swordie.ms.handlers.login.tms282;
import net.swordie.ms.connection.packet.Tms282ZeroAssistRequest;
import net.swordie.ms.connection.packet.Tms282ZeroPackets;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.handlers.header.InHeader;

/** Verified assist1→2 sender/remote-handler symmetry; CP01DF tag counters remain record-only. */
final class Tms282ZeroHandlers {
    void register(Tms282Routes routes) {
        routes.channel(Tms282ZeroAssistRequest.OPCODE, InHeader.ZERO_LAST_ASSIST_STATE).when(Tms282Guards.IN_FIELD)
                .handle("Zero assist end -> native LP04A4 (remote assist1->2)", this::assistEnd);
    }
    private void assistEnd(Tms282LoginState session, int op, byte[] packet) {
        try {Tms282ZeroAssistRequest.validate(packet);}
        catch (IllegalArgumentException e) {Tms282SocialHandlers.rejected(op, packet, e); return;}
        var chr = session.fieldCharacter();
        if (!JobConstants.isZero(chr.getJob())) {
            System.out.printf("[TMS282] CP01E1 ignored for non-Zero char=%d job=%d%n", chr.getId(), chr.getJob()); return;
        }
        chr.getField().broadcast(Tms282ZeroPackets.assistEnd(chr.getId()), chr);
    }
}
