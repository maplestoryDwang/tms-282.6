package net.swordie.ms.handlers.login.tms282;

import java.nio.charset.Charset;
import java.util.Arrays;
import net.swordie.ms.client.character.tms282.*;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.handlers.header.InHeader;

final class Tms282SkillRingHandlers {
    private final Charset charset;
    Tms282SkillRingHandlers(Charset charset) { this.charset=charset; }
    void register(Tms282Routes routes) {
        routes.channel(Tms282SkillRingRequest.OPCODE,InHeader.CP0498).when(Tms282Guards.IN_FIELD)
                .handle("native skill ring grinding",this::upgrade);
        // equips README WI-7: continuous ring trigger -> 80003342 buff (Tms282OzRingEffects.continuous)
        routes.channel(Tms282ContinuousRingRequest.OPCODE,InHeader.CP03F8).when(Tms282Guards.IN_FIELD)
                .handle("continuous ring trigger",Tms282SkillRingHandlers::continuous);
    }
    /** CP03F8: a malformed / forged request or an unprepared ring is ignored (the client shows nothing either way). */
    static void continuous(Tms282LoginState session,int opcode,byte[] packet) {
        var chr=session.fieldCharacter();
        Tms282ContinuousRingRequest request;
        try {
            request=Tms282ContinuousRingRequest.parse(packet);
        } catch(IllegalArgumentException e) {
            System.out.printf("[TMS282] CP03F8 rejected character=%d: %s%n",chr.getId(),e.getMessage());
            return;
        }
        boolean applied=net.swordie.ms.client.character.items.Tms282OzRingEffects.continuous(chr,
                request.triggeringSkill(),request.sourceLevel(),System.currentTimeMillis());
        System.out.printf("[TMS282] CP03F8 continuous ring character=%d trigger=%d level=%d applied=%s%n",
                chr.getId(),request.triggeringSkill(),request.sourceLevel(),applied);
    }
    private void upgrade(Tms282LoginState session,int opcode,byte[] packet) throws Exception {
        var chr=session.fieldCharacter();
        try {
            var request=Tms282SkillRingRequest.parse(Arrays.copyOfRange(packet,2,packet.length));
            var inventory=Tms282InventoryWatch.of(chr);
            var stats=Tms282StatDiff.of(chr);
            boolean success=Tms282SkillRingRuntime.upgrade(chr,request);
            session.send(Tms282InventoryPackets.inventoryOperation(false,inventory.changes(),charset));
            session.send(Tms282StatPackets.statChanged(false,stats.changes()));
            session.send(Tms282SkillRingPackets.result(success));
        } catch(IllegalArgumentException e) {
            chr.chatMessage(e.getMessage());
            session.send(Tms282SkillRingPackets.rejected());
            session.send(Tms282StatPackets.unlock());
        }
        // SQL/commit failures propagate: disconnect and preserve the pending recovery record.
    }
}
