package net.swordie.ms.client.character.union;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.QuestConstants;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.loaders.Etc.Artifact.ArtifactData;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.util.Util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

public class UnionArtifact {

    private long id;
    private int accId;
    private int index;
    private int level;
    private int skillID1;
    private int skillID2;
    private int skillID3;
    private FileTime expirationTime;

    public static void updateCommonMission(Char chr, Mob mob) {
        int minLv = chr.getLevel() - 10;
        int maxLv = chr.getLevel() + 10;
        int level = mob.getLevel();
        if (!chr.isGM()) {
            if (level < minLv || level > maxLv) {
                return;
            }
        }
        if (chr.getQRValueByKey(QuestConstants.UNION_ARTIFACT_DATA, "mobCount") == null) {
            return;
        }
        int currentMob = Integer.parseInt(chr.getQRValueByKey(QuestConstants.UNION_ARTIFACT_DATA,  "mobCount"));
        if (currentMob >= 20000) {
            return;
        }
        currentMob++;
        chr.setQRValueByKey(QuestConstants.UNION_ARTIFACT_DATA, "mobCount", String.valueOf(currentMob));
        var info = ArtifactData.getArtifactInfo();
        boolean showMessage = false;
        boolean increased = false;
        int point = Integer.parseInt(chr.getQRValueByKey(QuestConstants.UNION_ARTIFACT, "point"));
        int incExp = 0;
        for (int missionIdx = 0; missionIdx <= 3; missionIdx++) {
            if (!chr.isArtifactMissionCompleted(chr, ArtifactData.MissionType.Common, missionIdx))  {
                var commonMission = info.getCommonMission(missionIdx);
                if (!showMessage) {
                    chr.chatScriptMessage(commonMission.getName() + String.format(" (%d/%d)", currentMob, commonMission.getValue()));
                    showMessage = true;
                }
                if (currentMob >= commonMission.getValue()) {
                    chr.updateArtifactMission(chr, ArtifactData.MissionType.Common, missionIdx);
                    point += commonMission.getArtifactPoint();
                    incExp += commonMission.getArtifactExp();
                    increased = true;
                    chr.write(WvsContext.unionArtifactQuestMsg(1, ArtifactData.MissionType.Common, missionIdx, incExp, point));
                }
            }
        }
        if (increased) {
            chr.setQRValueByKey(QuestConstants.UNION_ARTIFACT, "point", point);
            chr.incArtifactExp(incExp);
        }
    }

    public static void updateHuntMission(Char chr, Mob mob) {
        if (!mob.isBoss()) {
            return;
        }
        int mobID = mob.getTemplateId();
        if (chr.getQRValueByKey(QuestConstants.UNION_ARTIFACT_BOSS, "mobid") == null) {
            return;
        }
        chr.setQRValueByKey(QuestConstants.UNION_ARTIFACT_DATA, "mobid", String.valueOf(mobID));
        var info = ArtifactData.getArtifactInfo();
        boolean increased = false;
        int point = Integer.parseInt(chr.getQRValueByKey(QuestConstants.UNION_ARTIFACT, "point"));
        int incExp = 0;
        int count = Integer.parseInt(chr.getQRValueByKey(QuestConstants.UNION_ARTIFACT_BOSS, "count"));
        for (var entry : info.getHuntMission().getMissions().int2ObjectEntrySet()) {
            var missionIdx = entry.getIntKey();
            if (!chr.isArtifactMissionCompleted(chr, ArtifactData.MissionType.Hunt, missionIdx))  {
                var commonMission = entry.getValue();
                if (commonMission.getMobID() == mobID) {
                    chr.chatScriptMessage(commonMission.getName() + String.format(" (%d/1)", mobID));
                    chr.updateArtifactMission(chr, ArtifactData.MissionType.Hunt, missionIdx);
                    var rankReward = info.getHuntMission().getRankReward(commonMission.getRank());
                    if (rankReward != null) {
                        point += rankReward.getArtifactPoint();
                        incExp += rankReward.getArtifactExp();
                        increased = true;
                        chr.write(WvsContext.unionArtifactQuestMsg(1, ArtifactData.MissionType.Hunt, missionIdx, incExp, point));
                        count++;
                    }
                    break;
                }
            }
        }
        if (increased) {
            chr.setQRValueByKey(QuestConstants.UNION_ARTIFACT, "point", point);
            chr.incArtifactExp(incExp);
            chr.createQuestWithQRValue(QuestConstants.UNION_ARTIFACT_BOSS, "count="+count+";mobid="+mobID+";lasttime="+FileTime.currentTime().toYYYYMMDD_HHMMss());
        }
    }

