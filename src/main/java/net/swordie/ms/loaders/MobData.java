package net.swordie.ms.loaders;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.constants.BossHpConstants;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.life.drop.DropInfo;
import net.swordie.ms.life.mob.ForcedMobStat;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobZoneInfo;
import net.swordie.ms.life.mob.skill.MobSkill;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.container.Tuple;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import net.swordie.ms.util.Util;
import net.swordie.ms.util.XMLApi;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static net.swordie.ms.ServerConstants.version;
import static net.swordie.ms.life.mob.MobStat.*;

public class MobData {
    private static final boolean LOG_UNKS = false;
    private static final int NORMAL_MOBS_CACHE_MAGIC = 0x4E4D4269;
    private static final Int2ObjectMap<Mob> mobs = new Int2ObjectOpenHashMap<>();
    private static final List<Mob> normalMobs = new ArrayList<>();
    private static final List<MobZoneInfo> mobZoneInfos = new ArrayList<>();

    public static void generateDatFiles() {
        System.out.println("Started generating mob data.");
        long start = System.currentTimeMillis();
        loadMobZoneInfoFromWZ();
        loadMobsFromWz();
        QuestData.linkMobData();
        saveToFile(ServerConstants.DAT_DIR + "/mobs");
        System.out.printf("Completed generating mob data in %dms.%n", System.currentTimeMillis() - start);
    }

    public static Mob getMobById(int templateID) {
        if (templateID <= 0) {
            return null;
        }
        Mob mob = mobs.get(templateID);
        if (mob != null) {
            return mob;
        }
        synchronized (mobs) {
            mob = mobs.get(templateID);
            if (mob != null) {
                return mob;
            }
            mob = loadMobFromFile(templateID);
            if (mob == null) {
                mob = createFallbackBossMob(templateID);
            }
            if (mob != null) {
                cacheMob(templateID, mob);
            }
            return mob;
        }
    }

    private static void cacheMob(int templateID, Mob mob) {
        int cacheId = mob.getTemplateId() > 0 ? mob.getTemplateId() : templateID;
        if (cacheId <= 0) {
            return;
        }
        try {
            mobs.put(cacheId, mob);
        } catch (Exception e) {
            System.err.printf("Failed to cache mob %d: %s%n", cacheId, e.getMessage());
        }
    }

    private static Mob createFallbackBossMob(int templateID) {
        long hp = BossHpConstants.getMaxHp(templateID);
        if (hp <= 0) {
            return null;
        }
        Mob mob = new Mob(templateID);
        ForcedMobStat fms = mob.getForcedMobStat();
        fms.setLevel(200);
        fms.setMaxHP(hp);
        fms.setMaxMP(10_000);
        fms.setPad(1_000);
        fms.setMad(1_000);
        fms.setPdr(50);
        fms.setMdr(50);
        fms.setExp(0);
        mob.setBoss(true);
        mob.setHp(hp);
        mob.setMaxHp(hp);
        mob.setAppearType((byte) -2);
        mob.setAfterAttack(-1);
        mob.setCurrentAction(-1);
        mob.setEliteGrade(-1);
        mob.setMoveAction((byte) 5);
        return mob;
    }

    public static Mob getMobDeepCopyById(int templateID) {
        Mob from = getMobById(templateID);
        Mob copy = null;
        if (from != null) {
            copy = from.deepCopy();
        } else {
            System.out.println("Could not find mob " + templateID);
        }
        return copy;
    }

