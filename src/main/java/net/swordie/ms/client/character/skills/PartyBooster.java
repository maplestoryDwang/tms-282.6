package net.swordie.ms.client.character.skills;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TwoStateTemporaryStat;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.enums.TSIndex;
import net.swordie.ms.util.Util;

public class PartyBooster extends TwoStateTemporaryStat {

    private int currentTime;
    private long startTime;

    public PartyBooster() {
        super(false);
        startTime = 0;
        currentTime = 0;
        expireTerm = 0;
    }

    @Override
    public int getExpireTerm() {
        return expireTerm;
    }

    @Override
    public boolean hasExpired(long now) {
        if (net.swordie.ms.connection.packet.Tms282RemoteBuffs.active()) {
            Option o = getOption();
            long duration = o.isInMillis() ? o.tOption : o.tOption * 1000L;
            return o.startTime > 0 && duration > 0 && now - o.startTime >= duration;
        }
        return startTime > 0 && expireTerm > 0 && (now - startTime) >= expireTerm * 1000L;
    }

    @Override
    public void setExpireTerm(int seconds) {
        super.setExpireTerm(seconds);
        if (net.swordie.ms.connection.packet.Tms282RemoteBuffs.active()) {
            // The 282 encoders receive Options, while 265 still serializes this TSB directly.
            getOption().tOption = seconds * 1000;
            getOption().setInMillis(true);
        }
    }

    public int getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(int currentTime) {
        this.currentTime = currentTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
        if (net.swordie.ms.connection.packet.Tms282RemoteBuffs.active()) getOption().startTime = startTime;
    }

    /** 282's accepted-cast bridge bypasses 265's massSpell loop. Only these two verified party speed buffs. */
    public static void applyToParty(Char source, SkillInfo info, int level) {
        int id = info.getSkillId();
        if (!net.swordie.ms.connection.packet.Tms282RemoteBuffs.active()
                || (id != 5121009 && id != 15121005) || !info.isMassSpell()
                || source.getParty() == null || source.getPartyID() == 0 || source.getField() == null
                || info.getFirstRect() == null || source.getHP() <= 0
                || !source.getTemporaryStatManager().hasStat(CharacterTemporaryStat.PartyBooster)) return;
        var bounds = source.getRectAround(info.getFirstRect());
        for (Char target : source.getParty().getPartyMembersInSameField(source)) {
            if (target == null || target == source || target.getField() != source.getField()
                    || target.getPartyID() != source.getPartyID() || target.getHP() <= 0
                    || !bounds.hasPositionInside(target.getPosition())) continue;
            var tsm = target.getTemporaryStatManager();
            var state = (PartyBooster) tsm.getTSBByTSIndex(TSIndex.PartyBooster);
            state.setNOption(info.getValue(SkillStat.x, level));
            state.setROption(id);
            state.setStartTime(System.currentTimeMillis());
            state.setExpireTerm(info.getValue(SkillStat.time, level));
            tsm.sendStat(CharacterTemporaryStat.PartyBooster, state.getOption());
        }
    }

    @Override
    public void reset() {
        super.reset();
        setCurrentTime(0);
    }

    @Override
    public void encode(OutPacket outPacket) {
        super.encode(outPacket);
        outPacket.encodeTime(getCurrentTime());
        outPacket.encodeShort(getExpireTerm());
    }
}
