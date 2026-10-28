package net.swordie.ms.handlers.life;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.achievement.AchievementHandler;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.SkillPetPacket;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.SkillPet;
import net.swordie.ms.life.drop.Drop;
import net.swordie.ms.life.movement.MovementInfo;
import net.swordie.ms.util.Position;
import net.swordie.ms.world.field.Field;

public class SkillPetHandler {

    @Handler(op = InHeader.SKILL_PET_MOVE)
    public static void handleSkillPetMove(Char chr, InPacket inPacket) {
        SkillPet skillpet = chr.getSkillPet();
        int ObjectID = inPacket.decodeInt();
        inPacket.decodeByte();
        if (skillpet != null && skillpet.getObjectId() != ObjectID) {
            MovementInfo mi = new MovementInfo(inPacket);
            mi.applyTo(skillpet);
            chr.getField().broadcast(SkillPetPacket.move(chr.getId(), mi), chr);
        }
    }

    @Handler(op = InHeader.SKILL_PET_ACTION)
    public static void handleSkillPetAction(Char chr, InPacket inPacket) {
        SkillPet skillpet = chr.getSkillPet();
        int ObjectID = inPacket.decodeInt();
        if (skillpet == null) {
            return;
        }
        if (skillpet.getObjectId() != ObjectID) {
            chr.dispose();
            return;
        }
        inPacket.decodeInt(); // crc
        skillpet.setActionType(inPacket.decodeByte());
        skillpet.setAction(inPacket.decodeByte()); // v26 != 0 ? 0 : v6
        String sMsg = inPacket.decodeString();

        chr.getField().broadcast(SkillPetPacket.action(skillpet, sMsg));
    }

    @Handler(op = InHeader.SKILL_PET_DROP_PICK_UP_REQUEST)
    public static void handleSkillPetDropPickUpRequest(Char chr, InPacket inPacket) {
        Field field = chr.getField();
        int ObjectID = inPacket.decodeInt();
        byte fieldKey = inPacket.decodeByte();
        inPacket.decodeInt(); // update_time
        Position pos = inPacket.decodePosition();
        int dropID = inPacket.decodeInt();
        inPacket.decodeInt(); // 0
        Life life = field.getLifeByObjectID(dropID);
        if (life instanceof Drop drop) {
            boolean success = drop.canBePickedUpByPet() && drop.canBePickedUpBy(chr) && chr.addDrop(drop, true);
            if (success) {
                AchievementHandler.handleLoot(chr, drop);
                field.removeDrop(dropID, chr.getId(), false, ObjectID);
            } else {
                chr.dispose();
            }
        }
    }
}
