package net.swordie.ms.connection.protocol;

/**
 * Why an opcode is in a table. The TMS values mirror the {@code status} column of
 * research/tms2823/packet-audit/opcode-status-282.tsv.
 */
public enum OpcodeEvidence {
    /** Taken unchanged from the existing InHeader/OutHeader enums (GMS 265 profile). */
    BASELINE,
    LIVE_VERIFIED,
    CAPTURED,
    IMPLEMENTED_UNTESTED,
    /** UNVERIFIED in the status table, but the in-game map marked the 265 mapping 【證實】. */
    STATIC_CONFIRMED,
    /**
     * UNVERIFIED in the status table and not generated into the tables: the opcode is in a profile only because
     * its handler overlay declares it (the research probe answers or records it; layout static only).
     */
    UNVERIFIED;

    /** Evidence strong enough for the phase-0 skeleton (login and channel entry). */
    public boolean isLoginOrChannelKnown() {
        return this == LIVE_VERIFIED || this == CAPTURED || this == IMPLEMENTED_UNTESTED;
    }
}
