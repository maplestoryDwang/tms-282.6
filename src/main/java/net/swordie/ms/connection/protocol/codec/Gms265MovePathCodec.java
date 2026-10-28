package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.life.movement.MovementInfo;

/** GMS 265 paths: exactly {@link MovementInfo#MovementInfo(InPacket)} and {@link MovementInfo#encode}. */
public final class Gms265MovePathCodec implements MovePathCodec {
    public static final Gms265MovePathCodec INSTANCE = new Gms265MovePathCodec();

    private Gms265MovePathCodec() {
    }

    @Override
    public MovementInfo decode(InPacket inPacket) {
        return new MovementInfo(inPacket);
    }

    @Override
    public void encode(OutPacket outPacket, MovementInfo path) {
        path.encode(outPacket);
    }

    /** 265 broadcasts carry the whole path, key pad included (MobPool.move, UserRemote.move...). */
    @Override
    public void encodeBroadcast(OutPacket outPacket, MovementInfo path) {
        path.encode(outPacket);
    }
}
