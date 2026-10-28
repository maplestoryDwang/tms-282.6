package net.swordie.ms.handlers.user;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.protocol.codec.ProfileEncoders;
import net.swordie.ms.life.mob.Mob;

/**
 * The server-side gates of {@link AttackHandler#handleAttack} that decide whether client-computed damage is applied,
 * as a profile family (strategy, looked up like the codec families through {@link ProfileEncoders}):
 * <ul>
 *     <li>{@link Gate#MP_CON} / {@link Gate#BULLET_CON}: the 265 skill data cost could not be paid;</li>
 *     <li>{@link Gate#COOLTIME}: the 265 cooltime of the skill has not run out;</li>
 *     <li>{@link #gmOneHitKill}: a GM's hit takes the mob's whole HP.</li>
 * </ul>
 * GMS 265 registers none and keeps {@link #GMS_265}: every gate enforced, GM one-hit kill on (unchanged behaviour).
 * The TMS 282.3 profile registers {@code Tms282AttackGates}: 265 SkillData costs and cooltimes differ from the 282
 * client's (packet-audit/data282-diff-skills.tsv), so a gate there is logged instead of silently dropping damage the
 * 282 client already showed, unless {@code tms282.combat.antiCheat=drop}.
 */
public interface AttackGatePolicy {

    /** A 265 gate that can refuse an attack. */
    enum Gate {
        MP_CON, BULLET_CON, COOLTIME
    }

    /**
     * Called only when {@code gate} failed for {@code skillId}; true refuses the attack (265), false lets the damage
     * through (the policy may log the bypass).
     */
    boolean enforce(Char chr, int skillId, Gate gate);

    /** Whether {@code chr}'s (a GM's) hit on {@code mob} is raised to the mob's whole HP. */
    boolean gmOneHitKill(Char chr, Mob mob);

    /** GMS 265: all gates enforced, GM one-hit kill. */
    AttackGatePolicy GMS_265 = new AttackGatePolicy() {
        @Override
        public boolean enforce(Char chr, int skillId, Gate gate) {
            return true;
        }

        @Override
        public boolean gmOneHitKill(Char chr, Mob mob) {
            return true;
        }
    };

    /** The active profile's policy, else {@link #GMS_265}. */
    static AttackGatePolicy active() {
        return ProfileEncoders.find(AttackGatePolicy.class).orElse(GMS_265);
    }
}
