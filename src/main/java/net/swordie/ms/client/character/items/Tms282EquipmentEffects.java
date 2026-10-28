package net.swordie.ms.client.character.items;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.FieldConstants;
import net.swordie.ms.world.field.Field;
import java.util.EnumMap;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.loaders.SkillData;

/** Exact equipment-source checks; no skills are permanently granted by this helper. */
public final class Tms282EquipmentEffects {
    public static final int FIRESTARTER = 80011492;
    public static final int FIRESTARTER_RING = 1114400;
    public static final int ASCETIC = 80011513;
    public static final int ASCETIC_RING = 1114401;
    public static final int CONTINUOUS_READY = 80003341;
    public static final int CONTINUOUS_EFFECT = 80003342;
    public static final int LUCID_NIGHTMARE = 80011540;
    public static final int LUCID_EARRINGS = 1033000;
    private Tms282EquipmentEffects() { }

    /** Original282 grants these hidden records but triggers them through CP03F8 after another skill.
     * A forged ordinary skill-use request must not bypass the 120-second equipment preparation. */
    public static boolean serverTriggered(int skillId) {
        return skillId == CONTINUOUS_READY || skillId == CONTINUOUS_EFFECT
                || skillId == 80002634 || skillId == 80003875 || skillId == 80004117
                || Tms282SpecialWeaponSkills.allPassiveIds().contains(skillId);
    }

    public static boolean wearing(Char chr, int itemId) {
        return chr != null && chr.getEquippedInventory() != null
                && chr.getEquippedInventory().getItems().stream().anyMatch(item -> item.getItemId() == itemId);
    }

    public static boolean burningMap(Field field) {
        return field != null && !field.isTown() && !field.getMobGens().isEmpty()
                && field.getMobGens().stream().anyMatch(gen -> gen.getMob() != null && !gen.getMob().isBoss()
                && gen.getMob().getForcedMobStat().getLevel() >= FieldConstants.BURNING_FIELD_MIN_MOB_LEVEL);
    }

    /** Used before the service charges HP/MP or starts the 1800-second cooldown. */
    public static boolean canFirestart(Char chr) {
        return wearing(chr, FIRESTARTER_RING) && chr.getInstance() == null && burningMap(chr.getField())
                && chr.getBurningFieldLevel() < FieldConstants.BURNING_FIELD_MAX_LEVEL;
    }

    public static void firestart(Char chr) {
        if (Protocol.active().version() != ProtocolVersion.TMS_282_3 || !canFirestart(chr)) return;
        Field field = chr.getField();
        field.getTms282Burning().ignite(System.currentTimeMillis());
        // Uses the existing trace-verified LP04CA TextEffect translator, for every player already in the field.
        field.getChars().forEach(Char::showBurningLevel);
    }

    public static EnumMap<CharacterTemporaryStat, Option> asceticBuffs() {
        var info = SkillData.getSkillInfoById(ASCETIC);
        var buffs = new EnumMap<CharacterTemporaryStat, Option>(CharacterTemporaryStat.class);
        Option exp = new Option(); exp.nReason = ASCETIC; exp.nValue = info.getValue(SkillStat.y, 1);
        exp.tTerm = info.getValue(SkillStat.time, 1);
        Option damage = new Option(); damage.nReason = ASCETIC; damage.nValue = -info.getValue(SkillStat.x, 1);
        damage.tTerm = exp.tTerm;
        buffs.put(CharacterTemporaryStat.IndieEXP, exp);
        buffs.put(CharacterTemporaryStat.IndiePMdR, damage);
        return buffs;
    }

    public static void ascetic(Char chr) {
        if (Protocol.active().version() == ProtocolVersion.TMS_282_3 && wearing(chr, ASCETIC_RING))
            chr.getTemporaryStatManager().sendStat(asceticBuffs());
    }

    /** Original282: 15 targets in the data rectangle, nine seconds, independent 90-second resistance.
     * SkillService owns the 110-second character cooldown before this job dispatch. */
    public static void lucidNightmare(Char chr) {
        if (Protocol.active().version() != ProtocolVersion.TMS_282_3 || chr == null || chr.getHP() <= 0
                || chr.getField() == null || !wearing(chr, LUCID_EARRINGS)) return;
        var info = SkillData.getSkillInfoById(LUCID_NIGHTMARE);
        if (info == null || info.getFirstRect() == null) return;
        var rect = chr.getPosition().getRectAround(info.getFirstRect());
        chr.getField().getMobsInRect(rect).stream().filter(mob -> !mob.isNoDebuff())
                .sorted(java.util.Comparator.comparing(net.swordie.ms.life.mob.Mob::isBoss).reversed()
                        .thenComparingInt(net.swordie.ms.life.mob.Mob::getObjectId))
                .limit(info.getValue(SkillStat.mobCount, 1)).forEach(mob -> {
                    Option bind = new Option(); bind.nOption = 1; bind.rOption = LUCID_NIGHTMARE;
                    bind.tOption = info.getValue(SkillStat.time, 1); bind.cOption = chr.getId();
                    mob.getTemporaryStat().addStatOptions(mob, net.swordie.ms.life.mob.MobStat.Stun, bind);
                });
    }
}
