package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.client.character.runestones.RuneStone;
import net.swordie.ms.client.character.skills.ProcessType;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.jobs.Jianghu.Lynn;
import net.swordie.ms.client.jobs.adventurer.pirate.Buccaneer;
import net.swordie.ms.client.jobs.anima.Ren;
import net.swordie.ms.client.jobs.cygnus.NightWalker;
import net.swordie.ms.client.jobs.legend.Mercedes;
import net.swordie.ms.client.jobs.legend.Shade;
import net.swordie.ms.client.jobs.nova.AngelicBuster;
import net.swordie.ms.client.jobs.resistance.Blaster;
import net.swordie.ms.client.jobs.resistance.WildHunter;
import net.swordie.ms.client.jobs.resistance.demon.DemonAvenger;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.client.character.skills.info.MobAttackInfo;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * The standard {@link Tms282AttackSection}s of a TMS 282.3 attack request, from the client's send sites
 * (ingame-cp-map-2823.md 3.2 and ingame-cp-map-2823.tsv rows 0x010D..0x0111; melee main site 57CA050/57CCFE0).
 * Tags: [verified] = order and width read from the send site; [presumed] = linear extraction of a sub-function
 * whose branches were not all expanded, or 265 order kept where the send site shows only conditional fields.
 * <p>
 * Live samples decide: when a capture disagrees, replace the one section in {@link Tms282AttackLayout}.
 */
public final class Tms282AttackSections {
    /** Upper bound of the list counts inside the bonus block (the client's lists are a few entries). */
    static final int MAX_LIST = 64;

    private Tms282AttackSections() {
    }

    /** After slv (and melee/shoot addAttackProc): three u32 [3A8],[3AC],[3B0] (265: three skipInt) [verified]. */
    public static final Tms282AttackSection CRCS = (in, kind, ai) -> {
        ai.crc1 = in.decodeInt();
        ai.crc2 = in.decodeInt();
        ai.crc3 = in.decodeInt();
    };

    /**
     * sub_118EDF0 (265 attackBonusReader position), linear extraction [presumed]:
     * <pre>
     * u8 u16 u32 u8 u8 u8 u32 B8 u32 u32 | u32 n1, n1 x u32 | u8 u8 u32 u32 B8 B8 B8 u8 | u32 n2, n2 x u32 |
     * u32 u32 u32 u32 str u32 u32 u8 | B4 n3, n3 x (B4 B4) | u32 | B4 n4, n4 x B4 | u32
     * </pre>
     * 265 differs: one more u8 in the leading bytes, one more u32 before the string, and no n4 list.
     */
    public static final Tms282AttackSection BONUS_282_LINEAR = (in, kind, ai) -> {
        in.decodeByte();
        in.decodeShort();
        in.decodeInt();
        in.decodeByte();
        in.decodeByte();
        in.decodeByte();
        in.decodeInt();
        in.decodeLong();
        in.decodeInt();
        in.decodeInt();
        skipInts(in, count(in, "bonus list 1"));
        in.decodeByte();
        in.decodeByte();
        in.decodeInt();
        in.decodeInt();
        in.decodeLong();
        in.decodeLong();
        in.decodeLong();
        in.decodeByte();
        skipInts(in, count(in, "bonus list 2"));
        in.decodeInt();
        in.decodeInt();
        in.decodeInt();
        in.decodeInt();
        in.decodeString();
        in.decodeInt();
        in.decodeInt();
        in.decodeByte();
        skipInts(in, 2 * count(in, "bonus list 3"));
        in.decodeInt();
        skipInts(in, count(in, "bonus list 4"));
        in.decodeInt();
    };

    /** sub_1207F20: u32, u8 flag, [...] -- the 265 {@link ProcessType#decode} form [verified: sequence]. */
    public static final Tms282AttackSection PROCESS_TYPE = (in, kind, ai) -> {
        ProcessType processType = new ProcessType(new SkillUseInfo(ai.skillInfo, ai.slv));
        processType.decode(in);
        ai.processType = processType;
    };

