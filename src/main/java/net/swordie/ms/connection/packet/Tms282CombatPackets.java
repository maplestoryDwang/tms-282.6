package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;

/**
 * TMS 282.3 combat replies whose layout the emulated client confirmed (research/tms2823/combat-trace,
 * tools/Tms282CombatCheck.java). Every read below is the 282 client's; the 265 builder with the same body is named.
 * <ul>
 *     <li>LP00AE MESSAGE (CWvsContext 5ED27F0, type at 5ED281A):
 *     <ul>
 *         <li>type 3 IncEXP (13394D0): {@code B lastHit, L incExp, B onQuest, Buf4 diseaseType, Buf8 bonusMask,
 *         Buf4 count} -- the 265 {@code ExpIncreaseInfo.encode} with no bonus, no disease and no quest bonus, the only
 *         form sent (other mask bits, the disease and quest branches and a non-zero count are not verified);</li>
 *         <li>type 0 DropPickUp (5EEE7E0): {@code I 0, B 0, B kind}; kind 1 meso {@code B lost, I meso, S smallChange,
 *         I 0}; kind 0 item {@code I itemId, I quantity, B 0} -- 265 {@code WvsContext.dropPickupMessage}.</li>
 *     </ul></li>
 *     <li>LP047F REMOTE_HIT (CUserRemote 5AD9370 after the dispatcher's {@code I characterId}): the 265
 *     {@code UserRemote.hit} form. Only type -3 (no mob) is built: {@code B -3, I damage, B crit, B damage==0,
 *     [damage != 0: B 0, I damage]} -- 265 turns every hit without a mob id into -3, and the 282 USER_HIT request's
 *     mob fields are not identified yet. A miss (damage -1) adds {@code I 0} (userSkillID); damage 0 ends after the flag byte
 *     (265 wrote one more i32 there, which the 282 client does not read).</li>
 * </ul>
 */
public final class Tms282CombatPackets {
    public static final int MESSAGE = 0x00AE;
    public static final int REMOTE_HIT = 0x047F;
    public static final int MESSAGE_DROP_PICK_UP = 0;
    public static final int MESSAGE_INC_EXP = 3;
    /** 265 AttackIndex.Obstacle: a hit shown without mob information. */
    public static final int HIT_TYPE_NO_MOB = -3;

    private Tms282CombatPackets() {
    }

    /** LP00AE type 3 with no bonus lines; an extended frame with an 8-byte AUX region (see the end of the method). */
    public static OutPacket incExpMessage(long incExp, boolean lastHit) {
        OutPacket out = Tms282LoginPacketSupport.packet(MESSAGE);
        out.encodeByte(MESSAGE_INC_EXP);
        out.encodeByte(lastHit ? 1 : 0);
        out.encodeLong(incExp);
        out.encodeByte(0); // onQuest
        out.encodeInt(0); // diseaseType (non-zero would add an L expLost)
        out.encodeLong(0L); // bonus mask
        out.encodeInt(0); // count
        // The decoder ends with an 8-byte read from the AUX stream (1339A85 -> 0939890, r8b=1; stored at +0x168, the
        // handler shows string 0x5137 with it when > 0). Without an AUX region that read throws and the client dies
        // (live 2026-09-24: crash after every mob kill; daily-trace incexp-noaux NOT CLEAN / incexp-aux8 CLEAN).
        out.setAuxiliaryData(new byte[8]);
        return out;
    }

