package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Macro;
import net.swordie.ms.client.character.keys.FuncKeyMap;
import net.swordie.ms.connection.OutPacket;

import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.str;

/**
 * TMS 282.3 key configuration and skill macro packets. Evidence: research/tms2823/packet-audit/ingame-layouts-282.md
 * section 4. A separate factory because both belong to neither CStage ({@link Tms282StagePackets}, LP02EF..LP02F3)
 * nor the login factories: LP0B16 is dispatched by the CField sub-dispatcher 3AE8D60 (LP0B16..LP0B1C, key config and
 * pet auto-potions) and LP02EE fills the macro system data, and both are answered by the KeyConfig CPs
 * (CP020F, CP01B0).
 * <p>
 * LP0B16 FUNC_KEY_MAPPED_MAN_INIT (handler 3AE7F40) [verified]: for each of <b>4</b> presets
 * {@code u8 isDefault}; when 0, 89 x (u8 type, i32 val) keys and 10 x (u8 type, i32 val) combination slots.
 * Then {@code u8 hasQuickSlots}, and when non-zero 32 x i32. 265 sends only 3 presets, which makes the 282 client
 * read the quick-slot flag as the fourth isDefault and misread everything after it.
 * <p>
 * LP02EE MACRO_SYS_DATA_INIT (5F2B620 -> 903910) [verified]: {@code u8 count}, then per macro Str name, u8 muted,
 * 3 x i32 skill. The client reads at most 5 entries; this builder never sends more.
 */
public final class Tms282KeyPackets {
    public static final int FUNC_KEY_MAPPED_MAN_INIT = 0x0B16;
    public static final int MACRO_SYS_DATA_INIT = 0x02EE;
    public static final int PRESET_COUNT = 4;
    public static final int KEY_COUNT = 89;
    public static final int COMBINATION_COUNT = 10;
    public static final int QUICK_SLOT_COUNT = 32;
    /** 90392F cmova: the client keeps at most 5 macros. */
    public static final int MAX_MACROS = 5;

    private Tms282KeyPackets() {
    }

    /**
     * LP0B16. Preset p is taken from {@code presets.get(p)}; a missing preset, or one with no bindings, is sent as
     * {@code u8 1} (the client's built-in default table 6CB530). A preset with bindings is encoded by the 265
     * {@link FuncKeyMap#encode(OutPacket)}, whose per-preset form is the same as 282's (u8 0, 89 keys, combination
     * indexes 102..111). {@code quickSlotKeys} null or empty sends {@code u8 0} (client default 6CB540); otherwise
     * exactly the first 32 values, missing ones as 0 (265 FieldPacket.quickslotInit).
     */
    public static OutPacket funcKeyMappedInit(Map<Integer, FuncKeyMap> presets, List<Integer> quickSlotKeys) {
        Objects.requireNonNull(presets, "presets");
        OutPacket out = Tms282LoginPacketSupport.packet(FUNC_KEY_MAPPED_MAN_INIT);
        for (int preset = 0; preset < PRESET_COUNT; preset++) {
            FuncKeyMap map = presets.get(preset);
            if (map == null || map.getKeymap() == null || map.getKeymap().isEmpty()) {
                out.encodeByte(1);
            } else {
                map.encode(out);
            }
        }
        boolean hasQuickSlots = quickSlotKeys != null && !quickSlotKeys.isEmpty();
        out.encodeByte(hasQuickSlots ? 1 : 0);
        if (hasQuickSlots) {
            for (int i = 0; i < QUICK_SLOT_COUNT; i++) {
                Integer key = i < quickSlotKeys.size() ? quickSlotKeys.get(i) : null;
                out.encodeInt(key != null ? key : 0);
            }
        }
        return out;
    }

    /**
     * LP02EE. Macros are ordered by database ID (as 265 Char.initKeyBoards does) and cut to the first 5; names are
     * written in {@code charset} (the same charset as character names). A null name is sent empty.
     */
    public static OutPacket macroSysDataInit(List<Macro> macros, Charset charset) {
        Objects.requireNonNull(charset, "charset");
        List<Macro> sent = macros == null ? List.of() : macros.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingLong(Macro::getId))
                .limit(MAX_MACROS)
                .toList();
        OutPacket out = Tms282LoginPacketSupport.packet(MACRO_SYS_DATA_INIT);
        out.encodeByte(sent.size());
        for (Macro macro : sent) {
            str(out, macro.getName() == null ? "" : macro.getName(), charset);
            out.encodeByte(macro.isMuted() ? 1 : 0);
            int[] skills = macro.getSkills();
            for (int i = 0; i < 3; i++) {
                out.encodeInt(skills != null && i < skills.length ? skills[i] : 0);
            }
        }
        return out;
    }
}
