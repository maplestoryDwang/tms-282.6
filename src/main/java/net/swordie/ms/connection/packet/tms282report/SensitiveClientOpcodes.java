package net.swordie.ms.connection.packet.tms282report;

import java.util.Set;

/**
 * Client opcodes that carry, or may carry, a credential or user-typed text of unknown meaning.
 * They are never parsed, stored or displayed as client reports: {@link Tms282ClientReport}
 * refuses to exist for them and {@link ClientReportParsers#parse} returns empty.
 * <ul>
 *     <li>CP0074 account name + password</li>
 *     <li>CP0079 select character (SPW)</li>
 *     <li>CP0090 / CP0091 delete character (SPW)</li>
 *     <li>CP00C3 SPW check before creation, CP00C4 SPW registration</li>
 *     <li>CP00B4 SPW setup dialog: account, new SPW, option (unknown-misc-cp-2823.md)</li>
 *     <li>CP00A2 resent after the OTP check passed: two strings (+4C8, +4D0) whose content is unknown</li>
 *     <li>CP0073 login environment: may carry the token of a token login (the str after CLogin+388)</li>
 *     <li>CP00DC text of an input box on the title screen; what the player typed there is unknown</li>
 * </ul>
 * Fields that are secret inside an otherwise reportable packet are marked by the parser instead
 * ({@link ReportFields#secret}); they are zeroed in the stored raw bytes.
 */
public final class SensitiveClientOpcodes {
    private static final Set<Integer> OPCODES = Set.of(0x0073, 0x0074, 0x0079, 0x0090, 0x0091, 0x00A2, 0x00B4,
            0x00C3, 0x00C4, 0x00DC);

    private SensitiveClientOpcodes() {
    }

    public static boolean contains(int opcode) {
        return OPCODES.contains(opcode);
    }

    /** Immutable set of every sensitive opcode. */
    public static Set<Integer> all() {
        return OPCODES;
    }
}
