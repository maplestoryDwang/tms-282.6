package net.swordie.ms.loaders.containerclasses;

import net.swordie.ms.client.character.quest.progress.QuestProgressRequirement;
import net.swordie.ms.client.character.quest.requirement.QuestStartRequirement;
import net.swordie.ms.client.character.quest.reward.QuestReward;

import java.util.*;

/**
 * Created on 3/2/2018.
 */
public class QuestInfo {
    private int questID;
    private int startNpc;
    private String questName;
    private boolean normalAutoStart;
    private Set<QuestStartRequirement> questStartRequirements = new HashSet<>();
    private Set<QuestProgressRequirement> questProgressRequirements = new HashSet<>();
    private Set<QuestReward> questRewards = new HashSet<>();
    private Set<Integer> fieldEnters = new HashSet<>();
    private int infoNumber;
    private long end;
    private long endT;
    private String startScript;
    private String endScript;
    private long start;
    private long startT;
    private int endNpc;
    private int subJobFlags;
    private boolean completeNpcAutoGuide;
    private int skill;
    private int fieldSetKeepTime;
    private String fieldSet;
    private boolean autoStart;
    private int deathCount;
    private List<Integer> scenarios = new ArrayList<>();
    private Map<Integer, String> speech = new HashMap<>();
    private int mobDropMeso;
    private int morph;
    private boolean secret;
    private int transferField;
    private int nextQuest;
    private boolean autoComplete;
    private int medalItemId;
    private String demandSummary;
    private String rewardSummary;

    public Set<QuestReward> getQuestRewards() {
        return questRewards;
    }

    public int getQuestID() {
        return questID;
    }

    public void setQuestID(int questID) {
        this.questID = questID;
    }

    public int getStartNpc() {
        return startNpc;
    }

    public void setStartNpc(int startNpc) {
        this.startNpc = startNpc;
    }

    public String getQuestName() {
        return questName;
    }

    public void setQuestName(String questName) {
        this.questName = questName;
    }

    public boolean isNormalAutoStart() {
        return normalAutoStart;
    }

    public void setNormalAutoStart(boolean normalAutoStart) {
        this.normalAutoStart = normalAutoStart;
    }

    public Set<QuestStartRequirement> getQuestStartRequirements() {
        return questStartRequirements;
    }

    public void addRequirement(QuestStartRequirement qr) {
        getQuestStartRequirements().add(qr);
    }

    public int getInfoNumber() {
        return infoNumber;
    }

    public void setInfoNumber(int infoNumber) {
        this.infoNumber = infoNumber;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public long getEndT() {
        return endT;
    }

    public void setEndT(long endT) {
        this.endT = endT;
    }

    public String getStartScript() {
        return startScript == null ? "" : startScript;
    }

    public void setStartScript(String startScript) {
        this.startScript = startScript;
    }

    public String getEndScript() {
        return endScript == null ? "" : endScript;
    }

    public void setEndScript(String endScript) {
        this.endScript = endScript;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getStartT() {
        return startT;
    }

    public void setStartT(long startT) {
        this.startT = startT;
    }

    public int getEndNpc() {
        return endNpc;
    }

    public void setEndNpc(int endNpc) {
        this.endNpc = endNpc;
    }

    public Set<QuestProgressRequirement> getQuestProgressRequirements() {
        return questProgressRequirements;
    }

    public void addProgressRequirement(QuestProgressRequirement qpr) {
        getQuestProgressRequirements().add(qpr);
    }

    public void addReward(QuestReward qir) {
        getQuestRewards().add(qir);
    }

    public int getSubJobFlags() {
        return subJobFlags;
    }

    public void setSubJobFlags(int subJobFlags) {
        this.subJobFlags = subJobFlags;
    }

    public Set<Integer> getFieldEnters() {
        return fieldEnters;
    }

    public void addFieldEnter(int fieldID) {
        getFieldEnters().add(fieldID);
    }

    public boolean isCompleteNpcAutoGuide() {
        return completeNpcAutoGuide;
    }

    public void setCompleteNpcAutoGuide(boolean completeNpcAutoGuide) {
        this.completeNpcAutoGuide = completeNpcAutoGuide;
    }

    public int getSkill() {
        return skill;
    }

    public void setSkill(int skill) {
        this.skill = skill;
    }

    public int getFieldSetKeepTime() {
        return fieldSetKeepTime;
    }

    public void setFieldSetKeepTime(int fieldSetKeepTime) {
        this.fieldSetKeepTime = fieldSetKeepTime;
    }

    public String getFieldSet() {
        return fieldSet == null ? "" : fieldSet;
    }

    public void setFieldSet(String fieldSet) {
        this.fieldSet = fieldSet;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    public int getDeathCount() {
        return deathCount;
    }

    public void setDeathCount(int deathCount) {
        this.deathCount = deathCount;
    }

    public void addSpeech(Integer id, String script) {
        getSpeech().put(id, script);
    }

    public Map<Integer, String> getSpeech() {
        return speech;
    }

    public void setSpeech(Map<Integer, String> speech) {
        this.speech = speech;
    }

    public void addScenario(int value) {
        getScenarios().add(value);
    }

    public List<Integer> getScenarios() {
        return scenarios;
    }

    public void setScenarios(List<Integer> scenarios) {
        this.scenarios = scenarios;
    }

    public int getMobDropMeso() {
        return mobDropMeso;
    }

    public void setMobDropMeso(int mobDropMeso) {
        this.mobDropMeso = mobDropMeso;
    }

    public int getMorph() {
        return morph;
    }

    public void setMorph(int morph) {
        this.morph = morph;
    }

    public boolean isSecret() {
        return secret;
    }

    public void setSecret(boolean secret) {
        this.secret = secret;
    }

    public int getTransferField() {
        return transferField;
    }

    public void setTransferField(int transferField) {
        this.transferField = transferField;
    }

    public int getNextQuest() {
        return nextQuest;
    }

    public void setNextQuest(int nextQuest) {
        this.nextQuest = nextQuest;
    }

    public boolean isAutoComplete() {
        return autoComplete;
    }

    public void setAutoComplete(boolean autoComplete) {
        this.autoComplete = autoComplete;
    }

    public int getMedalItemId() {
        return medalItemId;
    }

    public void setMedalItemId(int medalItem) {
        this.medalItemId = medalItem;
    }

    public String getDemandSummary() {
        return demandSummary;
    }

    public void setDemandSummary(String demandSummary) {
        this.demandSummary = demandSummary;
    }

    public String getRewardSummary() {
        return rewardSummary;
    }

    public void setRewardSummary(String rewardSummary) {
        this.rewardSummary = rewardSummary;
    }
}
