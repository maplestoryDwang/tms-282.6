package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.util.FileTime;

import java.nio.charset.Charset;
import java.util.Objects;

import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.checkByte;
import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.str;

/**
 * Burning block read by 8D93D0, the same reader CharacterStat (931E40) and LP01E9 r=1 use
 * (avatar-wire-layout-2823.md "Burning block08D93D0", burning-display-chars-2823.md 1.3):
 * raw8, raw8, i32, i32, i32, u8, i32, str, i32, str, str, str - nothing after the fourth string.
 * Four empty strings give 45 bytes. Only widths and order are verified; the names of the fields after
 * burningType are placeholders, and the mapping of BurningCheckDlg kinds to type/burningType is unconfirmed.
 */
public record Tms282BurningBlock(FileTime start, FileTime end, int minLevel, int maxLevel, int type,
                                 int burningType, int value1, String text1, int value2,
                                 String text2, String text3, String text4) {
    public Tms282BurningBlock {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        checkByte(burningType, "Burning type");
        Objects.requireNonNull(text1, "text1");
        Objects.requireNonNull(text2, "text2");
        Objects.requireNonNull(text3, "text3");
        Objects.requireNonNull(text4, "text4");
    }

    /** Block with the four strings and both trailing ints empty/zero, as CharacterStat.encodeBurning writes them. */
    public static Tms282BurningBlock of(FileTime start, FileTime end, int minLevel, int maxLevel, int type,
                                        int burningType) {
        return new Tms282BurningBlock(start, end, minLevel, maxLevel, type, burningType, 0, "", 0, "", "", "");
    }

    void encode(OutPacket out, Charset charset) {
        Objects.requireNonNull(charset, "charset");
        out.encodeFT(start);
        out.encodeFT(end);
        out.encodeInt(minLevel);
        out.encodeInt(maxLevel);
        out.encodeInt(type);
        out.encodeByte(burningType);
        out.encodeInt(value1);
        str(out, text1, charset);
        out.encodeInt(value2);
        str(out, text2, charset);
        str(out, text3, charset);
        str(out, text4, charset);
    }
}
