package net.swordie.ms.constants;

import java.util.*;

public class HexaMatrixConstants {

    public static int jobAdvanceQuest = 1488;

    public static int solErda = 1489;

    public static int[] solErdaFragments = new int[]{
            4009548, // Untrablable
            4009547,
            4009550
    };

    public static int[][] erdaConversions = new int[][]{
            {2638879,30},
            // Converting 1 Sol Erda using Sol Erda Conversion (Rewards Faint Sol Erda Energy x 30)
            {2638603,1},
            // HEXA Booster: 	Converting 1 Sol Erda using Sol Erda Conversion (Rewards HEXA Booster x 1)
    };

    public static int getHexaStatByCoreID(int coreID) {
        switch (coreID) {
            case 50000000: // Stat Node I
                return 0;
            case 50000001: // Stat Node II
                return 1;
            case 50000002: // Stat Node III
                return 2;
        }
        return 0;
    }

    public static boolean isSolErdaEnergy(int itemID) {
        return isFaintSolErdaEnergy(itemID) || isCommonSolErdaEnergy(itemID) || isDenseSolErdaEnergy(itemID);
    }

    public static boolean isFaintSolErdaEnergy(int itemID) {
        return itemID == 2636420 // Untrablable | runOnPickup | consume_2636420
                || itemID == 2637468 // consume_2637468
                || itemID == 2638208 // consume_2638208
                || itemID == 2636953 // Untrablable | runOnPickup | consume_2636953
                || itemID == 2636954 // Untrablable | runOnPickup | consume_2636954
                || itemID == 2638879 // consume_2638879
                ;
    }

    public static boolean isCommonSolErdaEnergy(int itemID) {
        return itemID == 2636421 // Untrablable | runOnPickup | consume_2636421 | Sol Erda Energy: +200
                || itemID == 2637469 // consume_2637469 | Sol Erda Energy: +200
                || itemID == 2638391 // consume_2638391 | Sol Erda Energy: +200
                || itemID == 2636883 // Untrablable | consume_2636883  | Sol Erda Energy: +200
                || itemID == 2636955 // Untrablable | runOnPickup | consume_2636955  | Sol Erda Energy: +200
                || itemID == 2638456 // consume_2638456 | Sol Erda Energy: +200
                ;
    }

    public static boolean isDenseSolErdaEnergy(int itemID) {
        return itemID == 2636422 // Untrablable | runOnPickup | consume_2636422
                || itemID == 2638231 // Untrablable | consume_2638231
                || itemID == 2638392 // consume_2638392
                || itemID == 2636884 // Untrablable | runOnPickup | consume_2636884
                || itemID == 2636956 // Untrablable | runOnPickup | consume_2636956
                ;
    }

    public static Set<HexaStatCore> statCores = new java.util.LinkedHashSet<>(); // insertion order 50000000..02: HashSet used identity hashes, so the LP0272 order changed with unrelated class loading

    static {
        init();
    }

    public static void init() {
        HexaStatCore statNodeI = new HexaStatCore(50000000, 5, 10);
        for (int level = 10; level <= 30; level++) {
            statNodeI.coreCosts10To30.add(new HexaStatCore.HexaStatCoreCost(level, 10000000L));
        }
        for (int level = 0; level <= 30; level++) {
            statNodeI.coreCosts0to30.add(new HexaStatCore.HexaStatCoreCost(level, 100000000L));
        }
        statNodeI.coreCosts10To30.sort(Comparator.comparingInt(a -> a.level));
        statNodeI.coreCosts0to30.sort(Comparator.comparingInt(a -> a.level));
        statCores.add(statNodeI);

        HexaStatCore statNodeII = new HexaStatCore(50000001, 10, 200);
        for (int level = 10; level <= 30; level++) {
            statNodeII.coreCosts10To30.add(new HexaStatCore.HexaStatCoreCost(level, 20000000L));
        }
        for (int level = 0; level <= 30; level++) {
            statNodeII.coreCosts0to30.add(new HexaStatCore.HexaStatCoreCost(level, 100000000L));
        }
        statNodeII.coreCosts10To30.sort(Comparator.comparingInt(a -> a.level));
        statNodeII.coreCosts0to30.sort(Comparator.comparingInt(a -> a.level));
        statCores.add(statNodeII);

        HexaStatCore statNodeIII = new HexaStatCore(50000002, 15, 350);
        for (int level = 10; level <= 30; level++) {
            statNodeIII.coreCosts10To30.add(new HexaStatCore.HexaStatCoreCost(level, 35000000L));
        }
        for (int level = 0; level <= 30; level++) {
            statNodeIII.coreCosts0to30.add(new HexaStatCore.HexaStatCoreCost(level, 100000000L));
        }
        statNodeIII.coreCosts10To30.sort(Comparator.comparingInt(a -> a.level));
        statNodeIII.coreCosts0to30.sort(Comparator.comparingInt(a -> a.level));
        statCores.add(statNodeIII);
    }

