package net.swordie.ms.connection;

import net.swordie.ms.util.Util;

import java.io.IOException;

public class Packet {
    public static final int MAX_SHORT_PACKET_SIZE = 0xFF00;
    private int pos = 0;
    private long bytesRead = 0L;
    private final byte[] arr;

    public Packet(byte[] arr) {
        this.arr = arr;
    }

    public byte[] getByteArray() {
        return this.arr;
    }

    public long getPosition() {
        return this.pos;
    }

    public void seek(long offset) throws IOException {
        this.pos = (int)offset;
    }

    public long getBytesRead() {
        return this.bytesRead;
    }

    public int readByte() {
        this.bytesRead++;
        return this.arr[this.pos++] & 0xFF;
    }

    @Override
    public String toString() {
        return this.toString(false);
    }

    public String toString(boolean b) {
        String nows = "";
        if (this.arr.length - this.pos > 0) {
            byte[] now = new byte[this.arr.length - this.pos];
            System.arraycopy(this.arr, this.pos, now, 0, this.arr.length - this.pos);
            nows = Util.readableByteArray(now);
        }

        return b ? "All: " + Util.readableByteArray(this.arr) + "\nNow: " + nows : "Data: " + nows;
    }

    public int available() {
        return this.arr.length - this.pos;
    }
}
