package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.Tms282InnerAbilityPackets;
import net.swordie.ms.constants.QuestConstants;

/**
 * 282 內在潛能 outside the ability window (research/tms2823/innerability-282/REPORT.md):
 * <ul>
 *     <li>{@link #unlockLines}: the 265 line unlocks (quests 12394 at 30, 12395 / 12396 at 50) ran only in
 *     {@code Char.initialize}, after the SetField, so the client saw new lines only after the next login. Run before
 *     the first-entry CharacterData [28] and on level-up (then pushed as LP0137).</li>
 *     <li>{@link #afterEntry}: the active preset is not in CharacterData; LP0138 (excl 0) after LP02EF.</li>
 *     <li>{@link #honorForLevel}: the 265 honor EXP of a level-up (Job.handleLevelUp).</li>
 * </ul>
 */
public final class Tms282InnerAbility {
    private Tms282InnerAbility() {
    }

    /** Adds the lines the character's level allows; true when lines were added. */
    public static boolean unlockLines(Char chr) {
        if (chr.getLevel() < 30) {
            return false;
        }
        int before = chr.getPotentials().size();
        try {
            var sm = chr.getScriptManager();
            sm.initCharacterPotential(12394);
            sm.initCharacterPotential(12395);
            sm.initCharacterPotential(12396);
        } catch (RuntimeException e) {
            System.out.println("[TMS282] inner ability line unlock failed character=" + chr.getId() + ": " + e);
        }
        if (!chr.hasQuest(QuestConstants.CHARACTER_POTENTIAL_PRESET) && !chr.getPotentials().isEmpty()) {
            chr.createQuestWithQRValue(QuestConstants.CHARACTER_POTENTIAL_PRESET, "potential=0");
        }
        boolean added = chr.getPotentials().size() > before;
        if (added) {
            System.out.printf("[TMS282] inner ability lines %d -> %d character=%d level=%d%n", before,
                    chr.getPotentials().size(), chr.getId(), chr.getLevel());
        }
        return added;
    }

    public static void afterEntry(Client c, Char chr) {
        if (chr.getPotentials().isEmpty()) {
            return;
        }
        String v = chr.getQRValueByKey(QuestConstants.CHARACTER_POTENTIAL_PRESET, "potential");
        int preset;
        try {
            preset = v == null ? 0 : Math.max(0, Math.min(2, Integer.parseInt(v.trim())));
        } catch (NumberFormatException e) {
            preset = 0;
        }
        c.write(Tms282InnerAbilityPackets.changePreset(false, preset));
    }

    /** 265 Job.handleLevelUp: 700 + ((level - 50) / 10) * 100 honor EXP from level 50. */
    public static int honorForLevel(int level) {
        return level >= 50 ? 700 + ((level - 50) / 10) * 100 : 0;
    }
}
