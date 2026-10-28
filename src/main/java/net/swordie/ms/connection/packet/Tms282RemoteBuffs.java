package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.tms282.Tms282BuffDirectory;
import net.swordie.ms.connection.OutPacket;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Other users' view of the buffs the 265 {@code TemporaryStatManager} sets (job handlers after a 282 skill use,
 * potions, party buffs applied to members, scripts, mob debuffs) - multiplayer-0930. The 265
 * REMOTE_SET/RESET_TEMPORARY_STAT had no 282 codec and were dropped by the fail-closed guard, so only the 282 skill
 * service's own book (LP048F/LP0490, {@code Tms282SkillService}) reached the others. Here the TSM's stats go through
 * the same traced {@link Tms282RemoteStatEncoder} (Tms282RemoteBuffCheck: LP048F 5ADDAD0 / LP0490 5ADDD30 CLEAN):
 * only rows the 282 remote table supports are sent, the rest are logged once; stats the character's 282 book
 * already announced are left to it (no double LP048F / early LP0490). An LP0490 whose indie list the client cannot
 * carry is followed by the LP048F refresh (the Reset contract).
 */
public final class Tms282RemoteBuffs {
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();
    /** Source-dependent stats (icon / soul gauge by reason): never from the TSM path (Tms282ManagedBuffs). */
    static final Set<CharacterTemporaryStat> SOURCE_DEPENDENT = Tms282ManagedBuffs.SOURCE_DEPENDENT;

    private static volatile Tms282RemoteStatEncoder encoder;
    private static volatile boolean failed;

    private Tms282RemoteBuffs() {
    }

    public static boolean active() {
        return net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
    }

    /** The shared remote encoder (data/protocol/tms282.3/cts-remote.tsv), null when the table cannot be loaded. */
    public static Tms282RemoteStatEncoder encoder() {
        Tms282RemoteStatEncoder e = encoder;
        if (e != null || failed) {
            return e;
        }
        synchronized (Tms282RemoteBuffs.class) {
            if (encoder == null && !failed) {
                try {
                    encoder = new Tms282RemoteStatEncoder(Tms282RemoteCtsTable.load(Path.of("data", "protocol", "tms282.3")));
                } catch (IOException | RuntimeException ex) {
                    failed = true;
                    System.out.println("[TMS282] remote CTS table not loaded, TSM buffs stay invisible to others: " + ex);
                }
            }
            return encoder;
        }
    }

    /** For checks: a given encoder instead of the file table. */
    public static void useEncoder(Tms282RemoteStatEncoder e) {
        encoder = e;
        failed = e == null;
    }

    /** The stats of {@code stats} the TSM path may announce for {@code characterId}. */
    static Map<CharacterTemporaryStat, List<Option>> own(int characterId, Map<CharacterTemporaryStat, List<Option>> stats) {
        EnumMap<CharacterTemporaryStat, List<Option>> out = new EnumMap<>(CharacterTemporaryStat.class);
        if (stats == null || stats.isEmpty()) {
            return out;
        }
        Map<CharacterTemporaryStat, List<Option>> book;
        try {
            book = Tms282BuffDirectory.INSTANCE.activeStats(characterId);
        } catch (RuntimeException e) {
            book = Map.of();
        }
        for (Map.Entry<CharacterTemporaryStat, List<Option>> e : stats.entrySet()) {
            if (e.getKey() == null || SOURCE_DEPENDENT.contains(e.getKey())) {
                continue;
            }
            List<Option> owned = book.get(e.getKey());
            if (owned != null) {
                if (!e.getKey().isIndie() || e.getValue() == null || e.getValue().stream()
                        .allMatch(o -> owned.stream().anyMatch(known -> known.nReason == o.nReason))) continue;
                // An additional legacy source requires the complete indie list, including book-owned options.
            }
            out.put(e.getKey(), e.getValue() == null ? List.of() : e.getValue());
        }
        return out;
    }

    /** LP048F for what the TSM set, or null when nothing of it can be shown. */
    public static OutPacket set(int characterId, Map<CharacterTemporaryStat, List<Option>> stats, long nowMillis) {
        Tms282RemoteStatEncoder enc = encoder();
        Map<CharacterTemporaryStat, List<Option>> mine = own(characterId, stats);
        if (enc == null || mine.isEmpty()) {
            return null;
        }
        TemporaryStatEncoder.Selection sel = enc.select(mine);
        log("set", sel);
        if (sel.isEmpty()) {
            return null;
        }
        return Tms282UserRemotePackets.setTemporaryStat(characterId, enc, sel, nowMillis);
    }

