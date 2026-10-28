package net.swordie.ms.connection.packet;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Optional;

/**
 * CP0150 (282.3 cash item use) bodies after the common prefix, one record per cash-item family. The family comes from
 * the client's GetCashItemType (AEFA50, table {@code data/resources/tms282/cashuse/cash-item-type.tsv}); the switch
 * 5E11F10 writes the prefix (5E120AF: {@code u16 0x150, i32 0, i32 tick, u16 slot, i32 itemId}) and each case appends
 * its tail. Every layout below was produced by running the client's own sender under Unicorn
 * ({@code research/tms2823/item-usability-20260928/cash-use/cash_use_sender_check.py}, bins in {@code out/}):
 * <ul>
 *     <li>{@link Family#PREFIX_ONLY} pet food 0x1E (5E17C40), meso bag 0x13 (5E1589C), charm 0x07 (5E1A125);</li>
 *     <li>{@link Family#TEXT} 0x0C / 0x0F megaphones and 0x10 weather: {@code str};</li>
 *     <li>{@link Family#WHISPER_TEXT} 0x0D, 0x82..0x85 megaphones (5E1286A) and 0x0E / 0xAD item megaphones
 *     (UI 3B51F60, send 3B55580): {@code str, u8 whisper, link} where link (134A630) is
 *     {@code i32 kind; kind 1: i32 tab, i32 slot; kind 2: i32, f64};</li>
 *     <li>{@link Family#TELEPORT} 0x16 / 0x3E (tail 5E33DC0): {@code u8 0, i32 field, str, i32} or
 *     {@code u8 1, str name}; 5042000..5042999 send only {@code u8 0};</li>
 *     <li>{@link Family#BEAUTY} 0x01 / 0x02 / 0x03 / 0x1F / 0x63 without {@code info/choice} (5F83680):
 *     {@code u8 target, u8 flag [, u64 android SN when target == 100]}; coupons with {@code info/choice} send CP02BC
 *     instead ({@link ChoiceOpen}), get LP023E ({@link #choiceListBody}) and send the pick as this CP0150 with
 *     {@code i32 style} appended ({@code i32 style, i32 style2} for target 101; 5F872F0 block 5F880F4);</li>
 *     <li>{@link Family#SCISSORS} 0x29 / 0x37 (UI 4B54C40, send 4B560B0): {@code i32 tab, i32 slot};</li>
 *     <li>{@link Family#SEAL_UNLOCK} 0x9E (dialog 61B5030, send 61B5580): {@code i32 tab, i32 slot, str PIC, i32 tick};</li>
 *     <li>{@link Family#ANVIL} 0x49 / 0x4B / 0x4C (UI 4B94EB0, send 4B97C90): {@code i32 flag, i32 appearance slot,
 *     i32 target slot} (0x4A writes a widget value in place of the appearance slot and is not claimed);</li>
 *     <li>{@link Family#PET_BOX} 0x9A (UI 61C5790, send 61C6D10): {@code u64 pet SN, i32 pet slot}.</li>
 * </ul>
 * Type 0x09 (effect items such as 5010044) is the switch default: the client sends nothing.
 */
public record Tms282CashUseVariant(int slot, int itemId, int type, Family family, Body body) {
    public static final int OPCODE = 0x0150;
    public static final int PREFIX_LENGTH = 16;

    public enum Family {
        PREFIX_ONLY, TEXT, WHISPER_TEXT, TELEPORT, BEAUTY, SCISSORS, SEAL_UNLOCK, ANVIL, PET_BOX;

        /** Family of a client cash-item type, empty when this parser does not own that type. */
        public static Optional<Family> ofType(int type) {
            return Optional.ofNullable(switch (type) {
                case 0x07, 0x13, 0x1E -> PREFIX_ONLY;
                case 0x0C, 0x0F, 0x10 -> TEXT;
                case 0x0D, 0x0E, 0x82, 0x83, 0x84, 0x85, 0xAD -> WHISPER_TEXT;
                case 0x16, 0x3E -> TELEPORT;
                case 0x01, 0x02, 0x03, 0x1F, 0x63 -> BEAUTY;
                case 0x29, 0x37 -> SCISSORS;
                case 0x9E -> SEAL_UNLOCK;
                case 0x49, 0x4B, 0x4C -> ANVIL;
                case 0x9A -> PET_BOX;
                default -> null;
            });
        }
    }

    public sealed interface Body permits Empty, Text, WhisperText, Teleport, Beauty, InventoryTarget, SealUnlock, Anvil,
            PetBox {
    }

    public record Empty() implements Body {
    }

    public record Text(String text) implements Body {
    }

    /** Chat-item link written by 134A630: kind 1 = an inventory item (tab, slot); kind 2 = (value, number). */
    public record Link(int kind, int a, int b, double number) {
        public boolean hasItem() {
            return kind == 1;
        }
    }

