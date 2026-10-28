package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.MobAttackInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.world.field.Field;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Damage and anti-cheat checks of a TMS 282.3 attack, made safe for a 282 client. The 265 path would ban and
 * disconnect for three skill ids ({@code AttackHandler.isExploitSkill}) and compares hits / mob counts / mob level
 * with 265 skill and mob data ({@code ClientHackHandler}), which can misjudge 282 skills and characters. Here each
 * rule is a {@link Rule} (specification) producing a {@link Finding}; nothing disconnects or bans:
 * <ul>
 *     <li>blocking rules (the attack is not applied in {@link Tms282CombatSettings.AntiCheatMode#DROP} mode, only
 *     logged in LOG mode): {@code exploit-skill} (the 265 ban list), {@code hit-damage} (a hit below 0 or above
 *     {@code tms282.combat.maxHitDamage});</li>
 *     <li>record-only rules (always just logged): {@code unknown-skill} (no 265 data for the id), {@code hit-count}
 *     and {@code mob-count} above the 265 skill data, {@code mob-level} (265: mob more than 30 levels above the
 *     character).</li>
 * </ul>
 */
public final class Tms282CombatGuard {
    /** Skill ids the 265 handler bans for (AttackHandler.isExploitSkill) plus ClientHackHandler's 80001431. */
    static final Set<Integer> EXPLOIT_SKILLS = Set.of(80001332, 80001431, 80001593, 80001706);

    private final Tms282CombatSettings settings;
    private final List<Rule> rules;

    public Tms282CombatGuard(Tms282CombatSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.rules = List.of(exploitSkill(), hitDamage(settings.maxHitDamage()), unknownSkill(), hitCount(), mobCount(),
                mobLevel());
    }

    /** One finding: the rule's name, whether it blocks the attack, and a log text. */
    public record Finding(String rule, boolean blocking, String detail) {
        @Override
        public String toString() {
            return rule + (blocking ? "[blocking]" : "") + ": " + detail;
        }
    }

    /** All findings for one attack and whether it is applied. */
    public record Verdict(List<Finding> findings, boolean apply) {
        public Verdict {
            findings = List.copyOf(findings);
        }

        public boolean clean() {
            return findings.isEmpty();
        }
    }

    /** A check of one attack (specification pattern). */
    @FunctionalInterface
    public interface Rule {
        Optional<Finding> check(Char chr, AttackInfo ai);
    }

    public Verdict inspect(Char chr, AttackInfo ai) {
        List<Finding> findings = new ArrayList<>();
        for (Rule rule : rules) {
            try {
                rule.check(chr, ai).ifPresent(findings::add);
            } catch (RuntimeException e) {
                findings.add(new Finding("rule-error", false, e.toString()));
            }
        }
        boolean blocked = settings.antiCheat() == Tms282CombatSettings.AntiCheatMode.DROP
                && findings.stream().anyMatch(Finding::blocking);
        boolean unknownDropped = !settings.applyUnknownSkill() && findings.stream().anyMatch(f -> f.rule().equals("unknown-skill"));
        return new Verdict(findings, !blocked && !unknownDropped);
    }

    static Rule exploitSkill() {
        return (chr, ai) -> EXPLOIT_SKILLS.contains(ai.skillId)
                ? Optional.of(new Finding("exploit-skill", true, "skill " + ai.skillId + " (265 bans for it; 282 does not)"))
                : Optional.empty();
    }

    static Rule hitDamage(long max) {
        return (chr, ai) -> {
            for (MobAttackInfo mai : ai.mobAttackInfo) {
                for (long d : mai.damages) {
                    if (d < 0 || d > max) {
                        return Optional.of(new Finding("hit-damage", true, "mob " + mai.mobId + " hit " + d
                                + " outside 0.." + max));
                    }
                }
            }
            return Optional.empty();
        };
    }

    static Rule unknownSkill() {
        return (chr, ai) -> ai.skillId != 0 && ai.skillInfo == null
                ? Optional.of(new Finding("unknown-skill", false, "skill " + ai.skillId + " has no 265 data"))
                : Optional.empty();
    }

    static Rule hitCount() {
        return (chr, ai) -> {
            SkillInfo si = ai.skillInfo;
            if (si == null || ai.skillId == 0) {
                return Optional.empty();
            }
            int data = si.getValue(SkillStat.attackCount, Math.max(1, ai.slv));
            return data > 0 && ai.hits > data
                    ? Optional.of(new Finding("hit-count", false, ai.hits + " hits, 265 data " + data))
                    : Optional.empty();
        };
    }

    static Rule mobCount() {
        return (chr, ai) -> {
            SkillInfo si = ai.skillInfo;
            if (si == null || ai.skillId == 0) {
                return Optional.empty();
            }
            int data = si.getValue(SkillStat.mobCount, Math.max(1, ai.slv));
            return data > 0 && ai.mobAttackInfo.size() > data
                    ? Optional.of(new Finding("mob-count", false, ai.mobAttackInfo.size() + " mobs, 265 data " + data))
                    : Optional.empty();
        };
    }

    static Rule mobLevel() {
        return (chr, ai) -> {
            Field field = chr.getField();
            if (field == null) {
                return Optional.empty();
            }
            for (MobAttackInfo mai : ai.mobAttackInfo) {
                Life life = field.getLifeByObjectID(mai.mobId);
                if (life instanceof Mob mob && mob.getLevel() - chr.getLevel() > 30 && mai.damages.length > 0
                        && mai.damages[0] != 1) {
                    return Optional.of(new Finding("mob-level", false, "mob " + mai.mobId + " level " + mob.getLevel()
                            + " vs character " + chr.getLevel()));
                }
            }
            return Optional.empty();
        };
    }
}
