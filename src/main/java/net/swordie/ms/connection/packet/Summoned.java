package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.avatar.AvatarLook;
import net.swordie.ms.client.character.skills.ShootObject;
import net.swordie.ms.client.character.skills.ShootObjectSkillInfo;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.MobAttackInfo;
import net.swordie.ms.client.jobs.adventurer.PinkBean;
import net.swordie.ms.client.jobs.cygnus.BlazeWizard;
import net.swordie.ms.client.jobs.cygnus.DawnWarrior;
import net.swordie.ms.client.jobs.cygnus.NightWalker;
import net.swordie.ms.client.jobs.flora.Adele;
import net.swordie.ms.client.jobs.legend.Mercedes;
import net.swordie.ms.client.jobs.legend.Shade;
import net.swordie.ms.client.jobs.resistance.BattleMage;
import net.swordie.ms.client.jobs.resistance.Mechanic;
import net.swordie.ms.client.jobs.sengoku.Kanna;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.enums.LeaveType;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.movement.MovementInfo;
import net.swordie.ms.util.Position;

import java.util.Map;

public class Summoned {

    public static OutPacket setReference(Summon summon) {
        OutPacket outPacket = new OutPacket(OutHeader.SUMMONED_SET_REFERENCE);
        outPacket.encodeInt(summon.getOwnerId());
        outPacket.encodeShort(0);
        outPacket.encodeShort(0);
        outPacket.encodeInt(0);
        return outPacket;
    }

    public static OutPacket assistAttackRequest(Summon summon, int summonSkillIdentifier) {
        OutPacket outpacket = new OutPacket(OutHeader.SUMMONED_ASSIST_ATTACK_REQUEST);

        outpacket.encodeInt(summon.getOwnerId());
        outpacket.encodeInt(summon.getObjectId());
        outpacket.encodeInt(summonSkillIdentifier);
        outpacket.encodeInt(0);

        return outpacket;
    }

    public static OutPacket assistSpecialAttackRequest(Summon summon, int summonSkillIdentifier) {
        // something with 400051028~400051032 (Suborbital Strike)
        OutPacket outPacket = new OutPacket(OutHeader.SUMMONED_ASSIST_SPECIAL_ATTACK_REQUEST);

        outPacket.encodeInt(summon.getOwnerId());
        outPacket.encodeInt(summon.getObjectId());
        outPacket.encodeInt(summonSkillIdentifier);
        outPacket.encodeByte(true); // isLeft (?)
        outPacket.encodePositionInt(summon.getPosition());

        return outPacket;
    }

    public static OutPacket attackActive(Summon summon) {
        OutPacket outPacket = new OutPacket(OutHeader.SUMMONED_SUMMON_ATTACK_ACTIVE);

        outPacket.encodeInt(summon.getOwnerId());
        outPacket.encodeInt(summon.getObjectId());
        outPacket.encodeByte(summon.isAttackActive());

        return outPacket;
    }

    public static OutPacket attackActiveNew(Summon summon) {
        OutPacket outPacket = new OutPacket(OutHeader.SUMMONED_SUMMON_ATTACK_ACTIVE_NEW);

        outPacket.encodeInt(summon.getOwnerId());
        outPacket.encodeInt(summon.getObjectId());
        outPacket.encodeByte(summon.isAttackActive());

        return outPacket;
    }

