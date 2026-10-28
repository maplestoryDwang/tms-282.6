package net.swordie.ms.connection.packet;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * CP0205 buddy list request of TMS 282.4 (multiplayer-0930 RE of client-2823; the in-game map's CP0204
 * "FRIEND_REQUEST" label was wrong: 0x204 has one sender 5E30400 that sends a string list, a log). Fifteen static
 * senders, each {@code Encode1 type} first; 282 renumbered the types and every id is an ACCOUNT id (the client's
 * friend lookup 600D200 matches record +0x29):
 * <pre>
 * type  sender    body after the type                           265 FRIEND_REQUEST                evidence
 *  1    3809920   Str name, Str group, Str memo, Str nick        1 Str,Str,Str,u8 1,Str (account)  證實
 *  2    3734930   i32 account                                    3 AccountAccept i32               證實
 *  3    3734480   i32 account, Str name                          5 AccountDelete i32               推定(強)
 *  4    3734630   i32 account, u8 auto, Str ""                   7 AccountRejected i32             證實
 *  5    535E290   (none) reject every pending request            7 per pending request             證實
 *  6    37342F0   i32, i32, Str nick, Str memo (modify)          - (logged)                        推定
 *  7    3734C30   i32 n, n x (i32 account, Str group)            14 AccountGroupModify per entry   推定
 *  8    5EE59B0   u8 status (idle 3 / 0)                         - (ignored)                       推定
 *  9..B           i32 id (best friend invite / accept / refuse)  - (logged)                        推定
 *  C    600E460   i32 n, n x (u8 index, Str group) group names   - (sent unasked by 6010070)       證實
 * </pre>
 */
public sealed interface Tms282FriendRequest {
    int OPCODE = 0x0205;

    int type();

    /** Add: 265 type 1 as an account friend (the 282 list only has account friends) with the nickname. */
    record Add(String name, String group, String memo, String nick) implements Tms282FriendRequest {
        public int type() {
            return 1;
        }

        public byte[] toGms265Body() {
            return new Gms265BodyWriter().u8(1).str(name).str(group).str(memo).u8(1).str(nick).bytes();
        }
    }

    /** 2 accept -> 265 3, 3 delete -> 265 5, 4 reject -> 265 7: one i32 account id. */
    record ByAccount(int type, int accountId, int auto, String text) implements Tms282FriendRequest {
        public int gms265Type() {
            return switch (type) {
                case 2 -> 3;
                case 3 -> 5;
                case 4 -> 7;
                default -> throw new IllegalStateException("type " + type);
            };
        }

        public byte[] toGms265Body() {
            return new Gms265BodyWriter().u8(gms265Type()).i32(accountId).bytes();
        }
    }

    record RejectAll() implements Tms282FriendRequest {
        public int type() {
            return 5;
        }
    }

    record GroupMove(String group, int accountId) {
        /** 265 AccountGroupModify (14): i32 account, Str group. */
        public byte[] toGms265Body() {
            return new Gms265BodyWriter().u8(14).i32(accountId).str(group).bytes();
        }
    }

    record MoveGroups(List<GroupMove> moves) implements Tms282FriendRequest {
        public int type() {
            return 7;
        }
    }

    /** 6 modify, 8 status, 9..B best friend, C group names: parsed for the log, no 265 action. */
    record Other(int type, byte[] rest) implements Tms282FriendRequest {
    }

    static Tms282FriendRequest parse(byte[] packet, Charset charset) {
        Objects.requireNonNull(charset, "charset");
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, OPCODE, "friend request");
        int type = in.u8();
        Tms282FriendRequest r = switch (type) {
            case 1 -> new Add(in.str(charset), in.str(charset), in.str(charset), in.str(charset));
            case 2 -> new ByAccount(2, in.i32(), 0, "");
            case 3 -> new ByAccount(3, in.i32(), 0, in.str(charset));
            case 4 -> new ByAccount(4, in.i32(), in.u8(), in.str(charset));
            case 5 -> new RejectAll();
            case 7 -> {
                int n = in.i32();
                if (n < 0 || n > 200) {
                    throw new IllegalArgumentException("friend group move count " + n);
                }
                List<GroupMove> moves = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    int account = in.i32();
                    moves.add(new GroupMove(in.str(charset), account));
                }
                yield new MoveGroups(moves);
            }
            default -> new Other(type, in.rest());
        };
        in.end();
        return r;
    }
}
