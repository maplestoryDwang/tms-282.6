package net.swordie.ms.connection.protocol;

import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable opcode map of one protocol profile, built with {@link #builder(ProtocolVersion)}.
 * <ul>
 *     <li>Inbound: wire opcode -> {@link InboundOpcode} (and the {@link InHeader} the dispatcher uses).</li>
 *     <li>Outbound: {@link OutHeader} -> wire opcode and {@link WireFormat}; wire opcode -> {@link OutboundOpcode}.</li>
 * </ul>
 * An OutHeader without an entry has no wire opcode in this profile: {@link #wire(OutHeader)} returns
 * {@link #NO_OPCODE} and {@code OutPacket(OutHeader)} marks the packet suppressed.
 */
public final class OpcodeTable {
    /** {@link #wire(OutHeader)} result for a header the profile cannot send. Never a valid short. */
    public static final int NO_OPCODE = Integer.MIN_VALUE;
    private static final int OP_SPACE = 0x10000;

    private final ProtocolVersion version;
    private final InboundOpcode[] inboundByOp;
    private final OutboundOpcode[] outboundByOp;
    private final OutboundOpcode[] outboundByHeader;
    private final int[] wireByHeader;
    private final List<InboundOpcode> inbound;
    private final List<OutboundOpcode> outbound;

    private OpcodeTable(Builder b) {
        this.version = b.version;
        this.inboundByOp = b.inboundByOp.clone();
        this.outboundByOp = b.outboundByOp.clone();
        this.outboundByHeader = b.outboundByHeader.clone();
        this.wireByHeader = b.wireByHeader.clone();
        this.inbound = List.copyOf(b.inbound);
        this.outbound = List.copyOf(b.outbound);
    }

    public static Builder builder(ProtocolVersion version) {
        return new Builder(version);
    }

    public ProtocolVersion version() {
        return version;
    }

    /** Inbound entry for a wire opcode as decoded ({@code short} or unsigned). */
    public Optional<InboundOpcode> inbound(int op) {
        return Optional.ofNullable(inboundByOp[op & 0xFFFF]);
    }

    /** Dispatch header for a wire opcode, or null (unknown opcode, or no server constant yet). Hot path. */
    public InHeader inHeader(int op) {
        InboundOpcode entry = inboundByOp[op & 0xFFFF];
        return entry == null ? null : entry.header();
    }

    /** Wire opcode of a 265 builder header, or {@link #NO_OPCODE}. Hot path (every OutPacket). */
    public int wire(OutHeader header) {
        return wireByHeader[header.ordinal()];
    }

    public boolean canSend(OutHeader header) {
        return wire(header) != NO_OPCODE;
    }

    /** Body format of a header in this profile; {@link WireFormat#UNKNOWN} when it has no opcode. */
    public WireFormat format(OutHeader header) {
        OutboundOpcode entry = outboundByHeader[header.ordinal()];
        return entry == null ? WireFormat.UNKNOWN : entry.format();
    }

    public Optional<OutboundOpcode> outbound(OutHeader header) {
        return Optional.ofNullable(outboundByHeader[header.ordinal()]);
    }

    /** First outbound entry registered for a wire opcode (debug naming). */
    public Optional<OutboundOpcode> outbound(int op) {
        return Optional.ofNullable(outboundByOp[op & 0xFFFF]);
    }

    public List<InboundOpcode> inboundOpcodes() {
        return inbound;
    }

    public List<OutboundOpcode> outboundOpcodes() {
        return outbound;
    }

    @Override
    public String toString() {
        return "OpcodeTable[" + version.id() + ", inbound=" + inbound.size() + ", outbound=" + outbound.size() + "]";
    }

    /** Collects entries and rejects inconsistent ones; {@link #build()} freezes a copy. */
    public static final class Builder {
        private final ProtocolVersion version;
        private final InboundOpcode[] inboundByOp = new InboundOpcode[OP_SPACE];
        private final OutboundOpcode[] outboundByOp = new OutboundOpcode[OP_SPACE];
        private final OutboundOpcode[] outboundByHeader = new OutboundOpcode[OutHeader.values().length];
        private final int[] wireByHeader = new int[OutHeader.values().length];
        private final List<InboundOpcode> inbound = new ArrayList<>();
        private final List<OutboundOpcode> outbound = new ArrayList<>();
        private boolean legacyOutboundValues;

        private Builder(ProtocolVersion version) {
            this.version = Objects.requireNonNull(version, "version");
            Arrays.fill(wireByHeader, NO_OPCODE);
        }

        /**
         * Accepts the GMS 265 OutHeader quirks unchanged: negative values ({@code UnkValue} = -1, written as
         * FF FF) and several headers sharing one value (the reverse lookup keeps the first, like
         * {@code OutHeader.getOutHeaderByOp}). Only the 265 baseline table may use this.
         */
        public Builder allowLegacyOutboundValues() {
            this.legacyOutboundValues = true;
            return this;
        }

        /** Inbound entry already added for {@code op}, if any (profiles merging declared opcodes). */
        public Optional<InboundOpcode> inboundAt(int op) {
            return Optional.ofNullable(inboundByOp[op & 0xFFFF]);
        }

        /** @throws IllegalArgumentException for an opcode outside 0..0xFFFF or registered twice */
        public Builder inbound(InboundOpcode entry) {
            Objects.requireNonNull(entry, "entry");
            if (entry.op() < 0 || entry.op() > 0xFFFF) {
                throw new IllegalArgumentException("Inbound opcode out of range: " + entry);
            }
            InboundOpcode previous = inboundByOp[entry.op()];
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate inbound opcode: " + previous + " / " + entry);
            }
            inboundByOp[entry.op()] = entry;
            inbound.add(entry);
            return this;
        }

        /** @throws IllegalArgumentException for a bad opcode, a duplicate opcode or a header mapped twice */
        public Builder outbound(OutboundOpcode entry) {
            Objects.requireNonNull(entry, "entry");
            int op = entry.op();
            if (legacyOutboundValues ? op < Short.MIN_VALUE || op > 0xFFFF : op < 0 || op > 0xFFFF) {
                throw new IllegalArgumentException("Outbound opcode out of range: " + entry);
            }
            OutHeader header = entry.header();
            if (header != null) {
                OutboundOpcode previous = outboundByHeader[header.ordinal()];
                if (previous != null) {
                    throw new IllegalArgumentException("OutHeader mapped twice: " + previous + " / " + entry);
                }
            }
            OutboundOpcode sameOp = outboundByOp[op & 0xFFFF];
            if (sameOp != null && !legacyOutboundValues) {
                throw new IllegalArgumentException("Duplicate outbound opcode: " + sameOp + " / " + entry);
            }
            if (sameOp == null) {
                outboundByOp[op & 0xFFFF] = entry;
            }
            if (header != null) {
                outboundByHeader[header.ordinal()] = entry;
                wireByHeader[header.ordinal()] = op;
            }
            outbound.add(entry);
            return this;
        }

        public OpcodeTable build() {
            return new OpcodeTable(this);
        }
    }
}
