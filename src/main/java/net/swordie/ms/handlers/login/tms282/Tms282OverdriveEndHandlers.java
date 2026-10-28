package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.tms282.Tms282HiddenSkillParents;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.connection.packet.Tms282OverdriveEndRequest;
import net.swordie.ms.handlers.header.InHeader;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.ConvertAD;

/** Negative Overdrive cleanup only after the authoritative server cooldown has expired. */
final class Tms282OverdriveEndHandlers {
    void register(Tms282Routes routes) {
        routes.channel(0x0441, InHeader.TMS282_OVERDRIVE_END_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("owned negative Overdrive + expired server cooldown -> CTS reset", (s, op, p) -> {
                    var chr = s.fieldCharacter();
                    try {
                        Tms282OverdriveEndRequest.parse(p);
                        var tsm = chr.getTemporaryStatManager();
                        var negative = tsm.getOptByCTSAndSkill(ConvertAD, Job.OVERDRIVE);
                        if (Tms282HiddenSkillParents.authorisedLevel(chr, Job.OVERDRIVE) <= 0
                                || negative == null || negative.nOption >= 0 || chr.hasSkillOnCooldown(Job.OVERDRIVE))
                            throw new IllegalArgumentException("Inactive, unowned or still-cooling Overdrive penalty");
                        tsm.removeStatsBySkill(Job.OVERDRIVE);
                    } catch (IllegalArgumentException e) { Tms282SocialHandlers.rejected(op, p, e); }
                    finally { chr.dispose(); }
                });
    }
}
