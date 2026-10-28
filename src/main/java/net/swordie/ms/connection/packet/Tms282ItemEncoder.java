package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.items.PetItem;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.EquipBaseStat;
import net.swordie.ms.life.Android;
import net.swordie.ms.util.FileTime;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.List;
import java.util.Objects;

import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.str;

/**
 * TMS 282.3 GW_ItemSlotBase wire form: the reader 932790 (u8 type -> 8DB5C0 -> vtbl+0x410) that INVENTORY_OPERATION,
 * 8F1450 and the CharacterData item lists (9343B0, same wire form) share. Evidence:
 * research/tms2823/packet-audit/ingame-layouts-282.md section 3 (RVAs below are the client's Decode call sites) and
 * the emulated SetField traces in research/tms2823/setfield-trace (tools/Tms282SetFieldCheck.java).
 * <p>
 * Each item kind is one {@link Kind} (a strategy chosen from the 265 item class): the shared header 932800, then its
 * body (Equip 9331A0, Bundle 933700, Pet 933870). Values come from the 265 item where 265 has the field; fields that
 * are new in 282 and whose meaning is unknown are written in their empty form (0, empty string, count 0), which is
 * what the client code does with a zeroed object. Tags: [verified] = width/order from the client code;
 * [presumed] = meaning or value presumed.
 * <p>
 * Differences from 265 {@link Item#encode}/{@link Equip#encode}/{@link PetItem#encode}: Equip gains a leading
 * 609E120 block, EquipBase bit 2 widens to i32 and bits 21/22 exist, owner is 15 bytes, only one short follows the
 * seven potential options (265: socket mask + 3 sockets), ftEquipped is gone, 8DEF20 gains a u8, one more i32,
 * new 8D98B0/8DCF80/8DE3A0 blocks and a 132-byte tail; Bundle quantity is an i32, owner 15 bytes, the Buf8 option
 * has another item set, and a Buf21 + 8D98B0 tail follows; Pet name is 21 bytes and three i32 follow activeState.
 */
public final class Tms282ItemEncoder {
    /** Equip E28 (93367E): DecodeBuffer(0x5C). */
    static final int EQUIP_TAIL_BUFFER = 92;
    /** Bundle B7 (8E094F) and Pet P1 (933897) buffer width. */
    static final int BUFFER_21 = 21;
    /** Owner width of Equip E4 (9331E7) and Bundle B2 (933757); 265 used 13. */
    static final int OWNER_WIDTH = 15;
    /** 265 EquipBaseStat pos-0 flags written by EquipCalcStat 9328A0 (bits 0..12, same as 265). */
    private static final List<EquipBaseStat> CALC_STATS = List.of(EquipBaseStat.values()).stream()
            .filter(s -> s.getPos() == 0 && s.getVal() > 0).toList();
    /** 265 EquipBaseStat pos-1 flags, bits 0..20 of the 282 EquipBase mask (932B66). Bits 21/22 are never set. */
    private static final List<EquipBaseStat> BASE_STATS = List.of(EquipBaseStat.values()).stream()
            .filter(s -> s.getPos() == 1 && s.getVal() > 0).toList();

    private Tms282ItemEncoder() {
    }

    /** The three item kinds of 8DB5C0; the byte is the same as 265 {@link Item.Type}. */
    public enum Kind {
        EQUIP(1) {
            @Override
            void encodeBody(OutPacket out, Item item, long cashSn, Charset charset) {
                encodeEquipBody(out, (Equip) item, cashSn, charset);
            }
        },
        BUNDLE(2) {
            @Override
            void encodeBody(OutPacket out, Item item, long cashSn, Charset charset) {
                encodeBundleBody(out, item, charset);
            }
        },
        PET(3) {
            @Override
            void encodeBody(OutPacket out, Item item, long cashSn, Charset charset) {
                encodePetBody(out, (PetItem) item, charset);
            }
        };

        private final int wire;

        Kind(int wire) {
            this.wire = wire;
        }

