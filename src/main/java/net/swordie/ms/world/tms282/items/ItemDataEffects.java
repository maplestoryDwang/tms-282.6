package net.swordie.ms.world.tms282.items;

import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.TimedSkills;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.scripts.ScriptManagerImpl;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generic effect of a TMS 282 script item whose script file does not exist, taken from the 282 client's own item data
 * ({@code data/wz282/Item.wz/{Consume,Cash}/NNNN.img.xml} info/spec, {@code String.wz} name/desc). Only effects the
 * data defines unambiguously are applied; everything else returns {@code false} and keeps the caller's
 * "not implemented, not consumed" behaviour:
 * <ul>
 *     <li>{@code info/damageSkinID} - registers the damage skin with that id (the 265 skin table does not know the
 *     282-only coupons);</li>
 *     <li>{@code info/ridingSkillID} - grants the ride skill, timed ({@link TimedSkills}) when the description names a
 *     day count (N天 / N일), permanent when it says 永久/영구 or names no period;</li>
 *     <li>{@code info/incHonorExp} - honor EXP;</li>
 *     <li>storage ({@code info/slotExpand}=7, 倉庫欄位N格擴充券) and fixed-type 裝備/消耗/裝飾/其他欄 N格擴充券 coupons;</li>
 *     <li>growth potions whose name or description gives the level range and a one-level growth
 *     (成長1級/提升一等級, no probability table): +1 level inside the range, refused (not consumed) outside it.</li>
 * </ul>
 * Hooked from {@code ScriptManagerImpl.start} (script item whose {@code spec/script} file is missing) and meant for
 * {@code ItemHandler.handleUserScriptItemUseRequest} (item without any script). 282 profile only; the item has
 * already been validated to be in the player's inventory by the caller.
 */
public final class ItemDataEffects {
    private static final Logger LOG = Logger.getLogger(ItemDataEffects.class.getName());

    /** info/spec values of one 282 item (flat: only the direct children of info and spec). */
    public record WzItem(int id, Map<String, String> info, Map<String, String> spec) {
        public int infoInt(String key, int def) {
            return parse(info.get(key), def);
        }
    }

    private static final Map<String, Map<Integer, WzItem>> ITEM_FILES = new ConcurrentHashMap<>();
    private static final Map<String, Map<Integer, String[]>> STRING_FILES = new ConcurrentHashMap<>();

    private static final Pattern RANGE = Pattern.compile("(\\d{2,3})\\s*[~～]\\s*(\\d{2,3})");
    private static final Pattern ONE_LEVEL = Pattern.compile("成長\\s*(1|一)\\s*(個)?\\s*(級|等級)|提升\\s*(1|一)\\s*(個)?\\s*(級|等級)");
    private static final Pattern DAYS = Pattern.compile("(\\d+)\\s*(天|일)");
    private static final Pattern FIXED_SLOT_COUPON = Pattern.compile("^(裝備|消耗|裝飾|其他)欄\\s*(\\d+)\\s*格擴充券");
    private static final Pattern STORAGE_COUPON = Pattern.compile("倉庫欄位\\s*(\\d+)\\s*格擴充券");

    private ItemDataEffects() {
    }

    public static boolean active() {
        return Protocol.active().version() == ProtocolVersion.TMS_282_3;
    }

    static Path wzRoot() {
        return Path.of(ServerConstants.DIR, "data", "wz282");
    }

    /**
     * Applies the data-defined effect of {@code itemId}.
     *
     * @return true when the item was handled (effect applied, or refused with a message); false when its data defines
     * no generic effect (the caller keeps its own fallback)
     */
    public static boolean tryApply(Char chr, int itemId) {
        if (chr == null || !active()) {
            return false;
        }
        if (item(itemId) == null && legacyPlan(itemId) == null) {
            return false;
        }
        try {
            return apply(chr, itemId);
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Item data effect of " + itemId + " failed", e);
            chr.chatMessage("道具使用失敗，請聯繫管理員。");
            return true;
        }
    }

