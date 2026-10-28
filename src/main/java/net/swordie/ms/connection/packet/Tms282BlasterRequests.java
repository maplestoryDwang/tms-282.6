package net.swordie.ms.connection.packet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Native 5A3A390 / 5A38440: complete frames, including the transport trailer. */
public final class Tms282BlasterRequests {
    private Tms282BlasterRequests() { }

    public record ClearCurrentAttack(int skillId, int flag) {
        public static ClearCurrentAttack parse(byte[] packet) {
            var r = Tms282FieldRequestReader.open(packet, 0x03C3, "CP03C3");
            int skill = r.i32(), flag = r.u8(); r.i32(); r.end();
            if (skill != 37121004 || flag > 1) throw new IllegalArgumentException("Invalid Blaster attack release");
            return new ClearCurrentAttack(skill, flag);
        }
    }

    public record MultiChargeCancel(List<Integer> skillIds) {
        public MultiChargeCancel { skillIds = List.copyOf(skillIds); }

        public static MultiChargeCancel parse(byte[] packet) {
            var r = Tms282FieldRequestReader.open(packet, 0x03C4, "CP03C4");
            // 5A38471 reads the list size at actor+AF58; the single-skill path overrides it with 1.
            int count = r.u8();
            if (count == 0 || r.remaining() != count * 4 + 4)
                throw new IllegalArgumentException("Invalid Blaster cancel list size");
            var skills = new ArrayList<Integer>(count);
            var seen = new HashSet<Integer>();
            for (int i = 0; i < count; i++) {
                int skill = r.i32();
                if (skill <= 0 || !seen.add(skill)) throw new IllegalArgumentException("Invalid Blaster cancel skill");
                skills.add(skill);
            }
            r.i32(); r.end();
            return new MultiChargeCancel(skills);
        }
    }
}
