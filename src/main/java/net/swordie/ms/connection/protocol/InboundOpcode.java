package net.swordie.ms.connection.protocol;

import net.swordie.ms.handlers.header.InHeader;

import java.util.Objects;

/**
 * One client-to-server opcode of a profile.
 *
 * @param op       wire opcode (0..0xFFFF)
 * @param name     265 enum name, or a profile-only name when {@code header} is null
 * @param header   the InHeader the dispatcher routes to; null when the server has no constant for it yet
 * @param format   body layout relative to 265
 * @param evidence why the opcode is known
 */
public record InboundOpcode(int op, String name, InHeader header, WireFormat format, OpcodeEvidence evidence) {
    public InboundOpcode {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(evidence, "evidence");
    }

    @Override
    public String toString() {
        return String.format("CP%04X %s %s %s", op & 0xFFFF, name, format, evidence);
    }
}
