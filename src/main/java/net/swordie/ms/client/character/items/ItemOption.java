package net.swordie.ms.client.character.items;

import net.swordie.ms.enums.BaseStat;
import net.swordie.ms.enums.ItemGrade;

import java.util.HashMap;
import java.util.Map;

/**
 * Created on 1/26/2018.
 */
public class ItemOption {
    private int optionType;
    private int weight;
    private int id;
    private int reqLevel;
    private final Map<Integer, Map<BaseStat, Double>> statValuesPerLevel = new HashMap<>();
    private String string;
    private final Map<Integer, Map<ItemOptionType, Integer>> miscValuesPerLevel = new HashMap<>();

    private String[] statStrings = new String[]{

    };

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getOptionType() {
        return optionType;
    }

    public void setOptionType(int optionType) {
        this.optionType = optionType;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "id: " + getId() + ", optionType: " + getOptionType() + ", weight: " + getWeight();
    }

    public boolean hasMatchingGrade(short itemState) {
        return ItemGrade.isMatching(itemState, ItemGrade.getGradeByOption(getId()).getVal());
    }

    public boolean isBonus() {
        return getId() > 2000 && getId() / 1000 % 10 == 2;
    }

    public int getReqLevel() {
        return reqLevel;
    }

    public void setReqLevel(int reqLevel) {
        this.reqLevel = reqLevel;
    }

    public Map<Integer, Map<BaseStat, Double>> getStatValuesPerLevel() {
        return statValuesPerLevel;
    }

    public void addStatValue(int level, BaseStat baseStat, double value) {
        Map<BaseStat, Double> valMap = getStatValuesPerLevel().getOrDefault(level, new HashMap<>());
        valMap.put(baseStat, value);
        getStatValuesPerLevel().put(level, valMap);
    }

    public Map<Integer, Map<ItemOptionType, Integer>> getMiscValuesPerLevel() {
        return miscValuesPerLevel;
    }

    public void addMiscValue(int level, ItemOptionType type, int value) {
        Map<ItemOptionType, Integer> valMap = getMiscValuesPerLevel().getOrDefault(level, new HashMap<>());
        valMap.put(type, value);
        getMiscValuesPerLevel().put(level, valMap);
    }

    public Map<BaseStat, Double> getStatValuesByLevel(int level) {
        return getStatValuesPerLevel().getOrDefault(level, new HashMap<>());
    }

    public Map<ItemOptionType, Integer> getMiscValuesByLevel(int level) {
        return getMiscValuesPerLevel().getOrDefault(level, new HashMap<>());
    }

    public String getString(int level) {
        //Level ở đây bằng Level của Equip + iIncReq .
        int optionLevel = getOptionLevel(level);
        String str = getString();
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '#') {
                String[] split = str.split("#");
                String[] split2 = split[1].split("[% ]");
                String nameValue = split2[0];
                int val = -1;
                for (Map.Entry<ItemOptionType, Integer> e : getMiscValuesByLevel(optionLevel).entrySet()) {
                    if (e.getValue() != 0) {
                        val = e.getValue();
                    }
                }
                for (Map.Entry<BaseStat, Double> e : getStatValuesByLevel(optionLevel).entrySet()) {
                    if (e.getValue() != 0) {
                        val = e.getValue().intValue();
                    }
                }
                str = str.replace("#" + nameValue, val + "");
            }
        }
        return str;
    }

    public int getValue(int level) {
        //Level ở đây bằng Level của Equip + iIncReq .
        int optionLevel = getOptionLevel(level);
        int val = -1;
        for (Map.Entry<ItemOptionType, Integer> e : getMiscValuesByLevel(optionLevel).entrySet()) {
            if (e.getValue() != 0) {
                val = e.getValue();
            }
        }
        for (Map.Entry<BaseStat, Double> e : getStatValuesByLevel(optionLevel).entrySet()) {
            if (e.getValue() != 0) {
                val = e.getValue().intValue();
            }
        }
        return val;
    }

    public String getPotentialTag() {
        String tag = null;
        String str = getString();
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '#') {
                String[] split = str.split("#");
                String[] split2 = split[1].split("[% ]");
                tag = split2[0];
            }
        }
        return tag;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public int getOptionLevel(int rawLevel) {
        int optionLevel = rawLevel / 10;
        if (rawLevel == 0) {
            optionLevel = 1;
        }
        if (rawLevel % 10 > 1) {
            optionLevel++;
        }
        return optionLevel;
    }

    public enum ItemOptionType {
        prop,
        face,
        hpRecoveryOnHit,
        mpRecoveryOnHit,
        attackType,
        level,
        ignoreDam,
        damReflect,
        time
    }
}
