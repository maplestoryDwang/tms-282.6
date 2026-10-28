package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;

import java.nio.charset.Charset;
import java.util.Objects;

/**
 * LP028D, the reply to CP04A7 change character [handler 5FAB390 via CWvsContext case 5DF95FE, static; reads
 * {@code u8 result} (F8CE50) first and clears ctx+0x4138 on every path]:
 * <ul>
 *     <li>0 accepted: {@code i32 characterId, u8 flag, str secondaryPassword}. The client checks the id again (not the
 *     current one, both ids known, target not pending deletion), shows SP[0x4345] when flag is 0, then 5E647C0 stores
 *     the target (5E56560), sends CP077C and leaves the field to log in with that character;</li>
 *     <li>1: SP[0xC50] 「請確認第2道密碼後再試一次。」 (second password wrong);</li>
 *     <li>2: SP[0x379B] 「目前無法進行。」;</li>
 *     <li>other: nothing shown, only the pending flag cleared.</li>
 * </ul>
 */
public final class Tms282CharacterSwitchPackets {
    public static final int OPCODE = 0x028D;
    public static final int ACCEPTED = 0;
    public static final int WRONG_SECONDARY_PASSWORD = 1;
    public static final int UNAVAILABLE = 2;

    private Tms282CharacterSwitchPackets() {
    }

    /** LP015F: unlike 265, 5E27897 reads target even when the cookie is empty. Keep existing login credentials. */
    public static OutPacket relogin(int characterId, String secondaryPassword, Charset charset) {
        OutPacket out=Tms282LoginPacketSupport.packet(0x015F);
        Tms282LoginPacketSupport.str(out,"",charset);
        out.encodeInt(characterId);
        if(characterId!=0) Tms282LoginPacketSupport.str(out,secondaryPassword,charset);
        return out;
    }

    /** LP015F stores these bytes directly at ctx+40F0 (5FAB5C0). LP0013 auto-reselect first
     * rotates them left4 (3CA7852..3CA791F), then applies its random CP0079 rotation. */
    public static OutPacket reloginHandoff(int characterId, String token) {
        return reloginHandoff(characterId, token, "");
    }

    /** Nonempty cookie selects native CP0073 authentication and automatic CP0077. */
    public static OutPacket reloginHandoff(int characterId, String token, String authCookie) {
        if (authCookie == null || (!authCookie.isEmpty() && !authCookie.matches("[A-Za-z0-9_-]{32}")))
            throw new IllegalArgumentException("Invalid relogin cookie");
        if (characterId <= 0 || token == null || !token.matches("[A-Za-z0-9_-]{16}"))
            throw new IllegalArgumentException("Invalid handoff identity/credential");
        byte[] bytes = token.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 255;
            bytes[i] = (byte) ((value << 4) | (value >>> 4));
        }
        OutPacket out = Tms282LoginPacketSupport.packet(0x015F);
        Tms282LoginPacketSupport.str(out, authCookie, java.nio.charset.StandardCharsets.US_ASCII);
        out.encodeInt(characterId);
        out.encodeShort(bytes.length);
        out.encodeArr(bytes); // Binary ZXString bytes, never a charset round trip.
        return out;
    }

    public static OutPacket accepted(int characterId, int flag, String secondaryPassword, Charset charset) {
        OutPacket out = Tms282LoginPacketSupport.packet(OPCODE);
        out.encodeByte(ACCEPTED);
        out.encodeInt(characterId);
        out.encodeByte(flag);
        Tms282LoginPacketSupport.str(out, Objects.requireNonNull(secondaryPassword, "secondaryPassword"), charset);
        return out;
    }

    /** {@link #WRONG_SECONDARY_PASSWORD}, {@link #UNAVAILABLE} or another non-zero code: no body. */
    public static OutPacket refused(int result) {
        if (result == ACCEPTED) {
            throw new IllegalArgumentException("refused() needs a non-zero result");
        }
        OutPacket out = Tms282LoginPacketSupport.packet(OPCODE);
        out.encodeByte(result);
        return out;
    }
}
