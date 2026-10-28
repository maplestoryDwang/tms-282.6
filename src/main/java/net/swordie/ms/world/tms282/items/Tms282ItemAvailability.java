package net.swordie.ms.world.tms282.items;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.StringData;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToIntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Central "may the server hand this item to a TMS 282.3 client" check for every custom grant path (GM SendItem,
 * AdminEquipment presets, custom shops, 自選清單, and - via the hook in Char.addItemToInventory - scripts).
 * <p>
 * Plan B1 of research/tms2823/item-usability-20260928/consumables. An item is grantable when
 * <ol>
 *     <li>the server has a template for it (dat282 overlay first, then dat265 - {@link ItemData}); otherwise creating
 *     it would NPE or the client would get an item neither data set knows (MISSING_ID_CRASH), and</li>
 *     <li>under the 282 profile it is not in {@code data/protocol/tms282.3/items-removed.tsv} (the 14,988 ids of the
 *     265 data the 282 client no longer has, from the full client diff packet-audit/data282-diff-item.tsv). The 282
 *     client asserts (0x21000003) and disconnects when it has to draw such an item (live 2026-09-24).</li>
 * </ol>
 * 282 String.wz presence is <b>not</b> a gate: 10,157 dat282 overlay items and ~29k client items have no String entry
 * but exist in the client. It is only used to explain a rejection (282-only item the server has no data for yet vs.
 * an id neither version knows). The 265 profile only applies the template check.
 */
public final class Tms282ItemAvailability {

    public enum Verdict {
        OK,
        /** Present in the 265 server data but removed from the 282 client. */
        REMOVED_282,
        /** Only the 282 client has it; the server has no template to create it from. */
        CLIENT_ONLY_282,
        /** Neither the server data nor the 282 client strings know the id. */
        UNKNOWN_ID
    }

    public static final Path STRING_DIR = Path.of("data", "wz282", "String.wz");
    private static final String[] STRING_FILES = {"Eqp", "Consume", "Cash", "Etc", "Ins", "Pet"};
    private static final Pattern ITEM_DIR = Pattern.compile("<imgdir name=\"(\\d{7,8})\"");

    private static volatile net.swordie.ms.connection.packet.Tms282ItemAvailability removed;
    private static volatile Map<Integer, String> names282;
    private static final Map<Integer, Boolean> TEMPLATE = new ConcurrentHashMap<>();
    private static final Set<Integer> LOGGED = ConcurrentHashMap.newKeySet();

    private Tms282ItemAvailability() {
    }

    public static boolean is282() {
        return Protocol.active().version() == ProtocolVersion.TMS_282_3;
    }

    public static Verdict check(int itemId) {
        if (itemId <= 0) {
            return Verdict.UNKNOWN_ID;
        }
        if (is282() && !removedList().isAvailable(itemId)) {
            return Verdict.REMOVED_282;
        }
        if (!hasTemplate(itemId)) {
            return is282() && names282().containsKey(itemId) ? Verdict.CLIENT_ONLY_282 : Verdict.UNKNOWN_ID;
        }
        return Verdict.OK;
    }

    public static boolean isGrantable(int itemId) {
        return check(itemId) == Verdict.OK;
    }

    /** Traditional Chinese reason for players/GMs, or null when the item is grantable. */
    public static String rejectMessage(int itemId) {
        Verdict v = check(itemId);
        String name = displayName(itemId);
        String label = name == null || name.isEmpty() ? String.valueOf(itemId) : itemId + "（" + name + "）";
        return switch (v) {
            case OK -> null;
            case REMOVED_282 -> "道具 " + label + " 已從 282 客戶端移除，無法發放（避免客戶端斷線）。";
            case CLIENT_ONLY_282 -> "道具 " + label + " 是 282 新道具，伺服器尚無資料，暫時無法發放。";
            case UNKNOWN_ID -> "道具 ID " + itemId + " 不存在於伺服器與 282 客戶端資料，無法發放。";
        };
    }

