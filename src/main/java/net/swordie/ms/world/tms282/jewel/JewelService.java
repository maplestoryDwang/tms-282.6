package net.swordie.ms.world.tms282.jewel;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.util.DataPrinter;

import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntUnaryOperator;

/**
 * Jewel (寶玉) engrave / awaken / polish service, rules from data/resources/tms282/jewel/jewel.json (TMS279 jewel.toml).
 *
 * <p>282.3 wire status: the client request/result for the jewel window is UNKNOWN (InHeader USER_JEWEL_CRAFT_REQUEST
 * and USER_REQUEST_JEWEL_CRAFT are UnkValue; opcode-status-282.tsv has no jewel row; Character/Jewel wz is an empty
 * placeholder). Jewels are therefore server-side records (tms282_jewel) driven from scripts:
 * data/scripts/content/expand/menu/裝備系統/寶玉工坊.py and item scripts consume_2832549 / consume_2832550.</p>
 *
 * <p>Order of effects: validate (under the character lock) -> optimistic DB update + audit row -> take materials/meso
 * -> chr.saveToSQL(). A concurrent change makes the DB step fail and nothing is taken.</p>
 */
public final class JewelService {
    private JewelService() { }

    public record Result(boolean ok, String message, JewelState state) {
        static Result fail(String message) {
            return new Result(false, message, null);
        }
    }

    static IntUnaryOperator random = bound -> ThreadLocalRandom.current().nextInt(bound);

    public static JewelConfig config() {
        return JewelConfig.get();
    }

    private static String fmt(long n) {
        return NumberFormat.getIntegerInstance(Locale.US).format(n);
    }

    private static int size() {
        return config().maxAwaken();
    }

    public static List<JewelState> list(Char chr) {
        try {
            return JewelStore.list(chr.getId(), size());
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
            return List.of();
        }
    }

    /** Creates a new tier-{@code tier} jewel for the character (GM / reward script / future item hook). */
    public static Result grant(Char chr, int tier, int sourceItemId) {
        JewelConfig cfg = config();
        if (!cfg.tiers().containsKey(tier)) return Result.fail("沒有第 " + tier + " 階的寶玉。");
        try {
            JewelState s = JewelStore.insert(chr.getId(), tier, sourceItemId, cfg.maxAwaken());
            return new Result(true, "獲得第 " + tier + " 階寶玉（編號 " + s.id() + "）。", s);
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
            return Result.fail("寶玉建立失敗。");
        }
    }

    /** Converts an inventory item listed in jewel.json "jewelItems" into a jewel record (consumes 1). */
    public static Result grantFromItem(Char chr, int itemId) {
        Integer tier = config().jewelItems().get(itemId);
        if (tier == null) return Result.fail("此道具不是寶玉。");
        synchronized (net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) { synchronized (chr) { // multiplayer-0930 lock order
            if (!chr.hasItemCount(itemId, 1)) return Result.fail("沒有該道具。");
            Result r = grant(chr, tier, itemId);
            if (r.ok()) {
                chr.consumeItem(itemId, 1);
                chr.saveToSQL();
            }
            return r;
        } }
    }

    private static JewelState load(Char chr, long jewelId) throws SQLException {
        JewelState s = JewelStore.get(chr.getId(), jewelId, size());
        return s != null && s.active() ? s : null;
    }

    private static String need(JewelConfig.Ingredient ing) {
        return "#i" + ing.id() + "# #t" + ing.id() + "# x" + ing.count();
    }

    public static Result engrave(Char chr, long jewelId) {
        JewelConfig cfg = config();
        if (!cfg.enabled()) return Result.fail("寶玉系統目前暫停服務。");
        synchronized (net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) { synchronized (chr) { // multiplayer-0930 lock order
            try {
                JewelState s = load(chr, jewelId);
                if (s == null) return Result.fail("找不到該寶玉。");
                JewelConfig.EngraveStep step = JewelRules.nextEngrave(cfg, s);
                if (step == null) return Result.fail("刻印已達最高等級（" + cfg.maxEngrave() + "）。");
                JewelConfig.Ingredient ing = step.ingredient();
                if (!chr.hasItemCount(ing.id(), ing.count())) return Result.fail("材料不足，需要 " + need(ing) + "。");
                JewelRules.Engrave r = JewelRules.engrave(cfg, s, random);
                if (!JewelStore.update(s, r.after(), "ENGRAVE", r.outcome().name(), ing.id(), ing.count(), 0))
                    return Result.fail("寶玉狀態已變更，請重新操作。");
                chr.consumeItem(ing.id(), ing.count());
                chr.saveToSQL();
                String msg = switch (r.outcome()) {
                    case UPGRADE -> "刻印成功！刻印等級 " + s.engraveLevel() + " → " + r.after().engraveLevel() + "。";
                    case DOWNGRADE -> "刻印失敗，刻印等級 " + s.engraveLevel() + " → " + r.after().engraveLevel() + "。";
                    case DESTROY -> "刻印失敗，寶玉損毀了。";
                };
                return new Result(r.outcome() == JewelRules.EngraveOutcome.UPGRADE, msg, r.after());
            } catch (SQLException e) {
                DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
                return Result.fail("操作失敗，請稍後再試。");
            }
        } }
    }

