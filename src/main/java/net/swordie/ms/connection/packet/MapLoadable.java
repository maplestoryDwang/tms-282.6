package net.swordie.ms.connection.packet;

import net.swordie.ms.Server;
import net.swordie.ms.ServerConfig;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.enums.SpineMsgType;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.world.field.MapTaggedObject;

import java.util.Collections;
import java.util.Set;

import static net.swordie.ms.ServerConstants.version;

public class MapLoadable {

    public static OutPacket setBackEffect(byte effect, int fieldID, int pageID, int duration) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_BACK_EFFECT);

        outPacket.encodeByte(effect);
        outPacket.encodeInt(fieldID);
        outPacket.encodeByte(pageID);
        outPacket.encodeInt(duration);

        return outPacket;
    }

    public static OutPacket setMapTaggedObjectVisisble(MapTaggedObject object) {
        return setMapTaggedObjectVisisble(Collections.singleton(object));
    }

    public static OutPacket setMapTaggedObjectVisisble(Set<MapTaggedObject> objects) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_MAP_TAGGED_OBJECT_VISIBLE);

        outPacket.encodeInt(objects.size());
        for (MapTaggedObject mto : objects) {
            outPacket.encode(mto);
        }

        return outPacket;
    }

    public static OutPacket setMapTaggedObjectVisibles(int type) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_MAP_TAGGED_OBJECT_VISIBLE);

        outPacket.encodeByte(4);// count
        outPacket.encodeString("A0");
        outPacket.encodeByte(false);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeString("A1");
        outPacket.encodeByte(type == 1);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeString("A2");
        outPacket.encodeByte(type == 2);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeString("A3");
        outPacket.encodeByte(type == 3);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket setMapTaggedObjectVisible(String tagName, boolean isVisible, int manual, int delay) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_MAP_TAGGED_OBJECT_VISIBLE);

        outPacket.encodeInt(1);// count
        outPacket.encodeString(tagName); // <string name="tags" value="..." />
        outPacket.encodeByte(isVisible);
        outPacket.encodeInt(manual);
        outPacket.encodeInt(delay);

        return outPacket;
    }

    public static OutPacket setMapTaggedObjectVisibleLogin(String tagName) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_MAP_TAGGED_OBJECT_VISIBLE_LOGIN);

        outPacket.encodeString(tagName);

        return outPacket;
    }

    public static OutPacket setMapTaggedObjectAnimation(String tagName, int type) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_MAP_TAGGED_OBJECT_ANIMATION);

        outPacket.encodeInt(1);
        outPacket.encodeString(tagName);
        outPacket.encodeInt(type);// Gr2dAniType

        return outPacket;
    }

    public static OutPacket setMapObjectVisible(String tagName, boolean isVisible) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_MAP_OBJECT_VISIBLE);

        outPacket.encodeString(tagName);
        outPacket.encodeInt(isVisible ? 1 : 0);

        return outPacket;
    }

    public static OutPacket setSpineObjectEffectAlpha(boolean back, String key, int alpha, int delay) {
        OutPacket outPacket = new OutPacket(back ? OutHeader.SET_SPINE_BACK_EFFECT : OutHeader.SET_SPINE_OBJECT_EFFECT);

        outPacket.encodeString(key);
        outPacket.encodeInt(SpineMsgType.ALPHA.getVal());
        outPacket.encodeInt(alpha);
        outPacket.encodeInt(delay);

        return outPacket;
    }

    public static OutPacket setSpineObjectEffectPlay(boolean back, String key, String name, boolean loop, boolean randomStart) {
        OutPacket outPacket = new OutPacket(back ? OutHeader.SET_SPINE_BACK_EFFECT : OutHeader.SET_SPINE_OBJECT_EFFECT);

        outPacket.encodeString(key);
        outPacket.encodeInt(SpineMsgType.PLAY.getVal());
        outPacket.encodeString(name);
        outPacket.encodeByte(loop);
        outPacket.encodeByte(randomStart);

        return outPacket;
    }

    public static OutPacket setSpineObjectEffectAddPlay(boolean back, String key, String name, boolean loop) {
        OutPacket outPacket = new OutPacket(back ? OutHeader.SET_SPINE_BACK_EFFECT : OutHeader.SET_SPINE_OBJECT_EFFECT);

        outPacket.encodeString(key);
        outPacket.encodeInt(SpineMsgType.ADD_PLAY.getVal());
        outPacket.encodeString(name);
        outPacket.encodeByte(loop);

        return outPacket;
    }

    public static OutPacket setSpineObjectEffectClearTracks(boolean back, String key, boolean setupPose) {
        OutPacket outPacket = new OutPacket(back ? OutHeader.SET_SPINE_BACK_EFFECT : OutHeader.SET_SPINE_OBJECT_EFFECT);

        outPacket.encodeString(key);
        outPacket.encodeInt(SpineMsgType.CLEAR_TRACKS.getVal());
        outPacket.encodeByte(setupPose);

        return outPacket;
    }

    public static OutPacket setSpineObjectEffectPlayrate(boolean back, String key, int scale) {
        OutPacket outPacket = new OutPacket(back ? OutHeader.SET_SPINE_BACK_EFFECT : OutHeader.SET_SPINE_OBJECT_EFFECT);

        outPacket.encodeString(key);
        outPacket.encodeInt(SpineMsgType.PLAYRATE.getVal());
        outPacket.encodeInt(scale);

        return outPacket;
    }

    public static OutPacket setSpineObjectEffectStop(boolean back, String key, boolean setupPose) {
        OutPacket outPacket = new OutPacket(back ? OutHeader.SET_SPINE_BACK_EFFECT : OutHeader.SET_SPINE_OBJECT_EFFECT);

        outPacket.encodeString(key);
        outPacket.encodeInt(SpineMsgType.STOP.getVal());
        outPacket.encodeByte(setupPose);

        return outPacket;
    }

    public static OutPacket reloadBack() {
        return new OutPacket(OutHeader.RELOAD_BACK);
    }
}