        public int wire() {
            return wire;
        }

        /** By the 265 item class, the way 265 builds items from the database (Item.getItemsFromSQLByQuery). */
        public static Kind of(Item item) {
            Objects.requireNonNull(item, "item");
            if (item instanceof Equip) {
                return EQUIP;
            }
            if (item instanceof PetItem) {
                return PET;
            }
            return BUNDLE;
        }

        abstract void encodeBody(OutPacket out, Item item, long cashSn, Charset charset);
    }

    /** One item in the 932790 form: u8 kind, header 932800, body. */
    public static void encode(OutPacket out, Item item, Charset charset) {
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(charset, "charset");
        Kind kind = Kind.of(item);
        out.encodeByte(kind.wire());                        // 9327A3 [verified] type
        long cashSn = encodeHeader(out, item, kind);
        kind.encodeBody(out, item, cashSn, charset);
    }

    /** 932800; returns the cash SN the client stores at +0x38 (0 for a non-cash item). */
    private static long encodeHeader(OutPacket out, Item item, Kind kind) {
        int itemId = item.getItemId();
        out.encodeInt(itemId);                              // 932818 [verified]
        boolean cash = item.isCash();
        out.encodeByte(cash);                               // 93282B [verified]
        long cashSn = cash ? item.getId() : 0;
        if (cash) {
            out.encodeLong(cashSn);                         // 932841 Buf8 -> +0x38 [verified] 265 sends the item ID
        }
        out.encodeFT(expire(item));                         // 93285D Buf8 -> +0x40 [verified]
        out.encodeInt(item.getBagIndex());                  // 932865 -> +0x48 [verified] 265 value
        out.encodeByte(kind == Kind.EQUIP || GameConstants.isIntensePowerCrystal(itemId)
                || ItemConstants.isTitleItem(itemId));      // 932870 -> +0x4C [verified width] 265 value
        return cashSn;
    }

