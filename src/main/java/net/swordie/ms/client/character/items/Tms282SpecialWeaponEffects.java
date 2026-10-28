package net.swordie.ms.client.character.items;

import java.util.*;
import java.lang.ref.WeakReference;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.connection.packet.Tms282SpecialWeaponPackets;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.util.Rect;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.header.OutHeader;

/** Original282 weapon invulnerability and explicitly authorized early-cancel bursts. */
public final class Tms282SpecialWeaponEffects {
    public static final int INDOMITABLE = 80004116;
    private static final int[] ACTIVES = {80002633,80003874,INDOMITABLE};
    // At most one pending packet per character. Weak field/client links prevent values retaining the weak key.
    private static final Map<Char, Burst> PENDING = Collections.synchronizedMap(new WeakHashMap<>());
    public record Burst(Equip weapon, WeakReference<Field> field, WeakReference<Client> client,
                        int parent, Rect rect, long expires) { }
    private Tms282SpecialWeaponEffects() { }

    private static CharacterTemporaryStat stat(int skill) {
        return switch(skill) {
            case 80002633 -> CharacterTemporaryStat.BlackMageWeaponCreation;
            case 80003874 -> CharacterTemporaryStat.DestinyWeaponIndomitable;
            case INDOMITABLE -> CharacterTemporaryStat.Destiny2ndWeaponIndomitable;
            default -> throw new IllegalArgumentException("Unsupported weapon active");
        };
    }

    public static boolean managedStat(CharacterTemporaryStat stat, Option option) {
        if (option == null) return false;
        for (int skill : ACTIVES)
            if (stat == stat(skill) && option.rOption == skill
                    || stat == CharacterTemporaryStat.IndieNotDamaged && option.nReason == skill) return true;
        return false;
    }

    public static boolean preflight(Char chr, int level) {
        return preflight(chr, INDOMITABLE, level);
    }
    public static boolean preflight(Char chr, int skill, int level) {
        return level == 1 && Tms282SpecialWeaponSkills.sourceEligible(chr, skill)
                && chr.getField() != null && chr.getClient() != null && chr.getClient().getChr() == chr
                && chr.getJobHandler() != null;
    }

    /** Called only after Service validates cooldown and generic costs; effects do not depend on the Job adapter. */
    public static boolean activate(Char chr, int level) {
        return activate(chr, INDOMITABLE, level);
    }
    public static boolean activate(Char chr, int skill, int level) {
        if (chr == null) return false;
        synchronized (chr) {
            if (!preflight(chr, skill, level) || activeOption(chr, skill, System.currentTimeMillis())) return false;
            int seconds = SkillData.getSkillInfoById(skill).getValue(SkillStat.time, level);
            if (seconds <= 0) return false;
            var stats = new EnumMap<CharacterTemporaryStat, Option>(CharacterTemporaryStat.class);
            Option visual = new Option(); visual.nOption = 1; visual.rOption = skill; visual.tOption = seconds;
            Option protection = new Option(); protection.nValue = 1; protection.nReason = skill; protection.tTerm = seconds;
            stats.put(stat(skill), visual); stats.put(CharacterTemporaryStat.IndieNotDamaged, protection);
            PENDING.remove(chr);
            chr.getTemporaryStatManager().sendStat(stats);
            return activeOption(chr, skill, System.currentTimeMillis());
        }
    }

    private static boolean activeOption(Char chr, int skill, long now) {
        Option o = chr.getTemporaryStatManager().getOption(stat(skill));
        long duration = o.isInMillis ? o.tOption : o.tOption * 1000L;
        return o.rOption == skill && o.nOption > 0 && duration > 0 && now < o.startTime + duration;
    }

    /** Recast ends the existing protection without charging/restarting its120s cooldown or creating an attack. */
    public static boolean cancelActive(Char chr, long now) {
        return cancelActive(chr, INDOMITABLE, now);
    }
    public static boolean cancelActive(Char chr, int skill, long now) {
        if (chr == null || Protocol.active().version() != ProtocolVersion.TMS_282_3) return false;
        synchronized (chr) {
            if (!preflight(chr, skill, 1) || !activeOption(chr, skill, now)) return false;
            chr.getTemporaryStatManager().removeStatsBySkill(skill);
            if (skill != INDOMITABLE) {
                var child = SkillData.getSkillInfoById(skill + 1);
                // A data-bounded10s server response window, not a claim about an original-client grant timer.
                long window = SkillData.getSkillInfoById(skill).getValue(SkillStat.time, 1) * 1000L;
                if (child == null || child.getFirstRect() == null || window <= 0) return false;
                var grant = new Burst((Equip) chr.getEquippedItemByBodyPart(BodyPart.Weapon),
                        new WeakReference<>(chr.getField()), new WeakReference<>(chr.getClient()), skill,
                        chr.getPosition().getRectAround(child.getFirstRect()), now + window);
                PENDING.put(chr, grant);
                chr.write(Tms282SpecialWeaponPackets.requestBurst(skill + 1));
            }
            return true;
        }
    }

