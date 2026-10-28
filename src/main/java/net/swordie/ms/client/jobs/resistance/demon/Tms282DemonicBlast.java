package net.swordie.ms.client.jobs.resistance.demon;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.FieldType;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.world.field.Field;
import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.IndieDamReduceR;

/** Current direct attack, not the removed 265 three-stage keydown. Existing field attack queue owns execution. */
public final class Tms282DemonicBlast {
    public static final int SKILL = 400011038;
    private final Char owner;
    private AttackInfo pendingHeal;
    private Field castField;
    private long nextEffect;
    private int castLevel;
    private boolean lifeSapApplied;

    public Tms282DemonicBlast(Char owner) { this.owner = owner; }
    public static boolean active() { return Protocol.active().version() == ProtocolVersion.TMS_282_3; }
    public static boolean removedStage(int id) { return id >= 400011062 && id <= 400011064; }

    public synchronized boolean begin(AttackInfo attack, long now) {
        int level = owner.getSkillLevel(SKILL);
        if (!active() || attack.skillId != SKILL || !JobConstants.isDemonAvenger(owner.getJob())
                || level <= 0 || owner.getHP() <= 0 || owner.getField() == null || now < nextEffect || pendingHeal == attack) return false;
        var info = SkillData.getSkillInfoById(SKILL);
        if (info == null) return false;
        attack.slv = level; attack.skillInfo = info;
        nextEffect = now + info.getValue(cooltime, level) * 1000L;
        pendingHeal = attack; castLevel = level; castField = owner.getField(); lifeSapApplied = false;
        int cost = (int) ((long) owner.getMaxHP() * info.getValue(hpRCon, level) / 100);
        if (cost > 0) owner.heal(-Math.min(cost, owner.getHP() - 1), false);
        Option reduction = new Option();
        reduction.nReason = SKILL; reduction.nValue = info.getValue(w, level);
        reduction.tTerm = info.getValue(w2, level); reduction.isInMillis = true;
        owner.getTemporaryStatManager().sendStat(IndieDamReduceR, reduction);
        return true;
    }

    /** Called only after the attack pipeline verified a real HP decrease / kill. One heal per accepted attack. */
    public synchronized void onDamageApplied(AttackInfo attack, Mob target, boolean killed) {
        if (!active() || pendingHeal != attack) return;
        if (owner.getHP() <= 0 || castField == null || owner.getField() != castField || target.getField() != castField
                || castField.getFieldType() == FieldType.GHOST_PARK) return;
        if (!lifeSapApplied && owner.getJobHandler() instanceof DemonAvenger avenger) {
            lifeSapApplied = true;
            avenger.lifeSapHealing();
        }
        if (!(killed || target.isBoss())) return;
        pendingHeal = null;
        var info = SkillData.getSkillInfoById(SKILL);
        int amount = (int) ((long) owner.getMaxHP() * info.getValue(x, castLevel) / 100);
        // Bypasses ordinary no-heal areas; Char.heal still applies Demonic Frenzy's recovery cap.
        owner.heal(amount, false);
    }
}
