package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.skills.Skill;

/**
 * Where the TMS 282.3 services write a character's changes. {@link #SQL} uses the 265 persistence methods
 * (tables skills, characterstats, extendsp/spset); offline checks pass a recording store instead, so the services
 * never need a database to be tested.
 */
public interface Tms282CharacterStore {

    /** One skill row (265 {@link Skill#saveToSQL()}: INSERT when its ID is 0, UPDATE otherwise). */
    void saveSkill(Skill skill);

    /** The character's stats incl. extended SP (265 {@link CharacterStat#updateCharacterStatToSQL()}). */
    void saveStats(CharacterStat stat);

    Tms282CharacterStore SQL = new Tms282CharacterStore() {
        @Override
        public void saveSkill(Skill skill) {
            skill.saveToSQL();
        }

        @Override
        public void saveStats(CharacterStat stat) {
            stat.updateCharacterStatToSQL();
        }

        @Override
        public String toString() {
            return "SQL";
        }
    };
}
