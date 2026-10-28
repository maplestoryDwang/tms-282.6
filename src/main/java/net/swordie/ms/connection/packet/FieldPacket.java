package net.swordie.ms.connection.packet;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.*;
import net.swordie.ms.client.character.runestones.RuneStone;
import net.swordie.ms.client.character.skills.ForceAtom;
import net.swordie.ms.client.character.skills.PsychicArea;
import net.swordie.ms.client.character.skills.info.ForceAtomInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.daily.DailyCoin;
import net.swordie.ms.client.jobs.adventurer.magician.FirePoison;
import net.swordie.ms.client.jobs.adventurer.warrior.Paladin;
import net.swordie.ms.client.trunk.TrunkDlg;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.constants.*;
import net.swordie.ms.enums.*;
import net.swordie.ms.handlers.PsychicLock;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.AffectedAreaSpeacial;
import net.swordie.ms.life.Wreckage;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.pet.Pet;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.loaders.containerclasses.MakingSkillRecipe;
import net.swordie.ms.util.*;
import net.swordie.ms.util.container.Triple;
import net.swordie.ms.world.field.ClockPacket;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.fieldeffect.FieldEffect;
import net.swordie.ms.world.field.obtacleatom.ObtacleAtomInfo;
import net.swordie.ms.world.field.obtacleatom.ObtacleInRowInfo;
import net.swordie.ms.world.field.obtacleatom.ObtacleRadianInfo;

import java.util.*;

public class FieldPacket {

    public static void quickslotInit(OutPacket outPacket, List<Integer> keys) {
        boolean encode = keys != null && !keys.isEmpty();
        outPacket.encodeByte(encode);
        if (encode) {
            for (int i = 0; i < GameConstants.QUICKSLOT_LENGTH; i++) {
                outPacket.encodeInt(i < keys.size() ? keys.get(i) : 0);
            }
        }
    }

    public static OutPacket funcKeyMappedManInit(Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.FUNC_KEY_MAPPED_MAN_INIT);

        for (int i = 0; i < 3; i++) {
            chr.getFuncKeyMapByPreset(i).encode(outPacket);
        }
        quickslotInit(outPacket, chr.getQuickslotKeys());

