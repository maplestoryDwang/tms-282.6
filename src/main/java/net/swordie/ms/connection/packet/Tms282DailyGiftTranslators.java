package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.Optional;

/**
 * 265 DAILY_GIFT_INIT (2676) as TMS 282.3 LP0AA6 (2026-09-29). The main menu enables 每日禮物 only when the DailyGift
 * manager (global 9ECDA90) has a current entry whose required level is reached (0x2878870 -> 0x1F20380); nothing was
 * sent on 282, so it stayed disabled. Dispatcher 48B0930 (f(this, nType, pkt)) case 0xAA6 reads u8 mode:
 * <ul>
 *     <li>0: u8 has; has ? entry (133F510); i32 n, n x entry. The 282 entry is the 265 {@code DailyGift.encode} with
 *     one more i32 at the end of every reward (10 fields), then after the two 265 lists a new {@code i32 n x (i32, i32)}
 *     list and one i32. The 265 packet converted this way is read CLEAN by the emulated client (daily-trace, 950 of 950
 *     bytes, --fake-global 9ECDA90).</li>
 *     <li>1: i32 (265 item id, copied); 2: i32, i32 (265 {@code 2, itemId}, copied).</li>
 * </ul>
 * Menu gate (dailyui-1005, 282.4 client): 0x2878870(menu, level) takes the current entry from 0x1F20380 - the
 * "has" entry when one was sent (no date check), else the first list entry with start <= now <= end (0x133F030) - and
 * disables item 0x1F when there is none or level < entry +0x2C (the i32 after the rewards). That i32 is
 * {@link net.swordie.ms.constants.DailyGiftConstants#reqLevel()} (282: 10; it was 33, so Lv.10 saw nothing).
 * Claiming: CP03B9 (sender 48D82B0, i32) -> 265 DAILY_GIFT_REQUEST, see Tms282DailyPlayHandlers.
 */
public final class Tms282DailyGiftTranslators implements Tms282LegacyPackets.Contributor {
    public static final Tms282DailyGiftTranslators INSTANCE = new Tms282DailyGiftTranslators();
    public static final int DAILY_GIFT = 0x0AA6;
    /** 265 reward record: i32, i32, i32, u8, i32, u8, i32, i32, u8. */
    static final int GMS265_REWARD_BYTES = 4 + 4 + 4 + 1 + 4 + 1 + 4 + 4 + 1;

    private Tms282DailyGiftTranslators() {
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        builder.on(OutHeader.DAILY_GIFT_INIT, Tms282DailyGiftTranslators::dailyGift);
    }

    static Optional<OutPacket> dailyGift(OutPacket legacy) {
        Tms282LegacyPackets.Body b = Tms282LegacyPackets.Body.of(legacy);
        int mode = b.u8();
        OutPacket out = Tms282LoginPacketSupport.packet(DAILY_GIFT);
        out.encodeByte(mode);
        if (mode != 0) {
            out.encodeArr(b.rest()); // 1: i32; 2: i32, i32 - same widths as 265
            return Optional.of(out);
        }
        int has = b.u8();
        out.encodeByte(has);
        if (has != 0) {
            entry(b, out);
        }
        int n = b.i32();
        out.encodeInt(n);
        for (int i = 0; i < n; i++) {
            entry(b, out);
        }
        b.end();
        return Optional.of(out);
    }

    /** One 265 DailyGift.encode block -> the 282 entry (133F510). */
    static void entry(Tms282LegacyPackets.Body b, OutPacket out) {
        out.encodeArr(b.bytes(8 + 8 + 4 * 4)); // FT start, FT end, i32 x4
        int len = b.u16();
        out.encodeShort(len);
        out.encodeArr(b.bytes(len));          // tooltip string (raw bytes, unchanged)
        int rewards = b.i32();
        out.encodeInt(rewards);
        for (int i = 0; i < rewards; i++) {
            out.encodeArr(b.bytes(GMS265_REWARD_BYTES));
            out.encodeInt(0);                  // 282: tenth field
        }
        out.encodeInt(b.i32());                // required level (+0x2C): menu item 0x1F needs level >= it
        int list1 = b.i32();
        out.encodeInt(list1);
        out.encodeArr(b.bytes(list1 * 8));     // i32, i32
        int list2 = b.i32();
        if (list2 != 0) {
            throw new IllegalArgumentException("265 daily gift second list not empty: " + list2);
        }
        out.encodeInt(0);
        out.encodeInt(0);                      // 282: new i32 n x (i32, i32) list
        out.encodeInt(0);                      // 282: new i32 (+0x28)
    }
}
