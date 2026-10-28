package net.swordie.ms.connection.packet;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;

/**
 * CP04B4 USER_SKILL_USE_REQUEST (265 InHeader 0x484) [verified, 48 static senders; ingame-cp-map-2823.md 3.3].
 * Only the prefix is common to every sender:
 * <pre>
 * u32 tick
 * u32 0xFFFFFFFF      282 only: 265 reads the skill ID here and would get -1
 * u32 skillId
 * [u8 0]              only when 117D3D0(skillId): the Zero skills (265 reads it for a Zero character)
 * u32 slv
 * u32, u32            10D4250 / 10E4F30 results (0 when there is no data)
 * ... rest            ProcessType, specialMoveAdditionalInfo and per-skill fields, then the u32 that 346A070
 *                     appends to this CP; kept raw
 * </pre>
 * The Zero byte rule is 117D3D0: job = skillId / 10000 (skillId / 100 when that is 8000 or 8001), true for job 10000
 * and 10100..10199. The parser is strict about the prefix; the rest is not interpreted.
 */
public record Tms282SkillUseRequest(int tick, int marker, int skillId, boolean zeroByte, int slv, int info1, int info2,
                                    byte[] rest) {
    public static final int OPCODE = 0x04B4;
    /** Value of the second field in every sender. */
    public static final int MARKER = -1;

    public Tms282SkillUseRequest {
        rest = Objects.requireNonNull(rest, "rest").clone();
    }

    @Override
    public byte[] rest() {
        return rest.clone();
    }

    /** Skills such as installed Omega Blaster end immediately after the native common option. */
    public int nativeOptionWithoutBody() {
        if(rest.length<4)throw new IllegalArgumentException("missing transport trailer");
        ByteBuffer in=ByteBuffer.wrap(rest).order(ByteOrder.LITTLE_ENDIAN);in.limit(rest.length-4);
        int option=Tms282SkillProcess.decodeUsePrefix(in,
                new net.swordie.ms.client.character.skills.info.SkillUseInfo(skillId,slv));
        if(in.hasRemaining())throw new IllegalArgumentException("unexpected native skill body");
        return option;
    }

    /** 117D3D0: does the client write the extra byte before slv for this skill? */
    public static boolean hasZeroByte(int skillId) {
        int job = skillId / 10000;
        if (job == 8000 || job == 8001) {
            job = skillId / 100;
        }
        return job == 10000 || (job >= 10100 && job < 10200);
    }

    /** Accepts the full plaintext packet, including the opcode. */
    public static Tms282SkillUseRequest parse(byte[] packet) {
        Objects.requireNonNull(packet, "packet");
        ByteBuffer in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        try {
            if (Short.toUnsignedInt(in.getShort()) != OPCODE) {
                throw new IllegalArgumentException("Wrong CP04B4 opcode");
            }
            int tick = in.getInt();
            int marker = in.getInt();
            if (marker != MARKER) {
                throw new IllegalArgumentException("CP04B4 second field is " + marker + ", expected -1 (282 layout)");
            }
            int skillId = in.getInt();
            boolean zeroByte = hasZeroByte(skillId);
            if (zeroByte) {
                in.get();
            }
            int slv = in.getInt();
            int info1 = in.getInt();
            int info2 = in.getInt();
            byte[] rest = Arrays.copyOfRange(packet, in.position(), packet.length);
            return new Tms282SkillUseRequest(tick, marker, skillId, zeroByte, slv, info1, info2, rest);
        } catch (BufferUnderflowException e) {
            throw new IllegalArgumentException("Truncated CP04B4", e);
        }
    }

    @Override
    public String toString() {
        return "CP04B4[skill=" + skillId + " slv=" + slv + (zeroByte ? " zero" : "") + " info=" + info1 + "/" + info2
                + " rest=" + rest.length + "B]";
    }
}
