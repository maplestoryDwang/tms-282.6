package net.swordie.ms.life.drop;

public class DropExpire {

    public final int dropId;
    public final long expireAt;

    public DropExpire(int dropId, long expireAt) {
        this.dropId = dropId;
        this.expireAt = expireAt;
    }
}
