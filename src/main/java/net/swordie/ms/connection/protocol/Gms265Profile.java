package net.swordie.ms.connection.protocol;

import net.swordie.ms.handlers.dispatch.HandlerRegistry;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.Map;

/**
 * The existing server protocol as one profile: identity opcodes taken from InHeader/OutHeader, the former
 * acceptor handshake, the former PacketEncoder cipher rule, a pass-through guard and no overlay. Selecting it
 * must leave every byte unchanged (Gms265GoldenPacketCheck).
 */
public final class Gms265Profile implements ProtocolProfile {
    public static final Gms265Profile INSTANCE = new Gms265Profile();

    private final OpcodeTable opcodes = baselineTable();
    private final HandlerRegistry overlay = HandlerRegistry.empty("gms265-overlay");

    private Gms265Profile() {
    }

    private static OpcodeTable baselineTable() {
        OpcodeTable.Builder b = OpcodeTable.builder(ProtocolVersion.GMS_265).allowLegacyOutboundValues();
        for (Map.Entry<Short, InHeader> e : InHeader.opcodeMap().entrySet()) {
            InHeader h = e.getValue();
            b.inbound(new InboundOpcode(e.getKey() & 0xFFFF, h.name(), h, WireFormat.SAME_AS_265, OpcodeEvidence.BASELINE));
        }
        for (OutHeader h : OutHeader.values()) {
            if (OutHeader.isTms282Only(h)) {
                continue; // 282 names (UnkValue) have no 265 opcode: new OutPacket(h) is suppressed under 265
            }
            b.outbound(new OutboundOpcode(h.getValue(), h.name(), h, WireFormat.SAME_AS_265, OpcodeEvidence.BASELINE));
        }
        return b.build();
    }

    @Override
    public ProtocolVersion version() {
        return ProtocolVersion.GMS_265;
    }

    @Override
    public OpcodeTable opcodes() {
        return opcodes;
    }

    @Override
    public HandshakeStrategy handshake() {
        return Gms265HandshakeStrategy.INSTANCE;
    }

    @Override
    public CipherPolicy cipherPolicy() {
        return Gms265CipherPolicy.INSTANCE;
    }

    @Override
    public OutboundGuard outboundGuard() {
        return Gms265OutboundGuard.INSTANCE;
    }

    @Override
    public ProfileCodecs codecs() {
        return ProfileCodecs.none();
    }

    @Override
    public HandlerRegistry handlerOverlay() {
        return overlay;
    }

    @Override
    public String toString() {
        return "Gms265Profile" + opcodes;
    }
}
