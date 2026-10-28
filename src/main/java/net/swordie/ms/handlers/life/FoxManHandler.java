package net.swordie.ms.handlers.life;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.jobs.sengoku.Kanna;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.Effect;
import net.swordie.ms.connection.packet.FoxManPacket;
import net.swordie.ms.connection.packet.UserPacket;
import net.swordie.ms.connection.packet.UserRemote;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.life.FoxMan;
import net.swordie.ms.life.movement.MovementInfo;

import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.ChangeFoxMan;

public class FoxManHandler {

    @Handler(op = InHeader.FOX_MAN_MOVE)
    public static void handleAndroidMove(Char chr, InPacket inPacket) {
        FoxMan foxman = chr.getFoxMan();
        if (foxman == null) {
            return;
        }
        MovementInfo mi = new MovementInfo(inPacket);
        mi.applyTo(foxman);
        chr.getField().broadcast(FoxManPacket.move(foxman, mi), chr);
    }

    @Handler(op = InHeader.FOX_MAN_ACTION_SET_USE_REQUEST)
    public static void handleFoxManActionSetUseRequest(Char chr, InPacket inPacket) {
        if (!chr.hasSkill(Kanna.HAKU) && !chr.hasSkill(Kanna.HAKU_REBORN) && chr.getTemporaryStatManager().hasStat(ChangeFoxMan)) {
            return;
        }
        FoxMan foxMan = chr.getFoxMan();
        inPacket.decodeInt(); // tick
        int eventID = inPacket.decodeInt(); // nEventID in KMSL
        byte upgrade = inPacket.decodeByte();
        byte slv = inPacket.decodeByte();
        byte byServer = inPacket.decodeByte();
        // more packet, but seems useless
        if (foxMan.getUpgrade() != upgrade) {
            foxMan.setUpgrade(upgrade);
            chr.write(FoxManPacket.update(foxMan));
        }
        if (eventID == 1) {
            Kanna.hakuGift(chr);
        } else if (eventID == 3) {
            Kanna.hakuFoxFire(chr);
        } else if (eventID == 4) {
            Kanna.hakuHakuBlessing(chr);
        } else if (eventID == 5) {
            Kanna.hakuBreathUnseen(chr);
        }
        Effect eff = Effect.foxManActionSetUsed(eventID, upgrade, slv, byServer);
        chr.write(UserPacket.effect(eff));
        chr.getField().broadcast(UserRemote.effect(chr.getId(), eff), chr);
    }
}
