package net.swordie.ms.world.tms282.symbols;

import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.BaseStat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 豪華真實符文 (grand authentic symbol, 1714xxx worn in 1750..1752) per-level EXP / meso / drop bonuses. The 282
 * client data only has the description text (String.wz Eqp 1714000/1714001: 楓幣獲得量、道具掉落率、追加經驗值獲得量),
 * so the values come from {@code data/resources/tms282/symbols/grand-authentic.tsv}
 * ({@code itemId, name, level, expR, mesoR, dropR, aut, source}; sources: research/tms2823/grand-authentic-symbol).
 * The bonuses are plain percentages added into {@link BaseStat#expR} / {@link BaseStat#mesoR} / {@link BaseStat#dropR}
 * through {@code Equip.getBaseStat}, i.e. additive with every other % source. 282 profile only.
 */
public final class Tms282GrandAuthenticSymbols {
    /** One level of one symbol. */
    public record Level(int itemId, int level, int expR, int mesoR, int dropR, int aut) {
    }

    private static volatile Tms282GrandAuthenticSymbols shared;
    private final Map<Integer, Map<Integer, Level>> levels;
    private final Map<Integer, Integer> maxLevel;

    Tms282GrandAuthenticSymbols(Map<Integer, Map<Integer, Level>> levels) {
        Map<Integer, Map<Integer, Level>> copy = new HashMap<>();
        Map<Integer, Integer> max = new HashMap<>();
        levels.forEach((id, byLevel) -> {
            copy.put(id, Map.copyOf(byLevel));
            max.put(id, byLevel.keySet().stream().mapToInt(Integer::intValue).max().orElse(0));
        });
        this.levels = Map.copyOf(copy);
        this.maxLevel = Map.copyOf(max);
    }

    public static Path defaultFile() {
        return Path.of(System.getProperty("tms282.boxesDir", "data/resources/tms282"), "symbols", "grand-authentic.tsv");
    }

    /** Loaded once from {@link #defaultFile()}; a missing file gives an empty table (no bonus). */
    public static Tms282GrandAuthenticSymbols get() {
        Tms282GrandAuthenticSymbols t = shared;
        if (t == null) {
            synchronized (Tms282GrandAuthenticSymbols.class) {
                if (shared == null) {
                    Path file = defaultFile();
                    if (Files.isRegularFile(file)) {
                        shared = load(file);
                    } else {
                        System.out.println("[TMS282] grand authentic symbol table missing: " + file.toAbsolutePath());
                        shared = new Tms282GrandAuthenticSymbols(Map.of());
                    }
                }
                t = shared;
            }
        }
        return t;
    }

    public static Tms282GrandAuthenticSymbols load(Path file) {
        Map<Integer, Map<Integer, Level>> map = new HashMap<>();
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isBlank() || !Character.isDigit(line.charAt(0))) {
                    continue; // comments and the header
                }
                String[] c = line.split("\t", -1);
                if (c.length < 8 || c[7].isBlank()) {
                    throw new IllegalArgumentException("grand authentic symbol row needs 8 columns and a source: " + line);
                }
                Level l = new Level(Integer.parseInt(c[0].trim()), Integer.parseInt(c[2].trim()),
                        Integer.parseInt(c[3].trim()), Integer.parseInt(c[4].trim()), Integer.parseInt(c[5].trim()),
                        Integer.parseInt(c[6].trim()));
                if (map.computeIfAbsent(l.itemId(), k -> new HashMap<>()).put(l.level(), l) != null) {
                    throw new IllegalArgumentException("duplicate grand authentic symbol level: " + line);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new Tms282GrandAuthenticSymbols(map);
    }

    /** The row of {@code itemId} at {@code level}, clamped to the table's max level; null below level 1 or unknown. */
    public Level level(int itemId, int level) {
        Map<Integer, Level> byLevel = levels.get(itemId);
        if (byLevel == null || level < 1) {
            return null;
        }
        return byLevel.get(Math.min(level, maxLevel.get(itemId)));
    }

    public int maxLevel(int itemId) {
        return maxLevel.getOrDefault(itemId, 0);
    }

    public int size() {
        return levels.values().stream().mapToInt(Map::size).sum();
    }

    /** The % a symbol at {@code level} gives to expR / mesoR / dropR, else 0. */
    public int bonus(int itemId, int level, BaseStat stat) {
        Level l = level(itemId, level);
        if (l == null) {
            return 0;
        }
        return switch (stat) {
            case expR -> l.expR();
            case mesoR -> l.mesoR();
            case dropR -> l.dropR();
            default -> 0;
        };
    }

    /**
     * What a worn equip adds to {@code stat} as a grand authentic symbol: only on the 282 profile, only for a 1714xxx
     * item in 1750..1752 (an unworn symbol or the 265 profile gives 0).
     */
    public static int wornBonus(int itemId, int bagIndex, int symbolLevel, BaseStat stat) {
        if (itemId / 1000 != 1714
                || (stat != BaseStat.expR && stat != BaseStat.mesoR && stat != BaseStat.dropR)
                || Protocol.active().version() != ProtocolVersion.TMS_282_3) {
            return 0;
        }
        int slot = Math.abs(bagIndex);
        if (slot < ItemConstants.GRAND_AUTHENTIC_SYMBOL_BASE || slot >= ItemConstants.GRAND_AUTHENTIC_SYMBOL_END) {
            return 0;
        }
        return get().bonus(itemId, symbolLevel, stat);
    }
}
