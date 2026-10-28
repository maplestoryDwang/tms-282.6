package net.swordie.ms.loaders;

import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.loaders.overlay.DataOverlays;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/** Original282 legacy skills whose only explicit level is level/1, without any explicit maxLevel.
 * DAT282 inherited the old loader's missing max=0; repair only the separately cached TMS overlay.
 * This supplies metadata, never ownership, skill grants or permission to spend SP on hidden skills. */
final class Tms282LegacySkillLevels {
    private Tms282LegacySkillLevels() { }
    private static final class Holder {
        static final Set<Integer> IDS = load();
    }
    private static Set<Integer> load() {
        Set<Integer> result = new HashSet<>();
        try {
            for (String row : Files.readAllLines(Path.of(ServerConstants.DIR,
                    "data/protocol/tms282.3/legacy-single-level-skills.tsv"))) {
                if (row.startsWith("skill_id\t") || row.isBlank()) continue;
                String[] values = row.split("\t", -1);
                if (values.length != 3 || !values[1].equals("1") || values[2].isBlank())
                    throw new IllegalArgumentException("Invalid original282 single-level metadata");
                int id = Integer.parseInt(values[0]);
                if (id <= 0 || !result.add(id)) throw new IllegalArgumentException("Invalid/duplicate single-level skill: " + id);
            }
            if (result.isEmpty()) throw new IllegalArgumentException("Missing single-level metadata");
            return Set.copyOf(result);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load original282 single-level metadata", e);
        }
    }

    static SkillInfo enrichOverlay(SkillInfo info) {
        if (info.getMaxLevel() == 0 && "tms282.3".equals(DataOverlays.current().id())
                && Holder.IDS.contains(info.getSkillId())) info.setMaxLevel(1);
        return info;
    }
}
