package net.swordie.ms.client.character.items;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.social.Guild.GuildSkill;
import net.swordie.ms.connection.Encodable;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.constants.GuildConstants;
import net.swordie.ms.enums.EnchantStat;
import net.swordie.ms.enums.EquipBaseStat;
import net.swordie.ms.enums.SpellTraceScrollType;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.Util;

import java.util.Map;
import java.util.TreeMap;

public class ScrollUpgradeInfo implements Encodable {
    private int iconID;
    private String title;
    private SpellTraceScrollType type;
    private int option;
    private TreeMap<EnchantStat, Integer> stats; // needs to be sorted
    private int oldCost;
    private int cost;
    private int chance;

    public ScrollUpgradeInfo(int iconID, String title, SpellTraceScrollType scrollType, int scrollOption,
                             TreeMap<EnchantStat, Integer> scrollStats, int cost, int chance) {
        this.iconID = iconID;
        this.title = title;
        this.type = scrollType;
        this.option = scrollOption;
        this.stats = scrollStats;
        this.cost = cost;
        this.chance = chance;
    }

    public int getMask() {
        int mask = 0;
        for (EnchantStat es : getStats().keySet()) {
            mask |= es.getVal();
        }
        return mask;
    }

    @Override
    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(2);
        outPacket.encodeInt(111);
        outPacket.encodeInt(0);
        outPacket.encodeInt(getIconID());
        outPacket.encodeString(getTitle());
        outPacket.encodeInt(getType().ordinal()); // 0
        outPacket.encodeInt(getOption()); // 0
        outPacket.encodeInt(getMask()); // -1?
        for (Map.Entry<EnchantStat, Integer> entry : getStats().entrySet()) {
            outPacket.encodeInt(entry.getValue());
        }
        outPacket.encodeInt(getChance());
        outPacket.encodeInt(getOldCost());
        outPacket.encodeInt(getCost());
        outPacket.encodeByte(0); // ignored?
        outPacket.encodeInt(1); // ?
        outPacket.encodeInt(4); // bonus +4 % success
        outPacket.encodeInt(40); // ?
    }

    public boolean applyTo(Char chr, boolean isArkInno, Equip equip, Equip zeroEquip) {
        boolean success = false;
        switch (getType()) {
            case Normal: {
                int chance = getChance();
                if (equip.hasAttribute(EquipAttribute.LuckyDay)) {
                    chance += 10;
                }
                success = Util.succeedProp(chance);
                if (success) {
                    equip.setLastScrollFail(false);
                    if (zeroEquip != null) {
                        zeroEquip.setLastScrollFail(false);
                    }
                    for (Map.Entry<EnchantStat, Integer> entry : getStats().entrySet()) {
                        EnchantStat es = entry.getKey();
                        int val = entry.getValue();
                        equip.addStat(es.getEquipBaseStat(), val);
                        if (zeroEquip != null) {
                            zeroEquip.addStat(es.getEquipBaseStat(), val);
                        }
                    }
                    equip.addStat(EquipBaseStat.tuc, -1);
                    equip.addStat(EquipBaseStat.cuc, 1);
                    if (zeroEquip != null) {
                        zeroEquip.addStat(EquipBaseStat.tuc, -1);
                        zeroEquip.addStat(EquipBaseStat.cuc, 1);
                    }
                } else {
                    boolean tucProtect = false;
                    if (chr.getGuild() != null) {
                        GuildSkill guildSkill = chr.getGuild().getSkillById(GuildConstants.UPGRADE_SALVATION);
                        SkillInfo skillInfo = SkillData.getSkillInfoById(GuildConstants.UPGRADE_SALVATION);
                        if (guildSkill != null && skillInfo != null) {
                            int chanceNoDecreaseTuc = skillInfo.getValue(SkillStat.itemTUCProtectR, guildSkill.getLevel());
                            chr.chatMessage("[公會技能] 搭配公會技能使用時，有 " + chanceNoDecreaseTuc + "% 的機率讓道具不會損壞。");
                            tucProtect = Util.succeedProp(chanceNoDecreaseTuc);
                        }
                    }
                    if (tucProtect) {
                        chr.sendPopupSay("[公會技能] 已保護 1 次可升級次數。");
                    }
                    equip.setLastScrollFail(true);
                    if (!equip.hasAttribute(EquipAttribute.UpgradeCountProtection)) {
                        if (!tucProtect) {
                            equip.addStat(EquipBaseStat.tuc, -1);
                        }
                    }
                    if (zeroEquip != null) {
                        zeroEquip.setLastScrollFail(true);
                        if (!zeroEquip.hasAttribute(EquipAttribute.UpgradeCountProtection)) {
                            if (!tucProtect) {
                                zeroEquip.addStat(EquipBaseStat.tuc, -1);
                            }
                        }
                    }

                }
                equip.removeAttribute(EquipAttribute.LuckyDay);
                equip.removeAttribute(EquipAttribute.UpgradeCountProtection);
                if (zeroEquip != null) {
                    zeroEquip.removeAttribute(EquipAttribute.LuckyDay);
                    zeroEquip.removeAttribute(EquipAttribute.UpgradeCountProtection);
                }
                break;
            }
            case CleanSlate: {
                success = Util.succeedProp(getChance());
                if (success) {
                    Equip fullTucEquip = ItemData.getEquipDeepCopyFromID(equip.getItemId(), false);
                    int maxTuc = fullTucEquip.getTuc();
                    if (equip.getTuc() < maxTuc && (maxTuc + equip.getIuc() - equip.getTuc() > equip.getCuc()) && equip.isLastScrollFail()) {
                        equip.addStat(EquipBaseStat.tuc, 1);
                        if (zeroEquip != null) {
                            zeroEquip.addStat(EquipBaseStat.tuc, 1);
                        }
                    }
                }
                equip.removeAttribute(EquipAttribute.LuckyDay);
                equip.removeAttribute(EquipAttribute.UpgradeCountProtection);
                if (zeroEquip != null) {
                    zeroEquip.removeAttribute(EquipAttribute.LuckyDay);
                    zeroEquip.removeAttribute(EquipAttribute.UpgradeCountProtection);
                }
                break;
            }
            case Innocence: {
                success = Util.succeedProp(getChance());
                if (success) {
                    equip.reset(false, isArkInno);
                    if (zeroEquip != null) {
                        zeroEquip.reset(false, isArkInno);
                    }
                }
                equip.removeAttribute(EquipAttribute.LuckyDay);
                equip.removeAttribute(EquipAttribute.UpgradeCountProtection);
                if (zeroEquip != null) {
                    zeroEquip.removeAttribute(EquipAttribute.LuckyDay);
                    zeroEquip.removeAttribute(EquipAttribute.UpgradeCountProtection);
                }
                break;
            }
        }
        return success;
    }

    public int getIconID() {
        return iconID;
    }

    public void setIconID(int iconID) {
        this.iconID = iconID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public SpellTraceScrollType getType() {
        return type;
    }

    public void setType(SpellTraceScrollType type) {
        this.type = type;
    }

    public int getOption() {
        return option;
    }

    public void setOption(int option) {
        this.option = option;
    }

    public TreeMap<EnchantStat, Integer> getStats() {
        return stats;
    }

    public void setStats(TreeMap<EnchantStat, Integer> stats) {
        this.stats = stats;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public int getChance() {
        return chance;
    }

    public void setChance(int chance) {
        this.chance = chance;
    }

    @Override
    public String toString() {
        return "ScrollUpgradeInfo [" +
                "iconID=" + iconID +
                ", title='" + title + '\'' +
                ", type=" + type +
                ", option=" + option +
                ", stats=" + stats +
                ", cost=" + cost +
                ", chance=" + chance +
                ']';
    }

    public int getOldCost() {
        return oldCost;
    }

    public void setOldCost(int oldCost) {
        this.oldCost = oldCost;
    }
}
