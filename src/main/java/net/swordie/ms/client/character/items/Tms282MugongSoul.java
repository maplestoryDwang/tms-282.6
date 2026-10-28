package net.swordie.ms.client.character.items;

import java.util.*;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.loaders.SkillData;

/** Original282 Mu Gong 80001805. No item grants/SQL or permanent resource state. */
public final class Tms282MugongSoul {
    public static final int SKILL = 80001805;
    private Tms282MugongSoul() { }
    public record Charge(Equip weapon, int soulOption, int before, int level) { }

    private static Equip weapon(Char chr) {
        var item = chr.getEquippedItemByBodyPart(BodyPart.Weapon);
        return item instanceof Equip equip && ItemConstants.isWeapon(equip.getItemId()) && !equip.isCash()
                && chr.getEquippedInventory().getItems().stream().anyMatch(owned -> owned == equip)
                && equip.getDateExpire() != null && !equip.getDateExpire().isExpired()
                && equip.getSoulSocketId() != 0 && ItemConstants.getSoulSkillFromSoulID(equip.getSoulOptionId()) == SKILL
                ? equip : null;
    }

    /** No HP/MP/soul/cooldown mutation. Throws before the generic service costs are committed. */
    public static Charge preflight(Char chr, int level) {
        var info = SkillData.getSkillInfoById(SKILL);
        var source = weapon(chr);
        var tsm = chr.getTemporaryStatManager();
        var gauge = tsm.getOption(CharacterTemporaryStat.SoulMP);
        if (Protocol.active().version() != ProtocolVersion.TMS_282_3 || source == null || info == null
                // Existing verified EquipmentSkills soul sources grant level one. Data max two alone
                // is not authorization for an upgraded soul skill, whose acquisition is not yet proven.
                || level != 1 || level > info.getMaxLevel() || chr.getSkillLevel(SKILL) != level
                || chr.getField() == null || chr.getClient() == null || chr.getHP() <= 0
                || chr.getJobHandler() == null || !tsm.hasStat(CharacterTemporaryStat.SoulMP)
                || gauge.rOption != SKILL || gauge.nOption < info.getValue(SkillStat.soulmpCon, level)
                || gauge.nOption > ItemConstants.MAX_SOUL_CAPACITY)
            throw new IllegalArgumentException("Mu Gong requires its equipped soul weapon and sufficient soul charge");
        return new Charge(source, source.getSoulOptionId(), gauge.nOption, level);
    }

    /** Call after ALL service eligibility checks, BEFORE any other costs/cooldown are mutated. */
    public static boolean commit(Char chr, Charge intent) {
        synchronized (chr) {
            if (intent == null) return false;
            final Charge current;
            try { current = preflight(chr, intent.level()); }
            catch (IllegalArgumentException e) { return false; }
            if (current.weapon() != intent.weapon() || current.soulOption() != intent.soulOption()
                    || current.before() != intent.before()) return false;
            int cost = SkillData.getSkillInfoById(SKILL).getValue(SkillStat.soulmpCon, intent.level());
            var tsm = chr.getTemporaryStatManager();
            Option next = tsm.getOption(CharacterTemporaryStat.SoulMP).deepCopy();
            next.nOption = intent.before() - cost; next.xOption = ItemConstants.MAX_SOUL_CAPACITY;
            tsm.sendStat(CharacterTemporaryStat.SoulMP, next);
            if (tsm.hasStat(CharacterTemporaryStat.FullSoulMP) && next.nOption < ItemConstants.MAX_SOUL_CAPACITY)
                tsm.removeStat(CharacterTemporaryStat.FullSoulMP);
            // The optional legacy Job effect adapter may be disabled. Finish the paid effect here.
            tsm.sendStat(buffs(intent.level()));
            return true;
        }
    }

    public static EnumMap<CharacterTemporaryStat, Option> buffs(int level) {
        var info = SkillData.getSkillInfoById(SKILL);
        if (level < 1 || level > info.getMaxLevel()) throw new IllegalArgumentException("Unsupported Mu Gong level");
        var result = new EnumMap<CharacterTemporaryStat, Option>(CharacterTemporaryStat.class);
        for (var stat : List.of(CharacterTemporaryStat.IndiePADR, CharacterTemporaryStat.IndieMADR)) {
            Option o = new Option(); o.nReason = SKILL; o.nValue = info.getValue(SkillStat.x, level);
            o.tTerm = info.getValue(SkillStat.time, level); result.put(stat, o);
        }
        return result;
    }
}
