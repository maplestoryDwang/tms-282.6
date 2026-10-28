package net.swordie.ms.loaders.Etc.VCore;


import net.swordie.ms.ServerConstants;
import net.swordie.ms.constants.MatrixConstants;
import net.swordie.ms.loaders.Etc.VCore.VCoreData.CoreOption;
import net.swordie.ms.loaders.Etc.VCore.VCoreData.EnforceOption;
import net.swordie.ms.loaders.Etc.VCore.VCoreData.JobSkillOption;
import net.swordie.ms.util.*;

import org.w3c.dom.Node;

import java.io.*;
import java.util.*;

/**
 * Created by MechAviv on 2/16/2019.
 */
public class VCore {
    public static final int SKILL = 0, BOOST = 1, SPECIAL = 2, EXP = 3;
    private static Map<Integer, EnforceOption> boostEnforce = new HashMap<>();
    private static Map<Integer, EnforceOption> specialEnforce = new HashMap<>();
    private static Map<Integer, EnforceOption> expCoreEnforce = new HashMap<>();
    private static Map<Integer, VCoreData> vcore = new HashMap<>();
    private static Map<Short, Map<Short, JobSkillOption>> vSkillsByJob = new HashMap<>();

    private static Map<Integer, EnforceOption> skillEnforce = new HashMap<>();
    public static VCoreData getCore(int coreID) {
        return vcore.getOrDefault(coreID, null);
    }

    public static Map<Short, Map<Short, JobSkillOption>> getAllBoostSkill() {
        return vSkillsByJob;
    }

    public static List<Integer> getBoostSkillByJobID(short jobID) {
        List<Integer> listSkill = new LinkedList<>();
        for (Map.Entry<Short, Map<Short, JobSkillOption>> entry : getAllBoostSkill().entrySet()) {
            for (Map.Entry<Short, JobSkillOption> entryInfo : entry.getValue().entrySet()) {
                if (entry.getKey() == jobID) {
                    listSkill.add(entryInfo.getValue().getID());
                }
            }
        }
        return listSkill;
    }

    public static boolean isSkillNode(int coreID) {
        return getCore(coreID).getType() == SKILL;
    }

    public static boolean isBoostNode(int coreID) {
        return getCore(coreID).getType() == BOOST;
    }

    public static boolean isSpecialNode(int coreID) {
        return getCore(coreID).getType() == SPECIAL;
    }

    public static boolean isExpNode(int coreID) {
        return getCore(coreID).getType() == EXP;
    }

    public static int getMaxLevel(int type) {
        return type == SKILL || type == BOOST ? MatrixConstants.GRADE_MAX : 1;
    }

    public static Map<Integer, EnforceOption> getEnforceOption(int type) {
        if (type == SKILL) {
            return skillEnforce;
        } else if (type == BOOST) {
            return boostEnforce;
        } else if (type == SPECIAL) {
            return specialEnforce;
        } else if (type == EXP) {
            return expCoreEnforce;
        }
        return null;
    }

    public static List<VCoreData> getNodesForJob(int jobID) {
        List<VCoreData> coreData = new ArrayList<>();
        for (VCoreData core : vcore.values()) {
            if (core.isJobSkill(jobID)) {
                coreData.add(core);
            }
        }
        return coreData;
    }

    public static List<VCoreData> getSkillNodes() {
        List<VCoreData> coreData = new ArrayList<>();
        for (VCoreData core : vcore.values()) {
            if (core.getType() == SKILL) {
                coreData.add(core);
            }
        }
        return coreData;
    }

    public static List<VCoreData> getBoostNodes() {
        List<VCoreData> coreData = new ArrayList<>();
        for (VCoreData core : vcore.values()) {
            if (core.getType() == BOOST && !core.getJobs().contains("none")) {
                coreData.add(core);
            }
        }
        return coreData;
    }

    public static List<VCoreData> getSpecialNodes() {
        List<VCoreData> coreData = new ArrayList<>();
        for (VCoreData core : vcore.values()) {
            if (core.getType() == SPECIAL) {
                coreData.add(core);
            }
        }
        return coreData;
    }

    public static List<VCoreData> getExpNodes() {
        List<VCoreData> coreData = new ArrayList<>();
        for (VCoreData core : vcore.values()) {
            if (core.getType() == EXP) {
                coreData.add(core);
            }
        }
        return coreData;
    }

