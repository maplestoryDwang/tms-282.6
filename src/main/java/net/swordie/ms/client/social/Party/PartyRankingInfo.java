package net.swordie.ms.client.social.Party;

import java.util.List;

public class PartyRankingInfo {

    private int value;
    private List<String> pmName;

    public PartyRankingInfo(int value, List<String> pmName) {
        this.value = value;
        this.pmName = pmName;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public List<String> getPmName() {
        return pmName;
    }

    public void setPmName(List<String> pmName) {
        this.pmName = pmName;
    }
}