    /**
     * LP00AE type 3 with the bonus lines the server computed (research/tms2823/combat-trace/incexp-bonus/INCEXP-BONUS.md,
     * decoder 13394D0, display 5EF5390; emulator CLEAN for the served bits and for every bit). 282 read order: the
     * header, u64 mask; 0x1 i64, 0x4 u8; quest rate / remaining; i64 0x20, 0x10, 0x40 .. 0x4000, 0x10000 .. 0x200000;
     * 0x400000 i64 + i32 rate; i64 0x800000 .. 0x80000000; 0x8000 i64 + i32; i32 sub-mask; 8-byte AUX. Bits above
     * 0x80000000 (282 reads other widths there, the server fills none) are left out.
     */
    public static OutPacket incExpMessage(net.swordie.ms.client.character.info.ExpIncreaseInfo info, long incExp) {
        long mask = info.getMask() & 0xFFFFFFFFL;
        OutPacket out = Tms282LoginPacketSupport.packet(MESSAGE);
        out.encodeByte(MESSAGE_INC_EXP);
        out.encodeByte(info.isLastHit() ? 1 : 0);
        out.encodeLong(incExp);
        out.encodeByte(info.isOnQuest() ? 1 : 0);
        out.encodeInt(info.getDiseaseType());
        if (info.getDiseaseType() != 0) {
            out.encodeLong(info.getExpLost());
        }
        out.encodeLong(mask);
        if ((mask & 0x1) != 0) out.encodeLong(info.getSelectedMobBonusExp());
        if ((mask & 0x4) != 0) out.encodeByte(info.getPartyBonusPercentage());
        if (info.isOnQuest()) {
            out.encodeByte(info.getQuestBonusRate());
            if (info.getQuestBonusRate() > 0) out.encodeByte(info.getQuestBonusRemainCount());
        }
        if ((mask & 0x20) != 0) out.encodeLong(info.getWeddingBonusExp());
        if ((mask & 0x10) != 0) out.encodeLong(info.getPartyBonusExp());
        if ((mask & 0x40) != 0) out.encodeLong(info.getItemBonusExp());
        if ((mask & 0x80) != 0) out.encodeLong(info.getPremiumIPBonusExp());
        if ((mask & 0x100) != 0) out.encodeLong(info.getRainbowWeekEventBonusExp());
        if ((mask & 0x200) != 0) out.encodeLong(info.getBoomupEventBonusExp());
        if ((mask & 0x400) != 0) out.encodeLong(info.getPlusExpBuffBonusExp());
        if ((mask & 0x800) != 0) out.encodeLong(info.getPsdBonusExpRate());
        if ((mask & 0x1000) != 0) out.encodeLong(info.getIndieBonusExp());
        if ((mask & 0x2000) != 0) out.encodeLong(info.getRelaxBonusExp());
        if ((mask & 0x4000) != 0) out.encodeLong(info.getInstallItemBonusExp());
        if ((mask & 0x10000) != 0) out.encodeLong(info.getExpByIncExpR());
        if ((mask & 0x20000) != 0) out.encodeLong(info.getValuePackBonusExp());
        if ((mask & 0x40000) != 0) out.encodeLong(info.getExpByIncPQExpR());
        if ((mask & 0x80000) != 0) out.encodeLong(info.getBaseAddExp());
        if ((mask & 0x100000) != 0) out.encodeLong(info.getBloodAllianceBonusExp());
        if ((mask & 0x200000) != 0) out.encodeLong(info.getFreezeHotEventBonusExp());
        if ((mask & 0x400000) != 0) {
            out.encodeLong(info.getRestFieldBonusExp());
            out.encodeInt(info.getRestFieldExpRate());
        }
        if ((mask & 0x800000) != 0) out.encodeLong(info.getUserHPRateBonusExp());
        if ((mask & 0x1000000) != 0) out.encodeLong(info.getFieldValueBonusExp());
        if ((mask & 0x2000000) != 0) out.encodeLong(info.getMobKillBonusExp());
        if ((mask & 0x4000000) != 0) out.encodeLong(info.getLiveEventBonusExp());
        if ((mask & 0x8000000) != 0) out.encodeLong(info.getUnknownBonusExp());
        if ((mask & 0x10000000) != 0) out.encodeLong(info.getPartyRingBonusExp());
        if ((mask & 0x20000000) != 0) out.encodeLong(info.getUnknownBonusExp2());
        if ((mask & 0x40000000) != 0) out.encodeLong(info.getSetItemBonusExp());
        if ((mask & 0x80000000L) != 0) out.encodeLong(info.getSetItemBonusPartyExp());
        if ((mask & 0x8000) != 0) {
            out.encodeLong(info.getAswanWinnerBonusExp());
            out.encodeInt(0);
        }
        out.encodeInt(0); // sub-mask
        out.setAuxiliaryData(new byte[8]); // see incExpMessage(long, boolean)
        return out;
    }

    /** LP00AE type 0, meso picked up. */
    public static OutPacket mesoPickUpMessage(int meso, int smallChangeBonus) {
        OutPacket out = dropPickUpHeader(1);
        out.encodeByte(0); // part lost on the ground
        out.encodeInt(meso);
        out.encodeShort((short) smallChangeBonus);
        out.encodeInt(0);
        return out;
    }

    /** LP00AE type 0, item picked up. */
    public static OutPacket itemPickUpMessage(int itemId, int quantity) {
        OutPacket out = dropPickUpHeader(0);
        out.encodeInt(itemId);
        out.encodeInt(quantity);
        out.encodeByte(0);
        return out;
    }

    private static OutPacket dropPickUpHeader(int kind) {
        OutPacket out = Tms282LoginPacketSupport.packet(MESSAGE);
        out.encodeByte(MESSAGE_DROP_PICK_UP);
        out.encodeInt(0); // delay
        out.encodeByte(0);
        out.encodeByte(kind);
        return out;
    }

    /** LP047F for {@code characterId} taking {@code damage} without mob information (type -3). */
    public static OutPacket remoteHit(int characterId, int damage, boolean critical) {
        OutPacket out = Tms282LoginPacketSupport.packet(REMOTE_HIT);
        out.encodeInt(characterId);
        out.encodeByte(HIT_TYPE_NO_MOB);
        out.encodeInt(damage);
        out.encodeByte(critical ? 1 : 0);
        boolean noDamage = damage == 0;
        out.encodeByte(noDamage ? 1 : 0);
        if (noDamage) {
            return out; // the client reads nothing after the flag (traced: remote-hit-0)
        }
        out.encodeByte(0);
        out.encodeInt(damage);
        if (damage == -1) {
            out.encodeInt(0); // 265: userSkillID of a miss
        }
        return out;
    }
}
