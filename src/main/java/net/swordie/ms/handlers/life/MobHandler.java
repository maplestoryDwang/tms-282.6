package net.swordie.ms.handlers.life;

import net.swordie.ms.connection.protocol.codec.MobPacketEncoder;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.MobZoneDebuff;
import net.swordie.ms.client.character.SpiritSavior;
import net.swordie.ms.client.character.quest.Quest;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.cygnus.DawnWarrior;
import net.swordie.ms.client.jobs.nova.AngelicBuster;
import net.swordie.ms.client.jobs.resistance.Xenon;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.BossConstants;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.*;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.*;
import net.swordie.ms.life.mob.boss.demian.stigma.DemianStigma;
import net.swordie.ms.life.mob.boss.demian.stigma.DemianStigmaIncinerateObject;
import net.swordie.ms.life.mob.boss.demian.stigma.StigmaDeliveryType;
import net.swordie.ms.life.mob.boss.demian.sword.DemianFlyingSword;
import net.swordie.ms.life.mob.boss.demian.sword.DemianFlyingSwordPath;
import net.swordie.ms.life.mob.boss.demian.sword.DemianFlyingSwordPathIdx;
import net.swordie.ms.life.mob.boss.demian.sword.DemianFlyingSwordType;
import net.swordie.ms.life.mob.skill.MobSkill;
import net.swordie.ms.life.mob.skill.MobSkillID;
import net.swordie.ms.life.mob.skill.MobSkillStat;
import net.swordie.ms.life.mob.skill.SpiderWeb;
import net.swordie.ms.life.movement.MovementInfo;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.loaders.containerclasses.MobSkillInfo;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Randomizer;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.util.container.Tuple;
import net.swordie.ms.world.boss.BossHelper;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Foothold;
import net.swordie.ms.world.field.Instance;
import net.swordie.ms.world.field.Portal;
import net.swordie.ms.world.field.fieldeffect.FieldEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MobHandler {

    @Handler(op = InHeader.MOB_APPLY_CTRL)
    public static void handleMobApplyCtrl(Char chr, InPacket inPacket) {
        if (chr.getHP() <= 0) {
            chr.openUIOnDead();
        }
        applyMobCtrl(chr, inPacket.decodeInt());
    }

    /**
     * MOB_APPLY_CTRL game logic for a decoded object id (any protocol; TMS 282.3 decodes CP0517 in
     * Tms282MobHandlers): unknown objects are removed from the client, Magnus zone effects for its controller.
     */
    public static void applyMobCtrl(Char chr, int objectID) {
        Field field = chr.getField();
        Life life = field.getLifeByObjectID(objectID);
        if (life == null) {
            chr.write(MobPacketEncoder.active().leaveField(objectID, DeathType.NO_ANIMATION_DEATH));
            return;
        }
        if (!(life instanceof Mob mob)) {
            chr.write(MobPacketEncoder.active().leaveField(objectID, DeathType.NO_ANIMATION_DEATH));
            return;
        }
        if (mob.getHp() <= 0) {
            return;
        }
        switch (life.getTemplateId()) {
            case 8880000: // Hard Magnus
            case 8880002: // Normal magnus
            case 8880010: // Easy Magnus
                ScriptManagerImpl sm = chr.getScriptManager();
                if (chr.getId() == mob.getControllerID()) {
                    byte percent = (byte) ((mob.getHp() * 100) / mob.getMaxHp());
                    if (percent <= 30) {
                        sm.createObstacleAtom(ObtacleAtomEnum.PurpleMeteor, 1, BossConstants.MAGNUS_PURPLE_ATOM_DAMAGE, 2, 1, BossConstants.MAGNUS_PURPLE_ATOM_PROP);
                        sm.createObstacleAtom(ObtacleAtomEnum.BlueMeteor, 1, BossConstants.MAGNUS_BLUE_ATOM_DAMAGE, 3, 1, BossConstants.MAGNUS_BLUE_ATOM_PROP);
                        sm.createObstacleAtom(ObtacleAtomEnum.GreenMeteor, 1, BossConstants.MAGNUS_GREEN_ATOM_DAMAGE, BossConstants.MAGNUS_OBSTACLE_ATOM_VELOCITY, 2, BossConstants.MAGNUS_GREEN_ATOM_PROP);
                    } else if (percent <= 60) {
                        sm.createObstacleAtom(ObtacleAtomEnum.BlueMeteor, 1, BossConstants.MAGNUS_BLUE_ATOM_DAMAGE, BossConstants.MAGNUS_OBSTACLE_ATOM_VELOCITY, 1, BossConstants.MAGNUS_BLUE_ATOM_PROP);
                        sm.createObstacleAtom(ObtacleAtomEnum.GreenMeteor, 1, BossConstants.MAGNUS_GREEN_ATOM_DAMAGE, BossConstants.MAGNUS_OBSTACLE_ATOM_VELOCITY, 4, BossConstants.MAGNUS_PURPLE_ATOM_PROP);
                    } else {
                        sm.createObstacleAtom(ObtacleAtomEnum.GreenMeteor, 1, BossConstants.MAGNUS_GREEN_ATOM_DAMAGE, BossConstants.MAGNUS_OBSTACLE_ATOM_VELOCITY, 5, BossConstants.MAGNUS_PURPLE_ATOM_PROP);
                    }
                }
                MobZoneDebuff mobZoneDebuff = chr.getMobZoneDebuff();
                if (mobZoneDebuff == null) {
                    chr.setMobZoneDebuff(new MobZoneDebuff(chr));
                    return;
                }
                MobZoneInfo mobZoneInfo = mob.getMobZone();
                if (mobZoneInfo == null) {
                    return;
                }
                Rect rect = mob.getMobZone().getMobZoneRect().get(mob.getCurZoneDataType());
                if (rect == null) {
                    return;
                }
                if (mob.getRectAround(rect).hasPositionInside(chr.getPosition())) {
                    if (mobZoneDebuff.getDamage() != mobZoneInfo.getIn().getDamage()) {
                        mobZoneDebuff.setDamage(mobZoneInfo.getIn().getDamage());
                    }
                    if (mobZoneDebuff.getHealRate() != mobZoneInfo.getIn().getHealRate()) {
                        mobZoneDebuff.setHealRate(mobZoneInfo.getIn().getHealRate());
                    }
                } else {
                    int hp = (int) (chr.getMaxHP() * (mobZoneInfo.getOut().getAutoDec() / 10000.0D));
                    chr.damage(hp);
                    if (mobZoneDebuff.getDamage() != mobZoneInfo.getOut().getDamage()) {
                        mobZoneDebuff.setDamage(mobZoneInfo.getOut().getDamage());
                    }
                    if (mobZoneDebuff.getHealRate() != mobZoneInfo.getOut().getHealRate()) {
                        mobZoneDebuff.setHealRate(mobZoneInfo.getOut().getHealRate());
                    }
                }
                break;
        }
    }

    @Handler(op = InHeader.MOB_MOVE)
    public static void handleMobMove(Char chr, InPacket inPacket) {
        if (chr == null || chr.getField() == null) {
            // client already disconnected / not fully in field yet
            return;
        }
        moveMob(chr, MobMoveRequestDecoder.GMS_265.decode(inPacket));
    }

    /**
     * MOB_MOVE game logic for a decoded request (any protocol; TMS 282.3 decodes CP0516 in Tms282MobHandlers):
     * position and move action, mob skills, controller change, MOB_CONTROL_ACK to the controller and MOB_MOVE to
     * the rest of the field, all through {@link MobPacketEncoder#active()}.
     */
    public static void moveMob(Char chr, MobMoveRequest request) {
        if (chr.getHP() <= 0) {
            chr.openUIOnDead();
        }
        Field field = chr.getField();
        int objectID = request.objectId();
        Life life = field.getLifeByObjectID(objectID);
        if (life == null) {
            chr.write(MobPacketEncoder.active().leaveField(objectID, DeathType.NO_ANIMATION_DEATH));
            return;
        }
        if (!(life instanceof Mob mob)) {
            chr.write(MobPacketEncoder.active().leaveField(objectID, DeathType.NO_ANIMATION_DEATH));
            return;
        }
        if (mob.getHp() <= 0) {
            return;
        }
        MobSkillAttackInfo mobSkillAttackInfo = request.attackInfo();
        short moveID = request.moveId();
        boolean changeController = request.changeController();
        int nextMobSkillID = 0;
        int nextMobSkillSLV = 0;
        byte action = request.action();

        life.setMoveAction(action);

        final int skillID = mobSkillAttackInfo.skillID;
        final int slv = mobSkillAttackInfo.slv;

        MovementInfo movementInfo = request.path();
        movementInfo.applyTo(mob);
        //BossHelper.changePhase(mob);
        BossHelper.setBlockAttack(mob);
        if ((mob.getTemplateId() == 8645009 || mob.getTemplateId() == 8645066) && (action == 30 || action == 31)) {
            field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.BossDarknell, "來試試看吧！這是連星辰都能劈開的究極之劍！", 3000));
        }
        if (CrimsonQueen.isCrimsonQueen(mob.getTemplateId())) {
            CrimsonQueen.handleMobMove(chr, field, mob, action, moveID, mobSkillAttackInfo, movementInfo);
        } else if (Pierre.isPierre(mob.getTemplateId())) {
            Pierre.handleMobMove(chr, field, mob, action, moveID, mobSkillAttackInfo, movementInfo);
        } else {
            MobSkill nextMobSkill = Util.getRandomFromCollection(mob.getSkills());
            int afterAttack = -1;
            int afterAttackCount = 0;
            boolean didSkill = skillID != 0;
            int attackIndex = mobSkillAttackInfo.action - 12;
            boolean suspend = mob.getTemporaryStat().hasCurrentMobStat(MobStat.Stun) || mob.getTemporaryStat().hasCurrentMobStat(MobStat.Freeze);
            boolean isVellumBreath = false;

            if ((skillID != 0 && !suspend
                    || (skillID == MobSkillID.Lucid.getVal() && Util.succeedProp(chr.getLucidMode() != -1 ? (chr.getLucidMode() * 5 + 5) : 100)))
                    && mob.getControllerID() == chr.getId()) {
                List<MobSkill> skillList = mob.getSkills();
                if (mob.getControllerID() != -1 && mob.getControllerID() == chr.getId()) {
                    MobSkill mobSkill;
                    mobSkill = skillList.stream().filter(ms -> ms.getSkillSN() == skillID && ms.getLevel() == slv).findFirst().orElse(null);
                    if (mobSkill == null) {
                        skillList = skillList.stream().filter(ms -> mob.hasSkillOffCooldown(ms.getSkillID(), ms.getLevel())).collect(Collectors.toList());
                        if (skillList.size() > 0) {
                            mobSkill = MobSkill.handleMobSkillLogic(mob, skillList);
                            if (BossConstants.isVellum(mob.getTemplateId())) {
                                if (attackIndex >= 3 && attackIndex <= 5 || attackIndex >= 14 && attackIndex <= 16 || attackIndex == 8 || mob.getHPPercent() >= 60) {
                                    mobSkill = null;
                                }
                            }
                            //Vellum Breath.
                            if (mobSkill != null && mobSkill.getSkillID() == MobSkillID.Teleport.getVal()) {
                                isVellumBreath = true;
                            }
                        }
                    }
                    didSkill = mobSkill != null;
                    if (didSkill) {
                        mobSkill.setFlip(mobSkillAttackInfo.flip);
                        MobSkillInfo msi = SkillData.getMobSkillInfoByIdAndLevel(skillID, slv);
                        if (msi != null) {
                            long curTime = System.currentTimeMillis();
                            long interval = msi.getSkillStatIntValue(MobSkillStat.interval) * 1000L;
                            long nextUseableTime = curTime + interval;
                            mob.putSkillCooldown(skillID, slv, nextUseableTime);
                            ZakumSkillsHandler.onSkillActionDisplay(chr, mob, mobSkill);
                            if (mobSkill.getSkillDelay() > 0) {
                                List<Rect> rects = new ArrayList<>();
                                MobSkillID msID = MobSkillID.getMobSkillIDByVal(skillID);
                                if (msID == MobSkillID.Toos) {
                                    Rect rectangle = new Rect(-49, -1016, 211, -16);
                                    rects.add(rectangle);
                                    rects.add(rectangle.moveRight().moveRight());
                                    rects.add(rectangle.moveLeft().moveLeft());
                                }
                                mob.getSkillDelays().add(mobSkill);
                                mob.setSkillDelay(mobSkill.getSkillDelay());
                                if (mob.getOwner() != null) {
                                    mob.getOwner().write(MobPool.setSkillDelay(mob, mobSkill.getSkillDelay(), msi, 0, rects));
                                } else {
                                    mob.getField().broadcast(MobPool.setSkillDelay(mob, mobSkill.getSkillDelay(), msi, 0, rects));
                                }
                            }
                            if (BossConstants.isZakumMob(mob.getTemplateId())) {
                                mobSkill.zakumApplyEffect(mob);
                            } else if (BossConstants.isVellum(mob.getTemplateId())) {
                                mobSkill.vellumApplyEffect(mob);
                            } else {
                                mobSkill.applyEffect(mob);
                            }
                        }
                    }
                }
            }
            if (!didSkill) {
                int attackIdx = skillID + 17;
                if (mob.hasAttackOffCooldown(attackIdx)) {
                    MobSkill ms = mob.getAttackById(attackIdx);
                    if (ms != null && ms.getAfterAttack() >= 0) {
                        afterAttack = ms.getAfterAttack();
                        afterAttackCount = ms.getAfterAttackCount();
                    }
                }
            }

            if (mob.getControllerID() != -1 && mob.getControllerID() != chr.getId()) {
                if (!changeController) {
                    chr.write(MobPacketEncoder.active().changeController(objectID));
                    return;
                }
                mob.setControllerID(chr.getId());
                mob.notifyControllerChange();
            }
            if (nextMobSkill != null && skillID == 0 && mob.hasSkillDelayExpired() && Util.succeedProp(40)) {
                nextMobSkillID = nextMobSkill.getSkillID();
                nextMobSkillSLV = nextMobSkill.getLevel();
            }
            if (mob.getControllerID() == chr.getId()) {
                chr.write(MobPacketEncoder.active().controlAck(mob, true, moveID, nextMobSkillID, nextMobSkillSLV, 0));
            }
            int forcedAttack = MobSkill.getForceAttack(mob, attackIndex, isVellumBreath);
            MobSkill.handleMobSkillByAttackIndex(chr, mob, forcedAttack);
            mob.setInAttack(afterAttackCount >= 0);
            if (afterAttackCount >= 0) {
                chr.write(MobPool.setAfterAttack(mob.getObjectId(), (short) afterAttack, mobSkillAttackInfo.action, afterAttackCount, !mob.isFlip()));
            }
            field.messageByMob(mob, action);
            field.checkMobInAffectedAreas(mob);
            if (mob.isBoss()) {
                //MobSkill.handleDarkSight(mob);
            }
            field.broadcast(MobPacketEncoder.active().move(mob, mobSkillAttackInfo, movementInfo, action), chr);
        }
    }

    @Handler(op = InHeader.MOB_SKILL_DELAY_END)
    public static void handleMobSkillDelayEnd(Char chr, InPacket inPacket) {
        int objectID = inPacket.decodeInt();
        Life life = chr.getField().getLifeByObjectID(objectID);
        if (life == null) {
            chr.write(MobPacketEncoder.active().leaveField(objectID, DeathType.NO_ANIMATION_DEATH));
            return;
        }
        if (!(life instanceof Mob mob)) {
            chr.write(MobPacketEncoder.active().leaveField(objectID, DeathType.NO_ANIMATION_DEATH));
            return;
        }
        if (mob.getHp() <= 0) {
            return;
        }
        int skillID = inPacket.decodeInt();
        int slv = inPacket.decodeInt();
        int remainCount = 0; // only set in MobDelaySkill::UpdateSequenceMode
        if (inPacket.decodeByte() != 0) {
            remainCount = inPacket.decodeInt();
        }
        List<MobSkill> delays = mob.getSkillDelays();
        MobSkill mobSkill = null;// = Util.findWithPred(delays, skill -> skill.getSkillID() == skillID && skill.getLevel() == slv);
        for (MobSkill skill : delays) {
            if (skill.getSkillID() == skillID && skill.getLevel() == slv) {
                mobSkill = skill;
                break;
            }
        }
        if (mobSkill != null) {// && chr.getParty().isLeader(chr)) {
            if (skillID == MobSkillID.ReturnTeleport.getVal()) {
                return;
            }
            if (BossConstants.isZakumMob(mob.getTemplateId())) {
                mobSkill.zakumApplyEffect(mob);
                ZakumSkillsHandler.onFuseArmSkillEffect(chr, mob, mobSkill);
            } else if (CrimsonQueen.isCrimsonQueen(mob.getTemplateId())) {
                CrimsonQueen.applyEffect(chr, mobSkill, mob);
            } else if (Pierre.isPierre(mob.getTemplateId())) {
                Pierre.applyEffect(chr, mobSkill, mob);
            } else {
                mobSkill.applyEffect(mob);
            }
        }
    }


    @Handler(op = InHeader.USER_BAN_MAP_BY_MOB)
    public static void handleBanMapByMob(Char chr, InPacket inPacket) {
        Field field = chr.getField();
        int mobID = inPacket.decodeInt();
        Life life = field.getLifeByTemplateId(mobID);
        if (!(life instanceof Mob mob)) {
            chr.write(MobPacketEncoder.active().leaveField(mobID, DeathType.NO_ANIMATION_DEATH));
            return;
        }
        if (mob.getHp() <= 0) {
            return;
        }
        if (mob.isBanMap()) {
            if (mob.getBanType() == 1) {
                if (mob.getBanMsgType() == 1) { // found 2 types (1(most of ban types), 2).
                    String banMsg = mob.getBanMsg();
                    if (banMsg != null && !banMsg.equals("")) {
                        chr.write(WvsContext.message(MessageType.SYSTEM_MESSAGE, 0, banMsg, (byte) 0));
                    }
                }
                Tuple<Integer, String> banMapField = mob.getBanMapFields().get(0);
                if (banMapField != null) {
                    Field toField = chr.getOrCreateFieldByCurrentInstanceType(banMapField.getLeft());
                    if (toField == null) {
                        return;
                    }
                    Portal toPortal = toField.getPortalByName(banMapField.getRight());
                    if (toPortal == null) {
                        toPortal = toField.getPortalByName("sp");
                    }

                    chr.warp(toField, toPortal);
                }
            }
        }
    }

    @Handler(op = InHeader.MOB_EXPLOSION_START)
    public static void handleMobExplosionStart(Char chr, InPacket inPacket) {
        int mobID = inPacket.decodeInt();
        int charID = inPacket.decodeInt();
        int skillID = inPacket.decodeInt(); //tick
        if (JobConstants.isXenon(chr.getJob()) && chr.hasSkill(Xenon.TRIANGULATION)) {
            skillID = Xenon.TRIANGULATION;
        } else if (JobConstants.isDawnWarrior(chr.getJob()) && chr.hasSkill(DawnWarrior.IMPALING_RAYS)) {
            skillID = DawnWarrior.IMPALING_RAYS_EXPLOSION;
        } else if (JobConstants.isAngelicBuster(chr.getJob()) && chr.hasSkill(AngelicBuster.LOVELY_STING)) {
            skillID = AngelicBuster.LOVELY_STING_EXPLOSION;
        } else {
            System.out.printf("Unhandled mob explosion skill ID %d for your job.%n", skillID);
            return;
        }
        Mob mob = (Mob) chr.getField().getLifeByObjectID(mobID);
        if (mob != null) {
            if (mob.getHp() <= 0) {
                return;
            }
            MobTemporaryStat mts = mob.getTemporaryStat();
            if ((mts.hasCurrentMobStat(MobStat.Explosion) && mts.getCurrentOptionsByMobStat(MobStat.Explosion).wOption == chr.getId())
                    || (mts.hasCurrentMobStat(MobStat.SoulExplosion) && mts.getCurrentOptionsByMobStat(MobStat.SoulExplosion).wOption == chr.getId())) {
                chr.write(UserLocal.explosionAttack(skillID, mob.getPosition(), mobID, 1));

                if (mts.hasCurrentMobStat(MobStat.SoulExplosion) && skillID == DawnWarrior.IMPALING_RAYS_EXPLOSION) {
                    mts.removeMobStat(mob, MobStat.SoulExplosion);
                } else if (mts.hasCurrentMobStat(MobStat.Explosion)) {
                    mts.removeMobStat(mob, MobStat.Explosion);
                }
            }
        } else {
            chr.getField().broadcast(MobPacketEncoder.active().leaveField(mobID, DeathType.NO_ANIMATION_DEATH));
        }
    }

    @Handler(op = InHeader.MOB_LIFTING_END)
    public static void handleMobLiftingEnd(Char chr, InPacket inPacket) {
        inPacket.decodeByte(); // idk
        short x = inPacket.decodeShort();
        inPacket.decodeByte(); // 0?
        chr.dispose();
    }

    @Handler(op = InHeader.MOB_UPDATE_FIXED_MOVE_DIR)
    public static void handleMobUpdateFixedMoveDir(Char chr, InPacket inPacket) {
        int objectID = inPacket.decodeInt();
        Life life = chr.getField().getLifeByObjectID(objectID);
        if (life == null) {
            chr.write(MobPacketEncoder.active().leaveField(objectID, DeathType.NO_ANIMATION_DEATH));
            return;
        }
        if (life instanceof Mob mob) {
            Foothold fixedFootHold = mob.getField().findFootHoldBelow(mob.getPosition());
            if (fixedFootHold != null) {
                mob.teleport(0, fixedFootHold.getX1(), fixedFootHold.getY1());
            } else {
                mob.teleport(0, 0, 0);
            }
        }
        chr.dispose();
    }


    @Handler(op = InHeader.USER_REQUEST_CHANGE_MOB_ZONE_STATE)
    public static void handleUserRequestChangeMobZoneState(Char chr, InPacket inPacket) {
        int objectID = inPacket.decodeInt();
        Position pos = inPacket.decodePositionInt();
        Life life = chr.getField().getLifeByObjectID(objectID);
        if (life == null) {
            chr.write(MobPacketEncoder.active().leaveField(objectID, DeathType.NO_ANIMATION_DEATH));
            return;
        }
        if (life instanceof Mob mob) {
            int dataType = 0;
            switch (life.getTemplateId()) {
                case 8880000: // Hard Magnus
                case 8880002: // Normal magnus
                case 8880010: // Easy Magnus
                case 8880100: // Hard Damien
                case 8880110: // Hard Damien
                case 8880120: // Normal Damien
                case 8880130: // Normal Damien
                {
                    double perc = mob.getHp() / (double) mob.getMaxHp();
                    if (perc <= 0.25) {
                        dataType = 4;
                    } else if (perc <= 0.5) {
                        dataType = 3;
                    } else if (perc <= 0.75) {
                        dataType = 2;
                    } else {
                        dataType = 1;
                    }
                    if (dataType != mob.getPhase()) {
                        mob.setPhase(dataType);
                        mob.setCurZoneDataType(dataType);
                        mob.getField().broadcast(FieldPacket.changePhase(mob, false));
                        chr.getField().broadcast(FieldPacket.changeMobZone(objectID, dataType));
                        chr.getField().broadcast(MobPool.registerRelMobZone(mob, objectID, dataType));
                    }
                    break;
                }
                case 8880101: // Hard Damien
                case 8880111: // Hard Damien
                case 8880121: // Normal Damien
                case 8880131: // Normal Damien
                {
                    List<Mob> zones = mob.getField().getMobs().stream().filter(m -> m.getTemplateId() == 8880102).collect(Collectors.toList());
                    if (zones.isEmpty()) {
                        zones.add(mob.getField().spawnMob(8880102, mob.getX(), mob.getY(), false, 0));
                    }
                    double percent = mob.getHp() / (double) mob.getMaxHp();
                    if (percent <= 10) {
                        dataType = 4;
                    } else if (percent <= 15) {
                        dataType = 3;
                    } else if (percent <= 20) {
                        dataType = 2;
                    } else {
                        dataType = 1;
                    }
                    if (dataType != mob.getPhase()) {
                        mob.setPhase(dataType);
                        mob.setCurZoneDataType(dataType);
                        mob.getField().broadcast(FieldPacket.changePhase(mob, false));
                        mob.getField().broadcast(FieldPacket.changeMobZone(mob.getObjectId(), dataType));
                        for (Mob zone : zones) {
                            mob.getField().broadcast(MobPool.registerRelMobZone(mob, zone.getObjectId(), dataType));
                        }
                    }
                    chr.getScriptManager().onDamienZoneHandle(chr, mob);
                    break;
                }
            }
            chr.write(UserLocal.serverAckMobZoneStateChange());
        }
    }

    @Handler(ops = InHeader.MOB_HIT_BY_MOB)
    public static void handleMobHitByMob(Char chr, InPacket inPacket) {
        int attackIdx = inPacket.decodeInt();
        inPacket.decodeInt();
        int mobID = inPacket.decodeInt();
        Field field = chr.getField();
        Mob mob = (Mob) field.getLifeByObjectID(mobID);
        if (mob != null) {
            if (mob.getHp() <= 0) {
                return;
            }
            int damage = Randomizer.nextInt((int) (((mob.getMaxHp() / 13 + mob.getPdr() * 10)) * 2 + 500)) / 10;
            if (mob.getHp() - damage < 1) {     // friendly dies
                mob.removeWithAnimation();
                if (mob.getTemplateId() == GameConstants.MOON_BUNNY) {  // Moon Bunny
                    field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.MoonBunny, "月妙因爲生病回家了。", 7000));
                    chr.getPartyQuestManager().getPartyQuest().end(chr);
                } else if (mob.getTemplateId() == 9300138) {   // Romeo (Brady)
                    field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.RomeoNPC, "羅密歐在戰鬥中昏倒了。", 7000));
                    chr.getPartyQuestManager().getPartyQuest().end(chr);
                } else if (mob.getTemplateId() == 9300137) {   // Juliet (Brittany)
                    field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.JulietNPC, "朱麗葉在戰鬥中昏倒了。", 7000));
                    chr.getPartyQuestManager().getPartyQuest().end(chr);
                } else if (mob.getTemplateId() == 9300275) { // Shammos
                    field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.ShammosPQ, "沙莫斯在戰鬥中倒下了。", 7000));
                    chr.getPartyQuestManager().getPartyQuest().end(chr);
                } else if (mob.getTemplateId() == 9300460) { // Kenta
                    field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.KentaPQ, "肯塔在戰鬥中倒下了。", 7000));
                    chr.getPartyQuestManager().getPartyQuest().end(chr);
                } else if (mob.getTemplateId() == 9400322 || mob.getTemplateId() == 9400327 || mob.getTemplateId() == 9400332) { // snowman
                    field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.Snowflakes, "雪人在激烈戰鬥中融化了。", 7000));
                    // Christmas Event
                }
                chr.dispose();
                return;
            }
            mob.damageByMob(damage);
            chr.dispose();
        } else {
            chr.write(MobPacketEncoder.active().leaveField(mobID, DeathType.NO_ANIMATION_DEATH));
        }
    }

    @Handler(ops = InHeader.MOB_ATTACK_MOB)
    public static void handleMobAttackMob(Char chr, InPacket inPacket) {
        int attackIdx = inPacket.decodeInt();
        inPacket.decodeInt();
        int mobID = inPacket.decodeInt();
        boolean magic = inPacket.decodeByte() == 0;
        inPacket.decodeByte();
        inPacket.decodeByte();
        long damage = inPacket.decodeLong();
        inPacket.decodeByte();
        inPacket.decodeInt();
        inPacket.decodeInt();
        inPacket.decodeShort();
        inPacket.decodeShort();
        inPacket.decodeByte();
        Field field = chr.getField();
        Mob mob = (Mob) field.getLifeByObjectID(mobID);
        if (mob != null) {
            // TODO:
        } else {
            chr.write(MobPacketEncoder.active().leaveField(mobID, DeathType.NO_ANIMATION_DEATH));
        }
    }

    @Handler(ops = {InHeader.MOB_SELF_DESTRUCT, InHeader.MOB_SELF_DESTRUCT_COLLISION_GROUP})
    public static void handleMobSelfDestruct(Char chr, InPacket inPacket) {
        int mobID = inPacket.decodeInt();
        Field field = chr.getField();
        Mob mob = (Mob) field.getLifeByObjectID(mobID);
        if (mob != null) {
            if (mob.getHp() <= 0) {
                return;
            }
            int templateId = mob.getTemplateId();
            Position pos = mob.getPosition();
            switch (templateId) {
                case 8880160:
                case 8880170:
                case 8880180:
                case 8880182:
                case 8880192:
                case 8880194:
                    // Nightmare Golem
                    field.removeMob(mobID);
                    mob.getField().spawnMob(templateId + 1, pos.getX(), pos.getY(), false, 0);
                    break;
                case 8644201: {
                    // Spirit Debris
                    int life = Integer.parseInt(chr.getQRValueByKey(16215, "life"));
                    chr.setQRValueByKey(16215, "life", "" + (life - 5));
                    field.removeMob(mobID);
                    if (Integer.parseInt(chr.getQRValueByKey(16215, "life")) <= 0 && chr.getFieldID() == SpiritSavior.SPIRIT_SAVIOR_MAP) {
                        chr.getSpiritSavior().end();
                    }
                    break;
                }
                case 8644301:
                case 8644302:
                case 8644303:
                case 8644304:
                case 8644305: {
                    // Toxic Stalker
                    Instance instance = chr.getInstance();
                    ScriptManagerImpl sm = chr.getScriptManager();
                    int life = Integer.parseInt(chr.getQRValueByKey(16215, "life"));
                    chr.setQRValueByKey(16215, "life", "" + (life - 50));
                    int x = 0;
                    for (Summon summon : chr.getField().getSummons()) {
                        if (summon.getSkillID() == 80002310 && summon.getOwnerId() == chr.getId()) {
                            chr.getField().removeLife(summon.getObjectId(), false);
                            x++;
                        }
                    }
                    if (x == 5) {
                        sm.showWeatherNotice("岩石精靈遭到猛毒的精靈攻擊了！", WeatherEffNoticeType.Arcana);
                        sm.showEffect("Map/Effect3.img/savingSpirit/failed");
                    }
                    chr.setQRValueByKey(16215, "chase", "0");
                    field.removeMob(mobID);
                    if (Integer.parseInt(chr.getQRValueByKey(16215, "life")) <= 0 && chr.getFieldID() == SpiritSavior.SPIRIT_SAVIOR_MAP) {
                        chr.getSpiritSavior().end();
                    }
                    break;
                }
                case 8644101:
                case 8644102:
                case 8644103:
                case 8644104:
                case 8644105:
                case 8644106:
                case 8644107:
                case 8644108:
                case 8644109:
                case 8644110:
                case 8644111:
                case 8644112: {
                    // Bound Rock Spirit
                    ScriptManagerImpl sm = chr.getScriptManager();
                    int life = Integer.parseInt(chr.getQRValueByKey(16215, "life"));
                    chr.setQRValueByKey(16215, "life", "" + (life - 5));
                    sm.showWeatherNotice("猛毒的精靈越來越強了……！", WeatherEffNoticeType.Arcana);
                    field.removeMob(mobID);
                    if (Integer.parseInt(chr.getQRValueByKey(16215, "life")) <= 0 && chr.getFieldID() == SpiritSavior.SPIRIT_SAVIOR_MAP) {
                        chr.getSpiritSavior().end();
                    }
                    break;
                }
                default:
                    int hpDamage = chr.getMaxHP() * Util.getRandom(1, 5) / 1000;
                    if (hpDamage > 0) {
                        chr.damage(hpDamage);
                    }
                    field.removeMob(mobID);
                    break;
            }
        } else {
            chr.write(MobPacketEncoder.active().leaveField(mobID, DeathType.NO_ANIMATION_DEATH));
        }
    }

    @Handler(op = InHeader.MOB_REQUEST_ESCORT_INFO)
    public static void handleMobRequestEscortInfo(Char chr, InPacket inPacket) {
        Field field = chr.getField();
        int objectID = inPacket.decodeInt();
        Life life = field.getLifeByObjectID(objectID);
        if (life == null) {
            chr.write(MobPacketEncoder.active().leaveField(objectID, DeathType.NO_ANIMATION_DEATH));
            return;
        }
        if (!(life instanceof Mob mob)) {
            chr.write(MobPacketEncoder.active().leaveField(objectID, DeathType.NO_ANIMATION_DEATH));
            return;
        }
        if (mob.isEscortMob()) {
            if (mob.getTemplateId() == 8230000) { // [Grand Athenaeum] Ariant : Escort Hatsar's Servant
                mob.addEscortDest(-1616, 233, -1);
                mob.addEscortDest(1898, 233, 0);
                mob.escortFullPath(-1);
                chr.write(FieldPacket.removeBlowWeather());
                chr.write(FieldPacket.blowWeather(5120118, "很高興你來了，" + chr.getName() + "！請幫忙趕走這些討厭的傢伙。", 7000, null));
            } else if (mob.getTemplateId() == 9300460) { // Kenta PQ
                mob.addEscortDest(413, 35, 0);
                mob.escortFullPath(-1);
                chr.write(FieldPacket.removeBlowWeather());
                chr.write(FieldPacket.blowWeather(5120118, "很高興你來了，" + chr.getName() + "！請幫忙趕走這些討厭的傢伙。", 7000, null));
            }
        }
    }

    @Handler(op = InHeader.MOB_ESCORT_COLLISION)
    public static void handleMobEscortCollision(Char chr, InPacket inPacket) {
        Field field = chr.getField();
        int objectID = inPacket.decodeInt();
        Life life = field.getLifeByObjectID(objectID);
        if (life == null) {
            chr.write(MobPacketEncoder.active().leaveField(objectID, DeathType.NO_ANIMATION_DEATH));
            return;
        }
        if (!(life instanceof Mob mob)) {
            chr.write(MobPacketEncoder.active().leaveField(objectID, DeathType.NO_ANIMATION_DEATH));
            return;
        }
        int collision = inPacket.decodeInt();

        EscortDest escortDest = mob.getEscortDest().get(collision - 1);
        if (escortDest != null) {
            // mob movement don't updating mob position so I disabled it until it will.
            /*if (escortDest.getDestPos().getX() != mob.getPosition().getX() || escortDest.getDestPos().getY() != mob.getPosition().getY()) {
             return;
             }*/

            if (mob.getTemplateId() == 8230000) {
                if (collision == 1) {
                    chr.write(FieldPacket.removeBlowWeather());
                    chr.write(FieldPacket.blowWeather(5120118, "很高興你來了，" + chr.getName() + "！請幫忙趕走這些討厭的傢伙。", 7000, null));
                } else if (collision == 2) {
                    chr.write(FieldPacket.fieldEffect(FieldEffect.getFieldEffectFromWz("quest/party/clear", 0)));
                    chr.write(FieldPacket.fieldEffect(FieldEffect.playSound("Party1/Clear", 100)));
                    chr.write(FieldPacket.removeBlowWeather());
                    chr.write(FieldPacket.blowWeather(5120118, "看來大家都平安抵達了。趁那些討厭的傢伙還沒再來找麻煩，快離開這裡吧。", 7000, null));
                    Quest quest = chr.getQuestById(32628);
                    if (quest == null) {
                        quest = new Quest(chr.getId(), 32628, QuestStatus.Started);
                        chr.addQuest(quest);
                    }
                    quest.setProperty("guard1", "1");// needed to complete quest
                    chr.write(WvsContext.message(MessageType.QUEST_RECORD_EX_MESSAGE, quest.getQRKey(), quest.getQRValue(), (byte) 0));
                }
            }
            mob.setCurrentDestIndex(collision);
            if (collision == mob.getEscortDest().size()) {
                mob.clearEscortDest();// finished escort
            }
        }
    }

    @Handler(op = InHeader.DEMIAN_OBJECT_NODE_END)
    public static void handleDemianObjectNodeEnd(Char chr, InPacket inPacket) {
        Field field = chr.getField();

        int objectID = inPacket.decodeInt();
        Life life = field.getLifeByObjectID(objectID);
        if (life == null) {
            return;
        }
        if (!(life instanceof DemianFlyingSword flyingSword)) {
            return;
        }
        short pathIdx = inPacket.decodeShort(); // pathIdx
        short nodeIdx = inPacket.decodeShort(); // nodeIdx that just ended
        Position swordPosition = inPacket.decodePositionInt(); // sword position
        Position targetChrPosition = inPacket.decodePositionInt(); // targeted character position

        flyingSword.setPosition(swordPosition);
        DemianFlyingSwordPathIdx path = DemianFlyingSwordPathIdx.getByVal(pathIdx);
        if (nodeIdx == flyingSword.getDemianFlyingSwordPath().getNodes().size() - 2 || path.equals(DemianFlyingSwordPathIdx.Creation)) {
            switch (path) {
                case Creation: // Creation -> Bouncing1 or Bouncing2
                    flyingSword.setDemianFlyingSwordPath(DemianFlyingSwordPath.flyingSwordBouncingPath(DemianFlyingSwordPath.flyingSwordPathBouncing2));
                    flyingSword.getTimer().addEvent(flyingSword::startPath, 1800, TimeUnit.MILLISECONDS);
                    break;
                case Bouncing1: // Bouncing -> Targeting
                case Bouncing2: // Bouncing -> Targeting
                    flyingSword.setDemianFlyingSwordPath(DemianFlyingSwordPath.flyingSwordTargetingPath(targetChrPosition));
                    flyingSword.startPath();
                    break;
                case Targeting: // Targeting -> AA and FlyingSwordTarget OutPacket
                    flyingSword.target();

                    // Create Affected Area
                    Mob mob = flyingSword.getOwner();
                    if (mob == null) {
                        return;
                    }
                    MobSkillInfo msi = SkillData.getMobSkillInfoByIdAndLevel(131, 28);
                    AffectedArea aa = AffectedArea.getMobAA(mob, (short) 131, (short) 28, SkillData.getMobSkillInfoByIdAndLevel(131, 28));
                    Rect rect = new Rect(msi.getLt(), msi.getRb());
                    Position position = new Position(swordPosition.getX(), 16);
                    aa.setPosition(position);
                    aa.setRect(position.getRectAround(rect));
                    aa.setOption(5);
                    aa.setLinkingSkillID(flyingSword.getObjectId());
                    field.spawnAffectedArea(aa);
                    break;
            }

        }
    }

    @Handler(op = InHeader.DEMIAN_OBJECT_ERR__RECREATE)
    public static void handleDemianObjectErrRecreate(Char chr, InPacket inPacket) {
        Field field = chr.getField();

        DemianFlyingSword sword = (DemianFlyingSword) field.getLifeByObjectID(inPacket.decodeInt());
        DemianFlyingSwordType type = DemianFlyingSwordType.getValBy(inPacket.decodeInt());
        if (sword == null) {
            return;
        }
        field.removeLife(sword);

        sword.startPath();
        sword.target();
        Mob mob = field.getMobs().stream().findFirst().orElse(null);
        if (mob != null) {
            DemianFlyingSword newSword = DemianFlyingSword.createDemianFlyingSword(chr, mob);
            newSword.setSwordType(type);
            newSword.setPosition(sword.getPosition());
            newSword.setDemianFlyingSwordPath(sword.getDemianFlyingSwordPath());
            field.spawnLife(newSword, null);
            newSword.startPath();
            newSword.target();
        }

    }

    @Handler(op = InHeader.STIGMA_DELEVERY_REQUEST)
    public static void handleStigmaDeliveryRequest(Char chr, InPacket inPacket) {
        Field field = chr.getField();

        int stigmaDelivery = inPacket.decodeInt();
        int unk = inPacket.decodeInt();
        int chrId = inPacket.decodeInt();
        StigmaDeliveryType stigmaDeliveryType = StigmaDeliveryType.getValBy(stigmaDelivery);

        if (stigmaDeliveryType.equals(StigmaDeliveryType.Success)) {
            DemianStigmaIncinerateObject o = (DemianStigmaIncinerateObject) field.getLifes().values().stream().filter(l -> l instanceof DemianStigmaIncinerateObject).findFirst().orElse(null);
            if (o == null) {
                return;
            }
            field.removeLife(o); // remove pillar
            DemianStigma.resetStigma(chr); // reset stigma
            field.broadcast(DemianFieldPacket.stigmaEffect(chr.getId(), false)); // show stigma reset effect
        }
    }

    @Handler(op = InHeader.MOB_AREA_ATTACK_DISEASE)
    public static void handleMobAreaAttackDisease(Char chr, InPacket inPacket) {
        int objectId = inPacket.decodeInt();
        int attackId = inPacket.decodeInt();
        Position areaPos = inPacket.decodePositionInt();
        inPacket.decodeInt(); // v80 + pMobAttackInfo->tAttackAfter
        Life life = chr.getField().getLifeByObjectID(objectId);
        if (life == null) {
            return;
        }
        Mob mob = (Mob) life;
        if (mob != null) {
            if (chr.getPosition().equals(areaPos)) {
                chr.damage((int) (chr.getMaxHP() * 0.05D));
            }
        }
    }

    @Handler(op = InHeader.MOB_DAMAGE_SHARE_INFO)
    public static void handleMobDamageShareInfo(Char chr, InPacket inPacket) {
        inPacket.decodeInt();
        inPacket.decodeByte();
    }

    @Handler(op = InHeader.USER_DAZZLE_HIT)
    public static void handleUserDazzleHit(Char chr, InPacket inPacket) {
        int mobID = inPacket.decodeInt();
        if (chr.getHP() > 0) {
            chr.damage((int) (chr.getMaxHP() * 0.05D));
        }
    }

    @Handler(op = InHeader.MOB_CREATE_AFFECTED_AREA)
    public static void handleMobCreateAffectedArea(Char chr, InPacket inPacket) {
        int objectId = inPacket.decodeInt();
        int snKey = inPacket.decodeInt();
        int mobSkillID = inPacket.decodeInt();
        int mobSLV = inPacket.decodeInt();
        int ptInstallX = inPacket.decodeInt();
        int ptInstallY = inPacket.decodeInt();
        byte unk = inPacket.decodeByte();
        Life life = chr.getField().getLifeByObjectID(objectId);
        if (life == null) {
            return;
        }
        Mob mob = (Mob) life;
        if (mob != null) {
            MobSkillInfo msi = SkillData.getMobSkillInfoByIdAndLevel(mobSkillID, mobSLV);
            if (msi != null) {
                Rect rect = new Rect(msi.getLt(), msi.getRb());
                AffectedArea aa = AffectedArea.getMobAA(mob, mobSkillID, mobSLV, msi);
                Position position = new Position(ptInstallX, ptInstallY);
                aa.setPosition(position);
                aa.setRect(position.getRectAround(rect));
                aa.setOption(snKey);
                mob.getField().spawnAffectedArea(aa);
            }
        }
    }

    @Handler(op = InHeader.MOB_BLACK_HAND)
    public static void handleBlackHand(Char chr, InPacket inPacket) {
        Mob mob = (Mob) chr.getField().getLifeByObjectID(inPacket.decodeInt());
        if (mob != null) {
            int skillLevel = inPacket.decodeInt();
            int id = inPacket.decodeInt();
            Position pos = inPacket.decodePosition();
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            if (chr.getId() == id && !tsm.hasStat(CharacterTemporaryStat.Stun)) {
                Rect rect = new Rect(inPacket.decodeInt(), inPacket.decodeInt(), inPacket.decodeInt(), inPacket.decodeInt());
                if (skillLevel == 2) {
                    skillLevel = 1;
                }
                int act_x = 0, apply_x = 0;
                boolean minus = false;
                int[] randXs = {0, 280, 560, 840};
                pos.setX(pos.getX() - 50);
                if (pos.getX() < 0) {
                    pos.setX(pos.getX() * -1);
                    minus = true;
                }
                int[] arrayOfInt1;
                int i;
                byte b;
                for (arrayOfInt1 = randXs, i = arrayOfInt1.length, b = 0; b < i; ) {
                    int randX = arrayOfInt1[b];
                    int calc_x = pos.getX() - randX;
                    if (calc_x < 0) {
                        calc_x *= -1;
                    }
                    if (act_x == 0) {
                        act_x = calc_x;
                    }
                    if (calc_x < act_x) {
                        act_x = calc_x;
                        apply_x = randX;
                    }
                    b++;
                }
                pos.setX(apply_x);
                if (minus) {
                    pos.setX(pos.getX() * -1);
                }

                // Give Stun
                Option o = new Option();
                o.nOption = 1;
                o.rOption = MobSkillID.Stun.getVal();
                o.slv = 83;
                o.tOption = 2;
                tsm.sendSetStatFromMobSkillPacket(CharacterTemporaryStat.Stun, o);
                chr.getField().broadcast(MobPool.spirit(mob, chr.getId(), rect, pos, skillLevel));
                if (chr.getDeathCount() == 1) {
                    for (int j = 0; j < chr.getVHDeathCount().length; j++) {
                        if (chr.getVHDeathCount()[j]) {
                            chr.getVHDeathCount()[j] = false;
                            break;
                        }
                    }
                    chr.setDeathCount(0);
                    chr.showDeathCount(0);
                    chr.openUIOnDead();
                } else {
                    for (int j = 0; j < chr.getVHDeathCount().length; j++) {
                        if (chr.getVHDeathCount()[j]) {
                            chr.getVHDeathCount()[j] = false;
                            break;
                        }
                    }
                    chr.setDeathCount(chr.getDeathCount() - 1);
                    chr.showDeathCount(chr.getDeathCount());
                    if (chr.getField().getLightCandles() < chr.getField().getCandles()) {
                        chr.getField().setLightCandles(chr.getField().getLightCandles() + 1);
                        chr.getField().broadcast(VerusHillaPacket.encode(1, chr, chr.getField()));
                    }
                }
                chr.write(VerusHillaPacket.encode(3, chr, chr.getField()));
                chr.getField().broadcast(VerusHillaPacket.encode(10, chr, chr.getField()));
                if (chr.getField().getCandles() == chr.getField().getLightCandles() && chr.getField().getReqTouched() == 0) {
                    chr.getField().setReqTouched(30);
                    chr.getField().broadcast(VerusHillaPacket.encode(6, chr, chr.getField()));
                    chr.getField().broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.BossVerusHilla,
                            "在祭壇按下採集鍵，就能在真希拉靠近之前奪回靈魂！", 8000));
                }
            }
        }
    }

    @Handler(op = InHeader.WILL_USE_MOON_GAUGE)
    public static void handleWillUseMoonGauge(Char chr, InPacket inPacket) {
        if (chr == null) return;

        Field field = chr.getField();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (chr.getFieldID() == 450008150 || chr.getFieldID() == 450008750) {
            String name = (chr.getPosition().getY() > -1000) ? "ptup" : "ptdown";
            chr.setMoonGauge(Math.max(0, chr.getMoonGauge() - 45));
            chr.write(WillPacket.addMoonGauge(chr.getMoonGauge()));
            field.broadcast(WillPacket.teleport());
            field.broadcast(UserLocal.portalTeleport(name));
            field.broadcast(UserRemote.effect(chr.getId(), Effect.skillUse(80002421, (byte) 1, 0)));
        } else if (chr.getFieldID() == 450008250 || chr.getFieldID() == 450008850) {
            if (tsm.hasStatBySkillId(80002404)) {
                tsm.removeStatsBySkill(80002404);
            }
            chr.setMoonGauge(Math.max(0, chr.getMoonGauge() - 50));
            chr.write(WillPacket.addMoonGauge(chr.getMoonGauge()));
            chr.write(WillPacket.cooldownMoonGauge(7000));
            chr.getTimer().addEvent(() -> field.broadcast(UserRemote.effect(chr.getId(), Effect.skillUse(80002404, (byte) 1, 0))), 7000L);
        } else if (chr.getFieldID() == 450008350 || chr.getFieldID() == 450008950) {
            chr.setMoonGauge(Math.max(0, chr.getMoonGauge() - 5));
            chr.setClearSpiderWeb(2);
            chr.write(WillPacket.addMoonGauge(chr.getMoonGauge()));
            chr.write(WillPacket.cooldownMoonGauge(5000));
            chr.getTimer().addEvent(() -> chr.setClearSpiderWeb(0), 5000L);
        }
        chr.dispose();
    }

    @Handler(op = InHeader.WILL_BEHOLDER_EYES_HIT)
    public static void handleWillBeholderEyesHit(Char chr, InPacket inPacket) {
        if (chr == null) return;

        if (chr.getMoonGauge() > 0) {
            // was Math.min(0, ...): any eye hit zeroed the whole gauge (or made it negative)
            // bossmech2-0930 (research/tms2823/boss-mech-0930/patches/MobHandler-will-eyes.diff)
            chr.setMoonGauge(Math.max(0, chr.getMoonGauge() - 10));
            chr.write(WillPacket.addMoonGauge(chr.getMoonGauge()));
        }
        chr.dispose();
    }

    @Handler(op = InHeader.WILL_SPIDER_WEB_HIT)
    public static void handleWillSpiderWebHit(Char chr, InPacket inPacket) {
        if (chr == null) return;

        Field field = chr.getField();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int objectID = inPacket.decodeInt();
        SpiderWeb spiderWeb = (SpiderWeb) field.getLifeByObjectID(objectID);
        if (spiderWeb == null) {
            chr.dispose();
            return;
        }
        if (chr.getClearSpiderWeb() > 0) {
            field.removeLife(spiderWeb);
        } else if (!tsm.hasStat(CharacterTemporaryStat.NotDamaged) && chr.getHP() > 0) {
            chr.damage((int) (chr.getMaxHP() * 0.1D));
            boolean isSeal = inPacket.decodeByte() == 1;
            if (isSeal) {
                Option o = new Option(MobSkillID.Seal.getVal());
                o.nOption = 1;
                o.rOption = MobSkillID.Seal.getVal();
                o.slv = 40;
                o.tOption = 5;
                tsm.sendSetStatFromMobSkillPacket(CharacterTemporaryStat.Seal, o);
            }
        }
    }

    @Handler(op = InHeader.MOB_CREATE_FIRE_WALK)
    public static void handleMobCreateFireWalk(Char chr, InPacket inPacket) {
        int mobID = inPacket.decodeInt();
        int skillID = inPacket.decodeInt();
        int slv = inPacket.decodeInt();
        Position position = inPacket.decodePosition();

        //chr.chatMessage("Skill ID: " + skillID + ", slv: " + slv + ", Pos: " + position);
        Life life = chr.getField().getLifeByObjectID(mobID);
        if (life instanceof Mob mob) {
            //Blue Pierre Should define at somewhere not use id like this
            if (mob.getTemplateId() == 8900002 || mob.getTemplateId() == 8900102) {
                if (skillID == MobSkillID.PassiveFirewalk.getVal()) {
                    skillID = MobSkillID.AreaPoison.getVal();
                    slv = mob.getTemplateId() == 8900002 ? 16 : 18;
                }
                MobSkillInfo mobSkillInfo = SkillData.getMobSkillInfoByIdAndLevel(skillID, slv);
                AffectedArea affectedArea = AffectedArea.getMobAAWithPosition(mob, skillID, slv, position.getRectAround(new Rect(mobSkillInfo.getLt(), mobSkillInfo.getRb())), 6000);
                chr.getField().spawnAffectedArea(affectedArea);
            }
        }
    }
}
