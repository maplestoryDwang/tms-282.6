package net.swordie.ms.connection.packet;

import java.util.HashSet;

/** CP03E8 native 446A3E0 -> 1207F20. Only the two captured Khali completion shapes are supported. */
public record Tms282SkillProcessFinishedRequest(int processId, int skillId, int level) {
    public static final int OPCODE = 0x03E8;

    public static Tms282SkillProcessFinishedRequest parse(byte[] packet) {
        var r = Tms282FieldRequestReader.open(packet, OPCODE, "CP03E8");
        r.i32(); // CRC, not server authority
        if (r.u8() != 1) throw new IllegalArgumentException("Missing completed process");
        int id = r.i32(), skill = 0, level = 0, flag = -1;
        var tags = new HashSet<Integer>();
        while (true) {
            int tag = r.i32();
            if (tag == -1) break;
            if (!tags.add(tag) || r.u8() != 1) throw new IllegalArgumentException("Duplicate or absent completion tag");
            switch (tag) {
                case 1 -> {
                    skill = r.i32();
                    if (r.i32() != skill) throw new IllegalArgumentException("Foreign completion skill");
                    level = r.i32(); flag = r.u8();
                    if (r.u8() != 0) throw new IllegalArgumentException("Unknown completion target list");
                }
                case 39 -> {
                    if (r.i32() != 4 || r.i32() != 0 || r.i32() != 0)
                        throw new IllegalArgumentException("Unknown Void Rush completion payload");
                }
                case 19 -> {
                    for (int i = 0; i < 5; i++) if (r.i32() != 0)
                        throw new IllegalArgumentException("Unknown Sandstorm completion payload");
                }
                default -> throw new IllegalArgumentException("Unsupported completion tag " + tag);
            }
        }
        if (r.u8() != 0) throw new IllegalArgumentException("Unknown completion main-stream vector");
        r.i32(); // 346A070 transport trailer
        r.end();
        if (id <= 0 || level <= 0 || level > 30
                || (skill == 154141008 ? flag != 0 || !tags.equals(java.util.Set.of(1,39))
                    : skill != 154141500 || flag != 1 || !tags.equals(java.util.Set.of(1,19))))
            throw new IllegalArgumentException("Unsupported Khali process completion");
        return new Tms282SkillProcessFinishedRequest(id,skill,level);
    }
}