    public static OutPacket doSkill(Summon summon, byte summonSkillType, int summonSkillID, ShootObjectSkillInfo shootObjectSkillInfo) {
        OutPacket outPacket = new OutPacket(OutHeader.SUMMONED_SKILL);

        outPacket.encodeInt(summon.getOwnerId());
        outPacket.encodeInt(summon.getObjectId());
        outPacket.encodeByte(summonSkillType);
        outPacket.encodeInt(summonSkillID);

        if (summon.getSkillID() == BlazeWizard.SALAMANDER_MISCHIEF) {
            outPacket.encodeInt(summon.isHide() ? 1 : 0); // hide = 1  |  unhide = 0
        }

        if (SkillConstants.isShootObjectSummon(summon.getSkillID())) {
            outPacket.encodeInt(shootObjectSkillInfo.getSkillId());
            outPacket.encodeInt(shootObjectSkillInfo.getSlv());
            outPacket.encodeInt(shootObjectSkillInfo.getAction());
            outPacket.encodeInt(shootObjectSkillInfo.getActionSpeed());
            outPacket.encodeInt(shootObjectSkillInfo.getProjectileItemId());
            outPacket.encodePosition(shootObjectSkillInfo.getPosition());

            outPacket.encodeInt(shootObjectSkillInfo.extraEncodeInt1);
            outPacket.encodeInt(shootObjectSkillInfo.extraEncodeInt2);
            outPacket.encodeInt(shootObjectSkillInfo.extraEncodeInt3);
            outPacket.encodeInt(shootObjectSkillInfo.extraEncodeInt4);
            outPacket.encodeInt(shootObjectSkillInfo.extraEncodeInt5);
            outPacket.encodeInt(shootObjectSkillInfo.extraEncodeInt6);
            outPacket.encodeByte(shootObjectSkillInfo.extraEncodeByte1);
            outPacket.encodeInt(shootObjectSkillInfo.extraEncodeInt7);
            outPacket.encodeByte(shootObjectSkillInfo.extraEncodeByte2);
            outPacket.encodeLong(shootObjectSkillInfo.extraEncodeLong1);
            outPacket.encodeInt(shootObjectSkillInfo.extraEncodeInt8);
            outPacket.encodeInt(shootObjectSkillInfo.extraEncodeInt9);

            outPacket.encodeInt(shootObjectSkillInfo.getShootObjects().size());
            for (ShootObject shootObject : shootObjectSkillInfo.getShootObjects()) {
                shootObject.encodeShootObjectRemote(outPacket);
            }
        }

        return outPacket;
    }

    public static OutPacket beholderRevengeAttack(Summon summon, int mob) {
        OutPacket outPacket = new OutPacket(OutHeader.SUMMONED_BEHOLDER_REVENGE_ATTACK);

        outPacket.encodeInt(summon.getOwnerId());//char ID
        outPacket.encodeInt(summon.getObjectId());//summon
        outPacket.encodeInt(mob);//mob

        return outPacket;
    }

