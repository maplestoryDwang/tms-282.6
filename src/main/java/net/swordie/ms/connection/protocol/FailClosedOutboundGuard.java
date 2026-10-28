package net.swordie.ms.connection.protocol;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.Objects;

/**
 * Guard for a non-265 profile. A packet passes only when
 * <ol>
 *     <li>a codec built it for this profile ({@link OutPacket#forProfile}), or</li>
 *     <li>a 265 builder built it from an OutHeader whose opcode exists here AND whose body is proven
 *     {@link WireFormat#SAME_AS_265}.</li>
 * </ol>
 * Everything else is dropped: suppressed packets (no opcode), 265 bodies with a different or unknown layout,
 * packets built for another profile, and raw-opcode packets ({@code new OutPacket(int)}), whose layout
 * nobody vouches for.
 */
public final class FailClosedOutboundGuard implements OutboundGuard {
    private final ProtocolVersion version;
    private final OpcodeTable opcodes;

    public FailClosedOutboundGuard(ProtocolVersion version, OpcodeTable opcodes) {
        this.version = Objects.requireNonNull(version, "version");
        this.opcodes = Objects.requireNonNull(opcodes, "opcodes");
    }

    @Override
    public OutboundDecision inspect(OutPacket packet) {
        if (packet.isSuppressed()) {
            return OutboundDecision.drop("no " + version.id() + " opcode for " + packet.getOrigin());
        }
        if (packet.getBuiltFor() != null) {
            return packet.getBuiltFor() == version
                    ? OutboundDecision.allow()
                    : OutboundDecision.drop("built for " + packet.getBuiltFor().id());
        }
        OutHeader origin = packet.getOrigin();
        if (origin == null) {
            return OutboundDecision.drop("raw opcode packet without a " + version.id() + " codec");
        }
        WireFormat format = opcodes.format(origin);
        if (format != WireFormat.SAME_AS_265) {
            return OutboundDecision.drop(origin + " body is " + format + " in " + version.id()
                    + "; needs a profile codec");
        }
        return OutboundDecision.allow();
    }
}