    public static Result awaken(Char chr, long jewelId) {
        JewelConfig cfg = config();
        if (!cfg.enabled()) return Result.fail("寶玉系統目前暫停服務。");
        synchronized (net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) { synchronized (chr) { // multiplayer-0930 lock order
            try {
                JewelState s = load(chr, jewelId);
                if (s == null) return Result.fail("找不到該寶玉。");
                JewelConfig.AwakenStep step = JewelRules.nextAwaken(cfg, s);
                if (step == null) return Result.fail("覺醒已達最高等級（" + cfg.maxAwaken() + "）。");
                JewelConfig.Ingredient ing = step.ingredient();
                if (!chr.hasItemCount(ing.id(), ing.count())) return Result.fail("材料不足，需要 " + need(ing) + "。");
                JewelState after = JewelRules.awaken(cfg, s);
                if (!JewelStore.update(s, after, "AWAKEN", "UPGRADE", ing.id(), ing.count(), 0))
                    return Result.fail("寶玉狀態已變更，請重新操作。");
                chr.consumeItem(ing.id(), ing.count());
                chr.saveToSQL();
                return new Result(true, "覺醒成功！覺醒等級 " + s.awakenLevel() + " → " + after.awakenLevel()
                        + "，第 " + after.awakenLevel() + " 格可以研磨了。", after);
            } catch (SQLException e) {
                DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
                return Result.fail("操作失敗，請稍後再試。");
            }
        } }
    }

    public static Result polish(Char chr, long jewelId, int slot) {
        JewelConfig cfg = config();
        if (!cfg.enabled()) return Result.fail("寶玉系統目前暫停服務。");
        synchronized (net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) { synchronized (chr) { // multiplayer-0930 lock order
            try {
                JewelState s = load(chr, jewelId);
                if (s == null) return Result.fail("找不到該寶玉。");
                if (slot < 1 || slot > s.awakenLevel()) return Result.fail("第 " + slot + " 格尚未覺醒。");
                if (s.polishAt(slot) > 0) return Result.fail("第 " + slot + " 格已研磨（" + s.polishAt(slot) + "%），請先重設。");
                JewelConfig.AwakenStep step = cfg.awaken().get(slot);
                JewelConfig.Ingredient ing = step.polishIngredient();
                if (!chr.hasItemCount(ing.id(), ing.count())) return Result.fail("材料不足，需要 " + need(ing) + "。");
                int grade = JewelRules.rollPolish(cfg, slot, random);
                JewelState after = s.withPolish(slot, grade);
                if (!JewelStore.update(s, after, "POLISH", "GRADE_" + grade, ing.id(), ing.count(), 0))
                    return Result.fail("寶玉狀態已變更，請重新操作。");
                chr.consumeItem(ing.id(), ing.count());
                chr.saveToSQL();
                return new Result(true, "研磨完成！第 " + slot + " 格研磨等級 " + grade + "%。", after);
            } catch (SQLException e) {
                DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
                return Result.fail("操作失敗，請稍後再試。");
            }
        } }
    }

    /** Resets the polish of one slot, paying the tier's polishResetCost meso, or a reset ticket when useTicket. */
    public static Result resetPolish(Char chr, long jewelId, int slot, boolean useTicket) {
        JewelConfig cfg = config();
        if (!cfg.enabled()) return Result.fail("寶玉系統目前暫停服務。");
        synchronized (net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) { synchronized (chr) { // multiplayer-0930 lock order
            try {
                JewelState s = load(chr, jewelId);
                if (s == null) return Result.fail("找不到該寶玉。");
                if (s.polishAt(slot) <= 0) return Result.fail("第 " + slot + " 格尚未研磨。");
                JewelState after = s.withPolish(slot, 0);
                if (useTicket) {
                    JewelConfig.Ticket t = cfg.polishResetTicket();
                    if (t == null) return Result.fail("沒有設定研磨重設券。");
                    if (s.tier() > t.maxTier()) return Result.fail("研磨重設券只能用於第 " + t.maxTier() + " 階以下的寶玉。");
                    if (!chr.hasItemCount(t.itemId(), 1)) return Result.fail("沒有 #i" + t.itemId() + "# #t" + t.itemId() + "#。");
                    if (!JewelStore.update(s, after, "POLISH_RESET", "TICKET", t.itemId(), 1, 0))
                        return Result.fail("寶玉狀態已變更，請重新操作。");
                    chr.consumeItem(t.itemId(), 1);
                } else {
                    long cost = cfg.tier(s.tier()).polishResetCost();
                    if (chr.getMoney() < cost) return Result.fail("楓幣不足，重設需要 " + fmt(cost) + " 楓幣。");
                    if (!JewelStore.update(s, after, "POLISH_RESET", "MESO", 0, 0, cost))
                        return Result.fail("寶玉狀態已變更，請重新操作。");
                    chr.deductMoney(cost);
                }
                chr.saveToSQL();
                return new Result(true, "第 " + slot + " 格研磨已重設。", after);
            } catch (SQLException e) {
                DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
                return Result.fail("操作失敗，請稍後再試。");
            }
        } }
    }

