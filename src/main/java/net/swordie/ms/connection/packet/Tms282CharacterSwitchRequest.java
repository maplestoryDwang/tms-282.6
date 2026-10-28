package net.swordie.ms.connection.packet;

import java.nio.charset.Charset;
import java.util.Objects;

/**
 * CP04A7, the in-game "change character" request of TMS 282.3 [opcode and layout verified statically: sender 5FAB150
 * f(ctx, charId, secondaryPassword, flag) builds COutPacket(0x4A7) at 5FAB2D5: Encode4 charId, EncodeStr, Encode1
 * flag, then 346A070; before sending it sets ctx+0x4138 = 1 (switch pending, cleared by the LP028D reply 5FAB390).
 * The sender refuses locally when charId is the current character (5DF2BA0) with SP[0x460C] 「無法以等待刪除中的角色
 * 登入。」 for a character pending deletion and SP[0x460D] 「在目前的伺服器上無法使用。」]. Live 2026-09-24
 * (formal-channel-1790193171279-3): {@code a704 22000000 0000 00} = character 34, empty password, flag 0; without a
 * reply the client stayed waiting ("換角色也卡住").
 * <pre>
 * i32 characterId       the character to switch to (same account and world)
 * str secondaryPassword the second password as typed; empty when the client skips it
 * u8  flag              echoed in LP028D; 0 makes the client show SP[0x4345] (second password skipped notice)
 * </pre>
 */
public record Tms282CharacterSwitchRequest(int characterId, String secondaryPassword, int flag) {
    public static final int OPCODE = 0x04A7;

    public Tms282CharacterSwitchRequest {
        Objects.requireNonNull(secondaryPassword, "secondaryPassword");
    }

    /** The full plaintext packet, including the opcode. */
    public static Tms282CharacterSwitchRequest parse(byte[] packet, Charset charset) {
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, OPCODE, "CP04A7");
        int characterId = in.i32();
        String password = in.str(charset);
        int flag = in.u8();
        in.end();
        return new Tms282CharacterSwitchRequest(characterId, password, flag);
    }

    /** Never prints the password, only whether one was typed. */
    @Override
    public String toString() {
        return String.format("CP04A7 change character to %d (secondary password %s, flag %d)", characterId,
                secondaryPassword.isEmpty() ? "empty" : "typed", flag);
    }
}
