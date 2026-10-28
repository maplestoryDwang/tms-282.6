package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282FamiliarPackets;
import net.swordie.ms.connection.packet.Tms282InventoryPackets;
import net.swordie.ms.enums.InvType;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static net.swordie.ms.client.character.tms282.Tms282FamiliarRuntime.Outcome;
import static net.swordie.ms.client.character.tms282.Tms282FamiliarRuntime.actorId;

/**
 * 萌獸 phases 3-4 (research/tms2823/familiar-20260927/CUBE-RESEARCH.md, UPGRADE-RESEARCH.md, LINK-RESEARCH.md):
 * <ul>
 *     <li>cube 5743003 (wire 103: i32 cash slot, i64 serial): three options from the familiar's grade pool, option
 *     level 5; LP008F, leaf21 snapshot, wire 94 (clears the cube pending flag);</li>
 *     <li>memorial cube 5743007 (wire 104) then the choice (wire 105): wire 95 shows [current, rolled], the choice is
 *     the index (0 keep, 1 apply - logged, unverified), wire 96;</li>
 *     <li>upgrade (wire 91: i64 target, i64 material, i32 essence): essence into the gauge (exp), grade + 1 when the
 *     gauge reaches the leaf44 requirement; wire 93 (its effect clears pending). Material upgrades (special
 *     familiars) are refused (code 25) until the special flag's source is known;</li>
 *     <li>disassemble (wire 101: serial vector): rows deleted, essence += leaf48 return per grade; wire 125 event 7;</li>
 *     <li>link (羈絆, wires 92 / 119 / 120 / 121): slots in character_familiar282_user, expiry / unlock / extend
 *     time in QR68469 (E1-E3, L2-L3, A1-A3); fees in mesos (currency not proven); results as wire 125 events
 *     3 / 1 / 4 / 5 after the slot leaves 37..40.</li>
 * </ul>
 * Every change refreshes the server-side option stats (Tms282FamiliarStats) and sends wires 97 / 132.
 */
public final class Tms282FamiliarGrowth {
    public static final int CUBE = 5743003;
    public static final int MEMORIAL_CUBE = 5743007;
    public static final Path POOLS = Path.of("data", "protocol", "tms282.3", "familiar-cube-pools.tsv");
    public static final int OPTION_LEVEL = 5;
    public static final int MAX_GRADE = 4;

    private record Pending(long serial, int[] options, int itemId) {
    }

    private static final Map<Integer, Pending> MEMORIAL = new ConcurrentHashMap<>();
    private static volatile Map<Integer, List<int[]>> pools;

    private Tms282FamiliarGrowth() {
    }

    // ---- option pools ------------------------------------------------------------------------------------------------

