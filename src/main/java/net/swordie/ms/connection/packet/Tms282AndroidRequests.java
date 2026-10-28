package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.protocol.codec.MovePathCodec;
import net.swordie.ms.life.movement.MovementInfo;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * TMS 282.3 android client requests (ingame-cp-map-2823.tsv; none of them is in the 346A070 tail-u32 list). Each
 * record parses the whole plaintext packet strictly (opcode included) and gives the 265 body where a 265 handler
 * takes over ({@code AndroidHandler}).
 */
public final class Tms282AndroidRequests {
    private Tms282AndroidRequests() {
    }

    /**
     * CP02FE ANDROID_MOVE [presumed opcode: +0xE shift of 2C9..2F5, 11 anchors; sender 5C5BE60]: u32 (265 reads and
     * ignores it), then CMovePath::Encode 3FDB6B0 (the CP0106 path encoder, 282 path form).
     */
    public record Move(int unknown, MovementInfo path) {
        public static final int OPCODE = 0x02FE;

        public Move {
            Objects.requireNonNull(path, "path");
        }

        public static Move parse(byte[] packet, MovePathCodec paths) {
            Tms282FieldRequestReader r = Tms282FieldRequestReader.open(packet, OPCODE, "CP02FE");
            int unknown = r.i32();
            MovementInfo path = r.path(paths);
            r.end();
            return new Move(unknown, path);
        }
    }

    /** CP02FF ANDROID_ACTION_SET [verified: sender 1D11150 writes u8, u8 as 265]: action, random key. */
    public record Action(int action, int randomKey) {
        public static final int OPCODE = 0x02FF;

        public static Action parse(byte[] packet) {
            Tms282FieldRequestReader r = Tms282FieldRequestReader.open(packet, OPCODE, "CP02FF");
            Action a = new Action(r.u8(), r.u8());
            r.end();
            return a;
        }

        public byte[] toGms265Body() {
            return new byte[]{(byte) action, (byte) randomKey};
        }
    }

    /** CP011B ANDROID_EMOTION [verified: sender 5EE1FE0 writes i32, i32 as 265]: emotion, duration. */
    public record Emotion(int emotion, int duration) {
        public static final int OPCODE = 0x011B;

        public static Emotion parse(byte[] packet) {
            Tms282FieldRequestReader r = Tms282FieldRequestReader.open(packet, OPCODE, "CP011B");
            Emotion e = new Emotion(r.i32(), r.i32());
            r.end();
            return e;
        }

        public byte[] toGms265Body() {
            return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(emotion).putInt(duration).array();
        }
    }

    /**
     * CP025E USER_ANDROID_SHOP [verified: sender 1D0C9F0, sub_792470 writes four i32 as 265]: characterId, shop
     * template, position x, y (the android's own shop).
     */
    public record Shop(int characterId, int template, int x, int y) {
        public static final int OPCODE = 0x025E;

        public static Shop parse(byte[] packet) {
            Tms282FieldRequestReader r = Tms282FieldRequestReader.open(packet, OPCODE, "CP025E");
            Shop s = new Shop(r.i32(), r.i32(), r.i32(), r.i32());
            r.end();
            return s;
        }

        public byte[] toGms265Body() {
            return ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN).putInt(characterId).putInt(template)
                    .putInt(x).putInt(y).array();
        }
    }
}
