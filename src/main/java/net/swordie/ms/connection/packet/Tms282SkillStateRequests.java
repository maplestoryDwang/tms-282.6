package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.BufferUnderflowException;

/**
 * TMS 282.3 skill state requests (constructor sites checked statically; all three are in the 346A070 list that
 * appends one u32 after the body, ingame-cp-map-2823.md 1.3, so a trailing u32 is expected and ignored):
 * <ul>
 *     <li>CP04B7 USER_SKILL_CANCEL_REQUEST (59DEEF0): {@code i32 skillId, i32, u8, Buf(152) = 38 x u32 CTS mask}
 *     [+u32]; 265 reads {@code i32 skillId} and one i32 per CTS mask word.</li>
 *     <li>CP04B8 USER_SKILL_PREPARE_STOP (586AB6E): {@code i32 keyDownSkillId, u8 1, i32} [+u32]; 265 reads the
 *     skill id.</li>
 *     <li>CP04B9 USER_SKILL_PREPARE_START (59EDFF0): {@code i32 (282 new), i32 skillId, i32 slv, ProcessType
 *     (1207F20, variable), 118FB50 {u8, i32}, i16 mask, u8 actionType, i32, i32 tick} [+u32]. The ProcessType block
 *     is validated using its282 schema before mask / action / tick are read in order.</li>
 * </ul>
 */
public final class Tms282SkillStateRequests {
    public static final int CANCEL = 0x04B7;
    public static final int PREPARE_STOP = 0x04B8;
    public static final int PREPARE_START = 0x04B9;
    /** CTS mask words 265 reads in USER_SKILL_CANCEL_REQUEST (the 282 buffer holds 38). */
    static final int MASK_WORDS_282 = 38;

    private Tms282SkillStateRequests() {
    }

    public record Cancel(int skillId, int second, int flag) {
        public static Cancel parse(byte[] packet) {
            Tms282FieldRequestReader r = Tms282FieldRequestReader.open(packet, CANCEL, "CP04B7");
            int skill = r.i32();
            int second = r.i32();
            int flag = r.u8();
            r.bytes(MASK_WORDS_282 * 4);
            r.i32(); // 346A070 transport trailer
            r.end();
            return new Cancel(skill, second, flag);
        }

        /** Body of 265 SkillHandler.handleTemporaryStatResetRequest: skill id and {@code maskWords} zero words. */
        public byte[] toGms265Body(int maskWords) {
            OutPacket out = new OutPacket();
            out.encodeInt(skillId);
            for (int i = 0; i < maskWords; i++) {
                out.encodeInt(0);
            }
            return out.getData();
        }
    }

    public record PrepareStop(int skillId) {
        public static PrepareStop parse(byte[] packet) {
            Tms282FieldRequestReader r = Tms282FieldRequestReader.open(packet, PREPARE_STOP, "CP04B8");
            int skill=r.i32();r.u8();r.i32();r.i32();r.end();
            return new PrepareStop(skill);
        }

        public byte[] toGms265Body() {
            OutPacket out = new OutPacket();
            out.encodeInt(skillId);
            return out.getData();
        }
    }

    public record PrepareStart(int skillId, int slv, int mask, int actionType, int tick) {
        public static PrepareStart parse(byte[] packet) {
            Tms282FieldRequestReader r = Tms282FieldRequestReader.open(packet, PREPARE_START, "CP04B9");
            r.i32();
            int skill = r.i32();
            int slv = r.i32();
            try {
                ByteBuffer in=ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
                in.limit(packet.length-4);in.position(r.position()); // Exclude346A070, not any part of native body.
                Tms282SkillProcess.decode(in,new SkillUseInfo(skill,slv));
                in.get();in.getInt(); // 118FB50: u8, i32
                int mask=Short.toUnsignedInt(in.getShort()),action=Byte.toUnsignedInt(in.get());
                in.getInt();int tick=in.getInt();
                if(in.hasRemaining())throw new IllegalArgumentException("CP04B9 trailing native body bytes");
                return new PrepareStart(skill,slv,mask,action,tick);
            } catch(BufferUnderflowException e) {
                throw new IllegalArgumentException("CP04B9 truncated native body",e);
            }
        }

        /**
         * Body of 265 SkillHandler.handleUserSkillPrepareStart: skill, level, an empty ProcessType (crc 0, no list),
         * mask, action, speed 0, tick (the two skill-specific position pairs of 265 are sent as 0).
         */
        public byte[] toGms265Body() {
            OutPacket out = new OutPacket();
            out.encodeInt(skillId);
            out.encodeInt(slv);
            out.encodeInt(0);
            out.encodeByte(0);
            out.encodeShort(mask);
            out.encodeByte(actionType);
            out.encodeByte(0);
            if (skillId == 13111020 || skillId == 112111016) {
                out.encodeShort(0);
                out.encodeShort(0);
            }
            out.encodeInt(tick);
            out.encodeShort(0);
            out.encodeShort(0);
            return out.getData();
        }
    }
}
