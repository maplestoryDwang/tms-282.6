package net.swordie.ms.connection.protocol;

/** How a profile's packet body relates to the GMS 265 layout of the same logical packet. */
public enum WireFormat {
    /** Body is byte-compatible with the 265 builder/handler; only the opcode may differ. */
    SAME_AS_265,
    /** Body differs from 265; only a codec written for the profile may build or parse it. */
    PROFILE_CODEC,
    /** Body layout not established; treated like {@link #PROFILE_CODEC} for outbound (fail-closed). */
    UNKNOWN
}
