package net.swordie.ms.world.boss;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.MobPool;
import net.swordie.ms.connection.packet.WillPacket;
import net.swordie.ms.enums.DeathType;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.skill.MobSkill;
import net.swordie.ms.life.mob.skill.MobSkillID;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Instance;

import java.util.ArrayList;
import java.util.List;

/**
 * TMS 282 Will phase 1 (research/tms2823/will-flow-0930/README.md).
 *
 * <p>Phase 1 has an HP-bar holder (8880340 normal / 8880300 hard, mobType 14, never attacked by the client) and two
 * attackable dimension bodies (+3 lower, +4 upper). The 265 code only moves to phase 2 when the holder dies, but the
 * holder is only ever touched by mob skill 242/5, which is {@code onlyFsm} in Mob.wz (never cast by the client), and
 * the 265 HP gates compared the new HP with the <i>current</i> HP instead of the max HP. Live 2026-09-30: the lower
 * body died from a normal hit, nothing counted it, the holder stayed at 100 % and the fight never continued.
 *
 * <p>This controller owns the phase-1 HP on 282: each body stops at the next gate of {@code [66.6 %, 33.3 %, 0.3 %]}
 * (the WILL_SET_HP list 666/333/3) and is invincible there until the other body reaches the same gate; then the gate
 * is released (and Will is stunned, skill 242/8 as the 265 pattern does). When both bodies reach the last gate the
 * phase is over: the bodies are removed and every entrant of the instance is sent to the phase-2 direction map
 * (field + 50, the same target as BossHelper's holder-death branch). The holder's HP mirrors the higher body.
 */
public final class Tms282WillPhase1 {

    private static final String GATES = "tms282WillGates";
    private static final String DONE = "tms282WillPhase1Done";
    private static final String LAST_HP_PACKET = "tms282WillHpPacketAt";
    private static final long HP_PACKET_INTERVAL_MS = 300L;
    private static final long WARP_DELAY_MS = 2500L;

    private Tms282WillPhase1() {
    }

    public static boolean isBody(int templateId) {
        return templateId == 8880343 || templateId == 8880344 || templateId == 8880303 || templateId == 8880304;
    }

    static int holderOf(int bodyTemplateId) {
        return bodyTemplateId >= 8880340 ? 8880340 : 8880300;
    }

    static int otherBodyOf(int bodyTemplateId) {
        int holder = holderOf(bodyTemplateId);
        return bodyTemplateId == holder + 3 ? holder + 4 : holder + 3;
    }

    /** Gate in thousandths of the max HP (666 = 66.6 %). */
    static long floorOf(long maxHp, int gate) {
        return gate <= 0 ? 0L : Math.max(1L, (long) (maxHp * (gate / 1000.0D)));
    }

    static long maxHpOf(Mob mob) {
        double bonus = mob.getBonusHp();
        return (long) (mob.getMaxHp() * (bonus > 0 ? bonus : 1.0D));
    }

    /**
     * New HP of a body hit for {@code damage} while {@code gate} is the next gate: never below the gate, and no damage
     * at all once the body already sits on it (invincible until the other body catches up).
     */
    static long gatedHp(long hp, long damage, long floor) {
        if (hp <= floor) {
            return hp;
        }
        return Math.max(floor, hp - Math.max(0L, damage));
    }

