package net.swordie.ms.console.config;

import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.swordie.ms.constants.BossHpConstants;
import net.swordie.ms.enums.BossPartyType;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public final class BossRuntimeConfig {
    public static final Path CONFIG_FILE = Path.of("config", "boss.properties");

    private static final Properties defaults = new Properties();
    private static Properties values;

    static {
        for (BossPartyType type : BossPartyType.values()) {
            defaults.setProperty(enabledKey(type), "true");
            Int2LongOpenHashMap ctx = BossHpConstants.getDefaultContextHpMap(type);
            if (ctx != null) {
                ctx.forEach((mobId, hp) -> defaults.setProperty(hpKey(type, mobId), String.valueOf(hp)));
            }
        }
        reload();
    }

    private BossRuntimeConfig() {
    }

    public static synchronized void reload() {
        Properties next = new Properties();
        next.putAll(defaults);
        if (Files.isRegularFile(CONFIG_FILE)) {
            try (FileInputStream in = new FileInputStream(CONFIG_FILE.toFile())) {
                next.load(in);
            } catch (IOException ignored) {
            }
        }
        values = next;
    }

    public static synchronized Properties snapshot() {
        Properties copy = new Properties();
        copy.putAll(defaults);
        copy.putAll(values);
        return copy;
    }

    public static synchronized void save(Map<String, String> updates) throws IOException {
        Properties next = snapshot();
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            next.setProperty(entry.getKey(), entry.getValue() == null ? "" : entry.getValue().trim());
        }
        Path parent = CONFIG_FILE.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE.toFile())) {
            next.store(out, "TMS282.3 boss HP and difficulty settings");
        }
        values = next;
    }

    public static String enabledKey(BossPartyType type) {
        return "boss.enable." + type.name();
    }

    public static String hpKey(BossPartyType type, int mobId) {
        return "boss.hp." + type.name() + "." + mobId;
    }

    public static boolean isDifficultyEnabled(BossPartyType type) {
        if (type == null) {
            return false;
        }
        return Boolean.parseBoolean(stringValue(enabledKey(type), "true"));
    }

    public static long getContextHp(BossPartyType type, int mobId) {
        if (type == null) {
            return 0L;
        }
        String raw = stringValue(hpKey(type, mobId), "");
        if (raw != null && !raw.isBlank()) {
            try {
                long parsed = Long.parseLong(raw.trim());
                if (parsed > 0) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return BossHpConstants.getDefaultContextHp(type, mobId);
    }

    public static Map<String, String> exportBossSettings() {
        Map<String, String> map = new LinkedHashMap<>();
        for (BossPartyType type : BossPartyType.values()) {
            map.put(enabledKey(type), String.valueOf(isDifficultyEnabled(type)));
            Int2LongOpenHashMap ctx = BossHpConstants.getDefaultContextHpMap(type);
            if (ctx != null) {
                ctx.forEach((mobId, ignored) -> {
                    long hp = getContextHp(type, mobId);
                    if (hp > 0) {
                        map.put(hpKey(type, mobId), String.valueOf(hp));
                    }
                });
            }
        }
        return map;
    }

    public static String bossChineseName(int orderId) {
        return switch (orderId) {
            case 0 -> "蝙蝠怪";
            case 1 -> "扎昆";
            case 2 -> "暗黑龍王";
            case 3 -> "希拉";
            case 4 -> "皮埃爾";
            case 5 -> "半半";
            case 6 -> "血腥女王";
            case 7 -> "貝倫";
            case 8 -> "獅子王";
            case 9 -> "阿卡伊勒";
            case 10 -> "麥格納斯";
            case 11 -> "品克繽";
            case 12 -> "希納斯";
            case 13 -> "斯烏";
            case 14 -> "烏魯斯";
            case 15 -> "戴米安";
            case 16 -> "貝勒德";
            case 17 -> "森蘭丸";
            case 18 -> "濃姬";
            case 19 -> "露希妲";
            case 21 -> "卡翁";
            case 22 -> "鬧鐘";
            case 23 -> "威爾";
            case 24 -> "真希拉";
            case 25 -> "黑魔法師";
            case 26 -> "黑暗魔眼";
            case 27 -> "敦凱爾";
            case 28 -> "塞倫";
            case 29 -> "守護天使綠水靈";
            case 30 -> "卡洛斯";
            case 31 -> "卡凌";
            case 32 -> "極限怪物公園";
            case 33 -> "林波";
            case 34 -> "巴德里克斯";
            case 35 -> "第一使徒";
            case 107 -> "明智光秀";
            default -> "首領#" + orderId;
        };
    }

    public static String difficultyLabel(BossPartyType type) {
        return type.getDifficulty().getName();
    }

    public static String hpStatus(long hp, int mobId) {
        if (BossHpConstants.isBalrogPart(mobId)) {
            return hp <= 1 ? "正常(傷害彙總機制)" : "部位血量異常";
        }
        if (hp <= 0) {
            return "未配置(可能使用WZ血量)";
        }
        if (hp < 10_000) {
            return "危險: 極易被秒殺";
        }
        if (hp < 100_000) {
            return "偏低: 建議提高";
        }
        return "正常";
    }

    private static String stringValue(String key, String fallback) {
        String fromFile = snapshot().getProperty(key);
        if (fromFile != null) {
            return fromFile;
        }
        return fallback;
    }
}
