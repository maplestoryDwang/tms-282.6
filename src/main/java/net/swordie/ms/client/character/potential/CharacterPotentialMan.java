package net.swordie.ms.client.character.potential;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.achievement.AchievementHandler;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.enums.PotentialResetType;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.Util;

import java.util.Set;

public class CharacterPotentialMan {

    private final Char chr;

    public CharacterPotentialMan(Char chr) {
        this.chr = chr;
    }

    private Set<CharacterPotential> getPotentials(int preset) {
        return chr.getPotentialsByPreset(preset);
    }

    public CharacterPotential getPotentialByKey(int preset, byte key) {
        return getPotentials(preset).stream().filter(pot -> pot.getKey() == key).findAny().orElse(null);
    }

    /**
     * Adds a potential to the char's potential list. Will override the old one with the same key if one exists.
     * Also sends a packet to the client to indicate the change.
     *
     * @param potential The potential to add
     */
    public void addPotential(int preset, CharacterPotential potential) {
        potential.setCharID(chr.getId());
        getPotentials(preset).add(potential);
        potential.saveToSQL();
        chr.write(WvsContext.characterPotentialSet(chr, true, true, preset));
    }

    /**
     * Removes a potential from the char's potential list by key. Will do nothing if there is no such potential.
     * Also sends a packet to the client to indicate the change.
     *
     * @param key the potential's key to remove
     */
    public void removePotential(int preset, byte key) {
        CharacterPotential cp = getPotentialByKey(preset, key);
        if (cp != null) {
            getPotentials(preset).removeIf(x -> x.getKey() == key);
            chr.write(WvsContext.characterPotentialReset(chr));
        }
    }

    /**
     * Returns the current grade of a Char's potential, which is equivalent to the highest potential of the Char.
     *
     * @return the current grade of a Char's potential
     */
    public byte getGrade(int preset) {
        int max = 0;
        for (CharacterPotential cp : getPotentials(preset)) {
            if (cp.getGrade() > max) {
                max = cp.getGrade();
            }
        }
        return (byte) max;
    }

    /**
     * Generates a new CharacterPotential, based off of the current grade.
     *
     * @param key the key (line number, 1-3) the generated potential should have
     * @return the generated CharacterPotential.
     */
    public CharacterPotential generateRandomPotential(int preset, byte key) {
        // slv are 1-40 inclusive, split up into 4 "tiers" of 10 slv per grade (0-3 inclusive)
        int skillID = Util.getRandom(GameConstants.CHAR_POT_BASE_ID, GameConstants.CHAR_POT_END_ID + 1);
        SkillInfo skillInfo = SkillData.getSkillInfoById(skillID);
        byte grade = getGrade(preset); //0 = Rare, 1 = Epic, 2 = Unique, 3 = Legendary.
        int baseSlv = grade * 10;
        int maxSlv = Math.min(baseSlv + 10, skillInfo.getMaxLevel());
        int slv = Math.min(1 + Util.getRandom(baseSlv, maxSlv), skillInfo.getMaxLevel());
        if (!chr.getPotentialSkills(preset).contains(skillID)) {
            return new CharacterPotential(preset, key, skillID, (byte) slv, grade);
        } else {
            return generateRandomPotential(preset, key);
        }
    }

    public CharacterPotentialValueHolder generateRandomPotentialTemp(int preset, byte key) {
        // slv are 1-40 inclusive, split up into 4 "tiers" of 10 slv per grade (0-3 inclusive)
        byte grade = getGrade(preset);
        int baseSlv = grade * 10;
        int maxSlv = baseSlv + 10;
        int slv = 1 + Util.getRandom(baseSlv, maxSlv);
        int skillID = Util.getRandom(GameConstants.CHAR_POT_BASE_ID, GameConstants.CHAR_POT_END_ID + 1);
        if (!chr.getPotentialSkills(preset).contains(skillID)) {
            AchievementHandler.handleResetCharPotentials(chr, skillID, slv);
            return new CharacterPotentialValueHolder(key, skillID, (byte) slv, grade);
        } else {
            return generateRandomPotentialTemp(preset, key);
        }
    }
}