    /** 9331A0, after the header. Minimal body (all masks 0, no symbol/android) is 320 bytes. */
    private static void encodeEquipBody(OutPacket out, Equip equip, long cashSn, Charset charset) {
        int itemId = equip.getItemId();
        out.encodeByte(0);                                  // 609E133 E1 has=0: no Buf4 hdr / lists [verified width, meaning unknown]
        equip.encodeEquipCalcStat(out, calcStatMask(equip, 0), 0); // 9328BD E2 total stats [verified] same as 265
        encodeEquipBase(out, equip);                        // 932B66 E3 [verified]
        out.encodeArr(fixed(equip.getOwner(), OWNER_WIDTH, charset)); // 9331E7 E4 Buf15 [verified]
        out.encodeByte(equip.getGrade());                   // 9331F3 E5 [verified]
        out.encodeByte(equip.getChuc());                    // 933232 E6 [verified]
        List<Integer> options = equip.getOptions();
        for (int i = 0; i < 7; i++) {                       // 933271..933319 E7 7 x u16 [verified width] 265 order
            out.encodeShort(options != null && i < options.size() && options.get(i) != null ? options.get(i) : 0);
        }
        out.encodeShort(0);                                 // 933336 E8 -> +0x744 [verified width] one short where 265
                                                            // sent socket mask + 3 sockets; 0 = none [presumed]
        if (cashSn == 0) {
            out.encodeLong(equip.getId());                  // 933364 E9 read only when +0x38 == 0 (933350) [verified]
        }
        // 265 ftEquipped: not read by 282.
        out.encodeLong(0);                                  // 8DEC9B E10 CashItemOption SN [verified] 265 value
        out.encodeFT(expire(equip));                        // 8DECB7 [verified] 265 value
        out.encodeInt(equip.getGrade());                    // 8DECC8 [verified] 265 value
        for (int i = 0; i < 3; i++) {
            out.encodeInt(equip.getOptionBase(i));          // 8DECD2..8DECE9 [verified] 265 value
        }
        out.encodeLong(0);                                  // 8DEF3B E11 [verified] 265 L 0
        out.encodeInt(0);                                   // 8DEF4C [verified] 265 I 0
        out.encodeByte(0);                                  // 8DEF57 u8; 0 skips the i32 at 8DEF63 [verified]
        out.encodeInt(0);                                   // 933394 E12 -> +0x34A [verified] 265 I 0
        out.encodeInt(0);                                   // 9333A2 -> +0x34E [verified width] 282-only
        out.encodeInt(0);                                   // 8DF0EE E13 n=0 (<= 5 of Buf8,i32,i32) [verified]
        out.encodeLong(0);                                  // 8D98CE E14 [verified width] 282-only
        out.encodeLong(0);                                  // 8D98EA
        out.encodeInt(0);                                   // 8D98FB
        out.encodeShort(equip.getSoulOptionId());           // 9333CE E15 [verified]
        out.encodeShort(equip.getSoulSocketId());           // 9333EB
        out.encodeShort(equip.getSoulOption());             // 933408
        if (isSymbol(itemId)) {                             // E16 1712xxx / E17 1713xxx..1714xxx
            var symbol = equip.getSymbol();
            out.encodeShort(symbol == null ? 0 : symbol.getInc()); // 93343A / 9334D7 [verified]
            out.encodeInt(symbol == null ? 0 : symbol.getExp());   // 933457 / 9334F4
            out.encodeShort(symbol == null ? 0 : symbol.getLevel()); // 933490 / 93352D
        }
        out.encodeShort(-1);                                // 8DCCE0 E18 [verified] 265 value
        out.encodeFT(FileTime.MAX_TIME());                  // 8DCCF5
        out.encodeFT(FileTime.MIN_TIME());                  // 8DCD07
        out.encodeFT(FileTime.MAX_TIME());                  // 8DCD23
        out.encodeInt(0);                                   // 8DCD83 E19 [verified] 265 values
        out.encodeInt(0);                                   // 8DCD8D
        out.encodeInt(0);                                   // 8DCD98
        out.encodeInt(0);                                   // 8DCFA8 E20 n=0 of (i32, str) [verified] 282-only
        out.encodeInt(0);                                   // 8DE3B3 E21 [verified width] 282-only
        str(out, "", charset);                              // 8DE3C2
        if (isAndroid(itemId)) {                            // E22 8D9C20, same as 265 encodeAndroidInfo [verified]
            Android android = equip.getAndroidLife() != null ? equip.getAndroidLife() : new Android(itemId);
            out.encodeShort(android.getSkin());             // 265 encodeAndroidInfo with full hair/face ids (Tms282AndroidLook)
            out.encodeInt(Tms282AndroidLook.hair(equip.getAndroid(), android.getHair()));
            out.encodeInt(Tms282AndroidLook.face(equip.getAndroid(), android.getFace()));
            out.encodeString(android.getName());
            out.encodeInt(0);
            out.encodeFT(FileTime.MIN_TIME());
            out.encodeInt(0);
        }
        out.encodeByte(equip.getExceptionalSlot());         // 9335B1 E23 [verified]
        out.encodeByte(0);                                  // 9335E7 [verified] 265 value
        equip.encodeEquipCalcStat(out, calcStatMask(equip, 2), 2); // 933620 E24 exceptional [verified]
        int enhanceMask = calcStatMask(equip, 1);
        out.encodeByte(enhanceMask != 0);                   // 933628 E25 [verified]
        if (enhanceMask != 0) {
            equip.encodeEquipCalcStat(out, enhanceMask, 1); // 93363B
        }
        out.encodeInt(0);                                   // 933654 E26 -> +0x70 [verified width] 282-only
        out.encodeLong(0);                                  // 933669 E27 Buf8 -> +0x74
        out.encodeArr(new byte[EQUIP_TAIL_BUFFER]);         // 93367E E28 Buf92 -> +0xAA
        out.encodeLong(0);                                  // 933686 E29 i64 -> +0x68D
        for (int i = 0; i < 5; i++) {
            out.encodeInt(0);                               // 933695..9336CD E30 -> +0x695..+0x6A5
        }
    }

