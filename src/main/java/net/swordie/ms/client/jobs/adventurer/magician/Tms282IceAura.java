package net.swordie.ms.client.jobs.adventurer.magician;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.loaders.SkillData;
import java.util.*;
import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

/** 282's 2201009 on/off aura. Existing character update owns all work; no client-provided owner/level is trusted. */
public final class Tms282IceAura {
    public static final int SKILL = 2201009;
    private final Char owner;
    private final Set<Char> recipients = Collections.newSetFromMap(new IdentityHashMap<>());
    private long lastTick;

    public Tms282IceAura(Char owner) { this.owner = owner; }

    public static boolean active(Char chr) {
        Option o = chr.getTemporaryStatManager().getOption(IceAura);
        return o.nOption > 0 && o.rOption == SKILL && o.xOption == chr.getId();
    }

    public synchronized void toggle() {
        if (active(owner)) { stop(); return; }
        if (owner.getSkillLevel(SKILL) <= 0 || owner.getHP() <= 0) return;
        if (owner.getTemporaryStatManager().getOption(IceAuraZone).rOption == SKILL)
            owner.getTemporaryStatManager().removeStat(IceAuraZone);
        Option o = new Option();
        // Native 11772919 places IceAura.n into the outbound aura entry's skill-level field.
        o.nOption = owner.getSkillLevel(SKILL); o.rOption = SKILL; o.xOption = owner.getId();
        owner.getTemporaryStatManager().sendStat(IceAura, o);
        lastTick = System.currentTimeMillis();
        sync();
    }

    public synchronized void update(long now) {
        if (!active(owner) || owner.getHP() <= 0 || owner.getSkillLevel(SKILL) <= 0 || owner.getField() == null) {
            stop(); return;
        }
        if (now - lastTick < 1000) return;
        // Native 5FF37C4 has a 1000 ms aura-update gate. WZ describes 60 MP per second.
        SkillInfo si = SkillData.getSkillInfoById(SKILL);
        if (!owner.applyMpCon(si, owner.getSkillLevel(SKILL), true)) { stop(); return; }
        lastTick = now;
        sync();
    }

    private void sync() {
        SkillInfo si = SkillData.getSkillInfoById(SKILL);
        Option controller = owner.getTemporaryStatManager().getOption(IceAura);
        if (controller.nOption != owner.getSkillLevel(SKILL)) {
            controller = controller.deepCopy(); controller.nOption = owner.getSkillLevel(SKILL);
            owner.getTemporaryStatManager().sendStat(IceAura, controller);
        }
        Set<Char> wanted = Collections.newSetFromMap(new IdentityHashMap<>());
        wanted.add(owner);
        if (owner.getParty() != null && owner.getField() != null) {
            var rect = owner.getRectAround(si.getFirstRect());
            for (Char chr : owner.getParty().getPartyMembersInSameField(owner)) {
                if (chr.getHP() > 0 && chr.getField() == owner.getField() && rect.hasPositionInside(chr.getPosition())
                        && !active(chr)) wanted.add(chr);
            }
        }
        for (Char old : new ArrayList<>(recipients)) if (!wanted.contains(old)) { clear(old); recipients.remove(old); }
        for (Char chr : wanted) {
            Option previous = chr.getTemporaryStatManager().getOption(IceAuraZone);
            if (chr != owner && previous.rOption == SKILL && previous.xOption != owner.getId()) {
                Char other = owner.getField().getCharByID(previous.xOption);
                if (other != null && other.getHP() > 0 && other.getParty() == chr.getParty() && active(other)
                        && other.getSkillLevel(SKILL) >= owner.getSkillLevel(SKILL)
                        && other.getRectAround(si.getFirstRect()).hasPositionInside(chr.getPosition())) continue;
            }
            apply(chr, si); recipients.add(chr);
        }
    }

    private void apply(Char chr, SkillInfo si) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int slv = owner.getSkillLevel(SKILL);
        Option zone = tsm.getOption(IceAuraZone);
        if (hasBenefit(tsm, IndieAsrR, si.getValue(z, slv)) && hasBenefit(tsm, IndieTerR, si.getValue(w, slv))
                && hasBenefit(tsm, IndieNormalDamReduceR, si.getValue(y, slv))
                && (chr == owner || zone.rOption == SKILL && zone.xOption == owner.getId() && zone.nOption > 0)) return;
        var stats = new EnumMap<CharacterTemporaryStat, Option>(CharacterTemporaryStat.class);
        if (chr != owner) {
            zone = new Option(); zone.nOption = 1; zone.rOption = SKILL; zone.xOption = owner.getId();
            stats.put(IceAuraZone, zone);
        }
        // Native 7C53FA and 7C5793 apply these reductions in the same normal-hit branch.
        // Indie67 has independent source ownership; standard DamageReduce has only one slot.
        Option reduction = new Option(); reduction.nValue = si.getValue(y, slv); reduction.nReason = SKILL; reduction.xOption = owner.getId();
        stats.put(IndieNormalDamReduceR, reduction);
        Option asr = new Option(); asr.nValue = si.getValue(z, slv); asr.nReason = SKILL; asr.xOption = owner.getId();
        stats.put(IndieAsrR, asr);
        Option ter = asr.deepCopy(); ter.nValue = si.getValue(w, slv); stats.put(IndieTerR, ter);
        tsm.sendStat(stats);
    }

    private boolean hasBenefit(TemporaryStatManager tsm, CharacterTemporaryStat stat, int value) {
        Option option = tsm.getOptByCTSAndSkill(stat, SKILL);
        return option != null && option.xOption == owner.getId() && option.nValue == value;
    }

    private void clear(Char chr) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option zone = tsm.getOption(IceAuraZone);
        if (zone.rOption == SKILL && zone.xOption == owner.getId()) tsm.removeStat(IceAuraZone);
        for (var cts : List.of(IndieAsrR, IndieTerR, IndieNormalDamReduceR)) {
            Option o = tsm.getOptByCTSAndSkill(cts, SKILL);
            if (o != null && o.xOption == owner.getId()) tsm.removeIndieStat(cts, o);
        }
    }

    public synchronized void stop() {
        if (active(owner)) owner.getTemporaryStatManager().removeStat(IceAura);
        for (Char chr : new ArrayList<>(recipients)) clear(chr);
        recipients.clear();
    }
}
