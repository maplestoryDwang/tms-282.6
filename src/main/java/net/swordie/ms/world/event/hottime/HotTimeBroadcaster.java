package net.swordie.ms.world.event.hottime;

import net.swordie.ms.Server;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.BroadcastMsg;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.ChatType;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.loaders.ItemData;

import java.util.ArrayList;
import java.util.List;

/**
 * HotTime／排程公告共用的全服廣播與發獎工具。
 */
final class HotTimeBroadcaster {

    private HotTimeBroadcaster() {
    }

    /** 目前可安全操作的線上角色（有 client、不在換頻/商城遷移中、已在地圖內、非機器人）。 */
    static List<Char> onlineChars() {
        List<Char> list = new ArrayList<>();
        for (Char chr : Server.get().getChars().values()) {
            if (chr == null || chr.isBot()) {
                continue;
            }
            Client c = chr.getClient();
            if (c == null || c.isMigrating() || chr.getField() == null) {
                continue;
            }
            list.add(chr);
        }
        return list;
    }

    static void worldNotice(String msg) {
        if (msg == null || msg.isEmpty()) {
            return;
        }
        Server.get().broadcastForWorld(WvsContext.broadcastMsg(BroadcastMsg.notice(msg)));
    }

    static void slide(String msg, boolean show) {
        if (msg == null) {
            return;
        }
        Server.get().broadcastForWorld(WvsContext.broadcastMsg(BroadcastMsg.slideNotice(msg, show)));
    }

    static void popup(String msg) {
        if (msg == null || msg.isEmpty()) {
            return;
        }
        Server.get().broadcastForWorld(WvsContext.broadcastMsg(BroadcastMsg.popUpMessage(msg)));
    }

    /** 與 WvsContext.weatherEffectNotice 相同編碼，但允許任意數值 type（如 273 的 328、371）。 */
    static void weather(String msg, int type, int duration) {
        if (msg == null || msg.isEmpty()) {
            return;
        }
        OutPacket outPacket = new OutPacket(OutHeader.WEATHER_EFFECT_NOTICE);
        outPacket.encodeString(msg);
        outPacket.encodeInt(type);
        outPacket.encodeInt(duration);
        outPacket.encodeByte(1);
        Server.get().broadcastForWorld(outPacket);
    }

    static void chat(String msg, int chatTypeVal) {
        if (msg == null || msg.isEmpty()) {
            return;
        }
        ChatType type = ChatType.Tip;
        for (ChatType ct : ChatType.values()) {
            if (ct.getVal() == chatTypeVal) {
                type = ct;
                break;
            }
        }
        for (Char chr : onlineChars()) {
            try {
                chr.chatMessage(type, msg);
            } catch (Exception ignored) {
            }
        }
    }

    static void send(String style, String msg, HotTimeEntry e) {
        String s = style == null ? "notice" : style.toLowerCase();
        switch (s) {
            case "chat" -> chat(msg, e.chatType);
            case "slide" -> slide(msg, true);
            case "weather" -> weather(msg, e.weatherType, e.weatherDuration);
            case "popup" -> popup(msg);
            default -> worldNotice(msg);
        }
    }

    static boolean itemExists(int itemId) {
        if (itemId <= 0) {
            return false;
        }
        try {
            if (ItemConstants.isEquip(itemId)) {
                return ItemData.getEquipById(itemId) != null;
            }
            return ItemData.getItemDeepCopy(itemId) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 發放獎勵給所有線上角色。背包不足則略過並記錄。
     *
     * @return 成功發放人數
     */
    static int giveReward(HotTimeEntry e) {
        int itemId = e.rewardItem;
        int count = Math.max(1, e.rewardCount);
        if (!itemExists(itemId)) {
            System.err.printf("[HotTime] %s 的獎勵道具 %d 不存在，略過發獎。%n", e.key(), itemId);
            return 0;
        }
        boolean equip = ItemConstants.isEquip(itemId);
        int given = 0;
        for (Char chr : onlineChars()) {
            try {
                if (chr.getLevel() < e.rewardMinLevel) {
                    continue;
                }
                if (!chr.canHold(itemId, count)) {
                    System.out.printf("[HotTime] %s 背包已滿，略過發放 %d x%d (%s)%n", chr.getName(), itemId, count, e.key());
                    chr.chatMessage(ChatType.Tip, "[熱門時段] 背包空間不足，無法領取熱門時段獎勵。");
                    continue;
                }
                if (equip) {
                    for (int i = 0; i < count; i++) {
                        chr.addItemToInventory(itemId, 1);
                    }
                } else {
                    chr.addItemToInventory(itemId, count);
                }
                chr.chatMessage(ChatType.Tip, "[熱門時段] 已獲得熱門時段獎勵！");
                given++;
            } catch (Exception ex) {
                System.err.printf("[HotTime] 發放獎勵給 %s 失敗: %s%n", chr.getName(), ex);
            }
        }
        return given;
    }
}
