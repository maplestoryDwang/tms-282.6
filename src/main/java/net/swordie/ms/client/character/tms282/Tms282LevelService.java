package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.Tms282StatRecoveryService;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.jobs.special.SpecialJobProgression;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282SkillPackets;
import net.swordie.ms.connection.packet.Tms282StatPackets;
import net.swordie.ms.constants.EventConstants;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.loaders.SkillData;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * TMS 282.3 level-up on the 265 rules, without a Server/World/Field: a port of the numeric part of
 * {@code Char.addExp} (EXP table, multi-level steps of the global burning events) and {@code Job.handleLevelUp}
 * (AP, SP incl. extended SP, HP/MP gains, level-140 skill maxing, special event jobs' automatic skills), then a
 * full heal to the 265 total max ({@link Tms282StatRecoveryService#maxHpMp}).
 * <p>
 * Honor EXP (名聲值, LP0139) and the inner ability lines due at 30 / 50 (LP0137) are given as in 265
 * ({@link Tms282InnerAbility}); a character reaching 140 gets its hyper stat rows on the first 極限屬性 request.
 * <p>
 * Not ported (logged when they would happen): link skills, level rewards and guide popups,
 * achievements, auto-AP, hyper stat init, guild notices, the level-up effect (LP04CA effect type codes are not
 * verified for 282, so no effect is sent).
 * <p>
 * Packets: LP009D ({@link Tms282StatPackets}: level, exp, ap, base mhp/mmp, hp, mp), LP009D SP
 * ({@link Tms282SkillPackets#spChanged}) when SP changed, LP00A2 for skills learned or maxed. Everything is written
 * through {@link Tms282CharacterStore}.
 */
public final class Tms282LevelService {
    private final Tms282CharacterStore store;

    public Tms282LevelService(Tms282CharacterStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    /** What a call changed. {@code spGained}: SP added per level reached (0 for none), in order. */
    public record Result(int fromLevel, int toLevel, long exp, Map<Integer, Integer> spGained,
                         List<Skill> skillsChanged, List<OutPacket> packets, List<String> log) {
        public Result {
            spGained = Map.copyOf(spGained);
            skillsChanged = List.copyOf(skillsChanged);
            packets = List.copyOf(packets);
            log = List.copyOf(log);
        }

        public int totalSpGained() {
            return spGained.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    /** {@code count} level-up events: each adds exactly the EXP missing to the next level (265 addExp). */
    public Result gainLevels(Char chr, int count) {
        if (count < 1) {
            throw new IllegalArgumentException("count must be at least 1");
        }
        CharacterStat stat = stat(chr);
        Session s = new Session(chr, stat);
        for (int i = 0; i < count && stat.getLevel() < GameConstants.MAX_LEVEL; i++) {
            long missing = GameConstants.charExp[stat.getLevel()] - Math.abs(stat.getExp());
            s.addExp(Math.max(1, missing));
        }
        return s.finish();
    }

    /** 265 {@code Char.addExp(amount)} (without the EXP-rate / death-penalty quest parts). */
    public Result addExp(Char chr, long amount) {
        Session s = new Session(chr, stat(chr));
        s.addExp(amount);
        return s.finish();
    }

    /** Adds SP to the character's current job level (265 {@code Char.addSpToJobByCurrentLevel}). */
    public Result addSp(Char chr, int sp) {
        if (sp < 1) {
            throw new IllegalArgumentException("sp must be at least 1");
        }
        Session s = new Session(chr, stat(chr));
        chr.addSpToJobByCurrentLevel(sp);
        s.spChanged = true;
        s.spGained.put((int) stat(chr).getLevel(), sp);
        s.log.add("SP +" + sp + " at job level " + JobConstants.getJobLevelByCharLevel(chr.getJob(), chr.getLevel(), chr.getSubJob()));
        return s.finish();
    }

    /** SP 265 grants when reaching {@code level} with an ordinary job (Job.handleLevelUp, level != 140). */
    public static int baseSpForLevel(int level) {
        int sp = SkillConstants.getBaseSpByLevel((short) level);
        if ((level % 10) % 3 == 0 && level > 100) {
            sp *= 2; // 265: (level % 10) % 3 == 0, i.e. levels ending in 0, 3, 6, 9 (its comment says 3/6/9)
        }
        return sp;
    }

    /** Burning step of 265 addExp when crossing one EXP threshold at {@code level}. */
    public static int burningStep(int level, boolean zero) {
        final int hyperMax = 260;
        final int beyondMax = 270;
        int step = 1;
        if (EventConstants.HYPER_BURNING_MAX && level < hyperMax) {
            int start = zero ? 100 : 10;
            if (level >= start) {
                step = Math.max(1, Math.min(5, hyperMax - level));
            }
        } else if (EventConstants.BEYOND_BURNING && level >= hyperMax && level < beyondMax) {
            step = Math.max(1, Math.min(2, beyondMax - level));
        }
        return step;
    }

    private final class Session {
        final Char chr;
        final CharacterStat stat;
        final int fromLevel;
        final Map<Integer, Integer> spGained = new LinkedHashMap<>();
        final List<Skill> skills = new ArrayList<>();
        final List<String> log = new ArrayList<>();
        boolean spChanged;
        boolean statsChanged;
        int honorGained;

        Session(Char chr, CharacterStat stat) {
            this.chr = chr;
            this.stat = stat;
            this.fromLevel = stat.getLevel();
        }

        void addExp(long amount) {
            if (amount <= 0) {
                return;
            }
            int level = stat.getLevel();
            long exp = Math.abs(stat.getExp());
            statsChanged = true;
            if (level >= GameConstants.MAX_LEVEL) {
                stat.setExp(0);
                return;
            }
            long newExp = exp + amount;
            boolean zero = JobConstants.isZero(stat.getJob());
            while (level < GameConstants.MAX_LEVEL && newExp >= GameConstants.charExp[level]) {
                newExp -= GameConstants.charExp[level];
                int step = burningStep(level, zero);
                for (int i = 0; i < step && level < GameConstants.MAX_LEVEL; i++) {
                    level++;
                    stat.setLevel(level);
                    levelUp((short) level);
                }
            }
            if (level >= GameConstants.MAX_LEVEL) {
                newExp = 0;
            }
            stat.setExp(Math.abs(newExp));
        }

        /** Job.handleLevelUp(level), numeric part. */
        void levelUp(short level) {
            short job = stat.getJob();
            if (level > 10) {
                stat.setAp(stat.getAp() + 5);
            }
            int[] gain = GameConstants.getHpMpPerLevel(job);
            stat.setMaxHp(stat.getMaxHp() + gain[0]);
            if (!JobConstants.isNoManaJob(job)) {
                stat.setMaxMp(stat.getMaxMp() + gain[1]);
            }
            if (JobConstants.isSpecialEventJob(job)) {
                for (Skill skill : SpecialJobProgression.getLearnableSkills(job, level)) {
                    if (chr.getSkillLevel(skill.getSkillId()) >= skill.getCurrentLevel()) {
                        continue;
                    }
                    Skill owned = chr.getSkill(skill.getSkillId());
                    Skill record = owned != null ? owned : skill;
                    record.setCurrentLevel(skill.getCurrentLevel());
                    record.setCharId(chr.getId());
                    if (owned == null) {
                        chr.putSkill(record);
                    }
                    remember(record);
                }
                spGained.put((int) level, 0);
                return;
            }
            honorGained += Tms282InnerAbility.honorForLevel(level);
            if (Tms282SkillLearning.enabled() && JobConstants.isZero(job)) {
                int sp = net.swordie.ms.client.jobs.Zero.spPerSetAtLevel(level);
                if (sp > 0) {
                    for (var set : stat.getExtendSP().getSpSet()) set.addSp(sp);
                    spChanged = true;
                }
                spGained.put((int) level, sp * stat.getExtendSP().getSpSet().size());
                return;
            }
            int sp = 0;
            if (level == 140) {
                for (Skill skill : SkillData.getSkillsByJob(job)) {
                    Skill cur = chr.getSkill(skill.getSkillId());
                    if (cur != null && cur.getRootId() == job && !SkillConstants.isHyperSkill(job, skill.getSkillId())
                            && !SkillConstants.isHyperStat(skill.getSkillId())) {
                        sp += skill.getMaxLevel() - cur.getCurrentLevel();
                    }
                }
                for (Skill skill : new ArrayList<>(chr.getSkills())) {
                    if (job - skill.getRootId() >= 1 && SkillConstants.isMatching(skill.getRootId(), job)
                            && skill.getCurrentLevel() != skill.getMaxLevel()
                            && !JobConstants.isBeastTamer(job) && !JobConstants.isZero(job)) {
                        skill.setCurrentLevel(skill.getMaxLevel());
                        remember(skill);
                    }
                }
                log.add("L140: previous-job skills maxed; hyper stat init not ported");
            } else {
                sp = baseSpForLevel(level);
            }
            if (!JobConstants.isBeastTamer(job) && !JobConstants.isZero(job)) {
                chr.addSpToJobByCurrentLevel(sp);
                spChanged = true;
                spGained.put((int) level, sp);
            } else {
                spGained.put((int) level, 0);
            }
        }

        void remember(Skill skill) {
            skills.removeIf(s -> s.getSkillId() == skill.getSkillId());
            skills.add(skill);
        }

        Result finish() {
            int toLevel = stat.getLevel();
            List<OutPacket> packets = new ArrayList<>();
            boolean saved = false;
            if (Tms282SkillLearning.enabled() && JobConstants.isZero(stat.getJob())) {
                for (Skill skill : net.swordie.ms.client.jobs.Zero.availableLevelSkills(toLevel)) {
                    if (chr.getSkillLevel(skill.getSkillId()) >= skill.getCurrentLevel()) continue;
                    skill.setCharId(chr.getId());
                    Tms282SkillLearning.putWithStats(chr, skill);
                    remember(skill);
                }
                if (toLevel >= 200) {
                    int previousSp = stat.getExtendSP().getTotalSp();
                    net.swordie.ms.client.jobs.Zero.maxBaseSkillsAt200(chr, skill -> {
                        Tms282SkillLearning.putWithStats(chr, skill);
                        remember(skill);
                    });
                    spChanged |= previousSp != stat.getExtendSP().getTotalSp();
                }
            }
            if (toLevel != fromLevel) {
                Tms282SkillLearning.reconcileFixed(chr, skill -> {
                    Tms282SkillLearning.putWithStats(chr, skill);
                    remember(skill);
                });
                Tms282StatRecoveryService.MaxHpMp cap = Tms282StatRecoveryService.maxHpMp(chr);
                stat.setHp(cap.maxHp());
                stat.setMp(cap.maxMp());
                log.add("healed to " + (cap.fromTotal() ? "265 total" : "base") + " max " + cap.maxHp() + "/" + cap.maxMp());
                log.add("level-up effect (LP04CA/LP048E) not sent: 282 effect type codes unverified");
            }
            boolean zeroMilestonePending = Tms282SkillLearning.enabled() && JobConstants.isZero(stat.getJob())
                    && toLevel >= 200 && chr.hasQuestCompleted(1461) && !chr.hasQuestCompleted(1465);
            if (statsChanged || spChanged || !skills.isEmpty() || zeroMilestonePending) {
                try {
                    for (Skill skill : skills) {
                        store.saveSkill(skill);
                    }
                    store.saveStats(stat);
                    saved = true;
                    log.add("saved characterstats" + (skills.isEmpty() ? "" : " + " + skills.size() + " skill rows"));
                } catch (RuntimeException e) {
                    log.add("SAVE FAILED " + e.getClass().getSimpleName() + " (kept in memory)");
                }
            }
            if (saved && Tms282SkillLearning.enabled() && toLevel >= 200
                    && chr.getJobHandler() instanceof net.swordie.ms.client.jobs.Zero zero) {
                try { zero.completeLevel200Milestone(); }
                catch (RuntimeException e) {
                    // Numeric progress is already saved. Retain it and retry the durable quest gate later.
                    log.add("Zero level200 milestone retry required: " + e.getClass().getSimpleName());
                }
            }
            if (statsChanged) {
                Map<Tms282StatPackets.Stat, Long> values = new EnumMap<>(Tms282StatPackets.Stat.class);
                values.put(Tms282StatPackets.Stat.LEVEL, (long) toLevel);
                values.put(Tms282StatPackets.Stat.EXP, stat.getExp());
                values.put(Tms282StatPackets.Stat.AP, (long) stat.getAp());
                values.put(Tms282StatPackets.Stat.MAX_HP, (long) stat.getMaxHp());
                values.put(Tms282StatPackets.Stat.MAX_MP, (long) stat.getMaxMp());
                values.put(Tms282StatPackets.Stat.HP, (long) stat.getHp());
                values.put(Tms282StatPackets.Stat.MP, (long) stat.getMp());
                packets.add(Tms282StatPackets.statChanged(false, values));
            }
            if (spChanged) {
                packets.add(Tms282SkillPackets.spChanged(false, stat.getJob(), stat));
            }
            if (!skills.isEmpty()) {
                packets.add(Tms282SkillPackets.changeSkillRecordResult(skills, true, false, false));
            }
            if (honorGained > 0) {
                chr.setHonorExp(chr.getHonorExp() + honorGained);
                packets.add(net.swordie.ms.connection.packet.Tms282InnerAbilityPackets.honorExp(chr.getHonorExp()));
                log.add("honor +" + honorGained + " -> " + chr.getHonorExp());
            }
            if ((fromLevel < 30 && toLevel >= 30) || (fromLevel < 50 && toLevel >= 50)) {
                if (Tms282InnerAbility.unlockLines(chr)) {
                    packets.add(net.swordie.ms.connection.packet.Tms282InnerAbilityPackets.reset(chr));
                    log.add("inner ability lines unlocked");
                }
            }
            return new Result(fromLevel, toLevel, stat.getExp(), spGained, skills, packets, log);
        }
    }

    private static CharacterStat stat(Char chr) {
        Objects.requireNonNull(chr, "chr");
        if (chr.getAvatarData() == null || chr.getAvatarData().getCharacterStat() == null) {
            throw new IllegalArgumentException("Character " + chr.getId() + " has no CharacterStat");
        }
        return chr.getAvatarData().getCharacterStat();
    }
}
