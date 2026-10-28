package net.swordie.ms.connection.api.launcher.gift;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.tms282.EconomyLocks;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.lifecycle.OnlineCharacterSaver;
import net.swordie.ms.util.DataPrinter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * P7: hands launcher_reward_queue rows (gift codes) to a character at its first field entry (MailLoginHook, like
 * parcels), with the game's own grant path: under ECONOMY_MONITOR -> Char (EconomyLocks) each line is checked with
 * {@code canHold}; one that fits is first claimed in the database ({@code UPDATE ... SET delivered_at WHERE
 * delivered_at IS NULL}: a second login, channel or process can never claim it again) and then given with
 * {@code Char.addItemToInventory} (logged to zlog_inventory by the core hook). A line that does not fit stays for
 * the next entry. After a delivery the character is saved at once (save gate, periodic-save path) so a crash cannot
 * lose the items before the next periodic save.
 */
public final class LauncherRewardDelivery {
    private LauncherRewardDelivery() {
    }

    record Line(long id, int itemId, int quantity, int periodDays) {
    }

    public static void onFirstFieldEntry(Char chr) {
        if (chr == null || chr.getId() <= 0) return;
        try {
            int[] r = deliver(chr);
            if (r[0] > 0) {
                chr.chatMessage("登入器禮包：已送達 " + r[0] + " 項道具，請查看背包。");
                Thread.ofVirtual().name("reward-save-" + chr.getId()).start(() -> OnlineCharacterSaver.save(chr));
            }
            if (r[1] > 0) {
                chr.chatMessage("登入器禮包：還有 " + r[1] + " 項道具因背包空間不足未送達，整理背包後重新登入即可領取。");
            }
        } catch (SQLException | RuntimeException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    /** {delivered, left} lines. */
    static int[] deliver(Char chr) throws SQLException {
        List<Line> lines = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection()) {
            if (!GiftCodeService.tablesExist(c)) return new int[]{0, 0};
            // 2026-10-08: with the period of a timed line (side table launcher_reward_period; RewardQueuePeriod), 0 = permanent
            try (PreparedStatement ps = c.prepareStatement(RewardQueuePeriod.selectSql())) {
                ps.setInt(1, chr.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) lines.add(new Line(rs.getLong(1), rs.getInt(2), rs.getInt(3), rs.getInt(4)));
                }
            }
        }
        if (lines.isEmpty()) return new int[]{0, 0};
        int delivered = 0, left = 0;
        for (Line l : lines) {
            boolean[] done = {false};
            boolean[] full = {false};
            SQLException[] failure = {null};
            EconomyLocks.run(chr, () -> {
                if (!net.swordie.ms.world.tms282.items.Tms282ItemAvailability.isGrantable(l.itemId())
                        || !fits(chr, l)) {
                    // a line the grant path would refuse is never claimed (kept for the host to look at)
                    full[0] = true;
                    return;
                }
                try (Connection c = DatabaseManager.getConnection();
                     PreparedStatement ps = c.prepareStatement("UPDATE launcher_reward_queue SET delivered_at = ? WHERE id = ? AND delivered_at IS NULL")) {
                    ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                    ps.setLong(2, l.id());
                    if (ps.executeUpdate() != 1) return; // claimed elsewhere
                } catch (SQLException e) {
                    failure[0] = e;
                    return;
                }
                if (l.periodDays() > 0) {
                    chr.addItemToInventory(l.itemId(), l.quantity(), "day", l.periodDays());
                } else {
                    chr.addItemToInventory(l.itemId(), l.quantity());
                }
                done[0] = true;
                System.out.printf("[GiftCode] delivered queue %d: item %d x%d to character %d%n", l.id(), l.itemId(), l.quantity(), chr.getId());
            });
            if (failure[0] != null) throw failure[0];
            if (done[0]) delivered++;
            if (full[0]) left++;
        }
        return new int[]{delivered, left};
    }

    /**
     * Room for the line. A timed line is checked the way the daily gift checks its own timed rewards ({@code DailyGift}):
     * with a probe that carries the expiry, because an expiring item does not join a permanent stack of the same item.
     */
    private static boolean fits(Char chr, Line l) {
        if (l.periodDays() <= 0 || net.swordie.ms.constants.ItemConstants.isEquip(l.itemId())) {
            return chr.canHold(l.itemId(), l.quantity());
        }
        net.swordie.ms.client.character.items.Item probe = net.swordie.ms.loaders.ItemData.getItemDeepCopy(l.itemId());
        if (probe == null) {
            return false;
        }
        probe.setQuantity(l.quantity());
        probe.setDateExpire(net.swordie.ms.util.FileTime.fromDate(java.time.LocalDateTime.now().plusDays(l.periodDays())));
        return chr.canHold(List.of(probe));
    }
}
