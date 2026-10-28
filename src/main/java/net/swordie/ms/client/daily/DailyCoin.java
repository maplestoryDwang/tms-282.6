package net.swordie.ms.client.daily;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.EventConstants;
import net.swordie.ms.constants.QuestConstants;
import net.swordie.ms.util.DataPrinter;

import java.sql.*;

public class DailyCoin {

    private long id;
    private int charID;
    private int point;
    private int coin;
    private long time;
    private long currentTime;
    private boolean isLock = false;

    public static DailyCoin getDailyCoinFromSQLByCharID(int charID) {
        String query = "SELECT * FROM dailycoin WHERE charid = ?";
        DailyCoin dailyCoin = null;
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    int point = rs.getInt("point");
                    int coin = rs.getInt("coin");
                    boolean isLock = rs.getByte("isLock") == 1;

                    dailyCoin = new DailyCoin();
                    dailyCoin.setId(id);
                    dailyCoin.setCharID(charID);
                    dailyCoin.setPoint(point);
                    dailyCoin.setCoin(coin);
                    dailyCoin.setLock(isLock);
                }
            }
        } catch (SQLException sqlException) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, sqlException);
        }
        return dailyCoin;
    }

    public void saveToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `dailycoin` (" +
                    "`charid`, " +
                    "`point`, " +
                    "`coin`, " +
                    "`isLock` " +
                    ") VALUES (" +
                    String.format("%d, ", getCharID()) +
                    String.format("%d, ", getPoint()) +
                    String.format("%d, ", getCoin()) +
                    String.format("%d ", isLock() ? 1 : 0) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        }
        else {
            String query = "UPDATE dailycoin SET " +
                    String.format("charid = %d, ", getCharID()) +
                    String.format("point = %d, ", getPoint()) +
                    String.format("coin = %d, ", getCoin()) +
                    String.format("isLock = %d ", isLock() ? 1 : 0) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public void deleteDailyCoinFromSQL() {
        String query = "DELETE FROM `dailycoin` WHERE " +
                String.format("`id` = %d", getId());
        DatabaseManager.executeStatement(query);
        setId(0);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getCharID() {
        return charID;
    }

    public void setCharID(int charID) {
        this.charID = charID;
    }

    public int getPoint() {
        return point;
    }

    public void setPoint(int point) {
        this.point = point;
    }

    public void increasePoint(Char chr, int point) {
        int increasePoint = getPoint() + point;
        if (increasePoint >= 100) {
            this.point = increasePoint - 100;
            this.coin += 1;
            chr.write(UserLocal.increasePointEventGauge(this, increasePoint - 100, 1));
            return;
        }
        this.point = this.point + point;
        chr.write(UserLocal.increasePointEventGauge(this, point, 0));
    }

    public int getCoin() {
        return coin;
    }

    public void setCoin(int coin) {
        this.coin = coin;
    }

    public void gainCoin(Char chr) {
        if (EventConstants.ARK_EVENT) {
            chr.addItemToInventory(4310248, getCoin());
            this.coin = 0;
            chr.write(WvsContext.scriptProgressItemMessage(" 獲得了額外硬幣", 4310248));
            chr.write(UserLocal.increasePointEventGauge(this, 0, 1));
        }
    }

    public void decCoin(Char chr, int amount) {
        int curCoin = getCoin();
        setCoin(Math.max(curCoin - amount, 0));
        chr.write(UserLocal.increasePointEventGauge(this, getPoint(), -amount));
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(long currentTime) {
        this.currentTime = currentTime;
    }

    public boolean isLock() {
        return isLock;
    }

    public void setLock(boolean lock) {
        isLock = lock;
    }

    @Override
    public String toString() {
        return "DailyQuest{" +
                "id=" + id +
                ", charID=" + charID +
                ", point=" + point +
                ", coin=" + coin +
                '}';
    }
}
