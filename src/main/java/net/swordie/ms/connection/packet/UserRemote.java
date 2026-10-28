package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.PortableChair;
import net.swordie.ms.client.character.avatar.AvatarLook;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.ShootObject;
import net.swordie.ms.client.character.skills.ShootObjectSkillInfo;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.MobAttackInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.adventurer.magician.FirePoison;
import net.swordie.ms.client.jobs.adventurer.pirate.Buccaneer;
import net.swordie.ms.client.jobs.anima.HoYoung;
import net.swordie.ms.client.jobs.cygnus.DawnWarrior;
import net.swordie.ms.client.jobs.resistance.BattleMage;
import net.swordie.ms.client.jobs.resistance.Blaster;
import net.swordie.ms.client.social.Guild.Guild;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.constants.SixthJobSkillConstants;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.enums.AvatarModifiedMask;
import net.swordie.ms.enums.BaseStat;
import net.swordie.ms.enums.ChairType;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.life.movement.MovementInfo;
import net.swordie.ms.util.Position;

import java.util.EnumMap;
import java.util.List;

public class UserRemote {
    public static OutPacket setActiveNickItem(Char chr, String msg) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_SET_ACTIVE_NICK_ITEM);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeInt(chr.getActiveNickItemID());
        outPacket.encodeByte(msg != null);
        if (msg != null) {
            outPacket.encodeString(msg);
        }
        if (chr.getActiveNickItemID() == 3700623) {
            chr.encodeCustomNickName(outPacket);
        }

        return outPacket;
    }

    public static OutPacket move(Char chr, MovementInfo movementInfo) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_MOVE);

        outPacket.encodeInt(chr.getId());

        outPacket.encode(movementInfo);

        return outPacket;
    }

    public static OutPacket emotion(int charID, int emotion, int duration, boolean byItemOption) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_EMOTION);

        outPacket.encodeInt(charID);

        outPacket.encodeInt(emotion);
        outPacket.encodeInt(duration);
        outPacket.encodeByte(byItemOption);

        return outPacket;
    }

    public static OutPacket attack(Char chr, AttackInfo ai) {
        OutHeader attackType = ai.attackHeader;
        int skillID = ai.skillId;
        OutPacket outPacket = new OutPacket(attackType);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();

        outPacket.encodeInt(chr.getId());

        outPacket.encodeByte(ai.fieldKey);
        outPacket.encodeByte(ai.mobCount << 4 | ai.hits);
        outPacket.encodeInt(chr.getLevel());
        outPacket.encodeInt(ai.slv);
        if (ai.slv > 0) {
            outPacket.encodeInt(skillID);
        }
        if (SkillConstants.isZeroSkill(skillID)) {
            outPacket.encodeByte(ai.zeroTag);
            if (ai.zeroTag != 0) {
                outPacket.encodePosition(chr.getPosition());
            }
        }
        if (skillID != 101110104 && skillID != 101141016) {
            if (attackType == OutHeader.REMOTE_SHOOT_ATTACK
                    && (SkillConstants.getAdvancedCountHyperSkill(skillID) != 0
                    || SkillConstants.getAdvancedAttackCountHyperSkill(skillID) != 0)) {
                int passiveId;
                int passiveLv;
                if (SkillConstants.getAdvancedCountHyperSkill(skillID) == 0) {
                    if (SkillConstants.getAdvancedAttackCountHyperSkill(skillID) == 0) {
                        passiveId = 0;
                        passiveLv = 0;
                    } else {
                        passiveId = SkillConstants.getAdvancedAttackCountHyperSkill(skillID);
                        passiveLv = chr.getSkillLevel(passiveId);
                    }
                } else {
                    passiveId = SkillConstants.getAdvancedCountHyperSkill(skillID);
                    passiveLv = chr.getSkillLevel(passiveId);
                }
                outPacket.encodeInt(passiveLv);
                if (passiveLv > 0) {
                    outPacket.encodeInt(passiveId);
                }
            }
            if (skillID == 80001850) {
                int passiveLv = chr.getSkillLevel(80001851);
                outPacket.encodeInt(passiveLv);
                if (passiveLv != 0) {
                    outPacket.encodeInt(80001851);
                }
            }
            outPacket.encodeByte(ai.someMask);
            outPacket.encodeByte(ai.buckShot); // check
            outPacket.encodeInt(ai.option3);
            outPacket.encodeInt(ai.bySummonedID);
            outPacket.encodeInt(ai.processType.skillUseInfo.count); // v263
            outPacket.encodeByte(0); // v263
            if ((ai.buckShot & 2) != 0) {
                if (tsm.hasStat(CharacterTemporaryStat.BuckShot)) {
                    var o = tsm.getOption(CharacterTemporaryStat.BuckShot);
                    outPacket.encodeInt(o.rOption);
                    outPacket.encodeInt(o.nOption);
                } else {
                    outPacket.encodeInt(0);
                    outPacket.encodeInt(0);
                }
            }
            if ((ai.buckShot & 8) != 0) {
                outPacket.encodeByte(ai.slv);
            }
            byte left = (byte) (ai.left ? 1 : 0);
            outPacket.encodeShort((left << 15) | ai.attackAction);
            outPacket.encodeByte(ai.dragon ? ai.dragonAttackActionType : ai.attackActionType);
            outPacket.encodeShort(ai.x);
            outPacket.encodeShort(ai.y);
            outPacket.encodeByte(ai.dragonAttackStart);
            outPacket.encodeByte(ai.dragonAttackProgess);
            outPacket.encodeByte(ai.attackSpeed);

            outPacket.encodeByte(ai.mastery);
            outPacket.encodeInt(ai.bulletID);
            for (MobAttackInfo mai : ai.mobAttackInfo) {
                outPacket.encodeInt(mai.mobId);
                if (mai.mobId != 0) {
                    outPacket.encodeByte(mai.byteIdk1);
                    outPacket.encodeByte(mai.byteIdk2);
                    outPacket.encodeByte(mai.byteIdk3);
                    outPacket.encodeShort(mai.byteIdk4);
                    outPacket.encodeInt(0); // v214
                    outPacket.encodeInt(0); // v214
                    if (ai.skillId == 162111005 || ai.skillId == 400021122) {
                        outPacket.encodeInt(0); // v214
                    }
                    if (ai.skillId == 80001835 || ai.skillId == 42111002 || ai.skillId == 80011050) {
                        // Soul Shear
                        outPacket.encodeByte(ai.hits);
                        outPacket.encodeLong(0); // not exactly sure
                    }
                    var hitCodec = net.swordie.ms.connection.protocol.codec.RemoteAttackHitCodec.active(); // 282: crit in bit 63
                    for (int i = 0; i < mai.damages.length; i++) {
                        hitCodec.writeHit(outPacket, mai.crits != null && mai.crits[i], mai.damages[i]); // isCrit, damage
                    }
                    if (SkillConstants.isKinesisPsychicLockSkill(ai.skillId)) {
                        outPacket.encodeInt(mai.psychicLockInfo);
                    }
                    if (ai.skillId == Blaster.ROCKET_RUSH) {
                        outPacket.encodeByte(mai.isResWarriorLiftPress);
                    }
                    if (skillID == HoYoung.TALISMAN_EVIL_SEALING_GOURD_EFFECT) {
                        outPacket.encodeInt(mai.templateID); // mobId
                    }
                }
            }
            if (skillID == 2221052 || skillID == 80003075) {
                outPacket.encodeInt(ai.keyDown);
            }
            if (skillID == 80001762) {
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
            }
            if (SkillConstants.isSuperNovaSkill(skillID)
                    || SkillConstants.isScreenCenterAttackSkill(skillID)
                    || skillID == 101000202
                    || skillID == 101141032
                    || skillID == 101000102
                    || skillID == 101141028
                    || skillID == 80002212
                    || skillID == 80002463
                    || skillID == 400041019 || skillID == 400041024 || skillID == 400041080 // (v145 = v246 - 400041019, v145 <= 61) && _bittest64(&v141, v145)
                    || skillID == 400031016
                    || skillID == 31240014
                    || skillID == 3221019
                    || SkillConstants.isWingedJavelinOrAbyssalCast(skillID)
                    || skillID == 400021075
                    || skillID == 14111036
                    || skillID == 400001055 || skillID == 400001056) {
                outPacket.encodePositionInt(ai.ptTarget);
            }
            if (skillID == 400021062) { // Reaction - Spectral Blast
                outPacket.encodePositionInt(chr.getPosition());
            }
            if (skillID == 80002452) { // Fruitful Bounty
                outPacket.encodePositionInt(chr.getPosition());
            }
            if (skillID == 400011132 || skillID == 400011134) { // Mighty Mjolnir + Ego Weapon
                outPacket.encodePositionInt(chr.getPosition());
            }
            if (skillID == 400021097 || skillID == 400021098) { // Law of Gravity
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
            }
            if (skillID == 400051075 || skillID == 500061030) { // Poolmaker
                outPacket.encodeByte(ai.poolmakerEnabled); // bEnable
                outPacket.encodePositionInt(ai.ptTarget);
            }
            if (skillID == 400041062 || skillID == 400041064 || skillID == 400041065 || skillID == 400041066 || skillID == 400041074 || skillID == 400041079
                    || skillID == 400051080
                    || skillID == 400011125 || skillID == 400011126
                    || skillID == 36141008
                    || skillID == 155121007 // sub_141083250(155121007, v144)
                    || skillID == 80003017
                    || skillID == 22201501
                    || skillID == 1241006
                    || skillID == 11141007
                    || skillID == 400041082 || skillID == 400041083
                    || skillID == 5241501
                    || skillID == 64141003) {
                outPacket.encodePositionInt(ai.pos);
            }
            if (skillID == 400051065 || skillID == 400051067) {
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
            }
            if (skillID == 64121012
                    || skillID == 64121013
                    || skillID == 64121014
                    || skillID == 64121015
                    || skillID == 64121017
                    || skillID == 64121018
                    || skillID == 64121019) {
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
            }
            if (skillID == 35121019 || skillID == 35141002) {
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
            }
            if (skillID == 27141000 || skillID == 36141000 || skillID == 36141001 || skillID == 400021107) {
                outPacket.encodeInt(0); // todo
            }
            if (skillID == 63111005
                    || skillID == 63111105
                    || skillID == 400031033
                    || skillID == 400031070) {
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
            }
            if (skillID == 63111106) {
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
            }
            if (skillID == 162101009
                    || skillID == 162121017
                    || skillID == 162121007) {
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
            }
            if (skillID == 1221020
                    || skillID == 1241502
                    || skillID == 5121025
                    || skillID == 5141006
                    || skillID == 5311014
                    || skillID == 5311015
                    || skillID == 164121043
                    || skillID == 12141006
                    || skillID == 5141503
                    || SixthJobSkillConstants.isKnownAscentSkill(skillID)
                    || skillID == 35141010
                    || skillID == 400011099 || skillID == 400011101
                    || skillID == 21121005
                    || skillID == 400011145 || skillID == 400011146
                    || skillID == 400021141) {
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
            }
            if (skillID == 400041092) {
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
            }
            if (skillID >= 37120055 && skillID <= 37120058) {
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
            }
            if (SkillConstants.isKeydownSkillRectMoveXY(skillID)) {
                outPacket.encodePosition(ai.keyDownRectMoveXY);
            }
            if (skillID == 400021074 || skillID == 63141501 || skillID == 63141505 || skillID == 22170093) {
                outPacket.encodeByte(ai.showFixedDamage);
            }
            if (skillID == 112110003) { // formation attack
                outPacket.encodeInt(0);
            }
            if (skillID == 42100007) { // Soul Bomb
                outPacket.encodeShort(0);
                byte size = 0;
                outPacket.encodeByte(size);
                for (int i = 0; i < size; i++) {
                    outPacket.encodePosition(new Position());
                }
            }
            if (skillID == Blaster.HYPER_MAGNUM_PUNCH
                    || SkillConstants.isShadowAssault(skillID)
                    || skillID == DawnWarrior.EQUINOX_SLASH
                    || SkillConstants.isOctoPunch(skillID)) {
                outPacket.encodeByte(ai.teleportByte);
                outPacket.encodePositionInt(ai.teleportPt);
            }
            if (skillID == BattleMage.ABYSSAL_LIGHTNING_PORTAL_ATTACK) {
                outPacket.encodeRectInt(ai.rect);
            }
            if (skillID == 35121016 || skillID == 35141001 || skillID == 35111007 || skillID == 14141003) {
                outPacket.encodeLong(0);
                outPacket.encodeLong(0);
            }
            if (SkillConstants.isRenSpiritStrike(skillID)) {
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
            }
            if (SkillConstants.isShootObjectSkill(skillID)) {
                outPacket.encodePosition(ai.teleportPt);
                if (SkillConstants.isSomePathfinderSkill(skillID)) {
                    outPacket.encodeInt(ai.shootObjId);
                    outPacket.encodeByte(ai.pathFinderBool);
                }
            }
            if (skillID == 400001069) { // Transcendent: Light
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
            }
            if (SkillConstants.isThrowableAttackSkill(skillID)) {
                outPacket.encodeInt(ai.throwableAttack);
                outPacket.encodeByte(ai.throwableAttackFootHold);
            }
            if (SkillConstants.sub_144DB7E40(skillID)
                    || skillID == Buccaneer.SERPENT_VORTEX
                    || skillID == 155101104
                    || skillID == 155101204
                    || SkillConstants.isJetPack(skillID)
                    || skillID == 41121022) {
                outPacket.encodeByte(ai.across);
                if (ai.across) {
                    outPacket.encodePositionInt(ai.acrossPos);
                }
            }
            if (skillID == 400011139) { // Rift of Damnation
                outPacket.encodeByte(ai.rift1);
                if (ai.rift1) {
                    outPacket.encodeInt(0);
                    outPacket.encodeInt(0);
                    outPacket.encodeInt(0);
                }
                outPacket.encodeByte(ai.rift2);
                if (ai.rift2) {
                    outPacket.encodeInt(0);
                    outPacket.encodeInt(0);
                    outPacket.encodeInt(0);
                }
            }
            if (skillID == FirePoison.CREEPING_TOXIN_EXTRA) {
                outPacket.encodeByte(ai.across);
                if (ai.across) {
                    outPacket.encodePositionInt(ai.acrossPos);
                }
            }
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
        }

        return outPacket;
    }

    public static OutPacket shootObject(ShootObjectSkillInfo shootObjectSkillInfo) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_SHOOT_OBJECT);

        outPacket.encodeInt(shootObjectSkillInfo.getCharID());
        outPacket.encodeInt(shootObjectSkillInfo.getSkillId());
        outPacket.encodeInt(shootObjectSkillInfo.getSlv());
        outPacket.encodeInt(shootObjectSkillInfo.getAction());
        outPacket.encodeInt(shootObjectSkillInfo.getActionSpeed());
        outPacket.encodeByte(shootObjectSkillInfo.getUnknownBool());
        outPacket.encodeInt(shootObjectSkillInfo.getProjectileItemId());
        outPacket.encodeByte(shootObjectSkillInfo.isEncodeExtra());
        if (shootObjectSkillInfo.isEncodeExtra()) {
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
        }
        if (shootObjectSkillInfo.getSkillId() == 154121001
                || shootObjectSkillInfo.getSkillId() == 154141009) { // Khali
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeByte(0);
        }
        outPacket.encodeInt(shootObjectSkillInfo.getShootObjects().size());
        for (ShootObject shootObject : shootObjectSkillInfo.getShootObjects()) {
            shootObject.encodeShootObjectRemote(outPacket);
        }

        return outPacket;
    }

    public static OutPacket avatarModified(Char chr, byte mask, byte carryItemEffect) {
        var profile = net.swordie.ms.connection.protocol.codec.ProfileEncoders.find(
                net.swordie.ms.connection.protocol.codec.AvatarModifiedEncoder.class);
        if (profile.isPresent()) {
            return profile.get().avatarModified(chr, mask, carryItemEffect);
        }
        AvatarLook al = chr.getAvatarData().getAvatarLook();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();

        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_AVATAR_MODIFIED);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeByte(mask);
        if ((mask & AvatarModifiedMask.AvatarLook.getVal()) != 0) {
            al.encode(outPacket);
            outPacket.encodeString("");
        }
        if ((mask & AvatarModifiedMask.SubAvatarLook.getVal()) != 0) {
            al.encode(outPacket); // subAvatarLook
        }
        if ((mask & AvatarModifiedMask.Speed.getVal()) != 0) {
            outPacket.encodeByte(tsm.getOption(CharacterTemporaryStat.Speed).nOption);
        }
        if ((mask & AvatarModifiedMask.CarryItemEffect.getVal()) != 0) {
            outPacket.encodeByte(carryItemEffect);
        }
        boolean hasCouple = chr.getCouple() != null;
        outPacket.encodeByte(hasCouple);
        if (hasCouple) {
            chr.getCouple().encodeForRemote(outPacket);
        }
        boolean hasFriendShip = chr.getFriendshipRingRecord() != null;
        outPacket.encodeByte(hasFriendShip);
        if (hasFriendShip) {
            chr.getFriendshipRingRecord().encode(outPacket);
        }
        boolean hasWedding = chr.getMarriageRecord() != null;
        outPacket.encodeByte(hasWedding);
        if (hasWedding) {
            chr.getMarriageRecord().encode(outPacket);
        }
        outPacket.encodeInt(chr.getCompletedSetItemID());
        outPacket.encodeInt(chr.getTotalChuc(false));
        outPacket.encodeInt(chr.getTotalArc());
        outPacket.encodeInt(chr.getTotalAut());

        return outPacket;
    }

    public static OutPacket androidEmotion(int charID, int emotion, int duration) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_ANDROID_EMOTION);

        outPacket.encodeInt(charID);

        outPacket.encodeInt(emotion);
        outPacket.encodeInt(duration);

        return outPacket;
    }

    public static OutPacket setActiveEffectItem(Char chr, int itemID) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_SET_ACTIVE_EFFECT_ITEM);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeInt(itemID);

        return outPacket;
    }

    public static OutPacket setMonkeyEffectItem(Char chr, int itemID) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_SET_MONKEY_EFFECT_ITEM);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeInt(itemID);

        return outPacket;
    }

    public static OutPacket throwGrenade(int charID, int grenadeID, Position pos, int keyDown, int skillID, int bySummonedID,
                                         int slv, boolean left, int attackSpeed, int unk1, int unk2, int unk3, int unk4) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_THROW_GRENADE);

        outPacket.encodeInt(charID);

        outPacket.encodeInt(grenadeID);
        outPacket.encodePositionInt(pos);
        outPacket.encodeInt(keyDown);
        outPacket.encodeInt(skillID);
        outPacket.encodeInt(bySummonedID);
        outPacket.encodeInt(slv);
        outPacket.encodeByte(left);
        outPacket.encodeInt(attackSpeed);
        outPacket.encodeInt(unk3);
        outPacket.encodeInt(unk4);

        outPacket.encodeInt(unk1);
        outPacket.encodeInt(unk2);

        return outPacket;
    }

    public static OutPacket keyDownAreaMovePath(int charID, List<Byte> keys) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_KEY_DOWN_AREA_MOVE_PATH);

        outPacket.encodeInt(charID);

        outPacket.encodeByte(keys.size());
        for (Byte key : keys) {
            outPacket.encodeByte(key);
        }

        return outPacket;
    }

    public static OutPacket destroyGrenade(int charID, int grenadeID, int skillID) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_DESTROY_GRENADE);

        outPacket.encodeInt(charID);

        outPacket.encodeInt(grenadeID);
        outPacket.encodeInt(skillID);

        return outPacket;
    }

    public static OutPacket receiveHP(Char chr) {
        return receiveHP(chr.getId(), chr.getHP(), chr.getTotalStat(BaseStat.mhp));
    }

    public static OutPacket receiveHP(int charID, int curHP, int maxHP) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_RECEIVE_HP);

        outPacket.encodeInt(charID);

        outPacket.encodeInt(curHP);
        outPacket.encodeInt(maxHP);

        return outPacket;
    }

    public static OutPacket guildNameChanged(int charID, String newName) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_GUILD_NAME_CHANGED);

        outPacket.encodeInt(charID);

        outPacket.encodeString(newName);

        return outPacket;
    }

    public static OutPacket guildMarkChanged(int charID, Guild guild) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_GUILD_MARK_CHANGED);

        outPacket.encodeInt(charID);

        outPacket.encodeShort(guild.getMarkBg());
        outPacket.encodeByte(guild.getMarkBgColor());
        outPacket.encodeShort(guild.getMark());
        outPacket.encodeByte(guild.getMarkColor());
        outPacket.encodeInt((guild.getCustomEmblem() != null && guild.getCustomEmblem().length > 0) ? 1 : 0);
        if (guild.getCustomEmblem() != null && guild.getCustomEmblem().length > 0) {
            outPacket.encodeInt(guild.getId());
            outPacket.encodeInt(guild.getCustomEmblem().length);
            for (byte b : guild.getCustomEmblem()) {
                outPacket.encodeByte(b);
            }
        }

        return outPacket;
    }

    public static OutPacket hit(Char chr, HitInfo hitInfo) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_HIT);

        outPacket.encodeInt(chr.getId());

        if (hitInfo.mobID == 0) {
            hitInfo.type = -3;
        }

        boolean hit = hitInfo.hpDamage == 0;
        outPacket.encodeByte(hitInfo.type);
        outPacket.encodeInt(hitInfo.hpDamage);
        outPacket.encodeByte(hitInfo.isCrit);
        outPacket.encodeByte(hit);
        boolean idk = false;
        if (!hit) {
            outPacket.encodeByte(idk);
        }
        if (!idk) {
            if (hitInfo.type == -8) {
                outPacket.encodeInt(hitInfo.blockSkillId);
                outPacket.encodeInt(0); // ignored
                outPacket.encodeInt(hitInfo.otherUserID);
                outPacket.encodeByte(0);
            } else if (hitInfo.type == -12) {
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
                outPacket.encodeByte(0);
                outPacket.encodeInt(0);
                // todo
            } else if (hitInfo.type >= -1) {
                outPacket.encodeInt(hitInfo.templateID);
                outPacket.encodeByte(hitInfo.action);
                outPacket.encodeInt(hitInfo.mobID);

                outPacket.encodeInt(0); // ignored
                outPacket.encodeInt(hitInfo.reflectDamage);
                outPacket.encodeByte(hit); // bGuard
                if (hitInfo.reflectDamage > 0) {
                    outPacket.encodeByte(hitInfo.isGuard);
                    outPacket.encodeInt(hitInfo.mobID);
                    outPacket.encodeByte(hitInfo.hitAction);
                    outPacket.encodePosition(chr.getPosition());
                }
                outPacket.encodeByte(hitInfo.specialEffectSkill);
                if ((hitInfo.specialEffectSkill & 1) != 0) {
                    outPacket.encodeInt(hitInfo.stanceSkillID);
                }
            }
            outPacket.encodeInt(hitInfo.hpDamage);
            if (hitInfo.hpDamage == -1) {
                outPacket.encodeInt(hitInfo.userSkillID);
            }
        }

        return outPacket;
    }

    public static OutPacket effect(int charID, Effect effect) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_EFFECT);

        outPacket.encodeInt(charID);

        effect.encode(outPacket);
        outPacket.encodeInt(-1); // v265.1

        return outPacket;
    }

    public static OutPacket zeroTag(Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_ZERO_TAG);

        outPacket.encodeInt(chr.getId());

        chr.getAvatarData().getAvatarLook(chr.isZeroBeta()).encode(outPacket);

        return outPacket;
    }

    public static OutPacket zeroLastAssistState(int charID) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_ZERO_LAST_ASSIST_STATE);

        outPacket.encodeInt(charID);

        return outPacket;
    }

    public static OutPacket setMoveGrenade(Char chr, int skillID, int grenadeID, long walkSpeed, int moveEndingX, boolean isLeft) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_SET_MOVE_GRENADE);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeInt(skillID);
        outPacket.encodeInt(grenadeID);
        outPacket.encodeLong(walkSpeed);
        outPacket.encodeInt(moveEndingX);
        outPacket.encodeByte(isLeft);

        return outPacket;
    }

    public static OutPacket setCustomizeEffect(Char chr, int itemID, String path) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_SET_CUSTOMIZE_EFFECT);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeInt(itemID);
        outPacket.encodeString(path);

        return outPacket;
    }

    public static OutPacket setDefaultWingItem(Char chr, int itemID) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_SET_DEFAULT_WING_ITEM);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeInt(itemID);

        return outPacket;
    }

    public static OutPacket setKaiserTransformItem(Char chr, int itemID, byte type) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_SET_KAISER_TRANSFORM_ITEM);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeInt(itemID);
        outPacket.encodeByte(type);

        return outPacket;
    }

    public static OutPacket showUpgradeTombEffect(Char chr, int itemID, Position pos) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_SHOW_UPGRADE_TOMB_EFFECT);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeInt(itemID);
        outPacket.encodePositionInt(pos);

        return outPacket;
    }

    public static OutPacket setTemporaryStat(Char chr, EnumMap<CharacterTemporaryStat, List<Option>> newStats) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_SET_TEMPORARY_STAT);

        outPacket.encodeInt(chr.getId());

        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        tsm.encodeForRemote(outPacket, newStats);
        outPacket.encodeShort(0); // delay hardcored 0
        outPacket.encodeByte(tsm.hasStat(CharacterTemporaryStat.Larkness)
                || tsm.hasStat(CharacterTemporaryStat.TransformOverMan)
                || tsm.hasStat(CharacterTemporaryStat.BlessedHammerActive)
                || tsm.hasStat(CharacterTemporaryStat.IceAura));

        return outPacket;
    }

    public static OutPacket resetTemporaryStat(Char chr, EnumMap<CharacterTemporaryStat, List<Option>> removeStats) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_RESET_TEMPORARY_STAT);

        outPacket.encodeInt(chr.getId());

        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int[] mask = tsm.getMaskByCollection(removeStats);
        for (int maskElem : mask) {
            outPacket.encodeInt(maskElem);
        }
        if (removeStats.containsKey(CharacterTemporaryStat.IndieLoopEffect)
                || removeStats.containsKey(CharacterTemporaryStat.IndieNotDamaged)
                || removeStats.containsKey(CharacterTemporaryStat.IndieFlyAcc)
                || removeStats.containsKey(CharacterTemporaryStat.IndieSpecificSkill)
                || removeStats.containsKey(CharacterTemporaryStat.IndieBuffIcon)
                || removeStats.containsKey(CharacterTemporaryStat.IndieBarrier)
                || removeStats.containsKey(CharacterTemporaryStat.IndieForceSpeed)
                || removeStats.containsKey(CharacterTemporaryStat.IndieAllHitDamR)
                || removeStats.containsKey(CharacterTemporaryStat.IndieCharColor)) {
            tsm.encodeIndieTempStat(outPacket, removeStats);
        }
        int hasPoseType = 0;
        if (tsm.hasStat(CharacterTemporaryStat.PoseType)) {
            hasPoseType = tsm.getOption(CharacterTemporaryStat.PoseType).bOption;
        }
        outPacket.encodeByte(hasPoseType);

        boolean hasBattleSurvivalDefence = removeStats.containsKey(CharacterTemporaryStat.BattleSurvivalDefence);
        outPacket.encodeByte(hasBattleSurvivalDefence);

        boolean hasTwoStats = removeStats.containsKey(CharacterTemporaryStat.RideVehicle) || removeStats.containsKey(CharacterTemporaryStat.RideVehicleExpire);
        // if true, show a ride vehicle effect. Why should this be called on reset tho?
        outPacket.encodeByte(hasTwoStats);

        return outPacket;
    }

    public static OutPacket remoteSetActivePortableChair(int charID, PortableChair chair) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_SET_ACTIVE_PORTABLE_CHAIR);

        outPacket.encodeInt(charID);
        outPacket.encodeInt(chair.getItemID());
        outPacket.encodeByte(chair.getType() != ChairType.None);
        if (chair.getType() != ChairType.None) {
            chair.encode(outPacket);
        }

        return outPacket;
    }

    public static OutPacket skillPrepare(Char chr, int skillId, int slv) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_SKILL_PREPARE);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeInt(skillId);
        outPacket.encodeByte(slv);
        outPacket.encodeShort(0); // unknown
        outPacket.encodeByte(chr.getTotalStat(BaseStat.booster)); // action Speed
        outPacket.encodePosition(chr.getPosition());

        return outPacket;
    }

    public static OutPacket movingShootAttackPrepare(Char chr, int skillId, int slv, short mask, byte attackSpeed) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_MOVING_SHOOT_ATTACK_PREPARE);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeByte(slv);
        outPacket.encodeByte(skillId == 0); // skill or normal
        outPacket.encodeInt(skillId);
        outPacket.encodeShort(mask);
        outPacket.encodeByte(attackSpeed);

        return outPacket;
    }

    public static OutPacket skillCancel(int charID, int skillId) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_SKILL_CANCEL);

        outPacket.encodeInt(charID);

        outPacket.encodeInt(skillId);
        outPacket.encodeInt(skillId);

        return outPacket;
    }

    public static OutPacket gatherActionSet(Char chr, int itemID, int action) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_GATHER_ACTION_SET);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeByte(true);
        outPacket.encodeInt(itemID);
        outPacket.encodeInt(action);

        return outPacket;
    }

    public static OutPacket dragonGlide(Char chr, int glide) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_DRAGON_GLIDE);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeInt(0);
        outPacket.encodeInt(glide);

        return outPacket;
    }

    public static OutPacket dragonAction(Char chr, int attackAction, int skillId, int slv) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_DRAGON_ACTION);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeInt(attackAction);
        outPacket.encodeInt(skillId);
        outPacket.encodeInt(slv);

        return outPacket;
    }

    public static OutPacket dressUp(Char chr, byte isShowEff, int dressUpID, boolean isChanged) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_ANGELIC_BUSTER_DRESS_UP);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeByte(isShowEff);
        outPacket.encodeInt(dressUpID);
        outPacket.encodeByte(isChanged);

        return outPacket;
    }

    public static OutPacket dragonBreathEarthEffect(Char chr, int skillId, int slv, Position start, Position end) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_DRAGON_BREATH_EARTH_EFFECT);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeInt(skillId);
        outPacket.encodeInt(slv);
        outPacket.encodePositionInt(start);
        outPacket.encodePositionInt(end);

        return outPacket;
    }

    public static OutPacket kaiserColorOrMorphChange(Char chr, int RotateHueExtern, int RotateHueInnner, int PrimiumBlack) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_KAISER_COLOR_OR_MORPH_CHANGE);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeInt(RotateHueExtern);
        outPacket.encodeInt(RotateHueInnner);
        outPacket.encodeByte(PrimiumBlack);

        return outPacket;
    }

    public static OutPacket intrusion(int charID) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_INTRUSION);

        outPacket.encodeInt(charID);

        return outPacket;
    }

    public static OutPacket psychicEnergyShieldEffect(Char chr, boolean isToggle) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_KINESIS_PSYCHIC_ENERGY_SHIELD_EFFECT);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeByte(isToggle);

        return outPacket;
    }

    public static OutPacket releaseRWGrab(Char chr, int skillID, int mobID) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_RELEASE_RW_GRAB);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeInt(skillID);
        outPacket.encodeInt(mobID);

        return outPacket;
    }

    public static OutPacket requestRWMultiChargeCancel(Char chr, int skillID) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_RW_MULTI_CHARGE_CANCEL_REQUEST);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeByte(1);
        outPacket.encodeInt(skillID);

        return outPacket;
    }

    public static OutPacket requestRWMultiChargeCancel(Char chr, byte size, List<Integer> skills) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_RW_MULTI_CHARGE_CANCEL_REQUEST);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeByte(size);
        for (int i = 0; i < size; i++) {
            outPacket.encodeInt(skills.get(i));
        }

        return outPacket;
    }

    public static OutPacket stigmaDeliveryResponse(Char chr, int type, int charID) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOTE_STIGMA_DELIVERY_RESPONSE);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeInt(type); // 3: start; 4: end
        outPacket.encodeInt(charID);
        outPacket.encodeInt(0);

        return outPacket;
    }
}
