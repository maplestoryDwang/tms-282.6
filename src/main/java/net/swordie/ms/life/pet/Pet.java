package net.swordie.ms.life.pet;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.PetItem;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.PetPacket;
import net.swordie.ms.life.Life;
import net.swordie.ms.loaders.ItemData;

import java.util.Map;

/**
 * Created on 12/20/2017.
 */
public class Pet extends Life {
    private final int ownerID;
    private int id;
    private int idx;
    private String name;
    private long petLockerSN;
    private int hue = -1;
    private short wonderGrade;
    private short giantRate = 100;
    private boolean transformed;
    private boolean reinforced;
    private PetItem item;

    public Pet(int templateId, int ownerID) {
        super(templateId);
        this.ownerID = ownerID;
    }

    public int getActiveSkillCoolTime() {
        return 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdx() {
        return idx;
    }

    public void setIdx(int idx) {
        this.idx = idx;
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(getTemplateId());
        outPacket.encodeString(getName());
        outPacket.encodeLong(getItem().getId());
        outPacket.encodePosition(getPosition());
        outPacket.encodeByte(getMoveAction());
        outPacket.encodeShort(getFh());
        outPacket.encodeInt(getHue()); // -1
        outPacket.encodeInt(getTemplateId());
        outPacket.encodeShort(getWonderGrade());
        outPacket.encodeInt(getGiantRate());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getPetLockerSN() {
        return petLockerSN;
    }

    public void setPetLockerSN(long petLockerSN) {
        this.petLockerSN = petLockerSN;
    }

    public int getHue() {
        return hue;
    }

    public void setHue(int hue) {
        this.hue = hue;
    }

    public short getWonderGrade() {
        return wonderGrade;
    }

    public void setWonderGrade(short wonderGrade) {
        this.wonderGrade = wonderGrade;
    }

    public short getGiantRate() {
        return giantRate;
    }

    public void setGiantRate(short giantRate) {
        this.giantRate = giantRate;
    }

    public boolean isTransformed() {
        return transformed;
    }

    public void setTransformed(boolean transformed) {
        this.transformed = transformed;
    }

    public boolean isReinforced() {
        return reinforced;
    }

    public void setReinforced(boolean reinforced) {
        this.reinforced = reinforced;
    }

    public PetItem getItem() {
        return item;
    }

    public void setItem(PetItem item) {
        this.item = item;
    }

    @Override
    public void broadcastSpawnPacket(Char onlyChar) {
        onlyChar.write(PetPacket.activated(this));
    }

    @Override
    public void broadcastLeavePacket() {
        getField().broadcast(PetPacket.deactivated(getOwnerID(), getIdx()));
    }

    public int getOwnerID() {
        return ownerID;
    }

    public boolean canConsume(int foodID) {
        if (!ItemData.getPetFoods().isEmpty()) {
            for (Int2IntMap.Entry food : ItemData.getPetFoods().int2IntEntrySet()) {
                if (getItem().getItemId() == food.getIntValue() && foodID == food.getIntKey()) {
                    return true;
                }
            }
        }
        return false;
    }
}
