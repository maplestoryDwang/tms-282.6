package net.swordie.ms.client.character.items;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;

public class ReturnEffectInfo {

    public static int RETURN_QR = 5064400;
    public static int RETURN_SCROLL = 5064400;
    public Char chr;
    public Equip equip;

    public ReturnEffectInfo(Char chr, Equip equip) {
        this.chr = chr;
        this.equip = equip;
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeByte(equip != null);
        if (equip != null) {
            equip.encode(outPacket);
            outPacket.encodeInt(Integer.parseInt(chr.getQRValueByKey(RETURN_QR, "scrollID")));
        }
    }
}
