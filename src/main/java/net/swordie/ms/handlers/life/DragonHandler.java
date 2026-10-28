package net.swordie.ms.handlers.life;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.DragonPool;
import net.swordie.ms.connection.packet.UserRemote;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.life.Dragon;
import net.swordie.ms.life.movement.MovementInfo;

public class DragonHandler {

    @Handler(op = InHeader.DRAGON_MOVE)
    public static void handleDragonMove(Char chr, InPacket inPacket) {
        if (chr == null) return;

        Dragon dragon = chr.getDragon();
        if (dragon != null && dragon.getOwnerId() == chr.getId()) {
            MovementInfo movementInfo = new MovementInfo(inPacket);
            movementInfo.applyTo(dragon);
            chr.getField().broadcast(DragonPool.moveDragon(dragon, movementInfo), chr);
        }
    }

    @Handler(op = InHeader.DRAGON_GLIDE)
    public static void handleDragonGlide(Char chr, InPacket inPacket) {
        if (chr == null) return;

        Dragon dragon = chr.getDragon();
        if (dragon != null && dragon.getOwnerId() == chr.getId()) {
            int EvanDragonGlide_Riding = inPacket.decodeInt();
            chr.setEvanDragonGlide(EvanDragonGlide_Riding);
            chr.getField().broadcast(UserRemote.dragonGlide(chr, EvanDragonGlide_Riding));
        }
    }
}
