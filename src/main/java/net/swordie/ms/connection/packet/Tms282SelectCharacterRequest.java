package net.swordie.ms.connection.packet;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * CP0079, built in CLogin::Update 3C90100 (send 3C9042F); evidence in
 * research/tms2823/packet-audit/enter-game-2823.md. Payload is 40 bytes plus the SPW.
 * Triple A is CWvsContext+E0..EC, triple B is +F0..F8; normally both are
 * (CLogin+62C, CLogin+630, characterId), which are believed to be world and channel.
 */
public record Tms282SelectCharacterRequest(int spwKey, byte[] spwRaw, int characterId, int uiByte,
                                           int worldA, int channelA, int charIdA, int flagA,
                                           int worldB, int channelB, int charIdB, int trailer) {
    public Tms282SelectCharacterRequest {
        spwRaw = spwRaw.clone();
    }

    @Override
    public byte[] spwRaw() {
        return spwRaw.clone();
    }

    /** Accepts the full plaintext packet, including opcode. */
    public static Tms282SelectCharacterRequest parse(byte[] packet) {
        Objects.requireNonNull(packet, "packet");
        ByteBuffer in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        try {
            if (Short.toUnsignedInt(in.getShort()) != 0x0079) {
                throw new IllegalArgumentException("Wrong select-character request opcode");
            }
            int key = in.getInt();
            int length = Short.toUnsignedInt(in.getShort());
            if (in.remaining() != length + 34) {
                throw new IllegalArgumentException("CP0079 SPW length or trailer mismatch");
            }
            byte[] spw = new byte[length];
            in.get(spw);
            return new Tms282SelectCharacterRequest(key, spw, in.getInt(), Byte.toUnsignedInt(in.get()),
                    in.getInt(), in.getInt(), in.getInt(), Byte.toUnsignedInt(in.get()),
                    in.getInt(), in.getInt(), in.getInt(), in.getInt());
        } catch (BufferUnderflowException e) {
            throw new IllegalArgumentException("Truncated CP0079", e);
        }
    }

    /** Inverse of 3CA7AFF..3CA7BDE: each SPW byte was rotated left by (key % 7) + 1. */
    public byte[] spwPlainBytes() {
        int n = Math.floorMod(spwKey, 7) + 1;
        byte[] out = new byte[spwRaw.length];
        for (int i = 0; i < out.length; i++) {
            int b = spwRaw[i] & 0xFF;
            out[i] = (byte) ((b >>> n) | (b << (8 - n)));
        }
        return out;
    }
}
