package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

/**
 * CP0211 link skill request (research/tms2823/link-skill-1006). The 282 link skill window writes the 265
 * LINK_SKILL_REQUEST layout: i32 type, u8 preset; type 0 (save preset): i32 n + n x i32 skill (8E2FF0); type 1 (switch
 * preset): nothing; type 2 (link) / 3 (unlink): i32 skill. Send sites 2409760, 2409A6F, 240D977, 240DCE5, 240DE71,
 * 240E734. Type 4 is 282-only (2409A6F with a variable type) and has no 265 handling: refused here.
 */
public record Tms282LinkSkillRequest(int type, int preset, List<Integer> skills) {
    public static final int OPCODE = 0x0211;
    /** A preset holds at most 12 links in the window (240DC76 checks the count against 0xC). */
    static final int MAX_SKILLS = 12;

    public static Tms282LinkSkillRequest parse(byte[] packet) {
        var in = Tms282FieldRequestReader.open(packet, OPCODE, "link skill request");
        int type = in.i32();
        int preset = in.u8();
        List<Integer> skills;
        switch (type) {
            case 0 -> {
                int n = in.i32();
                if (n < 0 || n > MAX_SKILLS) throw new IllegalArgumentException("Bad link preset size " + n);
                Integer[] ids = new Integer[n];
                for (int i = 0; i < n; i++) ids[i] = in.i32();
                skills = List.of(ids);
            }
            case 1 -> skills = List.of();
            case 2, 3 -> skills = List.of(in.i32());
            default -> throw new IllegalArgumentException("Unsupported link skill request type " + type);
        }
        in.end();
        return new Tms282LinkSkillRequest(type, preset, skills);
    }

    /** The same fields as the 265 body (no opcode) for SkillHandler.handleLinkSkillRequest. */
    public byte[] toGms265Body() {
        ByteBuffer o = ByteBuffer.allocate(9 + skills.size() * 4).order(ByteOrder.LITTLE_ENDIAN);
        o.putInt(type).put((byte) preset);
        if (type == 0) o.putInt(skills.size());
        for (int id : skills) o.putInt(id);
        return Arrays.copyOf(o.array(), o.position());
    }
}
