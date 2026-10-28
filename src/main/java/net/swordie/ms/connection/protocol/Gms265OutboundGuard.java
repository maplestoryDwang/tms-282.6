package net.swordie.ms.connection.protocol;

import net.swordie.ms.connection.OutPacket;

/**
 * GMS 265: every packet the existing builders produce is by definition 265, so everything passes except a
 * packet built for another profile or suppressed (neither happens in a 265 process).
 */
public final class Gms265OutboundGuard implements OutboundGuard {
    public static final Gms265OutboundGuard INSTANCE = new Gms265OutboundGuard();

    private Gms265OutboundGuard() {
    }

    @Override
    public OutboundDecision inspect(OutPacket packet) {
        if (packet.isSuppressed()) {
            return OutboundDecision.drop("no opcode in this profile");
        }
        if (packet.getBuiltFor() != null && packet.getBuiltFor() != ProtocolVersion.GMS_265) {
            return OutboundDecision.drop("built for " + packet.getBuiltFor().id());
        }
        return OutboundDecision.allow();
    }
}
