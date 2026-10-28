package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282StatPackets;
import net.swordie.ms.connection.packet.Tms282StatPackets.Stat;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * What a 265 stat change did, as TMS 282.3 LP009D stats (memento: {@link #of} before, {@link #changes} after), in
 * the manner of {@link Tms282InventoryDiff}. The 265 code ({@code Char.heal}, {@code addStat},
 * {@code syncMaxHpMpWithClient}, ...) writes its own 265 STAT_CHANGED, which the fail-closed guard drops; comparing
 * the character's stats before and after gives the 282 packet without touching that code.
 * <p>
 * Only the fixed-width stats of {@link Tms282StatPackets.Stat} are compared (level, STR/DEX/INT/LUK, HP/MaxHP,
 * MP/MaxMP, AP, EXP, fame, money), read from the 265 {@link CharacterStat} (base values: the 282 client adds
 * equipment and buffs itself, as for {@link Tms282LevelService}).
 */
public final class Tms282StatDiff {
    private final Char chr;
    private final Map<Stat, Long> before;

    private Tms282StatDiff(Char chr) {
        this.chr = chr;
        this.before = snapshot(chr);
    }

    /** Remembers {@code chr}'s stats as they are now. */
    public static Tms282StatDiff of(Char chr) {
        return new Tms282StatDiff(Objects.requireNonNull(chr, "chr"));
    }

    /** The stats whose value changed since {@link #of}, with their new values (client read order). */
    public Map<Stat, Long> changes() {
        Map<Stat, Long> now = snapshot(chr);
        Map<Stat, Long> out = new EnumMap<>(Stat.class);
        for (Map.Entry<Stat, Long> e : now.entrySet()) {
            if (!Objects.equals(before.get(e.getKey()), e.getValue())) {
                out.put(e.getKey(), e.getValue());
            }
        }
        return Collections.unmodifiableMap(out);
    }

    /** LP009D with {@link #changes()} (mask 0 when nothing changed: then it only releases the request lock). */
    public OutPacket packet(boolean exclRequest) {
        return Tms282StatPackets.statChanged(exclRequest, changes());
    }

    /** The current values of every compared stat. */
    public static Map<Stat, Long> snapshot(Char chr) {
        CharacterStat s = chr.getAvatarData().getCharacterStat();
        Map<Stat, Long> m = new EnumMap<>(Stat.class);
        m.put(Stat.LEVEL, (long) s.getLevel());
        m.put(Stat.STR, (long) s.getStr());
        m.put(Stat.DEX, (long) s.getDex());
        m.put(Stat.INT, (long) s.getInt());
        m.put(Stat.LUK, (long) s.getLuk());
        m.put(Stat.HP, (long) s.getHp());
        m.put(Stat.MAX_HP, (long) s.getMaxHp());
        m.put(Stat.MP, (long) s.getMp());
        m.put(Stat.MAX_MP, (long) s.getMaxMp());
        m.put(Stat.AP, (long) s.getAp());
        m.put(Stat.EXP, s.getExp());
        m.put(Stat.POP, (long) s.getPop());
        m.put(Stat.MONEY, chr.getMoney());
        return m;
    }
}
