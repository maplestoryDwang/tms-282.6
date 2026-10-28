package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.skills.Option;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * dropfix-0930 (research/tms2823/dropfix-0930/cts-inv): 265 summon time icons (IndieBuffIcon, nReason = the summon
 * skill) for the 282 client. The 282 TEMPORARY_STAT_SET 5FDD030 / RESET 5FE26F0 decode of the indie list does not
 * branch on nReason (cts_trace CLEAN for 400021069, 32111016, 33001007, 3111005 and a two-summon list), so the only
 * question is whether the client has the skill for its icon: only ids of the 282 Skill.wz (skill-ids-282.txt) pass.
 * Used by {@link Tms282SkillPackets} managerStat. {@code -Dtms282.summonIcon.enabled=false} (or
 * tms282.dropfix.enabled=false): the old fail-closed behaviour.
 */
public final class Tms282SummonIcons {
    static final Set<Integer> SKILLS_282 = load(Path.of("data", "protocol", "tms282.3", "skill-ids-282.txt"));

    private Tms282SummonIcons() {
    }

    public static boolean enabled() {
        return !"false".equalsIgnoreCase(System.getProperty("tms282.summonIcon.enabled", "true"))
                && !"false".equalsIgnoreCase(System.getProperty("tms282.dropfix.enabled", "true"));
    }

    public static boolean allowed(Option o) {
        return enabled() && o != null && o.nReason > 0 && SKILLS_282.contains(o.nReason);
    }

    static Set<Integer> load(Path file) {
        Set<Integer> ids = new HashSet<>();
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.ISO_8859_1)) {
                String t = line.strip();
                if (!t.isEmpty() && !t.startsWith("#")) {
                    ids.add(Integer.parseInt(t.split("[ \t]+")[0]));
                }
            }
        } catch (IOException | RuntimeException e) {
            System.out.println("[TMS282] skill-ids-282.txt unreadable, summon icons stay fail-closed: " + e);
            return Set.of();
        }
        return Set.copyOf(ids);
    }
}
