package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.potential.CharacterPotential;
import net.swordie.ms.client.character.potential.CharacterPotentialValueHolder;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.util.FileTime;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 282 內在潛能 / 名聲值 packets (research/tms2823/innerability-282/REPORT.md). Line = u8 key, i32 skillId, u8 slv,
 * u8 grade (8E3780).
 * <ul>
 *     <li>LP0136 SET (5FA5A20): u8 excl, u8 preset, u16 n, lines. 265 writes a "changed" byte where 282 reads the
 *     preset, so the 265 body is declined and {@code Tms282InnerAbilityHandlers} sends {@link #set}.</li>
 *     <li>LP0137 RESET (5FA5AA0): 3 x (u16 n, lines) - 265 body.</li>
 *     <li>LP0138 CHANGE_PRESET (5FA5AF0): u8 excl, u8 preset - 265 body.</li>
 *     <li>LP0139 HONOR_EXP (5F38400): i32 honor EXP - 265 body.</li>
 *     <li>LP013D HONOR_GIFT (5E490A0): i32 result (0 given / 4 bag full), i32 amount; 265 writes the empty slots
 *     first, rewritten to 0 (265 only sends it after the item was given).</li>
 *     <li>LP015E MIRACLE_CIRCULATOR_RESULT (5FA5820 -> 8F3A00): i32 n, n x (i32 skill, u8 slv, u8 key, u8 grade),
 *     i32 itemId, FILETIME, u8 flag, u8 preset, i32 (ignored). 265 writes preset 0: declined, {@link #circulatorResult}.</li>
 * </ul>
 */
public final class Tms282InnerAbilityPackets implements Tms282LegacyPackets.Contributor {
    public static final Tms282InnerAbilityPackets INSTANCE = new Tms282InnerAbilityPackets();

    public static final int SET = 0x0136;
    public static final int RESET = 0x0137;
    public static final int CHANGE_PRESET = 0x0138;
    public static final int HONOR_EXP = 0x0139;
    public static final int HONOR_GIFT = 0x013D;
    public static final int CIRCULATOR_RESULT = 0x015E;

    private Tms282InnerAbilityPackets() {
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        builder.on(OutHeader.CHARACTER_POTENTIAL_RESET, p -> Tms282SkillLegacyTranslators.copy(p, RESET))
                .on(OutHeader.CHARACTER_POTENTIAL_CHANGE_PRESET, p -> Tms282SkillLegacyTranslators.copy(p, CHANGE_PRESET))
                .on(OutHeader.CHARACTER_HONOR_EXP, p -> Tms282SkillLegacyTranslators.copy(p, HONOR_EXP))
                .on(OutHeader.CHARACTER_HONOR_GIFT, Tms282InnerAbilityPackets::honorGift)
                // no preset in the 265 bodies: the 282 handlers send set() / circulatorResult() themselves
                .on(OutHeader.CHARACTER_POTENTIAL_SET, p -> Optional.empty())
                .on(OutHeader.MIRACLE_CIRCULATOR_RESULT, p -> Optional.empty());
    }

    static Optional<OutPacket> honorGift(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        body.i32(); // 265 empty use slots
        return Optional.of(honorGift(0, body.i32()));
    }

    static List<CharacterPotential> sorted(Collection<CharacterPotential> lines) {
        return lines.stream().sorted(Comparator.comparingInt(CharacterPotential::getKey)).toList();
    }

    public static OutPacket set(boolean excl, int preset, Collection<CharacterPotential> lines) {
        OutPacket out = Tms282LoginPacketSupport.packet(SET);
        out.encodeByte(excl);
        out.encodeByte(preset);
        List<CharacterPotential> sorted = sorted(lines);
        out.encodeShort(sorted.size());
        for (CharacterPotential cp : sorted) {
            cp.encode(out);
        }
        return out;
    }

    public static OutPacket reset(Char chr) {
        OutPacket out = Tms282LoginPacketSupport.packet(RESET);
        for (int i = 0; i < 3; i++) {
            List<CharacterPotential> sorted = sorted(chr.getPotentialsByPreset(i));
            out.encodeShort(sorted.size());
            for (CharacterPotential cp : sorted) {
                cp.encode(out);
            }
        }
        return out;
    }

    public static OutPacket changePreset(boolean excl, int preset) {
        OutPacket out = Tms282LoginPacketSupport.packet(CHANGE_PRESET);
        out.encodeByte(excl);
        out.encodeByte(preset);
        return out;
    }

    public static OutPacket honorExp(int exp) {
        OutPacket out = Tms282LoginPacketSupport.packet(HONOR_EXP);
        out.encodeInt(exp);
        return out;
    }

    public static OutPacket honorGift(int result, int amount) {
        OutPacket out = Tms282LoginPacketSupport.packet(HONOR_GIFT);
        out.encodeInt(result);
        out.encodeInt(amount);
        return out;
    }

    public static OutPacket circulatorResult(List<CharacterPotentialValueHolder> lines, int itemId, int preset) {
        OutPacket out = Tms282LoginPacketSupport.packet(CIRCULATOR_RESULT);
        out.encodeInt(lines.size());
        for (CharacterPotentialValueHolder cp : lines) {
            out.encodeInt(cp.getSkillID());
            out.encodeByte(cp.getSlv());
            out.encodeByte(cp.getKey());
            out.encodeByte(cp.getGrade());
        }
        out.encodeInt(itemId);
        out.encodeFT(FileTime.currentTime());
        out.encodeByte(1);
        out.encodeByte(preset);
        out.encodeInt(4);
        return out;
    }
}
