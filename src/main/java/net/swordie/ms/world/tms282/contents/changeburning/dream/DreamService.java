package net.swordie.ms.world.tms282.contents.changeburning.dream;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.protocol.ProtocolConfig;
import net.swordie.ms.enums.ChatType;
import net.swordie.ms.world.tms282.contents.changeburning.ChangeBurning;
import net.swordie.ms.world.tms282.contents.changeburning.LucidDream;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;

/**
 * 變身燃燒：露希妲 夢幻的裝備 (content-1005 feature 5, minimum slice of docs/lucid-dream-plan.md D1..D5): the
 * 露希妲物品欄 of the designated character (LP02C8), its requests (CP02D3) and the equipment bonus of the Lucid
 * ability (RPEventStat via {@code LucidAbility.rpEventStatInts}).
 * <ul>
 *     <li>CP02D3 is a command registry by sub ({@link Command}); every request is answered with LP02C8
 *     exclusiveReset = 1, also the refused and the not-handled ones, so the client never stays locked;</li>
 *     <li>sub 0 move / equip / unequip (equipped slot = -bodyPart, 推定), sub 2 enhance (Etc/EventItem.img rates and
 *     dust, failure keeps the level), sub 3 disassemble (dust); subs 4..12 (craft, fusion, sockets, lock, filter)
 *     answer "尚未開放";</li>
 *     <li>dust and items change in one {@link DreamStore} transaction; the client sees dust in QR-ex 102882
 *     {@code dust} and the season-quest flags {@code epic / unique / legend} (grades 2 / 3 / 4) when an equip is
 *     obtained;</li>
 *     <li>no gems (夢幻的結晶) in this slice: socket flags are always 0.</li>
 * </ul>
 * Off unless {@code tms282.lucid.dreamItems=true} (config/server.properties or system property; GM: {@code @dream on}).
 */
public final class DreamService {
    public static final String PROPERTY = "tms282.lucid.dreamItems";
    public static final int QR_EQUIP = LucidDream.QR_EQUIP; // 102882
    public static final int BAG_SLOTS = 128;
    public static final Path CATALOG = Path.of("data/tms282-changeburning/eventitem.tsv");
    public static final String[] GRADE_KEYS = {null, null, "epic", "unique", "legend"};

    public enum Result {OK, OFF, NOT_DESIGNATED, FULL, BAD, FAILED}

    /** What one command did; {@code change} null = nothing stored. */
    record Outcome(DreamStore.Change change, List<DreamPackets.Op> ops, String message, boolean statsChanged) {
        static Outcome refuse(String message) {
            return new Outcome(null, List.of(), message, false);
        }
    }

    /** One CP02D3 sub (Command pattern). */
    interface Command<R extends DreamPackets.Request> {
        Outcome run(DreamService s, DreamStore.Bag bag, R request);
    }

    private static volatile DreamService instance;
    private static volatile Boolean enabled;
    private final DreamCatalog catalog;
    private final DreamStore store;
    private final Predicate<Char> eligible;
    private final Consumer<Char> refresh;
    private final IntUnaryOperator random;
    private volatile BoxInventory boxes = BoxInventory.SERVER;
    private final Map<Integer, DreamStore.Bag> cache = new ConcurrentHashMap<>();
    private final Map<Integer, Object> locks = new ConcurrentHashMap<>();
    private final Map<Integer, Command<? extends DreamPackets.Request>> commands = new HashMap<>();

    public DreamService(DreamCatalog catalog, DreamStore store, Predicate<Char> eligible, Consumer<Char> refresh,
                        IntUnaryOperator random) {
        this.catalog = Objects.requireNonNull(catalog);
        this.store = Objects.requireNonNull(store);
        this.eligible = Objects.requireNonNull(eligible);
        this.refresh = Objects.requireNonNull(refresh);
        this.random = Objects.requireNonNull(random);
        commands.put(0, (Command<DreamPackets.Move>) DreamService::move);
        commands.put(2, (Command<DreamPackets.Enhance>) DreamService::enhance);
        commands.put(3, (Command<DreamPackets.Disassemble>) DreamService::disassemble);
    }

