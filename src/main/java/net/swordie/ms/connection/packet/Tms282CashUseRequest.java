package net.swordie.ms.connection.packet;

import net.swordie.ms.constants.ItemConstants;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/** CP0150: native 5E120AF prefix; pet-skill case27 at5E1786D appends owned pet SN8. */
public record Tms282CashUseRequest(int slot, int itemId, long petSn, byte[] body) {
    public static Tms282CashUseRequest parse(byte[] packet) {
        if (packet == null || packet.length < 16) throw new IllegalArgumentException("Short cash use");
        var in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        if (Short.toUnsignedInt(in.getShort()) != 0x150 || in.getInt() != 0)
            throw new IllegalArgumentException("Cash use prefix");
        in.getInt(); // client tick
        int slot = Short.toUnsignedInt(in.getShort()), id = in.getInt();
        boolean petSkill = ItemConstants.isPetSkillItem(id) && ItemConstants.getPetSkillFromID(id) != null;
        if (id / 10000 != 553 && !petSkill) throw new IllegalArgumentException("Unsupported cash use variant");
        int length = petSkill ? 24 : 16;
        if (slot == 0 || slot > Short.MAX_VALUE || (packet.length != length && packet.length != length + 4))
            throw new IllegalArgumentException("Cash use length or slot");
        long petSn = petSkill ? in.getLong() : 0;
        if (petSkill && petSn <= 0) throw new IllegalArgumentException("Invalid pet SN");
        if (packet.length == length + 4) {
            int trailer = in.getInt();
            if (trailer < 0 || trailer > 1) throw new IllegalArgumentException("Cash use trailer");
        }
        return new Tms282CashUseRequest(slot, id, petSn, Arrays.copyOfRange(packet, 2, length));
    }
}
