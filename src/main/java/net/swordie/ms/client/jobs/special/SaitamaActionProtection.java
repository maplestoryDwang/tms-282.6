package net.swordie.ms.client.jobs.special;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.world.field.Field;

/** Server-owned action windows, not renewed by attack packets or repeated prepare requests. */
public final class SaitamaActionProtection {
    public static final int PUNCH = 122001003, SIDE_STEP = 122001007, SERIOUS_PUNCH = 122001008;
    private volatile int skill;
    private volatile long until;
    private long punchReady, sideStepReady;
    private volatile Field field;

    public static SaitamaActionProtection of(Char chr) {
        return chr != null && Protocol.active().version() == ProtocolVersion.TMS_282_3
                && chr.getJobHandler() instanceof SpecialEventJob job ? job.getActionProtection() : null;
    }

    public static boolean blocksDamage(Char chr, long now) {
        var state = of(chr);
        return state != null && state.active(chr, now) && (state.skill == SIDE_STEP || state.skill == SERIOUS_PUNCH);
    }

    public static boolean blocksDebuff(Char chr, CharacterTemporaryStat cts, long now) {
        var state = of(chr);
        // Ordinary dispellable statuses only; never blanket-block boss-specific mechanics.
        return state != null && state.active(chr, now)
                && net.swordie.ms.client.character.skills.temp.TemporaryStatManager.DISPELLABLE_DEBUFFS.contains(cts);
    }

    public static boolean isKeyDown(int id) { return id == PUNCH || id == SIDE_STEP; }

    public synchronized boolean startKeyDown(Char chr, int id, long now) {
        if (!isKeyDown(id) || !eligible(chr, id) || active(chr, now)
                || now < (id == PUNCH ? punchReady : sideStepReady) || chr.hasSkillOnCooldown(id)) return false;
        var data = SkillData.getSkillInfoById(id);
        long cooldown = data.getValue(SkillStat.cooltime, 1) * 1000L;
        if (id == PUNCH) punchReady = now + cooldown; else sideStepReady = now + cooldown;
        arm(chr, id, now, id == PUNCH ? 1800 : data.getValue(SkillStat.time, 1) * 1000);
        return true;
    }

    /** Called only after CP04B4 has accepted the visible skill, never for 009/010 follow-up attacks. */
    public synchronized void startSeriousPunch(Char chr, long now) {
        if (!eligible(chr, SERIOUS_PUNCH) || active(chr, now)) return;
        // Preserve Job.applyNdTime's existing one-second grace; exact native action-end timing is still unverified.
        arm(chr, SERIOUS_PUNCH, now, SkillData.getSkillInfoById(SERIOUS_PUNCH).getValue(SkillStat.ndTime, 1) + 1000);
    }

    private boolean eligible(Char chr, int id) {
        return Protocol.active().version() == ProtocolVersion.TMS_282_3 && chr.getHP() > 0 && chr.getField() != null
                && (chr.getField().getFieldLimit() & (net.swordie.ms.enums.FieldOption.SkillLimit.getVal()
                | net.swordie.ms.enums.FieldOption.MoveSkillOnly.getVal())) == 0
                && SpecialJobProgression.requestLevel(chr, id) > 0;
    }

    private void arm(Char chr, int id, long now, int duration) {
        skill = id;
        until = now + duration;
        field = chr.getField();
        Option option = new Option();
        option.nReason = id;
        option.nValue = 1;
        option.tTerm = duration;
        option.isInMillis = true;
        chr.getTemporaryStatManager().sendStat(id == PUNCH
                ? CharacterTemporaryStat.IndieApplySuperStance : CharacterTemporaryStat.IndieNotDamaged, option);
    }

    public boolean active(Char chr, long now) {
        // Read-only: called under the TSM lock too. Never acquire the action lock or send a packet here.
        return skill != 0 && now < until && chr.getHP() > 0 && chr.getField() == field;
    }

    public synchronized void stop(Char chr, int id) {
        if (skill == id) clear(chr);
    }

    public synchronized void clear(Char chr) {
        int old = skill;
        skill = 0;
        field = null;
        until = 0;
        if (old != 0) chr.getTemporaryStatManager().removeStatsBySkill(old);
    }

    public static boolean fixedDuration(CharacterTemporaryStat cts, Option option) {
        return Protocol.active().version() == ProtocolVersion.TMS_282_3
                && ((cts == CharacterTemporaryStat.IndieApplySuperStance && option.nReason == PUNCH)
                || (cts == CharacterTemporaryStat.IndieNotDamaged
                && (option.nReason == SIDE_STEP || option.nReason == SERIOUS_PUNCH)));
    }
}
