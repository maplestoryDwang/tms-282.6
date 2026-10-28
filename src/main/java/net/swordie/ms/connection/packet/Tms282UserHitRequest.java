package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.enums.AttackIndex;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;

/**
 * CP0115 USER_HIT (265 InHeader 0x113) [verified: 20 static senders, ingame-cp-map-2823.md 4.3]:
 * <pre>
 * u32 ?            (265: unknown)
 * u32 damagedTime
 * u8  attackIdx    (-1 = body touch)
 * u8  type         (historical Java name; native main-mob sender uses the elemental attribute here)
 * u8  elemAttr     (historical Java name; native main-mob sender writes a boolean here)
 * u32 hpDamage
 * u8  0, u8, u32 sourceMobId (57A8597 / 3E6DED0 reads CMob+558; older code called this zeroInt)
 * 47 bytes         sub_118F790: 10 x u32, 3 x u8, u32 -- 282 only, meaning unknown (the map calls it a 48-byte
 *                  block; the linear extraction gives 4*10+3+4 = 47)
 * ...              per sender (mob path 57A7972: presumed powerGuard/reflect/hitPos/stance...), kept raw
 * </pre>
 * The first nine fields retain the existing265 {@code Job.handleUserHit} adapter names. {@link #toGms265Body} rebuilds a265 request
 * body from them so the 265 hit logic (buff reductions, death, revive skills) runs unchanged; the 265 mob branch gets
 * zeros for the old job-specific mob branch. The source-bound282 indie52 policy separately validates the simple
 * mob tail before normalizing damage; it does not trust the discarded reflect/reduction claims.
 */
public record Tms282UserHitRequest(int unknown, int damagedTime, byte attackIdx, byte type, byte elemAttr, int hpDamage,
                                   byte critical, byte zero, int zeroInt, byte[] rest) {
    public static final int OPCODE = 0x0115;
    /** Width of the 282-only block after the nine 265 fields (ingame-cp-map-2823.tsv 0x0115, sub_118F790). */
    public static final int BLOCK_282 = 47;

    public Tms282UserHitRequest {
        rest = Objects.requireNonNull(rest, "rest").clone();
    }

    @Override
    public byte[] rest() {
        return rest.clone();
    }

    /** False when the packet ended before the 282 block (a sender this parser has not seen). */
    public boolean hasBlock282() {
        return rest.length >= BLOCK_282;
    }

    /** 265 obstacle-style hit (no mob branch): {@code type <= AttackIndex.Counter}. */
    public boolean isObstacleType() {
        return type <= AttackIndex.Counter.getVal();
    }

    /** Accepts the full plaintext packet, including the opcode. */
    public static Tms282UserHitRequest parse(byte[] packet) {
        Objects.requireNonNull(packet, "packet");
        ByteBuffer in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        try {
            if (Short.toUnsignedInt(in.getShort()) != OPCODE) {
                throw new IllegalArgumentException("Wrong CP0115 opcode");
            }
            int unknown = in.getInt();
            int damagedTime = in.getInt();
            byte attackIdx = in.get();
            byte type = in.get();
            byte elemAttr = in.get();
            int hpDamage = in.getInt();
            byte critical = in.get();
            byte zero = in.get();
            int zeroInt = in.getInt();
            byte[] rest = Arrays.copyOfRange(packet, in.position(), packet.length);
            return new Tms282UserHitRequest(unknown, damagedTime, attackIdx, type, elemAttr, hpDamage, critical, zero,
                    zeroInt, rest);
        } catch (BufferUnderflowException e) {
            throw new IllegalArgumentException("Truncated CP0115", e);
        }
    }

    /** The 265 USER_HIT body (no opcode) that {@code Job.handleUserHit} reads for this hit. */
    public byte[] toGms265Body() {
        return toGms265Body(hpDamage);
    }

    /** The source-bound282 damage policy may normalize only the proven ordinary-mob sender before265 job rules. */
    public byte[] toGms265Body(int damage) {
        OutPacket out = new OutPacket();
        out.encodeInt(unknown);
        out.encodeInt(damagedTime);
        out.encodeByte(attackIdx);
        out.encodeByte(type);
        out.encodeByte(elemAttr);
        out.encodeInt(damage);
        out.encodeByte(critical);
        out.encodeByte(zero);
        out.encodeInt(zeroInt);
        if (isObstacleType()) {
            out.encodeShort(0); // obstacle
        } else {
            out.encodeInt(0); // mobID
            out.encodeInt(0); // templateID
            out.encodeInt(0); // mobIdForMissCheck
            out.encodeByte(0); // isLeft
            out.encodeInt(0); // blockSkillId
            out.encodeInt(0); // reducedDamage (0: no guard block)
            out.encodeByte(0); // reflect
            out.encodeByte(0); // guard
            out.encodeByte(0); // stance
            out.encodeInt(0); // stanceSkillID
            out.encodeInt(0); // cancelSkillID
            out.encodeInt(0); // reductionSkillID
        }
        out.encodeByte(0);
        return out.getData();
    }

    @Override
    public String toString() {
        return "CP0115[type=" + type + " attackIdx=" + attackIdx + " damage=" + hpDamage + " elem=" + elemAttr
                + " rest=" + rest.length + "B" + (hasBlock282() ? "" : " (no 282 block)") + "]";
    }
}
