package net.swordie.ms.client.character.items;

import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.loaders.ItemData;
import java.util.HashMap;
import java.util.Map;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;

/** Skill levels supplied by equipment; duplicate sources contribute the highest level. */
public final class EquipmentSkills {
    private EquipmentSkills() { }

    public static Map<Integer, Integer> levels(Iterable<? extends Item> items, int beginnerJob) {
        Map<Integer, Integer> result = new HashMap<>();
        for (Item item : items) {
            if (!(item instanceof Equip equip)) continue;
            // Zero's active/alternate weapon remapping still needs original282 Tag evidence.
            if (beginnerJob != 10000)
                Tms282SpecialWeaponSkills.passiveLevels(equip).forEach((skill, level) -> result.merge(skill, level, Math::max));
            Map<Integer, Integer> original = Protocol.active().version() == ProtocolVersion.TMS_282_3
                    ? Tms282EquipmentSkillData.levels(equip.getItemId(), equip.getItemLevel()) : null;
            Equip template = original != null ? null
                    : equip.getItemSkills().isEmpty() ? ItemData.getEquipById(equip.getItemId()) : equip;
            if (original != null) {
                original.forEach((skill, level) -> result.merge(skill, level, Math::max));
            } else if (template != null) {
                for (ItemSkill skill : template.getItemSkills()) {
                    if (Protocol.active().version() == ProtocolVersion.TMS_282_3
                            && Tms282SpecialWeaponSkills.isReservedSkill(skill.getSkill())) continue;
                    int level = equip.getItemLevel() > 0 ? equip.getItemLevel() : skill.getSlv();
                    if (level > 0) result.merge(skill.getSkill(), level, Math::max);
                }
            }
            add(result, ItemConstants.getSoulSkillFromSoulID(equip.getSoulOptionId()));
            if (equip.getOptions().size() >= 3) {
                for (int skill : ItemConstants.getDecentPotentialSkillID(equip.getOptionBase())) {
                    if (skill != 0) add(result, beginnerJob * 10000 + skill);
                }
            }
            add(result, ItemConstants.getDecentNebuliteSkillID(equip.getSocket(0) + ItemConstants.NEBILITE_BASE_ID));
        }
        return result;
    }

    private static void add(Map<Integer, Integer> result, int skill) {
        if (skill != 0) result.merge(skill, 1, Math::max);
    }
}
