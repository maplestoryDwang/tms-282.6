package net.swordie.ms.loaders;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.ExtraSkillInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.constants.HexaBoostSkillConstants;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.life.mob.skill.MobSkillStat;
import net.swordie.ms.loaders.containerclasses.MakingSkillRecipe;
import net.swordie.ms.loaders.containerclasses.MobSkillInfo;
import net.swordie.ms.util.*;
import net.swordie.ms.util.container.Tuple;
import org.w3c.dom.Node;

import java.io.*;
import java.util.*;

public class SkillData {

    private static final boolean LOG_UNKS = false;
    private static final Int2ObjectMap<SkillInfo> skills = new Int2ObjectOpenHashMap<>();
    private static final Set<Integer> SPECIAL_282_BOOKS = Set.of(12005, 12100, 12006, 12200, 13000, 13100, 13001, 13500);

    private static final class Special282Skills {
        private static final Int2ObjectMap<SkillInfo> DATA = load();

        private static Int2ObjectMap<SkillInfo> load() {
            Int2ObjectMap<SkillInfo> result = new Int2ObjectOpenHashMap<>();
            File[] files = SPECIAL_282_BOOKS.stream().sorted()
                    .map(book -> new File(ServerConstants.DIR + "/data/wz282-special/Skill.wz/" + book + ".xml"))
                    .toArray(File[]::new);
            for (File file : files) {
                if (!file.isFile()) {
                    throw new IllegalStateException("Missing verified 282 special skill data: " + file);
                }
            }
            loadSkillXmlFiles(files, result, false);
            if (result.size() != 383 || result.values().stream().anyMatch(si -> !SPECIAL_282_BOOKS.contains(si.getRootId()))) {
                throw new IllegalStateException("Incomplete 282 special skill overlay: " + result.size());
            }
            return result;
        }
    }
    private static final class EnrichedSpecial282Skills {
        private static final Int2ObjectMap<SkillInfo> DATA = load();
        private static Int2ObjectMap<SkillInfo> load() {
            // Reparse into independent objects: the old special map is also visible in 265 mode.
            Int2ObjectMap<SkillInfo> result = Special282Skills.load();
            result.values().forEach(Tms282LegacySkillLevels::enrichOverlay);
            return result;
        }
    }
    private static Int2ObjectMap<SkillInfo> specialSkills() {
        return "tms282.3".equals(net.swordie.ms.loaders.overlay.DataOverlays.current().id())
                ? EnrichedSpecial282Skills.DATA : Special282Skills.DATA;
    }
    private static final Int2ObjectMap<SkillInfo> petPassiveSkills = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectMap<MakingSkillRecipe> makingSkillRecipes = new Int2ObjectOpenHashMap<>();

    private static final Int2ObjectMap<Int2IntMap> eliteMobSkills = new Int2ObjectOpenHashMap<>();
    private static final Short2ObjectMap<Short2ObjectMap<MobSkillInfo>> mobSkillInfos = new Short2ObjectOpenHashMap<>();

