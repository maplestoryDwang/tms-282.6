package net.swordie.ms.handlers.script;


import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.constants.QuestConstants;
import net.swordie.ms.enums.MedalReissueResultType;
import net.swordie.ms.enums.QuestType;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.QuestData;
import net.swordie.ms.loaders.containerclasses.QuestInfo;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.scripts.ScriptType;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.Position;
import net.swordie.ms.world.field.Field;

import java.util.ArrayList;
import java.util.List;

import static net.swordie.ms.enums.ChatType.SystemNotice;

public class QuestHandler {

    @Handler(op = InHeader.USER_QUEST_REQUEST)
    public static void handleUserQuestRequest(Char chr, InPacket inPacket) {
        byte type = inPacket.decodeByte();
        int questID = 0;
        int npcTemplateID = 0;
        Position position = null;
        QuestType qt = QuestType.getQTFromByte(type);
        boolean success = false;
        if (qt != null) {
            switch (qt) {
                case QuestReq_AcceptQuest: // Quest start
                case QuestReq_CompleteQuest: // Quest end
                case QuestReq_OpeningScript: // Scripted quest start
                case QuestReq_CompleteScript: // Scripted quest end
                    questID = inPacket.decodeInt();
                    npcTemplateID = inPacket.decodeInt();
                    if (inPacket.getUnreadAmount() > 4) {
                        position = inPacket.decodePosition();
                    }
                    break;
                case QuestReq_ResignQuest: //Quest forfeit
                    questID = inPacket.decodeInt();
                    chr.removeQuest(questID);
                    break;
                case QuestReq_LaterStep:
                    questID = inPacket.decodeInt();
                    break;
                default:
                    System.out.printf("Unhandled quest request %s!%n", qt);
                    break;
            }
        }
        if (questID == 0 || qt == null) {
            chr.chatMessage(SystemNotice, String.format("[任務錯誤] 找不到任務 %d。", questID));
            return;
        }
        QuestInfo qi = QuestData.getQuestInfoById(questID);
        if (qi == null) {
            return;
        }
        switch (qt) {
            case QuestReq_AcceptQuest:
                if (chr.canStartQuest(questID) && !QuestConstants.isIgnoredQuests(questID)) {
                    if (QuestConstants.isAccountQuest(questID)) {
                        chr.getAccount().addQuest(QuestData.createAccQuestFromId(questID, chr.getAccount().getId()));
                    } else {
                        chr.addQuest(QuestData.createQuestFromId(questID, chr.getId()));
                    }
                    success = true;
                }
                break;
            case QuestReq_CompleteQuest:
                chr.completeQuest(questID);
                success = true;
                break;
            case QuestReq_OpeningScript:
                String scriptName = qi.getStartScript();
                if (!chr.canStartQuest(questID)) {
                    return;
                }
                if (scriptName == null || scriptName.equalsIgnoreCase("")) {
                    scriptName = String.format("q%d%s", questID, ScriptManagerImpl.QUEST_START_SCRIPT_END_TAG);
                }
                chr.getScriptManager().startScript(chr, questID, scriptName, ScriptType.Quest);
                break;
            case QuestReq_CompleteScript:
                scriptName = qi.getEndScript();
                if (QuestConstants.isAccountQuest(questID)) {
                    if (!chr.hasQuestInProgress(questID) || !chr.getAccount().getQuestById(questID).isComplete(chr)) {
                        System.out.println("Could not complete account quest, as the prerequisites haven't been met.");
                        return;
                    }
                } else {
                    if (!chr.hasQuestInProgress(questID) || !chr.getQuestById(questID).isComplete(chr)) {
                        System.out.println("Could not complete character quest, as the prerequisites haven't been met.");
                        return;
                    }
                }
                if (scriptName == null || scriptName.equalsIgnoreCase("")) {
                    scriptName = String.format("q%d%s", questID, ScriptManagerImpl.QUEST_COMPLETE_SCRIPT_END_TAG);
                }
                chr.getScriptManager().startScript(chr, questID, scriptName, ScriptType.Quest);
                break;
            case QuestReq_LaterStep:
                if (qi != null) {
                    if (questID == 3714) {
                        chr.getScriptManager().startScript(chr, questID, "q3714e", ScriptType.Quest);
                    }
                    if (qi.getTransferField() != 0) {
                        Field field = chr.getOrCreateFieldByCurrentInstanceType(qi.getTransferField());
                        chr.warp(field);
                    }
                }
                break;
        }
        if (success) {
            //chr.write(UserLocal.questResult(QuestType.QuestRes_Act_Success, questID, npcTemplateID, 0, false));
        }
    }