    public static void updateSpecialQuestCompletedMission(Char chr, int questID) {
        var info = ArtifactData.getArtifactInfo();
        var mission = info.getSpecialMissionByQuestID(questID, null);
        if (mission != null) {
            boolean increased = false;
            var type = mission.getType();
            int point = Integer.parseInt(chr.getQRValueByKey(QuestConstants.UNION_ARTIFACT, "point"));
            int incExp = 0;
            if (type.equals("setQuestComplete")) {
                if (chr.isArtifactMissionCompleted(chr, ArtifactData.MissionType.Special, mission.getIndex())) {
                    return;
                }
                if (chr.hasQuestCompleted(questID)) {
                    chr.chatScriptMessage(mission.getName());
                    chr.updateArtifactMission(chr, ArtifactData.MissionType.Special, mission.getIndex());
                    point += mission.getArtifactPoint();
                    incExp += mission.getArtifactExp();
                    increased = true;
                    chr.write(WvsContext.unionArtifactQuestMsg(1, ArtifactData.MissionType.Special, mission.getIndex(), incExp, point));
                }
            }
            if (increased) {
                chr.setQRValueByKey(QuestConstants.UNION_ARTIFACT, "point", point);
                chr.incArtifactExp(incExp);
            }
        }
    }

    public static void updateSpecialQuestQRMission(Char chr, int questID, String questQRValue) {
        var info = ArtifactData.getArtifactInfo();
        var mission = info.getSpecialMissionByQuestID(questID, questQRValue);
        if (mission != null) {
            boolean increased = false;
            var type = mission.getType();
            int point = Integer.parseInt(chr.getQRValueByKey(QuestConstants.UNION_ARTIFACT, "point"));
            int incExp = 0;
            if (type.equals("qrexIntValue")) {
                var cond = mission.getConditions().getFirst();
                var key = cond.getKey();
                var value = cond.getRequiredValue();
                if (chr.isArtifactMissionCompleted(chr, ArtifactData.MissionType.Special, mission.getIndex())) {
                    return;
                }
                if (key != null) {
                    var qrValue = chr.getQRValueByKey(questID, key);
                    if (Util.isNumber(qrValue) && Integer.parseInt(qrValue) >= value) {
                        chr.chatScriptMessage(mission.getName());
                        chr.updateArtifactMission(chr, ArtifactData.MissionType.Special, mission.getIndex());
                        point += mission.getArtifactPoint();
                        incExp += mission.getArtifactExp();
                        increased = true;
                        chr.write(WvsContext.unionArtifactQuestMsg(1, ArtifactData.MissionType.Special, mission.getIndex(), incExp, point));
                    }
                }
            }
            if (increased) {
                chr.setQRValueByKey(QuestConstants.UNION_ARTIFACT, "point", point);
                chr.incArtifactExp(incExp);
            }
        }
    }

    public static void updateSpecialItemMission(Char chr, int symbolID, int symbolLevel) {
        var info = ArtifactData.getArtifactInfo();
        var mission = info.getSpecialMissionByItemID(symbolID);
        if (mission != null) {
            boolean increased = false;
            var type = mission.getType();
            int point = Integer.parseInt(chr.getQRValueByKey(QuestConstants.UNION_ARTIFACT, "point"));
            int incExp = 0;
            if (type.equals("symbolLevelUp")) {
                var cond = mission.getConditions().getFirst();
                var itemID = cond.getQuestId();
                var value = cond.getRequiredValue();
                if (chr.isArtifactMissionCompleted(chr, ArtifactData.MissionType.Special, mission.getIndex()))  {
                    return;
                }
                if (itemID == symbolID && symbolLevel >= value) {
                    chr.chatScriptMessage(mission.getName());
                    chr.updateArtifactMission(chr, ArtifactData.MissionType.Special, mission.getIndex());
                    point += mission.getArtifactPoint();
                    incExp += mission.getArtifactExp();
                    increased = true;
                    chr.write(WvsContext.unionArtifactQuestMsg(1, ArtifactData.MissionType.Special, mission.getIndex(), incExp, point));
                }
            }
            if (increased) {
                chr.setQRValueByKey(QuestConstants.UNION_ARTIFACT, "point", point);
                chr.incArtifactExp(incExp);
            }
        }

    }

