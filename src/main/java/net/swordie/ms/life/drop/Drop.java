package net.swordie.ms.life.drop;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.packet.DropPool;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.DropMotionType;
import net.swordie.ms.enums.DropType;
import net.swordie.ms.life.Life;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.containerclasses.ItemInfo;
import net.swordie.ms.util.FileTime;

public class Drop extends Life {

    private Item item;
    private int money;
    private DropType dropType;
    private int ownerID = 0;
    private boolean explosiveDrop;
    private boolean specialDrop;
    private boolean canBePickedUpByPet;
    private FileTime expireTime;
    private long mobExp;
    private DropMotionType dropMotionType;
    private byte moneyType;
    private boolean byPickPocket = false;

    public Drop(int templateId) {
        super(templateId);
        dropMotionType = DropMotionType.Normal;
        canBePickedUpByPet = true;
    }

    public Drop(int templateId, Item item) {
        super(templateId);
        this.item = item;
        dropType = DropType.Item;
        dropMotionType = DropMotionType.Normal;
        expireTime = FileTime.MIN_TIME();
    }

    public Drop(int templateId, int money) {
        super(templateId);
        this.money = money;
        dropType = DropType.Mesos;
        dropMotionType = DropMotionType.Normal;
        expireTime = FileTime.MIN_TIME();
    }

    public DropType getDropType() {
        return dropType;
    }

    public void setDropType(DropType dropType) {
        this.dropType = dropType;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
        setDropType(DropType.Item);
    }

    public int getMoney() {
        return money;
    }

    public boolean isMoney() {
        return getDropType() == DropType.Mesos;
    }

    public void setMoney(int money) {
        this.money = money;
        setDropType(DropType.Mesos);
    }

    public int getOwnerID() {
        return ownerID;
    }

    public void setOwnerID(int ownerID) {
        this.ownerID = ownerID;
    }

    public boolean isExplosiveDrop() {
        return explosiveDrop;
    }

    public void setExplosiveDrop(boolean explosiveDrop) {
        this.explosiveDrop = explosiveDrop;
    }

    public boolean isSpecialDrop() {
        return specialDrop;
    }

    public void setSpecialDrop(boolean specialDrop) {
        this.specialDrop = specialDrop;
    }

    public FileTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(FileTime expireTime) {
        this.expireTime = expireTime;
    }

    public byte getItemGrade() {
        byte res = 0;
        if (getItem() != null && getItem() instanceof Equip) {
            res = (byte) ((Equip) getItem()).getGrade();
        }
        return res;
    }

    @Override
    public void broadcastSpawnPacket(Char onlyChar) {
        Item item = getItem();
        ItemInfo ii = null;
        if (item != null) {
            ii = ItemData.getItemInfoByID(item.getItemId());
        }
        boolean canSpawn = isMoney()
                || (item != null && ItemConstants.isEquip(item.getItemId()))
                || (ii != null && onlyChar.hasAnyQuestsInProgress(ii.getQuestIDs()));
        if (canSpawn) {
            onlyChar.write(DropPool.dropEnterField(this, getPosition(), getOwnerID(), canBePickedUpBy(onlyChar)));
        }
    }

    public long getMobExp() {
        return mobExp;
    }

    public void setMobExp(long mobExp) {
        this.mobExp = mobExp;
    }

    public boolean canBePickedUpBy(Char chr) {
        int owner = getOwnerID();
        return owner == chr.getId() ||
                (chr.getParty() != null && chr.getParty().hasPartyMember(owner))
                || owner == 0;
    }

    public boolean canBePickedUpByPet() {
        return canBePickedUpByPet;
    }

    public void setCanBePickedUpByPet(boolean canBePickedUpByPet) {
        this.canBePickedUpByPet = canBePickedUpByPet;
    }

    public DropMotionType getDropMotionType() {
        return dropMotionType;
    }

    public void setDropMotionType(DropMotionType dropMotionType) {
        this.dropMotionType = dropMotionType;
    }

    public byte getMoneyType() {
        return moneyType;
    }

    public void setMoneyType(byte moneyType) {
        this.moneyType = moneyType;
    }

    public void setByPickPocket(boolean byPickPocket) {
        this.byPickPocket = byPickPocket;
    }

    public boolean isByPickPocket() {
        return byPickPocket;
    }
}