    public static class HexaStatCore {
        public int id;
        public int solErdaCostToUnlock;
        public int solErdaFragmentCostToUnlock;
        public List<HexaStatCoreCost> coreCosts10To30 = new ArrayList<>();
        public List<HexaStatCoreCost> coreCosts0to30 = new ArrayList<>();

        public HexaStatCore(int id, int solErdaToUnlock, int solErdaFragmentToUnlock) {
            this.id = id;
            this.solErdaCostToUnlock = solErdaToUnlock;
            this.solErdaFragmentCostToUnlock = solErdaFragmentToUnlock;
        }

        public static class HexaStatCoreCost {
            public int level;
            public long cost;

            public HexaStatCoreCost(int level, long cost) {
                this.level = level;
                this.cost = cost;
            }
        }

    }
    private static final int[] skillNodeUpgradeSolErdaCost = {
            1, 1, 1, 2, 2, 2, 3, 3, 10, 3, 3, 4, 4, 4, 4, 4, 4, 5, 15, 5, 5, 5, 5, 5, 6, 6, 6, 7, 20
    };
    private static final int[] skillNodeUpgradeSolErdaFragmentCost = {
            30, 35, 40, 45, 50, 55, 60, 65, 200, 80, 90, 100, 110, 120, 130, 140, 150, 160, 350, 170, 180, 190, 200, 210, 220, 230, 240, 250, 500
    };
    private static final int[] masteryNodeUpgradeSolErdaCost = {
            1, 1, 1, 1, 1, 1, 2, 2, 5, 2, 2, 2, 2, 2, 2, 2, 2, 3, 8, 3, 3, 3, 3, 3, 3, 3, 3, 4, 10
    };
    private static final int[] masteryNodeUpgradeSolErdaFragmentCost = {
            15, 18, 20, 23, 25, 28, 30, 33, 100, 40, 45, 50, 55, 60, 65, 70, 75, 80, 175, 85, 90, 95, 100, 105, 110, 115, 120, 125, 250
    };
    private static final int[] boostNodeUpgradeSolErdaCost = {
            1,1,1,2,2,2,3,3,8,3,3,3,3,3,3,3,3,4,12,4,4,4,4,4,5,5,5,6,15
    };
    private static final int[] boostNodeUpgradeSolErdaFragmentCost = {
            23,27,30,34,38,42,45,49,150,60,68,75,83,90,98,105,113,120,263,128,135,143,150,158,165,173,180,188,375
    };
    private static final int[] commonNodeUpgradeSolErdaCost = {
            2,2,2,3,3,3,5,5,14,5,5,6,6,6,6,6,6,7,17,7,7,7,7,7,9,9,9,10,20
    };
    private static final int[] commonNodeUpgradeSolErdaFragmentCost = {
            38,44,50,57,63,69,75,82,300,110,124,138,152,165,179,193,207,220,525,234,248,262,275,289,303,317,330,344,750
    };
    public static final int[] hexaStatUpgradeSolErdaCost = {
            0,0,0,0,0,0,0,0,0,0,0
    };
    public static final int[] hexaStatUpgradeSolErdaFragmentCost = {
            10,10,10,20,20,20,20,30,40,50,50
    };
    public static final double[] hexaStatUpgradeWeight = {
            0.35,0.35,0.35,0.20,0.20,0.20,0.20,0.15,0.10,0.05
    };
    public static int solErdaStrengthMax = 1000;
    public static int solErdaMax = 20;
    public static final Map<Integer, List<Integer>> sixthJobSkillCore = new HashMap<>();
    public static final Map<Integer, List<Integer>> sixthJobMasteryCore = new HashMap<>();
    public static final Map<Integer, List<Integer>> sixthJobBoostCore = new HashMap<>();
    public static final Map<Integer, List<Integer>> sixthJobCommonCore = new HashMap<>();
    public static final Map<Integer, List<Integer>> linkedCoreSkill = new HashMap<>();
    public static final Map<Integer, List<Map<Integer, Integer>>> reqForActivation = new HashMap<>();
    private static final Map<Integer, String> coreName = new HashMap<>();
    private static final Map<Integer, String> coreDesc = new HashMap<>();
    private static final Map<HexaMatrixConstants.HexaStatOption, Map<Integer, Double>> mainHexaStatValue = new EnumMap<>(
            HexaMatrixConstants.HexaStatOption.class
    );
    private static final Map<HexaMatrixConstants.HexaStatOption, Map<Integer, Double>> subHexaStatValue = new EnumMap<>(HexaMatrixConstants.HexaStatOption.class);