    public static List<VCoreData> getClassNodes() {
        List<VCoreData> coreData = new ArrayList<>();
        for (VCoreData core : getSkillNodes()) {
            if (core.getJobs().contains("warrior") || core.getJobs().contains("magician") || core.getJobs().contains("archer")
                    || core.getJobs().contains("rogue") || core.getJobs().contains("pirate")) {
                coreData.add(core);
            }
        }
        return coreData;
    }

    public static List<VCoreData> getJobNodes() {
        List<VCoreData> coreData = new ArrayList<>();
        for (VCoreData core : getSkillNodes()) {
            if (!core.getJobs().contains("all") && !core.getJobs().contains("none") && !core.getJobs().contains("warrior") && !core.getJobs().contains("magician")
                    && !core.getJobs().contains("archer") && !core.getJobs().contains("rogue") && !core.getJobs().contains("pirate")) {
                coreData.add(core);
            }
        }
        return coreData;
    }

    public static List<VCoreData> getDecentNodes() {
        List<VCoreData> coreData = new ArrayList<>();
        for (VCoreData core : getSkillNodes()) {
            if (core.getName().contains("Decent")) {
                coreData.add(core);
            }
        }
        return coreData;
    }

    private static void loadVCoreDataFromWz() {
        Node coreDataNode = XMLApi.getNodeByPath("Etc.wz/VCore.img", "CoreData");
        VCoreData coreData;

        for (Node coreNode : XMLApi.getAllChildren(coreDataNode)) {
            coreData = new VCoreData();
            coreData.setCoreID(Integer.parseInt(XMLApi.getNamedAttribute(coreNode, "name")));
            for (Node core : XMLApi.getAllChildren(coreNode)) {
                String name = XMLApi.getNamedAttribute(core, "name");
                String value = XMLApi.getNamedAttribute(core, "value");
                switch (name) {
                    case "type":
                        coreData.setType(Byte.parseByte(value));
                        break;
                    case "maxLevel":
                        coreData.setMaxLevel(Short.parseShort(value));
                        break;
                    case "name":
                        coreData.setName(value);
                        break;
                    case "desc":
                        coreData.setDesc(value);
                        break;
                    case "expireAfter":
                        coreData.setExpireAfter(Integer.parseInt(value));
                        break;
                    case "connectSkill":
                        for (Node connectedSkill : XMLApi.getAllChildren(core)) {
                            String skillID = XMLApi.getNamedAttribute(connectedSkill, "value");
                            if (Util.isNumber(skillID)) {
                                coreData.addConnectedSkill(Integer.parseInt(skillID));
                            }
                        }
                        break;
                    case "job":
                        for (Node job : XMLApi.getAllChildren(core)) {
                            coreData.addJob(XMLApi.getNamedAttribute(job, "value"));
                        }
                        break;
                    case "spCoreOption":
                        CoreOption coreOption = new CoreOption();
                        for (Node opt : XMLApi.getAllChildren(core)) {
                            switch (XMLApi.getNamedAttribute(opt, "name")) {
                                case "cond":
                                    for (Node cond : XMLApi.getAllChildren(opt)) {
                                        String condName = XMLApi.getNamedAttribute(cond, "name");
                                        String condValue = XMLApi.getNamedAttribute(cond, "value");
                                        switch (condName) {
                                            case "type":
                                                coreOption.setCondType(condValue);
                                                break;
                                            case "cooltime":
                                                coreOption.setCooltime(Integer.parseInt(condValue));
                                                break;
                                            case "validTime":
                                                coreOption.setValidTime(Integer.parseInt(condValue));
                                                break;
                                            case "count":
                                                coreOption.setCount(Integer.parseInt(condValue));
                                                break;
                                            case "prob":
                                                coreOption.setProb(Double.parseDouble(condValue));
                                                break;
                                            default:
                                                System.out.printf("[VCore] Unknown core option value (Cond) [%s], [%s]%n", condName, condValue);
                                                break;
                                        }
                                    }
                                    break;
                                case "effect":
                                    for (Node effect : XMLApi.getAllChildren(opt)) {
                                        String effName = XMLApi.getNamedAttribute(effect, "name");
                                        String effValue = XMLApi.getNamedAttribute(effect, "value");
                                        switch (effName) {
                                            case "type":
                                                coreOption.setEffectType(effValue);
                                                break;
                                            case "skill_id":
                                                coreOption.setSkillID(Integer.parseInt(effValue));
                                                break;
                                            case "skill_level":
                                                coreOption.setSLV(Short.parseShort(effValue));
                                                break;
                                            case "heal_percent":
                                                coreOption.setHealPercent(Integer.parseInt(effValue));
                                                break;
                                            case "reducePercent":
                                                coreOption.setReducePercent(Integer.parseInt(effValue));
                                                break;
                                            default:
                                                System.out.printf("[VCore] Unknown core option value (Effect) [%s], [%s]%n", effName, effValue);
                                                break;
                                        }
                                    }
                                    break;
                            }
                        }
                        coreData.setOption(coreOption);
                        break;
                    case "icon":
                    case "iconMouseOver":
                    case "iconDisabled":
                    case "NotDuringEquipRemove":
                    case "notAbleCraft":
                    case "nobAbleGemStone":
                    case "checkCooltimeSkill":
                    case "noDisassemble":
                    case "checkRemoveSkill":
                        break;
                    default:
                        System.out.printf("[VCore] Unknown v core value [%s], [%s]%n", name, value);
                        break;
                }
            }
            vcore.put(coreData.getCoreID(), coreData);
        }
        loadVCoreEnforcementDataFromWz(XMLApi.getNodeByPath("Etc.wz/VCore.img", "Enforcement"));
        loadVCoreJobSkillDataFromWz(XMLApi.getNodeByPath("Etc.wz/VCore.img", "JobSkill"));
    }

