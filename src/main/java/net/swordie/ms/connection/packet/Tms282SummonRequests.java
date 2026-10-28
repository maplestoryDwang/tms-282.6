package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.codec.MovePathCodec;
import net.swordie.ms.life.movement.MovementInfo;

import java.util.Objects;

/**
 * The small TMS 282.3 summon requests (senders from ingame-cp-map-2823.tsv, constructor sites checked statically):
 * <ul>
 *     <li>CP02E7 SUMMONED_MOVE (5C7CB8E): {@code i32 summonObjectId} + the CMovePath of 3FDB6B0 (282 user-move element
 *     table, key pad included); LP0670 SUMMONED_MOVE to the field: {@code i32 owner, i32 summon} + the broadcast path
 *     (5481E50 -> 4522000 -> 3FDD1D0, keyPad 0, the same writer as LP046D REMOTE_MOVE).</li>
 *     <li>CP02E9 SUMMONED_HIT (450FF5E): {@code i32 summon, u8 attackIdx, i32 damage} and, for a mob hit,
 *     {@code i32 mobTemplate, u8 left} (+ an optional i32); 265 reads the long form.</li>
 *     <li>CP02EA SUMMONED_SKILL (44FC668): {@code i32 summon, i32 skillId, u8 0, u8 action, i32 attackAfter,
 *     Buf8 (282 new), u8 (282 new)}; 265 reads the first five.</li>
 *     <li>CP02EC SUMMONED_REMOVE (451BB44): {@code i32 summon, i32 skillId} as 265.</li>
 * </ul>
 */
public final class Tms282SummonRequests {
    public static final int MOVE = 0x02E7;
    public static final int HIT = 0x02E9;
    public static final int SKILL = 0x02EA;
    public static final int REMOVE = 0x02EC;
    /** LP0670 SUMMONED_MOVE (265 1600 + 48, summoned pool jump table 45AC1B4). */
    public static final int LP_SUMMONED_MOVE = 0x0670;

    private Tms282SummonRequests() {
    }

    public record Move(int summonObjectId, MovementInfo path) {
        public static Move parse(byte[] packet, MovePathCodec paths) {
            Tms282FieldRequestReader r = Tms282FieldRequestReader.open(packet, MOVE, "CP02E7");
            int summon = r.i32();
            MovementInfo path = r.path(paths);
            return new Move(summon, path);
        }
    }

    public record Hit(int summonObjectId, int attackIdx, int damage, int mobTemplateId, boolean left) {
        public static Hit parse(byte[] packet) {
            Tms282FieldRequestReader r = Tms282FieldRequestReader.open(packet, HIT, "CP02E9");
            int summon = r.i32();
            int idx = r.u8();
            int damage = r.i32();
            int template = r.remaining() >= 5 ? r.i32() : 0;
            boolean left = template != 0 && r.u8() != 0;
            return new Hit(summon, idx, damage, template, left);
        }

        /** Body of 265 SummonedHandler.handleSummonedHit. */
        public byte[] toGms265Body() {
            OutPacket out = new OutPacket();
            out.encodeInt(summonObjectId);
            out.encodeByte(attackIdx);
            out.encodeInt(damage);
            out.encodeInt(mobTemplateId);
            out.encodeByte(left);
            return out.getData();
        }
    }

    public record Skill(int summonObjectId, int skillId, int flag, int action, int attackAfter) {
        public static Skill parse(byte[] packet) {
            Tms282FieldRequestReader r = Tms282FieldRequestReader.open(packet, SKILL, "CP02EA");
            return new Skill(r.i32(), r.i32(), r.u8(), r.u8(), r.i32());
        }

        /** Body of 265 SummonedHandler.handleSummonedSkill. */
        public byte[] toGms265Body() {
            OutPacket out = new OutPacket();
            out.encodeInt(summonObjectId);
            out.encodeInt(skillId);
            out.encodeByte(flag);
            out.encodeByte(action);
            out.encodeInt(attackAfter);
            return out.getData();
        }
    }

    public record Remove(int summonObjectId, int skillId) {
        public static Remove parse(byte[] packet) {
            Tms282FieldRequestReader r = Tms282FieldRequestReader.open(packet, REMOVE, "CP02EC");
            return new Remove(r.i32(), r.i32());
        }

        public byte[] toGms265Body() {
            OutPacket out = new OutPacket();
            out.encodeInt(summonObjectId);
            out.encodeInt(skillId);
            return out.getData();
        }
    }

    /** LP0670 SUMMONED_MOVE: owner, summon, broadcast path. */
    public static OutPacket summonedMove(int ownerId, int summonObjectId, MovementInfo path, MovePathCodec paths) {
        Objects.requireNonNull(path, "path");
        OutPacket out = Tms282LoginPacketSupport.packet(LP_SUMMONED_MOVE);
        out.encodeInt(ownerId);
        out.encodeInt(summonObjectId);
        paths.encodeBroadcast(out, path);
        return out;
    }
}
