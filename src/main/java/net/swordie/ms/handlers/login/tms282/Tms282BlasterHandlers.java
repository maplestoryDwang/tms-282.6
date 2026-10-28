package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.tms282.Tms282HiddenSkillParents;
import net.swordie.ms.client.jobs.resistance.Blaster;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282BlasterRequests;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.user.JobSkillHandler;

/** Native Blaster status callbacks; never a second attack or a guessed bind reply. */
final class Tms282BlasterHandlers {
    void register(Tms282Routes routes) {
        routes.channel(0x03C3, InHeader.RW_CLEAR_CURRENT_ATTACK_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("Blaster Revolving Blast release -> own Freeze/CTS reset", (s, op, p) -> guarded(s, op, p, () -> {
                    var r = Tms282BlasterRequests.ClearCurrentAttack.parse(p);
                    var chr = s.fieldCharacter();
                    if (!(chr.getJobHandler() instanceof Blaster blaster)
                            || Tms282HiddenSkillParents.authorisedLevel(chr, r.skillId()) <= 0)
                        throw new IllegalArgumentException("Unowned Blaster attack release");
                    blaster.releaseRevolvingBlast();
                }));
        routes.channel(0x03C4, InHeader.RW_MULTI_CHARGE_CANCEL_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("Blaster charged skill cancellation -> LP04AE", (s, op, p) -> guarded(s, op, p, () -> {
                    var r = Tms282BlasterRequests.MultiChargeCancel.parse(p);
                    var chr = s.fieldCharacter();
                    if (!(chr.getJobHandler() instanceof Blaster)) throw new IllegalArgumentException("Non-Blaster cancel");
                    for (int skill : r.skillIds()) {
                        if (Tms282HiddenSkillParents.authorisedLevel(chr, skill) <= 0)
                            throw new IllegalArgumentException("Unowned Blaster cancel skill");
                    }
                    OutPacket body = new OutPacket(); body.encodeByte(r.skillIds().size());
                    for (int skill : r.skillIds()) body.encodeInt(skill);
                    JobSkillHandler.handleRWMultiChargeCancelRequest(chr, new InPacket(body.getData()));
                }));
    }

    private static void guarded(Tms282LoginState s, int op, byte[] p, Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException e) { Tms282SocialHandlers.rejected(op, p, e); }
        finally { s.fieldCharacter().dispose(); }
    }
}
