package net.swordie.ms.world.tms282.collab.frieren;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.loaders.containerclasses.PetInfo;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.life.pet.Pet;
import net.swordie.ms.loaders.Etc.SetItemInfo.SetItemInfo;
import net.swordie.ms.loaders.Etc.SetItemInfo.SetItemInfoData;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.SkillData;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 迷你葬送的芙莉蓮 Lv.1..3 (80000922..80000924): the pet-set passive of SetItemInfo 1127. The real trigger is the
 * number of <b>different</b> set pets out at the same time (Effect/1, /2, /3 = 1, 2, 3 pieces; completeCount 3;
 * the fourth pet 5002831 only counts as a piece). Only the highest level applies: IndiePAD / IndieMAD
 * +8 / +10 / +18 with the skill id as reason, through the same TemporaryStatManager pet-passive list the 265
 * {@code Job.givePetPassiveBuffs} uses, so its "pets deactivated" branch removes it too.
 * <p>The 265 generic branch maps each pet to the Effect whose index equals the pet's ItemID index (pet 1 -> Lv.1,
 * pet 2 -> Lv.2, pet 3 -> Lv.3, pet 4 -> nothing) and stacks them; {@link #handles} lets that method skip set 1127.
 * <p>Lv.3's 「道具拾取範圍增加」 is applied by the client from the skill data; the server has no pick-up range check.
 */
public final class MiniFrierenPassive {
    private MiniFrierenPassive() {
    }

    public static boolean handles(int setItemId) {
        return setItemId == FrierenSkills.PET_SET_ID;
    }

    /** Distinct items of the set among {@code petItemIds}. */
    public static int pieces(Collection<Integer> petItemIds, Collection<Integer> setItems) {
        Set<Integer> distinct = new LinkedHashSet<>();
        for (Integer id : petItemIds) {
            if (id != null && setItems.contains(id)) {
                distinct.add(id);
            }
        }
        return distinct.size();
    }

    /** The skill for {@code pieces} set pets out (0 = none), from the set's Effect list when loaded. */
    public static int skillFor(int pieces) {
        if (pieces <= 0) {
            return 0;
        }
        SetItemInfo info = setInfo();
        if (info != null && !info.getActiveSkills().isEmpty()) {
            int best = 0;
            int bestIndex = 0;
            for (SetItemInfo.ActiveSkill a : info.getActiveSkills()) {
                if (a.getEffectIndex() <= pieces && a.getEffectIndex() > bestIndex) {
                    bestIndex = a.getEffectIndex();
                    best = a.getSkillID();
                }
            }
            if (best != 0) {
                return best;
            }
        }
        return FrierenSkills.MINI_FRIEREN.get(Math.min(pieces, FrierenSkills.MINI_FRIEREN.size()) - 1);
    }

    /** padX (= madX) of a Mini Frieren skill: server skill data, else the 282 values 8 / 10 / 18. */
    public static int attack(int skillId, boolean magic) {
        int index = FrierenSkills.MINI_FRIEREN.indexOf(skillId);
        if (index < 0) {
            return 0;
        }
        try {
            SkillInfo si = SkillData.getSkillInfoById(skillId);
            if (si == null) {
                si = SkillData.getPetPassiveSkillInfoById(skillId);
            }
            if (si != null) {
                int v = si.getValue(magic ? SkillStat.madX : SkillStat.padX, 1);
                if (v > 0) {
                    return v;
                }
            }
        } catch (RuntimeException | LinkageError ignored) {
            // skill data not loaded (offline check): client values below
        }
        return FrierenSkills.MINI_FRIEREN_ATTACK[index];
    }

    static List<Integer> setItems() {
        SetItemInfo info = setInfo();
        if (info != null && !info.getItemIDs().isEmpty()) {
            return info.getItemIDs().stream().map(SetItemInfo.ItemID::getItemID).toList();
        }
        return FrierenSkills.SET_PETS;
    }

    private static SetItemInfo setInfo() {
        try {
            for (SetItemInfo info : SetItemInfoData.getSetItemInfos()) {
                if (info.getSetItemID() == FrierenSkills.PET_SET_ID) {
                    return info;
                }
            }
        } catch (RuntimeException | LinkageError ignored) {
            // not loaded
        }
        return null;
    }

    /** Pet item ids currently out. */
    static List<Integer> petItemIds(Char chr) {
        return chr.getPets().stream()
                .filter(p -> p != null && p.getItem() != null)
                .map(Pet::getItem)
                .map(item -> item.getItemId())
                .filter(id -> {
                    PetInfo pi = ItemData.getPetInfoByID(id);
                    return pi == null || pi.getSetItemID() == 0 || handles(pi.getSetItemID());
                })
                .toList();
    }

    /**
     * Re-evaluates the passive: removes every Mini Frieren level, then applies the one matching the pets out.
     * Returns the applied skill id (0 = none).
     */
    public static int apply(Char chr) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        for (int id : FrierenSkills.MINI_FRIEREN) {
            if (tsm.hasStatBySkillId(id)) {
                tsm.removeStatsBySkill(id);
            }
            tsm.getPetPassiveSkills().remove(Integer.valueOf(id));
        }
        int skillId = skillFor(pieces(petItemIds(chr), setItems()));
        if (skillId == 0) {
            return 0;
        }
        EnumMap<CharacterTemporaryStat, Option> stats = new EnumMap<>(CharacterTemporaryStat.class);
        Option pad = new Option();
        pad.nValue = attack(skillId, false);
        pad.nReason = skillId;
        stats.put(CharacterTemporaryStat.IndiePAD, pad);
        Option mad = new Option();
        mad.nValue = attack(skillId, true);
        mad.nReason = skillId;
        stats.put(CharacterTemporaryStat.IndieMAD, mad);
        tsm.addPetPassiveSkills(skillId);
        tsm.sendStat(stats);
        return skillId;
    }
}
