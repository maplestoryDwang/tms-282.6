package net.swordie.ms.handlers.ui;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.Core;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.EvolvingPacket;
import net.swordie.ms.enums.EvolvingSystemType;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.scripts.ScriptManagerImpl;

public class EvolvingSystemHandler {

    @Handler(op = InHeader.EVOLVING_REQUEST)
    public static void handleEvolvingRequest(Char chr, InPacket inPacket) {
        byte type = inPacket.decodeByte();
        EvolvingSystemType est = EvolvingSystemType.getRequestTypeByVal(type);
        if (est == null) {
            chr.chatMessage("[進化系統] 無法繼續。");
            return;
        }
        switch (est) {
            case Req_Add:
                inPacket.decodeInt(); // crc
                byte fromSlotType = inPacket.decodeByte();
                byte fromPos = inPacket.decodeByte();
                byte toSlotType = inPacket.decodeByte();
                byte toPos = inPacket.decodeByte();
                Core fromCore = chr.getCores().stream().filter(core -> core.getPos() == fromPos && core.getSlotType() == fromSlotType).findAny().orElse(null);
                if (fromCore == null) {
                    chr.write(EvolvingPacket.coreChangeSlotPositionResult(true, fromSlotType, fromPos, toSlotType, toPos));
                    chr.dispose();
                    return;
                }
                Core toCore = chr.getCores().stream().filter(core -> core.getPos() == toPos && core.getSlotType() == toSlotType).findAny().orElse(null);
                if (toCore != null) {
                    if (fromCore != toCore) {
                        chr.write(EvolvingPacket.coreChangeSlotPositionResult(true, fromSlotType, fromPos, toSlotType, toPos));
                        chr.dispose();
                        return;
                    }
                }
                switch (fromCore.getCoreID()) {
                    case 3604002:
                        if (toSlotType == 0) {
                            chr.createQuestWithQRValue(1851, "1");
                        } else {
                            chr.deleteQuest(1851);
                        }
                    case 3604003:
                        if (toSlotType == 0) {
                            chr.createQuestWithQRValue(1852, "1");
                        } else {
                            chr.deleteQuest(1852);
                        }
                    case 3604004:
                        if (toSlotType == 0) {
                            chr.createQuestWithQRValue(1853, "1");
                        } else {
                            chr.deleteQuest(1853);
                        }
                    case 3604005:
                        if (toSlotType == 0) {
                            chr.createQuestWithQRValue(1854, "1");
                        } else {
                            chr.deleteQuest(1854);
                        }
                }
                fromCore.setPos(toPos);
                fromCore.setSlotType(toSlotType);
                chr.write(EvolvingPacket.coreChangeSlotPositionResult(false, fromSlotType, fromPos, toSlotType, toPos));
                fromCore.saveToSQL();
                break;
            case Req_Sort:
                //chr.write(EvolvingPacket.coreInvenSort(chr.getCores()));
                break;
            case Req_Start:
                chr.getEvolutionSystem().start();
                break;
            case Req_CancelAccess:
                chr.getEvolutionSystem().event();
                break;
        }
        chr.dispose();
    }
}