    /** The 282 info/damageSkinID of an item, or -1. */
    public static int damageSkinId(int itemId) {
        if (!active()) {
            return -1;
        }
        WzItem item = item(itemId);
        return item == null ? -1 : item.infoInt("damageSkinID", -1);
    }

    /** What the 282 data says the item does; {@code a}/{@code b} are kind-specific (see {@link #plan(int)}). */
    public enum Kind { DAMAGE_SKIN, RIDING_SKILL, HONOR_EXP, STORAGE_SLOTS, INVENTORY_SLOTS, GROWTH_LEVEL }

    /**
     * DAMAGE_SKIN a=skin id; RIDING_SKILL a=skill id, b=days (0 = permanent); HONOR_EXP a=honor; STORAGE_SLOTS a=slots;
     * INVENTORY_SLOTS a=inventory type (1..4), b=slots; GROWTH_LEVEL a=min level, b=max level.
     */
    public record Plan(int itemId, Kind kind, int a, int b, String label) {
    }

    /**
     * Items the server still hands out whose node is gone from the 282 Item.wz (only the name is left in String.wz):
     * 2430480 菇菇寶貝騎寵30天使用券 of the newbie gift, GMS 265 info/ridingSkillID 80001013 (菇菇寶貝, in the 282 client).
     */
    static Plan legacyPlan(int itemId) {
        // 2430480 was listed here on 10-06 by mistake: the 282 client has the item and data/scripts/item/consume_2430480.js
        // already grants 80001013 for 90 days (live use 10-07 00:4x). No legacy items at the moment.
        return null;
    }

    /** The data-defined effect of a 282 item, or null. Pure (no Char); used by tools to count coverage. */
    public static Plan plan(int itemId) {
        WzItem item = item(itemId);
        if (item == null) {
            return legacyPlan(itemId);
        }
        String name = string(itemId, 0);
        String desc = string(itemId, 1);
        int damageSkinId = item.infoInt("damageSkinID", -1);
        if (damageSkinId >= 0) {
            return new Plan(itemId, Kind.DAMAGE_SKIN, damageSkinId, 0, name);
        }
        int ridingSkillId = item.infoInt("ridingSkillID", 0);
        if (ridingSkillId > 0) {
            String text = name + " " + desc;
            int days = 0;
            if (!text.contains("永久") && !text.contains("영구")) {
                Matcher m = DAYS.matcher(text);
                if (m.find()) {
                    days = Integer.parseInt(m.group(1));
                }
            }
            return new Plan(itemId, Kind.RIDING_SKILL, ridingSkillId, days, name);
        }
        int honor = item.infoInt("incHonorExp", 0);
        if (honor > 0) {
            return new Plan(itemId, Kind.HONOR_EXP, honor, 0, name);
        }
        Matcher storage = STORAGE_COUPON.matcher(name);
        if (item.infoInt("slotExpand", 0) == 7 && storage.find()) {
            return new Plan(itemId, Kind.STORAGE_SLOTS, Integer.parseInt(storage.group(1)), 0, "倉庫");
        }
        Matcher fixed = FIXED_SLOT_COUPON.matcher(name);
        if (fixed.find()) {
            int type = switch (fixed.group(1)) {
                case "裝備" -> 1;
                case "消耗" -> 2;
                case "裝飾" -> 3;
                default -> 4;
            };
            return new Plan(itemId, Kind.INVENTORY_SLOTS, type, Integer.parseInt(fixed.group(2)), fixed.group(1));
        }
        if ((name.contains("成長") || desc.contains("成長")) && ONE_LEVEL.matcher(desc).find() && !desc.contains("機率")) {
            Matcher range = RANGE.matcher(name);
            if (!range.find()) {
                range = RANGE.matcher(desc);
                if (!range.find()) {
                    return null;
                }
            }
            int lo = Integer.parseInt(range.group(1)), hi = Integer.parseInt(range.group(2));
            if (lo > hi || hi > GameConstants.MAX_LEVEL) {
                return null;
            }
            return new Plan(itemId, Kind.GROWTH_LEVEL, lo, hi, name);
        }
        return null;
    }

