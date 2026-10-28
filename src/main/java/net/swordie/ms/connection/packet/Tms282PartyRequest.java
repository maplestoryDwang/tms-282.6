package net.swordie.ms.connection.packet;

import java.nio.charset.Charset;
import java.util.Objects;

/**
 * CP01F2 PARTY_REQUEST of TMS 282.3 [opcode 證實: ingame-cp-map-2823, 20 senders 2FD2160..2FD4170, anchors Δ=+0xB].
 * The type byte is the first Encode1 of each sender; the 265 PartyHandler.handlePartyRequest body is rebuilt:
 * <pre>
 * 282 type  sender   282 body                         265 type / body
 *  1 create 2FD2160  Str name, u8 private, Buf1, Buf4   1  Str, u8, i32 (the new Buf1 is dropped)
 *  3 leave  2FD2780  u8                                  3  (nothing)
 *  4 invite 2FD2360  Str name, Buf4                      4  Str, i32
 *  6 kick   2FD2AA0  i32 character                       6  i32
 *  7 leader 2FD28C0  i32 character                       7  i32
 *  8 apply  2FD3290  i32 party                           8  i32
 * 14 setting 2FD2200 Str name, u8 private, Buf1, Buf4  13  Str, u8, i32 (the new Buf1 is dropped)
 * </pre>
 * The other 282 types (5 intrusion invite by name, 17..27 recruitment / boss party) have no 265 counterpart and are
 * {@link Other}: logged, not handed to 265. Type meanings: 1/4/6/7/8 equal 265 and the body shapes match; 3 and 14
 * by shape and order [推定].
 */
public sealed interface Tms282PartyRequest {
    int OPCODE = 0x01F2;

    int type();

    /** The 265 PARTY_REQUEST body (no opcode), or null for {@link Other}. */
    byte[] toGms265Body();

    record Create(String name, int privateFlag, int extra, int setting) implements Tms282PartyRequest {
        public int type() {
            return 1;
        }

        public byte[] toGms265Body() {
            return new Gms265BodyWriter().u8(1).str(name).u8(privateFlag).i32(setting).bytes();
        }
    }

    record Leave(int flag) implements Tms282PartyRequest {
        public int type() {
            return 3;
        }

        public byte[] toGms265Body() {
            return new Gms265BodyWriter().u8(3).bytes();
        }
    }

    record Invite(String name, int value) implements Tms282PartyRequest {
        public int type() {
            return 4;
        }

        public byte[] toGms265Body() {
            return new Gms265BodyWriter().u8(4).str(name).i32(value).bytes();
        }
    }

    /** 6 kick, 7 change leader, 8 apply: one i32 in both versions. */
    record ById(int type, int id) implements Tms282PartyRequest {
        public byte[] toGms265Body() {
            return new Gms265BodyWriter().u8(type).i32(id).bytes();
        }
    }

    record Setting(String name, int privateFlag, int extra, int setting) implements Tms282PartyRequest {
        public int type() {
            return 14;
        }

        public byte[] toGms265Body() {
            return new Gms265BodyWriter().u8(13).str(name).u8(privateFlag).i32(setting).bytes();
        }
    }

    record Other(int type, byte[] rest) implements Tms282PartyRequest {
        public byte[] toGms265Body() {
            return null;
        }
    }

    static Tms282PartyRequest parse(byte[] packet, Charset charset) {
        Objects.requireNonNull(charset, "charset");
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, OPCODE, "party request");
        int type = in.u8();
        Tms282PartyRequest r = switch (type) {
            case 1 -> new Create(in.str(charset), in.u8(), in.u8(), in.i32());
            case 3 -> new Leave(in.u8());
            case 4 -> new Invite(in.str(charset), in.i32());
            case 6, 7, 8 -> new ById(type, in.i32());
            case 14 -> new Setting(in.str(charset), in.u8(), in.u8(), in.i32());
            default -> new Other(type, in.rest());
        };
        in.end();
        return r;
    }
}