    public static DreamService get() {
        DreamService s = instance;
        if (s == null) {
            synchronized (DreamService.class) {
                s = instance;
                if (s == null) {
                    s = new DreamService(DreamCatalog.load(CATALOG), new DreamStore.Sql(),
                            chr -> ChangeBurning.get().isDesignated(chr),
                            chr -> ChangeBurning.get().refreshAbility(chr),
                            bound -> java.util.concurrent.ThreadLocalRandom.current().nextInt(bound));
                    instance = s;
                }
            }
        }
        return s;
    }

    public static void install(DreamService service) {
        instance = service;
    }

    public static boolean enabled() {
        Boolean e = enabled;
        if (e == null) {
            String sys = System.getProperty(PROPERTY);
            if (sys != null) {
                e = Boolean.parseBoolean(sys.trim());
            } else {
                Properties p = new Properties();
                if (Files.isRegularFile(ProtocolConfig.CONFIG_FILE)) {
                    try (InputStream in = Files.newInputStream(ProtocolConfig.CONFIG_FILE)) {
                        p.load(in);
                    } catch (Exception ignored) {
                        // unreadable config: off
                    }
                }
                e = Boolean.parseBoolean(p.getProperty(PROPERTY, "false").trim());
            }
            enabled = e;
        }
        return e;
    }

    public static void setEnabled(Boolean value) {
        enabled = value;
    }

    /** Tests: the box consumption to use. */
    public void setBoxes(BoxInventory boxes) {
        this.boxes = Objects.requireNonNull(boxes);
    }

    public DreamCatalog catalog() {
        return catalog;
    }

    private Object lock(Char chr) {
        return locks.computeIfAbsent(chr.getId(), k -> new Object());
    }

    /** The character's bag (cached after the first load; null when the store cannot be read). */
    public DreamStore.Bag bag(Char chr) {
        DreamStore.Bag b = cache.get(chr.getId());
        if (b != null) {
            return b;
        }
        try {
            b = store.load(chr.getId());
        } catch (Exception e) {
            System.out.println("[DreamEquipment] load failed character=" + chr.getId() + " error=" + e);
            return null;
        }
        cache.put(chr.getId(), b);
        return b;
    }

    /** Equipped bonus, the 9-stat vector of {@code LucidAbility.finalBlock} (zeros when off / nothing equipped). */
    public int[] equippedStats(Char chr) {
        int[] v = new int[DreamCatalog.STATS];
        if (chr == null || !enabled()) {
            return v;
        }
        DreamStore.Bag b = bag(chr);
        if (b == null) {
            return v;
        }
        for (DreamStore.Equip e : b.equips()) {
            if (e.equipped()) {
                int[] s = catalog.equipStats(e.bodyPart(), e.grade(), e.enhance());
                for (int i = 0; i < v.length; i++) {
                    v[i] += s[i];
                }
            }
        }
        return v;
    }

    /** Field entry / designation: the whole bag (op 0 per item, exclusiveReset 0) and the dust record. */
    public void sendAll(Char chr) {
        if (chr == null || !enabled() || !eligible.test(chr)) {
            return;
        }
        DreamStore.Bag b = bag(chr);
        if (b == null) {
            return;
        }
        List<DreamPackets.Op> ops = new ArrayList<>();
        for (DreamStore.Equip e : b.equips()) {
            ops.add(DreamPackets.Op.add(e));
        }
        chr.write(DreamPackets.inventoryOperation(false, 0, 0, 0, ops));
        chr.setQRValueByKey(QR_EQUIP, "dust", String.valueOf(b.dust()));
    }

