package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;

/** Original282 LP0589:58D2F20 reads one skill ID and invokes the native local skill dispatcher.
 * Restricted to the two weapon bursts whose parent cancellation is server-authorized. */
public final class Tms282SpecialWeaponPackets {
    private Tms282SpecialWeaponPackets() { }
    public static OutPacket requestBurst(int child) {
        if (child != 80002634 && child != 80003875) throw new IllegalArgumentException("Unsupported weapon burst");
        OutPacket out = Tms282LoginPacketSupport.packet(0x589);
        out.encodeInt(child);
        return out;
    }
}
