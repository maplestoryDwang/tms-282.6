package net.swordie.ms.client.character.avatar;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.WeaponType;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.util.DataPrinter;

import java.sql.*;
import java.util.*;

public class AvatarLook {

    private int id;
    private String name;
    private int gender;
    private int skin;
    private int face;
    private int hair;
    private int weaponStickerId;
    private int weaponId;
    private int subWeaponId;
    private List<Integer> hairEquips;
    private List<Integer> unseenEquips;
    private List<Integer> petIDs;
    private int job;
    private boolean drawElfEar;
    private int demonSlayerDefFaceAcc;
    private int xenonDefFaceAcc;
    private int beastTamerDefFaceAcc;
    private boolean isZeroBetaLook;
    private int mixedHairColor;
    private int mixHairPercent;
    private List<Integer> totems;
    private int ears;
    private int tail;
    private int demonWingID;
    private int kaiserWingID;
    private int kaiserTailID;
    private boolean isDressUp;

    public static AvatarLook getAvatarLookFromSQLByAvatarDataID(int avatarDataID) {
        AvatarLook avatarLook = null;
        String query = "SELECT al.*, he.equipid as he_equipid, ue.equipid as ue_equipid, " +
                "p.petid as p_petid, t.totemid as t_totemid " +
                "FROM avatarlook al " +
                "LEFT JOIN hairequips he ON al.id = he.alid " +
                "LEFT JOIN unseenequips ue ON al.id = ue.alid " +
                "LEFT JOIN petids p ON al.id = p.alid " +
                "LEFT JOIN totems t ON al.id = t.alid " +
                "WHERE al.id = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, avatarDataID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    avatarLook = new AvatarLook();
                    avatarLook.setId(rs.getInt("id"));
                    avatarLook.setGender(rs.getInt("gender"));
                    avatarLook.setSkin(rs.getInt("skin"));
                    avatarLook.setFace(rs.getInt("face"));
                    avatarLook.setHair(rs.getInt("hair"));
                    avatarLook.setWeaponStickerId(rs.getInt("weaponstickerid"));
                    avatarLook.setWeaponId(rs.getInt("weaponid"));
                    avatarLook.setSubWeaponId(rs.getInt("subweaponid"));
                    avatarLook.setJob(rs.getInt("job"));
                    avatarLook.setDrawElfEar(rs.getByte("drawelfear") != 0);
                    avatarLook.setDemonSlayerDefFaceAcc(rs.getInt("demonslayerdeffaceacc"));
                    avatarLook.setXenonDefFaceAcc(rs.getInt("xenondeffaceacc"));
                    avatarLook.setBeastTamerDefFaceAcc(rs.getInt("beasttamerdeffaceacc"));
                    avatarLook.setZeroBetaLook(rs.getByte("iszerobetalook") != 0);
                    avatarLook.setMixedHairColor(rs.getInt("mixedhaircolor"));
                    avatarLook.setMixHairPercent(rs.getInt("mixhairpercent"));
                    avatarLook.setEars(rs.getInt("ears"));
                    avatarLook.setTail(rs.getInt("tail"));
                    avatarLook.setHairEquips(new ArrayList<>());
                    avatarLook.setUnseenEquips(new ArrayList<>());
                    avatarLook.setPetIDs(new ArrayList<>());
                    avatarLook.setTotems(new ArrayList<>());
                    do {
                        if (rs.getObject("he_equipid") != null) {
                            avatarLook.getHairEquips().add(rs.getInt("he_equipid"));
                        }
                        if (rs.getObject("ue_equipid") != null) {
                            avatarLook.getUnseenEquips().add(rs.getInt("ue_equipid"));
                        }
                        if (rs.getObject("p_petid") != null) {
                            avatarLook.getPetIDs().add(rs.getInt("p_petid"));
                        }
                        if (rs.getObject("t_totemid") != null) {
                            avatarLook.getTotems().add(rs.getInt("t_totemid"));
                        }
                    } while (rs.next());
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        }
        return avatarLook;
    }

    public void updateAvatarLookToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `avatarlook` (" +
                    "`gender`, " +
                    "`skin`, " +
                    "`face`, " +
                    "`hair`, " +
                    "`weaponstickerid`, " +
                    "`weaponid`, " +
                    "`subweaponid`, " +
                    "`job`, " +
                    "`drawelfear`, " +
                    "`demonslayerdeffaceacc`, " +
                    "`xenondeffaceacc`, " +
                    "`beasttamerdeffaceacc`, " +
                    "`iszerobetalook`, " +
                    "`mixedhaircolor`, " +
                    "`mixhairpercent`, " +
                    "`ears`, " +
                    "`tail` " +
                    ") VALUES (" +
                    String.format("%d, ", getGender()) +
                    String.format("%d, ", getSkin()) +
                    String.format("%d, ", getFace()) +
                    String.format("%d, ", getHair()) +
                    String.format("%d, ", getWeaponStickerId()) +
                    String.format("%d, ", getWeaponId()) +
                    String.format("%d, ", getSubWeaponId()) +
                    String.format("%d, ", getJob()) +
                    String.format("%d, ", isDrawElfEar() ? 1 : 0) +
                    String.format("%d, ", getDemonSlayerDefFaceAcc()) +
                    String.format("%d, ", getXenonDefFaceAcc()) +
                    String.format("%d, ", getBeastTamerDefFaceAcc()) +
                    String.format("%d, ", isZeroBetaLook() ? 1 : 0) +
                    String.format("%d, ", getMixedHairColor()) +
                    String.format("%d, ", getMixHairPercent()) +
                    String.format("%d, ", getEars()) +
                    String.format("%d ", getTail()) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
            if (!getHairEquips().isEmpty()) {
                for (int hairEquip : getHairEquips()) {
                    insertHairEquipToSQL(hairEquip);
                }
            }
            if (!getUnseenEquips().isEmpty()) {
                for (int unseenEquip : getUnseenEquips()) {
                    insertUnseenEquipToSQL(unseenEquip);
                }
            }
            if (!getPetIDs().isEmpty()) {
                for (int petID : getPetIDs()) {
                    insertPetToSQL(petID);
                }
            }
            if (!getTotems().isEmpty()) {
                for (int totem : getTotems()) {
                    insertTotemToSQL(totem);
                }
            }
        } else {
            String query = "UPDATE avatarlook SET " +
                    String.format("gender = %d, ", getGender()) +
                    String.format("skin = %d, ", getSkin()) +
                    String.format("face = %d, ", getFace()) +
                    String.format("hair = %d, ", getHair()) +
                    String.format("weaponstickerid = %d, ", getWeaponStickerId()) +
                    String.format("weaponid = %d, ", getWeaponId()) +
                    String.format("subweaponid = %d, ", getSubWeaponId()) +
                    String.format("job = %d, ", getJob()) +
                    String.format("drawelfear = %d, ", isDrawElfEar() ? 1 : 0) +
                    String.format("demonslayerdeffaceacc = %d, ", getDemonSlayerDefFaceAcc()) +
                    String.format("xenondeffaceacc = %d, ", getXenonDefFaceAcc()) +
                    String.format("beasttamerdeffaceacc = %d, ", getBeastTamerDefFaceAcc()) +
                    String.format("iszerobetalook = %d, ", isZeroBetaLook() ? 1 : 0) +
                    String.format("mixedhaircolor = %d, ", getMixedHairColor()) +
                    String.format("mixhairpercent = %d, ", getMixHairPercent()) +
                    String.format("ears = %d, ", getEars()) +
                    String.format("tail = %d ", getTail()) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public void deleteFromSQL(Connection conn) throws SQLException {
        String hairEquipsQuery = "DELETE FROM `hairequips` WHERE `alid` = ?";
        try (PreparedStatement ps = conn.prepareStatement(hairEquipsQuery)) {
            ps.setInt(1, getId());
            ps.executeUpdate();
        }
        String unseenEquipsQuery = "DELETE FROM `unseenequips` WHERE `alid` = ?";
        try (PreparedStatement ps = conn.prepareStatement(unseenEquipsQuery)) {
            ps.setInt(1, getId());
            ps.executeUpdate();
        }
        String petsQuery = "DELETE FROM `petids` WHERE `alid` = ?";
        try (PreparedStatement ps = conn.prepareStatement(petsQuery)) {
            ps.setInt(1, getId());
            ps.executeUpdate();
        }
        String totemsQuery = "DELETE FROM `totems` WHERE `alid` = ?";
        try (PreparedStatement ps = conn.prepareStatement(totemsQuery)) {
            ps.setInt(1, getId());
            ps.executeUpdate();
        }
        String avatarLookQuery = "DELETE FROM `avatarlook` WHERE `id` = ?";
        try (PreparedStatement ps = conn.prepareStatement(avatarLookQuery)) {
            ps.setInt(1, getId());
            ps.executeUpdate();
        }
    }

    private void insertHairEquipToSQL(int equipID) {
        String query = "INSERT INTO `hairequips` (`alid`, `equipid`) VALUES (" +
                String.format("%d, ", getId()) +
                String.format("%d", equipID)
                + ");";
        DatabaseManager.executeStatement(query);
    }

    private void deleteHairEquipFromSQL(int equipID) {
        String query = "DELETE FROM `hairequips` WHERE " +
                String.format("`alid` = %d", getId()) +
                " AND " +
                String.format("`equipid` = %d", equipID);

        DatabaseManager.executeStatement(query);
    }

    private void insertUnseenEquipToSQL(int equipID) {
        String query = "INSERT INTO `unseenequips` (`alid`, `equipid`) VALUES (" +
                String.format("%d, ", getId()) +
                String.format("%d", equipID)
                + ");";
        DatabaseManager.executeStatement(query);
    }

    private void deleteUnseenEquipFromSQL(int equipID) {
        String query = "DELETE FROM `unseenequips` WHERE " +
                String.format("`alid` = %d", getId()) +
                " AND " +
                String.format("`equipid` = %d", equipID);

        DatabaseManager.executeStatement(query);
    }

    private void insertPetToSQL(int petID) {
        String query = "INSERT INTO `petids` (`alid`, `petid`) VALUES (" +
                String.format("%d, ", getId()) +
                String.format("%d", petID)
                + ");";
        DatabaseManager.executeStatement(query);
    }

    private void deletePetFromSQL(int petID) {
        String query = "DELETE FROM `petids` WHERE " +
                String.format("`alid` = %d", getId()) +
                " AND " +
                String.format("`petid` = %d", petID);

        DatabaseManager.executeStatement(query);
    }

    private void insertTotemToSQL(int totemID) {
        String query = "INSERT INTO `totems` (`alid`, `totemid`) VALUES (" +
                String.format("%d, ", getId()) +
                String.format("%d", totemID)
                + ");";
        DatabaseManager.executeStatement(query);
    }

    private void deleteTotemFromSQL(int totemID) {
        String query = "DELETE FROM `totems` WHERE " +
                String.format("`alid` = %d", getId()) +
                " AND " +
                String.format("`totemid` = %d", totemID);

        DatabaseManager.executeStatement(query);
    }

    public AvatarLook() {
        hairEquips = new ArrayList<>();
        unseenEquips = new ArrayList<>();
        petIDs = Arrays.asList(0, 0, 0);
        totems = new ArrayList<>();
    }

    public AvatarLook deepCopy() {
        AvatarLook res = new AvatarLook();
        res.setGender(getGender());
        res.setSkin(getSkin());
        res.setFace(getFace());
        res.setHair(getHair());
        res.setWeaponStickerId(getWeaponStickerId());
        res.setWeaponId(getWeaponId());
        res.setSubWeaponId(getSubWeaponId());
        List<Integer> resHairEquips = new ArrayList<>(getHairEquips());
        res.setHairEquips(resHairEquips);
        List<Integer> resUnseenEquips = new ArrayList<>(getUnseenEquips());
        res.setUnseenEquips(resUnseenEquips);
        List<Integer> resPetIDs = new ArrayList<>(getPetIDs());
        res.setPetIDs(resPetIDs);
        res.setJob(getJob());
        res.setDrawElfEar(isDrawElfEar());
        res.setDemonSlayerDefFaceAcc(getDemonSlayerDefFaceAcc());
        res.setXenonDefFaceAcc(getXenonDefFaceAcc());
        res.setBeastTamerDefFaceAcc(getBeastTamerDefFaceAcc());
        res.setZeroBetaLook(isZeroBetaLook());
        res.setMixedHairColor(getMixedHairColor());
        res.setMixHairPercent(getMixHairPercent());
        List<Integer> resTotems = new ArrayList<>(getTotems());
        res.setTotems(resTotems);
        res.setEars(getEars());
        res.setTail(getTail());
        return res;
    }

    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public int getSkin() {
        return skin;
    }

    public void setSkin(int skin) {
        this.skin = skin;
    }

    public int getFace() {
        return face;
    }

    public void setFace(int face) {
        this.face = face;
    }

    public int getWeaponStickerId() {
        return weaponStickerId;
    }

    public void setWeaponStickerId(int weaponStickerId) {
        this.weaponStickerId = weaponStickerId;
    }

    public int getWeaponId() {
        return weaponId;
    }

    public void setWeaponId(int weaponId) {
        this.weaponId = weaponId;
    }

    public int getSubWeaponId() {
        return subWeaponId;
    }

    public void setSubWeaponId(int subWeaponId) {
        this.subWeaponId = subWeaponId;
    }

    public List<Integer> getHairEquips() {
        return hairEquips;
    }

    public void setHairEquips(List<Integer> hairEquips) {
        this.hairEquips = hairEquips;
    }

    public void addHairEquip(int equipID) {
        this.hairEquips.add(equipID);
        insertHairEquipToSQL(equipID);
    }

    public void removeHairEquip(int equipID) {
        getHairEquips().removeIf(integer -> integer == equipID);
        deleteHairEquipFromSQL(equipID);
    }

    public List<Integer> getUnseenEquips() {
        return unseenEquips;
    }

    public void setUnseenEquips(List<Integer> unseenEquips) {
        this.unseenEquips = unseenEquips;
    }

    public void addUnseenEquip(int equipID) {
        this.unseenEquips.add(equipID);
        insertUnseenEquipToSQL(equipID);
    }

    public void removeUnseenEquip(int equipID) {
        getUnseenEquips().removeIf(integer -> integer == equipID);
        deleteUnseenEquipFromSQL(equipID);
    }

    public List<Integer> getPetIDs() {
        return petIDs;
    }

    public void setPetIDs(List<Integer> petIDs) {
        this.petIDs = petIDs;
    }

    public void addPetID(int petID) {
        this.petIDs.add(petID);
        insertPetToSQL(petID);
    }

    public void removePetID(int petID) {
        getPetIDs().removeIf(integer -> integer == petID);
        deletePetFromSQL(petID);
    }

    public int getJob() {
        return job;
    }

    public void setJob(int job) {
        this.job = job;
    }

    public boolean isDrawElfEar() {
        return drawElfEar;
    }

    public void setDrawElfEar(boolean drawElfEar) {
        this.drawElfEar = drawElfEar;
    }

    public int getDemonSlayerDefFaceAcc() {
        return demonSlayerDefFaceAcc;
    }

    public void setDemonSlayerDefFaceAcc(int demonSlayerDefFaceAcc) {
        this.demonSlayerDefFaceAcc = demonSlayerDefFaceAcc;
    }

    public int getXenonDefFaceAcc() {
        return xenonDefFaceAcc;
    }

    public void setXenonDefFaceAcc(int xenonDefFaceAcc) {
        this.xenonDefFaceAcc = xenonDefFaceAcc;
    }

    public int getBeastTamerDefFaceAcc() {
        return beastTamerDefFaceAcc;
    }

    public void setBeastTamerDefFaceAcc(int beastTamerDefFaceAcc) {
        this.beastTamerDefFaceAcc = beastTamerDefFaceAcc;
    }

    public boolean isZeroBetaLook() {
        return isZeroBetaLook;
    }

    public void setZeroBetaLook(boolean zeroBetaLook) {
        isZeroBetaLook = zeroBetaLook;
    }

    public int getMixedHairColor() {
        return mixedHairColor;
    }

    public void setMixedHairColor(int mixedHairColor) {
        this.mixedHairColor = mixedHairColor;
    }

    public int getMixHairPercent() {
        return mixHairPercent;
    }

    public void setMixHairPercent(int mixHairPercent) {
        this.mixHairPercent = mixHairPercent;
    }

    public int getHair() {
        return hair;
    }

    public void setHair(int hair) {
        this.hair = hair;
    }

    public List<Integer> getTotems() {
        return totems;
    }

    public void setTotems(List<Integer> totems) {
        this.totems = totems;
    }

    public void addTotemID(int totemID) {
        this.totems.add(totemID);
        insertTotemToSQL(totemID);
    }

    public void removeTotemID(int totemID) {
        getTotems().removeIf(integer -> integer == totemID);
        deleteTotemFromSQL(totemID);
    }

    public int getEars() {
        return ears;
    }

    public void setEars(int ears) {
        this.ears = ears;
    }

    public int getTail() {
        return tail;
    }

    public void setTail(short tail) {
        this.tail = tail;
    }

    public void setTail(int tail) {
        this.tail = tail;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDemonWingID() {
        return demonWingID;
    }

    public void setDemonWingID(int demonWingID) {
        this.demonWingID = demonWingID;
    }

    public int getKaiserWingID() {
        return kaiserWingID;
    }

    public void setKaiserWingID(int kaiserWingID) {
        this.kaiserWingID = kaiserWingID;
    }

    public int getKaiserTailID() {
        return kaiserTailID;
    }

    public void setKaiserTailID(int kaiserTailID) {
        this.kaiserTailID = kaiserTailID;
    }

    public boolean isDressUp() {
        return isDressUp;
    }

    public void setDressUp(boolean isDressUp) {
        this.isDressUp = isDressUp;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void removeItem(int itemID) {
        List<Integer> hairEquips = getHairEquips();
        if (ItemConstants.isCashWeapon(itemID)) {
            setWeaponStickerId(0);
        }
        if (ItemConstants.isWeapon(itemID)) {
            setWeaponId(0);
        }
        if (ItemConstants.isSubWeapon(itemID)) {
            setSubWeaponId(0);
        }
        if (ItemConstants.isTotem(itemID)) {
            removeTotemID(itemID);
        }
        if (hairEquips.contains(itemID)) {
            removeHairEquip(itemID);
        }
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeByte(getGender());
        outPacket.encodeByte(getSkin());
        outPacket.encodeInt(0);
        outPacket.encodeInt(getFace());
        outPacket.encodeInt(getJob());
        outPacket.encodeByte(0); // ignored
        outPacket.encodeInt(getHair());
        encodeEquipmentLists(outPacket);
        encodeLegacyTail(outPacket);
    }

    /** Shared slot/item lists; version-specific look fields follow these three lists. */
    public void encodeEquipmentLists(OutPacket outPacket) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            encodeEquipmentLists282(outPacket);
            return;
        }
        final List<Integer> hairEquips = getHairEquips().stream().filter(i -> i != null && ItemData.getEquipById(i) != null).toList();
        List<Integer> cashItems = hairEquips.stream().filter(i -> ItemData.getEquipById(i).isCash()).toList();
        for (int itemId : hairEquips) {
            boolean hasCashSameBodyPart = false;
            int bodyPart = ItemConstants.getBodyPartFromItem(itemId, getGender());
            if (bodyPart != 0) {
                outPacket.encodeByte(bodyPart);
                if ((isZeroBetaLook() && ItemConstants.isLongSword(itemId)) || (!isZeroBetaLook() && ItemConstants.isBigSword(itemId))) {
                    outPacket.encodeInt(0);
                } else {
                    if (ItemData.getEquipById(itemId) != null && !ItemData.getEquipById(itemId).isCash()) {
                        hasCashSameBodyPart = cashItems.stream().anyMatch(item -> ItemConstants.getBodyPartFromItem(item, getGender()) == bodyPart);
                    }
                    if (hasCashSameBodyPart) {
                        outPacket.encodeInt(0);
                    } else {
                        outPacket.encodeInt(itemId);
                    }
                }
            }
        }
        outPacket.encodeByte(-1);
        for (int itemId : getUnseenEquips()) {
            outPacket.encodeByte(ItemConstants.getBodyPartFromItem(itemId, getGender())); // body part
            outPacket.encodeInt(itemId);
        }
        outPacket.encodeByte(-1);
        for (int itemId : getTotems()) {
            outPacket.encodeByte(ItemConstants.getBodyPartFromItem(itemId, getGender()));
            outPacket.encodeInt(itemId);
        }
        outPacket.encodeByte(-1);
    }

    /**
     * TMS 282 equip lists (reader 0901B97, equips README WI-9). The visible and hidden lists keep a record only when
     * the slot byte is 1..39 and the item fits that slot, so the byte is the 282 client slot
     * ({@link net.swordie.ms.connection.packet.Tms282BodySlots#avatarSlot}: medal 21, shoulder 28, android 32, ...;
     * cash items use their non-cash slot as in 265). Items without such a slot are not sent. The totem list keeps
     * index 0..3 (1..3 checked against 5000 + index), so totems are sent as 0, 1, 2.
     */
    private void encodeEquipmentLists282(OutPacket outPacket) {
        final List<Integer> hairEquips = getHairEquips().stream().filter(i -> i != null && ItemData.getEquipById(i) != null).toList();
        List<Integer> cashItems = hairEquips.stream().filter(i -> ItemData.getEquipById(i).isCash()).toList();
        // A normal item under a cash item of the same slot is not in the visible list at all (it used to be sent as
        // "slot, 0" next to the cash one): the 282 select screen and quick login then drew the slot with the normal
        // item's vslot and lost the hair (2026-10-08, a normal cap under a cash hood). It goes to the hidden list instead.
        List<Integer> covered = new java.util.ArrayList<>();
        for (int itemId : hairEquips) {
            int slot = net.swordie.ms.connection.packet.Tms282BodySlots.avatarSlot(itemId);
            if (slot == 0 || ItemConstants.getBodyPartFromItem(itemId, getGender()) == 0) {
                continue; // no 282 visible slot, or the other gender's item
            }
            boolean hasCashSameSlot = !ItemData.getEquipById(itemId).isCash() && cashItems.stream()
                    .anyMatch(item -> net.swordie.ms.connection.packet.Tms282BodySlots.avatarSlot(item) == slot);
            if (hasCashSameSlot) {
                covered.add(itemId);
                continue;
            }
            outPacket.encodeByte(slot);
            if ((isZeroBetaLook() && ItemConstants.isLongSword(itemId)) || (!isZeroBetaLook() && ItemConstants.isBigSword(itemId))) {
                outPacket.encodeInt(0);
            } else {
                outPacket.encodeInt(itemId);
            }
        }
        outPacket.encodeByte(-1);
        java.util.Set<Integer> hiddenSlots = new java.util.HashSet<>();
        for (int itemId : getUnseenEquips()) {
            int slot = net.swordie.ms.connection.packet.Tms282BodySlots.avatarSlot(itemId);
            if (slot != 0 && hiddenSlots.add(slot)) {
                outPacket.encodeByte(slot);
                outPacket.encodeInt(itemId);
            }
        }
        for (int itemId : covered) {
            int slot = net.swordie.ms.connection.packet.Tms282BodySlots.avatarSlot(itemId);
            if (hiddenSlots.add(slot)) {
                outPacket.encodeByte(slot);
                outPacket.encodeInt(itemId);
            }
        }
        outPacket.encodeByte(-1);
        int index = 0;
        for (int itemId : getTotems()) {
            if (index > 2) {
                break; // 282 totem slots 5000..5002
            }
            outPacket.encodeByte(index++);
            outPacket.encodeInt(itemId);
        }
        outPacket.encodeByte(-1);
    }

    private void encodeLegacyTail(OutPacket outPacket) {
        outPacket.encodeInt(getWeaponStickerId());
        outPacket.encodeInt(getWeaponId());
        outPacket.encodeInt(getSubWeaponId());
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(isDrawElfEar());
        outPacket.encodeInt(0);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        for (int i = 0; i < 3; i++) {
            if (getPetIDs().size() > i) {
                outPacket.encodeInt(getPetIDs().get(i)); // always 3
            } else {
                outPacket.encodeInt(0);
            }
        }
        outPacket.encodeArr(new byte[264]);
        if (JobConstants.isDemon((short) getJob())) {
            outPacket.encodeInt(getDemonSlayerDefFaceAcc());
        } else if (JobConstants.isXenon((short) getJob())) {
            outPacket.encodeInt(getXenonDefFaceAcc());
        } else if (JobConstants.isArk((short) getJob())) {
            outPacket.encodeInt(0); // face acc?
        } else if (JobConstants.isHoYoung((short) getJob())) {
            outPacket.encodeInt(getDemonSlayerDefFaceAcc());
        } else if (JobConstants.isAngelicBuster((short) getJob())) {
            outPacket.encodeByte(isDressUp());
        } else if (JobConstants.isZero((short) getJob())) {
            outPacket.encodeByte(isZeroBetaLook());
        } else if (JobConstants.isBeastTamer((short) getJob())) {
            boolean hasEars = getEars() > 0;
            boolean hasTail = getTail() > 0;
            outPacket.encodeInt(getBeastTamerDefFaceAcc());
            outPacket.encodeByte(hasEars);
            outPacket.encodeInt(getEars());
            outPacket.encodeByte(hasTail);
            outPacket.encodeInt(getTail());
        }
        outPacket.encodeInt(0);
        outPacket.encodeString(name, 13); // NẰM TRONG AVATAR LOOK
    }

    public byte[] getPackedCharacterLook() {
        int[] equipArr = new int[11];
        List<Integer> hairEquips = getHairEquips().stream()
                .filter(i -> i != null && ItemData.getEquipById(i) != null)
                .toList();
        int n = Math.min(equipArr.length, hairEquips.size());
        for (int i = 0; i < n; i++) {
            equipArr[i] = hairEquips.get(i);
        }
        int visibleWeaponId = weaponStickerId != 0 ? weaponStickerId : weaponId;
        int weaponType = 0;
        for (WeaponType type : WeaponType.values()) {
            if (type.getVal() == visibleWeaponId / 1000 % 1000) {
                break;
            }
            if (++weaponType > 36) {
                weaponType = 0;
                break;
            }
        }
        int defFaceAcc = 0;
        if (JobConstants.isDemon((short) getJob())) {
            defFaceAcc = demonSlayerDefFaceAcc;
        }
        if (JobConstants.isXenon((short) getJob())) {
            defFaceAcc = xenonDefFaceAcc;
        }
        if (JobConstants.isHoYoung((short) getJob())) {
            defFaceAcc = demonSlayerDefFaceAcc;
        }
        byte[] data = new byte[120];
        encodeArrIndex(data, 0, gender & 1);
        encodeArrIndex(data, 0, (skin & 0x3FF) << 1);
        encodeArrIndex(data, 1, ((face > 0 ? face % 1000 : -1) & 0x3FF) << 3);
        encodeArrIndex(data, 2, (face / 1000 % 10 & 0xF) << 5);
        encodeArrIndex(data, 3, hair / 10000 == 4 ? 2 : 0);
        encodeArrIndex(data, 3, ((hair > 0 ? hair % 1000 : -1) & 0x3FF) << 2);
        encodeArrIndex(data, 4, (hair / 1000 % 10 & 0xF) << 4);
        encodeArrIndex(data, 5, (equipArr[1] > 0 ? equipArr[1] % 1000 : -1) & 0x3FF);
        encodeArrIndex(data, 6, (equipArr[1] / 1000 % 10 & 7) << 2);
        encodeArrIndex(data, 6, ((defFaceAcc > 0 ? defFaceAcc % 1000 : -1) & 0x3FF) << 5);
        encodeArrIndex(data, 7, (defFaceAcc / 1000 % 10 & 3) << 7);
        encodeArrIndex(data, 8, ((equipArr[3] > 0 ? equipArr[3] % 1000 : -1) & 0x3FF) << 1);
        encodeArrIndex(data, 9, (equipArr[3] / 1000 % 10 & 3) << 3);
        encodeArrIndex(data, 9, ((equipArr[4] > 0 ? equipArr[4] % 1000 : -1) & 0x3FF) << 5);
        encodeArrIndex(data, 10, (equipArr[4] / 1000 % 10 & 3) << 7);
        encodeArrIndex(data, 11, equipArr[5] / 10000 == 105 ? 2 : 0);
        encodeArrIndex(data, 11, ((equipArr[5] > 0 ? equipArr[5] % 1000 : -1) & 0x3FF) << 2);
        encodeArrIndex(data, 12, (equipArr[5] / 1000 % 10 & 0xF) << 4);
        encodeArrIndex(data, 13, (equipArr[6] > 0 ? equipArr[6] % 1000 : -1) & 0x3FF);
        encodeArrIndex(data, 14, (equipArr[6] / 1000 % 10 & 3) << 2);
        encodeArrIndex(data, 14, ((equipArr[7] > 0 ? equipArr[7] % 1000 : -1) & 0x3FF) << 4);
        encodeArrIndex(data, 15, (equipArr[7] / 1000 % 10 & 3) << 6);
        encodeArrIndex(data, 16, (equipArr[8] > 0 ? equipArr[8] % 1000 : -1) & 0x3FF);
        encodeArrIndex(data, 17, (equipArr[8] / 1000 % 10 & 3) << 2);
        encodeArrIndex(data, 17, ((equipArr[9] > 0 ? equipArr[9] % 1000 : -1) & 0x3FF) << 4);
        encodeArrIndex(data, 18, (equipArr[9] / 1000 % 10 & 3) << 6);
        encodeArrIndex(data, 19, equipArr[10] > 0 ? equipArr[10] / 10000 == 109 ? 1 : 3 - (equipArr[10] / 10000 == 134 ? 1 : 0) : 0);
        encodeArrIndex(data, 19, ((equipArr[10] > 0 ? equipArr[10] % 1000 : -1) & 0x3FF) << 2);
        encodeArrIndex(data, 20, (equipArr[10] / 1000 % 10 & 0xF) << 4);
        encodeArrIndex(data, 21, visibleWeaponId / 10000 == 170 ? 1 : 0);
        encodeArrIndex(data, 21, ((visibleWeaponId > 0 ? visibleWeaponId % 1000 : -1) & 0x3FF) << 1);
        encodeArrIndex(data, 22, (visibleWeaponId / 1000 % 10 & 3) << 3);
        encodeArrIndex(data, 22, weaponType << 5);
        encodeArrIndex(data, 119, 29);
        return data;
    }

    private void encodeArrIndex(byte[] arr, int index, int value) {
        arr[index] |= (byte) (value & 0xFF);
        if (value > 0xFF && index < arr.length - 1) {
            arr[index + 1] |= (byte) (value >>> 8 & 0xFF);
        }
    }
}