    /** Puts a new equip (ItemInfo {@code index}) into the first free bag slot. */
    public Result grant(Char chr, int index) {
        if (chr == null) {
            return Result.FAILED;
        }
        if (!enabled()) {
            return Result.OFF;
        }
        if (!eligible.test(chr)) {
            return Result.NOT_DESIGNATED;
        }
        DreamCatalog.Info info = catalog.info(index);
        if (info == null || info.type() != DreamCatalog.TYPE_EQUIP) {
            return Result.BAD;
        }
        synchronized (lock(chr)) {
            DreamStore.Bag b = bag(chr);
            if (b == null) {
                return Result.FAILED;
            }
            int pos = freeSlot(b);
            if (pos < 0) {
                return Result.FULL;
            }
            DreamStore.Equip e = new DreamStore.Equip(b.nextSn(), index, info.grade(), info.bodyPart(), 0, pos, 0);
            DreamStore.Change ch = new DreamStore.Change(List.of(e), List.of(), b.dust(), b.nextSn() + 1);
            if (!commit(chr, b, ch)) {
                return Result.FAILED;
            }
            chr.write(DreamPackets.inventoryOperation(false, 0, 0, 0, List.of(DreamPackets.Op.add(e))));
            String key = info.grade() < GRADE_KEYS.length ? GRADE_KEYS[info.grade()] : null;
            if (key != null) {
                chr.setQRValueByKey(QR_EQUIP, key, "1");
            }
            System.out.println("[DreamEquipment] character=" + chr.getId() + " got " + info.name() + " sn=" + e.sn());
            return Result.OK;
        }
    }

    /** Choice boxes 2831845..2831847: grade of each box (稀有 / 罕見 / 傳說). */
    public static final Map<Integer, Integer> EQUIP_BOXES = Map.of(2831845, 2, 2831846, 3, 2831847, 4);

    /**
     * Opens an equipment choice box for {@code bodyPart}: checks first (enabled, designated, a free slot), then in the
     * economy lock consumes the box and saves the character, then stores the equip; if that fails the box is given
     * back. A crash between the two loses the box (logged), it never duplicates an equip.
     */
    public Result openEquipBox(Char chr, int boxItemId, int bodyPart) {
        return openEquipBox(chr, boxItemId, bodyPart, boxes);
    }

    public Result openEquipBox(Char chr, int boxItemId, int bodyPart, BoxInventory inv) {
        Integer grade = EQUIP_BOXES.get(boxItemId);
        if (chr == null || grade == null || bodyPart < 1 || bodyPart > 3) {
            return Result.BAD;
        }
        if (!enabled()) {
            return Result.OFF;
        }
        if (!eligible.test(chr)) {
            return Result.NOT_DESIGNATED;
        }
        DreamCatalog.Info info = catalog.equip(bodyPart, grade);
        DreamStore.Bag b = bag(chr);
        if (info == null || b == null) {
            return Result.FAILED;
        }
        if (freeSlot(b) < 0) {
            return Result.FULL;
        }
        if (!inv.take(chr, boxItemId)) {
            return Result.BAD;
        }
        Result r = grant(chr, info.index());
        if (r != Result.OK) {
            inv.giveBack(chr, boxItemId);
            System.out.println("[DreamEquipment] box " + boxItemId + " returned to character " + chr.getId() + " (" + r + ")");
        }
        return r;
    }

    /** Box consumption (the server uses the economy lock and saves; tests record). */
    public interface BoxInventory {
        boolean take(Char chr, int boxItemId);

        void giveBack(Char chr, int boxItemId);

        BoxInventory SERVER = new BoxInventory() {
            @Override
            public boolean take(Char chr, int boxItemId) {
                return net.swordie.ms.client.character.tms282.EconomyLocks.call(chr, () -> {
                    if (!chr.hasItemCount(boxItemId, 1)) {
                        return false;
                    }
                    chr.consumeItem(boxItemId, 1);
                    net.swordie.ms.lifecycle.OnlineCharacterSaver.save(chr);
                    return true;
                });
            }

            @Override
            public void giveBack(Char chr, int boxItemId) {
                net.swordie.ms.client.character.tms282.EconomyLocks.run(chr, () -> {
                    chr.addItemToInventory(boxItemId, 1);
                    net.swordie.ms.lifecycle.OnlineCharacterSaver.save(chr);
                });
            }
        };
    }

    /** GM: sets the dust. */
    public Result setDust(Char chr, long dust) {
        if (chr == null || !enabled()) {
            return Result.OFF;
        }
        synchronized (lock(chr)) {
            DreamStore.Bag b = bag(chr);
            if (b == null || !commit(chr, b, new DreamStore.Change(List.of(), List.of(), Math.max(0, dust), b.nextSn()))) {
                return Result.FAILED;
            }
            chr.setQRValueByKey(QR_EQUIP, "dust", String.valueOf(Math.max(0, dust)));
            return Result.OK;
        }
    }

