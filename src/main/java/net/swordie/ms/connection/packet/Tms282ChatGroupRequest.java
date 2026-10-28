package net.swordie.ms.connection.packet;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The TMS 282.3 whisper and group chat requests; both keep the 265 layout (strings re-encoded from the 282 charset to
 * the 265 UTF-8), so {@link #toGms265Body} is the 265 body of ChatHandler.handleWhisper / handleGroupMessage.
 * <ul>
 *     <li>CP01ED WHISPER [opcode 證實, ingame-cp-map-2823]: {@code u8 type, i32 tick, Str target[, Str msg]};
 *     senders 37302B0 (type 6 whisper: + Str msg), 3730BA0 (type 5 /find), 3756A00 (type 34, from the LP0305
 *     handler) -- 265 reads the same (5 / 68 find, 6 whisper; other types end in its switch);</li>
 *     <li>CP01EA GROUP_MESSAGE [opcode 證實]: {@code u8 type (0 buddy, 1 party, 2 guild, 3 alliance), u16 n,
 *     n x i32 recipients, Str msg} (senders 343FB00 / 34407F0) as 265.</li>
 * </ul>
 */
public sealed interface Tms282ChatGroupRequest {
    int WHISPER_OPCODE = 0x01ED;
    int GROUP_MESSAGE_OPCODE = 0x01EA;

    byte[] toGms265Body();

    record Whisper(int type, int tick, String target, String message) implements Tms282ChatGroupRequest {
        public byte[] toGms265Body() {
            Gms265BodyWriter w = new Gms265BodyWriter().u8(type).i32(tick).str(target);
            return message == null ? w.bytes() : w.str(message).bytes();
        }
    }

    record Group(int type, List<Integer> recipients, String message) implements Tms282ChatGroupRequest {
        public Group {
            recipients = List.copyOf(recipients);
        }

        public byte[] toGms265Body() {
            Gms265BodyWriter w = new Gms265BodyWriter().u8(type).u16(recipients.size());
            recipients.forEach(w::i32);
            return w.str(message).bytes();
        }
    }

    /** CP01ED: a message follows the target when bytes are left (type 6 and the whisper-like types). */
    static Whisper parseWhisper(byte[] packet, Charset charset) {
        Objects.requireNonNull(charset, "charset");
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, WHISPER_OPCODE, "whisper");
        int type = in.u8();
        int tick = in.i32();
        String target = in.str(charset);
        String message = in.remaining() > 0 ? in.str(charset) : null;
        in.end();
        return new Whisper(type, tick, target, message);
    }

    static Group parseGroup(byte[] packet, Charset charset) {
        Objects.requireNonNull(charset, "charset");
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, GROUP_MESSAGE_OPCODE, "group message");
        int type = in.u8();
        int n = in.u16();
        if (n > 1000) {
            throw new IllegalArgumentException("group message with " + n + " recipients");
        }
        List<Integer> ids = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            ids.add(in.i32());
        }
        String message = in.str(charset);
        in.end();
        return new Group(type, ids, message);
    }
}
