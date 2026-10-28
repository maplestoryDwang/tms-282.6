package net.swordie.ms.connection.packet;

/**
 * CP00FC USER_TRANSFER_CHANNEL_REQUEST [opcode and layout verified: sender 3732E90 builds COutPacket(0xFC) at
 * 3733497, Encode1 of the chosen channel (r13b), Encode4 of 5B4FC50() = the update tick, then 346A070 and
 * 5E04DA0(ctx, 1), i.e. the client locks until a reply]. The byte is the wire channel (0 = channel 1), as the first
 * byte of the 265 body (265 adds 1).
 */
public record Tms282ChannelTransferRequest(int wireChannel, int tick) {
    public static final int OPCODE = 0x00FC;

    /** The full plaintext packet, including the opcode: exactly 2 + 1 + 4 bytes. */
    public static Tms282ChannelTransferRequest parse(byte[] packet) {
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, OPCODE, "CP00FC");
        Tms282ChannelTransferRequest request = new Tms282ChannelTransferRequest(in.u8(), in.i32());
        in.end();
        return request;
    }

    /** The 265 server channel id (wire channel + 1). */
    public int serverChannel() {
        return wireChannel + 1;
    }

    @Override
    public String toString() {
        return String.format("CP00FC change channel wire=%d (server channel %d)", wireChannel, serverChannel());
    }
}
