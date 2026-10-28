package net.swordie.ms.client.character;

import java.io.Serializable;

public class MonsterCollectionReward implements Serializable {

    /** A claimed reward: {@code session} -1 = the region's reward, {@code group} -1 = the session's reward. */
    public static MonsterCollectionReward of(int region, int session, int group) {
        MonsterCollectionReward reward = new MonsterCollectionReward();
        reward.setRegion(region);
        reward.setSession(session);
        reward.setGroup(group);
        return reward;
    }

    private int region;
    private int session; // -1 == region (medal)
    private int group; // -1 == session

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

    public int getGroup() {
        return group;
    }

    public void setGroup(int group) {
        this.group = group;
    }
}
