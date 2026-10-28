package net.swordie.ms.client.jobs.special;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.ForceAtomInfo;
import net.swordie.ms.connection.packet.Tms282SaitamaPsychicPackets;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Util;

/** The client requests targets with CP04B4; the server authoritatively validates the range and creates the rocks. */
public final class SaitamaPsychicPower {
    public static final int SKILL = 122001014;
    private SaitamaPsychicPower() { }

    public static List<Mob> targets(Char chr) {
        var info = SkillData.getSkillInfoById(SKILL);
        var rect = chr.getRectAround(info.getFirstRect());
        return chr.getField().getMobsInRect(rect).stream().filter(m -> m.getHp() > 0)
                .sorted(Comparator.comparingLong(Mob::getHp).reversed().thenComparingInt(Mob::getObjectId))
                .limit(info.getValue(SkillStat.mobCount, 1)).toList();
    }

    public static void cast(Char chr) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                != net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                || SpecialJobProgression.requestLevel(chr, SKILL) <= 0 || chr.getField() == null) return;
        var targets = targets(chr);
        if (targets.isEmpty()) return;
        var info = SkillData.getSkillInfoById(SKILL);
        int count = info.getValue(SkillStat.bulletCount, 1);
        List<Integer> ids = new ArrayList<>();
        List<ForceAtomInfo> atoms = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(targets.get(i % targets.size()).getObjectId());
            // Reuse the ordinary targeted-force-atom trajectory. These are server launch parameters,
            // not a claim that official random launch angles have been recovered.
            ForceAtomInfo atom = new ForceAtomInfo(chr.getNewForceAtomKey(), 1, 40, 5,
                    270, 0, Util.getCurrentTime(), 1, 0, new Position());
            atom.setDisappearDelay(info.getValue(SkillStat.u, 1));
            atoms.add(atom);
        }
        chr.getField().broadcast(Tms282SaitamaPsychicPackets.create(chr.getId(), ids, atoms));
    }
}
