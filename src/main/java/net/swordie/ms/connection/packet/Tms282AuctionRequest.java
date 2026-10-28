package net.swordie.ms.connection.packet;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/**
 * CP013B AUCTION_HOUSE_REQUEST (282 = 265 0x136 + 5, 17 static senders of COutPacket(0x13B)), {@code u8 type} and a body
 * per type. The write order of every sender (research/tms2823/auction-trace/README.md) against 265
 * {@code AuctionHouseHandler.handleActionHouseRequest}:
 * <pre>
 *  0 home list      2CB0DB0          -                              265 Req_ShowHomeList: -
 *  5 sell           27DE1C0          u8 inv, i32 pos, i32 item, i32 qty, i64 price, i32 hours   = 265 Req_Sell
 *  6 list again     27D0DA0          i64, i32, i32, i32               (265 Req_ItemListAgain: no case)
 *  7 cancel sell    27DA580          i64 id, i32 world                265 reads i64
 * 10 buy            2CB5590          i64 id, i32 world, i32 item, i32 qty, i64 price, i32        = 265 Req_Buy
 * 11 buy amount     2CB5E10          sub 6E7750 {i32, i64, i32, i32}  (265 Req_Buy_Amount: no case)
 * 21                27D3370          i64, i32, i32, u8                (no 265 type)
 * 25 wish add       2CA9AC0          i64 id, u8, [i32]                265 reads i64
 * 26 wish remove    27D53A0          i64 id, i32 world                265 reads i64
 * 32 search         2CAB0B0          u8, [str, str, u8, i32, i32, u8, i32, ...] (265 Req_Show is -1: unreachable)
 * 50 selling list   27DF070          -                              = 265 Req_ShowSellList
 * 52 history        27D4C90          -                              = 265 Req_History
 * 54 wish list      27D55D0          -                              = 265 Req_WishListShow
 * 57 / 60 / 65      2CA90E0 / 27C6690 / 27DE1C0: u8 / i32 / i32 Buf8 i64 i64 (no 265 type)
 * </pre>
 * The 265 handler reads a prefix of each body, so a {@link #passThrough} type goes on as sent (type included).
 */
public record Tms282AuctionRequest(int type, byte[] body, String searchName, String describe) {
    public static final int OPCODE = 0x013B;
    /** CP0100 USER_MIGRATE_TO_AUCTION_HOUSE_REQUEST (5DEF680: i32 only). */
    public static final int ENTER = 0x0100;
    /** CP013C AUCTION_LEAVE_REQUEST (2CBFCF0: no body). */
    public static final int LEAVE = 0x013C;

    public static final int HOME = 0, SELL = 5, CANCEL = 7, BUY = 10, WISH_ADD = 25, WISH_REMOVE = 26, SEARCH = 32,
            SELL_LIST = 50, HISTORY = 52, WISH_LIST = 54;

    public Tms282AuctionRequest {
        body = body.clone();
        Objects.requireNonNull(describe, "describe");
    }

    @Override
    public byte[] body() {
        return body.clone();
    }

    /** Types whose 282 body starts with what the 265 handler reads. */
    public boolean passThrough() {
        return switch (type) {
            case HOME, SELL, CANCEL, BUY, WISH_ADD, WISH_REMOVE, SELL_LIST, HISTORY, WISH_LIST -> true;
            default -> false;
        };
    }

    /** The whole plaintext packet, opcode included. */
    public static Tms282AuctionRequest parse(byte[] packet, Charset charset) {
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, OPCODE, "CP013B");
        int type = in.u8();
        String name = null;
        switch (type) {
            case HOME, SELL_LIST, HISTORY, WISH_LIST -> in.end();
            case SELL -> {
                in.u8();
                in.i32();
                in.i32();
                in.i32();
                in.i64();
                in.i32();
                in.end();
            }
            case CANCEL, WISH_REMOVE -> {
                in.i64();
                in.i32();
                in.end();
            }
            case BUY -> {
                in.i64();
                in.i32();
                in.i32();
                in.i32();
                in.i64();
                in.i32();
                in.end();
            }
            case WISH_ADD -> {
                in.i64();
                in.u8();
                if (in.remaining() >= 4) {
                    in.i32();
                }
                in.end();
            }
            case SEARCH -> {
                in.u8();
                if (in.remaining() >= 4) {
                    String a = in.str(charset);
                    String b = in.str(charset);
                    name = !b.isBlank() ? b : a;
                }
                // the filters (level, price, potential, options) are not used: the rest is kept unread
            }
            default -> {
                // not ported: kept raw for the log
            }
        }
        byte[] body = Arrays.copyOfRange(packet, 2, packet.length);
        String describe = String.format("CP013B auction type %d%s body=%s", type,
                name == null ? "" : " name=\"" + name + "\"", HexFormat.of().formatHex(body, 0, Math.min(body.length, 48)));
        return new Tms282AuctionRequest(type, body, name, describe);
    }
}