    private static void loadVCoreJobSkillDataFromWz(Node jobSkillDataNode) {
        //This is List of Job ID
        for (Node jobSkillType : XMLApi.getAllChildren(jobSkillDataNode)) {
            short jobID = Short.parseShort(XMLApi.getNamedAttribute(jobSkillType, "name"));
            //This is List of Available skill
            Map<Short, VCoreData.JobSkillOption> listSkill = new HashMap<>();
            for (Node jobSkillOptionNode : XMLApi.getAllChildren(jobSkillType)) {
                //short index
                short index = Short.parseShort(XMLApi.getNamedAttribute(jobSkillOptionNode, "name"));
                JobSkillOption jobSkillOption = new JobSkillOption();
                for (Node skillData : XMLApi.getAllChildren(jobSkillOptionNode)) {
                    String name = XMLApi.getNamedAttribute(skillData, "name");
                    String value = XMLApi.getNamedAttribute(skillData, "value");
                    switch (name) {
                        case "id":
                            //System.out.println("Job ID: " + jobID + " Skill ID:" + Integer.parseInt(value));
                            jobSkillOption.setID(Integer.parseInt(value));
                            break;
                        case "name":
                            //System.out.println("Job ID: " + jobID + " Skill Name:" + value);
                            jobSkillOption.setName(value);
                            break;
                        case "prob":
                            jobSkillOption.setProb(Integer.parseInt(value));
                            break;
                        default:
                            System.out.printf("[VCore] Unknown v core value [%s], [%s]%n", name, value);
                            break;
                    }
                }
                listSkill.put(index, jobSkillOption);
            }
            vSkillsByJob.put(jobID, listSkill);
        }
    }

    private static void loadVCoreEnforcementDataFromWz(Node enforcementDataNode) {
        //This is Enforce, Skill, Special
        for (Node enforcementType : XMLApi.getAllChildren(enforcementDataNode)) {
            String type = XMLApi.getNamedAttribute(enforcementType, "name");
            for (Node enforcementOptionNode : XMLApi.getAllChildren(enforcementType)) {
                EnforceOption option = new EnforceOption();
                int level = Integer.parseInt(XMLApi.getNamedAttribute(enforcementOptionNode, "name"));
                for (Node levelData : XMLApi.getAllChildren(enforcementOptionNode)) {
                    String name = XMLApi.getNamedAttribute(levelData, "name");
                    String value = XMLApi.getNamedAttribute(levelData, "value");
                    switch (name) {
                        case "expEnforce":
                            option.setEnforceExp(Integer.parseInt(value));
                            break;
                        case "nextExp":
                            option.setNextExp(Integer.parseInt(value));
                            break;
                        case "extract":
                            option.setExtract(Integer.parseInt(value));
                            break;
                        default:
                            System.out.printf("[VCore] Unknown enforcement value [%s], [%s]%n", name, value);
                            break;
                    }
                }
                switch (type) {
                    case "Skill":
                        skillEnforce.put(level, option);
                        break;
                    case "Enforce":
                        boostEnforce.put(level, option);
                        break;
                    case "Special":
                        specialEnforce.put(level, option);
                        break;
                    case "expCore":
                        expCoreEnforce.put(level, option);
                        break;
                    default:
                        System.out.printf("[VCore] Unknown enforcement type [%s]%n", type);
                        break;
                }
            }
        }
    }