    /** EquipBase 932B40: i32 mask, then one field per set bit from bit 0 (bit 2 is an i32 in 282). */
    private static void encodeEquipBase(OutPacket out, Equip equip) {
        int mask = baseStatMask(equip);
        out.encodeInt(mask);
        for (EquipBaseStat stat : BASE_STATS) {
            if ((mask & stat.getVal()) == 0) {
                continue;
            }
            switch (stat) {
                case tuc -> out.encodeByte(equip.getTuc());                       // 932B74
                case cuc -> out.encodeByte(equip.getCuc());                       // 932BB4
                case attribute -> out.encodeInt(equip.getAttribute());            // 932BF6 i32 (265 short)
                case levelUpType -> out.encodeByte(equip.getLevelUpType());       // 932C36
                case level -> out.encodeByte(equip.getItemLevel());               // 932C80
                case exp -> out.encodeLong(equip.getItemEXP());                   // 932CCA
                case durability -> out.encodeInt(equip.getDurability());          // 932CF8
                case iuc -> out.encodeInt(equip.getIuc());                        // 932D3D
                case iReduceReq -> out.encodeByte(equip.getTotalStat(stat));      // 932D55
                case specialAttribute -> out.encodeShort(equip.getSpecialAttribute()); // 932D9F
                case durabilityMax -> out.encodeInt(equip.getDurabilityMax());    // 932DC6
                case iIncReq -> out.encodeByte(equip.getiIncReq());               // 932E07
                case growthEnchant -> out.encodeByte(equip.getGrowthEnchant());   // 932E51
                case psEnchant -> out.encodeByte(equip.getPsEnchant());           // 932E9B
                case bdr, imdr, damR, statR -> out.encodeByte(equip.getTotalStat(stat)); // 932EE5..932FC3
                case cuttable -> out.encodeByte(equip.getCuttable());             // 93300D
                case exGradeOption -> out.encodeLong(equip.getExGradeOption());   // 933057
                case hyperUpgrade -> out.encodeInt(equip.getHyperUprade());       // 93307F
                default -> throw new IllegalStateException("EquipBase bit without a 282 width: " + stat);
            }
        }
    }

    /** 265 Equip.getStatMask(1): the same presence test, applied to bits 0..20. */
    static int baseStatMask(Equip equip) {
        int mask = 0;
        for (EquipBaseStat stat : BASE_STATS) {
            if (equip.getTotalStat(stat) != 0 || equip.getBaseStatFlame(stat) != 0
                    || equip.getBaseStatExceptional(stat) != 0 || equip.getEnchantmentStat(stat) != 0
                    || (stat == EquipBaseStat.exGradeOption && equip.getExGradeOption() != 0)) {
                mask |= stat.getVal();
            }
        }
        return mask;
    }

    /** 265 Equip.getStatMask(0, type): 0 = total, 1 = enchant (star force), 2 = exceptional. */
    static int calcStatMask(Equip equip, int type) {
        int mask = 0;
        for (EquipBaseStat stat : CALC_STATS) {
            long value = switch (type) {
                case 0 -> equip.getTotalEnhanceStat(stat);
                case 1 -> equip.getEnchantmentStat(stat);
                case 2 -> equip.getBaseStatExceptional(stat);
                default -> throw new IllegalArgumentException("EquipCalcStat type " + type);
            };
            if (value != 0) {
                mask |= stat.getVal();
            }
        }
        return mask;
    }

