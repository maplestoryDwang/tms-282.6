package net.swordie.ms.handlers.life;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.AndroidPacket;
import net.swordie.ms.connection.packet.UserRemote;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.life.Android;
import net.swordie.ms.life.movement.MovementInfo;
import net.swordie.ms.util.Position;

public class AndroidHandler {

    @Handler(op = InHeader.ANDROID_MOVE)
    public static void handleAndroidMove(Char chr, InPacket inPacket) {
        Android android = chr.getAndroid();
        if (android == null) {
            return;
        }
        inPacket.decodeInt(); // 0
        MovementInfo mi = new MovementInfo(inPacket);
        mi.applyTo(android);
        chr.getField().broadcast(AndroidPacket.move(android, mi), chr);
    }

    @Handler(op = InHeader.ANDROID_ACTION_SET)
    public static void handleAndroidActionSet(Char chr, InPacket inPacket) {
        Android android = chr.getAndroid();
        if (android == null) {
            return;
        }
        byte action = inPacket.decodeByte();
        byte randomKey = inPacket.decodeByte();
        chr.getField().broadcast(AndroidPacket.actionSet(android, action, randomKey));
    }

    @Handler(op = InHeader.ANDROID_EMOTION)
    public static void handleAndroidEmotion(Char chr, InPacket inPacket) {
        int emotion = inPacket.decodeInt();
        int duration = inPacket.decodeInt();
        if (chr.getAndroid() != null) {
            chr.getField().broadcast(UserRemote.androidEmotion(chr.getId(), emotion, duration), chr);
        }
    }

    @Handler(op = InHeader.USER_ANDROID_SHOP)
    public static void handleAndroidShop(Char chr, InPacket inPacket) {
        int charID = inPacket.decodeInt();
        int templateID = inPacket.decodeInt();
        Position position = inPacket.decodePositionInt();
        Android android = chr.getAndroid();
        if (android == null || chr.getId() != charID) {
            return;
        }
        chr.getScriptManager().openShop(templateID, GameConstants.GENERAL_SHOP);
    }
}
