package net.swordie.ms.client.character.skills.temp;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.world.field.Field;

/** Native282 key8 uses the no-expiry virtual method12C27F0; it ends through explicit RESET. */
public final class SecondAtomLockOnStat extends TwoStateTemporaryStat {
    public SecondAtomLockOnStat() { super(true); }

    @Override
    public boolean hasExpired(long now) {
        return !net.swordie.ms.connection.packet.Tms282RemoteBuffs.active() && super.hasExpired(now);
    }

    public static void clear(Char chr, int targetId) {
        if (!net.swordie.ms.connection.packet.Tms282RemoteBuffs.active()) return;
        var tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(CharacterTemporaryStat.SecondAtomLockOn)
                && (targetId == 0 || tsm.getOption(CharacterTemporaryStat.SecondAtomLockOn).xOption == targetId)) {
            tsm.removeStat(CharacterTemporaryStat.SecondAtomLockOn);
        }
    }

    public static void targetRemoved(Field field, int targetId) {
        if (net.swordie.ms.connection.packet.Tms282RemoteBuffs.active()) {
            for (Char chr : field.getChars()) clear(chr, targetId);
        }
    }
}
