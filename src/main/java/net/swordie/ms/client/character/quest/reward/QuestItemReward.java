package net.swordie.ms.client.character.quest.reward;

import net.swordie.ms.client.Account;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.enums.ItemGrade;
import net.swordie.ms.loaders.DatSerializable;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.containerclasses.ItemInfo;
import net.swordie.ms.util.FileTime;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;

public class QuestItemReward implements QuestReward {
    private int id;
    private short quantity;
    private String potentialGrade = "normal";
    private int potentialCount;
    private int status;
    private int prop;
    private int gender;
    private int period;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public short getQuantity() {
        return quantity;
    }

    public void setQuantity(short quantity) {
        this.quantity = quantity;
    }

    public void giveReward(Char chr, String questName) {
        if (getQuantity() < 0) {
            chr.consumeItem(getId(), -getQuantity());
        } else {
            giveItem(chr, questName);
        }
    }

    @Override
    public void giveReward(Char chr) {
        // no use
    }

    private void giveItem(Char chr, String questName) {
        if (getPotentialGrade() != null && ItemConstants.isEquip(getId())) {
            if (chr.getInventoryByType(InvType.EQUIP).getEmptySlots() >= 1) {
                Equip equip = ItemData.getEquipDeepCopyFromID(getId(), false);
                equip.setQuantity(getQuantity());
                if (ItemConstants.canEquipHavePotential(equip)) {
                    ItemGrade grade = ItemGrade.None;
                    if (getPotentialGrade().equalsIgnoreCase("Rare")) {
                        grade = ItemGrade.HiddenRare;
                    } else if (getPotentialGrade().equalsIgnoreCase("Epic")) {
                        grade = ItemGrade.HiddenEpic;
                    } else if (getPotentialGrade().equalsIgnoreCase("Unique")) {
                        grade = ItemGrade.HiddenUnique;
                    }
                    if (grade != ItemGrade.None) {
                        equip.setHiddenOptionBase(grade.getVal(), ItemConstants.THIRD_LINE_CHANCE);
                    }
                }
                if (getPeriod() > 0) {
                    equip.setDateExpire(FileTime.fromDate(LocalDateTime.now().plusMinutes(getPeriod())));
                }
                chr.addItemToInventory(equip);
            } else {
                chr.sendRewardToChar(getId(), getQuantity(), 0, "背包已滿，因此完成任務 " + questName + " 的獎勵將通過郵件發送。", 30);
            }
        } else {
            Item item = ItemData.getItemDeepCopy(getId());
            item.setQuantity(getQuantity());
            if (chr.getInventoryByType(item.getInvType()).getEmptySlots() >= 1) {
                if (getPeriod() > 0) {
                    item.setDateExpire(FileTime.fromDate(LocalDateTime.now().plusMinutes(getPeriod())));
                }
                chr.addItemToInventory(item);
            } else {
                chr.sendRewardToChar(getId(), getQuantity(), 0, "背包已滿，因此完成任務 " + questName + " 的獎勵將通過郵件發送。", 30);
            }
        }
    }

    @Override
    public void giveReward(Account account, String questName) {
        Char currentChr = account.getCurrentChr();
        ItemInfo ii = ItemData.getItemInfoByID(getId());
        if (ii == null) {
            return;
        }
        if (currentChr != null) {
            if (getQuantity() < 0) {
                currentChr.consumeItem(getId(), -getQuantity());
            } else {
                giveItem(currentChr, questName);
            }
            if (getQuantity() > 0) {
                if (ii.isTradeBlock()) {
                    for (Char chr : account.getCharacters()) {
                        if (chr.getId() != currentChr.getId()) {
                            if (chr.getRewardSystem() == null) {
                                chr.initRewardSystem();
                            }
                            chr.sendRewardToChar(getId(), getQuantity(), 0, "角色 " + currentChr.getName() + " 已完成任務 " + questName + "。", 30);
                        }
                    }
                }
            }
        }
    }

    public String getPotentialGrade() {
        return potentialGrade;
    }

    public void setPotentialGrade(String potentialGrade) {
        this.potentialGrade = potentialGrade;
    }

    public int setPotentialCount() {
        return potentialCount;
    }

    public void setPotentialCount(int potentialCount) {
        this.potentialCount = potentialCount;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getProp() {
        return prop;
    }

    public void setProp(int prop) {
        this.prop = prop;
    }

    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    @Override
    public void write(DataOutputStream dos) throws IOException {
        dos.writeInt(getId());
        dos.writeShort(getQuantity());
        dos.writeUTF(getPotentialGrade());
        dos.writeInt(getStatus());
        dos.writeInt(getProp());
        dos.writeInt(getGender());
        dos.writeInt(getPeriod());
    }

    @Override
    public DatSerializable load(DataInputStream dis) throws IOException {
        QuestItemReward qir = new QuestItemReward();
        qir.setId(dis.readInt());
        qir.setQuantity(dis.readShort());
        qir.setPotentialGrade(dis.readUTF());
        qir.setStatus(dis.readInt());
        qir.setProp(dis.readInt());
        qir.setGender(dis.readInt());
        qir.setPeriod(dis.readInt());
        return qir;
    }
}
