package net.swordie.ms.connection;

import net.swordie.ms.ServerConfig;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class InPacket {

    private final Packet bs;
    private byte[] auxiliaryData = new byte[0];
    private int auxiliaryPosition;

    public InPacket(Packet bs) {
        this.bs = bs;
    }

    public InPacket(byte[] arr) {
        this.bs = new Packet(arr);
    }

    public InPacket(byte[] arr, byte[] auxiliary) {
        this(arr);
        auxiliaryData = auxiliary.clone();
    }

    public byte[] getAuxiliaryData() {
        return auxiliaryData.clone();
    }

    public int getAuxiliaryRemaining() {
        return auxiliaryData.length - auxiliaryPosition;
    }

    public byte decodeAuxiliaryByte() {
        if (auxiliaryPosition >= auxiliaryData.length) throw new IllegalStateException("Auxiliary packet underflow");
        return auxiliaryData[auxiliaryPosition++];
    }

    public int getLength() {
        return this.bs.getByteArray().length;
    }

    public byte[] getData() {
        return this.bs.getByteArray();
    }

    /**
     * Reads a single byte of the ByteBuf.
     *
     * @return The byte that has been read.
     */
    public byte decodeByte() {
        return (byte) this.bs.readByte();
    }

    /**
     * Reads an <code>amount</code> of bytes from the ByteBuf.
     *
     * @param amount The amount of bytes to read.
     * @return The bytes that have been read.
     */
    public byte[] decodeArr(int amount) {
        byte[] arr = new byte[amount];
        for (int i = 0; i < amount; i++) {
            arr[i] = this.decodeByte();
        }
        return arr;
    }

    /**
     * Reads an integer from the ByteBuf.
     *
     * @return The integer that has been read.
     */
    public int decodeInt() {
        int byte1 = this.bs.readByte();
        int byte2 = this.bs.readByte();
        int byte3 = this.bs.readByte();
        int byte4 = this.bs.readByte();
        return (byte4 << 24) + (byte3 << 16) + (byte2 << 8) + byte1;
    }

    /**
     * Reads a short from the ByteBuf.
     *
     * @return The short that has been read.
     */
    public short decodeShort() {
        int byte1 = this.bs.readByte();
        int byte2 = this.bs.readByte();
        return (short) ((byte2 << 8) + byte1);
    }

    /**
     * Reads a char array of a given length of this ByteBuf.
     *
     * @param amount The length of the char array
     * @return The char array as a String
     */
    public String decodeString(int amount) {
        byte[] bytes = decodeArr(amount);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Reads a String, by first reading a short, then reading a char array of that length.
     *
     * @return The char array as a String
     */
    public String decodeString() {
        int amount = decodeShort();
        return decodeString(amount);
    }

    @Override
    public String toString() {
        return Util.readableByteArray(Arrays.copyOfRange(getData(), getData().length - getUnreadAmount(), getData().length)); // Substring after copy of range xd
    }


    /**
     * Reads and returns a long from this net.swordie.ms.connection.packet.
     *
     * @return The long that has been read.
     */
    public long decodeLong() {
        long byte1 = this.bs.readByte();
        long byte2 = this.bs.readByte();
        long byte3 = this.bs.readByte();
        long byte4 = this.bs.readByte();
        long byte5 = this.bs.readByte();
        long byte6 = this.bs.readByte();
        long byte7 = this.bs.readByte();
        long byte8 = this.bs.readByte();
        return (byte8 << 56) + (byte7 << 48) + (byte6 << 40) + (byte5 << 32) + (byte4 << 24) + (byte3 << 16) + (byte2 << 8) + byte1;
    }

    /**
     * Reads a position (short x, short y) and returns this.
     *
     * @return The position that has been read.
     */
    public Position decodePosition() {
        return new Position(decodeShort(), decodeShort());
    }

    /**
     * Reads a rectangle (short l, short t, short r, short b) and returns this.
     *
     * @return The rectangle that has been read.
     */
    public Rect decodeShortRect() {
        return new Rect(decodePosition(), decodePosition());
    }

    /**
     * Reads a rectangle (int l, int t, int r, int b) and returns this.
     *
     * @return The rectangle that has been read.
     */
    public Position decodePositionInt() {
        return new Position(decodeInt(), decodeInt());
    }

    /**
     * Returns the amount of bytes that are unread.
     *
     * @return The amount of bytes that are unread.
     */
    public int getUnreadAmount() {
        return this.bs.available();
    }

    /**
     * Reads a rectangle (int l, int t, int r, int b) and returns this.
     *
     * @return The rectangle that has been read.
     */
    public Rect decodeIntRect() {
        return new Rect(decodePositionInt(), decodePositionInt());
    }

    public final long getPosition() {
        return this.bs.getPosition();
    }

    public final void seek(long offset) {
        try {
            this.bs.seek(offset);
        } catch (IOException var4) {
            System.err.println("Seek failed" + var4);
        }
    }

    public final void skip(int num) {
        this.seek(this.getPosition() + num);
    }

    public final void skipByte() {
        skip(1);
    }

    public final void skipShort() {
        skip(2);
    }

    public final void skipInt() {
        skip(4);
    }

    public final void skipPosition() {
        skip(4);
    }

    public final void skipPositionInt() {
        skip(8);
    }

    public final void skipLong() {
        skip(8);
    }

    public final void skipString() {
        int len = decodeShort() & 0xFFFF;
        skip(len);
    }
}
