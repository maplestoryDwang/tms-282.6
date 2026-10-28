package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.enums.BaseStat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 萌獸 option bonuses on the server's stat totals (CUBE-RESEARCH.md / LINK-RESEARCH.md): the client shows the
 * summoned familiar's three options plus those of every linked familiar whose QR68469 E{slot} expiry has not passed
 * (640F760); the server adds the same set to the character's base stat cache so its own totals match. Options come
 * from Item/FamiliarOption.img (level 5, {@code familiar-options.tsv}). Familiar-side effects (recovery, on-attack
 * heals, attackType status effects, userD2F/userS2F, passivePlus) are not character stats and are left out.
 */
public final class Tms282FamiliarStats {
    public static final Path OPTIONS = Path.of("data", "protocol", "tms282.3", "familiar-options.tsv");
    public static final int LINK_QR = 68469;
    static final DateTimeFormatter QR_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Map<String, BaseStat> KEYS = Map.ofEntries(
            Map.entry("incSTR", BaseStat.str), Map.entry("incDEX", BaseStat.dex), Map.entry("incINT", BaseStat.inte),
            Map.entry("incLUK", BaseStat.luk), Map.entry("incMHP", BaseStat.mhp), Map.entry("incMMP", BaseStat.mmp),
            Map.entry("incPAD", BaseStat.pad), Map.entry("incMAD", BaseStat.mad), Map.entry("incPDD", BaseStat.pdd),
            Map.entry("incCr", BaseStat.cr), Map.entry("incFinalDam", BaseStat.fd), Map.entry("incAllStat", BaseStat.allStat),
            Map.entry("incSTRr", BaseStat.strR), Map.entry("incDEXr", BaseStat.dexR), Map.entry("incINTr", BaseStat.intR),
            Map.entry("incLUKr", BaseStat.lukR), Map.entry("incMHPr", BaseStat.mhpR), Map.entry("incMMPr", BaseStat.mmpR),
            Map.entry("incAllStatR", BaseStat.allStatR), Map.entry("incPADr", BaseStat.padR),
            Map.entry("incMADr", BaseStat.madR), Map.entry("incPDDr", BaseStat.pddR),
            Map.entry("ignoreTargetDEF", BaseStat.ied), Map.entry("bufftimeR", BaseStat.buffTimeR));

    private static volatile Map<Integer, Map<BaseStat, Integer>> options;
    /** What was added to each character's base stat cache, and to which Char object (a relog makes a new one). */
    private record Applied(java.lang.ref.WeakReference<Char> owner, Map<BaseStat, Integer> stats) {
    }
    private static final Map<Integer, Applied> APPLIED = new ConcurrentHashMap<>();

    private Tms282FamiliarStats() {
    }

    public static Map<Integer, Map<BaseStat, Integer>> options() {
        Map<Integer, Map<BaseStat, Integer>> map = options;
        if (map == null) {
            map = new HashMap<>();
            try {
                for (String line : Files.readAllLines(OPTIONS)) {
                    if (line.isBlank() || line.startsWith("#")) {
                        continue;
                    }
                    String[] f = line.split("\t", -1);
                    Map<BaseStat, Integer> stats = new EnumMap<>(BaseStat.class);
                    for (String kv : f[1].split(";")) {
                        int eq = kv.indexOf('=');
                        BaseStat bs = eq < 0 ? null : KEYS.get(kv.substring(0, eq));
                        if (bs != null) {
                            stats.merge(bs, Integer.parseInt(kv.substring(eq + 1).trim()), Integer::sum);
                        }
                    }
                    map.put(Integer.parseInt(f[0].strip()), Map.copyOf(stats));
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            map = Map.copyOf(map);
            options = map;
        }
        return map;
    }

    /** The option ids of one grade (grade * 10000 + n). */
    public static boolean isOption(int optionId) {
        return options().containsKey(optionId);
    }

    /** Whether link slot {@code slot} (0-based) is active: QR68469 E{slot+1} in the future. */
    public static boolean linkActive(Char chr, int slot) {
        String value = chr.getQRValueByKey(LINK_QR, "E" + (slot + 1));
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            return LocalDateTime.parse(value.trim(), QR_TIME).isAfter(LocalDateTime.now());
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** The stats the character should get from its familiars now. */
    static Map<BaseStat, Integer> wanted(Char chr, Tms282FamiliarState state, long[] links) {
        Map<BaseStat, Integer> total = new EnumMap<>(BaseStat.class);
        add(total, state.registered().get(state.summonedSerial()));
        for (int i = 0; links != null && i < 3; i++) {
            if (links[i] != 0 && links[i] != state.summonedSerial() && linkActive(chr, i)) {
                add(total, state.registered().get(links[i]));
            }
        }
        return total;
    }

    private static void add(Map<BaseStat, Integer> total, Tms282FamiliarState.RegisteredInfo record) {
        if (record == null) {
            return;
        }
        for (int option : new int[]{record.option1(), record.option2(), record.option3()}) {
            Map<BaseStat, Integer> stats = options().get(option);
            if (option != 0 && stats != null) {
                stats.forEach((bs, v) -> total.merge(bs, v, Integer::sum));
            }
        }
    }

    /** Moves the character's base stat cache from the applied familiar stats to the current ones. */
    public static void refresh(Char chr, Tms282FamiliarState state, long[] links) {
        Map<BaseStat, Integer> next = wanted(chr, state, links);
        Applied applied = APPLIED.get(chr.getId());
        Map<BaseStat, Integer> before = applied != null && applied.owner().get() == chr ? applied.stats() : Map.of();
        if (before.equals(next) && applied != null && applied.owner().get() == chr) {
            return;
        }
        before.forEach(chr::removeBaseStat);
        next.forEach(chr::addBaseStat);
        APPLIED.put(chr.getId(), new Applied(new java.lang.ref.WeakReference<>(chr), Map.copyOf(next)));
    }

    /** What is applied to {@code chr} (tests / logs). */
    public static Map<BaseStat, Integer> applied(Char chr) {
        Applied applied = APPLIED.get(chr.getId());
        return applied != null && applied.owner().get() == chr ? applied.stats() : Map.of();
    }
}
