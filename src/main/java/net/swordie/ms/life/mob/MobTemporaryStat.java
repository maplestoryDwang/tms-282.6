package net.swordie.ms.life.mob;

import net.swordie.ms.connection.protocol.codec.MobPacketEncoder;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.MobPool;
import net.swordie.ms.life.mob.skill.BurnedInfo;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static net.swordie.ms.life.mob.MobStat.*;

public class MobTemporaryStat {
    
    private final Lock lock = new ReentrantLock();
    private String linkTeam;
    private final Map<Integer, Set<BurnedInfo>> burnedInfos = new ConcurrentHashMap<>(); // chrId -> skillId -> bis
    private final Map<MobStat, Option> currentStats = new ConcurrentHashMap<>();
    private long lucidNightmareResistUntil;

    private static boolean lucidNightmare(Option option) {
        return option != null && option.rOption == net.swordie.ms.client.character.items.Tms282EquipmentEffects.LUCID_NIGHTMARE
                && net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
    }

    private boolean reserveLucidNightmare(Mob mob, long now) {
        lock.lock();
        try {
            if (mob.isNoDebuff() || now < lucidNightmareResistUntil) return false;
            lucidNightmareResistUntil = now + SkillData.getSkillInfoById(
                    net.swordie.ms.client.character.items.Tms282EquipmentEffects.LUCID_NIGHTMARE).getValue(SkillStat.v, 1);
            return true;
        } finally { lock.unlock(); }
    }

    /** Both sources remain independently resistant; the one-slot Stun representation must never
     * shorten a longer overlapping bind just because the other source was applied afterwards. */
    private Option preserveLongerLucidStun(MobStat stat, Option incoming, long now) {
        Option old = currentStats.get(stat);
        if (stat != Stun || old == null || (!lucidNightmare(old) && !lucidNightmare(incoming))) return incoming;
        long oldEnd = old.tOption == 0 ? Long.MAX_VALUE : old.startTime + old.tOption;
        long newEnd = incoming.tOption == 0 ? Long.MAX_VALUE : now + incoming.tOption;
        if (oldEnd <= newEnd) return incoming;
        Option retained = old.deepCopy();
        retained.tOption = oldEnd == Long.MAX_VALUE ? 0 : (int)Math.min(Integer.MAX_VALUE, oldEnd - now);
        retained.startTime = now;
        retained.tStart = (int)now;
        return retained;
    }

    public MobTemporaryStat() {}

    public void update(Mob mob, long now) {
        boolean locked = lock.tryLock();
        if (!locked) return;
        var toRemove = new HashSet<MobStat>();
        var toRemoveBurns = new HashSet<BurnedInfo>();
        try {
            if (!currentStats.isEmpty()) {
                for (var entry : currentStats.entrySet()) {
                    var cts = entry.getKey();
                    var opt = entry.getValue();
                    if (cts.ordinal() == BurnedInfo.ordinal()) {
                        for (var burns : burnedInfos.values()) {
                            for (var burn : burns) {
                                if (burn.getEnd() < (int) now) {
                                    toRemoveBurns.add(burn);
                                }
                            }
                        }
                    } else {
                        var endTime = opt.startTime + (cts.isIndie() ? opt.tTerm : opt.tOption);
                        if (opt.tOption != 0 && endTime < now) {
                            toRemove.add(cts);
                        }
                    }
                }
            }
        } finally {
            lock.unlock();
        }
        toRemove.forEach(mobStat -> removeMobStat(mob, mobStat));
        toRemoveBurns.forEach(burnedInfo -> removeBurnedInfo(mob, burnedInfo));
    }

    public MobTemporaryStat deepCopy() {
        MobTemporaryStat copy = new MobTemporaryStat();
        copy.linkTeam = linkTeam;
        return copy;
    }

    private static Option getOptionByMobStat(Map<MobStat, Option> map, MobStat mobStat) {
        return map.getOrDefault(mobStat, null);
    }

    public Option getCurrentOptionsByMobStat(MobStat mobStat) {
        return currentStats.getOrDefault(mobStat, null);
    }

    public static int[] getMaskByCollection(TreeMap<MobStat, Option> map) {
        int[] res = new int[MobStat.LENGTH];
        for (MobStat mobStat : map.keySet()) {
            res[mobStat.getPos()] |= mobStat.getVal();
        }
        return res;
    }

    public boolean hasCurrentMobStat(MobStat mobStat) {
        if (currentStats.isEmpty()) {
            return false;
        }
        return currentStats.containsKey(mobStat);
    }

    public boolean hasCurrentMobStatBySkillId(int skillId) {
        if (currentStats.isEmpty()) {
            return false;
        }
        for (Option o : currentStats.values()) {
            if (o.rOption == skillId) {
                return true;
            }
        }
        return false;
    }