    static Map<Integer, List<int[]>> pools() {
        Map<Integer, List<int[]>> map = pools;
        if (map == null) {
            map = new HashMap<>();
            try {
                for (String line : Files.readAllLines(POOLS)) {
                    if (line.isBlank() || line.startsWith("#")) {
                        continue;
                    }
                    String[] f = line.split("\t");
                    map.computeIfAbsent(Integer.parseInt(f[0].strip()), g -> new ArrayList<>())
                            .add(new int[]{Integer.parseInt(f[1].strip()), Integer.parseInt(f[2].strip())});
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            pools = map;
        }
        return map;
    }

    /** Three distinct weighted options of {@code grade}. */
    public static int[] roll(int grade) {
        List<int[]> pool = new ArrayList<>(pools().getOrDefault(grade, List.of()));
        int[] out = new int[3];
        for (int i = 0; i < 3 && !pool.isEmpty(); i++) {
            long total = 0;
            for (int[] e : pool) {
                total += e[1];
            }
            long ticket = ThreadLocalRandom.current().nextLong(total);
            for (int j = 0; j < pool.size(); j++) {
                ticket -= pool.get(j)[1];
                if (ticket < 0) {
                    out[i] = pool.remove(j)[0];
                    break;
                }
            }
        }
        return out;
    }

    static Tms282FamiliarState.RegisteredInfo withOptions(Tms282FamiliarState.RegisteredInfo r, int[] o) {
        return new Tms282FamiliarState.RegisteredInfo(r.serial(), r.state(), r.familiarId(), r.name(), r.level(),
                r.skillId(), r.exp(), OPTION_LEVEL, o[0], o[1], o[2], r.attributes(), r.grade(), r.lookId());
    }

    static Tms282FamiliarState replace(Tms282FamiliarState state, Tms282FamiliarState.RegisteredInfo r) {
        Map<Long, Tms282FamiliarState.RegisteredInfo> records = new HashMap<>(state.registered());
        records.put(r.serial(), r);
        return new Tms282FamiliarState(state.characterId(), state.revision() + 1, records, state.summonedSerial(),
                state.essence());
    }

    private static List<OutPacket> statPackets(Char chr, long serial) {
        return List.of(Tms282FamiliarPackets.optionStatDirty(chr.getId(), actorId(chr), serial),
                Tms282FamiliarPackets.validateStat(chr.getId(), actorId(chr)));
    }

    // ---- cubes -------------------------------------------------------------------------------------------------------

    /** Wire 103. */
    public static Outcome cube(Char chr, int cashSlot, long serial, Charset charset) throws SQLException {
        Item item = chr.getCashInventory().getItemBySlot(cashSlot);
        if (item == null || item.getItemId() != CUBE || item.getQuantity() <= 0) {
            return Outcome.refused("cube: slot " + cashSlot + " holds no " + CUBE);
        }
        Tms282FamiliarState before = Tms282FamiliarRuntime.state(chr);
        var record = before.registered().get(serial);
        if (record == null) {
            return Outcome.refused("cube: serial " + Long.toUnsignedString(serial) + " not owned", 1);
        }
        var next = withOptions(record, roll(record.grade()));
        if (!Tms282FamiliarRuntime.store().update(chr.getId(), next)) {
            return Outcome.refused("cube: row not saved");
        }
        Tms282InventoryDiff bag = Tms282InventoryDiff.of(InvType.CASH, chr.getCashInventory());
        try {
            chr.consumeItem(item, 1);
        } catch (RuntimeException e) {
            Tms282FamiliarRuntime.store().update(chr.getId(), record);
            throw e;
        }
        Tms282FamiliarState after = replace(before, next);
        Tms282FamiliarRuntime.put(chr, after);
        List<OutPacket> packets = new ArrayList<>();
        var ops = bag.changes();
        if (!ops.isEmpty()) {
            packets.add(Tms282InventoryPackets.inventoryOperation(false, ops, charset));
        }
        packets.add(Tms282FamiliarPackets.snapshot(chr.getId(), actorId(chr), after));
        packets.add(Tms282FamiliarPackets.cubeResult(chr.getId(), actorId(chr), next));
        packets.addAll(statPackets(chr, serial));
        return new Outcome(packets, "cube serial " + serial + " grade " + next.grade() + " options "
                + next.option1() + "/" + next.option2() + "/" + next.option3(), true);
    }

    /** Wire 104: consumes one memorial cube and shows [current, rolled]. */
    public static Outcome memorialCube(Char chr, long serial, List<Integer> cashSlots, Charset charset) throws SQLException {
        Item item = null;
        for (int slot : cashSlots) {
            Item it = chr.getCashInventory().getItemBySlot(slot);
            if (it != null && it.getItemId() == MEMORIAL_CUBE && it.getQuantity() > 0) {
                item = it;
                break;
            }
        }
        if (item == null) {
            return Outcome.refused("memorial cube: no " + MEMORIAL_CUBE + " in slots " + cashSlots);
        }
        var record = Tms282FamiliarRuntime.state(chr).registered().get(serial);
        if (record == null) {
            return Outcome.refused("memorial cube: serial " + Long.toUnsignedString(serial) + " not owned", 1);
        }
        int[] rolled = roll(record.grade());
        Tms282InventoryDiff bag = Tms282InventoryDiff.of(InvType.CASH, chr.getCashInventory());
        chr.consumeItem(item, 1);
        MEMORIAL.put(chr.getId(), new Pending(serial, rolled, MEMORIAL_CUBE));
        List<OutPacket> packets = new ArrayList<>();
        var ops = bag.changes();
        if (!ops.isEmpty()) {
            packets.add(Tms282InventoryPackets.inventoryOperation(false, ops, charset));
        }
        packets.add(Tms282FamiliarPackets.optionSelect(chr.getId(), actorId(chr), serial,
                List.of(new int[]{record.option1(), record.option2(), record.option3()}, rolled), MEMORIAL_CUBE, true));
        return new Outcome(packets, "memorial cube serial " + serial + " rolled " + rolled[0] + "/" + rolled[1] + "/"
                + rolled[2], true);
    }

    /** Wire 105: choice 1 applies the rolled set, anything else keeps the current one (logged: unverified). */
    public static Outcome memorialSelect(Char chr, long serial, int choice) throws SQLException {
        Pending pending = MEMORIAL.remove(chr.getId());
        Tms282FamiliarState before = Tms282FamiliarRuntime.state(chr);
        var record = before.registered().get(serial);
        if (pending == null || pending.serial() != serial || record == null) {
            return Outcome.refused("memorial select: no pending roll for " + Long.toUnsignedString(serial));
        }
        var next = choice == 1 ? withOptions(record, pending.options()) : record;
        if (next != record && !Tms282FamiliarRuntime.store().update(chr.getId(), next)) {
            return Outcome.refused("memorial select: row not saved");
        }
        Tms282FamiliarState after = next == record ? before : replace(before, next);
        Tms282FamiliarRuntime.put(chr, after);
        List<OutPacket> packets = new ArrayList<>();
        packets.add(Tms282FamiliarPackets.snapshot(chr.getId(), actorId(chr), after));
        packets.add(Tms282FamiliarPackets.optionSelectResult(chr.getId(), actorId(chr), next, choice));
        packets.addAll(statPackets(chr, serial));
        return new Outcome(packets, "memorial select serial " + serial + " choice " + choice
                + (next == record ? " (kept)" : " (applied)"), true);
    }

    // ---- upgrade / disassemble -----------------------------------------------------------------------------------------

    /** Wire 91. */
    public static Outcome upgrade(Char chr, long target, long material, int essence) throws SQLException {
        Tms282FamiliarState before = Tms282FamiliarRuntime.state(chr);
        var record = before.registered().get(target);
        if (record == null) {
            return Outcome.refused("upgrade: serial " + Long.toUnsignedString(target) + " not owned", 1);
        }
        if (material != 0) {
            return Outcome.refused("upgrade with material familiar (special) not supported", 25);
        }
        if (record.grade() >= MAX_GRADE) {
            return Outcome.refused("upgrade: already grade " + record.grade(), 2);
        }
        if (essence <= 0) {
            return Outcome.refused("upgrade: essence " + essence, 23);
        }
        if (essence > before.essence()) {
            return Outcome.refused("upgrade: has " + before.essence() + " essence, asked " + essence, 20);
        }
        int need = Tms282FamiliarFees.upgradeEssence(record.grade());
        if (need <= 0 || record.exp() + essence > need) {
            return Outcome.refused("upgrade: gauge " + record.exp() + " + " + essence + " > " + need, 24);
        }
        int exp = record.exp() + essence;
        boolean gradeUp = exp >= need;
        var next = new Tms282FamiliarState.RegisteredInfo(record.serial(), record.state(), record.familiarId(),
                record.name(), record.level(), record.skillId(), gradeUp ? 0 : exp, record.optionLevel(), record.option1(),
                record.option2(), record.option3(), record.attributes(), gradeUp ? record.grade() + 1 : record.grade(),
                record.lookId());
        if (!Tms282FamiliarRuntime.store().update(chr.getId(), next)) {
            return Outcome.refused("upgrade: row not saved");
        }
        int left = before.essence() - essence;
        Tms282FamiliarRuntime.store().saveEssence(chr.getId(), left);
        Map<Long, Tms282FamiliarState.RegisteredInfo> records = new HashMap<>(before.registered());
        records.put(next.serial(), next);
        Tms282FamiliarState after = new Tms282FamiliarState(chr.getId(), before.revision() + 1, records,
                before.summonedSerial(), left);
        Tms282FamiliarRuntime.put(chr, after);
        List<OutPacket> packets = new ArrayList<>();
        packets.add(Tms282FamiliarPackets.snapshot(chr.getId(), actorId(chr), after));
        packets.add(Tms282FamiliarPackets.upgradeResult(chr.getId(), actorId(chr), gradeUp, next));
        return new Outcome(packets, "upgrade serial " + target + " +" + essence + " essence -> gauge " + next.exp()
                + " grade " + next.grade() + (gradeUp ? " (grade up)" : "") + ", essence left " + left, true);
    }

    /** Wire 101: disassemble into essence (the only essence source). */
    public static Outcome disassemble(Char chr, List<Long> serials) throws SQLException {
        if (new HashSet<>(serials).size() != serials.size()) {
            return Outcome.refused("disassemble: duplicate serial selection", 13);
        }
        Tms282FamiliarState before = Tms282FamiliarRuntime.state(chr);
        long[] links = Tms282FamiliarRuntime.links(chr);
        if (serials.isEmpty()) {
            return Outcome.refused("disassemble: nothing selected");
        }
        int gain = 0;
        for (long serial : serials) {
            var record = before.registered().get(serial);
            if (record == null) {
                return Outcome.refused("disassemble: serial " + Long.toUnsignedString(serial) + " not owned", 1);
            }
            if (record.locked()) {
                return Outcome.refused("disassemble: " + serial + " locked", 18);
            }
            if (serial == before.summonedSerial()) {
                return Outcome.refused("disassemble: " + serial + " summoned", 5);
            }
            for (long l : links) {
                if (l == serial) {
                    return Outcome.refused("disassemble: " + serial + " linked", 21);
                }
            }
            gain += Tms282FamiliarFees.disassembleEssence(record.grade());
        }
        int essence = Math.addExact(before.essence(), gain);
        if (!Tms282FamiliarRuntime.store().disassemble(before, serials, essence)) {
            return Outcome.refused("disassemble: owned records or essence changed; reload first", 13);
        }
        Map<Long, Tms282FamiliarState.RegisteredInfo> records = new HashMap<>(before.registered());
        for (long serial : serials) records.remove(serial);
        Tms282FamiliarState after = new Tms282FamiliarState(chr.getId(), before.revision() + 1, records,
                before.summonedSerial(), essence);
        Tms282FamiliarRuntime.put(chr, after);
        return new Outcome(List.of(Tms282FamiliarPackets.snapshot(chr.getId(), actorId(chr), after),
                Tms282FamiliarPackets.serverUpdated(chr.getId(), actorId(chr), 7, new long[]{gain})),
                "disassembled " + serials + " -> +" + gain + " essence (" + essence + ")", true);
    }

    // ---- link (羈絆) ------------------------------------------------------------------------------------------------

    static String now() {
        return LocalDateTime.now().format(Tms282FamiliarStats.QR_TIME);
    }

    static boolean unlocked(Char chr, int slot) {
        return slot == 0 || "1".equals(chr.getQRValueByKey(Tms282FamiliarStats.LINK_QR, "L" + (slot + 1)));
    }

    private static Outcome linkRefused(Char chr, String why) {
        return new Outcome(List.of(Tms282FamiliarPackets.linkError(chr.getId(), actorId(chr), 0)), why, false, 14);
    }

    private static boolean pay(Char chr, long mesos) {
        if (mesos <= 0) {
            return true;
        }
        if (chr.getMoney() < mesos) {
            chr.chatMessage("楓幣不足（需要 " + String.format("%,d", mesos) + " 楓幣）。");
            return false;
        }
        chr.deductMoney(mesos);
        return true;
    }

    private static List<OutPacket> linkPackets(Char chr, long[] links, int event, long value) throws SQLException {
        List<OutPacket> packets = new ArrayList<>();
        packets.add(Tms282FamiliarPackets.linkSlots(chr.getId(), actorId(chr), links));
        packets.add(Tms282FamiliarPackets.serverUpdated(chr.getId(), actorId(chr), event, new long[]{value}));
        Tms282FamiliarRuntime.put(chr, Tms282FamiliarRuntime.state(chr)); // re-apply the linked stats
        packets.add(Tms282FamiliarPackets.validateStat(chr.getId(), actorId(chr)));
        return packets;
    }

    /** Wire 92: i32 slot, i64 serial. */
    public static Outcome link(Char chr, int slot, long serial) throws SQLException {
        if (slot < 0 || slot > 2) {
            return linkRefused(chr, "link: slot " + slot + " (VIP not supported)");
        }
        if (!unlocked(chr, slot)) {
            return linkRefused(chr, "link: slot " + slot + " locked");
        }
        Tms282FamiliarState state = Tms282FamiliarRuntime.state(chr);
        long[] links = Tms282FamiliarRuntime.links(chr);
        if (serial != 0 && !state.registered().containsKey(serial)) {
            return linkRefused(chr, "link: serial " + Long.toUnsignedString(serial) + " not owned");
        }
        for (int i = 0; serial != 0 && i < links.length; i++) {
            if (i != slot && links[i] == serial) {
                return linkRefused(chr, "link: " + serial + " already in slot " + i);
            }
        }
        if (!pay(chr, Tms282FamiliarFees.linkApply(slot))) {
            return linkRefused(chr, "link: mesos");
        }
        links[slot] = serial;
        Tms282FamiliarRuntime.putLinks(chr, links);
        return new Outcome(linkPackets(chr, links, 3, serial), "link slot " + slot + " = " + serial, true);
    }

    /** Wire 119: u8 index 1 / 2 unlocks slot 2 / 3 (QR L2 / L3). */
    public static Outcome unlock(Char chr, int index) throws SQLException {
        if (index < 1 || index > 2) {
            return linkRefused(chr, "unlock: index " + index);
        }
        if (unlocked(chr, index)) {
            return linkRefused(chr, "unlock: slot " + index + " already unlocked");
        }
        if (!pay(chr, Tms282FamiliarFees.linkUnlock(index - 1))) {
            return linkRefused(chr, "unlock: mesos");
        }
        chr.setQRValueByKey(Tms282FamiliarStats.LINK_QR, "L" + (index + 1), "1");
        return new Outcome(linkPackets(chr, Tms282FamiliarRuntime.links(chr), 1, index), "unlock link slot " + index, true);
    }

    /** Wire 120: u8 slot; the slot's bonus counts for LinkActivateMaxHour hours (QR E{slot+1}). */
    public static Outcome activate(Char chr, int slot) throws SQLException {
        if (slot < 0 || slot > 2 || !unlocked(chr, slot)) {
            return linkRefused(chr, "activate: slot " + slot);
        }
        if (!pay(chr, Tms282FamiliarFees.linkActivate(slot))) {
            return linkRefused(chr, "activate: mesos");
        }
        String until = LocalDateTime.now().plusHours(Tms282FamiliarFees.LINK_ACTIVATE_MAX_HOURS)
                .format(Tms282FamiliarStats.QR_TIME);
        chr.setQRValueByKey(Tms282FamiliarStats.LINK_QR, "E" + (slot + 1), until);
        return new Outcome(linkPackets(chr, Tms282FamiliarRuntime.links(chr), 4, slot), "activate link slot " + slot
                + " until " + until, true);
    }

    /** Wire 121: u8 slot, i64 client time; the expiry back to LinkActivateMaxHour from now, once per hour. */
    public static Outcome extend(Char chr, int slot) throws SQLException {
        if (slot < 0 || slot > 2 || !Tms282FamiliarStats.linkActive(chr, slot)) {
            return linkRefused(chr, "extend: slot " + slot + " not active");
        }
        String last = chr.getQRValueByKey(Tms282FamiliarStats.LINK_QR, "A" + (slot + 1));
        try {
            if (last != null && !last.isBlank() && Duration.between(LocalDateTime.parse(last.trim(),
                    Tms282FamiliarStats.QR_TIME), LocalDateTime.now()).toMinutes() < 60) {
                return linkRefused(chr, "extend: extended within the hour");
            }
        } catch (RuntimeException ignored) {
            // unreadable: allow
        }
        String until = LocalDateTime.now().plusHours(Tms282FamiliarFees.LINK_ACTIVATE_MAX_HOURS)
                .format(Tms282FamiliarStats.QR_TIME);
        chr.setQRValueByKey(Tms282FamiliarStats.LINK_QR, "E" + (slot + 1), until);
        chr.setQRValueByKey(Tms282FamiliarStats.LINK_QR, "A" + (slot + 1), now());
        return new Outcome(linkPackets(chr, Tms282FamiliarRuntime.links(chr), 5, slot), "extend link slot " + slot
                + " until " + until, true);
    }
}