    public static Map<Integer, UnionArtifact> getUnionArtifactsFromSQLByAccountID(int accountID) {
        Map<Integer, UnionArtifact> unionArtifacts = new HashMap<>();
        String query = "SELECT * FROM unionartifacts WHERE accid = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, accountID);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UnionArtifact artifact = new UnionArtifact();
                    artifact.setId(rs.getInt("id"));
                    artifact.setAccId(rs.getInt("accid"));
                    artifact.setIndex(rs.getInt("idx"));
                    artifact.setLevel(rs.getInt("level"));
                    artifact.setSkillID1(rs.getInt("skill1"));
                    artifact.setSkillID2(rs.getInt("skill2"));
                    artifact.setSkillID3(rs.getInt("skill3"));

                    String exp = rs.getString("expiration");
                    artifact.setExpirationTime(exp == null ? FileTime.MIN_TIME() : DatabaseManager.getFileTimeFromString(exp));

                    unionArtifacts.put(artifact.getIndex(), artifact);
                }
            }
        } catch (SQLException | ParseException e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
        }
        return unionArtifacts;
    }

    public static void saveToSQL(Map<Integer, UnionArtifact> unionArtifacts, int accID) {
        if (unionArtifacts == null || unionArtifacts.isEmpty()) {
            return;
        }
        final String sql =
                "INSERT INTO unionartifacts (`accid`, `idx`, `level`, `skill1`, `skill2`, `skill3`, `expiration`) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "`level`=VALUES(`level`), " +
                        "`skill1`=VALUES(`skill1`), " +
                        "`skill2`=VALUES(`skill2`), " +
                        "`skill3`=VALUES(`skill3`), " +
                        "`expiration`=VALUES(`expiration`)";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (UnionArtifact ua : unionArtifacts.values()) {
                int i = 1;
                ps.setInt(i++, accID);
                ps.setInt(i++, ua.getIndex());
                ps.setInt(i++, ua.getLevel());
                ps.setInt(i++, ua.getSkillID1());
                ps.setInt(i++, ua.getSkillID2());
                ps.setInt(i++, ua.getSkillID3());
                FileTime ft = ua.getExpirationTime();
                if (ft == null) {
                    ps.setNull(i++, java.sql.Types.TIMESTAMP);
                } else {
                    ps.setTimestamp(i++, DatabaseManager.convertToDateTimeSQL(ft));
                }
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
        }
    }

    public UnionArtifact() {}

    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(getLevel());
        outPacket.encodeInt(getSkillID1());
        outPacket.encodeInt(getSkillID2());
        outPacket.encodeInt(getSkillID3());
        outPacket.encodeFT(getExpirationTime());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getAccId() {
        return accId;
    }

    public void setAccId(int accId) {
        this.accId = accId;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void levelUp() {
        if (level >= 5) {
            return;
        }
        this.level++;
    }

    public int getSkillID1() {
        return skillID1;
    }

    public void setSkillID1(int skillID1) {
        this.skillID1 = skillID1;
    }

    public int getSkillID2() {
        return skillID2;
    }

    public void setSkillID2(int skillID2) {
        this.skillID2 = skillID2;
    }

    public int getSkillID3() {
        return skillID3;
    }

    public void setSkillID3(int skillID3) {
        this.skillID3 = skillID3;
    }

    public FileTime getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(FileTime expirationTime) {
        this.expirationTime = expirationTime;
    }
}
