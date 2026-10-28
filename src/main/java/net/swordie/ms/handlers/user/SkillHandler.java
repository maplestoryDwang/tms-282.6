package net.swordie.ms.handlers.user;

import net.swordie.ms.client.jobs.special.SpecialJobProgression;
import net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime;
import net.swordie.ms.client.character.tms282.Tms282VMatrixSpecialEngine;

import net.swordie.ms.client.Account;
import net.swordie.ms.client.LinkSkill;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.b2body.B2Body;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.EquipAttribute;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.skills.*;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.character.skills.jupiterthunder.JupiterThunder;
import net.swordie.ms.client.character.skills.jupiterthunder.JupiterThunderUpdateInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.client.jobs.adventurer.*;
import net.swordie.ms.client.jobs.adventurer.archer.BowMaster;
import net.swordie.ms.client.jobs.adventurer.archer.Pathfinder;
import net.swordie.ms.client.jobs.adventurer.magician.Bishop;
import net.swordie.ms.client.jobs.adventurer.magician.FirePoison;
import net.swordie.ms.client.jobs.adventurer.magician.IceLightning;
import net.swordie.ms.client.jobs.adventurer.pirate.Buccaneer;
import net.swordie.ms.client.jobs.adventurer.pirate.Cannoneer;
import net.swordie.ms.client.jobs.adventurer.thief.DualBlade;
import net.swordie.ms.client.jobs.adventurer.thief.Shadower;
import net.swordie.ms.client.jobs.adventurer.thief.Thief;
import net.swordie.ms.client.jobs.adventurer.warrior.DarkKnight;
import net.swordie.ms.client.jobs.adventurer.warrior.Paladin;
import net.swordie.ms.client.jobs.anima.HoYoung;
import net.swordie.ms.client.jobs.anima.Ren;
import net.swordie.ms.client.jobs.cygnus.BlazeWizard;
import net.swordie.ms.client.jobs.cygnus.NightWalker;
import net.swordie.ms.client.jobs.cygnus.WindArcher;
import net.swordie.ms.client.jobs.flora.Adele;
import net.swordie.ms.client.jobs.flora.Ark;
import net.swordie.ms.client.jobs.flora.Illium;
import net.swordie.ms.client.jobs.legend.Aran;
import net.swordie.ms.client.jobs.legend.Evan;
import net.swordie.ms.client.jobs.legend.Phantom;
import net.swordie.ms.client.jobs.nova.AngelicBuster;
import net.swordie.ms.client.jobs.nova.Kaiser;
import net.swordie.ms.client.jobs.resistance.Blaster;
import net.swordie.ms.client.jobs.resistance.Xenon;
import net.swordie.ms.client.jobs.resistance.demon.DemonAvenger;
import net.swordie.ms.client.jobs.sengoku.Hayato;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.*;
import net.swordie.ms.enums.*;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.skill.MobSkillID;
import net.swordie.ms.life.mob.skill.MobSkillStat;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.loaders.containerclasses.MakingSkillRecipe;
import net.swordie.ms.loaders.containerclasses.MobSkillInfo;
import net.swordie.ms.util.*;
import net.swordie.ms.util.container.Tuple;
import net.swordie.ms.world.field.Field;

import java.util.*;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;
import static net.swordie.ms.client.jobs.Job.*;
import static net.swordie.ms.client.jobs.legend.Luminous.PRESSURE_VOID;

public class SkillHandler {

