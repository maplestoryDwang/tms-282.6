package net.swordie.ms.connection.packet;

import net.swordie.ms.enums.CashItemType;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntPredicate;

/**
 * CP0741 CASH_SHOP_CASH_ITEM_REQUEST (265 InHeader 0x717, +0x2A like the other CPs of this range): u8 kind, then a
 * per-kind body. Native senders define fixed field positions; unsupported mutations fail closed:
 * <pre>
 * kind 0x02 buy          21326F0/21329B2: u8 useMaple ([rbx+178]==2), two option bytes, i32 commodity SN,
 *                        i32 selected-offer id (0 for none), i32 batch item-count hint. One commodity per entry.
 * kind 0x22 package      same branch: u8 useMaple, i32 SN, i32 batch hint (one package per request)
 * kind 0x2F limit query  2213400: i32 SN, i32 scope; answered separately with LP0165, never a buy failure
 * kind 0x0B locker->slot 2130FB0 (2131393): Buf8 SN, i32 item, u8 destination inventory, u16 empty destination slot
 * kind 0x0C slot->locker 2131500 (21315F9): Buf8 SN, i32 item, u8 source inventory, i32 occupied source slot
 * </pre>
 * All three senders set the shop's pending-request flag (CCashShop+0x88 = 1) that a CASH_ITEM_RESULT clears, so
 * every request must be answered. 346A070 does not append a tail u32 for 0x741.
 */
