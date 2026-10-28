package net.swordie.ms.world.shop.cashshop;

import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.loaders.Etc.Commodity.CommodityInfo;
import java.sql.*;
import java.time.LocalDate;

/** Purchase history belongs to the login account, not to movable locker items. */
public final class CashShopPurchaseCounts {
    private CashShopPurchaseCounts() { }
    private static volatile boolean ready;
    private static synchronized void ensure() throws SQLException {
        if (ready) return;
        try (Connection c=DatabaseManager.getConnection(); Statement p=c.createStatement()) {
            p.executeUpdate("CREATE TABLE IF NOT EXISTS tms282_cash_purchase_counts (userid INT NOT NULL,sn INT NOT NULL,periodkey INT NOT NULL,quantity INT NOT NULL,lastpurchase TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,PRIMARY KEY(userid,sn,periodkey)) ENGINE=InnoDB");
        }
        ready=true;
    }
    public static int period(CommodityInfo item, LocalDate date) {
        return item.isMonthlyLimited() ? date.getYear()*100+date.getMonthValue() : 0;
    }
    public static int count(int userId, CommodityInfo item) throws SQLException {
        ensure();
        try (Connection c=DatabaseManager.getConnection(); PreparedStatement p=c.prepareStatement(
                "SELECT quantity FROM tms282_cash_purchase_counts WHERE userid=? AND sn=? AND periodkey=?")) {
            p.setInt(1,userId);p.setInt(2,item.getSN());p.setInt(3,period(item,LocalDate.now()));
            try(ResultSet r=p.executeQuery()){return r.next()?r.getInt(1):0;}
        }
    }
    /** Reserve before charging. A failed payment cancels the reservation; unconfirmed failures retain it. */
    public static boolean reserve(int userId, CommodityInfo item, int quantity, int period) throws SQLException {
        if(quantity<=0) return false;
        ensure();
        try(Connection c=DatabaseManager.getConnection()) {
            c.setAutoCommit(false);
            try {
                try(PreparedStatement p=c.prepareStatement("INSERT IGNORE INTO tms282_cash_purchase_counts(userid,sn,periodkey,quantity) VALUES(?,?,?,0)")) {
                    p.setInt(1,userId);p.setInt(2,item.getSN());p.setInt(3,period);p.executeUpdate();
                }
                int maximum=item.getLimitMax()>0?item.getLimitMax():Integer.MAX_VALUE;
                try(PreparedStatement p=c.prepareStatement("UPDATE tms282_cash_purchase_counts SET quantity=quantity+?,lastpurchase=CURRENT_TIMESTAMP WHERE userid=? AND sn=? AND periodkey=? AND quantity<=?")) {
                    p.setInt(1,quantity);p.setInt(2,userId);p.setInt(3,item.getSN());p.setInt(4,period);p.setInt(5,maximum-quantity);
                    if(p.executeUpdate()!=1){c.rollback();return false;}
                }
                c.commit();return true;
            } catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }
    }
    public static void cancel(int userId, CommodityInfo item, int quantity, int period) throws SQLException {
        ensure();
        try(Connection c=DatabaseManager.getConnection();PreparedStatement p=c.prepareStatement(
                "UPDATE tms282_cash_purchase_counts SET quantity=quantity-? WHERE userid=? AND sn=? AND periodkey=? AND quantity>=?")) {
            p.setInt(1,quantity);p.setInt(2,userId);p.setInt(3,item.getSN());p.setInt(4,period);p.setInt(5,quantity);
            if(p.executeUpdate()!=1)throw new SQLException("Cash purchase reservation changed");
        }
    }
}
