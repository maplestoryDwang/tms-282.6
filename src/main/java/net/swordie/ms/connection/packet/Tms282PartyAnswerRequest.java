package net.swordie.ms.connection.packet;

/**
 * CP01F3 PARTY_RESULT of TMS 282.3 (the answer to an invitation): {@code u8 type, Buf4 result, i32 inviter}
 * [證實: senders 2FD55AD / 2FD586B in CWvsContext::OnPartyResult and the invitation UI 3701CE0 / 3702F20].
 * Type 47 answers an invitation (282 PARTY_RESULT type 4), 48 an intrusion invitation (type 5), 49 an application
 * (type 8); result 5 = accept (3702424), 4 = decline (3703230), 1..3 = automatic refusals (2FD559D..2FD5593).
 * The 265 PartyHandler.handlePartyResult reads the same {@code i32 result, i32 inviter} after its type
 * Response_Invite_Success (46) with the same result values, so 47 and 48 become 265 type 46; 49 has no 265 path.
 */
public record Tms282PartyAnswerRequest(int type, int result, int inviterId) {
    public static final int OPCODE = 0x01F3;
    public static final int GMS265_INVITE_ANSWER = 46;

    public static Tms282PartyAnswerRequest parse(byte[] packet) {
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, OPCODE, "party answer");
        Tms282PartyAnswerRequest r = new Tms282PartyAnswerRequest(in.u8(), in.i32(), in.i32());
        in.end();
        return r;
    }

    /** True for an answer to an invitation (47, 48), which the 265 handler understands. */
    public boolean isInvitationAnswer() {
        return type == 47 || type == 48;
    }

    /** The 265 PARTY_RESULT body (no opcode). */
    public byte[] toGms265Body() {
        return new Gms265BodyWriter().u8(GMS265_INVITE_ANSWER).i32(result).i32(inviterId).bytes();
    }
}