    public static OutPacket created(Summon summon, Char owner) {
        var profile = net.swordie.ms.connection.protocol.codec.ProfileEncoders.find(
                net.swordie.ms.connection.protocol.codec.SummonedPacketEncoder.class);
        if (profile.isPresent()) {
            return profile.get().created(summon, owner);
        }
        OutPacket outPacket = new OutPacket(OutHeader.SUMMONED_CREATED);

        outPacket.encodeInt(summon.getOwnerId());
        outPacket.encodeInt(summon.getObjectId());

        outPacket.encodeInt(summon.getSkillID());
        outPacket.encodeInt(summon.getCharLevel());
        outPacket.encodeInt(summon.getSlv());
        // CSummoned::Init
        outPacket.encodePosition(summon.getPosition());
        outPacket.encodeByte(summon.getMoveAction());
        outPacket.encodeShort(summon.getCurFoothold());
        outPacket.encodeByte(summon.getMoveAbility().getVal());
        outPacket.encodeByte(summon.getAssistType().getVal());
        outPacket.encodeByte(summon.getEnterType().getVal());
        outPacket.encodeInt(summon.getMobID()); //maelstrom
        outPacket.encodeByte(summon.isFlyMob());
        outPacket.encodeByte(summon.isBeforeFirstAttack());
        outPacket.encodeInt(summon.getTemplateId()); // 0?
        outPacket.encodeInt(summon.getBulletID());
        AvatarLook al = summon.getAvatarLook();
        outPacket.encodeByte(al != null);
        if (al != null) {
            al.encode(outPacket);
        }
        switch (summon.getSkillID()) {
            case Mechanic.ROCK_N_SHOCK: // 35111002
                outPacket.encodeByte(summon.getTeslaCoilState());
                for (Position pos : summon.getTeslaCoilPositions()) {
                    outPacket.encodePosition(pos);
                }
                break;
            case PinkBean.PINK_SHADOW:
            case PinkBean.PINK_SHADOW_1:
            case PinkBean.PINK_SHADOW_2:
            case NightWalker.DARK_SERVANT:
            case NightWalker.GREATER_DARK_SERVANT:
            case NightWalker.HEXA_GREATER_DARK_SERVANT:
            case NightWalker.SHADOW_ILLUSION:
            case NightWalker.SHADOW_ILLUSION_1:
            case NightWalker.SHADOW_ILLUSION_2:
            case Mercedes.SPIRIT_OF_ELLUEL:
            case Mercedes.SPIRIT_OF_ELLUEL_1:
            case Mercedes.SPIRIT_OF_ELLUEL_2:
            case Mercedes.HEXA_SPIRIT_OF_ELLUEL:
            case Mercedes.HEXA_SPIRIT_OF_ELLUEL_1:
            case Mercedes.HEXA_SPIRIT_OF_ELLUEL_2:
            case 400001071: // Transcendent: Time (Zero)
                outPacket.encodeInt(summon.getActionDelay()); // in ms
                outPacket.encodeInt(summon.getMovementDelay()); // unsure what this exactly is
                break;
            case 400001065: // Afterimage of the Otherworld
                outPacket.encodeInt(0);
                break;
            case Kanna.KISHIN_SHOUKAN:
                outPacket.encodeShort(summon.getKishinValue().getLeft().getLeft());
                outPacket.encodeShort(summon.getKishinValue().getLeft().getRight());
                outPacket.encodeShort(summon.getKishinValue().getRight().getLeft());
                outPacket.encodeShort(summon.getKishinValue().getRight().getRight());
                break;
            case 400051028: // Suborbital Strike
            case 400051029: // Suborbital Strike
            case 400051030: // Suborbital Strike
            case 400051031: // Suborbital Strike
                outPacket.encodeByte(0);
                break;
            case Shade.SPIRIT_BOND_MAX_2:
                //outPacket.encodeInt((summon.getSkillID() - 25121131 + 1) * 400);
                //outPacket.encodeInt((summon.getSkillID() - 25121131 + 1) * 30);
                break;
        }
        outPacket.encodeByte(summon.isJaguarActive());
        outPacket.encodeInt(summon.getSummonTerm());
        outPacket.encodeByte(summon.isAttackActive());
        outPacket.encodeInt(owner.isLeft() ? 1 : 0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(summon.getSkillID() == 162101012 ? 400 : 0);
        if ((summon.getSkillID() >= 33141014 && summon.getSkillID() <= 33141022) || (summon.getSkillID() >= 33001007 && summon.getSkillID() <= 33001015)) {
            outPacket.encodeByte(summon.isSpecialJaguarActive());
            outPacket.encodeByte(0);
        }
        boolean isStateUsing = SkillConstants.isStateUsingSummon(summon.getSkillID());
        outPacket.encodeByte(isStateUsing);
        if (isStateUsing) {
            outPacket.encodeInt(summon.getCount());
            outPacket.encodeInt(summon.getState()); // 0 - 4
        }
        outPacket.encodeInt(summon.getLinkedSummonSkillIds().size());
        for (int linkedSummonSkillId : summon.getLinkedSummonSkillIds()) {
            outPacket.encodeInt(linkedSummonSkillId);
        }
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(-1);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeInt(0);
        if (summon.getSkillID() == 14141017 // HEXA Dark Omen
                || summon.getSkillID() == NightWalker.DARK_OMEN
                || summon.getSkillID() == 14121012 // Dark Omen
                || summon.getSkillID() == 14141020 // HEXA Dark Omen
        ) {
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
        }
        if (summon.getSkillID() == 2341006) { // HEXA Fountain of Vengeance
            outPacket.encodeInt(0);
        }
        if (summon.getSkillID() == Kanna.HEXA_KISHIN_SHOUKAN) {
            outPacket.encodeInt(0);
        }
        if (summon.getSkillID() == Adele.REIGN_OF_DESTRUCTION || summon.getSkillID() == Adele.HEXA_REIGN_OF_DESTRUCTION) {
            outPacket.encodeInt(summon.getSummonTerm()); // 7000
        }
        if (summon.getSkillID() == DawnWarrior.RIFT_OF_DAMNATION_SUMMON) {
            outPacket.encodeInt(0);
        }
        if (summon.getSkillID() == BattleMage.ALTAR_OF_ANNIHILATION) {
            outPacket.encodeInt(0);
        }
        if (summon.getSkillID() == 164141056) { // HEXA Talisman: Seeking Ghost Flame
            outPacket.encodeInt(0);
        }
        if (summon.getSkillID() == 152141501) { // Mytocrystal Expanse
            outPacket.encodeInt(0);
        }
        if (summon.getSkillID() == Mechanic.ROCK_N_SHOCK
                || summon.getSkillID() == 35141007 // HEXA Rock 'n Shock
        ) {
            //outPacket.encodeInt(0);
        }
        if (summon.getSkillID() == Kanna.TENGU_STRIKE
                || summon.getSkillID() == Kanna.HEXA_TENGU_STRIKE) {
            outPacket.encodeInt(0);
        }
        if (summon.getSkillID() == Kanna.TENGU_STRIKE_SUMMON_L
                || summon.getSkillID() == Kanna.TENGU_STRIKE_SUMMON_R
                || summon.getSkillID() == Kanna.HEXA_TENGU_STRIKE_SUMMON_L
                || summon.getSkillID() == Kanna.HEXA_TENGU_STRIKE_SUMMON_R) {
            outPacket.encodeByte(0);
        }

        return outPacket;
    }

    public static OutPacket removed(Summon summon, LeaveType leaveType) {
        OutPacket outPacket = new OutPacket(OutHeader.SUMMONED_REMOVED);

        outPacket.encodeInt(summon.getOwnerId());
        outPacket.encodeInt(summon.getObjectId());
        outPacket.encodeByte(leaveType.getVal());

        return outPacket;
    }

    public static OutPacket attack(Char chr, AttackInfo ai, boolean counterAttack) {
        OutPacket outPacket = new OutPacket(OutHeader.SUMMONED_ATTACK);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeInt(ai.summon.getObjectId());
        outPacket.encodeInt(ai.summon.getCharLevel());
        byte left = (byte) (ai.left ? 1 : 0);
        outPacket.encodeByte((left << 7) | ai.attackActionType);
        byte attackCount = (byte) (!ai.mobAttackInfo.isEmpty() ? ai.mobAttackInfo.getFirst().damages.length : 0);
        outPacket.encodeByte((ai.mobCount << 4) | (attackCount & 0xF));
        for (MobAttackInfo mai : ai.mobAttackInfo) {
            outPacket.encodeInt(mai.mobId);
            outPacket.encodeByte(mai.byteIdk1);
            if (ai.skillId == 400001071) {
                outPacket.encodeInt(0);
            }
            for (long dmg : mai.damages) {
                outPacket.encodeLong(dmg);
            }
        }
        outPacket.encodeByte(counterAttack); // bCounterAttack
        outPacket.encodeByte(ai.attackAction == 0); // bNoAction
        outPacket.encodePosition(ai.grenadePos != null ? ai.grenadePos : ai.summon.getPosition());
        outPacket.encodeInt(ai.skillId == ai.summon.getSkillID() ? (ai.summonSpecialSkillId == 0 ? ai.skillId : ai.summonSpecialSkillId) : ai.skillId); // Attack Skill ID  TODO
        outPacket.encodeByte(false);
        outPacket.encodePosition(new Position());
        outPacket.encodeByte(ai.skillId == 400021151);

        return outPacket;
    }

    public static OutPacket move(int charID, int summonID, MovementInfo movementInfo) {
        OutPacket outPacket = new OutPacket(OutHeader.SUMMONED_MOVE);

        outPacket.encodeInt(charID);

        outPacket.encodeInt(summonID);
        outPacket.encode(movementInfo);

        return outPacket;
    }

    public static OutPacket updateHPTag(Summon summon) {
        OutPacket outPacket = new OutPacket(OutHeader.SUMMONED_UPDATE_HP_TAG);

        outPacket.encodeInt(summon.getOwnerId());
        outPacket.encodeInt(summon.getObjectId());
        outPacket.encodeInt(summon.getHp());

        return outPacket;
    }

    public static OutPacket specialAssistSkill(Summon summon, int skillID) {
        OutPacket outPacket = new OutPacket(OutHeader.SUMMONED_SPECIAL_ASSIST_SKILL);

        outPacket.encodeInt(summon.getOwnerId());
        outPacket.encodeInt(summon.getObjectId());
        outPacket.encodeInt(skillID);
        outPacket.encodeByte((skillID == 400041050 || skillID == 400041051) ? 1 : 0);

        return outPacket;
    }

    public static OutPacket stateChanged(Summon summon, int type, Map<Integer, Boolean> crystalSkillModeMap) {
        OutPacket outPacket = new OutPacket(OutHeader.SUMMONED_STATE_CHANGED);

        outPacket.encodeInt(summon.getOwnerId());
        outPacket.encodeInt(summon.getObjectId());
        outPacket.encodeInt(type);
        switch (type) {
            case 1: // Rift of Damnation
                break;
            case 2: // Illium
                outPacket.encodeInt(crystalSkillModeMap.size()); // loop size
                for (Map.Entry<Integer, Boolean> entry : crystalSkillModeMap.entrySet()) {
                    outPacket.encodeInt(entry.getKey());            // Crystal Skill Idx
                    outPacket.encodeInt(entry.getValue() ? 1 : 0);  // boolean  Can use
                }
                break;
            // maybe more cases
        }

        return outPacket;
    }

    public static OutPacket attackDone(Summon summon, boolean isMiss, int targetID) {
        OutPacket outPacket = new OutPacket(OutHeader.SUMMONED_ATTACK_DONE);

        outPacket.encodeInt(summon.getObjectId());
        outPacket.encodeByte(!isMiss);
        outPacket.encodeInt(targetID);

        return outPacket;
    }

    public static OutPacket guardiansAttack(Summon summon, int type) {
        OutPacket outpacket = new OutPacket(OutHeader.SUMMONED_NOVA_GUARDIANS_ATTACK);

        outpacket.encodeInt(summon.getOwnerId());
        outpacket.encodeInt(summon.getObjectId());
        outpacket.encodeInt(type);
        outpacket.encodeInt(0);

        return outpacket;
    }

    public static OutPacket setResist(Summon summon, int type) {
        OutPacket outpacket = new OutPacket(OutHeader.SUMMONED_SET_RESIST);

        outpacket.encodeInt(summon.getOwnerId());
        outpacket.encodeInt(summon.getObjectId());
        outpacket.encodeInt(type);

        return outpacket;
    }

    public static OutPacket actionChange(Summon summon, byte action) {
        OutPacket outpacket = new OutPacket(OutHeader.SUMMONED_ACTION_CHANGE);

        outpacket.encodeInt(summon.getOwnerId());
        outpacket.encodeInt(summon.getObjectId());
        outpacket.encodeByte(action);

        return outpacket;
    }

    public static OutPacket reposition(Summon summon, int skillId, Position newPosition) {
        OutPacket outPacket = new OutPacket(OutHeader.SUMMONED_REPOSITION_SUMMON);

        outPacket.encodeInt(summon.getOwnerId());
        outPacket.encodeInt(summon.getObjectId());
        outPacket.encodeInt(skillId);
        outPacket.encodeInt(summon.getSlv());
        outPacket.encodePositionInt(newPosition);

        return outPacket;
    }

    public static OutPacket effect(Summon summon, int effectType) {
        // something with 400021054 (Spirit's Domain)
        // also used by Hayato's 3rd V skill summon
        OutPacket outPacket = new OutPacket(OutHeader.SUMMONED_EFFECT);

        outPacket.encodeInt(summon.getOwnerId());
        outPacket.encodeInt(summon.getObjectId());
        outPacket.encodeInt(effectType); // seems to show different summon effects
        switch (effectType) {
            case 0:
            case 1:
            case 2:
                // no decodes in IDA
                break;
            case 3:
                outPacket.encodeInt(0); // unk
                break;
            case 4:
                outPacket.encodeInt(0); // unk
                break;
        }

        return outPacket;
    }

    public static OutPacket useSpecifiedSkill(Summon summon, int skillId) {
        OutPacket outPacket = new OutPacket(OutHeader.SUMMONED_SUMMON_USE_SPECIFIED_SKILL);

        outPacket.encodeInt(summon.getOwnerId());
        outPacket.encodeInt(summon.getObjectId());
        outPacket.encodeInt(skillId);

        return outPacket;
    }

    public static OutPacket upgradeStage(Summon summon, int type) {
        OutPacket outPacket = new OutPacket(OutHeader.SUMMONED_UPGRADE_STAGE);

        outPacket.encodeInt(summon.getOwnerId());
        outPacket.encodeInt(summon.getObjectId());

        outPacket.encodeInt(type); // still unsure about the exact structure.
        switch (type) {
            case 0:
                outPacket.encodeInt(0); // unk
                outPacket.encodeInt(0); // unk
                break;
            case 1:
                // no decodes
                break;
            case 2:
                outPacket.encodeInt(summon.getCount()); // crystal Count
                outPacket.encodeInt(summon.getState()); // crystal State
                break;
            case 3:
                // no decodes
                break;
            case 4:
                outPacket.encodeInt(0); // unk
                outPacket.encodeInt(0); // unk
                break;

        }

        return outPacket;
    }

    public static OutPacket sequenceAttack(Summon summon, int a, int b, int c) {
        OutPacket outPacket = new OutPacket(OutHeader.SUMMONED_SEQUENCE_ATTACK);

        outPacket.encodeInt(summon.getOwnerId());
        outPacket.encodeInt(summon.getObjectId());
        outPacket.encodeInt(a);
        outPacket.encodeInt(b);
        outPacket.encodeInt(c);

        return outPacket;
    }

    public static OutPacket lotusFlower(Summon summon) {
        OutPacket outPacket = new OutPacket(OutHeader.SUMMONED_LOTUS_POWER);

        outPacket.encodeInt(summon.getOwnerId());
        outPacket.encodeInt(summon.getObjectId());

        return outPacket;
    }

    public static OutPacket erdaFountain(Summon summon, int defeated, boolean isReleased) {
        OutPacket outPacket = new OutPacket(OutHeader.SUMMONED_ERDA_FOUNTAIN);

        outPacket.encodeInt(summon.getOwnerId());
        outPacket.encodeInt(summon.getObjectId());
        outPacket.encodeInt(defeated);
        outPacket.encodeByte(isReleased);

        return outPacket;
    }

    public static OutPacket spiritDomainState(int broadcastToChr, int ownerChr, int state) {
        OutPacket outPacket = new OutPacket(OutHeader.SUMMONED_SPIRIT_DOMAIN_STATE);

        outPacket.encodeInt(broadcastToChr);
        outPacket.encodeInt(ownerChr);
        outPacket.encodeInt(state);

        return outPacket;
    }
}
