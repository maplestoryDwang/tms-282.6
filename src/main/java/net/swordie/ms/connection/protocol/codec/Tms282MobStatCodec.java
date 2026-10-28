package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.skill.BurnedInfo;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.Util;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;

import static net.swordie.ms.life.mob.MobStat.*;

/**
 * TMS 282.3 MobTemporaryStat (decoder 0xECA3D0, mobstat-bits-282.md):
 * <ol>
 * <li>24-byte mask (bits from {@link Tms282MobStatBits});</li>
 * <li>indie bits 0..9 in bit order (ZArray of 10, EC45D4): {@code I n} + n x {@code I reason, I value, I key,
 * I elapsed, I term, I m + m x (I, I), I k + k x (I, I)} (0x9DDEB0; 7 ints per entry like 265, but 265 repeats the
 * whole indie set once per indie stat);</li>
 * <li>standard bits 11..104 in bit order: {@code I n, I r, S t/500};</li>
 * <li>the 54 explicit blocks in the order of section 3 ({@link #EXPLICIT}).</li>
 * </ol>
 * 265 stats without a 282 bit are left out of the mask and the values, and reported once to the
 * {@link SkipListener}.
 */
public final class Tms282MobStatCodec implements MobStatCodec {
    /** Receives the stats a packet had to leave out. */
    @FunctionalInterface
    public interface SkipListener {
        void skipped(MobStat stat, String reason);
    }

    /** Console + {@code Packets/mobstat-282-skipped.txt}, once per stat per process. */
    public static final SkipListener LOG_ONCE = new SkipListener() {
        private final Set<MobStat> seen = ConcurrentHashMap.newKeySet();

        @Override
        public void skipped(MobStat stat, String reason) {
            if (seen.add(stat)) {
                String line = "[Tms282MobStat] not sent: " + stat + " (" + reason + ")";
                System.out.println(line);
                try {
                    DataPrinter.send("Packets/mobstat-282-skipped.txt", line, false);
                } catch (RuntimeException ignored) {
                }
            }
        }
    };

    public static final Tms282MobStatCodec INSTANCE = new Tms282MobStatCodec(LOG_ONCE, Util::getCurrentTime);

    private final SkipListener skipListener;
    private final IntSupplier clock;

    /** @param clock current time in the unit of {@link Option#tStart} (indie elapsed time) */
    public Tms282MobStatCodec(SkipListener skipListener, IntSupplier clock) {
        this.skipListener = skipListener;
        this.clock = clock;
    }

    @Override
    public int maskInts() {
        return Tms282MobStatBits.MASK_INTS;
    }

    @Override
    public int[] mask(Collection<MobStat> stats) {
        int[] mask = new int[Tms282MobStatBits.MASK_INTS];
        for (MobStat s : stats) {
            Tms282MobStatBits.bit(s).ifPresent(bit -> mask[bit / 32] |= 1 << (31 - bit % 32));
        }
        return mask;
    }

