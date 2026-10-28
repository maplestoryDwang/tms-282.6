package net.swordie.ms.connection.packet;

/**
 * CP014D USER_TAMING_MOB_FOOD_ITEM_USE_REQUEST (265 InHeader 0x148, which 265 does not handle). Single static sender
 * 5E10630 (packet built at 5E106E1..5E10720):
 * <pre>
 * u32 tick      5B4FC50()
 * u16 slot      CONSUME bag position
 * u32 itemId    the taming mob food (226xxxx)
 * </pre>
 * The sender sets the client's request lock (5E10725: [ctx+0x248C] = 1), so every answer must carry exclRequest.
 */
public record Tms282TamingMobFoodRequest(int tick, short slot, int itemId) {
    public static final int OPCODE = 0x014D;
    public static final int LENGTH = 2 + 4 + 2 + 4;

    /** Accepts the full plaintext packet, including the opcode. Exactly {@value #LENGTH} bytes. */
    public static Tms282TamingMobFoodRequest parse(byte[] packet) {
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, OPCODE, "CP014D");
        Tms282TamingMobFoodRequest request = new Tms282TamingMobFoodRequest(in.i32(), in.i16(), in.i32());
        in.end();
        return request;
    }

    @Override
    public String toString() {
        return String.format("CP014D taming mob food slot=%d item=%d", slot, itemId);
    }
}
