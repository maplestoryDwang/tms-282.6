package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;

/**
 * ALynnController (client class name rva 8A34EE8, meta registration 37DD90: size 0x3B0, signature 0x243FF351, parent
 * AActor, property builder 77F8DC0, method table 782A430) in the 282 user actor transport LP0425 - the envelope
 * {@link Tms282FamiliarPackets} uses for AFamiliarController. 265 sent the same object as LYNN_RESULT (ExtraTMSSystem
 * kind 7, class 608170833 = 0x243FF351) with AActor leaves 15..18; 282's AActor has 19 leaves (NetOwnerID 16,
 * RemoteRole 17), so the controller's own leaves start at 20:
 * <pre>
 * 20 FocusTargetID i32            21 bEnableFocus u8              22 GuardianFlags u16
 * 23 ActivatingPeckingSkillID i32 24 ActivatingPeckingTargetMobID 25 ActivatingPeckingSkillLevel
 * 26 PeckingBladeAssaultEndTime   27 PeckingBladeAssaultLevel     28 ActivatingAmbushSkillID
 * 29..31 BlockCategorySkillTime   32..34 BlockCategorySkillID     35 FocusBuffList ...
 * </pre>
 * (research/tms2823/WORK-STATE.md 2026-10-07, scratch lynn_schema.py). Without the controller the client refuses the
 * [集中] skills locally (live: no CP04B4 172001001 at all). Leaf numbers above 17 are 推定 from the schema order and
 * the 265 numbering + 1; live untested.
 */
public final class Tms282LynnPackets {
    public static final int OPCODE = Tms282FamiliarPackets.OPCODE;
    public static final int CLASS_SIGNATURE = 0x243FF351;
    /** 265 ExtraTMSSystem sent GuardianFlags 113. */
    public static final int GUARDIAN_FLAGS = 113;

    private Tms282LynnPackets() {
    }

    /** Creates (or refreshes) the controller: owner, role, focus enabled, guardian flags. */
    public static OutPacket bootstrap(int characterId, long actorId, int userId) {
        if (characterId <= 0 || userId <= 0 || actorId <= 0 || actorId > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("Invalid Lynn controller owner / actor");
        }
        OutPacket out = Tms282LoginPacketSupport.packet(OPCODE);
        out.encodeInt(characterId);
        out.encodeByte(0);
        out.encodeInt((int) actorId);
        out.encodeInt(CLASS_SIGNATURE);
        out.encodeByte(3);
        out.encodeByte(0);
        out.encodeByte(0);
        out.encodeShort(16); // NetOwnerID: user, character
        out.encodeInt(userId);
        out.encodeInt(characterId);
        out.encodeShort(17); // AActor.RemoteRole
        out.encodeByte(3);
        out.encodeShort(21); // bEnableFocus
        out.encodeByte(1);
        out.encodeShort(22); // GuardianFlags
        out.encodeShort(GUARDIAN_FLAGS);
        out.encodeShort(0);
        out.encodeShort(0);
        out.encodeByte(0);
        out.encodeInt(0);
        return out;
    }
}
