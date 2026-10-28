package net.swordie.ms.handlers.ui;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.matrix.MatrixCore;
import net.swordie.ms.client.character.skills.matrix.MatrixSlot;
import net.swordie.ms.client.character.skills.matrix.NodeEnhance;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.MatrixConstants;
import net.swordie.ms.enums.MatrixStateType;
import net.swordie.ms.enums.MatrixUpdateType;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.loaders.Etc.VCore.VCore;
import net.swordie.ms.loaders.Etc.VCore.VCoreData;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.scripts.ScriptType;
import net.swordie.ms.util.Randomizer;
import net.swordie.ms.util.container.Tuple;

import java.util.*;

public class MatrixHandler {

    @Handler(op = InHeader.NODE_STONE_CRAFTING)
    public static void handleNodeStoneCrafting(Char chr, InPacket inPacket) {
        chr.getScriptManager().startScript(chr, 0, 0, "Craft_NodeStone", ScriptType.Npc);
    }

    @Handler(op = InHeader.UPDATE_MATRIX)
    public static void handleUpdateMatrix(Char chr, InPacket inPacket) {
        List<MatrixCore> sortedMatrixCores = chr.getSortedMatrixCores();
        MatrixCore core;
        MatrixSlot slot;
        int action = inPacket.decodeInt();
        MatrixUpdateType type = MatrixUpdateType.getUpdateTypeByVal(action);
        switch (type) {
            case Activate:
                int nodeId = inPacket.decodeInt();
                int otherNodeId = inPacket.decodeInt(); // if there's a node already on the Position
                int otherNodeId_toPos = inPacket.decodeInt(); // toPosition of the nodeID that will be force moved
                int pos = inPacket.decodeInt();
                boolean byDrag = inPacket.decodeByte() != 0;
                if (nodeId >= sortedMatrixCores.size() || pos >= chr.getMaxMatrixSlots()) {
                    chr.dispose();
                    return;
                }
                if (!byDrag && pos < 0) {
                    pos = chr.getFirstOpenMatrixSlot(); // Check  Locked Slots
                }
                if (pos == -1337) {
                    chr.chatMessage("沒有空的核心欄位。");
                    return;
                }
                core = sortedMatrixCores.get(nodeId);
                slot = chr.getMatrixSlotByPosition(pos);
                if (core == null || slot == null) {
                    chr.chatPopup("發生未知錯誤。");
                    return;
                }
                for (MatrixCore existCore : chr.getMatrixCore().stream().filter(MatrixCore::isActive).toList()) {
                    if (!VCore.isBoostNode(existCore.getCoreID())) {
                        if (existCore.getSkillID1() != 0) {
                            if (existCore.getSkillID1() == core.getSkillID1()
                                    || existCore.getSkillID1() == core.getSkillID2()
                                    || existCore.getSkillID1() == core.getSkillID3()) {
                                chr.chatPopup(String.format("該核心包含你已激活的技能 %s。", StringData.getSkillStringById(existCore.getSkillID1()).getName()));
                                return;
                            }
                        }
                        if (existCore.getSkillID2() != 0) {
                            if (existCore.getSkillID2() == core.getSkillID1()
                                    || existCore.getSkillID2() == core.getSkillID2()
                                    || existCore.getSkillID2() == core.getSkillID3()) {
                                chr.chatPopup(String.format("該核心包含你已激活的技能 %s。", StringData.getSkillStringById(existCore.getSkillID2()).getName()));
                                return;
                            }
                        }
                        if (existCore.getSkillID3() != 0) {
                            if (existCore.getSkillID3() == core.getSkillID1()
                                    || existCore.getSkillID3() == core.getSkillID2()
                                    || existCore.getSkillID3() == core.getSkillID3()) {
                                chr.chatPopup(String.format("該核心包含你已激活的技能 %s。", StringData.getSkillStringById(existCore.getSkillID3()).getName()));
                                return;
                            }
                        }
                    }
                }
                if (otherNodeId >= 0) {
                    final MatrixCore prev = sortedMatrixCores.get(otherNodeId);
                    if (prev.getSlot() == -1 || prev.getState() == MatrixStateType.INACTIVE) {
                        chr.chatPopup("發生未知錯誤。");
                    } else {
                        if (otherNodeId_toPos < 0) {
                            prev.setState(MatrixStateType.INACTIVE);
                            prev.setSlot(otherNodeId_toPos);
                            setNodeSkill(chr, prev, MatrixUpdateType.Deactivate);
                        } else {
                            prev.setSlot(otherNodeId_toPos);
                        }
                        core.setState(MatrixStateType.ACTIVE);
                        core.setSlot(pos);
                        setNodeSkill(chr, core, MatrixUpdateType.Activate);
                    }
                } else if (core.getSlot() >= 0 || core.getState() == MatrixStateType.ACTIVE) {
                    chr.chatPopup("發生未知錯誤。");
                } else {
                    core.setState(MatrixStateType.ACTIVE);
                    core.setSlot(pos);
                    setNodeSkill(chr, core, MatrixUpdateType.Activate);
                }
                chr.write(WvsContext.updateVMatrix(chr, true, type.getVal(), nodeId));
                break;
            case Deactivate:
                nodeId = inPacket.decodeInt();
                pos = inPacket.decodeInt();
                core = sortedMatrixCores.get(nodeId);
                if (core == null) {
                    chr.chatPopup("發生未知錯誤。");
                    return;
                }
                if (core.isLock()) {
                    chr.dispose();
                    return;
                }
                slot = chr.getMatrixSlotByPosition(core.getSlot());
                if (slot == null) {
                    chr.chatPopup("發生未知錯誤。");
                    return;
                }
                if (core.getSlot() == -1 || core.getState() == MatrixStateType.INACTIVE) {
                    chr.chatPopup("發生未知錯誤。");
                } else {
                    core.setState(MatrixStateType.INACTIVE);
                    core.setSlot(-1);
                    setNodeSkill(chr, core, MatrixUpdateType.Deactivate);
                }
                chr.write(WvsContext.updateVMatrix(chr, true, type.getVal(), nodeId));
                break;
            case Swap:
                int switcherId = inPacket.decodeInt(); // the main node that is dragged
                int switchedId = inPacket.decodeInt(); // one that is automatically swapped as well
                int fromPos = inPacket.decodeInt();
                int toPos = inPacket.decodeInt();
                if (switcherId >= 0 && switcherId < sortedMatrixCores.size() && toPos < chr.getMaxMatrixSlots()) {
                    MatrixCore mc = sortedMatrixCores.get(switcherId);
                    if (mc != null) {
                        if (mc.isLock()) {
                            chr.dispose();
                            return;
                        }
                        if (switchedId >= 0 && switchedId < sortedMatrixCores.size()) {
                            MatrixCore otherCore = sortedMatrixCores.get(switchedId);
                            if (otherCore != null) {
                                if (otherCore.isLock()) {
                                    chr.dispose();
                                    return;
                                }
                                setNodeSkill(chr, otherCore, MatrixUpdateType.Deactivate);
                                otherCore.setSlot(fromPos);
                                setNodeSkill(chr, otherCore, MatrixUpdateType.Activate);
                            }
                        }
                        setNodeSkill(chr, mc, MatrixUpdateType.Deactivate);
                        mc.setSlot(toPos);
                        setNodeSkill(chr, mc, MatrixUpdateType.Activate);
                    }
                    chr.write(WvsContext.updateVMatrix(chr, true, type.getVal(), switcherId));
                }
                break;
            case Enhance: {
                nodeId = inPacket.decodeInt();
                int size = inPacket.decodeInt();
                core = sortedMatrixCores.get(nodeId);
                if (core == null) {
                    chr.chatPopup("發生未知錯誤。");
                    chr.dispose();
                    return;
                }
                Set<MatrixCore> removeCores = new HashSet<>();
                for (int i = 0; i < size; i++) {
                    int fromPos_ = inPacket.decodeInt();
                    MatrixCore targetCore = sortedMatrixCores.get(fromPos_);
                    if (targetCore != null) {
                        if (!targetCore.isActive() && targetCore.getCoreID() == core.getCoreID()) {
                            removeCores.add(targetCore);
                        }
                    }
                }
                if (removeCores.isEmpty()) {
                    chr.chatPopup("[未知錯誤]\r\n請再試一次。");
                    return;
                }
                int exp = 0;
                final int currentCoreLevel = core.getSkillLevel();
                for (MatrixCore removeCore : removeCores) {
                    exp += VCore.getEnforceOption(VCore.getCore(removeCore.getCoreID()).getType()).get(removeCore.getSkillLevel()).getEnforceExp();
                }
                int nextExp = VCore.getEnforceOption(VCore.getCore(core.getCoreID()).getType()).get(currentCoreLevel).getNextExp();
                core.setExperience(core.getExperience() + exp);
                boolean changeLevel = false;
                while (core.getExperience() >= nextExp) {
                    core.setSkillLevel(core.getSkillLevel() + 1);
                    core.setExperience(core.getExperience() - nextExp);
                    if (core.getSkillLevel() == core.getMaxLevel()) {
                        core.setExperience(0);
                    }
                    nextExp = VCore.getEnforceOption(VCore.getCore(core.getCoreID()).getType()).get(core.getSkillLevel()).getNextExp();
                    changeLevel = true;
                }
                if (changeLevel) {
                    relocCoreSkill(chr, core);
                }
                for (MatrixCore remove : removeCores) {
                    remove.setState(MatrixStateType.DISASSEMBLED);
                }
                var enhance = new NodeEnhance();
                enhance.pos = nodeId;
                enhance.expGained = exp;
                enhance.oldSlv = currentCoreLevel;
                enhance.newSlv = core.getSkillLevel();
                chr.write(WvsContext.nodeEnhanceResult(enhance));
                chr.write(WvsContext.updateVMatrix(chr, true, type.getVal(), -1));
                break;
            }
            case EnhanceAll: {
                int size0 = inPacket.decodeInt();
                int unk = inPacket.decodeInt();
                List<NodeEnhance> enhances = new ArrayList<>(size0);
                for (int x = 0; x < size0; x++) {
                    nodeId = inPacket.decodeInt();
                    int size = inPacket.decodeInt();
                    core = sortedMatrixCores.get(nodeId);
                    if (core == null) {
                        continue;
                    }
                    Set<MatrixCore> removeCores = new HashSet<>();
                    for (int i = 0; i < size; i++) {
                        int fromPos_ = inPacket.decodeInt();
                        MatrixCore targetCore = sortedMatrixCores.get(fromPos_);
                        if (targetCore != null) {
                            if (!targetCore.isActive() && targetCore.getCoreID() == core.getCoreID()) {
                                removeCores.add(targetCore);
                            }
                        }
                    }
                    if (removeCores.isEmpty()) {
                        continue;
                    }
                    int exp = 0;
                    final int currentCoreLevel = core.getSkillLevel();
                    for (MatrixCore removeCore : removeCores) {
                        exp += VCore.getEnforceOption(VCore.getCore(removeCore.getCoreID()).getType()).get(removeCore.getSkillLevel()).getEnforceExp();
                    }
                    int nextExp = VCore.getEnforceOption(VCore.getCore(core.getCoreID()).getType()).get(currentCoreLevel).getNextExp();
                    core.setExperience(core.getExperience() + exp);
                    boolean changeLevel = false;
                    while (core.getExperience() >= nextExp) {
                        core.setSkillLevel(core.getSkillLevel() + 1);
                        core.setExperience(core.getExperience() - nextExp);
                        if (core.getSkillLevel() == core.getMaxLevel()) {
                            core.setExperience(0);
                        }
                        nextExp = VCore.getEnforceOption(VCore.getCore(core.getCoreID()).getType()).get(core.getSkillLevel()).getNextExp();
                        changeLevel = true;
                    }
                    if (changeLevel) {
                        relocCoreSkill(chr, core);
                    }
                    for (MatrixCore remove : removeCores) {
                        remove.setState(MatrixStateType.DISASSEMBLED);
                    }
                    var enhance = new NodeEnhance();
                    enhance.pos = nodeId;
                    enhance.expGained = exp;
                    enhance.oldSlv = currentCoreLevel;
                    enhance.newSlv = core.getSkillLevel();
                    enhances.add(enhance);
                }
                chr.write(WvsContext.nodeEnhanceResultInBulk(enhances));
                chr.write(WvsContext.updateVMatrix(chr, true, type.getVal(), 0));
                break;
            }
            case Disassemble: {
                nodeId = inPacket.decodeInt();
                core = sortedMatrixCores.get(nodeId);
                if (core == null) {
                    chr.dispose();
                    return;
                }
                if (core.isLock()) {
                    chr.dispose();
                    return;
                }
                int shards = VCore.getEnforceOption(VCore.getCore(core.getCoreID()).getType()).get(core.getSkillLevel()).getExtract();
                core.setState(MatrixStateType.DISASSEMBLED);
                gainNodeShards(chr, shards);
                chr.write(WvsContext.nodeShardResult(shards));
                chr.write(WvsContext.updateVMatrix(chr, true, type.getVal(), 0));
                break;
            }
            case DisassembleGroup: {
                int size = inPacket.decodeInt();
                Set<MatrixCore> removeCores = new HashSet<>();
                int shards = 0;
                for (int i = 0; i < size; i++) {
                    nodeId = inPacket.decodeInt();
                    core = sortedMatrixCores.get(nodeId);
                    if (core.isLock()) {
                        continue;
                    }
                    if (core != null && !core.isActive()) {
                        removeCores.add(core);
                        shards += VCore.getEnforceOption(VCore.getCore(core.getCoreID()).getType()).get(core.getSkillLevel()).getExtract();
                    }
                }
                for (MatrixCore remove : removeCores) {
                    remove.setState(MatrixStateType.DISASSEMBLED);
                }
                gainNodeShards(chr, shards);
                chr.write(WvsContext.nodeShardResult(shards));
                chr.write(WvsContext.updateVMatrix(chr, true, type.getVal(), 0));
                break;
            }
            case CraftNode: {
                nodeId = inPacket.decodeInt();
                int nCount = inPacket.decodeInt();
                int cost = getNodeShardsCost(nodeId) * nCount;
                if (chr.getAvatarData().getCharacterStat().getNodeShards() >= cost) {
                    int skillID1 = 0, skillID2 = 0, skillID3 = 0;
                    VCoreData vCoreData = VCore.getCore(nodeId);
                    if (vCoreData.getConnectSkills().size() > 0) {
                        skillID1 = vCoreData.getConnectSkills().get(0);
                    }
                    switch (vCoreData.getType()) {
                        case VCore.BOOST:
                            short jobID = Short.parseShort(vCoreData.getJobs().get(0));
                            List<Integer> boostSkills = VCore.getBoostSkillByJobID(jobID);
                            boostSkills.remove((Integer) skillID1);
                            skillID2 = boostSkills.get(Randomizer.nextInt(boostSkills.size()));
                            boostSkills.remove((Integer) skillID2);
                            skillID3 = boostSkills.get(Randomizer.nextInt(boostSkills.size()));
                            boostSkills.remove((Integer) skillID3);
                            break;
                        case VCore.SKILL:
                        case VCore.SPECIAL:
                            break;
                    }
                    List<MatrixCore> cores = new ArrayList<>(nCount);
                    for (int i = 0; i < nCount; i++) {
                        core = new MatrixCore(chr.getId(), nodeId, skillID1, skillID2, skillID3);
                        cores.add(core);
                    }
                    MatrixCore.saveToSQL(cores);
                    for (MatrixCore add : cores) {
                        chr.getMatrixCore().add(add);
                    }
                    gainNodeShards(chr, -cost);
                    chr.write(WvsContext.nodeCraftResult(nodeId, 1, skillID1, skillID2, skillID3));
                    chr.write(WvsContext.updateVMatrix(chr, true, type.getVal(), 0));
                }
                break;
            }
            case CraftNodestone: {
                chr.getScriptManager().startScript(chr, 0, 0, "Craft_NodeStone", ScriptType.Npc);
                break;
            }
            case CraftCustomBoostNode: {
                int coreID = inPacket.decodeInt();
                int skillID1 = 0;
                int skillID2 = inPacket.decodeInt();
                int skillID3 = inPacket.decodeInt();
                VCoreData vCoreData = VCore.getCore(coreID);
                if (vCoreData != null) {
                    if (vCoreData.getConnectSkills().size() > 0) {
                        skillID1 = vCoreData.getConnectSkills().get(0);
                    }
                    int cost = getNodeShardsCost(coreID);
                    core = new MatrixCore(chr.getId(), coreID, skillID1, skillID2, skillID3);
                    core.saveToSQL();
                    chr.getMatrixCore().add(core);
                    gainNodeShards(chr, -cost);
                    chr.write(WvsContext.nodeCraftResult(coreID, 1, skillID1, skillID2, skillID3));
                }
                chr.write(WvsContext.updateVMatrix(chr, true, type.getVal(), 0));
                break;
            }
            case EnhanceSlot: {
                int slotPos = inPacket.decodeInt();
                if ((slotPos >= 0 && slotPos < MatrixConstants.MAX_NODE_SLOTS) && chr.getAvailableMatrixPoints() > 0) {
                    slot = chr.getMatrixSlotByPosition(slotPos);
                    core = chr.getMatrixCoreByPosition(slotPos);
                    if (slot == null) {
                        chr.dispose();
                        return;
                    }
                    final int nextLevel = slot.getLevel() + 1;
                    slot.setLevel(nextLevel);
                    if (core != null) {
                        relocCoreSkill(chr, core);
                    }
                }
                chr.write(WvsContext.updateVMatrix(chr, false, 0, 0));
                break;
            }
            case ExpandSlot: {
                nodeId = inPacket.decodeInt();
                inPacket.decodeInt(); // Always -1
                slot = chr.getMatrixSlotByPosition(nodeId);
                if (slot == null) {
                    chr.chatPopup("發生未知錯誤。");
                    chr.dispose();
                    return;
                }
                int[] aSlotPrice = {0, 0, 0, 0, 25900000, 30800000, 36400000, 43400000, 51800000, 60900000, 72100000,
                        85400000, 101500000, 119700000, 142100000, 168000000, 198800000, 235200000, 278600000};
                int nReqLevel = 200 + ((nodeId - 3) * 5);
                double dSlotOneMultiplier = 1.0;
                double dSlotTwoMultiplier = 3.7029;
                int nLevelGap = nReqLevel - chr.getLevel();
                switch (nLevelGap) {
                    case 1:
                        dSlotOneMultiplier = 1.0;
                        break;
                    case 2:
                        dSlotOneMultiplier = 1.3656;
                        break;
                    case 3:
                        dSlotOneMultiplier = 1.8042;
                        break;
                    case 4:
                        dSlotOneMultiplier = 2.3307;
                        break;
                    case 5:
                        dSlotOneMultiplier = 2.9624;
                        break;
                    case 6:
                        dSlotTwoMultiplier = 3.7029;
                        break;
                    case 7:
                        dSlotTwoMultiplier = 4.6285;
                        break;
                    case 8:
                        dSlotTwoMultiplier = 5.7857;
                        break;
                    case 9:
                        dSlotTwoMultiplier = 7.2321;
                        break;
                    case 10:
                        dSlotTwoMultiplier = 9.0401;
                        break;
                }
                double dPrice = aSlotPrice[nodeId] * (nLevelGap <= 5 ? dSlotOneMultiplier : dSlotTwoMultiplier);
                if (chr.getMoney() < dPrice) {
                    chr.chatPopup("你的楓幣不足，無法解鎖該位置。");
                    return;
                }
                chr.deductMoney((long) dPrice);
                slot.setUnLock(true);
                chr.write(WvsContext.updateVMatrix(chr, false, 0, 0));
                break;
            }
            case EnhanceReset: {
                if (chr.getMoney() < 1_000_000) {
                    chr.chatPopup("你的楓幣不足，無法重置這些位置。");
                    return;
                }
                for (pos = 0; pos < MatrixConstants.MAX_NODE_SLOTS; pos++) {
                    MatrixSlot ms = chr.getMatrixSlotByPosition(pos);
                    if (ms != null && ms.getLevel() >= 0) {
                        ms.setLevel(0);
                        core = chr.getMatrixCoreByPosition(pos);
                        if (core != null) {
                            relocCoreSkill(chr, core);
                        }
                    }
                }
                chr.deductMoney(1_000_000);
                chr.write(WvsContext.updateVMatrix(chr, false, 0, 0));
                break;
            }
            case Lock: {
                int slotPos = inPacket.decodeInt();
                core = sortedMatrixCores.get(slotPos);
                if (core != null) {
                    core.setLock(true);
                }
                chr.write(WvsContext.updateVMatrix(chr, true, type.getVal(), 0));
                break;
            }
            case Unlock: {
                int slotPos = inPacket.decodeInt();
                core = sortedMatrixCores.get(slotPos);
                if (core != null) {
                    core.setLock(false);
                }
                chr.write(WvsContext.updateVMatrix(chr, true, type.getVal(), 0));
                break;
            }
        }
        chr.dispose();
    }

