package net.swordie.ms.connection;

public final class SharedPacket {
    public final byte[] data;
    public final boolean auxiliary;

    public SharedPacket(byte[] data) {
        this(data, false);
    }

    public SharedPacket(byte[] data, boolean auxiliary) {
        this.data = data;
        this.auxiliary = auxiliary;
    }
}
