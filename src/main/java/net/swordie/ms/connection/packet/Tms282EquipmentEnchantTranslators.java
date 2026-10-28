package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.Optional;

/**
 * {@link Tms282LegacyPackets.Contributor} for the 265 EQUIPMENT_ENCHANT packets of the star force window
 * (FieldPacket.hyperUpgradeDisplay / showUpgradeResult / showUnknownEnchantFailResult), sent by the unchanged 265
 * {@code ItemHandler.handleUserEquipmentEnchantWithSingleUIRequest} after CP0179 (research: equip-enhance-2823.md).
 * <p>
 * LP01A1 [推定]: CWvsContext case 0x1A1 (context-dispatch.tsv) calls 231F170(packet), which reads the u8 type and
 * hands the packet to the windows registered for key 0x21; the star force window's 232D620(this, type, packet) -
 * the class of the CP0179 senders 232FB70..2340230 - handles types 0x34, 0x36, 0x67, 0x68, 0x69, 0x6C (jump table
 * 232D858). Layouts read statically from the window:
 * <ul>
 *     <li>0x34 HyperUpgradeDisplay, 955F30 [證實 static]: 4 x i64 cost, u8 mvp, u8 (282 only), u8 mvp-over-pc,
 *     u8 (282 only), i32 success, i32 destroy, i32 x3, u8 chance time, i32 next tuc (the five i32 as 265); then
 *     HyperEnchantStat 9551F0: u8 (282 only, sent 0), i32 mask, one i32 per set bit of 14 (the 265 EnchantStat
 *     bits, 265 order);</li>
 *     <li>0x68 ShowHyperUpgradeResult, 23307E0 [推定]: i32 result, u8, i32 (265: i32 result, i32 0, u8 0);</li>
 *     <li>0x69 ShowUnknownFailResult, 232D6D4 [證實 static]: u8, u8 (= 265).</li>
 * </ul>
 * Other types (spell trace display/result 0x32/0x64/0x65, scroll timer 0x33, mini game 0x35, transmission 0x67)
 * are declined and dropped as before: they belong to other windows or carry a 265 item body.
 */
public final class Tms282EquipmentEnchantTranslators implements Tms282LegacyPackets.Contributor {
    public static final int EQUIPMENT_ENCHANT = 0x01A1;
    static final int HYPER_UPGRADE_DISPLAY = 0x34;
    static final int SHOW_HYPER_UPGRADE_RESULT = 0x68;
    static final int SHOW_UNKNOWN_FAIL_RESULT = 0x69;

    public static final Tms282EquipmentEnchantTranslators INSTANCE = new Tms282EquipmentEnchantTranslators();

    private Tms282EquipmentEnchantTranslators() {
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        builder.on(OutHeader.EQUIPMENT_ENCHANT, Tms282EquipmentEnchantTranslators::translate);
    }

    static Optional<OutPacket> translate(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int type = body.u8();
        OutPacket out = Tms282LoginPacketSupport.packet(EQUIPMENT_ENCHANT);
        out.encodeByte(type);
        switch (type) {
            case HYPER_UPGRADE_DISPLAY -> hyperUpgradeDisplay(body, out);
            case SHOW_HYPER_UPGRADE_RESULT -> {
                int result = body.i32();
                int zero = body.i32();
                int flag = body.u8();
                out.encodeInt(result);
                out.encodeByte(flag);
                out.encodeInt(zero);
            }
            case SHOW_UNKNOWN_FAIL_RESULT -> {
                out.encodeByte(body.u8());
                out.encodeByte(body.u8());
            }
            default -> {
                return Optional.empty();
            }
        }
        body.end();
        return Optional.of(out);
    }

    private static void hyperUpgradeDisplay(Tms282LegacyPackets.Body body, OutPacket out) {
        for (int i = 0; i < 4; i++) {
            out.encodeLong(body.i64());                     // 955F43.. costs
        }
        int mvp = body.u8();
        int mvpOverPc = body.u8();
        out.encodeByte(mvp);                                // 955F73 -> +0x60
        out.encodeByte(0);                                  // 955F83 -> +0x64 (282 only)
        out.encodeByte(mvpOverPc);                          // 955F91 -> +0x61
        out.encodeByte(0);                                  // 955FA1 -> +0x62 (282 only)
        out.encodeInt(body.i32());                          // 955FB1 success
        out.encodeInt(body.i32());                          // 955FBC destroy
        out.encodeInt(body.i32());                          // 955FC7
        out.encodeInt(body.i32());                          // 955FD2
        out.encodeInt(body.i32());                          // 955FDD
        out.encodeByte(body.u8());                          // 955FE8 chance time
        out.encodeInt(body.i32());                          // 955FF6 next tuc
        out.encodeByte(0);                                  // 95520C HyperEnchantStat u8 (282 only)
        int mask = body.i32();
        out.encodeInt(mask);                                // 95521C
        for (int bit = 0; bit < 14; bit++) {
            if ((mask & (1 << bit)) != 0) {
                out.encodeInt(body.i32());                  // 955259
            }
        }
    }
}
