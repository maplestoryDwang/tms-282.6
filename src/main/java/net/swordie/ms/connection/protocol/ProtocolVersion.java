package net.swordie.ms.connection.protocol;

import java.util.Locale;

/**
 * Wire protocols one server process can speak. A process speaks exactly one of them, chosen by
 * {@code protocol.version} (see {@link ProtocolConfig}).
 */
public enum ProtocolVersion {
    /** The existing GMS 265 based opcode set and packet layouts (default; behavior unchanged). */
    GMS_265("gms265", "265"),
    /** TMS 282.3: opcodes and layouts proven in research/tms2823/packet-audit. */
    TMS_282_3("tms282.3", "282.3", "282", "tms282");

    private final String id;
    private final String[] aliases;

    ProtocolVersion(String id, String... aliases) {
        this.id = id;
        this.aliases = aliases;
    }

    /** Config value and data directory name ({@code data/protocol/<id>}). */
    public String id() {
        return id;
    }

    /**
     * Parses a config value: the id, an alias or the enum name, case-insensitive.
     *
     * @throws IllegalArgumentException for an unknown value (a typo must never silently fall back)
     */
    public static ProtocolVersion parse(String value) {
        String v = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        for (ProtocolVersion version : values()) {
            if (version.id.equals(v) || version.name().toLowerCase(Locale.ROOT).equals(v)) {
                return version;
            }
            for (String alias : version.aliases) {
                if (alias.equals(v)) {
                    return version;
                }
            }
        }
        throw new IllegalArgumentException("Unknown protocol.version '" + value + "'");
    }
}
