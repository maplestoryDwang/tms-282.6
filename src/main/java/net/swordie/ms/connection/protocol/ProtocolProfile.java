package net.swordie.ms.connection.protocol;

import net.swordie.ms.handlers.dispatch.HandlerRegistry;

/**
 * Everything that differs between wire protocols. Game logic never branches on the version; it asks the
 * active profile ({@link Protocol#active()}) for the part that differs.
 */
public interface ProtocolProfile {
    ProtocolVersion version();

    /** Opcode map and per-opcode body format. */
    OpcodeTable opcodes();

    /** Hello and unprompted packets of a new socket. */
    HandshakeStrategy handshake();

    /** Body encryption rule of PacketEncoder. */
    CipherPolicy cipherPolicy();

    /** Fail-closed filter at every send exit. */
    OutboundGuard outboundGuard();

    /** Profile-specific packet codecs (empty for 265: its builders are the base). */
    ProfileCodecs codecs();

    /**
     * Handlers that replace or add to the 265 base registry for this profile. Every inbound
     * {@link WireFormat#PROFILE_CODEC} opcode must be covered here ({@link HandlerRegistry#validate}).
     */
    HandlerRegistry handlerOverlay();
}
