package net.swordie.ms.connection.protocol.codec;

import java.util.List;
import java.util.Objects;

/**
 * How {@link Tms282AttackDecoder} reads a TMS 282.3 attack request: the fixed header (always the verified form),
 * then these replaceable strategies in order.
 * <pre>
 * header      u32 u32 [shoot: u8 box] u8 fieldKey u8 mask(hits|mobCount&lt;&lt;4) u32 (282 new) u32 skillId u32 slv
 *             [melee/shoot: u8 addAttackProc]                                            [verified: melee, shoot]
 * beforeMobs  CRCS, BONUS_282_LINEAR, PROCESS_TYPE, PRE_MOB_265_ORDER                  [verified/presumed, see sections]
 * per mob     located by {@link #mobLocator}; u32 id, 5 x u8, u32 template, u8, 4 x u16, u16 (magic: u8 u16),
 *             u32, u32, u8, u32 (282 new), hits x u64 damage, u32 mobUpDownYRange, u32, u32  [verified]
 *             then {@link #betweenMobs}                                                             [presumed]
 * tail        {@link #tail} (client VM)                                                              [unknown]
 * </pre>
 *
 * @param beforeMobs  sections between the header and the first mob block
 * @param betweenMobs section after each mob's crc ints (only its end position is used, to find the next mob)
 * @param mobLocator  how each mob block is found
 * @param tail        what follows the last mob block
 */
public record Tms282AttackLayout(List<Tms282AttackSection> beforeMobs, Tms282AttackSection.PerMob betweenMobs,
                                 Tms282MobLocator mobLocator, Tms282AttackTail tail) {
    /** The static-analysis layout with anchored mob blocks and a raw tail. */
    public static final Tms282AttackLayout STANDARD = new Tms282AttackLayout(
            List.of(Tms282AttackSections.CRCS, Tms282AttackSections.BONUS_282_LINEAR, Tms282AttackSections.PROCESS_TYPE,
                    Tms282AttackSections.PRE_MOB_265_ORDER),
            Tms282AttackSections.MOB_INFO_265_ORDER, Tms282MobLocator.ANCHORED, Tms282AttackTail.RAW);

    public Tms282AttackLayout {
        beforeMobs = List.copyOf(Objects.requireNonNull(beforeMobs, "beforeMobs"));
        Objects.requireNonNull(betweenMobs, "betweenMobs");
        Objects.requireNonNull(mobLocator, "mobLocator");
        Objects.requireNonNull(tail, "tail");
    }

    public Tms282AttackLayout withMobLocator(Tms282MobLocator locator) {
        return new Tms282AttackLayout(beforeMobs, betweenMobs, locator, tail);
    }

    public Tms282AttackLayout withBeforeMobs(List<Tms282AttackSection> sections) {
        return new Tms282AttackLayout(sections, betweenMobs, mobLocator, tail);
    }

    public Tms282AttackLayout withBetweenMobs(Tms282AttackSection.PerMob section) {
        return new Tms282AttackLayout(beforeMobs, section, mobLocator, tail);
    }

    public Tms282AttackLayout withTail(Tms282AttackTail t) {
        return new Tms282AttackLayout(beforeMobs, betweenMobs, mobLocator, t);
    }
}
