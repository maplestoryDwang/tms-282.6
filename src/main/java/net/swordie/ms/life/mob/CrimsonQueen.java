package net.swordie.ms.life.mob;

import net.swordie.ms.connection.protocol.codec.MobPacketEncoder;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.connection.packet.MobPool;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.mob.skill.MobSkill;
import net.swordie.ms.life.mob.skill.MobSkillID;
import net.swordie.ms.life.mob.skill.MobSkillStat;
import net.swordie.ms.life.movement.MovementInfo;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.loaders.containerclasses.MobSkillInfo;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;
import static net.swordie.ms.life.mob.skill.MobSkillStat.*;

public class CrimsonQueen {

    public static boolean isCrimsonQueen(int mobID) {
        return mobID >= 8920000 && mobID <= 8920003 || mobID >= 8920100 && mobID <= 8920103;
    }

    public static List<Position> heartPosition = List.of(
            new Position(900, 135),
            new Position(800, 135),
            new Position(700, 135),
            new Position(600, 135),
            new Position(500, 135),
            new Position(400, 135),
            new Position(300, 135),
            new Position(200, 135),
            new Position(100, 135),
            new Position(0, 135),
            new Position(-100, 135),
            new Position(-200, 135),
            new Position(-300, 135),
            new Position(-400, 135),
            new Position(-500, 135),
            new Position(-600, 135),
            new Position(-700, 135),
            new Position(-800, 135)
    );

    public static int getForceAttack(Mob mob, int attackIndex) {
        if (mob.getTemplateId() >= 8920000 && mob.getTemplateId() <= 8920003) {
            switch (attackIndex) {
                case 2:
                    return 3;
                case 3:
                    return 4;
                case 4:
                    return 5;
                case 5:
                    return 6;
            }
        }
        return -1;
    }

    public static void applyMobSkillByAttackIndex(Char chr, Mob mob, int attackIndex) {
        if (mob.getTemplateId() == 8920001 && attackIndex == 3) {
            MobSkill mobSkill = new MobSkill();
            mobSkill.setSkillID(186);
            mobSkill.setLevel(2);
            applyEffect(chr, mobSkill, mob);
        }
    }

