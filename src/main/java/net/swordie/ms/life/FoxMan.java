package net.swordie.ms.life;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.BodyPart;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.connection.Encodable;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.FoxManPacket;
import net.swordie.ms.enums.EquipBaseStat;

public class FoxMan extends Life implements Encodable {

    private Char ownerChar;
    private short form = 0;
    private int upgrade;
    private boolean tranformed;

    public FoxMan(Char chr, boolean tranformed) {
        super(42101002);
        this.ownerChar = chr;
        this.tranformed = tranformed;
    }

    @Override
    public void encode(OutPacket outPacket) {
        outPacket.encodeShort(getForm());   // 1 = Haku Old Form,  0 = Haku New Form
        outPacket.encodePosition(getOwnerChar().getPosition());
        outPacket.encodeByte(getMoveAction());
        outPacket.encodeShort(0);
        outPacket.encodeInt(getUpgrade());
        outPacket.encodeInt(getEquip() != null ? getEquip().getItemId() : 0); //FanID Equipped by Haku
    }

    public Char getOwnerChar() {
        return ownerChar;
    }

    public void setOwnerChar(Char ownerChar) {
        this.ownerChar = ownerChar;
    }

    public int getOwnerId() {
        return ownerChar.getId();
    }

    public short getForm() {
        return form;
    }

    public void setForm(short form) {
        this.form = form;
    }

    public int getUpgrade() {
        return upgrade;
    }

    public void setUpgrade(int upgrade) {
        this.upgrade = upgrade;
    }

    public Equip getEquip() {
        if (getOwnerChar().getEquippedInventory().getItemBySlot(BodyPart.HakuFan.getVal()) != null) {
            return (Equip) getOwnerChar().getEquippedInventory().getItemBySlot(BodyPart.HakuFan.getVal());
        }
        return null;
    }

    public double getTotalMAD() {
        if (getEquip() != null) {
            return getEquip().getTotalStat(EquipBaseStat.iMAD);
        }
        return 0;
    }

    public boolean isTranformed() {
        return tranformed;
    }

    public void setTranformed(boolean tranformed) {
        this.tranformed = tranformed;
    }

    @Override
    public void broadcastSpawnPacket(Char onlyChar) {
        getField().broadcast(FoxManPacket.created(this));
        getField().broadcast(FoxManPacket.showChangeEffect(this));
    }

    @Override
    public void broadcastLeavePacket() {
        getField().broadcast(FoxManPacket.removed(this));
    }

}
