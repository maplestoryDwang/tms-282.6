package net.swordie.ms.connection.protocol;

import net.swordie.ms.handlers.header.OutHeader;

import java.util.Objects;

/**
 * One server-to-client opcode of a profile.
 *
 * @param op       wire opcode written by {@code OutPacket(OutHeader)}
 * @param name     265 enum name, or a profile-only name when {@code header} is null
 * @param header   the OutHeader 265 builders use for it; null when only a profile codec can build it
 * @param format   body layout relative to 265
 * @param evidence why the opcode is known
 */
public record OutboundOpcode(int op, String name, OutHeader header, WireFormat format, OpcodeEvidence evidence) {
    public OutboundOpcode {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(evidence, "evidence");
    }

    @Override
    public String toString() {
        return String.format("LP%04X %s %s %s", op & 0xFFFF, name, format, evidence);
    }
}