        return outPacket;
    }

    public static OutPacket petConsumeItemInit(int itemId) {
        OutPacket outPacket = new OutPacket(OutHeader.PET_CONSUME_ITEM_INIT);

        outPacket.encodeInt(itemId);

        return outPacket;
    }

    public static OutPacket petConsumeMPItem(int itemId) {
        OutPacket outPacket = new OutPacket(OutHeader.PET_CONSUME_MP_ITEM);

        outPacket.encodeInt(itemId);

        return outPacket;
    }

    public static OutPacket petConsumeCureItem(int itemId) {
        OutPacket outPacket = new OutPacket(OutHeader.PET_CONSUME_CURE_ITEM);

        outPacket.encodeInt(itemId);

        return outPacket;
    }

    public static OutPacket affectedAreaCreated(AffectedArea aa) {
        OutPacket outPacket = new OutPacket(OutHeader.AFFECTED_AREA_CREATED);

        outPacket.encodeInt(aa.getObjectId());
        outPacket.encodeByte(aa.getMobOrigin());
        if (aa.getMobOrigin() > 0) {
            outPacket.encodeInt(aa.getMob().getObjectId());
        } else {
            outPacket.encodeInt(aa.getCharID());
        }
        outPacket.encodeInt(aa.getSkillID());
        outPacket.encodeShort(aa.getSlv());
        outPacket.encodeShort(aa.getDelay());
        if (aa.getRect() != null) {
            aa.getRect().encode(outPacket);
        } else {
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
        }
        if (aa.getSkillID() == 162111000) { // Manifestation: Wind Swing
            if (aa.getRect() != null) {
                aa.getRect().encode(outPacket);
            } else {
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
            }
        }
        outPacket.encodeInt(aa.getElemAttr());
        outPacket.encodeInt(aa.getElemAttr()); // ?
        outPacket.encodePosition(aa.getPosition());
        outPacket.encodeInt(aa.getForce());
        outPacket.encodeInt(aa.getOption());
        outPacket.encodeByte(aa.getOption() != 0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(aa.getMobOrigin() > 0 ? aa.getMobLvl() : 0);
        if (SkillConstants.isFlipAffectedAreaSkill(aa.getSkillID())) {
            outPacket.encodeByte(aa.isFlip());
        }
        outPacket.encodeInt(aa.getDuration()); // duration
        outPacket.encodeInt(0); // v208
        outPacket.encodeInt(0); // new v214
        outPacket.encodeByte(aa.hasHitMob());
        outPacket.encodeByte(true);
        boolean bool = false;
        outPacket.encodeByte(bool); // ?
        if (bool) {
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            int size = 0;
            outPacket.encodeInt(size);
            for (int i = 0; i < size; i++) {
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
                outPacket.encodeString("");
            }
        }
        outPacket.encodeInt(aa.getAffectedAreaSpeacials() != null ? aa.getAffectedAreaSpeacials().size() : 0);
        if (aa.getAffectedAreaSpeacials() != null) {
            for (AffectedAreaSpeacial aas : aa.getAffectedAreaSpeacials()) {
                outPacket.encodeInt(aas.getOriginalSkillID());
                outPacket.encodeInt(aa.getSkillID());
                outPacket.encodeInt(aas.getValue());
                outPacket.encodePositionInt(aas.getPosition());
                outPacket.encodeRectInt(aas.getRect());
                outPacket.encodeInt(aas.getY());
                outPacket.encodeInt(aas.getU());
            }
        }
        if (aa.getSkillID() == 400001098) { // Vestige of Divinity
            outPacket.encodeInt(0);
        }
        if (aa.getSkillID() == 2321015) { // Holy Water
            outPacket.encodeInt(0);
        }

        return outPacket;
    }

    public static OutPacket affectedAreaInstallAreaFire(AffectedArea aa) {
        OutPacket outPacket = new OutPacket(OutHeader.INSTALLED_AREA_FIRE);

        outPacket.encodeInt(aa.getSkillID());
        outPacket.encodeInt(aa.getSlv());
        outPacket.encodeInt(1);
        outPacket.encodeInt(aa.getObjectId());

        return outPacket;
    }

    public static OutPacket affectedAreaRemoved(AffectedArea aa) {
        OutPacket outPacket = new OutPacket(OutHeader.AFFECTED_AREA_REMOVED);

        outPacket.encodeInt(aa.getObjectId());
        if (aa.getSkillID() == FirePoison.POISON_MIST || aa.getSkillID() == FirePoison.HEXA_POISON_MIST) {
            outPacket.encodeInt(aa.getLinkingSkillID());
            outPacket.encodeInt(0);
        } else {
            outPacket.encodeInt(1);
        }

        return outPacket;
    }

    public static OutPacket curNodeEventEnd(boolean enable) {
        OutPacket outPacket = new OutPacket(OutHeader.CUR_NODE_EVENT_END);

        outPacket.encodeByte(enable);

        return outPacket;
    }

    public static OutPacket createForceAtom(ForceAtom fa) {
        OutPacket outPacket = new OutPacket(OutHeader.CREATE_FORCE_ATOM);

        int forceAtomType = fa.getForceAtomEnum().getForceAtomType();
        outPacket.encodeByte(fa.isByMob());
        if (fa.isByMob()) {
            outPacket.encodeInt(fa.getUserOwner());
        }
        outPacket.encodeInt(fa.getCharId());
        outPacket.encodeInt(forceAtomType);
        if (forceAtomType == 36 || forceAtomType == 37 || forceAtomType == 89 || forceAtomType == 91) {
            outPacket.encodeInt(fa.getSkillId());
        }
        else if (forceAtomType != 0
                && forceAtomType != 9
                && forceAtomType != 14
                && forceAtomType != 29
                && forceAtomType != 35
                && forceAtomType != 42) {
            outPacket.encodeByte(fa.isToMob());
            switch (forceAtomType) {
                case 2:
                case 3:
                case 7:
                case 11:
                case 12:
                case 13:
                case 17:
                case 19:
                case 20:
                case 23:
                case 24:
                case 25:
                case 27:
                case 28:
                case 30:
                case 32:
                case 34:
                case 38:
                case 39:
                case 40:
                case 41:
                case 47:
                case 48:
                case 49:
                case 52:
                case 53:
                case 54:
                case 55:
                case 56:
                case 57:
                case 58:
                case 60:
                case 64:
                case 65:
                case 66:
                case 67:
                case 68:
                case 72:
                case 73:
                case 75:
                case 76:
                case 77:
                case 84:
                case 86:
                case 87:
                case 90:
                case 92:
                case 93:
                case 95:
                case 97:
                case 101:
                case 110:
                case 112:
                case 114:
                    outPacket.encodeInt(fa.getTargetIdList().size());
                    for (int i : fa.getTargetIdList()) {
                        outPacket.encodeInt(i);
                        if (forceAtomType == 114) {
                            outPacket.encodeRectInt(fa.getRect());
                        }
                    }
                    break;
                default:
                    outPacket.encodeInt(fa.getTargetIdList().getFirst());
                    if (forceAtomType == 62) {
                        for (int i : fa.getTargetIdList()) {
                            outPacket.encodeInt(i);
                        }
                    }
                    break;
            }
            outPacket.encodeInt(fa.getSkillId());
        }
        switch (forceAtomType) {
            case 29:
            case 42:
                outPacket.encodeInt(fa.getSkillId());
                if (fa.getSkillId() == 400021069) {
                    outPacket.encodeInt(0); // grimReaperIncGuage 200 thường 2000 boss
                }
                break;
        }
        for (ForceAtomInfo fai : fa.getFaiList()) {
            outPacket.encodeByte(true);
            fai.encode(outPacket);
        }
        outPacket.encodeByte(false);
        switch (forceAtomType) {
            case 11:
                outPacket.encodeRectInt(fa.getRect());
                outPacket.encodeInt(fa.getBulletId());
                break;
            case 9:
                outPacket.encodeRectInt(fa.getRect());
                break;
            case 15:
                outPacket.encodeRectInt(fa.getRect());
                outPacket.encodeByte(0);
                outPacket.encodeInt(fa.getUserOwner()); // Summon Obj Id
                break;
            case 29:
                outPacket.encodeRectInt(fa.getRect());
                outPacket.encodePositionInt(fa.getForcedTargetPosition());
                break;
            case 2:
            case 3:
            case 4:
            case 16:
            case 20:
            case 25:
            case 26:
            case 30:
            case 33:
            case 61:
            case 64:
            case 67:
            case 69:
            case 74:
            case 76:
            case 85:
            case 93:
            case 94:
            case 95:
            case 97:
            case 101:
            case 110:
            case 111:
            case 112:
            case 113:
                outPacket.encodePositionInt(fa.getForcedTargetPosition());
                break;
            case 17:
            case 77:
                outPacket.encodeInt(fa.getArriveDir());
                outPacket.encodeInt(fa.getArriveRange());
                break;
            case 18:
                outPacket.encodePositionInt(fa.getForcedTargetPosition());
            case 27:
                outPacket.encodeRectInt(fa.getRect());
                outPacket.encodeInt(0);
                break;
            case 28:
            case 34:
                outPacket.encodeRectInt(fa.getRect());
                outPacket.encodeInt(fa.getTime());
                break;
            case 57:
            case 58:
            case 86:
            case 87:
                outPacket.encodeRectInt(fa.getRect());
                outPacket.encodeInt(fa.getTime());
                outPacket.encodePositionInt(fa.getPosition());
                break;
            case 36:
            case 39:
            case 89:
                outPacket.encodeInt(4);
                outPacket.encodeInt(550);
                outPacket.encodeInt(0);
                outPacket.encodeRectInt(fa.getRect());
                if (forceAtomType == 36 || forceAtomType == 89) {
                    outPacket.encodeRectInt(fa.getRect2());
                    outPacket.encodeInt(0);
                }
                break;
            case 37:
            case 91:
                outPacket.encodeInt(0);
                outPacket.encodeRectInt(fa.getRect());
                outPacket.encodePositionInt(fa.getForcedTargetPosition());
                break;
            case 42:
                outPacket.encodeRectInt(fa.getRect());
                break;
            case 49:
                outPacket.encodeInt(fa.getBulletId());
                outPacket.encodeInt(fa.getUserOwner()); // Summon Obj Id
                outPacket.encodeRectInt(fa.getRect());
                break;
            case 50:
                outPacket.encodePositionInt(fa.getForcedTargetPosition());
                outPacket.encodeInt(0);
                break;
            case 7:
                outPacket.encodeRectInt(fa.getRect());
                break;
            default:
                outPacket.encodeInt(-1);
                break;
        }
        switch (fa.getSkillId()) {
            case 25100010:
            case 25120115:
            case 400011059:
                outPacket.encodeInt(0); // getnFoxSpiritSkillId
                break;
            case Paladin.MIGHTY_MJOLNIR: // 400011131
                outPacket.encodeInt(32768);
                outPacket.encodeByte(3);
                break;
            case 400041023:
                outPacket.encodeInt(0);
                outPacket.encodePositionInt(fa.getPosition());
                break;
        }

        return outPacket;
    }

    public static OutPacket createArkForceAtom(int chrId, int skillId, List<ForceAtom> forceAtoms) {
        OutPacket outPacket = new OutPacket(OutHeader.CREATE_ARK_FORCE_ATOM);

        outPacket.encodeInt(chrId);
        outPacket.encodeInt(skillId);

        outPacket.encodeInt(forceAtoms.size()); // size of the different force atoms Ark can get.

        for (ForceAtom forceAtom : forceAtoms) {
            outPacket.encodeInt(forceAtom.getForceAtomEnum().getForceAtomType());
            outPacket.encodeInt(forceAtom.getSkillId());
            outPacket.encodeInt(forceAtom.getTargetIdList().size());
            for (int i = 0; i < forceAtom.getTargetIdList().size(); i++) {
                outPacket.encodeInt(forceAtom.getTargetIdList().get(i));
                outPacket.encodeByte(forceAtom.getFaiList().size());
                forceAtom.getFaiList().get(i).encode(outPacket);
                outPacket.encodeByte(0);
            }
        }

        return outPacket;
    }

    public static OutPacket setAchieveRate(int achieveRate) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_ACHIEVE_RATE);

        outPacket.encodeInt(achieveRate);

        return outPacket;
    }

    public static OutPacket guideForceAtom(int chrId, int faKey, int mobId) {
        OutPacket outPacket = new OutPacket(OutHeader.GUIDE_FORCE_ATOM);

        outPacket.encodeInt(faKey);
        outPacket.encodeInt(chrId);
        outPacket.encodeInt(1);
        outPacket.encodeInt(1);
        outPacket.encodeInt(1);
        outPacket.encodeInt(mobId);

        return outPacket;
    }

    public static OutPacket finalAttackRequest(int wt, int skillID, int finalSkillID, int[] mobsHit) {
        OutPacket outPacket = new OutPacket(OutHeader.FINAL_ATTACK_REQUEST);

        outPacket.encodeInt(Util.getCurrentTime());
        outPacket.encodeInt(1); // success
        outPacket.encodeInt(skillID);
        outPacket.encodeInt(finalSkillID);
        outPacket.encodeInt(wt);
        outPacket.encodeInt(mobsHit.length);
        for (int i = 0; i < mobsHit.length; i++) {
            outPacket.encodeInt(mobsHit[i]);
        }

        return outPacket;
    }

    public static OutPacket createPsychicArea(int charID, PsychicArea pa) {
        OutPacket outPacket = new OutPacket(OutHeader.CREATE_PSYCHIC_AREA);

        outPacket.encodeInt(charID);

        outPacket.encodeByte(pa.success);
        if (pa.success) {
            outPacket.encodeInt(pa.action);
            outPacket.encodeInt(pa.actionSpeed);
            outPacket.encodeInt(pa.psychicAreaKey);
            outPacket.encodeInt(pa.skillID);
            outPacket.encodeShort(pa.slv);
            outPacket.encodeInt(pa.localPsychicAreaKey);
            outPacket.encodeInt(pa.duration);
            outPacket.encodeByte(pa.isLeft);
            outPacket.encodeShort(pa.skeletonFilePathIdx);
            outPacket.encodeShort(pa.skeletonAniIdx);
            outPacket.encodeShort(pa.skeletonLoop);
            outPacket.encodePositionInt(pa.start);
        } else {
            outPacket.encodeInt(0);
        }

        return outPacket;
    }

    public static OutPacket releasePsychicArea(int charID, int localAreaKey) {
        OutPacket outPacket = new OutPacket(OutHeader.RELEASE_PSYCHIC_AREA);

        outPacket.encodeInt(charID);
        outPacket.encodeInt(localAreaKey);

        return outPacket;
    }

    public static OutPacket createPsychicLock(int charID, boolean approved, PsychicLock pl) {
        OutPacket outPacket = new OutPacket(OutHeader.CREATE_PSYCHIC_LOCK);

        outPacket.encodeInt(charID);
        outPacket.encodeByte(approved);
        if (approved) {
            pl.encode(outPacket);
        }

        return outPacket;
    }

    public static OutPacket createPsychicLockAttack(int charID, int skillid, int subskillid, int unk, int id, byte a, int b, List<Integer> grab) {
        OutPacket outPacket = new OutPacket(OutHeader.CREATE_PSYCHIC_LOCK_ATTACK);

        outPacket.encodeInt(charID);
        outPacket.encodeInt(skillid);
        outPacket.encodeShort(unk);
        outPacket.encodeInt(id);
        outPacket.encodeInt(6);
        outPacket.encodeByte(a);
        outPacket.encodeInt(b);
        if (skillid == 142110003 || skillid == 142120001) {
            outPacket.encodeInt(subskillid);
            outPacket.encodeInt(unk);
        }
        outPacket.encodeInt(grab.size());
        for (Integer g : grab) {
            outPacket.encodeInt(g.intValue());
        }

        return outPacket;
    }

    public static OutPacket releasePsychicLock(int charID, int id) {
        OutPacket outPacket = new OutPacket(OutHeader.RELEASE_PSYCHIC_LOCK);

        outPacket.encodeInt(charID);
        outPacket.encodeInt(id);

        return outPacket;
    }

    public static OutPacket releasePsychicLockMob(int charID, List<Integer> ids) {
        OutPacket outPacket = new OutPacket(OutHeader.RELEASE_PSYCHIC_LOCK_MOB);

        outPacket.encodeInt(charID);
        for (int i : ids) {
            outPacket.encodeByte(1);
            outPacket.encodeInt(i);
        }
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket characterInfo(Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.CHARACTER_INFO_REMOTE);

        var cs = chr.getAvatarData().getCharacterStat();
        var tsm = chr.getTemporaryStatManager();
        var forcedStats = chr.getForcedStats();
        var guild = chr.getGuild();
        var alliance = guild != null ? guild.getAlliance() : null;
        var union = chr.getUnion();
        var unionBoard = union != null ? union.getBoardByPreset(union.getPresets()) : null;
        Int2IntOpenHashMap passiveskills = new Int2IntOpenHashMap();
        Int2IntOpenHashMap unionSkills = new Int2IntOpenHashMap();
        for (int i = 91000000; i <= 91001050; i++) {
            if (SkillData.getSkillInfoById(i) != null && chr.hasSkill(i)) {
                passiveskills.put(i, chr.getSkillLevel(i));
            }
        }
        for (int i = 71000000; i <= 71010000; i++) {
            if (SkillData.getSkillInfoById(i) != null && chr.hasSkill(i)) {
                unionSkills.put(i, chr.getSkillLevel(i));
            }
        }

        outPacket.encodeInt(0);
        outPacket.encodeInt(chr.getUser().getId());
        chr.encode(outPacket, DBChar.All);
        chr.encodeMatrixSkills(outPacket);

        outPacket.encodeInt(0); // Unk
        outPacket.encodeInt(0); // Unk
        outPacket.encodeInt(0); // Unk

        // CCharacterStat::DecodeForRemoteInfo
        outPacket.encodeByte(cs.getGender());
        outPacket.encodeShort(cs.getLevel());
        outPacket.encodeInt(chr.getJob()); // Job
        outPacket.encodeInt(cs.getStr());
        outPacket.encodeInt(cs.getDex());
        outPacket.encodeInt(cs.getInt());
        outPacket.encodeInt(cs.getLuk());
        outPacket.encodeInt(cs.getPop()); //Fame
        outPacket.encodeInt(chr.getMaxHP());
        outPacket.encodeInt(chr.getMaxMP());
        outPacket.encodeInt(chr.getMaxHP());
        outPacket.encodeInt(0); // 45?
        outPacket.encodeInt(0); // Unk
        outPacket.encodeInt(0); // 17
        outPacket.encodeInt(0); // 31
        outPacket.encodeInt(0); // 4
        outPacket.encodeInt(0); // 32
        outPacket.encodeInt(0); // Unk
        outPacket.encodeInt(0); // Unk
        outPacket.encodeLong(0); // 4607182418800017408
        outPacket.encodeLong(0); // 4607182418800017408
        outPacket.encodeInt(0); // Unk

        // ForcedStatSet
        int updateMask = 0;
        for (var entry : forcedStats.entrySet()) {
            updateMask |= entry.getKey().getVal();
        }
        outPacket.encodeInt(updateMask);
        for (var entry : forcedStats.entrySet()) {
            var stat = entry.getKey();
            var value = entry.getValue();
            switch (stat) {
                case speed:
                case jump:
                case speedMax:
                case optOff:
                case jumpMax:
                    outPacket.encodeByte((byte) value);
                    break;
                case Unk3:
                case addMHP:
                case speedDec:
                case Unk4:
                    outPacket.encodeInt((int) value);
                    break;
                default:
                    outPacket.encodeShort((short) value);
                    break;
            }
        }

        tsm.encodeForLocal(outPacket);

        outPacket.encodeByte(true); // 48 bytes
        outPacket.encodeArr("D5 0C 04 00 7B 48 77 03"); // 8 bytes
        outPacket.encodeInt(chr.getId());
        outPacket.encodeShort(chr.getPets().size());
        outPacket.encodeLong(0);
        outPacket.encodeString("", 13);
        outPacket.encodeString(chr.getName(), 13);

        for (Pet pet : chr.getPets()) {
            PetItem pi = pet.getItem();
            outPacket.encodeByte(1);
            outPacket.encodeInt(pet.getIdx());
            outPacket.encodeInt(pi.getBagIndex());
            pi.encode(outPacket);
        }
        outPacket.encodeByte(0); // end of pets

        outPacket.encodeArr(new byte[44]);

        for (int i = 0; i < 15; i++) {
            outPacket.encodeInt(0);
        }
        // F3 03 00 00 74 09 00 00 F0 03 00 00 F2 03 00 00 F3 03 00 00 66 09 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00

        for (int i = 0; i < 40; i++) {
            outPacket.encodeByte(-1);
        }
        // 28 2E 02 0A 2C 29 2B 09 2E 2C 2B 2A 29 28 0A 09 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF

        outPacket.encodeInt(0); // familiar Decode

        outPacket.encodeInt(0);

        outPacket.encodeString(alliance != null ? alliance.getName() : "");

        outPacket.encodeString(guild != null ? guild.getName() : "");

        outPacket.encodeInt(0); // Mulung floor clear => 84
        outPacket.encodeString(""); // "26/01/09" last clear?

        outPacket.encodeInt(unionBoard != null ? unionBoard.getUnionPower() : 0); // Legion
        outPacket.encodeLong(0); // sub_14032DF90 | 9F 04 00 00 4E 01 00 00 | 1A 05 00 00 8A 01 00 00
        outPacket.encodeByte(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeInt(0); // 5
        outPacket.encodeInt(0);
        outPacket.encodeByte(0);
        outPacket.encodeLong(chr.getCombatPower()); // combat Power

        outPacket.encodeInt(0); // sub_1410E79B0

        outPacket.encodeInt(chr.getActiveNickItemID());
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeByte(0);
        outPacket.encodeInt(0); // 60

        outPacket.encodeInt(passiveskills.size());
        for (var entry : passiveskills.int2IntEntrySet()) {
            outPacket.encodeInt(entry.getIntKey());
            outPacket.encodeInt(entry.getIntValue());
        }
        outPacket.encodeInt(unionSkills.size());
        for (var entry : unionSkills.int2IntEntrySet()) {
            outPacket.encodeInt(entry.getIntKey());
            outPacket.encodeInt(entry.getIntValue());
        }

        var size = 5;
        outPacket.encodeInt(size); // size
        for (int i = 0; i < size; i++) {
            outPacket.encodeInt(-1);
        }

        outPacket.encodeByte(true);
        outPacket.encodeByte(0); // true
        outPacket.encodeByte(0);
        outPacket.encodeByte(0); // true
        outPacket.encodeByte(0);
        outPacket.encodeInt(0);

        outPacket.encodeInt(0); // sub_145362520

        outPacket.encodeInt(0);
        outPacket.encodeByte(0);
        outPacket.encodeString("");
        outPacket.encodeInt(0); /// 5
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket hyperUpgradeDisplay(Equip equip,
                                                long costFinal,
                                                long costBeforeDiscount,
                                                long costBeforeMVP,
                                                long costBeforePC,
                                                int successChance, int destroyChance,
                                                boolean chanceTime, int nextTuc) {
        OutPacket outPacket = new OutPacket(OutHeader.EQUIPMENT_ENCHANT);

        outPacket.encodeByte(EquipmentEnchantType.HyperUpgradeDisplay.getVal());
        // HyperUpgradeDisplay::Decode
        outPacket.encodeLong(costFinal); // Meso Cost Final
        outPacket.encodeLong(costBeforeDiscount); // Meso Cost Before Discount
        outPacket.encodeLong(costBeforeMVP); // Meso Before MVP Discount
        outPacket.encodeLong(costBeforePC); // Meso Before Free Discount
        outPacket.encodeByte(costBeforeMVP != 0); // bMvp
        outPacket.encodeByte(costBeforePC != 0); // mvp over pc
        outPacket.encodeInt(successChance); // Success Chance
        outPacket.encodeInt(destroyChance); // Destroy Chance
        outPacket.encodeInt(0); // Success Change Before.
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeByte(chanceTime); // Chance Time
        outPacket.encodeInt(nextTuc);
        // HyperEnchantStat::Decode
        TreeMap<EnchantStat, Integer> vals = equip.getHyperUpgradeStats();
        int mask = 0;
        for (EnchantStat es : vals.keySet()) {
            mask |= es.getVal();
        }
        outPacket.encodeInt(mask);
        vals.forEach((es, val) -> outPacket.encodeInt(val));

        return outPacket;
    }

    public static OutPacket scrollTimerEffective() {
        OutPacket outPacket = new OutPacket(OutHeader.EQUIPMENT_ENCHANT);

        outPacket.encodeByte(EquipmentEnchantType.ScrollTimerEffective.getVal());
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket miniGameDisplay(EquipmentEnchantType eeType) {
        OutPacket outPacket = new OutPacket(OutHeader.EQUIPMENT_ENCHANT);

        outPacket.encodeByte(eeType.getVal());
        outPacket.encodeByte(0);
        outPacket.encodeInt(2000); // TODO nSeed

        return outPacket;
    }

    public static OutPacket showUpgradeResult(boolean succeed, boolean boom) {
        OutPacket outPacket = new OutPacket(OutHeader.EQUIPMENT_ENCHANT);

        outPacket.encodeByte(EquipmentEnchantType.ShowHyperUpgradeResult.getVal());
        outPacket.encodeInt(boom ? 2 : succeed ? 1 : 3);
        outPacket.encodeInt(0);
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket showUnknownEnchantFailResult(byte msg) {
        OutPacket outPacket = new OutPacket(OutHeader.EQUIPMENT_ENCHANT);

        outPacket.encodeByte(EquipmentEnchantType.ShowUnknownFailResult.getVal());
        outPacket.encodeByte(msg);
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket scrollUpgradeDisplay(boolean feverTime, List<ScrollUpgradeInfo> infos) {
        OutPacket outPacket = new OutPacket(OutHeader.EQUIPMENT_ENCHANT);

        outPacket.encodeByte(EquipmentEnchantType.ScrollUpgradeDisplay.getVal());
        outPacket.encodeByte(feverTime);

        int index = 0;
        outPacket.encodeByte(infos.size());
        for (ScrollUpgradeInfo sui : infos) {
            outPacket.encodeInt(2);
            outPacket.encodeInt(71);
            outPacket.encodeInt(index);
            outPacket.encodeInt(sui.getIconID());
            outPacket.encodeString(sui.getTitle());
            outPacket.encodeInt(sui.getType().ordinal()); // 0
            outPacket.encodeInt(sui.getOption()); // 0
            outPacket.encodeInt(-1);
            outPacket.encodeInt(sui.getMask());
            for (Map.Entry<EnchantStat, Integer> entry : sui.getStats().entrySet()) {
                outPacket.encodeInt(entry.getValue());
            }
            outPacket.encodeInt(sui.getChance());
            outPacket.encodeInt(sui.getOldCost());
            outPacket.encodeInt(sui.getCost());
            outPacket.encodeByte(sui.getType().ordinal()); // inno
            outPacket.encodeInt(1); // ?
            outPacket.encodeInt(4); // bonus +4 % success
            outPacket.encodeInt(40); // ?
            index++;
        }

        return outPacket;
    }

    public static OutPacket showScrollUpgradeResult(boolean feverAfter, int result, String desc, Equip prevEquip,
                                                    Equip newEquip) {
        OutPacket outPacket = new OutPacket(OutHeader.EQUIPMENT_ENCHANT);

        outPacket.encodeByte(EquipmentEnchantType.ShowScrollUpgradeResult.getVal());

        outPacket.encodeByte(feverAfter);
        outPacket.encodeInt(result);
        outPacket.encodeString(desc);
        outPacket.encode(prevEquip);
        outPacket.encode(newEquip);

        return outPacket;
    }

    public static OutPacket showChaosScrollUpgradeResult(int pos, int scrollID, TreeMap<EnchantStat, Integer> vals, boolean success, boolean boom, boolean tucProtect) {
        OutPacket outPacket = new OutPacket(OutHeader.EQUIPMENT_ENCHANT);

        outPacket.encodeByte(EquipmentEnchantType.ShowChaosScrollUpgradeResult.getVal());

        outPacket.encodeInt(pos);
        outPacket.encodeInt(scrollID);
        int mask = 0;
        for (EnchantStat es : vals.keySet()) {
            mask |= es.getVal();
        }
        outPacket.encodeInt(mask);
        vals.forEach((es, val) -> outPacket.encodeInt(val));
        outPacket.encodeByte(success);
        outPacket.encodeByte(false);
        outPacket.encodeByte(tucProtect);

        return outPacket;
    }

    public static OutPacket showScrollVestigeCompensationResult(boolean refresh) {
        OutPacket outPacket = new OutPacket(OutHeader.EQUIPMENT_ENCHANT);

        outPacket.encodeByte(EquipmentEnchantType.ShowScrollVestigeCompensationResult.getVal());
        outPacket.encodeByte(refresh);

        return outPacket;
    }

    public static OutPacket showTranmissionResult(Equip fromEq, Equip toEq) {
        OutPacket outPacket = new OutPacket(OutHeader.EQUIPMENT_ENCHANT);

        outPacket.encodeByte(EquipmentEnchantType.ShowTransmissionResult.getVal());
        fromEq.encode(outPacket);
        toEq.encode(outPacket);

        return outPacket;
    }

    public static OutPacket violetCubeResult(int mode, List<Integer> Potentials) {
        OutPacket outPacket = new OutPacket(OutHeader.VIOLET_CUBE_RESULT);

        outPacket.encodeInt(mode);
        outPacket.encodeInt(0);
        // Mode 0 is sent when the cube is used from USE_CASH_ITEM
        // Mode 2 is sent when the potentials are applied
        switch (mode) {
            case 0:
            case 1: { // Sent by using Violet Cube for the first time
                outPacket.encodeInt(Potentials.size() == 6 ? 3 : 2); //Potential Lines to select acording to the existing potential lines
                outPacket.encodeInt(Potentials.size());
                for (int i = 0; i < Potentials.size(); i++) {
                    outPacket.encodeInt(Potentials.get(i)); //PotentialID
                }
            }
            case 2: { // Potential Chosen by the user
                outPacket.encodeLong(0); //Violet Cube Unique Id? Not sure but is not needed.
                outPacket.encodeInt(Potentials.size());
                for (int i = 0; i < Potentials.size(); i++) {
                    outPacket.encodeInt(Potentials.get(i));
                }
            }
        }

        return outPacket;
    }

    public static OutPacket redCubeResult(int charID, boolean upgrade, int cubeID, int ePos, Equip equip, int cubeCount) {
        OutPacket outPacket = new OutPacket(OutHeader.RED_CUBE_RESULT);

        outPacket.encodeInt(charID);
        outPacket.encodeByte(upgrade);
        outPacket.encodeInt(cubeID);
        outPacket.encodeInt(ePos);
        outPacket.encodeInt(cubeCount);
        equip.encode(outPacket);

        return outPacket;
    }

    public static OutPacket bonusCubeResult(int charID, boolean upgrade, int cubeID, int ePos, Equip equip, int cubeCount) {
        OutPacket outPacket = new OutPacket(OutHeader.ADDITIONAL_CUBE_RESULT);

        outPacket.encodeInt(charID);
        outPacket.encodeByte(upgrade);
        outPacket.encodeInt(cubeID);
        outPacket.encodeInt(ePos);
        outPacket.encodeInt(cubeCount);
        equip.encode(outPacket);

        return outPacket;
    }

    public static OutPacket inGameCubeResult(int charID, boolean upgrade, int cubeID, int ePos, Equip equip, int cubeCount) {
        OutPacket outPacket = new OutPacket(OutHeader.IN_GAME_CUBE_RESULT); // This is Type Cube dont need UI show result

        outPacket.encodeInt(charID);
        outPacket.encodeByte(upgrade);
        outPacket.encodeInt(cubeID);
        outPacket.encodeInt(ePos);
        outPacket.encodeInt(cubeCount);
        equip.encode(outPacket);

        return outPacket;
    }

    public static OutPacket sitResult(int chrId, int fieldSeatId) {
        OutPacket outPacket = new OutPacket(OutHeader.SIT_RESULT);

        outPacket.encodeInt(chrId);
        if (fieldSeatId == -1) {
            outPacket.encodeByte(0);
        } else {
            outPacket.encodeByte(1);
            outPacket.encodeShort(fieldSeatId);
        }

        return outPacket;
    }

    public static OutPacket setActiveEmotionItem(Char chr, int itemID) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_ACTIVE_EMOTION_ITEM);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeInt(itemID);
        outPacket.encodePosition(chr.getPosition());

        return outPacket;
    }

    public static OutPacket setTamingMobInfo(int charID, int tamingMobLevel, int tamingMobExp, int tamingMobFatigue, boolean showEffect) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_TAMING_MOB_INFO);

        outPacket.encodeInt(charID);
        outPacket.encodeInt(tamingMobLevel);
        outPacket.encodeInt(tamingMobExp);
        outPacket.encodeInt(tamingMobFatigue);
        outPacket.encodeByte(showEffect);

        return outPacket;
    }

    public static OutPacket questClear(int qrKey) {
        OutPacket outPacket = new OutPacket(OutHeader.QUEST_CLEAR);

        outPacket.encodeInt(qrKey);

        return outPacket;
    }

    public static OutPacket setQuestClear() {
        return new OutPacket(OutHeader.SET_QUEST_CLEAR);
    }

    public static OutPacket setQuestTime(List<Triple<Integer, FileTime, FileTime>> questTimes) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_QUEST_TIME);

        outPacket.encodeByte(questTimes.size());
        for (Triple<Integer, FileTime, FileTime> times : questTimes) {
            outPacket.encodeInt(times.getLeft());
            outPacket.encodeFT(times.getMiddle());
            outPacket.encodeFT(times.getRight());
        }

        return outPacket;
    }

    public static OutPacket addWreckage(Wreckage wreckage, int wreckageCount) {
        OutPacket outPacket = new OutPacket(OutHeader.ADD_WRECKAGE);

        outPacket.encodeInt(wreckage.getOwnerId());  // owner Id
        outPacket.encodePositionInt(wreckage.getPosition());
        outPacket.encodeInt(wreckage.getDuration());  // duration
        outPacket.encodeInt(wreckage.getObjectId());  //evanWreckage.nIDx
        outPacket.encodeInt(wreckage.getSkillId());  //nSkillID
        outPacket.encodeInt(wreckage.getType());  //nType

        outPacket.encodeInt(wreckageCount);  //Number on Skill Icon, # of Wreckages on map

        return outPacket;
    }

    public static OutPacket delWreckage(Char chr, List<Wreckage> wreckageList) {
        OutPacket outPacket = new OutPacket(OutHeader.DEL_WRECKAGE);

        outPacket.encodeInt(chr.getId()); // Owner Id
        outPacket.encodeInt(wreckageList.size()); //Count
        outPacket.encodeByte(true); //Unk Boolean
        for (Wreckage wreckage : wreckageList) {
            outPacket.encodeInt(wreckage.getObjectId()); // Wreckage Id
        }

        return outPacket;
    }

    public static OutPacket whisper(Char from, byte channelIdx, String msg, boolean notFound, Item item) {
        OutPacket outPacket = new OutPacket(OutHeader.WHISPER);

        if (notFound) {
            outPacket.encodeByte(9);
            outPacket.encodeString(from.getName());
            outPacket.encodeByte(2);
            outPacket.encodeInt(channelIdx);
        } else {
            outPacket.encodeByte(18);
            outPacket.encodeInt(0);
            outPacket.encodeString(from.getName());
            outPacket.encodeInt(from.getId());
            outPacket.encodeByte(channelIdx); // channel
            outPacket.encodeByte(0); // ?
            outPacket.encodeInt(from.getWorld().getWorldId()); // ?
            outPacket.encodeString(msg);
            from.encodeChatInfo(outPacket, msg);
            outPacket.encodeInt(item != null ? 1 : 0);
            if (item != null) {
                outPacket.encodeByte(true);
                item.encode(outPacket);
                outPacket.encodeString(StringData.getItemStringById(item.getItemId()));
            }
            outPacket.encodeByte(0);
        }

        return outPacket;
    }

    public static OutPacket teleport(Position position, Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.TELEPORT);

        outPacket.encodeByte(true);
        outPacket.encodeByte(6); // nUserCallingType
        outPacket.encodeInt(chr.getId());
        outPacket.encodePosition(position);

        return outPacket;
    }

    public static OutPacket fieldEffect(FieldEffect fieldEffect) {
        OutPacket outPacket = new OutPacket(OutHeader.FIELD_EFFECT);

        fieldEffect.encode(outPacket);

        return outPacket;
    }

    public static OutPacket removeBlowWeather() {
        return blowWeather(0, null, 0, null);
    }

    public static OutPacket blowWeather(int itemID, String message, int seconds, byte[] packedAvatarLook) {
        OutPacket outPacket = new OutPacket(OutHeader.BLOW_WEATHER);

        outPacket.encodeInt(itemID);
        if (itemID > 0) {
            outPacket.encodeString(message);
            outPacket.encodeInt(seconds); // seconds
            outPacket.encodeByte(packedAvatarLook != null);// boolean if true send PackedCharacterLook
            if (packedAvatarLook != null) {
                outPacket.encodeArr(packedAvatarLook);
            }
        }

        return outPacket;
    }

    public static OutPacket playJukeBox(int itemID, String message) {
        OutPacket outPacket = new OutPacket(OutHeader.PLAY_JUKE_BOX);

        outPacket.encodeInt(itemID);
        if (itemID > 0) {
            outPacket.encodeString(message);
        }
        return outPacket;
    }

    /** Built by the active profile's {@link net.swordie.ms.connection.protocol.codec.TrunkPacketEncoder} (GMS 265: {@link net.swordie.ms.connection.protocol.codec.Gms265TrunkPackets}). */
    public static OutPacket trunkDlg(TrunkDlg trunkDlg) {
        return net.swordie.ms.connection.protocol.codec.TrunkPacketEncoder.active().trunkDlg(trunkDlg);
    }

    public static OutPacket tryMigrateCS() {
        return new OutPacket(OutHeader.TRY_MIGRATE_CASH_SHOP);
    }

    public static OutPacket openUI(UIType uiType) {
        return openUI(uiType.getVal());
    }

    public static OutPacket openUI(int uiID) {
        OutPacket outpacket = new OutPacket(OutHeader.OPEN_UI);

        outpacket.encodeInt(uiID);

        return outpacket;
    }

    public static OutPacket closeUI(UIType uiType) {
        return closeUI(uiType.getVal());
    }

    public static OutPacket closeUI(int uiID) {
        OutPacket outpacket = new OutPacket(OutHeader.CLOSE_UI);

        outpacket.encodeInt(uiID);

        return outpacket;
    }

    public static OutPacket openUIWithOption(int uiID, int option, int[] minigameOptions) {
        OutPacket outPacket = new OutPacket(OutHeader.OPEN_UI_WITH_OPTION);

        outPacket.encodeInt(uiID);
        outPacket.encodeInt(option);
        outPacket.encodeInt(minigameOptions.length);
        for (int i = 0; i < minigameOptions.length; i++) {
            outPacket.encodeInt(minigameOptions[i]);
        }

        return outPacket;
    }

    public static OutPacket openUIWithFocus(int uiID, int option, int unk) {
        OutPacket outPacket = new OutPacket(OutHeader.OPEN_UI_WITH_FOCUS);

        outPacket.encodeInt(uiID);
        outPacket.encodeInt(option);
        outPacket.encodeInt(unk);

        return outPacket;
    }

    public static OutPacket socketCreateResult(boolean success) {
        OutPacket outPacket = new OutPacket(OutHeader.SOCKET_CREATE_RESULT);

        outPacket.encodeByte(success ? 2 : 3);

        return outPacket;
    }

    public static OutPacket smartMobNotice(int templateID, int unk0, int unk1, int unk2, String txt) {
        OutPacket outPacket = new OutPacket(OutHeader.SMART_MOB_NOTICE);

        outPacket.encodeInt(unk0);
        outPacket.encodeInt(templateID);
        outPacket.encodeInt(unk1);
        outPacket.encodeInt(unk2);
        outPacket.encodeString(txt);

        return outPacket;
    }

    public static OutPacket changePhase(Mob mob, boolean isBalrog) {
        OutPacket outPacket = new OutPacket(OutHeader.CHANGE_PHASE);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeInt(mob.getPhase());
        outPacket.encodeByte(isBalrog);

        return outPacket;
    }

    public static OutPacket changeMobZone(int mobID, int dataType) {
        OutPacket outPacket = new OutPacket(OutHeader.CHANGE_MOB_ZONE);

        outPacket.encodeInt(mobID);
        outPacket.encodeInt(dataType);

        return outPacket;
    }

    public static OutPacket createObtacle(ObtacleAtomCreateType oact, ObtacleInRowInfo oiri, ObtacleRadianInfo ori,
                                          Set<ObtacleAtomInfo> atomInfos) {
        OutPacket outPacket = new OutPacket(OutHeader.CREATE_OBSTACLE);

        outPacket.encodeInt(0); // ? gets used in 1 function, which forwards it to another, which does nothing with it
        outPacket.encodeInt(atomInfos.size());
        outPacket.encodeByte(oact.getVal());
        if (oact == ObtacleAtomCreateType.IN_ROW) {
            oiri.encode(outPacket);
        } else if (oact == ObtacleAtomCreateType.RADIAL) {
            ori.encode(outPacket);
        }
        for (ObtacleAtomInfo atomInfo : atomInfos) {
            outPacket.encodeByte(true); // false -> no encode
            atomInfo.encode(outPacket);
            if (oact == ObtacleAtomCreateType.DIAGONAL) {
                atomInfo.getObtacleDiagonalInfo().encode(outPacket);
            }
        }

        return outPacket;
    }

    public static OutPacket clearObtacle() {
        return new OutPacket(OutHeader.CLEAR_OBSTACLE);
    }

    public static OutPacket runeStoneAppear(RuneStone runeStone) {
        OutPacket outPacket = new OutPacket(OutHeader.RUNE_STONE_APPEAR);

        outPacket.encodeInt(0);
        outPacket.encodeInt(runeStone.getEventType().ordinal());
        outPacket.encodeInt(0);
        outPacket.encodeInt(runeStone.getRuneType().getVal()); // Rune Type
        outPacket.encodePositionInt(runeStone.getPosition());
        outPacket.encodeByte(runeStone.isFlip());

        return outPacket;
    }

    public static OutPacket runeStoneUseAck(int type, int time, byte[] action) {
        OutPacket outPacket = new OutPacket(OutHeader.RUNE_STONE_USE_ACK);

        outPacket.encodeInt(type);
        if (type == 9) {
            outPacket.encodeByte(0);
            outPacket.encodeByte(1);
            outPacket.encodeByte(0);
            outPacket.encodeArr(action);
        } else {
            outPacket.encodeInt(time);
        }

        return outPacket;
    }

    public static OutPacket runeStoneDisappear(int charID) { //RuneStone is Used
        OutPacket outPacket = new OutPacket(OutHeader.RUNE_STONE_DISAPPEAR);

        outPacket.encodeInt(0); // Has to be 0
        outPacket.encodeInt(charID);
        outPacket.encodeInt(0); // EXP%?
        outPacket.encodeByte(false); // new 188
        outPacket.encodeByte(1); // new

        return outPacket;
    }

    public static OutPacket runeActSuccess(RuneType runeType) {
        OutPacket outPacket = new OutPacket(OutHeader.RUNE_ACT_SUCCESS);

        outPacket.encodeInt(runeType.getVal());

        return outPacket;
    }

    public static OutPacket runeStoneSkillAck(RuneType runeType) {
        OutPacket outPacket = new OutPacket(OutHeader.RUNE_STONE_SKILL_ACK);

        outPacket.encodeInt(runeType.getVal());

        return outPacket;
    }

    public static OutPacket runeStoneClearAndAllRegister() {
        OutPacket outPacket = new OutPacket(OutHeader.RUNE_STONE_CLEAR_AND_ALL_REGISTER);
        int count = 0;
        outPacket.encodeInt(count); // count
        outPacket.encodeInt(1); // new
        for (int i = 0; i < count; i++) {
            outPacket.encodeInt(0); // not sure, but whatever
        }

        return outPacket;
    }

    /**
     * Creates a Clock on a Field.
     *
     * @param clockPacket the clock to display
     * @return packet for the client
     */
    public static OutPacket clock(ClockPacket clockPacket) {
        OutPacket outPacket = new OutPacket(OutHeader.CLOCK);

        clockPacket.encode(outPacket);

        return outPacket;
    }

    public static OutPacket destroy() {
        return new OutPacket(OutHeader.DESTROY_CLOCK);
    }

    /**
     * Creates a packet for changing the elite state of a field.
     *
     * @param eliteState             The new elite state
     * @param notShowPopup           whether or not the popup should show up
     *                               (warning message for boss spawn, countdown
     *                               for bonus)
     * @param bgm                    The new bgm if the state is ELITE_BOSS
     * @param propSpecialEliteEffect special elite effect
     * @param backUOL                back uol
     * @return packet for the client
     */
    public static OutPacket eliteState(EliteState eliteState, boolean notShowPopup, String bgm, String propSpecialEliteEffect,
                                       String backUOL) {
        OutPacket outPacket = new OutPacket(OutHeader.ELITE_STATE);

        outPacket.encodeInt(eliteState.getVal()); // elite state
        outPacket.encodeInt(notShowPopup ? 1 : 0); // ?
        outPacket.encodeInt(0); // new v20X (character id)
        switch (eliteState) {
            case BonusStage:
            case BonusStage2:
                boolean custom = bgm != null || backUOL != null || propSpecialEliteEffect != null;
                outPacket.encodeByte(custom); // Bgm36.img/HappyTimeShort | Map/Map/Map9/924050000.img/back | Effect/EliteMobEff.img/eliteBonusStage
                if (custom) {
                    outPacket.encodeString(bgm); //sBgm Sound/%s
                    outPacket.encodeString(backUOL); //sBackUOL Map/Map/Map9/924050000.img/back
                    outPacket.encodeString(propSpecialEliteEffect); //sSpecialEffect Effect/EliteMobEff.img/eliteBonusStage
                    outPacket.encodeString("");
                    outPacket.encodeString("");
                    outPacket.encodeByte(0);
                }
                break;
            case EliteBoss:
            case EliteBoss2:
                outPacket.encodeString(bgm); //sBgm Sound/%s
                outPacket.encodeString(backUOL); //sFrame Effect/EliteMobEff.img/eliteMonsterFrame
                outPacket.encodeString(propSpecialEliteEffect); //sFrameEffect Effect/EliteMobEff.img/eliteMonsterEffect
                break;
            case Unk6:
                outPacket.encodeString("");
                break;
        }

        return outPacket;
    }

    public static OutPacket inviteGroupChair(int type, int invitedUserID) {
        OutPacket outPacket = new OutPacket(OutHeader.INVITE_GROUP_CHAIR);

        // 3: That player is already sitting on your Group Chair.
        // 4: You are too close to another group Chair.
        // 7: There are no remaining seats on the Group Chair.
        // 8: Unable to find Group chair.
        // 9: You invited the player to sit on your Group Chair.
        // 10: Player not found.
        // 11: The Group Chair you were invited to was not found.
        // 12: That player is already sitting.
        // 13: The Group Chair invitation was declined.
        outPacket.encodeInt(type);
        if (type == 6) {
            outPacket.encodeInt(invitedUserID);
        }

        return outPacket;
    }

    public static OutPacket practiceMode(boolean enable) {
        OutPacket outPacket = new OutPacket(OutHeader.PRACTICE_MODE);

        outPacket.encodeByte(enable);

        return outPacket;
    }

    public static OutPacket setQuickMoveInfo(boolean enable, List<QuickMoveInfo> quickMoveInfos) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_QUICK_MOVE_INFO);

        outPacket.encodeShort(enable ? 1 : 0);
        outPacket.encodeByte(quickMoveInfos.size());
        quickMoveInfos.forEach(qmi -> qmi.encode(outPacket));

        return outPacket;
    }

    public static OutPacket groupMessage(GroupMessageType gmt, Char from, String msg) {
        OutPacket outPacket = new OutPacket(OutHeader.GROUP_MESSAGE); // same 265 bytes; the header lets TMS 282 translate it

        outPacket.encodeByte(gmt.ordinal());

        outPacket.encodeInt(from.getUser().getId());
        outPacket.encodeInt(from.getId());
        outPacket.encodeString(from.getName());
        outPacket.encodeString(msg);
        from.encodeChatInfo(outPacket, msg);

        return outPacket;
    }

    public static OutPacket itemLinkedGroupMessage(GroupMessageType gmt, Char from, String msg, Item item) {

        OutPacket outPacket = new OutPacket(OutHeader.GROUP_MESSAGE); // same 265 bytes; the header lets TMS 282 translate it

        outPacket.encodeByte(gmt.ordinal());

        outPacket.encodeInt(from.getUser().getId());
        outPacket.encodeInt(from.getId());
        outPacket.encodeString(from.getName());
        outPacket.encodeString(msg);
        from.encodeChatInfo(outPacket, msg);
        outPacket.encodeByte(item != null);
        if (item != null) {
            outPacket.encode(item);
        }


        return outPacket;
    }

    public static OutPacket createMirrorImage(Position position, int alpha, int red, int green, int blue, boolean left) {
        OutPacket outPacket = new OutPacket(OutHeader.CREATE_MIRROR_IMAGE);

        outPacket.encodePositionInt(position);
        outPacket.encodeInt(alpha); // nAlpha   out of 1,000 (?)
        outPacket.encodeInt(red); // R  out of 100,000 (?)
        outPacket.encodeInt(green); // G  out of 100,000 (?)
        outPacket.encodeInt(blue); // B  out of 100,000 (?)
        outPacket.encodeInt(left ? 1 : 0); // bLeft

        return outPacket;
    }

    public static OutPacket setOneTimeAction(int charID, int action, int duration) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_ONE_TIME_ACTION);

        outPacket.encodeInt(charID);

        outPacket.encodeInt(action);
        outPacket.encodeInt(duration);

        return outPacket;
    }

    public static OutPacket makingSkillResult(int charID, int recipeCode, MakingSkillResult result, MakingSkillRecipe.TargetElem target, int incSkillProficiency) {
        OutPacket outPacket = new OutPacket(OutHeader.MAKING_SKILL_RESULT);

        outPacket.encodeInt(charID);

        outPacket.encodeInt(0); // 0 hoac 2
        outPacket.encodeInt(recipeCode);
        outPacket.encodeInt(result.getVal());
        if (result == MakingSkillResult.SUCESS_SOSO || result == MakingSkillResult.SUCESS_GOOD || result == MakingSkillResult.SUCESS_COOL) {
            outPacket.encodeByte(true);
            outPacket.encodeInt(target.getItemID());
            outPacket.encodeInt(target.getCount());
        }
        outPacket.encodeInt(incSkillProficiency);

        return outPacket;
    }

    public static OutPacket setMakingMeisterSkillEff(int charID, int recipeCode, int time) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_MAKING_MEISTER_SKILL_EFF);

        outPacket.encodeInt(charID);
        outPacket.encodeInt(recipeCode);
        outPacket.encodeInt(time);

        return outPacket;
    }

    public static OutPacket playSound(String dir) {
        OutPacket outPacket = new OutPacket(OutHeader.PLAY_SOUND);

        outPacket.encodeString(dir);

        return outPacket;
    }

    public static OutPacket golluxOpenPortal(String action, int show) {
        OutPacket outPacket = new OutPacket(OutHeader.GOLLUX_PORTAL_OPEN);

        outPacket.encodeString(action);
        outPacket.encodeInt(show);

        return outPacket;
    }

    public static OutPacket golluxUpdateMiniMap(Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.GOLLUX_MINIMAP);

        Map<String, Object> golluxMaps = chr.getOrCreateFieldByCurrentInstanceType(BossConstants.GOLLUX_FIRST_MAP).getProperties();
        outPacket.encodeInt(golluxMaps.size());
        for (Map.Entry<String, Object> entry : golluxMaps.entrySet()) {
            outPacket.encodeString(entry.getKey());
            outPacket.encodeString(String.valueOf(entry.getValue()));
        }

        return outPacket;
    }

    public static OutPacket createFallingCatcher(String name, int index, int count, List<Position> positions) {
        OutPacket outPacket = new OutPacket(OutHeader.CREATE_FALLING_CATCHER);

        outPacket.encodeString(name);
        outPacket.encodeInt(index);

        outPacket.encodeInt(count);

        for (int i = 0; i < count; i++) {
            outPacket.encodePositionInt(positions.get(i));
        }

        return outPacket;
    }

    public static OutPacket createFallingCatcherGollux(int mobId, Position position) {
        ArrayList<Position> pos = new ArrayList<Position>();
        pos.add(position);
        switch (mobId) {
            case 9390610:
                return createFallingCatcher("palmAttackGiantBossL", 50, 1, pos);
            case 9390611:
                return createFallingCatcher("palmAttackGiantBossR", 50, 1, pos);
            default:
                Random ran = new Random();
                int x = ran.nextInt(3) + 1;
                return createFallingCatcher("DropStoneGiantBoss" + x, 25, 1, pos);
        }

    }

    public static OutPacket chaseEffectSet(Char chr, Mob mob) {
        OutPacket outPacket = new OutPacket(OutHeader.CHASE_EFFECT_SET);

        outPacket.encodeInt(chr.getId());
        outPacket.encodeShort(1);
        outPacket.encodeInt(mob.getObjectId());

        return outPacket;
    }

    public static OutPacket setObjectState(String objName, int curState) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_OBJECT_STATE);

        outPacket.encodeString(objName);
        outPacket.encodeInt(curState);

        return outPacket;
    }

    public static OutPacket dynamicObjMove(String objName, Position pos) {
        OutPacket outPacket = new OutPacket(OutHeader.FIELD_DYNAMIC_OBJ_MOVE);

        outPacket.encodeString(objName);
        outPacket.encodePositionInt(pos);

        return outPacket;
    }

    public static OutPacket dynamicObjVisible(String objName, int curState) {
        OutPacket outPacket = new OutPacket(OutHeader.FIELD_DYNAMIC_OBJ_VISIBLE);

        outPacket.encodeString(objName);
        outPacket.encodeInt(curState);
        outPacket.encodeInt(curState);
        outPacket.encodeByte(0);
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket syncDynamicFootHold(String footholdName, boolean show, Position pos) {
        OutPacket outPacket = new OutPacket(OutHeader.FIELD_SYNC_DYNAMIC_FOOTHOLD);

        int loopSize = 1;
        outPacket.encodeInt(loopSize);
        for (int i = 0; i < loopSize; i++) {
            outPacket.encodeString(footholdName);
            outPacket.encodeByte(0);
            outPacket.encodeInt(show ? 1 : 0);
            outPacket.encodePositionInt(pos);
        }

        return outPacket;
    }

    public static OutPacket syncDynamicFootHold(List<Field.DynamicFootHood> dynamicFootHoods) {
        OutPacket outPacket = new OutPacket(OutHeader.FIELD_SYNC_DYNAMIC_FOOTHOLD);

        outPacket.encodeInt(dynamicFootHoods.size());
        for (Field.DynamicFootHood dynamicFootHood : dynamicFootHoods) {
            outPacket.encodeString(dynamicFootHood.name);
            outPacket.encodeByte(dynamicFootHood.unk);
            outPacket.encodeInt(dynamicFootHood.visible);
            outPacket.encodePositionInt(dynamicFootHood.pos);
        }

        return outPacket;
    }

    public static OutPacket stalkResult(List<Char> stalkers, boolean isRemoval) {
        OutPacket outPacket = new OutPacket(OutHeader.STALK_RESULT);

        outPacket.encodeInt(stalkers.size());
        for (Char stalkee : stalkers) {
            outPacket.encodeInt(stalkee.getId());
            outPacket.encodeByte(isRemoval);
            if (!isRemoval) {
                outPacket.encodeString(stalkee.getName());
                outPacket.encodePositionInt(stalkee.getPosition());
            }
        }

        return outPacket;
    }

    public static OutPacket momentSwimAreaSetWaterLevel(int waterLevel, byte maxLevel, int time) {
        OutPacket outPacket = new OutPacket(OutHeader.MOMENT_SWIM_AREA_SET_WATER_LEVEL);

        outPacket.encodeInt(waterLevel);
        outPacket.encodeByte(maxLevel); //max waterLevel17 from Kenta Map ID: 923040300
        outPacket.encodeInt(time); //m_tNextWaterUpTime

        return outPacket;
    }

    public static OutPacket momentAreaOffAll(List<String> data) {
        OutPacket outPacket = new OutPacket(OutHeader.MOMENT_AREA_OFF_ALL);

        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(data.size() > 0 ? 1 : 0);
        if (data.size() > 0) {
            outPacket.encodeInt(data.size());
            for (String value : data) {
                outPacket.encodeString(value);
            }
        }

        return outPacket;
    }

    public static OutPacket alienVisitorResult(int type) {
        OutPacket outPacket = new OutPacket(OutHeader.ALIEN_VISITOR_RESULT);

        outPacket.encodeInt(type);

        return outPacket;
    }

    public static OutPacket openPointEventGauge(DailyCoin dailyCoin) {
        OutPacket outPacket = new OutPacket(OutHeader.POINT_EVENT_GAUGE);

        boolean bEnable = true;
        outPacket.encodeByte(bEnable);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        //UI/UIWindow6.img/starDust
        //UI/UIWindow6.img/ArkPoint
        //UI/UIWindow6.img/gom3UI
        outPacket.encodeString("UI/UIWindow6.img/starDust");
        outPacket.encodeInt(0); //idk
        outPacket.encodeInt(0); //idk
        outPacket.encodeByte(0);
        outPacket.encodeLong(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        if (bEnable) {
            outPacket.encodeInt(dailyCoin.getPoint()); //point
            outPacket.encodeInt(dailyCoin.getCoin()); //coin
            outPacket.encodeByte(dailyCoin.isLock()); //1 = lock bar
            outPacket.encodeLong(FileTime.MAX_TIME().toLong());
        }

        return outPacket;

    }

}
