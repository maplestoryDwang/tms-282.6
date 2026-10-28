package net.swordie.ms.handlers.login.tms282;

import java.nio.charset.Charset;
import net.swordie.ms.client.jobs.resistance.Xenon;
import net.swordie.ms.connection.packet.Tms282MultiSkillModeRequest;
import net.swordie.ms.handlers.header.InHeader;

/** Persist the selected native mode under its own parent skill; selection does not cast/cancel a skill. */
final class Tms282MultiSkillModeHandlers {
    private final Charset charset;
    private final Tms282FieldStore store;
    Tms282MultiSkillModeHandlers(Tms282LoginContext context) {
        charset = context.settings().nameCharset(); store = context.fieldServices().store();
    }
    void register(Tms282Routes routes) {
        routes.channel(Tms282MultiSkillModeRequest.OPCODE,InHeader.CP03BE).when(Tms282Guards.IN_FIELD)
                .handle("Xenon multi-skill selection -> saved QRex21770 + LP00AE13",(s,op,p) -> {
                    var chr = s.fieldCharacter();
                    try {
                        var r = Tms282MultiSkillModeRequest.parse(p);
                        if (!(chr.getJobHandler() instanceof Xenon xenon)) throw new IllegalArgumentException("Non-Xenon mode");
                        var quest = xenon.selectTms282SkillMode(r.skillId());
                        try { store.saveQuestState(chr,quest); }
                        catch (RuntimeException e) {
                            System.out.printf("[TMS282] CP03BE character=%d mode persistence failed: %s; retained for character save%n",chr.getId(),e);
                        }
                        chr.write(Tms282MultiSkillModeRequest.reply(quest,charset));
                    } catch (IllegalArgumentException e) { Tms282SocialHandlers.rejected(op,p,e); }
                });
    }
}
