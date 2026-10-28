package net.swordie.ms.connection.hotfix;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Serializes hotfix entries as a WZ "Property" image, the format of the 265 hotfix sample
 * {@code data/resources/Data2.wz} (decoded 2026-09-28: 47 entries, all type 8 strings, names = full WZ paths):
 * <pre>
 *   0x73 str("Property")  u16 0  cint count
 *   count x { 0x00 str(name)  u8 type  value }
 * </pre>
 * Strings use the WZ encoding with an all-zero key (only the rolling 0xAA / 0xAAAA mask), exactly as in the
 * sample: ASCII {@code s8 -len (or -128 + i32 len)} and bytes {@code c ^ (0xAA + i)}; otherwise UTF-16
 * {@code s8 len (or 127 + i32 len)} and chars {@code c ^ (0xAAAA + i)}. Whether the 282 client uses the same key
 * is unknown (see HotfixService).
 * <p>
 * Types: 0 null, 2 short, 3 int (compressed), 4 float, 5 double, 8 string, 9 object (Shape2D#Vector2D, UOL),
 * 11 bool (short 0 / -1), 20 long (compressed).
 */
public final class WzImageWriter {
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    private WzImageWriter() {
    }

    public static byte[] write(List<HotfixEntry> entries) {
        WzImageWriter w = new WzImageWriter();
        w.u8(0x73);
        w.wzString("Property");
        w.u16(0);
        w.cint(entries.size());
        for (HotfixEntry e : entries) {
            w.u8(0x00);
            w.wzString(e.path());
            w.value(e.value());
        }
        return w.out.toByteArray();
    }

    private void value(HotfixEntry.Value v) {
        switch (v) {
            case HotfixEntry.Null n -> u8(0);
            case HotfixEntry.Short s -> {
                u8(2);
                u16(s.v());
            }
            case HotfixEntry.Int i -> {
                u8(3);
                cint(i.v());
            }
            case HotfixEntry.Float f -> {
                u8(4);
                if (f.v() == 0f) {
                    u8(0);
                } else {
                    u8(0x80);
                    i32(java.lang.Float.floatToIntBits(f.v()));
                }
            }
            case HotfixEntry.Double d -> {
                u8(5);
                i64(java.lang.Double.doubleToLongBits(d.v()));
            }
            case HotfixEntry.Str s -> {
                u8(8);
                u8(0x00);
                wzString(s.v());
            }
            case HotfixEntry.Vector vec -> {
                u8(9);
                WzImageWriter body = new WzImageWriter();
                body.u8(0x73);
                body.wzString("Shape2D#Vector2D");
                body.cint(vec.x());
                body.cint(vec.y());
                object(body);
            }
            case HotfixEntry.Uol uol -> {
                u8(9);
                WzImageWriter body = new WzImageWriter();
                body.u8(0x73);
                body.wzString("UOL");
                body.u8(0);
                body.u8(0x00);
                body.wzString(uol.link());
                object(body);
            }
            case HotfixEntry.Bool b -> {
                u8(11);
                u16(b.v() ? -1 : 0);
            }
            case HotfixEntry.Long l -> {
                u8(20);
                clong(l.v());
            }
        }
    }

    private void object(WzImageWriter body) {
        byte[] b = body.out.toByteArray();
        i32(b.length);
        out.writeBytes(b);
    }

    // ---- primitives ------------------------------------------------------------------------------------------

    private void u8(int b) {
        out.write(b & 0xFF);
    }

    private void u16(int v) {
        u8(v);
        u8(v >>> 8);
    }

    private void i32(int v) {
        u16(v);
        u16(v >>> 16);
    }

    private void i64(long v) {
        i32((int) v);
        i32((int) (v >>> 32));
    }

    private void cint(int v) {
        if (v > -128 && v <= 127) {
            u8(v);
        } else {
            u8(0x80);
            i32(v);
        }
    }

    private void clong(long v) {
        if (v > -128 && v <= 127) {
            u8((int) v);
        } else {
            u8(0x80);
            i64(v);
        }
    }

    private void wzString(String s) {
        if (s == null || s.isEmpty()) {
            u8(0);
            return;
        }
        boolean ascii = s.chars().allMatch(c -> c < 0x80);
        int n = s.length();
        if (ascii) {
            if (n <= 127) {
                u8(-n);
            } else {
                u8(0x80);
                i32(n);
            }
            int mask = 0xAA;
            for (int i = 0; i < n; i++) {
                u8(s.charAt(i) ^ mask);
                mask = (mask + 1) & 0xFF;
            }
        } else {
            if (n < 127) {
                u8(n);
            } else {
                u8(127);
                i32(n);
            }
            int mask = 0xAAAA;
            for (int i = 0; i < n; i++) {
                u16(s.charAt(i) ^ mask);
                mask = (mask + 1) & 0xFFFF;
            }
        }
    }
}