    /**
     * From ProcessType to the first mob, in 265 order (the send sites show the same fields: skill extras [presumed],
     * u8 buckShot, u8 someMask, [shoot: u32, u8, box], u16 action|left<<15, u32 requestTime, u8 actionType,
     * [evan: u8], u8 attackSpeed, u32 tick, then per kind: melee/shoot u32 bulletSlot + u32 finalAttack (+u8 when
     * &gt; 0); magic/body u32; area dot u32 affectedArea + u32; shoot u16 u8 + 4 x u16 rect) [verified for melee
     * main site, presumed for the others].
     */
    public static final Tms282AttackSection PRE_MOB_265_ORDER = (in, kind, ai) -> {
        int skillID = ai.skillId;
        // Native109EF80 recognizes WZ additional_process11; these captured282 forms are missing it in265 data.
        if ((SkillConstants.isKeyDownSkill(skillID) && (skillID != 33141002
                || net.swordie.ms.connection.protocol.Protocol.active().version()
                != net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3)) || SkillConstants.isSuperNovaSkill(skillID)
                || (skillID == 154141500 || skillID == 154141010 || skillID == 400031072)
                && net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            ai.keyDown = in.decodeInt();
        }
        if (SkillConstants.isRushBombSkill(skillID) || skillID == 5300007 || skillID == 11101030 || skillID == 64101008) {
            ai.grenadeId = in.decodeInt();
        }
        if (SkillConstants.isZeroSkill(skillID)) {
            ai.zeroTag = in.decodeByte();
        }
        if (SkillConstants.isUsercloneSummonedAbleSkill(skillID)) {
            ai.bySummonedID = in.decodeInt();
        }
        if (skillID == AngelicBuster.MIGHTY_MASCOT || skillID == Mercedes.ROLLING_MOONSAULT) {
            in.decodeInt();
        }
        if (skillID == NightWalker.SHADOW_SPEAR_AA_LARGE) {
            ai.shadowSpear1 = in.decodeInt();
            ai.shadowSpear2 = in.decodeInt();
        }
        if (skillID == 400011133 || skillID == DemonAvenger.BAT_SWARM || skillID == Ren.THOUSAND_BLOSSOM_FLURRY_EX
                || skillID == WildHunter.ANOTHER_BITE || SkillConstants.isDelugeSkill(skillID)) {
            in.decodeInt();
        }
        ai.buckShot = in.decodeByte();
        ai.someMask = in.decodeByte();
        if (kind == Tms282AttackKind.SHOOT) {
            in.decodeInt(); // bAdditionalBolt
            ai.isJablin = in.decodeByte() != 0;
            if (ai.boxAttack) {
                in.decodeInt();
                in.decodeShort();
                in.decodeShort();
            }
        }
        if (skillID == 400031033 || skillID == 400031070) {
            ai.isJablin = in.decodeByte() != 0;
            if (ai.boxAttack) {
                in.decodeInt();
                in.decodeShort();
                in.decodeShort();
            }
        }
        ai.mask = in.decodeShort();
        ai.left = ((ai.mask >>> 15) & 1) != 0;
        ai.attackAction = (short) (ai.mask & 0x7FFF);
        ai.requestTime = in.decodeInt();
        ai.attackActionType = in.decodeByte();
        if (skillID == 23111001 || skillID == 80001915 || skillID == 36111010) {
            in.decodeInt();
            in.decodeInt();
            in.decodeInt();
        }
        if (SkillConstants.isEvanForceSkill(skillID)) {
            in.decodeByte();
        }
        ai.attackSpeed = in.decodeByte();
        ai.tick = in.decodeInt();
        switch (kind) {
            case MELEE, SHOOT -> {
                ai.bulletSlot = in.decodeInt();
                ai.finalAttackLastSkillID = in.decodeInt();
                if (ai.finalAttackLastSkillID > 0) {
                    ai.finalAttackByte = in.decodeByte();
                }
            }
            case AREA_DOT -> {
                ai.affectedAreaObjId = in.decodeInt();
                in.decodeInt();
            }
            case MAGIC, BODY -> in.decodeInt();
        }
        if (skillID == RuneStone.LIBERATE_THE_RUNE_OF_THUNDER_ATTACK) {
            ai.maximumMobCount = in.decodeInt();
        }
        if (kind == Tms282AttackKind.SHOOT) {
            in.decodeShort();
            in.decodeByte();
            ai.rect = in.decodeShortRect();
        }
        if (skillID == Buccaneer.SPIRAL_ASSAULT) {
            ai.ignorePCounter = in.decodeByte() != 0;
        }
        if (skillID == Shade.SPIRIT_FRENZY) {
            ai.spiritCoreEnhance = in.decodeInt();
        }
        if (skillID == Lynn.STRIKE_1 || skillID == Lynn.STRIKE_2 || skillID == Lynn.STRIKE_3 || skillID == Lynn.STRIKE_4
                || skillID == Lynn.PREDATOR_BLOW) {
            in.decodeInt();
            if (skillID == Lynn.PREDATOR_BLOW) {
                in.decodeByte();
            }
        }
    };

