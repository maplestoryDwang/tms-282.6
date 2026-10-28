package net.swordie.ms.handlers.ui;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.EventNameTag;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.UserPacket;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.enums.EventNameTagType;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;

public class EventNameTagHandler {

    @Handler(op = InHeader.SET_EVENT_NAME_TAG)
    public static void handleSetEventNameTag(Char chr, InPacket inPacket) {
        //nAction = 1 Disable
        //nAction = 2 Active
        byte nCategory = inPacket.decodeByte();
        byte nIdx = inPacket.decodeByte();
        byte nAction = inPacket.decodeByte();
        //Get EventNameTag Data
        EventNameTag eventNameTag = chr.getEventNameTag();
        //Get Type (like Blue, Red,..)
        EventNameTagType eventNameTagType = EventNameTagType.getNameTagTypeByVal(nCategory);
        if (eventNameTag != null) {
            String[] arrayEventNameTag = eventNameTag.getsNameTags();
            //Get current String of tag. Default: "0000000000"
            StringBuilder sbEventNameTag = new StringBuilder(arrayEventNameTag[nCategory]);
            switch (nAction) {
                //To Lazy for enum class.
                case 1: //Disable
                    //Set Current Type to -1.
                    eventNameTag = setActiveNameTagByType(eventNameTag, eventNameTagType, (byte) -1);
                    //Set Current Idx to 1.
                    sbEventNameTag.setCharAt(nIdx, '1');
                    break;
                case 2: //Active
                    //Set Current Active At Slot
                    eventNameTag = setActiveNameTagByType(eventNameTag, eventNameTagType, nIdx);
                    //Get Category of NameTag and get any active if not return -1.
                    int currentActiveIdx = arrayEventNameTag[nCategory].indexOf('2');
                    if (currentActiveIdx != -1) {
                        //Turn off current Idx.
                        sbEventNameTag.setCharAt(currentActiveIdx, '1');
                    }
                    //Turn on current Idx
                    sbEventNameTag.setCharAt(nIdx, '2');
                    chr.getField().broadcast(UserPacket.categoryEventNameTag(chr.getId(), nIdx, sbEventNameTag.toString(), nCategory));
                    break;
            }
            eventNameTag = setStringNameTagByType(eventNameTag, eventNameTagType, sbEventNameTag.toString());
            chr.setEventNameTags(eventNameTag);
            chr.write(WvsContext.updateEventNameTag(chr, eventNameTag.getActiveNameTags()));
            chr.getField().broadcast(UserPacket.categoryEventNameTag(chr.getId(), nAction == 1 ? -1 : nIdx, sbEventNameTag.toString(), nCategory));
        }
    }

    public static EventNameTag setActiveNameTagByType(EventNameTag eventNameTag, EventNameTagType type, byte nIdx) {
        switch (type) {
            case RED:
                eventNameTag.setActiveRed(nIdx);
                break;
            case BLUE:
                eventNameTag.setActiveBlue(nIdx);
                break;
            case YELLOW:
                eventNameTag.setActiveYellow(nIdx);
                break;
            case GREEN:
                eventNameTag.setActiveGreen(nIdx);
                break;
            case PURPLE:
                eventNameTag.setActivePurple(nIdx);
                break;
        }
        return eventNameTag;
    }

    public static String getStringNameTagByType(EventNameTag eventNameTag, EventNameTagType type) {
        switch (type) {
            case RED:
                return eventNameTag.getsRed();
            case BLUE:
                return eventNameTag.getsBlue();
            case YELLOW:
                return eventNameTag.getsYellow();
            case GREEN:
                return eventNameTag.getsGreen();
            case PURPLE:
                return eventNameTag.getsPurple();
            default:
                return "";
        }
    }

    public static EventNameTag setStringNameTagByType(EventNameTag eventNameTag, EventNameTagType type, String string) {
        switch (type) {
            case RED:
                eventNameTag.setsRed(string);
                break;
            case BLUE:
                eventNameTag.setsBlue(string);
                break;
            case YELLOW:
                eventNameTag.setsYellow(string);
                break;
            case GREEN:
                eventNameTag.setsGreen(string);
                break;
            case PURPLE:
                eventNameTag.setsPurple(string);
                break;
        }
        return eventNameTag;
    }
}
