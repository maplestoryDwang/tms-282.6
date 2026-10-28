package net.swordie.ms.connection.packet;

import java.util.HexFormat;
import java.util.Objects;

/**
 * The death window's answer. The window (CUIDead, sender 328CE80) calls the transfer-field sender 3732A60 with
 * target 0, no portal and its "premium" argument (r9b) = the option the user picked; on some fields (3739D30) it
 * sends CP02A7 {@code u8 premium, u8 flag} instead [verified: static sender].
 * <p>
 * Where the premium argument lands in CP00FB is NOT verified: 3732A60 runs through the VM. Its encode list is
 * {@code u8, u8 fieldKey, i32 target, str portal, [i16 x, i16 y], u8, u8, u8, u8, [i32, i32]}; the portal walk
 * (all flags 0) ends with {@code 00 00 00 00}. Following the GMS order ({@code u8 0, u8 bPremium, u8 bChase}) the
 * premium flag is taken as the second of the four bytes [推定]. The server only revives in place when the user also
 * holds a revive item, so a wrong guess costs the option, never an item-free revive.
 */
public record Tms282ReviveRequest(int opcode, boolean premium, byte[] flags) {
    public static final int SPECIAL_FIELD_REVIVE = 0x02A7;

    public Tms282ReviveRequest {
        flags = Objects.requireNonNull(flags, "flags").clone();
    }

    @Override
    public byte[] flags() {
        return flags.clone();
    }

    /** From CP00FB: the flag bytes follow the portal name (after {@code i16 x, i16 y} when a portal is named). */
    public static Tms282ReviveRequest fromTransfer(Tms282TransferFieldRequest request) {
        byte[] tail = request.tail();
        int from = request.hasPortal() ? Math.min(4, tail.length) : 0;
        byte[] flags = java.util.Arrays.copyOfRange(tail, from, tail.length);
        return new Tms282ReviveRequest(request.opcode(), flags.length >= 2 && flags[1] != 0, flags);
    }

    /** CP02A7: exactly {@code u8 premium, u8 flag}. */
    public static Tms282ReviveRequest parseSpecial(byte[] packet) {
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, SPECIAL_FIELD_REVIVE, "CP02A7");
        int premium = in.u8();
        int flag = in.u8();
        in.end();
        return new Tms282ReviveRequest(SPECIAL_FIELD_REVIVE, premium != 0, new byte[]{(byte) premium, (byte) flag});
    }

    @Override
    public String toString() {
        return String.format("CP%04X revive premium=%s flags=%s", opcode, premium, HexFormat.of().formatHex(flags));
    }
}
