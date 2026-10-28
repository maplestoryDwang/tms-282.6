package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * CP0120 title (稱號) activation of TMS 282.3 = 265 USER_ACTIVATE_NICK_ITEM (0x011C), same body: i32 itemId,
 * i32 install-inventory slot. Sender 5EE28D0 (8 callers, e.g. the install-item use path 362CD1F for 370xxxx):
 * itemId is 0 (take the title off) or 3700000..3709999; when the chosen title is already worn the client sends 0;
 * the client applies its own title locally (5456EE0) before sending, takes no exclusive lock and waits for no
 * reply. Unicorn trace (research/tms2823/collab-frieren/title_emu.py): (3700821, 5) -> {@code 2001 55783800
 * 05000000}; (0, 0) -> {@code 2001 00000000 00000000} (also sent right after field entry); 2000000 -> nothing.
 * Titles with item info OnUseScriptOverride take an actor-RPC path instead (wire 0x57) and are not covered.
 * A trailing u32 (socket tail) is tolerated.
 */
public record Tms282NickItemRequest(int itemId, int slot) {
    public static final int OPCODE = 0x0120;
    public static final int FIRST_TITLE = 3700000;
    public static final int TITLE_RANGE = 10000;

    public static Tms282NickItemRequest parse(byte[] packet) {
        if (packet == null || packet.length < 2) {
            throw new IllegalArgumentException("CP0120: no opcode");
        }
        int op = (packet[0] & 0xFF) | (packet[1] & 0xFF) << 8;
        if (op != OPCODE) {
            throw new IllegalArgumentException(String.format("CP0120: opcode %04X", op));
        }
        int body = packet.length - 2;
        if (body != 8 && body != 12) {
            throw new IllegalArgumentException("CP0120: body of " + body + " bytes");
        }
        ByteBuffer b = ByteBuffer.wrap(packet, 2, 8).order(ByteOrder.LITTLE_ENDIAN);
        int item = b.getInt();
        int slot = b.getInt();
        if (item != 0 && !isTitle(item)) {
            throw new IllegalArgumentException("CP0120: not a title item " + item);
        }
        if (slot < 0 || slot > 0x7FFF) {
            throw new IllegalArgumentException("CP0120: slot " + slot);
        }
        return new Tms282NickItemRequest(item, slot);
    }

    public static boolean isTitle(int itemId) {
        return itemId >= FIRST_TITLE && itemId < FIRST_TITLE + TITLE_RANGE;
    }

    /** 265 USER_ACTIVATE_NICK_ITEM body (ItemHandler.handleUserActivateNickItem: i32 item, i32 slot). */
    public byte[] toGms265Body() {
        return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(itemId).putInt(slot).array();
    }
}
