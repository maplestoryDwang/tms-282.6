package net.swordie.ms.world.outfit;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.avatar.AvatarLook;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.Inventory;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282UserPoolPackets;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.connection.protocol.codec.ProfileEncoders;
import net.swordie.ms.connection.protocol.codec.UserPoolPacketEncoder;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.handlers.item.InventoryHandler;
import net.swordie.ms.loaders.CashShopData;
import net.swordie.ms.loaders.Etc.Commodity.CommodityInfo;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.util.Position;
import net.swordie.ms.world.field.Field;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 時裝搭配共享（{@code @outfit}／{@code @搭配}，腳本 data/scripts/content/outfit_share.py）：把目前的外觀存成一套搭配、看別人分享的、
 * 用「模特兒」預覽、列出缺哪幾件、一鍵購買缺的、一鍵穿上。
 * <ul>
 *     <li>{@link #capture}：只讀角色的 AvatarLook（看得到的那一層），不動任何道具。</li>
 *     <li>{@link #preview}：在自己旁邊放一個只有自己看得到的模特兒（LP03D0 假的遠端角色，id {@link #modelId}），
 *     {@link #PREVIEW_SECONDS} 秒後或下一次預覽時收掉；不改自己的外觀與背包。</li>
 *     <li>{@link #check}：這套裡的時裝（現金裝備）哪些已經有、哪些沒有、沒有的各要多少楓葉點數；一般裝備與髮型臉型只列出、不賣。</li>
 *     <li>{@link #buyMissing}：一次買齊缺的時裝（楓葉點數；商城有上架用商城價，否則 {@link #fallbackPrice()}），放進時裝欄。</li>
 *     <li>{@link #wear}：把已經有的那幾件穿上，走的是玩家自己拖曳裝備的同一個處理器（{@link InventoryHandler}），規則完全相同。</li>
 * </ul>
 */
public final class OutfitShare {
    public static final int PREVIEW_SECONDS = 60;
    public static final int NAME_MAX = 12;
    private static final int MODEL_BASE = 0x7F000000;
    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "outfit-preview");
        t.setDaemon(true);
        return t;
    });
    /** What a viewer shows now: a later preview or close invalidates the timer. */
    private record Shown(long generation, Field field, boolean toField) {
    }

    /** viewer character id -> the model shown now. */
    private static final Map<Integer, Shown> SHOWN = new ConcurrentHashMap<>();
    private static volatile Map<Integer, Integer> prices;

    private OutfitShare() {
    }

    /** Price of a cash item nobody put on sale: {@code -Doutfit.fallbackPrice} (default 1000; 0 = not sold). */
    public static int fallbackPrice() {
        return Math.max(0, Integer.getInteger("outfit.fallbackPrice", 1000));
    }

    public static int modelId(Char viewer) {
        return MODEL_BASE | (viewer.getId() & 0xFFFFFF);
    }

    // ------------------------------------------------------------------ capture / store

    /** The character's visible look as an outfit named {@code name} (not saved yet). */
    public static Outfit capture(Char chr, String name) {
        AvatarLook look = chr.getAvatarData().getAvatarLook();
        List<Integer> items = new ArrayList<>();
        // an outfit is the fashion only: hairEquips holds the normal equips too, and those are left out
        for (Integer id : look.getHairEquips()) {
            if (id != null && id > 0 && !items.contains(id) && isCash(id)) {
                items.add(id);
            }
        }
        // the real weapon is not part of the outfit (not bought, not worn): it is kept only as the weapon TYPE a fashion
        // weapon cover is drawn with (preview model, launcher image); without a cover it is not stored
        int weapon = items.stream().anyMatch(OutfitShare::isWeaponCover) ? realWeapon(look) : 0;
        return new Outfit(0, chr.getUser().getId(), chr.getId(), chr.getName(), cleanName(name), look.getGender(),
                look.getSkin(), look.getFace(), look.getHair(), look.getMixedHairColor(), look.getMixHairPercent(),
                look.getWeaponStickerId(), weapon, look.isDrawElfEar(), items, 0, System.currentTimeMillis());
    }

    /** A fashion weapon cover (170xxxx). */
    public static boolean isWeaponCover(Integer id) {
        return id != null && id / 10000 == 170;
    }

    /** A real weapon id (121xxxx..159xxxx, not a secondary 134/135), or 0. */
    public static boolean isRealWeapon(Integer id) {
        if (id == null) {
            return false;
        }
        int p = id / 10000;
        return p >= 121 && p <= 159 && p != 134 && p != 135;
    }

    static int realWeapon(AvatarLook look) {
        if (isRealWeapon(look.getWeaponId())) {
            return look.getWeaponId();
        }
        for (Integer id : look.getHairEquips()) {
            if (isRealWeapon(id)) {
                return id;
            }
        }
        return 0;
    }

    /** What the launcher draws for an outfit: its fashion, plus the weapon type under a cover. */
    public static List<Integer> renderItems(Outfit o) {
        List<Integer> out = new ArrayList<>(fashion(o.items()));
        if (isRealWeapon(o.weapon()) && out.stream().anyMatch(OutfitShare::isWeaponCover)) {
            out.add(o.weapon());
        }
        return out;
    }

    /** Trimmed, control characters and script markup ({@code #}) removed, at most {@link #NAME_MAX} characters. */
    public static String cleanName(String name) {
        String s = name == null ? "" : name.replaceAll("[\\p{Cntrl}#\\\\]", "").strip();
        return s.codePointCount(0, s.length()) > NAME_MAX ? s.substring(0, s.offsetByCodePoints(0, NAME_MAX)) : s;
    }

    /** Saves the character's current look; null (and a chat line) when refused. */
    public static Outfit share(Char chr, String name) {
        Outfit o = capture(chr, name);
        if (o.name().isEmpty()) {
            chr.chatMessage("搭配名稱不能是空的。");
            return null;
        }
        if (o.items().isEmpty()) {
            chr.chatMessage("身上沒有穿時裝，沒有東西可以分享。");
            return null;
        }
        if (!dressed(o.items())) {
            chr.chatMessage("分享的搭配要穿時裝衣服：時裝套服，或時裝上衣加時裝褲裙。");
            return null;
        }
        try {
            if (OutfitStore.standard().countOf(o.userId()) >= OutfitStore.MAX_PER_USER) {
                chr.chatMessage("每個帳號最多分享 " + OutfitStore.MAX_PER_USER + " 套，請先刪掉舊的。");
                return null;
            }
            return OutfitStore.standard().save(o);
        } catch (SQLException e) {
            chr.chatMessage("搭配沒有存成功，請稍後再試。");
            return null;
        }
    }

    // ------------------------------------------------------------------ preview

    /** The outfit as a look: the viewer's own look is the base so every list has the size the encoder expects. */
    static AvatarLook lookOf(Outfit o, AvatarLook base) {
        AvatarLook look = base.deepCopy();
        look.setGender(o.gender());
        look.setSkin(o.skin());
        look.setFace(o.face());
        look.setHair(o.hair());
        look.setMixedHairColor(o.mixColor());
        look.setMixHairPercent(o.mixPercent());
        look.setWeaponStickerId(o.weaponSticker());
        // the cover is drawn over a weapon of its type: the sharer's (only when the outfit has a cover)
        look.setWeaponId(isRealWeapon(o.weapon()) && o.weaponSticker() > 0 ? o.weapon() : 0);
        look.setSubWeaponId(0);
        look.setHairEquips(fashion(o.items())); // older rows may still list normal equips
        look.setUnseenEquips(new ArrayList<>());
        List<Integer> pets = new ArrayList<>();
        for (int i = 0; i < base.getPetIDs().size(); i++) {
            pets.add(0);
        }
        look.setPetIDs(pets);
        look.setJob(0);
        look.setDrawElfEar(o.elfEar());
        return look;
    }

    /** Shows the model next to {@code viewer}, to them only. False when this protocol has no model packet. */
    public static boolean preview(Char viewer, Outfit o) {
        return preview(viewer, o, false);
    }

    /**
     * Shows the model next to {@code viewer}: to them only, or ({@code toField}) to everybody who is in the map at
     * that moment - somebody who walks in later does not get it. One model per viewer; it is removed after
     * {@link #PREVIEW_SECONDS}, on the next preview, and from the others as soon as the viewer has left the map.
     */
    public static boolean preview(Char viewer, Outfit o, boolean toField) {
        var encoder = ProfileEncoders.find(UserPoolPacketEncoder.class);
        if (encoder.isEmpty() || !(encoder.get() instanceof Tms282UserPoolPackets packets)) {
            return false;
        }
        closePreview(viewer);
        Position at = viewer.getPosition() == null ? new Position(0, 0) : viewer.getPosition();
        Position pos = new Position(at.getX() + 70, at.getY());
        AvatarLook look = lookOf(o, viewer.getAvatarData().getAvatarLook());
        Tms282UserPoolPackets.RemoteUser model = Tms282UserPoolPackets.RemoteUser.basic(0, modelId(viewer), 1,
                "搭配" + o.id(), o.gender(), 0, look, null, pos, 4, viewer.getFoothold());
        OutPacket packet = packets.userEnterField(model);
        Field field = viewer.getField();
        boolean shared = toField && field != null;
        if (shared) {
            field.broadcast(packet);
        } else {
            viewer.write(packet);
        }
        Shown shown = new Shown(System.nanoTime(), field, shared);
        SHOWN.put(viewer.getId(), shown);
        TIMER.schedule(() -> {
            try {
                if (SHOWN.remove(viewer.getId(), shown)) {
                    remove(viewer, shown);
                }
            } catch (RuntimeException e) {
                // the viewer left meanwhile
            }
        }, PREVIEW_SECONDS, TimeUnit.SECONDS);
        return true;
    }

    /** Removes the model now (a new preview, or the player closed it). */
    public static void closePreview(Char viewer) {
        Shown shown = SHOWN.remove(viewer.getId());
        if (shown != null) {
            remove(viewer, shown);
        }
    }

    /** Takes the model away from whoever got it: the map it was shown in, or the viewer while still in that map. */
    private static void remove(Char viewer, Shown shown) {
        OutPacket leave = Tms282UserPoolPackets.userLeaveField(modelId(viewer));
        if (shown.toField() && shown.field() != null) {
            shown.field().broadcast(leave);
        } else if (viewer.getField() == shown.field() && viewer.getClient() != null) {
            viewer.write(leave);
        }
    }

    // ------------------------------------------------------------------ what is missing

    /**
     * @param owned     cash pieces the character has (worn or in the bag)
     * @param missing   cash pieces it lacks and that are sold (price > 0)
     * @param unsold    cash pieces it lacks and that cannot be bought here (price 0)
     * @param plain     non-cash equips of the look (never sold; shown for information)
     * @param wrongSex  pieces the character's gender cannot wear
     * @param total     sum of the {@code missing} prices
     */
    public record Check(List<Integer> owned, List<Integer> missing, List<Integer> unsold, List<Integer> plain,
                        List<Integer> wrongSex, Map<Integer, Integer> price, int total) {
    }

    /** Every piece of the outfit a character could wear: the visible equips plus the cash weapon cover. */
    static List<Integer> pieces(Outfit o) {
        List<Integer> out = new ArrayList<>(o.items());
        if (o.weaponSticker() > 0 && !out.contains(o.weaponSticker())) {
            out.add(o.weaponSticker());
        }
        return out;
    }

    /** The cash equips of a list (outfits shared before 2026-10-07 22:40 also stored normal equips). */
    public static List<Integer> fashion(List<Integer> items) {
        List<Integer> out = new ArrayList<>();
        for (Integer id : items) {
            if (id != null && isCash(id)) {
                out.add(id);
            }
        }
        return out;
    }

    /** Not undressed: a fashion overall, or a fashion top and fashion bottoms (an outfit is the fashion only). */
    public static boolean dressed(List<Integer> items) {
        boolean top = false, bottom = false;
        for (int id : fashion(items)) {
            switch (id / 10000) {
                case 105 -> { return true; }
                case 104 -> top = true;
                case 106 -> bottom = true;
                default -> { }
            }
        }
        return top && bottom;
    }

    public static boolean isCash(int itemId) {
        if (!ItemConstants.isEquip(itemId)) {
            return false;
        }
        Equip e = ItemData.getEquipById(itemId);
        return e != null && e.isCash();
    }

    static Item find(Char chr, int itemId) {
        for (Inventory inv : List.of(chr.getEquippedInventory(), chr.getDecorationInventory(), chr.getEquipInventory())) {
            for (Item i : new ArrayList<>(inv.getItems())) {
                if (i.getItemId() == itemId) {
                    return i;
                }
            }
        }
        return null;
    }

    /** Maple-point price: the cheapest commodity on sale for the item, else {@link #fallbackPrice()}. */
    public static int price(int itemId) {
        Map<Integer, Integer> p = prices;
        if (p == null) {
            p = new HashMap<>();
            try {
                for (CommodityInfo c : CashShopData.getCommodities()) {
                    if (c.getOnSale() != 0 && c.getPrice() > 0) {
                        p.merge(c.getItemId(), c.getPrice(), Math::min);
                    }
                }
            } catch (RuntimeException e) {
                // no commodity data: everything at the fallback price
            }
            prices = p;
        }
        return p.getOrDefault(itemId, fallbackPrice());
    }

    public static Check check(Char chr, Outfit o) {
        List<Integer> owned = new ArrayList<>(), missing = new ArrayList<>(), unsold = new ArrayList<>(), plain = new ArrayList<>(),
                wrong = new ArrayList<>();
        Map<Integer, Integer> price = new HashMap<>();
        int total = 0;
        int gender = chr.getAvatarData().getAvatarLook().getGender();
        for (int id : pieces(o)) {
            if (!isCash(id)) {
                continue; // not fashion: never part of an outfit (older rows)
            } else if (ItemConstants.getBodyPartArrayFromItem(id, gender).isEmpty()) {
                wrong.add(id);
            } else if (find(chr, id) != null) {
                owned.add(id);
            } else {
                int p = price(id);
                if (p > 0) {
                    missing.add(id);
                    price.put(id, p);
                    total += p;
                } else {
                    unsold.add(id);
                }
            }
        }
        return new Check(owned, missing, unsold, plain, wrong, price, total);
    }

    // ------------------------------------------------------------------ buy / wear

    /** Buys every missing piece with maple points. Returns a line for the player; nothing is charged on a refusal. */
    public static String buyMissing(Char chr, Outfit o) {
        synchronized (chr) {
            Check c = check(chr, o);
            if (c.missing().isEmpty()) {
                return "沒有需要購買的時裝。";
            }
            if (chr.getDecorationInventory().getEmptySlots() < c.missing().size()) {
                return "時裝欄空位不夠，需要 " + c.missing().size() + " 格。";
            }
            var user = chr.getUser();
            if (user.getMaplePoints() < c.total()) {
                return "楓葉點數不足：需要 " + c.total() + "，目前有 " + user.getMaplePoints() + "。";
            }
            List<Equip> made = new ArrayList<>();
            for (int id : c.missing()) {
                Equip equip = ItemData.getEquipDeepCopyFromID(id, false);
                if (equip == null || !net.swordie.ms.world.tms282.items.Tms282ItemAvailability.allowGrant(chr, id, "OutfitShare.buyMissing")) {
                    return "道具 " + id + " 目前不能取得，這次沒有購買。";
                }
                made.add(equip);
            }
            user.deductMaplePoints(c.total());
            chr.write(WvsContext.setMaplePoint(user.getMaplePoints()));
            for (Equip equip : made) {
                chr.addItemToInventory(InvType.DECORATION, equip, false, false);
                chr.write(WvsContext.inventoryOperation(true, false, net.swordie.ms.enums.InventoryOperation.Add,
                        (short) equip.getBagIndex(), (byte) -1, 0, equip));
            }
            System.out.println("[Outfit] character=" + chr.getId() + " bought outfit " + o.id() + " pieces=" + c.missing()
                    + " for " + c.total() + " maple points");
            return "已購買 " + made.size() + " 件，花費 " + c.total() + " 楓葉點數。";
        }
    }

    /** Puts on the pieces the character owns. Returns how many were put on. */
    public static int wear(Char chr, Outfit o) {
        int worn = 0;
        int gender = chr.getAvatarData().getAvatarLook().getGender();
        for (int id : pieces(o)) {
            if (!isCash(id)) {
                continue;
            }
            Item item = find(chr, id);
            List<Integer> slots = ItemConstants.getBodyPartArrayFromItem(id, gender);
            if (item == null || slots.isEmpty() || item.getBagIndex() < 0 || item.getInvType() == InvType.EQUIPPED) {
                continue; // not owned, wrong gender, or already worn
            }
            int before = item.getBagIndex();
            ByteBuffer b = ByteBuffer.allocate(4 + 1 + 2 + 2 + 2 + 4 + 4).order(ByteOrder.LITTLE_ENDIAN);
            b.putInt(0).put(item.getInvType().getVal()).putShort((short) before).putShort((short) -slots.get(0)).putShort((short) -1)
                    .putInt(id).putInt(0);
            try {
                InventoryHandler.handleUserChangeSlotPositionRequest(chr, new InPacket(b.array()));
            } catch (RuntimeException e) {
                System.out.println("[Outfit] character=" + chr.getId() + " wear " + id + " failed: " + e);
                continue;
            }
            if (item.getInvType() == InvType.EQUIPPED || item.getBagIndex() != before) {
                worn++;
            }
        }
        return worn;
    }
}
