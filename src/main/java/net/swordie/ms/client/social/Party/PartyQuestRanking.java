package net.swordie.ms.client.social.Party;

import net.swordie.ms.enums.PartyQuestRankingType;

import java.util.List;

public class PartyQuestRanking {

    private int rank;
    private int questValueType;
    private PartyQuestRankingType rankingType;
    private List<PartyRankingInfo> partyRankingInfoList;

    public PartyQuestRanking(int rank, int questValueType, PartyQuestRankingType rankingType, List<PartyRankingInfo> partyRankingInfoList) {
        this.rank = rank;
        this.questValueType = questValueType;
        this.rankingType = rankingType;
        this.partyRankingInfoList = partyRankingInfoList;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getQuestValueType() {
        return questValueType;
    }

    public void setQuestValueType(int questValueType) {
        this.questValueType = questValueType;
    }

    public PartyQuestRankingType getRankingType() {
        return rankingType;
    }

    public void setRankingType(PartyQuestRankingType rankingType) {
        this.rankingType = rankingType;
    }

    public List<PartyRankingInfo> getPartyRankingInfoList() {
        return partyRankingInfoList;
    }

    public void setPartyRankingInfoList(List<PartyRankingInfo> partyRankingInfoList) {
        this.partyRankingInfoList = partyRankingInfoList;
    }
}