    private static boolean apply(Char chr, int itemId) {
        if (!inInventory(chr, itemId)) {
            return false;
        }
        Plan plan = plan(itemId);
        if (plan == null) {
            return false;
        }
        ScriptManagerImpl sm = chr.getScriptManager();
        switch (plan.kind()) {
            case DAMAGE_SKIN -> sm.addDamageSkin(itemId, plan.a()); // consumes on success, sends the result either way
            case RIDING_SKILL -> grantRidingSkill(chr, sm, itemId, plan.a(), plan.b(), true);
            case HONOR_EXP -> {
                chr.addHonorExp(plan.a());
                chr.consumeItem(itemId, 1);
                chr.chatMessage(String.format("獲得了 %,d 名聲值。", plan.a()));
            }
            case STORAGE_SLOTS -> expand(chr, sm, itemId, 0, plan.a(), plan.label());
            case INVENTORY_SLOTS -> expand(chr, sm, itemId, plan.a(), plan.b(), plan.label());
            case GROWTH_LEVEL -> {
                int level = chr.getLevel();
                if (level < plan.a() || level > plan.b() || level >= GameConstants.MAX_LEVEL) {
                    chr.chatMessage(String.format("此道具只能在 %d~%d 級使用，本次沒有扣除道具。", plan.a(), plan.b()));
                } else {
                    chr.consumeItem(itemId, 1);
                    sm.addLevel(1);
                }
            }
        }
        return true;
    }

    private static boolean inInventory(Char chr, int itemId) {
        var inv = itemId / 1000000 == 5 ? chr.getCashInventory() : chr.getConsumeInventory();
        return inv != null && inv.getItems().stream().anyMatch(i -> i != null && i.getItemId() == itemId && i.getQuantity() > 0);
    }

    private static void expand(Char chr, ScriptManagerImpl sm, int itemId, int type, int amount, String label) {
        if (sm.useSelectableInventoryExpansionCoupon(itemId, type, amount)) {
            chr.chatMessage(String.format("%s欄位增加了 %d 格。", label, amount));
        } else {
            chr.chatMessage(String.format("%s欄位無法再擴充 %d 格，本次沒有扣除道具。", label, amount));
        }
    }

    /** True for an item that is given as its effect instead of an inventory item (not in the 282 client's Item.wz). */
    public static boolean isLegacyEffectItem(int itemId) {
        return active() && legacyPlan(itemId) != null;
    }

    /** Gives the effect of a {@link #isLegacyEffectItem} item directly (the newbie gift); nothing is consumed. */
    public static void grantLegacyEffect(Char chr, int itemId) {
        Plan plan = legacyPlan(itemId);
        if (chr != null && plan != null && plan.kind() == Kind.RIDING_SKILL) {
            grantRidingSkill(chr, chr.getScriptManager(), itemId, plan.a(), plan.b(), false);
        }
    }

    private static void grantRidingSkill(Char chr, ScriptManagerImpl sm, int itemId, int skillId, int days, boolean consume) {
        long expire = days > 0 ? System.currentTimeMillis() + days * 24L * 60 * 60 * 1000 : 0;
        if (expire == 0) {
            if (chr.hasSkill(skillId)) {
                if (consume) chr.chatMessage("已經擁有這個騎寵技能了，本次沒有扣除道具。");
                return;
            }
            sm.changeSkillLevel(skillId, 1);
        } else {
            if (!TimedSkills.recordGrant(chr, skillId, expire)) {
                if (consume) chr.chatMessage("已經永久擁有這個騎寵技能了，本次沒有扣除道具。");
                return;
            }
            sm.giveSkill(skillId, 1, 1);
        }
        if (consume) {
            chr.consumeItem(itemId, 1);
        }
        chr.chatMessage(expire == 0 ? "已獲得騎寵技能（永久）。" : "已獲得騎寵技能，可使用 " + days + " 天。");
    }

    // ---- 282 WZ data (lazy, one img file at a time) ----

    public static WzItem item(int itemId) {
        String dir = switch (itemId / 1000000) {
            case 2 -> "Consume";
            case 5 -> "Cash";
            default -> null;
        };
        if (dir == null) {
            return null;
        }
        String file = dir + "/" + String.format("%04d", itemId / 10000) + ".img.xml";
        return ITEM_FILES.computeIfAbsent(file, f -> loadItems(wzRoot().resolve("Item.wz").resolve(f))).get(itemId);
    }

