package net.swordie.ms.connection.packet;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;

/**
 * CP0741 CASH_SHOP_CASH_ITEM_REQUEST bodies (after the u8 kind of {@link Tms282CashItemRequest}) that the 265 handler
 * has no equivalent for. Layouts from the static client (research/tms2823/cash-gift-wish-20260928):
 * <pre>
 * kind 0x03 gift       2133030 (send 2133759..2133F02): str second-password ("Passed" when the ctx+38C flag is set,
 *                      else the entered password; 21390F0), u8 option (dialog 214EF00 out byte), i32 commodity SN,
 *                      i32 selected offer (shop+2B0), str receiver name, str message, u8 0.
 *                      The kind byte is 0x21 / 0x27 / 0x23 for special items (215CEA0 / 215CEE0 / 91xxxxx); only 3
 *                      is handled here.
 * kind 0x04 set wish   212FFD0 (mode 0, add; refuses when 12 entries or duplicate) / 2130600 (mode 1, remove):
 *                      u8 mode, i32 SN, Buf48 = 12 x i32 wish-list SNs (2142AC0, zero padded).
 * </pre>
 * Strings are CP0741's COutPacket::EncodeStr (F91180): u16 length + bytes in the client charset.
 */
public final class Tms282CashShopRequests {
    public static final int GIFT = 0x03;
    public static final int SET_WISH = 0x04;
    public static final int WISH_SIZE = 12;

    private Tms282CashShopRequests() {
    }

    public record Gift(String secondPassword, int option, int sn, int selectedOffer, String receiver, String message) {
        public Gift {
            Objects.requireNonNull(receiver, "receiver");
            Objects.requireNonNull(message, "message");
        }
    }

    public record SetWish(int mode, int sn, int[] list) {
        public boolean add() {
            return mode == 0;
        }

        @Override
        public int[] list() {
            return list.clone();
        }

        @Override
        public String toString() {
            return "SetWish[mode=" + mode + ", sn=" + sn + ", list=" + Arrays.toString(list) + "]";
        }
    }

    /** Kind 3 body. */
    public static Gift gift(byte[] body, Charset charset) {
        try {
            ByteBuffer in = ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN);
            String password = str(in, charset, 64);
            int option = Byte.toUnsignedInt(in.get());
            int sn = in.getInt();
            int offer = in.getInt();
            String receiver = str(in, charset, 64);
            String message = str(in, charset, 512);
            if (in.remaining() >= 1) in.get(); // 2133EF4 u8 0
            if (in.hasRemaining()) {
                System.out.printf("[TMS282] CP0741 gift: %d unexpected trailing bytes ignored%n", in.remaining());
            }
            if (sn <= 0) throw new IllegalArgumentException("CP0741 gift: invalid SN");
            return new Gift(password, option, sn, offer, receiver.trim(), message);
        } catch (BufferUnderflowException e) {
            throw new IllegalArgumentException("CP0741 gift: truncated body");
        }
    }

    /** Kind 4 body: 1 + 4 + 48 bytes. */
    public static SetWish setWish(byte[] body) {
        if (body.length != 1 + 4 + 4 * WISH_SIZE) {
            throw new IllegalArgumentException("CP0741 set wish: " + body.length + " body bytes, expected 53");
        }
        ByteBuffer in = ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN);
        int mode = Byte.toUnsignedInt(in.get());
        int sn = in.getInt();
        int[] list = new int[WISH_SIZE];
        for (int i = 0; i < WISH_SIZE; i++) list[i] = in.getInt();
        if (mode > 1) throw new IllegalArgumentException("CP0741 set wish: mode " + mode);
        return new SetWish(mode, sn, list);
    }

    private static String str(ByteBuffer in, Charset charset, int max) {
        int length = Short.toUnsignedInt(in.getShort());
        if (length > max) throw new IllegalArgumentException("CP0741: string of " + length + " bytes");
        byte[] bytes = new byte[length];
        in.get(bytes);
        return new String(bytes, charset);
    }
}
