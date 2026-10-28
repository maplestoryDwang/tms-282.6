package net.swordie.ms.connection.protocol;

import net.swordie.ms.handlers.dispatch.HandlerRegistry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * TMS 282.3. The opcode table holds only the login and channel-entry opcodes with live, captured or implemented
 * evidence ({@link OpcodeEvidence#isLoginOrChannelKnown()}).
 * <ul>
 *     <li>{@link #skeleton}: phase 0, empty overlay and codecs; {@link HandlerRegistry#validate} refuses it (the 265
 *     login handlers would answer PROFILE_CODEC opcodes). Kept for the checks.</li>
 *     <li>Phase 1 (what {@code protocol.version=tms282.3} starts): the table plus the opcodes the login overlay
 *     declares, the overlay itself and a handshake that attaches the overlay's session state
 *     ({@code net.swordie.ms.handlers.login.tms282.Tms282Profiles#standard}).</li>
 * </ul>
 */
public final class Tms2823Profile implements ProtocolProfile {
    /** Rows loaded in phase 0. */
    public static final Predicate<InboundOpcode> PHASE0_INBOUND = e -> e.evidence().isLoginOrChannelKnown();
    public static final Predicate<OutboundOpcode> PHASE0_OUTBOUND = e -> e.evidence().isLoginOrChannelKnown();

    private final OpcodeTable opcodes;
    private final HandlerRegistry overlay;
    private final ProfileCodecs codecs;
    private final OutboundGuard guard;
    private final HandshakeStrategy handshake;

    public Tms2823Profile(OpcodeTable opcodes, HandlerRegistry overlay, ProfileCodecs codecs) {
        this(opcodes, overlay, codecs, Tms282HandshakeStrategy.INSTANCE);
    }

    /** @param handshake the 282 hello strategy, usually with the overlay's session-start listener */
    public Tms2823Profile(OpcodeTable opcodes, HandlerRegistry overlay, ProfileCodecs codecs, HandshakeStrategy handshake) {
        this.handshake = Objects.requireNonNull(handshake, "handshake");
        this.opcodes = Objects.requireNonNull(opcodes, "opcodes");
        if (opcodes.version() != ProtocolVersion.TMS_282_3) {
            throw new IllegalArgumentException("Not a TMS 282.3 table: " + opcodes);
        }
        this.overlay = Objects.requireNonNull(overlay, "overlay");
        this.codecs = Objects.requireNonNull(codecs, "codecs");
        this.guard = new FailClosedOutboundGuard(ProtocolVersion.TMS_282_3, opcodes);
    }

    /** Phase-0 skeleton from {@code <dataDir>/inbound.tsv} and {@code outbound.tsv}. */
    public static Tms2823Profile skeleton(Path dataDir) {
        try {
            OpcodeTable table = Tms282OpcodeTables.load(dataDir, PHASE0_INBOUND, PHASE0_OUTBOUND);
            return new Tms2823Profile(table, HandlerRegistry.empty("tms282.3-overlay"), ProfileCodecs.none());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load TMS 282.3 opcode tables from " + dataDir, e);
        }
    }

    @Override
    public ProtocolVersion version() {
        return ProtocolVersion.TMS_282_3;
    }

    @Override
    public OpcodeTable opcodes() {
        return opcodes;
    }

    @Override
    public HandshakeStrategy handshake() {
        return handshake;
    }

    @Override
    public CipherPolicy cipherPolicy() {
        return Tms282CipherPolicy.INSTANCE;
    }

    @Override
    public OutboundGuard outboundGuard() {
        return guard;
    }

    @Override
    public ProfileCodecs codecs() {
        return codecs;
    }

    @Override
    public HandlerRegistry handlerOverlay() {
        return overlay;
    }

    @Override
    public String toString() {
        return "Tms2823Profile" + opcodes;
    }
}