    @Handler(op = InHeader.USER_MEDAL_REISSUE_REQUEST)
    public static void handleUserMedalReissueRequest(Char chr, InPacket inPacket) {
        int questId = inPacket.decodeInt();
        int medalItemId = inPacket.decodeInt();
        long actualMesoCost;
        int count = 0;
        if (chr.getQRValue(QuestConstants.MEDAL_REISSUE_QUEST).contains("count=")) {
            String countString = chr.getQRValue(QuestConstants.MEDAL_REISSUE_QUEST).replace("count=", "");
            count = Integer.parseInt(countString);
        } else {
            chr.createQuestWithQRValue(QuestConstants.MEDAL_REISSUE_QUEST, "");
        }
        switch (count) {
            case 0:
                actualMesoCost = 100;
                break;
            case 1:
                actualMesoCost = 1000;
                break;
            case 2:
                actualMesoCost = 10000;
                break;
            case 3:
                actualMesoCost = 100000;
                break;
            default:
                actualMesoCost = 1000000;
                break;
        }
        if (QuestData.getQuestInfoById(questId).getMedalItemId() != medalItemId || !(ItemConstants.isMedal(medalItemId))) {
            DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("Character %d tried to reissue an item {%d} that isn't a medal or tried to reissue a medal from a quest {%d} that doesn't give the given medal", chr.getId(), medalItemId, questId));

        } else if (!chr.hasQuestCompleted(questId)) {
            DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("Character %d tried to reissue a medal from a quest {%d} which they have not completed.", chr.getId(), questId));

        } else if (ItemData.getItemDeepCopy(medalItemId) == null || QuestData.getQuestInfoById(questId) == null) {
            chr.write(UserLocal.medalReissueResult(MedalReissueResultType.Unknown, medalItemId));

        } else if (chr.getMoney() < actualMesoCost) {
            chr.write(UserLocal.medalReissueResult(MedalReissueResultType.NoMoney, medalItemId));

        } else if (!chr.canHold(medalItemId)) {
            chr.write(UserLocal.medalReissueResult(MedalReissueResultType.NoSlot, medalItemId));

        } else if (chr.hasItem(medalItemId)) {
            chr.write(UserLocal.medalReissueResult(MedalReissueResultType.AlreadyHas, medalItemId));

        } else {
            count++;
            chr.setQRValue(QuestConstants.MEDAL_REISSUE_QUEST, "count=" + count);
            chr.deductMoney(actualMesoCost);
            chr.addItemToInventory(QuestData.getQuestInfoById(questId).getMedalItemId(), 1);
            chr.write(UserLocal.medalReissueResult(MedalReissueResultType.Success, medalItemId));
        }
        chr.dispose();
    }

    @Handler(op = InHeader.ELODIN_SMALL_BOTTLE_CHECK)
    public static void handleElodinSmallBottleCheck(Char chr, InPacket inPacket) {
        int itemID = inPacket.decodeInt();
    }

    @Handler(op = InHeader.ELODIN_SMALL_BOTTLE_SETUP)
    public static void handleElodinSmallBottleSetup(Char chr, InPacket inPacket) {
        int itemID = inPacket.decodeInt();
        byte status = inPacket.decodeByte();
        if (itemID == 4220196) {
            chr.setQRValue(37169, status + "", false);
            if (status == 1) {
                chr.consumeItem(4036503, 1);
            }
        } else if (itemID == 4220197) {
            chr.setQRValue(37173, status + "", false);
            if (status == 1) {
                chr.consumeItem(4036505, 1);
            }
        } else if (itemID == 4220198) {
            chr.setQRValue(37176, status + "", false);
            if (status == 1) {
                chr.consumeItem(4036506, 1);
            }
        }
    }

    @Handler(op = InHeader.ELODIN_PURE_WATER)
    public static void handleElodinPureWater(Char chr, InPacket inPacket) {
        int toItemID = inPacket.decodeInt();
        short newStatus = inPacket.decodeShort();
        int fromItemID = inPacket.decodeInt();
        int questID = inPacket.decodeInt();
        if (toItemID == 4220196 && fromItemID == 4036503) {
            int count = Integer.parseInt(chr.getQRValue(questID));
            count += 1;
            chr.setQRValue(questID, count + "", false);
            chr.consumeItem(fromItemID, 1);
            chr.dispose();
        } else if (toItemID == 4220197 && fromItemID == 4036505) {
            int count = Integer.parseInt(chr.getQRValue(questID));
            count += 1;
            chr.setQRValue(questID, count + "", false);
            chr.consumeItem(fromItemID, 1);
            chr.dispose();
        } else if (toItemID == 4220198 && fromItemID == 4036506) {
            int count = Integer.parseInt(chr.getQRValue(questID));
            count += 1;
            chr.setQRValue(questID, count + "", false);
            chr.consumeItem(fromItemID, 1);
            chr.dispose();
        }
    }

    @Handler(op = InHeader.REWARD_MOB_LIST_RESULT)
    public static void handleRewardMobListResult(Char chr, InPacket inPacket) {
        List<Integer> items = new ArrayList<>();
        short size = inPacket.decodeShort();
        for (int i = 0; i < size; i++) {
            items.add(inPacket.decodeInt());
        }
        //chr.write(UserLocal.getRewardMobListResult(items));
    }
}
