package net.swordie.ms.client.social.Party;

import net.swordie.ms.Server;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;

import java.util.LinkedList;
import java.util.List;

public class PartyMember {
    private int id;
    private int charID;
    private String name;
    private short job;
    private short subJob;
    private int level;
    private int channel;
    private int fieldID;
    private boolean isOnline;
    private int arcaneForce;
    private int sacredForce;
    private int dojangRank;
    private long dojangRankTime;
    private int totalUnion;
    private byte[] packedAvatar = new byte[120];

    private Char chr;
    private TownPortal townPortal;

    public PartyMember(Char chr) {
        this.chr = chr;
        updateInfoByChar(chr);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCharID() {
        return charID;
    }

    public void setCharID(int charID) {
        this.charID = charID;
    }

    public String getCharName() {
        return name;
    }

    public short getJob() {
        return job;
    }

    public void setJob(short job) {
        this.job = job;
    }

    public short getSubJob() {
        return subJob;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean isOnline) {
        this.isOnline = isOnline;
    }

    public Char getChr() {
        if (chr == null) {
            return Server.get().getWorld().getCharById(charID);
        }
        return chr;
    }

    public void setChr(Char chr) {
        this.chr = chr;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getFieldID() {
        return fieldID;
    }

    public void setFieldID(int fieldID) {
        this.fieldID = fieldID;
    }

    public TownPortal getTownPortal() {
        return townPortal;
    }

    public void setTownPortal(TownPortal townPortal) {
        this.townPortal = townPortal;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSubJob(short subJob) {
        this.subJob = subJob;
    }

    public void updateInfoByChar(Char chr) {
        if (chr != null) {
            this.chr = chr;
            setCharID(chr.getId());
            setName(chr.getName());
            setJob(chr.getJob());
            setSubJob((short) chr.getSubJob());
            setLevel(chr.getLevel());
            setChannel(chr.getClient() == null ? -1 : chr.getClient().getChannel());
            setFieldID(chr.getFieldID());
            setOnline(chr.isOnline());
            setArcaneForce(chr.getTotalArc());
            setSacredForce(chr.getTotalAut());
            setDojangRank(141);
            setDojangRankTime(975);
            if (chr.getTownPortal() != null) {
                setTownPortal(new TownPortal(
                        chr.getTownPortal().getTownFieldId(),
                        chr.getTownPortal().getFieldFieldId(),
                        chr.getTownPortal().getSkillid(),
                        chr.getTownPortal().getTownPosition()
                ));
            } else {
                setTownPortal(new TownPortal(
                        999999999,
                        999999999,
                        0,
                        chr.getPosition()
                ));
            }
            setPackedAvatar(chr.getAvatarData().getAvatarLook().getPackedCharacterLook());
        } else {
            setFieldID(0);
            setChannel(-1);
        }
    }

    public List<Integer> getChangedTypes(Char chr) {
        List<Integer> changedTypes = new LinkedList<>();
        if (chr != null) {
            if (!chr.getName().equalsIgnoreCase(getCharName())) {
                changedTypes.add(0);
            }
            if (chr.getJob() != getJob()) {
                changedTypes.add(1);
            }
            if (chr.getLevel() != getLevel()) {
                changedTypes.add(2);
            }
            if (chr.getClient() == null) {
                if (getChannel() != -1) {
                    changedTypes.add(3);
                }
            } else if (chr.getClient().getChannel() != getChannel()) {
                changedTypes.add(3);
            }
            if (chr.getTotalArc() != getArcaneForce() || chr.getTotalAut() != getSacredForce()) {
                changedTypes.add(6);
            }
            if (chr.getAvatarData().getAvatarLook().getPackedCharacterLook() != getPackedAvatar()) {
                changedTypes.add(8);
            }
        } else {
            changedTypes.add(3);
        }
        return changedTypes;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PartyMember && getChr() != null && ((PartyMember) obj).getCharID() == getChr().getId();
    }

    public int getArcaneForce() {
        return arcaneForce;
    }

    public void setArcaneForce(int arcaneForce) {
        this.arcaneForce = arcaneForce;
    }

    public int getSacredForce() {
        return sacredForce;
    }

    public void setSacredForce(int sacredForce) {
        this.sacredForce = sacredForce;
    }

    public int getDojangRank() {
        return dojangRank;
    }

    public void setDojangRank(int dojangRank) {
        this.dojangRank = dojangRank;
    }

    public long getDojangRankTime() {
        return dojangRankTime;
    }

    public void setDojangRankTime(long dojangRankTime) {
        this.dojangRankTime = dojangRankTime;
    }

    public int getTotalUnion() {
        return totalUnion;
    }

    public void setTotalUnion(int totalUnion) {
        this.totalUnion = totalUnion;
    }

    public byte[] getPackedAvatar() {
        return packedAvatar;
    }

    public void setPackedAvatar(byte[] packedAvatar) {
        this.packedAvatar = packedAvatar;
    }

    public void encodeVal(OutPacket outPacket, int valType) {
        switch (valType) {
            case 0:
                outPacket.encodeString(getCharName());
                break;
            case 1:
                outPacket.encodeInt(getJob());
                break;
            case 2:
                outPacket.encodeInt(getLevel());
                break;
            case 3:
                outPacket.encodeInt(getChannel() - 1);
                break;
            case 4:
                outPacket.encodeByte(0);
                break;
            case 5:
                outPacket.encodeByte(0);
                break;
            case 6:
                outPacket.encodeInt(getArcaneForce());
                outPacket.encodeInt(getSacredForce());
                outPacket.encodeInt(getDojangRank());
                outPacket.encodeLong(getDojangRankTime());
                break;
            case 7:
                outPacket.encodeInt(0);
                break;
            case 8:
                outPacket.encodeArr(getPackedAvatar());
                break;
        }
    }
}
