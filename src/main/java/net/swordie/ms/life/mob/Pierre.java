package net.swordie.ms.life.mob;

import net.swordie.ms.connection.protocol.codec.MobPacketEncoder;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.connection.packet.FieldPacket;
import net.swordie.ms.connection.packet.MobPool;
import net.swordie.ms.life.mob.skill.MobSkill;
import net.swordie.ms.life.mob.skill.MobSkillID;
import net.swordie.ms.life.mob.skill.MobSkillStat;
import net.swordie.ms.life.movement.MovementInfo;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.loaders.containerclasses.MobSkillInfo;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Randomizer;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class Pierre {

    public static List<Position> hatPositionList = List.of(
            new Position(-400, 551),
            new Position(-300, 551),
            new Position(-200, 551),
            new Position(-100, 551),
            new Position(0, 551),
            new Position(100, 551),
            new Position(200, 551),
            new Position(300, 551),
            new Position(400, 551),
            new Position(500, 551),
            new Position(600, 551),
            new Position(700, 551),
            new Position(800, 551),
            new Position(900, 551),
            new Position(1000, 551),
            new Position(1100, 551),
            new Position(1200, 551),
            new Position(1300, 551),
            new Position(1400, 551),
            new Position(1500, 551)
    );

    public static List<Position> chaosHatPositionList = List.of(
            new Position(-400, 551),
            new Position(-350, 551),
            new Position(-300, 551),
            new Position(-250, 551),
            new Position(-200, 551),
            new Position(-150, 551),
            new Position(-100, 551),
            new Position(-50, 551),
            new Position(0, 551),
            new Position(50, 551),
            new Position(100, 551),
            new Position(150, 551),
            new Position(200, 551),
            new Position(250, 551),
            new Position(300, 551),
            new Position(350, 551),
            new Position(400, 551),
            new Position(450, 551),
            new Position(500, 551),
            new Position(550, 551),
            new Position(600, 551),
            new Position(650, 551),
            new Position(700, 551),
            new Position(750, 551),
            new Position(800, 551),
            new Position(850, 551),
            new Position(900, 551),
            new Position(950, 551),
            new Position(1000, 551),
            new Position(1050, 551),
            new Position(1100, 551),
            new Position(1150, 551),
            new Position(1200, 551),
            new Position(1250, 551),
            new Position(1300, 551),
            new Position(1350, 551),
            new Position(1400, 551),
            new Position(1450, 551),
            new Position(1500, 551)
    );

    public static boolean isPierre(int mobID) {
        return (mobID >= 8900100 && mobID <= 8900102) || (mobID >= 8900000 && mobID <= 8900002);
    }

    public static MobSkill getMobSkill(Mob mob, List<MobSkill> mobSkillList) {
        MobSkill mobSkill = null;
        if (mob.getHPPercent() <= 30 && !mob.isPierreSpawnTwice() && mob.getTemplateId() >= 8900000 && mob.getTemplateId() <= 8900002) {
            mobSkill = new MobSkill();
            mobSkill.setSpawnTwice(true);
            mobSkill.setSkillID(MobSkillID.Summon2.getVal());
            mobSkill.setLevel(40);
            mob.setLifeTime(System.currentTimeMillis());
            mob.setPierreSpawnTwice(true);
            return mobSkill;
        }
        if (mob.getHPPercent() <= 70 && (System.currentTimeMillis() - mob.getLifeTime()) >= 30000) {
            mobSkill = new MobSkill();
            mobSkill.setSpawnTwice(false);
            mobSkill.setSkillID(MobSkillID.Summon2.getVal());
            mobSkill.setLevel(40);
            mob.setLifeTime(System.currentTimeMillis());
            return mobSkill;
        } else {
            if (Util.succeedProp(20) && mobSkillList.size() > 0) {
                mobSkill = mobSkillList.get(Randomizer.nextInt(mobSkillList.size()));
                if (mobSkill.getSkillID() == MobSkillID.Summon2.getVal()) {
                    mobSkill = null;
                }
            }
        }
        return mobSkill;
    }

    public static int getNextPierre(int mobID, boolean isSpawnTwice) {
        if (isSpawnTwice) {
            return mobID == 8900001 ? 8900002 : 8900001;
        }
        boolean isSecond = Util.succeedProp(50);
        switch (mobID) {
            case 8900000 -> {
                return isSecond ? 8900002 : 8900001;
            }
            case 8900001 -> {
                return isSecond ? 8900002 : 8900000;
            }
            case 8900002 -> {
                return isSecond ? 8900001 : 8900000;
            }
            case 8900100 -> {
                return isSecond ? 8900102 : 8900101;
            }
            case 8900101 -> {
                return isSecond ? 8900102 : 8900100;
            }
            case 8900102 -> {
                return isSecond ? 8900101 : 8900100;
            }
            default -> {
                return 0;
            }
        }
    }

    public static void createHatFalling(Mob mob) {
        if (mob.getTemplateId() >= 8900000 && mob.getTemplateId() <= 8900002) {
            if (System.currentTimeMillis() - mob.getCapEffectTime() < 5000) {
                return;
            }
            if (mob.isPierreSpawnTwice()) {
                if (mob.getTemplateId() == 8900002) {
                    return; //Give hat control for Red
                }
            }
            List<Position> positionList = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                positionList.add(Util.getRandomFromCollection(chaosHatPositionList));
            }
            mob.getField().broadcast(FieldPacket.createFallingCatcher("CapEffect", 0, positionList.size(), positionList));
            positionList.clear();
            mob.setCapEffectTime(System.currentTimeMillis());
        } else if (mob.getTemplateId() >= 8900100 && mob.getTemplateId() <= 8900102) {
            if (System.currentTimeMillis() - mob.getCapEffectTime() < 5000) {
                return;
            }
            List<Position> positionList = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                positionList.add(Util.getRandomFromCollection(hatPositionList));
            }
            mob.getField().broadcast(FieldPacket.createFallingCatcher("CapEffect", 0, positionList.size(), positionList));
            positionList.clear();
            mob.setCapEffectTime(System.currentTimeMillis());
        }
    }

    public static void applyEffect(Char chr, MobSkill mobSkill, Mob mob) {
        MobTemporaryStat mts = mob.getTemporaryStat();
        short skill = (short) mobSkill.getSkillID();
        short slv = (short) mobSkill.getLevel();
        MobSkillInfo msi = SkillData.getMobSkillInfoByIdAndLevel(skill, slv);
        MobSkillID msID = MobSkillID.getMobSkillIDByVal(skill);
        Field field = mob.getField();

        //chr.chatMessage(String.format("[Mob Skill] Controller: %s | Mob ID: %d | %s (%d) | level = %d", chr.getName(), mob.getTemplateId(), msID, mobSkill.getSkillID(), mobSkill.getLevel()));
        switch (msID) {
            case Summon2 -> {
                boolean isRedHat = Util.succeedProp(50);
                for (Char charInField : field.getChars()) {
                    TemporaryStatManager tsm = charInField.getTemporaryStatManager();
                    Option option = new Option();
                    option.nOption = isRedHat ? 100 : 200;
                    option.slv = 1;
                    option.rOption = isRedHat ? MobSkillID.CapdebuffRed.getVal() : MobSkillID.CapdebuffBlue.getVal();
                    option.tOption = 120;
                    tsm.sendSetStatFromMobSkillPacket(CharacterTemporaryStat.CapDebuff, option);
                }
                boolean isFlip = mob.isFlip();
                boolean isSpawnTwice = mob.isPierreSpawnTwice();
                if (mobSkill.isSpawnTwice()) {
                    //Remove old Mob
                    mob.remove();
                    //Spawn 2 Pierre
                    List<Integer> pierreMobList = new ArrayList<>(List.of(8900001, 8900002));
                    if (mob.getHp() > 0) {
                        for (int i = 0; i < pierreMobList.size(); i++) {
                            Mob m = mob.getField().spawnPierre(pierreMobList.get(i), new Position(mob.getPosition().getX() + (i * 100), mob.getPosition().getY()), mob.getHp(), isFlip, System.currentTimeMillis());
                            m.setPierreSpawnTwice(isSpawnTwice);
                            m.setMobSpawnerId(mob.getObjectId());
                        }
                    }
                    pierreMobList.clear();
                } else {
                    int mobSpawn = getNextPierre(mob.getTemplateId(), isSpawnTwice);
                    //Remove old Mob
                    mob.remove();
                    //Spawn New Mob
                    if (mob.getHp() > 0) {
                        Mob m = mob.getField().spawnPierre(mobSpawn, mob.getPosition(), mob.getHp(), isFlip, System.currentTimeMillis());
                        m.setPierreSpawnTwice(isSpawnTwice);
                        m.setMobSpawnerId(mob.getObjectId());
                        if (isSpawnTwice) {
                            Option o1 = new Option();
                            o1.nOption = 17000;
                            o1.tOption = 210000000;
                            o1.slv = 1;
                            o1.rOption = MobSkillID.NearBuff.getVal();
                            m.getTemporaryStat().addMobSkillOptions(mob, MobStat.PAD, o1);

                            Option o2 = new Option();
                            o2.nOption = 17000;
                            o2.tOption = 210000000;
                            o1.slv = 1;
                            o2.rOption = MobSkillID.NearBuff.getVal();
                            m.getTemporaryStat().addMobSkillOptions(mob, MobStat.MAD, o2);
                        }
                    }
                }
            }
            case Teleport -> {
                mob.getTimer().addEvent(() -> {
                    Position position = chr.getPosition();
                    mob.setPosition(position);
                    mob.getField().broadcast(MobPool.teleportRequest(mob, false, 5, position));
                }, 3800);
            }
        }
    }

    public static void applyHitDebuff(Char chr, int mobID, TemporaryStatManager tsm, int skillID, int type) {
        if (!isPierre(mobID)) {
            return;
        }
        if (chr.getHP() < 0) {
            return;
        }
        switch (mobID) {
            case 8900000 -> {
                Option o = new Option();
                //use Umbrella to stab
                if (type == 0 && !tsm.hasStat(ReverseInput)) {
                    o.nOption = 1;
                    o.rOption = MobSkillID.ReverseInput.getVal();
                    o.slv = 11;
                    o.tOption = 2;
                    tsm.sendSetStatFromMobSkillPacket(ReverseInput, o);
                }
                //use Umbrella to push
                else if (type == 1 && !tsm.hasStat(Stun)) {
                    o.nOption = 1;
                    o.rOption = MobSkillID.Stun.getVal();
                    o.slv = 44;
                    o.tOption = 2;
                    tsm.sendSetStatFromMobSkillPacket(Stun, o);
                }
            }
            case 8900001 -> {
                Option o = new Option();
                if (type == 0 && !tsm.hasStat(Slow)) {
                    o.nOption = 1;
                    o.rOption = MobSkillID.Slow.getVal();
                    o.slv = 20;
                    o.tOption = 5;
                    tsm.sendSetStatFromMobSkillPacket(Slow, o);
                }
            }
            case 8900002, 8900102 -> {
                Option o = new Option();
                if (type == -1 && !tsm.hasStat(Seal)) {
                    o.nOption = 1;
                    o.rOption = MobSkillID.Seal.getVal();
                    o.slv = 11;
                    o.tOption = 3;
                    tsm.sendSetStatFromMobSkillPacket(Seal, o);
                }
            }
            case 8900100, 8900101 -> {
                Option o = new Option();
                if (type == 1 && !tsm.hasStat(Stun)) {
                    o.nOption = 1;
                    o.rOption = MobSkillID.Stun.getVal();
                    o.slv = 44;
                    o.tOption = 2;
                    tsm.sendSetStatFromMobSkillPacket(Stun, o);
                }
            }
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
        if (mob.getTemplateId() >= 8900001 && mob.getTemplateId() <= 8900002 && mob.getFirstPierreDieTime() != 0 && System.currentTimeMillis() - mob.getFirstPierreDieTime() >= 5000) {
            int mobSpawn = mob.getTemplateId() == 8900001 ? 8900002 : 8900001;
            mob.setFirstPierreDieTime(0);
            Mob m = mob.getField().spawnPierre(mobSpawn, mob.getPosition(), 10000000000L, mob.isFlip(), System.currentTimeMillis());
            m.setPierreSpawnTwice(mob.isPierreSpawnTwice());
            m.setMobSpawnerId(mob.getObjectId());
            mob.setLifeTime(System.currentTimeMillis());
        }
        //Give Buff if Blue And Red active
        if (field.hasMobById(8900001) && field.hasMobById(8900002)) {
            for (Mob mobInField : field.getMobs()) {
                if (!mobInField.getTemporaryStat().hasCurrentMobStat(MobStat.PAD)) {
                    Option o1 = new Option();
                    o1.nOption = 17000;
                    o1.tOption = 999999;
                    o1.slv = 1;
                    o1.rOption = MobSkillID.NearBuff.getVal();
                    mobInField.getTemporaryStat().addMobSkillOptions(mob, MobStat.PAD, o1);
                }
                if (!mobInField.getTemporaryStat().hasCurrentMobStat(MobStat.MAD)) {
                    Option o2 = new Option();
                    o2.nOption = 17000;
                    o2.tOption = 999999;
                    o2.slv = 1;
                    o2.rOption = MobSkillID.NearBuff.getVal();
                    mobInField.getTemporaryStat().addMobSkillOptions(mob, MobStat.MAD, o2);
                }
            }
        }
        createHatFalling(mob);
        if (mob.getTemplateId() == 8900002 || mob.getTemplateId() == 8900102) {
            Char controller = mob.getField().getCharByID(mob.getControllerID());
            if (controller != null) {
                controller.write(FieldPacket.chaseEffectSet(chr, mob));
            }
        }
        if (didSkill && mob.hasSkillDelayExpired() && !mob.isInAttack() && !suspend) {
            List<MobSkill> skillList = mob.getSkills();
            if (Util.succeedProp(100) && mob.getControllerID() != -1 && mob.getControllerID() == chr.getId()) {
                MobSkill mobSkill = null;
                skillList = skillList.stream().filter(ms -> mob.hasSkillOffCooldown(ms.getSkillID(), ms.getLevel())).collect(Collectors.toList());
                mobSkill = getMobSkill(mob, skillList);
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

        int forcedAttack = 0;
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
