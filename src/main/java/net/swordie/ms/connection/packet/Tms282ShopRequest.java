package net.swordie.ms.connection.packet;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * CP012E USER_SHOP_REQUEST [verified opcode; 265 0x12B], one sender per mode (static layouts):
 * <pre>
 * 0 BUY      (4352685): u8 0, u16 index, i32 itemId, i32 quantity, i32 (shop field +0x5CC), str + i32 (8E5590)
 * 1 SELL     (4352FBB): u8 1, u16 slot, i32 itemId, i32 quantity
 * 2 RECHARGE (43534CC): u8 2, u16 slot
 * 3 CLOSE    (43535B9): u8 3
 * 4 SELL_ALL (4353209): u8 4, i32 n, n x (u16 slot, i32 itemId, i32 1)
 * </pre>
 * 265 has the same modes, with an i16 quantity (BUY, SELL, SELL_ALL) and without the BUY tail. {@link #toGms265Body}
 * rewrites a request for the 265 NpcHandler.handleUserShopRequest. The shop itself is opened by LP0962 SHOP_OPEN
 * ({@link Tms282ShopPackets}); the route is {@code Tms282DailyPlayHandlers}.
 */
public sealed interface Tms282ShopRequest {
    int OPCODE = 0x012E;

    int mode();

    /** The 265 body (no opcode). */
    byte[] toGms265Body();

    record Buy(int index, int itemId, int quantity, int shopValue, String shopTag, int shopTagValue) implements Tms282ShopRequest {
        public int mode() {
            return 0;
        }

        public byte[] toGms265Body() {
            return le(9).put((byte) 0).putShort((short) index).putInt(itemId).putShort(quantity265(quantity)).array();
        }
    }

    record Sell(int slot, int itemId, int quantity) implements Tms282ShopRequest {
        public int mode() {
            return 1;
        }

        public byte[] toGms265Body() {
            return le(9).put((byte) 1).putShort((short) slot).putInt(itemId).putShort(quantity265(quantity)).array();
        }
    }

    record Recharge(int slot) implements Tms282ShopRequest {
        public int mode() {
            return 2;
        }

        public byte[] toGms265Body() {
            return le(3).put((byte) 2).putShort((short) slot).array();
        }
    }

    record Close() implements Tms282ShopRequest {
        public int mode() {
            return 3;
        }

        public byte[] toGms265Body() {
            return new byte[]{3};
        }
    }

    record SellAll(List<Sell> items) implements Tms282ShopRequest {
        public SellAll {
            items = List.copyOf(items);
        }

        public int mode() {
            return 4;
        }

        public byte[] toGms265Body() {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            b.writeBytes(le(5).put((byte) 4).putInt(items.size()).array());
            for (Sell s : items) {
                b.writeBytes(le(8).putShort((short) s.slot()).putInt(s.itemId()).putShort(quantity265(s.quantity())).array());
            }
            return b.toByteArray();
        }
    }

    static Tms282ShopRequest parse(byte[] packet, Charset charset) {
        Objects.requireNonNull(charset, "charset");
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, OPCODE, "shop request");
        int mode = in.u8();
        Tms282ShopRequest request = switch (mode) {
            // 8E5590 writes the string, then the i32 at source+0x1F. It is part of BUY, not a socket trailer.
            case 0 -> new Buy(in.u16(), in.i32(), in.i32(), in.i32(), in.str(charset), in.i32());
            case 1 -> new Sell(in.u16(), in.i32(), in.i32());
            case 2 -> new Recharge(in.u16());
            case 3 -> new Close();
            case 4 -> {
                int n = in.i32();
                if (n < 0 || n > 200) {
                    throw new IllegalArgumentException("sell-all count " + n);
                }
                List<Sell> items = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    items.add(new Sell(in.u16(), in.i32(), in.i32()));
                }
                yield new SellAll(items);
            }
            default -> throw new IllegalArgumentException("unknown shop mode " + mode);
        };
        in.end();
        return request;
    }

    private static ByteBuffer le(int n) {
        return ByteBuffer.allocate(n).order(ByteOrder.LITTLE_ENDIAN);
    }

    private static short quantity265(int quantity) {
        if (quantity < 0 || quantity > Short.MAX_VALUE) {
            throw new IllegalArgumentException("quantity " + quantity + " does not fit the 265 i16");
        }
        return (short) quantity;
    }
}
