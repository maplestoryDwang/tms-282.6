package net.swordie.ms.world.tms282.items;

import net.swordie.ms.Server;
import net.swordie.ms.client.character.BroadcastMsg;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.Inventory;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.FieldPacket;
import net.swordie.ms.connection.packet.Tms282CashUseVariant;
import net.swordie.ms.connection.packet.Tms282CashUseVariant.Family;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.ChatType;
import net.swordie.ms.client.character.items.EquipAttribute;
import net.swordie.ms.enums.FieldOption;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.loaders.FieldData;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.FieldInstanceType;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * CP0150 cash item use for the families whose 282 layout was verified by running the client's sender under Unicorn
 * ({@link Tms282CashUseVariant}; research/tms2823/item-usability-20260928/cash-use/README.md). Handler registry
 * (command pattern): {@link #recognize} picks the family from the client cash-item type table
 * ({@link Tms282CashItemTypes}), {@link #use} validates the owned item and runs the family's handler. Where 265
 * {@code ItemHandler.handleUserConsumeCashItemUseRequest} already owns the item (pet food, meso bags, weather) the 282
 * body is rewritten to the 265 one and handed over; the other families are handled here.
 * <p>
 * Families that parse but have no server behaviour (charm debuff removal, pet enhancement box, seal unlock, 5042xxx
 * fixed-destination rocks) are refused without consuming anything and logged. A family can be switched off with
 * {@code -Dtms282.cashUse.<family>=false} (lower case, e.g. {@code teleport}); every family defaults ON because every
 * layout here is emulation-verified. Replies (inventory sync, unlock) are sent by the caller.
 */
public final class Tms282CashItemUse {
    /** Outcome of one CP0150 for logging and checks. */
    public enum Outcome { HANDLED, DELEGATED_265, REFUSED, DROPPED_NO_SERVER_LOGIC, DISABLED }

    private static final Set<Family> ALL = EnumSet.allOf(Family.class);

    private Tms282CashItemUse() {
    }

    public static boolean enabled(Family family) {
        return Boolean.parseBoolean(System.getProperty("tms282.cashUse." + family.name().toLowerCase(Locale.ROOT), "true"));
    }

    /**
     * The verified variant of this CP0150, or empty when the item's client type is not owned here (553 reward boxes,
     * pet skills, incubators and Luna Crystal keep their own paths in Tms282CashShopHandlers).
     *
     * @throws IllegalArgumentException for an owned type whose body does not parse
     */
    public static Optional<Tms282CashUseVariant> recognize(byte[] packet, Tms282CashItemTypes types, Charset charset) {
        int itemId = Tms282CashUseVariant.itemIdOf(packet);
        Tms282CashItemTypes.Entry entry = itemId < 0 ? null : types.entry(itemId);
        if (entry == null || Family.ofType(entry.type()).isEmpty() || !ALL.contains(Family.ofType(entry.type()).get())) {
            return Optional.empty();
        }
        return Optional.of(Tms282CashUseVariant.parse(packet, entry.type(), entry.teleportKind(), entry.choice(), charset));
    }

    /** Runs one recognised CP0150 for {@code chr}; never throws for a player error (refusal + chat line + log). */
    public static Outcome use(Char chr, Tms282CashUseVariant v, byte[] packet) {
        if (!enabled(v.family())) {
            log(chr, v, "family disabled by tms282.cashUse." + v.family().name().toLowerCase(Locale.ROOT));
            return Outcome.DISABLED;
        }
        Item item = chr.getCashInventory().getItemBySlot((short) v.slot());
        if (item == null || item.getItemId() != v.itemId() || item.getQuantity() < 1) {
            return refuse(chr, v, "cash slot does not hold the item", null);
        }
        if (item.getDateExpire() != null && item.getDateExpire().isExpired()) {
            return refuse(chr, v, "expired", "道具已過期。");
        }
        return switch (v.family()) {
            case PREFIX_ONLY -> prefixOnly(chr, v, item, packet);
            case TEXT -> text(chr, v, item, packet);
            case WHISPER_TEXT -> whisper(chr, v, item, (Tms282CashUseVariant.WhisperText) v.body());
            case TELEPORT -> teleport(chr, v, item, (Tms282CashUseVariant.Teleport) v.body());
            case BEAUTY -> beauty(chr, v, item, (Tms282CashUseVariant.Beauty) v.body());
            case SCISSORS -> scissors(chr, v, item, (Tms282CashUseVariant.InventoryTarget) v.body());
            case ANVIL -> anvil(chr, v, item, (Tms282CashUseVariant.Anvil) v.body());
            case SEAL_UNLOCK -> dropped(chr, v, "seal unlock needs the PIC check and the one-day delayed unseal");
            case PET_BOX -> dropped(chr, v, "pet enhancement buffs have no server data");
        };
    }

    // ---------------------------------------------------------------- families

    private static Outcome prefixOnly(Char chr, Tms282CashUseVariant v, Item item, byte[] packet) {
        int id = v.itemId();
        if (v.type() == 0x07) {
            return charm(chr, v, item);
        }
        if (ItemConstants.isPetFood(id) || ItemConstants.isMesoBagItem(id) || ItemConstants.isRandomMesoBagItem(id)) {
            return delegate(chr, packet, new byte[0]);
        }
        return dropped(chr, v, "no 265 branch for this item");
    }

    private static Outcome text(Char chr, Tms282CashUseVariant v, Item item, byte[] packet) {
        String text = ((Tms282CashUseVariant.Text) v.body()).text();
        if (v.type() == 0x10 && ItemConstants.isChangeMapEffectItem(v.itemId())) {
            return delegate(chr, packet, string265(text, chr));
        }
        if (v.type() == 0x0C && ItemConstants.isMegaphoneItem(v.itemId())) {
            return delegate(chr, packet, string265(text, chr));
        }
        if (v.type() == 0x0F) { // world megaphone (5078001): every channel of every world
            String line = String.format("%s : %s", chr.getName(), text);
            Server.get().broadcastForWorld(WvsContext.broadcastMsg(BroadcastMsg.megaphone(line, channel(chr), false, chr)));
            chr.consumeItem(item, 1);
            return Outcome.HANDLED;
        }
        return dropped(chr, v, "no handler for text type 0x" + Integer.toHexString(v.type()));
    }

    private static Outcome whisper(Char chr, Tms282CashUseVariant v, Item item, Tms282CashUseVariant.WhisperText w) {
        String line = String.format("%s : %s", chr.getName(), w.text());
        if (v.type() == 0x0E || v.type() == 0xAD) { // item megaphone
            Item shown = null;
            if (w.link().hasItem()) {
                InvType t = InvType.getInvTypeByVal(w.link().a());
                if (t == InvType.EQUIP && w.link().b() < 0) {
                    t = InvType.EQUIPPED;
                }
                Inventory inv = t == null ? null : chr.getInventoryByType(t);
                shown = inv == null ? null : inv.getItemBySlot((short) w.link().b());
                if (shown == null) {
                    return refuse(chr, v, "linked item not found", "找不到要顯示的道具。");
                }
            }
            BroadcastMsg msg = shown == null ? BroadcastMsg.itemMegaphoneNoItem(line, channel(chr))
                    : BroadcastMsg.itemMegaphone(line, channel(chr), w.whisper(), true, shown, chr);
            Server.get().broadcastForWorld(WvsContext.broadcastMsg(msg));
        } else {
            Server.get().broadcastForWorld(WvsContext.broadcastMsg(BroadcastMsg.megaphone(line, channel(chr), w.whisper(), chr)));
        }
        chr.consumeItem(item, 1);
        return Outcome.HANDLED;
    }

    private static Outcome teleport(Char chr, Tms282CashUseVariant v, Item item, Tms282CashUseVariant.Teleport t) {
        if (chr.getField() == null) {
            return refuse(chr, v, "no field", null);
        }
        if (t.fixedDestination()) { // 5042xxx: Item.wz info/map is the only destination
            Tms282CashItemTypes.Entry e = Tms282CashItemTypes.get().entry(v.itemId());
            int destination = e == null ? 0 : e.map();
            if (destination <= 0) {
                return refuse(chr, v, "no info/map for " + v.itemId(), "此瞬移之石沒有目的地資料，未消耗此道具。");
            }
            if ((chr.getField().getFieldLimit() & FieldOption.TeleportItemLimit.getVal()) != 0) {
                return refuse(chr, v, "current field forbids teleport items", "目前的地圖無法使用瞬移道具。");
            }
            Field field = chr.getOrCreateFieldByCurrentInstanceType(destination);
            if (field == null || !teleportable(chr, field)) {
                return refuse(chr, v, "destination blocked " + destination, "無法移動到該地圖。");
            }
            if (field != chr.getField()) {
                chr.warp(field);
                if (chr.getField() != field) {
                    return refuse(chr, v, "warp refused " + destination, null);
                }
            }
            consumeUnlessPeriod(chr, item);
            return Outcome.HANDLED;
        }
        if (t.byName()) {
            Char target = chr.getWorld() == null ? null : chr.getWorld().getCharByName(t.name());
            if (target == null || target.getField() == null) {
                return refuse(chr, v, "target offline", String.format("%s 目前不在線上。", t.name()));
            }
            Field field = target.getField();
            if (!teleportable(chr, field)) {
                return refuse(chr, v, "target field blocked", "無法移動到該地圖。");
            }
            if (target.getFieldInstanceType() != FieldInstanceType.CHANNEL) {
                return refuse(chr, v, "target in an instance", String.format("%s 目前在無法前往的地圖。", t.name()));
            }
            if (target.getClient().getChannel() != chr.getClient().getChannel()) {
                return refuse(chr, v, "target on another channel", "只能移動到相同頻道的角色所在地圖。");
            }
            if (field != chr.getField()) {
                chr.warp(field);
            }
            chr.write(FieldPacket.teleport(target.getPosition(), chr));
        } else {
            if (!FieldData.getWorldMapFields().contains(t.fieldId())) { // as 265 HYPER_TELEPORT_ROCK
                return refuse(chr, v, "not a world-map field " + t.fieldId(), "無法移動到該地圖。");
            }
            Field field = chr.getOrCreateFieldByCurrentInstanceType(t.fieldId());
            if (field == null || !teleportable(chr, field)) {
                return refuse(chr, v, "field blocked " + t.fieldId(), "無法移動到該地圖。");
            }
            chr.warp(field);
            if (chr.getField() != field) {
                return refuse(chr, v, "warp refused " + t.fieldId(), null); // e.g. a map the 282 client lacks
            }
        }
        consumeUnlessPeriod(chr, item);
        return Outcome.HANDLED;
    }

    /** Counted items are consumed; period items ([1天], 45-day charms ...) are reusable until they expire. */
    static void consumeUnlessPeriod(Char chr, Item item) {
        if (item.getDateExpire() == null || item.getDateExpire().isMaxTime()) {
            chr.consumeItem(item, 1);
        }
    }

    /** Death EXP/drop-rate debuff (Char.revive: quest 100716 + EXP_DROP_PENALTY); the client only sends while it runs. */
    public static final int DEATH_DEBUFF_QUEST = 100716;

    private static Outcome charm(Char chr, Tms282CashUseVariant v, Item item) {
        Tms282CashItemTypes.Entry e = Tms282CashItemTypes.get().entry(v.itemId());
        if (e == null || e.type() != 0x07) {
            return refuse(chr, v, "not a charm", null);
        }
        if (!chr.hasQuest(DEATH_DEBUFF_QUEST)) {
            return refuse(chr, v, "no death debuff running", "目前沒有死亡導致的經驗值、掉寶率減少效果。");
        }
        chr.removeQuest(DEATH_DEBUFF_QUEST); // Char.handleExpDropPenalty stops counting once the quest is gone
        chr.write(WvsContext.updateExpDropPenalty(false, 0, 0, 0, 0));
        consumeUnlessPeriod(chr, item);
        chr.chatMessage(ChatType.GameDesc, "已解除死亡導致的經驗值、掉寶率減少效果。");
        return Outcome.HANDLED;
    }

    private static boolean teleportable(Char chr, Field field) {
        return (field.getFieldLimit() & FieldOption.TeleportItemLimit.getVal()) == 0 && chr.getLevel() >= field.getLvLimit();
    }

    private static Outcome beauty(Char chr, Tms282CashUseVariant v, Item item, Tms282CashUseVariant.Beauty b) {
        if (b.target() != 0) {
            return refuse(chr, v, "beauty target " + b.target(), "此美容券目前只能用在角色本身。");
        }
        Optional<Tms282BeautyCoupons.Rule> rule = Tms282BeautyCoupons.get().rule(v.itemId());
        if (b.chosen()) { // choice coupon: the id picked from our LP023E list
            if (rule.isEmpty() || !rule.get().choice()) {
                return refuse(chr, v, "choice coupon without a pool", "此美容券目前沒有造型資料，未消耗此道具。");
            }
            List<Integer> pool = Tms282BeautyCoupons.pool(chr, rule.get());
            if (!pool.contains(b.style())) {
                return refuse(chr, v, "style " + b.style() + " not offered", "所選的造型不在此美容券的清單中。");
            }
            return applyStyle(chr, v, item, rule.get(), b.style());
        }
        if (net.swordie.ms.world.tms282.boxes.Tms282BoxUse.handles(v.itemId())) {
            net.swordie.ms.world.tms282.boxes.Tms282BoxUse.tryUse(chr, item);
            return Outcome.HANDLED;
        }
        if (rule.isEmpty() || rule.get().choice()) {
            return refuse(chr, v, "no style pool for this coupon", "此美容券目前沒有造型資料，未消耗此道具。");
        }
        List<Integer> pool = Tms282BeautyCoupons.pool(chr, rule.get());
        if (pool.isEmpty()) {
            return refuse(chr, v, "empty pool", "此美容券目前沒有適合你的造型，未消耗此道具。");
        }
        return applyStyle(chr, v, item, rule.get(), pool.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(pool.size())));
    }

    private static Outcome applyStyle(Char chr, Tms282CashUseVariant v, Item item, Tms282BeautyCoupons.Rule rule, int style) {
        try {
            net.swordie.ms.client.character.items.Tms282Beauty.apply(chr, rule.applyKind(), style);
        } catch (RuntimeException e) {
            return refuse(chr, v, "apply " + style + ": " + e.getMessage(), e.getMessage());
        }
        chr.consumeItem(item, 1);
        chr.chatMessage(ChatType.GameDesc, String.format("外觀已變更為「%s」。",
                net.swordie.ms.client.character.items.Tms282Beauty.name(rule.kind(), style)));
        return Outcome.HANDLED;
    }

    /**
     * CP02BC (choice coupon opened): validates the owned coupon and answers LP023E with its pool, or with result
     * {@link Tms282CashUseVariant#CHOICE_FAIL_BUSY} when there is none (the client then shows its busy message).
     * Nothing is consumed here; the pick comes back as a CP0150 (beauty family, {@code style} set).
     */
    public static byte[] openChoice(Char chr, Tms282CashUseVariant.ChoiceOpen req) {
        int fail = Tms282CashUseVariant.CHOICE_FAIL_BUSY;
        Item item = chr.getCashInventory().getItemBySlot((short) req.slot());
        Tms282CashItemTypes.Entry entry = Tms282CashItemTypes.get().entry(req.itemId());
        String why = null;
        List<Integer> pool = List.of();
        if (item == null || item.getItemId() != req.itemId() || item.getQuantity() < 1) {
            why = "slot does not hold the coupon";
        } else if (item.getDateExpire() != null && item.getDateExpire().isExpired()) {
            why = "expired";
        } else if (entry == null || !entry.choice() || Family.ofType(entry.type()).orElse(null) != Family.BEAUTY) {
            why = "not a choice beauty coupon";
        } else if (req.target() != 0) {
            why = "target " + req.target() + " (android / second look) not supported";
        } else {
            var rule = Tms282BeautyCoupons.get().rule(req.itemId()).filter(Tms282BeautyCoupons.Rule::choice);
            if (rule.isEmpty()) {
                why = "no style pool for this coupon";
            } else {
                pool = Tms282BeautyCoupons.pool(chr, rule.get());
                if (pool.isEmpty()) {
                    why = "empty pool for this character";
                }
            }
        }
        if (why != null) {
            System.out.printf("[TMS282] CP02BC coupon %d slot %d (%s): refused: %s%n", req.itemId(), req.slot(), chr.getName(), why);
            chr.chatMessage(ChatType.SystemNotice, "此美容券目前沒有可選擇的造型，未消耗此道具。");
            return Tms282CashUseVariant.choiceListBody(req.target(), req.itemId(), fail, 0, 0, null);
        }
        int[] styles = pool.stream().limit(255).mapToInt(Integer::intValue).toArray();
        return Tms282CashUseVariant.choiceListBody(req.target(), req.itemId(), 1, req.slot(), req.androidSn(), styles);
    }

    private static Outcome scissors(Char chr, Tms282CashUseVariant v, Item item, Tms282CashUseVariant.InventoryTarget t) {
        InvType type = InvType.getInvTypeByVal(t.tab());
        Inventory inv = type == null ? null : chr.getInventoryByType(type);
        Item target = inv == null ? null : inv.getItemBySlot((short) t.slot());
        if (!(target instanceof Equip equip)) {
            return refuse(chr, v, "target is not an equip (tab " + t.tab() + ", slot " + t.slot() + ")", "找不到目標裝備。");
        }
        if (!equip.isCash()) {
            return refuse(chr, v, "target is not a cash equip", "此剪刀只能用在時裝（現金）道具上。");
        }
        if (!equip.hasAttribute(EquipAttribute.UnTradable) && !equip.isTradeBlock()) {
            return refuse(chr, v, "target is already tradable", "該道具不需要使用剪刀。");
        }
        equip.setTradeBlock(false);
        equip.removeAttribute(EquipAttribute.UnTradable);
        equip.addAttribute(EquipAttribute.UnTradableAfterTransaction);
        equip.updateToChar(chr);
        chr.consumeItem(item, 1);
        return Outcome.HANDLED;
    }

    private static Outcome anvil(Char chr, Tms282CashUseVariant v, Item item, Tms282CashUseVariant.Anvil a) {
        // 0x4C fashion anvil works on cash equips (decoration tab); 0x49 / 0x4B on the equip tab.
        Inventory inv = chr.getInventoryByType(v.type() == 0x4C ? InvType.DECORATION : InvType.EQUIP);
        Item look = inv == null ? null : inv.getItemBySlot((short) a.appearanceSlot());
        Item into = inv == null ? null : inv.getItemBySlot((short) a.targetSlot());
        if (!(look instanceof Equip appearance) || !(into instanceof Equip function) || a.appearanceSlot() == a.targetSlot()) {
            return refuse(chr, v, "anvil slots " + a.appearanceSlot() + "/" + a.targetSlot(), "請放入兩件裝備。");
        }
        if (ItemConstants.getItemPrefix(appearance.getItemId()) != ItemConstants.getItemPrefix(function.getItemId())) {
            return refuse(chr, v, "different equip kinds", "只能合成相同部位的裝備。");
        }
        if (v.type() == 0x4C && (ItemConstants.isWeapon(function.getItemId()) || !function.isCash() || !appearance.isCash())) {
            return refuse(chr, v, "fashion anvil needs two non-weapon cash equips", "時裝神祕鐵砧只能用在非武器的現金裝備。");
        }
        function.getOptions().set(6, appearance.getItemId() % 10000); // as 265 isFusionAnvil
        function.updateToChar(chr);
        chr.consumeItem(item, 1);
        return Outcome.HANDLED;
    }

    // ---------------------------------------------------------------- helpers

    private static Outcome delegate(Char chr, byte[] packet, byte[] tail265) {
        net.swordie.ms.handlers.item.ItemHandler.handleUserConsumeCashItemUseRequest(chr,
                new InPacket(Tms282CashUseVariant.gms265Body(packet, tail265)));
        return Outcome.DELEGATED_265;
    }

    /** 265 decodeString: u16 length + bytes (InPacket.decodeString reads UTF-8). */
    private static byte[] string265(String text, Char chr) {
        byte[] b = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ByteBuffer.allocate(2 + b.length).order(ByteOrder.LITTLE_ENDIAN).putShort((short) b.length).put(b).array();
    }

    private static byte channel(Char chr) {
        return (byte) (chr.getClient() == null || chr.getClient().getChannelInstance() == null ? 1
                : chr.getClient().getChannelInstance().getChannelId());
    }

    private static Outcome refuse(Char chr, Tms282CashUseVariant v, String why, String chat) {
        log(chr, v, "refused: " + why);
        if (chat != null) {
            chr.chatMessage(ChatType.SystemNotice, chat);
        }
        return Outcome.REFUSED;
    }

    private static Outcome dropped(Char chr, Tms282CashUseVariant v, String why) {
        log(chr, v, "DROPPED (layout verified, no server logic): " + why);
        chr.chatMessage(ChatType.SystemNotice, "此道具目前尚未開放使用，未消耗此道具。");
        return Outcome.DROPPED_NO_SERVER_LOGIC;
    }

    private static void log(Char chr, Tms282CashUseVariant v, String what) {
        System.out.printf("[TMS282] CP0150 %s type 0x%02X item %d slot %d (%s): %s%n", v.family(), v.type(), v.itemId(),
                v.slot(), chr.getName(), what);
    }
}