    /** 寶玉最高等級研磨券: sets one awakened slot to 100%. */
    public static Result maxPolish(Char chr, long jewelId, int slot) {
        JewelConfig cfg = config();
        if (!cfg.enabled()) return Result.fail("寶玉系統目前暫停服務。");
        JewelConfig.Ticket t = cfg.maxPolishTicket();
        if (t == null) return Result.fail("沒有設定最高等級研磨券。");
        synchronized (net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) { synchronized (chr) { // multiplayer-0930 lock order
            try {
                JewelState s = load(chr, jewelId);
                if (s == null) return Result.fail("找不到該寶玉。");
                if (s.tier() > t.maxTier()) return Result.fail("此研磨券只能用於第 " + t.maxTier() + " 階以下的寶玉。");
                if (slot < 1 || slot > s.awakenLevel()) return Result.fail("第 " + slot + " 格尚未覺醒。");
                if (s.polishAt(slot) >= 100) return Result.fail("第 " + slot + " 格已是最高研磨等級。");
                if (!chr.hasItemCount(t.itemId(), 1)) return Result.fail("沒有 #i" + t.itemId() + "# #t" + t.itemId() + "#。");
                JewelState after = s.withPolish(slot, 100);
                if (!JewelStore.update(s, after, "POLISH_MAX", "TICKET", t.itemId(), 1, 0))
                    return Result.fail("寶玉狀態已變更，請重新操作。");
                chr.consumeItem(t.itemId(), 1);
                chr.saveToSQL();
                return new Result(true, "第 " + slot + " 格研磨等級提升為 100%。", after);
            } catch (SQLException e) {
                DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
                return Result.fail("操作失敗，請稍後再試。");
            }
        } }
    }

    /** Multi-line description for the script window. */
    public static String describe(JewelState s) {
        JewelConfig cfg = config();
        JewelRules.Stats st = JewelRules.stats(cfg, s);
        StringBuilder sb = new StringBuilder();
        sb.append("#e寶玉 #").append(s.id()).append("#n  第 ").append(s.tier()).append(" 階（最大屬性 ")
                .append(cfg.tier(s.tier()).maxStat()).append("）\r\n");
        sb.append("刻印等級：").append(s.engraveLevel()).append(" / ").append(cfg.maxEngrave())
                .append("    覺醒等級：").append(s.awakenLevel()).append(" / ").append(cfg.maxAwaken()).append("\r\n");
        sb.append("研磨：");
        if (s.awakenLevel() == 0) sb.append("（尚未覺醒）");
        for (int i = 1; i <= s.awakenLevel(); i++) {
            int g = s.polishAt(i);
            sb.append(i).append(":").append(g == 0 ? "-" : g + "%").append(i < s.awakenLevel() ? "  " : "");
        }
        sb.append("\r\n預估全屬性 +").append(st.allStat()).append("，刻印加成 ").append(st.engraveBonus()).append("\r\n");
        return sb.toString();
    }

    public static String describeNextCosts(JewelState s) {
        JewelConfig cfg = config();
        StringBuilder sb = new StringBuilder();
        JewelConfig.EngraveStep e = JewelRules.nextEngrave(cfg, s);
        if (e != null) sb.append("下次刻印：").append(need(e.ingredient())).append("（成功 ").append(e.upgrade())
                .append("%／降級 ").append(e.downgrade()).append("%／損毀 ").append(e.destroy()).append("%）\r\n");
        JewelConfig.AwakenStep a = JewelRules.nextAwaken(cfg, s);
        if (a != null) sb.append("下次覺醒：").append(need(a.ingredient())).append("\r\n");
        sb.append("研磨重設：").append(fmt(cfg.tier(s.tier()).polishResetCost())).append(" 楓幣\r\n");
        return sb.toString();
    }

    public static String describePolishOdds(int slot) {
        JewelConfig.AwakenStep a = config().awaken().get(slot);
        if (a == null) return "";
        StringBuilder sb = new StringBuilder("第 " + slot + " 格研磨材料：" + need(a.polishIngredient()) + "\r\n機率：");
        a.polishWeights().forEach((g, w) -> sb.append(g).append("%=").append(w * 100 / a.totalWeight()).append("%  "));
        return sb.toString();
    }
}
