package net.swordie.ms.client.character.tms282;

import net.swordie.ms.ServerConstants;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.loaders.StringData;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The soul weapon option a soul (2591xxx) gives, without depending on the item's display language.
 * <p>
 * GMS 265 {@link ItemConstants#getSoulOptionFromSoul} derives it from the English item name (prefix = stat kind,
 * boss = tier). The TMS 282.3 server's names are Chinese, so every soul matched nothing and got a random
 * {@code soulPotList} option. Soul ids are shared by GMS and TMS (checked pair by pair: 271 of the 350 282 souls /
 * marbles have a GMS 265 name with the same prefix and boss), so under the 282 profile the option is resolved from
 * the soul's GMS name, read by id from {@code data/wz265/String.wz/Consume.img.xml}. A 282-only soul
 * (2591849.. 凱伊 / 燦爛的凶星 / 尤比太 / 龍咆哮) gets its prefix and boss translated from its 282 name with the
 * pairs learnt from the shared ids; a boss GMS 265 does not know keeps the 265 fallback (random soulPotList).
 * <p>
 * Under the GMS 265 profile {@link #optionFor} is exactly {@link ItemConstants#getSoulOptionFromSoul}.
 */
public final class Tms282SoulOptions {
    static final Path GMS_STRINGS = Path.of(ServerConstants.DIR, "data", "wz265", "String.wz", "Consume.img.xml");
    static final Path TMS_STRINGS = Path.of(ServerConstants.DIR, "data", "wz282", "String.wz", "Consume.img.xml");

    /** 265 prefixes (stat kind), and their TMS 282 words (from the shared ids). */
    static final Map<String, String> PREFIX_ZH = Map.ofEntries(
            Map.entry("充滿力量的", "Beefy"), Map.entry("敏捷的", "Swift"), Map.entry("聰明的", "Clever"),
            Map.entry("驚人的", "Fortuitous"), Map.entry("強韌的", "Hearty"), Map.entry("豐富的", "Ample"),
            Map.entry("華麗的", "Flashy"), Map.entry("強力的", "Potent"), Map.entry("發光的", "Radiant"),
            Map.entry("銳利的", "Sharp"), Map.entry("破壞的", "Destructive"), Map.entry("偉大的", "Magnificent"));

    /** 265 boss tiers 1..9 in 265's check order (substring match on the English name). */
    static final List<List<String>> TIERS = List.of(
            List.of("Spirit of Rock", "Prison Guard Ani", "Xerxes", "Black Slime"),
            List.of("Dragon Rider", "Rex"),
            List.of("Mu Gong", "Pianus"),
            List.of("Balrog", "Tutu", "Nene"),
            List.of("Zakum", "Gold Dragon", "Red Tiger"),
            List.of("Von Leon", "Hilla"),
            List.of("Arkarium", "Mad Mage", "Rampant Cyborg", "Vicious Hunter", "Bad Brawler"),
            List.of("Pink Bean", "Black Knight", "Pierre", "Von Bon"),
            List.of("Cygnus", "Magnus", "Murgoth", "Crimson Queen", "Vellum", "Lotus", "Damien", "Lucid"));

    private static volatile Map<Integer, String> gmsNames;

    private Tms282SoulOptions() {
    }

    /**
     * The option for {@code soulItemId}. GMS 265 profile: {@link ItemConstants#getSoulOptionFromSoul} unchanged.
     * TMS 282: {@link #tableOption} of the soul's English name, else the 265 fallback (random soulPotList, through
     * {@link ItemConstants#getSoulOptionFromSoul} when the item has a name). Throws when neither exists, before the
     * caller has changed anything.
     */
    public static int optionFor(int soulItemId) {
        if (Protocol.active().version() != ProtocolVersion.TMS_282_3) {
            return ItemConstants.getSoulOptionFromSoul(soulItemId);
        }
        String english = englishName(soulItemId);
        if (english != null) {
            OptionalInt fixed = tableOption(english);
            if (fixed.isPresent()) {
                return fixed.getAsInt();
            }
        }
        if (StringData.getItemStringById(soulItemId) == null) {
            throw new IllegalStateException("soul " + soulItemId + " has no name in any table");
        }
        return ItemConstants.getSoulOptionFromSoul(soulItemId); // 265 fallback: random soulPotList
    }

    /** The GMS English name of a soul: GMS 265 String by id, else translated from its TMS 282 name. */
    public static String englishName(int soulItemId) {
        String gms = gmsNames().get(soulItemId);
        if (gms != null) {
            return gms;
        }
        String zh = tmsNames().get(soulItemId);
        return zh == null ? null : translate(zh);
    }

    /**
     * The 265 {@code getSoulOptionFromSoul} table for an English name, or empty where 265 falls back to a random
     * soulPotList option (Magnificent, unknown boss, a prefix without a value for that tier).
     */
    public static OptionalInt tableOption(String name) {
        int tier = 0;
        for (int t = 0; t < TIERS.size() && tier == 0; t++) {
            for (String boss : TIERS.get(t)) {
                if (name.contains(boss)) {
                    tier = t + 1;
                    break;
                }
            }
        }
        if (name.startsWith("Beefy") || name.startsWith("Swift") || name.startsWith("Clever") || name.startsWith("Fortuitous")) {
            int[] base = {0, 101, 105, 109, 113, 163, 117, 185, 121, 167};
            if (tier == 0) return OptionalInt.empty();
            int k = name.startsWith("Beefy") ? 0 : name.startsWith("Swift") ? 1 : name.startsWith("Clever") ? 2 : 3;
            return OptionalInt.of(base[tier] + k);
        }
        if (name.startsWith("Hearty") || name.startsWith("Ample")) {
            int[] hearty = {0, 131, 133, 135, 137, 175, 138, 189, 139, 177};
            int[] ample = {0, 132, 134, 136, 0, 0, 0, 0, 0, 0};
            if (tier == 0) return OptionalInt.empty();
            return OptionalInt.of(name.startsWith("Hearty") ? hearty[tier] : ample[tier]);
        }
        if (name.startsWith("Flashy")) {
            int[] flashy = {0, 201, 202, 203, 204, 210, 205, 0, 40081, 70057};
            return tier == 0 || flashy[tier] == 0 ? OptionalInt.empty() : OptionalInt.of(flashy[tier]);
        }
        if (name.startsWith("Potent") || name.startsWith("Radiant")) {
            boolean potent = name.startsWith("Potent");
            return switch (tier) {
                case 5 -> OptionalInt.of(potent ? 2011 : 2012);
                case 6, 7 -> OptionalInt.of(potent ? 153 : 154);
                case 8 -> OptionalInt.of(potent ? 155 : 156);
                case 9 -> OptionalInt.of(potent ? 171 : 172);
                default -> OptionalInt.empty();
            };
        }
        if (name.startsWith("Sharp")) return OptionalInt.of(42292);
        if (name.startsWith("Destructive")) return OptionalInt.of(60009);
        return OptionalInt.empty();
    }

    /** "<prefix><boss>(的)靈魂" -> "<Prefix> <Boss> Soul" with the boss pairs learnt from the shared ids. */
    static String translate(String zh) {
        String prefix = null;
        String rest = zh;
        for (Map.Entry<String, String> e : PREFIX_ZH.entrySet()) {
            if (zh.startsWith(e.getKey())) {
                prefix = e.getValue();
                rest = zh.substring(e.getKey().length());
                break;
            }
        }
        String boss = bossKey(rest);
        String en = bosses().get(boss);
        if (prefix == null || en == null) {
            return null;
        }
        return prefix + " " + en + " Soul";
    }

    private static String bossKey(String rest) {
        String s = rest.replace("靈魂寶珠", "").replace("靈魂", "");
        return s.endsWith("的") ? s.substring(0, s.length() - 1) : s;
    }

    // ---- tables -------------------------------------------------------------------------------------------------

    private static volatile Map<Integer, String> tmsNames;
    private static volatile Map<String, String> bosses;

    static Map<Integer, String> gmsNames() {
        Map<Integer, String> m = gmsNames;
        if (m == null) {
            gmsNames = m = souls(GMS_STRINGS);
        }
        return m;
    }

    static Map<Integer, String> tmsNames() {
        Map<Integer, String> m = tmsNames;
        if (m == null) {
            tmsNames = m = souls(TMS_STRINGS);
        }
        return m;
    }

    /** TMS boss word -> GMS boss name, from the souls both tables name with a known prefix. */
    static Map<String, String> bosses() {
        Map<String, String> m = bosses;
        if (m == null) {
            Map<String, String> b = new HashMap<>();
            for (Map.Entry<Integer, String> e : tmsNames().entrySet()) {
                String en = gmsNames().get(e.getKey());
                if (en == null) continue;
                for (Map.Entry<String, String> p : PREFIX_ZH.entrySet()) {
                    if (e.getValue().startsWith(p.getKey()) && en.startsWith(p.getValue() + " ")) {
                        String enBoss = en.substring(p.getValue().length() + 1).replaceAll("('s)? Soul$", "");
                        b.putIfAbsent(bossKey(e.getValue().substring(p.getKey().length())), enBoss);
                        break;
                    }
                }
            }
            bosses = m = Collections.unmodifiableMap(b);
        }
        return m;
    }

    private static final Pattern SOUL = Pattern.compile(
            "<imgdir name=\"(2591\\d{3})\">\\s*<string name=\"name\" value=\"([^\"]*)\"");

    private static Map<Integer, String> souls(Path strings) {
        Map<Integer, String> m = new HashMap<>();
        try {
            Matcher x = SOUL.matcher(Files.readString(strings));
            while (x.find()) {
                m.put(Integer.parseInt(x.group(1)), x.group(2));
            }
        } catch (Exception e) {
            System.out.println("[TMS282] soul names: cannot read " + strings + ": " + e);
        }
        return Collections.unmodifiableMap(m);
    }
}