    @Saver(varName = "VCore")
    private static void saveVCore(File file) {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
            // VCore Data
            //private static Map<Integer, VCoreData> vcore = new HashMap<>();
            dos.writeInt(vcore.size());
            for (Map.Entry<Integer, VCoreData> entry : vcore.entrySet()) {
                VCoreData value = entry.getValue();
                value.saveToDat(dos);
            }
            // Job Skill Data
            dos.writeInt(vSkillsByJob.size());
            for (Map.Entry<Short, Map<Short, JobSkillOption>> entry : vSkillsByJob.entrySet()) {
                dos.writeInt(entry.getValue().size());
                for (Map.Entry<Short, JobSkillOption> listSkill : entry.getValue().entrySet()) {
                    dos.writeShort(listSkill.getKey()); //This is Index
                    JobSkillOption jobSkillOption = listSkill.getValue();
                    jobSkillOption.saveToDat(dos);
                }
                dos.writeShort(entry.getKey()); //This is Job ID
            }
            // Enforcement Data
            dos.writeInt(skillEnforce.size());
            for (Map.Entry<Integer, EnforceOption> entry : skillEnforce.entrySet()) {
                EnforceOption value = entry.getValue();
                dos.writeInt(entry.getKey());// level
                value.saveToDat(dos);
            }
            dos.writeInt(boostEnforce.size());
            for (Map.Entry<Integer, EnforceOption> entry : boostEnforce.entrySet()) {
                EnforceOption value = entry.getValue();
                dos.writeInt(entry.getKey());// level
                value.saveToDat(dos);
            }
            dos.writeInt(specialEnforce.size());
            for (Map.Entry<Integer, EnforceOption> entry : specialEnforce.entrySet()) {
                EnforceOption value = entry.getValue();
                dos.writeInt(entry.getKey());// level
                value.saveToDat(dos);
            }
            dos.writeInt(expCoreEnforce.size());
            for (Map.Entry<Integer, EnforceOption> entry : expCoreEnforce.entrySet()) {
                EnforceOption value = entry.getValue();
                dos.writeInt(entry.getKey());// level
                value.saveToDat(dos);
            }

        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    @Loader(varName = "VCore")
    public static void loadVCoreFromFile(File file, boolean exists) {
        if (!exists) {
            loadVCoreDataFromWz();
            saveVCore(file);
        } else {
            try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
                // VCore Data
                int VCoreSize = dis.readInt();
                for (int i = 0; i < VCoreSize; i++) {
                    VCoreData data = new VCoreData();
                    data.loadFromDat(dis);
                    vcore.put(data.getCoreID(), data);
                }
                // Job Skill Data
                int VSkillSize = dis.readInt();
                for (int i = 0; i < VSkillSize; i++) {
                    Map<Short, JobSkillOption> listSkill = new HashMap<>();
                    int VSkillListSize = dis.readInt();
                    for (int j = 0; j < VSkillListSize; j++) {
                        short index = dis.readShort();
                        JobSkillOption data = new JobSkillOption();
                        data.loadFromDat(dis);
                        listSkill.put(index, data);
                    }
                    short jobID = dis.readShort();
                    vSkillsByJob.put(jobID, listSkill);
                }
                // Enforcement Data
                for (int i = 0; i < 4; i++) {
                    int enforcementSize = dis.readInt();
                    for (int j = 0; j < enforcementSize; j++) {
                        EnforceOption data = new EnforceOption();
                        int level = dis.readInt();
                        data.loadFromDat(dis);
                        switch (i) {
                            case 0:
                                skillEnforce.put(level, data);
                                break;
                            case 1:
                                boostEnforce.put(level, data);
                                break;
                            case 2:
                                specialEnforce.put(level, data);
                                break;
                            case 3:
                                expCoreEnforce.put(level, data);
                                break;
                        }
                    }
                }


            } catch (IOException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            }
        }
    }

    public static void main(String[] args) {
        loadVCoreDataFromWz();
        saveVCore(new File(ServerConstants.DAT_DIR + "/VCore.dat"));
    }
}