    public static void relocCoreSkill(Char chr, MatrixCore core) {
        MatrixSlot slot = chr.getMatrixSlotByPosition(core.getSlot());
        int slotLevel = slot != null ? slot.getLevel() : 0;
        Map<Integer, Tuple<Integer, Integer>> skillInfo = new HashMap<>();
        Tuple<Integer, Integer> skillDetail;
        if (!VCore.isSpecialNode(core.getCoreID())) {
            int[] skills = {core.getSkillID1(), core.getSkillID2(), core.getSkillID3()};
            for (int skillID : skills) {
                if (skillID > 0) {
                    if (!skillInfo.containsKey(skillID)) {
                        skillDetail = new Tuple<>(core.getSkillLevel(), core.getMaxLevel());
                        skillInfo.put(skillID, skillDetail);
                    } else if (skillInfo.containsKey(skillID)) {
                        skillDetail = skillInfo.get(skillID);
                        skillDetail.setLeft(skillDetail.getLeft() + core.getSkillLevel());
                    }
                }
            }
        } else if (VCore.isSpecialNode(core.getCoreID())) {
            VCoreData vCoreData = VCore.getCore(core.getCoreID());
            VCoreData.CoreOption coreOption = vCoreData.getOption();
            skillDetail = new Tuple<>(1, 1);
            skillInfo.put(coreOption.getSkillID(), skillDetail);
        }
        List<Skill> skills = new ArrayList<>();
        for (Map.Entry<Integer, Tuple<Integer, Integer>> entry : skillInfo.entrySet()) {
            int skillID = entry.getKey();
            if (skillID > 0) {
                Skill skill = chr.getSkill(skillID);
                if (skill == null) {
                    continue; //Just for Sure.
                }
                skill.setCurrentLevel(entry.getValue().getLeft() + slotLevel);
                skill.setMasterLevel(entry.getValue().getRight() + slotLevel);
                skills.add(skill);
            }
        }
        chr.addListSkill(skills);
    }

