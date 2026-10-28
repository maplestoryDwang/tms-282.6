package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.enums.DimensionalPortalType;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.life.npc.NpcMessageType;
import net.swordie.ms.life.npc.NpcScriptInfo;

public class ScriptMan {

    public static OutPacket scriptMessage(NpcScriptInfo nsi, NpcMessageType nmt) {
        OutPacket outPacket = new OutPacket(OutHeader.SCRIPT_MESSAGE);

        outPacket.encodeInt(nsi.getObjectID());
        outPacket.encodeByte(nsi.getSpeakerType());
        int overrideTemplate = nsi.getOverrideSpeakerTemplateID();
        outPacket.encodeInt(overrideTemplate != 0 ? overrideTemplate : nsi.getTemplateID());
        outPacket.encodeByte(overrideTemplate > 0);
        if (overrideTemplate > 0) {
            outPacket.encodeInt(overrideTemplate);
        }
        outPacket.encodeByte(nmt.getVal());
        outPacket.encodeShort(nsi.getParam());
        outPacket.encodeByte(nsi.getColor());

        switch (nmt) {
            case Say:
            case SayOk:
            case SayNext:
            case SayPrev:
                outPacket.encodeInt(nsi.getIndex());
                if ((nsi.getParam() & 4) != 0) {
                    outPacket.encodeInt(nsi.getOverrideSpeakerTemplateID());
                }
                outPacket.encodeString(nsi.getText());
                outPacket.encodeByte(nmt.isPrevPossible());
                outPacket.encodeByte(nmt.isNextPossible());
                outPacket.encodeInt(nmt.getDelay());
                outPacket.encodeByte(0);
                break;
            case AskMenu:
            case AskYesNo:
                if ((nsi.getParam() & 4) != 0) {
                    outPacket.encodeInt(nsi.getOverrideSpeakerTemplateID());
                }
                outPacket.encodeString(nsi.getText());
                break;
            case AskAccept:
                outPacket.encodeInt(nsi.getOverrideSpeakerTemplateID());
                outPacket.encodeString(nsi.getText());
                break;
            case SayImage:
                String[] images = nsi.getImages();
                outPacket.encodeByte(images.length);
                for (String image : images) {
                    outPacket.encodeString(image);
                }
                break;
            case AskText:
                if ((nsi.getParam() & 4) != 0) {
                    outPacket.encodeInt(nsi.getOverrideSpeakerTemplateID());
                }
                outPacket.encodeString(nsi.getText());
                outPacket.encodeString(nsi.getDefaultText());
                outPacket.encodeShort(nsi.getMin());
                outPacket.encodeShort(nsi.getMax());
                break;
            case AskNumber:
                if ((nsi.getParam() & 4) != 0) {
                    outPacket.encodeInt(nsi.getOverrideSpeakerTemplateID());
                }
                outPacket.encodeString(nsi.getText());
                outPacket.encodeLong(nsi.getDefaultNumber());
                outPacket.encodeLong(nsi.getMin());
                outPacket.encodeLong(nsi.getMax());
                break;
            case InitialQuiz:
                outPacket.encodeByte(nsi.getType());
                if (nsi.getType() != 1) {
                    outPacket.encodeString(nsi.getTitle());
                    outPacket.encodeString(nsi.getProblemText());
                    outPacket.encodeString(nsi.getHintText());
                    outPacket.encodeInt(nsi.getMin());
                    outPacket.encodeInt(nsi.getMax());
                    outPacket.encodeInt(nsi.getTime()); // in seconds
                }
                break;
            case InitialSpeedQuiz:
                outPacket.encodeByte(nsi.getType());
                if (nsi.getType() != 1) {
                    outPacket.encodeInt(nsi.getQuizType());
                    outPacket.encodeInt(nsi.getAnswer());
                    outPacket.encodeInt(nsi.getCorrectAnswers());
                    outPacket.encodeInt(nsi.getRemaining());
                    outPacket.encodeInt(nsi.getTime()); // in seconds
                }
                break;
            case ICQuiz:
                outPacket.encodeByte(nsi.getType());
                if (nsi.getType() != 1) {
                    outPacket.encodeString(nsi.getText());
                    outPacket.encodeString(nsi.getHintText());
                    outPacket.encodeInt(nsi.getTime()); // in seconds
                }
                break;
            case AskAvatar:
                int[] options = nsi.getOptions();
                outPacket.encodeInt(nsi.getIndex());
                outPacket.encodeString(nsi.getText());
                outPacket.encodeByte(nsi.isAngelicBuster());
                outPacket.encodeByte(options.length);
                for (int option : options) {
                    outPacket.encodeInt(option);
                }
                outPacket.encodeInt(nsi.isZeroBeta() ? 1 : 0);
                break;
            case AskAndroid:
                outPacket.encodeInt(nsi.getIndex());
                outPacket.encodeString(nsi.getText());
                if (nsi.getAndroids() != null) {
                    outPacket.encodeString(nsi.getAndroidName());
                    outPacket.encodeInt(nsi.getAndroids().length);
                    for (int i : nsi.getAndroids()) {
                        outPacket.encodeInt(i);
                    }
                }
                break;
            case AskAvatar2:
            case AskAvatar3:
                int[] options2 = nsi.getOptions();
                outPacket.encodeByte(nsi.isAngelicBuster());
                outPacket.encodeByte(nsi.isZeroBeta());
                outPacket.encodeString(nsi.getText());
                outPacket.encodeInt(0); // v210+ hair coupon?
                outPacket.encodeInt(0); // v210+ face coupon?
                outPacket.encodeInt(0); // consumed itemId
                outPacket.encodeByte(options2.length);
                for (int option : options2) {
                    outPacket.encodeInt(option);
                }
                break;
            case AskSlideMenu:
                outPacket.encodeInt(nsi.getDlgType());
                // start CSlideMenuDlg::SetSlideMenuDlg
                outPacket.encodeInt((nsi.getDlgType() == 0) ? nsi.getDefaultSelect() : 0);
                StringBuilder sb = new StringBuilder();
                for (DimensionalPortalType dpt : DimensionalPortalType.values()) {
                    if (dpt.getMapID() != 0 && dpt.getType() == nsi.getDlgType()) {
                        sb.append("#").append(dpt.getVal()).append("#").append(dpt.getDesc());
                    }
                }
                outPacket.encodeString(sb.toString());
                break;
            case AskSelectMenu:
                outPacket.encodeInt(nsi.getDlgType());
                if (nsi.getDlgType() <= 0 || nsi.getDlgType() == 1) {
                    outPacket.encodeInt(0);
                    outPacket.encodeByte(0);
                    outPacket.encodeInt(nsi.getDefaultSelect());
                    outPacket.encodeInt(nsi.getSelectText().length);
                    for (String selectText : nsi.getSelectText()) {
                        outPacket.encodeString(selectText);
                    }
                }
                break;
            case AskStoryUI:
                outPacket.encodeString(nsi.getText()); // UI/StoryUI.img/tutoskipUI/0
                outPacket.encodeByte(nsi.getType());
                outPacket.encodeString(nsi.getHintText()); // yes
                break;
            case AskCustomMixHair:
                outPacket.encodeInt(1); // isSecond?
                outPacket.encodeByte(0);
                outPacket.encodeInt(nsi.isZeroBeta() ? 2 : 1);
                outPacket.encodeInt(nsi.isAngelicBuster() ? 1 : 0);
                outPacket.encodeString(nsi.getText());
                break;
        }

        return outPacket;
    }

}