    public static void saveSkills(String dir) {
        Util.makeDirIfAbsent(dir);
        skills.forEach((key, si) -> {
            File file = new File(String.format("%s/%d.dat", dir, si.getSkillId()));
            try (DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(file))) {
                dataOutputStream.writeInt(si.getSkillId());
                dataOutputStream.writeInt(si.getRootId());
                dataOutputStream.writeInt(si.getMaxLevel());
                dataOutputStream.writeInt(si.getMasterLevel());
                dataOutputStream.writeInt(si.getFixLevel());
                dataOutputStream.writeBoolean(si.isInvisible());
                dataOutputStream.writeBoolean(si.isMassSpell());
                dataOutputStream.writeInt(si.getType());
                dataOutputStream.writeUTF(si.getElemAttr());
                dataOutputStream.writeInt(si.getHyper());
                dataOutputStream.writeInt(si.getVSkill());
                dataOutputStream.writeInt(si.getHyperStat());
                dataOutputStream.writeInt(si.getVehicleId());
                dataOutputStream.writeInt(si.getReqTierPoint());
                dataOutputStream.writeBoolean(si.isNotCooltimeReset());
                dataOutputStream.writeBoolean(si.isNotIncBuffDuration());
                dataOutputStream.writeBoolean(si.isAscentSkill());
                dataOutputStream.writeBoolean(si.isOriginSkill());
                dataOutputStream.writeBoolean(si.isShootObject());
                dataOutputStream.writeBoolean(si.isFinalAttack());
                dataOutputStream.writeShort(si.getFinalAttacks().size());
                for (var faSkill : si.getFinalAttacks()) {
                    dataOutputStream.writeInt(faSkill);
                }
                dataOutputStream.writeBoolean(si.isPsd());
                dataOutputStream.writeInt(si.getBoostDamR());
                dataOutputStream.writeShort(si.getSkillStatInfo().size());
                for (Map.Entry<SkillStat, String> ssEntry : si.getSkillStatInfo().entrySet()) {
                    dataOutputStream.writeUTF(ssEntry.getKey().toString());
                    if (ssEntry.getValue() == null) {
                        dataOutputStream.writeUTF("");
                    } else {
                        dataOutputStream.writeUTF(ssEntry.getValue());
                    }
                }
                dataOutputStream.writeShort(si.getRects().size());
                for (Rect r : si.getRects()) {
                    dataOutputStream.writeInt(r.getLeft());
                    dataOutputStream.writeInt(r.getTop());
                    dataOutputStream.writeInt(r.getRight());
                    dataOutputStream.writeInt(r.getBottom());
                }
                dataOutputStream.writeShort(si.getPsdSkills().size());
                for (int i : si.getPsdSkills()) {
                    dataOutputStream.writeInt(i);
                }
                dataOutputStream.writeShort(si.getReqSkills().size());
                for (Map.Entry<Integer, Integer> reqSkill : si.getReqSkills().entrySet()) {
                    dataOutputStream.writeInt(reqSkill.getKey());
                    dataOutputStream.writeInt(reqSkill.getValue());
                }
                dataOutputStream.writeShort(si.getAddAttackSkills().size());
                for (int i : si.getAddAttackSkills()) {
                    dataOutputStream.writeInt(i);
                }
                dataOutputStream.writeShort(si.getExtraSkillInfo().size());
                for (Map.Entry<Integer, ExtraSkillInfo> entry : si.getExtraSkillInfo().entrySet()) {
                    dataOutputStream.writeInt(entry.getKey());
                    dataOutputStream.writeInt(entry.getValue().getSkillId());
                    dataOutputStream.writeInt(entry.getValue().getDelay());
                    dataOutputStream.writeInt(entry.getValue().getDelay());
                }
                dataOutputStream.writeShort(si.getRandomSkills().size());
                for (Map.Entry<Map<Integer, Integer>, Integer> randomSkillInfo : si.getRandomSkills().entrySet()) {
                    dataOutputStream.writeShort(randomSkillInfo.getKey().size());
                    for (Map.Entry<Integer, Integer> randomSkill : randomSkillInfo.getKey().entrySet()) {
                        dataOutputStream.writeInt(randomSkill.getKey()); // skillID
                        dataOutputStream.writeInt(randomSkill.getValue()); // delay
                    }
                    dataOutputStream.writeInt(randomSkillInfo.getValue()); // prob
                }
                dataOutputStream.writeBoolean(si.isPetPassive());
                dataOutputStream.writeInt(si.getSetItemPartsCount());
                dataOutputStream.writeInt(si.getSetItemReason());
                dataOutputStream.writeShort(si.getSecondAtomInfos().size());
                for (var entry : si.getSecondAtomInfos().int2ObjectEntrySet()) {
                    dataOutputStream.writeInt(entry.getIntKey());
                    var value = entry.getValue();
                    dataOutputStream.writeInt(value.getCreateDelay());
                    dataOutputStream.writeInt(value.getEnableDelay());
                    dataOutputStream.writeInt(value.getRotate());
                    dataOutputStream.writeInt(value.getExpire());
                    dataOutputStream.writeInt(value.getAttackableCount());
                    dataOutputStream.writeInt(value.getDataIndex());
                    dataOutputStream.writeInt(value.getFirstAngleStart());
                    dataOutputStream.writeInt(value.getFirstAngleRange());
                    dataOutputStream.writeInt(value.getNotRotateEndEffect());
                    dataOutputStream.writeInt(value.getPosRandomOffset());
                    dataOutputStream.writeShort(value.getPos() != null ? value.getPos().getX() : 0);
                    dataOutputStream.writeShort(value.getPos() != null ? value.getPos().getY() : 0);
                    dataOutputStream.writeInt(value.getFollowAngle());
                    dataOutputStream.writeInt(value.getLocalOnly());
                    dataOutputStream.writeShort(value.getCustoms().size());
                    for (var custom : value.getCustoms().int2IntEntrySet()) {
                        dataOutputStream.writeInt(custom.getIntKey());
                        dataOutputStream.writeInt(custom.getIntValue());
                    }
                    dataOutputStream.writeShort(value.getExtraPos().size());
                    for (var extraPos : value.getExtraPos().int2ObjectEntrySet()) {
                        dataOutputStream.writeInt(extraPos.getIntKey());
                        dataOutputStream.writeShort(extraPos.getValue() != null ? extraPos.getValue().getX() : 0);
                        dataOutputStream.writeShort(extraPos.getValue() != null ? extraPos.getValue().getY() : 0);
                    }
                }
            } catch (IOException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            }
        });
    }

    public static synchronized void saveSkillToDat(SkillInfo si) throws IOException {
        String dir = ServerConstants.DAT_DIR + "/skills";
        Util.makeDirIfAbsent(dir);
        File file = new File(String.format("%s/%d.dat", dir, si.getSkillId()));
        try (DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(file))) {
            writeSkillInfo(dataOutputStream, si);
        }
    }

    private static void writeSkillInfo(DataOutputStream dataOutputStream, SkillInfo si) throws IOException {
        dataOutputStream.writeInt(si.getSkillId());
        dataOutputStream.writeInt(si.getRootId());
        dataOutputStream.writeInt(si.getMaxLevel());
        dataOutputStream.writeInt(si.getMasterLevel());
        dataOutputStream.writeInt(si.getFixLevel());
        dataOutputStream.writeBoolean(si.isInvisible());
        dataOutputStream.writeBoolean(si.isMassSpell());
        dataOutputStream.writeInt(si.getType());
        dataOutputStream.writeUTF(si.getElemAttr());
        dataOutputStream.writeInt(si.getHyper());
        dataOutputStream.writeInt(si.getVSkill());
        dataOutputStream.writeInt(si.getHyperStat());
        dataOutputStream.writeInt(si.getVehicleId());
        dataOutputStream.writeInt(si.getReqTierPoint());
        dataOutputStream.writeBoolean(si.isNotCooltimeReset());
        dataOutputStream.writeBoolean(si.isNotIncBuffDuration());
        dataOutputStream.writeBoolean(si.isAscentSkill());
        dataOutputStream.writeBoolean(si.isOriginSkill());
        dataOutputStream.writeBoolean(si.isShootObject());
        dataOutputStream.writeBoolean(si.isFinalAttack());
        dataOutputStream.writeShort(si.getFinalAttacks().size());
        for (var faSkill : si.getFinalAttacks()) {
            dataOutputStream.writeInt(faSkill);
        }
        dataOutputStream.writeBoolean(si.isPsd());
        dataOutputStream.writeInt(si.getBoostDamR());
        dataOutputStream.writeShort(si.getSkillStatInfo().size());
        for (Map.Entry<SkillStat, String> ssEntry : si.getSkillStatInfo().entrySet()) {
            dataOutputStream.writeUTF(ssEntry.getKey().toString());
            dataOutputStream.writeUTF(ssEntry.getValue() == null ? "" : ssEntry.getValue());
        }
        dataOutputStream.writeShort(si.getRects().size());
        for (Rect r : si.getRects()) {
            dataOutputStream.writeInt(r.getLeft());
            dataOutputStream.writeInt(r.getTop());
            dataOutputStream.writeInt(r.getRight());
            dataOutputStream.writeInt(r.getBottom());
        }
        dataOutputStream.writeShort(si.getPsdSkills().size());
        for (int i : si.getPsdSkills()) {
            dataOutputStream.writeInt(i);
        }
        dataOutputStream.writeShort(si.getReqSkills().size());
        for (Map.Entry<Integer, Integer> reqSkill : si.getReqSkills().entrySet()) {
            dataOutputStream.writeInt(reqSkill.getKey());
            dataOutputStream.writeInt(reqSkill.getValue());
        }
        dataOutputStream.writeShort(si.getAddAttackSkills().size());
        for (int i : si.getAddAttackSkills()) {
            dataOutputStream.writeInt(i);
        }
        dataOutputStream.writeShort(si.getExtraSkillInfo().size());
        for (Map.Entry<Integer, ExtraSkillInfo> entry : si.getExtraSkillInfo().entrySet()) {
            dataOutputStream.writeInt(entry.getKey());
            dataOutputStream.writeInt(entry.getValue().getSkillId());
            dataOutputStream.writeInt(entry.getValue().getDelay());
            dataOutputStream.writeInt(entry.getValue().getManual());
        }
        dataOutputStream.writeShort(si.getRandomSkills().size());
        for (Map.Entry<Map<Integer, Integer>, Integer> randomSkillInfo : si.getRandomSkills().entrySet()) {
            dataOutputStream.writeShort(randomSkillInfo.getKey().size());
            for (Map.Entry<Integer, Integer> randomSkill : randomSkillInfo.getKey().entrySet()) {
                dataOutputStream.writeInt(randomSkill.getKey());
                dataOutputStream.writeInt(randomSkill.getValue());
            }
            dataOutputStream.writeInt(randomSkillInfo.getValue());
        }
        dataOutputStream.writeBoolean(si.isPetPassive());
        dataOutputStream.writeInt(si.getSetItemPartsCount());
        dataOutputStream.writeInt(si.getSetItemReason());
        dataOutputStream.writeShort(si.getSecondAtomInfos().size());
        for (var entry : si.getSecondAtomInfos().int2ObjectEntrySet()) {
            dataOutputStream.writeInt(entry.getIntKey());
            var value = entry.getValue();
            dataOutputStream.writeInt(value.getCreateDelay());
            dataOutputStream.writeInt(value.getEnableDelay());
            dataOutputStream.writeInt(value.getRotate());
            dataOutputStream.writeInt(value.getExpire());
            dataOutputStream.writeInt(value.getAttackableCount());
            dataOutputStream.writeInt(value.getDataIndex());
            dataOutputStream.writeInt(value.getFirstAngleStart());
            dataOutputStream.writeInt(value.getFirstAngleRange());
            dataOutputStream.writeInt(value.getNotRotateEndEffect());
            dataOutputStream.writeInt(value.getPosRandomOffset());
            dataOutputStream.writeShort(value.getPos() != null ? value.getPos().getX() : 0);
            dataOutputStream.writeShort(value.getPos() != null ? value.getPos().getY() : 0);
            dataOutputStream.writeInt(value.getFollowAngle());
            dataOutputStream.writeInt(value.getLocalOnly());
            dataOutputStream.writeShort(value.getCustoms().size());
            for (var custom : value.getCustoms().int2IntEntrySet()) {
                dataOutputStream.writeInt(custom.getIntKey());
                dataOutputStream.writeInt(custom.getIntValue());
            }
            dataOutputStream.writeShort(value.getExtraPos().size());
            for (var extraPos : value.getExtraPos().int2ObjectEntrySet()) {
                dataOutputStream.writeInt(extraPos.getIntKey());
                dataOutputStream.writeShort(extraPos.getValue() != null ? extraPos.getValue().getX() : 0);
                dataOutputStream.writeShort(extraPos.getValue() != null ? extraPos.getValue().getY() : 0);
            }
        }
    }

    private static void loadSkillsOfJob(int job) {
        String dir = ServerConstants.DAT_DIR + "/skills";
        File folder = new File(dir);
        for (File f : folder.listFiles()) {
            if (f.getName().matches(String.format("^%d(.)*", job))) {
                loadSkill(f);
            }
        }
    }

    public static SkillInfo loadSkill(File file) {
        return readSkill(file, true);
    }

    /** One skill DAT; {@code cache} puts it into the 265 {@code skills} map (the 282 overlay does not). */
    private static SkillInfo readSkill(File file, boolean cache) {
        SkillInfo skillInfo = null;
        try (DataInputStream dataInputStream = new DataInputStream(new FileInputStream(file))) {
            skillInfo = new SkillInfo();
            skillInfo.setSkillId(dataInputStream.readInt());
            SkillInfo special = specialSkills().get(skillInfo.getSkillId());
            if (special != null) {
                return special;
            }
            skillInfo.setRootId(dataInputStream.readInt());
            skillInfo.setMaxLevel(dataInputStream.readInt());
            skillInfo.setMasterLevel(dataInputStream.readInt());
            skillInfo.setFixLevel(dataInputStream.readInt());
            skillInfo.setInvisible(dataInputStream.readBoolean());
            skillInfo.setMassSpell(dataInputStream.readBoolean());
            skillInfo.setType(dataInputStream.readInt());
            skillInfo.setElemAttr(dataInputStream.readUTF());
            skillInfo.setHyper(dataInputStream.readInt());
            skillInfo.setVSkill(dataInputStream.readInt());
            skillInfo.setHyperStat(dataInputStream.readInt());
            skillInfo.setVehicleId(dataInputStream.readInt());
            skillInfo.setReqTierPoint(dataInputStream.readInt());
            skillInfo.setNotCooltimeReset(dataInputStream.readBoolean());
            skillInfo.setNotIncBuffDuration(dataInputStream.readBoolean());
            skillInfo.setAscentSkill(dataInputStream.readBoolean());
            skillInfo.setOriginSkill(dataInputStream.readBoolean());
            skillInfo.setShootObject(dataInputStream.readBoolean());
            skillInfo.setFinalAttack(dataInputStream.readBoolean());
            short fasize = dataInputStream.readShort();
            for (int j = 0; j < fasize; j++) {
                skillInfo.getFinalAttacks().add(dataInputStream.readInt());
            }
            skillInfo.setPsd(dataInputStream.readBoolean());
            skillInfo.setBoostDamR(dataInputStream.readInt());
            short ssSize = dataInputStream.readShort();
            for (int j = 0; j < ssSize; j++) {
                SkillStat skillStat = SkillStat.getSkillStatByString(dataInputStream.readUTF());
                skillInfo.addSkillStatInfo(skillStat, dataInputStream.readUTF());
            }
            short rectSize = dataInputStream.readShort();
            for (int j = 0; j < rectSize; j++) {
                int left = dataInputStream.readInt();
                int top = dataInputStream.readInt();
                int right = dataInputStream.readInt();
                int bottom = dataInputStream.readInt();
                skillInfo.addRect(new Rect(left, top, right, bottom));
            }
            short psdSize = dataInputStream.readShort();
            for (int j = 0; j < psdSize; j++) {
                skillInfo.addPsdSkill(dataInputStream.readInt());
            }
            short reqSkillSize = dataInputStream.readShort();
            for (int j = 0; j < reqSkillSize; j++) {
                skillInfo.addReqSkill(dataInputStream.readInt(), dataInputStream.readInt());
            }
            short addAttackSize = dataInputStream.readShort();
            for (int j = 0; j < addAttackSize; j++) {
                skillInfo.addAddAttackSkills(dataInputStream.readInt());
            }
            short extraSkillInfoSize = dataInputStream.readShort();
            for (int j = 0; j < extraSkillInfoSize; j++) {
                var key = dataInputStream.readInt();
                var skillId = dataInputStream.readInt();
                var delay = dataInputStream.readInt();
                var manual = dataInputStream.readInt();
                var extraSkillInfo = new ExtraSkillInfo();
                extraSkillInfo.setSkillId(skillId);
                extraSkillInfo.setDelay(delay);
                extraSkillInfo.setManual(manual);
                skillInfo.getExtraSkillInfo().put(key, extraSkillInfo);
            }
            short randomSkillInfoSize = dataInputStream.readShort();
            for (int j = 0; j < randomSkillInfoSize; j++) {
                short randomSkillListSize = dataInputStream.readShort();
                Map<Integer, Integer> randomSkillList = new HashMap<>();
                for (int z = 0; z < randomSkillListSize; z++) {
                    randomSkillList.put(dataInputStream.readInt(), dataInputStream.readInt());
                }
                skillInfo.addRandomSkill(randomSkillList, dataInputStream.readInt());
            }
            skillInfo.setPetPassive(dataInputStream.readBoolean());
            skillInfo.setSetItemPartsCount(dataInputStream.readInt());
            skillInfo.setSetItemReason(dataInputStream.readInt());
            short secondAtomInfos = dataInputStream.readShort();
            for (int j = 0; j < secondAtomInfos; j++) {
                int index = dataInputStream.readInt();
                SkillInfo.SecondAtomInfo secondAtomInfo = new SkillInfo.SecondAtomInfo();
                secondAtomInfo.setCreateDelay(dataInputStream.readInt());
                secondAtomInfo.setEnableDelay(dataInputStream.readInt());
                secondAtomInfo.setRotate(dataInputStream.readInt());
                secondAtomInfo.setExpire(dataInputStream.readInt()); // the writer stores expire here (2026-09-29 job-fixes)
                secondAtomInfo.setAttackableCount(dataInputStream.readInt());
                secondAtomInfo.setDataIndex(dataInputStream.readInt());
                secondAtomInfo.setFirstAngleStart(dataInputStream.readInt());
                secondAtomInfo.setFirstAngleRange(dataInputStream.readInt());
                secondAtomInfo.setNotRotateEndEffect(dataInputStream.readInt());
                secondAtomInfo.setPosRandomOffset(dataInputStream.readInt());
                secondAtomInfo.setPos(new Position(dataInputStream.readShort(), dataInputStream.readShort()));
                secondAtomInfo.setFollowAngle(dataInputStream.readInt());
                secondAtomInfo.setLocalOnly(dataInputStream.readInt()); // the writer stores localOnly here (2026-09-29 job-fixes)
                short customs = dataInputStream.readShort();
                for (int x = 0; x < customs; x++) {
                    secondAtomInfo.getCustoms().put(dataInputStream.readInt(), dataInputStream.readInt());
                }
                short extraPos = dataInputStream.readShort();
                for (int x = 0; x < extraPos; x++) {
                    secondAtomInfo.getExtraPos().put(dataInputStream.readInt(), new Position(dataInputStream.readShort(), dataInputStream.readShort()));
                }
                skillInfo.getSecondAtomInfos().put(index, secondAtomInfo);
            }
            if (!cache) {
                Tms282SkillRequirements.enrichOverlay(skillInfo); // never mutate the shared265 cache
                restoreTms282BatteryRate(skillInfo);
            }
            if (cache) {
                skills.put(skillInfo.getSkillId(), skillInfo);
            }
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
        return skillInfo;
    }

    /** Existing DAT282 omitted s3. Restore only the three verified recharge sources from the selected XML layer. */
    private static void restoreTms282BatteryRate(SkillInfo info) {
        int id = info.getSkillId();
        if ((id != 152100010 && id != 152110008 && id != 152120014)
                || !"tms282.3".equals(net.swordie.ms.loaders.overlay.DataOverlays.current().id())
                || info.getSkillStatInfo().containsKey(SkillStat.s3)) return;
        String filename = "Skill.wz/Skill/" + id / 10000 + ".img.xml";
        for (File file : net.swordie.ms.loaders.overlay.DataOverlays.current().wzLayers(filename)) {
            if (!file.isFile()) continue;
            Int2ObjectMap<SkillInfo> parsed = new Int2ObjectOpenHashMap<>();
            loadSkillXmlFiles(new File[]{file}, parsed, true);
            SkillInfo source = parsed.get(id);
            if (source != null && source.getSkillStatInfo().containsKey(SkillStat.s3))
                info.addSkillStatInfo(SkillStat.s3, source.getSkillStatInfo().get(SkillStat.s3));
            return;
        }
    }

    public static void loadSkillsFromWz() {
        String[] wzs = new String[]{"Skill.wz/Skill_00000", "Skill.wz/Skill_00001", "Skill.wz/Skill_00002", "Skill.wz/Skill_00003", "Skill.wz/Skill_00004", "Skill.wz/Skill_00005", "Skill.wz/Skill_00006", "Skill.wz/Skill_00007"};
        for (String wz : wzs) {
            String wzDir = ServerConstants.WZ_DIR + "/" + wz;
            File dir = new File(wzDir);
            File[] files = dir.listFiles();
            loadSkillXmlFiles(files, skills, false);
        }
    }

    private static void loadSkillXmlFiles(File[] files, Int2ObjectMap<SkillInfo> target, boolean include282S3) {
            for (File file : files) {
                if (file.isDirectory()) {
                    continue;
                }
                Node node = XMLApi.getRoot(file);
                if (node == null) {
                    continue;
                }
                List<Node> nodes = XMLApi.getAllChildren(node);
                for (Node mainNode : nodes) {
                    Map<String, String> attributes = XMLApi.getAttributes(mainNode);
                    String rootIdStr = attributes.get("name").replace(".img", "");
                    int rootId;
                    if (Util.isNumber(rootIdStr)) {
                        rootId = Integer.parseInt(rootIdStr);
                    } else {
                        continue;
                    }
                    Set<String> unkVals = new HashSet<>();
                    Node skillChild = XMLApi.getFirstChildByNameBF(mainNode, "skill");
                    if (skillChild == null) {
                        continue;
                    }
                    for (Node skillNode : XMLApi.getAllChildren(skillChild)) {
                        Map<String, String> skillAttributes = XMLApi.getAttributes(skillNode);
                        String skillIdStr = skillAttributes.get("name").replace(".img", "");
                        int skillId;
                        if (Util.isNumber(skillIdStr)) {
                            SkillInfo skill = new SkillInfo();
                            skill.setRootId(rootId);
                            if (Util.isNumber(skillIdStr)) {
                                skillId = Integer.parseInt(skillIdStr);
                                skill.setSkillId(skillId);
                            } else {
                                if (LOG_UNKS) {
                                    System.out.println(skillIdStr + " is not a number.");
                                }
                                continue;
                            }
                            for (Node mainLevelNode : XMLApi.getAllChildren(skillNode)) {
                                String mainName = XMLApi.getNamedAttribute(mainLevelNode, "name");
                                String mainValue = XMLApi.getNamedAttribute(mainLevelNode, "value");
                                int intVal = -1337;
                                if (mainValue != null && Util.isNumber(mainValue)) {
                                    intVal = Integer.parseInt(mainValue);
                                }
                                switch (mainName) {
                                    case "masterLevel":
                                        skill.setMasterLevel(intVal);
                                        break;
                                    case "fixLevel":
                                        skill.setFixLevel(intVal);
                                        break;
                                    case "reqLev":
                                        skill.setReqLevel(intVal);
                                        break;
                                    case "invisible":
                                        skill.setInvisible(intVal != 0);
                                        break;
                                    case "massSpell":
                                        skill.setMassSpell(intVal != 0);
                                        break;
                                    case "type":
                                        skill.setType(intVal);
                                        break;
                                    case "psd":
                                        skill.setPsd(intVal != 0);
                                        break;
                                    case "damR_5th":
                                        skill.setBoostDamR(intVal);
                                        break;
                                    case "psdSkill":
                                        for (Node psdSkillNode : XMLApi.getAllChildren(mainLevelNode)) {
                                            skill.addPsdSkill(Integer.parseInt(XMLApi.getAttributes(psdSkillNode).get("name")));
                                        }
                                        break;
                                    case "elemAttr":
                                        skill.setElemAttr(mainValue);
                                        break;
                                    case "hyper":
                                        skill.setHyper(intVal);
                                        break;
                                    case "vSkill":
                                        skill.setVSkill(intVal);
                                        break;
                                    case "hyperStat":
                                        skill.setHyperStat(intVal);
                                        break;
                                    case "vehicleID":
                                        skill.setVehicleId(intVal);
                                        break;
                                    case "notCooltimeReset":
                                        skill.setNotCooltimeReset(intVal != 0);
                                        break;
                                    case "notIncBuffDuration":
                                        skill.setNotIncBuffDuration(intVal != 0);
                                        break;
                                    case "ascent":
                                        skill.setAscentSkill(intVal != 0);
                                        break;
                                    case "origin":
                                        skill.setOriginSkill(intVal != 0);
                                        break;
                                    case "shootobj":
                                        skill.setShootObject(true);
                                        break;
                                    case "processtype":
                                        skill.setProcessType(intVal);
                                        break;
                                    case "additional_process":
                                        for (Node reqChild : XMLApi.getAllChildren(mainLevelNode)) {
                                            String childName = XMLApi.getNamedAttribute(reqChild, "name");
                                            String childValue = XMLApi.getNamedAttribute(reqChild, "value");
                                            if (Util.isNumber(childName)) {
                                                skill.getAdditionalProcess().add(Integer.parseInt(childValue));
                                            }
                                        }
                                        break;
                                    case "SecondAtom": {
                                        Int2ObjectMap<SkillInfo.SecondAtomInfo> secondAtomInfos = new Int2ObjectOpenHashMap<>();

                                        // 1) default/root info (áp cho tất cả atom, nếu atom không override)
                                        SkillInfo.SecondAtomInfo def = new SkillInfo.SecondAtomInfo();
                                        Node atomDir = null;

                                        for (Node c : XMLApi.getAllChildren(mainLevelNode)) {
                                            String n = XMLApi.getNamedAttribute(c, "name");

                                            if ("atom".equals(n)) {
                                                atomDir = c;
                                                continue;
                                            }

                                            if ("custom".equals(n)) {
                                                Util.parseCustomDirInto(def.getCustoms(), c); // root custom
                                                continue;
                                            }

                                            if ("extraPos".equals(n)) {
                                                Util.parseExtraPosDirInto(def.getExtraPos(), c); // root extraPos
                                                continue;
                                            }

                                            // root scalar fields
                                            String v = XMLApi.getNamedAttribute(c, "value");
                                            if (v == null) continue;

                                            switch (n) {
                                                case "dataIndex" -> def.setDataIndex(Integer.parseInt(v));
                                                case "expire" -> def.setExpire(Integer.parseInt(v));
                                                case "attackableCount" -> def.setAttackableCount(Integer.parseInt(v));
                                                case "createDelay" -> def.setCreateDelay(Integer.parseInt(v));
                                                case "enableDelay" -> def.setEnableDelay(Integer.parseInt(v));
                                                case "rotate" -> def.setRotate(Integer.parseInt(v));
                                                case "firstAngleStart" -> def.setFirstAngleStart(Integer.parseInt(v));
                                                case "firstAngleRange" -> def.setFirstAngleRange(Integer.parseInt(v));
                                                case "notRotateEndEffect" -> def.setNotRotateEndEffect(Integer.parseInt(v));
                                                case "posRandomOffset" -> def.setPosRandomOffset(Integer.parseInt(v));
                                                case "followAngle" -> def.setFollowAngle(Integer.parseInt(v));
                                                case "localOnly" -> def.setLocalOnly(Integer.parseInt(v));
                                                case "pos" -> def.setPos(Util.parseVectorNode(c));
                                                // pos ở root thường không dùng, extraPos mới là list
                                            }
                                        }

                                        // 2) Nếu không có atom -> chỉ 1 entry
                                        if (atomDir == null) {
                                            secondAtomInfos.put(0, def);
                                            skill.setSecondAtomInfos(secondAtomInfos);
                                            break;
                                        }

                                        // 3) Có atom -> mỗi atom = copy default rồi override
                                        for (Node atomChild : XMLApi.getAllChildren(atomDir)) {
                                            String atomIdStr = XMLApi.getNamedAttribute(atomChild, "name"); // <-- đúng: atomChild
                                            if (!Util.isInteger(atomIdStr)) continue;
                                            int atomId = Integer.parseInt(atomIdStr);

                                            SkillInfo.SecondAtomInfo info = copySecondAtomInfo(def);

                                            for (Node p : XMLApi.getAllChildren(atomChild)) {
                                                String pn = XMLApi.getNamedAttribute(p, "name");

                                                if ("custom".equals(pn)) {
                                                    Util.parseCustomDirInto(info.getCustoms(), p);
                                                    continue;
                                                }
                                                if ("extraPos".equals(pn)) {
                                                    Util.parseExtraPosDirInto(info.getExtraPos(), p);
                                                    continue;
                                                }
                                                if ("pos".equals(pn)) {
                                                    info.setPos(Util.parseVectorNode(p));
                                                    continue;
                                                }

                                                String pv = XMLApi.getNamedAttribute(p, "value");
                                                if (pv == null) continue;

                                                switch (pn) {
                                                    case "dataIndex" -> info.setDataIndex(Integer.parseInt(pv)); // atom override
                                                    case "expire" -> info.setExpire(Integer.parseInt(pv));
                                                    case "attackableCount" -> info.setAttackableCount(Integer.parseInt(pv));
                                                    case "createDelay" -> info.setCreateDelay(Integer.parseInt(pv));
                                                    case "enableDelay" -> info.setEnableDelay(Integer.parseInt(pv));
                                                    case "rotate" -> info.setRotate(Integer.parseInt(pv));
                                                    case "firstAngleStart" -> info.setFirstAngleStart(Integer.parseInt(pv));
                                                    case "firstAngleRange" -> info.setFirstAngleRange(Integer.parseInt(pv));
                                                    case "notRotateEndEffect" -> info.setNotRotateEndEffect(Integer.parseInt(pv));
                                                    case "posRandomOffset" -> info.setPosRandomOffset(Integer.parseInt(pv));
                                                    case "followAngle" -> info.setFollowAngle(Integer.parseInt(pv));
                                                    case "localOnly" -> info.setLocalOnly(Integer.parseInt(pv));
                                                }
                                            }

                                            secondAtomInfos.put(atomId, info);
                                        }
                                        skill.setSecondAtomInfos(secondAtomInfos);
                                        break;
                                    }
                                    case "req":
                                        for (Node reqChild : XMLApi.getAllChildren(mainLevelNode)) {
                                            String childName = XMLApi.getNamedAttribute(reqChild, "name");
                                            String childValue = XMLApi.getNamedAttribute(reqChild, "value");
                                            if ("reqTierPoint".equalsIgnoreCase(childName)) {
                                                skill.setReqTierPoint(Integer.parseInt(childValue));
                                            } else if (Util.isNumber(childName)) {
                                                skill.addReqSkill(Integer.parseInt(childName), Integer.parseInt(childValue));
                                            }
                                        }
                                        break;
                                    case "level":
                                        for (Node reqChild : XMLApi.getAllChildren(mainLevelNode)) {
                                            String childName = XMLApi.getNamedAttribute(reqChild, "name");
                                            if ("1".equalsIgnoreCase(childName)) {
                                                for (Node levelNode : XMLApi.getAllChildren(reqChild)) {
                                                    Map<String, String> commonAttr = XMLApi.getAttributes(levelNode);
                                                    String nodeName = commonAttr.get("name");
                                                    if (nodeName.equals("maxLevel")) {
                                                        skill.setMaxLevel(Integer.parseInt(XMLApi.getNamedAttribute(levelNode, "value")));
                                                    } else if (nodeName.contains("lt") && nodeName.length() <= 3) {
                                                        Node ltNode = XMLApi.getFirstChildByNameBF(reqChild, "lt");
                                                        Node rbNode = XMLApi.getFirstChildByNameBF(reqChild, "rb");
                                                        int[] lt = new int[0];
                                                        int[] rb = new int[0];
                                                        if (ltNode != null) {
                                                            lt = XMLApi.parseVec(ltNode);
                                                        }
                                                        if (rbNode != null) {
                                                            rb = XMLApi.parseVec(rbNode);
                                                        }
                                                        skill.addRect(new Rect(lt[0], lt[1], rb[0], rb[1]));
                                                    } else if (nodeName.equals("invisible")) {
                                                        skill.setInvisible(Integer.parseInt(commonAttr.get("value")) != 0);
                                                    } else if (nodeName.equals("massSpell")) {
                                                        skill.setMassSpell(Integer.parseInt(commonAttr.get("value")) != 0);
                                                    } else if (nodeName.equals("type")) {
                                                        skill.setType(Integer.parseInt(commonAttr.get("value")));
                                                    } else if (nodeName.equals("elemAttr")) {
                                                        skill.setElemAttr(commonAttr.get("value"));
                                                    } else {
                                                        SkillStat skillStat = SkillStat.getSkillStatByString(nodeName);
                                                        if (skillStat != null && (skillStat != SkillStat.s3 || include282S3)) {
                                                            skill.addSkillStatInfo(skillStat, commonAttr.get("value"));
                                                        } else if (!unkVals.contains(nodeName)) {
                                                            if (LOG_UNKS) {
                                                                System.out.println("Unknown SkillStat " + nodeName);
                                                            }
                                                            unkVals.add(nodeName);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    case "common":
                                    case "info":
                                    case "info2":
                                        for (Node commonNode : XMLApi.getAllChildren(mainLevelNode)) {
                                            Map<String, String> commonAttr = XMLApi.getAttributes(commonNode);
                                            String nodeName = commonAttr.get("name");
                                            if (nodeName.equals("maxLevel")) {
                                                skill.setMaxLevel(Integer.parseInt(XMLApi.getNamedAttribute(commonNode, "value")));
                                            } else if (nodeName.contains("lt") && nodeName.length() <= 3) {
                                                Node ltNode = XMLApi.getFirstChildByNameBF(mainLevelNode, "lt");
                                                Node rbNode = XMLApi.getFirstChildByNameBF(mainLevelNode, "rb");
                                                int[] lt = new int[]{0, 0};
                                                int[] rb = new int[]{0, 0};
                                                if (ltNode != null) {
                                                    lt = XMLApi.parseVec(ltNode);
                                                }
                                                if (rbNode != null) {
                                                    rb = XMLApi.parseVec(rbNode);
                                                }
                                                skill.addRect(new Rect(lt[0], lt[1], rb[0], rb[1]));
                                            } else if (nodeName.equals("finalAttack") && mainName.equals("info")) {
                                                skill.setFinalAttack(true);
                                            } else if (nodeName.equals("invisible")) {
                                                skill.setInvisible(Integer.parseInt(commonAttr.get("value")) != 0);
                                            } else if (nodeName.equals("massSpell")) {
                                                skill.setMassSpell(Integer.parseInt(commonAttr.get("value")) != 0);
                                            } else if (nodeName.equals("type")) {
                                                skill.setType(Integer.parseInt(commonAttr.get("value")));
                                            } else if (nodeName.equals("elemAttr")) {
                                                skill.setElemAttr(commonAttr.get("value"));
                                            } else if (nodeName.equals("dot") && mainName.equals("common")) {
                                                skill.addSkillStatInfo(SkillStat.dot, commonAttr.get("value"));
                                            } else {
                                                SkillStat skillStat = SkillStat.getSkillStatByString(nodeName);
                                                if (skillStat != null && skillStat != SkillStat.dot
                                                        && (skillStat != SkillStat.s3 || include282S3)) {
                                                    skill.addSkillStatInfo(skillStat, commonAttr.get("value"));
                                                } else if (!unkVals.contains(nodeName)) {
                                                    if (LOG_UNKS) {
                                                        System.out.println("Unknown SkillStat " + nodeName);
                                                    }
                                                    unkVals.add(nodeName);
                                                }
                                            }
                                        }
                                        break;
                                    case "finalAttack":
                                        for (Node finalAttackNode : XMLApi.getAllChildren(mainLevelNode)) {
                                            skill.getFinalAttacks().add(Integer.parseInt(XMLApi.getAttributes(finalAttackNode).get("name")));
                                        }
                                        break;
                                    case "skillList":
                                        for (Node skillListNode : XMLApi.getAllChildren(mainLevelNode)) {
                                            skill.getSkillList1().add(Integer.parseInt(XMLApi.getAttributes(skillListNode).get("value")));
                                        }
                                        break;
                                    case "skillList2":
                                        for (Node skillListNode : XMLApi.getAllChildren(mainLevelNode)) {
                                            skill.getSkillList2().add(Integer.parseInt(XMLApi.getAttributes(skillListNode).get("value")));
                                        }
                                        break;
                                    case "addAttack":
                                        for (Node addAttackNode : XMLApi.getAllChildren(mainLevelNode)) {
                                            Map<String, String> addAttackAttr = XMLApi.getAttributes(addAttackNode);
                                            String nodeName = addAttackAttr.get("name");
                                            String nodeValue = addAttackAttr.get("value");
                                            switch (nodeName) {
                                                case "skillPlus":
                                                    for (Node skillPlusNode : XMLApi.getAllChildren(addAttackNode)) {
                                                        Map<String, String> skillPlusAttr = XMLApi.getAttributes(skillPlusNode);
                                                        String skillPlusNodeName = skillPlusAttr.get("name");
                                                        String skillPlusNodeValue = skillPlusAttr.get("value");
                                                        skill.addAddAttackSkills(Integer.parseInt(skillPlusNodeValue));
                                                    }
                                                    break;
                                            }
                                        }
                                        break;
                                    case "petPassive":
                                        skill.setPetPassive(intVal != 0);
                                        break;
                                    case "isSequenceOn":
                                        skill.setSequenceOn(intVal != 0);
                                        break;
                                    case "weapon":
                                        skill.setWeapon(intVal);
                                        break;
                                    case "setItemPartsCount":
                                        skill.setSetItemPartsCount(intVal);
                                        break;
                                    case "setItemReason":
                                        skill.setSetItemReason(intVal);
                                        break;
                                    case "keydown":
                                        //System.out.println("skillId == " + skillId + " || ");
                                        break;
                                    case "extraSkillInfo":
                                        for (Node extraSkillInfoNode : XMLApi.getAllChildren(mainLevelNode)) {
                                            String extraSkillID = XMLApi.getAttributes(extraSkillInfoNode).get("name");
                                            ExtraSkillInfo extraSkillInfo = new ExtraSkillInfo();
                                            for (Node extraSkillInfoIndividual : XMLApi.getAllChildren(extraSkillInfoNode)) {
                                                Map<String, String> extraSkillAttr = XMLApi.getAttributes(extraSkillInfoIndividual);
                                                String extraSkillName = extraSkillAttr.get("name");
                                                String extraSkillValue = extraSkillAttr.get("value");
                                                switch (extraSkillName) {
                                                    case "delay":
                                                        extraSkillInfo.setDelay(Integer.parseInt(extraSkillValue));
                                                        break;
                                                    case "skill":
                                                        extraSkillInfo.setSkillId(Integer.parseInt(extraSkillValue));
                                                        break;
                                                    case "manual":
                                                        extraSkillInfo.setManual(Integer.parseInt(extraSkillValue));
                                                        break;
                                                    default:
                                                        if (LOG_UNKS) {
                                                            System.out.printf("Unknown Extra Skill Info Name: %s", extraSkillName);
                                                        }
                                                        break;
                                                }
                                            }
                                            skill.getExtraSkillInfo().put(Integer.parseInt(extraSkillID), extraSkillInfo);
                                        }
                                        break;
                                    case "randomSkill":
                                        for (Node randomSkillNode : XMLApi.getAllChildren(mainLevelNode)) {
                                            int prob = 0;
                                            Map<Integer, Integer> skillList = new HashMap<>();
                                            for (Node randomSkillIndividual : XMLApi.getAllChildren(randomSkillNode)) {
                                                Map<String, String> extraSkillAttr = XMLApi.getAttributes(randomSkillIndividual);
                                                String randomSkillName = extraSkillAttr.get("name");
                                                String randomSkillValue = extraSkillAttr.get("value");
                                                switch (randomSkillName) {
                                                    case "skillID":
                                                        skillList.put(Integer.parseInt(randomSkillValue), 0);
                                                        break;
                                                    case "prob":
                                                        prob = Integer.parseInt(randomSkillValue);
                                                        break;
                                                    case "skillList":
                                                        for (Node randomSkillListIndividual : XMLApi.getAllChildren(randomSkillIndividual)) {
                                                            Map<String, String> extraSkillListAttr = XMLApi.getAttributes(randomSkillListIndividual);
                                                            String randomSkillListName = extraSkillListAttr.get("name");
                                                            String randomSkillListValue = extraSkillListAttr.get("value");
                                                            skillList.put(Integer.parseInt(randomSkillListName), Integer.parseInt(randomSkillListValue));
                                                        }
                                                        break;
                                                    default:
                                                        if (LOG_UNKS) {
                                                            System.out.printf("Unknown Extra Skill Info Name: %s", randomSkillName);
                                                        }
                                                        break;
                                                }
                                            }
                                            if (!skillList.isEmpty()) {
                                                skill.addRandomSkill(skillList, prob);
                                            }
                                        }
                                        break;
                                }
                            }
                            target.put(skillId, skill);
                        }
                    }
                }
            }
    }

    private static SkillInfo.SecondAtomInfo copySecondAtomInfo(SkillInfo.SecondAtomInfo src) {
        SkillInfo.SecondAtomInfo dst = new SkillInfo.SecondAtomInfo();
        dst.setCreateDelay(src.getCreateDelay());
        dst.setEnableDelay(src.getEnableDelay());
        dst.setRotate(src.getRotate());
        dst.setExpire(src.getExpire());
        dst.setAttackableCount(src.getAttackableCount());
        dst.setDataIndex(src.getDataIndex());
        dst.setFirstAngleStart(src.getFirstAngleStart());
        dst.setFirstAngleRange(src.getFirstAngleRange());
        dst.setNotRotateEndEffect(src.getNotRotateEndEffect());
        dst.setPosRandomOffset(src.getPosRandomOffset());
        dst.setFollowAngle(src.getFollowAngle());
        dst.setLocalOnly(src.getLocalOnly());

        // copy maps (đừng share reference)
        Int2IntMap c = new Int2IntOpenHashMap();
        c.putAll(src.getCustoms());
        dst.setCustoms(c);

        Int2ObjectMap<Position> ep = new Int2ObjectOpenHashMap<>();
        ep.putAll(src.getExtraPos());
        dst.setExtraPos(ep);

        return dst;
    }

    public static void loadSkillStatData() {
        for (SkillInfo si : skills.values()) {
            si.load();
        }
    }

    public static Map<Integer, SkillInfo> getPetPassiveSkillInfos() {
        if (petPassiveSkills.isEmpty()) {
            for (int skillID = 80000000; skillID < 80020000; skillID++) {
                SkillInfo si = skills.get(skillID);
                if (si != null && si.isPetPassive()) {
                    si.setInvisible(true);
                    petPassiveSkills.put(skillID, si);
                }
            }
        }
        return petPassiveSkills;
    }

    public static SkillInfo getPetPassiveSkillInfoById(int skillId) {
        return getPetPassiveSkillInfos().get(skillId);
    }

    public static SkillInfo getSkillInfoById(int skillId) {
        SkillInfo skillInfo = getLoadedSkillInfo(skillId);
        if (skillInfo != null) {
            return skillInfo;
        }
        int linkedSkillId = SkillConstants.getActualSkillIDfromSkillID(skillId);
        if (linkedSkillId != skillId) {
            SkillInfo linkedInfo = getLoadedSkillInfo(linkedSkillId);
            if (linkedInfo != null) {
                return copySkillInfoWithId(linkedInfo, skillId);
            }
        }
        if (HexaBoostSkillConstants.isHexaBoostSkill(skillId)) {
            int baseSkillId = HexaBoostSkillConstants.getBaseSkillId(skillId);
            if (baseSkillId != skillId) {
                SkillInfo baseInfo = getLoadedSkillInfo(baseSkillId);
                if (baseInfo != null) {
                    return copySkillInfoWithId(baseInfo, skillId);
                }
            }
        }
        return null;
    }

    private static SkillInfo copySkillInfoWithId(SkillInfo source, int skillId) {
        SkillInfo copy = new SkillInfo();
        copy.setSkillId(skillId);
        copy.setRootId(skillId / 10000);
        copy.setMaxLevel(source.getMaxLevel());
        copy.setMasterLevel(source.getMasterLevel());
        copy.setFixLevel(source.getFixLevel());
        copy.setReqLevel(source.getReqLevel());
        copy.setInvisible(source.isInvisible());
        copy.setMassSpell(source.isMassSpell());
        copy.setType(source.getType());
        copy.setOriginSkill(source.isOriginSkill());
        copy.setAscentSkill(source.isAscentSkill());
        copy.setNotCooltimeReset(source.isNotCooltimeReset());
        copy.setShootObject(source.isShootObject());
        for (Map.Entry<SkillStat, String> entry : source.getSkillStatInfo().entrySet()) {
            copy.addSkillStatInfo(entry.getKey(), entry.getValue());
        }
        return Tms282SkillRequirements.enrichOverlay(copy);
    }

    /** 282 profile only: data/dat282/skills (the 282 client's skill books), read before the 265 skills. */
    private static final net.swordie.ms.loaders.overlay.OverlayRecordCache<SkillInfo> OVERLAY_SKILLS =
            new net.swordie.ms.loaders.overlay.OverlayRecordCache<>("skills");

    private static SkillInfo getLoadedSkillInfo(int skillId) {
        SkillInfo special = specialSkills().get(skillId);
        if (special != null) {
            return special;
        }
        SkillInfo overlay = OVERLAY_SKILLS.get(skillId, file -> readSkill(file, false));
        if (overlay != null) {
            return Tms282LegacySkillLevels.enrichOverlay(overlay);
        }
        SkillInfo skillInfo = skills.get(skillId);
        if (skillInfo != null) {
            return skillInfo;
        }
        File file = new File(String.format("%s/skills/%d.dat", ServerConstants.DAT_DIR, skillId));
        if (file.exists()) {
            return loadSkill(file);
        }
        return null;
    }

    public static Iterable<SkillInfo> getLoadedSkillInfos() {
        Int2ObjectMap<SkillInfo> all = new Int2ObjectOpenHashMap<>(skills);
        all.putAll(specialSkills());
        return all.values();
    }

    public static Skill getSkillDeepCopyById(int skillId) {
        if (SkillConstants.isMakingSkill(skillId)) { // Recipe Skill
            Skill skill = new Skill();
            skill.setSkillId(skillId);
            skill.setRootId(skillId / 10000);
            skill.setMasterLevel(1);
            skill.setMaxLevel(0);
            skill.setCurrentLevel(1 << 24); // Making Skill SLV is weird.
            return skill;
        } else {
            SkillInfo si = getSkillInfoById(skillId);
            if (si == null) {
                return null;
            }
            Skill skill = new Skill();
            skill.setSkillId(skillId);
            skill.setRootId(skillId / 10000);
            if (si.getRootId() - (int) (Math.floor(si.getSkillId() / 100000D) * 10) >= 2) { // Make sure it's a 4th job skills
                skill.setMasterLevel(si.getMasterLevel());
            } else {
                skill.setMasterLevel(si.getMaxLevel());
            }
            skill.setMaxLevel(si.getMaxLevel());
            if (si.getMasterLevel() <= 0) {
                skill.setMasterLevel(skill.getMaxLevel());
            }
            if (si.getFixLevel() > 0) {
                skill.setCurrentLevel(si.getFixLevel());
            } else {
                skill.setCurrentLevel(0);
            }
            return skill;
        }
    }

    public static List<Skill> getSkillsByJob(short id) {
        return getSkillsByJob(id, false);
    }

    public static List<Skill> getAllSkillsByJob(short id) {
        List<Skill> res = new ArrayList<>();
        (SPECIAL_282_BOOKS.contains((int) id) ? specialSkills() : skills).forEach((key, si) -> {
            if (si.getRootId() == id) {
                res.add(getSkillDeepCopyById(key));
            }
        });
        return res;
    }

    private static List<Skill> getSkillsByJob(short id, boolean rec) {
        List<Skill> res = new ArrayList<>();
        (SPECIAL_282_BOOKS.contains((int) id) ? specialSkills() : skills).forEach((key, si) -> {
            if (si.getRootId() == id && !si.isInvisible()) {
                res.add(getSkillDeepCopyById(key));
            }
        });
        if (!rec && res.isEmpty()) {
            loadSkillsOfJob(id);
            return getSkillsByJob(id, true);
        }
        if (!SPECIAL_282_BOOKS.contains((int) id)) {
            addOverlayJobSkills(id, res);
        }
        return res;
    }

    /** 282 profile: skill ids per job that exist in the overlay's skills directory (data/dat282/skills). */
    private static volatile java.util.Map<Integer, List<Integer>> overlayJobIndex;
    private static volatile int overlayJobIndexGeneration = -1;

    /**
     * 282 profile: adds the visible skills of {@code job} that only the 282 skill books have (e.g. Kinesis 142000006,
     * 142121004). The 265 {@code skills} map is filled from the 265 DAT per job, so 282 additions were never part of a
     * job list and Char.setJob / maxSkills never granted them. No-op without the 282 overlay.
     */
    private static void addOverlayJobSkills(short job, List<Skill> res) {
        var overlay = net.swordie.ms.loaders.overlay.DataOverlays.current();
        if (!overlay.isActive() || !(overlay instanceof net.swordie.ms.loaders.overlay.DirectoryDataOverlay dir)) {
            return;
        }
        java.util.Map<Integer, List<Integer>> index = overlayJobIndex;
        int generation = net.swordie.ms.loaders.overlay.DataOverlays.generation();
        if (index == null || overlayJobIndexGeneration != generation) {
            java.util.Map<Integer, List<Integer>> built = new java.util.HashMap<>();
            File[] files = new File(dir.datRoot(), "skills").listFiles();
            if (files != null) {
                for (File f : files) {
                    String n = f.getName();
                    if (!n.endsWith(".dat")) {
                        continue;
                    }
                    try {
                        int sid = Integer.parseInt(n.substring(0, n.length() - 4));
                        built.computeIfAbsent(sid / 10000, k -> new ArrayList<>()).add(sid);
                    } catch (NumberFormatException ignored) {
                        // not a skill book file
                    }
                }
            }
            overlayJobIndex = index = built;
            overlayJobIndexGeneration = generation;
        }
        Set<Integer> present = new java.util.HashSet<>();
        for (Skill s : res) {
            present.add(s.getSkillId());
        }
        for (int sid : index.getOrDefault((int) job, List.of())) {
            if (present.contains(sid)) {
                continue;
            }
            SkillInfo info = getSkillInfoById(sid);
            if (info != null && info.getRootId() == job && !info.isInvisible()) {
                Skill copy = getSkillDeepCopyById(sid);
                if (copy != null) {
                    res.add(copy);
                }
            }
        }
    }

    public static Short2ObjectMap<Short2ObjectMap<MobSkillInfo>> getMobSkillInfos() {
        return mobSkillInfos;
    }

    public static MakingSkillRecipe getRecipeById(int recipeID) {
        MakingSkillRecipe makingSkillRecipe = makingSkillRecipes.get(recipeID);
        if (makingSkillRecipe != null) {
            return makingSkillRecipe;
        }
        // 282 profile: data/dat282/recipes first (282 client Skill/Recipe_92xx); 265: DAT_DIR as before.
        File file = net.swordie.ms.loaders.overlay.DataOverlays.current().datFile(String.format("recipes/%d.dat", recipeID));
        if (file.exists()) {
            makingSkillRecipe = loadRecipe(file);
        }
        return makingSkillRecipe;

    }

    public static void addMobSkillInfo(MobSkillInfo msi) {
        short skillId = msi.getId();
        Short2ObjectMap<MobSkillInfo> levelMap = mobSkillInfos.get(skillId);
        if (levelMap == null) {
            levelMap = new Short2ObjectOpenHashMap<>();
            mobSkillInfos.put(skillId, levelMap);
        }
        levelMap.put(msi.getLevel(), msi);
    }

    private static void loadEliteMobSkillsFromWZ() {
        String wzDir = ServerConstants.WZ_DIR + "/Skill.wz/Skill_00007/EliteMobSkill.xml";
        File file = new File(wzDir);
        Node root = XMLApi.getRoot(file);
        Node mainNode = XMLApi.getAllChildren(root).getFirst();
        List<Node> nodes = XMLApi.getAllChildren(mainNode);
        for (Node node : nodes) {
            int grade = Integer.parseInt(XMLApi.getNamedAttribute(node, "name"));
            for (Node skillNode : XMLApi.getAllChildren(node)) {
                Node skillIdNode = XMLApi.getFirstChildByNameBF(skillNode, "skill");
                Node skillLevelNode = XMLApi.getFirstChildByNameBF(skillNode, "level");
                int skillID = Integer.parseInt(XMLApi.getNamedAttribute(skillIdNode, "value"));
                int skillLevel = Integer.parseInt(XMLApi.getNamedAttribute(skillLevelNode, "value"));
                addEliteMobSkill(grade, skillID, skillLevel);
            }
        }
    }

    private static void addEliteMobSkill(int grade, int skillID, int skillLevel) {
        Int2IntMap skills = eliteMobSkills.get(grade);
        if (skills == null) {
            skills = new Int2IntOpenHashMap();
            eliteMobSkills.put(grade, skills);
        }
        skills.put(skillID, skillLevel);
    }

    @Saver(varName = "eliteMobSkills")
    private static void saveEliteMobSkills(File file) {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
            dos.writeInt(eliteMobSkills.size());
            for (var entry : eliteMobSkills.int2ObjectEntrySet()) {
                int grade = entry.getIntKey();
                Int2IntMap skills = entry.getValue();

                dos.writeInt(grade);
                dos.writeInt(skills.size());
                for (var inner : skills.int2IntEntrySet()) {
                    dos.writeInt(inner.getIntKey());    // skillID
                    dos.writeInt(inner.getIntValue());  // skillLevel
                }
            }
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    @Loader(varName = "eliteMobSkills")
    public static void loadEliteMobSkills(File file, boolean exists) {
        // 282 profile: data/dat282/eliteMobSkills.dat (282 client Skill/EliteMobSkill) replaces the 265 table.
        var overlay = net.swordie.ms.loaders.overlay.DataOverlays.current().overlayDat("eliteMobSkills.dat");
        if (overlay.isPresent()) {
            file = overlay.get();
            exists = true;
        }
        if (!exists) {
            loadEliteMobSkillsFromWZ();
            saveEliteMobSkills(file);
        } else {
            try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
                int gradeSize = dis.readInt();
                for (int i = 0; i < gradeSize; i++) {
                    int grade = dis.readInt();
                    int gradeSkillSize = dis.readInt();
                    for (int j = 0; j < gradeSkillSize; j++) {
                        int skillID = dis.readInt();
                        int skillLevel = dis.readInt();
                        addEliteMobSkill(grade, skillID, skillLevel);
                    }
                }
            } catch (IOException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            }
        }
    }

    public static Map<Integer, Integer> getEliteMobSkillsByGrade(int grade) {
        return eliteMobSkills.getOrDefault(grade, null);
    }

    public static void loadMobSkillsFromWz() {
        String wzDir = ServerConstants.WZ_DIR + "/Skill.wz/Skill_00007/MobSkill";
        File topFile = new File(wzDir);
        for (File file : topFile.listFiles()) {
            if (file.isDirectory()) continue;
            short skillID = Short.parseShort(file.getName().replace(".xml", ""));
            Node root = XMLApi.getRoot(file);
            Node mainNode = XMLApi.getAllChildren(root).getFirst();
            Node litLevelNode = XMLApi.getFirstChildByNameBF(mainNode, "level");
            List<Node> nodes = XMLApi.getAllChildren(litLevelNode);
            for (Node levelNode : nodes) {
                short level = Short.parseShort(XMLApi.getNamedAttribute(levelNode, "name"));
                MobSkillInfo msi = new MobSkillInfo();
                msi.setId(skillID);
                msi.setLevel(level);
                for (Node skillStatNode : XMLApi.getAllChildren(levelNode)) {
                    String name = XMLApi.getNamedAttribute(skillStatNode, "name");
                    String value = XMLApi.getNamedAttribute(skillStatNode, "value");
                    String x = XMLApi.getNamedAttribute(skillStatNode, "x");
                    String y = XMLApi.getNamedAttribute(skillStatNode, "y");
                    if (Util.isNumber(name)) {
                        if (value != null) {
                            msi.addIntToList(Integer.parseInt(value));
                            continue;
                        }
                    }
                    switch (name) {
                        case "x":
                            msi.putMobSkillStat(MobSkillStat.x, value);
                            break;
                        case "mpCon":
                            msi.putMobSkillStat(MobSkillStat.mpCon, value);
                            break;
                        case "interval":
                        case "inteval":
                            msi.putMobSkillStat(MobSkillStat.interval, value);
                            break;
                        case "hp":
                        case "HP":
                            msi.putMobSkillStat(MobSkillStat.hp, value);
                            break;
                        case "info":
                            msi.putMobSkillStat(MobSkillStat.info, value);
                            break;
                        case "y":
                            msi.putMobSkillStat(MobSkillStat.y, value);
                            break;
                        case "lt":
                            msi.setLt(XMLApi.parsePos(skillStatNode));
                            break;
                        case "rb":
                            msi.setRb(XMLApi.parsePos(skillStatNode));
                            break;
                        case "lt2":
                            msi.setLt2(XMLApi.parsePos(skillStatNode));
                            break;
                        case "rb2":
                            msi.setRb2(XMLApi.parsePos(skillStatNode));
                            break;
                        case "lt3":
                            msi.setLt3(XMLApi.parsePos(skillStatNode));
                            break;
                        case "rb3":
                            msi.setRb3(XMLApi.parsePos(skillStatNode));
                            break;
                        case "limit":
                            msi.putMobSkillStat(MobSkillStat.limit, value);
                            break;
                        case "broadCastScreenMsg":
                            msi.putMobSkillStat(MobSkillStat.broadCastScreenMsg, value);
                            break;
                        case "w":
                            msi.putMobSkillStat(MobSkillStat.w, value);
                            break;
                        case "z":
                            msi.putMobSkillStat(MobSkillStat.z, value);
                            break;
                        case "parsing":
                            msi.putMobSkillStat(MobSkillStat.parsing, value);
                            break;
                        case "prop":
                            msi.putMobSkillStat(MobSkillStat.prop, value);
                            break;
                        case "ignoreResist":
                            msi.putMobSkillStat(MobSkillStat.ignoreResist, value);
                            break;
                        case "count":
                            msi.putMobSkillStat(MobSkillStat.count, value);
                            break;
                        case "time":
                            msi.putMobSkillStat(MobSkillStat.time, value);
                            break;
                        case "targetAggro":
                            msi.putMobSkillStat(MobSkillStat.targetAggro, value);
                            break;
                        case "fieldScript":
                            msi.putMobSkillStat(MobSkillStat.fieldScript, value);
                            break;
                        case "elemAttr":
                            msi.putMobSkillStat(MobSkillStat.elemAttr, value);
                            break;
                        case "delay":
                            msi.putMobSkillStat(MobSkillStat.delay, value);
                            break;
                        case "rank":
                            msi.putMobSkillStat(MobSkillStat.rank, value);
                            break;
                        case "HPDeltaR":
                            msi.putMobSkillStat(MobSkillStat.HPDeltaR, value);
                            break;
                        case "summonEffect":
                            msi.putMobSkillStat(MobSkillStat.summonEffect, value);
                            break;
                        case "y2":
                            msi.putMobSkillStat(MobSkillStat.y2, value);
                            break;
                        case "q":
                            msi.putMobSkillStat(MobSkillStat.q, value);
                            break;
                        case "q2":
                            msi.putMobSkillStat(MobSkillStat.q2, value);
                            break;
                        case "s2":
                            msi.putMobSkillStat(MobSkillStat.s2, value);
                            break;
                        case "u":
                            msi.putMobSkillStat(MobSkillStat.u, value);
                            break;
                        case "u2":
                            msi.putMobSkillStat(MobSkillStat.u2, value);
                            break;
                        case "v":
                            msi.putMobSkillStat(MobSkillStat.v, value);
                            break;
                        case "z2":
                            msi.putMobSkillStat(MobSkillStat.z2, value);
                            break;
                        case "w2":
                            msi.putMobSkillStat(MobSkillStat.w2, value);
                            break;
                        case "skillAfter":
                            msi.putMobSkillStat(MobSkillStat.skillAfter, value);
                            break;
                        case "x2":
                            msi.putMobSkillStat(MobSkillStat.x2, value);
                            break;
                        case "script":
                            msi.putMobSkillStat(MobSkillStat.script, value);
                            break;
                        case "attackSuccessProp":
                            msi.putMobSkillStat(MobSkillStat.attackSuccessProp, value);
                            break;
                        case "bossHeal":
                            msi.putMobSkillStat(MobSkillStat.bossHeal, value);
                            break;
                        case "face":
                            msi.putMobSkillStat(MobSkillStat.face, value);
                            break;
                        case "callSkill":
                            msi.putMobSkillStat(MobSkillStat.callSkill, value);
                            break;
                        case "level":
                            msi.putMobSkillStat(MobSkillStat.level, value);
                            break;
                        case "linkHP":
                            msi.putMobSkillStat(MobSkillStat.linkHP, value);
                            break;
                        case "timeLimitedExchange":
                            msi.putMobSkillStat(MobSkillStat.timeLimitedExchange, value);
                            break;
                        case "summonDir":
                            msi.putMobSkillStat(MobSkillStat.summonDir, value);
                            break;
                        case "summonTerm":
                            msi.putMobSkillStat(MobSkillStat.summonTerm, value);
                            break;
                        case "castingTime":
                            msi.putMobSkillStat(MobSkillStat.castingTime, value);
                            break;
                        case "subTime":
                            msi.putMobSkillStat(MobSkillStat.subTime, value);
                            break;
                        case "reduceCasting":
                            msi.putMobSkillStat(MobSkillStat.reduceCasting, value);
                            break;
                        case "additionalTime":
                            msi.putMobSkillStat(MobSkillStat.additionalTime, value);
                            break;
                        case "force":
                            msi.putMobSkillStat(MobSkillStat.force, value);
                            break;
                        case "targetType":
                            msi.putMobSkillStat(MobSkillStat.targetType, value);
                            break;
                        case "forcex":
                            msi.putMobSkillStat(MobSkillStat.forcex, value);
                            break;
                        case "sideAttack":
                            msi.putMobSkillStat(MobSkillStat.sideAttack, value);
                            break;
                        case "afterEffect":
                        case "rangeGap":
                            msi.putMobSkillStat(MobSkillStat.rangeGap, value);
                            break;
                        case "noGravity":
                            msi.putMobSkillStat(MobSkillStat.noGravity, value);
                            break;
                        case "notDestroyByCollide":
                            msi.putMobSkillStat(MobSkillStat.notDestroyByCollide, value);
                            break;
                        case "effect":
                        case "mob":
                        case "mob0":
                        case "hit":
                        case "affected":
                        case "affectedOtherSkill":
                        case "crash":
                        case "effectToUser":
                        case "affected_after":
                        case "fixDamR":
                        case "limitMoveSkill":
                        case "tile":
                        case "footholdRect":
                        case "targetMobType":
                        case "areaWarning":
                        case "arType":
                        case "tremble":
                        case "otherSkill":
                        case "etcEffect":
                        case "etcEffect1":
                        case "etcEffect2":
                        case "etcEffect3":
                        case "bombInfo":
                        case "affected_pre":
                        case "fixDamR_BT":
                        case "affectedPhase":
                        case "screen":
                        case "notMissAttack":
                        case "ignoreEvasion":
                        case "fadeinfo":
                        case "randomTarget":
                        case "option_linkedMob":
                        case "affected0":
                        case "summonOnce":
                        case "head":
                        case "mobGroup":
                        case "exceptRange":
                        case "exchangeAttack":
                        case "range":
                        case "addDam":
                        case "special":
                        case "target":
                        case "fixedPos":
                        case "fixedDir":
                        case "i52":
                        case "start":
                        case "cancleType":
                        case "succeed":
                        case "failed":
                        case "during":
                        case "castingBarHide":
                        case "skillCancelAlways":
                        case "cancleDamage":
                        case "cancleDamageMultiplier":
                        case "bounceBall":
                        case "info2":
                        case "regen":
                        case "kockBackD":
                        case "areaSequenceDelay":
                        case "areaSequenceRandomSplit":
                        case "accelerationEffect":
                        case "repeatEffect":
                        case "brightness":
                        case "brightnessDuration":
                        case "success":
                        case "fail":
                        case "affected_S":
                        case "appear":
                        case "affected_XS":
                        case "disappear":
                        case "command":
                        case "damIncPos": // May be useful
                        case "option_poison": // ?
                        case "phaseUserCount": // I think this is done client side (users hit mapped to phase?)
                            break;
                        default:
                            break;
                    }
                }
                addMobSkillInfo(msi);
            }
        }
    }

    public static void saveMobSkillsToDat(String dir) {
        Util.makeDirIfAbsent(dir);
        for (var entry : getMobSkillInfos().short2ObjectEntrySet()) {
            short id = entry.getShortKey();
            Short2ObjectMap<MobSkillInfo> byLevel = entry.getValue();

            for (var entry2 : byLevel.short2ObjectEntrySet()) {
                short level = entry2.getShortKey();
                MobSkillInfo msi = entry2.getValue();

                File file = new File(String.format("%s/%d-%d.dat", dir, id, level));
                try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
                    dos.writeShort(msi.getId());
                    dos.writeShort(msi.getLevel());
                    boolean hasLt = msi.getLt() != null;
                    dos.writeBoolean(hasLt);
                    if (hasLt) {
                        dos.writeInt(msi.getLt().getX());
                        dos.writeInt(msi.getLt().getY());
                    }
                    boolean hasRb = msi.getRb() != null;
                    dos.writeBoolean(hasRb);
                    if (hasRb) {
                        dos.writeInt(msi.getRb().getX());
                        dos.writeInt(msi.getRb().getY());
                    }
                    boolean hasLt2 = msi.getLt2() != null;
                    dos.writeBoolean(hasLt2);
                    if (hasLt2) {
                        dos.writeInt(msi.getLt2().getX());
                        dos.writeInt(msi.getLt2().getY());
                    }
                    boolean hasRb2 = msi.getRb2() != null;
                    dos.writeBoolean(hasRb2);
                    if (hasRb2) {
                        dos.writeInt(msi.getRb2().getX());
                        dos.writeInt(msi.getRb2().getY());
                    }
                    boolean hasLt3 = msi.getLt3() != null;
                    dos.writeBoolean(hasLt3);
                    if (hasLt3) {
                        dos.writeInt(msi.getLt3().getX());
                        dos.writeInt(msi.getLt3().getY());
                    }
                    boolean hasRb3 = msi.getRb3() != null;
                    dos.writeBoolean(hasRb3);
                    if (hasRb3) {
                        dos.writeInt(msi.getRb3().getX());
                        dos.writeInt(msi.getRb3().getY());
                    }
                    dos.writeShort(msi.getMobSkillStats().size());
                    for (Map.Entry<MobSkillStat, String> msiString : msi.getMobSkillStats().entrySet()) {
                        dos.writeByte(msiString.getKey().ordinal());
                        dos.writeUTF(msiString.getValue());
                    }
                    dos.writeShort(msi.getInts().size());
                    for (int i : msi.getInts()) {
                        dos.writeInt(i);
                    }
                } catch (IOException e) {
                    DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
                }
            }
        }
    }

    private static MobSkillInfo loadMobSkillFromFile(File f) {
        MobSkillInfo msi = null;
        try (DataInputStream dis = new DataInputStream(new FileInputStream(f))) {
            msi = new MobSkillInfo();
            msi.setId(dis.readShort());
            msi.setLevel(dis.readShort());
            boolean hasPos = dis.readBoolean();
            if (hasPos) {
                msi.setLt(new Position(dis.readInt(), dis.readInt()));
            }
            hasPos = dis.readBoolean();
            if (hasPos) {
                msi.setRb(new Position(dis.readInt(), dis.readInt()));
            }
            hasPos = dis.readBoolean();
            if (hasPos) {
                msi.setLt2(new Position(dis.readInt(), dis.readInt()));
            }
            hasPos = dis.readBoolean();
            if (hasPos) {
                msi.setRb2(new Position(dis.readInt(), dis.readInt()));
            }
            hasPos = dis.readBoolean();
            if (hasPos) {
                msi.setLt3(new Position(dis.readInt(), dis.readInt()));
            }
            hasPos = dis.readBoolean();
            if (hasPos) {
                msi.setRb3(new Position(dis.readInt(), dis.readInt()));
            }
            short size = dis.readShort();
            for (int i = 0; i < size; i++) {
                MobSkillStat mss = MobSkillStat.values()[dis.readByte()];
                String value = dis.readUTF();
                msi.putMobSkillStat(mss, value);
            }
            size = dis.readShort();
            for (int i = 0; i < size; i++) {
                msi.addIntToList(dis.readInt());
            }
            addMobSkillInfo(msi);
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
        return msi;
    }

    private static MobSkillInfo getMobSkillInfoByIdAndLevel(short id, short level) {
        Short2ObjectMap<MobSkillInfo> innerMap = mobSkillInfos.get(id);
        if (innerMap != null) {
            MobSkillInfo cached = innerMap.get(level);
            if (cached != null) {
                return cached;
            }
        }
        MobSkillInfo msi = loadMobSkillInfoByIdAndLevel(id, level);
        if (msi != null) {
            addMobSkillInfo(msi);
        }
        return msi;
    }

    public static MobSkillInfo getMobSkillInfoByIdAndLevel(int id, int level) {
        return getMobSkillInfoByIdAndLevel((short) id, (short) level);
    }

    private static MobSkillInfo loadMobSkillInfoByIdAndLevel(int id, short level) {
        // 282 profile: data/dat282/mobSkills first (282 client Skill/MobSkill, 2026-09-29); 265: DAT_DIR as before.
        File file = net.swordie.ms.loaders.overlay.DataOverlays.current().datFile(String.format("mobSkills/%d-%d.dat", id, level));
        if (file.exists()) {
            return loadMobSkillFromFile(file);
        } else {
            System.out.printf("Could not load mob skill %d (level %d).%n", id, level);
            return null;
        }
    }

    public static void loadMakingRecipeSkillsFromWz() {
        int[] recipes = {9200, 9201, 9202, 9203, 9204};
        for (Integer recipeCategory : recipes) {
            String wzDir = String.format(ServerConstants.WZ_DIR + "/Skill.wz/Skill_00007/Recipe_%d.xml", recipeCategory);
            File file = new File(wzDir);
            Node root = XMLApi.getRoot(file);
            if (root == null) {
                continue;
            }
            Node mainNode = XMLApi.getAllChildren(root).getFirst();
            List<Node> nodes = XMLApi.getAllChildren(mainNode);
            for (Node node : nodes) {
                MakingSkillRecipe msr = new MakingSkillRecipe();
                int recipeID = Integer.parseInt(XMLApi.getNamedAttribute(node, "name"));
                msr.setRecipeID(recipeID);
                msr.setReqSkillID(10000 * (recipeID / 10000));
                for (Node recipe : XMLApi.getAllChildren(node)) {
                    String name = XMLApi.getNamedAttribute(recipe, "name");
                    String value = XMLApi.getNamedAttribute(recipe, "value");
                    switch (name) {
                        case "target":
                            for (Node targets : XMLApi.getAllChildren(recipe)) {
                                MakingSkillRecipe.TargetElem tar = new MakingSkillRecipe.TargetElem();
                                for (Node target : XMLApi.getAllChildren(targets)) {
                                    String targetName = XMLApi.getNamedAttribute(target, "name");
                                    int targetValue = Integer.parseInt(XMLApi.getNamedAttribute(target, "value"));
                                    switch (targetName) {
                                        case "item":
                                            tar.setItemID(targetValue);
                                            break;
                                        case "count":
                                            tar.setCount(targetValue);
                                            break;
                                        case "probWeight":
                                            tar.setProbWeight(targetValue);
                                            break;
                                        default:
                                            if (LOG_UNKS) {
                                                System.out.println("Unknown target value " + targetName);
                                            }
                                            break;
                                    }
                                }
                                msr.addTarget(tar);
                            }
                            break;
                        case "weatherItem":
                            msr.setWeatherItemID(Integer.parseInt(value));
                            break;
                        case "incSkillProficiency":
                            msr.setIncSkillProficiency(Integer.parseInt(value));
                            break;
                        case "incSkillProficiencyOnFailure":
                            msr.setIncSkillProficiencyOnFailure(Integer.parseInt(value));
                            break;
                        case "incSkillMasterProficiency":
                            msr.setIncSkillMasterProficiency(Integer.parseInt(value));
                            break;
                        case "incSkillMasterProficiencyOnFailure":
                            msr.setIncSkillMasterProficiencyOnFailure(Integer.parseInt(value));
                            break;
                        case "incFatigability":
                            msr.setIncFatigability(Integer.parseInt(value));
                            break;
                        case "addedCoolProb":
                            msr.setAddedCoolProb(Integer.parseInt(value));
                            break;
                        case "coolTimeSec":
                            msr.setCoolTimeSec(Integer.parseInt(value));
                            break;
                        case "addedTimeTaken":
                            msr.setAddedSecForMaxGauge(Integer.parseInt(value));
                            break;
                        case "period":
                            msr.setExpiredPeriod(Integer.parseInt(value));
                            break;
                        case "premium":
                            msr.setPremiumItem(Integer.parseInt(value) != 0);
                            break;
                        case "needOpenItem":
                            msr.setNeedOpenItem(Integer.parseInt(value) != 0);
                            break;
                        case "reqSkillLevel":
                            msr.setRecommandedSkillLevel(Integer.parseInt(value));
                            break;
                        case "reqSkillProficiency":
                            msr.setReqSkillProficiency(Integer.parseInt(value));
                            break;
                        case "reqMeso":
                            msr.setReqMeso(Integer.parseInt(value));
                            break;
                        case "reqMapObjectTag":
                            msr.setReqMapObjectTag(value);
                            break;
                        case "recipe":
                            for (Node ingredients : XMLApi.getAllChildren(recipe)) {
                                int itemID = -1, count = -1;
                                for (Node ingredient : XMLApi.getAllChildren(ingredients)) {
                                    String ingredientName = XMLApi.getNamedAttribute(ingredient, "name");
                                    int ingredientValue = Integer.parseInt(XMLApi.getNamedAttribute(ingredient, "value"));
                                    switch (ingredientName) {
                                        case "item":
                                            itemID = ingredientValue;
                                            break;
                                        case "count":
                                            count = ingredientValue;
                                            break;
                                        default:
                                            if (LOG_UNKS) {
                                                System.out.println("Unknown ingredient value " + ingredientName);
                                            }
                                            break;
                                    }
                                }
                                if (itemID != -1 && count != -1) {
                                    msr.addIngredient(itemID, count);
                                }
                            }
                            break;
                        default:
                            if (LOG_UNKS) {
                                System.out.println("Unknown recipe value " + name);
                            }
                            break;
                    }
                }
                makingSkillRecipes.put(recipeID, msr);
            }
        }
    }

    public static void saveMakingRecipeSkillsToDat(String dir) {
        Util.makeDirIfAbsent(dir);
        for (MakingSkillRecipe msr : makingSkillRecipes.values()) {
            int recipeID = msr.getRecipeID();
            File file = new File(String.format("%s/%d.dat", dir, recipeID));
            try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
                dos.writeInt(recipeID);
                dos.writeShort(msr.getTarget().size());
                for (MakingSkillRecipe.TargetElem target : msr.getTarget()) {
                    dos.writeInt(target.getItemID());
                    dos.writeInt(target.getCount());
                    dos.writeInt(target.getProbWeight());
                }
                dos.writeInt(msr.getWeatherItemID());
                dos.writeInt(msr.getIncSkillProficiency());
                dos.writeInt(msr.getIncSkillProficiencyOnFailure());
                dos.writeInt(msr.getIncFatigability());
                dos.writeInt(msr.getIncSkillMasterProficiency());
                dos.writeInt(msr.getIncSkillMasterProficiencyOnFailure());
                dos.writeBoolean(msr.isNeedOpenItem());
                dos.writeInt(msr.getReqSkillID());
                dos.writeInt(msr.getRecommandedSkillLevel());
                dos.writeInt(msr.getReqSkillProficiency());
                dos.writeInt(msr.getReqMeso());
                dos.writeUTF(msr.getReqMapObjectTag());
                dos.writeShort(msr.getIngredient().size());
                for (Tuple<Integer, Integer> ingredient : msr.getIngredient()) {
                    dos.writeInt(ingredient.getLeft());
                    dos.writeInt(ingredient.getRight());
                }
                dos.writeInt(msr.getAddedCoolProb());
                dos.writeInt(msr.getCoolTimeSec());
                dos.writeInt(msr.getAddedSecForMaxGauge());
                dos.writeInt(msr.getExpiredPeriod());
                dos.writeBoolean(msr.isPremiumItem());
            } catch (IOException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            }
        }
    }

    public static MakingSkillRecipe loadRecipe(File file) {
        MakingSkillRecipe msr = null;
        try (DataInputStream dataInputStream = new DataInputStream(new FileInputStream(file))) {
            msr = new MakingSkillRecipe();
            msr.setRecipeID(dataInputStream.readInt());
            short targets = dataInputStream.readShort();
            for (int j = 0; j < targets; j++) {
                MakingSkillRecipe.TargetElem target = new MakingSkillRecipe.TargetElem();
                target.setItemID(dataInputStream.readInt());
                target.setCount(dataInputStream.readInt());
                target.setProbWeight(dataInputStream.readInt());
                msr.addTarget(target);
            }
            msr.setWeatherItemID(dataInputStream.readInt());
            msr.setIncSkillProficiency(dataInputStream.readInt());
            msr.setIncSkillProficiencyOnFailure(dataInputStream.readInt());
            msr.setIncFatigability(dataInputStream.readInt());
            msr.setIncSkillMasterProficiency(dataInputStream.readInt());
            msr.setIncSkillMasterProficiencyOnFailure(dataInputStream.readInt());
            msr.setNeedOpenItem(dataInputStream.readBoolean());
            msr.setReqSkillID(dataInputStream.readInt());
            msr.setRecommandedSkillLevel(dataInputStream.readInt());
            msr.setReqSkillProficiency(dataInputStream.readInt());
            msr.setReqMeso(dataInputStream.readInt());
            msr.setReqMapObjectTag(dataInputStream.readUTF());
            short ingredients = dataInputStream.readShort();
            for (int j = 0; j < ingredients; j++) {
                msr.addIngredient(dataInputStream.readInt(), dataInputStream.readInt());
            }
            msr.setAddedCoolProb(dataInputStream.readInt());
            msr.setCoolTimeSec(dataInputStream.readInt());
            msr.setAddedSecForMaxGauge(dataInputStream.readInt());
            msr.setExpiredPeriod(dataInputStream.readInt());
            msr.setPremiumItem(dataInputStream.readBoolean());
            makingSkillRecipes.put(msr.getRecipeID(), msr);
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
        return msr;
    }

    public static void generateDatFiles() {
        System.out.println("Started generating skill data.");
        long start = System.currentTimeMillis();
        loadSkillsFromWz();
        saveSkills(ServerConstants.DAT_DIR + "/skills");
        System.out.printf("Completed generating skill data in %dms%n", System.currentTimeMillis() - start);
        System.out.println("Started generating mob skill data.");
        start = System.currentTimeMillis();
        loadMobSkillsFromWz();
        saveMobSkillsToDat(ServerConstants.DAT_DIR + "/mobSkills");
        System.out.printf("Completed generating mob skill data in %dms%n", System.currentTimeMillis() - start);
        System.out.println("Started generating recipe skill data.");
        start = System.currentTimeMillis();
        loadMakingRecipeSkillsFromWz();
        saveMakingRecipeSkillsToDat(ServerConstants.DAT_DIR + "/recipes");
        System.out.printf("Completed generating recipe skill data in %dms%n", System.currentTimeMillis() - start);

    }

    public static void main(String[] args) {
        StringData.load();
        generateDatFiles();
    }

    public static void clear() {
        skills.clear();
        mobSkillInfos.clear();
        makingSkillRecipes.clear();
    }

    public static void load() {
        loadSkillsFromWz();
        loadMobSkillsFromWz();
        loadMakingRecipeSkillsFromWz();
    }
}