    private static void saveToFile(String dir) {
        Util.makeDirIfAbsent(dir);
        for (Mob mob : mobs.values()) {
            File file = new File(dir + "/" + mob.getTemplateId() + ".dat");
            try (DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(file))) {
                ForcedMobStat fms = mob.getForcedMobStat();
                MobTemporaryStat mts = mob.getTemporaryStat();
                dataOutputStream.writeInt(mob.getTemplateId());
                dataOutputStream.writeInt(fms.getLevel());
                dataOutputStream.writeInt(mob.getFirstAttack());
                dataOutputStream.writeInt(mts.getCurrentOptionsByMobStat(BodyAttack) != null ? mts.getCurrentOptionsByMobStat(BodyAttack).nOption : 0);
                dataOutputStream.writeLong(fms.getMaxHP());
                dataOutputStream.writeLong(fms.getMaxMP());
                dataOutputStream.writeInt(fms.getPad());
                dataOutputStream.writeInt(fms.getPdr());
                dataOutputStream.writeInt(fms.getMad());
                dataOutputStream.writeInt(fms.getMdr());
                dataOutputStream.writeInt(fms.getAcc());
                dataOutputStream.writeInt(fms.getEva());
                dataOutputStream.writeLong(fms.getPushed());
                dataOutputStream.writeLong(fms.getExp());
                dataOutputStream.writeInt(mob.getSummonType());
                dataOutputStream.writeInt(mob.getCategory());
                dataOutputStream.writeUTF(mob.getMobType());
                dataOutputStream.writeInt(mob.getLink());
                dataOutputStream.writeInt(fms.getSpeed());
                dataOutputStream.writeDouble(mob.getFs());
                dataOutputStream.writeUTF(mob.getElemAttr());
                dataOutputStream.writeInt(mob.getHpTagColor());
                dataOutputStream.writeInt(mob.getHpTagBgcolor());
                dataOutputStream.writeBoolean(mob.isHPgaugeHide());
                dataOutputStream.writeBoolean(mob.isBoss());
                dataOutputStream.writeBoolean(mob.isUndead());
                dataOutputStream.writeBoolean(mob.isNoRegen());
                dataOutputStream.writeBoolean(mob.isInvincible());
                dataOutputStream.writeBoolean(mob.isHideName());
                dataOutputStream.writeBoolean(mob.isHideHP());
                dataOutputStream.writeBoolean(mob.isChangeable());
                dataOutputStream.writeBoolean(mob.isNoFlip());
                dataOutputStream.writeBoolean(mob.isTower());
                dataOutputStream.writeBoolean(mob.isPartyBonusMob());
                dataOutputStream.writeBoolean(mob.isUseReaction());
                dataOutputStream.writeBoolean(mob.isPublicReward());
                dataOutputStream.writeBoolean(mob.isMinion());
                dataOutputStream.writeBoolean(mob.isForward());
                dataOutputStream.writeBoolean(mob.isRemoteRange());
                dataOutputStream.writeBoolean(mob.isIgnoreFieldOut());
                dataOutputStream.writeBoolean(mob.isIgnoreMoveImpact());
                dataOutputStream.writeBoolean(mob.isSkeleton());
                dataOutputStream.writeBoolean(mob.isHideUserDamage());
                dataOutputStream.writeBoolean(mob.isIndividualReward());
                dataOutputStream.writeBoolean(mob.isNotConsideredFieldSet());
                dataOutputStream.writeBoolean(mob.isNoDoom());
                dataOutputStream.writeBoolean(mob.isUseCreateScript());
                dataOutputStream.writeBoolean(mob.isBlockUserMove());
                dataOutputStream.writeBoolean(mob.isKnockback());
                dataOutputStream.writeBoolean(mob.isRemoveQuest());
                dataOutputStream.writeBoolean(mob.isOnFieldSetSummon());
                dataOutputStream.writeBoolean(mob.isUserControll());
                dataOutputStream.writeBoolean(mob.isNoDebuff());
                dataOutputStream.writeBoolean(mob.isTargetFromSvr());
                dataOutputStream.writeUTF(mob.getChangeableMobType());
                dataOutputStream.writeInt(mob.getRareItemDropLevel());
                dataOutputStream.writeInt(mob.getHpRecovery());
                dataOutputStream.writeInt(mob.getMpRecovery());
                dataOutputStream.writeInt(mob.getMbookID());
                dataOutputStream.writeInt(mob.getChaseSpeed());
                dataOutputStream.writeInt(mob.getExplosiveReward());
                dataOutputStream.writeInt(mob.getCharismaEXP());
                dataOutputStream.writeInt(mob.getFlySpeed());
                dataOutputStream.writeInt(mob.getWp());
                dataOutputStream.writeInt(mob.getSummonEffect());
                dataOutputStream.writeInt(mob.getFixedDamage());
                dataOutputStream.writeInt(mob.getRemoveAfter());
                dataOutputStream.writeInt(mob.getBodyDisease());
                dataOutputStream.writeInt(mob.getBodyDiseaseLevel());
                dataOutputStream.writeInt(mob.getPoint());
                dataOutputStream.writeInt(mob.getPartyBonusR());
                dataOutputStream.writeInt(mob.getPassiveDisease());
                dataOutputStream.writeInt(mob.getCoolDamage());
                dataOutputStream.writeInt(mob.getCoolDamageProb());
                dataOutputStream.writeInt(mob.getDamageRecordQuest());
                dataOutputStream.writeInt(mob.getSealedCooltime());
                dataOutputStream.writeInt(mob.getWillEXP());
                dataOutputStream.writeUTF(mob.getFixedMoveDir());
                dataOutputStream.writeBoolean(mob.isEscortMob());
                dataOutputStream.writeBoolean(mob.isBanMap());
                if (mob.isBanMap()) {
                    dataOutputStream.writeInt(mob.getBanType());
                    dataOutputStream.writeInt(mob.getBanMsgType());
                    dataOutputStream.writeUTF(mob.getBanMsg());
                    dataOutputStream.writeShort(mob.getBanMapFields().size());
                    for (Tuple<Integer, String> banMap : mob.getBanMapFields()) {
                        dataOutputStream.writeInt(banMap.getLeft());
                        dataOutputStream.writeUTF(banMap.getRight());
                    }
                }
                dataOutputStream.writeBoolean(mob.isPatrolMob());
                if (mob.isPatrolMob()) {
                    dataOutputStream.writeInt(mob.getRange());
                    dataOutputStream.writeInt(mob.getDetectX());
                    dataOutputStream.writeInt(mob.getSenseX());
                }
                dataOutputStream.writeShort(mob.getQuests().size());
                for (int i : mob.getQuests()) {
                    dataOutputStream.writeInt(i);
                }
                dataOutputStream.writeShort(mob.getRevives().size());
                for (int i : mob.getRevives()) {
                    dataOutputStream.writeInt(i);
                }
                dataOutputStream.writeShort(mob.getMobSet().size());
                for (int i : mob.getMobSet()) {
                    dataOutputStream.writeInt(i);
                }
                dataOutputStream.writeShort(mob.getParentMobSet().size());
                for (int i : mob.getParentMobSet()) {
                    dataOutputStream.writeInt(i);
                }
                dataOutputStream.writeShort(mob.getSkills().size());
                dataOutputStream.writeShort(mob.getAttacks().size());
                List<MobSkill> all = mob.getSkills();
                all.addAll(mob.getAttacks());
                for (MobSkill ms : all) {
                    dataOutputStream.writeInt(ms.getSkillSN());
                    dataOutputStream.writeInt(ms.getSkillID());
                    dataOutputStream.writeByte(ms.getAction());
                    dataOutputStream.writeInt(ms.getLevel());
                    dataOutputStream.writeInt(ms.getEffectAfter());
                    dataOutputStream.writeInt(ms.getSkillDelay());
                    dataOutputStream.writeByte(ms.getPriority());
                    dataOutputStream.writeBoolean(ms.isOnlyFsm());
                    dataOutputStream.writeBoolean(ms.isOnlyOtherSkill());
                    dataOutputStream.writeBoolean(ms.isDoFirst());
                    dataOutputStream.writeBoolean(ms.isAfterDead());
                    dataOutputStream.writeInt(ms.getSkillForbid());
                    dataOutputStream.writeInt(ms.getAfterAttack());
                    dataOutputStream.writeInt(ms.getAfterAttackCount());
                    dataOutputStream.writeInt(ms.getAfterDelay());
                    dataOutputStream.writeInt(ms.getFixDamR());
                    dataOutputStream.writeInt(ms.getPreSkillIndex());
                    dataOutputStream.writeInt(ms.getPreSkillCount());
                    dataOutputStream.writeInt(ms.getCastTime());
                    dataOutputStream.writeInt(ms.getCoolTime());
                    dataOutputStream.writeInt(ms.getDelay());
                    dataOutputStream.writeInt(ms.getUseLimit());
                    dataOutputStream.writeInt(ms.getDisease());
                    dataOutputStream.writeUTF(ms.getInfo());
                    dataOutputStream.writeUTF(ms.getText());
                    dataOutputStream.writeUTF(ms.getSpeak());
                }
                dataOutputStream.writeInt(mts.getCurrentOptionsByMobStat(PImmune) != null ? mts.getCurrentOptionsByMobStat(PImmune).nOption : 0);
                dataOutputStream.writeBoolean(mob.getMobZone() != null);
                if (mob.getMobZone() != null) {
                    dataOutputStream.writeInt(mob.getMobZone().getIn().getDamage());
                    dataOutputStream.writeInt(mob.getMobZone().getIn().getConsume());
                    dataOutputStream.writeInt(mob.getMobZone().getIn().getAutoDec());
                    dataOutputStream.writeInt(mob.getMobZone().getIn().getHealRate());
                    dataOutputStream.writeInt(mob.getMobZone().getOut().getDamage());
                    dataOutputStream.writeInt(mob.getMobZone().getOut().getConsume());
                    dataOutputStream.writeInt(mob.getMobZone().getOut().getAutoDec());
                    dataOutputStream.writeInt(mob.getMobZone().getOut().getHealRate());
                    dataOutputStream.writeShort(mob.getMobZone().getMobZoneRect().size());
                    for (Map.Entry<Integer, Rect> mobZoneRect : mob.getMobZone().getMobZoneRect().entrySet()) {
                        dataOutputStream.writeInt(mobZoneRect.getKey());
                        dataOutputStream.writeInt(mobZoneRect.getValue().getLeft());
                        dataOutputStream.writeInt(mobZoneRect.getValue().getTop());
                        dataOutputStream.writeInt(mobZoneRect.getValue().getRight());
                        dataOutputStream.writeInt(mobZoneRect.getValue().getBottom());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static Mob loadMobFromFile(int templateID) {
        if (templateID <= 0) {
            return null;
        }
        // 282 profile: data/dat282 overlay first; 265 profile: exactly DAT_DIR/mobs as before.
        File file = net.swordie.ms.loaders.overlay.DataOverlays.current().datFile("mobs/" + templateID + ".dat");
        if (!file.exists()) {
            return null;
        }
        Mob mob = null;
        try (DataInputStream dataInputStream = new DataInputStream(new FileInputStream(file))) {
            int fileTemplateId = dataInputStream.readInt();
            mob = new Mob(fileTemplateId > 0 ? fileTemplateId : templateID);
            ForcedMobStat fms = mob.getForcedMobStat();
            fms.setLevel(dataInputStream.readInt());
            mob.setFirstAttack(dataInputStream.readInt());
            Option bodyOpt = new Option();
            bodyOpt.nOption = dataInputStream.readInt();
//            mts.addStatOptions(mob, BodyAttack, bodyOpt);
            fms.setMaxHP(dataInputStream.readLong());
            fms.setMaxMP(dataInputStream.readLong());
            fms.setPad(dataInputStream.readInt());
            fms.setPdr(dataInputStream.readInt());
            fms.setMad(dataInputStream.readInt());
            fms.setMdr(dataInputStream.readInt());
            fms.setAcc(dataInputStream.readInt());
            fms.setEva(dataInputStream.readInt());
            fms.setPushed(dataInputStream.readLong());
            fms.setExp(dataInputStream.readLong());
            mob.setSummonType(dataInputStream.readInt());
            mob.setCategory(dataInputStream.readInt());
            mob.setMobType(dataInputStream.readUTF());
            mob.setLink(dataInputStream.readInt());
            fms.setSpeed(dataInputStream.readInt());
            mob.setFs(dataInputStream.readDouble());
            mob.setElemAttr(dataInputStream.readUTF());
            mob.setHpTagColor(dataInputStream.readInt());
            mob.setHpTagBgcolor(dataInputStream.readInt());
            mob.setHPgaugeHide(dataInputStream.readBoolean());
            mob.setBoss(dataInputStream.readBoolean());
            mob.setUndead(dataInputStream.readBoolean());
            mob.setNoRegen(dataInputStream.readBoolean());
            mob.setInvincible(dataInputStream.readBoolean());
            mob.setHideName(dataInputStream.readBoolean());
            mob.setHideHP(dataInputStream.readBoolean());
            mob.setChangeable(dataInputStream.readBoolean());
            mob.setNoFlip(dataInputStream.readBoolean());
            mob.setTower(dataInputStream.readBoolean());
            mob.setPartyBonusMob(dataInputStream.readBoolean());
            mob.setUseReaction(dataInputStream.readBoolean());
            mob.setPublicReward(dataInputStream.readBoolean());
            mob.setMinion(dataInputStream.readBoolean());
            mob.setForward(dataInputStream.readBoolean());
            mob.setRemoteRange(dataInputStream.readBoolean());
            mob.setIgnoreFieldOut(dataInputStream.readBoolean());
            mob.setIgnoreMoveImpact(dataInputStream.readBoolean());
            mob.setSkeleton(dataInputStream.readBoolean());
            mob.setHideUserDamage(dataInputStream.readBoolean());
            mob.setIndividualReward(dataInputStream.readBoolean());
            mob.setNotConsideredFieldSet(dataInputStream.readBoolean());
            mob.setNoDoom(dataInputStream.readBoolean());
            mob.setUseCreateScript(dataInputStream.readBoolean());
            mob.setBlockUserMove(dataInputStream.readBoolean());
            mob.setKnockback(dataInputStream.readBoolean());
            mob.setRemoveQuest(dataInputStream.readBoolean());
            mob.setOnFieldSetSummon(dataInputStream.readBoolean());
            mob.setUserControll(dataInputStream.readBoolean());
            mob.setNoDebuff(dataInputStream.readBoolean());
            mob.setTargetFromSvr(dataInputStream.readBoolean());
            mob.setChangeableMobType(dataInputStream.readUTF());
            mob.setRareItemDropLevel(dataInputStream.readInt());
            mob.setHpRecovery(dataInputStream.readInt());
            mob.setMpRecovery(dataInputStream.readInt());
            mob.setMbookID(dataInputStream.readInt());
            mob.setChaseSpeed(dataInputStream.readInt());
            mob.setExplosiveReward(dataInputStream.readInt());
            mob.setCharismaEXP(dataInputStream.readInt());
            mob.setFlySpeed(dataInputStream.readInt());
            mob.setWp(dataInputStream.readInt());
            mob.setSummonEffect(dataInputStream.readInt());
            mob.setFixedDamage(dataInputStream.readInt());
            mob.setRemoveAfter(dataInputStream.readInt());
            mob.setBodyDisease(dataInputStream.readInt());
            mob.setBodyDiseaseLevel(dataInputStream.readInt());
            mob.setPoint(dataInputStream.readInt());
            mob.setPartyBonusR(dataInputStream.readInt());
            mob.setPassiveDisease(dataInputStream.readInt());
            mob.setCoolDamage(dataInputStream.readInt());
            mob.setCoolDamageProb(dataInputStream.readInt());
            mob.setDamageRecordQuest(dataInputStream.readInt());
            mob.setSealedCooltime(dataInputStream.readInt());
            mob.setWillEXP(dataInputStream.readInt());
            mob.setFixedMoveDir(dataInputStream.readUTF());
            mob.setEscortMob(dataInputStream.readBoolean());
            boolean banMap = dataInputStream.readBoolean();
            if (banMap) {
                mob.setBanMap(true);
                mob.setBanType(dataInputStream.readInt());
                mob.setBanMsgType(dataInputStream.readInt());
                mob.setBanMsg(dataInputStream.readUTF());
                short size = dataInputStream.readShort();
                for (int i = 0; i < size; i++) {
                    mob.addBanMap(dataInputStream.readInt(), dataInputStream.readUTF());
                }
            }
            boolean patrolMob = dataInputStream.readBoolean();
            if (patrolMob) {
                mob.setPatrolMob(true);
                mob.setRange(dataInputStream.readInt());
                mob.setDetectX(dataInputStream.readInt());
                mob.setSenseX(dataInputStream.readInt());
            }
            short size = dataInputStream.readShort();
            for (int i = 0; i < size; i++) {
                mob.addQuest(dataInputStream.readInt());
            }
            size = dataInputStream.readShort();
            for (int i = 0; i < size; i++) {
                mob.addRevive(dataInputStream.readInt());
            }
            size = dataInputStream.readShort();
            for (int i = 0; i < size; i++) {
                mob.addMob(dataInputStream.readInt());
            }
            size = dataInputStream.readShort();
            for (int i = 0; i < size; i++) {
                mob.addParentMob(dataInputStream.readInt());
            }
            short skillSize = dataInputStream.readShort();
            short attackSize = dataInputStream.readShort();
            for (int i = 0; i < skillSize + attackSize; i++) {
                MobSkill ms = new MobSkill();
                ms.setSkillSN(dataInputStream.readInt());
                ms.setSkillID(dataInputStream.readInt());
                ms.setAction(dataInputStream.readByte());
                ms.setLevel(dataInputStream.readInt());
                ms.setEffectAfter(dataInputStream.readInt());
                ms.setSkillAfter(dataInputStream.readInt());
                ms.setPriority(dataInputStream.readByte());
                ms.setOnlyFsm(dataInputStream.readBoolean());
                ms.setOnlyOtherSkill(dataInputStream.readBoolean());
                ms.setDoFirst(dataInputStream.readBoolean());
                ms.setAfterDead(dataInputStream.readBoolean());
                ms.setSkillForbid(dataInputStream.readInt());
                ms.setAfterAttack(dataInputStream.readInt());
                ms.setAfterAttackCount(dataInputStream.readInt());
                ms.setAfterDelay(dataInputStream.readInt());
                ms.setFixDamR(dataInputStream.readInt());
                ms.setPreSkillIndex(dataInputStream.readInt());
                ms.setPreSkillCount(dataInputStream.readInt());
                ms.setCastTime(dataInputStream.readInt());
                ms.setCoolTime(dataInputStream.readInt());
                ms.setDelay(dataInputStream.readInt());
                ms.setUseLimit(dataInputStream.readInt());
                ms.setDisease(dataInputStream.readInt());
                ms.setInfo(dataInputStream.readUTF());
                ms.setText(dataInputStream.readUTF());
                ms.setSpeak(dataInputStream.readUTF());
                if (i < skillSize) {
                    mob.addSkill(ms);
                } else {
                    mob.addAttack(ms);
                }
            }

            Option pImmuneOpt = new Option();
            pImmuneOpt.nOption = dataInputStream.readInt();
//            mts.addStatOptions(mob, PImmune, pImmuneOpt);
            boolean hasMobZone = dataInputStream.readBoolean();
            if (hasMobZone) {
                MobZoneInfo mobZoneInfo = new MobZoneInfo();
                mobZoneInfo.setMobId(mob.getTemplateId());
                mobZoneInfo.setIn(new MobZoneInfo.In());
                mobZoneInfo.setOut(new MobZoneInfo.Out());
                mob.setMobZone(mobZoneInfo);
                mob.getMobZone().getIn().setDamage(dataInputStream.readInt());
                mob.getMobZone().getIn().setConsume(dataInputStream.readInt());
                mob.getMobZone().getIn().setAutoDec(dataInputStream.readInt());
                mob.getMobZone().getIn().setHealRate(dataInputStream.readInt());
                mob.getMobZone().getOut().setDamage(dataInputStream.readInt());
                mob.getMobZone().getOut().setConsume(dataInputStream.readInt());
                mob.getMobZone().getOut().setAutoDec(dataInputStream.readInt());
                mob.getMobZone().getOut().setHealRate(dataInputStream.readInt());
                short mobZoneSize = dataInputStream.readShort();
                for (int i = 0; i < mobZoneSize; i++) {
                    int dataType = dataInputStream.readInt();
                    int left = dataInputStream.readInt();
                    int top = dataInputStream.readInt();
                    int right = dataInputStream.readInt();
                    int bottom = dataInputStream.readInt();
                    mob.getMobZone().getMobZoneRect().put(dataType, new Rect(left, top, right, bottom));
                }
            }

            mob.setAppearType((byte) -2); // new spawn
            mob.setAfterAttack(-1);
            mob.setCurrentAction(-1);
            mob.setEliteGrade(-1);
            mob.setMoveAction((byte) 5); // normal monster?
            mob.setHp(fms.getMaxHP());
            mob.setMaxHp(fms.getMaxHP());
            mob.setMp(fms.getMaxMP());
            mob.setMaxMp(fms.getMaxMP());
            mob.setDrops(DropData.getDropInfoByID(templateID).stream().filter(dropInfo -> !dropInfo.getReactorDrop()).collect(Collectors.toSet()));
            mob.getDrops().add(new DropInfo(GameConstants.MAX_DROP_CHANCE,
                    GameConstants.MIN_MONEY_MULT * mob.getForcedMobStat().getLevel(),
                    GameConstants.MAX_MONEY_MULT * mob.getForcedMobStat().getLevel()
            ));
            for (DropInfo di : mob.getDrops()) {
                di.generateNextDrop();
            }
        } catch (Exception e) {
            System.err.printf("Failed to load mob %d from dat: %s%n", templateID, e.getMessage());
            return null;
        }
        return mob;
    }

    public static void loadMobsFromWz() {
        String[] wzDirs = new String[]{"/Mob.wz"};
        List<File> files = new ArrayList<>();
        for (String dir : wzDirs) {
            File fileDir = new File(ServerConstants.WZ_DIR + dir);
            files.addAll(Arrays.asList(fileDir.listFiles()));
        }
        File questCountGroup = new File(ServerConstants.WZ_DIR + wzDirs[0] + "/QuestCountGroup/");
        for (File file : files) {
            if (file.isDirectory()) {
                // QuestCountGroup, AbyssExpeditionMob, BossPatern, MExplorerMob, RogueLikeMob
                continue;
            }
            Document doc = XMLApi.getRoot(file);
            Node node = XMLApi.getAllChildren(doc).getFirst();
            if (node == null) {
                continue;
            }
            String fileName = XMLApi.getNamedAttribute(node, "name").replace(".img", "");
            if (!Util.isNumber(fileName)) {
                continue;
            }
            int id = Integer.parseInt(fileName);
            Node infoNode = XMLApi.getFirstChildByNameBF(node, "info");
            Mob mob = new Mob(id);
            ForcedMobStat fms = mob.getForcedMobStat();
            MobTemporaryStat mts = mob.getTemporaryStat();
            for (Node n : XMLApi.getAllChildren(infoNode)) {
                String name = XMLApi.getNamedAttribute(n, "name");
                String value = XMLApi.getNamedAttribute(n, "value");
                switch (name) {
                    case "level":
                    case "Level":
                        fms.setLevel(Integer.parseInt(value));
                        break;
                    case "firstAttack":
                    case "firstattack":
                        mob.setFirstAttack((int) Double.parseDouble(value));
                        break;
                    case "bodyAttack":
                    case "bodyattack": // ...
                        Option bodyOpt = new Option();
                        bodyOpt.nOption = Integer.parseInt(value);
                        mts.addStatOptions(mob, BodyAttack, bodyOpt, false);
                        break;
                    case "maxHP":
                    case "finalmaxHP":
                        if (Util.isNumber(value)) {
                            fms.setMaxHP(Long.parseLong(value));
                        } else {
                            long bossHp = BossHpConstants.getMaxHp(mob.getTemplateId());
                            fms.setMaxHP(bossHp > 0 ? bossHp : 1337);
                        }
                        break;
                    case "maxMP":
                        fms.setMaxMP(Integer.parseInt(value));
                        break;
                    case "PADamage":
                        fms.setPad(Integer.parseInt(value));
                        break;
                    case "PDDamage":
//                            mts.addStatOptions(mob, PDR, "nPDR", Integer.parseInt(value));
                        break;
                    case "PDRate":
                        fms.setPdr(Integer.parseInt(value));
                        break;
                    case "MADamage":
                        fms.setMad(Integer.parseInt(value));
                        break;
                    case "MDDamage":
//                            mts.addStatOptions(mob, PDR, "nMDR", Integer.parseInt(value));
                        break;
                    case "MDRate":
                        fms.setMdr(Integer.parseInt(value));
                        break;
                    case "acc":
                        fms.setAcc(Integer.parseInt(value));
                        break;
                    case "eva":
                        fms.setEva(Integer.parseInt(value));
                        break;
                    case "pushed":
                        fms.setPushed(Long.parseLong(value));
                        break;
                    case "exp":
                        fms.setExp(Integer.parseInt(value));
                        break;
                    case "summonType":
                        mob.setSummonType(Integer.parseInt(value));
                        break;
                    case "category":
                        mob.setCategory(Integer.parseInt(value));
                        break;
                    case "mobType":
                        mob.setMobType(value);
                        break;
                    case "link":
                        mob.setLink(Integer.parseInt(value));
                        break;
                    case "speed":
                    case "Speed":
                        fms.setSpeed(Integer.parseInt(value));
                        break;
                    case "fs":
                        mob.setFs(Double.parseDouble(value));
                        break;
                    case "elemAttr":
                        mob.setElemAttr(value);
                        break;
                    case "hpTagColor":
                        mob.setHpTagColor(Integer.parseInt(value));
                        break;
                    case "hpTagBgcolor":
                        mob.setHpTagBgcolor(Integer.parseInt(value));
                        break;
                    case "HPgaugeHide":
                        mob.setHPgaugeHide(Integer.parseInt(value) == 1);
                        break;
                    case "boss":
                        mob.setBoss(Integer.parseInt(value) == 1);
                        break;
                    case "undead":
                    case "Undead":
                        mob.setUndead(Integer.parseInt(value) == 1);
                        break;
                    case "noregen":
                        mob.setNoRegen(Integer.parseInt(value) == 1);
                        break;
                    case "invincible":
                    case "invincibe": // neckson pls
                        mob.setInvincible(Integer.parseInt(value) == 1);
                        break;
                    case "hideName":
                    case "hidename":
                        mob.setHideName(Integer.parseInt(value) == 1);
                        break;
                    case "hideHP":
                        mob.setHideHP(Integer.parseInt(value) == 1);
                        break;
                    case "changeableMob":
                        mob.setChangeable(Integer.parseInt(value) == 1);
                        break;
                    case "noFlip":
                        mob.setNoFlip(Integer.parseInt(value) == 1);
                        break;
                    case "tower":
                        mob.setTower(Integer.parseInt(value) == 1);
                        break;
                    case "partyBonusMob":
                        mob.setPartyBonusMob(Integer.parseInt(value) == 1);
                        break;
                    case "useReaction":
                        mob.setUseReaction(Integer.parseInt(value) == 1);
                        break;
                    case "publicReward":
                        mob.setPublicReward(Integer.parseInt(value) == 1);
                        break;
                    case "minion":
                        mob.setMinion(Integer.parseInt(value) == 1);
                        break;
                    case "forward":
                        mob.setForward(Integer.parseInt(value) == 1);
                        break;
                    case "isRemoteRange":
                        mob.setIsRemoteRange(Integer.parseInt(value) == 1);
                        break;
                    case "ignoreFieldOut":
                        mob.setIgnoreFieldOut(Integer.parseInt(value) == 1);
                        break;
                    case "ignoreMoveImpact":
                        mob.setIgnoreMoveImpact(Integer.parseInt(value) == 1);
                        break;
                    case "skeleton":
                        mob.setSkeleton(Integer.parseInt(value) == 1);
                        break;
                    case "hideUserDamage":
                        mob.setHideUserDamage(Integer.parseInt(value) == 1);
                        break;
                    case "individualReward":
                        mob.setIndividualReward(Integer.parseInt(value) == 1);
                        break;
                    case "notConsideredFieldSet":
                        mob.setNotConsideredFieldSet(Integer.parseInt(value) == 1);
                        break;
                    case "noDoom":
                        mob.setNoDoom(Integer.parseInt(value) == 1);
                        break;
                    case "useCreateScript":
                        mob.setUseCreateScript(Integer.parseInt(value) == 1);
                        break;
                    case "blockUserMove":
                        mob.setBlockUserMove(Integer.parseInt(value) == 1);
                        break;
                    case "knockback":
                        mob.setKnockback(Integer.parseInt(value) == 1);
                        break;
                    case "removeQuest":
                        mob.setRemoveQuest(Integer.parseInt(value) == 1);
                        break;
                    case "onFieldSetSummon":
                        mob.setOnFieldSetSummon(Integer.parseInt(value) == 1);
                        break;
                    case "userControll":
                        mob.setUserControll(Integer.parseInt(value) == 1);
                        break;
                    case "noDebuff":
                        mob.setNoDebuff(Integer.parseInt(value) == 1);
                        break;
                    case "targetFromSvr":
                        mob.setTargetFromSvr(Integer.parseInt(value) == 1);
                        break;
                    case "changeableMob_Type":
                        mob.setChangeableMobType(value);
                        break;
                    case "rareItemDropLevel":
                        mob.setRareItemDropLevel(Integer.parseInt(value));
                        break;
                    case "hpRecovery":
                        mob.setHpRecovery(Integer.parseInt(value));
                        break;
                    case "mpRecovery":
                        mob.setMpRecovery(Integer.parseInt(value));
                        break;
                    case "mbookID":
                        mob.setMbookID(Integer.parseInt(value));
                        break;
                    case "chaseSpeed":
                        mob.setChaseSpeed(Integer.parseInt(value));
                        break;
                    case "explosiveReward":
                        mob.setExplosiveReward(Integer.parseInt(value));
                        break;
                    case "charismaEXP":
                        mob.setCharismaEXP(Integer.parseInt(value));
                        break;
                    case "flyspeed":
                    case "flySpeed":
                    case "FlySpeed":
                        mob.setFlySpeed(Integer.parseInt(value));
                        break;
                    case "wp":
                        mob.setWp(Integer.parseInt(value));
                        break;
                    case "summonEffect":
                        mob.setSummonEffect(Integer.parseInt(value));
                        break;
                    case "fixedDamage":
                        mob.setFixedDamage(Integer.parseInt(value));
                        break;
                    case "removeAfter":
                        mob.setRemoveAfter(Integer.parseInt(value));
                        break;
                    case "bodyDisease":
                        mob.setBodyDisease(Integer.parseInt(value));
                        break;
                    case "bodyDiseaseLevel":
                        mob.setBodyDiseaseLevel(Integer.parseInt(value));
                        break;
                    case "point":
                        mob.setPoint(Integer.parseInt(value));
                        break;
                    case "partyBonusR":
                        mob.setPartyBonusR(Integer.parseInt(value));
                        break;
                    case "PassiveDisease":
                        mob.setPassiveDisease(Integer.parseInt(value));
                        break;
                    case "coolDamage":
                        mob.setCoolDamage(Integer.parseInt(value));
                        break;
                    case "coolDamageProb":
                        mob.setCoolDamageProb(Integer.parseInt(value));
                        break;
                    case "damageRecordQuest":
                        mob.setDamageRecordQuest(Integer.parseInt(value));
                        break;
                    case "sealedCooltime":
                        mob.setSealedCooltime(Integer.parseInt(value));
                        break;
                    case "willEXP":
                        mob.setWillEXP(Integer.parseInt(value));
                        break;
                    case "fixedMoveDir":
                        mob.setFixedMoveDir(value);
                        break;
                    case "PImmune":
                        Option immOpt = new Option();
                        immOpt.nOption = Integer.parseInt(value);
                        mts.addStatOptions(mob, PImmune, immOpt, false);
                        break;
                    case "patrol":
                        mob.setPatrolMob(true);
                        for (Node patrolNode : XMLApi.getAllChildren(n)) {
                            String patrolNodeName = XMLApi.getNamedAttribute(patrolNode, "name");
                            String patrolNodeValue = XMLApi.getNamedAttribute(patrolNode, "value");
                            switch (patrolNodeName) {
                                case "range":
                                    mob.setRange(Integer.parseInt(patrolNodeValue));
                                    break;
                                case "detectX":
                                    mob.setDetectX(Integer.parseInt(patrolNodeValue));
                                    break;
                                case "senseX":
                                    mob.setSenseX(Integer.parseInt(patrolNodeValue));
                                    break;
                                default:
                                    break;
                            }
                        }
                        break;
                    case "ban":
                        mob.setBanMap(true);
                        for (Node banNode : XMLApi.getAllChildren(n)) {
                            String banNodeName = XMLApi.getNamedAttribute(banNode, "name");
                            String banNodeValue = XMLApi.getNamedAttribute(banNode, "value");
                            switch (banNodeName) {
                                case "banType":
                                    mob.setBanType(Integer.parseInt(banNodeValue));
                                    break;
                                case "banMsgType":
                                    mob.setBanMsgType(Integer.parseInt(banNodeValue));
                                    break;
                                case "banMsg":
                                    mob.setBanMsg(banNodeValue);
                                    break;
                                case "banMap":
                                    for (Node banMaps : XMLApi.getAllChildren(banNode)) {
                                        int banFieldID = 0;
                                        String banPortal = "";
                                        for (Node banMap : XMLApi.getAllChildren(banMaps)) {
                                            String banMapName = XMLApi.getNamedAttribute(banMap, "name");
                                            String banMapValue = XMLApi.getNamedAttribute(banMap, "value");
                                            switch (banMapName) {
                                                case "field":
                                                    banFieldID = Integer.parseInt(banMapValue);
                                                    break;
                                                case "portal":
                                                    banPortal = banMapValue;
                                                    break;
                                                default:
                                                    if (LOG_UNKS) {
                                                        System.out.printf("Unknown ban map node %s with value %s%n", banMapName, banMapValue);
                                                    }
                                                    break;
                                            }
                                            if (banFieldID != 0) {
                                                mob.addBanMap(banFieldID, banPortal);
                                            }
                                        }
                                    }
                                    break;
                            }
                        }
                        break;
                    case "revive":
                        for (Node reviveNode : XMLApi.getAllChildren(n)) {
                            mob.addRevive(Integer.parseInt((XMLApi.getNamedAttribute(reviveNode, "value"))));
                        }
                        break;
                    case "skill":
                    case "attack":
                        boolean attack = "attack".equalsIgnoreCase(name);
                        for (Node skillIDNode : XMLApi.getAllChildren(n)) {
                            if (!Util.isNumber(XMLApi.getNamedAttribute(skillIDNode, "name"))) {
                                continue;
                            }
                            MobSkill mobSkill = new MobSkill();
                            mobSkill.setSkillSN(Integer.parseInt(XMLApi.getNamedAttribute(skillIDNode, "name")));
                            for (Node skillInfoNode : XMLApi.getAllChildren(skillIDNode)) {
                                String skillNodeName = XMLApi.getNamedAttribute(skillInfoNode, "name");
                                String skillNodeValue = XMLApi.getNamedAttribute(skillInfoNode, "value");
                                switch (skillNodeName) {
                                    case "skill":
                                        mobSkill.setSkillID(Integer.parseInt(skillNodeValue));
                                        break;
                                    case "action":
                                        mobSkill.setAction(Byte.parseByte(skillNodeValue));
                                        break;
                                    case "level":
                                        mobSkill.setLevel(Integer.parseInt(skillNodeValue));
                                        break;
                                    case "effectAfter":
                                        if (!skillNodeValue.equals("")) {
                                            mobSkill.setEffectAfter(Integer.parseInt(skillNodeValue));
                                        }
                                        break;
                                    case "skillAfter":
                                        mobSkill.setSkillAfter(Integer.parseInt(skillNodeValue));
                                        break;
                                    case "priority":
                                        mobSkill.setPriority(Byte.parseByte(skillNodeValue));
                                        break;
                                    case "onlyFsm":
                                        mobSkill.setOnlyFsm(Integer.parseInt(skillNodeValue) != 0);
                                        break;
                                    case "onlyOtherSkill":
                                        mobSkill.setOnlyOtherSkill(Integer.parseInt(skillNodeValue) != 0);
                                        break;
                                    case "doFirst":
                                        mobSkill.setDoFirst(Integer.parseInt(skillNodeValue) != 0);
                                        break;
                                    case "afterDead":
                                        mobSkill.setAfterDead(Integer.parseInt(skillNodeValue) != 0);
                                        break;
                                    case "skillForbid":
                                        mobSkill.setSkillForbid(Integer.parseInt(skillNodeValue));
                                        break;
                                    case "afterAttack":
                                        mobSkill.setAfterAttack(Integer.parseInt(skillNodeValue));
                                        break;
                                    case "afterAttackCount":
                                        mobSkill.setAfterAttackCount(Integer.parseInt(skillNodeValue));
                                        break;
                                    case "afterDelay":
                                        mobSkill.setAfterDelay(Integer.parseInt(skillNodeValue));
                                        break;
                                    case "fixDamR":
                                        mobSkill.setFixDamR(Integer.parseInt(skillNodeValue));
                                        break;
                                    case "preSkillIndex":
                                        mobSkill.setPreSkillIndex(Integer.parseInt(skillNodeValue));
                                        break;
                                    case "preSkillCount":
                                        mobSkill.setPreSkillCount(Integer.parseInt(skillNodeValue));
                                        break;
                                    case "castTime":
                                        mobSkill.setCastTime(Integer.parseInt(skillNodeValue));
                                        break;
                                    case "cooltime":
                                        mobSkill.setCoolTime(Integer.parseInt(skillNodeValue));
                                        break;
                                    case "delay":
                                        mobSkill.setDelay(Integer.parseInt(skillNodeValue));
                                        break;
                                    case "useLimit":
                                        mobSkill.setUseLimit(Integer.parseInt(skillNodeValue));
                                        break;
                                    case "disease":
                                        mobSkill.setDisease(Integer.parseInt(skillNodeValue));
                                        break;
                                    case "info":
                                        mobSkill.setInfo(skillNodeValue);
                                        break;
                                    case "text":
                                        mobSkill.setText(skillNodeValue);
                                        break;
                                    case "speak":
                                        mobSkill.setSpeak(skillNodeValue);
                                        break;
                                    default:
                                        if (LOG_UNKS) {
                                            System.out.printf("Unknown skill node %s with value %s%n", skillNodeName, skillNodeValue);
                                        }
                                }
                            }
                            if (attack) {
                                mob.addAttack(mobSkill);
                            } else {
                                mob.addSkill(mobSkill);
                            }
                        }
                        break;
                    case "selfDestruction":
                        // TODO Maybe more info?
                        mob.setSelfDestruction(true);
                        break;
                    case "escort":
                        mob.setEscortMob(Integer.parseInt(value) != 0);
                        break;
                    case "dropItemPeriod":
                        mob.setDropItemPeriod(Integer.parseInt(value));
                        break;
                    case "mobZone":
                        if (version >= 263) {
                            break;
                        }
                        mob.setMobZone(mobZoneInfos.stream().filter(m -> m.getMobId() == id).findAny().orElse(null));
                        if (mob.getMobZone() != null) {
                            for (Node mobZoneNode : XMLApi.getAllChildren(n)) {
                                if (!Util.isNumber(XMLApi.getNamedAttribute(mobZoneNode, "name"))) {
                                    continue;
                                }
                                int dataType = Integer.parseInt(XMLApi.getNamedAttribute(mobZoneNode, "name"));
                                int x1 = 0, y1 = 0, x2 = 0, y2 = 0;
                                for (Node mobZoneInfoNode : XMLApi.getAllChildren(mobZoneNode)) {
                                    String RectName = XMLApi.getNamedAttribute(mobZoneInfoNode, "name");
                                    if (RectName.equals("lt")) {
                                        x1 = Integer.parseInt(XMLApi.getNamedAttribute(mobZoneInfoNode, "x"));
                                        y1 = Integer.parseInt(XMLApi.getNamedAttribute(mobZoneInfoNode, "y"));
                                    }
                                    if (RectName.equals("rb")) {
                                        x2 = Integer.parseInt(XMLApi.getNamedAttribute(mobZoneInfoNode, "x"));
                                        y2 = Integer.parseInt(XMLApi.getNamedAttribute(mobZoneInfoNode, "y"));
                                    }
                                }
                                Rect rect = new Rect(x1, y1, x2, y2);
                                mob.getMobZone().getMobZoneRect().put(dataType, rect);
                            }
                        }
                        break;
                    case "speak":
                    case "thumbnail":
                    case "default":
                    case "defaultHP":
                    case "defaultMP":
                    case "passive":
                    case "firstAttackRange":
                    case "nonLevelCheckEVA":
                    case "nonLevelCheckACC":
                    case "changeImg":
                    case "showNotRemoteDam":
                    case "buff":
                    case "damagedBySelectedMob":
                    case "damagedByMob":
                    case "getCP":
                    case "loseItem":
                    case "0":
                    case "onlyNormalAttack":
                    case "notConsiderFieldSet":
                    case "overSpeed":
                    case "ignoreMovable":
                    case "jsonLoad":
                    case "fixedBodyAttackDamageR":
                    case "adjustLayerZ":
                    case "damagedBySelectedSkill":
                    case "option_damagedByMob":
                    case "bodyKnockBack":
                    case "straightMoveDir":
                    case "onlyHittedByCommonAttack":
                    case "invincibleAttack":
                    case "noChase":
                    case "notAttack":
                    case "alwaysInAffectedRect":
                    case "firstShowHPTag":
                    case "pointFPSMode":
                    case "11":
                    case "prevLinkMob":
                    case "option_skeleton":
                    case "lifePoint":
                    case "defenseMob":
                    case "forceChaseEscort":
                    case "damageModification":
                    case "randomFlyingBoss":
                    case "randomFlyingMob":
                    case "stalking":
                    case "minimap":
                    case "removeOnMiss":
                    case "fieldEffect":
                    case "onceReward":
                    case "onMobDeadQR":
                    case "peddlerMob":
                    case "peddlerDamR":
                    case "rewardSprinkle":
                    case "rewardSprinkleCount":
                    case "rewardSprinkleSpeed":
                    case "ignorMoveImpact":
                    case "hideMove":
                    case "atom":
                    case "smartPhase":
                    case "trans":
                    case "chaseEffect":
                    case "dualGauge":
                    case "noReturnByDead":
                    case "AngerGauge":
                    case "ChargeCount":
                    case "upperMostLayer":
                    case "cannotEvade":
                    case "phase":
                    case "doNotRemove":
                    case "healOnDestroy":
                    case "debuffobj":
                    case "obtacle":
                    case "weapon":
                    case "forcedFlip":
                    case "buffStack":
                    case "001":
                    case "002":
                    case "003":
                    case "004":
                    case "005":
                    case "006":
                    case "007":
                    case "008":
                    case "009":
                    case "010":
                    case "011":
                    case "onlySelectedSkill":
                    case "finalAdjustedDamageRate":
                    case "battlePvP":
                    case "mobJobCategory":
                    case "considerUserCount":
                    case "randomMob":
                    case "dieHeight":
                    case "dieHeightTime":
                    case "notChase":
                    case "fixedStat":
                    case "allyMob":
                    case "linkMob":
                    case "skelAniMixRate":
                    case "mobZoneObjID":
                    case "mobZoneObjType":
                    case "holdRange":
                    case "targetChaseTime":
                    case "copyCharacter":
                    case "disable":
                    case "underObject":
                    case "1582":
                    case "peddlerCount":
                    case "bodyAttackInfo":
                    case "mobZoneType":
                    case "largeDamageRecord":
                    case "considerUserCounter":
                    case "damageByObtacleAtom":
                    case "info":
                    case "cantPassByTeleport":
                    case "250000":
                    case "forward_direction":
                        break;
                    default:
                        if (LOG_UNKS) {
                            System.out.printf("Unkown property %s with value %s.%n", name, value);
                        }
                }
            }
            mobs.put(mob.getTemplateId(), mob);
        }
        // Quest count group
        for (File file : questCountGroup.listFiles()) {
            Document doc = XMLApi.getRoot(file);
            Node node = XMLApi.getAllChildren(doc).getFirst();
            int templateID = Integer.parseInt(XMLApi.getNamedAttribute(node, "name").replace(".img", ""));
            Node infoNode = XMLApi.getFirstChildByNameBF(node, "info");
            Mob mob = getMobById(templateID);
            if (mob == null) {
                mob = new Mob(templateID);
                mobs.put(templateID, mob);
            }
            for (Node n : XMLApi.getAllChildren(infoNode)) {
                String name = XMLApi.getNamedAttribute(n, "name");
                String value = XMLApi.getNamedAttribute(n, "value");
                if (Util.isNumber(name) && value != null) {
                    mob.addMob(Integer.parseInt(value));
                }
            }
        }
        for (Mob mob : mobs.values()) {
            for (int childId : mob.getMobSet()) {
                Mob child = getMobById(childId);
                if (child != null) {
                    child.addParentMob(mob.getTemplateId());
                }
            }
        }
    }

    public static void loadNormalMobsFromWz() {
        File cache = new File(ServerConstants.DAT_DIR + "/normalMobs.dat");
        long start = System.currentTimeMillis();
        if (cache.isFile() && loadNormalMobsFromDat(cache)) {
            System.out.printf("[WZ Data] Loaded normal mob index from \"normalMobs.dat\" in %dms%n",
                    System.currentTimeMillis() - start);
            return;
        }
        loadNormalMobsFromWzUncached();
        saveNormalMobsToDat(cache);
        System.out.printf("[WZ Data] Built normal mob index from Mob.wz in %dms%n",
                System.currentTimeMillis() - start);
    }

    private static int countMobWzXmlFiles() {
        File mobDir = new File(ServerConstants.WZ_DIR + "/Mob.wz");
        File[] files = mobDir.listFiles();
        if (files == null) {
            return 0;
        }
        int count = 0;
        for (File file : files) {
            if (file.isFile()) {
                count++;
            }
        }
        return count;
    }

    private static boolean loadNormalMobsFromDat(File cache) {
        int wzFileCount = countMobWzXmlFiles();
        try (DataInputStream in = new DataInputStream(new FileInputStream(cache))) {
            if (in.readInt() != NORMAL_MOBS_CACHE_MAGIC) {
                return false;
            }
            if (in.readInt() != wzFileCount) {
                return false;
            }
            int count = in.readInt();
            normalMobs.clear();
            for (int i = 0; i < count; i++) {
                int id = in.readInt();
                int level = in.readInt();
                long exp = in.readLong();
                Mob mob = new Mob(id);
                ForcedMobStat fms = mob.getForcedMobStat();
                fms.setLevel(level);
                fms.setExp(exp);
                normalMobs.add(mob);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void saveNormalMobsToDat(File cache) {
        Util.makeDirIfAbsent(ServerConstants.DAT_DIR);
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(cache))) {
            out.writeInt(NORMAL_MOBS_CACHE_MAGIC);
            out.writeInt(countMobWzXmlFiles());
            out.writeInt(normalMobs.size());
            for (Mob mob : normalMobs) {
                out.writeInt(mob.getTemplateId());
                out.writeInt(mob.getLevel());
                out.writeLong(mob.getForcedMobStat().getExp());
            }
        } catch (IOException e) {
            System.out.println("[WZ Data] Failed to save normalMobs.dat: " + e.getMessage());
        }
    }

    private static void loadNormalMobsFromWzUncached() {
        normalMobs.clear();
        String[] wzDirs = new String[]{"/Mob.wz"};
        List<File> files = new ArrayList<>();
        for (String dir : wzDirs) {
            File fileDir = new File(ServerConstants.WZ_DIR + dir);
            files.addAll(Arrays.asList(fileDir.listFiles()));
        }
        File questCountGroup = new File(ServerConstants.WZ_DIR + wzDirs[0] + "/QuestCountGroup/");
        for (File file : files) {
            if (file.isDirectory()) {
                // QuestCountGroup
                continue;
            }
            Document doc = XMLApi.getRoot(file);
            Node node = XMLApi.getAllChildren(doc).getFirst();
            if (node == null) {
                continue;
            }
            String fileName = XMLApi.getNamedAttribute(node, "name").replace(".img", "");
            if (!Util.isNumber(fileName)) {
                continue;
            }
            int id = Integer.parseInt(fileName);
            Node infoNode = XMLApi.getFirstChildByNameBF(node, "info");
            Mob mob = new Mob(id);
            ForcedMobStat fms = mob.getForcedMobStat();
            for (Node n : XMLApi.getAllChildren(infoNode)) {
                String name = XMLApi.getNamedAttribute(n, "name");
                String value = XMLApi.getNamedAttribute(n, "value");
                switch (name) {
                    case "level":
                    case "Level":
                        fms.setLevel(Integer.parseInt(value));
                        break;
                    case "exp":
                        fms.setExp(Integer.parseInt(value));
                        break;
                    case "boss":
                        mob.setBoss(Integer.parseInt(value) == 1);
                        break;
                }
            }
            if (!mob.isBoss()) {
                normalMobs.add(mob);
            }
        }
    }

    public static void loadMobZoneInfoFromWZ() {
        File file = net.swordie.ms.loaders.Etc.EtcData.etcWzFile("MobZoneInfo.img.xml");
        Node root = XMLApi.getRoot(file);
        Node mainNode = XMLApi.getAllChildren(root).getFirst();
        List<Node> nodes = XMLApi.getAllChildren(mainNode);
        for (Node node : nodes) {
            int mobId = Integer.parseInt(XMLApi.getNamedAttribute(node, "name"));
            MobZoneInfo mobZoneInfo = new MobZoneInfo();
            mobZoneInfo.setMobId(mobId);
            mobZoneInfo.setIn(new MobZoneInfo.In());
            mobZoneInfo.setOut(new MobZoneInfo.Out());
            List<Node> nodes1 = XMLApi.getAllChildren(node);
            for (Node node1 : nodes1) {
                String type = XMLApi.getNamedAttribute(node1, "name");
                switch (type) {
                    case "damage": {
                        List<Node> nodesList = XMLApi.getAllChildren(node1);
                        for (Node node3 : nodesList) {
                            String name = XMLApi.getNamedAttribute(node3, "name");
                            String value = XMLApi.getNamedAttribute(node3, "value");
                            if (name.equals("in")) {
                                mobZoneInfo.getIn().setDamage(Integer.parseInt(value));
                            }
                            if (name.equals("out")) {
                                mobZoneInfo.getOut().setDamage(Integer.parseInt(value));
                            }
                        }
                        break;
                    }
                    case "consume": {
                        List<Node> nodesList = XMLApi.getAllChildren(node1);
                        for (Node node3 : nodesList) {
                            String name = XMLApi.getNamedAttribute(node3, "name");
                            String value = XMLApi.getNamedAttribute(node3, "value");
                            if (name.equals("in")) {
                                mobZoneInfo.getIn().setConsume(Integer.parseInt(value));
                            }
                            if (name.equals("out")) {
                                mobZoneInfo.getOut().setConsume(Integer.parseInt(value));
                            }
                        }
                        break;
                    }
                    case "autoDec": {
                        List<Node> nodesList = XMLApi.getAllChildren(node1);
                        for (Node node3 : nodesList) {
                            String name = XMLApi.getNamedAttribute(node3, "name");
                            String value = XMLApi.getNamedAttribute(node3, "value");
                            if (name.equals("in")) {
                                mobZoneInfo.getIn().setAutoDec(Integer.parseInt(value));
                            }
                            if (name.equals("out")) {
                                mobZoneInfo.getOut().setAutoDec(Integer.parseInt(value));
                            }
                        }
                        break;
                    }
                    case "healRate": {
                        List<Node> nodesList = XMLApi.getAllChildren(node1);
                        for (Node node3 : nodesList) {
                            String name = XMLApi.getNamedAttribute(node3, "name");
                            String value = XMLApi.getNamedAttribute(node3, "value");
                            if (name.equals("in")) {
                                mobZoneInfo.getIn().setHealRate(Integer.parseInt(value));
                            }
                            if (name.equals("out")) {
                                mobZoneInfo.getOut().setHealRate(Integer.parseInt(value));
                            }
                        }
                        break;
                    }
                }
            }
            mobZoneInfos.add(mobZoneInfo);
        }
    }

    public static void main(String[] args) {
        generateDatFiles();
    }

    public static void clear() {
        mobs.clear();
        normalMobs.clear();
        mobZoneInfos.clear();
    }

    public static int getMobByCharLevel(short charLevel) {
        List<Integer> mobids = new ArrayList<>();
        for (Mob mob : normalMobs) {
            if (mob.getTemplateId() < 9000001 && charLevel >= mob.getLevel() - 20 && charLevel <= mob.getLevel() + 20 && mob.getForcedMobStat().getExp() > 0) {
                mobids.add(mob.getTemplateId());
            }
        }
        if (mobids.isEmpty()) {
            return 0;
        }
        return Util.getRandomFromCollection(mobids);
    }
}
