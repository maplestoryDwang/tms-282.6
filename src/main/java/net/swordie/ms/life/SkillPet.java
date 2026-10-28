package net.swordie.ms.life;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.Encodable;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.SkillPetPacket;

public class SkillPet extends Life implements Encodable {

    private Char ownerChar;
    private int skillID;
    private byte actionType;
    private byte action;
    private byte state; // 0: removed, 1: created, 2: hided

    public SkillPet(int templateId) {
        super(templateId);
    }

    public SkillPet(Char chr) {
        super(40020109);
        this.ownerChar = chr;
    }

    @Override
    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(getObjectId());
        outPacket.encodeInt(getTemplateId());
        outPacket.encodeByte(getState());
        outPacket.encodePosition(getPosition());
        outPacket.encodeShort(getMoveAction());
        outPacket.encodeShort(getFh());
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

    public int getSkillID() {
        return skillID;
    }

    public void setSkillID(int skillID) {
        this.skillID = skillID;
    }

    public byte getActionType() {
        return actionType;
    }

    public void setActionType(byte actionType) {
        this.actionType = actionType;
    }

    public byte getAction() {
        return action;
    }

    public void setAction(byte action) {
        this.action = action;
    }

    public byte getState() {
        return state;
    }

    public void setState(byte state) {
        this.state = state;
    }

    @Override
    public void broadcastSpawnPacket(Char onlyChar) {
        // at the owner, on the foothold below it (position / foothold were never set), then the state: a state for a
        // pet the client does not have yet was ignored (pet-dragon-fox-trace README, 09-29)
        Char owner = getOwnerChar();
        if (owner != null && owner.getPosition() != null) {
            setPosition(owner.getPosition().deepCopy());
            var below = getField().findFootHoldBelow(owner.getPosition());
            if (below != null) {
                setFh(below.getId());
            }
        }
        setState((byte) 1);
        getField().broadcast(SkillPetPacket.created(this));
        getField().broadcast(SkillPetPacket.state(this));
    }

    @Override
    public void broadcastLeavePacket() {
        // state 2 only: sending the create again after it re-added the pet
        setState((byte) 2);
        getField().broadcast(SkillPetPacket.state(this));
    }
}
