package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.tms282.Tms282HiddenSkillParents;
import net.swordie.ms.connection.packet.Tms282VisualSkillRequests;
import net.swordie.ms.connection.packet.UserRemote;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.life.AffectedArea;

/** Native movement visuals plus owned affected-area expiry; all gameplay authority stays on the server. */
final class Tms282VisualSkillHandlers {
    void register(Tms282Routes routes) {
        routes.channel(0x0134, InHeader.USER_EFFECT_LOCAL).when(Tms282Guards.IN_FIELD)
                .handle("native rope/blink visuals -> LP048E and requested LP04CA", (s,op,p) -> guard(s,op,p, () -> {
                    var request = Tms282VisualSkillRequests.MovementEffect.parse(p);
                    var chr = s.fieldCharacter();
                    int level = Tms282HiddenSkillParents.authorisedLevel(chr, request.skillId());
                    if (level <= 0) throw new IllegalArgumentException("Unowned movement effect");
                    if (request.local()) chr.write(request.effect(chr.getId(),chr.getLevel(),level,false));
                    chr.getField().broadcast(request.effect(chr.getId(),chr.getLevel(),level,true),chr);
                }));
        routes.channel(0x0271, InHeader.USER_AFFECTED_AREA_REMOVE_BY_TIME).when(Tms282Guards.IN_FIELD)
                .handle("owned affected-area end -> existing removal and LP0754", (s,op,p) -> guard(s,op,p, () -> {
                    var request = Tms282VisualSkillRequests.AreaEnd.parse(p);
                    var chr = s.fieldCharacter(); var field = chr.getField();
                    AffectedArea own = field.getAffectedAreas().stream()
                            .filter(a -> a.getOwner()==chr && a.getField()==field && a.getMobOrigin()==0
                                    && a.getSkillID()==request.skillId()).findFirst().orElse(null);
                    if (own != null) field.removeLife(own);
                }));
        routes.channel(0x0114, InHeader.USER_MOVING_SHOOT_ATTACK_PREPARE).when(Tms282Guards.IN_FIELD)
                .handle("normal/owned moving-shoot pose -> native LP047D", (s,op,p) -> guard(s,op,p, () -> {
                    var request = Tms282VisualSkillRequests.MovingPrepare.parse(p);
                    var chr = s.fieldCharacter();
                    if (chr.getHP()<=0) return;
                    int level = request.skillId()==0 ? 0 : Tms282HiddenSkillParents.authorisedLevel(chr,request.skillId());
                    if (request.skillId()!=0 && level<=0) throw new IllegalArgumentException("Unowned moving pose");
                    chr.getField().broadcast(UserRemote.movingShootAttackPrepare(chr,request.skillId(),level,request.mask(),(byte)request.speed()),chr);
                }));
    }

    private static void guard(Tms282LoginState s, int op, byte[] packet, Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException e) { Tms282SocialHandlers.rejected(op,packet,e); }
        catch (RuntimeException | LinkageError e) {
            System.out.printf("[TMS282] FAILED CP%04X character=%d: %s%n",op,s.fieldCharacter().getId(),e);
        }
    }
}
