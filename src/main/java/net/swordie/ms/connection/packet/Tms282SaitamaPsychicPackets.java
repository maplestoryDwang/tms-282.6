package net.swordie.ms.connection.packet;

import java.util.List;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.client.character.skills.info.ForceAtomInfo;

/** LP032B, native377C380. Restricted to the traced Saitama psychic power type115. */
public final class Tms282SaitamaPsychicPackets {
    private Tms282SaitamaPsychicPackets() { }
    public static OutPacket create(int owner, List<Integer> targets, List<ForceAtomInfo> atoms) {
        if (owner <= 0 || targets.size() != atoms.size() || atoms.size() != 10
                || targets.stream().anyMatch(id -> id <= 0)) throw new IllegalArgumentException("psychic power targets/atoms");
        OutPacket out = Tms282LoginPacketSupport.packet(0x032B);
        out.encodeByte(false); // owner is a character, not a mob
        out.encodeInt(owner);
        out.encodeInt(115); // native36F3526: skill122001014 ->115
        out.encodeByte(true);
        out.encodeInt(targets.size()); //115 belongs to377C4DF list branch
        targets.forEach(out::encodeInt);
        out.encodeInt(122001014);
        for (ForceAtomInfo atom : atoms) { out.encodeByte(true); atom.encode(out); }
        out.encodeByte(false);
        return out;
    }
}