    public Set<BurnedInfo> getBurnsFromOwner(int chrId, int skillId) {
        Set<BurnedInfo> bis = new HashSet<>();
        for (Map.Entry<Integer, Set<BurnedInfo>> entry : burnedInfos.entrySet()) {
            if (entry.getKey() == chrId) {
                if (skillId == 0) {
                    bis.addAll(entry.getValue());
                } else {
                    for (net.swordie.ms.life.mob.skill.BurnedInfo burnedInfo : entry.getValue()) {
                        if (skillId == burnedInfo.getSkillId()) {
                            bis.add(burnedInfo);
                        }
                    }
                }
            }
        }
        return bis;
    }

    public Set<BurnedInfo> getAllBurns() {
        Set<BurnedInfo> bis = new HashSet<>();
        for (Map.Entry<Integer, Set<BurnedInfo>> entry : burnedInfos.entrySet()) {
            bis.addAll(entry.getValue());
        }
        return bis;
    }

    public boolean hasBurnFromOwner(int ownerCID) {
        return !getBurnsFromOwner(ownerCID, 0).isEmpty();
    }

    public boolean hasBurnFromOwner(int ownerCID, int skillID) {
        return !getBurnsFromOwner(ownerCID, skillID).isEmpty();
    }

    public TreeMap<MobStat, Option> getCurrentStatVals() {
        return new TreeMap<>(currentStats);
    }