    public static int getHexaStatCoreIdByIndex(int index) {
        return 50000000;
    }

    public static int getHexaStatIndexByCoreId(int coreid) {
        return 0;
    }

    public static HexaMatrixSkill[] getHexaMatrixSkillTypes() {
        return new HexaMatrixSkill[]{
                HexaMatrixSkill.SKILL_CORE,
                HexaMatrixSkill.MASTERY_CORE,
                HexaMatrixSkill.BOOST_CORE,
                HexaMatrixSkill.COMMON_CORE
        };
    }

    public static int getSolErdaCostToActivate(HexaMatrixSkill flag) {
        int need = 5;
        switch (flag) {
            case SKILL_CORE:
                need = 5;
                break;
            case MASTERY_CORE:
                need = 3;
                break;
            case BOOST_CORE:
                need = 4;
                break;
            case COMMON_CORE:
                need = 7;
                break;
            case HEXA_STAT:
                need = 5;
        }

        return need;
    }

    public static int getSolErdaFragmentCostToActivate(HexaMatrixSkill flag) {
        int need = 100;
        switch (flag) {
            case SKILL_CORE:
                need = 100;
                break;
            case MASTERY_CORE:
                need = 50;
                break;
            case BOOST_CORE:
                need = 75;
                break;
            case COMMON_CORE:
                need = 125;
                break;
            case HEXA_STAT:
                need = 10;
                break;
        }

        return need;
    }

    public static int getSolErdaCostToUpgrade(HexaMatrixSkill skillType, int level) {
        int[] req = null;
        switch (skillType) {
            case SKILL_CORE:
                req = skillNodeUpgradeSolErdaCost;
                break;
            case MASTERY_CORE:
                req = masteryNodeUpgradeSolErdaCost;
                break;
            case BOOST_CORE:
                req = boostNodeUpgradeSolErdaCost;
                break;
            case COMMON_CORE:
                req = commonNodeUpgradeSolErdaCost;
                break;
            case HEXA_STAT:
                req = hexaStatUpgradeSolErdaCost;
                level++;
        }

        return req != null && req.length >= level ? req[level - 1] : 20;
    }

    public static int getSolErdaFragmentCostToUpgrade(HexaMatrixSkill flag, int level) {
        int[] req = null;
        switch (flag) {
            case SKILL_CORE:
                req = skillNodeUpgradeSolErdaFragmentCost;
                break;
            case MASTERY_CORE:
                req = masteryNodeUpgradeSolErdaFragmentCost;
                break;
            case BOOST_CORE:
                req = boostNodeUpgradeSolErdaFragmentCost;
                break;
            case COMMON_CORE:
                req = commonNodeUpgradeSolErdaFragmentCost;
                break;
            case HEXA_STAT:
                req = hexaStatUpgradeSolErdaFragmentCost;
                level++;
        }

        return req != null && req.length >= level ? req[level - 1] : 20;
    }

    public static int getHexaSkillMasterLevel(HexaMatrixSkill flag) {
        int[] needs = null;
        switch (flag) {
            case SKILL_CORE:
                needs = skillNodeUpgradeSolErdaFragmentCost;
                break;
            case MASTERY_CORE:
                needs = masteryNodeUpgradeSolErdaFragmentCost;
                break;
            case BOOST_CORE:
                needs = boostNodeUpgradeSolErdaFragmentCost;
                break;
            case COMMON_CORE:
                needs = commonNodeUpgradeSolErdaFragmentCost;
                break;
            case HEXA_STAT:
                needs = hexaStatUpgradeSolErdaFragmentCost;
        }

        return needs == null ? 30 : needs.length;
    }

    public static int getHexaStatMasterLevel() {
        return hexaStatUpgradeWeight.length;
    }

    public static double getHexaStatWeight(int level) {
        return level >= hexaStatUpgradeWeight.length ? 0.0 : hexaStatUpgradeWeight[level];
    }

    public static double getHexaStatMainValue(HexaMatrixConstants.HexaStatOption opt, int level) {
        if (mainHexaStatValue.get(opt) == null) {
            return 0.0;
        } else {
            return mainHexaStatValue.get(opt).get(level) == null ? 0.0 : mainHexaStatValue.get(opt).get(level);
        }
    }

