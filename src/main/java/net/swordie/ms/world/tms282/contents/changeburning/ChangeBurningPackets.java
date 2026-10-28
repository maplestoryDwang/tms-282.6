package net.swordie.ms.world.tms282.contents.changeburning;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.ProtocolVersion;

import java.util.List;

/**
 * TMS 282.3 packets of the contents framework, content type 5 = 變身燃燒 (research/tms2823/change-burning-lucid,
 * static analysis 09-29):
 * <ul>
 *     <li>LP02C9 CWvsContext 18255D0: {@code u16 5, i32 1} creates manager 5 (19BAE80); 1825880 looks the type up
 *     first, so a repeated create keeps the existing manager;</li>
 *     <li>LP064D CUserLocal 1825750: {@code u16 5, i32 1} creates the user component (1825CB0); ignored without a
 *     CUserLocal (5955A80), so it must follow SetField;</li>
 *     <li>LP02C7 5E8CB90: {@code u16 5} + manager body 19BB070: {@code u8 2, u8 mode} (0 end: closes 0x6E9 / 0x795 /
 *     0x78D / 0x6E4; 1 start: opens 0x795 when QR 102930 ui=max, else 0x6E9; 2 start without UI) and
 *     {@code u8 3, i64 exp} (exp particle 19BB570);</li>
 *     <li>LP02CC 1013AA0: the RP tables (stat tables, then level -> demand exp tables, key 4 = RPDemandExpT4);</li>
 *     <li>CP04E1 (senders 19BB150 / 19BF060 / 19BF110): {@code u16 type, u8 action}.</li>
 * </ul>
 */
public final class ChangeBurningPackets {
    public static final int CONTENTS_CREATE = 0x02C9;
    public static final int CONTENTS_PACKET = 0x02C7;
    public static final int USER_CONTENTS_CREATE = 0x064D;
    public static final int CONTENTS_REMOVE = 0x02CA;
    public static final int USER_CONTENTS_REMOVE = 0x064E;
    /** LP064C CUserLocal 5955AA0: {@code u16 type} + user component body (content 5: 19BD2C0). */
    public static final int USER_CONTENTS_PACKET = 0x064C;
    public static final int RP_TABLES = 0x02CC;
    public static final int CONTENTS_REQUEST = 0x04E1;
    public static final int TYPE = 5;
    /** RPDemandExp table key the UI reads ({@code #RPDemandExpT4R600384Exlv#}). */
    public static final int EXP_TABLE_KEY = 4;

    private ChangeBurningPackets() {
    }

    public static OutPacket contentsCreate() {
        OutPacket out = OutPacket.forProfile(ProtocolVersion.TMS_282_3, CONTENTS_CREATE);
        out.encodeShort(TYPE);
        out.encodeInt(1);
        return out;
    }

    public static OutPacket userContentsCreate() {
        OutPacket out = OutPacket.forProfile(ProtocolVersion.TMS_282_3, USER_CONTENTS_CREATE);
        out.encodeShort(TYPE);
        out.encodeInt(1);
        return out;
    }

    /** LP02CA 18256C0: {@code u16 5} removes manager 5 (1826730; FNV lookup miss -> no-op, static lucidprog 09-30). */
    public static OutPacket contentsRemove() {
        OutPacket out = OutPacket.forProfile(ProtocolVersion.TMS_282_3, CONTENTS_REMOVE);
        out.encodeShort(TYPE);
        return out;
    }

    /** LP064E 1825820: {@code u16 5} removes the user component (1826950; ignored without a CUserLocal / when absent). */
    public static OutPacket userContentsRemove() {
        OutPacket out = OutPacket.forProfile(ProtocolVersion.TMS_282_3, USER_CONTENTS_REMOVE);
        out.encodeShort(TYPE);
        return out;
    }

    /**
     * LP064C {@code u16 5, u8 1, u8 act}: the answer to CP04E1 action 1 (research change-burning-lucid/SKILLS-FIX.md).
     * The user component (19BD2C0, jump table 19BDB34) runs act 0 -> 19BE1D0(80004200): transformation animation
     * (0x47/0xBFE/0x48 while RPEventStat is off, 0xBEF/0xBFF/0xBF0 while it is on, so send this BEFORE the CTS
     * change), CHANGE cooldown and request state +0x68 = 2. Only state 2 is ever cleared again (tick 19BDB80 or the
     * animation-end callback 19BDCE0, both then send CP04E1 action 2); without this packet the state stays 1 and the
     * client never sends CP04E1 for 80004200 again (19BDE66), so the transformation could not be ended by the skill.
     * act 1..6 are message variants (not used).
     */
    public static OutPacket requestResult(int act) {
        OutPacket out = OutPacket.forProfile(ProtocolVersion.TMS_282_3, USER_CONTENTS_PACKET);
        out.encodeShort(TYPE);
        out.encodeByte(1);
        out.encodeByte(act);
        return out;
    }

    /** LP02C7 sub 2: 0 = transformation ended, 1 = started (opens the status window), 2 = started, no window. */
    public static OutPacket transformUi(int mode) {
        OutPacket out = OutPacket.forProfile(ProtocolVersion.TMS_282_3, CONTENTS_PACKET);
        out.encodeShort(TYPE);
        out.encodeByte(2);
        out.encodeByte(mode);
        return out;
    }

    /** LP02C7 sub 3: the exp gain effect (the numbers themselves come from QR 600384). */
    public static OutPacket expGain(long exp) {
        OutPacket out = OutPacket.forProfile(ProtocolVersion.TMS_282_3, CONTENTS_PACKET);
        out.encodeShort(TYPE);
        out.encodeByte(3);
        out.encodeLong(exp);
        return out;
    }

    /**
     * LP02CC with no stat table and one exp table ({@code key 4}): {@code i32 nStat = 0; i32 nExp = 1; i32 key;
     * i32 n1 = 0; i32 n2, n2 x (i32 level, i32 demandExp); i32 tail = 0}.
     */
    public static OutPacket expTable(List<int[]> levelDemand) {
        OutPacket out = OutPacket.forProfile(ProtocolVersion.TMS_282_3, RP_TABLES);
        out.encodeInt(0);
        out.encodeInt(1);
        out.encodeInt(EXP_TABLE_KEY);
        out.encodeInt(0);
        out.encodeInt(levelDemand.size());
        for (int[] row : levelDemand) {
            out.encodeInt(row[0]);
            out.encodeInt(row[1]);
        }
        out.encodeInt(0);
        return out;
    }

    /** CP04E1 body. */
    public record Request(int type, int action) {
        public static Request parse(byte[] packet) {
            if (packet.length < 5) {
                throw new IllegalArgumentException("CP04E1 needs u16 type, u8 action; got " + packet.length + " bytes");
            }
            int op = (packet[0] & 0xFF) | (packet[1] & 0xFF) << 8;
            if (op != CONTENTS_REQUEST) {
                throw new IllegalArgumentException(String.format("not CP04E1: %04X", op));
            }
            int type = (short) ((packet[2] & 0xFF) | (packet[3] & 0xFF) << 8);
            return new Request(type, packet[4] & 0xFF);
        }
    }
}