    /** 933700, after the header: 67 bytes, plus 8 when {@link #hasBundleOption}. */
    private static void encodeBundleBody(OutPacket out, Item item, Charset charset) {
        out.encodeInt(item.getQuantity());                  // 93371D B1 i32 (265 short) [verified]
        out.encodeArr(fixed(item.getOwner(), OWNER_WIDTH, charset)); // 933757 B2 Buf15 [verified]
        out.encodeShort(item.getItemAttribute());           // 933763 B3 [verified]
        out.encodeByte(item.isTitleOn());                   // 93377A B4 [verified]
        if (hasBundleOption(item.getItemId())) {
            out.encodeLong(item.getId());                   // 933825 B5 [verified condition] 265 value
        }
        out.encodeInt(0);                                   // 93382D B6 [verified] 265 I 0
        out.encodeArr(new byte[BUFFER_21]);                 // 8E094F B7 Buf21 [verified width] 282-only
        out.encodeLong(0);                                  // 8D98CE B8 (same reader as Equip E14)
        out.encodeLong(0);                                  // 8D98EA
        out.encodeInt(0);                                   // 8D98FB
    }

    /**
     * 9337B6..93380C: the Buf8 after titleOn is read for 207xxxx, 233xxxx, 302xxxx, 287xxxx and the six items of
     * AECCA0 (4001886 + table at AECCD8: 4001886, 4001928, 4001929, 4001944, 4001945, 4001946).
     */
    public static boolean hasBundleOption(int itemId) {
        int prefix = itemId / 10000;
        return prefix == 207 || prefix == 233 || prefix == 302 || prefix == 287
                || itemId == 4001886 || itemId == 4001928 || itemId == 4001929
                || (itemId >= 4001944 && itemId <= 4001946);
    }

    /** 933870, after the header: 68 bytes. */
    private static void encodePetBody(OutPacket out, PetItem pet, Charset charset) {
        out.encodeArr(fixed(pet.getName(), BUFFER_21, charset)); // 933897 P1 Buf21 (265 13) [verified]
        out.encodeByte(pet.getLevel());                     // 93389F P2 [verified]
        out.encodeShort(pet.getTameness());                 // 9338D5 P3
        out.encodeByte(pet.getRepleteness());               // 9338EC P4
        out.encodeFT(pet.getDateExpire() == null ? FileTime.MAX_TIME() : pet.getDateExpire()); // 93392F P5 265 value
        out.encodeShort(pet.getPetAttribute());             // 933937 P6
        out.encodeShort(pet.getPetSkill());                 // 93394E P7
        out.encodeInt(pet.getRemainLife());                 // 933988 P8
        out.encodeShort(pet.getAttribute());                // 9339C1 P9
        out.encodeByte(pet.getActiveState());               // 9339DE P10
        out.encodeInt(0);                                   // 933A1D P11 [verified width] 282-only
        out.encodeInt(0);                                   // 933A56 P12
        out.encodeInt(0);                                   // 933A8F P13
        out.encodeInt(pet.getPetHue());                     // 933AC8 P14 [presumed position of petHue]
        out.encodeShort(pet.getGiantRate());                // 933B01 P15
        out.encodeShort(0);                                 // 933B1E P16 265 S 0
        out.encodeInt(0);                                   // 933B3B P17 265 I 0
    }

    /** E16 (1712000..1712999) and E17 (1713000..1714999) read the same u16, i32, u16. */
    static boolean isSymbol(int itemId) {
        return itemId >= 1712000 && itemId <= 1714999;
    }

    /** E22 8D9C20: 1660000..1669999. */
    static boolean isAndroid(int itemId) {
        return itemId >= 1660000 && itemId <= 1669999;
    }

    /** 265 writes the expiry through a LocalDateTime round trip; kept so both servers send the same value. */
    private static FileTime expire(Item item) {
        FileTime expire = item.getDateExpire();
        return expire == null ? FileTime.MAX_TIME() : FileTime.fromDate(expire.toLocalDateTime());
    }

    /**
     * A fixed-width client buffer: the text in the charset, cut before the first character that does not fit
     * (never in the middle of a multi-byte character), zero-padded.
     */
    static byte[] fixed(String text, int width, Charset charset) {
        ByteBuffer buffer = ByteBuffer.allocate(width);
        charset.newEncoder().onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
                .encode(CharBuffer.wrap(text == null ? "" : text), buffer, true);
        return buffer.array();
    }
}
