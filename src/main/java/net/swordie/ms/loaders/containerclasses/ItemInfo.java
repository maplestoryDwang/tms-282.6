package net.swordie.ms.loaders.containerclasses;

import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.enums.ScrollStat;
import net.swordie.ms.enums.SpecStat;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.util.Util;

import java.util.*;

/**
 * Created on 1/9/2018.
 */
public class ItemInfo {
    private int itemId;
    private InvType invType;
    private boolean cash;
    private int price;
    private int slotMax = 200;
    private boolean tradeBlock;
    private boolean notSale;
    private String path = "";
    private boolean noCursed;
    private Map<ScrollStat, Integer> scrollStats = new HashMap<>();
    private Map<SpecStat, Integer> specStats = new HashMap<>();
    private int bagType;
    private int charismaEXP;
    private int charmEXP;
    private int senseEXP;
    private int craftEXP;
    private int willEXP;
    private int nickSkill;
    private boolean quest;
    private int reqQuestOnProgress;
    private Set<Integer> questIDs = new HashSet<>();
    private int mobID;
    private int mobHP;
    private int createID;
    private int npcID;
    private int linkedID;
    private boolean monsterBook;
    private boolean notConsume;
    private String script = "";
    private int scriptNPC;
    private int life;
    private int masterLv;
    private int reqSkillLv;
    private Set<Integer> skills = new HashSet<>();
    private int moveTo;
    private Set<ItemRewardInfo> itemRewardInfos = new HashSet<>();
    private int skillId;
    private int grade;
    private int android;
    private int stateChangeItem;
    private int meso;
    private int unitPrice;
    private int recoveryHP, recoveryMP;
    private int minusLevel;
    private Set<Integer> reqItemIds = new HashSet<>();
    private boolean expireOnLogout = false;
    private int maplepoint;
    private int pointCost;
    private boolean exNew = false;
    private boolean accountSharable = false;
    private boolean sharableOnce = false;