    /** Read-only; safe for damage paths already holding the temporary-stat lock. */
    public static boolean blocksDamage(Char chr, long now) {
        if (chr == null) return false;
        for (int skill : ACTIVES) if (preflight(chr, skill, 1) && activeOption(chr, skill, now)) return true;
        return false;
    }

    /** Equipment reconciliation also clears the indie protection used by shared boss-damage handlers. */
    public static void reconcile(Char chr) {
        if (chr == null || Protocol.active().version() != ProtocolVersion.TMS_282_3) return;
        for (int skill : ACTIVES)
            if (chr.getTemporaryStatManager().hasStatBySkillId(skill)
                    && !Tms282SpecialWeaponSkills.sourceEligible(chr, skill))
                chr.getTemporaryStatManager().removeStatsBySkill(skill);
        Burst pending = PENDING.get(chr);
        if (pending != null && !validSource(chr, pending, System.currentTimeMillis())) PENDING.remove(chr, pending);
    }

    public static boolean requiresBurstGrant(int skill) {
        return Protocol.active().version() == ProtocolVersion.TMS_282_3 && (skill == 80002634 || skill == 80003875);
    }

    public static boolean hasBurstGrant(Char chr, int child) {
        Burst grant = PENDING.get(chr);
        return grant != null && child == grant.parent()+1 && validSource(chr, grant, System.currentTimeMillis());
    }

    /** Consumed before queueing; duplicate/direct/forged attack paths cannot reuse a cancel event. */
    public static Burst claimBurst(Char chr, AttackInfo attack) {
        if (chr == null || !requiresBurstGrant(attack.skillId)) return null;
        synchronized (chr) {
            Burst grant = PENDING.remove(chr);
            return grant != null && validAttack(chr, grant, attack, System.currentTimeMillis()) ? grant : null;
        }
    }

    private static boolean validSource(Char chr, Burst grant, long now) {
        return now < grant.expires() && preflight(chr, grant.parent(), 1)
                && chr.getField() == grant.field().get() && chr.getClient() == grant.client().get()
                && chr.getEquippedItemByBodyPart(BodyPart.Weapon) == grant.weapon();
    }

    /** Also run inside the field queue: a source/field/session change after receipt cancels the damage. */
    public static boolean validAttack(Char chr, Burst grant, AttackInfo attack, long now) {
        if (!validSource(chr, grant, now) || attack.skillId != grant.parent()+1 || attack.mobAttackInfo == null) return false;
        // A forged BODY/SUMMON header would bypass the shared mob immunity path.
        boolean ordinary = attack.inHeader == InHeader.USER_MELEE_ATTACK && attack.attackHeader == OutHeader.REMOTE_MELEE_ATTACK
                || attack.inHeader == InHeader.USER_SHOOT_ATTACK && attack.attackHeader == OutHeader.REMOTE_SHOOT_ATTACK
                || attack.inHeader == InHeader.USER_MAGIC_ATTACK && attack.attackHeader == OutHeader.REMOTE_MAGIC_ATTACK;
        if (!ordinary) return false;
        var data = SkillData.getSkillInfoById(attack.skillId);
        if (data == null) return false;
        int targets = data.getValue(SkillStat.mobCount, 1), hits = data.getValue(SkillStat.attackCount, 1);
        int reportedHits = Byte.toUnsignedInt(attack.hits);
        if (targets <= 0 || hits <= 0 || attack.mobCount != attack.mobAttackInfo.size()
                || attack.mobCount > targets || reportedHits > hits) return false;
        Set<Integer> unique = new HashSet<>();
        for (var target : attack.mobAttackInfo) {
            if (target == null || !unique.add(target.mobId) || target.damages == null
                    || target.damages.length == 0 || target.damages.length != reportedHits) return false;
            if (!(chr.getField().getLifeByObjectID(target.mobId) instanceof Mob mob) || mob.getHp() <= 0
                    || !grant.rect().hasPositionInside(mob.getPosition())) return false;
            for (long damage : target.damages) if (damage < 0 || damage > GameConstants.DAMAGE_CAP) return false;
        }
        return true;
    }
}
