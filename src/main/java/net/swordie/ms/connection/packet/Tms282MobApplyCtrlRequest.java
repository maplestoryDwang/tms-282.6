package net.swordie.ms.connection.packet;

/**
 * CP0517 MOB_APPLY_CTRL [presumed] (265 0x0500, the same +0x17 delta as MOB_MOVE; sender 3EDD6C0;
 * ingame-cp-map-2823.md 4.2): {@code u32 objId [mob+558], u32 templateId [[mob+560]+80], u32} (sequence
 * {@code 4 4 4}; 265 reads the object id only). Parsed strictly (14 bytes).
 */
public record Tms282MobApplyCtrlRequest(int objectId, int templateId, int value) {
    public static final int OPCODE = 0x0517;

    /** Whole plaintext packet, opcode included; must be read exactly. */
    public static Tms282MobApplyCtrlRequest parse(byte[] packet) {
        Tms282FieldRequestReader r = Tms282FieldRequestReader.open(packet, OPCODE, "CP0517");
        var request = new Tms282MobApplyCtrlRequest(r.i32(), r.i32(), r.i32());
        r.end();
        return request;
    }
}
