package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282FamiliarPackets;
import net.swordie.ms.connection.packet.Tms282InventoryPackets;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.loaders.StringData;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 282 萌獸 phase 1 (research/tms2823/familiar-20260927): register a 287xxxx card from the use bag (CP027D wire 86,
 * i32 consume slot - native 362EC38 via 7693780), rename (wire 99) and favorite (wire 117). The window then gets the
 * whole registered array again (LP0425 leaf21, {@link Tms282FamiliarPackets#snapshot}); registration, rename and
 * favorite do not set the UI's pending flag (only the cube / upgrade senders 6428352 / 6428962 / 6441CB8 do), so no
 * extra reply is needed.
 * <p>
 * First-registration values are a server policy (no native evidence of official defaults, CARD-MAPPING-RESEARCH.md):
 * state 0, level 1, no skill / options, grade from the card, name = the card name without "萌獸". The familiar row is
 * written first, then the card is consumed (Char.consumeItem persists it); a failed consume deletes the row.
 */
public final class Tms282FamiliarRuntime {
    public static final Path CARDS = Path.of("data", "protocol", "tms282.3", "familiar-cards.tsv");
    public static final int MAX_FAMILIARS = 500;
    private static final Charset CP950 = Charset.forName("MS950");

    public record Card(int itemId, int familiarId, int grade, String name) {
    }

    /**
     * The packets of an accepted operation (inventory first), or a refusal: OnRequestFailed {@code reason} (64265D0
     * table), or the refusal's own packets when it has some (e.g. OnLinkFamiliarError).
     */
    public record Outcome(List<OutPacket> packets, String log, boolean accepted, int reason) {
        public Outcome(List<OutPacket> packets, String log, boolean accepted) {
            this(packets, log, accepted, 14);
        }

        static Outcome refused(String why) {
            return new Outcome(List.of(), why, false, 14);
        }

        static Outcome refused(String why, int reason) {
            return new Outcome(List.of(), why, false, reason);
        }
    }

    private static volatile Map<Integer, Card> cards;
    private static final Map<Integer, Tms282FamiliarState> STATES = new ConcurrentHashMap<>();
    /** Link slots 1..3 + VIP per character (serials, 0 = empty). */
    private static final Map<Integer, long[]> LINKS = new ConcurrentHashMap<>();
    private static volatile Tms282FamiliarStore store = new Tms282FamiliarStore();

    private Tms282FamiliarRuntime() {
    }

    /** Tests: an in-memory / test database store. */
    public static void useStore(Tms282FamiliarStore next) {
        store = Objects.requireNonNull(next, "store");
        STATES.clear();
        LINKS.clear();
    }

    public static Map<Integer, Card> cards() {
        Map<Integer, Card> map = cards;
        if (map == null) {
            map = new HashMap<>();
            try {
                for (String line : Files.readAllLines(CARDS)) {
                    String t = line.strip();
                    if (t.isEmpty() || t.startsWith("#")) {
                        continue;
                    }
                    String[] f = line.split("\t");
                    Card card = new Card(Integer.parseInt(f[0].strip()), Integer.parseInt(f[1].strip()),
                            Integer.parseInt(f[2].strip()), f.length > 3 ? f[3].strip() : "");
                    map.put(card.itemId(), card);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            map = Map.copyOf(map);
            cards = map;
        }
        return map;
    }

    static boolean knownFamiliar(int familiarId) {
        return cards().values().stream().anyMatch(c -> c.familiarId() == familiarId);
    }

    /** Loads the character's familiars from the database (called at every window bootstrap). */
    public static Tms282FamiliarState reload(Char chr) throws SQLException {
        Tms282FamiliarState state = store.load(chr.getId());
        STATES.put(chr.getId(), state);
        long[] links = store.loadLinks(chr.getId());
        for (int i = 0; i < links.length; i++) {
            if (links[i] != 0 && !state.registered().containsKey(links[i])) {
                links[i] = 0; // the familiar is gone
            }
        }
        LINKS.put(chr.getId(), links);
        Tms282FamiliarStats.refresh(chr, state, links);
        return state;
    }

    static Tms282FamiliarStore store() {
        return store;
    }

    static void put(Char chr, Tms282FamiliarState state) {
        STATES.put(chr.getId(), state);
        Tms282FamiliarStats.refresh(chr, state, links(chr));
    }

    /** The character's link slots (a copy). */
    public static long[] links(Char chr) {
        long[] links = LINKS.get(chr.getId());
        return links == null ? new long[4] : links.clone();
    }

    static void putLinks(Char chr, long[] links) throws SQLException {
        store.saveLinks(chr.getId(), links);
        LINKS.put(chr.getId(), links.clone());
    }

    /** The loaded state without a database read (null when not loaded in this server). */
    public static Tms282FamiliarState cached(Char chr) {
        return STATES.get(chr.getId());
    }

    public static Tms282FamiliarState state(Char chr) throws SQLException {
        Tms282FamiliarState state = STATES.get(chr.getId());
        return state != null ? state : reload(chr);
    }

    public static long actorId(Char chr) {
        return 0x50000000L + chr.getId();
    }

    /** CP027D wire 86: the 287xxxx card at {@code consumeSlot} becomes a registered familiar. */
    public static Outcome register(Char chr, int consumeSlot, Charset charset) throws SQLException {
        Item item = chr.getConsumeInventory().getItemBySlot(consumeSlot);
        if (item == null || item.getQuantity() <= 0) {
            return Outcome.refused("no item in use slot " + consumeSlot);
        }
        Card card = cards().get(item.getItemId());
        if (card == null) {
            return Outcome.refused("item " + item.getItemId() + " is not a 282 familiar card");
        }
        if (item.getDateExpire() != null && item.getDateExpire().isExpired()) {
            return Outcome.refused("card " + item.getItemId() + " expired");
        }
        Tms282FamiliarState before = state(chr);
        if (before.registered().size() >= MAX_FAMILIARS) {
            return Outcome.refused("already " + before.registered().size() + " familiars");
        }
        var draft = new Tms282FamiliarState.RegisteredInfo(1, 0, card.familiarId(), defaultName(card.itemId()), 1, 0,
                0, 0, 0, 0, 0, 0, card.grade(), 0);
        long serial = store.insert(chr.getId(), card.itemId(), draft);
        Tms282InventoryDiff bag = Tms282InventoryDiff.of(InvType.CONSUME, chr.getConsumeInventory());
        try {
            chr.consumeItem(item, 1);
        } catch (RuntimeException e) {
            store.delete(chr.getId(), serial);
            throw e;
        }
        var info = new Tms282FamiliarState.RegisteredInfo(serial, draft.state(), draft.familiarId(), draft.name(),
                draft.level(), draft.skillId(), draft.exp(), draft.optionLevel(), draft.option1(), draft.option2(),
                draft.option3(), draft.attributes(), draft.grade(), draft.lookId());
        Map<Long, Tms282FamiliarState.RegisteredInfo> records = new HashMap<>(before.registered());
        records.put(serial, info);
        Tms282FamiliarState after = new Tms282FamiliarState(chr.getId(), before.revision() + 1, records,
                before.summonedSerial(), before.essence());
        put(chr, after);
        List<OutPacket> packets = new ArrayList<>(2);
        var ops = bag.changes();
        if (!ops.isEmpty()) {
            packets.add(Tms282InventoryPackets.inventoryOperation(false, ops, charset));
        }
        packets.add(Tms282FamiliarPackets.snapshot(chr.getId(), actorId(chr), after));
        return new Outcome(packets, "registered card " + card.itemId() + " -> familiar " + card.familiarId()
                + " grade " + card.grade() + " serial " + serial + " (" + records.size() + " total)", true);
    }

    /** CP027D wire 84: summon an owned familiar (leaf36 SummonedSN); a summoned one is replaced. */
    public static Outcome summon(Char chr, long serial) throws SQLException {
        Tms282FamiliarState before = state(chr);
        if (!before.registered().containsKey(serial)) {
            return Outcome.refused("summon: serial " + Long.toUnsignedString(serial) + " not owned", 1);
        }
        if (chr.getField() != null && !Tms282FamiliarCombat.allowedIn(chr.getField().getId())) {
            return Outcome.refused("summon: familiars are not allowed in field " + chr.getField().getId(), 12);
        }
        return summoned(chr, before, serial, "summon serial " + Long.toUnsignedString(serial));
    }

    /** CP027D wire 85: unsummon. */
    public static Outcome unsummon(Char chr) throws SQLException {
        Tms282FamiliarState before = state(chr);
        if (before.summonedSerial() == 0) {
            return Outcome.refused("unsummon: nothing summoned");
        }
        return summoned(chr, before, 0, "unsummon serial " + Long.toUnsignedString(before.summonedSerial()));
    }

    static Outcome summoned(Char chr, Tms282FamiliarState before, long serial, String what) throws SQLException {
        store.saveSummoned(chr.getId(), serial);
        Tms282FamiliarState after = new Tms282FamiliarState(chr.getId(), before.revision() + 1, before.registered(),
                serial, before.essence());
        put(chr, after);
        List<OutPacket> packets = new ArrayList<>(Tms282FamiliarPawn.onSummonChanged(chr, before.summonedSerial(), after));
        packets.add(0, Tms282FamiliarPackets.snapshot(chr.getId(), actorId(chr), after));
        packets.add(Tms282FamiliarPackets.optionStatDirty(chr.getId(), actorId(chr), serial));
        packets.add(Tms282FamiliarPackets.validateStat(chr.getId(), actorId(chr)));
        if (before.summonedSerial() != 0) {
            Tms282FamiliarCombat.hideFromOthers(chr);
        }
        if (serial != 0) {
            Tms282FamiliarCombat.showToOthers(chr, after);
        }
        return new Outcome(packets, what, true);
    }

    /** CP027D wire 99: CP950 name, at most 14 bytes. */
    public static Outcome rename(Char chr, long serial, byte[] nameBytes) throws SQLException {
        String name = new String(nameBytes, CP950);
        return edit(chr, serial, "rename to " + name,
                (service, state) -> service.rename(state, chr.getId(), serial, name));
    }

    /** CP027D wire 117. */
    public static Outcome favorite(Char chr, long serial, boolean value) throws SQLException {
        return edit(chr, serial, "favorite " + value,
                (service, state) -> service.favorite(state, chr.getId(), serial, value));
    }

    interface Edit {
        Tms282FamiliarState apply(Tms282FamiliarService service, Tms282FamiliarState state);
    }

    static Outcome edit(Char chr, long serial, String what, Edit edit) throws SQLException {
        Tms282FamiliarState before = state(chr);
        Tms282FamiliarState after;
        try {
            after = edit.apply(new Tms282FamiliarService(Tms282FamiliarRuntime::knownFamiliar), before);
        } catch (IllegalArgumentException e) {
            return Outcome.refused(what + " of " + Long.toUnsignedString(serial) + ": " + e.getMessage());
        }
        if (after != before && !store.update(chr.getId(), after.registered().get(serial))) {
            return Outcome.refused(what + ": row " + serial + " not saved");
        }
        put(chr, after);
        return new Outcome(List.of(Tms282FamiliarPackets.snapshot(chr.getId(), actorId(chr), after)),
                what + " serial " + Long.toUnsignedString(serial), true);
    }

    /** The card name without the trailing "萌獸", cut to 14 CP950 bytes (the wire limit). */
    static String defaultName(int cardItemId) {
        Card card = cards().get(cardItemId);
        String name = card != null && !card.name().isEmpty() ? card.name() : StringData.getItemStringById(cardItemId);
        if (name == null) {
            return "";
        }
        name = name.strip();
        if (name.endsWith("萌獸") && name.length() > 2) {
            name = name.substring(0, name.length() - 2).strip();
        }
        while (!name.isEmpty()) {
            try {
                Tms282FamiliarState.encodeName(name);
                return name;
            } catch (IllegalArgumentException tooLongOrUnmappable) {
                name = name.substring(0, name.length() - 1);
            }
        }
        return "";
    }
}
