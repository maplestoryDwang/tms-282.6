package net.swordie.ms.world.tms282.bossrun;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.social.Party.PartyBoss;
import net.swordie.ms.enums.BossPartyType;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * GM view / reset of the boss-run ledger (used by {@link BossRunCommands} "!" and {@link BossRunChatCommands} "@").
 * Targets are resolved online first ({@code online}), else through the store (offline character by name).
 */
public final class BossRunAdmin {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("MM/dd HH:mm");

    private final BossRunService service;
    private final Function<String, Char> online;

    public BossRunAdmin(BossRunService service, Function<String, Char> online) {
        this.service = service;
        this.online = online;
    }

    public record Target(int charId, int accountId, String name, Char chr) {
    }

    public Target resolve(String name) {
        Char c = null;
        try {
            c = online.apply(name);
        } catch (RuntimeException ignored) {
        }
        if (c != null) return new Target(c.getId(), BossRunService.accountIdOf(c), c.getName(), c);
        BossRunStore.CharRef ref = service.store().findChar(name);
        return ref == null ? null : new Target(ref.charId(), ref.accountId(), ref.name(), null);
    }

    /** Boss keys named by an argument: enum name (ZAKUM_EASY), English boss name (Zakum / "Kalos the Guardian" as Kalos), Chinese name, or aufheben. */
    public static Set<String> bossKeys(String arg) {
        Set<String> out = new LinkedHashSet<>();
        if (arg == null || arg.isBlank()) return out;
        String a = arg.trim();
        if (a.equalsIgnoreCase("aufheben") || a.equals(AufhebenBoss.DISPLAY)) {
            out.add(AufhebenBoss.BOSS_KEY);
            return out;
        }
        for (BossPartyType b : BossPartyType.values()) {
            String zh = BossRunNames.boss(b.getOrderId(), "");
            if (b.name().equalsIgnoreCase(a) || b.getBossName().equalsIgnoreCase(a)
                    || b.getBossName().toLowerCase(Locale.ROOT).startsWith(a.toLowerCase(Locale.ROOT) + " ")
                    || b.getBossName().replace(" ", "").equalsIgnoreCase(a)
                    || (!zh.isEmpty() && (zh.equals(a) || BossRunNames.of(b).equals(a)))) {
                out.add(b.name());
            }
        }
        return out;
    }

    public List<String> log(String name, int limit) {
        List<String> lines = new ArrayList<>();
        Target t = resolve(name);
        if (t == null) {
            lines.add("找不到角色 " + name + "。");
            return lines;
        }
        lines.add("[首領紀錄] " + t.name() + "（角色 " + t.charId() + "，帳號 " + t.accountId() + "）模式："
                + (service.config().limitMode() == BossRunConfig.LimitMode.ACCOUNT ? "帳號次數" : "角色次數"));
        var since = BossRunLimits.weekStart(service.now(), service.config().weekResetDay());
        TreeMap<String, Integer> keys = new TreeMap<>();
        for (BossRunStore.ClearRow r : service.store().countedClears(t.accountId(), since)) keys.merge(r.bossKey(), 1, Integer::sum);
        if (keys.isEmpty()) {
            lines.add("本週沒有計次的通關。");
        } else {
            for (String k : keys.keySet()) {
                BossRunLedger.Usage u = service.ledger().usage(t.accountId(), k, service.now(), 0);
                lines.add(label(k) + "：今日 " + u.today() + " 次、本週 " + u.week() + " 次（" + BossRunService.limitText(u) + "）");
            }
        }
        List<BossRunStore.ClearRow> recent = service.store().recentClears(t.accountId(), Math.max(1, limit));
        if (!recent.isEmpty()) lines.add("最近 " + recent.size() + " 筆：");
        for (BossRunStore.ClearRow r : recent) {
            lines.add(TIME.format(r.clearedAt()) + " " + label(r.bossKey()) + " " + r.charName() + " 用時 "
                    + BossRunService.clock(r.clearMillis()) + " 傷害 " + BossRunService.pct(r.damagePermille())
                    + (r.counted() ? "" : " [不計次" + (r.note().isEmpty() ? "" : ":" + r.note()) + "]"));
        }
        int pending = service.pendingCount(t.charId());
        if (pending > 0) lines.add("暫存獎勵 " + pending + " 項。");
        int au = service.store().pqCount(t.charId(), service.ledger().limits().aufhebenLogKey(), service.now().toLocalDate());
        if (au > 0) lines.add("奧芙赫班今日入場 " + au + "/" + service.ledger().limits().aufhebenDaily() + "。");
        return lines;
    }

    /** Resets the account ledger (counted -> uncounted), the character's legacy partyboss rows and Aufheben entries. */
    public String reset(String name, String bossArg) {
        Target t = resolve(name);
        if (t == null) return "找不到角色 " + name + "。";
        Set<String> keys = bossKeys(bossArg);
        if (bossArg != null && !bossArg.isBlank() && keys.isEmpty()) return "找不到首領「" + bossArg + "」。";
        int ledgerRows = 0;
        int legacyRows = 0;
        int pqRows = 0;
        if (keys.isEmpty()) {
            ledgerRows = service.store().resetCounted(t.accountId(), null);
            legacyRows = resetLegacy(t, null);
            pqRows = service.store().pqReset(t.charId(), service.ledger().limits().aufhebenLogKey());
        } else {
            for (String k : keys) {
                ledgerRows += service.store().resetCounted(t.accountId(), k);
                if (k.equals(AufhebenBoss.BOSS_KEY)) {
                    pqRows += service.store().pqReset(t.charId(), service.ledger().limits().aufhebenLogKey());
                } else {
                    legacyRows += resetLegacy(t, BossPartyType.valueOf(k));
                }
            }
        }
        return "已重置 " + t.name() + (keys.isEmpty() ? " 的全部首領次數" : " 的 " + String.join("、", keys.stream().map(BossRunAdmin::label).toList()))
                + "：帳號紀錄 " + ledgerRows + " 筆改為不計次、角色次數 " + legacyRows + " 筆、奧芙赫班入場 " + pqRows + " 筆。";
    }

    private int resetLegacy(Target t, BossPartyType bpt) {
        int n = 0;
        if (t.chr() != null) {
            List<PartyBoss> remove = new ArrayList<>();
            for (PartyBoss pb : t.chr().getPartyboss()) {
                if (bpt == null || (pb.getOrderId() == bpt.getOrderId() && pb.getDifficulty() == bpt.getDifficulty().getVal())) {
                    remove.add(pb);
                }
            }
            for (PartyBoss pb : remove) {
                t.chr().getPartyboss().remove(pb);
                n++;
            }
        }
        // Rows of the character in the table (online rows too: deleting them keeps the next save consistent).
        int db = bpt == null ? service.store().resetLegacyPartyBoss(t.charId(), -1, -1)
                : service.store().resetLegacyPartyBoss(t.charId(), bpt.getOrderId(), bpt.getDifficulty().getVal());
        return Math.max(n, db);
    }

    public static String label(String bossKey) {
        if (AufhebenBoss.BOSS_KEY.equals(bossKey)) return AufhebenBoss.DISPLAY;
        try {
            return BossRunNames.of(BossPartyType.valueOf(bossKey));
        } catch (IllegalArgumentException e) {
            return bossKey;
        }
    }
}