    public static double getHexaStatSubValue(HexaMatrixConstants.HexaStatOption opt, int level) {
        if (subHexaStatValue.get(opt) == null) {
            return 0.0;
        } else {
            return subHexaStatValue.get(opt).get(level) == null ? 0.0 : subHexaStatValue.get(opt).get(level);
        }
    }

    public static Map<Integer, String> searchCoreName(String search) {
        Map<Integer, String> result = new HashMap<>();

        for (Map.Entry<Integer, String> entry : coreName.entrySet()) {
            if (entry.getValue().contains(search)) {
                String name = entry.getValue();
                String desc = coreDesc.get(entry.getKey());
                result.put(entry.getKey(), name + " : " + desc);
            }
        }

        return result;
    }

    public static int getNeedSolErdaPieceToUpgradeMainHexaStat(int level) {
        switch (level) {
            case 0:
            case 1:
            case 2:
                return 10;
            case 3:
            case 4:
            case 5:
            case 6:
                return 20;
            case 7:
            case 8:
                return 30;
            case 9:
                return 50;
            default:
                return 50;
        }
    }

    public static List<Integer> getAllJobCores(int job) {
        List<Integer> list = new ArrayList<>();
        if (sixthJobSkillCore.get(job) != null) {
            list.addAll(sixthJobSkillCore.get(job));
        }
        if (sixthJobMasteryCore.get(job) != null) {
            list.addAll(sixthJobMasteryCore.get(job));
        }
        if (sixthJobBoostCore.get(job) != null) {
            list.addAll(sixthJobBoostCore.get(job));
        }
        if (sixthJobCommonCore.get(job) != null) {
            list.addAll(sixthJobCommonCore.get(job));
        }
        return list;
    }

    public static List<Integer> getOriginSkillCores(int job) {
        List<Integer> list = new ArrayList<>();
        if (sixthJobSkillCore.get(job) != null) {
            list.addAll(sixthJobSkillCore.get(job));
        }

        return list;
    }

    public static int searchCoreIdBySkill(int skillId) {
        for (Map.Entry<Integer, List<Integer>> entry : linkedCoreSkill.entrySet()) {
            if (entry.getValue().contains(skillId)) {
                return entry.getKey();
            }
        }

        return 0;
    }

    public static boolean isAllowCooldownSkill(int jobId, int skillId) {
        List<Integer> coreList = new ArrayList<>();
        if (sixthJobBoostCore.get(jobId) != null) {
            coreList.addAll(sixthJobBoostCore.get(jobId));
        }
        if (sixthJobMasteryCore.containsKey(jobId)) {
            coreList.addAll(sixthJobMasteryCore.get(jobId));
        }
        for (int core : coreList) {
            List<Integer> coreSkillList = linkedCoreSkill.get(core);
            if (coreSkillList != null) {
                for (int skill : coreSkillList) {
                    if (skill == skillId) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public enum HexaMatrixSkill {
        HEXA_STAT(-1),
        SKILL_CORE(0),
        MASTERY_CORE(1),
        BOOST_CORE(2),
        COMMON_CORE(3),
        ;

        final int type;

        HexaMatrixSkill(int type) {
            this.type = type;
        }

        public int getType() {
            return this.type;
        }
    }

    public static enum HexaMatrixMsg {
        Success(0),
        UnknownError(1),
        InvalidRequest(2),
        InvalidCharacterStat(3),
        NotEnoughSolErda(4),
        NotEnoughMeso(5),
        ErrorOccuredOnIncMoney(6),
        NotEnoughToReqSkill(7),
        TooManyChangeInfo(8),
        BlockedByLiveValue(10);

        int type;

        private HexaMatrixMsg(int type) {
            this.type = type;
        }

        public int getType() {
            return this.type;
        }
    }

    public static enum HexaStatOption {
        CRITICAL_DAMAGE(0, "cdPerM"),
        BOSS_DAMAGE(1, "bdRPerM"),
        IGNORE_DEFENSE(2, "ignoreMobpdpRPerM"),
        INCREASE_DAMAGE(3, "damRPerM"),
        INCREASE_PAD(4, "padX"),
        INCREASE_MAD(5, "madX"),
        INCREASE_MAINSTAT(6, "indieStat");

        int type;
        String wzValueName;

        private HexaStatOption(int type, String wzValueName) {
            this.type = type;
            this.wzValueName = wzValueName;
        }

        public int getType() {
            return this.type;
        }

        public String getWzValueName() {
            return this.wzValueName;
        }

        public static HexaMatrixConstants.HexaStatOption findByValue(int type) {
            for (HexaMatrixConstants.HexaStatOption opt : values()) {
                if (opt.getType() == type) {
                    return opt;
                }
            }

            return null;
        }
    }
}
