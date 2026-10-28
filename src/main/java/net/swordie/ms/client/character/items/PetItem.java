package net.swordie.ms.client.character.items;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.life.pet.Pet;
import net.swordie.ms.life.pet.PetSkill;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.util.FileTime;

import java.util.List;

public class PetItem extends Item {

    private String name;
    private byte level;
    private short tameness;
    private byte repleteness; // hungry thing
    private short petAttribute;
    private int petSkill;
    private FileTime dateDead;
    private int remainLife;
    private short attribute;
    private byte activeState;
    private int autoBuffSkill;
    private int petHue;
    private short giantRate;
    private List<Integer> exceptionList;

    public void insertPetItemToSQL(long id) {
        String query = "INSERT INTO `petitems` (" +
                "`itemid`, " +
                "`name`, " +
                "`charid`, " +
                "`level`, " +
                "`tameness`, " +
                "`repleteness`, " +
                "`petattribute`, " +
                "`petskill`, " +
                "`datedead`, " +
                "`remainlife`, " +
                "`attribute`, " +
                "`activestate`, " +
                "`autobuffskill`, " +
                "`pethue`, " +
                "`giantrate`,  " +
                "`exceptionList` " +
                ") VALUES (" +
                String.format("%d, ", id) +
                String.format("'%s', ", DatabaseManager.getStringFilter(getName())) +
                (getCharID() != 0 ? String.format("%d, ", getCharID()) : "NULL, ") +
                String.format("%d, ", getLevel()) +
                String.format("%d, ", getTameness()) +
                String.format("%d, ", getRepleteness()) +
                String.format("%d, ", getPetAttribute()) +
                String.format("%d, ", getPetSkill()) +
                DatabaseManager.getSQLStringSyntax(false, "", getDateDead(), false) +
                String.format("%d, ", getRemainLife()) +
                String.format("%d, ", getAttribute()) +
                String.format("%d, ", getActiveState()) +
                String.format("%d, ", getAutoBuffSkill()) +
                String.format("%d, ", getPetHue()) +
                String.format("%d, ", getGiantRate()) +
                DatabaseManager.getSQLStringSyntax(false, "", getPetExceptionList(), true) +
                ");";
        DatabaseManager.executeStatement(query);
    }

    public void updatePetItemToSQL() {
        String query = "UPDATE petitems SET " +
                String.format("name = '%s', ", DatabaseManager.getStringFilter(getName())) +
                (getCharID() != 0 ? String.format("charid = %d, ", getCharID()) : "charid = NULL, ") +
                String.format("level = %d, ", getLevel()) +
                String.format("tameness = %d, ", getTameness()) +
                String.format("repleteness = %d, ", getRepleteness()) +
                String.format("petattribute = %d, ", getPetAttribute()) +
                String.format("petskill = %d, ", getPetSkill()) +
                DatabaseManager.getSQLStringSyntax(true, "datedead", getDateDead(), false) +
                String.format("remainlife = %d, ", getRemainLife()) +
                String.format("attribute = %d, ", getAttribute()) +
                String.format("activestate = %d, ", getActiveState()) +
                String.format("autobuffskill = %d, ", getAutoBuffSkill()) +
                String.format("pethue = %d, ", getPetHue()) +
                String.format("giantrate = %d, ", getGiantRate()) +
                DatabaseManager.getSQLStringSyntax(true, "exceptionList", getPetExceptionList(), true) +
                String.format(" WHERE itemid = %d;", getId());
        DatabaseManager.executeStatement(query);
    }

