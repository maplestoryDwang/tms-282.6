package net.swordie.ms.handlers.life;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.world.field.Field;

public class AffectedAreaHandler {

    @Handler(op = InHeader.USER_AFFECTED_AREA_REMOVE_BY_TIME)
    public static void handleUserAffectedAreaRemoveByTime(Char chr, InPacket inPacket) {
        int skillID = inPacket.decodeInt();

        Field field = chr.getField();
        field.getAffectedAreas().stream().filter(aa -> aa.getCharID() == chr.getId() && aa.getSkillID() == skillID).findFirst().ifPresent(field::removeLife);
    }
}