    public static void applyEffect(Char chr, MobSkill mobSkill, Mob mob) {
        MobTemporaryStat mts = mob.getTemporaryStat();
        short skill = (short) mobSkill.getSkillID();
        short slv = (short) mobSkill.getLevel();
        MobSkillInfo msi = SkillData.getMobSkillInfoByIdAndLevel(skill, slv);
        MobSkillID msID = MobSkillID.getMobSkillIDByVal(skill);
        Field field = mob.getField();
        switch (msID) {
            case PMCounter -> {
                mob.getTimer().addEvent(() -> {
                    Option o = new Option(skill);
                    o.slv = slv;
                    o.tOption = Util.getRandom(3, 10);//msi.getSkillStatIntValue(time);
                    Option o2 = new Option(skill);
                    o2.slv = slv;
                    o2.tOption = Util.getRandom(3, 10);//msi.getSkillStatIntValue(time);
                    if (!mts.hasCurrentMobStat(MobStat.PCounter) && !mts.hasCurrentMobStat(MobStat.MCounter)) {
                        o.nOption = msi.getSkillStatIntValue(x);
                        o.mOption = 100;
                        o.bOption = msi.getSkillStatIntValue(MobSkillStat.delay);
                        o.wOption = msi.getSkillStatIntValue(y);
                        mts.addMobSkillOptions(mob, MobStat.PCounter, o);
                        o2.nOption = msi.getSkillStatIntValue(x);
                        o2.mOption = 100;
                        o2.bOption = msi.getSkillStatIntValue(MobSkillStat.delay);
                        o2.wOption = msi.getSkillStatIntValue(y);
                        mts.addMobSkillOptions(mob, MobStat.MCounter, o2);
                    }
                }, 2, TimeUnit.SECONDS);
            }
            case Undead -> {
                for (Char charInField : chr.getField().getChars()) {
                    mobSkill.applyEffect(charInField);
                }
            }
            case FireBomb -> {
                for (Char charInField : mob.getField().getChars()) {
                    if (charInField != null) {
                        Option o = new Option(skill);
                        o.slv = slv;
                        o.tOption = Util.getRandom(3, 10);//msi.getSkillStatIntValue(time);
                        TemporaryStatManager tsm = charInField.getTemporaryStatManager();
                        o.nOption = tsm.getOption(FireBomb) != null ? tsm.getOption(FireBomb).nOption + 1 : 1;
                        o.rOption = MobSkillID.FireBomb.getVal();
                        o.slv = 2;
                        o.tOption = 10; //3 sec
                        tsm.sendSetStatFromMobSkillPacket(FireBomb, o);
                    }
                }
            }
            case AreaForce -> {
                boolean canCreate = true;
                for (var aaInField : chr.getField().getAffectedAreas()) {
                    if (aaInField.getSkillID() == skill) {
                        canCreate = false;
                        break;
                    }
                }
                if (!canCreate) {
                    return;
                }
                boolean isLeft = chr.getPosition().getX() - mob.getPosition().getX() >= 0;
                AffectedArea aa = AffectedArea.getAreaForce(mob, skill, slv, 2000);
                aa.setPosition(new Position(chr.getPosition().getX(), mob.getPosition().getY()));
                if (slv == 2) {
                    aa.setForce(isLeft ? 9275169 : 9175169);
                }
                aa.setRect(aa.getPosition().getRectAround(new Rect(msi.getLt(), msi.getRb())));
                field.spawnAffectedArea(aa);
            }
            case Summon2 -> {
                Rect rect;
                Position spawnPos = chr.getPosition();
                if (msi.getLt() != null) {
                    rect = new Rect(msi.getLt(), msi.getRb());
                    spawnPos = rect.getRandomPositionInside();
                }
                Set<Mob> spawnedMobs = field.getMobs().stream().filter(m -> m.getMobSpawnerId() == mob.getObjectId()).collect(Collectors.toSet());
                for (int mobId : msi.getInts()) {
                    long spawnedSize = spawnedMobs.stream().filter(m -> m.getTemplateId() == mobId).count();
                    int maxSpawned = 2;
                    if (mobId >= 8920000 && mobId <= 8920003 || mobId >= 8920100 && mobId <= 8920103) {
                        Position finalSpawnPos = mob.getPosition();
                        long hp = mob.getHp();
                        boolean flip = mob.isFlip();
                        long lastTimeChangeState = mob.getLastTimeChangeState();
                        if (System.currentTimeMillis() - lastTimeChangeState < 30000) {
                            continue;
                        }
                        mob.remove();
                        mob.getTimer().addEvent(() -> {
                            if (mob.getHp() > 0) {
                                Mob m = mob.getField().spawnMob(mobId, finalSpawnPos.getX(), finalSpawnPos.getY(), false, 0);
                                m.setHp(hp);
                                m.setMobSpawnerId(mob.getObjectId());
                                m.setFlip(flip);
                                m.setLastTimeChangeState(System.currentTimeMillis());
                            }
                        }, 3, TimeUnit.SECONDS);
                    } else if ((mobId == 8920004 || mobId == 8920104) && field.getMobs().size() < heartPosition.size()) { //Should be get mobs with heart ID but it fine
                        for (Position position : heartPosition) {
                            Mob m = mob.getField().spawnMob(mobId, position.getX(), position.getY(), false, 0);
                            m.setMobSpawnerId(mob.getObjectId());
                        }
                    } else {
                        if (spawnedSize < maxSpawned) {
                            try {
                                Mob m = mob.getField().spawnMob(mobId, spawnPos.getX(), spawnPos.getY(), false, 0);
                                m.setMobSpawnerId(mob.getObjectId());
                            } catch (NullPointerException e) {
                                for (Char member : mob.getField().getChars()) {
                                    if (member != null) {
                                        spawnPos = member.getPosition();
                                        break;
                                    }
                                }
                                Mob m = mob.getField().spawnMob(mobId, spawnPos.getX(), spawnPos.getY(), false, 0);
                                m.setMobSpawnerId(mob.getObjectId());
                            }
                        }
                    }
                }
            }
            default -> {

            }
        }
        //chr.chatMessage(String.format("[Mob Skill] Controller: %s | Mob ID: %d | %s (%d) | level = %d", chr.getName(), mob.getTemplateId(), msID, mobSkill.getSkillID(), mobSkill.getLevel()));
    }