    public Set<Integer> getReqItemIds() {
        return reqItemIds;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public InvType getInvType() {
        return invType;
    }

    public void setInvType(InvType invType) {
        this.invType = invType;
    }

    public boolean isCash() {
        return cash;
    }

    public void setCash(boolean cash) {
        this.cash = cash;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getSlotMax() {
        switch (itemId) {
            case ItemConstants.SPECIAL_BEAUTY_COUPON:
                return 9999;
            case 2048716: //Powerful Rebirth Flame
            case 2048724: //Powerful Rebirth Flame
                return 100;
            case 2049027: //Pure Clean Slate Scroll 5%
            case 2049030: //Pure Clean Slate Scroll 5%
            case 2049035: //Pure Clean Slate Scroll 5%
            case 2049606: //Innocence Scroll 50%
            case 2049612: //Innocence Scroll 50%
                return 500;
            case 4031249: //Phong bao lì xì
                return 1000;
            default:
                return slotMax;
        }
    }

    public void setSlotMax(int slotMax) {
        this.slotMax = slotMax;
    }

    public boolean isTradeBlock() {
        return tradeBlock;
    }

    public void setTradeBlock(boolean tradeBlock) {
        this.tradeBlock = tradeBlock;
    }

    public boolean isNotSale() {
        return notSale;
    }

    public void setNotSale(boolean notSale) {
        this.notSale = notSale;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isNoCursed() {
        return noCursed;
    }

    public void setNoCursed(boolean noCursed) {
        this.noCursed = noCursed;
    }

    public Map<ScrollStat, Integer> getScrollStats() {
        return scrollStats;
    }

    public void setScrollStats(Map<ScrollStat, Integer> scrollStats) {
        this.scrollStats = scrollStats;
    }

    public void putScrollStat(ScrollStat scrollStat, int val) {
        getScrollStats().put(scrollStat, val);
    }

    public int getBagType() {
        return bagType;
    }

    public void setBagType(int bagType) {
        this.bagType = bagType;
    }

    public boolean isQuest() {
        return quest;
    }

    public void setQuest(boolean quest) {
        this.quest = quest;
    }

    public int getReqQuestOnProgress() {
        return reqQuestOnProgress;
    }

    public void setReqQuestOnProgress(int reqQuestOnProgress) {
        this.reqQuestOnProgress = reqQuestOnProgress;
    }

    public int getCharismaEXP() {
        return charismaEXP;
    }

    public void setCharismaEXP(int charismaEXP) {
        this.charismaEXP = charismaEXP;
    }

    public int getCharmEXP() {
        return charmEXP;
    }

    public void setCharmEXP(int charmEXP) {
        this.charmEXP = charmEXP;
    }

    public int getSenseEXP() {
        return senseEXP;
    }

    public void setSenseEXP(int senseEXP) {
        this.senseEXP = senseEXP;
    }

    public int getCraftEXP() {
        return craftEXP;
    }

    public void setCraftEXP(int craftEXP) {
        this.craftEXP = craftEXP;
    }

    public int getWillEXP() {
        return willEXP;
    }

    public void setWillEXP(int willEXP) {
        this.willEXP = willEXP;
    }

    public int getNickSkill() {
        return nickSkill;
    }

    public void setNickSkill(int nickSkill) {
        this.nickSkill = nickSkill;
    }

    public void addQuest(int questID) {
        getQuestIDs().add(questID);
    }

    public Set<Integer> getQuestIDs() {
        return questIDs;
    }

    public int getMobID() {
        return mobID;
    }

    public void setMobID(int mobID) {
        this.mobID = mobID;
    }

    public void setCreateID(int createID) {
        this.createID = createID;
    }

    public int getCreateID() {
        return this.createID;
    }

    public void setMobHP(int mobHP) {
        this.mobHP = mobHP;
    }

    public int getMobHP() {
        return mobHP;
    }

    public int getNpcID() {
        return npcID;
    }

    public void setNpcID(int npcID) {
        this.npcID = npcID;
    }

    public int getLinkedID() {
        return linkedID;
    }

    public void setLinkedID(int linkedID) {
        this.linkedID = linkedID;
    }

    public boolean isMonsterBook() {
        return monsterBook;
    }

    public void setMonsterBook(boolean monsterBook) {
        this.monsterBook = monsterBook;
    }

    public boolean isNotConsume() {
        return notConsume;
    }

    public void setNotConsume(boolean notConsume) {
        this.notConsume = notConsume;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public void putSpecStat(SpecStat ss, int i) {
        getSpecStats().put(ss, i);
    }

    public Map<SpecStat, Integer> getSpecStats() {
        return specStats;
    }

    public void setSpecStats(Map<SpecStat, Integer> specStats) {
        this.specStats = specStats;
    }

    public int getScriptNPC() {
        return scriptNPC;
    }

    public void setScriptNPC(int scriptNPC) {
        this.scriptNPC = scriptNPC;
    }

    public int getLife() {
        return life;
    }

    public void setLife(int life) {
        this.life = life;
    }

    public int getMasterLv() {
        return masterLv;
    }

    public void setMasterLv(int masterLv) {
        this.masterLv = masterLv;
    }

    public int getReqSkillLv() {
        return reqSkillLv;
    }

    public void setReqSkillLv(int reqSkillLv) {
        this.reqSkillLv = reqSkillLv;
    }

    public Set<Integer> getSkills() {
        return skills;
    }

    public void setSkills(Set<Integer> skills) {
        this.skills = skills;
    }

    public void addSkill(int skill) {
        skills.add(skill);
    }

    public int getMoveTo() {
        return moveTo;
    }

    public void setMoveTo(int moveTo) {
        this.moveTo = moveTo;
    }

    public int getSkillId() {
        return skillId;
    }

    public void setSkillId(int skillId) {
        this.skillId = skillId;
    }

    public void addItemReward(ItemRewardInfo iri) {
        getItemRewardInfos().add(iri);
    }

    public Set<ItemRewardInfo> getItemRewardInfos() {
        return itemRewardInfos;
    }

    public Item getRandomReward() {
        List<ItemRewardInfo> iris = getItemRewardInfos().stream()
                .filter(r -> r.getItemID()>0 && r.getProb()>0 && Double.isFinite(r.getProb())).toList();
        double total=iris.stream().mapToDouble(ItemRewardInfo::getProb).sum();
        if(total<=0 || !Double.isFinite(total))return null;
        double roll=java.util.concurrent.ThreadLocalRandom.current().nextDouble(total);
        ItemRewardInfo selected=iris.getLast();
        for(ItemRewardInfo reward:iris){roll-=reward.getProb();if(roll<0){selected=reward;break;}}
        Item item=ItemData.getItemDeepCopy(selected.getItemID());
        if(item==null)return null;
        item.setQuantity(Math.max(1,selected.getCount()));
        if(selected.getPeriod()>0) {
            item.setDateExpire(net.swordie.ms.util.FileTime.fromDate(java.time.LocalDateTime.now().plusMinutes(selected.getPeriod())));
            if(item instanceof net.swordie.ms.client.character.items.PetItem pet)pet.setDateDead(item.getDateExpire());
        }
        return item;
    }

    public int getRandomMesoReward() {
        List<ItemRewardInfo> iris = new ArrayList<>(getItemRewardInfos());
        iris.sort(Comparator.comparingDouble(ItemRewardInfo::getProb));
        Collections.reverse(iris);
        double rand = new Random().nextDouble() * 100 + 1;
        for (ItemRewardInfo iri : iris) {
            if (rand <= iri.getProb()) {
                if (iri.getMeso() > 0) {
                    return iri.getMeso();
                }
            }
            rand -= iri.getProb();
        }
        return 0;
    }

    public int getGrade() {
        return grade;
    }

    public void setGrade(int grade) {
        this.grade = grade;
    }

    public int getAndroid() {
        return android;
    }

    public void setAndroid(int android) {
        this.android = android;
    }

    public int getStateChangeItem() {
        return stateChangeItem;
    }

    public void setStateChangeItem(int stateChangeItem) {
        this.stateChangeItem = stateChangeItem;
    }

    public int getMeso() {
        return meso;
    }

    public void setMeso(int meso) {
        this.meso = meso;
    }

    public int getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(int unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getRecoveryHP() {
        return recoveryHP;
    }

    public void setRecoveryHP(int recoveryHP) {
        this.recoveryHP = recoveryHP;
    }

    public int getRecoveryMP() {
        return recoveryMP;
    }

    public void setRecoveryMP(int recoveryMP) {
        this.recoveryMP = recoveryMP;
    }

    public int getMinusLevel() {
        return minusLevel;
    }

    public void setMinusLevel(int minusLevel) {
        this.minusLevel = minusLevel;
    }

    public boolean isExpireOnLogout() {
        return expireOnLogout;
    }

    public void setExpireOnLogout(boolean expireOnLogout) {
        this.expireOnLogout = expireOnLogout;
    }

    public int getMaplepoint() {
        return maplepoint;
    }

    public void setMaplepoint(int maplepoint) {
        this.maplepoint = maplepoint;
    }

    public int getPointCost() {
        return pointCost;
    }

    public void setPointCost(int pointCost) {
        this.pointCost = pointCost;
    }

    public boolean isExNew() {
        return exNew;
    }

    public void setExNew(boolean exNew) {
        this.exNew = exNew;
    }

    public boolean isAccountSharable() {
        return accountSharable;
    }

    public void setAccountSharable(boolean accountSharable) {
        this.accountSharable = accountSharable;
    }

    public boolean isSharableOnce() {
        return sharableOnce;
    }

    public void setSharableOnce(boolean sharableOnce) {
        this.sharableOnce = sharableOnce;
    }
}
