package net.swordie.ms.loaders.Etc.Artifact;

import net.swordie.ms.ServerConstants;
import net.swordie.ms.util.Util;
import net.swordie.ms.util.XMLApi;
import org.w3c.dom.Node;

import java.io.File;
import java.util.List;

public class ArtifactData {

    public enum MissionType {
        Common(0),
        Hunt(1),
        Special(2);

        private final int val;

        MissionType(int val) {
            this.val = val;
        }

        public int getVal() {
            return val;
        }
    }

    public static int[][] settings = {
            {10100, 2500, 1},
            {10200, 2550, 2},
            {10300, 2600, 3},
            {10400, 2650, 4},
            {10500, 2700, 6},
            {10600, 2750, 7},
            {10700, 2800, 8},
            {10800, 2850, 9},
            {10900, 2900, 10},
            {11000, 2950, 12},
            {11100, 3000, 13},
            {11200, 3050, 14},
            {11300, 3100, 15},
            {11400, 3150, 16},
            {11500, 3200, 18},
            {11600, 3250, 19},
            {11700, 3300, 20},
            {11800, 3350, 21},
            {11900, 3400, 22},
            {12000, 3450, 24},
            {12100, 3500, 25},
            {12200, 3550, 26},
            {12300, 3600, 27},
            {12400, 3700, 28},
            {12500, 3800, 30},
            {12600, 3900, 31},
            {12700, 4000, 32},
            {12800, 4500, 33},
            {12900, 5000, 34},
            {13000, 5500, 36},
            {13200, 6000, 37},
            {13400, 6500, 38},
            {13600, 7000, 39},
            {13800, 7500, 40},
            {14000, 8000, 42},
            {14200, 8500, 43},
            {14400, 9000, 44},
            {14600, 9500, 45},
            {14800, 10000, 46},
            {15000, 12000, 48},
            {15200, 14000, 49},
            {15400, 16000, 50},
            {15600, 18000, 51},
            {15800, 20000, 52},
            {16000, 22000, 54},
            {16200, 24000, 55},
            {16400, 26000, 56},
            {16600, 28000, 57},
            {16800, 30000, 58},
            {17000, 50000, 60},
            {17300, 55000, 61},
            {17600, 60000, 62},
            {17900, 65000, 63},
            {18200, 70000, 64},
            {18500, 100000, 66},
            {18800, 110000, 67},
            {19100, 120000, 68},
            {19400, 130000, 69},
            {19700, 200000, 70},
            {20000, 0, 72},
    };

    private static ArtifactInfo info = new ArtifactInfo();

    public static ArtifactInfo getArtifactInfo() {
        return info;
    }

