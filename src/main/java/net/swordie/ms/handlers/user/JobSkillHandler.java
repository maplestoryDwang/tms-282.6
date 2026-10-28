package net.swordie.ms.handlers.user;


import net.swordie.ms.Server;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.BodyPart;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.quest.Quest;
import net.swordie.ms.client.character.skills.*;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.adventurer.Kinesis;
import net.swordie.ms.client.jobs.adventurer.archer.BowMaster;
import net.swordie.ms.client.jobs.adventurer.pirate.Cannoneer;
import net.swordie.ms.client.jobs.adventurer.warrior.Paladin;
import net.swordie.ms.client.jobs.cygnus.BlazeWizard;
import net.swordie.ms.client.jobs.cygnus.NightWalker;
import net.swordie.ms.client.jobs.legend.Aran;
import net.swordie.ms.client.jobs.nova.AngelicBuster;
import net.swordie.ms.client.jobs.nova.Kaiser;
import net.swordie.ms.client.jobs.resistance.Blaster;
import net.swordie.ms.client.jobs.resistance.WildHunterInfo;
import net.swordie.ms.client.jobs.resistance.demon.DemonAvenger;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.constants.QuestConstants;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.enums.AvatarModifiedMask;
import net.swordie.ms.enums.QuestStatus;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.PsychicLock;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.life.FieldAttackObj;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.Position;
import net.swordie.ms.world.field.Field;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static net.swordie.ms.client.character.skills.SkillStat.time;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;
import static net.swordie.ms.enums.StealMemoryType.REMOVE_STEAL_MEMORY;
import static net.swordie.ms.enums.StealMemoryType.STEAL_SKILL;

public class JobSkillHandler {

    @Handler(op = InHeader.CREATE_KINESIS_PSYCHIC_AREA)
    public static void handleCreateKinesisPsychicArea(Char chr, InPacket inPacket) {
        PsychicArea pa = new PsychicArea();
        pa.action = inPacket.decodeInt();
        pa.actionSpeed = inPacket.decodeInt();
        pa.localPsychicAreaKey = inPacket.decodeInt() + 1;
        pa.psychicAreaKey = inPacket.decodeInt();
        pa.skillID = inPacket.decodeInt();
        pa.slv = inPacket.decodeShort();
        pa.duration = inPacket.decodeInt();
        pa.isLeft = inPacket.decodeByte() != 0;
        pa.skeletonFilePathIdx = inPacket.decodeShort();
        pa.skeletonAniIdx = inPacket.decodeShort();
        pa.skeletonLoop = inPacket.decodeShort();
        pa.start = inPacket.decodePositionInt();
        pa.success = true;
        if (!chr.hasSkillWithSlv(pa.skillID, pa.slv)) {
            return;
        }
        chr.addPsychicArea(pa);
        chr.write(FieldPacket.createPsychicArea(chr.getId(), pa));
        //chr.getField().broadcastPacket(UserLocal.enterFieldPsychicInfo(chr.getId(), null, Collections.singletonList(pa)), chr);
    }

    @Handler(op = InHeader.RELEASE_PSYCHIC_AREA)
    public static void handleReleasePsychicArea(Char chr, InPacket inPacket) {
        int key = inPacket.decodeInt();
        chr.write(FieldPacket.releasePsychicArea(chr.getId(), key));
        chr.removePsychicArea(key);
    }

