package net.swordie.ms.life;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.DragonPool;
import net.swordie.ms.util.Position;
import net.swordie.ms.world.field.Foothold;

public class Dragon extends Life {

    private int ownerId;
    private short foothold;
    private short job;

    public Dragon(Char chr) {
        super(0);
        this.ownerId = chr.getId();
        this.foothold = chr.getFoothold();
        this.job = chr.getJob();
    }

    public void resetToPlayer(Position pos) {
        setPosition(pos.deepCopy());
        setMoveAction((byte) 4); // default
    }

    @Override
    public void broadcastSpawnPacket(Char onlyChar) {
        getField().broadcast(DragonPool.createDragon(this));
    }

    @Override
    public void broadcastLeavePacket() {
        getField().broadcast(DragonPool.removeDragon(this));
    }

    public int getOwnerId() {
        return ownerId;
    }

    public short getFoothold() {
        return foothold;
    }

    public void setFoothold(short foothold) {
        this.foothold = foothold;
    }

    public short getJob() {
        return job;
    }

    public void setJob(short job) {
        this.job = job;
    }
}