public record Tms282CashItemRequest(int kind, byte[] body) {
    public static final int OPCODE = 0x0741;
    public static final int BUY = 0x02;
    public static final int BUY_PACKAGE = 0x22;
    public static final int MOVE_S_TO_L = 0x0C;
    public static final int MOVE_L_TO_S = 0x0B;
    public record Expansion(int kind,int currency,int commoditySn) { }
    /**2132A8F: kinds5/6 are inventory/storage expansion, independently handled (never legacy forwarding). */
    public Expansion expansion() {
        if(kind!=5 && kind!=6)throw new IllegalArgumentException("Not an expansion request");
        expect(11);var in=ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN);
        int maple=Byte.toUnsignedInt(in.get()),sn=in.getInt(),option=Byte.toUnsignedInt(in.get()),extra=Byte.toUnsignedInt(in.get()),offer=in.getInt();
        if(maple>1 || sn<=0 || option!=0 || extra!=0 || offer!=0)throw new IllegalArgumentException("Unsupported expansion offer");
        return new Expansion(kind,maple==1?2:1,sn);
    }
    static final int BUY_LENGTH = 1 + 1 + 1 + 4 + 4 + 4;
    static final int MOVE_S_TO_L_LENGTH = 8 + 4 + 1 + 4;
    static final int MOVE_L_TO_S_LENGTH = 8 + 4 + 1 + 2;

    public Tms282CashItemRequest {
        body = Objects.requireNonNull(body, "body").clone();
    }

    @Override
    public byte[] body() {
        return body.clone();
    }

    /** The full plaintext packet, opcode included. Any kind is accepted; {@link #toGms265Body} checks the body. */
    public static Tms282CashItemRequest parse(byte[] packet) {
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, OPCODE, "CP0741");
        int kind = in.u8();
        return new Tms282CashItemRequest(kind, in.rest());
    }

    public boolean supported() {
        return kind == BUY || kind == BUY_PACKAGE || kind == MOVE_S_TO_L || kind == MOVE_L_TO_S || kind == 5 || kind == 6;
    }

    /** Native 2213400: commodity SN and count scope, used while browsing limited goods. */
    public int[] purchaseCountQuery() {
        if (kind != 0x2F || body.length != 8) throw new IllegalArgumentException("Invalid purchase-count query");
        ByteBuffer in=ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN);
        int sn=in.getInt(), scope=in.getInt();
        if (sn<=0 || (scope!=3 && scope!=4)) throw new IllegalArgumentException("Unsupported purchase-count scope");
        return new int[]{sn,scope};
    }

    /**
     * The 265 CASH_SHOP_CASH_ITEM_REQUEST body (265 kind byte first) for a supported kind, else empty.
     *
     * @param isCommodity validates the SN at its native fixed offset; fields are never guessed or swapped
     * @throws IllegalArgumentException when the body of a supported kind has the wrong length
     */
    public Optional<byte[]> toGms265Body(IntPredicate isCommodity) {
        ByteBuffer in = ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN);
        switch (kind) {
            case BUY -> {
                expect(BUY_LENGTH);
                byte flag = in.get();
                byte option = in.get();
                byte extra = in.get();
                // 213288F loads the SN into r14d; 5ED9F10 resolves that commodity.
                // 2132A71 writes shop+2B0, copied at214EE58 from dialog+28C8.
                // 213E775/213E78A fills that value from the context's selected offer list,
                // NOT from a quantity input. Live ordinary buys send zero. Each recursive
                // 21326F0 invocation processes one purchase entry; do not multiply by batchHint.
                int sn = in.getInt(), selectedOffer = in.getInt(), batchHint = in.getInt();
                if(flag<0 || flag>1 || batchHint<0 || batchHint>40000)throw new IllegalArgumentException("Invalid purchase flags");
                if(selectedOffer!=0)throw new IllegalArgumentException("Selected cash offer is not supported");
                int count = 1;
                int payment = flag==1?2:1; // 213288F/2131A20: mode2 is maple points; bit at21328E6.
                validateBuy(isCommodity, sn, count, payment);
                return Optional.of(ByteBuffer.allocate(1 + BUY_LENGTH).order(ByteOrder.LITTLE_ENDIAN)
                        .put((byte) CashItemType.Req_Buy.getVal()).put(flag).put((byte) payment).put(option).putInt(extra)
                        .putInt(sn).putInt(count).array());
            }
            case BUY_PACKAGE -> {
                expect(9); // Native package branch omits two option bytes and the quantity field.
                byte flag=in.get();int sn=in.getInt(), batchHint=in.getInt();
                if(flag<0 || flag>1 || batchHint<0 || batchHint>40000)throw new IllegalArgumentException("Invalid package flags");
                int payment=flag==1?2:1;
                validateBuy(isCommodity,sn,1,payment);
                return Optional.of(ByteBuffer.allocate(14).order(ByteOrder.LITTLE_ENDIAN)
                        .put((byte)CashItemType.Req_BuyPackage.getVal()).put(flag).putInt(payment).putInt(sn).putInt(1).array());
            }
            case MOVE_S_TO_L -> {
                expect(MOVE_S_TO_L_LENGTH);
                long sn=in.getLong();int item=in.getInt();int inventory=Byte.toUnsignedInt(in.get());int slot=in.getInt();
                if(sn<=0 || item<=0 || (inventory!=5 && inventory!=6) || slot<=0 || slot>Short.MAX_VALUE)
                    throw new IllegalArgumentException("Invalid cash inventory source");
                return Optional.of(ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
                        .put((byte) CashItemType.Req_MoveStoL.getVal()).putLong(sn).putInt(item).put((byte)inventory).putShort((short)slot).array());
            }
            case MOVE_L_TO_S -> {
                expect(MOVE_L_TO_S_LENGTH);
                long sn=in.getLong();int item=in.getInt();int inventory=Byte.toUnsignedInt(in.get());int slot=Short.toUnsignedInt(in.getShort());
                if(sn<=0 || item<=0 || inventory<1 || inventory>6 || slot<=0 || slot>Short.MAX_VALUE)
                    throw new IllegalArgumentException("Invalid cash locker destination");
                // Legacy handler allocates its own free slot and returns the actual destination in kind32.
                return Optional.of(ByteBuffer.allocate(1 + 8 + 4 + 1).order(ByteOrder.LITTLE_ENDIAN)
                        .put((byte) CashItemType.Req_MoveLtoS.getVal()).put(body, 0, 8 + 4 + 1).array());
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    /** The 265 failure kind to answer a request of this kind with (a buy for unsupported kinds). */
    public CashItemType failureType() {
        return switch (kind) {
            case MOVE_S_TO_L -> CashItemType.Res_MoveStoL_Failed;
            case MOVE_L_TO_S -> CashItemType.Res_MoveLtoS_Failed;
            default -> CashItemType.Res_Buy_Failed;
        };
    }

    private void expect(int length) {
        if (body.length != length) {
            throw new IllegalArgumentException(String.format("CP0741 kind 0x%02X: %d body bytes, expected %d", kind,
                    body.length, length));
        }
    }

    private static void validateBuy(IntPredicate known, int sn, int count, int payment) {
        if(!known.test(sn) || count<1 || count>100 || payment<1 || payment>3)
            throw new IllegalArgumentException("Invalid commodity, quantity or cash payment method");
    }

    @Override
    public String toString() {
        return String.format("CP0741 cash item kind=0x%02X body[%d]=%s", kind, body.length,
                HexFormat.of().formatHex(body, 0, Math.min(body.length, 48)));
    }
}