    @Handler(op = InHeader.PSYCHIC_OVER_REQUEST)
    public static void handlePsychicOverRequest(Char chr, InPacket inPacket) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int mobCount = inPacket.decodeInt();
        if (chr.hasSkill(Kinesis.PSYCHIC_TORNADO) && mobCount > 0) {
            if (!tsm.hasStat(Kinesis_DustTornado)) {
                ((Kinesis) chr.getJobHandler()).substractPP(15);
            }
            SkillInfo si = SkillData.getSkillInfoById(Kinesis.PSYCHIC_TORNADO);
            Option o = new Option();
            o.nOption = 2;
            o.rOption = Kinesis.PSYCHIC_TORNADO;
            o.tOption = si.getValue(time, chr.getSkillLevel(Kinesis.PSYCHIC_TORNADO));
            tsm.sendStat(Kinesis_DustTornado, o);
        }
    }

    @Handler(op = InHeader.DEBUFF_PSYCHIC_AREA)
    public static void handleDebuffPsychicArea(Char chr, InPacket inPacket) {
        int skillID = inPacket.decodeInt();
        short slv = inPacket.decodeShort();
        int posx = inPacket.decodeInt();
        inPacket.decodeByte();
        int posy = inPacket.decodeInt();
        inPacket.decodeInt();
        int mobCount = inPacket.decodeShort();

        if (!chr.hasSkillWithSlv(skillID, slv)) {
            return;
        }
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        for (int i = 0; i < mobCount; i++) {
            int mobID = inPacket.decodeInt();
            Life life = chr.getField().getLifeByObjectID(mobID);
            if (life instanceof Mob mob) {
                if (mob == null) {
                    continue;
                }
                MobTemporaryStat mts = mob.getTemporaryStat();
                EnumMap<MobStat, Option> map = new EnumMap<>(MobStat.class);
                Option o1 = new Option();
                Option o2 = new Option();
                if (skillID == Kinesis.MIND_TREMOR || skillID == Kinesis.MIND_QUAKE) {
                    if (mts.hasCurrentMobStatBySkillId(skillID)) {
                        continue;
                    }
                    int effect = (skillID == 142120003) ? 3 : 2;
                    if (chr.hasSkill(142120036)) {
                        effect *= 2;
                    }
                    o1.nOption = mobCount * effect;
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    map.put(MobStat.PsychicGroundMark, o1);
                    o2.nOption = -mobCount;
                    o2.rOption = skillID;
                    o2.tOption = si.getValue(time, slv);
                    map.put(MobStat.Speed, o2);
                    mts.addStatOptions(mob, map);
                } else {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    mts.addStatOptions(mob, MobStat.PsychicLock, o1);
                }
            }
        }
    }

    @Handler(op = InHeader.CREATE_PSYCHIC_LOCK)
    public static void handleCreatePsychicLock(Char chr, InPacket inPacket) {
        Field f = chr.getField();
        PsychicLock pl = new PsychicLock();
        pl.skillID = inPacket.decodeInt();
        pl.slv = inPacket.decodeShort();
        pl.action = inPacket.decodeInt();
        pl.actionSpeed = inPacket.decodeInt();
        int i = 1;
        while (inPacket.decodeByte() != 0) {
            PsychicLockBall plb = new PsychicLockBall();
            plb.localKey = inPacket.decodeInt();
            plb.psychicLockKey = inPacket.decodeInt();
            plb.psychicLockKey = i++;
            int mobID = inPacket.decodeInt();
            Life life = f.getLifeByObjectID(mobID);
            plb.mob = life == null ? null : (Mob) life;
            plb.stuffID = inPacket.decodeShort();
            inPacket.decodeInt(); // usually 0
            plb.usableCount = inPacket.decodeShort();
            plb.posRelID = inPacket.decodeByte();
            plb.start = inPacket.decodePositionInt();
            plb.rel = inPacket.decodePositionInt();
            pl.psychicLockBalls.add(plb);
        }
        if (!chr.hasSkillWithSlv(pl.skillID, pl.slv)) {
            return;
        }
        //chr.getField().broadcastPacket(UserLocal.enterFieldPsychicInfo(chr.getId(), pl, null), chr);
        chr.write(FieldPacket.createPsychicLock(chr.getId(), pl.success, pl));
        chr.addPsychicLock(pl);
    }

    @Handler(op = InHeader.RELEASE_PSYCHIC_LOCK)
    public static void handleReleasePsychicLock(Char chr, InPacket inPacket) {
        int skillID = inPacket.decodeInt();
        short slv = inPacket.decodeShort();
        if (!chr.hasSkillWithSlv(skillID, slv)) {
            return;
        }
        short count = inPacket.decodeShort();
        int id = inPacket.decodeInt();
        int mobID = inPacket.decodeInt();
        if (mobID >= 0) {
            List<Integer> l = new ArrayList<>();
            l.add(mobID);
            chr.write(FieldPacket.releasePsychicLockMob(chr.getId(), l));
            ((Kinesis) chr.getJobHandler()).addPP(1);
        } else {
            chr.write(FieldPacket.releasePsychicLock(chr.getId(), id));
            chr.removePsychicLock(id);
        }
    }

    @Handler(op = InHeader.DO_ACTIVE_PSYCHIC_AREA)
    public static void handleDoActivePsychicArea(Char chr, InPacket inPacket) {
        int localPsychicAreaKey = inPacket.decodeInt();
        PsychicArea pa = chr.getPsychicArea(localPsychicAreaKey);
        chr.write(UserLocal.doActivePsychicArea(pa));
    }

    @Handler(op = InHeader.REQUEST_ARROW_PLATTER_OBJ)
    public static void handleRequestArrowPlatterObj(Char chr, InPacket inPacket) {
        boolean flip = inPacket.decodeByte() != 0;
        Position position = inPacket.decodePositionInt(); // ignoring this, we just take the char's info we know
        int skillID = BowMaster.ARROW_PLATTER;
        Skill skill = chr.getSkill(skillID);
        if (skill != null && skill.getCurrentLevel() > 0) {
            Field field = chr.getField();
            Set<FieldAttackObj> currentFaos = field.getFieldAttackObjects();
            // remove the old arrow platter
            currentFaos.stream()
                    .filter(fao -> fao.getOwnerId() == chr.getId() && fao.getTemplateId() == 1)
                    .findAny().ifPresent(field::removeLife);
            SkillInfo si = SkillData.getSkillInfoById(skillID);
            int slv = skill.getCurrentLevel();
            FieldAttackObj fao = new FieldAttackObj(1, chr.getId(), chr.getPosition().deepCopy(), flip);
            field.spawnLife(fao, chr);
            field.broadcast(FieldAttackObjPool.objCreate(fao), chr);
            field.addLifeSchedule(fao.getObjectId(), chr.getTimer().addEvent(() -> field.removeLife(fao.getObjectId(), true),
                    si.getValue(SkillStat.u, slv), TimeUnit.SECONDS));
            field.broadcast(FieldAttackObjPool.setAttack(fao.getObjectId(), 0));
        }
        chr.dispose();
    }


    @Handler(op = InHeader.USER_FLAME_ORB_REQUEST)
    public static void handleUserFlameOrbRequest(Char chr, InPacket inPacket) {
        // already Handled in BlazeWizard
    }

    @Handler(op = InHeader.ZERO_TAG)
    public static void handleZeroTag(Char chr, InPacket inPacket) {
        int newTF = inPacket.decodeInt();
        int oldTF = inPacket.decodeInt();
        chr.swapZeroState(oldTF, newTF);
        chr.write(WvsContext.zeroInfoSubHP(chr.getZeroInfo()));
        chr.getField().broadcast(UserRemote.zeroTag(chr));
    }

    @Handler(op = InHeader.ZERO_SHARE_CASH_EQUIP_PART)
    public static void handleZeroShareCashEquipPart(Char chr, InPacket inPacket) {
        int bodyPart = inPacket.decodeInt();
        boolean isShareEquip = inPacket.decodeByte() != 0;
        if (isShareEquip) {
            Item alphaCash = chr.getEquippedInventory().getItemBySlot(BodyPart.getCashEquipByBodyPart(bodyPart));
            Item betaCash = chr.getEquippedInventory().getItemBySlot(BodyPart.getZeroBetaCashEquipByBodyPart(bodyPart));
            if (alphaCash != null) {
                Item.equipZeroShareItemByBodyPart(chr, alphaCash, BodyPart.getZeroBetaCashEquipByBodyPart(bodyPart));
            } else if (betaCash != null) {
                Item.equipZeroShareItemByBodyPart(chr, betaCash, BodyPart.getCashEquipByBodyPart(bodyPart));
            } else {
                chr.chatPopup("更換裝備的速度太快，請稍後再試");
            }
        }
        chr.dispose();
    }

    @Handler(op = InHeader.ZERO_LAST_ASSIST_STATE)
    public static void handleZeroLastAssistState(Char chr, InPacket inPacket) {
        if (JobConstants.isZero(chr.getJob())) {
            chr.getField().broadcast(UserRemote.zeroLastAssistState(chr.getId()));
        }
    }

    @Handler(op = InHeader.REQUEST_SET_BLESS_OF_DARKNESS)
    public static void handleRequestSetBlessOfDarkness(Char chr, InPacket inPacket) {
        if (JobConstants.isLuminous(chr.getJob())) {
            //Luminous.changeBlackBlessingCount(chr, true);
        }
    }

    @Handler(op = InHeader.USER_KEY_DOWN_STEP_REQUEST)
    public static void handleKeyDownStepRequest(Char chr, InPacket inPacket) {
        int skillId = inPacket.decodeInt();
        int keydownDurationMS = inPacket.decodeInt();

        if (keydownDurationMS <= 0) {
            return;
        }

        if (JobConstants.isPaladin(chr.getJob()) && skillId == Paladin.GRAND_GUARDIAN && chr.hasSkill(Paladin.GRAND_GUARDIAN)) {
            ((Paladin) chr.getJobHandler()).increaseGrandGuardianState();
        }
    }

    @Handler(op = InHeader.KEY_DOWN_SKILL_COST)
    public static void handleKeyDownSkillCost(Char chr, InPacket inPacket) {
        int skillId = inPacket.decodeInt();
        chr.getJobHandler().handleKeyDownSkillCost(skillId);
    }

    @Handler(op = InHeader.SKILL_COMMAND_LOCK_ARK)
    public static void handleSkillCommandLockArk(Char chr, InPacket inPacket) {
        int skillId = inPacket.decodeInt();
        int questId = QuestConstants.SKILL_COMMAND_LOCK_ARK;
        Quest quest = chr.getQuestById(questId);
        // Ark Skills
        if (quest == null) {
            chr.createQuestWithQRValue(questId, String.format("%d=1", skillId));
        } else {
            String value = "1";
            if (quest.getProperty(skillId + "") == null) { // doesn't have the skill in qrValue yet
                quest.setProperty(skillId + "", value);
            } else {
                value = quest.getIntProperty(skillId + "") == 1 ? "0" : "1";
                quest.setProperty(skillId + "", value);
            }
            chr.dbgChatMsg("QuestID " + questId + " : " + skillId + " : " + value);
            chr.write(WvsContext.questRecordExMessage(quest));
        }
        chr.write(UserPacket.skillOnOffEffect(chr.getId(), skillId));
        chr.dispose();
    }

    @Handler(op = InHeader.RESET_AIR_HIT_COUNT_REQUEST)
    public static void handleResetAirHitCountRequest(Char chr, InPacket inPacket) {
        if (JobConstants.isAran(chr.getJob())) {
            // TODO
        }
    }

    @Handler(op = InHeader.RW_LIFT_PRESS_REQUEST)
    public static void handleRWLiftPressRequest(Char chr, InPacket inPacket) {
        boolean isDead = inPacket.decodeInt() != 0;
        if (!isDead) {
            int skillID = inPacket.decodeInt();
            for (Mob mob : chr.getField().getMobs()) {
                if (mob == null) {
                    continue;
                }
                MobTemporaryStat mts = mob.getTemporaryStat();
                if (mts.hasCurrentMobStatBySkillId(skillID)) {
                    mts.removeMobStat(mob, MobStat.RWLiftPress);
                }
            }
        }
    }

    @Handler(op = InHeader.RW_ACTION_CANCEL)
    public static void handleRWActionCancel(Char chr, InPacket inPacket) {
        int skillID = inPacket.decodeInt();
        chr.getField().broadcast(UserRemote.requestRWMultiChargeCancel(chr, skillID));
    }

    @Handler(op = InHeader.RELEASE_RW_GRAB)
    public static void handleReleaseRWGrab(Char chr, InPacket inPacket) {
        int skillID = inPacket.decodeInt();
        int mobID = inPacket.decodeInt();
        chr.getField().broadcast(UserRemote.releaseRWGrab(chr, skillID, mobID));
    }

    @Handler(op = InHeader.RW_CLEAR_CURRENT_ATTACK_REQUEST)
    public static void handleRWClearCurrentAttackRequest(Char chr, InPacket inPacket) {
        int skillID = inPacket.decodeInt();
        if (skillID == Blaster.REVOLVING_BLAST) {
            ((Blaster) chr.getJobHandler()).releaseRevolvingBlast();
        }
    }

    @Handler(op = InHeader.RW_MULTI_CHARGE_CANCEL_REQUEST)
    public static void handleRWMultiChargeCancelRequest(Char chr, InPacket inPacket) {
        byte size = inPacket.decodeByte();
        List<Integer> skills = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            skills.add(inPacket.decodeInt());
        }
        chr.getField().broadcast(UserRemote.requestRWMultiChargeCancel(chr, size, skills));
    }

    @Handler(op = InHeader.USER_CREATE_HOLIDOM_REQUEST)
    public static void handleUserCreateHolidomRequest(Char chr, InPacket inPacket) {
        Field field = chr.getField();

        inPacket.decodeInt(); //tick
        inPacket.decodeByte(); //unk
        int skillID = inPacket.decodeInt();
        inPacket.decodeInt(); //unk

        if (field.getAffectedAreas().stream().noneMatch(ss -> ss.getSkillID() == skillID)) {
            DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("Character %d tried to heal from Holy Fountain (%d) whilst there isn't any on the field.", chr.getId(), skillID));
            return;
        }
        if (skillID == Cannoneer.POOLMAKER_AA || skillID == Cannoneer.HEXA_POOLMAKER_AA) {
            SkillInfo si = SkillData.getSkillInfoById(skillID);
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            int slv = chr.getSkillLevel(skillID - 2);
            chr.heal(chr.getMaxHP() * si.getValue(SkillStat.x, slv) / 100);
            Option o = new Option();
            o.nValue = si.getValue(SkillStat.indieDamR, slv);
            o.nReason = skillID;
            o.tTerm = si.getValue(SkillStat.y, slv);
            tsm.sendStat(IndieDamR, o);
            chr.getField().removeAffectedArea(skillID, chr.getId());
        } else {
            chr.heal((int) (chr.getMaxHP() / ((double) 100 / 40)));
        }
    }

    @Handler(op = InHeader.REQUEST_INC_COMBO)
    public static void handleRequestIncCombo(Char chr, InPacket inPacket) {
        if (JobConstants.isAran(chr.getJob())) {
            Aran aranJobHandler = ((Aran) chr.getJobHandler());
            aranJobHandler.setCombo(aranJobHandler.getCombo() + 10);
        }
    }

    @Handler(op = InHeader.REQUEST_DEC_COMBO)
    public static void handleRequestDecCombo(Char chr, InPacket inPacket) {
        if (JobConstants.isAran(chr.getJob())) {
            Aran aranJobHandler = ((Aran) chr.getJobHandler());
            aranJobHandler.setCombo(aranJobHandler.getCombo() - 100);
        }
    }

    @Handler(op = InHeader.CYGNUS_SKILL_COMMAND_LOCK)
    public static void handleCygnusSkillCommandLock(Char chr, InPacket inPacket) {
        int skillID = inPacket.decodeInt();
        int questID = QuestConstants.SKILL_COMMAND_LOCK_ARAN;
        Quest quest = chr.getQuestById(questID);
        String lockID = "";
        switch (skillID) {
            case BlazeWizard.BLAZING_EXTINCTION:
                lockID = "fw0";
                break;
            case BlazeWizard.TOWERING_INFERNO:
                lockID = "fw2";
                break;
            case NightWalker.SHADOW_MOMENTUM:
                lockID = "nw0";
                break;
        }
        if (quest == null)
            chr.createQuestWithQRValue(questID, String.format("%d=1", lockID));
        else {
            if (quest.getProperty(lockID) == null)
                quest.setProperty(lockID, "1");
            else
                quest.setProperty(lockID, Integer.parseInt(quest.getProperty(lockID)) == 1 ? "0" : "1");

            chr.write(WvsContext.questRecordExMessage(quest));
        }
        chr.dispose();

    }

    @Handler(op = InHeader.SKILL_COMMAND_LOCK)
    public static void handleSkillCommandLock(Char chr, InPacket inPacket) {
        int skillID = inPacket.decodeInt();
        int questID = QuestConstants.SKILL_COMMAND_LOCK_ARAN;
        Quest quest = chr.getQuestById(questID);
        int lockID = -1;

        switch (skillID) {
            case Aran.COMBAT_STEP:
                lockID = 0;
                break;
            case Aran.SMASH_WAVE:
                lockID = 1;
                break;
            case Aran.FINAL_CHARGE:
                lockID = 2;
                break;
            case Aran.FINAL_TOSS:
                lockID = 3;
                break;
            case Aran.ROLLING_SPIN:
                lockID = 4;
                break;
            case Aran.FINAL_BLOW:
                lockID = 7;
                break;
            case Aran.JUDGEMENT_DRAW:
                lockID = 5;
                break;
            case Aran.GATHERING_HOOK:
                lockID = 6;
                break;
            case Aran.FINISHER_STORM_OF_FEAR:
                lockID = 8;
                break;
            case Aran.FINISHER_HUNTER_PREY:
                lockID = 9;
                break;
            default:
                questID = QuestConstants.SKILL_COMMAND_LOCK_ARK;
                quest = chr.getQuestById(questID);
                if (quest == null) {
                    chr.createQuestWithQRValue(questID, String.format("%d=1", skillID));
                } else {
                    if (quest.getProperty(skillID + "") == null) { // doesn't have the skill in qrValue yet
                        quest.setProperty(skillID + "", "1");
                    } else {
                        quest.setProperty(skillID + "", quest.getIntProperty(skillID + "") == 1 ? "0" : "1");
                    }
                    chr.write(WvsContext.questRecordExMessage(quest));
                }
                chr.write(UserPacket.skillOnOffEffect(chr.getId(), skillID));
                chr.dispose();
                return;
        }
        if (quest == null)
            chr.createQuestWithQRValue(questID, String.format("%d=1", lockID));
        else {
            if (quest.getProperty(lockID + "") == null)
                quest.setProperty(lockID + "", "1");
            else
                quest.setProperty(lockID + "", Integer.parseInt(quest.getProperty(lockID + "")) == 1 ? "0" : "1");

            chr.write(WvsContext.questRecordExMessage(quest));
        }
        chr.dispose();
    }

    @Handler(op = InHeader.REQUEST_SET_HP_BASE_DAMAGE)
    public static void handleRequestSetHpBaseDamage(Char chr, InPacket inPacket) {
        if (JobConstants.isDemonAvenger(chr.getJob())) {
            ((DemonAvenger) chr.getJobHandler()).sendHpUpdate();
        }
    }

    @Handler(op = InHeader.REQUEST_MATRIX_SUMMON_ATTACK)
    public static void handleRequestMatrixSummonAttack(Char chr, InPacket inPacket) {
        if (JobConstants.isDemonAvenger(chr.getJob())) {
            ((DemonAvenger) chr.getJobHandler()).giveDemonFrenzy();
        }
    }

    @Handler(op = InHeader.USER_REQUEST_FLYING_SWORD_START)
    public static void handleUserRequestFlyingSwordStart(Char chr, InPacket inPacket) {
        if (JobConstants.isKaiser(chr.getJob())) {
            ((Kaiser) chr.getJobHandler()).createFlyingSwordForceAtom(inPacket);
        }
    }

    @Handler(op = InHeader.USER_REQUEST_SET_OFF_TRINITY)
    public static void handleUserRequestSetOffTrinity(Char chr, InPacket inPacket) {
        if (JobConstants.isAngelicBuster(chr.getJob()) && chr.hasSkill(AngelicBuster.TRINITY)) {
            ((AngelicBuster) chr.getJobHandler()).rechargeABSkills();
        }
        chr.dispose();
    }

    @Handler(op = InHeader.USER_REQUEST_STEAL_SKILL_LIST)
    public static void handleUserRequestStealSkillList(Char chr, InPacket inPacket) {
        int targetChrID = inPacket.decodeInt();
        Char targetChr;
        Char serverBot = Server.get().bot;
        if (serverBot != null && targetChrID == serverBot.getId()) {
            targetChr = serverBot;
        } else {
            targetChr = chr.getField().getCharByID(targetChrID);
        }
        if (targetChr == null) {
            chr.chatPopup("找不到角色數據。");
            chr.dispose();
            return;
        }
        Collection<Skill> targetSkillsList = targetChr.getSkills();
        if (targetSkillsList.isEmpty()) {
            chr.chatPopup("該角色沒有可偷取的技能。");
            chr.dispose();
            return;
        }
        chr.write(UserLocal.resultStealSkillList(targetSkillsList, 4, targetChrID, targetChr.getJob()));
        chr.dispose();
    }

    @Handler(op = InHeader.USER_REQUEST_STEAL_SKILL_MEMORY)
    public static void handleUserRequestStealSkillMemory(Char chr, InPacket inPacket) {
        int stealSkillID = inPacket.decodeInt();
        int targetChrID = inPacket.decodeInt();
        boolean add = inPacket.decodeByte() != 0;   // 0 = add  |  1 = remove

        Char targetChr;
        Char serverBot = Server.get().bot;
        if (serverBot != null && targetChrID == serverBot.getId()) {
            targetChr = serverBot;
        } else {
            targetChr = chr.getField().getCharByID(targetChrID);
        }

        Skill stolenSkill = SkillData.getSkillDeepCopyById(stealSkillID);
        int stealSkillMaxLv = 0;
        if (stolenSkill != null) {
            stealSkillMaxLv = stolenSkill.getMasterLevel();
        }
        int stealSkillCurLv = targetChr == null ? stealSkillMaxLv : targetChr.getSkill(stealSkillID).getCurrentLevel();
        //TODO this is for testing,  needs to be:    targetChr.getSkillID(stealSkillID).getCurrentLevel();

        if (!add) {
            // /Add Stolen Skill

            if (chr.getStolenSkillBySkillId(stealSkillID) != null) {
                chr.chatMessage("你已經擁有該技能。");
                chr.dispose();
                return;
            }

            int position = StolenSkill.getFirstEmptyPosition(chr, stealSkillID);
            if (position == -1) {
                chr.dispose();
                return;
            }
            StolenSkill.setSkill(chr, stealSkillID, position, (byte) stealSkillCurLv);

            int positionPerTab = StolenSkill.getPositionForTab(position, stealSkillID);
            chr.write(UserLocal.changeStealMemoryResult(STEAL_SKILL.getVal(), SkillConstants.getStealSkillManagerTabFromSkill(stealSkillID), positionPerTab, stealSkillID, stealSkillCurLv, stealSkillMaxLv));
        } else {
            //Remove Stolen Skill
            int position = StolenSkill.getPositionPerTabFromStolenSkill(chr.getStolenSkillBySkillId(stealSkillID));
            StolenSkill.removeSkill(chr, stealSkillID);
            chr.write(UserLocal.changeStealMemoryResult(REMOVE_STEAL_MEMORY.getVal(), SkillConstants.getStealSkillManagerTabFromSkill(stealSkillID), position, stealSkillID, stealSkillCurLv, stealSkillMaxLv));
        }
        chr.dispose();
    }

    @Handler(op = InHeader.USER_REQUEST_SET_STEAL_SKILL_SLOT)
    public static void handleUserRequestSetStealSkillSlot(Char chr, InPacket inPacket) {
        int impeccableSkillID = inPacket.decodeInt();
        int stealSkillID = inPacket.decodeInt();

        ChosenSkill.setChosenSkill(chr, stealSkillID, impeccableSkillID);
        chr.write(UserLocal.resultSetStealSkill(true, impeccableSkillID, stealSkillID));
        chr.dispose();
    }

    @Handler(op = InHeader.ENTER_OPEN_GATE_REQUEST)
    public static void handleEnterOpenGateRequest(Char chr, InPacket inPacket) {
        int chrId = inPacket.decodeInt();
        Position position = inPacket.decodePosition();
        byte gateId = inPacket.decodeByte();

        // Probably needs remote player position handling
        chr.dispose(); // Necessary as going through the portal will stuck you
    }

    @Handler(op = InHeader.USER_SET_DRESS_CHANGED_REQUEST)
    public static void handleUserSetDressChangedRequest(Char chr, InPacket inPacket) {
        boolean on = inPacket.decodeByte() != 0;
        if (JobConstants.isAngelicBuster(chr.getJob())) {
            chr.write(UserLocal.setDressChanged(on, true));
        }
        chr.getField().broadcast(UserRemote.avatarModified(chr, AvatarModifiedMask.AvatarLook.getVal(), (byte) 0), chr);
    }

    @Handler(op = InHeader.USER_JAGUAR_CHANGE_REQUEST)
    public static void handleUserJaguarChangeRequest(Char chr, InPacket inPacket) {
        final int questID = QuestConstants.WILD_HUNTER_JAGUAR_STORAGE_ID;
        Quest quest = chr.getQuestById(questID);
        if (quest == null) {
            return;
        }
        quest.convertQRValueToProperties();
        int fromID = inPacket.decodeInt();
        int toID = inPacket.decodeInt();
        String value = quest.getProperty("" + (toID + 1));
        if (value != null) {
            WildHunterInfo whi = chr.getWildHunterInfo();
            whi.setIdx((byte) toID);
            whi.setRidingType((byte) toID);
            chr.write(WvsContext.wildHunterInfo(whi));
            // could make WildHunterInfo an entity for this
            Quest chosenQuest = chr.getQuestById(QuestConstants.WILD_HUNTER_JAGUAR_CHOSEN_ID);
            if (chosenQuest == null) {
                chosenQuest = new Quest(chr.getId(), QuestConstants.WILD_HUNTER_JAGUAR_CHOSEN_ID, QuestStatus.Started);
                chr.addQuest(chosenQuest);
            }
            chosenQuest.setQrValue("" + toID);
        } else {
            chr.chatMessage("你沒有可更換的豹。");
        }
    }

    @Handler(op = InHeader.USER_DRAGON_ACTION)
    public static void handleUserDragonActionRequest(Char chr, InPacket inPacket) {
        int attackAction = inPacket.decodeInt();
        int skillID = inPacket.decodeInt();
        int slv = inPacket.decodeInt();
        chr.getField().broadcast(UserRemote.dragonAction(chr, attackAction, skillID, slv), chr);
    }

    @Handler(op = InHeader.USER_DRAGON_BREATH_EARTH_EFFECT)
    public static void handleUserDragonBreathEarthEffectRequest(Char chr, InPacket inPacket) {
        int skillID = inPacket.decodeInt();
        int slv = inPacket.decodeInt();
        Position start = new Position(inPacket.decodeInt(), inPacket.decodeInt());
        Position end = new Position(inPacket.decodeInt(), inPacket.decodeInt());
        chr.getField().broadcast(UserRemote.dragonBreathEarthEffect(chr, skillID, slv, start, end), chr);
    }

    @Handler(op = InHeader.USER_SHADOW_MOMENTUM_REQUEST)
    public static void handleShadowMomentumRequest(Char chr, InPacket inPacket) {
        int skillID = inPacket.decodeInt();
        if (chr.hasSkill(skillID)) {
            if (skillID == NightWalker.SHADOW_MOMENTUM) {
                TemporaryStatManager tsm = chr.getTemporaryStatManager();
                SkillInfo si = SkillData.getSkillInfoById(skillID);
                int slv = chr.getSkillLevel(skillID);
                int oldVal = tsm.getOption(ShadowMomentum).nOption;
                Option o = new Option();
                o.nOption = Math.min(oldVal + 1, si.getValue(SkillStat.x, slv));
                o.rOption = skillID;
                o.tOption = si.getValue(SkillStat.time, slv);
                tsm.sendStat(ShadowMomentum, o);
            }
        }
    }
}
