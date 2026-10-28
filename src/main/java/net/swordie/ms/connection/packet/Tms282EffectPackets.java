package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * TMS 282.3 user effects. LP04CA EFFECT (CUserLocal) and LP048E REMOTE_EFFECT (CUserPool::OnUserRemotePacket, after
 * its i32 characterId, {@link Tms282UserRemotePackets#effect}) both run CUser::OnEffect 545EC40, so the effect body is
 * the same: {@code u8 type} and the type's fields. The type switch is the jump table 54792D8 at 5463C83 (types 0..0x70).
 * Only types whose reads were traced are built (research/tms2823/skill-trace/remote_trace.py, "CLEAN"):
 * <ul>
 *     <li>0 LevelUp (case 5463CFA): no field. Same as 265 {@code UserEffectType.LevelUp}.</li>
 *     <li>1 SkillUse (case 5464222, shared with type 2 SkillUseBySummoned as in 265): {@code i32 skillId}
 *     (5464296), {@code i32 charLevel} (54642EF), {@code i32 slv} (546431A), then reads that depend on the skill
 *     (skill-id constants and predicates, e.g. 54647DA / 5464C38 / 5467785). A skill is built only when its own
 *     packet was traced with no further read: {@link #SKILL_USE_TRACED}.</li>
 * </ul>
 * Other types (265 type numbers are not checked against 282) are not built.
 */
public final class Tms282EffectPackets {
    public static final int EFFECT = 0x04CA;

    /**
     * Skills whose SkillUse effect was traced CLEAN with exactly the three i32 (Tms282RemoteBuffCheck compares the
     * builder with research/tms2823/skill-trace/expected/eff-skill-ID.tsv for every entry).
     */
    public static final Set<Integer> SKILL_USE_TRACED = Set.of(
            1101006,   // Warrior: Rage (IndiePAD, IndiePowerGuard)
            2101001,   // Magician: Meditation (IndieMAD)
            30001001,  // Resistance: Infiltrate (Speed)
            1310016,   // Dark Knight: (ACC, EVA, EPAD, EPDD)
            4001003);  // Thief: Dark Sight

    /**
     * Skills whose SkillUse effect the batch trace read CLEAN with exactly the three i32
     * (research/tms2823/skill-trace/batch_skilluse.py over every named 282 skill, CUser::OnEffect 545EC40 with the
     * eff- options of remote_trace.py), from data/protocol/tms282.3/skilluse-traced.tsv. Empty when the file is absent.
     */
    public static final Set<Integer> SKILL_USE_BATCH = loadBatch(java.nio.file.Path.of("data", "protocol", "tms282.3",
            "skilluse-traced.tsv"));

    static Set<Integer> loadBatch(java.nio.file.Path file) {
        try {
            if (!java.nio.file.Files.isRegularFile(file)) {
                return Set.of();
            }
            java.util.Set<Integer> ids = new java.util.HashSet<>();
            for (String line : java.nio.file.Files.readAllLines(file, java.nio.charset.StandardCharsets.UTF_8)) {
                String t = line.strip();
                if (!t.isEmpty() && !t.startsWith("#")) {
                    ids.add(Integer.parseInt(t.split("[ \t]+")[0]));
                }
            }
            return Set.copyOf(ids);
        } catch (java.io.IOException | NumberFormatException e) {
            System.out.println("[TMS282] skilluse-traced.tsv unreadable, batch SkillUse effects off: " + e);
            return Set.of();
        }
    }

    /** One effect body (the part after the LP048E characterId). */
    public sealed interface UserEffect permits LevelUp, SkillUse {
        int type();

        void encode(OutPacket out);
    }

    /** Type 0. */
    public record LevelUp() implements UserEffect {
        @Override
        public int type() {
            return 0;
        }

        @Override
        public void encode(OutPacket out) {
            out.encodeByte(type());
        }
    }

    /** Type 1; build with {@link #skillUse}. */
    public record SkillUse(int skillId, int charLevel, int slv) implements UserEffect {
        @Override
        public int type() {
            return 1;
        }

        @Override
        public void encode(OutPacket out) {
            out.encodeByte(type());
            out.encodeInt(skillId);
            out.encodeInt(charLevel);
            out.encodeInt(slv);
        }
    }

    private Tms282EffectPackets() {
    }

    public static UserEffect levelUp() {
        return new LevelUp();
    }

    /** The SkillUse effect, empty when the skill's 282 reads were not traced ({@link #SKILL_USE_TRACED}). */
    public static Optional<UserEffect> skillUse(int skillId, int charLevel, int slv) {
        return SKILL_USE_TRACED.contains(skillId) || SKILL_USE_BATCH.contains(skillId)
                ? Optional.of(new SkillUse(skillId, charLevel, slv)) : Optional.empty();
    }

    /** LP04CA: the effect on the user's own character. */
    public static OutPacket local(UserEffect effect) {
        Objects.requireNonNull(effect, "effect");
        OutPacket out = Tms282LoginPacketSupport.packet(EFFECT);
        effect.encode(out);
        return out;
    }
}
