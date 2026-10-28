package net.swordie.ms.client.daily;

import net.swordie.ms.client.Account;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.connection.packet.UIContextPacket;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.constants.DailyGiftConstants;
import net.swordie.ms.util.Common;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class DailyGift {

    private long id;

    private int accountID;

    private long date;

    private long currentDate;

    private int dateComplete = 0;

    //0 = can Receive
    //1 = You can get the following gift after midnight
    private boolean isClaim = false;

    public DailyGift() {
        currentDate = OffsetDateTime.now(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    public static DailyGift getDailyGiftFromSQLByAccountID(int accountID) {
        String query = "SELECT * FROM dailygift WHERE accountID = ?";
        DailyGift dailyGift = null;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, accountID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    dailyGift = new DailyGift();
                    dailyGift.setId(rs.getLong("id"));
                    dailyGift.setAccountID(accountID);
                    dailyGift.setDate(rs.getLong("date"));
                    dailyGift.setDateComplete(rs.getInt("dateComplete"));
                    dailyGift.setClaim(rs.getByte("isClaim") == 1);
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, exception);
        }
        return dailyGift;
    }

    public void saveToSQL() {
        String sql;
        boolean isInsert = (getId() == 0);
        if (isInsert) {
            sql = "INSERT INTO `dailygift` (`accountID`, `date`, `dateComplete`, `isClaim`) VALUES (?, ?, ?, ?)";
        } else {
            sql = "UPDATE `dailygift` SET `accountID` = ?, `date` = ?, `dateComplete` = ?, `isClaim` = ? WHERE `id` = ?";
        }
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, isInsert ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS)) {
            ps.setInt(1, getAccountID());
            ps.setLong(2, getDate());
            ps.setInt(3, getDateComplete());
            ps.setBoolean(4, isClaim());
            if (!isInsert) {
                ps.setLong(5, getId());
            }
            int affectedRows = ps.executeUpdate();
            if (affectedRows != 1
                    && net.swordie.ms.connection.protocol.Protocol.active().version() == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
                throw new java.sql.SQLException("Daily gift record was not saved");
            }
            if (isInsert && affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        setId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
            if (net.swordie.ms.connection.protocol.Protocol.active().version() == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3)
                throw new IllegalStateException("Daily gift save failed; no reward may be granted", e);
        }
    }

    public void deleteDailyGiftFromSQL() {
        String query = "DELETE FROM `dailygift` WHERE " +
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

    public int getAccountID() {
        return accountID;
    }

    public void setAccountID(int accountID) {
        this.accountID = accountID;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public long getCurrentDate() {
        return currentDate;
    }

    public void setCurrentDate(long currentDate) {
        this.currentDate = currentDate;
    }

    public int getDateComplete() {
        return dateComplete;
    }

    public void setDateComplete(int dateComplete) {
        this.dateComplete = dateComplete;
    }

    public void addDateComplete() {
        this.dateComplete += 1;
    }

    public boolean isClaim() {
        return isClaim;
    }

    public void setClaim(boolean claim) {
        isClaim = claim;
    }

    public String getDateString() {
        return Common.getTimeStringWidthFormat("yyyyMMdd", getDate());
    }

    public boolean isNextDate() {
        //2 type Reset
        if (Common.getDateFromCurrentTime(getCurrentDate()) != Common.getDateFromCurrentTime(getDate())) {
            return true;
        }
//        if (getCurrentDate() - getDate() >= 86400000L) {
//            return true;
//        }
        return false;
    }

    public boolean isFirstDayOfMonth() {
        if (Common.getDateFromCurrentTime(getCurrentDate()) == 1) {
            return true;
        }
        return false;
    }

    public enum ClaimResult {
        CLAIMED, NO_ACCOUNT, ALREADY_CLAIMED, MONTH_COMPLETE, INVENTORY_FULL
    }

    /** Same local calendar day (the previous check compared only the day-of-month). */
    static boolean sameDay(long a, long b) {
        return Common.getTimeStringWidthFormat("yyyyMMdd", a).equals(Common.getTimeStringWidthFormat("yyyyMMdd", b));
    }

    static boolean sameMonth(long a, long b) {
        return Common.getTimeStringWidthFormat("yyyyMM", a).equals(Common.getTimeStringWidthFormat("yyyyMM", b));
    }

    /** Applies the monthly and daily resets for {@code now}. Returns {monthReset, dayReset}; the caller saves. */
    public boolean[] rollover(long now) {
        setCurrentDate(now);
        boolean monthReset = !sameMonth(getDate(), now) && getDateComplete() != 0;
        if (monthReset) {
            setDateComplete(0);
        }
        boolean dayReset = !sameDay(getDate(), now);
        if (dayReset) {
            setClaim(false);
            setDate(now);
        }
        return new boolean[]{monthReset, dayReset};
    }

    /** The account's record, created (and saved) on first use, with today's rollover applied. */
    public static DailyGift ofAccount(Account account, long now) {
        synchronized (account) {
            return ofAccountLocked(account, now);
        }
    }

    private static DailyGift ofAccountLocked(Account account, long now) {
        DailyGift dailyGift = account.getDailyGift();
        if (dailyGift == null) {
            dailyGift = new DailyGift();
            dailyGift.setDate(now);
            dailyGift.setAccountID(account.getId());
            dailyGift.saveToSQL();
            account.setDailyGift(dailyGift);
        }
        boolean[] reset = dailyGift.rollover(now);
        if (reset[0] || reset[1]) {
            dailyGift.saveToSQL();
        }
        return dailyGift;
    }

    /** Next unclaimed reward, or null when all 28 days of this month are done. */
    public DailyGiftItemInfo nextReward() {
        var rewards = DailyGiftConstants.rewards();
        return getDateComplete() >= 0 && getDateComplete() < rewards.size() ? rewards.get(getDateComplete()) : null;
    }

    /** Shared claim for the native window and the control panel. {@code nativeUi} also sends the 265 UI packets. */
    public static ClaimResult claim(Char chr, boolean nativeUi) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version() == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            ClaimResult[] result = {ClaimResult.NO_ACCOUNT};
            net.swordie.ms.client.character.tms282.EconomyLocks.packet(chr, () -> {
                net.swordie.ms.client.Client client = chr.getClient();
                if (!chr.isOnline() || client == null || client.getChr() != chr
                        || net.swordie.ms.Server.get().getChars().get(chr.getId()) != chr
                        || client.getCh() == null || !client.getCh().isActive()
                        || client.getCurrentState() != net.swordie.ms.client.Client.IN_FIELD
                        || client.getNextState() != net.swordie.ms.client.Client.NONE)
                    throw new IllegalStateException("Daily gift requires the current online field character");
                net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.requireEconomicAccess(chr);
                result[0] = claimLocked(chr, nativeUi);
                return null;
            });
            return result[0];
        }
        return claimLocked(chr, nativeUi);
    }

    private static ClaimResult claimLocked(Char chr, boolean nativeUi) {
        Account account = chr.getAccount();
        if (account == null) {
            return ClaimResult.NO_ACCOUNT;
        }
        synchronized (account) {
            DailyGift dailyGift = ofAccount(account, System.currentTimeMillis());
            if (dailyGift.isClaim()) {
                return ClaimResult.ALREADY_CLAIMED;
            }
            DailyGiftItemInfo reward = dailyGift.nextReward();
            if (reward == null) {
                return ClaimResult.MONTH_COMPLETE;
            }
            int itemID = reward.getItemId(), quantity = reward.getQuantity();
            if (net.swordie.ms.connection.protocol.Protocol.active().version() == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                    && !net.swordie.ms.world.tms282.items.Tms282ItemAvailability.isGrantable(itemID)) {
                throw new IllegalStateException("Daily gift item is unavailable: " + itemID);
            }
            int period = DailyGiftConstants.periodDays(reward);
            boolean canHold;
            if (net.swordie.ms.connection.protocol.Protocol.active().version() == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
                net.swordie.ms.client.character.items.Item probe = net.swordie.ms.loaders.ItemData.getItemDeepCopy(itemID);
                if (probe == null) throw new IllegalStateException("Daily gift item template missing: " + itemID);
                probe.setQuantity(quantity);
                if (period > 0) probe.setDateExpire(FileTime.fromDate(java.time.LocalDateTime.now().plusDays(period)));
                canHold = chr.canHold(java.util.List.of(probe));
            } else {
                canHold = chr.canHold(itemID, quantity);
            }
            if (!canHold) {
                return ClaimResult.INVENTORY_FULL;
            }
            // Mark claimed and persist before granting: a failed save must not allow a second grant.
            dailyGift.addDateComplete();
            dailyGift.setClaim(true);
            try {
                dailyGift.saveToSQL();
            } catch (RuntimeException e) {
                dailyGift.setDateComplete(dailyGift.getDateComplete() - 1);
                dailyGift.setClaim(false);
                throw e;
            }
            if (period > 0) {
                chr.addItemToInventory(itemID, quantity, "day", period);
            } else {
                chr.addItemToInventory(itemID, quantity);
            }
            if (nativeUi) {
                dailyGift.update(chr, itemID);
            }
            try {
                // dailygift_log (launcher stamp card): advisory, after the grant; never affects the claim
                DailyGiftClaimLog.claimed(account.getId(), dailyGift.getDate(), dailyGift.getDateComplete(), itemID, quantity,
                        nativeUi ? "game" : "panel");
            } catch (Throwable ignored) {
                // also covers a missing class in a partial deployment
            }
            // 2026-10-08 streak bonus (3 / 7 / 14 consecutive days): queued, then handed over at once, off this thread
            // (the claim holds the account and economy locks)
            int userIdForBonus = account.getUser() == null ? 0 : account.getUser().getId();
            int accountIdForBonus = account.getId();
            Thread.ofVirtual().name("streak-bonus-" + chr.getId()).start(() -> {
                try {
                    var granted = DailyStreakBonus.afterClaim(userIdForBonus, accountIdForBonus, chr.getId());
                    if (!granted.isEmpty()) {
                        chr.chatMessage("連續簽到加碼：連續 " + granted.stream().map(g -> String.valueOf(g.days())).collect(java.util.stream.Collectors.joining("、"))
                                + " 天的獎勵已送出。");
                        net.swordie.ms.connection.api.launcher.gift.LauncherRewardDelivery.onFirstFieldEntry(chr);
                    }
                } catch (Throwable e) {
                    System.out.println("[StreakBonus] after game claim of character " + chr.getId() + ": " + e);
                }
            });
            return ClaimResult.CLAIMED;
        }
    }

    public static void init(Char chr) {
        Account account = chr.getAccount();
        if (account == null) {
            return;
        }
        boolean tms = net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
        boolean existed = account.getDailyGift() != null;
        long previousDate = existed ? account.getDailyGift().getDate() : 0;
        int previousComplete = existed ? account.getDailyGift().getDateComplete() : 0;
        DailyGift dailyGift = ofAccount(account, System.currentTimeMillis());
        if (tms) {
            Tms282MonsterRecord.ensureToday(chr, dailyGift.getDateString());
        } else if (!chr.hasQuest(DailyGiftConstants.MONSTER_COUNT_QR)) {
            chr.createQuestWithQRValue(DailyGiftConstants.MONSTER_COUNT_QR, String.format("date=%s;count=0;", dailyGift.getDateString()));
        }
        if (existed && !sameMonth(previousDate, dailyGift.getDate()) && previousComplete != 0 && !tms) {
            chr.sendPopupSay("每日禮物已於月初重置。");
        }
        if (existed && !sameDay(previousDate, dailyGift.getDate())) {
            if (!tms) {
                chr.sendPopupSay("你的每日禮物已重置。");
            }
            if (!tms) {
                chr.createQuestWithQRValue(DailyGiftConstants.MONSTER_COUNT_QR, String.format("date=%s;count=0;", dailyGift.getDateString()));
            }
        }
        String syntax = String.format("date=%s;day=%d;count=%d;", dailyGift.getDateString(), dailyGift.getDateComplete(), dailyGift.isClaim() ? 1 : 0);
        chr.write(UserLocal.dailyGiftMessage(syntax));
        chr.write(UIContextPacket.dailyGiftInit(dailyGift, 0, 0));
    }

    public int getCurrentMonster(Char chr) {
        if (chr.getQRValue(DailyGiftConstants.MONSTER_COUNT_QR).equals("Quest is Null")) {
            return 0;
        }
        return Integer.parseInt(chr.getQRValueByKey(DailyGiftConstants.MONSTER_COUNT_QR,  "count"));
    }

    public void updateMonsterCount(Char chr, int level) {
        int minLv = chr.getLevel() - 10;
        int maxLv = chr.getLevel() + 10;
        if (level < minLv || level > maxLv) {
            return;
        }
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            Tms282MonsterRecord.kill(chr, Common.getTimeStringWidthFormat("yyyyMMdd", System.currentTimeMillis()));
            return;
        }
        if (chr.getQRValue(DailyGiftConstants.MONSTER_COUNT_QR).equals("Quest is Null")) {
            return;
        }
        int currentMob = Integer.parseInt(chr.getQRValueByKey(DailyGiftConstants.MONSTER_COUNT_QR,  "count"));
        if (currentMob < 300) {
            currentMob++;
        }
        chr.setQRValueByKey(DailyGiftConstants.MONSTER_COUNT_QR, "count", String.valueOf(currentMob));
    }

    public void update(Char chr, int itemID) {
        String value = String.format("date=%s;day=%d;count=%d;", getDateString(), getDateComplete(), isClaim() ? 1 : 0);
        chr.write(UserLocal.dailyGiftMessage(value));
        chr.write(UIContextPacket.dailyGiftInit(this, 2, itemID));
        chr.write(UIContextPacket.dailyGiftInit(this, 0, itemID));
        saveToSQL();
    }

    public void encode(OutPacket outPacket) {
        var rewards = DailyGiftConstants.rewards(); // one immutable version for this whole packet
        boolean tms = net.swordie.ms.connection.protocol.Protocol.active().version() == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
        outPacket.encodeFT(FileTime.currentTime());
        outPacket.encodeFT(FileTime.MAX_TIME());
        outPacket.encodeInt(28);
        outPacket.encodeInt(2);
        outPacket.encodeInt(DailyGiftConstants.MONSTER_COUNT_QR); // Custom Quest Key?
        outPacket.encodeInt(DailyGiftConstants.REQ_MONSTER); // Amount Req
        outPacket.encodeString("mvpTooltip_default");
        outPacket.encodeInt(rewards.size()); // 28
        for (DailyGiftItemInfo dailyGiftItemInfo : rewards) {
            outPacket.encodeInt(dailyGiftItemInfo.getId());
            outPacket.encodeInt(dailyGiftItemInfo.getItemId());
            outPacket.encodeInt(dailyGiftItemInfo.getQuantity());
            outPacket.encodeByte(1);
            outPacket.encodeInt(tms ? dailyGiftItemInfo.getPeriodDays() * 1440 : (dailyGiftItemInfo.getSN() > 0) ? 0 : 10080);
            outPacket.encodeByte((dailyGiftItemInfo.getSN() > 0) ? 1 : 0);
            outPacket.encodeInt((dailyGiftItemInfo.getSN() > 0) ? 1 : 0);
            outPacket.encodeInt(0);
            outPacket.encodeByte(false);
        }
        outPacket.encodeInt(DailyGiftConstants.reqLevel()); // 282: entry +0x2C, the main menu level gate
        int size = 0;
        outPacket.encodeInt(size);
        for (int i = 0; i < size; i++) {
            outPacket.encodeInt(i);
            outPacket.encodeInt(40914);
        }
        outPacket.encodeInt(size);
        for (int i = 0; i < size; i++) {
            outPacket.encodeInt(0);
            // sub_C5C360
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(size);
            for (i = 0; i < size; i++) {
                // sub_C5C360
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
            }
        }
    }

    /** Refresh online native windows after the host saves a reward table; no client resource changes. */
    public static int refreshOnline() {
        if (net.swordie.ms.connection.protocol.Protocol.active().version() != net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) return 0;
        int[] count = {0};
        for (Char chr : new java.util.ArrayList<>(net.swordie.ms.Server.get().getChars().values())) {
            try {
                net.swordie.ms.client.character.tms282.EconomyLocks.packet(chr, () -> {
                    if (chr.getClient() != null && chr.getClient().getChr() == chr
                            && chr.getClient().getCurrentState() == net.swordie.ms.client.Client.IN_FIELD
                            && chr.getClient().getNextState() == net.swordie.ms.client.Client.NONE
                            && chr.getAccount() != null && chr.getAccount().getDailyGift() != null) {
                        chr.write(UIContextPacket.dailyGiftInit(chr.getAccount().getDailyGift(), 0, 0));
                        count[0]++;
                    }
                    return null;
                });
            } catch (RuntimeException e) { System.out.println("[DailyRewards] native refresh failed for " + chr.getId() + ": " + e.getClass().getSimpleName()); }
        }
        return count[0];
    }

    @Override
    public String toString() {
        return "DailyGift{" +
                "accountID=" + accountID +
                ", date=" + date +
                ", currentDate=" + currentDate +
                ", dateComplete=" + dateComplete +
                ", isClaim=" + isClaim +
                '}';
    }
}