    public static void setNodeSkill(Char chr, MatrixCore core, MatrixUpdateType type) {
        MatrixSlot slot = chr.getMatrixSlotByPosition(core.getSlot());
        int slotLevel = slot != null ? slot.getLevel() : 0;
        if (type == MatrixUpdateType.Activate) {
            //<Skill ID, <Skill Level>, Skill Max Level>>
            Map<Integer, Tuple<Integer, Integer>> skillInfo = new HashMap<>();
            Tuple<Integer, Integer> skillDetail;
            if (!VCore.isSpecialNode(core.getCoreID())) {
                int[] skills = {core.getSkillID1(), core.getSkillID2(), core.getSkillID3()};
                for (int skillID : skills) {
                    if (skillID > 0) {
                        if (!skillInfo.containsKey(skillID)) {
                            skillDetail = new Tuple<>(core.getSkillLevel(), core.getMaxLevel());
                            skillInfo.put(skillID, skillDetail);
                        } else if (skillInfo.containsKey(skillID)) {
                            skillDetail = skillInfo.get(skillID);
                            skillDetail.setLeft(skillDetail.getLeft() + core.getSkillLevel());
                        }
                    }
                }
            } else if (VCore.isSpecialNode(core.getCoreID())) {
                VCoreData vCoreData = VCore.getCore(core.getCoreID());
                VCoreData.CoreOption coreOption = vCoreData.getOption();
                skillDetail = new Tuple<>(1, 1);
                skillInfo.put(coreOption.getSkillID(), skillDetail);
            }
            List<Skill> skills = new ArrayList<>();
            for (Map.Entry<Integer, Tuple<Integer, Integer>> entry : skillInfo.entrySet()) {
                int id = entry.getKey();
                Skill skill = SkillData.getSkillDeepCopyById(id);
                if (skill == null) {
                    continue; //Just for Sure.
                }
                int skillLevel = entry.getValue().getLeft() + slotLevel;
                int maxLevel = VCore.isBoostNode(core.getCoreID()) ? 55 : 30;
                skill.setCurrentLevel(Math.min(maxLevel, skillLevel));
                skill.setMasterLevel(entry.getValue().getRight());
                skills.add(skill);
            }
            chr.addListSkill(skills);
        } else if (type == MatrixUpdateType.Deactivate) {
            List<Skill> skills = new ArrayList<>();
            int[] skillIDs = new int[]{};
            if (!VCore.isSpecialNode(core.getCoreID())) {
                skillIDs = new int[]{core.getSkillID1(), core.getSkillID2(), core.getSkillID3()};
            } else if (VCore.isSpecialNode(core.getCoreID())) {
                VCoreData vCoreData = VCore.getCore(core.getCoreID());
                VCoreData.CoreOption coreOption = vCoreData.getOption();
                skillIDs = new int[]{coreOption.getSkillID()};
            }

            for (int id : skillIDs) {
                if (id > 0) {
                    Skill skill = SkillData.getSkillDeepCopyById(id);
                    if (skill == null) {
                        continue; //Just for Sure.
                    }
                    skill.setCurrentLevel(0);
                    skills.add(skill);
                }
            }
            chr.addListSkill(skills);
        }
    }

    public static int getNodeShardsCost(int coreID) {
        switch (VCore.getCore(coreID).getType()) {
            case 0:
                return 140;
            case 1:
                return 70;
            case 2:
                return 250;
            default:
                return 0;
        }
    }

    public static void gainNodeShards(Char chr, int nodeShards) {
        int value = Math.min(nodeShards + chr.getAvatarData().getCharacterStat().getNodeShards(), Integer.MAX_VALUE);
        chr.getAvatarData().getCharacterStat().setNodeShards(value);
        chr.createQuestWithQRValue(MatrixConstants.MATRIX_SHARDS, "count=" + value);
    }
}