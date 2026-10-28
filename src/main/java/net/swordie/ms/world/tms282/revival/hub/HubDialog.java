package net.swordie.ms.world.tms282.revival.hub;

import java.util.function.IntFunction;

/**
 * The hub NPC dialog as plain text pages (Command pattern on the selection number). Every page is either
 * {@code MENU:<text with #L links>} (ask again) or {@code SAY:<text>} (final message). The NPC scripts only loop:
 * <pre>page = sm.revivalHubPage(key); while page.startswith('MENU:'): page = sm.revivalHubSelect(key, sm.sendNext(page[5:]))</pre>
 * Formatting follows the panel rules (colour codes outside the links, nothing inside a #L).
 */
public final class HubDialog {
    public static final int MISSION_BASE = 100, SHOP = 900, BACK = 998, OFFER_BASE = 1000, BUY1_BASE = 3000, BUY10_BASE = 4000;
    private static final String GREY = "#fc0xFF8C8C8C#", TITLE = "#fc0xFF1098AD#";

    /** Live operations the dialog drives (adapter over MissionService / HubExchange for one character). */
    public interface Ops {
        MissionService.Status status(String missionId);

        MissionService.AcceptResult accept(String missionId);

        MissionService.ClaimResult claim(String missionId);

        HubExchange.Result buy(HubDefinition.Offer offer, int times);

        int count(int itemId);
    }

    private final HubDefinition hub;
    private final Ops ops;
    private final IntFunction<String> itemName;

    public HubDialog(HubDefinition hub, Ops ops, IntFunction<String> itemName) {
        this.hub = hub;
        this.ops = ops;
        this.itemName = itemName;
    }

    private String name(int itemId) {
        String n = itemName.apply(itemId);
        return n == null || n.isBlank() ? "道具 " + itemId : n;
    }

    private static String sanitize(String s) {
        return s.replace("#", "");
    }

    public String home() {
        StringBuilder t = new StringBuilder("MENU:#w").append(TITLE).append("#fs13##e").append(hub.title()).append("#n#k#fs12#\r\n");
        t.append(GREY).append("任務獎勵每個帳號每期限領一次。#k\r\n\r\n");
        for (int i = 0; i < hub.missions().size(); i++) {
            HubDefinition.Mission m = hub.missions().get(i);
            t.append("#b#L").append(MISSION_BASE + i).append('#').append('[').append(m.period().label()).append("] ")
                    .append(m.title()).append(' ').append(state(m)).append("#l#k\r\n");
        }
        if (!hub.offers().isEmpty()) t.append("\r\n#d#L").append(SHOP).append("#活動商店#l#k");
        return t.toString();
    }

    String state(HubDefinition.Mission m) {
        MissionService.Status s = ops.status(m.id());
        if (s.claimed()) return "(已領取)";
        if (!m.counted()) return s.count() >= m.target() ? "(可領取)" : "(需要 Lv." + m.target() + ")";
        if (!s.accepted()) return "(未接受)";
        return s.ready() ? "(可領取)" : "(" + s.count() + "/" + s.target() + ")";
    }

    public String shop() {
        StringBuilder t = new StringBuilder("MENU:#w").append(TITLE).append("#fs13##e").append(hub.title()).append(" 商店#n#k#fs12#\r\n");
        int coin = -1;
        for (HubDefinition.Offer o : hub.offers()) {
            if (o.costItem() != coin) {
                coin = o.costItem();
                t.append(GREY).append("持有 ").append(sanitize(name(coin))).append("：").append(ops.count(coin)).append("#k\r\n");
            }
        }
        t.append("\r\n");
        for (int j = 0; j < hub.offers().size(); j++) {
            HubDefinition.Offer o = hub.offers().get(j);
            t.append("#b#L").append(OFFER_BASE + j).append('#').append(sanitize(name(o.itemId()))).append(" x").append(o.quantity())
                    .append(" = ").append(sanitize(name(o.costItem()))).append(" x").append(o.costQuantity()).append("#l#k\r\n");
        }
        t.append("\r\n#b#L").append(BACK).append("#返回#l#k");
        return t.toString();
    }

