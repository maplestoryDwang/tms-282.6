package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.world.event.SunnySunday;

import java.nio.charset.Charset;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * LP02EF SetField T18: the Sunny Sunday (星期天楓之谷) block, read by 5E61220 (282.3; 282.4 identical, PORT.md §3).
 *
 * <pre>
 * 5E61256 u8   active; 0 = no event (5E60E30 clears the UI state) and nothing else follows
 * 5E61284 u8   flag (stored at +2)
 * 5E61298 str  uiPath   (+0x08, e.g. "UI/UIWindowEvent.img/sundayMaple")
 * 5E612DE str  uiName   (+0x10)
 * 5E61323 str  title    (+0x18)
 * 5E61369 str  desc     (+0x20)
 * 5E613AE str  dateText (+0x28)
 * 5E613EF i32  val1     (+0x30)
 * 5E613FA i32  val2     (+0x34)
 * </pre>
 * The same order as 265 {@link SunnySunday#encode}; strings use the 282 charset (DecodeStr F8D3D0, u16 byte length).
 */
public final class Tms282SunnySundayBlock {
    /** The calendar entry of today, while the revival switch "sunday" is on (revival-events.json). */
    private static final Supplier<SunnySunday.SunnySundayEntry> DEFAULT_SOURCE = () ->
            net.swordie.ms.world.tms282.revival.RevivalEvents.isOpen("sunday") ? SunnySunday.SunnySundayLogic.get() : null;
    private static volatile Supplier<SunnySunday.SunnySundayEntry> source = DEFAULT_SOURCE;

    private Tms282SunnySundayBlock() {
    }

    /** Checks replace the date lookup; {@code null} restores the configured calendar. */
    public static void setSource(Supplier<SunnySunday.SunnySundayEntry> replacement) {
        source = replacement == null ? DEFAULT_SOURCE : replacement;
    }

    /** Today's entry, or null. A broken or unloaded configuration means "no event", never a failed SetField. */
    public static SunnySunday.SunnySundayEntry current() {
        try {
            SunnySunday.SunnySundayEntry entry = source.get();
            return entry != null && valid(entry) ? entry : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    static boolean valid(SunnySunday.SunnySundayEntry e) {
        return e.uiPath != null && e.uiPath.startsWith("UI/") && e.uiName != null && !e.uiName.isEmpty();
    }

    public static void encode(OutPacket out, SunnySunday.SunnySundayEntry entry) {
        encode(out, entry, Tms282LoginPacketSupport.DEFAULT_CHARSET);
    }

    static void encode(OutPacket out, SunnySunday.SunnySundayEntry entry, Charset charset) {
        Objects.requireNonNull(out, "out");
        if (entry == null || !valid(entry)) {
            out.encodeByte(0);                                          // 5E61256 absent
            return;
        }
        out.encodeByte(1);                                              // 5E61256 active
        out.encodeByte(0);                                              // 5E61284 flag (+2); 265 sends false
        Tms282LoginPacketSupport.str(out, entry.uiPath, charset);       // 5E61298
        Tms282LoginPacketSupport.str(out, entry.uiName, charset);       // 5E612DE
        Tms282LoginPacketSupport.str(out, nz(entry.title), charset);    // 5E61323
        Tms282LoginPacketSupport.str(out, nz(entry.desc), charset);     // 5E61369
        Tms282LoginPacketSupport.str(out, nz(entry.dateText), charset); // 5E613AE
        out.encodeInt(entry.val1);                                      // 5E613EF
        out.encodeInt(entry.val2);                                      // 5E613FA
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