    @Handler(op = InHeader.USER_DEBUFF_OBJ_COLLISION)
    public static void handleDebuffObjCollision(Char chr, InPacket inPacket) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int key = inPacket.decodeInt(); //0 = end, 2 = move back, 1 = attack
        int dataType = inPacket.decodeInt();
        if (key == 0 && tsm.hasStat(CharacterTemporaryStat.GuidedArrow)) {
            Skill skill = chr.getSkill(GUIDED_ARROW);
            if (skill != null) {
                chr.getTemporaryStatManager().removeStatsBySkill(GUIDED_ARROW);

                SkillInfo skillInfo = SkillData.getSkillInfoById(skill.getSkillId());
                Option o = new Option();
                o.nOption = skillInfo.getValue(z, skill.getCurrentLevel());
                o.rOption = skill.getSkillId();
                o.tOption = skillInfo.getValue(time, skill.getCurrentLevel());
                tsm.sendStat(GuidedArrow, o);

                Summon summon = Summon.getSummonByAndSetStat(chr, skill.getSkillId(), (byte) skill.getCurrentLevel());
                summon.setFlyMob(true);
                summon.setMoveAbility(MoveAbility.Walk);
                summon.setAssistType(AssistType.Attack);
                chr.getField().spawnSummon(summon);
            }
        }
        if (chr.getHP() > 0 && !tsm.hasStat(CharacterTemporaryStat.NotDamaged) && !tsm.hasStat(CharacterTemporaryStat.IndieNotDamaged) && !tsm.hasStat(CharacterTemporaryStat.GuidedArrow)) {
            Option o = new Option(MobSkillID.GiveMeHeal.getVal());
            MobSkillInfo msi = SkillData.getMobSkillInfoByIdAndLevel(MobSkillID.GiveMeHeal.getVal(), 1);
            o.nOption = 1;
            o.slv = 1;
            o.tOption = msi.getSkillStatIntValue(MobSkillStat.time);
            tsm.sendSetStatFromMobSkillPacket(CharacterTemporaryStat.GiveMeHeal, o);
        }
    }

    @Handler(op = InHeader.USER_UPDATE_LAPIDIFICATION)
    public static void handleUpdateLapidification(Char chr, InPacket inPacket) {
        Field field = chr.getField();
        int phase = inPacket.decodeInt();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (phase == 0) {
            tsm.removeStat(CharacterTemporaryStat.Lapidification);
        }
    }

    @Handler(op = InHeader.USER_CREATE_LUCK_OF_THE_DRAW_FORCE_ATOM)
    public static void handleUserCreateLuckOfTheDrawForceAtom(Char chr, InPacket inPacket) {
        Field field = chr.getField();
        if (GameConstants.getMaplerunnerField(field.getId()) == -1
                && ((field.getFieldLimit() & FieldOption.SkillLimit.getVal()) > 0
                || (field.getFieldLimit() & FieldOption.MoveSkillOnly.getVal()) > 0)) {
            chr.dispose();
            return;
        }
        if (chr.hasSkill(Phantom.LUCK_OF_THE_DRAW)) {
            ((Phantom) chr.getJobHandler()).createLuckOfTheDrawForceAtom();
        }
    }

    @Handler(op = InHeader.USER_UPDATE_BENEDICTION_AREA)
    public static void handleUserUpdateBenedictionArea(Char chr, InPacket inPacket) {
        Field field = chr.getField();
        if (GameConstants.getMaplerunnerField(field.getId()) == -1
                && ((field.getFieldLimit() & FieldOption.SkillLimit.getVal()) > 0
                || (field.getFieldLimit() & FieldOption.MoveSkillOnly.getVal()) > 0)) {
            chr.dispose();
            return;
        }
        if (JobConstants.isBishop(chr.getJob())) {
            ((Bishop) chr.getJobHandler()).giveBenedictionBuff();
        }
    }

    @Handler(op = InHeader.RETURN_TELEPORT_DEBUFF)
    public static void handleReturnTeleportDebuff(Char chr, InPacket inPacket) {
        Field field = chr.getField();
        int phase = inPacket.decodeInt();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (phase == 0) {
            tsm.removeStat(CharacterTemporaryStat.ReturnTeleport);
        }
    }

    @Handler(op = InHeader.USER_KEY_DOWN_AREA_MOVING)
    public static void handleKeyDownAreaMoving(Char chr, InPacket inPacket) {
        byte size = inPacket.decodeByte();
        List<Byte> movingPathKeys = new ArrayList<>();
        for (byte i = 0; i < size; i++) {
            movingPathKeys.add(inPacket.decodeByte());
        }
        chr.getField().broadcast(UserRemote.keyDownAreaMovePath(chr.getId(), movingPathKeys), chr);
    }

    @Handler(op = InHeader.USER_CREATE_AURA_BY_GRENADE)
    public static void handleUserCreateAuraByGrenade(Char chr, InPacket inPacket) {
        Field field = chr.getField();
        if (GameConstants.getMaplerunnerField(field.getId()) == -1
                && ((field.getFieldLimit() & FieldOption.SkillLimit.getVal()) > 0
                || (field.getFieldLimit() & FieldOption.MoveSkillOnly.getVal()) > 0)) {
            chr.dispose();
            return;
        }
        int objID = inPacket.decodeInt();
        int skillID = SkillConstants.getActualSkillIDfromSkillID(inPacket.decodeInt());
        if (!chr.hasSkill(skillID)) {
            DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("Tried creating an aura by grenade with unavailable skill %d.", skillID));
            return;
        }
        Position position = inPacket.decodePosition();
        byte isLeft = inPacket.decodeByte();
        SkillInfo fci = SkillData.getSkillInfoById(skillID);
        int slv = chr.getSkill(skillID).getCurrentLevel();
        AffectedArea aa = AffectedArea.getPassiveAA(chr, skillID, slv);
        aa.setPosition(position);
        aa.setSkillID(skillID);
        aa.setSlv(slv);
        aa.setRect(aa.getPosition().getRectAround(fci.getRects().get(0)));
        chr.getField().spawnAffectedArea(aa);
    }

    @Handler(op = InHeader.USER_SET_MOVE_GRENADE)
    public static void handleUserSetMoveGrenade(Char chr, InPacket inPacket) {
        Field field = chr.getField();
        if (GameConstants.getMaplerunnerField(field.getId()) == -1
                && ((field.getFieldLimit() & FieldOption.SkillLimit.getVal()) > 0
                || (field.getFieldLimit() & FieldOption.MoveSkillOnly.getVal()) > 0)) {
            chr.dispose();
            return;
        }
        int skillID = inPacket.decodeInt();
        int grenadeID = inPacket.decodeInt();
        long walkSpeed = inPacket.decodeLong();
        int moveEndingX = inPacket.decodeInt();
        boolean left = inPacket.decodeByte() != 0;
        chr.getField().broadcast(UserRemote.setMoveGrenade(chr, skillID, grenadeID, walkSpeed, moveEndingX, left), chr);
    }

    @Handler(op = InHeader.USER_SKILL_USE_REQUEST)
    public static void handleUserSkillUseRequest(Char chr, InPacket inPacket) {
        Field field = chr.getField();
        if (GameConstants.getMaplerunnerField(field.getId()) == -1 && ((field.getFieldLimit() & FieldOption.SkillLimit.getVal()) > 0
                || (field.getFieldLimit() & FieldOption.MoveSkillOnly.getVal()) > 0)) {
            chr.dispose();
            return;
        }
        inPacket.decodeInt(); // crc
        int skillID = inPacket.decodeInt();
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                && net.swordie.ms.client.character.tms282.Tms282VMatrixEffects.source(skillID)) {
            chr.dispose();
            return; // These18 effects are exclusively applied by the validated special-core runtime.
        }
        inPacket.decodeInt(); // 0?
        if (JobConstants.isZero(chr.getJob())) {
            inPacket.decodeByte(); // Why zero has a byte before the skill Level and always 0
        }
        int slv = inPacket.decodeInt();

        if (SpecialJobProgression.isSpecialSkill(skillID)) {
            slv = SpecialJobProgression.requestLevel(chr, skillID);
            if (slv <= 0) {
                chr.dispose();
                return;
            }
        } else if (slv == 0) {
            slv = chr.getSkillLevel(skillID);
        } else {
            int curSLV = chr.getSkillLevel(skillID);
            if (chr.hasSkill(skillID) && slv != curSLV) {
                slv = curSLV;
            }
        }

        inPacket.decodeInt(); // 0?
        inPacket.decodeInt();

        if (chr.isGM()) {
            try {
                chr.dbgChatMsg("[SỬ DỤNG] skillID : " + skillID + " (" + StringData.getSkillStringById(skillID).getName() + "), slv : " + slv);
            } catch (Exception ignored) {}
        }

        SkillInfo si = SkillData.getSkillInfoById(skillID);
        if (si == null) {
            if (chr.isGM()) {
                chr.chatScriptMessage("技能 " + skillID + " 尚未實作。");
            }
            chr.dispose();
            return;
        }

        SkillUseInfo skillUseInfo = new SkillUseInfo(si, slv);
        skillUseInfo.skillID = skillID;
        new ProcessType(skillUseInfo).decode(inPacket); // Not using anything from Process type as of now
        specialMoveAdditionalInfo(inPacket);
        int option = inPacket.decodeInt();
        if (((option >> 4) & 1) != 0) {
            inPacket.decodeShort();
            inPacket.decodeShort();
        }
        if (skillID == 400031051) {
            skillUseInfo.isLeft = inPacket.decodeByte() != 0;
        }
        if (skillID == Adele.RESONANCE_RUSH) {
            skillUseInfo.objectId = inPacket.decodeInt();
        }
        if (skillID == Shadower.SHADOW_VEIL || skillID == Shadower.HEXA_SHADOW_VEIL || skillID == DualBlade.BLADES_OF_DESTINY) {
            chr.write(UserLocal.skillUseResult((byte) 1, skillID));
        }
        if (skillID == Paladin.HEAVENS_HAMMER || skillID == Paladin.HEXA_HEAVENS_HAMMER) {
            chr.write(UserLocal.skillUseResult((byte) 0, 0));
        }
        if (skillID == Cannoneer.NUCLEAR_OPTION) {
            chr.write(UserLocal.skillUseResult((byte) 0, skillID));
        }
        if (skillID == BlazeWizard.FLASHFIRE || skillID == Adele.NOBLE_SUMMONS || skillID == Adele.HEXA_NOBLE_SUMMONS) {
            chr.write(UserLocal.skillUseResult((byte) 1, 0));
        }
        if (skillID == Shadower.INTO_DARKNESS) {
            skillID = skillUseInfo.skillID = Thief.DARK_SIGHT;
        }

        if (chr.applyBulletCon(si, slv)) {
            if (chr.applyMpCon(si, slv, true)) {
                if (chr.checkAndSetSkillCooltime(skillID, false)) {
                    if (skillID != 11121014) {
                        chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.skillUse(skillID, chr.getLevel(), slv)), chr);
                    }
                    Job sourceJobHandler = chr.getJobHandler();
                    if (si.isMassSpell() && chr.getParty() != null) {
                        Rect r = si.getFirstRect();
                        if (r != null) {
                            Rect rectAround = chr.getRectAround(r);
                            for (Char ptChr : chr.getParty().getPartyMembersInSameFieldWithChr(chr)) {
                                if (ptChr != null && ptChr.getField().equals(chr.getField()) && rectAround.hasPositionInside(ptChr.getPosition())) {
                                    Effect effect = Effect.skillAffected(skillID, chr.getLevel(), slv);
                                    chr.getField().broadcast(UserRemote.effect(ptChr.getId(), effect), ptChr);
                                    ptChr.write(UserPacket.effect(effect));
                                    sourceJobHandler.handleSkill(ptChr.getClient(), inPacket, skillUseInfo);
                                }
                            }
                        }
                    } else {
                        sourceJobHandler.handleSkill(chr.getClient(), inPacket, skillUseInfo);
                    }
                    if (Tms282VMatrixRuntime.managed(chr) && skillID > 0 && slv > 0
                            && (chr.getSkillLevel(skillID) > 0 || SpecialJobProgression.isSpecialSkill(skillID)
                            && SpecialJobProgression.requestLevel(chr, skillID) > 0)) {
                        // Once for the caster, after decoding, resources, cooldown and dispatch.
                        Tms282VMatrixRuntime.event(chr, Tms282VMatrixSpecialEngine.Type.SKILL_USE, 1, 0);
                    }
                } else {
                    chr.chatMessage("技能冷卻中。");
                }
            } else {
                short job = chr.getJob();
                if (SkillConstants.isSoulSummonSkill(skillID)) {
                    chr.chatMessage("靈魂不足，無法使用技能。");
                } else if (JobConstants.isNoManaJob(job)) {
                    chr.chatMessage(String.format("%s不足，無法使用技能。",
                            JobConstants.isKinesis(job) ? "PP" : JobConstants.isZero(job) ? "能量" :
                                    JobConstants.isKanna(job) ? "MP" : "HP"));
                } else {
                    chr.chatMessage(JobConstants.isDemonSlayer(job) ? "惡魔之怒不足，無法使用技能。"
                            : "MP 不足，無法使用技能。");
                }
            }
        } else {
            chr.chatMessage("箭矢或武器不足，無法使用技能。");
        }
        chr.dispose();
    }

    private static void specialMoveAdditionalInfo(InPacket inPacket) {
        inPacket.decodeByte();
        inPacket.decodeByte();
        inPacket.decodeByte();
        inPacket.decodeShort();
        inPacket.decodeInt();
        inPacket.decodeByte();
        inPacket.decodeByte();
        inPacket.decodeInt();
        inPacket.decodeInt();
        inPacket.decodeString();
        inPacket.decodeInt();
    }

    @Handler(op = InHeader.USER_CHECK_TEMPORARY_STAT_DURATION_REQUEST)
    public static void handleUserCheckTemporaryStatDurationRequest(Char chr, InPacket inPacket) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (JobConstants.isFirePoison(chr.getJob())) {
            tsm.removeStatsBySkill(FirePoison.ELEMENTAL_DRAIN);
        }
        tsm.removeCheckByTime();
        chr.dispose();
    }

    @Handler(op = InHeader.LINK_SKILL_REQUEST)
    public static void handleLinkSkillRequest(Char chr, InPacket inPacket) {
        int type = inPacket.decodeInt(); // 0, 1, 2, 3
        byte preset = inPacket.decodeByte();
        List<Integer> linkSkills = new ArrayList<>();
        if (type == 0) { // init Preset
            int size = inPacket.decodeInt();
            for (int i = 0; i < size; i++) {
                int skillID = inPacket.decodeInt();
                linkSkills.add(skillID);
            }
            for (Integer skillID : chr.getLinked_LinkSkillIDs()) {
                chr.removeSkill(skillID);
            }
            Map<Integer, Integer> accLinkSkills = new HashMap<>();
            for (LinkSkill linkSkill : chr.getAccountLinkSkills()) {
                accLinkSkills.put(linkSkill.getLinkSkillID(), linkSkill.getLevel());
            }
            List<Skill> skills = new ArrayList<>();
            for (Integer skillID : linkSkills) {
                if (accLinkSkills.getOrDefault(skillID, 0) != 0) {
                    Skill skill = SkillData.getSkillDeepCopyById(skillID);
                    if (skill != null) {
                        int skillLevel = accLinkSkills.get(skillID);
                        skill.setCurrentLevel(skillLevel);
                        skill.setMasterLevel(skill.getMasterLevel());
                        skill.setMaxLevel(skill.getMasterLevel());
                        skills.add(skill);
                        LinkedSkill linkedSkill = chr.getLinkedSkill(preset, skillID);
                        if (linkedSkill != null) {
                            linkedSkill.setSkillID(skillID);
                            linkedSkill.setSkillLevel(skillLevel);
                        } else {
                            linkedSkill = new LinkedSkill(chr.getId(), preset, skillID, skillLevel);
                            chr.getLinkedSkills().add(linkedSkill);
                        }
                    }
                }
            }
            chr.addListSkill(skills);
            chr.write(WvsContext.linkedSkillInfo(type, preset, linkSkills));
        } else if (type == 1) { // change Preset
            chr.write(WvsContext.linkedSkillInfo(type, preset, null));
            chr.setQRValueByKey(QuestConstants.LINK_SKILL_PRESET, "preset", preset);
            chr.initLinkSkills(false);
        } else if (type == 2 || type == 3) { // add / remove skill
            int skillID = inPacket.decodeInt();
            List<Integer> skills = new ArrayList<>(5);
            switch (skillID) {
                case 80000055 -> { skills.add(80000066); skills.add(80000067); skills.add(80000068); skills.add(80000069); skills.add(80000070); }
                case 80000329 -> { skills.add(80000333); skills.add(80000334); skills.add(80000333); skills.add(80000378); }
                case 80002758 -> { skills.add(80002759); skills.add(80002760); skills.add(80002761); }
                case 80002762 -> { skills.add(80002763); skills.add(80002764); skills.add(80002765); }
                case 80002766 -> { skills.add(80002767); skills.add(80002768); skills.add(80002769); }
                case 80002770 -> { skills.add(80002771); skills.add(80002772); skills.add(80002773); }
                case 80002774 -> { skills.add(80002775); skills.add(80002776); skills.add(80002773); skills.add(80000000); }
                default -> skills.add(skillID);
            }
            Account acc = chr.getAccount();
            int stackedLevel = 0;
            int skillLevel = 0;
            for (Integer sid : skills) {
                LinkSkill linkSkill = acc.getLinkSkills().stream().filter(s -> s.getLinkSkillID() == sid).findAny().orElse(null);
                if (linkSkill == null) {
                    continue;
                }
                if (linkSkill.getOwnerID() == chr.getId()) {
                    continue;
                }
                skillLevel += linkSkill.getLevel();
            }
            if (skillLevel == 0) {
                chr.chatPopup("發生未知錯誤。");
                chr.dispose();
                return;
            }
            int ordinarySkill = 0;
            byte odrinaryMaxLevel = 0;
            if (skillID >= 80000066 && skillID <= 80000070) {
                ordinarySkill = 80000055;
                odrinaryMaxLevel = 10;
            } else if ((skillID >= 80000333 && skillID <= 80000335) || skillID == 80000378) {
                ordinarySkill = 80000329;
                odrinaryMaxLevel = 8;
            } else if (skillID >= 80002759 && skillID <= 80002761) {
                ordinarySkill = 80002758;
                odrinaryMaxLevel = 6;
            } else if (skillID >= 80002763 && skillID <= 80002765) {
                ordinarySkill = 80002762;
                odrinaryMaxLevel = 6;
            } else if (skillID >= 80002767 && skillID <= 80002769) {
                ordinarySkill = 80002766;
                odrinaryMaxLevel = 6;
            } else if (skillID >= 80002771 && skillID <= 80002773) {
                ordinarySkill = 80002770;
                odrinaryMaxLevel = 6;
            } else if ((skillID >= 80002775 && skillID <= 80002776) || skillID == 80000000) {
                ordinarySkill = 80002774;
                odrinaryMaxLevel = 6;
            }
            if (type == 2) {
                if (ordinarySkill > 0) {
                    stackedLevel = skillLevel + chr.getSkillLevel(ordinarySkill);
                    chr.addSkill(skillID, skillLevel, 2);
                    chr.addSkill(ordinarySkill, stackedLevel, odrinaryMaxLevel);
                } else {
                    chr.addSkill(skillID, skillLevel, SkillData.getSkillInfoById(skillID).getMaxLevel());
                }
                LinkedSkill linkedSkill = chr.getLinkedSkill(preset, skillID);
                if (linkedSkill != null) {
                    linkedSkill.setSkillID(skillID);
                    linkedSkill.setSkillLevel(skillLevel);
                    linkedSkill.saveToSQL();
                } else {
                    linkedSkill = new LinkedSkill(chr.getId(), preset, skillID, skillLevel);
                    chr.getLinkedSkills().add(linkedSkill);
                    linkedSkill.saveToSQL();
                }
            } else {
                if (ordinarySkill > 0) {
                    chr.removeSkill(skillID);
                    chr.removeSkill(ordinarySkill);
                } else {
                    chr.removeSkill(skillID);
                }
                LinkedSkill linkedSkill = chr.getLinkedSkill(preset, skillID);
                if (linkedSkill != null) {
                    linkedSkill.setSkillID(0);
                    linkedSkill.setSkillLevel(0);
                    linkedSkill.saveToSQL();
                }
            }
            linkSkills.add(skillID);
            chr.write(WvsContext.linkedSkillInfo(type, preset, linkSkills));
        }
    }

    @Handler(op = InHeader.USER_THROW_GRENADE)
    public static void handleUserThrowGrenade(Char chr, InPacket inPacket) {
        Position newPos = inPacket.decodePositionInt();
        Position oldPos = inPacket.decodePositionInt();
        int keyDown = inPacket.decodeInt();
        int bySummonedID = inPacket.decodeInt(); // slv according to ida, but let's just take that server side
        int skillID = inPacket.decodeInt();
        boolean left = inPacket.decodeByte() != 0;
        int attackSpeed = inPacket.decodeInt();
        int grenadeID = inPacket.decodeInt();
        inPacket.decodeByte();
        inPacket.decodeByte();
        int unk1 = inPacket.decodeInt();
        int unk2 = inPacket.decodeInt();
        int unk3 = 0, unk4 = 0;
        if (inPacket.getUnreadAmount() > 0) {
            unk3 = inPacket.decodeInt();
            unk4 = inPacket.decodeInt();
        }
        Skill skill = chr.getSkill(SkillConstants.getLinkedSkill(skillID));
        int slv = skill == null ? 0 : skill.getCurrentLevel();
        boolean success = true;
        SkillInfo si = SkillData.getSkillInfoById(SkillConstants.getActualSkillIDfromSkillID(skillID));
        if (si != null && si.hasCooltime()) {
            if (!chr.checkAndSetSkillCooltime(skillID, false)) {
                success = false;
            } else {
                chr.setSkillCooldown(skillID, slv);
            }
            if (success) {
                chr.getField().broadcast(UserRemote.throwGrenade(chr.getId(), grenadeID, newPos, keyDown, skillID,
                        bySummonedID, slv, left, attackSpeed, unk1, unk2, unk3, unk4), chr);
                Job jobHandler = chr.getJobHandler();
                jobHandler.handleSkill(chr.getClient(), inPacket, new SkillUseInfo(si, slv));
            }
        }
    }

    @Handler(op = InHeader.USER_DESTROY_GRENADE)
    public static void handleUserDestroyGrenade(Char chr, InPacket inPacket) {
        int grenadeID = inPacket.decodeInt();
        byte unk = inPacket.decodeByte();
        int skillID = inPacket.decodeInt();
        int unk2 = inPacket.decodeInt();
        chr.getField().broadcast(UserRemote.destroyGrenade(chr.getId(), grenadeID, skillID), chr);
    }

    @Handler(op = InHeader.USER_B2_BODY_REQUEST)
    public static void handleB2BodyRequest(Char chr, InPacket inPacket) {
        short requestType = inPacket.decodeShort();
        int b2BodyUnkId = inPacket.decodeInt();

        switch (requestType) {
            case 0:
                byte unk1 = inPacket.decodeByte();
                int b2BodyId = inPacket.decodeInt();
                byte type = inPacket.decodeByte();
                Position position = inPacket.decodePosition();
                short nRadius = 0;
                short fRadius = 0;
                if (type == 5) {
                    nRadius = inPacket.decodeShort();
                    fRadius = inPacket.decodeShort();
                }
                short scale = inPacket.decodeShort();
                int skillId = inPacket.decodeInt();
                int slv = inPacket.decodeShort();
                short unk2 = inPacket.decodeShort(); // 0 encoded
                int duration = inPacket.decodeInt(); // in MS
                short unk3 = inPacket.decodeShort(); // 10 encoded

                B2Body b2Body = new B2Body(b2BodyUnkId, chr, b2BodyId, type, position, nRadius, fRadius, scale, skillId, slv, duration);
                chr.write(UserLocal.b2BodyResult(requestType, b2Body));
                chr.write(UserLocal.b2BodyResultNew(requestType, b2Body));
                break;
            case 3:
                b2BodyId = inPacket.decodeInt();
                skillId = inPacket.decodeInt();
                slv = inPacket.decodeInt();
                int maxSpeedX = inPacket.decodeInt();
                int maxSpeedY = inPacket.decodeInt();
                b2Body = new B2Body(b2BodyUnkId, chr, b2BodyId, skillId, slv, maxSpeedX, maxSpeedY);
                chr.write(UserLocal.b2BodyResult(requestType, b2Body));
                chr.write(UserLocal.b2BodyResultNew(requestType, b2Body));
                break;
            case 4:
                b2BodyId = inPacket.decodeInt();
                position = inPacket.decodePosition();
                inPacket.decodePosition();
                skillId = inPacket.decodeInt();
                boolean left = inPacket.decodeByte() != 0;
                unk1 = inPacket.decodeByte(); // 0 encoded
                inPacket.decodeInt();
                inPacket.decodeInt();
                inPacket.decodeInt();
                inPacket.decodeInt();
                inPacket.decodeByte();
                slv = inPacket.decodeShort();
                unk2 = inPacket.decodeShort();
                unk3 = inPacket.decodeShort();
                inPacket.decodeByte();
                inPacket.decodeInt();
                maxSpeedX = inPacket.decodeInt();
                maxSpeedY = inPacket.decodeInt();
                b2Body = new B2Body(b2BodyUnkId, chr, b2BodyId, skillId, slv, maxSpeedX, maxSpeedY);
                b2Body.setPosition(position);
                chr.write(UserLocal.b2BodyResultNew(requestType, b2Body));
                break;
            default:
                System.out.printf("Unhandled B2Body Request Type: %d%n", requestType);
                break;
        }
    }

    @Handler(op = InHeader.MAKING_SKILL_REQUEST)
    public static void handleMakingSkillRequest(Char chr, InPacket inPacket) {
        int recipeID = inPacket.decodeInt();
        MakingSkillRecipe msr = SkillData.getRecipeById(recipeID);
        if (msr == null || !msr.isAbleToBeUsedBy(chr)) {
            return;
        }
        List<Tuple<Integer, Integer>> itemResult = new ArrayList<>();
        for (Tuple<Integer, Integer> recipe : msr.getIngredient()) {
            int itemID = recipe.getLeft();
            int count = recipe.getRight();
            if (chr.hasItemCount(itemID, count)) {
                chr.consumeItem(itemID, count);
                itemResult.add(new Tuple<>(itemID, -count));
            } else {
                chr.chatPopup("材料不足。");
                return;
            }
        }
        int reqSkillID = msr.getReqSkillID();
        Item crafted = null;
        MakingSkillRecipe.TargetElem target = new MakingSkillRecipe.TargetElem();
        MakingSkillResult result = MakingSkillResult.CRAFTING_FAILED;
        if (Randomizer.nextInt(100) < MakingSkillRecipe.getSuccessProb(reqSkillID, msr.getRecommandedSkillLevel(), chr.getMakingSkillLevel(reqSkillID)) || recipeID / 10000 <= 9201) {
            int rand = Randomizer.nextInt(100);
            List<MakingSkillRecipe.TargetElem> targets = msr.getTarget();
            while (true) {
                target = targets.get(Randomizer.rand(0, targets.size() - 1));
                if (target.getProbWeight() >= rand) {
                    break;
                } else {
                    rand = Randomizer.nextInt(100);
                }
            }
            crafted = ItemData.getItemDeepCopy(target.getItemID(), Randomizer.isSuccess(chr.getMakingSkillLevel(reqSkillID) * 2));
            if (crafted == null) {
                chr.getField().broadcast(FieldPacket.makingSkillResult(chr.getId(), recipeID, MakingSkillResult.UNKNOWN_ERROR, target, 0));
                return;
            }
            crafted.setQuantity(target.getCount());
            result = MakingSkillResult.SUCESS_COOL;
            if (ItemConstants.isEquip(target.getItemID())) {
                ((Equip) crafted).addAttribute(EquipAttribute.Crafted);
                crafted.setOwner(chr.getName());
                crafted.setQuantity(1);// equipment shouldn't be more than one
            }
            if (msr.getExpiredPeriod() > 0) {
                crafted.setDateExpire(FileTime.fromLong(System.currentTimeMillis() + ((long) msr.getExpiredPeriod() * 60 * 1000)));
            }
            if (msr.isNeedOpenItem()) {
                chr.removeSkillAndSendPacket(recipeID);
            }
        }

        boolean success = result != MakingSkillResult.CRAFTING_FAILED;
        int incSkillProficiency = msr.getIncProficiency(chr, success);
        if (crafted != null) {
            chr.addItemToInventory(crafted);
            itemResult.add(new Tuple<>(crafted.getItemId(), crafted.getQuantity()));
        }
        chr.addMakingSkillProficiency(recipeID, incSkillProficiency);
        chr.addStatAndSendPacket(Stat.fatigue, msr.getIncFatigability());
        if (success) {
            Stat trait = switch (reqSkillID) {
                case 92000000 -> Stat.senseEXP;
                case 92010000 -> Stat.willEXP;
                default -> Stat.craftEXP;
            };
            chr.addTraitExp(trait, (int) Math.pow(2, chr.getMakingSkillLevel(reqSkillID) + 2));
        }
        chr.getField().broadcast(FieldPacket.makingSkillResult(chr.getId(), recipeID, result, target, incSkillProficiency));
        chr.write(UserPacket.effect(Effect.gainQuestItem(itemResult)));
    }

    @Handler(op = InHeader.USER_CREATE_AREA_DOT_REQUEST)
    public static void handleUserCreateAreaDoTRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt();
        int skillID = inPacket.decodeInt();
        inPacket.decodeInt();
        short loopSize = inPacket.decodeShort();
        for (int i = 0; i < loopSize; i++) {
            Rect rect = inPacket.decodeIntRect();
        }
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        if (si == null) {
            if (chr.isGM()) {
                chr.chatScriptMessage("技能 " + skillID + " 尚未實作。");
            }
            chr.dispose();
            return;
        }
        SkillUseInfo skillUseInfo = new SkillUseInfo(si, chr.getSkillLevel(skillID));

        if (skillID == Adele.SHARDBREAKER || skillID == Adele.HEXA_SHARDBREAKER) {
            skillUseInfo.spawnCrystals = true;
        }

        chr.getJobHandler().handleSkill(chr.getClient(), inPacket, skillUseInfo);
    }

    @Handler(op = InHeader.USER_FORCE_ATOM_COLLISION)
    public static void handleForceAtomCollision(Char chr, InPacket inPacket) {
        int size = inPacket.decodeInt();
        int skillId = inPacket.decodeInt();
        int size2 = inPacket.decodeInt();
        for (int i = 0; i < size2; i++) {
            int skillId2 = inPacket.decodeInt();
            int forceAtomKey = inPacket.decodeInt();
            int unk = inPacket.decodeInt();
            Position position = inPacket.decodePositionInt();
            chr.getJobHandler().handleGuidedForceAtomCollision(forceAtomKey, skillId2, position);
        }
        for (int i = 0; i < size; i++) {
            int forceAtomKey = inPacket.decodeInt();
            byte unk = inPacket.decodeByte();
            int mobObjId = inPacket.decodeInt();
            int createTime = inPacket.decodeInt();
            int ownerId = inPacket.decodeInt(); // sometimes also the mobId (?)
            int unk2 = inPacket.decodeInt();
            Position position = inPacket.decodePositionInt();
            if (skillId == 0) {
                byte unkk = inPacket.decodeByte(); //new sometime after 207
                skillId = inPacket.decodeInt();
            }
            chr.getJobHandler().handleForceAtomCollision(forceAtomKey, skillId, mobObjId, position, inPacket);
        }
    }

    @Handler(op = InHeader.USER_SKILL_PREPARE_STOP)
    public static void handleUserSkillPrepareStop(Char chr, InPacket inPacket) {
        Field field = chr.getField();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if ((field.getFieldLimit() & FieldOption.SkillLimit.getVal()) > 0
                || (field.getFieldLimit() & FieldOption.MoveSkillOnly.getVal()) > 0) {
            chr.dispose();
            return;
        }
        int skillId = inPacket.decodeInt();
        if (!chr.hasSkill(skillId)) {
            return;
        }
        if (SkillConstants.isKeyDownSkill(skillId)) {
            chr.getField().broadcast(UserRemote.skillCancel(chr.getId(), skillId), chr);
            if (tsm.hasStat(CharacterTemporaryStat.IndieKeyDownTime)) {
                tsm.removeStat(CharacterTemporaryStat.IndieKeyDownTime);
            }
            if (tsm.hasStat(KeyDownAreaMoving)) {
                tsm.removeStat(KeyDownAreaMoving);
            }
            chr.getJobHandler().handleCancelKeyDownSkill(chr, SkillConstants.getCorrectCooltimeSkillID(skillId));
        }
        chr.getJobHandler().handleSkillRemove(chr, skillId);
    }

    @Handler(op = InHeader.USER_SKILL_PREPARE_START)
    public static void handleUserSkillPrepareStart(Char chr, InPacket inPacket) {
        int skillId = inPacket.decodeInt();
        int slv = inPacket.decodeInt();
        Skill skill = chr.getSkill(skillId);
        SkillInfo si = SkillData.getSkillInfoById(skillId);
        boolean tms282 = net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
        if (tms282 && skillId == Adele.AETHER_GUARD) { chr.dispose(); return; }
        if (net.swordie.ms.client.jobs.adventurer.thief.Tms282BladeTempest.handles(skillId)) { chr.dispose(); return; }
        if (tms282) {
            slv = SpecialJobProgression.isSpecialSkill(skillId) ? SpecialJobProgression.requestLevel(chr, skillId)
                    : net.swordie.ms.client.character.tms282.Tms282HiddenSkillParents.useLevel(chr, skillId);
        }
        if (si == null || (tms282 ? slv <= 0 : skill == null)) {
            return;
        }
        SkillUseInfo skillUseInfo = new SkillUseInfo(si, slv);
        skillUseInfo.skillID = skillId;
        new ProcessType(skillUseInfo).decode(inPacket); // Not using anything from Process type as of now

        int mask = inPacket.decodeShort();
        int attackActionType = inPacket.decodeByte();
        int attackSpeed = inPacket.decodeByte();
        if (skillId == 13111020 || skillId == 112111016) { // Sentient Arrow || Tornado Flight
            inPacket.decodeShort();
            inPacket.decodeShort();
        }
        int tick = inPacket.decodeInt();
        if (skillId == NightWalker.SHADOW_STITCH) { // Shadow Stitch
            inPacket.decodeShort();
            inPacket.decodeShort();
        }

        if (SkillConstants.isKeyDownSkill(skillId)) {
            if (!chr.applyMpCon(si, slv, true)) {
                return;
            }
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            Option o1 = new Option();
            Option o2 = new Option();

            if (skillId == Xenon.ION_THRUST) {
                ((Xenon) chr.getJobHandler()).applySupplyCost(skillId, chr.getSkillLevel(skillId), si);
            }

            if (skillId == PRESSURE_VOID) {
                // only gets sent once.
                if (chr.hasSkill(skillId) && tsm.getOptByCTSAndSkill(KeyDownAreaMoving, skillId) == null) {
                    o1.nOption = 16;
                    o1.rOption = skillId;
                    tsm.sendStat(KeyDownAreaMoving, o1);
                }
            } else {
                o1.nValue = 1;
                o1.nReason = chr.getJob();
                tsm.sendStat(IndieKeyDownTime, o1);
                net.swordie.ms.client.character.tms282.Tms282DragonMaster.prepared(chr, skillId, o1, System.currentTimeMillis());
            }
            if (skillId == Adele.AETHER_GUARD) {
                EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
                o1.nOption = 1;
                o1.rOption = skillId;
                o1.tOption = 8;
                newStats.put(LWDike, o1);
                o2.nValue = 40;
                o2.nReason = chr.getJob();
                o2.tOption = 8;
                newStats.put(IndieDamReduceR, o2);
                tsm.sendStat(newStats);
            }
            if (skillId == HoYoung.DREAM_GARDEN) {
                EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
                o1.nValue = 1;
                o1.nReason = skillId;
                o1.tTerm = 20;
                newStats.put(IndieNotDamaged, o1);
                o2.nValue = 1;
                o2.nReason = skillId;
                o2.tTerm = 20;
                newStats.put(KeyDownEnable, o2);
                tsm.sendStat(newStats);
            }
            chr.getJobHandler().handleKeyDownSkill(chr, si, inPacket);
            chr.getField().broadcast(UserRemote.skillPrepare(chr, skillId, chr.getSkillLevel(skillId)), chr);
        }
    }

    @Handler(op = InHeader.USER_SKILL_MOVING_SHOOT_ATTACK_PREPARE_REQUEST)
    public static void handleUsermovingShootAttackPrepareRequest(Char chr, InPacket inPacket) {
        if (chr == null) return;
        Field field = chr.getField();
        if ((field.getFieldLimit() & FieldOption.SkillLimit.getVal()) > 0
                || (field.getFieldLimit() & FieldOption.MoveSkillOnly.getVal()) > 0) {
            chr.dispose();
            return;
        }
        int skillId = inPacket.decodeInt();
        short mask = 0;
        byte actionSpeed = 0;
        if (inPacket.getUnreadAmount() > 0) {
            mask = inPacket.decodeShort();
            actionSpeed = inPacket.decodeByte();
        }
        if (!chr.hasSkill(skillId)) {
            return;
        }
        Skill skill = chr.getSkill(skillId);
        chr.getField().broadcast(UserRemote.movingShootAttackPrepare(chr, skillId, (byte) skill.getCurrentLevel(), mask, actionSpeed), chr);
    }

    @Handler(op = InHeader.USER_SKILL_KEY_DOWN_ATTACK_REQUEST)
    public static void handleUserKeyDownAttackRequest(Char chr, InPacket inPacket) {
        Field field = chr.getField();
        if ((field.getFieldLimit() & FieldOption.SkillLimit.getVal()) > 0
                || (field.getFieldLimit() & FieldOption.MoveSkillOnly.getVal()) > 0) {
            chr.dispose();
            return;
        }

        boolean start = inPacket.decodeByte() == 1;
        if (chr.hasSkill(Xenon.OMEGA_BLASTER)) {
            ((Xenon) chr.getJobHandler()).handleOmegaBlaster(start);
        }
    }

    @Handler(op = InHeader.USER_SKILL_CANCEL_REQUEST)
    public static void handleTemporaryStatResetRequest(Char chr, InPacket inPacket) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int skillId = inPacket.decodeInt();
        for (int i = 0; i < CharacterTemporaryStat.length; i++) {
            int mask = inPacket.decodeInt();
            for (CharacterTemporaryStat cts : CharacterTemporaryStat.values()) {
                if (cts.getPos() == i && (cts.getVal() & mask) != 0) {
                    System.out.println("USER_SKILL_CANCEL_REQUEST: " + cts.getBitPos());
                }
            }
        }
        if (skillId == Hayato.QUICK_DRAW) {
            ((Hayato) chr.getJobHandler()).toggleQuickDraw(tsm);
            return;
        }
        tsm.removeStatsBySkill(skillId);

        if (SkillConstants.isKeyDownSkill(skillId)) {
            if (SkillConstants.isKeydownCDSkill(skillId)) {
                Skill skill = chr.getSkill(skillId);
                chr.setSkillCooldown(skillId, (byte) skill.getCurrentLevel());
            }
            chr.getField().broadcast(UserRemote.skillCancel(chr.getId(), skillId), chr);
            if (tsm.hasStat(CharacterTemporaryStat.IndieKeyDownTime)) {
                tsm.removeStat(CharacterTemporaryStat.IndieKeyDownTime);
            }
            chr.getJobHandler().handleCancelKeyDownSkill(chr, SkillConstants.getCorrectCooltimeSkillID(skillId));
        } else {
            if (skillId == Evan.DRAGON_MASTER || skillId == Evan.DRAGON_MASTER_ATTACK) {
                //tsm.sendResetStatPacket(true);
            } else if (skillId == net.swordie.ms.client.jobs.resistance.Mechanic.HUMANOID_MECH || skillId == net.swordie.ms.client.jobs.resistance.Mechanic.TANK_MECH) {
                tsm.removeStatsBySkill(skillId + 100); // because of special use
            }
            if (skillId == Kaiser.NOVA_GUARDIANS || skillId == Kaiser.NOVA_GUARDIANS_2 || skillId == Kaiser.NOVA_GUARDIANS_3 || skillId == Buccaneer.LIGHTING_FORM) {
                return;
            }
            chr.getJobHandler().handleSkillRemove(chr, skillId);
        }
    }

    @Handler(op = InHeader.USER_FINAL_ATTACK_REQUEST)
    public static void handleUserFinalAttackRequest(Char chr, InPacket inPacket) {
        int skillID = inPacket.decodeInt();
        if (!chr.hasSkill(skillID)) {
            chr.dispose();
            return;
        }
        //chr.write(FieldPacket.finalAttackRequest(chr, skillID, chr.getJobHandler().getFinalAttackSkill()));
    }

    @Handler(op = InHeader.USER_PINKBEAN_YO_YO_STACK_REQUEST)
    public static void handleUserPinkBeanYoYoStack(Char chr, InPacket inPacket) {
        if (chr.hasSkill(PinkBean.BLAZING_YOYO)) {
            ((PinkBean) chr.getJobHandler()).incrementYoYoStack(1);
        }
    }

    @Handler(op = InHeader.USER_CHARGE_WIND_ENERGY_REQUEST)
    public static void handleUserWAChargeWindEnergyRequest(Char chr, InPacket inPacket) {
        if (JobConstants.isWindArcher(chr.getJob())) {
            ((WindArcher) chr.getJobHandler()).increaseWindEnergy();
        }
    }

    @Handler(op = InHeader.DEC_PSYCHIC_POINT_REQUEST)
    public static void handleDecPsychicPointRequest(Char chr, InPacket inPacket) {
        if (JobConstants.isKinesis(chr.getJob())) {
            ((Kinesis) chr.getJobHandler()).substractPP(1);
        }
    }

    @Handler(op = InHeader.SHOOT_OBJECT_INCREMENT_REQUEST)
    public static void handleShootObjectIncrementRequest(Char chr, InPacket inPacket) {
        int skillID = inPacket.decodeInt();
        if (JobConstants.isPathFinder(chr.getJob()) && (skillID == Pathfinder.CARDINAL_TORRENT
                || skillID == Pathfinder.CARDINAL_TORRENT_ADVANCED)) {
            ((Pathfinder) chr.getJobHandler()).incrementSwiftStrikeCharge();
        }
        if (JobConstants.isIllium(chr.getJob()) && (skillID == Illium.CRYSTALLINE_SPIRIT)) {
            ((Illium) chr.getJobHandler()).incrementCrystallineShard();
        }
        if (JobConstants.isRen(chr.getJob())) { // intended
            ((Ren) chr.getJobHandler()).incrementRiotousHeart();
        }
    }

    @Handler(op = InHeader.BULLET_BLAST_RELOAD_REQUEST)
    public static void handleBulletBlastReloadRequest(Char chr, InPacket inPacket) {
        int skillId = inPacket.decodeInt();
        byte unk = inPacket.decodeByte();

        if (skillId == Blaster.BULLET_BLAST && JobConstants.isBlaster(chr.getJob()) && chr.hasSkill(Blaster.BULLET_BLAST)) {
            ((Blaster) chr.getJobHandler()).reloadCylinder();
        }
    }

    @Handler(op = InHeader.DEMONIC_BLAST_KEYDOWN_COST)
    public static void handleDemonicBlastKeydownCost(Char chr, InPacket inPacket) {
        var skillId = inPacket.decodeInt();
        var stage = inPacket.decodeInt();

        if (skillId == DemonAvenger.DEMONIC_BLAST_HOLDDOWN && JobConstants.isDemonAvenger(chr.getJob())) {
            ((DemonAvenger) chr.getJobHandler()).demonicBlastKeydownCost();
        }
    }

    @Handler(op = InHeader.CLIENT_SYNC_COOLTIME_REQUEST)
    public static void handleClientSyncCooltimeRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        int loopSize = inPacket.decodeInt();
        Map<Integer, Integer> cooltimeMap = new HashMap<>();
        for (int i = 0; i < loopSize; i++) {
            int skillID = inPacket.decodeInt();
            int unk = inPacket.decodeInt(); // Remaining time  Client side
            if (unk <= 0 || chr.getRemainingCoolTime(skillID) <= 0) {
                cooltimeMap.put(skillID, 0);
            }
        }
        if (!cooltimeMap.isEmpty()) {
            chr.write(UserLocal.skillCooltimeSetM(cooltimeMap));
        }
    }

    @Handler(op = InHeader.V_BLESSING_UPDATE_REQUEST)
    public static void handleVBlessingUpdateRequest(Char chr, InPacket inPacket) {
        int skillID = inPacket.decodeInt();
        switch (skillID) {
            case Xenon.CORE_OVERLOAD_ATTACK:
                if (JobConstants.isXenon(chr.getJob())) {
                    chr.write(UserLocal.userBonusAttackRequest(Xenon.CORE_OVERLOAD_ATTACK));
                }
                break;
            case OTHERWORLD_GODDESS_BLESSING:
                if (chr.hasSkill(skillID)) {
                    EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
                    TemporaryStatManager tsm = chr.getTemporaryStatManager();
                    Option o1 = new Option();
                    Option o2 = new Option();
                    Option o3 = new Option();
                    int[] skills = {BLESSING_OF_RECOVERY, AEGIS_BLESSING, BLESSING_OF_FORTITUDE, OTHERWORLDLY_VOID};
                    int selectionSkillID = skills[Randomizer.nextInt(skills.length)];
                    SkillInfo si = SkillData.getSkillInfoById(skillID);
                    int slv = chr.getSkillLevel(skillID);
                    short jobID = chr.getJob();
                    int term = tsm.hasStatBySkillId(skillID) ? (int) tsm.getRemainingTime(FifthGoddessBless, skillID) / 1000 : si.getValue(time, slv);
                    switch (selectionSkillID) {
                        case BLESSING_OF_RECOVERY:
                            if (JobConstants.isDemonSlayer(jobID)) {
                                chr.healMP(chr.getMaxMP() / 100 * si.getValue(y, slv));
                            }
                            if (JobConstants.isDemonAvenger(jobID)) {
                                chr.heal(chr.getMaxHP() / 100 * si.getValue(y, slv));
                            }
                            if (JobConstants.isKinesis(jobID)) {
                                Kinesis kinesis = ((Kinesis) chr.getJobHandler());
                                kinesis.addPP(kinesis.getMaxPP() / 100 * si.getValue(y, slv));
                            }
                            break;
                        case AEGIS_BLESSING:
                            o1.nReason = skillID;
                            o1.tTerm = term;
                            o1.nValue = si.getValue(z, slv);
                            newStats.put(IndieDamReduceR, o1);
                            break;
                        case BLESSING_OF_FORTITUDE:
                            tsm.removeDebuff();
                            break;
                        case OTHERWORLDLY_VOID:
                            chr.write(UserLocal.userBonusAttackRequest(selectionSkillID));
                            break;
                    }
                    o2.nOption = 1;
                    o2.rOption = skillID;
                    o2.tOption = term;
                    newStats.put(FifthGoddessBless, o2);
                    o3.nReason = skillID;
                    o3.tTerm = term;
                    o3.nValue = si.getValue(indiePMdR, slv);
                    newStats.put(IndiePMdR, o3);
                    tsm.sendStat(newStats);
                    chr.write(UserPacket.effect(Effect.skillUse(selectionSkillID, chr.getLevel(), slv)));
                    chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.skillUse(selectionSkillID, chr.getLevel(), slv)), chr);
                }
                break;
        }
    }

    @Handler(op = InHeader.AURA_SKILL_UPDATE_REQUEST)
    public static void handleBuffIncStackRequest(Char chr, InPacket inPacket) {
        Field field = chr.getField();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();

        int auraSize = inPacket.decodeInt();
        for (int i = 0; i < auraSize; i++) {
            inPacket.decodeInt();
            int skillId = inPacket.decodeInt();
            int slv = inPacket.decodeInt();
            int auraOwnerChrId = inPacket.decodeInt();
            inPacket.decodeInt();
            inPacket.decodeInt();


            if (JobConstants.isAran(chr.getJob()) && skillId == Aran.BLIZZARD_TEMPEST_MOB) {
                ((Aran) chr.getJobHandler()).applyDireWolfCurse();
            } else {
                SkillInfo si = SkillData.getSkillInfoById(skillId);
                var isAuraOwner = chr.getId() == auraOwnerChrId;
                if (isAuraOwner) {
                    if (chr.getParty() != null) {
                        Rect auraRect = chr.getRectAround(si.getFirstRect());
                        var partyChrs = chr.getParty().getPartyMembersInSameField(chr);
                        for (var partyChr : partyChrs) {
                            if (auraRect.hasPositionInside(partyChr.getPosition())) {
                                if (!partyChr.getTemporaryStatManager().hasStatBySkillId(skillId)) {
                                    chr.getJobHandler().handleSkill(partyChr.getClient(), inPacket, new SkillUseInfo(si, slv));
                                }
                            } else if (partyChr.getTemporaryStatManager().hasStatBySkillId(skillId)) {
                                // Remove if chr has aura and is not in Rect
                                partyChr.getTemporaryStatManager().removeStatsBySkill(skillId);
                            }
                        }
                    }
                } else {
                    var auraOwner = field.getCharByID(auraOwnerChrId);
                    if (auraOwner == null) {
                        // Owner is in another field or offline -> Remove aura
                        tsm.removeStatsBySkill(skillId);
                        continue;
                    }

                    if (chr.getParty() == null || !chr.getParty().hasPartyMember(auraOwnerChrId)) {
                        // Owner is not in the party -> Remove aura
                        tsm.removeStatsBySkill(skillId);
                        continue;
                    }

                    if (!(auraOwner.getRectAround(si.getFirstRect()).hasPositionInside(chr.getPosition()))) {
                        // Chr is not within range of the Owner Aura
                        tsm.removeStatsBySkill(skillId);
                        continue;
                    }

                    if (!auraOwner.getTemporaryStatManager().hasStatBySkillId(skillId)) {
                        tsm.removeStatsBySkill(skillId);
                        continue;
                    }

                    if (skillId == Paladin.PARASHOCK_GUARD) {
                        Paladin.doParashockGaurd(chr, auraOwner.getSkillLevel(skillId));
                    }
                }

                if (JobConstants.isBattleMage(chr.getJob()) && isAuraOwner) {
                    //((BattleMage) chr.getJobHandler()).additionalAuraEffects(skillId);
                }
                if (JobConstants.isIceLightning(chr.getJob()) && isAuraOwner) {
                    ((IceLightning) chr.getJobHandler()).doIceAura();
                }
            }
        }
    }

    @Handler(op = InHeader.JUPITER_THUNDER_CREATE_REQUEST)
    public static void handleJupiterThunderCreateRequest(Char chr, InPacket inPacket) {
        var skillId = inPacket.decodeInt();
        var unk1 = inPacket.decodeInt(); // 120
        var unk2 = inPacket.decodeInt(); // 1
        var size = inPacket.decodeInt(); // 1

        var slv = chr.getSkillLevel(skillId);
        for (int i = 0; i < size; i++) {
            Rect rect = inPacket.decodeIntRect();
            if (slv <= 0 || chr.hasSkillOnCooldown(skillId)) {
                continue;
            }
            var jupiterThunder = JupiterThunder.getByInfo(chr, skillId, slv, rect, unk1, unk2, size);
            chr.createJupiterThunder(jupiterThunder);
        }
        chr.setSkillCooldown(skillId, slv);
    }

    @Handler(op = InHeader.JUPITER_THUNDER_REMOVE_REQUEST)
    public static void handleJupiterThunderRemoveRequest(Char chr, InPacket inPacket) {
        var objectId = inPacket.decodeInt();
        var size = inPacket.decodeInt();
        for (int i = 0; i < size; i++) {
            var shocksRemaining = inPacket.decodeInt();

            var jt = chr.getJupiterThunderById(objectId);
            if (jt != null && jt.getOwner().equals(chr)) {
                chr.removeJupiterThunder(objectId);
                chr.write(UserPacket.jupiterThunderRemoved(jt));

                if (JobConstants.isIceLightning(chr.getJob()) && jt.getSkillId() == IceLightning.JUPITER_THUNDER) {
                    ((IceLightning) chr.getJobHandler()).handleRemoveJupiterThunder(shocksRemaining);
                }
            }
        }
    }

    @Handler(op = InHeader.JUPITER_THUNDER_UPDATE_REQUEST)
    public static void handleJupiterThunderUpdateRequest(Char chr, InPacket inPacket) {
        var size = inPacket.decodeInt();
        for (int i = 0; i < size; i++) {
            var objectId = inPacket.decodeInt();

            var jt = chr.getJupiterThunderById(objectId);
            if (jt == null || !(jt.getOwner().equals(chr))) {
                return;
            }

            var jtui = new JupiterThunderUpdateInfo();

            var chrId = inPacket.decodeInt();
            var skillId = inPacket.decodeInt();
            jtui.objectId = objectId;
            jtui.unk2 = inPacket.decodeInt();
            jtui.curTime = inPacket.decodeInt();
            jtui.w = inPacket.decodeInt();
            jtui.y = inPacket.decodeInt();
            jtui.z = inPacket.decodeInt();

            chr.write(UserPacket.jupiterThunderUpdateResult(jt, jtui));
        }
    }

    @Handler(op = InHeader.CHOOSE_LOAD_DICE_NUMBER_REQUEST)
    public static void handleChooseLoadDiceNumberRequest(Char chr, InPacket inPacket) {
        int skillID = LOADED_DICE;
        if (chr.hasSkill(skillID)) {
            chr.getJobHandler().handleSkill(chr.getClient(), inPacket, new SkillUseInfo(SkillData.getSkillInfoById(skillID), chr.getSkillLevel(skillID)));
        }
    }

    @Handler(op = InHeader.DIMENSIONAL_SWORD_CHANGE)
    public static void handleDimensionalSwordChange(Char chr, InPacket inPacket) {
        int skillId = inPacket.decodeInt();
        short job = chr.getJob();
        switch (skillId) {
            case DemonAvenger.DIMENSIONAL_SWORD_SUMMON:
                if (JobConstants.isDemonAvenger(job)) {
                    ((DemonAvenger) chr.getJobHandler()).changeDimensionalSword();
                }
                break;
            case AngelicBuster.MIGHTY_MASCOT:
                if (JobConstants.isAngelicBuster(job)) {
                    ((AngelicBuster) chr.getJobHandler()).doBubbleBreath();
                }
                break;
        }
    }

    @Handler(op = InHeader.GREATER_DARK_SERVANT_SWAP_REQUEST)
    public static void handleGreaterDarkServantSwapRequest(Char chr, InPacket inPacket) {
        int skillId = inPacket.decodeInt();

        if (!chr.hasSkill(skillId)) {
            return;
        }

        switch (skillId) {
            case NightWalker.GREATER_DARK_SERVANT:
            case NightWalker.HEXA_GREATER_DARK_SERVANT:
                ((NightWalker) chr.getJobHandler()).swapWithServant();
                break;
        }
    }

    @Handler(op = InHeader.INHUMAN_SPEED_FORCE_ATOM_REQUEST)
    public static void handleInhumanSpeedForceAtomRequest(Char chr, InPacket inPacket) {
        int mobId = inPacket.decodeInt();
        int tick = inPacket.decodeInt();

        if (chr.getJobHandler() instanceof BowMaster bm) {
            bm.handleInhumanSpeedRequest(mobId);
        }
    }

    @Handler(op = InHeader.SPOTLIGHT_STACK_REQUEST)
    public static void handleSpotlightStackRequest(Char chr, InPacket inPacket) {
        boolean giveBuff = inPacket.decodeByte() != 0; // remove if 0;
        int stackAmount = inPacket.decodeInt();

        if (JobConstants.isAngelicBuster(chr.getJob()) && chr.hasSkill(AngelicBuster.SUPER_STAR_SPOTLIGHT)) {
            ((AngelicBuster) chr.getJobHandler()).giveSpotlightBuff(giveBuff, stackAmount);
        }
    }

    @Handler(op = InHeader.CRYSTAL_SKILL_REQUEST)
    public static void handleCrystalSkillRequest(Char chr, InPacket inPacket) {
        int skillId = inPacket.decodeInt();
        inPacket.decodeInt();

        if (chr.getJobHandler() instanceof Illium illium) {
            if (illium.getCrystal() != null) {
                illium.incrementCrystal(skillId);
            }
        }
    }

    @Handler(op = InHeader.UPDATE_ARK_SPECTRA_ENERGY)
    public static void handleUpdateArkSpectraEnergy(Char chr, InPacket inPacket) {
        int skillId = inPacket.decodeInt();

        if (chr.getJobHandler() instanceof Ark ark) {
            ark.modifySpectraEnergy();
        }
    }

    @Handler(op = InHeader.INCREASE_SKILL_STACK_REQUEST)
    public static void updateSolusRequest(Char chr, InPacket inPacket) {
        int solusStack = inPacket.decodeInt();
        int skillID = inPacket.decodeInt();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (chr.hasSkill(skillID)) {
            if (solusStack < 6 && solusStack > 0) {
                Option o1 = new Option();
                Option o2 = new Option();
                int slv = chr.getSkillLevel(skillID);
                SkillInfo si = SkillData.getSkillInfoById(skillID);
                EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
                o1.nOption = solusStack;
                o1.rOption = SOLUS;
                o1.tOption = si.getValue(time, slv);
                newStats.put(LPBattleMode, o1);
                o2.nValue = (solusStack - 1) * si.getValue(y, slv) + si.getValue(q, slv);
                o2.nReason = SOLUS;
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndieDamR, o2);
                tsm.sendStat(newStats);
            } else if (solusStack == 0) {
                tsm.removeStatsBySkill(SOLUS);
            }
        }
    }

    @Handler(op = InHeader.HEXA_SOL_JANUS_REQUEST)
    public static void handleHexaSolJanusRequest(Char chr, InPacket inPacket) {
        int skillID = inPacket.decodeInt();
        if (skillID == SOL_JANUS_DUSK) {
            for (Summon summon : chr.getField().getSummonsByChar(chr)) {
                if (summon.getSkillID() == SOL_JANUS_DAWN || summon.getSkillID() == SOL_JANUS_DAWN_2 || summon.getSkillID() == SOL_JANUS_DAWN_3) {
                    chr.getField().removeSummon(summon.getSkillID(), chr.getId());
                }
            }
        }
        chr.setQRValueByKey(QuestConstants.SKILL_COMMAND_LOCK_ARAN, "500001000", skillID + "");
        chr.setQRValueByKey(QuestConstants.SKILL_COMMAND_LOCK_ARAN, "pr0", 0 + "");
    }

    @Handler(op = InHeader.HEXA_FATE_SHUFFLE_REQUEST)
    public static void handleFateShuffleRequest(Char chr, InPacket inPacket) {
        int skillID = inPacket.decodeInt();
        chr.setQRValueByKey(QuestConstants.SKILL_COMMAND_LOCK_ARK, "24141003", skillID + "");
        chr.write(UserPacket.skillOnOffEffect(chr.getId(), Phantom.HEXA_MILLE_AIGUILLES));
        chr.write(UserPacket.skillOnOffEffect(chr.getId(), Phantom.HEXA_MILLE_AIGUILLES_FORTUNE));
    }

    @Handler(op = InHeader.REN_UNITY_REQUEST)
    public static void handleRenUnityRequest(Char chr, InPacket inPacket) {
        int skillID = inPacket.decodeInt();
        chr.setQRValueByKey(QuestConstants.SKILL_COMMAND_LOCK_ARK, "161121015", skillID + "");
        chr.write(UserPacket.skillOnOffEffect(chr.getId(), Ren.RIOTOUS_HEART));
        chr.write(UserPacket.skillOnOffEffect(chr.getId(), Ren.HEARTS_UNITED));
    }

    public static void handleFinalPactRequest(Char chr, InPacket inPacket) {
        int mode = inPacket.decodeInt();
        if (mode < 1 || mode > 3) {
            return;
        }
        if (JobConstants.isDarkKnight(chr.getJob()) && chr.hasSkill(DarkKnight.FINAL_PACT_INFO)) {
            ((DarkKnight) chr.getJobHandler()).setFinalPactMode(mode);
        }
    }
}
