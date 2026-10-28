package net.swordie.ms.world.tms282.collab.frieren;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.ProtocolVersion;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The two 282 packets of the partner event found in the client (static analysis of MapleStory.exe 282.3, see
 * research/tms2823/collab-frieren/README.md). Neither is live-verified.
 * <ul>
 *     <li>CP03BF skill ON/OFF: the skill window's mouse handlers 4F1C9A0 / 4F49150 / 4F62710 (three window
 *     variants), branch WM_RBUTTONDOWN (0x204): for 80012691 (explicit {@code cmp ebx,4C4E593}) and for every skill
 *     whose additional_process contains 90 (1207E40(skill, 0x5A) via 109EB20, e.g. 80012711), at most once per
 *     1000 ms, {@code COutPacket(0x3BF); Encode4(skillId); Send}. No exclusive-request lock is taken, so no reply
 *     is required;</li>
 *     <li>LP0616 (CUserLocal handler 592BE10): {@code Decode4 x6}; the first int is a skill id and selects the
 *     branch (80003155, 80003176, 80012711, else generic). In the 80012711 branch the third int is a mob object id
 *     (looked up in the mob pool 3FA2230; its position is used), the fifth / sixth ints are an x / y fallback, and
 *     an effect from StringPool 0x10EF is played for 1000 ms; ints two and four are not understood. Sent only when
 *     {@code tms282.frieren.gaugePacket=true}.</li>
 * </ul>
 */
public final class FrierenPackets {
    public static final int SKILL_ON_OFF_REQUEST = 0x03BF;
    public static final int SKILL_EVENT_EFFECT = 0x0616;

    private FrierenPackets() {
    }

    /** CP03BF body: i32 skillId (a trailing u32 socket tail is tolerated). */
    public record SkillOnOffRequest(int skillId) {
        public static SkillOnOffRequest parse(byte[] packet) {
            if (packet == null || packet.length < 2) {
                throw new IllegalArgumentException("CP03BF: no opcode");
            }
            int op = (packet[0] & 0xFF) | (packet[1] & 0xFF) << 8;
            if (op != SKILL_ON_OFF_REQUEST) {
                throw new IllegalArgumentException(String.format("CP03BF: opcode %04X", op));
            }
            int body = packet.length - 2;
            if (body != 4 && body != 8) {
                throw new IllegalArgumentException("CP03BF: body of " + body + " bytes");
            }
            return new SkillOnOffRequest(ByteBuffer.wrap(packet, 2, 4).order(ByteOrder.LITTLE_ENDIAN).getInt());
        }
    }

    /**
     * LP0616 for the 80012711 branch: skill id, {@code arg1} (unknown, 0), clone mob object id, {@code arg3}
     * (unknown, 0), x, y. Hypothesis only; keep behind the {@code gaugePacket} flag.
     */
    public static OutPacket skillEventEffect(int skillId, int arg1, int mobObjectId, int arg3, int x, int y) {
        OutPacket out = OutPacket.forProfile(ProtocolVersion.TMS_282_3, SKILL_EVENT_EFFECT);
        out.encodeInt(skillId);
        out.encodeInt(arg1);
        out.encodeInt(mobObjectId);
        out.encodeInt(arg3);
        out.encodeInt(x);
        out.encodeInt(y);
        return out;
    }
}
