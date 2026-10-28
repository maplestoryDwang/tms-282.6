package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.life.mob.skill.BurnedInfo;

import java.util.Collection;
import java.util.Set;
import java.util.TreeMap;

/** GMS 265: delegates to {@link MobTemporaryStat} (20-byte mask, 265 bit numbers and value order). */
public final class Gms265MobStatCodec implements MobStatCodec {
    public static final Gms265MobStatCodec INSTANCE = new Gms265MobStatCodec();

    private Gms265MobStatCodec() {
    }

    @Override
    public int maskInts() {
        return MobStat.LENGTH;
    }

    @Override
    public int[] mask(Collection<MobStat> stats) {
        return MobTemporaryStat.getMaskByCollection(keys(stats));
    }

    @Override
    public void encode(OutPacket out, TreeMap<MobStat, Option> stats, Set<BurnedInfo> burnedInfos, String linkTeam) {
        MobTemporaryStat.encode(out, stats, burnedInfos, linkTeam);
    }

    @Override
    public boolean hasMovementAffectingStat(Collection<MobStat> stats) {
        return MobTemporaryStat.hasRemovedMovementAffectingStat(keys(stats));
    }

    @Override
    public Set<MobStat> unsupported(Collection<MobStat> stats) {
        return Set.of();
    }

    private static TreeMap<MobStat, Option> keys(Collection<MobStat> stats) {
        TreeMap<MobStat, Option> map = new TreeMap<>();
        for (MobStat s : stats) {
            map.put(s, null);
        }
        return map;
    }
}
