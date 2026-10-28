package net.swordie.ms.connection.protocol.capture;

import net.swordie.ms.connection.packet.tms282report.SensitiveClientOpcodes;
import net.swordie.ms.connection.protocol.ProtocolProfile;
import net.swordie.ms.handlers.header.InHeader;

import java.util.EnumSet;
import java.util.Set;

/** Decides which received opcodes may only be stored redacted (opcode kept, body zeroed, length kept). */
@FunctionalInterface
public interface CaptureRedaction {
    /** 265 headers whose body carries an account name, password, PIC or SPW. */
    Set<InHeader> CREDENTIAL_HEADERS = EnumSet.of(InHeader.CHECK_LOGIN_AUTH_INFO, InHeader.CHECK_SPW_REQUEST,
            InHeader.CHECK_SPW_EXIST_REQUEST, InHeader.CHAR_SELECT, InHeader.SELECT_CHARACTER,
            InHeader.DELETE_CHARACTER, InHeader.RESERVED_DELETE_CHARACTER_CONFIRM, InHeader.SPECIAL_LOGIN_AUTH_REQUEST,
            InHeader.VIEW_REGISTER_PIC, InHeader.VIEW_SELECT_PIC);

    boolean mustRedact(int op);

    /**
     * {@link SensitiveClientOpcodes} (the TMS 282.3 credential/SPW/free-text opcodes, redacted in every profile
     * because the numbers are cheap to over-protect), plus any opcode the profile routes to a
     * {@link #CREDENTIAL_HEADERS} header.
     */
    static CaptureRedaction forProfile(ProtocolProfile profile) {
        return op -> {
            int unsigned = op & 0xFFFF;
            if (SensitiveClientOpcodes.contains(unsigned)) {
                return true;
            }
            InHeader header = profile.opcodes().inHeader(unsigned);
            return header != null && CREDENTIAL_HEADERS.contains(header);
        };
    }
}