    public record WhisperText(String text, boolean whisper, Link link) implements Body {
    }

    /** byName: u8 1 + name; else u8 0 + field + text + extra (5042xxx: fixedDestination, no more bytes). */
    public record Teleport(boolean byName, String name, int fieldId, String text, int extra, boolean fixedDestination)
            implements Body {
    }

    /**
     * target 0 = the character (Zero / Angelic Buster use 1 for the second look), 100 = android (androidSn).
     * style / style2: the id picked from the LP023E list (choice coupons only; -1 otherwise).
     */
    public record Beauty(int target, boolean flag, long androidSn, int style, int style2) implements Body {
        public boolean chosen() {
            return style >= 0;
        }
    }

    public record InventoryTarget(int tab, int slot) implements Body {
    }

    public record SealUnlock(int tab, int slot, String pic, int tick) implements Body {
    }

    public record Anvil(int flag, int appearanceSlot, int targetSlot) implements Body {
    }

    public record PetBox(long petSn, int petSlot) implements Body {
    }

    /** Item id at offset 12 of a CP0150 prefix, or -1 when the packet is not a CP0150. */
    public static int itemIdOf(byte[] packet) {
        if (packet == null || packet.length < PREFIX_LENGTH) {
            return -1;
        }
        var in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        return Short.toUnsignedInt(in.getShort(0)) == OPCODE ? in.getInt(12) : -1;
    }

    /**
     * Parses a whole CP0150 (opcode included) for an item of client cash type {@code type}.
     *
     * @param teleportKind AF16F0 of the item (only read for teleport types; 4 = world-map selection)
     * @throws IllegalArgumentException for a foreign type, a bad prefix, or a body that is short, long or malformed
     */
    public static Tms282CashUseVariant parse(byte[] packet, int type, int teleportKind, Charset charset) {
        return parse(packet, type, teleportKind, false, charset);
    }

    /**
     * As {@link #parse(byte[], int, int, Charset)}; {@code choice} = the coupon has Item.wz {@code info/choice}, whose
     * CP0150 is the selection sent from the LP023E list (5F872F0 block 5F880F4): the beauty tail then ends with
     * {@code i32 style} ({@code i32 style, i32 style2} for target 101).
     */
    public static Tms282CashUseVariant parse(byte[] packet, int type, int teleportKind, boolean choice, Charset charset) {
        Family family = Family.ofType(type).orElseThrow(() -> new IllegalArgumentException(
                "Cash type 0x" + Integer.toHexString(type) + " has no verified CP0150 layout"));
        if (packet == null || packet.length < PREFIX_LENGTH) {
            throw new IllegalArgumentException("Short cash use");
        }
        var in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        if (Short.toUnsignedInt(in.getShort()) != OPCODE || in.getInt() != 0) {
            throw new IllegalArgumentException("Cash use prefix");
        }
        in.getInt(); // tick
        int slot = Short.toUnsignedInt(in.getShort());
        int itemId = in.getInt();
        if (slot == 0 || slot > Short.MAX_VALUE) {
            throw new IllegalArgumentException("Cash use slot");
        }
        try {
            Body body = switch (family) {
                case PREFIX_ONLY -> new Empty();
                case TEXT -> new Text(string(in, charset));
                case WHISPER_TEXT -> new WhisperText(string(in, charset), in.get() != 0, link(in));
                case TELEPORT -> teleport(in, itemId, charset);
                case BEAUTY -> beauty(in, choice);
                case SCISSORS -> new InventoryTarget(in.getInt(), in.getInt());
                case SEAL_UNLOCK -> new SealUnlock(in.getInt(), in.getInt(), string(in, charset), in.getInt());
                case ANVIL -> new Anvil(in.getInt(), in.getInt(), in.getInt());
                case PET_BOX -> new PetBox(in.getLong(), in.getInt());
            };
            if (in.hasRemaining()) {
                throw new IllegalArgumentException("Cash use trailing bytes: " + in.remaining());
            }
            return new Tms282CashUseVariant(slot, itemId, type, family, body);
        } catch (BufferUnderflowException e) {
            throw new IllegalArgumentException("Short " + family + " body");
        }
    }

    /** The 265 USER_CONSUME_CASH_ITEM_USE_REQUEST body (i32 0, i32 tick, u16 slot, i32 itemId) plus {@code tail}. */
    public static byte[] gms265Body(byte[] packet, byte[] tail) {
        byte[] prefix = Arrays.copyOfRange(packet, 2, PREFIX_LENGTH);
        byte[] out = Arrays.copyOf(prefix, prefix.length + tail.length);
        System.arraycopy(tail, 0, out, prefix.length, tail.length);
        return out;
    }

