package net.swordie.ms.world.tms282.jewel;

import java.util.Arrays;

/**
 * One jewel (寶玉) owned by a character. polish[i] is the polish grade (0 = not polished, else 25/50/75/100)
 * of awaken slot i+1. Immutable; operations return a new state.
 */
public record JewelState(long id, int characterId, int tier, int engraveLevel, int awakenLevel, int[] polish,
                         int sourceItemId, String status) {

    public static final String ACTIVE = "ACTIVE", DESTROYED = "DESTROYED";

    public JewelState {
        polish = polish.clone();
    }

    public int polishAt(int slot) {
        return slot >= 1 && slot <= polish.length ? polish[slot - 1] : 0;
    }

    public JewelState withEngrave(int level) {
        return new JewelState(id, characterId, tier, level, awakenLevel, polish, sourceItemId, status);
    }

    public JewelState withAwaken(int level) {
        return new JewelState(id, characterId, tier, engraveLevel, level, polish, sourceItemId, status);
    }

    public JewelState withPolish(int slot, int grade) {
        int[] p = polish.clone();
        p[slot - 1] = grade;
        return new JewelState(id, characterId, tier, engraveLevel, awakenLevel, p, sourceItemId, status);
    }

    public JewelState destroyed() {
        return new JewelState(id, characterId, tier, engraveLevel, awakenLevel, polish, sourceItemId, DESTROYED);
    }

    public boolean active() {
        return ACTIVE.equals(status);
    }

    public String polishCsv() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < polish.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(polish[i]);
        }
        return sb.toString();
    }

    public static int[] parsePolish(String csv, int size) {
        int[] p = new int[size];
        if (csv != null && !csv.isBlank()) {
            String[] parts = csv.split(",");
            for (int i = 0; i < Math.min(size, parts.length); i++) p[i] = Integer.parseInt(parts[i].trim());
        }
        return p;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof JewelState s && s.id == id && s.characterId == characterId && s.tier == tier
                && s.engraveLevel == engraveLevel && s.awakenLevel == awakenLevel && Arrays.equals(s.polish, polish)
                && s.sourceItemId == sourceItemId && s.status.equals(status);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id) * 31 + Arrays.hashCode(polish);
    }

    @Override
    public String toString() {
        return "Jewel#" + id + "[tier=" + tier + ",engrave=" + engraveLevel + ",awaken=" + awakenLevel
                + ",polish=" + polishCsv() + "," + status + "]";
    }
}
