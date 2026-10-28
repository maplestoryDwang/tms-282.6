package net.swordie.ms.world.tms282.collab.frieren;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.enums.BaseStat;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.Position;
import net.swordie.ms.world.field.Field;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleSupplier;

/**
 * The companions' magic after a clone defeat, done on the server (the 282 client casts 80012712 / 80012713 on
 * its own only as a skill with charges, whose packets are not captured):
 * <ul>
 *     <li>targets: live mobs in the skill area around the owner (lt -1200,-800 / rb 1200,800), nearest first,
 *     at most mobCount (15); the owner's clone monster is never a target;</li>
 *     <li>damage per line (policy, same base as the 282 familiar): owner stat attack (4 x main stat + main / 4) x
 *     max(PAD, MAD) / 100 x damage% / 100, +-10%; attackCount lines per mob (10 for 芙莉蓮 1800%, 12 for 費倫 820%),
 *     applied as one sum through the 265 {@code Mob.damage} path (kills, exp, drops, quest counts).</li>
 * </ul>
 */
public final class FrierenCompanionAttack {
    /** A candidate target (plain values, so the check can plan without a field). */
    public record Target(int objectId, int x, int y, long hp, boolean excluded) {
    }

    private FrierenCompanionAttack() {
    }

    /** The attack of {@code base}, with damage / counts from the server skill data when present. */
    public static FrierenSkills.CompanionAttack resolve(FrierenSkills.CompanionAttack base) {
        try {
            SkillInfo si = SkillData.getSkillInfoById(base.skillId());
            if (si != null) {
                int damage = si.getValue(SkillStat.damage, 1);
                int count = si.getValue(SkillStat.attackCount, 1);
                int mobs = si.getValue(SkillStat.mobCount, 1);
                if (damage > 0 && count > 0 && mobs > 0) {
                    return new FrierenSkills.CompanionAttack(base.skillId(), base.speaker(), damage, count, mobs,
                            base.left(), base.top(), base.right(), base.bottom());
                }
            }
        } catch (RuntimeException | LinkageError ignored) {
            // skill data not loaded
        }
        return base;
    }

    /** One damage line before variance. */
    public static double lineBase(int mainStat, int attack, int damagePercent) {
        return (4.0 * mainStat + mainStat / 4.0) * Math.max(1, attack) / 100.0 * damagePercent / 100.0;
    }

    /** Targets and summed damage per object id; {@code random} yields [0,1). */
    public static Map<Integer, Long> plan(FrierenSkills.CompanionAttack attack, int ownerX, int ownerY,
                                          List<Target> candidates, int mainStat, int statAttack, DoubleSupplier random) {
        List<Target> inside = new ArrayList<>();
        for (Target t : candidates) {
            if (t.excluded() || t.hp() <= 0) {
                continue;
            }
            int dx = t.x() - ownerX;
            int dy = t.y() - ownerY;
            if (dx >= attack.left() && dx <= attack.right() && dy >= attack.top() && dy <= attack.bottom()) {
                inside.add(t);
            }
        }
        inside.sort(Comparator.comparingLong(t -> (long) (t.x() - ownerX) * (t.x() - ownerX)
                + (long) (t.y() - ownerY) * (t.y() - ownerY)));
        double base = lineBase(mainStat, statAttack, attack.damagePercent());
        Map<Integer, Long> hits = new LinkedHashMap<>();
        for (Target t : inside) {
            if (hits.size() >= attack.mobCount()) {
                break;
            }
            long total = 0;
            for (int i = 0; i < attack.attackCount(); i++) {
                long line = (long) (base * (0.9 + 0.2 * random.getAsDouble()));
                total += Math.max(1, Math.min(GameConstants.DAMAGE_CAP, line));
            }
            hits.put(t.objectId(), Math.min(GameConstants.DAMAGE_CAP, total));
        }
        return hits;
    }

    /** Runs one wave for {@code chr} in its current field. Returns a log line. */
    public static String execute(Char chr, FrierenSkills.CompanionAttack base, int excludedObjectId) {
        Field field = chr.getField();
        Position pos = chr.getPosition();
        if (field == null || pos == null) {
            return "no field";
        }
        FrierenSkills.CompanionAttack attack = resolve(base);
        List<Target> candidates = new ArrayList<>();
        for (Mob mob : field.getMobs()) {
            Position p = mob.getPosition();
            if (p == null) {
                continue;
            }
            boolean excluded = mob.getObjectId() == excludedObjectId || mob.getTemplateId() == FrierenSkills.CLONE_MOB;
            candidates.add(new Target(mob.getObjectId(), p.getX(), p.getY(), mob.getHp(), excluded));
        }
        int main = Math.max(Math.max(chr.getTotalStat(BaseStat.str), chr.getTotalStat(BaseStat.dex)),
                Math.max(chr.getTotalStat(BaseStat.inte), chr.getTotalStat(BaseStat.luk)));
        int statAttack = Math.max(chr.getTotalStat(BaseStat.pad), chr.getTotalStat(BaseStat.mad));
        Map<Integer, Long> hits = plan(attack, pos.getX(), pos.getY(), candidates, main, statAttack,
                java.util.concurrent.ThreadLocalRandom.current()::nextDouble);
        int applied = 0;
        for (Map.Entry<Integer, Long> e : hits.entrySet()) {
            Life life = field.getLifeByObjectID(e.getKey());
            if (life instanceof Mob mob && mob.getHp() > 0) {
                mob.damage(chr, e.getValue(), attack.skillId());
                applied++;
            }
        }
        return attack.speaker() + " " + FrierenSkills.skillName(attack.skillId()) + ": " + applied + " mobs";
    }
}
