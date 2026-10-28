package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.life.Android;
import net.swordie.ms.life.movement.MovementInfo;
import net.swordie.ms.util.FileTime;

public class AndroidPacket {

    public static OutPacket created(Android android) {
        OutPacket outPacket = new OutPacket(OutHeader.ANDROID_CREATED);

        outPacket.encodeInt(android.getOwnerId());
        outPacket.encode(android);

        return outPacket;
    }

    public static OutPacket removed(Android android) {
        OutPacket outPacket = new OutPacket(OutHeader.ANDROID_REMOVED);

        outPacket.encodeInt(android.getOwnerId());

        return outPacket;
    }

    public static OutPacket move(Android android, MovementInfo mi) {
        OutPacket outPacket = new OutPacket(OutHeader.ANDROID_MOVE);

        outPacket.encodeInt(android.getOwnerId());
        mi.encode(outPacket);

        return outPacket;
    }

    public static OutPacket actionSet(Android android, int action, int randomKey) {
        OutPacket outPacket = new OutPacket(OutHeader.ANDROID_ACTION_SET);

        outPacket.encodeInt(android.getOwnerId());
        outPacket.encodeByte(action);
        outPacket.encodeByte(randomKey);

        return outPacket;
    }

    public static OutPacket modified(Android android) {
        OutPacket outPacket = new OutPacket(OutHeader.ANDROID_MODIFIED);

        outPacket.encodeInt(android.getOwnerId());
        // from the right: 1st 7 bits for each equip, 8th bit for face+eye+hair+name
        // 0xFF is a full update
        outPacket.encodeShort(0xFF);
        for (int itemId : android.getItems()) {
            outPacket.encodeInt(itemId);
            outPacket.encodeInt(0);
            outPacket.encodeByte(0);
        }
        outPacket.encodeFT(FileTime.currentTime());
        android.encodeAndroidInfo(outPacket);

        return outPacket;
    }
}
