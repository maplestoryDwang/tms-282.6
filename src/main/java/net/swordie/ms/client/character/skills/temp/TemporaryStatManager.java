package net.swordie.ms.client.character.skills.temp;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.BodyPart;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.skills.*;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.jobs.legend.Shade;
import net.swordie.ms.client.jobs.sengoku.Kanna;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.UserRemote;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.enums.BaseStat;
import net.swordie.ms.enums.TSIndex;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.drop.DropInfo;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.Util;
import net.swordie.ms.util.container.Tuple;
import net.swordie.ms.world.field.Field;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class TemporaryStatManager {

    // The existing dispellable statuses; boss-specific mechanics are deliberately not blanket-immune.
    public static final Set<CharacterTemporaryStat> DISPELLABLE_DEBUFFS = Set.of(
            Stun, Poison, Seal, Darkness, Thaw, Weakness, Curse, Slow, Blind);

    private static boolean isTanjiroWill(CharacterTemporaryStat cts, Option option) {
        return cts == AsrR && (option.rOption == net.swordie.ms.client.jobs.special.SpecialEventJob.TANJIRO_WILL
                || option.rOption == net.swordie.ms.client.jobs.special.SpecialEventJob.SAITAMA_WILL)
                && net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
    }

    public boolean blocksTanjiroWillDebuff(CharacterTemporaryStat cts, long now) {
        if (!DISPELLABLE_DEBUFFS.contains(cts)) return false;
        lock.lock();
        try {
            Option immunity = getOption(AsrR);
            return isTanjiroWill(AsrR, immunity) && immunity.nOption > 0
                    && now >= immunity.startTime && now - immunity.startTime < 3000L;
        } finally {
            lock.unlock();
        }
    }

    private final Lock lock = new ReentrantLock();
    private int pvpDamage;
    private byte defenseState;
    private byte defenseAtt;
    private int[] diceInfo = new int[22];
    private int[] diceOption = new int[8];
    private List<Integer> mobZoneStates;
    private int viperEnergyCharge;
    private StopForceAtom stopForceAtom;
    private LarknessManager larknessManager;
    private final Char chr;
    private List<TemporaryStatBase> twoStates = new ArrayList<>();
    private Set<AffectedArea> affectedAreas = new HashSet<>();
    private Map<BaseStat, Integer> baseStats = new HashMap<>();
    private List<Integer> petPassiveSkills = new ArrayList<>();
    private final Map<CharacterTemporaryStat, List<Option>> currentStats = new ConcurrentHashMap<>();
    private volatile net.swordie.ms.client.character.tms282.Tms282BuffBook book;
    private final Map<CharacterTemporaryStat, List<Option>> bookStats = new EnumMap<>(CharacterTemporaryStat.class);
    private final Map<Option, Option> bookOriginals = new IdentityHashMap<>();
    private long bookRevision;

    public void bindBook(net.swordie.ms.client.character.tms282.Tms282BuffBook value, long revision) {
        lock.lock();
        try {
            if (book == value && revision < bookRevision) return;
            if (book != value) { bookStats.clear(); bookOriginals.clear(); }
            book = value;
            bookRevision = revision;
        } finally { lock.unlock(); }
    }

    public static boolean fixedBuffDuration(CharacterTemporaryStat cts, Option option) {
        return isTanjiroWill(cts, option) || net.swordie.ms.client.jobs.special.SaitamaActionProtection.fixedDuration(cts, option)
                || (net.swordie.ms.client.jobs.Jianghu.Tms282HowlingStorm.handles(option.nReason)
                    && net.swordie.ms.client.jobs.Jianghu.Tms282HowlingStorm.isHeld(option.nReason)
                    && (cts == IndieDamReduceR || cts == IndieAntiMagicShell));
    }

    public void releaseBook(net.swordie.ms.client.character.tms282.Tms282BuffBook value, long revision) {
        lock.lock();
        try {
            if (book == value && revision >= bookRevision) { bookStats.clear(); bookOriginals.clear(); book = null; }
        } finally { lock.unlock(); }
    }

    private static int source(CharacterTemporaryStat cts, Option option) {
        return cts.isIndie() ? option.nReason : option.rOption;
    }

    private boolean bookOwns(CharacterTemporaryStat cts, Option option) {
        return bookStats.getOrDefault(cts, List.of()).stream().anyMatch(o -> source(cts, o) == source(cts, option));
    }

    /** Full snapshots carry a revision: a delayed apply cannot resurrect a buff removed by a newer expiry. */
    public void syncBookStats(net.swordie.ms.client.character.tms282.Tms282BuffBook value, long revision,
                              Map<CharacterTemporaryStat, List<Option>> wanted) {
        Map<CharacterTemporaryStat, List<Option>> added = new EnumMap<>(CharacterTemporaryStat.class);
        Map<CharacterTemporaryStat, List<Option>> removed = new EnumMap<>(CharacterTemporaryStat.class);
        lock.lock();
        try {
            if (book != value || revision < bookRevision) return;
            bookRevision = revision;
            Set<Option> originals = Collections.newSetFromMap(new IdentityHashMap<>());
            wanted.values().forEach(originals::addAll);
            for (var e : bookStats.entrySet()) {
                var cts = e.getKey();
                var current = currentStats.get(cts);
                for (var it = e.getValue().iterator(); it.hasNext();) {
                    Option copy = it.next();
                    if (originals.contains(bookOriginals.get(copy))) continue;
                    bookOriginals.remove(copy);
                    if (current != null && current.removeIf(o -> o == copy)) {
                        BaseStat.getFromCTS(chr, cts, copy).forEach(this::removeBaseStat);
                        // A refresh of the same source is not an end-of-buff callback.
                        if (wanted.getOrDefault(cts, List.of()).stream().noneMatch(o -> source(cts, o) == source(cts, copy)))
                            removed.computeIfAbsent(cts, k -> new ArrayList<>()).add(copy);
                    }
                    it.remove();
                }
                if (current != null && current.isEmpty()) currentStats.remove(cts);
            }
            Set<Option> installed = Collections.newSetFromMap(new IdentityHashMap<>());
            installed.addAll(bookOriginals.values());
            for (var e : wanted.entrySet()) {
                var cts = e.getKey();
                var current = new ArrayList<>(currentStats.getOrDefault(cts, List.of()));
                var owned = bookStats.computeIfAbsent(cts, k -> new ArrayList<>());
                for (Option incoming : e.getValue()) {
                    if (installed.contains(incoming)) continue;
                    for (var it = current.iterator(); it.hasNext();) {
                        Option old = it.next();
                        if (!cts.isIndie() || source(cts, old) == source(cts, incoming)) {
                            BaseStat.getFromCTS(chr, cts, old).forEach(this::removeBaseStat);
                            owned.removeIf(o -> o == old);
                            it.remove();
                        }
                    }
                    Option copy = incoming.deepCopy();
                    current.add(copy);
                    owned.add(copy);
                    bookOriginals.put(copy, incoming);
                    installed.add(incoming);
                    BaseStat.getFromCTS(chr, cts, copy).forEach(this::addBaseStat);
                    added.computeIfAbsent(cts, k -> new ArrayList<>()).add(copy);
                }
                currentStats.put(cts, current);
            }
            bookStats.entrySet().removeIf(e -> e.getValue().isEmpty());
        } finally { lock.unlock(); }
        if (chr.getJobHandler() != null) {
            removed.forEach((cts, options) -> chr.getJobHandler().handleRemoveCTS(cts, options));
            added.forEach((cts, options) -> chr.getJobHandler().handleAddCTS(cts, options));
        }
    }

    /** Called outside the TSM lock after a legacy cancel/removal; book expiry must not announce it again. */
    private void syncRemovedBookStats() {
        net.swordie.ms.client.character.tms282.Tms282BuffBook value;
        Map<CharacterTemporaryStat, List<Option>> removed = new EnumMap<>(CharacterTemporaryStat.class);
        lock.lock();
        try {
            value = book;
            if (value == null) return;
            for (var e : bookStats.entrySet()) {
                var current = currentStats.getOrDefault(e.getKey(), List.of());
                for (var it = e.getValue().iterator(); it.hasNext();) {
                    Option option = it.next();
                    if (current.stream().noneMatch(o -> o == option)) {
                        Option original = bookOriginals.remove(option);
                        if (original != null) removed.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(original);
                        it.remove();
                    }
                }
            }
            bookStats.entrySet().removeIf(e -> e.getValue().isEmpty());
        } finally { lock.unlock(); }
        if (!removed.isEmpty()) value.discardStats(removed);
    }

    public void update(long now) {
        boolean locked = lock.tryLock();
        if (!locked) return;
        var toRemove = new HashSet<CharacterTemporaryStat>();
        var toRemoveIndie = new HashSet<Tuple<CharacterTemporaryStat, Option>>();
        EnumMap<CharacterTemporaryStat, List<Option>> changed = new EnumMap<>(CharacterTemporaryStat.class);
        try {
            if (!currentStats.isEmpty()) {
                for (var entry : currentStats.entrySet()) {
                    var cts = entry.getKey();
                    if (!cts.isIndie() && bookStats.getOrDefault(cts, List.of()).stream().anyMatch(o -> o == entry.getValue().getFirst())) continue;
                    if (cts.isIndie()) {
                        for (var opt : entry.getValue()) {
                            if (bookStats.getOrDefault(cts, List.of()).stream().anyMatch(o -> o == opt)) continue;
                            var time = opt.tOption > 0 ? opt.tOption : opt.tTerm;
                            var endTime = opt.startTime + (opt.isInMillis() ? time : time * 1000L);
                            if (time != 0 && endTime < now) {
                                toRemoveIndie.add(new Tuple<>(cts, opt));
                            }
                        }
                    } else if (TSIndex.isTwoStat(cts)) {
                        var tse = TSIndex.getTSEFromCTS(cts);
                        if (tse != null) {
                            var tsb = getTSBByTSIndex(tse);
                            if (tsb.hasExpired()) {
                                toRemove.add(cts);
                            }
                        }
                    } else {
                        var opt = entry.getValue().getFirst();
                        if (bookStats.getOrDefault(cts, List.of()).stream().anyMatch(o -> o == opt)) continue;
                        var time = opt.tOption > 0 ? opt.tOption : opt.tTerm;
                        var endTime = opt.startTime + (opt.isInMillis() ? time : time * 1000L);
                        if (cts == Arcana && opt.yOption > 0) {
                            // Reuse the character's stat tick: cancellation/recast cannot leave an independent timer alive.
                            boolean hexa = net.swordie.ms.client.jobs.adventurer.magician.Magician.isHexaArcana(opt.rOption);
                            int stage = now >= endTime ? 0 : Math.max(hexa ? 1 : 0, opt.xOption
                                    - (int) (Math.max(0, now - opt.startTime) / opt.yOption));
                            if (stage == 0) {
                                toRemove.add(cts);
                            } else if (stage != opt.nOption) {
                                BaseStat.getFromCTS(chr, cts, opt).forEach(this::removeBaseStat);
                                opt.nOption = stage;
                                BaseStat.getFromCTS(chr, cts, opt).forEach(this::addBaseStat);
                                Option remaining = opt.deepCopy();
                                remaining.tOption = (int) (endTime - now);
                                remaining.isInMillis = true;
                                changed.put(cts, List.of(remaining));
                                if (hexa) {
                                    Option magic = getOptByCTSAndSkill(IndieMADR, opt.rOption);
                                    if (magic != null) {
                                        BaseStat.getFromCTS(chr, IndieMADR, magic).forEach(this::removeBaseStat);
                                        magic.nValue = opt.uOption;
                                        BaseStat.getFromCTS(chr, IndieMADR, magic).forEach(this::addBaseStat);
                                        changed.put(IndieMADR, getOptions(IndieMADR).stream().map(Option::deepCopy).toList());
                                    }
                                }
                            }
                        }
                        if (time != 0 && endTime < now) {
                            toRemove.add(cts);
                        }
                    }
                }
            }
        } finally {
            lock.unlock();
        }
        toRemove.forEach(this::removeStat);
        toRemoveIndie.forEach(tup -> removeIndieStat(tup.getLeft(), tup.getRight()));
        if (changed.containsKey(IndieMADR))
            changed.put(IndieMADR, getOptions(IndieMADR).stream().map(Option::deepCopy).toList());
        if (!changed.isEmpty()) {
            chr.write(WvsContext.temporaryStatSet(this, changed, currentStats.size(), false));
            if (chr.getField() != null) net.swordie.ms.connection.packet.Tms282RemoteBuffs.broadcastSet(chr, changed);
        }
    }

    public TemporaryStatManager(Char chr) {
        this.chr = chr;
        for (CharacterTemporaryStat cts : TSIndex.getAllCTS()) {
            switch (cts) {
                case PartyBooster:
                    twoStates.add(new PartyBooster());
                    break;
                case GuidedBullet:
                    twoStates.add(new GuidedBullet());
                    break;
                case RideVehicleExpire:
                    twoStates.add(new RideVehicleExpireStat());
                    break;
                case SecondAtomLockOn:
                    twoStates.add(new SecondAtomLockOnStat());
                    break;
                case RideVehicle:
                case RelicGauge:
                    twoStates.add(new TwoStateTemporaryStat(false));
                    break;
                default:
                    twoStates.add(new TwoStateTemporaryStat(true));
                    break;
            }
        }
    }

    public boolean isErr38RemoteCTS(CharacterTemporaryStat cts) {
        return false;
    }

    public void updateStat(CharacterTemporaryStat cts, Option o) {
        Option oldOpt = getOptByCTSAndSkill(cts, o.rOption);
        if (oldOpt != null) {
            Option newOpt = oldOpt.deepCopy();
            newOpt.nKey = new Random().nextInt();
            newOpt.tOption = (int) getRemainingTime(cts, o.rOption);
            newOpt.setInMillis(true);
            sendStat(cts, newOpt);
        }
    }

    public void sendStat(EnumMap<CharacterTemporaryStat, Option> statsToUpdate) {
        sendStat(statsToUpdate, false);
    }

    public void sendStat(EnumMap<CharacterTemporaryStat, Option> statsToUpdate, boolean isHideBuff) {
        if (chr == null || chr.getJobHandler() == null) {
            return;
        }
        long buffTimeR = chr.getTotalStat(BaseStat.buffTimeR); // includes the 100% base
        if (buffTimeR == 0) {
            buffTimeR = 100;
        }
        boolean needBuffTimeR = buffTimeR != 100;
        EnumMap<CharacterTemporaryStat, List<Option>> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        EnumMap<CharacterTemporaryStat, List<Option>> newRemoteStats = new EnumMap<>(CharacterTemporaryStat.class);
        int curSize = 0;
        lock.lock();
        try {
            for (Map.Entry<CharacterTemporaryStat, Option> entry : statsToUpdate.entrySet()) {
                CharacterTemporaryStat cts = entry.getKey();
                Option option = entry.getValue();
                if (bookOwns(cts, option)) continue;
                snapshotStopForceAtom(cts, option);
                if (blocksTanjiroWillDebuff(cts, System.currentTimeMillis()) || net.swordie.ms.client.jobs.special.SaitamaActionProtection.blocksDebuff(chr, cts, System.currentTimeMillis())) continue;
                boolean indie = cts.isIndie();
                option.setTimeToMillis();
                if (needBuffTimeR && !fixedBuffDuration(cts, option)) {
                    SkillInfo skillinfo = SkillData.getSkillInfoById(indie ? option.nReason : option.rOption);
                    if (skillinfo != null && !skillinfo.isNotIncBuffDuration()) {
                        int duration = (indie ? option.tTerm : option.tOption);
                        if (indie) {
                            option.tTerm = (int) ((buffTimeR * duration) / 100);
                        } else {
                            option.tOption = (int) ((buffTimeR * duration) / 100);
                        }
                    }
                }
                List<Option> options;
                List<Option> remoteoptions = new ArrayList<>();
                final long now = System.currentTimeMillis();
                option.tStart = (int) now;
                option.startTime = now;
                if (!indie) {
                    List<Option> existing = currentStats.get(cts);
                    if (existing != null && !existing.isEmpty()) {
                        Option oldOption = existing.getFirst();
                        BaseStat.getFromCTS(chr, cts, oldOption).forEach(this::removeBaseStat);
                    }
                    options = new ArrayList<>();
                    options.add(option);
                    if (!isErr38RemoteCTS(cts)) {
                        remoteoptions = options;
                    }
                    currentStats.put(cts, options);
                    BaseStat.getFromCTS(chr, cts, option).forEach(this::addBaseStat);
                } else {
                    options = currentStats.getOrDefault(cts, new ArrayList<>());
                    if (options != null && !options.isEmpty()) {
                        for (Iterator<Option> it = options.iterator(); it.hasNext(); ) {
                            Option oldOption = it.next();
                            if (oldOption != null && oldOption.nReason == option.nReason) {
                                BaseStat.getFromCTS(chr, cts, oldOption).forEach(this::removeBaseStat);
                                it.remove(); // safe
                                break;
                            }
                        }
                    } else if (options == null) {
                        options = new ArrayList<>();
                        currentStats.put(cts, options);
                    }
                    options.add(option);
                    remoteoptions = options;
                    currentStats.put(cts, options);
                    BaseStat.getFromCTS(chr, cts, option).forEach(this::addBaseStat);
                }
                newStats.put(cts, options);
                if (!isErr38RemoteCTS(cts)) {
                    newRemoteStats.put(cts, remoteoptions);
                }
            }
            curSize = currentStats.size();
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT_SKILL, e);
            return;
        } finally {
            lock.unlock();
        }
        syncRemovedBookStats();
        if (!newStats.isEmpty()) {
            for (Map.Entry<CharacterTemporaryStat, Option> entry : statsToUpdate.entrySet()) {
                if (!newStats.containsKey(entry.getKey())) continue;
                chr.getJobHandler().handleAddCTS(entry.getKey(), Collections.singletonList(entry.getValue()));
            }
            chr.write(WvsContext.temporaryStatSet(this, newStats, curSize, isHideBuff));
            if (chr.getField() != null)  net.swordie.ms.connection.packet.Tms282RemoteBuffs.broadcastSet(chr, newRemoteStats); // multiplayer-0930: 282 LP048F/LP0490
        }
    }

    public void sendStat(CharacterTemporaryStat cts, Option option) {
        sendStat(cts, option, false);
    }

    public void sendStat(CharacterTemporaryStat cts, Option option, boolean isHideBuff) {
        if (chr == null || chr.getJobHandler() == null) {
            return;
        }
        long buffTimeR = chr.getTotalStat(BaseStat.buffTimeR); // includes the 100% base
        if (buffTimeR == 0) {
            buffTimeR = 100;
        }
        boolean needBuffTimeR = buffTimeR != 100;
        EnumMap<CharacterTemporaryStat, List<Option>> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        EnumMap<CharacterTemporaryStat, List<Option>> newRemoteStats = new EnumMap<>(CharacterTemporaryStat.class);
        List<Option> options = new ArrayList<>();
        List<Option> remoteoptions = new ArrayList<>();
        int curSize = 0;
        lock.lock();
        try {
            if (bookOwns(cts, option)) return;
            snapshotStopForceAtom(cts, option);
            if (blocksTanjiroWillDebuff(cts, System.currentTimeMillis()) || net.swordie.ms.client.jobs.special.SaitamaActionProtection.blocksDebuff(chr, cts, System.currentTimeMillis())) return;
            boolean indie = cts.isIndie();
            option.setTimeToMillis();
            if (needBuffTimeR && !fixedBuffDuration(cts, option)) {
                SkillInfo skillinfo = SkillData.getSkillInfoById(indie ? option.nReason : option.rOption);
                if (skillinfo != null && !skillinfo.isNotIncBuffDuration()) {
                    int duration = (indie ? option.tTerm : option.tOption);
                    if (indie) {
                        option.tTerm = (int) ((buffTimeR * duration) / 100);
                    } else {
                        option.tOption = (int) ((buffTimeR * duration) / 100);
                    }
                }
            }
            final long now = System.currentTimeMillis();
            option.tStart = (int) now;
            option.startTime = now;
            if (!indie) {
                List<Option> existing = currentStats.get(cts);
                if (existing != null && !existing.isEmpty()) {
                    Option oldOption = existing.getFirst();
                    BaseStat.getFromCTS(chr, cts, oldOption).forEach(this::removeBaseStat);
                }
                options = new ArrayList<>();
                options.add(option);
                if (!isErr38RemoteCTS(cts)) {
                    remoteoptions = options;
                }
                currentStats.put(cts, options);
                BaseStat.getFromCTS(chr, cts, option).forEach(this::addBaseStat);
            } else {
                options = currentStats.getOrDefault(cts, new ArrayList<>());
                if (options != null && !options.isEmpty()) {
                    for (Iterator<Option> it = options.iterator(); it.hasNext(); ) {
                        Option oldOption = it.next();
                        if (oldOption != null && oldOption.nReason == option.nReason) {
                            BaseStat.getFromCTS(chr, cts, oldOption).forEach(this::removeBaseStat);
                            it.remove(); // safe
                            break;
                        }
                    }
                } else if (options == null) {
                    options = new ArrayList<>();
                    currentStats.put(cts, options);
                }
                options.add(option);
                remoteoptions = options;
                currentStats.put(cts, options);
                BaseStat.getFromCTS(chr, cts, option).forEach(this::addBaseStat);
            }
            newStats.put(cts, options);
            if (!isErr38RemoteCTS(cts)) {
                newRemoteStats.put(cts, remoteoptions);
            }
            curSize = currentStats.size();
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT_SKILL, e);
            return;
        } finally {
            lock.unlock();
        }
        syncRemovedBookStats();
        if (!newStats.isEmpty()) {
            chr.getJobHandler().handleAddCTS(cts, options);
            chr.write(WvsContext.temporaryStatSet(this, newStats, curSize, isHideBuff));
            if (chr.getField() != null)  net.swordie.ms.connection.packet.Tms282RemoteBuffs.broadcastSet(chr, newRemoteStats); // multiplayer-0930: 282 LP048F/LP0490
        }
    }

    public void removeStat(CharacterTemporaryStat cts) {
        if (chr == null || chr.getJobHandler() == null) {
            return;
        }
        EnumMap<CharacterTemporaryStat, List<Option>> expiredStats = new EnumMap<>(CharacterTemporaryStat.class);
        List<Option> oldOptions;
        List<Option> options;
        int curSize = 0;
        lock.lock();
        try {
            if (!currentStats.containsKey(cts)) {
                return;
            }
            oldOptions = currentStats.get(cts);
            options = currentStats.remove(cts);
            expiredStats.put(cts, options);
            for (Option option : options) BaseStat.getFromCTS(chr, cts, option).forEach(this::removeBaseStat);
            curSize = currentStats.size();
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT_SKILL, e);
            return;
        } finally {
            lock.unlock();
        }
        syncRemovedBookStats();
        if (!expiredStats.isEmpty()) {
            chr.getJobHandler().handleRemoveCTS(cts, oldOptions);
            if (TSIndex.isTwoStat(cts)) {
                TSIndex tsIndex = TSIndex.getTSEFromCTS(cts);
                if (tsIndex != null) {
                    getTSBByTSIndex(tsIndex).reset();
                }
            }
            chr.write(WvsContext.temporaryStatReset(chr.getTemporaryStatManager(), expiredStats, curSize));
            if (chr.getField() != null)  net.swordie.ms.connection.packet.Tms282RemoteBuffs.broadcastReset(chr, expiredStats); // multiplayer-0930: 282 LP048F/LP0490
            net.swordie.ms.client.character.tms282.Tms282DragonMaster.afterReset(chr, expiredStats, System.currentTimeMillis());
        }
    }

    public void removeStat(List<CharacterTemporaryStat> ctsList) {
        if (chr == null || chr.getJobHandler() == null) {
            return;
        }
        EnumMap<CharacterTemporaryStat, List<Option>> expiredStats = new EnumMap<>(CharacterTemporaryStat.class);
        int curSize = 0;
        lock.lock();
        try {
            for (CharacterTemporaryStat cts : ctsList) {
                if (!currentStats.containsKey(cts)) {
                    continue;
                }
                List<Option> options = currentStats.remove(cts);
                expiredStats.put(cts, options);
                for (Option option : options) BaseStat.getFromCTS(chr, cts, option).forEach(this::removeBaseStat);
            }
            curSize = currentStats.size();
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT_SKILL, e);
            return;
        } finally {
            lock.unlock();
        }
        syncRemovedBookStats();
        if (!expiredStats.isEmpty()) {
            for (CharacterTemporaryStat cts : ctsList) {
                chr.getJobHandler().handleRemoveCTS(cts, expiredStats.getOrDefault(cts, new ArrayList<>()));
                if (TSIndex.isTwoStat(cts)) {
                    TSIndex tsIndex = TSIndex.getTSEFromCTS(cts);
                    if (tsIndex != null) {
                        getTSBByTSIndex(tsIndex).reset();
                    }
                }
            }
            chr.write(WvsContext.temporaryStatReset(chr.getTemporaryStatManager(), expiredStats, curSize));
            if (chr.getField() != null)  net.swordie.ms.connection.packet.Tms282RemoteBuffs.broadcastReset(chr, expiredStats); // multiplayer-0930: 282 LP048F/LP0490
            net.swordie.ms.client.character.tms282.Tms282DragonMaster.afterReset(chr, expiredStats, System.currentTimeMillis());
        }
    }

    public void removeIndieStat(CharacterTemporaryStat cts, Option option) {
        if (chr == null || chr.getJobHandler() == null) return;

        EnumMap<CharacterTemporaryStat, List<Option>> resetStats = new EnumMap<>(CharacterTemporaryStat.class);
        List<Summon> summons = new ArrayList<>();
        AtomicLong startTime = new AtomicLong();

        List<Option> oldListForHandler;
        int curSize;

        lock.lock();
        try {
            List<Option> list = currentStats.get(cts);
            if (list == null || list.isEmpty()) return;

            if (!(list instanceof ArrayList)) {
                list = new ArrayList<>(list);
                currentStats.put(cts, list);
            }

            // ✅ OLD list snapshot for handleRemoveCTS
            oldListForHandler = new ArrayList<>(list);

            boolean removed = false;

            Iterator<Option> it = list.iterator();
            while (it.hasNext()) {
                Option o = it.next();
                if (o == null) continue;

                if (o == option) {
                    if (o.summon != null && o.nReason != Shade.SPIRIT_BOND_MAX_2) {
                        if (o.nReason == Kanna.GHOST_YAKSHA_TRAINEE
                                || o.nReason == Kanna.GHOST_YAKSHA_BROTHER
                                || o.nReason == Kanna.GHOST_YAKSHA_LIEUTENANT
                                || o.nReason == Kanna.GHOST_YAKSHA_BOSS) {
                            startTime.set(o.startTime);
                        }
                        summons.add(o.summon);
                    }

                    // base stat remove for this option
                    for (Map.Entry<BaseStat, Integer> e : BaseStat.getFromCTS(chr, cts, o).entrySet()) {
                        removeBaseStat(e.getKey(), e.getValue());
                    }

                    it.remove();
                    removed = true;
                    break;
                }
            }

            if (removed) {
                // ✅ reset packet for INDIE wants "remaining list"
                if (list.isEmpty()) {
                    currentStats.remove(cts);
                    resetStats.put(cts, List.of());
                } else {
                    resetStats.put(cts, new ArrayList<>(list));
                }
            }
            curSize = currentStats.size();
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT_SKILL, e);
            return;
        } finally {
            lock.unlock();
        }
        if (resetStats.isEmpty()) {
            return;
        }
        syncRemovedBookStats();
        chr.getJobHandler().handleRemoveCTS(cts, oldListForHandler);
        chr.write(WvsContext.temporaryStatReset(chr.getTemporaryStatManager(), resetStats, curSize));
        if (chr.getField() != null)  net.swordie.ms.connection.packet.Tms282RemoteBuffs.broadcastReset(chr, resetStats); // multiplayer-0930: 282 LP048F/LP0490
        if (!summons.isEmpty()) {
            for (Summon s : summons) {
                Field f = s.getField();
                if (f != null) f.removeLife(s);
            }
            if (chr.getJobHandler() instanceof Kanna kanna) {
                if (startTime.get() != 0) {
                    kanna.giveKannaFifthAttract(startTime.get());
                }
            }
        }
    }

    public Option getOptByCTSAndSkill(CharacterTemporaryStat cts, int skillID) {
        lock.lock();
        try {
            if (currentStats.containsKey(cts)) {
                List<Option> options = currentStats.get(cts);
                if (options != null && !options.isEmpty()) {
                    if (cts.isIndie()) {
                        for (Option o : options) {
                            if (o != null && o.nReason == skillID) {
                                return o;
                            }
                        }
                    } else {
                        return options.getFirst();
                    }
                }
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    public boolean hasStat(CharacterTemporaryStat cts) {
        lock.lock();
        try {
            return currentStats.containsKey(cts);
        } finally {
            lock.unlock();
        }
    }

    public Option getOption(CharacterTemporaryStat cts) {
        lock.lock();
        try {
            if (!cts.isIndie() && currentStats.containsKey(cts)) {
                List<Option> options = currentStats.get(cts);
                if (options != null && !options.isEmpty()) {
                    return options.getFirst();
                }
            }
            return new Option();
        } finally {
            lock.unlock();
        }
    }

    public List<Option> getOptions(CharacterTemporaryStat cts) {
        lock.lock();
        try {
            return currentStats.getOrDefault(cts, new ArrayList<>()); // Returns the existing list or a new one
        } finally {
            lock.unlock();
        }
    }

    public void removeCheckByTime() {
        removeStat(new ArrayList<>(RESET_BY_TIME_CTS.stream().toList()));
    }

    public boolean hasStatBySkillId(int skillId) {
        lock.lock();
        try {
            if (currentStats.isEmpty()) {
                return false;
            }
            for (List<Option> list : currentStats.values()) {
                if (list == null) {
                    continue;
                }
                for (int i = 0, n = list.size(); i < n; i++) {
                    Option o = list.get(i);
                    if (o != null && (o.rOption == skillId || o.nReason == skillId)) {
                        return true;
                    }
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public void removeAllStats() {
        EnumMap<CharacterTemporaryStat, List<Option>> allStats = new EnumMap<>(CharacterTemporaryStat.class);
        Set<Summon> summons = new HashSet<>();
        AtomicLong startTime = new AtomicLong();
        lock.lock();
        try {
            for (Map.Entry<CharacterTemporaryStat, List<Option>> entry : currentStats.entrySet()) {
                CharacterTemporaryStat cts = entry.getKey();
                List<Option> options = entry.getValue();
                options.forEach(o -> {
                    BaseStat.getFromCTS(chr, cts, o).forEach(this::removeBaseStat);
                    if (o.summon != null) {
                        if (o.nReason == Kanna.GHOST_YAKSHA_TRAINEE
                                || o.nReason == Kanna.GHOST_YAKSHA_BROTHER
                                || o.nReason == Kanna.GHOST_YAKSHA_LIEUTENANT
                                || o.nReason == Kanna.GHOST_YAKSHA_BOSS) {
                            startTime.set(o.startTime);
                        }
                        summons.add(o.summon);
                    }
                });
            }
            allStats.putAll(currentStats);
            currentStats.clear();
            affectedAreas.clear();
            baseStats.clear();
        } finally {
            lock.unlock();
        }
        syncRemovedBookStats();
        if (!allStats.isEmpty()) {
            chr.write(WvsContext.temporaryStatReset(chr.getTemporaryStatManager(), allStats, 0));
            if (chr.getField() != null)  net.swordie.ms.connection.packet.Tms282RemoteBuffs.broadcastReset(chr, allStats); // multiplayer-0930: 282 LP048F/LP0490
        }
        if (!summons.isEmpty()) {
            summons.forEach(s -> {
                Field field = s.getField();
                if (field != null) {
                    field.removeLife(s);
                }
            });
            if (chr.getJobHandler() instanceof Kanna kanna) {
                if (startTime.get() != 0) {
                    kanna.giveKannaFifthAttract(startTime.get());
                }
            }
        }
    }

    public void removeStatsBySkill(int skillId) {
        EnumMap<CharacterTemporaryStat, List<Option>> resetStats = new EnumMap<>(CharacterTemporaryStat.class);
        Map<CharacterTemporaryStat, List<Option>> oldStatsForHandler = new EnumMap<>(CharacterTemporaryStat.class);

        Set<Summon> summons = new HashSet<>();
        AtomicLong startTime = new AtomicLong();

        int curSize;

        lock.lock();
        try {
            for (Iterator<Map.Entry<CharacterTemporaryStat, List<Option>>> it = currentStats.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<CharacterTemporaryStat, List<Option>> e = it.next();
                CharacterTemporaryStat cts = e.getKey();
                List<Option> list = e.getValue();
                if (list == null || list.isEmpty()) {
                    it.remove();
                    continue;
                }

                // ensure mutable (tránh trường hợp list bị put immutable ở đâu đó)
                if (!(list instanceof ArrayList)) {
                    list = new ArrayList<>(list);
                    e.setValue(list);
                }

                final boolean indie = cts.isIndie();

                // snapshot OLD list cho handleRemoveCTS (theo yêu cầu bạn)
                List<Option> oldList = null;

                // list removed (cho non-indie reset, và để biết có thay đổi hay không)
                List<Option> removed = null;

                for (Iterator<Option> optIt = list.iterator(); optIt.hasNext(); ) {
                    Option o = optIt.next();
                    if (o == null) continue;

                    boolean match = indie ? (o.nReason == skillId) : (o.rOption == skillId);
                    if (!match) continue;

                    if (oldList == null) oldList = new ArrayList<>(list); // chụp đúng trước lần xoá đầu tiên
                    if (removed == null) removed = new ArrayList<>();
                    removed.add(o);

                    // remove base stat
                    for (Map.Entry<BaseStat, Integer> bs : BaseStat.getFromCTS(chr, cts, o).entrySet()) {
                        removeBaseStat(bs.getKey(), bs.getValue());
                    }

                    // summons collect
                    if (o.summon != null && o.nReason != Shade.SPIRIT_BOND_MAX_2) {
                        if (o.nReason == Kanna.GHOST_YAKSHA_TRAINEE
                                || o.nReason == Kanna.GHOST_YAKSHA_BROTHER
                                || o.nReason == Kanna.GHOST_YAKSHA_LIEUTENANT
                                || o.nReason == Kanna.GHOST_YAKSHA_BOSS) {
                            startTime.set(o.startTime);
                        }
                        summons.add(o.summon);
                    }

                    optIt.remove(); // ✅ remove thật trong currentStats
                }

                if (removed != null && !removed.isEmpty()) {
                    // old list cho handler
                    oldStatsForHandler.put(cts, oldList);

                    // reset packet
                    if (indie) {
                        // The TMS codec validates removed sources and obtains remaining options separately.
                        // 265's encoder still consumes the historical remaining-list representation.
                        boolean tms282 = net.swordie.ms.connection.protocol.Protocol.active().version()
                                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
                        resetStats.put(cts, tms282 ? removed : list.isEmpty() ? List.of() : new ArrayList<>(list));
                    } else {
                        // non-indie reset = removed list
                        resetStats.put(cts, removed);
                    }
                }

                if (list.isEmpty()) {
                    it.remove();
                }
            }

            // remove affected areas by skill
            for (Iterator<AffectedArea> aaIt = affectedAreas.iterator(); aaIt.hasNext(); ) {
                AffectedArea aa = aaIt.next();
                if (aa != null && aa.getSkillID() == skillId) {
                    aaIt.remove();
                }
            }

            curSize = currentStats.size();
        } finally {
            lock.unlock();
        }

        // after unlock: handler must receive OLD list beside CTS
        syncRemovedBookStats();
        for (var e : oldStatsForHandler.entrySet()) {
            CharacterTemporaryStat cts = e.getKey();
            chr.getJobHandler().handleRemoveCTS(cts, e.getValue());
            if (TSIndex.isTwoStat(cts)) {
                TSIndex tsIndex = TSIndex.getTSEFromCTS(cts);
                if (tsIndex != null) {
                    getTSBByTSIndex(tsIndex).reset();
                }
            }
        }

        if (!resetStats.isEmpty()) {
            chr.write(WvsContext.temporaryStatReset(chr.getTemporaryStatManager(), resetStats, curSize));
            if (chr.getField() != null)  net.swordie.ms.connection.packet.Tms282RemoteBuffs.broadcastReset(chr, resetStats); // multiplayer-0930: 282 LP048F/LP0490
            net.swordie.ms.client.character.tms282.Tms282DragonMaster.afterReset(chr, resetStats, System.currentTimeMillis());
        }

        if (!summons.isEmpty()) {
            for (Summon s : summons) {
                Field field = s.getField();
                if (field != null) field.removeLife(s);
            }
            if (chr.getJobHandler() instanceof Kanna kanna) {
                if (startTime.get() != 0) {
                    kanna.giveKannaFifthAttract(startTime.get());
                } else if (skillId == Kanna.GHOST_YAKSHA_GREAT_ONI_LORD_LEGION) {
                    removeStat(KannaFifthAttract);
                }
            }
        }
    }

    public void removeDebuffs(boolean removeAll) {
        List<CharacterTemporaryStat> removeList = new ArrayList<>();
        Set<CharacterTemporaryStat> debuffs = DISPELLABLE_DEBUFFS;
        lock.lock();
        try {
            for (CharacterTemporaryStat cts : debuffs) {
                if (currentStats.containsKey(cts)) {
                    removeList.add(cts);
                    if (!removeAll) {
                        break;
                    }
                }
            }
        } finally {
            lock.unlock();
        }
        if (!removeList.isEmpty()) {
            removeStat(removeList);
        }
    }

    public List<TemporaryStatBase> getTwoStates() {
        return twoStates;
    }

    public TemporaryStatBase getTSBByTSIndex(TSIndex tsi) {
        return getTwoStates().get(tsi.getIndex());
    }

    public boolean hasNewStat(EnumMap<CharacterTemporaryStat, List<Option>> newStats, CharacterTemporaryStat cts) {
        return newStats.containsKey(cts);
    }

    public Option getOption(EnumMap<CharacterTemporaryStat, List<Option>> currentStats, CharacterTemporaryStat cts) {
        if (currentStats.containsKey(cts)) {
            if (!currentStats.get(cts).isEmpty()) {
                return currentStats.get(cts).getFirst();
            }
        }
        return new Option();
    }

    public long getRemainingTime(CharacterTemporaryStat cts, int skillId) {
        Option o = getOptByCTSAndSkill(cts, skillId);
        if (o != null) {
            return o.startTime + (long) (cts.isIndie() ? o.tTerm : o.tOption) * (o.isInMillis ? 1 : 1000) - System.currentTimeMillis();
        }
        return 0;
    }

    public long getRemainingTime(CharacterTemporaryStat cts) {
        Option o = getOption(cts);
        if (o != null) {
            return o.startTime + (long) (cts.isIndie() ? o.tTerm : o.tOption) * (o.isInMillis ? 1 : 1000) - System.currentTimeMillis();
        }
        return 0;
    }

    public int[] getMaskByCollection(Map<CharacterTemporaryStat, List<Option>> map) {
        int[] res = new int[CharacterTemporaryStat.length];
        for (CharacterTemporaryStat cts : map.keySet()) {
            res[cts.getPos()] |= cts.getVal();
        }
        return res;
    }

    public void encodeForLocal(OutPacket outPacket) {
        EnumMap<CharacterTemporaryStat, List<Option>> stats = new EnumMap<>(CharacterTemporaryStat.class);
        stats.putAll(currentStats);
        encodeForLocal(outPacket, stats);
    }

    public void encodeForLocal(OutPacket outPacket, EnumMap<CharacterTemporaryStat, List<Option>> newStats) {
        int[] mask = getMaskByCollection(newStats);
        for (int j : mask) {
            outPacket.encodeInt(j);
        }
        List<CharacterTemporaryStat> orderedAndFilteredCtsList = new ArrayList<>(newStats.keySet()).stream().filter(cts -> cts != null && cts.getOrder() != -1).sorted(Comparator.comparingInt(CharacterTemporaryStat::getOrder)).toList();
        for (CharacterTemporaryStat cts : orderedAndFilteredCtsList) {
            if (cts.getOrder() != -1) {
                Option o = getOption(newStats, cts);
                if (cts == ReturnTeleport) {
                    outPacket.encodeShort(chr.getPosition().getY());
                    outPacket.encodeShort(chr.getPosition().getX());
                } else if (cts.isEncodeInt() || SkillConstants.isEncode4Reason(o.rOption)) {
                    outPacket.encodeInt(o.nOption);
                } else {
                    outPacket.encodeShort(o.nOption);
                }
                outPacket.encodeInt(o.rOption);
                outPacket.encodeInt(o.tOption);
            }
        }
        if (hasNewStat(newStats, SoulMP)) {
            outPacket.encodeInt(getOption(newStats, SoulMP).xOption);
            outPacket.encodeInt(getOption(newStats, SoulMP).rOption);
        }
        if (hasNewStat(newStats, FullSoulMP)) {
            outPacket.encodeInt(getOption(newStats, FullSoulMP).xOption);
        }
        short size = 0;
        outPacket.encodeShort(size);
        for (int i = 0; i < size; i++) {
            outPacket.encodeInt(0); // nKey
            outPacket.encodeByte(0); // bEnable
        }
        if (hasNewStat(newStats, BladeStanceMode)) {
            outPacket.encodeInt(getOption(newStats, BladeStanceMode).xOption);
        }
        outPacket.encodeByte(getDefenseAtt());
        outPacket.encodeByte(getDefenseState());
        outPacket.encodeByte(getPvpDamage());
        outPacket.encodeInt(getOption(newStats, EtherealForm).xOption); // new 199  Red Blue Green, used in Ethereal Form  so far
        if (hasNewStat(newStats, Dice)) {
            for (int i = 0; i < getDiceInfo().length; i++) {
                outPacket.encodeInt(diceInfo[i]);
            }
        }
        if (hasNewStat(newStats, BlackMageCreate)) {
            outPacket.encodeInt(getOption(newStats, BlackMageCreate).xOption);
        }
        if (hasNewStat(newStats, BlackMageDestroy)) {
            outPacket.encodeInt(getOption(newStats, BlackMageDestroy).xOption);
        }
        if (hasNewStat(newStats, BigBangAttackCharge)) {
            outPacket.encodeInt(getOption(newStats, BigBangAttackCharge).xOption);
        }
        if (hasNewStat(newStats, KeyDownMoving)) {
            outPacket.encodeInt(getOption(newStats, KeyDownMoving).xOption);
        }
        if (hasNewStat(newStats, PinkbeanRollingGrade)) {
            outPacket.encodeByte(getOption(newStats, PinkbeanRollingGrade).nOption);
        }
        if (hasNewStat(newStats, Judgement)) {
            outPacket.encodeInt(getOption(newStats, Judgement).xOption);
        }
        if (hasNewStat(newStats, Infinity)) { // 219 correct
            outPacket.encodeByte(getOption(newStats, Infinity).xOption);
        }
        if (hasNewStat(newStats, StackBuff)) {
            outPacket.encodeByte(getOption(newStats, StackBuff).mOption);
        }
        if (hasNewStat(newStats, Trinity)) {
            outPacket.encodeByte(getOption(newStats, Trinity).mOption);
        }
        if (hasNewStat(newStats, ElementalCharge)) {
            outPacket.encodeByte(getOption(newStats, ElementalCharge).xOption);
            outPacket.encodeShort(getOption(newStats, ElementalCharge).yOption);
            outPacket.encodeByte(getOption(newStats, ElementalCharge).uOption);
            outPacket.encodeByte(getOption(newStats, ElementalCharge).wOption);
        }
        if (hasNewStat(newStats, LifeTidal)) {
            outPacket.encodeInt(getOption(newStats, LifeTidal).mOption);
        }
        if (hasNewStat(newStats, AntiMagicShell)) {
            outPacket.encodeByte(getOption(newStats, AntiMagicShell).bOption);
            outPacket.encodeInt(getOption(newStats, AntiMagicShell).tOption);
        }
        if (hasNewStat(newStats, Larkness)) {
            getLarknessManager().getDarkInfo().encode(outPacket);
            getLarknessManager().getLightInfo().encode(outPacket);
            getLarknessManager().encode(outPacket);
        }
        if (hasNewStat(newStats, IgnoreTargetDEF)) {
            outPacket.encodeInt(getOption(newStats, IgnoreTargetDEF).mOption);
        }
        if (hasNewStat(newStats, StrikerElectricUsed)) {
            outPacket.encodeByte(getOption(newStats, StrikerElectricUsed).xOption);
        }
        if (hasNewStat(newStats, StrikerElectricStack)) {
            outPacket.encodeByte(getOption(newStats, StrikerElectricStack).xOption);
        }
        if (hasNewStat(newStats, StopForceAtomInfo)) {
            getStopForceAtom().encode(outPacket);
        }
        if (hasNewStat(newStats, SmashStack)) {
            outPacket.encodeInt(getOption(newStats, SmashStack).xOption);
            outPacket.encodeInt(getOption(newStats, SmashStack).yOption);
            outPacket.encodeInt(getOption(newStats, SmashStack).tOption); // duration? (new v210?)
        }
        if (hasNewStat(newStats, MobZoneState)) {
            for (int zoneState : getMobZoneStates()) {
                outPacket.encodeInt(zoneState);
            }
            outPacket.encodeInt(0); // notify end
        }
        if (hasNewStat(newStats, NextSpecificSkillDamageUp)) {
            outPacket.encodeInt(getOption(newStats, NextSpecificSkillDamageUp).skillIds.size());
            for (Integer i : getOption(newStats, NextSpecificSkillDamageUp).skillIds) {
                outPacket.encodeInt(i);
            }
        }
        if (hasNewStat(newStats, Slow)) {
            outPacket.encodeByte(getOption(newStats, Slow).bOption);
        }
        if (hasNewStat(newStats, IgnoreMobpdpR)) {
            outPacket.encodeByte(getOption(newStats, IgnoreMobpdpR).bOption);
        }
        if (hasNewStat(newStats, BdR)) {
            outPacket.encodeByte(getOption(newStats, BdR).bOption);
        }
        if (hasNewStat(newStats, DropRIncrease)) {
            outPacket.encodeInt(getOption(newStats, DropRIncrease).xOption);
            outPacket.encodeByte(getOption(newStats, DropRIncrease).bOption);
        }
        if (hasNewStat(newStats, PoseType)) {
            outPacket.encodeByte(getOption(newStats, PoseType).bOption);
            outPacket.encodeByte(getOption(newStats, PoseType).sOption);
        }
        if (hasNewStat(newStats, Beholder)) {
            outPacket.encodeInt(getOption(newStats, Beholder).sOption);
        }
        if (hasNewStat(newStats, CrossOverChain)) {
            outPacket.encodeInt(getOption(newStats, CrossOverChain).xOption);
        }
        if (hasNewStat(newStats, ImmuneBarrier)) {
            outPacket.encodeInt(getOption(newStats, ImmuneBarrier).xOption);
        }
        if (hasNewStat(newStats, Stance)) {
            outPacket.encodeInt(getOption(newStats, Stance).xOption);
        }
        if (hasNewStat(newStats, SharpEyes)) {
            outPacket.encodeInt(getOption(newStats, SharpEyes).mOption);
        }
        if (hasNewStat(newStats, AdvancedBless)) {
            outPacket.encodeInt(getOption(newStats, AdvancedBless).xOption);
            outPacket.encodeInt(getOption(newStats, AdvancedBless).yOption);
        }
        if (hasNewStat(newStats, UsefulAdvancedBless)) {
            outPacket.encodeInt(getOption(newStats, UsefulAdvancedBless).xOption);
        }
        if (hasNewStat(newStats, Poison)) {
            outPacket.encodeInt(getOption(newStats, Poison).xOption);
        }
        if (hasNewStat(newStats, SoulExalt)) {
            outPacket.encodeInt(getOption(newStats, SoulExalt).xOption);
        }
        if (hasNewStat(newStats, Bless)) {
            outPacket.encodeInt(getOption(newStats, Bless).xOption);
        }
        if (hasNewStat(newStats, DotHealHPPerSecond)) {
            outPacket.encodeInt(getOption(newStats, DotHealHPPerSecond).xOption);
        }
        if (hasNewStat(newStats, DotHealMPPerSecond)) {
            outPacket.encodeInt(getOption(newStats, DotHealMPPerSecond).xOption);
        }
        if (hasNewStat(newStats, EunwolUnleashFoxOrb)) {
            outPacket.encodeInt(getOption(newStats, EunwolUnleashFoxOrb).xOption);
        }
        if (hasNewStat(newStats, SpiritGuard)) {
            outPacket.encodeInt(getOption(newStats, SpiritGuard).nOption);
        }
        if (hasNewStat(newStats, MastemaGuard)) {
            outPacket.encodeInt(getOption(MastemaGuard).xOption);
        }
        if (hasNewStat(newStats, KnockBack)) {
            outPacket.encodeInt(getOption(newStats, KnockBack).nOption);
            outPacket.encodeInt(getOption(newStats, KnockBack).bOption);
        }
        if (hasNewStat(newStats, ShieldAttack)) {
            outPacket.encodeInt(getOption(newStats, ShieldAttack).xOption);
        }
        if (hasNewStat(newStats, SSFShootingAttack)) {
            outPacket.encodeInt(getOption(newStats, SSFShootingAttack).xOption);
        }
        if (hasNewStat(newStats, BattlePvP_Helena_Mark)) {
            outPacket.encodeInt(getOption(newStats, BattlePvP_Helena_Mark).cOption);
        }
        if (hasNewStat(newStats, BattlePvP_Darklord_Explosion)) {
            outPacket.encodeInt(getOption(BattlePvP_Darklord_Explosion).xOption);
        }
        if (hasNewStat(newStats, PinkbeanAttackBuff)) {
            outPacket.encodeInt(getOption(newStats, PinkbeanAttackBuff).bOption);
        }
        if (hasNewStat(newStats, RoyalGuardState)) {
            outPacket.encodeInt(getOption(newStats, RoyalGuardState).bOption);
            outPacket.encodeInt(getOption(newStats, RoyalGuardState).xOption);
        }
        if (hasNewStat(newStats, MichaelSoulLink)) {
            outPacket.encodeInt(getOption(newStats, MichaelSoulLink).xOption);
            outPacket.encodeByte(getOption(newStats, MichaelSoulLink).bOption);
            outPacket.encodeInt(getOption(newStats, MichaelSoulLink).cOption);
            outPacket.encodeInt(getOption(newStats, MichaelSoulLink).yOption);
        }
        if (hasNewStat(newStats, RWCylinder)) {
            outPacket.encodeByte(getOption(newStats, RWCylinder).bOption);
            outPacket.encodeShort(getOption(newStats, RWCylinder).cOption);
            outPacket.encodeByte(getOption(newStats, RWCylinder).xOption);
        }
        if (hasNewStat(newStats, HitStackDamR)) {
            outPacket.encodeInt(getOption(HitStackDamR).xOption);
        }
        if (hasNewStat(newStats, RWMagnumBlow)) {
            outPacket.encodeShort(getOption(newStats, RWMagnumBlow).bOption);
            outPacket.encodeByte(getOption(newStats, RWMagnumBlow).xOption);
        }
        if (hasNewStat(newStats, BladeStance)) {
            outPacket.encodeInt(getOption(newStats, BladeStance).xOption);
        }
        if (hasNewStat(newStats, DarkSight)) {
            outPacket.encodeInt(getOption(newStats, DarkSight).cOption);
            outPacket.encodeInt(getOption(newStats, DarkSight).xOption);
            outPacket.encodeInt(getOption(newStats, DarkSight).yOption);
        }
        if (hasNewStat(newStats, Stigma)) {
            outPacket.encodeInt(getOption(newStats, Stigma).bOption);
        }
        if (hasNewStat(newStats, TempSecondaryStat)) {
            outPacket.encodeInt(getOption(newStats, TempSecondaryStat).xOption);
        }
        if (hasNewStat(newStats, CriticalGrowing)) {
            outPacket.encodeInt(getOption(newStats, CriticalGrowing).xOption);
        }
        if (hasNewStat(newStats, FlameDischarge)) {
            outPacket.encodeInt(getOption(newStats, FlameDischarge).xOption);
        }
        if (hasNewStat(newStats, PickPocket)) {
            outPacket.encodeInt(getOption(newStats, PickPocket).xOption);
        }
        if (hasNewStat(newStats, PairingUser)) {
            outPacket.encodeShort(getOption(newStats, PairingUser).xOption);
        }
        if (hasNewStat(newStats, Frenzy)) {
            outPacket.encodeShort(getOption(newStats, Frenzy).xOption);
        }
        if (hasNewStat(newStats, ShadowSpear)) {
            outPacket.encodeShort(getOption(newStats, ShadowSpear).xOption);
        }
        if (hasNewStat(newStats, Michael_RhoAias)) {
            outPacket.encodeInt(getOption(newStats, Michael_RhoAias).xOption);
            outPacket.encodeInt(getOption(newStats, Michael_RhoAias).bOption);
            outPacket.encodeInt(getOption(newStats, Michael_RhoAias).cOption);
            outPacket.encodeInt(getOption(newStats, Michael_RhoAias).yOption);
        }
        if (hasNewStat(newStats, VampDeath)) {
            outPacket.encodeInt(getOption(newStats, VampDeath).xOption);
        }
        if (hasNewStat(newStats, HolyMagicShell)) {
            outPacket.encodeInt(getOption(newStats, HolyMagicShell).xOption);
        }
        for (int i = 0; i < TSIndex.values().length; i++) {
            if (hasNewStat(newStats, TSIndex.getCTSFromTwoStatIndex(i))) {
                getTwoStates().get(i).encode(outPacket);
            }
        }
        encodeIndieTempStat(outPacket, newStats);
        if (hasNewStat(newStats, UsingScouter)) {
            outPacket.encodeInt(getOption(newStats, UsingScouter).nOption);
            outPacket.encodeInt(getOption(newStats, UsingScouter).xOption);
        }
        if (hasNewStat(newStats, OutSide)) {
            outPacket.encodeInt(getOption(newStats, OutSide).xOption);
        }
        if (hasNewStat(newStats, LefGloryWing)) {
            outPacket.encodeInt(getOption(newStats, LefGloryWing).xOption);
            outPacket.encodeInt(getOption(newStats, LefGloryWing).cOption);
        }
        if (hasNewStat(newStats, LefBuffMastery)) {
            outPacket.encodeInt(getOption(newStats, LefBuffMastery).xOption);
            outPacket.encodeInt(getOption(newStats, LefBuffMastery).cOption);
        }
        if (hasNewStat(newStats, Shadower_Assassination)) {
            outPacket.encodeInt(getOption(newStats, Shadower_Assassination).xOption);
        }
        if (hasNewStat(newStats, SixthAssassination)) {
            outPacket.encodeInt(getOption(newStats, SixthAssassination).xOption);
        }
        if (hasNewStat(newStats, WeaponVariety)) {
            outPacket.encodeInt(getOption(newStats, WeaponVariety).xOption); // flag
        }
        if (hasNewStat(newStats, OverloadMode)) {
            outPacket.encodeInt(getOption(newStats, OverloadMode).xOption);
        }
        if (hasNewStat(newStats, SpecterGauge)) {
            outPacket.encodeInt(getOption(newStats, SpecterGauge).xOption); // energy (out of 1000)
        }
        if (hasNewStat(newStats, SpecterMode)) {
            outPacket.encodeInt(getOption(newStats, SpecterMode).xOption); // energy (out of 1000)
        }
        if (hasNewStat(newStats, SpellBullet_Plain)) {
            outPacket.encodeInt(getOption(newStats, SpellBullet_Plain).xOption);
            outPacket.encodeInt(getOption(newStats, SpellBullet_Plain).cOption);
        }
        if (hasNewStat(newStats, SpellBullet_Scarlet)) {
            outPacket.encodeInt(getOption(newStats, SpellBullet_Scarlet).xOption);
            outPacket.encodeInt(getOption(newStats, SpellBullet_Scarlet).cOption);
        }
        if (hasNewStat(newStats, SpellBullet_Gust)) {
            outPacket.encodeInt(getOption(newStats, SpellBullet_Gust).xOption);
            outPacket.encodeInt(getOption(newStats, SpellBullet_Gust).cOption);
        }
        if (hasNewStat(newStats, SpellBullet_Abyss)) {
            outPacket.encodeInt(getOption(newStats, SpellBullet_Abyss).xOption);
            outPacket.encodeInt(getOption(newStats, SpellBullet_Abyss).cOption);
        }
        if (hasNewStat(newStats, BossWill_Infection)) {
            outPacket.encodeInt(getOption(newStats, BossWill_Infection).xOption);
        }
        if (hasNewStat(newStats, FlameWizardInfiniteFlame)) {
            outPacket.encodeInt(getOption(newStats, FlameWizardInfiniteFlame).xOption);
        }
        if (hasNewStat(newStats, PhantomMarkOfPhantomOwner)) {
            outPacket.encodeInt(getOption(newStats, PhantomMarkOfPhantomOwner).xOption);
        }
        if (hasNewStat(newStats, PhantomMarkOfPhantomTarget)) {
            outPacket.encodeInt(getOption(newStats, PhantomMarkOfPhantomTarget).xOption);
        }
        if (hasNewStat(newStats, NightWalkerBat)) {
            outPacket.encodeInt(getOption(newStats, NightWalkerBat).xOption);
        }
        if (hasNewStat(newStats, MemoryOfJourney)) {
            outPacket.encodeInt(getOption(newStats, MemoryOfJourney).xOption);
            outPacket.encodeInt(getOption(newStats, MemoryOfJourney).yOption);
            outPacket.encodeInt(getOption(newStats, MemoryOfJourney).zOption);
        }
        if (hasNewStat(newStats, (NewtroWarriors))) {
            outPacket.encodeInt(getOption(newStats, NewtroWarriors).xOption);
            outPacket.encodeInt(getOption(newStats, NewtroWarriors).yOption);
        }
        if (hasNewStat(newStats, (LuckyPapylus))) {
            outPacket.encodeInt(getOption(newStats, LuckyPapylus).xOption);
            outPacket.encodeInt(getOption(newStats, LuckyPapylus).yOption);
        }
        if (hasNewStat(newStats, DecBaseDamageDebuff)) {
            outPacket.encodeInt(getOption(newStats, DecBaseDamageDebuff).xOption);
        }
        if (hasNewStat(newStats, LimitEquipStatDebuff)) {
            outPacket.encodeInt(getOption(newStats, LimitEquipStatDebuff).xOption);
        }
        if (hasNewStat(newStats, ComboCounter)) {
            outPacket.encodeInt(getOption(newStats, ComboCounter).xOption);
            outPacket.encodeInt(getOption(newStats, ComboCounter).yOption);
        }
        if (hasNewStat(newStats, FifthGoddessBless)) {
            outPacket.encodeInt(getOption(newStats, FifthGoddessBless).xOption); // 400001050
            outPacket.encodeInt(getOption(newStats, FifthGoddessBless).yOption);
            outPacket.encodeInt(getOption(newStats, FifthGoddessBless).zOption);
        }
        if (hasNewStat(newStats, PathFinderAncientGuidance)) {
            outPacket.encodeInt(getOption(newStats, PathFinderAncientGuidance).xOption);
            outPacket.encodeInt(getOption(newStats, PathFinderAncientGuidance).yOption);
        }
        if (hasNewStat(newStats, BattlePvP_KeyDown)) {
            outPacket.encodeInt(getOption(newStats, BattlePvP_KeyDown).xOption);
        }
        if (hasNewStat(newStats, BattlePvP_Wongki_AwesomeFairy)) {
            outPacket.encodeInt(getOption(newStats, BattlePvP_Wongki_AwesomeFairy).xOption);
        }
        if (hasNewStat(newStats, HolySymbol)) {
            outPacket.encodeInt(getOption(newStats, HolySymbol).xOption);
            outPacket.encodeInt(getOption(newStats, HolySymbol).yOption);
            outPacket.encodeInt(getOption(newStats, HolySymbol).zOption);
            outPacket.encodeInt(getOption(newStats, HolySymbol).wOption);
            outPacket.encodeByte(getOption(newStats, HolySymbol).bOption);
            outPacket.encodeInt(getOption(newStats, HolySymbol).pOption);
        }
        if (hasNewStat(newStats, MinigameStat)) {
            outPacket.encodeInt(getOption(newStats, MinigameStat).xOption);
            outPacket.encodeInt(getOption(newStats, MinigameStat).yOption);
            outPacket.encodeInt(getOption(newStats, MinigameStat).zOption);
        }
        if (hasNewStat(newStats, AnimaThiefTaoistType)) {
            outPacket.encodeInt(getOption(newStats, AnimaThiefTaoistType).xOption);
            outPacket.encodeInt(getOption(newStats, AnimaThiefTaoistType).yOption);
        }
        if (hasNewStat(newStats, AnimaThiefTaoistGauge)) {
            outPacket.encodeInt(getOption(newStats, AnimaThiefTaoistGauge).xOption); // Tallisman Gauge 35->70->100?
        }
        if (hasNewStat(newStats, AnimaThiefMetaphysics)) {
            outPacket.encodeInt(getOption(newStats, AnimaThiefMetaphysics).xOption);
        }
        if (hasNewStat(newStats, NoviceMagicianLink)) {
            outPacket.encodeInt(getOption(newStats, NoviceMagicianLink).xOption);
        }
        if (hasNewStat(newStats, XenonHoloGramGraffiti)) {
            outPacket.encodeInt(getOption(newStats, XenonHoloGramGraffiti).xOption);
        }
        if (hasNewStat(newStats, LefWarriorNobility)) {
            outPacket.encodeInt(getOption(newStats, LefWarriorNobility).xOption);
            outPacket.encodeInt(getOption(newStats, LefWarriorNobility).yOption);
        }
        if (hasNewStat(newStats, RevenantGauge)) {
            outPacket.encodeInt(getOption(newStats, RevenantGauge).zOption);
        }
        if (hasNewStat(newStats, DeathDance)) {
            outPacket.encodeInt(getOption(newStats, DeathDance).xOption);
            outPacket.encodeInt(getOption(newStats, DeathDance).yOption);
            outPacket.encodeInt(getOption(newStats, DeathDance).zOption);
        }
        if (hasNewStat(newStats, ShadowShield)) {
            outPacket.encodeInt(getOption(newStats, ShadowShield).xOption);
        }
        if (hasNewStat(newStats, BMageAuraYellow)) {
            outPacket.encodeInt(getOption(newStats, BMageAuraYellow).xOption);
            outPacket.encodeInt(getOption(newStats, BMageAuraYellow).yOption);
        }
        if (hasNewStat(newStats, BMageAuraDrain)) {
            outPacket.encodeInt(getOption(newStats, BMageAuraDrain).xOption);
            outPacket.encodeInt(getOption(newStats, BMageAuraDrain).yOption);
        }
        if (hasNewStat(newStats, BMageAuraBlue)) {
            outPacket.encodeInt(getOption(newStats, BMageAuraBlue).xOption);
            outPacket.encodeInt(getOption(newStats, BMageAuraBlue).yOption);
            outPacket.encodeInt(getOption(newStats, BMageAuraBlue).zOption);
        }
        if (hasNewStat(newStats, BMageAuraDark)) {
            outPacket.encodeInt(getOption(newStats, BMageAuraDark).xOption);
            outPacket.encodeInt(getOption(newStats, BMageAuraDark).yOption);
        }
        if (hasNewStat(newStats, BMageAuraDebuff)) {
            outPacket.encodeInt(getOption(newStats, BMageAuraDebuff).xOption);
            outPacket.encodeInt(getOption(newStats, BMageAuraDebuff).yOption);
        }
        if (hasNewStat(newStats, BMageAuraUnion)) {
            outPacket.encodeInt(getOption(newStats, BMageAuraUnion).xOption);
            outPacket.encodeInt(getOption(newStats, BMageAuraUnion).yOption);
        }
        if (hasNewStat(newStats, IceAura)) {
            outPacket.encodeInt(getOption(newStats, IceAura).xOption);
            outPacket.encodeInt(getOption(newStats, IceAura).yOption);
        }
        if (hasNewStat(newStats, KnightsAura)) {
            outPacket.encodeInt(getOption(newStats, KnightsAura).xOption);
            outPacket.encodeInt(getOption(newStats, KnightsAura).yOption);
        }
        if (hasNewStat(newStats, ZeroAuraStr)) {
            outPacket.encodeInt(getOption(newStats, ZeroAuraStr).xOption);
            outPacket.encodeInt(getOption(newStats, ZeroAuraStr).yOption);
        }
        if (hasNewStat(newStats, NovaArcherIncanation)) {
            outPacket.encodeInt(getOption(newStats, NovaArcherIncanation).xOption);
            outPacket.encodeInt(getOption(newStats, NovaArcherIncanation).yOption);
        }
        if (hasNewStat(newStats, AranComboTempestAura)) {
            outPacket.encodeInt(getOption(newStats, AranComboTempestAura).xOption);
        }
        if (hasNewStat(newStats, XenonBursterLaser)) {
            outPacket.encodeInt(getOption(newStats, XenonBursterLaser).xOption);
        }
        if (hasNewStat(newStats, BMageAbyssalLightning)) {
            outPacket.encodeInt(getOption(newStats, BMageAbyssalLightning).xOption);
        }
        if (hasNewStat(newStats, KinesisLawOfGravity)) {
            outPacket.encodeInt(getOption(newStats, KinesisLawOfGravity).xOption);
        }
        if (hasNewStat(newStats, LefMageCrystalGate)) {
            outPacket.encodeInt(getOption(newStats, LefMageCrystalGate).xOption);
        }
        if (hasNewStat(newStats, HolyWater)) {
            outPacket.encodeInt(getOption(newStats, HolyWater).xOption);
        }
        if (hasNewStat(newStats, WeaponVarietyFinale)) {
            outPacket.encodeInt(getOption(newStats, WeaponVarietyFinale).xOption);
        }
        if (hasNewStat(newStats, Equinox)) {
            outPacket.encodeInt(getOption(newStats, Equinox).xOption);
            outPacket.encodeInt(getOption(newStats, Equinox).yOption);
        }
        if (hasNewStat(newStats, DarknessAura)) {
            outPacket.encodeInt(getOption(newStats, DarknessAura).xOption);
        }
        if (hasNewStat(newStats, SerpentScrew)) {
            outPacket.encodeInt(getOption(newStats, SerpentScrew).xOption);
            outPacket.encodeInt(getOption(newStats, SerpentScrew).yOption);
        }
        if (hasNewStat(newStats, EquinoxActive)) {
            outPacket.encodeInt(getOption(newStats, EquinoxActive).xOption);
        }
        if (hasNewStat(newStats, NAThanatosDescent)) {
            outPacket.encodeInt(getOption(newStats, NAThanatosDescent).xOption);
        }
        if (hasNewStat(newStats, NABrutalPang)) {
            outPacket.encodeInt(getOption(newStats, NABrutalPang).xOption);
            outPacket.encodeInt(getOption(newStats, NABrutalPang).yOption);
        }
        if (hasNewStat(newStats, Magnet)) {
            outPacket.encodeInt(getOption(newStats, Magnet).xOption);
        }
        if (hasNewStat(newStats, ATScrollPassive)) {
            outPacket.encodeInt(getOption(newStats, ATScrollPassive).xOption);
            outPacket.encodeInt(getOption(newStats, ATScrollPassive).yOption);
        }
        if (hasNewStat(newStats, YetiFuryGauge)) {
            outPacket.encodeInt(getOption(newStats, YetiFuryGauge).xOption);
        }
        if (hasNewStat(newStats, YetiFuryMode)) {
            outPacket.encodeInt(getOption(newStats, YetiFuryMode).xOption);
        }
        if (hasNewStat(newStats, AnimaThiefCloneAttack)) {
            outPacket.encodeInt(getOption(newStats, AnimaThiefCloneAttack).xOption);
        }
        if (hasNewStat(newStats, YetiCook)) {
            outPacket.encodeInt(getOption(newStats, YetiCook).xOption);
            outPacket.encodeByte(getOption(newStats, YetiCook).bOption);
        }
        if (hasNewStat(newStats, PinkbeanCheer)) {
            outPacket.encodeInt(getOption(newStats, PinkbeanCheer).xOption);
            outPacket.encodeByte(getOption(newStats, PinkbeanCheer).bOption);
        }
        if (hasNewStat(newStats, NewFlying)) {
            outPacket.encodeInt(getOption(newStats, NewFlying).xOption);
        }
        if (hasNewStat(newStats, ReincarnationMission)) {
            outPacket.encodeInt(getOption(newStats, ReincarnationMission).xOption);
        }
        if (hasNewStat(newStats, QuiverFullBurst)) {
            outPacket.encodeInt(getOption(newStats, QuiverFullBurst).xOption);
        }
        if (hasNewStat(newStats, ElementalFocus)) {
            outPacket.encodeInt(getOption(newStats, ElementalFocus).xOption);
        }
        if (hasNewStat(newStats, AdrenalinBoost)) {
            outPacket.encodeInt(getOption(newStats, AdrenalinBoost).xOption);
        }
        if (hasNewStat(newStats, ElementSoul)) {
            outPacket.encodeInt(getOption(newStats, ElementSoul).xOption);
            outPacket.encodeInt(getOption(newStats, ElementSoul).yOption);
        }
        if (hasNewStat(newStats, DarkCloud)) {
            outPacket.encodeInt(getOption(newStats, DarkCloud).xOption);
            outPacket.encodeInt(getOption(newStats, DarkCloud).yOption);
        }
        if (hasNewStat(newStats, UserAroundAttackDebuff)) {
            outPacket.encodeInt(getOption(newStats, UserAroundAttackDebuff).xOption);
        }
        if (hasNewStat(newStats, Confinement)) {
            outPacket.encodeInt(getOption(newStats, Confinement).xOption);
        }
        if (hasNewStat(newStats, FixedSpeedAndJump)) {
            outPacket.encodeInt(getOption(newStats, FixedSpeedAndJump).xOption);
            outPacket.encodeInt(getOption(newStats, FixedSpeedAndJump).yOption);
        }
        if (hasNewStat(newStats, GrabAndThrow)) {
            outPacket.encodeInt(getOption(newStats, GrabAndThrow).xOption);
            outPacket.encodeInt(getOption(newStats, GrabAndThrow).yOption);
            outPacket.encodeInt(getOption(newStats, GrabAndThrow).zOption);
            outPacket.encodeInt(getOption(newStats, GrabAndThrow).cOption);
            outPacket.encodeInt(getOption(newStats, GrabAndThrow).wOption);
        }
        if (hasNewStat(newStats, Stun)) {
            outPacket.encodeInt(getOption(newStats, Stun).xOption);
            outPacket.encodeInt(getOption(newStats, Stun).yOption);
        }
        if (hasNewStat(newStats, RPEventStat)) {
            outPacket.encodeInt(getOption(newStats, RPEventStat).xOption);
            outPacket.encodeInt(getOption(newStats, RPEventStat).yOption);
            outPacket.encodeInt(getOption(newStats, RPEventStat).zOption);
            outPacket.encodeInt(getOption(newStats, RPEventStat).cOption);
            outPacket.encodeInt(getOption(newStats, RPEventStat).wOption);
            outPacket.encodeInt(getOption(newStats, RPEventStat).uOption);
            outPacket.encodeInt(getOption(newStats, RPEventStat).bOption);
            outPacket.encodeInt(getOption(newStats, RPEventStat).sOption);
            outPacket.encodeInt(getOption(newStats, RPEventStat).ssOption);
            outPacket.encodeInt(getOption(newStats, RPEventStat).xOption);
            outPacket.encodeInt(getOption(newStats, RPEventStat).yOption);
            outPacket.encodeInt(getOption(newStats, RPEventStat).zOption);
            outPacket.encodeInt(getOption(newStats, RPEventStat).cOption);
            outPacket.encodeInt(getOption(newStats, RPEventStat).wOption);
            outPacket.encodeInt(getOption(newStats, RPEventStat).uOption);
        }
        if (hasNewStat(newStats, CommonItemSkillContinuous)) {
            outPacket.encodeInt(getOption(newStats, CommonItemSkillContinuous).xOption);
        }
        if (hasNewStat(newStats, BMageDeath)) {
            outPacket.encodeInt(getOption(newStats, BMageDeath).xOption);
        }
        if (hasNewStat(newStats, LamourCarte)) {
            outPacket.encodeInt(getOption(newStats, LamourCarte).xOption);
        }
        if (hasNewStat(newStats, AdrenalinMaximum)) {
            outPacket.encodeInt(getOption(newStats, AdrenalinMaximum).xOption);
            outPacket.encodeInt(getOption(newStats, AdrenalinMaximum).yOption);
        }
        if (hasNewStat(newStats, RapidFire)) {
            outPacket.encodeInt(getOption(newStats, RapidFire).xOption);
        }
        if (hasNewStat(newStats, Nightmare)) {
            outPacket.encodeInt(getOption(newStats, Nightmare).xOption);
        }
        if (hasNewStat(newStats, SixthJavelinStack)) {
            outPacket.encodeInt(getOption(newStats, SixthJavelinStack).xOption);
        }
        if (hasNewStat(newStats, SixthGloryWingJavelinStack)) {
            outPacket.encodeInt(getOption(newStats, SixthGloryWingJavelinStack).xOption);
        }
        if (hasNewStat(newStats, LimitBreakFinalAttack)) {
            outPacket.encodeInt(getOption(newStats, LimitBreakFinalAttack).xOption);
        }
        if (hasNewStat(newStats, EventSoccerMomentBuff)) {
            outPacket.encodeInt(getOption(newStats, EventSoccerMomentBuff).xOption);
        }
        if (hasNewStat(newStats, DslayerMetamorphosis)) {
            outPacket.encodeInt(getOption(newStats, DslayerMetamorphosis).xOption);
        }
        if (hasNewStat(newStats, RenPlumSwordEx)) {
            outPacket.encodeInt(getOption(newStats, RenPlumSwordEx).xOption);
            outPacket.encodeInt(getOption(newStats, RenPlumSwordEx).yOption);
        }
        if (hasNewStat(newStats, SpiritAwakeningStack)) {
            outPacket.encodeInt(getOption(newStats, SpiritAwakeningStack).xOption);
        }
        if (hasNewStat(newStats, Unk856)) {
            outPacket.encodeByte(getOption(newStats, Unk856).xOption);
            outPacket.encodeByte(getOption(newStats, Unk856).yOption);
        }
        if (hasNewStat(newStats, Unk857)) {
            outPacket.encodeByte(getOption(newStats, Unk857).xOption);
            outPacket.encodeByte(getOption(newStats, Unk857).yOption);
        }
        if (hasNewStat(newStats, ReduceMP)) {
            outPacket.encodeInt(getOption(newStats, ReduceMP).xOption);
            outPacket.encodeInt(getOption(newStats, ReduceMP).yOption);
            outPacket.encodeInt(getOption(newStats, ReduceMP).zOption);
        }
        if (hasNewStat(newStats, WorldExpBuff)) {
            outPacket.encodeInt(getOption(newStats, WorldExpBuff).xOption);
            outPacket.encodeInt(getOption(newStats, WorldExpBuff).yOption);
            outPacket.encodeInt(getOption(newStats, WorldExpBuff).zOption);
        }
        if (hasNewStat(newStats, WorldDropBuff)) {
            outPacket.encodeInt(getOption(newStats, WorldDropBuff).xOption);
            outPacket.encodeInt(getOption(newStats, WorldDropBuff).yOption);
            outPacket.encodeInt(getOption(newStats, WorldDropBuff).zOption);
        }
        if (hasNewStat(newStats, CurseRingBuff)) {
            outPacket.encodeInt(getOption(newStats, CurseRingBuff).xOption);
            outPacket.encodeInt(getOption(newStats, CurseRingBuff).yOption);
            outPacket.encodeInt(getOption(newStats, CurseRingBuff).zOption);
            outPacket.encodeInt(getOption(newStats, CurseRingBuff).uOption);
        }
        if (hasNewStat(newStats, SummonProp)) {
            outPacket.encodeInt(getOption(newStats, SummonProp).nOption);
            outPacket.encodeInt(getOption(newStats, SummonProp).yOption);
            outPacket.encodeInt(getOption(newStats, SummonProp).zOption);
        }
        if (hasNewStat(newStats, GhostLiberationStack)) {
            outPacket.encodeInt(getOption(newStats, GhostLiberationStack).xOption);
        }
        if (hasNewStat(newStats, Unk872)) {
            outPacket.encodeInt(getOption(newStats, Unk872).xOption);
        }
        if (hasNewStat(newStats, HowlingOfNature)) {
            outPacket.encodeInt(getOption(newStats, HowlingOfNature).xOption);
        }
        if (hasNewStat(newStats, MitsuhideDebuff)) {
            outPacket.encodeInt(getOption(newStats, MitsuhideDebuff).xOption);
        }
        if (hasNewStat(newStats, MitsuhideStigma)) {
            outPacket.encodeInt(getOption(newStats, MitsuhideStigma).xOption);
        }
        if (hasNewStat(newStats, ShamanIgnoreTargetDEF)) {
            outPacket.encodeInt(getOption(newStats, ShamanIgnoreTargetDEF).xOption);
        }
        if (hasNewStat(newStats, KannaFifthAttract)) {
            outPacket.encodeInt(getOption(newStats, KannaFifthAttract).zOption);
        }
        if (hasNewStat(newStats, Unk866)) {
            outPacket.encodeInt(getOption(newStats, Unk866).xOption);
        }
        if (hasNewStat(newStats, Unk882)) {
            outPacket.encodeByte(getOption(newStats, Unk882).xOption);
            outPacket.encodeByte(getOption(newStats, Unk882).yOption);
        }
        if (hasNewStat(newStats, HyperUpgradeDiscountR)) {
            outPacket.encodeInt(getOption(newStats, HyperUpgradeDiscountR).xOption);
            outPacket.encodeInt(getOption(newStats, HyperUpgradeDiscountR).yOption);
            outPacket.encodeInt(getOption(newStats, HyperUpgradeDiscountR).zOption);
        }
        if (hasNewStat(newStats, NeoTokyoBossPowOfLife)) {
            outPacket.encodeInt(getOption(newStats, NeoTokyoBossPowOfLife).xOption);
        }
        if (hasNewStat(newStats, KazaxDebuff)) {
            outPacket.encodeInt(getOption(newStats, KazaxDebuff).xOption);
            outPacket.encodeInt(getOption(newStats, KazaxDebuff).yOption);
            outPacket.encodeInt(getOption(newStats, KazaxDebuff).zOption);
            outPacket.encodeInt(getOption(newStats, KazaxDebuff).uOption);
        }
        if (hasNewStat(newStats, ShineMageHourglass)) {
            outPacket.encodeInt(getOption(newStats, ShineMageHourglass).xOption);
        }
        if (hasNewStat(newStats, RideOrDieIncDropRate)) {
            outPacket.encodeInt(getOption(newStats, RideOrDieIncDropRate).xOption);
            outPacket.encodeInt(getOption(newStats, RideOrDieIncDropRate).yOption);
            outPacket.encodeInt(getOption(newStats, RideOrDieIncDropRate).zOption);
        }
    }

    public void encodeForRemote(OutPacket outPacket, EnumMap<CharacterTemporaryStat, List<Option>> newStats) {
        int[] mask = getMaskByCollection(newStats);
        for (int maskElem : mask) {
            outPacket.encodeInt(maskElem);
        }
        RemoteSecondaryStat.encode(outPacket, newStats);
        outPacket.encodeByte(getDefenseAtt());
        outPacket.encodeByte(getDefenseState());
        outPacket.encodeByte(getPvpDamage());
        outPacket.encodeInt(getOption(newStats, EtherealForm).xOption); // Red Blue Green, used in Ethereal Form  so far
        Set<CharacterTemporaryStat> ctsSet = newStats.keySet();
        if (ctsSet.contains(BlackMageCreate)) {
            outPacket.encodeInt(newStats.get(BlackMageCreate).getFirst().xOption);
        }
        if (ctsSet.contains(BlackMageDestroy)) {
            outPacket.encodeInt(newStats.get(BlackMageDestroy).getFirst().xOption);
        }
        if (ctsSet.contains(PoseType)) {
            var o = newStats.get(PoseType).getFirst();
            outPacket.encodeInt(o.bOption);
            outPacket.encodeInt(o.uOption);
        }
        if (ctsSet.contains(BattlePvP_Helena_Mark)) {
            var o = newStats.get(BattlePvP_Helena_Mark).getFirst();
            outPacket.encodeInt(o.xOption);
            outPacket.encodeInt(o.yOption);
            outPacket.encodeInt(o.zOption);
        }
        if (ctsSet.contains(BattlePvP_Darklord_Explosion)) {
            var o = newStats.get(BattlePvP_Darklord_Explosion).getFirst();
            outPacket.encodeInt(o.xOption);
            outPacket.encodeInt(o.yOption);
            outPacket.encodeInt(o.zOption);
        }
        if (ctsSet.contains(BattlePvP_LangE_Protection)) {
            var o = newStats.get(BattlePvP_LangE_Protection).getFirst();
            outPacket.encodeInt(o.xOption);
            outPacket.encodeInt(o.yOption);
        }
        if (ctsSet.contains(MichaelSoulLink)) {
            var o = newStats.get(MichaelSoulLink).getFirst();
            outPacket.encodeInt(o.xOption);
            outPacket.encodeByte(o.bOption);
            outPacket.encodeInt(o.cOption);
            outPacket.encodeInt(o.yOption);
        }
        if (ctsSet.contains(Stigma)) {
            outPacket.encodeInt(newStats.get(Stigma).getFirst().bOption);
        }
        if (ctsSet.contains(PairingUser)) {
            outPacket.encodeShort(newStats.get(PairingUser).getFirst().xOption);
        }
        if (ctsSet.contains(Frenzy)) {
            outPacket.encodeShort(newStats.get(Frenzy).getFirst().xOption);
        }
        if (ctsSet.contains(SerpentScrew)) {
            outPacket.encodeShort(newStats.get(SerpentScrew).getFirst().xOption);
        }
        if (ctsSet.contains(BloodyExplosion)) {
            outPacket.encodeShort(newStats.get(BloodyExplosion).getFirst().xOption);
        }
        if (ctsSet.contains(ShadowSpear)) {
            outPacket.encodeShort(newStats.get(ShadowSpear).getFirst().xOption);
        }
        if (ctsSet.contains(Michael_RhoAias)) {
            var o = newStats.get(Michael_RhoAias).getFirst();
            outPacket.encodeInt(o.xOption);
            outPacket.encodeInt(o.bOption);
            outPacket.encodeInt(o.cOption);
            outPacket.encodeInt(o.yOption);
        }
        if (ctsSet.contains(VampDeath)) {
            outPacket.encodeInt(newStats.get(VampDeath).getFirst().xOption);
        }
        if (ctsSet.contains(LefGloryWing)) {
            var o = newStats.get(LefGloryWing).getFirst();
            outPacket.encodeInt(o.xOption);
            outPacket.encodeInt(o.cOption);
        }
        if (ctsSet.contains(LefBuffMastery)) {
            var o = newStats.get(LefBuffMastery).getFirst();
            outPacket.encodeInt(o.xOption);
            outPacket.encodeInt(o.cOption);
        }
        if (ctsSet.contains(BattlePvP_Ryude_Frozen)) {
            var o = newStats.get(BattlePvP_Ryude_Frozen).getFirst();
            outPacket.encodeInt(o.xOption);
            outPacket.encodeInt(o.yOption);
            outPacket.encodeInt(o.zOption);
        }
        if (ctsSet.contains(BattlePvP_LangE_LiverStack)) {
            var o = newStats.get(BattlePvP_LangE_LiverStack).getFirst();
            outPacket.encodeInt(o.xOption);
            outPacket.encodeInt(o.yOption);
            outPacket.encodeInt(o.zOption);
        }
        if (ctsSet.contains(BattlePvP_KeyDown)) {
            var o = newStats.get(BattlePvP_LangE_LiverStack).getFirst();
            outPacket.encodeInt(o.xOption);
            outPacket.encodeInt(o.yOption);
            outPacket.encodeInt(o.zOption);
        }
        if (ctsSet.contains(BattlePvP_Wongki_AwesomeFairy)) {
            outPacket.encodeInt(newStats.get(BattlePvP_Wongki_AwesomeFairy).getFirst().xOption);
        }
        if (ctsSet.contains(RenPlumSwordEx)) {
            var o = newStats.get(RenPlumSwordEx).getFirst();
            outPacket.encodeInt(o.xOption);
            outPacket.encodeInt(o.yOption);
        }
        if (getStopForceAtom() != null) {
            getStopForceAtom().encode(outPacket);
        } else {
            new StopForceAtom().encode(outPacket);
        }
        for (int i = 0; i < TSIndex.values().length; i++) {
            if (ctsSet.contains(TSIndex.getCTSFromTwoStatIndex(i))) {
                getTwoStates().get(i).encode(outPacket);
            }
        }
        encodeIndieTempStat(outPacket, newStats);
        if (ctsSet.contains(OutSide)) {
            outPacket.encodeInt(newStats.get(OutSide).getFirst().xOption);
        }
        if (ctsSet.contains(KeyDownMoving)) {
            outPacket.encodeInt(newStats.get(KeyDownMoving).getFirst().xOption);
        }
        if (ctsSet.contains(BossWill_Infection)) {
            outPacket.encodeInt(newStats.get(BossWill_Infection).getFirst().xOption);
        }
        if (ctsSet.contains(ComboCounter)) {
            var o = newStats.get(ComboCounter).getFirst();
            outPacket.encodeInt(o.xOption);
            outPacket.encodeInt(o.yOption);
        }
        if (ctsSet.contains(MinigameStat)) {
            var o = newStats.get(MinigameStat).getFirst();
            outPacket.encodeInt(o.xOption);
            outPacket.encodeInt(o.yOption);
            outPacket.encodeInt(o.zOption);
        }
        if (ctsSet.contains(XenonHoloGramGraffiti)) {
            outPacket.encodeInt(newStats.get(XenonHoloGramGraffiti).getFirst().xOption);
        }
        outPacket.encodeByte(0);
        if (ctsSet.contains(LefWarriorNobility)) {
            var o = newStats.get(LefWarriorNobility).getFirst();
            outPacket.encodeInt(o.xOption);
            outPacket.encodeInt(o.yOption);
        }
        if (ctsSet.contains(BMageAuraYellow)) {
            var o = newStats.get(BMageAuraYellow).getFirst();
            outPacket.encodeInt(o.xOption);
            outPacket.encodeInt(o.yOption);
        }
        if (ctsSet.contains(BMageAuraDrain)) {
            var o = newStats.get(BMageAuraDrain).getFirst();
            outPacket.encodeInt(o.xOption);
            outPacket.encodeInt(o.yOption);
        }
        if (ctsSet.contains(BMageAuraBlue)) {
            var o = newStats.get(BMageAuraBlue).getFirst();
            outPacket.encodeInt(o.xOption);
            outPacket.encodeInt(o.yOption);
            outPacket.encodeInt(o.zOption);
        }
        if (ctsSet.contains(BMageAuraDark)) {
            var o = newStats.get(BMageAuraDark).getFirst();
            outPacket.encodeInt(o.xOption);
            outPacket.encodeInt(o.yOption);
        }
        if (ctsSet.contains(BMageAuraDebuff)) {
            var o = newStats.get(BMageAuraDebuff).getFirst();
            outPacket.encodeInt(o.xOption);
            outPacket.encodeInt(o.yOption);
        }
        if (ctsSet.contains(BMageAuraUnion)) {
            var o = newStats.get(BMageAuraUnion).getFirst();
            outPacket.encodeInt(o.xOption);
            outPacket.encodeInt(o.yOption);
        }
        if (ctsSet.contains(IceAura)) {
            var o = newStats.get(IceAura).getFirst();
            outPacket.encodeInt(o.xOption);
            outPacket.encodeInt(o.yOption);
        }
        if (ctsSet.contains(KnightsAura)) {
            var o = newStats.get(KnightsAura).getFirst();
            outPacket.encodeInt(o.xOption);
            outPacket.encodeInt(o.yOption);
        }
        if (ctsSet.contains(ZeroAuraStr)) {
            var o = newStats.get(ZeroAuraStr).getFirst();
            outPacket.encodeInt(o.xOption);
            outPacket.encodeInt(o.yOption);
        }
        if (ctsSet.contains(NovaArcherIncanation)) {
            var o = newStats.get(NovaArcherIncanation).getFirst();
            outPacket.encodeInt(o.xOption);
            outPacket.encodeInt(o.yOption);
        }
        if (ctsSet.contains(XenonBursterLaser)) {
            outPacket.encodeInt(newStats.get(XenonBursterLaser).getFirst().xOption);
        }
        if (ctsSet.contains(ShadowShield)) {
            outPacket.encodeInt(newStats.get(ShadowShield).getFirst().xOption);
        }
        if (ctsSet.contains(Infinity)) {
            var o = newStats.get(Infinity).getFirst();
            outPacket.encodeInt(o.xOption);
            outPacket.encodeInt(o.yOption);
        }
        if (ctsSet.contains(YetiFuryMode)) {
            outPacket.encodeInt(newStats.get(YetiFuryMode).getFirst().xOption);
        }
        if (ctsSet.contains(AnimaThiefCloneAttack)) {
            outPacket.encodeInt(newStats.get(AnimaThiefCloneAttack).getFirst().xOption);
        }
        if (ctsSet.contains(AMAbsorptionWind)) {
            var o = newStats.get(AMAbsorptionWind).getFirst();
            outPacket.encodeInt(o.xOption);
            outPacket.encodeInt(o.bOption);
            outPacket.encodeInt(o.cOption);
            outPacket.encodeInt(o.yOption);
            outPacket.encodeInt(o.zOption);
        }
        if (ctsSet.contains(Stun)) {
            var o = newStats.get(Stun).getFirst();
            outPacket.encodeInt(o.xOption);
            outPacket.encodeInt(o.yOption);
        }
        if (ctsSet.contains(RPEventStat)) {
            var o = newStats.get(RPEventStat).getFirst();
            outPacket.encodeInt(o.xOption);
            outPacket.encodeInt(o.yOption);
            outPacket.encodeInt(o.zOption);
            outPacket.encodeInt(o.cOption);
            outPacket.encodeInt(o.wOption);
            outPacket.encodeInt(o.uOption);
            outPacket.encodeInt(o.bOption);
            outPacket.encodeInt(o.sOption);
            outPacket.encodeInt(o.ssOption);
            outPacket.encodeInt(o.xOption);
            outPacket.encodeInt(o.yOption);
            outPacket.encodeInt(o.zOption);
            outPacket.encodeInt(o.cOption);
            outPacket.encodeInt(o.wOption);
            outPacket.encodeInt(o.uOption);
        }
        if (ctsSet.contains(EventSoccerMomentBuff)) {
            outPacket.encodeInt(newStats.get(EventSoccerMomentBuff).getFirst().xOption);
        }
        if (ctsSet.contains(NewFlying)) {
            outPacket.encodeInt(newStats.get(NewFlying).getFirst().xOption);
        }
        if (ctsSet.contains(MitsuhideDebuff)) {
            outPacket.encodeInt(newStats.get(MitsuhideDebuff).getFirst().xOption);
        }
        if (ctsSet.contains(MitsuhideStigma)) {
            outPacket.encodeInt(newStats.get(MitsuhideStigma).getFirst().xOption);
        }
        if (ctsSet.contains(NeoTokyoBossBomb)) {
            outPacket.encodeInt(newStats.get(NeoTokyoBossBomb).getFirst().xOption);
        }
        if (ctsSet.contains(NeoTokyoBossPowOfLife)) {
            outPacket.encodeInt(newStats.get(NeoTokyoBossPowOfLife).getFirst().xOption);
        }
    }

    public void encodeIndieTempStat(OutPacket outPacket, EnumMap<CharacterTemporaryStat, List<Option>> removeStats) {
        TreeMap<CharacterTemporaryStat, List<Option>> sortedStats = new TreeMap<>();
        // add removed stats into a sorted map
        for (var entry : removeStats.entrySet()) {
            if (entry.getKey().isIndie() && entry.getValue() != null) {
                sortedStats.put(entry.getKey(), entry.getValue());
            }
        }
        for (var stat : sortedStats.entrySet()) {
            int curTime = Util.getCurrentTime();
            // encode remaining stats
            CharacterTemporaryStat key = stat.getKey();
            List<Option> options = getOptions(key);
            if (options == null) {
                outPacket.encodeInt(0);
                continue;
            }
            outPacket.encodeInt(options.size());
            for (Option option : options) {
                outPacket.encodeInt(option.nReason);
                outPacket.encodeInt(option.nValue);
                outPacket.encodeInt(option.nKey);
                outPacket.encodeInt(curTime - option.tStart); // elapsedTime
                outPacket.encodeInt(option.tTerm);
                outPacket.encodeInt(0);
                outPacket.encodeInt(0); // size
                outPacket.encodeInt(0); // size
            }
        }
    }

    public boolean hasRemovedMovingEffectingStat(Map<CharacterTemporaryStat, List<Option>> removeStats) {
        return removeStats.keySet().stream().anyMatch(CharacterTemporaryStat::isMovementAffectingStat);
    }

    public Map<CharacterTemporaryStat, List<Option>> getCurrentStats() {
        lock.lock();
        try {
            Map<CharacterTemporaryStat, List<Option>> snapshot = new EnumMap<>(CharacterTemporaryStat.class);
            currentStats.forEach((cts, options) -> snapshot.put(cts, new ArrayList<>(options)));
            return snapshot;
        } finally {
            lock.unlock();
        }
    }

    public EnumMap<CharacterTemporaryStat, List<Option>> getRemoteStats() {
        EnumMap<CharacterTemporaryStat, List<Option>> results = new EnumMap<>(CharacterTemporaryStat.class);
        lock.lock();
        try {
            for (Map.Entry<CharacterTemporaryStat, List<Option>> entry : currentStats.entrySet()) {
                if (!isErr38RemoteCTS(entry.getKey())) {
                    results.put(entry.getKey(), new ArrayList<>(entry.getValue()));
                }
            }
            return results;
        } finally {
            lock.unlock();
        }
    }

    public int getPvpDamage() {
        return pvpDamage;
    }

    public void setPvpDamage(int pvpDamage) {
        this.pvpDamage = pvpDamage;
    }

    public byte getDefenseState() {
        return defenseState;
    }

    public void setDefenseState(byte defenseState) {
        this.defenseState = defenseState;
    }

    public byte getDefenseAtt() {
        return defenseAtt;
    }

    public void setDefenseAtt(byte defenseAtt) {
        this.defenseAtt = defenseAtt;
    }

    public int[] getDiceInfo() {
        return diceInfo;
    }

    public void setDiceInfo(int[] diceInfo) {
        this.diceInfo = diceInfo;
    }

    public void throwDice(int roll) {
        throwDice(roll, 0);
    }

    public void throwDice(int dice1, int dice2) {
        throwDice(dice1, dice2, 0);
    }

    public void throwDice(int dice1, int dice2, int dice3) {
        int[] array = {0, 0, 30, 20, 15, 20, 30, 20};   // Stats for Normal Rolls
        int[] arrayDD = {0, 0, 40, 30, 25, 30, 40, 30}; // Stats for 2 identical numbers
        int[] arrayLD = {0, 0, 50, 40, 40, 40, 50, 40}; // Stats for 3 identical numbers
        for (int i = 0; i < diceOption.length; i++) {
            diceOption[i] = 0;
        }

        if (dice1 == dice2 && dice1 == dice3) { // 3 identical numbers
            diceOption[dice1] = arrayLD[dice1];
        } else if (dice1 == dice2) {
            diceOption[dice1] = arrayDD[dice1];
            diceOption[dice3] = array[dice3];
        } else if (dice1 == dice3) {
            diceOption[dice1] = arrayDD[dice1];
            diceOption[dice2] = array[dice2];
        } else if (dice2 == dice3) {
            diceOption[dice2] = arrayDD[dice2];
            diceOption[dice1] = array[dice1];
        } else {                                // 3 non-identical numbers
            diceOption[dice1] = array[dice1];
            diceOption[dice2] = array[dice2];
            diceOption[dice3] = array[dice3];
        }

        int[] diceinfo = new int[]{
                diceOption[3],  //nOption 3 (MHPR)
                diceOption[3],  //nOption 3 (MMPR)
                diceOption[4],  //nOption 4 (Cr)
                0,  // CritDamage Min
                0,  // ???  ( CritDamage Max (?) )
                0,  // EVAR
                0,  // AR
                0,  // ER
                diceOption[2],  //nOption 2 (PDDR)
                diceOption[2],  //nOption 2 (MDDR)
                0,  // PDR
                0,  // MDR
                diceOption[5],  //nOption 5 (PIDR)
                0,  // PDamR
                0,  // MDamR
                0,  // PADR
                0,  // MADR
                diceOption[6], //nOption 6 (EXP)
                diceOption[7], //nOption 7 (IED)
                0,  // ASRR
                0,  // TERR
                0,  // MesoRate
                0,
        };
        setDiceInfo(diceinfo);
    }

    public List<Integer> getMobZoneStates() {
        return mobZoneStates;
    }

    public void setMobZoneStates(List<Integer> mobZoneStates) {
        this.mobZoneStates = mobZoneStates;
    }

    public int getViperEnergyCharge() {
        return viperEnergyCharge;
    }

    public void setViperEnergyCharge(int viperEnergyCharge) {
        this.viperEnergyCharge = viperEnergyCharge;
    }

    public StopForceAtom getStopForceAtom() {
        return stopForceAtom;
    }

    private void snapshotStopForceAtom(CharacterTemporaryStat cts, Option option) {
        if (cts == StopForceAtomInfo && net.swordie.ms.connection.packet.Tms282RemoteBuffs.active()) {
            option.stopForceAtom = stopForceAtom == null ? null : stopForceAtom.deepCopy();
        }
    }

    public void setStopForceAtom(StopForceAtom stopForceAtom) {
        this.stopForceAtom = stopForceAtom;
    }

    public LarknessManager getLarknessManager() {
        return larknessManager;
    }

    public void setLarknessManager(LarknessManager larknessManager) {
        this.larknessManager = larknessManager;
    }

    public Char getChr() {
        return chr;
    }

    public void removeAllDebuffs() {
        removeDebuffs(true);
    }

    public void removeDebuff() {
        removeDebuffs(true);
    }

    public static final EnumSet<CharacterTemporaryStat> RESET_BY_TIME_CTS = EnumSet.of(
            Stun, Shock, Poison, Seal, Darkness, Weakness, WeaknessMdamage, Curse, Slow, /*TimeBomb,*/
            DisOrder, Thread, Attract, Magnet, MagnetArea, ReverseInput, BanMap, StopPortion, StopMotion,
            Fear, Frozen, Frozen2, Web, NotDamaged, FinalCut, Lapidification, VampDeath, VampDeathSummon,
            GiveMeHeal, TouchMe, Contagion, CrossOverChain, Reincarnation, ComboCostInc,
            DotBasedBuff, QuiverCatridge, ExtraQuiverCatridge, UserControlMob,
            CriticalGrowing, QuickDraw, BowMasterConcentration, ComboTempest, SiphonVitality, KnockBack, RWMovingEvar);

    public Set<AffectedArea> getAffectedAreas() {
        return affectedAreas;
    }

    public void addAffectedArea(AffectedArea aa) {
        getAffectedAreas().add(aa);
    }

    public void removeAffectedArea(AffectedArea aa) {
        Iterator<AffectedArea> itr = getAffectedAreas().iterator();
        while (itr.hasNext()) {
            if (itr.next().getSkillID() == aa.getSkillID() && itr.next().getCharID() == aa.getCharID()) {
                itr.remove();
            }
        }
        if (aa.getRemoveSkill()) {
            removeStatsBySkill(aa.getSkillID());
        }
    }

    public boolean hasAffectedArea(AffectedArea affectedArea) {
        return getAffectedAreas().contains(affectedArea);
    }

    public boolean hasStatBySkillId(EnumMap<CharacterTemporaryStat, List<Option>> newStats, int skillId) {
        final Set<CharacterTemporaryStat> ctsSet = newStats.keySet();
        for (CharacterTemporaryStat cts : ctsSet) {
            if (getOption(cts) != null) {
                if (getOption(cts).rOption == skillId || getOption(cts).nReason == skillId) {
                    return true;
                }
            }
        }
        return false;
    }

    public void addSoulMPFromMobDeath(Mob mob) {
        Option o1 = getOption(SoulMP);
        Option o2 = new Option();
        Option o3 = new Option();
        if (o1.nOption >= ItemConstants.MAX_SOUL_CAPACITY) return;

        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        int soulSocketID = ((Equip) chr.getEquippedItemByBodyPart(BodyPart.Weapon)).getSoulSocketId();
        int soulID = ((Equip) chr.getEquippedItemByBodyPart(BodyPart.Weapon)).getSoulOptionId();
        int soulItemID = ((Equip) chr.getEquippedItemByBodyPart(BodyPart.Weapon)).getSoulItemId();

        int incSoulMP = Util.getRandom(1, 3);
        if (mob.getLevel() - chr.getLevel() >= 30) {
            incSoulMP = Util.getRandom(3, 5);
        }
        int newSoulCount = o1.nOption + incSoulMP;
        o1.nOption = Math.min(ItemConstants.MAX_SOUL_CAPACITY, newSoulCount);
        o1.rOption = ItemConstants.getSoulSkillFromSoulID(soulID);
        o1.xOption = ItemConstants.MAX_SOUL_CAPACITY;
        newStats.put(SoulMP, o1);

        if (o1.nOption < 1000) {
            DropInfo mobSoul = new DropInfo(GameConstants.MOB_SOUL, 1000);
            for (int i = 0; i < incSoulMP; i++) {
                chr.getField().drop(mobSoul, mob.getPosition(), chr.getPosition(), chr, false, true);
            }
        }
        int incPadMad = 0;
        if (soulSocketID != 0) {
            if (soulID != 0) {
                if (newSoulCount == 1) {
                    incPadMad = ItemConstants.isSoulLowTier(soulItemID) ? 5 : 10;
                } else if (newSoulCount >= 101 && newSoulCount <= 200) {
                    incPadMad = ItemConstants.isSoulLowTier(soulItemID) ? 7 : 12;
                } else if (newSoulCount >= 201 && newSoulCount <= 300) {
                    incPadMad = ItemConstants.isSoulLowTier(soulItemID) ? 9 : 14;
                } else if (newSoulCount >= 301 && newSoulCount <= 400) {
                    incPadMad = ItemConstants.isSoulLowTier(soulItemID) ? 11 : 16;
                } else if (newSoulCount >= 401 && newSoulCount <= 500) {
                    incPadMad = ItemConstants.isSoulLowTier(soulItemID) ? 13 : 18;
                } else if (newSoulCount >= 501 && newSoulCount <= 1000) {
                    incPadMad = ItemConstants.isSoulLowTier(soulItemID) ? 15 : 20;
                }
            } else { // No Soul Weapon
                if (newSoulCount == 1) {
                    incPadMad = 1;
                } else if (newSoulCount >= 101 && newSoulCount <= 200) {
                    incPadMad = 2;
                } else if (newSoulCount >= 201 && newSoulCount <= 300) {
                    incPadMad = 4;
                } else if (newSoulCount >= 301 && newSoulCount <= 400) {
                    incPadMad = 6;
                } else if (newSoulCount >= 401 && newSoulCount <= 500) {
                    incPadMad = 8;
                } else if (newSoulCount >= 501 && newSoulCount <= 1000) {
                    incPadMad = 10;
                }
            }
        }
        if (incPadMad > 0 && getOption(PAD).nOption != incPadMad) {
            o2.nOption = incPadMad;
            o2.rOption = ItemConstants.getSoulSkillFromSoulID(soulID);
            newStats.put(PAD, o2);
            o3.nOption = incPadMad;
            o3.rOption = ItemConstants.getSoulSkillFromSoulID(soulID);
            newStats.put(MAD, o3);
        }
        sendStat(newStats);
    }

    public void sendSetStatFromMobSkillPacket(CharacterTemporaryStat cts, Option o) {
        if (blocksTanjiroWillDebuff(cts, System.currentTimeMillis()) || net.swordie.ms.client.jobs.special.SaitamaActionProtection.blocksDebuff(chr, cts, System.currentTimeMillis())) return;
        int duration = o.tOption;
        if (o.tOption >= 100 && o.tOption <= 999) {
            //tOption = min if > 100 then no way happen
            duration = o.tOption / 100;
        } else if (o.tOption >= 1000) {
            duration = o.tOption / 1000;
        }
        o.tOption = duration;
        o.rOption |= o.slv << 16; // mob skills are encoded differently: not an int, but short (skill ID), then short (slv).
        sendStat(cts, o);
        if (chr.getJobHandler() != null) {
            chr.getJobHandler().handleMobDebuffSkill(chr);
        }
    }

    public Map<BaseStat, Integer> getBaseStats() {
        return baseStats;
    }

    public void addBaseStat(BaseStat bs, int value) {
        baseStats.put(bs, baseStats.getOrDefault(bs, 0) + value);
    }

    public void removeBaseStat(BaseStat bs, int value) {
        addBaseStat(bs, -value);
    }

    public long getTotalNOptionOfStat(CharacterTemporaryStat cts) {
        if (cts.isIndie()) {
            return getOptions(cts).stream().mapToLong(o -> o.nValue).sum();
        } else {
            return getOptions(cts).stream().mapToLong(o -> o.nOption).sum();
        }
    }

    public void addPetPassiveSkills(int SkillID) {
        getPetPassiveSkills().add(SkillID);
    }

    public List<Integer> getPetPassiveSkills() {
        return petPassiveSkills;
    }
}