    /**
     * LP0490 (+ LP048F refresh) for what the TSM removed; {@code current} = the TSM's stats after the removal (the
     * options indie stats keep). Empty when nothing of it was showable.
     */
    public static List<OutPacket> reset(int characterId, Map<CharacterTemporaryStat, List<Option>> removed,
                                        Map<CharacterTemporaryStat, List<Option>> current, long nowMillis) {
        List<OutPacket> out = new ArrayList<>(2);
        Tms282RemoteStatEncoder enc = encoder();
        Map<CharacterTemporaryStat, List<Option>> mine = own(characterId, removed);
        if (enc == null || mine.isEmpty()) {
            return out;
        }
        EnumMap<CharacterTemporaryStat, List<Option>> left = new EnumMap<>(CharacterTemporaryStat.class);
        if (current != null) {
            for (CharacterTemporaryStat cts : mine.keySet()) {
                List<Option> stays = current.get(cts);
                if (cts.isIndie() && stays != null && !stays.isEmpty()) {
                    left.put(cts, new ArrayList<>(stays));
                }
            }
        }
        Tms282RemoteStatEncoder.Reset reset = enc.selectReset(mine, left);
        log("reset", reset.reset());
        if (!reset.reset().isEmpty()) {
            out.add(Tms282UserRemotePackets.resetTemporaryStat(characterId, enc, reset, nowMillis));
        }
        if (!reset.refresh().isEmpty()) {
            out.add(Tms282UserRemotePackets.setTemporaryStat(characterId, enc, reset.refresh(), nowMillis));
        }
        return out;
    }

    /** TSM set -> the others in the field (265: the unchanged 265 packet). */
    public static void broadcastSet(Char chr, java.util.EnumMap<CharacterTemporaryStat, List<Option>> stats) {
        if (chr == null || chr.getField() == null) {
            return;
        }
        if (!active()) {
            chr.getField().broadcast(UserRemote.setTemporaryStat(chr, stats), chr);
            return;
        }
        try {
            OutPacket p = set(chr.getId(), snapshot(stats), System.currentTimeMillis());
            if (p != null) {
                chr.getField().broadcast(p, chr);
            }
        } catch (RuntimeException e) {
            System.out.println("[TMS282] remote buff set of character " + chr.getId() + " not sent: " + e);
        }
    }

    /** TSM reset -> the others in the field (265: the unchanged 265 packet). */
    public static void broadcastReset(Char chr, java.util.EnumMap<CharacterTemporaryStat, List<Option>> removed) {
        if (chr == null || chr.getField() == null) {
            return;
        }
        if (!active()) {
            chr.getField().broadcast(UserRemote.resetTemporaryStat(chr, removed), chr);
            return;
        }
        try {
            Map<CharacterTemporaryStat, List<Option>> current = chr.getTemporaryStatManager() == null ? Map.of()
                    : snapshot(chr.getTemporaryStatManager().getCurrentStats());
            for (OutPacket p : reset(chr.getId(), snapshot(removed), current, System.currentTimeMillis())) {
                chr.getField().broadcast(p, chr);
            }
        } catch (RuntimeException e) {
            System.out.println("[TMS282] remote buff reset of character " + chr.getId() + " not sent: " + e);
        }
    }

    private static Map<CharacterTemporaryStat, List<Option>> snapshot(Map<CharacterTemporaryStat, List<Option>> m) {
        EnumMap<CharacterTemporaryStat, List<Option>> copy = new EnumMap<>(CharacterTemporaryStat.class);
        if (m != null) {
            for (Map.Entry<CharacterTemporaryStat, List<Option>> e : m.entrySet()) {
                copy.put(e.getKey(), e.getValue() == null ? List.of() : new ArrayList<>(e.getValue()));
            }
        }
        return copy;
    }

    /**
     * LP03D0 #24 source: the character's 282 book (Tms282BuffDirectory) plus what its TSM holds that the remote
     * table can show, so a user entering the field also sees buffs set through the TSM.
     */
    public static Map<CharacterTemporaryStat, List<Option>> activeStats(int characterId) {
        EnumMap<CharacterTemporaryStat, List<Option>> all = new EnumMap<>(CharacterTemporaryStat.class);
        try {
            all.putAll(Tms282BuffDirectory.INSTANCE.activeStats(characterId));
            var book = Tms282BuffDirectory.INSTANCE.book(characterId);
            if (book != null) all.putAll(own(characterId, book.serverSnapshot()));
        } catch (RuntimeException ignored) {
            // no book
        }
        try {
            var world = net.swordie.ms.Server.get().getWorld();
            Char chr = world == null ? null : world.getCharById(characterId);
            if (chr != null && chr.getTemporaryStatManager() != null) {
                all.putAll(own(characterId, snapshot(chr.getTemporaryStatManager().getRemoteStats())));
            }
        } catch (RuntimeException ignored) {
            // the TSM changed while it was read: the book alone
        }
        return all;
    }

    private static void log(String what, TemporaryStatEncoder.Selection sel) {
        for (TemporaryStatEncoder.Skipped s : sel.skipped()) {
            if (LOGGED.add(what + ":" + s.stat())) {
                System.out.printf("[TMS282] remote buff %s not shown to others: %s (%s)%n", what, s.stat(), s.reason());
            }
        }
    }
}
