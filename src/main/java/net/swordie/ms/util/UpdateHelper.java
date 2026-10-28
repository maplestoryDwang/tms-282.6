package net.swordie.ms.util;

import net.swordie.ms.client.Account;
import net.swordie.ms.client.LinkSkill;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.matrix.MatrixCore;
import net.swordie.ms.client.character.skills.matrix.MatrixSlot;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.enums.MatrixStateType;

import java.util.Set;

public class UpdateHelper {

    public static void main(String[] args) {
        DatabaseManager.init();

    }

    public void initRemovedAndAddNewSkills() {
        for (int i = 0; i <= 2000; i++) {
            Char chr = Char.getCharDataByID(i);
            if (chr != null) {
                if (chr.hasSkill(1111014)) {
                    chr.getSkill(1111014).setSkillId(1111008); // Shout
                }
                if (chr.hasSkill(1311013)) {
                    chr.getSkill(1311013).setSkillId(1310013); // Evil Eye of Domination
                }
                if (chr.hasSkill(13121004)) {
                    chr.getSkill(13121004).setSkillId(13120004); // Touch of the Wind
                }
                if (chr.hasSkill(23111004)) {
                    chr.getSkill(23111004).setSkillId(23110004); // Ignis Roar
                }
                if (chr.hasSkill(23120011)) {
                    chr.getSkill(23120011).setSkillId(23121011); // Rolling Moonsault
                }
                if (chr.hasSkill(24111005)) {
                    chr.getSkill(24111005).setSkillId(24110005); // Lune
                }
                if (chr.hasSkill(142101005)) {
                    chr.getSkill(142101005).setSkillId(142100005); // Pure Power
                }
                if (chr.hasSkill(142111008)) {
                    chr.getSkill(142111008).setSkillId(142110008); // Psychic Reinforcement
                }
                if (chr.hasSkill(142121006)) {
                    chr.getSkill(142121006).setSkillId(142120006); // Telepath Tactics
                }
                if (chr.getJob() == 4112 && chr.getLevel() >= 190 && !chr.hasSkill(41120049)) {
                    chr.addSkill(41120049, 0, 1); // New Hayato Hyper Skills:
                }

            }
        }
    }

    private static void initDecoration() {
        for (int i = 0; i <= 2000; i++) {
            Char chr = Char.getCharDataByID(i);
            if (chr != null) {
                chr.loadCharacterData();
                // TODO change every cash equip from equip / equipped inventory -> decoration inventory
            }
        }
    }

    private static void initLinkSkills() {
        for (int i = 1; i <= 354; i++) {
            boolean removedLinkSkill = false;
            Account acc = Account.getAccountFromSQLByAccountID(i);
            for (LinkSkill ls : acc.getLinkSkills()) {
                if (ls.getLinkSkillID() == 80000055 || ls.getLinkSkillID() == 80000329) {
                    ls.deleteLinkSkillFromSQL();
                    removedLinkSkill = true;
                }
            }
            if (removedLinkSkill) {
                System.out.println("Removed WRONG link skill: " + acc.getId());
            }
            for (Char chr : acc.getCharacters()) {
                if (chr != null) {
                    boolean removedSkillAndAddNewLinkSkill = false;
                    if (chr.hasSkill(80000055)) {
                        chr.removeSkill(80000055);
                    }
                    if (chr.hasSkill(80000329)) {
                        chr.removeSkill(80000329);
                    }
                    byte linkSkillLevel = (byte) SkillConstants.getLinkSkillLevelByCharLevel(chr.getLevel(), chr.getJob());
                    int linkSkillID = SkillConstants.getLinkSkillByJob(chr.getJob());
                    int originalOfLinkSkillID = SkillConstants.getOriginalOfLinkedSkill(linkSkillID);
                    if (originalOfLinkSkillID != 0 && linkSkillLevel > 0) {
                        Skill skill = chr.getSkill(originalOfLinkSkillID, true);
                        if (skill.getCurrentLevel() != linkSkillLevel) {
                            if (!chr.hasSkill(originalOfLinkSkillID)) {
                                skill.setCurrentLevel(1);
                                skill.saveToSQL();
                                int level = SkillConstants.getLinkSkillLevelByCharLevel(chr.getLevel(), chr.getJob());
                                LinkSkill ls = new LinkSkill(acc.getId(), chr.getId(), linkSkillID, 0, level, FileTime.MIN_TIME());
                                ls.updateLinkSkillToSQL();
                                removedSkillAndAddNewLinkSkill = true;
                            } else {
                                skill.setCurrentLevel(linkSkillLevel);
                                skill.saveToSQL();
                                int level = SkillConstants.getLinkSkillLevelByCharLevel(chr.getLevel(), chr.getJob());
                                LinkSkill ls = new LinkSkill(acc.getId(), chr.getId(), linkSkillID, 0, level, FileTime.MIN_TIME());
                                ls.updateLinkSkillToSQL();
                                removedSkillAndAddNewLinkSkill = true;
                            }
                        }
                    }
                    if (removedSkillAndAddNewLinkSkill) {
                        System.out.println("Removed skill ID and add New Link Skill: " + chr.getName());
                    }
                }
            }
        }
    }
}