    /**
     * After a mob's damages, mobUpDownYRange and two crc ints: 265 skill extras, then sub_5956260 read as the 265
     * {@link Gms265AttackDecoder#readMobAttackInfo} form with the verified extra short at 5956B72.
     * Conditional animation/optional-info bodies retain the 265 interpretation; subsequent unknown bytes still
     * require the anchored locator. Captured Tanjiro attacks have four unparsed bytes between mob records.
     */
    public static final Tms282AttackSection.PerMob MOB_INFO_265_ORDER = (in, kind, ai, mai) -> {
        int skillID = ai.skillId;
        if (skillID == Blaster.ROCKET_RUSH) {
            mai.isResWarriorLiftPress = in.decodeByte() != 0;
        } else if (skillID == 400021029) {
            // 282 57F6C8F..57F6CB2: cmp skillId 0x17D7D625 -> Encode4 (F90ED0) only; 265's leading u8 is gone
            in.decodeInt();
        }
        // No 265 PsychicLock pair (2 x i32) in 282: the client's per-mob encoder (57F6C5E..57F6CC8, before 5956260)
        // only has the Rocket Rush / 400021029 extras, and none of the 265 lock ids (142100010, 142110003, ...) exist in
        // the 282 client; 142140001 / 142111002 are 壓制VI / 開局 in 282 (Kinesis audit 2026-09-29).
        readMobAttackInfo(in, mai);
    };

    /** 5956260, including the 282-only short at 5956B72 before the optional-info flag.
     * Use bounded reads: Packet's legacy skip methods can silently seek past the physical packet. */
    static void readMobAttackInfo(InPacket packet, MobAttackInfo mai) {
        ByteBuffer in = ByteBuffer.wrap(packet.getData()).order(ByteOrder.LITTLE_ENDIAN);
        in.position(Math.toIntExact(packet.getPosition()));
        mai.type = in.get();
        mai.currentAnimationName = "";
        if (mai.type == 1 || mai.type == 2) {
            mai.currentAnimationName = mobString(in);
            mobString(in);
            mai.animationDeltaL = in.getInt();
            mai.animationBool = in.get() != 0;
            if (mai.type == 1) {
                mai.hitPartRunTimesSize = mobCount(in, mai.animationBool ? 2 : 0);
                if (mai.animationBool) {
                    mai.hitPartRunTimes = new String[mai.hitPartRunTimesSize];
                    for (int i = 0; i < mai.hitPartRunTimesSize; i++) mai.hitPartRunTimes[i] = mobString(in);
                }
            }
        } else if (mai.type != 0) {
            throw new IllegalArgumentException("unsupported mob animation type " + mai.type);
        }
        mobSkip(in, 1 + 12 + 3 + 4);
        in.getShort(); // 5956B72: mob+65C, absent in GMS 265.
        if (in.get() != 0) {
            mobSkip(in, 8);
            int size = mobCount(in, 22);
            for (int i = 0; i < size; i++) {
                mobSkip(in, 12);
                mobString(in);
                mobSkip(in, 8);
            }
            mobSkip(in, 44);
        }
        mobSkip(in, 8); // template id and context value (5956C04/5956C0F).
        mobSkip(in, 8 * mobCount(in, 8)); // 5956C27/5956C4D/5956C5F.
        packet.seek(in.position());
    }

    private static int mobCount(ByteBuffer in, int minimumWidth) {
        int size = in.getInt();
        if (size < 0 || (minimumWidth > 0 && size > in.remaining() / minimumWidth))
            throw new IllegalArgumentException("invalid mob auxiliary count " + size);
        return size;
    }

    private static String mobString(ByteBuffer in) {
        int size = Short.toUnsignedInt(in.getShort());
        byte[] bytes = new byte[size];
        in.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void mobSkip(ByteBuffer in, int size) {
        if (size < 0 || size > in.remaining()) throw new java.nio.BufferUnderflowException();
        in.position(in.position() + size);
    }

    static int count(InPacket in, String what) {
        int n = in.decodeInt();
        if (n < 0 || n > MAX_LIST) {
            throw new IllegalArgumentException(what + " count " + n + " out of range 0.." + MAX_LIST);
        }
        return n;
    }

    static void skipInts(InPacket in, int n) {
        for (int i = 0; i < n; i++) {
            in.decodeInt();
        }
    }
}