    private String getPetExceptionList() {
        if (getExceptionList() == null) {
            return null;
        }
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < getExceptionList().size(); i++) {
            if (i != getExceptionList().size() - 1) {
                value.append(getExceptionList().get(i)).append(",");
            } else {
                value.append(getExceptionList().get(i));
            }
        }
        return value.toString();
    }

    public PetItem() {
        super();
    }

    public PetItem deepCopy() {
        PetItem ret = new PetItem();
        ret.itemId = itemId;
        ret.bagIndex = bagIndex;
        ret.cashItemSerialNumber = cashItemSerialNumber;
        ret.dateExpire = dateExpire;
        ret.invType = invType;
        ret.isCash = isCash;
        ret.type = type;
        ret.owner = owner;
        ret.quantity = quantity;
        ret.expireOnLogout = expireOnLogout;
        ret.obtainedOnce = obtainedOnce;
        ret.name = name;
        ret.level = level;
        ret.tameness = tameness;
        ret.repleteness = repleteness;
        ret.petAttribute = petAttribute;
        ret.petSkill = petSkill;
        ret.dateDead = dateDead;
        ret.remainLife = remainLife;
        ret.attribute = attribute;
        ret.activeState = activeState;
        ret.autoBuffSkill = autoBuffSkill;
        ret.petHue = petHue;
        ret.giantRate = giantRate;
        ret.exceptionList = exceptionList == null ? null : new java.util.ArrayList<>(exceptionList);
        return ret;
    }

    @Override
    public Type getType() {
        return Type.PET;
    }

    public void encode(OutPacket outPacket) {
        super.encode(outPacket);
        outPacket.encodeString(getName(), 13);
        outPacket.encodeByte(getLevel());
        outPacket.encodeShort(getTameness() ); // closeness
        outPacket.encodeByte(getRepleteness()); // fullness
        outPacket.encodeFT(getDateExpire()); // 0 = no date dead
        outPacket.encodeShort(getPetAttribute());
        outPacket.encodeShort(getPetSkill());
        outPacket.encodeInt(getRemainLife());
        outPacket.encodeShort(getAttribute());
        outPacket.encodeByte(getActiveState());
        outPacket.encodeInt(getPetHue());
        outPacket.encodeShort(getGiantRate());
        outPacket.encodeShort(0);
        outPacket.encodeInt(0);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte getLevel() {
        return level;
    }

    public void setLevel(byte level) {
        this.level = level;
    }

    public short getTameness() {
        return tameness;
    }

    public void setTameness(short tameness) {
        this.tameness = tameness;
    }

    public byte getRepleteness() {
        return repleteness;
    }

    public void setRepleteness(byte repleteness) {
        this.repleteness = repleteness;
    }

    public short getPetAttribute() {
        return petAttribute;
    }

    public void setPetAttribute(short petAttribute) {
        this.petAttribute = petAttribute;
    }

    public int getPetSkill() {
        return petSkill;
    }

    public void setPetSkill(int petSkill) {
        this.petSkill = petSkill;
    }

    public FileTime getDateDead() {
        return dateDead;
    }

    public void setDateDead(FileTime dateDead) {
        this.dateDead = dateDead;
    }

    public int getRemainLife() {
        return remainLife;
    }

    public void setRemainLife(int remainLife) {
        this.remainLife = remainLife;
    }

    public short getAttribute() {
        return attribute;
    }

    public void setAttribute(short attribute) {
        this.attribute = attribute;
    }

    public byte getActiveState() {
        return activeState;
    }

    public void setActiveState(byte activeState) {
        this.activeState = activeState;
    }

    public int getAutoBuffSkill() {
        return autoBuffSkill;
    }

    public void setAutoBuffSkill(int autoBuffSkill) {
        this.autoBuffSkill = autoBuffSkill;
    }

    public int getPetHue() {
        return petHue;
    }

    public void setPetHue(int petHue) {
        this.petHue = petHue;
    }

    public short getGiantRate() {
        return giantRate;
    }

    public void setGiantRate(short giantRate) {
        this.giantRate = giantRate;
    }

    public Pet createPet(Char chr) {
        Pet pet = new Pet(getItemId(), chr.getId());
        // own copy of the position (sharing the owner's object moved the pet with every owner update), and the foothold
        // below it: the owner's foothold is stale until its first move after a warp / login (spawned-object audit 09-29)
        pet.setPosition(chr.getPosition() == null ? null : chr.getPosition().deepCopy());
        var below = chr.getField() == null || chr.getPosition() == null ? null : chr.getField().findFootHoldBelow(chr.getPosition());
        pet.setFh(below != null ? below.getId() : chr.getFoothold());
        int chosenIdx = getActiveState() - 1;
        if (chosenIdx < 0 || chosenIdx >= net.swordie.ms.constants.GameConstants.MAX_PET_AMOUNT
                || chr.getPetByIdx(chosenIdx) != null) {
            chosenIdx = chr.getFirstPetIdx();
        }
        if (chosenIdx == -1) {
            throw new IllegalStateException("No free pet slot");
        }
        setActiveState((byte) (chosenIdx + 1));
        pet.setIdx(chosenIdx);
        pet.setName(getName());
        pet.setPetLockerSN(getId());
        pet.setHue(getPetHue());
        pet.setWonderGrade(ItemData.getPetInfoByID(getItemId()).getWonderGrade());
        pet.setGiantRate(getGiantRate());
        pet.setItem(this);
        return pet;
    }

    public boolean canSummon() {
        return getQuantity() > 0 && (getDateDead() == null || !getDateDead().isExpired())
                && (getDateExpire() == null || !getDateExpire().isExpired());
    }

    public void addPetSkill(PetSkill petSkill) {
        setPetSkill(getPetSkill() | petSkill.getVal());
    }

    public void removePetSkill(PetSkill petSkill) {
        if (hasPetSkill(petSkill)) {
            setPetSkill(getPetSkill() ^ petSkill.getVal());
        }
    }

    public boolean hasPetSkill(PetSkill petSkill) {
        return (getPetSkill() & petSkill.getVal()) != 0;
    }

    public List<Integer> getExceptionList() {
        return exceptionList;
    }

    public void setExceptionList(List<Integer> exceptionList) {
        this.exceptionList = exceptionList;
    }
}
