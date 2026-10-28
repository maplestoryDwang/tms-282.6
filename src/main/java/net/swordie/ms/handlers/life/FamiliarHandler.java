package net.swordie.ms.handlers.life;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.constants.FieldConstants;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.life.Familiar;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.movement.MovementInfo;
import net.swordie.ms.loaders.Etc.EtcData;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;

import java.util.Arrays;

public class FamiliarHandler {

    @Handler(op = InHeader.FAMILIAR_ADD_REQUEST)
    public static void handleFamiliarAddRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        short slot = inPacket.decodeShort();
        int itemID = inPacket.decodeInt();
        if (true) {
            return;
        }
        Item item = chr.getConsumeInventory().getItemBySlot(slot);
        if (item == null || item.getItemId() != itemID || !ItemConstants.isFamiliar(itemID)) {
            DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("Character %d tried to add a familiar it doesn't have. (item id %d)", chr.getId(), itemID));
            return;
        }
        int suffix = itemID % 10000;
        int familiarID = (ItemConstants.FAMILIAR_PREFIX * 10000) + suffix;
        Familiar familiar = chr.getFamiliarByID(familiarID);
        boolean showInfo = true;
        if (familiar == null) {
            familiar = new Familiar(chr.getId(), familiarID, "Familiar", FileTime.MAX_TIME(), (short) 1);
            showInfo = false;
            chr.addFamiliar(familiar);
        } else {
            familiar.setVitality((short) Math.min(familiar.getVitality() + 1, 3));
        }
        chr.consumeItem(itemID, 1);
        familiar.saveToSQL();
        chr.write(UserLocal.familiarAddResult(familiar, showInfo, false));
    }

    @Handler(op = InHeader.FAMILIAR_SPAWN_REQUEST)
    public static void handleFamiliarSpawnRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        int familiarID = inPacket.decodeInt();
        boolean on = inPacket.decodeByte() != 0;
        if (Arrays.stream(FieldConstants.BLOCKED_RUNE_MAPS).anyMatch(m -> chr.getField() != null && m == chr.getField().getId())) {
            chr.dispose();
            return;
        }
        Familiar familiar = chr.getFamiliarByID(familiarID);
        if (familiar != null) {
            Familiar activeFam = chr.getActiveFamiliar();
            if (activeFam != null && activeFam != familiar) {
                // deactivate old familiar
                if (activeFam.getSkillID() != 0) {
                    chr.getTemporaryStatManager().removeStatsBySkill(-activeFam.getSkillID());
                    activeFam.setSkillID(0);
                }
                //chr.getField().broadcastPacket(CFamiliar.familiarEnterField(chr.getId(), false, chr.getActiveFamiliar(), false, true));
            }
            chr.setActiveFamiliar(on ? familiar : null);
            if (on) {
                familiar.setPosition(chr.getPosition().deepCopy());
                familiar.setFh(chr.getFoothold());
            } else if (familiar.getSkillID() != 0) {
                chr.getTemporaryStatManager().removeStatsBySkill(-familiar.getSkillID());
                familiar.setSkillID(0);
            }
            //chr.getField().broadcastPacket(CFamiliar.familiarEnterField(chr.getId(), false, familiar, on, true));
        }
        chr.dispose();
    }

    @Handler(op = InHeader.FAMILIAR_RENAME_REQUEST)
    public static void handleFamiliarRenameRequest(Char chr, InPacket inPacket) {
        int familiarID = inPacket.decodeInt();
        String name = inPacket.decodeString();
        if (name.length() > 13) {
            chr.chatMessage("萌獸名稱不能超過 13 個字元。");
            chr.dispose();
            return;
        }
        if (name.matches(".*\\s.*")) {
            chr.chatMessage("萌獸名稱無效。");
            chr.dispose();
            return;
        }
        Familiar familiar = chr.getFamiliarByID(familiarID);
        if (familiar != null) {
            familiar.setName(name);
            familiar.saveToSQL();
        }
        chr.dispose();
    }

    @Handler(op = InHeader.FAMILIAR_MOVE)
    public static void handleFamiliarMove(Char chr, InPacket inPacket) {
        inPacket.decodeByte(); // ?
        inPacket.decodeInt(); // familiar id
        Life life = chr.getActiveFamiliar();
        if (life instanceof Familiar) {
            MovementInfo movementInfo = new MovementInfo(inPacket);
            movementInfo.applyTo(life);
            //chr.getField().broadcastPacket(CFamiliar.familiarMove(chr.getId(), movementInfo), chr);
        }
    }


    @Handler(op = InHeader.FAMILIAR_SKILL)
    public static void handleFamiliarSkill(Char chr, InPacket inPacket) {
        inPacket.decodeByte();
        int familiarID = inPacket.decodeInt();
        Familiar activeFamiliar = chr.getActiveFamiliar();
        if (activeFamiliar == null || activeFamiliar.getFamiliarID() != familiarID) {
            return;
        }
        if (Arrays.stream(FieldConstants.BLOCKED_RUNE_MAPS).anyMatch(m -> chr.getField() != null && m == chr.getField().getId())) {
            chr.dispose();
            return;
        }
        if (activeFamiliar.getSkillID() != 0) {
            chr.getTemporaryStatManager().removeStatsBySkill(-activeFamiliar.getSkillID());
        }
        int skillID = EtcData.getSkillByFamiliarID(familiarID);
        Item item = ItemData.getItemDeepCopy(skillID);
        chr.useStatChangeItem(item, false);
        chr.getActiveFamiliar().setSkillID(skillID);
    }
}
