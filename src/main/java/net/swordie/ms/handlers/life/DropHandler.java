package net.swordie.ms.handlers.life;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.achievement.AchievementHandler;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.DropPool;
import net.swordie.ms.enums.FieldOption;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.drop.Drop;
import net.swordie.ms.util.Position;
import net.swordie.ms.world.field.Field;

public class DropHandler {

    @Handler(op = InHeader.DROP_PICK_UP_REQUEST)
    public static void handleDropPickUpRequest(Char chr, InPacket inPacket) {
        byte fieldKey = inPacket.decodeByte();
        inPacket.decodeInt(); // tick
        inPacket.decodeInt(); // 0
        Position pos = inPacket.decodePosition();
        int dropID = inPacket.decodeInt();
        inPacket.decodeInt(); // CliCrc
        // rest is some info about foreground info, not interested
        Field field = chr.getField();
        Life life = field.getLifeByObjectID(dropID);
        if (life == null) {
            field.broadcast(DropPool.dropLeaveField(dropID, chr.getId()));
            return;
        }
        if (life instanceof Drop drop) {
            boolean success = (drop.getOwnerID() == chr.getId() || drop.canBePickedUpBy(chr)) && chr.addDrop(drop, false);
            if (success) {
                field.removeDrop(dropID, chr.getId(), false, -1);
                chr.handleDropForPQs(drop);
                AchievementHandler.handleLoot(chr, drop);
            } else {
                chr.dispose();
            }
        }

    }

    @Handler(op = InHeader.USER_DROP_MONEY_REQUEST)
    public static void handleUserDropMoneyRequest(Char chr, InPacket inPacket) {
        if (!net.swordie.ms.GameSettings.mesoDropEnabled()) { // feature.meso.drop
            chr.chatMessage(net.swordie.ms.GameSettings.MESO_DROP_DISABLED);
            chr.dispose();
            return;
        }
        Field field = chr.getField();
        if ((field.getFieldLimit() & FieldOption.DropLimit.getVal()) > 0) {
            chr.dispose();
            return;
        }

        if (field.getDropsDisabled()) {
            chr.chatMessage("當前地圖不允許執行該操作。");
            chr.dispose();
            return;
        }

        inPacket.decodeInt(); // tick
        int amount = inPacket.decodeInt();
        if (amount < 0) {
            chr.dispose();
            return;
        }
        if (chr.getMoney() > amount) {
            chr.deductMoney(amount);
            Drop drop = new Drop(-1, amount);
            drop.setCanBePickedUpByPet(false);
            chr.getField().drop(drop, chr.getPosition());
            chr.dispose();
        }
    }
}
