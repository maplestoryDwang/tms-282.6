package net.swordie.ms.world.tms282.newbie;

import net.swordie.ms.client.User;
import net.swordie.ms.connection.api.launcher.OnlineAccounts;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import java.sql.*;
import java.util.*;

/** One welcome credit per login user, wallet update and receipt committed together. */
public final class NewbiePoints {
    private static final JdbcNewbieStore SCHEMA = new JdbcNewbieStore();
    private NewbiePoints() {}
    @FunctionalInterface private interface Work { NewbieGame.Points run() throws SQLException; }
    private static NewbieGame.Points locked(List<User> copies,int i,Work work) throws SQLException {
        if(i==copies.size()) return work.run();
        synchronized(copies.get(i)) {return locked(copies,i+1,work);}
    }
    public static NewbieGame.Points grant(User user,int characterId,int amount) throws SQLException {
        return grant(user,characterId,amount,OnlineAccounts.live());
    }
    public static NewbieGame.Points grant(User user,int characterId,int amount,OnlineAccounts online) throws SQLException {
        if(amount==0) return NewbieGame.Points.ALREADY;
        if(user==null || user.getId()<=0 || amount<0 || amount>1_000_000) throw new IllegalArgumentException("invalid welcome points");
        SCHEMA.ensureSchema();
        Set<User> unique=Collections.newSetFromMap(new IdentityHashMap<>());
        unique.addAll(online.copies(user.getId()));unique.add(user);
        List<User> copies=new ArrayList<>(unique);
        copies.sort(Comparator.comparingInt(System::identityHashCode)); // same order as AccountSecretWriter
        return locked(copies,0,()->{
            try(Connection c=DatabaseManager.getConnection()) {
                c.setAutoCommit(false);
                try {
                    int balance;
                    try(PreparedStatement p=c.prepareStatement("SELECT nxPrepaid FROM users WHERE id=? FOR UPDATE")) {
                        p.setInt(1,user.getId());try(ResultSet r=p.executeQuery()) {
                            if(!r.next()) throw new SQLException("user missing");balance=r.getInt(1);
                        }
                    }
                    boolean given;
                    try(PreparedStatement p=c.prepareStatement("INSERT IGNORE INTO newbie_point_log(user_id,character_id,amount,granted_at) VALUES(?,?,?,?)")) {
                        p.setInt(1,user.getId());p.setInt(2,characterId);p.setInt(3,amount);p.setTimestamp(4,new Timestamp(System.currentTimeMillis()));
                        given=p.executeUpdate()==1;
                    }
                    if(given) {
                        long next=(long)balance+amount;
                        if(next>Integer.MAX_VALUE || balance<0) throw new SQLException("wallet limit");
                        balance=(int)next;
                        try(PreparedStatement p=c.prepareStatement("UPDATE users SET nxPrepaid=? WHERE id=?")) {
                            p.setInt(1,balance);p.setInt(2,user.getId());
                            if(p.executeUpdate()!=1) throw new SQLException("wallet update failed");
                        }
                    }
                    c.commit();
                    for(User copy:copies) copy.setNxPrepaid(balance);
                    if(given) System.out.println("[Newbie] welcome points user="+user.getId()+" character="+characterId+" amount="+amount);
                    return given?NewbieGame.Points.GRANTED:NewbieGame.Points.ALREADY;
                } catch(SQLException|RuntimeException e) {c.rollback();throw e;}
            }
        });
    }
}
