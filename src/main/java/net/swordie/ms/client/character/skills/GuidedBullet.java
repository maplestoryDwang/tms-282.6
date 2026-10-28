package net.swordie.ms.client.character.skills;

import net.swordie.ms.client.character.skills.temp.TemporaryStatBase;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.world.field.Field;

public class GuidedBullet extends TemporaryStatBase {

    private int mobID;
    private int userID;

    public GuidedBullet() {
        super(false);
        mobID = 0;
        expireTerm = 0;
    }

    public int getMobID() {
        return mobID;
    }

    public void setMobID(int mobID) {
        this.mobID = mobID;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    @Override
    public void encode(OutPacket outPacket) {
        super.encode(outPacket);
        outPacket.encodeInt(getMobID());
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
    }

    @Override
    public void reset() {
        super.reset();
        setMobID(0);
        setUserID(0);
    }

    /** Native key4 has no timer expiry; field-owned target identity ends through RESET. */
    public static void clear(Char chr, int targetId) {
        if (!net.swordie.ms.connection.packet.Tms282RemoteBuffs.active()) return;
        var tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(CharacterTemporaryStat.GuidedBullet)
                && (targetId == 0 || tsm.getOption(CharacterTemporaryStat.GuidedBullet).xOption == targetId)) {
            tsm.removeStat(CharacterTemporaryStat.GuidedBullet);
        }
    }

    public static void targetRemoved(Field field, int targetId) {
        if (net.swordie.ms.connection.packet.Tms282RemoteBuffs.active()) {
            for (Char chr : field.getChars()) clear(chr, targetId);
        }
    }
}