    public void removeMobStat(Mob mob, List<MobStat> mobStats) {
        if (mob == null || mob.getHp() <= 0) {
            return;
        }
        Field field = mob.getField();
        if (field == null) {
            return;
        }
        TreeMap<MobStat, Option> expiredStats = new TreeMap<>();
        lock.lock();
        try {
            for (MobStat mobStat : mobStats) {
                expiredStats.put(mobStat, currentStats.remove(mobStat));
            }
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT_SKILL, e);
            return;
        } finally {
            lock.unlock();
        }
        if (!expiredStats.isEmpty()) {
            if (mob.getOwner() != null) {
                mob.getOwner().write(MobPacketEncoder.active().statReset(mob.getObjectId(), mob.getCalcDamageIndex(), expiredStats, null));
                mob.getOwner().write(MobPool.bindRegister(mob, mob.getMobBindInfos()));
            } else {
                field.broadcast(MobPacketEncoder.active().statReset(mob.getObjectId(), mob.getCalcDamageIndex(), expiredStats, null));
                field.broadcast(MobPool.bindRegister(mob, mob.getMobBindInfos()));
            }
        }
    }

    public void removeMobStat(Mob mob, MobStat mobStat) {
        if (mob == null || mob.getHp() <= 0) {
            return;
        }
        Field field = mob.getField();
        if (field == null) {
            return;
        }
        TreeMap<MobStat, Option> expiredStats = new TreeMap<>();
        lock.lock();
        try {
            expiredStats.put(mobStat, currentStats.remove(mobStat));
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT_SKILL, e);
            return;
        } finally {
            lock.unlock();
        }
        if (!expiredStats.isEmpty()) {
            if (mob.getOwner() != null) {
                mob.getOwner().write(MobPacketEncoder.active().statReset(mob.getObjectId(), mob.getCalcDamageIndex(), expiredStats, null));
                mob.getOwner().write(MobPool.bindRegister(mob, mob.getMobBindInfos()));
            } else {
                field.broadcast(MobPacketEncoder.active().statReset(mob.getObjectId(), mob.getCalcDamageIndex(), expiredStats, null));
                field.broadcast(MobPool.bindRegister(mob, mob.getMobBindInfos()));
            }
        }
    }

    public void addMobSkillOptions(Mob mob, MobStat mobStat, Option o) {
        o.rOption |= o.slv << 16; // mob skills are encoded differently: not an int, but short (skill ID), then short (slv).
        o.isUserSkill = false;
        addStatOptions(mob, mobStat, o);
    }

    public void addStatOptions(Mob mob, MobStat mobStat, Option option) {
        addStatOptions(mob, mobStat, option, true);
    }

    public void addStatOptions(Mob mob, MobStat mobStat, Option option, boolean isBroadcast) {
        if (mob == null || mob.getHp() <= 0) {
            return;
        }
        Field field = mob.getField();
        if (field == null) {
            return;
        }
        boolean hasFreezeEffect = mobStat == Freeze;
        final long now = System.currentTimeMillis();
        if (mobStat == Stun && lucidNightmare(option)) {
            if (!reserveLucidNightmare(mob, now)) return;
        } else if (mobStat == Stun || mobStat == Freeze || mobStat == OriginDebuff
                || mobStat == MagicCrash || mobStat == Smite) {
            if (option.rOption == Job.ORIGIN_SKILL) {
                long currentTime = now - mob.getLastDebuffTimes().getOrDefault(OriginDebuff, 0L);
                if (currentTime >= 100_000) { // 100 sec
                    mob.getLastDebuffTimes().put(OriginDebuff, now);
                } else {
                    return;
                }
            } else {
                long currentTime = now - mob.getLastDebuffTimes().getOrDefault(mobStat, 0L);
                if (currentTime >= 120_000) { // 120 sec
                    mob.getLastDebuffTimes().put(mobStat, now);
                } else {
                    return;
                }
            }
        }
        TreeMap<MobStat, Option> addList = new TreeMap<>();
        int skillID = 0;
        boolean isUserSkill = false;
        lock.lock();
        try {
            if (mobStat.isIndie()) {
                option.tTerm *= 1000;
            } else {
                option.tOption *= 1000;
            }
            option.tStart = (int) now;
            option.startTime = now;
            skillID = option.nReason > 0 ? option.nReason : option.rOption;
            if (option.rOption == Job.ORIGIN_SKILL) {
                MobBindInfo mobBindInfo = new MobBindInfo();
                mobBindInfo.setCharID(option.cOption);
                mobBindInfo.setValue(10);
                mobBindInfo.setSkillID(skillID);
                mobBindInfo.setStartTime(now);
                mobBindInfo.setEndTime(now + 100000);
                mob.getMobBindInfos().add(mobBindInfo);
            } else {
                MobBindInfo mobBindInfo = new MobBindInfo();
                mobBindInfo.setCharID(option.cOption);
                if (mobStat == Stun || mobStat == Freeze) {
                    mobBindInfo.setValue(option.nOption);
                } else if (mobStat == MagicCrash) {
                    mobBindInfo.setValue(6);
                } else if (mobStat == Smite) {
                    mobBindInfo.setValue(1);
                    mobBindInfo.setUnk(720);
                }
                mobBindInfo.setSkillID(skillID);
                mobBindInfo.setStartTime(now);
                mobBindInfo.setEndTime(lucidNightmare(option) ? lucidNightmareResistUntil : now + 120000);
                mob.getMobBindInfos().add(mobBindInfo);
            }
            isUserSkill = option.isUserSkill;
            Option applied = preserveLongerLucidStun(mobStat, option, now);
            currentStats.put(mobStat, applied);
            addList.put(mobStat, applied);
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT_SKILL, e);
            return;
        } finally {
            lock.unlock();
        }
        if (isBroadcast && !addList.isEmpty()) {
            if (mob.getOwner() != null) {
                mob.getOwner().write(MobPacketEncoder.active().statSet(mob.getObjectId(), mob.getCalcDamageIndex(), addList, new HashSet<>(), getLinkTeam()));
                if (isUserSkill) {
                    mob.getOwner().write(MobPool.affected(mob, skillID, 5, isUserSkill, (short) 0));
                }
                mob.getOwner().write(MobPool.bindRegister(mob, mob.getMobBindInfos()));
                if (hasFreezeEffect) {
                    mob.getOwner().write(MobPool.freezeEffect(mob));
                }
            } else {
                field.broadcast(MobPacketEncoder.active().statSet(mob.getObjectId(), mob.getCalcDamageIndex(), addList, new HashSet<>(), getLinkTeam()));
                if (isUserSkill) {
                    field.broadcast(MobPool.affected(mob, skillID, 5, isUserSkill, (short) 0));
                }
                field.broadcast(MobPool.bindRegister(mob, mob.getMobBindInfos()));
                if (hasFreezeEffect) {
                    field.broadcast(MobPool.freezeEffect(mob));
                }
            }
        }
    }

    public void addStatOptions(Mob mob, EnumMap<MobStat, Option> map) {
        addStatOptions(mob, map, true);
    }

    public void addStatOptions(Mob mob, EnumMap<MobStat, Option> map, boolean isBroadcast) {
        if (lucidNightmare(map.get(Stun))) {
            // Reuse the same exclusive resistance path, including mixed stat callers.
            map.forEach((stat, option) -> addStatOptions(mob, stat, option, isBroadcast));
            return;
        }
        if (mob == null || mob.getHp() <= 0) {
            return;
        }
        Field field = mob.getField();
        if (field == null) {
            return;
        }
        boolean hasFreezeEffect = map.containsKey(Freeze);
        boolean allowed = true;
        final long now = System.currentTimeMillis();
        if (map.containsKey(Stun) || map.containsKey(Freeze) || map.containsKey(OriginDebuff)
                || map.containsKey(MagicCrash) || map.containsKey(Smite)) {
            if (map.containsKey(OriginDebuff)) {
                long time = now - mob.getLastDebuffTimes().getOrDefault(OriginDebuff, 0L);
                if (time >= 100_000) { // 100 sec
                    mob.getLastDebuffTimes().put(OriginDebuff, now);
                } else {
                    allowed = false;
                }
            } else {
                for (var mobStat : map.keySet()) {
                    long time = now - mob.getLastDebuffTimes().getOrDefault(mobStat, 0L);
                    if (time >= 120_000) { // 120 sec
                        mob.getLastDebuffTimes().put(mobStat, now);
                    } else {
                        allowed = false;
                        break;
                    }
                }
            }
        }
        if (!allowed) {
            return;
        }
        TreeMap<MobStat, Option> addList = new TreeMap<>();
        int skillID = 0;
        boolean isUserSkill = false;
        lock.lock();
        try {
            for (Map.Entry<MobStat, Option> entry : map.entrySet()) {
                MobStat mobStat = entry.getKey();
                Option option = entry.getValue();
                if (mobStat.isIndie()) {
                    option.tTerm *= 1000;
                } else {
                    option.tOption *= 1000;
                }
                option.tStart = (int) now;
                option.startTime = now;
                skillID = option.nReason > 0 ? option.nReason : option.rOption;
                if (option.rOption == Job.ORIGIN_SKILL) {
                    MobBindInfo mobBindInfo = new MobBindInfo();
                    mobBindInfo.setCharID(option.cOption);
                    mobBindInfo.setValue(10);
                    mobBindInfo.setSkillID(skillID);
                    mobBindInfo.setStartTime(now);
                    mobBindInfo.setEndTime(now + 100000);
                    mob.getMobBindInfos().add(mobBindInfo);
                } else {
                    MobBindInfo mobBindInfo = new MobBindInfo();
                    mobBindInfo.setCharID(option.cOption);
                    if (mobStat == Stun || mobStat == Freeze) {
                        mobBindInfo.setValue(option.nOption);
                    } else if (mobStat == MagicCrash) {
                        mobBindInfo.setValue(6);
                    } else if (mobStat == Smite) {
                        mobBindInfo.setValue(1);
                        mobBindInfo.setUnk(720);
                    }
                    mobBindInfo.setSkillID(skillID);
                    mobBindInfo.setStartTime(now);
                    mobBindInfo.setEndTime(now + 120000);
                    mob.getMobBindInfos().add(mobBindInfo);
                }
                isUserSkill = option.isUserSkill;
                Option applied = preserveLongerLucidStun(mobStat, option, now);
                addList.put(mobStat, applied);
                currentStats.put(mobStat, applied);
            }
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT_SKILL, e);
            return;
        } finally {
            lock.unlock();
        }
        if (isBroadcast && !addList.isEmpty()) {
            if (mob.getOwner() != null) {
                mob.getOwner().write(MobPacketEncoder.active().statSet(mob.getObjectId(), mob.getCalcDamageIndex(), addList, new HashSet<>(), getLinkTeam()));
                if (isUserSkill) {
                    mob.getOwner().write(MobPool.affected(mob, skillID, 5, isUserSkill, (short) 0));
                }
                mob.getOwner().write(MobPool.bindRegister(mob, mob.getMobBindInfos()));
                if (hasFreezeEffect) {
                    mob.getOwner().write(MobPool.freezeEffect(mob));
                }
            } else {
                field.broadcast(MobPacketEncoder.active().statSet(mob.getObjectId(), mob.getCalcDamageIndex(), addList, new HashSet<>(), getLinkTeam()));
                if (isUserSkill) {
                    field.broadcast(MobPool.affected(mob, skillID, 5, isUserSkill, (short) 0));
                }
                field.broadcast(MobPool.bindRegister(mob, mob.getMobBindInfos()));
                if (hasFreezeEffect) {
                    field.broadcast(MobPool.freezeEffect(mob));
                }
            }
        }
    }

    public String getLinkTeam() {
        return linkTeam;
    }

    public static boolean hasRemovedMovementAffectingStat(Map<MobStat, Option> map) {
        return map.keySet().stream().anyMatch(MobStat::isMovementAffectingStat);
    }

    public void createAndAddBurnedInfo(Mob mob, BurnedInfo bi, int skillId) {
        if (mob == null || mob.getHp() <= 0) {
            return;
        }
        Field field = mob.getField();
        if (field == null) {
            return;
        }
        final int charID = bi.getCharacterId();
        Char chr = mob.getField().getCharByID(charID);
        if (chr == null) {
            return;
        }
        final int slv = chr.getSkillLevel(skillId);
        final SkillInfo si = SkillData.getSkillInfoById(skillId);
        final int dotSuperpos = si.getValue(SkillStat.dotSuperpos, slv);
        final int maxStacks = Math.max(1, dotSuperpos);
        if (getBurnsFromOwner(chr.getId(), skillId).size() >= maxStacks) {
            return;
        }
        TreeMap<MobStat, Option> addList = new TreeMap<>();
        Set<BurnedInfo> bis = new HashSet<>();
        lock.lock();
        try {
            Set<BurnedInfo> burnedInfoSet = burnedInfos.get(charID);
            if (burnedInfoSet != null) {
                bi.setSuperPos(burnedInfos.get(charID).size());
            } else {
                bi.setSuperPos(0);
                burnedInfos.put(charID, new HashSet<>());
            }
            long damage = bi.getDamage();
            bi.setDamageScheduledFuture(mob.getTimer().addFixedRateEvent(() -> {
                if (mob.getHp() <= 0) {
                    ScheduledFuture<?> sf = bi.getDamageScheduledFuture();
                    if (sf != null) {
                        sf.cancel(false);
                        bi.setDamageScheduledFuture(null);
                    }
                    return;
                }
                mob.damage(chr, damage, skillId);
            },bi.getAttackDelay() + bi.getInterval(), bi.getInterval(), bi.getDotCount()));
            burnedInfos.get(charID).add(bi);
            bis = burnedInfos.get(charID);
            if (!currentStats.containsKey(BurnedInfo)) {
                Option o = new Option();
                o.nOption = 0;
                o.rOption = skillId;
                currentStats.put(BurnedInfo, o);
            }
            addList.put(BurnedInfo, currentStats.get(BurnedInfo));
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT_SKILL, e);
            return;
        } finally {
            lock.unlock();
        }
        if (!bis.isEmpty()) {
            if (mob.getOwner() != null) {
                mob.getOwner().write(MobPacketEncoder.active().statSet(mob.getObjectId(), mob.getCalcDamageIndex(), addList, bis, getLinkTeam()));
                mob.getOwner().write(MobPool.bindRegister(mob, mob.getMobBindInfos()));
            } else {
                field.broadcast(MobPacketEncoder.active().statSet(mob.getObjectId(), mob.getCalcDamageIndex(), addList, bis, getLinkTeam()));
                field.broadcast(MobPool.bindRegister(mob, mob.getMobBindInfos()));
            }
        }
    }

    public void removeBurnedInfo(Mob mob, int charID, int skillID) {
        Field field = mob.getField();
        if (field == null) {
            return;
        }
        Set<BurnedInfo> burnedInfoSet = null;
        TreeMap<MobStat, Option> expiredStats = new TreeMap<>();
        lock.lock();
        try {
            burnedInfoSet = burnedInfos.get(charID);
            if (burnedInfoSet != null) {
                burnedInfos.get(charID).removeIf(burnedInfo -> burnedInfo.getSkillId() == skillID);
            } else {
                return;
            }
            if (burnedInfoSet.isEmpty()) {
                expiredStats.put(BurnedInfo, currentStats.remove(BurnedInfo));
            } else {
                expiredStats.put(BurnedInfo, currentStats.get(BurnedInfo));
            }
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT_SKILL, e);
        } finally {
            lock.unlock();
        }
        if (!expiredStats.isEmpty()) {
            if (mob.getOwner() != null) {
                mob.getOwner().write(MobPacketEncoder.active().statReset(mob.getObjectId(), mob.getCalcDamageIndex(), expiredStats, burnedInfoSet));
                mob.getOwner().write(MobPool.bindRegister(mob, mob.getMobBindInfos()));
            } else {
                field.broadcast(MobPacketEncoder.active().statReset(mob.getObjectId(), mob.getCalcDamageIndex(), expiredStats, burnedInfoSet));
                field.broadcast(MobPool.bindRegister(mob, mob.getMobBindInfos()));
            }
        }
    }

    public void removeBurnedInfo(Mob mob, BurnedInfo bi) {
        Field field = mob.getField();
        if (field == null) {
            return;
        }
        final int charID = bi.getCharacterId();
        Set<BurnedInfo> burnedInfoSet = null;
        TreeMap<MobStat, Option> expiredStats = new TreeMap<>();
        lock.lock();
        try {
            burnedInfoSet = burnedInfos.get(charID);
            if (burnedInfoSet != null) {
                burnedInfos.get(charID).removeIf(burnedInfo -> burnedInfo.getSkillId() == bi.getSkillId());
            } else {
                return;
            }
            if (burnedInfoSet.isEmpty()) {
                expiredStats.put(BurnedInfo, currentStats.remove(BurnedInfo));
            } else {
                expiredStats.put(BurnedInfo, currentStats.get(BurnedInfo));
            }
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT_SKILL, e);
        } finally {
            lock.unlock();
        }
        if (!expiredStats.isEmpty()) {
            if (mob.getOwner() != null) {
                mob.getOwner().write(MobPacketEncoder.active().statReset(mob.getObjectId(), mob.getCalcDamageIndex(), expiredStats, burnedInfoSet));
                mob.getOwner().write(MobPool.bindRegister(mob, mob.getMobBindInfos()));
            } else {
                field.broadcast(MobPacketEncoder.active().statReset(mob.getObjectId(), mob.getCalcDamageIndex(), expiredStats, burnedInfoSet));
                field.broadcast(MobPool.bindRegister(mob, mob.getMobBindInfos()));
            }
        }
    }

    public void removeBuffs(Mob mob) {
        List<MobStat> buffs = List.of(
                MobStat.PowerUp,
                MobStat.MagicUp,
                MobStat.PGuardUp,
                MobStat.MGuardUp,
                MobStat.PImmune,
                MobStat.MImmune,
                MobStat.PCounter,
                MobStat.MCounter,
                MobStat.ACC,
                MobStat.EVA
        );
        removeMobStat(mob, buffs);
    }

    public static void encodeIndieTempStat(OutPacket outPacket, TreeMap<MobStat, Option> map) {
        TreeMap<MobStat, Option> sortedStats = new TreeMap<>();
        // add removed stats into a sorted map
        for (var entry : map.entrySet()) {
            if (entry.getKey().getBitPos() < PAD.getBitPos() && entry.getValue() != null) {
                sortedStats.put(entry.getKey(), entry.getValue());
            }
        }
        for (var stat : sortedStats.entrySet()) {
            int curTime = Util.getCurrentTime();
            Option option = stat.getValue();
            if (option == null) {
                outPacket.encodeInt(0);
                continue;
            }
            outPacket.encodeInt(1); // size

            outPacket.encodeInt(option.nReason);
            outPacket.encodeInt(option.nValue);
            outPacket.encodeInt(option.nKey);
            outPacket.encodeInt(curTime - option.tStart); // elapsedTime
            outPacket.encodeInt(option.tTerm);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
        }
    }

    public static void encode(OutPacket outPacket, TreeMap<MobStat, Option> map, Set<BurnedInfo> burnedInfos, String linkteam) {
        // DecodeBuffer(12) + MobStat::DecodeTemporary
        int[] mask = getMaskByCollection(map);
        for (int i = 0; i < mask.length; i++) {
            outPacket.encodeInt(mask[i]);
        }
        List<MobStat> orderedAndFilteredCtsList = new ArrayList<>(map.keySet()).stream().filter(cts -> cts != null && cts.getOrder() != -1).sorted(Comparator.comparingInt(MobStat::getOrder)).toList();
        for (MobStat cts : orderedAndFilteredCtsList) {
            if (cts.getOrder() != -1) {
                if (cts.ordinal() < PAD.ordinal()) {
                    encodeIndieTempStat(outPacket, map);
                } else {
                    outPacket.encodeInt(getOptionByMobStat(map, cts).nOption);
                    outPacket.encodeInt(getOptionByMobStat(map, cts).rOption);
                    outPacket.encodeShort(getOptionByMobStat(map, cts).tOption / 500);
                }
            }
        }
        if (map.containsKey(PDR)) {
            outPacket.encodeInt(getOptionByMobStat(map, PDR).cOption);
        }
        if (map.containsKey(MDR)) {
            outPacket.encodeInt(getOptionByMobStat(map, MDR).cOption);
        }
        if (map.containsKey(Speed)) {
            outPacket.encodeInt(getOptionByMobStat(map, Speed).mOption);
        }
        if (map.containsKey(Stun)) {
            outPacket.encodeInt(getOptionByMobStat(map, Stun).cOption);
        }
        if (map.containsKey(Freeze)) {
            outPacket.encodeInt(getOptionByMobStat(map, Freeze).cOption);
        }
        if (map.containsKey(PCounter)) {
            outPacket.encodeInt(getOptionByMobStat(map, PCounter).wOption);
        }
        if (map.containsKey(MCounter)) {
            outPacket.encodeInt(getOptionByMobStat(map, MCounter).wOption);
        }
        if (map.containsKey(PCounter)) {
            outPacket.encodeInt(getOptionByMobStat(map, PCounter).mOption); // nCounterProb
            outPacket.encodeByte(getOptionByMobStat(map, PCounter).bOption); // bCounterDelay
            outPacket.encodeInt(getOptionByMobStat(map, PCounter).nReason); // nAggroRank
        } else if (map.containsKey(MCounter)) {
            outPacket.encodeInt(getOptionByMobStat(map, MCounter).mOption); // nCounterProb
            outPacket.encodeByte(getOptionByMobStat(map, MCounter).bOption); // bCounterDelay
            outPacket.encodeInt(getOptionByMobStat(map, MCounter).nReason); // nAggroRank
        }
        if (map.containsKey(ReduceFinalDamage)) {
            outPacket.encodeByte(getOptionByMobStat(map, ReduceFinalDamage).bOption);
            outPacket.encodeInt(getOptionByMobStat(map, ReduceFinalDamage).xOption);
        }
        if (map.containsKey(TotalDamParty)) {
            outPacket.encodeInt(getOptionByMobStat(map, TotalDamParty).wOption);
            outPacket.encodeInt(getOptionByMobStat(map, TotalDamParty).pOption);
            outPacket.encodeInt(getOptionByMobStat(map, TotalDamParty).cOption);
        }
        if (map.containsKey(Fatality)) {
            outPacket.encodeInt(getOptionByMobStat(map, Fatality).wOption);
            outPacket.encodeInt(getOptionByMobStat(map, Fatality).uOption);
            outPacket.encodeInt(getOptionByMobStat(map, Fatality).pOption);
        }
        if (map.containsKey(SpiritGate)) {
            outPacket.encodeInt(getOptionByMobStat(map, SpiritGate).xOption);
        }
        if (map.containsKey(CurseTransition)) {
            outPacket.encodeInt(getOptionByMobStat(map, CurseTransition).xOption); // sniff show always encoding of 1
        }
        if (map.containsKey(ElementDarkness)) {
            outPacket.encodeInt(getOptionByMobStat(map, ElementDarkness).xOption);
        }
        if (map.containsKey(DeadlyCharge)) {
            outPacket.encodeInt(getOptionByMobStat(map, DeadlyCharge).pOption);
            outPacket.encodeInt(getOptionByMobStat(map, DeadlyCharge).pOption);
        }
        if (map.containsKey(Incizing)) {
            outPacket.encodeInt(getOptionByMobStat(map, Incizing).wOption);
            outPacket.encodeInt(getOptionByMobStat(map, Incizing).uOption);
            outPacket.encodeInt(getOptionByMobStat(map, Incizing).pOption);
        }
        if (map.containsKey(BMageDebuff)) {
            outPacket.encodeInt(getOptionByMobStat(map, BMageDebuff).cOption);
        }
        if (map.containsKey(BattlePvPHelenaMark)) {
            outPacket.encodeInt(getOptionByMobStat(map, BattlePvPHelenaMark).cOption);
        }
        if (map.containsKey(BattlePvP_Darklord_Explosion)) {
            outPacket.encodeInt(getOptionByMobStat(map, BattlePvP_Darklord_Explosion).cOption);
        }
        if (map.containsKey(MultiPMDR)) {
            outPacket.encodeInt(getOptionByMobStat(map, MultiPMDR).cOption);
        }
        if (map.containsKey(BahamutLightElemAddDam)) {
            outPacket.encodeInt(getOptionByMobStat(map, BahamutLightElemAddDam).pOption);
            outPacket.encodeInt(getOptionByMobStat(map, BahamutLightElemAddDam).cOption);
        }
        if (map.containsKey(MultiDamSkill)) {
            outPacket.encodeInt(getOptionByMobStat(map, MultiDamSkill).cOption);
        }
        if (map.containsKey(LefDebuff)) {
            outPacket.encodeInt(getOptionByMobStat(map, LefDebuff).xOption); // -DEF%
            outPacket.encodeInt(getOptionByMobStat(map, LefDebuff).yOption);
            outPacket.encodeInt(getOptionByMobStat(map, LefDebuff).zOption); // rOption
        }
        if (map.containsKey(BuffControl)) {
            outPacket.encodeInt(getOptionByMobStat(map, BuffControl).xOption);
        }
        if (map.containsKey(BattlePvP_Ryude_Frozen)) {
            outPacket.encodeInt(getOptionByMobStat(map, BattlePvP_Ryude_Frozen).xOption);
        }
        if (map.containsKey(Poison)) {
            outPacket.encodeInt(getOptionByMobStat(map, Poison).xOption);
        }
        if (map.containsKey(Ambush)) {
            outPacket.encodeInt(getOptionByMobStat(map, Ambush).xOption);
        }
        if (map.containsKey(WindBreakerPinpointPierce)) {
            outPacket.encodeInt(getOptionByMobStat(map, WindBreakerPinpointPierce).xOption);
        }
        if (map.containsKey(MobLock)) {
            outPacket.encodeInt(getOptionByMobStat(map, MobLock).xOption);
        }
        if (map.containsKey(LWGathering)) {
            outPacket.encodeInt(getOptionByMobStat(map, LWGathering).xOption);
        }
        if (map.containsKey(Panic)) {
            outPacket.encodeInt(getOptionByMobStat(map, Panic).xOption);
            outPacket.encodeInt(getOptionByMobStat(map, Panic).yOption);
        }
        if (map.containsKey(AfterImage)) {
            outPacket.encodeInt(getOptionByMobStat(map, AfterImage).xOption);
        }
        if (map.containsKey(Weakness)) {
            outPacket.encodeInt(getOptionByMobStat(map, Weakness).xOption);
        }
        if (map.containsKey(Explosion)) {
            outPacket.encodeInt(getOptionByMobStat(map, Explosion).wOption);
        }
        if (map.containsKey(BurnedInfo)) {
            outPacket.encodeByte(burnedInfos.size());
            for (BurnedInfo bi : burnedInfos) {
                bi.encode(outPacket);
            }
        }
        if (map.containsKey(NewBurnedInfo)) {
            List<Option> values = getOptionByMobStat(map, NewBurnedInfo).extraOpts;
            outPacket.encodeInt(values.size());
            if (!values.isEmpty()) {
                for (Option option : values) {
                    outPacket.encodeInt(option.nOption);
                    outPacket.encodeInt(option.cOption); // charID
                    outPacket.encodeInt(option.rOption); // skillID1
                    outPacket.encodeInt(option.mOption); // skillID2
                    outPacket.encodeInt(option.xOption); // -122
                    outPacket.encodeInt(option.yOption); // 500
                    outPacket.encodeInt(option.zOption); // 510
                    outPacket.encodeInt(option.tOption); // 5000
                    outPacket.encodeInt(option.uOption); // 10
                    outPacket.encodeInt(0); // 0
                    outPacket.encodeInt(1); // 1
                    outPacket.encodeInt(1); // 1
                }
            }
        }
        if (map.containsKey(Sleep)) {
            outPacket.encodeByte(getOptionByMobStat(map, Sleep).nOption);
            outPacket.encodeByte(getOptionByMobStat(map, Sleep).bOption);
        }
        if (map.containsKey(ExchangeAttack)) {
            outPacket.encodeByte(getOptionByMobStat(map, ExchangeAttack).bOption);
        }
        if (map.containsKey(ExtraBuffStat)) {
            List<Option> values = getOptionByMobStat(map, ExtraBuffStat).extraOpts;
            outPacket.encodeByte(!values.isEmpty());
            if (!values.isEmpty()) {
                outPacket.encodeInt(getOptionByMobStat(map, ExtraBuffStat).extraOpts.getFirst().nOption); // nPAD
                outPacket.encodeInt(getOptionByMobStat(map, ExtraBuffStat).extraOpts.getFirst().mOption); // nMAD
                outPacket.encodeInt(getOptionByMobStat(map, ExtraBuffStat).extraOpts.getFirst().xOption); // nPDR
                outPacket.encodeInt(getOptionByMobStat(map, ExtraBuffStat).extraOpts.getFirst().yOption); // nMDR
            }
        }
        if (map.containsKey(LinkTeam)) {
            outPacket.encodeString(linkteam);
        }
        if (map.containsKey(Unk129)) {
            outPacket.encodeInt(getOptionByMobStat(map, Unk129).xOption);
        }
        if (map.containsKey(Unk130)) { // 121
            outPacket.encodeLong(getOptionByMobStat(map, Unk130).xOption);
        }
        if (map.containsKey(Unk131)) {
            outPacket.encodeInt(getOptionByMobStat(map, Unk131).xOption);
        }
        if (map.containsKey(Unk132)) {
            outPacket.encodeInt(getOptionByMobStat(map, Unk132).xOption);
        }
        if (map.containsKey(Unk134)) {
            outPacket.encodeInt(getOptionByMobStat(map, Unk134).xOption);
        }
        if (map.containsKey(Unk134)) {
            outPacket.encodeInt(getOptionByMobStat(map, Unk134).xOption);
        }
        if (map.containsKey(Unk135)) {
            outPacket.encodeInt(getOptionByMobStat(map, Unk135).xOption);
        }
        if (map.containsKey(OriginDebuff)) {
            outPacket.encodeShort(getOptionByMobStat(map, OriginDebuff).xOption);
        }
        if (map.containsKey(SoulExplosion)) {
            outPacket.encodeInt(getOptionByMobStat(map, SoulExplosion).nOption);
            outPacket.encodeInt(getOptionByMobStat(map, SoulExplosion).rOption);
            outPacket.encodeInt(getOptionByMobStat(map, SoulExplosion).wOption);
        }
        if (map.containsKey(TrueSight)) {
            outPacket.encodeInt(getOptionByMobStat(map, TrueSight).xOption);
            outPacket.encodeInt(getOptionByMobStat(map, TrueSight).yOption);
            outPacket.encodeInt(getOptionByMobStat(map, TrueSight).zOption);
            outPacket.encodeInt(getOptionByMobStat(map, TrueSight).cOption);
            outPacket.encodeInt(getOptionByMobStat(map, TrueSight).pOption);
            outPacket.encodeInt(getOptionByMobStat(map, TrueSight).uOption);
            outPacket.encodeInt(getOptionByMobStat(map, TrueSight).wOption);
        }
        if (map.containsKey(Laser)) {
            outPacket.encodeInt(getOptionByMobStat(map, Laser).nOption);
            outPacket.encodeInt(getOptionByMobStat(map, Laser).rOption);
            outPacket.encodeInt(getOptionByMobStat(map, Laser).tOption / 500);
            outPacket.encodeInt(getOptionByMobStat(map, Laser).wOption);
            outPacket.encodeInt(getOptionByMobStat(map, Laser).uOption);
        }
        if (map.containsKey(Unk128)) {
            outPacket.encodeInt(getOptionByMobStat(map, Unk128).xOption);
            outPacket.encodeInt(getOptionByMobStat(map, Unk128).yOption);
            outPacket.encodeInt(getOptionByMobStat(map, Unk128).zOption);
        }
        if (map.containsKey(Unk136)) {
            outPacket.encodeInt(getOptionByMobStat(map, Unk136).xOption);
            outPacket.encodeInt(getOptionByMobStat(map, Unk136).yOption);
        }
        if (map.containsKey(Unk137)) {
            outPacket.encodeInt(getOptionByMobStat(map, Unk137).xOption);
            outPacket.encodeInt(getOptionByMobStat(map, Unk137).yOption);
            outPacket.encodeInt(getOptionByMobStat(map, Unk137).zOption);
        }
        if (map.containsKey(Unk139)) {
            outPacket.encodeLong(getOptionByMobStat(map, Unk139).xOption);
        }
        if (map.containsKey(ChangeMobAction)) {
            outPacket.encodeInt(getOptionByMobStat(map, ChangeMobAction).xOption);
        }
        if (map.containsKey(Unk138)) {
            outPacket.encodeInt(getOptionByMobStat(map, Unk138).xOption);
            outPacket.encodeInt(getOptionByMobStat(map, Unk138).yOption);
            outPacket.encodeShort(getOptionByMobStat(map, Unk138).zOption);
        }
        if (map.containsKey(IndieAddFinalDamSkill)) {
            outPacket.encodeInt(getOptionByMobStat(map, IndieAddFinalDamSkill).xOption);
            outPacket.encodeInt(getOptionByMobStat(map, IndieAddFinalDamSkill).yOption);
            outPacket.encodeInt(getOptionByMobStat(map, IndieAddFinalDamSkill).zOption);
        }
        if (map.containsKey(CatKnitting)) {
            outPacket.encodeInt(getOptionByMobStat(map, CatKnitting).xOption);
            outPacket.encodeInt(getOptionByMobStat(map, CatKnitting).yOption);
        }
    }
}
