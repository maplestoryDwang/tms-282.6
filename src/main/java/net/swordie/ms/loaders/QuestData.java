package net.swordie.ms.loaders;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.AccountQuest;
import net.swordie.ms.client.character.quest.Quest;
import net.swordie.ms.client.character.quest.progress.*;
import net.swordie.ms.client.character.quest.requirement.*;
import net.swordie.ms.client.character.quest.reward.*;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.enums.QuestStatus;
import net.swordie.ms.enums.Stat;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.loaders.containerclasses.ItemInfo;
import net.swordie.ms.loaders.containerclasses.QuestInfo;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.Util;
import net.swordie.ms.util.XMLApi;
import org.w3c.dom.Node;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class QuestData {
    private static final boolean LOG_UNKS = false;
    private static final Int2ObjectMap<QuestInfo> baseQuests = new Int2ObjectOpenHashMap<>();

    public static void loadQuestsFromWZ() {
        String qDir = String.format("%s/Quest.wz/QuestData/", ServerConstants.WZ_DIR);
        try {
            Files.walk(Paths.get(qDir))
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".img.xml"))
                    .forEach(p -> {
                        try {
                            File file = p.toFile();
                            // 12345.img.xml -> 12345
                            int questID = Integer.parseInt(file.getName().replace(".img.xml", "").replace(".img", ""));
                            Node root = XMLApi.getRoot(file);
                            Node main = XMLApi.getAllChildren(root).get(0); // <imgdir name="12345.img">

                            QuestInfo quest = new QuestInfo();
                            quest.setQuestID(questID);

                            // -------- QuestInfo --------
                            Node qi = XMLApi.getFirstChildByNameBF(main, "QuestInfo");
                            if (qi != null) {
                                for (Node n : XMLApi.getAllChildren(qi)) {
                                    String name = XMLApi.getNamedAttribute(n, "name");
                                    String value = XMLApi.getNamedAttribute(n, "value");
                                    switch (name) {
                                        case "name": quest.setQuestName(value); break;
                                        case "parent": quest.setDemandSummary(value); break; // keep if you map parent elsewhere
                                        case "order": quest.setInfoNumber(Integer.parseInt(value)); break;
                                        case "area": quest.setFieldSetKeepTime(Integer.parseInt(value)); break;
                                        case "blocked": quest.setSecret(Integer.parseInt(value) != 0); break;
                                        // TMS 282 QuestInfo flags (the dat265 build never set this field): the quest
                                        // completes as soon as its end requirements are met.
                                        case "autoComplete":
                                        case "autoPreComplete":
                                            if (Integer.parseInt(value) != 0) {
                                                quest.setAutoComplete(true);
                                            }
                                            break;
                                    }
                                }
                            }

                            // -------- Check (requirements by status) --------
                            Node check = XMLApi.getFirstChildByNameBF(main, "Check");
                            if (check != null) {
                                for (Node statusNode : XMLApi.getAllChildren(check)) {
                                    String statusStr = XMLApi.getNamedAttribute(statusNode, "name"); // "0" or "1"
                                    if (!Util.isNumber(statusStr)) {
                                        continue;
                                    }
                                    byte status = Byte.parseByte(statusStr);
                                    for (Node infoNode : XMLApi.getAllChildren(statusNode)) {
                                        String name = XMLApi.getNamedAttribute(infoNode, "name");
                                        String value = XMLApi.getNamedAttribute(infoNode, "value");
                                        switch (name) {
                                            case "npc":
                                                if (status == 0) {
                                                    quest.setStartNpc(Integer.parseInt(value));
                                                } else {
                                                    quest.setEndNpc(Integer.parseInt(value));
                                                }
                                                break;
                                            case "infoNumber":
                                                quest.setInfoNumber(Integer.parseInt(value));
                                                break;
                                            case "fieldsetkeeptime":
                                                quest.setFieldSetKeepTime(Integer.parseInt(value));
                                                break;
                                            case "subJobFlags":
                                                quest.setSubJobFlags(Integer.parseInt(value));
                                                break;
                                            case "deathCount":
                                                quest.setDeathCount(Integer.parseInt(value));
                                                break;
                                            case "mobDropMeso":
                                                quest.setMobDropMeso(Integer.parseInt(value));
                                                break;
                                            case "morph":
                                                quest.setMorph(Integer.parseInt(value));
                                                break;
                                            case "start":
                                                try {
                                                    quest.setStart(Long.parseLong(value));
                                                } catch (Exception ignored) {}
                                                break;
                                            case "start_t":
                                                try {
                                                    quest.setStartT(Long.parseLong(value));
                                                } catch (Exception ignored) {}
                                                break;
                                            case "end":
                                                try {
                                                    quest.setEnd(Long.parseLong(value));
                                                } catch (Exception ignored) {}
                                                break;
                                            case "end_t":
                                                try {
                                                    quest.setEndT(Long.parseLong(value));
                                                } catch (Exception ignored) {}
                                                break;
                                            case "startscript":
                                                quest.setStartScript(value);
                                                break;
                                            case "endscript":
                                                quest.setEndScript(value);
                                                break;
                                            case "npcSpeech":;
                                                for (Node infoSpeechNode : XMLApi.getAllChildren(infoNode)) {
                                                    String speechID = XMLApi.getNamedAttribute(infoSpeechNode, "name");
                                                    for (Node speechNode : XMLApi.getAllChildren(infoSpeechNode)) {
                                                        String speechName = XMLApi.getNamedAttribute(speechNode, "name");
                                                        String speechValue = XMLApi.getNamedAttribute(speechNode, "value");
                                                        if (speechName.equals("script")) {
                                                            quest.addSpeech(Integer.valueOf(speechID), speechValue);
                                                        }
                                                    }
                                                }
                                                break;
                                            case "fieldset":
                                                quest.setFieldSet(value);
                                                break;
                                            case "normalAutoStart":
                                                quest.setNormalAutoStart(Integer.parseInt(value) != 0);
                                                break;
                                            case "completeNpcAutoGuide":
                                                quest.setCompleteNpcAutoGuide(Integer.parseInt(value) != 0);
                                                break;
                                            case "autoStart":
                                                quest.setAutoStart(Integer.parseInt(value) != 0);
                                                break;
                                            case "scenarioQuest":
                                                quest.setAutoStart(Integer.parseInt(value) != 0);
                                                break;
                                            case "secret":
                                                quest.setSecret(Integer.parseInt(value) != 0);
                                                break;
                                            case "marriaged":
                                                quest.addRequirement(new QuestStartMarriageRequirement());
                                                break;
                                            case "lvmin":
                                                quest.addRequirement(new QuestStartMinStatRequirement(Stat.level, Short.parseShort(value)));
                                                break;
                                            case "pop":
                                            case "fameGradeReq":
                                                quest.addRequirement(new QuestStartMinStatRequirement(Stat.pop, Short.parseShort(value)));
                                                break;
                                            case "charismaMin":
                                                quest.addRequirement(new QuestStartMinStatRequirement(Stat.charismaEXP, Short.parseShort(value)));
                                                break;
                                            case "insightMin":
                                                quest.addRequirement(new QuestStartMinStatRequirement(Stat.insightEXP, Short.parseShort(value)));
                                                break;
                                            case "willMin":
                                                quest.addRequirement(new QuestStartMinStatRequirement(Stat.willEXP, Short.parseShort(value)));
                                                break;
                                            case "craftMin":
                                                quest.addRequirement(new QuestStartMinStatRequirement(Stat.craftEXP, Short.parseShort(value)));
                                                break;
                                            case "senseMin":
                                                quest.addRequirement(new QuestStartMinStatRequirement(Stat.senseEXP, Short.parseShort(value)));
                                                break;
                                            case "charm":
                                            case "charmMin":
                                                quest.addRequirement(new QuestStartMinStatRequirement(Stat.charmEXP, Short.parseShort(value)));
                                                break;
                                            case "level":
                                                quest.addProgressRequirement(new QuestProgressLevelRequirement(Short.parseShort(value)));
                                                break;
                                            case "lvmax":
                                                quest.addRequirement(new QuestStartMaxLevelRequirement(Short.parseShort(value)));
                                                break;
                                            case "endmeso":
                                                quest.addProgressRequirement(new QuestProgressMoneyRequirement(Integer.parseInt(value)));
                                                break;
                                            case "order":
                                            case "notInTeleportItemLimitedField":
                                            case "anotherUserORCheck":
                                            case "damageOnFalling":
                                            case "hpR":
                                            case "dayByDay":
                                            case "QuestRecordAndOption":
                                            case "infoex":
                                            case "equipAllNeed":
                                            case "interval":
                                            case "interval_t":
                                            case "dayOfWeek":
                                            case "QuestOrOption":
                                            case "ItemOrOption":
                                            case "dayN":
                                            case "anotherUserCheckType":
                                            case "anotherUserCheck":
                                            case "userInteract":
                                            case "petRecallLimit":
                                            case "pettamenessmin":
                                            case "dayN_t":
                                            case "worldmin":
                                            case "worldmax":
                                            case "petAutoSpeakingLimit":
                                            case "name":
                                            case "multiKill":
                                            case "comboKill":
                                            case "job_JP":
                                            case "dayByDay_t":
                                            case "runeAct":
                                            case "weeklyRepeatResetDayOfWeek":
                                            case "weeklyRepeat":
                                            case "dressChanged":
                                            case "equipSelectNeed":
                                            case "infoAccount":
                                            case "infoAccountExt":
                                            case "breakTimeField":
                                            case "multiKillCount":
                                            case "randomGroupList":
                                            case "randomGroup":
                                            case "mbmin":
                                            case "duo":
                                            case "duoAssistPoint":
                                            case "wsrInfo":
                                            case "premium":
                                            case "dayOfWeek_t":
                                            case "nxInfo":
                                            case "episodeQuest":
                                            case "pvpGrade":
                                            case "vipStartGradeMin":
                                            case "vipStartGradeMax":
                                            case "vipStartAccount":
                                            case "dailyCommitment":
                                            case "purchasePeriodAbove":
                                            case "charisma": // Maybe implement later
                                            case "craft": // Maybe implement later
                                            case "gender": // it's 2018, so equal opportunity
                                            case "buff": // Maybe implement later
                                            case "exceptbuff": // Maybe implement later
                                                break;
                                            case "quest":
                                                for (Node idNode : XMLApi.getAllChildren(infoNode)) {
                                                    QuestStartCompletionRequirement qcr = new QuestStartCompletionRequirement();
                                                    qcr.setQuestStatus((byte) -1);
                                                    for (Node qn : XMLApi.getAllChildren(idNode)) {
                                                        String qnName = XMLApi.getNamedAttribute(qn, "name");
                                                        String qnVal = XMLApi.getNamedAttribute(qn, "value");
                                                        switch (qnName) {
                                                            case "id": qcr.setQuestID(Integer.parseInt(qnVal)); break;
                                                            case "state": qcr.setQuestStatus(Byte.parseByte(qnVal)); break;
                                                        }
                                                    }
                                                    if (qcr.getQuestStatus() != -1) quest.addRequirement(qcr);
                                                }
                                                break;

                                            case "pet":
                                                for (Node idNode : XMLApi.getAllChildren(infoNode)) {
                                                    QuestStartItemRequirement r = new QuestStartItemRequirement();
                                                    for (Node qn : XMLApi.getAllChildren(idNode)) {
                                                        String qnName = XMLApi.getNamedAttribute(qn, "name");
                                                        String qnVal = XMLApi.getNamedAttribute(qn, "value");
                                                        if ("id".equals(qnName)) r.setId(Integer.parseInt(qnVal));
                                                    }
                                                    quest.addRequirement(r);
                                                }
                                                break;

                                            case "job":
                                            case "job ":
                                            case "job_GL":
                                            case "job_TW":
                                                if (name.equals("job_TW") && net.swordie.ms.connection.protocol.Protocol.active().version() != net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) break;
                                                QuestStartJobRequirement qjr = new QuestStartJobRequirement();
                                                for (Node idNode : XMLApi.getAllChildren(infoNode)) {
                                                    qjr.addJobReq(Short.parseShort(XMLApi.getNamedAttribute(idNode, "value")));
                                                }
                                                quest.addRequirement(qjr);
                                                break;

                                            case "scenarioQuestList":
                                                for (Node idNode : XMLApi.getAllChildren(infoNode)) {
                                                    quest.addScenario(Integer.parseInt(XMLApi.getNamedAttribute(idNode, "value")));
                                                }
                                                break;

                                            case "fieldEnter":
                                                for (Node idNode : XMLApi.getAllChildren(infoNode)) {
                                                    quest.addFieldEnter(Integer.parseInt(XMLApi.getNamedAttribute(idNode, "value")));
                                                }
                                                break;

                                            case "mob":
                                                for (Node idNode : XMLApi.getAllChildren(infoNode)) {
                                                    QuestProgressMobRequirement qpmr = new QuestProgressMobRequirement();
                                                    qpmr.setOrder(Integer.parseInt(XMLApi.getNamedAttribute(idNode, "name")));
                                                    for (Node qn : XMLApi.getAllChildren(idNode)) {
                                                        String qnName = XMLApi.getNamedAttribute(qn, "name");
                                                        String qnVal = XMLApi.getNamedAttribute(qn, "value");
                                                        if ("id".equals(qnName)) qpmr.setMobID(Integer.parseInt(qnVal));
                                                        else if ("count".equals(qnName)) qpmr.setRequiredCount(Integer.parseInt(qnVal));
                                                    }
                                                    quest.addProgressRequirement(qpmr);
                                                }
                                                break;

                                            case "item":
                                                for (Node idNode : XMLApi.getAllChildren(infoNode)) {
                                                    QuestStartItemRequirement qir = new QuestStartItemRequirement();
                                                    QuestProgressItemRequirement qpir = new QuestProgressItemRequirement();
                                                    qpir.setOrder(Integer.parseInt(XMLApi.getNamedAttribute(idNode, "name")));
                                                    for (Node qn : XMLApi.getAllChildren(idNode)) {
                                                        String qnName = XMLApi.getNamedAttribute(qn, "name");
                                                        String qnVal = XMLApi.getNamedAttribute(qn, "value");
                                                        if ("id".equals(qnName)) {
                                                            if (status == 0) qir.setId(Integer.parseInt(qnVal));
                                                            else qpir.setItemID(Integer.parseInt(qnVal));
                                                        } else if ("count".equals(qnName)) {
                                                            if (status == 0) qir.setQuantity(Integer.parseInt(qnVal));
                                                            else qpir.setRequiredCount(Integer.parseInt(qnVal));
                                                        }
                                                    }
                                                    if (status == 0) quest.addRequirement(qir);
                                                    else quest.addProgressRequirement(qpir);
                                                }
                                                break;

                                            case "skill":
                                                for (Node idNode : XMLApi.getAllChildren(infoNode)) {
                                                    for (Node qn : XMLApi.getAllChildren(idNode)) {
                                                        String qnName = XMLApi.getNamedAttribute(qn, "name");
                                                        String qnVal = XMLApi.getNamedAttribute(qn, "value");
                                                        if ("id".equals(qnName)) quest.setSkill(Integer.parseInt(qnVal));
                                                    }
                                                }
                                                break;

                                            default:
                                                // ignore others as before
                                                break;
                                        }
                                    }
                                }
                            }

                            // -------- Act (rewards by status) --------
                            Node act = XMLApi.getFirstChildByNameBF(main, "Act");
                            if (act != null) {
                                for (Node statusNode : XMLApi.getAllChildren(act)) {
                                    String statusStr = XMLApi.getNamedAttribute(statusNode, "name"); // "0" or "1"
                                    byte status = Byte.parseByte(statusStr);
                                    for (Node rewardNode : XMLApi.getAllChildren(statusNode)) {
                                        String name = XMLApi.getNamedAttribute(rewardNode, "name");
                                        String value = XMLApi.getNamedAttribute(rewardNode, "value");
                                        switch (name) {
                                            case "transferField":
                                                quest.setTransferField(Integer.parseInt(value));
                                                break;
                                            case "nextQuest":
                                                quest.setNextQuest(Integer.parseInt(value));
                                                break;
                                            case "exp":
                                                quest.addReward(new QuestExpReward(Long.parseLong(value), ""));
                                                break;
                                            case "expTable":
                                                quest.addReward(new QuestExpReward(0, value));
                                                break;
                                            case "money":
                                                quest.addReward(new QuestMoneyReward(Long.parseLong(value)));
                                                break;
                                            case "pop":
                                                quest.addReward(new QuestPopReward(Integer.parseInt(value)));
                                                break;
                                            case "buffItemID":
                                                quest.addReward(new QuestBuffItemReward(Integer.parseInt(value), status));
                                                break;
                                            case "charismaEXP":
                                                quest.addReward(new QuestTraitReward(Stat.charismaEXP.getVal(), Integer.parseInt(value)));
                                                break;
                                            case "insightEXP":
                                                quest.addReward(new QuestTraitReward(Stat.insightEXP.getVal(), Integer.parseInt(value)));
                                                break;
                                            case "willEXP":
                                                quest.addReward(new QuestTraitReward(Stat.willEXP.getVal(), Integer.parseInt(value)));
                                                break;
                                            case "craftEXP":
                                                quest.addReward(new QuestTraitReward(Stat.craftEXP.getVal(), Integer.parseInt(value)));
                                                break;
                                            case "senseEXP":
                                                quest.addReward(new QuestTraitReward(Stat.senseEXP.getVal(), Integer.parseInt(value)));
                                                break;
                                            case "charmEXP":
                                                quest.addReward(new QuestTraitReward(Stat.charmEXP.getVal(), Integer.parseInt(value)));
                                                break;
                                            case "item":
                                                for (Node itemNode : XMLApi.getAllChildren(rewardNode)) {
                                                    QuestItemReward qir = new QuestItemReward();
                                                    qir.setStatus(status);
                                                    for (Node itemInfoNode : XMLApi.getAllChildren(itemNode)) {
                                                        String itemName = XMLApi.getNamedAttribute(itemInfoNode, "name");
                                                        String itemValue = XMLApi.getNamedAttribute(itemInfoNode, "value");
                                                        switch (itemName) {
                                                            case "id":
                                                                qir.setId(Integer.parseInt(itemValue));
                                                                break;
                                                            case "prop":
                                                                qir.setProp(Integer.parseInt(itemValue));
                                                                break;
                                                            case "count":
                                                                qir.setQuantity(Short.parseShort(itemValue));
                                                                break;
                                                            case "potentialGrade":
                                                                qir.setPotentialGrade(itemValue);
                                                                break;
                                                            case "gender":
                                                                qir.setGender(Integer.parseInt(itemValue));
                                                                break;
                                                            case "period":
                                                                qir.setPeriod(Integer.parseInt(itemValue));
                                                                break;
                                                            default:
                                                                if (LOG_UNKS) {
                                                                    System.out.printf("(%d) Unk item name %s with value %s status %d", questID, itemName, itemValue, status);
                                                                }
                                                                break;
                                                        }
                                                    }
                                                    quest.addReward(qir);
                                                }
                                                break;
                                            default:
                                                break;
                                        }
                                    }
                                }
                            }
                            baseQuests.put(questID, quest);
                        } catch (Exception e) {
                            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
                        }
                    });
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    public static void linkMobData() {
        if (baseQuests.isEmpty()) {
            loadQuestsFromWZ();
        }
        for (QuestInfo qi : baseQuests.values()) {
            for (QuestProgressMobRequirement qpmr :
                    qi.getQuestProgressRequirements()
                            .stream()
                            .filter(q -> q instanceof QuestProgressMobRequirement)
                            .map(q -> (QuestProgressMobRequirement) q)
                            .collect(Collectors.toSet())) { // readability is overrated
                Mob m = MobData.getMobById(qpmr.getMobID());
                if (m != null) {
                    m.addQuest(qi.getQuestID());
                    for (int childId : m.getMobSet()) {
                        Mob childMob = MobData.getMobById(childId);
                        if (childMob != null) {
                            childMob.addQuest(qi.getQuestID());
                        }
                    }
                }
            }
        }
    }

    public static void linkItemData() {
        if (baseQuests.isEmpty()) {
            loadQuestsFromWZ();
        }
        for (QuestInfo qi : baseQuests.values()) {
            for (QuestProgressItemRequirement qpmr :
                    qi.getQuestProgressRequirements()
                            .stream()
                            .filter(q -> q instanceof QuestProgressItemRequirement)
                            .map(q -> (QuestProgressItemRequirement) q)
                            .collect(Collectors.toSet())) { // readability is overrated
                int itemID = qpmr.getItemID();
                if (ItemConstants.isEquip(itemID)) {
                    // create new ItemInfos just for equips that are required for quests
                    // normally ItemInfo is just for non-equips.
                    ItemInfo ii = new ItemInfo();
                    ii.setItemId(itemID);
                    ii.setInvType(InvType.EQUIP);
                    ii.addQuest(qi.getQuestID());
                    ItemData.addItemInfo(ii);
                } else {
                    ItemInfo item = ItemData.getItemInfoByID(qpmr.getItemID());
                    if (item != null) {
                        item.addQuest(qi.getQuestID());
                    }
                }
            }
        }
    }

    public static void generateDatFiles() {
        System.out.println("Started generating quest data.");
        long start = System.currentTimeMillis();
        if (baseQuests.isEmpty()) {
            loadQuestsFromWZ();
        }
        saveAllQuestInfos(String.format("%s/quests", ServerConstants.DAT_DIR));
        System.out.printf("Completed generating quest data in %dms%n", System.currentTimeMillis() - start);
    }

    private static void saveAllQuestInfos(String dir) {
        Util.makeDirIfAbsent(dir);
        for (QuestInfo qi : baseQuests.values()) {
            File file = new File(String.format("%s/%d.dat", dir, qi.getQuestID()));
            try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
                dos.writeInt(qi.getQuestID());
                dos.writeInt(qi.getStartNpc());
                dos.writeBoolean(qi.isNormalAutoStart());
                dos.writeShort(qi.getQuestStartRequirements().size());
                for (QuestStartRequirement qsr : qi.getQuestStartRequirements()) {
                    dos.writeByte(QuestStartRequirementType.getQPRTByObj(qsr).getVal());
                    qsr.write(dos);
                }
                dos.writeShort(qi.getQuestProgressRequirements().size());
                for (QuestProgressRequirement qpr : qi.getQuestProgressRequirements()) {
                    dos.writeByte(QuestProgressRequirementType.getQPRTByObj(qpr).getVal());
                    qpr.write(dos);
                }
                dos.writeShort(qi.getQuestRewards().size());
                for (QuestReward qr : qi.getQuestRewards()) {
                    dos.writeByte(QuestRewardType.getQPRTByObj(qr).getVal());
                    qr.write(dos);
                }
                dos.writeShort(qi.getFieldEnters().size());
                for (int i : qi.getFieldEnters()) {
                    dos.writeInt(i);
                }
                dos.writeInt(qi.getInfoNumber());
                dos.writeLong(qi.getEnd());
                dos.writeLong(qi.getEndT());
                dos.writeUTF(qi.getStartScript());
                dos.writeUTF(qi.getEndScript());
                dos.writeLong(qi.getStart());
                dos.writeLong(qi.getStartT());
                dos.writeInt(qi.getEndNpc());
                dos.writeInt(qi.getSubJobFlags());
                dos.writeBoolean(qi.isCompleteNpcAutoGuide());
                dos.writeInt(qi.getSkill());
                dos.writeInt(qi.getFieldSetKeepTime());
                dos.writeUTF(qi.getFieldSet());
                dos.writeBoolean(qi.isAutoStart());
                dos.writeInt(qi.getDeathCount());
                dos.writeShort(qi.getScenarios().size());
                for (int i : qi.getScenarios()) {
                    dos.writeInt(i);
                }
                dos.writeShort(qi.getSpeech().size());
                for (Map.Entry<Integer, String> entry : qi.getSpeech().entrySet()) {
                    dos.writeInt(entry.getKey());
                    dos.writeUTF(entry.getValue());
                }
                dos.writeInt(qi.getMobDropMeso());
                dos.writeInt(qi.getMorph());
                dos.writeBoolean(qi.isSecret());
                dos.writeInt(qi.getTransferField());
                dos.writeInt(qi.getNextQuest());
                dos.writeBoolean(qi.isAutoComplete());
                dos.writeInt(qi.getMedalItemId());
                if (qi.getQuestName() != null) {
                    dos.writeUTF(qi.getQuestName());
                } else {
                    dos.writeUTF("");
                }
            } catch (IOException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            }
        }
    }

    private static QuestInfo getQuestInfo(int questID) {
        return baseQuests.get(questID);
    }

    private record NativeChecks(boolean verified, boolean emptyEnd, java.util.Set<String> endFields,
                                List<QuestStartCompletionRequirement> prerequisites, java.util.Set<Short> twJobs, boolean unknownGrouping, boolean jobBlacklist) {}
    private static final Map<Integer, NativeChecks> nativeChecks = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Set<Integer> patchedStarts = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final java.util.Set<String> END_NEUTRAL = java.util.Set.of("npc", "order", "endscript", "start", "end", "start_t", "end_t");

    private static Node child(Node node, String name) {
        if (node != null) for (Node candidate : XMLApi.getAllChildren(node)) if (name.equals(XMLApi.getNamedAttribute(candidate, "name"))) return candidate;
        return null;
    }

    private static NativeChecks nativeChecks(int id) {
        return nativeChecks.computeIfAbsent(id, key -> {
            File xml = new File("data/wz282/Quest.wz/QuestData/" + key + ".img.xml");
            if (!xml.isFile()) return new NativeChecks(false, false, java.util.Set.of(), List.of(), java.util.Set.of(), false, false);
            try {
                Node root = XMLApi.getRoot(xml);
                Node check = XMLApi.getFirstChildByNameBF(root, "Check");
                Node end = child(check, "1"), start = child(check, "0");
                java.util.Set<String> fields = new java.util.HashSet<>();
                if (end != null) for (Node child : XMLApi.getAllChildren(end)) fields.add(XMLApi.getNamedAttribute(child, "name"));
                List<QuestStartCompletionRequirement> prerequisites = new ArrayList<>();
                Node quest = child(start, "quest");
                if (quest != null) for (Node requirement : XMLApi.getAllChildren(quest)) {
                    Node qid = child(requirement, "id"), state = child(requirement, "state");
                    if (qid != null && state != null) prerequisites.add(new QuestStartCompletionRequirement(
                            Integer.parseInt(XMLApi.getNamedAttribute(qid, "value")), Byte.parseByte(XMLApi.getNamedAttribute(state, "value"))));
                }
                java.util.Set<Short> twJobs = new java.util.HashSet<>();
                Node jobs = child(start, "job_TW");
                if (jobs != null) for (Node job : XMLApi.getAllChildren(jobs)) twJobs.add(Short.parseShort(XMLApi.getNamedAttribute(job, "value")));
                Node option = child(start, "QuestOrOption");
                return new NativeChecks(true, END_NEUTRAL.containsAll(fields), java.util.Set.copyOf(fields), List.copyOf(prerequisites), java.util.Set.copyOf(twJobs),
                        option != null && !"0".equals(XMLApi.getNamedAttribute(option, "value")),
                        child(start, "editByBlacklist") != null && !"0".equals(XMLApi.getNamedAttribute(child(start, "editByBlacklist"), "value")));
            } catch (RuntimeException e) {return new NativeChecks(false, false, java.util.Set.of(), List.of(), java.util.Set.of(), false, false);}
        });
    }

    public static boolean hasUnsupportedStartGrouping(int id) {return nativeChecks(id).unknownGrouping();}

    private static void supplementNativeStart(QuestInfo qi) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version() != net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3 || patchedStarts.contains(qi.getQuestID())) return;
        synchronized (qi) {
            if (patchedStarts.contains(qi.getQuestID())) return;
            NativeChecks raw = nativeChecks(qi.getQuestID());
            if (raw.verified()) {
                qi.getQuestStartRequirements().removeIf(r -> r instanceof QuestStartCompletionRequirement);
                raw.prerequisites().forEach(qi::addRequirement);
                if (!raw.twJobs().isEmpty()) {
                    QuestStartJobRequirement tw = new QuestStartJobRequirement(); raw.twJobs().forEach(tw::addJobReq); qi.addRequirement(tw);
                }
                if (raw.jobBlacklist()) { // live 10-07: 504788 [師徒系統] (job 12006, 12200 + editByBlacklist) refused every other job
                    for (var r : qi.getQuestStartRequirements()) if (r instanceof QuestStartJobRequirement j) j.setBlacklist(true);
                }
            }
            patchedStarts.add(qi.getQuestID());
        }
    }

    /** An empty 265 progress list does not prove that the native Check-1 has no objectives. */
    public static boolean hasSupportedEndChecks(QuestInfo qi) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version() != net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) return true;
        NativeChecks raw = nativeChecks(qi.getQuestID());
        if (!raw.verified()) return !net.swordie.ms.loaders.overlay.DataOverlays.current().datFile("quests/" + qi.getQuestID() + ".dat").exists(); // server-defined custom quests
        for (String field : raw.endFields()) {
            if (END_NEUTRAL.contains(field)) continue;
            boolean modeled = switch (field) {
                case "mob" -> qi.getQuestProgressRequirements().stream().anyMatch(r -> r instanceof QuestProgressMobRequirement);
                case "item" -> qi.getQuestProgressRequirements().stream().anyMatch(r -> r instanceof QuestProgressItemRequirement);
                case "level" -> qi.getQuestProgressRequirements().stream().anyMatch(r -> r instanceof QuestProgressLevelRequirement);
                case "endmeso" -> qi.getQuestProgressRequirements().stream().anyMatch(r -> r instanceof QuestProgressMoneyRequirement);
                default -> false;
            };
            if (!modeled) return false;
        }
        return true;
    }

    public static boolean canAutoComplete(QuestInfo qi) {
        if (!qi.isAutoComplete() || !hasSupportedEndChecks(qi)) return false;
        NativeChecks raw = nativeChecks(qi.getQuestID());
        return !qi.getQuestProgressRequirements().isEmpty() || raw.emptyEnd()
                || !net.swordie.ms.loaders.overlay.DataOverlays.current().datFile("quests/" + qi.getQuestID() + ".dat").exists();
    }

    public static QuestInfo getQuestInfoById(int questID) {
        QuestInfo questInfo = getQuestInfo(questID);
        questInfo = questInfo == null ? loadQuestInfoById(questID) : questInfo;
        if (questInfo != null) supplementNativeStart(questInfo);
        return questInfo;
    }

    private static QuestInfo loadQuestInfoById(int questID) {
        // 282 profile: data/dat282 overlay first; 265 profile: exactly DAT_DIR/quests as before.
        File file = net.swordie.ms.loaders.overlay.DataOverlays.current().datFile("quests/" + questID + ".dat");
        boolean exists = file.exists();
        return exists ? loadQuestInfoByFile(file) : null;
    }

    private static QuestInfo loadQuestInfoByFile(File file) {
        QuestInfo qi = null;
        try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
            qi = new QuestInfo();
            qi.setQuestID(dis.readInt());
            qi.setStartNpc(dis.readInt());
            qi.setNormalAutoStart(dis.readBoolean());
            short size = dis.readShort();
            for (int i = 0; i < size; i++) {
                QuestStartRequirementType qsrt = QuestStartRequirementType.getQPRTByVal(dis.readByte());
                qi.addRequirement(qsrt.load(dis));
            }
            size = dis.readShort();
            for (int i = 0; i < size; i++) {
                QuestProgressRequirementType qprt = QuestProgressRequirementType.getQPRTByVal(dis.readByte());
                qi.addProgressRequirement(qprt.load(dis));
            }
            size = dis.readShort();
            for (int i = 0; i < size; i++) {
                QuestRewardType qr = QuestRewardType.getQPRTByVal(dis.readByte());
                qi.addReward(qr.load(dis));
            }
            size = dis.readShort();
            for (int i = 0; i < size; i++) {
                qi.addFieldEnter(dis.readInt());
            }
            qi.setInfoNumber(dis.readInt());
            qi.setEnd(dis.readLong());
            qi.setEndT(dis.readLong());
            qi.setStartScript(dis.readUTF());
            qi.setEndScript(dis.readUTF());
            qi.setStart(dis.readLong());
            qi.setStartT(dis.readLong());
            qi.setEndNpc(dis.readInt());
            qi.setSubJobFlags(dis.readInt());
            qi.setCompleteNpcAutoGuide(dis.readBoolean());
            qi.setSkill(dis.readInt());
            qi.setFieldSetKeepTime(dis.readInt());
            qi.setFieldSet(dis.readUTF());
            qi.setAutoStart(dis.readBoolean());
            qi.setDeathCount(dis.readInt());
            size = dis.readShort();
            for (int i = 0; i < size; i++) {
                qi.addScenario(dis.readInt());
            }
            size = dis.readShort();
            for (int i = 0; i < size; i++) {
                qi.addSpeech(dis.readInt(), dis.readUTF());
            }
            qi.setMobDropMeso(dis.readInt());
            qi.setMorph(dis.readInt());
            qi.setSecret(dis.readBoolean());
            qi.setTransferField(dis.readInt());
            qi.setNextQuest(dis.readInt());
            qi.setAutoComplete(dis.readBoolean());
            qi.setMedalItemId(dis.readInt());
            qi.setQuestName(dis.readUTF());
            baseQuests.put(qi.getQuestID(), qi);
        } catch (IOException e) {
            System.out.printf("[QuestInfo] IOException when loading %d.%n", qi.getQuestID());
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
        return qi;
    }

    public static Quest createQuestFromId(int questID, int charId) {
        QuestInfo qi = getQuestInfoById(questID);
        Quest quest = new Quest();
        quest.setCharId(charId);
        quest.setQRKey(questID);
        if (qi != null) {
            if (qi.isAutoComplete()) {
                quest.setStatus(QuestStatus.Started);
            } else {
                quest.setStatus(QuestStatus.Started);
            }
            for (QuestProgressRequirement qpr : qi.getQuestProgressRequirements()) {
                QuestProgressRequirement clone = qpr.deepCopy();
                quest.addQuestProgressRequirement(clone);
            }
        } else {
            quest.setStatus(QuestStatus.Started);
        }
        return quest;
    }

    public static AccountQuest createAccQuestFromId(int questID, int accid) {
        QuestInfo qi = getQuestInfoById(questID);
        AccountQuest quest = new AccountQuest();
        quest.setAccId(accid);
        quest.setQRKey(questID);
        if (qi != null) {
            if (qi.isAutoComplete()) {
                quest.setStatus(QuestStatus.Started);
            } else {
                quest.setStatus(QuestStatus.Started);
            }
            for (QuestProgressRequirement qpr : qi.getQuestProgressRequirements()) {
                QuestProgressRequirement clone = qpr.deepCopy();
                quest.addQuestProgressRequirement(clone);
            }
        } else {
            quest.setStatus(QuestStatus.Started);
        }
        return quest;
    }

    public static void main(String[] args) {
        generateDatFiles();
    }

    public static void clear() {
        baseQuests.clear();
        nativeChecks.clear();
        patchedStarts.clear();
    }

    public static void load() {
        if (baseQuests.isEmpty()) {
            loadQuestsFromWZ();
        }
    }
}
