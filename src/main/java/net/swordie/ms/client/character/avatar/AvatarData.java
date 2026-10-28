package net.swordie.ms.client.character.avatar;

import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.ExtendSP;
import net.swordie.ms.client.character.NonCombatStatDayLimit;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.SystemTime;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AvatarData {

    private int id;
    private CharacterStat characterStat;
    private AvatarLook avatarLook;
    private AvatarLook zeroAvatarLook;

    public static AvatarData getAvatarDataFromSQLByID(int avatarDataID) {
        AvatarData avatarData = null;
        String query = "SELECT " +
                "ad.*, cs.*, al.* " +
                "FROM avatardata ad " +
                "LEFT JOIN characterstats cs ON ad.characterstat = cs.id " +
                "LEFT JOIN avatarlook al ON ad.avatarlook = al.id " +
                "WHERE ad.id = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setInt(1, avatarDataID);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    avatarData = new AvatarData();
                    avatarData.setId(rs.getInt("id"));
                    String name = "NULL" + avatarDataID;

                    if (rs.getObject("characterstat") != null) {
                        CharacterStat characterStat = new CharacterStat();
                        characterStat.setId(rs.getInt("characterstat"));
                        characterStat.setCharacterId(rs.getInt("characterid"));
                        characterStat.setCharacterIdForLog(rs.getInt("characteridforlog"));
                        characterStat.setWorldIdForLog(rs.getInt("worldidforlog"));
                        name = rs.getString("name");
                        characterStat.setName(name);
                        characterStat.setGender(rs.getInt("gender"));
                        characterStat.setSkin(rs.getInt("skin"));
                        characterStat.setFace(rs.getInt("face"));
                        characterStat.setHair(rs.getInt("hair"));
                        characterStat.setMixBaseHairColor(rs.getInt("mixbasehaircolor"));
                        characterStat.setMixAddHairColor(rs.getInt("mixaddhaircolor"));
                        characterStat.setMixHairBaseProb(rs.getInt("mixhairbaseprob"));
                        characterStat.setLevel(rs.getInt("level"));
                        characterStat.setJob(rs.getInt("job"));
                        characterStat.setStr(rs.getInt("str"));
                        characterStat.setDex(rs.getInt("dex"));
                        characterStat.setInt(rs.getInt("inte"));
                        characterStat.setLuk(rs.getInt("luk"));
                        characterStat.setHp(rs.getInt("hp"));
                        characterStat.setMaxHp(rs.getInt("maxhp"));
                        characterStat.setMp(rs.getInt("mp"));
                        characterStat.setMaxMp(rs.getInt("maxmp"));
                        characterStat.setAp(rs.getInt("ap"));
                        characterStat.setSp(rs.getInt("sp"));
                        characterStat.setExp(rs.getLong("exp"));
                        characterStat.setPop(rs.getInt("pop"));
                        characterStat.setMoney(rs.getLong("money"));
                        characterStat.setWp(rs.getInt("wp"));
                        characterStat.setPosMap(rs.getLong("posmap"));
                        characterStat.setPortal(rs.getInt("portal"));
                        characterStat.setSubJob(rs.getInt("subjob"));
                        characterStat.setDefFaceAcc(rs.getInt("deffaceacc"));
                        characterStat.setFatigue(rs.getInt("fatigue"));
                        characterStat.setLastFatigueUpdateTime(rs.getInt("lastfatigueupdatetime"));
                        characterStat.setCharismaExp(rs.getInt("charismaexp"));
                        characterStat.setInsightExp(rs.getInt("insightexp"));
                        characterStat.setWillExp(rs.getInt("willexp"));
                        characterStat.setCraftExp(rs.getInt("craftexp"));
                        characterStat.setSenseExp(rs.getInt("senseexp"));
                        characterStat.setCharmExp(rs.getInt("charmexp"));
                        characterStat.setMcpoint(rs.getInt("mcpoint"));
                        characterStat.setPvpExp(rs.getInt("pvpexp"));
                        characterStat.setPvpGrade(rs.getInt("pvpgrade"));
                        characterStat.setPvpPoint(rs.getInt("pvppoint"));
                        characterStat.setPvpModeLevel(rs.getInt("pvpmodelevel"));
                        characterStat.setPvpModeType(rs.getInt("pvpmodetype"));
                        characterStat.setEventPoint(rs.getInt("eventpoint"));
                        characterStat.setAlbaActivityID(rs.getInt("albaactivityid"));
                        characterStat.setAlbaStartTime(DatabaseManager.getFileTimeFromString(rs.getString("albastarttime")));
                        characterStat.setAlbaDuration(rs.getInt("albaduration"));
                        characterStat.setAlbaSpecialReward(rs.getInt("albaspecialreward"));
                        characterStat.setBurning(rs.getByte("burning") != 0);
                        characterStat.setGachExp(rs.getInt("gachexp"));
                        characterStat.setHonorExp(rs.getInt("honorexp"));
                        characterStat.setNextAvailableFameTime(DatabaseManager.getFileTimeFromString(rs.getString("nextavailablefametime")));
                        characterStat.setNodeShards(rs.getInt("node_shards"));
                        characterStat.setMaxFriends(rs.getInt("maxfriends"));
                        characterStat.setCombatPower(rs.getLong("combatpower"));
                        characterStat.setChuc(rs.getInt("chuc"));
                        characterStat.setArc(rs.getInt("arc"));
                        characterStat.setAut(rs.getInt("aut"));
                        int extendSP = rs.getInt("extendsp");
                        if (extendSP != 0) {
                            characterStat.setExtendSP(ExtendSP.getExtendSPFromSQLByID(extendSP));
                        }
                        int nonCombatStatDayLimit = rs.getInt("noncombatstatdaylimit");
                        if (nonCombatStatDayLimit != 0) {
                            characterStat.setNonCombatStatDayLimit(NonCombatStatDayLimit.getNonCombatStatDayLimitFromSQLByID(nonCombatStatDayLimit));
                        }
                        int accountLastLogout = rs.getInt("accountlastlogout");
                        if (accountLastLogout != 0) {
                            characterStat.setAccountLastLogout(SystemTime.getSystemTimeFromSQLByID(accountLastLogout));
                        }
                        characterStat.setLastLogout(DatabaseManager.getFileTimeFromString(rs.getString("lastlogout")));
                        avatarData.setCharacterStat(characterStat);
                    }

                    int avatarLookID = rs.getInt("avatarlook");
                    if (avatarLookID != 0) {
                        avatarData.setAvatarLook(AvatarLook.getAvatarLookFromSQLByAvatarDataID(avatarLookID));
                        avatarData.getAvatarLook().setName(name);
                    }

                    int zeroAvatarLookID = rs.getInt("zeroavatarlook");
                    if (zeroAvatarLookID != 0) {
                        avatarData.setZeroAvatarLook(AvatarLook.getAvatarLookFromSQLByAvatarDataID(zeroAvatarLookID));
                    }
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        }
        return avatarData;
    }

    public void saveToSQL() {
        if (getCharacterStat() != null) {
            getCharacterStat().updateCharacterStatToSQL();
        }
        if (getAvatarLook() != null) {
            getAvatarLook().updateAvatarLookToSQL();
        }
        if (getZeroAvatarLook() != null) {
            getZeroAvatarLook().updateAvatarLookToSQL();
        }
        if (getId() == 0) {
            StringBuilder query = new StringBuilder();
            query.append("INSERT INTO `avatardata` (");
            query.append("`characterstat`, ");
            query.append("`avatarlook`, ");
            query.append("`zeroavatarlook` ");
            query.append(") VALUES (");
            query.append(String.format("%d, ", getCharacterStat().getId()));
            query.append(String.format("%d, ", getAvatarLook().getId()));
            if (getZeroAvatarLook() != null) {
                query.append(String.format("%d ", getZeroAvatarLook().getId()));
            } else {
                query.append("NULL");
            }
            query.append(");");
            int id = (int) DatabaseManager.executeStatementReturnID(query.toString());
            setId(id);
        }
    }

    public void deleteFromSQL(Connection con) throws SQLException {
        if (getCharacterStat() != null) {
            getCharacterStat().deleteFromSQL(con);
        }
        if (getAvatarLook() != null) {
            getAvatarLook().deleteFromSQL(con);
        }
        if (getZeroAvatarLook() != null) {
            getZeroAvatarLook().deleteFromSQL(con);
        }
        String query = "DELETE FROM `avatardata` WHERE `id` = ?";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, getId());
            ps.executeUpdate();
        }
    }

    public AvatarLook getAvatarLook() {
        return avatarLook;
    }

    public void setAvatarLook(AvatarLook avatarLook) {
        this.avatarLook = avatarLook;
    }

    public CharacterStat getCharacterStat() {
        return characterStat;
    }

    public void setCharacterStat(CharacterStat characterStat) {
        this.characterStat = characterStat;
    }

    public AvatarLook getZeroAvatarLook() {
        return zeroAvatarLook;
    }

    public void setZeroAvatarLook(AvatarLook zeroAvatarLook) {
        this.zeroAvatarLook = zeroAvatarLook;
    }

    public void encode(OutPacket outPacket) {
        characterStat.encode(outPacket);
        characterStat.encodeUnk(outPacket);
        avatarLook.encode(outPacket);
        if (JobConstants.isZero(characterStat.getJob())) {
            zeroAvatarLook.setZeroBetaLook(true);
            zeroAvatarLook.encode(outPacket);
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public AvatarLook getAvatarLook(boolean zeroBetaState) {
        return zeroBetaState ? getZeroAvatarLook() : getAvatarLook();
    }
}
