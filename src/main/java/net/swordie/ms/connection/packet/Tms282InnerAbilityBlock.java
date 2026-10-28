package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.potential.CharacterPotential;
import net.swordie.ms.connection.OutPacket;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 內在潛能 / 名聲值 in the 282 first-entry CharacterData (research/tms2823/innerability-282/REPORT.md):
 * <ul>
 *     <li>flag [28] (mask 9A7FA40), read at 92F1C3: 3 presets x (u16 n, n x line), line = u8 key, i32 skillId, u8 slv,
 *     u8 grade (8E3780; 265 CharacterPotential.encode);</li>
 *     <li>flag [29] (mask 9A7FAB0), read at 92F376: i32 honor level (+0xB45), i32 honor EXP (+0xB49).</li>
 * </ul>
 * Without them the 282 ability window was empty and honor 0. The active preset is not in CharacterData: it is sent as
 * LP0138 after the entry ({@code Tms282InnerAbility}).
 */
public record Tms282InnerAbilityBlock(List<List<Line>> presets, int honorExp) {
    public static final int ABILITY_FLAG = 28;
    public static final int HONOR_FLAG = 29;
    public static final int PRESETS = 3;
    public static final Tms282InnerAbilityBlock NONE = new Tms282InnerAbilityBlock(null, 0);

    public record Line(int key, int skillId, int slv, int grade) {
        public void encode(OutPacket out) {
            out.encodeByte(key);
            out.encodeInt(skillId);
            out.encodeByte(slv);
            out.encodeByte(grade);
        }
    }

    public Tms282InnerAbilityBlock {
        if (presets != null) {
            if (presets.size() != PRESETS) {
                throw new IllegalArgumentException("presets " + presets.size());
            }
            presets = presets.stream().map(List::copyOf).toList();
        }
    }

    /** Whether the blocks are sent (NONE keeps [28] / [29] off, the earlier stream). */
    public boolean present() {
        return presets != null;
    }

    public static Tms282InnerAbilityBlock of(Char chr) {
        Objects.requireNonNull(chr, "chr");
        List<List<Line>> presets = new ArrayList<>(PRESETS);
        for (int i = 0; i < PRESETS; i++) {
            presets.add(chr.getPotentialsByPreset(i).stream()
                    .sorted(Comparator.comparingInt(CharacterPotential::getKey))
                    .map(cp -> new Line(cp.getKey(), cp.getSkillID(), cp.getSlv(), cp.getGrade()))
                    .toList());
        }
        return new Tms282InnerAbilityBlock(presets, chr.getHonorExp());
    }

    /** [28]: the three presets. */
    public void encodeAbilities(OutPacket out) {
        for (List<Line> lines : presets) {
            out.encodeShort(lines.size());
            for (Line line : lines) {
                line.encode(out);
            }
        }
    }

    /** [29]: honor level 1 (as 265 writes it) and the honor EXP. */
    public void encodeHonor(OutPacket out) {
        out.encodeInt(1);
        out.encodeInt(honorExp);
    }
}
