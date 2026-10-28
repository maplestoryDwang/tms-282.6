package net.swordie.ms.connection.packet;

import java.nio.charset.Charset;
import java.util.HexFormat;
import java.util.Objects;

/**
 * CP01F0 MINI_ROOM (TMS 282.3) for the trading room, parsed and rewritten as the 265 body that
 * {@code RoomHandler.handleMiniRoom} reads (u8 265 type + the 265 fields). Research: research/tms2823/miniroom-trace.
 * <p>
 * The request type is the first byte. As in LP097F, the base types are the 265 value + 2 and the dialog types keep
 * the 265 value [base senders verified statically: 3E48EA0 accept/decline, 3E4AD60 invite, 3E4CCB0 chat, the 20
 * "leave" senders; dialog types presumed from the matching LP097F dialog cases 0/4/8]:
 * <pre>
 *  282  body (282)                    265  body (265)
 *   21  i32 serial, u8 0, u8 0         19  accept an invitation                (3E48EA0, result 0)
 *   24  i32 invitee character id       22  i32 character id                    (3E4AD60 via 3E4B0DA)
 *   25  i32 serial, u8 result          23  decline an invitation               (3E48EA0, result != 0)
 *   27  i32 tick, Str text             25  i32 tick, Str (UTF-8)               (3E4CCB0)
 *   31  u8                             29  leave                               (20 senders "1 1")
 * 0..3  u8 inv, i16 slot, i16 count, u8 trade slot   = 265 PlaceItem (4 obfuscated variants)
 * 4..7  i64 mesos                                     = 265 SetMesos
 * 8..11 (nothing)                                     = 265 Trade / TradeConfirm*
 *   76  u8 hand 0..2                  100  RPSResult u8 hand                  (battle RPS dialog 1FED860, 265 - 24)
 * </pre>
 * Personal shop / hired merchant rooms do not exist in the 282 client (room factory 3E507F0 has no types 5/6/7).
 * Other types (mini games, battle RPS ready 74 from 1FED5C0, the RPS invite is not found) have no 265 counterpart here: {@link #toGms265Body()}
 * returns null and the route only logs them.
 */
public record Tms282MiniRoomRequest(int type, byte[] body, byte[] gms265Body, String description) {
    public static final int OPCODE = 0x01F0;

    public Tms282MiniRoomRequest {
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(description, "description");
        body = body.clone();
        gms265Body = gms265Body == null ? null : gms265Body.clone();
    }

    /** The 265 body (u8 type + fields) for RoomHandler, or null when the type has no 265 counterpart. */
    public byte[] toGms265Body() {
        return gms265Body == null ? null : gms265Body.clone();
    }

    public static Tms282MiniRoomRequest parse(byte[] packet, Charset charset) {
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, OPCODE, "CP01F0 mini room");
        int type = in.u8();
        byte[] body = in.rest();
        Tms282FieldRequestReader r = Tms282FieldRequestReader.open(packet, OPCODE, "CP01F0 mini room");
        r.u8();
        Gms265BodyWriter w = new Gms265BodyWriter();
        String what;
        switch (type) {
            case 21 -> {
                int serial = r.i32();
                r.u8();
                r.u8();
                w.u8(19);
                what = "accept invitation serial " + serial;
            }
            case 24 -> {
                int invitee = r.i32();
                w.u8(22).i32(invitee);
                what = "invite character " + invitee;
            }
            case 25 -> {
                int serial = r.i32();
                int result = r.u8();
                w.u8(23);
                what = "decline invitation serial " + serial + " result " + result;
            }
            case 27 -> {
                int tick = r.i32();
                String text = r.str(charset);
                w.u8(25).i32(tick).str(text);
                what = "chat \"" + text + "\"";
            }
            case 31 -> {
                w.u8(29);
                what = "leave";
            }
            case 0, 1, 2, 3 -> {
                int inv = r.u8();
                short slot = r.i16();
                short count = r.i16();
                int tradeSlot = r.u8();
                w.u8(type).u8(inv).u16(slot).u16(count).u8(tradeSlot);
                what = String.format("put item inv %d slot %d count %d -> trade slot %d", inv, slot, count, tradeSlot);
            }
            case 4, 5, 6, 7 -> {
                long mesos = r.i64();
                w.u8(type).i64(mesos);
                what = "put mesos " + mesos;
            }
            case 76 -> {
                int hand = r.u8();
                w.u8(100).u8(hand);
                what = "battle RPS hand " + hand;
            }
            case 8, 9, 10, 11 -> {
                w.u8(type);
                what = "trade (confirm)";
            }
            default -> {
                return new Tms282MiniRoomRequest(type, body, null, "type " + type + " (no 265 counterpart) "
                        + HexFormat.of().formatHex(body));
            }
        }
        return new Tms282MiniRoomRequest(type, body, w.bytes(), what);
    }

    @Override
    public String toString() {
        return "type " + type + ": " + description;
    }
}
