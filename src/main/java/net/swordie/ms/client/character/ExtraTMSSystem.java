package net.swordie.ms.client.character;

import net.swordie.ms.client.jobs.Jianghu.Lynn;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.handlers.header.OutHeader;

public class ExtraTMSSystem {

    public Char chr;
    public int magicNumber;

    public static int unkNum = -2107976430;
    public static int unkNum2 = 608170833;
    public static int unkNum3 = -1208526693;

    public static int earthPulverizationNum1 = 892195225;
    public static int earthPulverizationNum2 = 1106562254;
    public static int earthPulverizationNum3 = -734084527;
    public static int earthPulverizationNum4 = -1330707179;

    public ExtraTMSSystem(Char chr, int magicNumber) {
        this.chr = chr;
        this.magicNumber = magicNumber;
    }

    public void encodeBuffer(OutPacket outPacket) {
        OutPacket buffer = new OutPacket();
        if (chr.getLevel() >= 200 || JobConstants.isLynn(chr.getJob())) {
            buffer.encodeInt(magicNumber);
            buffer.encodeInt(unkNum);
            buffer.encodeByte(1);

            buffer.encodeShort(15);
            buffer.encodeInt(chr.getUser().getId());
            buffer.encodeInt(chr.getId());

            buffer.encodeShort(16);
            buffer.encodeByte(0);

            buffer.encodeShort(17);
            buffer.encodeByte(1);

            buffer.encodeShort(18);
            buffer.encodeInt(0);

            if (JobConstants.isLynn(chr.getJob())) {
                buffer.encodeInt(magicNumber + 1);
                buffer.encodeInt(unkNum2);
                buffer.encodeByte(1);

                buffer.encodeShort(15);
                buffer.encodeInt(chr.getUser().getId());
                buffer.encodeInt(chr.getId());

                buffer.encodeShort(16);
                buffer.encodeByte(0);

                buffer.encodeShort(17);
                buffer.encodeByte(1);

                buffer.encodeShort(18);
                buffer.encodeByte(0);

                buffer.encodeShort(19);
                buffer.encodeInt(0);

                buffer.encodeShort(20);
                buffer.encodeByte(1);

                buffer.encodeShort(21);
                buffer.encodeShort(113);

                buffer.encodeShort(22);
                buffer.encodeInt(0);

                buffer.encodeShort(23);
                buffer.encodeInt(0);

                buffer.encodeShort(24);
                buffer.encodeInt(0);

                buffer.encodeShort(25);
                buffer.encodeLong(0);

                buffer.encodeShort(26);
                buffer.encodeInt(0);

                buffer.encodeShort(27);
                buffer.encodeInt(0);

                buffer.encodeShort(28);
                buffer.encodeLong(0);

                buffer.encodeShort(29);
                buffer.encodeLong(0);

                buffer.encodeShort(30);
                buffer.encodeLong(0);

                buffer.encodeShort(31);
                buffer.encodeInt(0);

                buffer.encodeShort(32);
                buffer.encodeInt(0);

                buffer.encodeShort(33);
                buffer.encodeInt(0);

                buffer.encodeShort(34);
                buffer.encodeLong(0);

                buffer.encodeByte(0);
                buffer.encodeByte(0);
                buffer.encodeByte(0);
            } else {
                buffer.encodeInt(0);
            }
        } else {
            buffer.encodeInt(0);
        }
        outPacket.encodeInt(buffer.getLength());
        outPacket.encodeArr(buffer.getData());
    }

    public void encodeEnterField(OutPacket outPacket) {
        if (JobConstants.isLynn(chr.getJob())) {
            outPacket.encodeInt(155);
            outPacket.encodeInt(unkNum2);
            outPacket.encodeByte(1);
            outPacket.encodeShort(15);
            outPacket.encodeInt(chr.getUser().getId());
            outPacket.encodeInt(chr.getId());
            outPacket.encodeShort(16);
            outPacket.encodeByte(0);
            outPacket.encodeShort(17);
            outPacket.encodeByte(1);
            outPacket.encodeShort(18);
            outPacket.encodeByte(0);
            outPacket.encodeShort(19);
            outPacket.encodeInt(0);
            outPacket.encodeShort(20);
            outPacket.encodeByte(1);
            outPacket.encodeShort(21);
            outPacket.encodeShort(113);
            outPacket.encodeShort(22);
            outPacket.encodeInt(0);
            outPacket.encodeShort(23);
            outPacket.encodeInt(0);
            outPacket.encodeShort(24);
            outPacket.encodeInt(0);
            outPacket.encodeShort(25);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeShort(26);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeByte(0);
            outPacket.encodeByte(0);
            outPacket.encodeByte(0);

        } else {
            outPacket.encodeInt(0);
        }
    }