    @Override
    public boolean hasMovementAffectingStat(Collection<MobStat> stats) {
        for (MobStat s : stats) {
            Integer bit = Tms282MobStatBits.bit(s).orElse(null);
            if (bit != null && Tms282MobStatBits.MOVEMENT_BITS.contains(bit)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<MobStat> unsupported(Collection<MobStat> stats) {
        Set<MobStat> out = EnumSet.noneOf(MobStat.class);
        for (MobStat s : stats) {
            if (!Tms282MobStatBits.isSupported(s)) {
                out.add(s);
            }
        }
        return out;
    }

    /** Reports the unsupported stats of {@code stats} to the listener; returns them. */
    public Set<MobStat> reportUnsupported(Collection<MobStat> stats) {
        Set<MobStat> skipped = unsupported(stats);
        for (MobStat s : skipped) {
            skipListener.skipped(s, Tms282MobStatBits.whyUnsupported(s));
        }
        return skipped;
    }

    @Override
    public void encode(OutPacket out, TreeMap<MobStat, Option> stats, Set<BurnedInfo> burnedInfos, String linkTeam) {
        reportUnsupported(stats.keySet());
        TreeMap<Integer, Map.Entry<MobStat, Option>> byBit = new TreeMap<>();
        for (Map.Entry<MobStat, Option> e : stats.entrySet()) {
            Tms282MobStatBits.bit(e.getKey()).ifPresent(bit -> byBit.put(bit, e));
        }
        encodeMask(out, stats.keySet());
        for (Map.Entry<Integer, Map.Entry<MobStat, Option>> e : byBit.entrySet()) {
            if (Tms282MobStatBits.isIndie(e.getKey())) {
                encodeIndie(out, e.getValue().getValue());
            }
        }
        for (Map.Entry<Integer, Map.Entry<MobStat, Option>> e : byBit.entrySet()) {
            if (Tms282MobStatBits.isStandard(e.getKey())) {
                Option o = option(e.getValue());
                out.encodeInt(o.nOption);
                out.encodeInt(o.rOption);
                out.encodeShort(o.tOption / 500);
            }
        }
        Context ctx = new Context(stats, burnedInfos, linkTeam);
        for (ExplicitBlock block : EXPLICIT) {
            if (block.applies(stats)) {
                block.writer().write(out, block.option(stats), ctx);
            }
        }
    }

    private void encodeIndie(OutPacket out, Option o) {
        if (o == null) {
            out.encodeInt(0);
            return;
        }
        out.encodeInt(1);
        out.encodeInt(o.nReason);
        out.encodeInt(o.nValue);
        out.encodeInt(o.nKey);
        out.encodeInt(clock.getAsInt() - o.tStart);
        out.encodeInt(o.tTerm);
        out.encodeInt(0); // m, then m x (I, I)
        out.encodeInt(0); // k, then k x (I, I)
    }

    private static Option option(Map.Entry<MobStat, Option> e) {
        return e.getValue() != null ? e.getValue() : new Option();
    }

    // ---------------------------------------------------------------- explicit blocks (section 3 order)

    private record Context(TreeMap<MobStat, Option> stats, Set<BurnedInfo> burnedInfos, String linkTeam) {
    }

    @FunctionalInterface
    private interface BlockWriter {
        void write(OutPacket out, Option o, Context ctx);
    }

    /** One explicit block; read when any of its stats is in the mask (only #8 has two). */
    private record ExplicitBlock(List<MobStat> triggers, BlockWriter writer) {
        boolean applies(Map<MobStat, Option> stats) {
            return triggers.stream().anyMatch(stats::containsKey);
        }

        Option option(Map<MobStat, Option> stats) {
            for (MobStat s : triggers) {
                if (stats.containsKey(s)) {
                    return stats.get(s) != null ? stats.get(s) : new Option();
                }
            }
            return new Option();
        }
    }

    private static ExplicitBlock block(MobStat stat, BlockWriter writer) {
        return new ExplicitBlock(List.of(stat), writer);
    }

    private static ExplicitBlock block(List<MobStat> stats, BlockWriter writer) {
        return new ExplicitBlock(stats, writer);
    }

    /**
     * Section 3 of mobstat-bits-282.md, in client read order. Not listed: #12 GhostDisposition (bit 61, 265
     * SpiritGate is not sent) and #34 bit 106 ({@code B B}, no 265 name).
     */
    private static final List<ExplicitBlock> EXPLICIT = List.of(
            block(PDR, (out, o, c) -> out.encodeInt(o.cOption)),                       // 1
            block(MDR, (out, o, c) -> out.encodeInt(o.cOption)),                       // 2
            block(Speed, (out, o, c) -> out.encodeInt(o.mOption)),                     // 3
            block(Freeze, (out, o, c) -> out.encodeInt(o.cOption)),                    // 4 (265 wrote Stun here)
            block(BeforeFreeze, (out, o, c) -> out.encodeInt(o.cOption)),              // 5
            block(PCounter, (out, o, c) -> out.encodeInt(o.wOption)),                  // 6
            block(MCounter, (out, o, c) -> out.encodeInt(o.wOption)),                  // 7
            block(List.of(PCounter, MCounter), (out, o, c) -> {                        // 8
                out.encodeInt(o.mOption);  // nCounterProb
                out.encodeByte(o.bOption); // bCounterDelay
                out.encodeInt(o.nReason);  // nAggroRank
            }),
            block(ReduceFinalDamage, (out, o, c) -> {                                  // 9
                out.encodeByte(o.bOption);
                out.encodeInt(o.xOption);
            }),
            block(TotalDamParty, (out, o, c) -> ints(out, o.wOption, o.pOption, o.cOption)),   // 10
            block(Fatality, (out, o, c) -> ints(out, o.wOption, o.uOption, o.pOption)),        // 11
            block(ElementDarkness, (out, o, c) -> out.encodeInt(o.xOption)),           // 13
            block(DeadlyCharge, (out, o, c) -> ints(out, o.pOption, o.pOption)),       // 14
            block(Incizing, (out, o, c) -> ints(out, o.wOption, o.uOption, o.pOption)),        // 15
            block(BMageDebuff, (out, o, c) -> out.encodeInt(o.cOption)),               // 16
            block(BattlePvPHelenaMark, (out, o, c) -> out.encodeInt(o.cOption)),       // 17
            block(BattlePvP_Darklord_Explosion, (out, o, c) -> out.encodeInt(o.cOption)),      // 18
            block(MultiPMDR, (out, o, c) -> out.encodeInt(o.cOption)),                 // 19
            block(BahamutLightElemAddDam, (out, o, c) -> ints(out, o.pOption, o.cOption)),     // 20
            block(MultiDamSkill, (out, o, c) -> out.encodeInt(o.cOption)),             // 21
            block(LefDebuff, (out, o, c) -> ints(out, o.xOption, o.yOption, o.zOption)),       // 22
            block(BuffControl, (out, o, c) -> out.encodeInt(o.xOption)),               // 23
            block(BattlePvP_Ryude_Frozen, (out, o, c) -> out.encodeInt(o.xOption)),    // 24
            block(Poison, (out, o, c) -> out.encodeInt(o.xOption)),                    // 25
            block(Ambush, (out, o, c) -> out.encodeInt(o.xOption)),                    // 26
            block(MobLock, (out, o, c) -> out.encodeInt(o.xOption)),                   // 27
            block(Panic, (out, o, c) -> ints(out, o.xOption, o.yOption)),              // 28
            block(Sleep, (out, o, c) -> ints(out, o.nOption, o.bOption)),              // 29 (265: B B)
            block(Weakness, (out, o, c) -> out.encodeInt(o.xOption)),                  // 30
            block(Explosion, (out, o, c) -> out.encodeInt(o.wOption)),                 // 31
            block(BurnedInfo, (out, o, c) -> {                                         // 32
                out.encodeByte(c.burnedInfos().size());
                for (BurnedInfo bi : c.burnedInfos()) {
                    encodeBurnedInfo(out, bi);
                }
            }),
            block(NewBurnedInfo, (out, o, c) -> {                                      // 33
                List<Option> values = o.extraOpts != null ? o.extraOpts : List.of();
                out.encodeInt(values.size());
                for (Option v : values) {
                    ints(out, v.nOption, v.cOption, v.rOption, v.mOption, v.xOption, v.yOption, v.zOption,
                            v.tOption, v.uOption, 0, 1, 1);
                }
            }),
            block(ExchangeAttack, (out, o, c) -> out.encodeByte(o.bOption)),          // 35
            block(ExtraBuffStat, (out, o, c) -> {                                      // 36
                List<Option> values = o.extraOpts != null ? o.extraOpts : List.of();
                out.encodeByte(!values.isEmpty());
                if (!values.isEmpty()) {
                    Option v = values.getFirst();
                    ints(out, v.nOption, v.mOption, v.xOption, v.yOption);
                }
            }),
            block(LinkTeam, (out, o, c) -> out.encodeString(c.linkTeam() != null ? c.linkTeam() : "")),  // 37
            block(Unk129, (out, o, c) -> out.encodeInt(o.xOption)),                    // 38
            block(Unk130, (out, o, c) -> out.encodeLong(o.xOption)),                   // 39
            block(Unk131, (out, o, c) -> out.encodeInt(o.xOption)),                    // 40
            block(Unk132, (out, o, c) -> out.encodeInt(o.xOption)),                    // 41
            block(Unk134, (out, o, c) -> out.encodeInt(o.xOption)),                    // 42 (265 wrote it twice)
            block(Unk135, (out, o, c) -> out.encodeInt(o.xOption)),                    // 43
            block(OriginDebuff, (out, o, c) -> {                                       // 44 (265: S only)
                out.encodeShort(o.xOption);
                out.encodeInt(o.yOption);
            }),
            block(SoulExplosion, (out, o, c) -> ints(out, o.nOption, o.rOption, o.wOption)),   // 45
            block(TrueSight, (out, o, c) -> ints(out, o.xOption, o.yOption, o.zOption, o.cOption, o.pOption,
                    o.uOption, o.wOption)),                                            // 46
            block(Laser, (out, o, c) -> ints(out, o.nOption, o.rOption, o.tOption / 500, o.wOption,
                    o.uOption)),                                                       // 47
            block(Unk128, (out, o, c) -> ints(out, o.xOption, o.yOption, o.zOption)),  // 48
            block(Unk136, (out, o, c) -> ints(out, o.xOption, o.yOption)),             // 49
            block(Unk137, (out, o, c) -> ints(out, o.xOption, o.yOption, o.zOption)),  // 50
            block(Unk139, (out, o, c) -> out.encodeLong(o.xOption)),                   // 51
            block(ChangeMobAction, (out, o, c) -> out.encodeInt(o.xOption)),           // 52
            block(Unk138, (out, o, c) -> {                                             // 53
                ints(out, o.xOption, o.yOption);
                out.encodeShort(o.zOption);
            }),
            block(IndieAddFinalDamSkill, (out, o, c) -> ints(out, o.xOption, o.yOption, o.zOption))   // 54
    );

    /**
     * 282 BurnedInfo entry {@code L I I L 11xI L I}: the 265 entry ({@code L I I L 12xI L}) with its last int moved
     * behind the final long (same length; field meaning of the moved int is inferred).
     */
    private static void encodeBurnedInfo(OutPacket out, BurnedInfo bi) {
        out.encodeLong(0);
        out.encodeInt(bi.getCharacterId());
        out.encodeInt(bi.getSkillId());
        out.encodeLong(bi.getDamage());
        ints(out, bi.getInterval(), bi.getEnd(), bi.getDotAnimation(), bi.getDotCount(), 0, bi.getAttackDelay(),
                bi.getSuperPos(), 0, bi.getDotTickDamR(), bi.getAddedTickDamR(), bi.getEnd() - bi.getStart());
        out.encodeLong(bi.getDamage());
        out.encodeInt(bi.getInterval() / 1000);
    }

    private static void ints(OutPacket out, int... values) {
        for (int v : values) {
            out.encodeInt(v);
        }
    }
}