    /**
     * dreamtrial-1005: adds {@code amount} dust in one store transaction (夢幻的試煉 clear reward). OFF when 夢幻的裝備
     * is off, NOT_DESIGNATED for other characters, FAILED when the store write fails (nothing changed then).
     */
    public Result addDust(Char chr, long amount) {
        if (chr == null || amount <= 0) {
            return Result.BAD;
        }
        if (!enabled()) {
            return Result.OFF;
        }
        if (!eligible.test(chr)) {
            return Result.NOT_DESIGNATED;
        }
        synchronized (lock(chr)) {
            DreamStore.Bag b = bag(chr);
            long dust = b == null ? 0 : Math.min(Long.MAX_VALUE - amount, b.dust()) + amount;
            if (b == null || !commit(chr, b, new DreamStore.Change(List.of(), List.of(), dust, b.nextSn()))) {
                return Result.FAILED;
            }
            chr.setQRValueByKey(QR_EQUIP, "dust", String.valueOf(dust));
            return Result.OK;
        }
    }

    private static int freeSlot(DreamStore.Bag b) {
        for (int p = 1; p <= BAG_SLOTS; p++) {
            if (b.at(p) == null) {
                return p;
            }
        }
        return -1;
    }

    private boolean commit(Char chr, DreamStore.Bag before, DreamStore.Change ch) {
        try {
            store.commit(chr.getId(), ch);
        } catch (Exception e) {
            System.out.println("[DreamEquipment] commit failed character=" + chr.getId() + " error=" + e);
            cache.remove(chr.getId());
            return false;
        }
        Map<Long, DreamStore.Equip> rows = new java.util.LinkedHashMap<>();
        for (DreamStore.Equip e : before.equips()) {
            rows.put(e.sn(), e);
        }
        ch.deletes().forEach(rows::remove);
        ch.upserts().forEach(e -> rows.put(e.sn(), e));
        cache.put(chr.getId(), new DreamStore.Bag(ch.dust(), ch.nextSn(), new ArrayList<>(rows.values())));
        return true;
    }

