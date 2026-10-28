package net.swordie.ms.loaders.containerclasses;

import java.io.Serializable;

public class MonsterCollectionSessionRewardInfo implements Serializable {

    private int region;
    private int session;
    private int rewardID;
    private int quantity;

    public int getRegion() {
        return region;
    }

    public void setRegion(int region) {
        this.region = region;
    }

    public int getSession() {
        return session;
    }

    public void setSession(int session) {
        this.session = session;
    }

    public int getRewardID() {
        return rewardID;
    }

    public void setRewardID(int rewardID) {
        this.rewardID = rewardID;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}

