package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;

import java.nio.charset.Charset;
import java.util.Objects;

import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.DEFAULT_CHARSET;
import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.checkByte;
import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.str;

/**
 * TMS 282.3 login-screen UI control: request unlock (LP0043), notices (LP0041, LP0042, LP006E),
 * the login question (LP004C/004D), the lie-detector dialog (LP0049, answered by CP00CC), the
 * waiting dialog (LP0B4D), the login list (LP0060/0061), return to title (LP000E) and the login
 * background marker (LP007D).
 * See {@link Tms282LoginPacketSupport} for the opcodes deliberately left out.
 */
public final class Tms282LoginUiPackets {
    /** LP0049 result: any value except 0x46 (notice 0x0C only) opens a question; 0x45 as in LP0B1D. */
    private static final int LIE_DETECTOR_QUESTION = 0x45;

    private Tms282LoginUiPackets() {
    }

    /** LP0043 (inline): no payload; only clears the CLogin+0x220 request lock. */
    public static OutPacket unlockRequest() {
        return Tms282LoginPacketSupport.packet(0x0043);
    }

    /** LP0041 (inline): no payload; clears the request lock and shows the cash-item check failure notice. */
    public static OutPacket cashCheckFailed() {
        return Tms282LoginPacketSupport.packet(0x0041);
    }

    /**
     * LP0042 (3CB8C60): u8 kind; 0 -> no notice (success, e.g. CP00B1 rename accepted), 1 -> notice 3,
     * 2 -> notice 6, 3 -> notice 0x0C; always unlocks (unknown-busy-cp-2823.md).
     */
    public static OutPacket requestNotice(int kind) {
        if (kind < 0 || kind > 3) {
            throw new IllegalArgumentException("Request notice kind must be 0, 1, 2 or 3");
        }
        OutPacket out = Tms282LoginPacketSupport.packet(0x0042);
        out.encodeByte(kind);
        return out;
    }

    /** LP006E (3CAB770): u8 (value unused); closes the dialog, notice 0x1E, unlocks. */
    public static OutPacket genericFailureNotice() {
        OutPacket out = Tms282LoginPacketSupport.packet(0x006E);
        out.encodeByte(0);
        return out;
    }

    /**
     * LP004C (3CAEB50): u8 flag; flag != 0 then str question (client answers with CP00D4 [u8]).
     * A null question sends flag 0 (CLogin+0x240 = 0xBB9 path).
     */
    public static OutPacket loginQuestion(String question) {
        OutPacket out = Tms282LoginPacketSupport.packet(0x004C);
        out.encodeByte(question != null);
        if (question != null) {
            str(out, question, DEFAULT_CHARSET);
        }
        return out;
    }

    /** LP004D (inline): u8; clears the lock, non-zero sets CLogin+0x240 = 0xBBA. */
    public static OutPacket loginQuestionResult(int value) {
        checkByte(value, "Login question result");
        OutPacket out = Tms282LoginPacketSupport.packet(0x004D);
        out.encodeByte(value);
        return out;
    }

    /** LP0B4D (3CAF240) type 0: open the waiting dialog (Notice_New/notice_ani/2) with i32 a, i32 b. */
    public static OutPacket waitDialogOpen(int a, int b) {
        OutPacket out = Tms282LoginPacketSupport.packet(0x0B4D);
        out.encodeShort(0);
        out.encodeInt(a);
        out.encodeInt(b);
        return out;
    }

    /** LP0B4D type 3: update the counters of an open waiting dialog. */
    public static OutPacket waitDialogUpdate(int a, int b) {
        OutPacket out = Tms282LoginPacketSupport.packet(0x0B4D);
        out.encodeShort(3);
        out.encodeInt(a);
        out.encodeInt(b);
        return out;
    }

    /** LP0B4D type 2 or 4: close the waiting dialog and unlock. Type 1 enters VM code and is not offered. */
    public static OutPacket waitDialogClose(int type) {
        if (type != 2 && type != 4) {
            throw new IllegalArgumentException("Wait-dialog close type must be 2 or 4");
        }
        OutPacket out = Tms282LoginPacketSupport.packet(0x0B4D);
        out.encodeShort(type);
        return out;
    }

    /**
     * LP0060 (3C9A330), reply to CP00DE: nonce must echo the request; one page with no entries.
     * Layout: int nonce, int totalPages, int n1 {str,str,str,FT,int}, int n2 {str,str,str,FT}.
     */
    public static OutPacket emptyLoginList(int nonce) {
        OutPacket out = Tms282LoginPacketSupport.packet(0x0060);
        out.encodeInt(nonce);
        out.encodeInt(1);
        out.encodeInt(0);
        out.encodeInt(0);
        return out;
    }

    /** LP0061 (inline), low confidence: no payload; notice 9 when the LP0060 list UI is not open. */
    public static OutPacket loginListUnavailable() {
        return Tms282LoginPacketSupport.packet(0x0061);
    }

    /** LP000E (3CA3C30): no payload; returns to the login title (3C9FB50). */
    public static OutPacket returnToTitle() {
        return Tms282LoginPacketSupport.packet(0x000E);
    }

    /**
     * LP0049 (3C99860) login lie detector, image variant: u8 result (anything but 0x46), u8 notice,
     * u8 unused, u8 c = 0 (image), u8 d -> UI+0x2F8, u8 e -> UI+0x330, u8 unused, i32 length + image
     * bytes (2110AE0, decoded by 5B874F0; JPEG or PNG is unverified). Opens the Macro window
     * (UIWindow2.img/Macro); the client answers with CP00CC. The body equals 3C99D20 (LP0017/001A/0B1D
     * result 0x45). Never clears CLogin+0x220.
     *
     * @param showNotice notice (0,0x35) before the dialog
     * @param d          meaning unknown (possibly remaining tries or time)
     * @param e          meaning unknown
     */
    public static OutPacket loginLieDetectorImage(boolean showNotice, byte[] image, int d, int e) {
        Objects.requireNonNull(image, "image");
        checkByte(d, "Lie-detector value d");
        checkByte(e, "Lie-detector value e");
        OutPacket out = lieDetectorHeader(showNotice, false, d, e);
        out.encodeInt(image.length);
        out.encodeArr(image);
        return out;
    }

    /**
     * LP0049 sound variant: c != 0, then str for the soundLieDetector UI (24D25C0 -> 24D6D10). d and e
     * are read but only used by the image UI, so they are sent as 0. showNotice shows notice (0,0x59).
     */
    public static OutPacket loginLieDetectorSound(boolean showNotice, String sound, Charset charset) {
        OutPacket out = lieDetectorHeader(showNotice, true, 0, 0);
        str(out, sound, charset);
        return out;
    }

    private static OutPacket lieDetectorHeader(boolean showNotice, boolean sound, int d, int e) {
        OutPacket out = Tms282LoginPacketSupport.packet(0x0049);
        out.encodeByte(LIE_DETECTOR_QUESTION);
        out.encodeByte(showNotice);
        out.encodeByte(0);      // read, unused
        out.encodeByte(sound);  // c: 0 image, non-zero sound
        out.encodeByte(d);
        out.encodeByte(e);
        out.encodeByte(0);      // read, unused
        return out;
    }

    /** LP007D (3CAF830), low confidence: str; on world select sets the login background marker ("%s/%s" path). */
    public static OutPacket loginBackgroundMarker(String path) {
        OutPacket out = Tms282LoginPacketSupport.packet(0x007D);
        str(out, Objects.requireNonNull(path, "path"), DEFAULT_CHARSET);
        return out;
    }
}