    /** CP02D3: always answers LP02C8 with exclusiveReset = 1. */
    @SuppressWarnings("unchecked")
    public void onRequest(Char chr, DreamPackets.Request request) {
        if (chr == null) {
            return;
        }
        Outcome out;
        if (!enabled()) {
            out = Outcome.refuse("夢幻的裝備目前沒有開放。");
        } else if (!eligible.test(chr)) {
            out = Outcome.refuse("只有變身燃燒指定角色才能使用露希妲物品欄。");
        } else {
            Command<DreamPackets.Request> cmd = (Command<DreamPackets.Request>) commands.get(request.sub());
            if (cmd == null) {
                out = Outcome.refuse("這個功能（製作、合成、結晶、鎖定）尚未開放。");
            } else {
                synchronized (lock(chr)) {
                    DreamStore.Bag b = bag(chr);
                    if (b == null) {
                        out = Outcome.refuse("露希妲物品欄暫時無法讀取，請稍後再試。");
                    } else {
                        out = cmd.run(this, b, request);
                        if (out.change() != null && !commit(chr, b, out.change())) {
                            out = Outcome.refuse("處理失敗，請稍後再試。");
                        }
                    }
                }
            }
        }
        chr.write(DreamPackets.inventoryOperation(true, 0, 0, 0, out.ops()));
        if (out.change() != null) {
            chr.setQRValueByKey(QR_EQUIP, "dust", String.valueOf(out.change().dust()));
        }
        if (out.message() != null) {
            chr.chatMessage(ChatType.SystemNotice, out.message());
        }
        if (out.statsChanged()) {
            try {
                refresh.accept(chr);
            } catch (RuntimeException e) {
                System.out.println("[DreamEquipment] ability refresh failed character=" + chr.getId() + ": " + e);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ commands

    /** sub 0: bag -> bag (move / swap), bag -> -bodyPart (equip, swapping the equipped one), -bodyPart -> bag (unequip). */
    private static Outcome move(DreamService s, DreamStore.Bag b, DreamPackets.Move r) {
        if (r.invType() != DreamPackets.INV_EQUIP) {
            return Outcome.refuse("夢幻的結晶尚未開放。");
        }
        DreamStore.Equip from = b.at(r.from());
        if (from == null || (r.snFrom() != 0 && from.sn() != r.snFrom()) || r.from() == r.to() || r.to() == 0
                || r.to() > BAG_SLOTS || r.from() > BAG_SLOTS) {
            return Outcome.refuse(null);
        }
        if (r.to() < 0 && -r.to() != from.bodyPart()) {
            return Outcome.refuse("這個部位不能裝備這件夢幻的裝備。");
        }
        DreamStore.Equip target = b.at(r.to());
        if (target != null && r.from() < 0 && target.bodyPart() != from.bodyPart()) {
            return Outcome.refuse("這個部位不能裝備這件夢幻的裝備。");
        }
        List<DreamStore.Equip> ups = new ArrayList<>();
        List<DreamPackets.Op> ops = new ArrayList<>();
        ops.add(DreamPackets.Op.remove(r.from()));
        if (target != null) {
            ops.add(DreamPackets.Op.remove(r.to()));
        }
        DreamStore.Equip moved = from.at(r.to());
        ups.add(moved);
        ops.add(DreamPackets.Op.add(moved));
        if (target != null) {
            DreamStore.Equip back = target.at(r.from());
            ups.add(back);
            ops.add(DreamPackets.Op.add(back));
        }
        boolean stats = r.from() < 0 || r.to() < 0;
        return new Outcome(new DreamStore.Change(ups, List.of(), b.dust(), b.nextSn()), ops, null, stats);
    }

    /** sub 2: dust is spent on every try; success raises +1 (rates of Etc/EventItem.img), failure keeps the level. */
    private static Outcome enhance(DreamService s, DreamStore.Bag b, DreamPackets.Enhance r) {
        DreamStore.Equip e = b.at(r.pos());
        if (e == null || (r.sn() != 0 && e.sn() != r.sn())) {
            return Outcome.refuse(null);
        }
        int success = s.catalog.enhanceSuccess(e.grade(), e.enhance());
        int cost = s.catalog.enhanceDust(e.grade(), e.enhance());
        if (success < 0 || cost < 0 || e.enhance() >= DreamCatalog.MAX_ENHANCE) {
            return Outcome.refuse("這件夢幻的裝備已經無法再強化了。");
        }
        if (b.dust() < cost) {
            return Outcome.refuse("夢幻粉末不足（需要 " + cost + "）。");
        }
        boolean ok = s.random.applyAsInt(1000) < success;
        DreamStore.Equip after = ok ? e.enhanced(e.enhance() + 1) : e;
        String msg = ok ? "強化成功！+" + after.enhance() + "（消耗夢幻粉末 " + cost + "）"
                : "強化失敗，等級維持 +" + e.enhance() + "（消耗夢幻粉末 " + cost + "）";
        return new Outcome(new DreamStore.Change(List.of(after), List.of(), b.dust() - cost, b.nextSn()),
                List.of(DreamPackets.Op.update(after)), msg, ok && after.equipped());
    }

    /** sub 3: bag equips only (not equipped); dust of DisassembleEquip/&lt;grade&gt;/&lt;enhance&gt;. */
    private static Outcome disassemble(DreamService s, DreamStore.Bag b, DreamPackets.Disassemble r) {
        List<Long> deletes = new ArrayList<>();
        List<DreamPackets.Op> ops = new ArrayList<>();
        long gained = 0;
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (long[] it : r.items()) {
            int pos = (int) it[0];
            DreamStore.Equip e = b.at(pos);
            if (pos <= 0 || e == null || !seen.add(pos) || (it[1] != 0 && e.sn() != it[1])) {
                continue;
            }
            gained += s.catalog.disassembleDust(e.grade(), e.enhance());
            deletes.add(e.sn());
            ops.add(DreamPackets.Op.remove(pos));
        }
        if (deletes.isEmpty()) {
            return Outcome.refuse(null);
        }
        return new Outcome(new DreamStore.Change(List.of(), deletes, b.dust() + gained, b.nextSn()), ops,
                "分解了 " + deletes.size() + " 件，獲得夢幻粉末 " + gained + "。", false);
    }
}
