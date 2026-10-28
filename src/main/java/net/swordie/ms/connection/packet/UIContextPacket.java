package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.daily.DailyGift;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.enums.DimensionMirrorType;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.List;

import static net.swordie.ms.client.character.MonsterPark.*;

public class UIContextPacket {

    public static OutPacket dailyGiftInit(DailyGift dailyGift, int type, int itemID) {
        OutPacket outPacket = new OutPacket(OutHeader.DAILY_GIFT_INIT);

        outPacket.encodeByte(type);
        if (type == 1) {
            outPacket.encodeInt(itemID);
        } else if (type == 2) {
            outPacket.encodeInt(type);
            outPacket.encodeInt(itemID);
        } else if (type == 0) {
            boolean bool = true;
            outPacket.encodeByte(bool);
            if (bool) {
                dailyGift.encode(outPacket);
            }
            int size = 0;
            outPacket.encodeInt(size);
            for (int i = 0; i < size; i++) {
                dailyGift.encode(outPacket);
            }
        }

        return outPacket;
    }

    public static OutPacket unityPortalResult() {
        OutPacket outPacket = new OutPacket(OutHeader.DIMENSIONAL_MIRROR);

        outPacket.encodeInt(DimensionMirrorType.values().length);
        for (DimensionMirrorType unityPortal : DimensionMirrorType.values()) {
            outPacket.encodeString(unityPortal.getName());
            outPacket.encodeString(unityPortal.getDesc());
            outPacket.encodeInt(unityPortal.getReqLevel());
            outPacket.encodeInt(0);// unk
            outPacket.encodeInt(unityPortal.getReqQuest());
            outPacket.encodeInt(unityPortal.getQuestToSave());
            outPacket.encodeInt(unityPortal.getRewards().length);
            for (Integer reward : unityPortal.getRewards()) {
                outPacket.encodeInt(reward);
            }
        }

        return outPacket;
    }

    public static OutPacket openDailyQuestBoard(Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.DAILY_QUEST_BOARD);

        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(Integer.parseInt(chr.getQRValueByKey(62150, "q1")));
        outPacket.encodeInt(Integer.parseInt(chr.getQRValueByKey(62150, "q2")));
        outPacket.encodeInt(Integer.parseInt(chr.getQRValueByKey(62150, "q3")));
        outPacket.encodeInt(Integer.parseInt(chr.getQRValueByKey(62150, "q4")));
        outPacket.encodeInt(Integer.parseInt(chr.getQRValueByKey(62150, "q5")));

        return outPacket;
    }

    public static OutPacket ArcLevelUpEffect(int ArcSlot) {
        OutPacket outPacket = new OutPacket(OutHeader.ARCANE_SYMBOL_ENHANCE);

        outPacket.encodeInt(1);
        outPacket.encodeInt(0);
        outPacket.encodeInt(ArcSlot);

        return outPacket;
    }

    public static OutPacket AutLevelUpEffect(int AutSlot) {
        OutPacket outPacket = new OutPacket(OutHeader.SACRED_SYMBOL_ENHANCE);

        outPacket.encodeInt(1);
        outPacket.encodeInt(0);
        outPacket.encodeInt(AutSlot);

        return outPacket;
    }

    public static OutPacket monsterParkUI(int type, int uiType, int errorType) {
        OutPacket outPacket = new OutPacket(OutHeader.MONSTER_PARK_UI);

        outPacket.encodeInt(type);
        if (type == 2) {
            outPacket.encodeInt(errorType);
            if (errorType == 16) {
                outPacket.encodeString("#b你已用完所有免費次數。#k\\n需要 #r600 楓點#k 或 #r怪物公園免費入場券#k 才能再次入場。");
            } else if (errorType == 15) {
                outPacket.encodeString("請先退出隊伍再入場。");
            } else if (errorType == 1) {
                outPacket.encodeString("發生未知的問題，無法入場。請稍後再試。");
            }
        } else if (type == 1) {
            outPacket.encodeInt(1);
            outPacket.encodeInt(uiType);
            outPacket.encodeInt(1); // free turn
            outPacket.encodeInt(6); // ticket turn
            outPacket.encodeInt(freeTicket);
            outPacket.encodeInt(CSTicket);
            outPacket.encodeInt(CSTicketPrice);
            List<GameConstants.MonsterParkInfo> mpis = GameConstants.getMonsterParkInfos(uiType);
            outPacket.encodeInt(mpis.size());
            for (GameConstants.MonsterParkInfo mpi : mpis) {
                outPacket.encodeInt(mpi.getType());
                outPacket.encodeInt(mpi.getIndex());
                outPacket.encodeString(mpi.getName());
                outPacket.encodeString(mpi.getDesc());
                outPacket.encodeInt(mpi.getMinLevel());
                outPacket.encodeInt(mpi.getMaxLevel());
                outPacket.encodeInt(mpi.getReqQuest());
            }
        }

        return outPacket;
    }
}
