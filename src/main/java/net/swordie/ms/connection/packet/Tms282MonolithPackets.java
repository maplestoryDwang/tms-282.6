package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;

import java.io.ByteArrayOutputStream;

/**
 * ASkillMonolithManager (輪迴碑石 80011261) as a field-world network actor, the envelope of {@link Tms282MyHomeActors}.
 * The client's MonolithBlockField skill condition (63CC220) refuses the skill with SP 0x4A0B "此技能無法在該地區使用."
 * unless this actor exists and its replicated bool bSummonableField (offset 0x2F8) is set; nothing in the client sets
 * it locally. Class registration 384F50: name ASkillMonolithManager, size 0x318, signature 0xB7F7589B, two
 * properties in this order: bSummonableField (0x2F8), MonolithContainer (0x300, vector of ASkillMonolithTotem).
 * [presumed] own properties start at leaf 20 as for the other AActor subclasses; not yet verified on a live client.
 */
public final class Tms282MonolithPackets {
    public static final int OPCODE = 0x0392, CLASS_SIGNATURE = 0xB7F7589B;
    private static final int LEAF_SUMMONABLE_FIELD = 20;

    private Tms282MonolithPackets() { }

    private static void le(ByteArrayOutputStream out, long value, int bytes) {
        for (int i = 0; i < bytes; i++) out.write((int) (value >>> (8 * i)) & 255);
    }

    private static OutPacket finish(ByteArrayOutputStream body) {
        le(body, 0, 4);
        OutPacket out = Tms282LoginPacketSupport.packet(OPCODE);
        out.encodeByte(0);
        out.encodeArr(body.toByteArray());
        return out;
    }

    /** Creates the manager (or updates it: same actor ID and class) with its owner, role and bSummonableField. */
    public static OutPacket manager(int actorId, int userId, int characterId, boolean summonable) {
        if (actorId <= 0 || userId <= 0 || characterId <= 0) throw new IllegalArgumentException("Invalid monolith manager actor");
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        le(body, actorId, 4); le(body, CLASS_SIGNATURE, 4); body.write(3); body.write(0); body.write(0);
        le(body, 16, 2); le(body, ((long) characterId << 32) | Integer.toUnsignedLong(userId), 8); // NetOwnerID
        le(body, 17, 2); le(body, 3, 1);                                                           // RemoteRole
        le(body, LEAF_SUMMONABLE_FIELD, 2); le(body, summonable ? 1 : 0, 1);
        le(body, 0, 2); le(body, 0, 2); body.write(0);
        return finish(body);
    }

    public static OutPacket destroy(int actorId) {
        if (actorId <= 0) throw new IllegalArgumentException("Invalid monolith manager actor");
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        le(body, actorId, 4); le(body, CLASS_SIGNATURE, 4); body.write(4); body.write(0);
        return finish(body);
    }
}
