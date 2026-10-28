package net.swordie.ms.client.character.skills.temp;

/** Timed mount metadata for the 282 Option-based writer; the 265 dynamic TSB format is unchanged. */
public final class RideVehicleExpireStat extends TwoStateTemporaryStat {
    public RideVehicleExpireStat() { super(true); }

    @Override
    public void setExpireTerm(int seconds) {
        super.setExpireTerm(seconds);
        if (net.swordie.ms.connection.packet.Tms282RemoteBuffs.active()) {
            getOption().tOption = seconds * 1000;
            getOption().setInMillis(true);
        }
    }

    @Override
    public boolean hasExpired(long now) {
        if (!net.swordie.ms.connection.packet.Tms282RemoteBuffs.active()) return super.hasExpired(now);
        var o = getOption();
        long duration = o.isInMillis() ? o.tOption : o.tOption * 1000L;
        return o.startTime > 0 && duration > 0 && now - o.startTime >= duration;
    }
}
