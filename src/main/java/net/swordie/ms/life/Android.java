package net.swordie.ms.life;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.BodyPart;
import net.swordie.ms.client.character.items.Inventory;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.Encodable;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.AndroidPacket;
import net.swordie.ms.loaders.containerclasses.AndroidInfo;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.util.Util;

import java.util.ArrayList;
import java.util.List;

public class Android extends Life implements Encodable {

    public static final int ANDROID_ITEM_SIZE = 7;

    private Char ownerChar;
    private short skin;
    private short hair;
    private short face;
    private String name;

    public Android(int templateId) {
        super(templateId);
    }

    public Android(int templateId, Char ownerChar, short skin, short hair, short face, String name) {
        super(templateId);
        this.ownerChar = ownerChar;
        this.skin = skin;
        this.hair = hair;
        this.face = face;
        this.name = name;
    }

    public Android(Char chr, AndroidInfo androidInfo) {
        super(androidInfo.getId());
        this.ownerChar = chr;
        this.skin = (short) (int) Util.getRandomFromCollection(androidInfo.getSkins()); // double cast...
        this.hair = (short) (int) Util.getRandomFromCollection(androidInfo.getHairs());
        this.face = (short) (int) Util.getRandomFromCollection(androidInfo.getFaces());
        this.name = "Android";
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(getTemplateId());
        outPacket.encodePosition(getPosition());
        outPacket.encodeByte(getMoveAction());
        outPacket.encodeShort(getFh());
        outPacket.encodeInt(0); // 370595
        encodeAndroidInfo(outPacket);
        final List<Integer> androidItems = getItems(); // size always 7
        for (int itemId : androidItems) {
            outPacket.encodeInt(itemId);
            outPacket.encodeInt(0); // Fusion Anvil
            outPacket.encodeByte(0);
        }
    }

    public void encodeAndroidInfo(OutPacket outPacket) {
        outPacket.encodeShort(getSkin());
        outPacket.encodeInt(getHair());
        outPacket.encodeInt(getFace());
        outPacket.encodeString(getName());
        outPacket.encodeInt(0); // new v207
        outPacket.encodeFT(FileTime.MIN_TIME());
        outPacket.encodeInt(0);
    }

    public Char getOwnerChar() {
        return ownerChar;
    }

    public void setOwnerChar(Char ownerChar) {
        this.ownerChar = ownerChar;
    }

    public short getSkin() {
        return skin;
    }

    public void setSkin(short skin) {
        this.skin = skin;
    }

    public short getHair() {
        return hair;
    }

    public void setHair(short hair) {
        this.hair = hair;
    }

    public short getFace() {
        return face;
    }

    public void setFace(short face) {
        this.face = face;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getOwnerId() {
        return ownerChar.getId();
    }

    public List<Integer> getItems() {
        List<Integer> items = new ArrayList<>(14);
        Inventory inv = getOwnerChar().getEquippedInventory();
        for (int i = BodyPart.APBase.getVal(); i < BodyPart.APEnd.getVal(); i++) {
            Item item = inv.getItemBySlot(i);
            items.add(item == null ? 0 : item.getItemId());
        }
        return items;
    }

    @Override
    public void broadcastSpawnPacket(Char onlyChar) {
        OutPacket outPacket = AndroidPacket.created(this);
        if (onlyChar == null) {
            // Field-wide create = the owner just equipped the android / heart (Char.equip -> initAndroid -> spawnLife),
            // which runs BEFORE the inventory operation that moves the item into its slot is sent. The 282 client
            // then builds the CAndroid while its android / heart slot is still empty and dies (0xE06D7363; every
            // LP044F of research/tms2823/android-trace/EQUIP-CRASH.md was sent that way). Send it a moment later so
            // the slot update arrives first, and only if this android is still the owner's one in the same field.
            var field = getField();
            net.swordie.ms.Server.get().getEventTimer().addEvent(() -> {
                Char owner = getOwnerChar();
                if (owner != null && owner.getAndroid() == this && owner.getField() == field && getField() == field) {
                    field.broadcast(outPacket);
                }
            }, 200, java.util.concurrent.TimeUnit.MILLISECONDS);
        } else {
            onlyChar.write(outPacket);
        }
    }

    @Override
    public void broadcastLeavePacket() {
        getField().broadcast(AndroidPacket.removed(this));
    }
}