    public static void load() {
        var wzDir = ServerConstants.WZ_DIR + "/Etc.wz/Artifact.img.xml";
        var file = new File(wzDir);
        var node = XMLApi.getAllChildren(XMLApi.getRoot(file)).getFirst();
        List<Node> nodes = XMLApi.getAllChildren(node);
        for (var mainNode : nodes) {
            var mainName = XMLApi.getNamedAttribute(mainNode, "name");
            switch (mainName) {
                case "artifact": {
                    List<Node> artifactNodes = XMLApi.getAllChildren(mainNode);
                    for (var artifactNode : artifactNodes) {
                        var artifactName = XMLApi.getNamedAttribute(artifactNode, "name");
                        switch (artifactName) {
                            case "info":
                                List<Node> infoNodes = XMLApi.getAllChildren(artifactNode);
                                for (var infoNode : infoNodes) {
                                    var infoName = XMLApi.getNamedAttribute(infoNode, "name");
                                    var infoValue = XMLApi.getNamedAttribute(infoNode, "value");
                                    switch (infoName) {
                                        case "extendPeriod":
                                            info.setExtendPeriod(Integer.parseInt(infoValue));
                                            break;
                                        case "extendPoint":
                                            info.setExtendPoint(Integer.parseInt(infoValue));
                                            break;
                                        case "changePoint":
                                            info.setChangePoint(Integer.parseInt(infoValue));
                                            break;
                                        case "resetPoint":
                                            info.setResetPoint(Integer.parseInt(infoValue));
                                            break;
                                        case "maxSlotLevel":
                                            info.setMaxSlotLevel(Integer.parseInt(infoValue));
                                            break;
                                    }
                                }
                                break;
                            case "artifacts":
                                List<Node> artifactsNodes = XMLApi.getAllChildren(artifactNode);
                                for (var artifactsNode : artifactsNodes) {
                                    var key = XMLApi.getNamedAttribute(artifactsNode, "name");
                                    if (Util.isNumber(key)) {
                                        List<Node> artifactsValueNodes = XMLApi.getAllChildren(artifactsNode);
                                        int reqLv = 0;
                                        for (var artifactsValueNode : artifactsValueNodes) {
                                            var ValName = XMLApi.getNamedAttribute(artifactsValueNode, "name");
                                            var ValValue = XMLApi.getNamedAttribute(artifactsValueNode, "value");
                                            if (ValName.equals("reqLv")) {
                                                reqLv = Integer.parseInt(ValValue);
                                            }
                                        }
                                        info.putArtifactReqLv(Integer.parseInt(key), reqLv);
                                    }
                                }
                                break;
                            case "slot":
                                List<Node> slotNodes = XMLApi.getAllChildren(artifactNode);
                                for (var slotNode : slotNodes) {
                                    var key = XMLApi.getNamedAttribute(slotNode, "name");
                                    if (Util.isNumber(key)) {
                                        List<Node> slotValNodes = XMLApi.getAllChildren(slotNode);
                                        var skillLevelPlus = 0;
                                        var enforceSlotPoint = 0;
                                        for (var slotValNode : slotValNodes) {
                                            var ValName = XMLApi.getNamedAttribute(slotValNode, "name");
                                            var ValValue = XMLApi.getNamedAttribute(slotValNode, "value");
                                            if (ValName.equals("skillLevelPlus")) {
                                                skillLevelPlus = Integer.parseInt(ValValue);
                                            } else if (ValName.equals("enforceSlotPoint")) {
                                                enforceSlotPoint = Integer.parseInt(ValValue);
                                            }
                                        }
                                        info.putSlotInfo(Integer.parseInt(key), skillLevelPlus, enforceSlotPoint);
                                    }
                                }
                                break;
                            case "stat":
                                List<Node> statNodes = XMLApi.getAllChildren(artifactNode);
                                for (var statNode : statNodes) {
                                    var key = XMLApi.getNamedAttribute(statNode, "name");
                                    if (Util.isNumber(key)) {
                                        List<Node> statValNodes = XMLApi.getAllChildren(statNode);
                                        var skillCode = 0;
                                        for (Node statValNode : statValNodes) {
                                            var ValName = XMLApi.getNamedAttribute(statValNode, "name");
                                            var ValValue = XMLApi.getNamedAttribute(statValNode, "value");
                                            if (ValName.equals("skillCode")) {
                                                skillCode = Integer.parseInt(ValValue);
                                            }
                                        }
                                        info.putStatSkillCode(Integer.parseInt(key), skillCode);
                                    }
                                }
                                break;
                        }
                    }
                    break;
                }
                case "commonMission": {
                    List<Node> commonNodes = XMLApi.getAllChildren(mainNode);
                    for (var commonNode : commonNodes) {
                        var key = XMLApi.getNamedAttribute(commonNode, "name");
                        if (Util.isNumber(key)) {
                            List<Node> commonValNodes = XMLApi.getAllChildren(commonNode);
                            var commonMission = new ArtifactInfo.CommonMission();
                            for (var commonValNode : commonValNodes) {
                                var ValName = XMLApi.getNamedAttribute(commonValNode, "name");
                                var ValValue = XMLApi.getNamedAttribute(commonValNode, "value");
                                switch (ValName) {
                                    case "name":
                                        commonMission.setName(ValValue);
                                        break;
                                    case "desc":
                                        commonMission.setDesc(ValValue);
                                        break;
                                    case "type":
                                        commonMission.setType(ValValue);
                                        break;
                                    case "artifactPoint":
                                        commonMission.setArtifactPoint(Integer.parseInt(ValValue));
                                        break;
                                    case "artifactExp":
                                        commonMission.setArtifactExp(Integer.parseInt(ValValue));
                                        break;
                                    case "check":
                                        List<Node> checkNodes = XMLApi.getAllChildren(commonValNode);
                                        for (var checkNode : checkNodes) {
                                            var checkName = XMLApi.getNamedAttribute(checkNode, "name");
                                            var checkValue = XMLApi.getNamedAttribute(checkNode, "value");
                                            if (checkName.equals("value")) {
                                                commonMission.setValue(Integer.parseInt(checkValue));
                                            }
                                        }
                                        break;
                                }
                            }
                            info.putCommonMission(Integer.parseInt(key), commonMission);
                        }
                    }
                    break;
                }
                case "huntMission": {
                    List<Node> huntNodes = XMLApi.getAllChildren(mainNode);
                    var huntMission = new ArtifactInfo.HuntMission();
                    for (var huntNode : huntNodes) {
                        var name = XMLApi.getNamedAttribute(huntNode, "name");
                        var value = XMLApi.getNamedAttribute(huntNode, "value");
                        switch (name) {
                            case "maxCount":
                                huntMission.setMaxCount(Integer.parseInt(value));
                                break;
                            case "rankReward":
                                List<Node> rankNodes = XMLApi.getAllChildren(huntNode);
                                for (var rankNode : rankNodes) {
                                    var key = XMLApi.getNamedAttribute(rankNode, "name");
                                    if (Util.isNumber(key)) {
                                        List<Node> rankValNodes = XMLApi.getAllChildren(rankNode);
                                        var rankReward = new ArtifactInfo.HuntMission.RankReward();
                                        for (var rankValNode : rankValNodes) {
                                            var ValName = XMLApi.getNamedAttribute(rankValNode, "name");
                                            var ValValue = XMLApi.getNamedAttribute(rankValNode, "value");
                                            switch (ValName) {
                                                case "artifactPoint":
                                                    rankReward.setArtifactPoint(Integer.parseInt(ValValue));
                                                    break;
                                                case "artifactExp":
                                                    rankReward.setArtifactExp(Integer.parseInt(ValValue));
                                                    break;
                                            }
                                        }
                                        huntMission.putRankReward(Integer.parseInt(key), rankReward);
                                    }
                                }
                                break;
                            case "mission":
                                List<Node> missionNodes = XMLApi.getAllChildren(huntNode);
                                for (var missionNode : missionNodes) {
                                    var key = XMLApi.getNamedAttribute(missionNode, "name");
                                    if (Util.isNumber(key)) {
                                        List<Node> missionValNodes = XMLApi.getAllChildren(missionNode);
                                        var mission = new ArtifactInfo.HuntMission.Mission();
                                        for (var missionValNode : missionValNodes) {
                                            var ValName = XMLApi.getNamedAttribute(missionValNode, "name");
                                            var ValValue = XMLApi.getNamedAttribute(missionValNode, "value");
                                            switch (ValName) {
                                                case "name":
                                                    mission.setName(ValValue);
                                                    break;
                                                case "desc":
                                                    mission.setDesc(ValValue);
                                                    break;
                                                case "rank":
                                                    mission.setRank(Integer.parseInt(ValValue));
                                                    break;
                                                case "mobID":
                                                    mission.setMobID(Integer.parseInt(ValValue));
                                                    break;
                                            }
                                        }
                                        huntMission.putMission(Integer.parseInt(key), mission);
                                    }
                                }
                                break;
                        }
                    }
                    info.setHuntMission(huntMission);
                    break;
                }
                case "specialMission": {
                    List<Node> specialNodes = XMLApi.getAllChildren(mainNode);
                    for (var specialNode : specialNodes) {
                        var key = XMLApi.getNamedAttribute(specialNode, "name");
                        if (Util.isNumber(key)) {
                            List<Node> specialValNodes = XMLApi.getAllChildren(specialNode);
                            var specialMission = new ArtifactInfo.SpecialMission();
                            for (var specialValNode : specialValNodes) {
                                var ValName = XMLApi.getNamedAttribute(specialValNode, "name");
                                var ValValue = XMLApi.getNamedAttribute(specialValNode, "value");
                                switch (ValName) {
                                    case "name":
                                        specialMission.setName(ValValue);
                                        break;
                                    case "desc":
                                        specialMission.setDesc(ValValue);
                                        break;
                                    case "type":
                                        specialMission.setType(ValValue);
                                        break;
                                    case "artifactPoint":
                                        specialMission.setArtifactPoint(Integer.parseInt(ValValue));
                                        break;
                                    case "artifactExp":
                                        specialMission.setArtifactExp(Integer.parseInt(ValValue));
                                        break;
                                    case "check":
                                        List<Node> checkNodes = XMLApi.getAllChildren(specialValNode);
                                        var cond = new ArtifactInfo.MissionCond();
                                        for (var checkNode : checkNodes) {
                                            var checkName = XMLApi.getNamedAttribute(checkNode, "name");
                                            var checkValue = XMLApi.getNamedAttribute(checkNode, "value");
                                            switch (checkName) {
                                                case "itemID":
                                                case "quest":
                                                    cond.setQuestId(Integer.parseInt(checkValue));
                                                    break;
                                                case "key":
                                                    cond.setKey(checkValue);
                                                    break;
                                                case "value":
                                                    cond.setRequiredValue(Integer.parseInt(checkValue));
                                                    break;
                                            }
                                            specialMission.getConditions().add(cond);
                                        }
                                        break;
                                }
                                specialMission.setIndex(Integer.parseInt(key));
                                info.putSpecialMission(specialMission.getIndex(), specialMission);
                            }
                        }
                    }
                    break;
                }
            }
        }
    }
}