    /**
     * @return true when the hit was handled here (the caller must not apply it again, and the body never dies);
     * false only on the 265 profile (the old code path runs).
     */
    public static boolean onBodyDamage(Char chr, Mob body, long damage) {
        if (!Will.isTms282() || body == null || !isBody(body.getTemplateId())) {
            return false;
        }
        Field field = body.getField();
        if (field == null) {
            return false;
        }
        int holderId = holderOf(body.getTemplateId());
        Mob holder = field.getMobByTemplateId(holderId);
        if (holder == null) {
            // Phase already over (holder removed) or a stray body: absorb. The 265 path would dereference the
            // missing holder (NullPointerException) or let the body die with nothing counting it.
            return true;
        }
        boolean releaseGate = false;
        boolean finished = false;
        List<Integer> gates;
        synchronized (holder) {
            if (holder.getProperty(DONE) != null) {
                return true;
            }
            gates = gates(holder);
            int gate = gates.isEmpty() ? 0 : gates.get(0);
            long floor = floorOf(maxHpOf(body), gate);
            long oldHp = body.getHp();
            long newHp = gatedHp(oldHp, damage, floor);
            body.setHp(newHp);
            if (oldHp > newHp && chr != null) {
                body.addDamage(chr, oldHp - newHp);
            }
            Mob other = field.getMobByTemplateId(otherBodyOf(body.getTemplateId()));
            long otherHp = other == null ? 0L : other.getHp();
            boolean otherAtGate = other == null || otherHp <= floorOf(maxHpOf(other), gate);
            holder.setHp(Math.max(1L, Math.max(newHp, otherHp)));
            if (gate > 0 && newHp <= floor && otherAtGate) {
                gates.remove(0);
                if (gate <= 3 || gates.isEmpty()) {
                    holder.setProperty(DONE, Boolean.TRUE);
                    finished = true;
                } else {
                    releaseGate = true;
                }
            }
        }
        if (finished) {
            finish(chr, field, holder);
            return true;
        }
        long now = System.currentTimeMillis();
        Object last = holder.getProperty(LAST_HP_PACKET);
        if (releaseGate || !(last instanceof Long) || now - (Long) last >= HP_PACKET_INTERVAL_MS) {
            holder.setProperty(LAST_HP_PACKET, now);
            field.broadcast(WillPacket.setHp(new ArrayList<>(gates), field, holderId, holderId + 3, holderId + 4));
        }
        if (releaseGate) {
            holder.setWillHPlist(new ArrayList<>(gates));
            stun(field, holder);
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static List<Integer> gates(Mob holder) {
        Object value = holder.getProperty(GATES);
        if (value instanceof List<?>) {
            return (List<Integer>) value;
        }
        List<Integer> gates = new ArrayList<>(List.of(666, 333, 3));
        holder.setProperty(GATES, gates);
        return gates;
    }

    /** Same cast as the 265 pattern end (MobSkill 242/8: stun notice, WILL_STUN, forced action on both bodies). */
    private static void stun(Field field, Mob holder) {
        try {
            MobSkill mobSkill = new MobSkill();
            mobSkill.setSkillID(MobSkillID.Will.getVal());
            mobSkill.setLevel(8);
            mobSkill.setFlip(holder.isFlip());
            holder.getSkillDelays().add(mobSkill);
            holder.setSkillDelay(mobSkill.getSkillDelay());
            field.broadcast(MobPool.setSkillDelay(holder, mobSkill.getSkillDelay(), MobSkillID.Will.getVal(), 8));
            mobSkill.applyEffect(holder);
        } catch (RuntimeException e) {
            // Cosmetic only: the gate is already released.
        }
    }

    private static void finish(Char chr, Field field, Mob holder) {
        int holderId = holder.getTemplateId();
        field.broadcast(WillPacket.setHp(new ArrayList<>(), field, holderId, holderId + 3, holderId + 4));
        for (Mob mob : new ArrayList<>(field.getMobs())) {
            field.removeMob(mob.getObjectId(), mob.getTemplateId() == holderId + 3 || mob.getTemplateId() == holderId + 4
                    ? DeathType.ANIMATION_DEATH : DeathType.NO_ANIMATION_DEATH);
        }
        int nextField = field.getId() + 50;
        Instance instance = chr != null ? chr.getInstance() : null;
        field.getTimer().addEvent(() -> {
            for (Char member : new ArrayList<>(field.getChars())) {
                if (member.getField() == field && (instance == null || member.getInstance() == instance)) {
                    member.warp(nextField);
                }
            }
        }, WARP_DELAY_MS);
    }
}