    public static void applyHitDebuff(Char chr, int mobID, TemporaryStatManager tsm, int skillID, int type) {
        if (!isCrimsonQueen(mobID)) {
            return;
        }
        Option o = new Option();
        switch (mobID) {
            case 8920100:
            case 8920000: //Chaos Queen: Simmering (Lovey)
                if (skillID == 0 && type == 0 && !tsm.hasStat(Seal)) {
                    o.nOption = 1;
                    o.rOption = MobSkillID.Seal.getVal();
                    o.slv = 1;
                    o.tOption = 3; //3 sec
                    tsm.sendSetStatFromMobSkillPacket(Seal, o);
                }
                break;
            case 8920101:
            case 8920001: //Chaos Queen: Joyous (Crazy)
                if (skillID == 0 && type == 0 && !tsm.hasStat(ReverseInput)) {
                    o.nOption = 100;
                    o.rOption = MobSkillID.ReverseInput.getVal();
                    o.slv = 1;
                    o.tOption = 3; //3 sec
                    tsm.sendSetStatFromMobSkillPacket(ReverseInput, o);
                }
                break;
            case 8920102:
            case 8920002: //Chaos Queen: Wrathful (Neutral)
                if (skillID == 0 && type == 0 && !tsm.hasStat(Fear)) {
                    o.nOption = 100;
                    o.rOption = MobSkillID.Fear.getVal();
                    o.slv = 11;
                    o.tOption = 3; //3 sec
                    tsm.sendSetStatFromMobSkillPacket(Fear, o);
                }
                break;
            case 8920103:
            case 8920003: //Chaos Queen: Sorrowful (Sad)
                if (skillID == 0 && type == 0 && !tsm.hasStat(Poison)) {
                    o.nOption = 1;
                    o.rOption = MobSkillID.Poison.getVal();
                    o.slv = 39;
                    o.tOption = 5; //5 sec
                    tsm.sendSetStatFromMobSkillPacket(Poison, o);
                }
                break;
        }
    }

    public static void handleMobMove(Char chr, Field field, Mob mob, byte action, short moveID, MobSkillAttackInfo mobSkillAttackInfo, MovementInfo movementInfo) {
        int skillID = mobSkillAttackInfo.action - 30;
        int attackIndex = mobSkillAttackInfo.action - 12;
        int skillSN = skillID;
        int slv = 0;
        int afterAttack = -1;
        int afterAttackCount = 0;
        boolean didSkill = action != 0;
        boolean suspend = mob.getTemporaryStat().hasCurrentMobStat(MobStat.Stun) || mob.getTemporaryStat().hasCurrentMobStat(MobStat.Freeze);
        if (didSkill && mob.hasSkillDelayExpired() && !mob.isInAttack() && !suspend) {
            List<MobSkill> skillList = mob.getSkills();
            if (Util.succeedProp(20) && mob.getControllerID() != -1 && mob.getControllerID() == chr.getId()) {
                MobSkill mobSkill;
                mobSkill = skillList.stream().filter(ms -> ms.getSkillSN() == skillSN).findFirst().orElse(null);
                if (mobSkill == null) {
                    skillList = skillList.stream().filter(ms -> mob.hasSkillOffCooldown(ms.getSkillID(), ms.getLevel())).collect(Collectors.toList());
                    if (skillList.size() > 0) {
                        mobSkill = MobSkill.handleMobSkillLogic(mob, skillList);
                    }
                }
                didSkill = mobSkill != null;
                if (didSkill) {
                    skillID = mobSkill.getSkillID();
                    slv = mobSkill.getLevel();
                    mobSkill.setFlip(mobSkillAttackInfo.flip);
                    MobSkillInfo msi = SkillData.getMobSkillInfoByIdAndLevel(skillID, slv);
                    long curTime = System.currentTimeMillis();
                    long interval = msi.getSkillStatIntValue(MobSkillStat.interval) * 1000L;
                    long nextUseableTime = curTime + interval;
                    mob.putSkillCooldown(skillID, slv, nextUseableTime);
                    if (mobSkill.getSkillDelay() > 0) {
                        List<Rect> rects = new ArrayList<>();
                        mob.getSkillDelays().add(mobSkill);
                        mob.setSkillDelay(mobSkill.getSkillDelay());
                        chr.write(MobPool.setSkillDelay(mob, mobSkill.getSkillDelay(), msi, 0, rects));
                    } else {
                        applyEffect(chr, mobSkill, mob);
                    }
                }
            }
        }
        if (!didSkill && !suspend) {
            int attackIdx = skillID + 17;
            if (mob.hasAttackOffCooldown(attackIdx)) {
                MobSkill ms = mob.getAttackById(attackIdx);
                if (ms != null && ms.getAfterAttack() >= 0) {
                    afterAttack = ms.getAfterAttack();
                    afterAttackCount = ms.getAfterAttackCount();
                }
            }
        }

        int forcedAttack = getForceAttack(mob, attackIndex);
        applyMobSkillByAttackIndex(chr, mob, forcedAttack);
        mob.setInAttack(afterAttackCount >= 0);
        if (afterAttackCount >= 0) {
            chr.write(MobPool.setAfterAttack(mob.getObjectId(), (short) afterAttack, mobSkillAttackInfo.action, afterAttackCount, !mob.isFlip()));
        }
        field.messageByMob(mob, action);
        field.checkMobInAffectedAreas(mob);
        chr.write(MobPacketEncoder.active().controlAck(mob, true, moveID, skillID, slv, forcedAttack));
        field.broadcast(MobPacketEncoder.active().move(mob, mobSkillAttackInfo, movementInfo, action), chr);
    }
}
