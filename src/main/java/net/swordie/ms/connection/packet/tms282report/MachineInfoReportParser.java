package net.swordie.ms.connection.packet.tms282report;

/**
 * CP00A5 machine information (static layout 3CAC753, matches the 64-byte captures):
 * {@code str (608A460, 32 bytes), str (6088ED0, 14 bytes)}, then three count-prefixed maps
 * {@code int n1, n1×(int,u8)}, {@code int n2, n2×(int,int)}, {@code int n3, n3×(int,str)}.
 * <p>
 * The two strings are obfuscated by rotation; rotating right by 4 and by 3 gives readable text
 * in every capture ("-|1733649110|...|" and "MapleStory.exe"). The rotation amounts are observed,
 * not read from the obfuscation routines.
 */
public final class MachineInfoReportParser implements ClientReportParser {
    static final int FIRST_STRING_ROTATION = 4;
    static final int SECOND_STRING_ROTATION = 3;

    @Override
    public String name() {
        return "機器資訊";
    }

    @Override
    public void parse(ReportReader in, ReportFields out) {
        out.add("識別字串", in.rotatedStr(FIRST_STRING_ROTATION));
        out.add("執行檔", in.rotatedStr(SECOND_STRING_ROTATION));
        int n1 = count(in, 5);
        out.add("map1 數量", n1);
        for (int i = 0; i < n1; i++) {
            out.add("map1[" + i + "]", in.i32() + ", " + in.u8());
        }
        int n2 = count(in, 8);
        out.add("map2 數量", n2);
        for (int i = 0; i < n2; i++) {
            out.add("map2[" + i + "]", in.i32() + ", " + in.i32());
        }
        int n3 = count(in, 6);
        out.add("map3 數量", n3);
        for (int i = 0; i < n3; i++) {
            out.add("map3[" + i + "]", in.i32() + ", " + in.str());
        }
    }

    /** Rejects counts the remaining bytes cannot hold, before looping. */
    private static int count(ReportReader in, int minimumEntrySize) {
        int n = in.i32();
        if (n < 0 || n > in.remaining() / minimumEntrySize) {
            throw new IllegalArgumentException("Invalid map count " + n);
        }
        return n;
    }
}
