package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.util.FileTime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * CharacterData block [8] (265 DBChar.SkillRecord) of the TMS 282.3 SetField first entry, read by 92AD20 at
 * 92C541..92CAEB when flag byte [8] is set (traced, skills phase 2):
 * <pre>
 * (superseded 2026-09-24: form 1 below left the skill window empty; encode() now writes form 0)
 * u8 1 (the list form; 0 would read other lists), u16 n,
 * n x (i32 skillId, i32 level, 8 bytes expiry (FileTime), [i32 masterLevel when 927330(skillId)]),
 * u8 hyper stat preset, 3 x (i32 count + count x 3 x i32 hyper stats)
 * </pre>
 * 265 writes the same up to the hyper stats and then its link-skill part, which 282 does not read in this block.
 * The master-level predicate is the client's (927330, table {@link #MASTER_LEVEL_TABLE}, emulated for every skill
 * id the server knows); 265's SkillConstants.isSkillNeedMasterLevel differs for some (e.g. the V skills). Hyper
 * stats: the active preset (QR 2498 {@code hyperstats}) and the three presets as 265 encodes them (i32 index,
 * i32 skillId, i32 level each; 8E36A0), so the 282 極限屬性 window shows the saved levels after a login.
 * Without this block the 282 skill window shows no learned skill after a login (the first entry did not send [8]).
 */
public record Tms282SkillRecordBlock(List<Entry> entries, int hyperPreset, List<HyperEntry> hyperStats) {
    public static final int FLAG = 8;
    public static final Path MASTER_LEVEL_TABLE = Path.of("data", "protocol", "tms282.3", "skill-master-level.tsv");
    public static final Tms282SkillRecordBlock EMPTY = new Tms282SkillRecordBlock(List.of());
    private static volatile Set<Integer> masterLevelSkills;

    public record Entry(int skillId, int level, int masterLevel) {
    }

    /** One 265 HyperStat row: preset index 0..2, hyper stat skill, level. */
    public record HyperEntry(int preset, int skillId, int level) {
    }

    public Tms282SkillRecordBlock {
        entries = List.copyOf(entries);
        hyperStats = List.copyOf(hyperStats);
    }

    public Tms282SkillRecordBlock(List<Entry> entries) {
        this(entries, 0, List.of());
    }

    public boolean isEmpty() {
        return entries.isEmpty() && hyperStats.isEmpty();
    }

    /** The character's skills as 265 writes them (union skills left out, sorted by id). */
    public static Tms282SkillRecordBlock of(Char chr) {
        Objects.requireNonNull(chr, "chr");
        String preset = chr.getQRValueByKey(net.swordie.ms.constants.QuestConstants.HYPER_STATS_PRESET, "hyperstats");
        int hyperPreset;
        try {
            hyperPreset = preset == null ? 0 : Math.max(0, Math.min(2, Integer.parseInt(preset.trim())));
        } catch (NumberFormatException e) {
            hyperPreset = 0;
        }
        List<HyperEntry> hyper = chr.getHyperStats() == null ? List.of() : chr.getHyperStats().stream()
                .filter(h -> h.getIndex() >= 0 && h.getIndex() <= 2)
                .map(h -> new HyperEntry(h.getIndex(), h.getSkillID(), h.getSkillLevel()))
                .toList();
        return new Tms282SkillRecordBlock(chr.getSkills().stream()
                .filter(s -> !SkillConstants.isUnionSkill(s.getSkillId()))
                .sorted(Comparator.comparingInt(Skill::getSkillId))
                .map(s -> new Entry(s.getSkillId(), s.getCurrentLevel(), s.getMasterLevel()))
                .toList(), hyperPreset, hyper);
    }

    /**
     * Form 0 (92C656..92CAAE): the only form that fills the client's current skill table (+0x8F9) as well as +0x929.
     * Form 1 (92C541 = 1) resets +0x8F9 and stores records in +0x929/+0x9E9/+0x971 only; live 2026-09-24 the skill
     * window then listed no learned skill (Tanjiro, formal-channel-1790191270956-3). Layout:
     * u16 n x (i32 id, i32 level); u16 0 removals; u16 n x (i32 id, FT expiry); u16 0; u16 m x (i32 id, i32 master
     * level); u16 0; then the hyper stat part both forms share (92CAB1).
     */
    public void encode(OutPacket out) {
        out.encodeByte(0);                                // 92C541: 0 = delta form
        out.encodeShort(entries.size());                  // 92C656
        for (Entry e : entries) {
            out.encodeInt(e.skillId());                   // 92C673
            out.encodeInt(e.level());                     // 92C682
        }
        out.encodeShort(0);                               // 92C710 removed skills
        out.encodeShort(entries.size());                  // 92C7CC expiries
        for (Entry e : entries) {
            out.encodeInt(e.skillId());                   // 92C7E3
            out.encodeFT(FileTime.MAX_TIME());            // 92C7FF DecodeBuffer(8)
        }
        out.encodeShort(0);                               // 92C897 removed expiries
        List<Entry> mastered = entries.stream().filter(e -> needsMasterLevel(e.skillId())).toList();
        out.encodeShort(mastered.size());                 // 92C94C master levels
        for (Entry e : mastered) {
            out.encodeInt(e.skillId());                   // 92C963
            out.encodeInt(e.masterLevel());               // 92C972
        }
        out.encodeShort(0);                               // 92CA04 removed master levels
        out.encodeByte(hyperPreset);                      // 92CAB4 hyper stat preset
        for (int i = 0; i < 3; i++) {
            final int preset = i;
            List<HyperEntry> rows = hyperStats.stream().filter(h -> h.preset() == preset).toList();
            out.encodeInt(rows.size());                   // 92CAEB hyper stat count per preset
            for (HyperEntry h : rows) {
                out.encodeInt(h.preset());                // 8E36A0 (265 HyperStat.encode)
                out.encodeInt(h.skillId());
                out.encodeInt(h.level());
            }
        }
    }

    /** Whether the 282 client reads a master level after this skill's expiry (927330). */
    public static boolean needsMasterLevel(int skillId) {
        Set<Integer> set = masterLevelSkills;
        if (set == null) {
            set = load(MASTER_LEVEL_TABLE);
            masterLevelSkills = set;
        }
        return set.contains(skillId);
    }

    static Set<Integer> load(Path file) {
        Set<Integer> set = new HashSet<>();
        try {
            for (String line : Files.readAllLines(file)) {
                String t = line.strip();
                if (!t.isEmpty() && !t.startsWith("#")) {
                    set.add(Integer.parseInt(t));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + file + " (TMS 282.3 master-level skills)", e);
        }
        return Set.copyOf(set);
    }
}