    private static Teleport teleport(ByteBuffer in, int itemId, Charset charset) {
        int mode = in.get();
        if (mode == 1) {
            return new Teleport(true, string(in, charset), 0, "", 0, false);
        }
        if (mode != 0) {
            throw new IllegalArgumentException("Teleport mode " + mode);
        }
        if (!in.hasRemaining()) {
            if (itemId / 1000 != 5042) {
                throw new IllegalArgumentException("Teleport without destination");
            }
            return new Teleport(false, "", 0, "", 0, true);
        }
        return new Teleport(false, "", in.getInt(), string(in, charset), in.getInt(), false);
    }

    private static Beauty beauty(ByteBuffer in, boolean choice) {
        int target = Byte.toUnsignedInt(in.get());
        boolean flag = in.get() != 0;
        if (target == 0xFF) {
            throw new IllegalArgumentException("Beauty target cancelled");
        }
        long sn = target == 100 ? in.getLong() : 0;
        if (!choice) {
            return new Beauty(target, flag, sn, -1, -1);
        }
        int style = in.getInt();
        int style2 = target == 101 ? in.getInt() : -1;
        return new Beauty(target, flag, sn, style, style2);
    }

    // ---------------------------------------------------------------- CP02BC / LP023E (choice beauty coupons)

    /** CP02BC opcode (5F83680 -> 5F83D2A when the coupon has info/choice). */
    public static final int CHOICE_OPEN_OPCODE = 0x02BC;
    /** LP023E opcode (CWvsContext dispatch 5DF5B20 case 5DF9D24 -> 5F86DE0). */
    public static final int CHOICE_LIST_OPCODE = 0x023E;
    /** LP023E result the client reports as "busy, try later" (5F8F360 code -10 -> StringPool 0x41B6). */
    public static final int CHOICE_FAIL_BUSY = -10;

    /** CP02BC: {@code u16 op, i32 slot, i32 itemId, u8 target [, u64 android SN when target == 100]}. */
    public record ChoiceOpen(int slot, int itemId, int target, long androidSn) {
        public static ChoiceOpen parse(byte[] packet) {
            if (packet == null || packet.length < 11) {
                throw new IllegalArgumentException("Short CP02BC");
            }
            var in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
            if (Short.toUnsignedInt(in.getShort()) != CHOICE_OPEN_OPCODE) {
                throw new IllegalArgumentException("Not CP02BC");
            }
            int slot = in.getInt();
            int itemId = in.getInt();
            int target = Byte.toUnsignedInt(in.get());
            long sn = 0;
            if (target == 100) {
                if (in.remaining() != 8) {
                    throw new IllegalArgumentException("CP02BC android SN");
                }
                sn = in.getLong();
            }
            if (in.hasRemaining()) {
                throw new IllegalArgumentException("CP02BC trailing bytes: " + in.remaining());
            }
            if (slot <= 0 || slot > Short.MAX_VALUE || target == 0xFF) {
                throw new IllegalArgumentException("CP02BC slot / target");
            }
            return new ChoiceOpen(slot, itemId, target, sn);
        }
    }

    /**
     * LP023E body read by 5F86DE0: {@code u8 target, i32 itemId, i32 result}; result 1 continues with
     * {@code i32 slot [, u64 SN when target 100], u8 n, n x i32 style [, u8 m, m x i32 for target 101]} and opens the
     * choice UI (5F872F0); any other result shows 5F8B3C0's message. The handler clears the exclusive flag itself.
     */
    public static byte[] choiceListBody(int target, int itemId, int result, int slot, long androidSn, int[] styles) {
        var out = ByteBuffer.allocate(32 + 4 * (styles == null ? 0 : styles.length)).order(ByteOrder.LITTLE_ENDIAN);
        out.put((byte) target).putInt(itemId).putInt(result);
        if (result == 1) {
            if (styles == null || styles.length == 0 || styles.length > 255 || target == 101) {
                throw new IllegalArgumentException("LP023E needs 1..255 styles for one list");
            }
            out.putInt(slot);
            if (target == 100) {
                out.putLong(androidSn);
            }
            out.put((byte) styles.length);
            for (int s : styles) {
                out.putInt(s);
            }
        }
        return Arrays.copyOf(out.array(), out.position());
    }

    private static Link link(ByteBuffer in) {
        int kind = in.getInt();
        return switch (kind) {
            case 1 -> new Link(kind, in.getInt(), in.getInt(), 0);
            case 2 -> new Link(kind, in.getInt(), 0, in.getDouble());
            default -> new Link(kind, 0, 0, 0);
        };
    }

    private static String string(ByteBuffer in, Charset charset) {
        int n = Short.toUnsignedInt(in.getShort());
        if (n > in.remaining()) {
            throw new IllegalArgumentException("String past the end");
        }
        byte[] b = new byte[n];
        in.get(b);
        return new String(b, charset);
    }
}
