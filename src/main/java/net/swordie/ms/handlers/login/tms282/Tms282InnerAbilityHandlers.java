package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.potential.CharacterPotential;
import net.swordie.ms.client.character.potential.CharacterPotentialValueHolder;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.tms282.Tms282InventoryDiff;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.Tms282InnerAbilityPackets;
import net.swordie.ms.connection.packet.Tms282InventoryPackets;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.user.UserHandler;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.Util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 282 內在潛能 window (research/tms2823/innerability-282/REPORT.md). Every request below locks the ability UI; the
 * LP0136 (excl 1) / LP0138 (excl 1) reply unlocks it, so every path ends with one of them.
 * <ul>
 *     <li>CP01CC (5F38470) 名聲值 reset: u8 preset, i32 n, n x locked line -> 265
 *     {@code UserHandler.handleUserRequestCharacterPotentialSkillRandSetUi} (honor cost, grade roll, save), then
 *     LP0136 with the preset (265's SET carries no preset and is declined).</li>
 *     <li>CP01CD (5FA5C50) circulator: u8 preset, i32 itemId, i32 slot. Rolled here: 265 needs exactly 3 lines over
 *     all presets (never true with 3 presets) and looks for cash circulators in the use bag. LP008F + LP015E.</li>
 *     <li>CP0185 (5FA7E80) circulator choice: i32 tick, u8 accept, i32 itemId, u8 preset (282 adds the last two).
 *     Accept writes the rolled lines into the preset and saves them (265 used the flag as preset and never saved).</li>
 *     <li>CP01CE (5FA6BA0) change preset: u8 preset -> 265 handler, LP0138.</li>
 * </ul>
 * CP01CB is not inner ability (an i32 from a timer gauge) and is not routed.
 */
final class Tms282InnerAbilityHandlers {
    static final int HONOR_RESET = 0x01CC;
    static final int CIRCULATOR = 0x01CD;
    static final int CHANGE_PRESET = 0x01CE;
    static final int CIRCULATOR_CHOICE = 0x0185;
    static final int PRESETS = 3;
    static final int LINES = 3;

    /** The preset of a character's pending circulator roll (the lines are Char.characterPotentialValueHolders). */
    static final Map<Integer, Integer> PENDING = new ConcurrentHashMap<>();

    private final Charset charset;

    Tms282InnerAbilityHandlers(Tms282LoginContext context) {
        this(Objects.requireNonNull(context, "context").settings().nameCharset());
    }

    Tms282InnerAbilityHandlers(Charset charset) {
        this.charset = Objects.requireNonNull(charset, "charset");
    }

    void register(Tms282Routes routes) {
        routes.channel(HONOR_RESET, InHeader.USER_REQUEST_CHARACTER_POTENTIAL_SKILL_RAND_VALUE_SET).when(Tms282Guards.IN_FIELD)
                .handle("honor reset -> LP0139 + LP0136 + LP0137", this::onHonorReset);
        routes.channel(CIRCULATOR, InHeader.USER_REQUEST_CHARACTER_POTENTIAL_SKILL_RAND_SET_UI).when(Tms282Guards.IN_FIELD)
                .handle("circulator -> LP008F + LP015E", this::onCirculator);
        routes.channel(CIRCULATOR_CHOICE, InHeader.USER_MIRACLE_CIRCULATOR_OPTION_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("circulator choice -> LP0136 + LP0137", this::onCirculatorChoice);
        routes.channel(CHANGE_PRESET, InHeader.USER_REQUEST_CHARACTER_POTENTIAL_SKILL_CHANGE_PRESET).when(Tms282Guards.IN_FIELD)
                .handle("ability preset -> LP0138", this::onChangePreset);
    }

    // ---- CP01CC ----------------------------------------------------------------------------------------------------

    void onHonorReset(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        ByteBuffer in = body(packet);
        int preset = in.remaining() >= 5 ? in.get() : -1;
        int locks = in.remaining() >= 4 ? in.getInt() : -1;
        if (preset < 0 || preset >= PRESETS || locks < 0 || in.remaining() != locks * 7) {
            refuse(session, chr, op, packet, Math.max(preset, 0), "layout / preset " + preset);
            return;
        }
        Set<CharacterPotential> lines = chr.getPotentialsByPreset(preset);
        if (lines.isEmpty() || locks > lines.size() - 1) {
            refuse(session, chr, op, packet, preset, lines.size() + " lines, " + locks + " locked");
            return;
        }
        int honor = chr.getHonorExp();
        UserHandler.handleUserRequestCharacterPotentialSkillRandSetUi(chr,
                new InPacket(Arrays.copyOfRange(packet, 2, packet.length)));
        clampLevels(lines);
        session.send(Tms282InnerAbilityPackets.set(true, preset, chr.getPotentialsByPreset(preset)));
        System.out.printf("[TMS282] CP01CC honor reset preset %d, %d locked, honor %d -> %d character=%d%n", preset, locks,
                honor, chr.getHonorExp(), chr.getId());
    }

    // ---- CP01CD / CP0185 -------------------------------------------------------------------------------------------

    void onCirculator(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        ByteBuffer in = body(packet);
        if (in.remaining() != 9) {
            refuse(session, chr, op, packet, 0, "length");
            return;
        }
        int preset = in.get();
        int itemId = in.getInt();
        if (preset < 0 || preset >= PRESETS) {
            refuse(session, chr, op, packet, 0, "preset " + preset);
            return;
        }
        InvType type = itemId / 1_000_000 == 5 ? InvType.CASH : InvType.CONSUME;
        Item item = chr.getInventoryByType(type).getItemByItemID(itemId);
        if (item == null || !ItemConstants.isCirculatorItem(itemId)) {
            refuse(session, chr, op, packet, preset, "no circulator " + itemId);
            return;
        }
        Set<CharacterPotential> lines = chr.getPotentialsByPreset(preset);
        if (lines.size() != LINES) {
            chr.chatMessage("需要開啟 3 行內在潛能才能使用。");
            refuse(session, chr, op, packet, preset, lines.size() + " lines");
            return;
        }
        byte grade = chr.getPotentialMan().getGrade(preset);
        boolean circulator506 = itemId / 1000 == 5062;
        if (circulator506 && grade < 1) {
            chr.chatMessage("該道具只能用於稀有以上等級的內在能力。");
            refuse(session, chr, op, packet, preset, "grade " + grade);
            return;
        }
        if (!circulator506 && grade >= 3) {
            chr.chatMessage("該道具不能用於傳說等級的內在潛能。");
            refuse(session, chr, op, packet, preset, "legendary");
            return;
        }
        Tms282InventoryDiff bag = Tms282InventoryDiff.of(type, chr.getInventoryByType(type));
        byte newGrade = rollGrade(grade, circulator506);
        List<CharacterPotentialValueHolder> rolled = roll(chr, preset, newGrade);
        chr.setCharacterPotentialValueHolders(rolled);
        PENDING.put(chr.getId(), preset);
        chr.consumeItem(itemId, 1);
        boolean refund = !circulator506 && Util.getRandom(0, 2) > 0 && chr.getConsumeInventory().getEmptySlots() > 0;
        if (refund) {
            chr.addItemToInventory(itemId, 1);
        }
        List<Tms282InventoryPackets.Operation> ops = bag.changes();
        if (!ops.isEmpty()) {
            session.send(Tms282InventoryPackets.inventoryOperation(false, ops, charset));
        }
        session.send(Tms282InnerAbilityPackets.circulatorResult(rolled, itemId, preset));
        if (refund) {
            session.send(Tms282InnerAbilityPackets.honorGift(0, 1));
        }
        System.out.printf("[TMS282] CP01CD circulator %d preset %d grade %d -> %d%s character=%d%n", itemId, preset, grade,
                newGrade, refund ? " (item given back)" : "", chr.getId());
    }

    void onCirculatorChoice(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        ByteBuffer in = body(packet);
        if (in.remaining() != 10) {
            refuse(session, chr, op, packet, 0, "length");
            return;
        }
        in.getInt(); // tick
        boolean accept = in.get() != 0;
        in.getInt(); // itemId
        int preset = in.get();
        Integer pending = PENDING.remove(chr.getId());
        List<CharacterPotentialValueHolder> rolled = new ArrayList<>(chr.getCharacterPotentialValueHolder());
        chr.getCharacterPotentialValueHolder().clear();
        if (pending == null || pending != preset || rolled.isEmpty()) {
            refuse(session, chr, op, packet, Math.max(0, Math.min(PRESETS - 1, preset)), "no pending roll for preset " + preset);
            return;
        }
        if (accept) {
            for (CharacterPotential cp : chr.getPotentialsByPreset(preset)) {
                CharacterPotentialValueHolder now = rolled.stream().filter(h -> h.getKey() == cp.getKey()).findFirst().orElse(null);
                if (now != null) {
                    cp.setSkillID(now.getSkillID());
                    cp.setSlv(now.getSlv());
                    cp.setGrade(now.getGrade());
                    cp.saveToSQL();
                }
            }
        }
        session.send(Tms282InnerAbilityPackets.set(true, preset, chr.getPotentialsByPreset(preset)));
        session.send(Tms282InnerAbilityPackets.reset(chr));
        System.out.printf("[TMS282] CP0185 circulator %s preset %d character=%d%n", accept ? "accepted" : "declined",
                preset, chr.getId());
    }

    /** 265 grade changes of the circulators: 5062xxx only up, 2702xxx up or down. */
    static byte rollGrade(byte grade, boolean circulator506) {
        int up;
        int down;
        switch (grade) {
            case 0 -> { up = GameConstants.BASE_CHAR_RARE_POT_UP_RATE; down = GameConstants.BASE_CHAR_RARE_POT_DOWN_RATE; }
            case 1 -> { up = GameConstants.BASE_CHAR_EPIC_POT_UP_RATE; down = GameConstants.BASE_CHAR_EPIC_POT_DOWN_RATE; }
            case 2 -> { up = GameConstants.BASE_CHAR_UNIQUE_POT_UP_RATE; down = GameConstants.BASE_CHAR_UNIQUE_POT_DOWN_RATE; }
            default -> { up = GameConstants.BASE_CHAR_LEGENDARY_POT_UP_RATE; down = GameConstants.BASE_CHAR_LEGENDARY_POT_DOWN_RATE; }
        }
        if (grade < 3 && Util.succeedProp(up)) {
            return (byte) (grade + 1);
        }
        if (!circulator506 && grade > 0 && Util.succeedProp(down)) {
            return (byte) (grade - 1);
        }
        return grade;
    }

    /** Three new lines (keys 1..3) of {@code grade}: distinct abilities, level within the grade and the skill's max. */
    static List<CharacterPotentialValueHolder> roll(Char chr, int preset, byte grade) {
        Set<Integer> used = new HashSet<>(chr.getPotentialSkills(preset));
        List<CharacterPotentialValueHolder> rolled = new ArrayList<>(LINES);
        for (byte key = 1; key <= LINES; key++) {
            int skillId;
            SkillInfo si;
            int guard = 0;
            do {
                skillId = Util.getRandom(GameConstants.CHAR_POT_BASE_ID, GameConstants.CHAR_POT_END_ID + 1); // end exclusive
                si = SkillData.getSkillInfoById(skillId);
            } while ((si == null || used.contains(skillId)) && ++guard < 500);
            used.add(skillId);
            int base = grade * 10;
            int slv = 1 + Util.getRandom(base, base + 10); // base+1 .. base+10
            if (si != null && si.getMaxLevel() > 0) {
                slv = Math.min(slv, si.getMaxLevel());
            }
            rolled.add(new CharacterPotentialValueHolder(key, skillId, (byte) slv, grade));
        }
        return rolled;
    }

    /** 265 generateRandomPotential can exceed an ability's max level (some stop at 10 / 20 / 30). */
    static void clampLevels(Set<CharacterPotential> lines) {
        for (CharacterPotential cp : lines) {
            SkillInfo si = SkillData.getSkillInfoById(cp.getSkillID());
            if (si != null && si.getMaxLevel() > 0 && cp.getSlv() > si.getMaxLevel()) {
                cp.setSlv((byte) si.getMaxLevel());
                cp.saveToSQL();
            }
        }
    }

    // ---- CP01CE ----------------------------------------------------------------------------------------------------

    void onChangePreset(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        ByteBuffer in = body(packet);
        int preset = in.remaining() == 1 ? in.get() : -1;
        if (preset < 0 || preset >= PRESETS) {
            session.send(Tms282InnerAbilityPackets.changePreset(true, currentPreset(chr)));
            System.out.printf("[TMS282] REFUSED CP01CE len=%d preset %d character=%d%n", packet.length, preset, chr.getId());
            return;
        }
        UserHandler.handleUserRequestCharacterPotentialSkillChangePreset(chr,
                new InPacket(Arrays.copyOfRange(packet, 2, packet.length)));
        System.out.printf("[TMS282] CP01CE ability preset -> %d character=%d%n", preset, chr.getId());
    }

    static int currentPreset(Char chr) {
        String v = chr.getQRValueByKey(net.swordie.ms.constants.QuestConstants.CHARACTER_POTENTIAL_PRESET, "potential");
        try {
            return v == null ? 0 : Math.max(0, Math.min(PRESETS - 1, Integer.parseInt(v.trim())));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ---- shared ----------------------------------------------------------------------------------------------------

    static ByteBuffer body(byte[] packet) {
        return ByteBuffer.wrap(packet, 2, Math.max(0, packet.length - 2)).slice().order(ByteOrder.LITTLE_ENDIAN);
    }

    /** LP0136 excl with the preset's current lines: unlocks the ability UI and changes nothing. */
    private static void refuse(Tms282LoginState session, Char chr, int op, byte[] packet, int preset, String why) {
        session.send(Tms282InnerAbilityPackets.set(true, preset, chr.getPotentialsByPreset(preset)));
        System.out.printf("[TMS282] REFUSED CP%04X len=%d character=%d: %s; SENT LP0136 unlock%n", op, packet.length,
                chr.getId(), why);
    }
}