    public static OutPacket initField(boolean isField, Char chr, int num) {
        OutPacket outPacket = new OutPacket(isField ? OutHeader.LYNN_RESULT_FIELD : OutHeader.LYNN_RESULT);

        if (isField) {
            outPacket.encodeByte(7);
            outPacket.encodeInt(num);
            outPacket.encodeInt(unkNum3);
            outPacket.encodeByte(1);
            outPacket.encodeShort(7);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeShort(8);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeShort(9);
            outPacket.encodeInt(0);
            outPacket.encodeShort(10);
            outPacket.encodeInt(0);
            outPacket.encodeShort(11);
            outPacket.encodeInt(0);
            outPacket.encodeShort(12);
            outPacket.encodeInt(0);
            outPacket.encodeShort(13);
            outPacket.encodeByte(0);
            outPacket.encodeShort(14);
            outPacket.encodeByte(0);
            outPacket.encodeShort(15);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeShort(16);
            outPacket.encodeByte(0);
            outPacket.encodeShort(17);
            outPacket.encodeByte(1);
            outPacket.encodeShort(18);
            outPacket.encodeByte(0);
            outPacket.encodeShort(19);
            outPacket.encodeByte(1);
            outPacket.encodeShort(20);
            outPacket.encodeInt(0);
        } else {
            outPacket.encodeInt(chr.getId());
            outPacket.encodeByte(7);
            outPacket.encodeInt(num);
            outPacket.encodeInt(unkNum);
            outPacket.encodeByte(1);
            outPacket.encodeShort(15);
            outPacket.encodeInt(chr.getUser().getId());
            outPacket.encodeInt(chr.getId());
            if (JobConstants.isLynn(chr.getJob())) {
                outPacket.encodeByte(0);
                outPacket.encodeByte(0);
                outPacket.encodeByte(0);
                outPacket.encodeInt(num + 1);
                outPacket.encodeInt(unkNum2);
                outPacket.encodeByte(1);
                outPacket.encodeShort(15);
                outPacket.encodeInt(chr.getUser().getId());
                outPacket.encodeInt(chr.getId());
                outPacket.encodeShort(19);
                outPacket.encodeInt(0);
            }
        }
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket focusHeal1(Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.LYNN_RESULT);

        outPacket.encodeInt(chr.getId());
        outPacket.encodeByte(7);
        outPacket.encodeInt(112);
        outPacket.encodeInt(unkNum2);
        outPacket.encodeByte(1);
        outPacket.encodeShort(34);
        outPacket.encodeShort(1);
        outPacket.encodeShort(1);
        outPacket.encodeInt(Lynn.FOCUS_HEAL);
        outPacket.encodeShort(2);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeShort(3);
        outPacket.encodeShort(0);
        outPacket.encodeInt(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket focusHeal2(Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.LYNN_RESULT);

        outPacket.encodeInt(chr.getId());
        outPacket.encodeByte(7);
        outPacket.encodeInt(112);
        outPacket.encodeInt(unkNum2);
        outPacket.encodeByte(1);
        outPacket.encodeShort(34);
        outPacket.encodeShort(1);
        outPacket.encodeShort(2);
        outPacket.encodePositionInt(chr.getPosition());
        outPacket.encodeShort(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket peck(Char chr, int mode) {
        OutPacket outPacket = new OutPacket(OutHeader.LYNN_RESULT);

        outPacket.encodeInt(chr.getId());
        outPacket.encodeByte(7);
        outPacket.encodeInt(112);
        outPacket.encodeInt(ExtraTMSSystem.unkNum2);
        outPacket.encodeByte(1);
        outPacket.encodeShort(21);
        outPacket.encodeShort(113);
        outPacket.encodeShort(22);
        outPacket.encodeInt(mode);
        outPacket.encodeShort(23);
        outPacket.encodeInt(0);
        outPacket.encodeShort(24);
        outPacket.encodeInt(mode != 0 ? 1 : 0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket purify(Char chr, int mode) {
        OutPacket outPacket = new OutPacket(OutHeader.LYNN_RESULT);

        outPacket.encodeInt(chr.getId());
        outPacket.encodeByte(7);
        outPacket.encodeInt(112);
        outPacket.encodeInt(ExtraTMSSystem.unkNum2);
        outPacket.encodeByte(1);
        outPacket.encodeShort(21);
        outPacket.encodeShort(113);
        outPacket.encodeShort(22);
        outPacket.encodeInt(mode);
        outPacket.encodeShort(23);
        outPacket.encodeInt(0);
        outPacket.encodeShort(24);
        outPacket.encodeInt(mode != 0 ? 1 : 0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket earthPulverization(boolean isField, Char chr, int mobOID) {
        OutPacket outPacket = new OutPacket(isField ? OutHeader.LYNN_RESULT_FIELD : OutHeader.LYNN_RESULT);

        if (isField) {
            outPacket.encodeByte(7);
            outPacket.encodeInt(113);
            outPacket.encodeInt(earthPulverizationNum1);
            outPacket.encodeByte(1);

            outPacket.encodeShort(1);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);

            outPacket.encodeShort(2);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);

            outPacket.encodeShort(3);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);

            outPacket.encodeShort(4);
            outPacket.encodeInt(0);
            outPacket.encodeArr("00 00 80 3F"); // TODO

            outPacket.encodeShort(5);
            outPacket.encodeShort(2);

            outPacket.encodeShort(6);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);

            outPacket.encodeShort(7);
            outPacket.encodeArr("00 C0 07 44 00 00 44 43"); // TODO

            outPacket.encodeShort(8);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);

            outPacket.encodeShort(9);
            outPacket.encodeInt(0);

            outPacket.encodeShort(10);
            outPacket.encodeInt(0);

            outPacket.encodeShort(11);
            outPacket.encodeInt(0);

            outPacket.encodeShort(12);
            outPacket.encodeInt(0);

            outPacket.encodeShort(13);
            outPacket.encodeByte(0);

            outPacket.encodeShort(14);
            outPacket.encodeByte(0);

            outPacket.encodeShort(15);
            outPacket.encodeInt(chr.getUser().getId());
            outPacket.encodeInt(chr.getId());

            outPacket.encodeShort(16);
            outPacket.encodeByte(0);

            outPacket.encodeShort(17);
            outPacket.encodeByte(1);

            outPacket.encodeShort(18);
            outPacket.encodeByte(16); // TODO

            outPacket.encodeShort(19);
            outPacket.encodeInt(172111002); // Earth Pulverization

            outPacket.encodeShort(20);
            outPacket.encodeInt(15);

            outPacket.encodeShort(21);
            outPacket.encodeInt(6);

            outPacket.encodeShort(22);
            outPacket.encodeArr("00 00 80 3F"); // TODO

            outPacket.encodeShort(23);
            outPacket.encodeInt(0);

            outPacket.encodeShort(24);
            outPacket.encodeArr("CE CC F4 41"); // TODO

            outPacket.encodeShort(25);
            outPacket.encodeByte(0);

            outPacket.encodeShort(26);
            outPacket.encodeByte(1);

            outPacket.encodeShort(27);
            outPacket.encodeByte(0);

            outPacket.encodeShort(28);
            outPacket.encodeByte(0);

            outPacket.encodeShort(28);
            outPacket.encodeInt(30);

            outPacket.encodeShort(29);
            outPacket.encodeArr("51 C2 3E D4"); // TODO
            outPacket.encodeArr("15 05 AF B0"); // TODO
        } else {
            outPacket.encodeInt(chr.getId());
            outPacket.encodeByte(7);
            outPacket.encodeInt(112);
            outPacket.encodeInt(unkNum2);
            outPacket.encodeByte(1);
            outPacket.encodeShort(23);
            outPacket.encodeInt(mobOID);
        }
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);

        return outPacket;
    }
}