    public String select(int sel) {
        if (sel == BACK) return home();
        if (sel == SHOP) return shop();
        if (sel >= MISSION_BASE && sel < MISSION_BASE + hub.missions().size()) return mission(hub.missions().get(sel - MISSION_BASE));
        if (sel >= OFFER_BASE && sel < OFFER_BASE + hub.offers().size()) {
            HubDefinition.Offer o = hub.offers().get(sel - OFFER_BASE);
            int j = sel - OFFER_BASE;
            return "MENU:" + sanitize(name(o.itemId())) + " x" + o.quantity() + "\r\n" + GREY + "每次需要 " + sanitize(name(o.costItem()))
                    + " x" + o.costQuantity() + "，持有 " + ops.count(o.costItem()) + "。#k\r\n\r\n#b#L" + (BUY1_BASE + j) + "#兌換 1 次#l #L"
                    + (BUY10_BASE + j) + "#兌換 10 次#l#k\r\n\r\n#b#L" + SHOP + "#返回商店#l#k";
        }
        if (sel >= BUY1_BASE && sel < BUY1_BASE + hub.offers().size()) return buy(hub.offers().get(sel - BUY1_BASE), 1);
        if (sel >= BUY10_BASE && sel < BUY10_BASE + hub.offers().size()) return buy(hub.offers().get(sel - BUY10_BASE), 10);
        return "SAY:";
    }

    private String mission(HubDefinition.Mission m) {
        MissionService.Status s = ops.status(m.id());
        if (s.claimed()) return "SAY:這一期的「" + m.title() + "」已經領取過了。";
        if (m.counted() && !s.accepted()) {
            return "SAY:" + switch (ops.accept(m.id())) {
                case ACCEPTED -> "已接受「" + m.title() + "」。\r\n擊敗怪物後會自動計算進度，完成後再來找我領獎。";
                case ALREADY -> "已經接受過了。";
                case CLAIMED -> "這一期已經領取過了。";
                case LEVEL -> "等級需要 " + m.minLevel() + " 以上。";
                case CLOSED -> "活動目前沒有開放。";
                case NOT_COUNTED -> "這個任務不需要接受。";
                case FAILED -> "暫時無法接受任務，請稍後再試。";
            };
        }
        if (m.counted() && !s.ready()) return "SAY:「" + m.title() + "」進度 " + s.count() + " / " + s.target() + "。";
        StringBuilder rewards = new StringBuilder();
        for (HubDefinition.Reward r : m.rewards()) rewards.append("\r\n").append(sanitize(name(r.itemId()))).append(" x").append(r.quantity());
        return "SAY:" + switch (ops.claim(m.id())) {
            case GRANTED -> "完成「" + m.title() + "」！獲得：" + rewards;
            case ACCOUNT_ALREADY -> "本帳號這一期已經由其他角色領取過「" + m.title() + "」。";
            case ALREADY -> "已經領取過了。";
            case NOT_READY -> "還沒有完成。";
            case NOT_ACCEPTED -> "請先接受任務。";
            case LEVEL -> "等級需要 " + m.target() + " 以上。";
            case FULL -> "背包空間不足，整理後再來領取（沒有扣除任何紀錄）。";
            case CLOSED -> "活動目前沒有開放。";
            case FAILED -> "暫時無法領取，請稍後再試。";
        };
    }

    private String buy(HubDefinition.Offer o, int times) {
        return "SAY:" + switch (ops.buy(o, times)) {
            case BOUGHT -> "兌換成功：" + sanitize(name(o.itemId())) + " x" + (o.quantity() * times) + "。";
            case NOT_ENOUGH -> sanitize(name(o.costItem())) + " 不足（需要 " + (o.costQuantity() * times) + "）。";
            case FULL -> "背包空間不足，沒有扣除任何道具。";
            case UNAVAILABLE -> "這個道具目前無法兌換。";
            case BAD_AMOUNT -> "數量不正確。";
            case CLOSED -> "活動目前沒有開放。";
            case OFFLINE -> "請在遊戲地圖中再試一次。";
            case FAILED -> "兌換時發生錯誤，請回報管理員。";
        };
    }
}