    /** index 0 = name, 1 = desc; "" when unknown. */
    static String string(int itemId, int index) {
        String file = switch (itemId / 1000000) {
            case 2 -> "Consume.img.xml";
            case 5 -> "Cash.img.xml";
            default -> null;
        };
        if (file == null) {
            return "";
        }
        String[] v = STRING_FILES.computeIfAbsent(file, f -> loadStrings(wzRoot().resolve("String.wz").resolve(f))).get(itemId);
        return v == null || v[index] == null ? "" : v[index];
    }

    private static int parse(String v, int def) {
        if (v == null || v.isBlank()) {
            return def;
        }
        try {
            return (int) Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static final XMLInputFactory XML = XMLInputFactory.newFactory();

    static Map<Integer, WzItem> loadItems(Path file) {
        if (!Files.isRegularFile(file)) {
            return Collections.emptyMap();
        }
        Map<Integer, WzItem> out = new HashMap<>();
        try (InputStream in = Files.newInputStream(file)) {
            XMLStreamReader r = XML.createXMLStreamReader(in, "UTF-8");
            int depth = 0;
            int currentId = -1;
            String section = null;
            Map<String, String> info = null, spec = null;
            while (r.hasNext()) {
                int ev = r.next();
                if (ev == XMLStreamConstants.START_ELEMENT) {
                    depth++;
                    String nameAttr = r.getAttributeValue(null, "name");
                    if (depth == 2 && "imgdir".equals(r.getLocalName())) {
                        currentId = parse(nameAttr, -1);
                        info = new HashMap<>();
                        spec = new HashMap<>();
                    } else if (depth == 3 && currentId >= 0) {
                        section = "imgdir".equals(r.getLocalName()) ? nameAttr : null;
                    } else if (depth == 4 && currentId >= 0 && section != null && nameAttr != null) {
                        String value = r.getAttributeValue(null, "value");
                        if (value != null) {
                            if ("info".equals(section)) info.put(nameAttr, value);
                            else if ("spec".equals(section)) spec.put(nameAttr, value);
                        }
                    }
                } else if (ev == XMLStreamConstants.END_ELEMENT) {
                    if (depth == 2 && currentId >= 0) {
                        out.put(currentId, new WzItem(currentId, Map.copyOf(info), Map.copyOf(spec)));
                        currentId = -1;
                    } else if (depth == 3) {
                        section = null;
                    }
                    depth--;
                }
            }
            r.close();
        } catch (IOException | XMLStreamException e) {
            LOG.log(Level.WARNING, "Cannot read " + file, e);
        }
        return out;
    }

    static Map<Integer, String[]> loadStrings(Path file) {
        if (!Files.isRegularFile(file)) {
            return Collections.emptyMap();
        }
        Map<Integer, String[]> out = new HashMap<>();
        try (InputStream in = Files.newInputStream(file)) {
            XMLStreamReader r = XML.createXMLStreamReader(in, "UTF-8");
            int depth = 0;
            String[] current = null;
            while (r.hasNext()) {
                int ev = r.next();
                if (ev == XMLStreamConstants.START_ELEMENT) {
                    depth++;
                    String nameAttr = r.getAttributeValue(null, "name");
                    if (depth == 2) {
                        int id = parse(nameAttr, -1);
                        current = id >= 0 ? out.computeIfAbsent(id, k -> new String[2]) : null;
                    } else if (depth == 3 && current != null && nameAttr != null) {
                        String value = r.getAttributeValue(null, "value");
                        if ("name".equals(nameAttr)) current[0] = value;
                        else if ("desc".equals(nameAttr)) current[1] = value;
                    }
                } else if (ev == XMLStreamConstants.END_ELEMENT) {
                    if (depth == 2) {
                        current = null;
                    }
                    depth--;
                }
            }
            r.close();
        } catch (IOException | XMLStreamException e) {
            LOG.log(Level.WARNING, "Cannot read " + file, e);
        }
        return out;
    }
}
