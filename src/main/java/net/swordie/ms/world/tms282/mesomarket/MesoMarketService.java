package net.swordie.ms.world.tms282.mesomarket;

import net.swordie.ms.Server;
import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.util.DataPrinter;

import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Meso market (楓點交易所): an order book where Maple Points (users.maplePoints, per login account) are bought and
 * sold for meso (per character).
 * <ul>
 *     <li>Placing an order escrows the payment immediately: meso (price * count) for a buy order, Maple Points for a
 *     sell order. The character is saved right after the meso escrow.</li>
 *     <li>Orders match with price-time priority at the resting order's price. The seller pays {@code taxRate}% of
 *     the meso proceeds. A buyer who bid higher than the trade price gets the difference back.</li>
 *     <li>Settlement is written to the order row (pending_meso / pending_points) so it works while the counterparty is
 *     offline; the owner receives it with {@link #claim(Char)} (market window, or the field-entry hook
 *     {@link #onFieldEntry(Char)}).</li>
 * </ul>
 * Player-facing text is Traditional Chinese. The native 282 meso-market window is NOT wired (no 282 opcode for the
 * request; LP0341 result layout differs from 265), so the front end is the script
 * data/scripts/content/expand/menu/交易中心/楓點交易所.py.
 */
public final class MesoMarketService {
    private MesoMarketService() { }

    /** Serializes all book mutations across channels. */
    public static final ReentrantLock LOCK = new ReentrantLock(true);

    public static MesoMarketConfig config() {
        return MesoMarketConfig.get();
    }

    public static String fmt(long n) {
        return NumberFormat.getIntegerInstance(Locale.US).format(n);
    }

    private static int worldId(Char chr) {
        try {
            return chr.getClient() != null ? chr.getClient().getWorldId() : 0;
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /** Places a buy order (buy=true: pay meso, receive Maple Points) or a sell order. Returns a player message. */
    public static String place(Char chr, boolean buy, long price, long count) {
        MesoMarketConfig cfg = config();
        if (!cfg.enabled()) return "楓點交易所目前暫停服務。";
        if (chr == null || chr.getUser() == null) return "無法取得帳號資料。";
        if (price < cfg.minPrice() || price > cfg.maxPrice())
            return "單價必須介於 " + fmt(cfg.minPrice()) + " 到 " + fmt(cfg.maxPrice()) + " 楓幣之間。";
        if (count < cfg.minCount() || count > cfg.maxCount())
            return "數量必須介於 " + fmt(cfg.minCount()) + " 到 " + fmt(cfg.maxCount()) + " 楓點之間。";
        long total;
        try {
            total = Math.multiplyExact(price, count);
        } catch (ArithmeticException e) {
            return "總金額過大。";
        }
        User user = chr.getUser();
        synchronized (net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) { synchronized (chr) { // multiplayer-0930 lock order
        LOCK.lock();
        try {
            try {
                if (MesoMarketStore.countCreatedToday(user.getId()) >= cfg.dailyOrderLimit())
                    return "今日委託次數已達上限（" + cfg.dailyOrderLimit() + " 次）。";
                if (MesoMarketStore.countOpen(user.getId()) >= cfg.maxOpenOrdersPerAccount())
                    return "進行中的委託已達上限（" + cfg.maxOpenOrdersPerAccount() + " 筆），請先取消或等待成交。";
            } catch (SQLException e) {
                DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
                return "交易所暫時無法使用，請稍後再試。";
            }
            // 1) escrow
            if (buy) {
                if (total > GameConstants.MAX_MONEY || chr.getMoney() < total) return "楓幣不足，需要 " + fmt(total) + " 楓幣。";
                chr.deductMoney(total);
                chr.saveToSQL();
            } else {
                if (user.getMaplePoints() < count) return "楓點不足，需要 " + fmt(count) + " 楓點。";
                try {
                    user.deductMaplePoints((int) count);
                } catch (RuntimeException e) {
                    return "楓點餘額已變動，請重新開啟後再試。";
                }
            }
            // 2) record + match
            MesoMarketStore.PlaceResult result;
            try {
                result = MesoMarketStore.placeAndMatch(cfg, worldId(chr), user.getId(), chr.getId(), buy, price, count);
            } catch (SQLException | RuntimeException e) {
                DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
                refundEscrow(chr, user, buy, total, count);
                return "委託登記失敗，已退回" + (buy ? fmt(total) + " 楓幣。" : fmt(count) + " 楓點。");
            }
            notifyMakers(result.fills());
            StringBuilder sb = new StringBuilder();
            sb.append(buy ? "已登記購買委託 #" : "已登記出售委託 #").append(result.orderId())
                    .append("：").append(fmt(count)).append(" 楓點，單價 ").append(fmt(price)).append(" 楓幣。");
            long filled = result.filled();
            if (filled > 0) {
                sb.append("\r\n立即成交 ").append(fmt(filled)).append(" 楓點");
                if (filled < count) sb.append("，其餘 ").append(fmt(count - filled)).append(" 楓點掛單中");
                sb.append("。");
            }
            String claimed = claimInternal(chr);
            if (claimed != null) sb.append("\r\n").append(claimed);
            return sb.toString();
        } finally {
            LOCK.unlock();
        } } }
    }

    private static void refundEscrow(Char chr, User user, boolean buy, long total, long count) {
        try {
            if (buy) {
                chr.addMoney(total);
                chr.saveToSQL();
            } else {
                user.addMaplePoints((int) count);
            }
        } catch (RuntimeException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            System.out.println("[MesoMarket] REFUND FAILED character=" + chr.getId() + " buy=" + buy
                    + " meso=" + total + " points=" + count + ": " + e);
        }
    }

    public static String cancel(Char chr, long orderId) {
        synchronized (net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) { synchronized (chr) { // multiplayer-0930 lock order
        LOCK.lock();
        try {
            MesoMarketOrder o = MesoMarketStore.cancel(chr.getId(), orderId);
            if (o == null) return "找不到可取消的委託 #" + orderId + "。";
            String msg = "委託 #" + orderId + " 已取消。";
            String claimed = claimInternal(chr);
            return claimed != null ? msg + "\r\n" + claimed : msg;
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
            return "取消失敗，請稍後再試。";
        } finally {
            LOCK.unlock();
        } } }
    }

    /** Delivers every pending meso / Maple Point of this character. Returns a message (never null). */
    public static String claim(Char chr) {
        synchronized (net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) { synchronized (chr) { // multiplayer-0930 lock order
        LOCK.lock();
        try {
            String s = claimInternal(chr);
            return s != null ? s : "目前沒有可領取的楓幣或楓點。";
        } finally {
            LOCK.unlock();
        } } }
    }

    /** Must hold LOCK. Returns null when nothing was pending. */
    private static String claimInternal(Char chr) {
        User user = chr.getUser();
        if (user == null) return null;
        long mesoRoom = Math.max(0, GameConstants.MAX_MONEY - chr.getMoney());
        long pointRoom = Math.max(0, (long) Integer.MAX_VALUE - user.getMaplePoints());
        MesoMarketStore.Claim claim;
        try {
            claim = MesoMarketStore.claim(chr.getId(), mesoRoom, pointRoom);
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
            return "領取失敗，請稍後再試。";
        }
        if (claim.empty() && !claim.mesoDeferred() && !claim.pointsDeferred()) return null;
        StringBuilder sb = new StringBuilder();
        if (claim.points() > 0) {
            try {
                user.addMaplePoints((int) claim.points());
                sb.append("領取 ").append(fmt(claim.points())).append(" 楓點。");
            } catch (RuntimeException e) {
                compensate(claim, 0, claim.points());
                sb.append("楓點入帳失敗，已保留待領取。");
            }
        }
        if (claim.meso() > 0) {
            try {
                chr.addMoney(claim.meso());
                chr.saveToSQL();
                sb.append(sb.isEmpty() ? "" : " ").append("領取 ").append(fmt(claim.meso())).append(" 楓幣。");
            } catch (RuntimeException e) {
                compensate(claim, claim.meso(), 0);
                sb.append("楓幣入帳失敗，已保留待領取。");
            }
        }
        if (claim.mesoDeferred()) sb.append(sb.isEmpty() ? "" : " ").append("楓幣已達持有上限，部分楓幣保留待領取。");
        if (claim.pointsDeferred()) sb.append(sb.isEmpty() ? "" : " ").append("楓點已達上限，部分楓點保留待領取。");
        return sb.toString();
    }

    private static void compensate(MesoMarketStore.Claim claim, long meso, long points) {
        try {
            MesoMarketStore.restorePending(claim.orderIds().getFirst(), meso, points);
        } catch (SQLException | RuntimeException e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
            System.out.println("[MesoMarket] COMPENSATION FAILED orders=" + claim.orderIds() + " meso=" + meso + " points=" + points);
        }
    }

    private static void notifyMakers(List<MesoMarketStore.Fill> fills) {
        for (MesoMarketStore.Fill f : fills) {
            try {
                Char maker = Server.get().getChars().get(f.makerCharacterId());
                if (maker != null && maker.isOnline()) {
                    maker.chatMessage("[楓點交易所] 委託 #" + f.makerId() + (f.makerIsBuy() ? " 買入 " : " 賣出 ")
                            + fmt(f.count()) + " 楓點（單價 " + fmt(f.price()) + "），請至楓點交易所領取。");
                }
            } catch (RuntimeException ignored) {
                // notification only; settlement is already stored
            }
        }
    }

    /** Hook for field entry / login: delivers settlements that happened while offline. */
    public static void onFieldEntry(Char chr) {
        if (chr == null || !config().enabled()) return;
        synchronized (net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) { synchronized (chr) { // multiplayer-0930 lock order
        LOCK.lock();
        try {
            String s = claimInternal(chr);
            if (s != null) chr.chatMessage("[楓點交易所] " + s);
        } catch (RuntimeException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        } finally {
            LOCK.unlock();
        } } }
    }

    /** Order book summary for the script window. */
    public static String describeBook(Char chr) {
        MesoMarketConfig cfg = config();
        int world = worldId(chr);
        try {
            StringBuilder sb = new StringBuilder("#e#b[楓點交易所]#k#n  稅率 ").append(cfg.taxRate()).append("%（由賣方支付）\r\n");
            long last = MesoMarketStore.lastTradePrice(world);
            sb.append("最近成交價：").append(last > 0 ? fmt(last) + " 楓幣／楓點" : "尚無").append("\r\n\r\n");
            sb.append("#r出售中（低→高）#k\r\n");
            List<MesoMarketStore.PriceLevel> asks = MesoMarketStore.book(world, false, cfg.listDepth());
            if (asks.isEmpty()) sb.append("  （無）\r\n");
            for (MesoMarketStore.PriceLevel l : asks)
                sb.append("  ").append(fmt(l.price())).append(" 楓幣 × ").append(fmt(l.count())).append(" 楓點（").append(l.orders()).append(" 筆）\r\n");
            sb.append("#b收購中（高→低）#k\r\n");
            List<MesoMarketStore.PriceLevel> bids = MesoMarketStore.book(world, true, cfg.listDepth());
            if (bids.isEmpty()) sb.append("  （無）\r\n");
            for (MesoMarketStore.PriceLevel l : bids)
                sb.append("  ").append(fmt(l.price())).append(" 楓幣 × ").append(fmt(l.count())).append(" 楓點（").append(l.orders()).append(" 筆）\r\n");
            return sb.toString();
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
            return "交易所暫時無法使用。";
        }
    }

    public static List<MesoMarketOrder> myOrders(Char chr) {
        try {
            return MesoMarketStore.ordersOf(chr.getId());
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
            return List.of();
        }
    }

    public static String describeOrder(MesoMarketOrder o) {
        StringBuilder sb = new StringBuilder();
        sb.append('#').append(o.id()).append(o.buy() ? " 收購 " : " 出售 ").append(fmt(o.count())).append(" 楓點 @ ")
                .append(fmt(o.price())).append("  已成交 ").append(fmt(o.tradeCount()));
        sb.append(switch (o.state()) {
            case MesoMarketOrder.OPEN -> "（掛單中）";
            case MesoMarketOrder.FILLED -> "（已完成）";
            default -> "（已取消）";
        });
        if (o.pendingMeso() > 0 || o.pendingPoints() > 0)
            sb.append(" 待領：").append(fmt(o.pendingMeso())).append(" 楓幣 / ").append(fmt(o.pendingPoints())).append(" 楓點");
        return sb.toString();
    }

    public static long mesos(Char chr) {
        return chr.getMoney();
    }

    public static int maplePoints(Char chr) {
        return chr.getUser() != null ? chr.getUser().getMaplePoints() : 0;
    }
}
