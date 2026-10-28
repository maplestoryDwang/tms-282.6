package net.swordie.ms.connection.packet;

import net.swordie.ms.client.jobs.flora.Illium;

/** CP02F4/02F5: EX's action begins/ends. No transport trailer (450F4A0 / 4548590). */
public final class Tms282ExSummonRequests {
    public static final int CANCEL = 0x02ED, BEGIN = 0x02F4, END = 0x02F5;
    public static final int MAX_REMAINING_MS = 60_000;
    private Tms282ExSummonRequests() { }

    public record Begin(int objectId, int action, int sourceSkill, int linkedSkill, int remainingMs,
                        int x, int y, int targetX, int targetY) {
        public static Begin parse(byte[] packet) {
            var r = Tms282FieldRequestReader.open(packet, BEGIN, "CP02F4");
            var result = new Begin(r.i32(), r.i32(), r.i32(), r.i32(), r.i32(), r.i32(), r.i32(), r.i32(), r.i32());
            r.end();
            if (result.objectId <= 0 || result.action != 8 || result.sourceSkill != Illium.EX || result.linkedSkill != 0
                    || result.remainingMs <= 0 || result.remainingMs > MAX_REMAINING_MS)
                throw new IllegalArgumentException("Unsupported EX action begin");
            for (int coordinate : new int[]{result.x, result.y, result.targetX, result.targetY})
                if (coordinate < Short.MIN_VALUE || coordinate > Short.MAX_VALUE)
                    throw new IllegalArgumentException("EX action coordinate outside field range");
            return result;
        }
    }

    public record End(int objectId) {
        public static End parse(byte[] packet) {
            var r = Tms282FieldRequestReader.open(packet, END, "CP02F5");
            var result = new End(r.i32()); r.end();
            if (result.objectId <= 0) throw new IllegalArgumentException("Invalid EX object ID");
            return result;
        }
    }

    /** 454C190 / 454C940 send this before invoking the CP02F5 action-end sender. */
    public record Cancel(int objectId) {
        public static Cancel parse(byte[] packet) {
            var r = Tms282FieldRequestReader.open(packet, CANCEL, "CP02ED");
            var result = new Cancel(r.i32()); r.end();
            if (result.objectId <= 0) throw new IllegalArgumentException("Invalid EX cancel object ID");
            return result;
        }
    }
}