    /** Throws {@link IllegalArgumentException} with {@link #rejectMessage} (AdminEquipment / script style). */
    public static void requireGrantable(int itemId) {
        String msg = rejectMessage(itemId);
        if (msg != null) {
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Grant-path hook (Char.addItemToInventory): true when the item may be granted; otherwise logs once per id,
     * tells the character why and returns false. Never throws.
     */
    public static boolean allowGrant(Char chr, int itemId, String source) {
        String msg;
        try {
            msg = rejectMessage(itemId);
        } catch (RuntimeException e) {
            System.out.printf("[TMS282 items] availability check failed for %d (%s): %s%n", itemId, source, e);
            return true; // never block because of a checker fault
        }
        if (msg == null) {
            return true;
        }
        if (LOGGED.add(itemId)) {
            System.out.printf("[TMS282 items] blocked grant of %d via %s: %s%n", itemId, source, check(itemId));
        }
        if (chr != null) {
            try {
                chr.chatMessage(msg);
            } catch (RuntimeException ignored) {
                // offline/dummy character: logging is enough
            }
        }
        return false;
    }

    /** Rows whose item id is grantable; logs each left-out id once per source. */
    public static <T> List<T> filter(List<T> rows, ToIntFunction<T> itemId, String source) {
        List<T> out = new ArrayList<>(rows.size());
        List<Integer> dropped = new ArrayList<>();
        for (T row : rows) {
            int id = itemId.applyAsInt(row);
            if (isGrantable(id)) {
                out.add(row);
            } else {
                dropped.add(id);
            }
        }
        if (!dropped.isEmpty()) {
            System.out.printf("[TMS282 items] %s: %d of %d item(s) left out (removed in 282 / no data): %s%n",
                    source, dropped.size(), rows.size(), dropped);
        }
        return out;
    }

    /** 282 client name, else the server (265) string, else null. */
    public static String displayName(int itemId) {
        String name = is282() ? names282().get(itemId) : null;
        return name != null ? name : StringData.getItemStringById(itemId);
    }

    private static boolean hasTemplate(int itemId) {
        return TEMPLATE.computeIfAbsent(itemId, id -> {
            try {
                if (ItemConstants.isEquip(id)) {
                    return ItemData.getEquipById(id) != null;
                }
                if (ItemConstants.isPet(id)) {
                    return ItemData.getPetInfoByID(id) != null;
                }
                return ItemData.getItemInfoByID(id) != null;
            } catch (RuntimeException e) {
                return false;
            }
        });
    }

    private static net.swordie.ms.connection.packet.Tms282ItemAvailability removedList() {
        var r = removed;
        if (r == null) {
            synchronized (Tms282ItemAvailability.class) {
                if (removed == null) {
                    removed = net.swordie.ms.connection.packet.Tms282ItemAvailability.load();
                }
                r = removed;
            }
        }
        return r;
    }

    /** Lazily scans the 282 String.wz item files (only needed for names/rejection reasons). */
    private static Map<Integer, String> names282() {
        var n = names282;
        if (n == null) {
            synchronized (Tms282ItemAvailability.class) {
                if (names282 == null) {
                    names282 = loadNames282();
                }
                n = names282;
            }
        }
        return n;
    }

    private static Map<Integer, String> loadNames282() {
        Map<Integer, String> names = new HashMap<>();
        Pattern nameValue = Pattern.compile("<string name=\"name\" value=\"([^\"]*)\"");
        for (String f : STRING_FILES) {
            Path file = STRING_DIR.resolve(f + ".img.xml");
            if (!Files.exists(file)) {
                continue;
            }
            try (BufferedReader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                Integer current = null;
                String line;
                while ((line = in.readLine()) != null) {
                    Matcher m = ITEM_DIR.matcher(line);
                    if (m.find()) {
                        current = Integer.parseInt(m.group(1));
                        names.putIfAbsent(current, "");
                        continue;
                    }
                    if (current != null) {
                        Matcher v = nameValue.matcher(line);
                        if (v.find() && names.get(current).isEmpty()) {
                            names.put(current, v.group(1).replace("&amp;", "&"));
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("[TMS282 items] cannot read " + file + ": " + e);
            }
        }
        return names;
    }
}
