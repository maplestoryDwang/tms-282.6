package net.swordie.ms.constants;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.hexa.HexaSkill;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.client.jobs.Jianghu.MoXuan;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.loaders.Etc.HexaCore.HexaCore;
import net.swordie.ms.loaders.SkillData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sixth job origin (H1) and ascent (H2) skill mappings.
 * Origin cores use IDs 10000000+; activating them should also grant the ascent skill.
 */
public final class SixthJobSkillConstants {

    public record OriginAscentEntry(int originCoreId, int fourthJobId, int originSkillId, int ascentSkillId, String name) {
    }

    private static final Map<Integer, OriginAscentEntry> BY_ORIGIN_CORE = new LinkedHashMap<>();
    private static final Map<Integer, OriginAscentEntry> BY_ORIGIN_SKILL = new LinkedHashMap<>();
    private static final Map<Integer, Integer> ORIGIN_TO_ASCENT_OVERRIDE = new LinkedHashMap<>();
    private static final Set<Integer> KNOWN_ASCENT_SKILL_IDS = new HashSet<>();
    private static final Set<Integer> KNOWN_ORIGIN_SKILL_IDS = new HashSet<>();

    static {
        register(10000000, 112, 1141500, "聖劍降臨");
        register(10000001, 122, 1241500, "聖域展開");
        register(10000002, 132, 1341500, "滅世永恆之槍");
        register(10000003, 212, 2141500, "劇毒煉獄");
        register(10000004, 222, 2241500, "殛凍領域");
        register(10000005, 232, 2341500, "天堂神罰");
        register(10000006, 312, 3141500, "箭影紛飛");
        register(10000007, 322, 3241500, "裂空狙擊");
        register(10000008, 332, 3341500, "忘卻遺蹟");
        register(10000009, 412, 4141500, "禁咒之符");
        register(10000010, 422, 4241500, "一刀兩斷");
        register(10000011, 434, 4361500, "無盡阿修羅");
        register(10000012, 512, 5141500, "海龍霸拳");
        register(10000013, 522, 5241500, "非常召集");
        register(10000014, 532, 5341500, "究極炸裂加農炮");
        register(10000015, 5112, 51141500, "輝煌聖劍");
        register(10000016, 1112, 11141500, "銀河星爆");
        register(10000017, 1212, 12141500, "永恆鳳炎");
        register(10000018, 1312, 13141500, "風之聖諭");
        register(10000019, 1412, 14141500, "靜謐之夜");
        register(10000020, 1512, 15141500, "浪馳雷掣");
        register(10000021, 2112, 21141500, "鬥破山河");
        register(10000022, 2217, 22201500, "龍星羣");
        register(10000023, 2712, 27141500, "創世紀");
        register(10000024, 2312, 23141500, "不朽榮光");
        register(10000025, 2412, 24141500, "命運詠歎調");
        register(10000026, 2512, 25141500, "狐神附體");
        register(10000027, 3712, 37141500, "星隕重拳");
        register(10000028, 3212, 32141500, "緋紅契約");
        register(10000029, 3312, 33141500, "絕對美洲豹時間");
        register(10000030, 3612, 36141500, "人工進化");
        register(10000031, 3512, 35141500, "原爆點");
        register(10000032, 3112, 31141500, "夢魘魔域");
        register(10000033, 3122, 31241500, "魔劍鎮魂曲");
        register(10000034, 6112, 61141500, "不滅凱撒意志");
        register(10000035, 6312, 63141500, "惡意主宰");
        register(10000036, 6412, 64141500, "鏈之藝術：屠殺");
        register(10000037, 6512, 65141500, "完美落幕");
        register(10000038, 10112, 101141500, "時之境界");
        register(10000039, 14212, 142141500, "將軍");
        register(10000040, 15112, 151141500, "無限劍制");
        register(10000041, 15212, 152141500, "無垠水晶");
        register(10000042, 15412, 154141500, "魔咒：沙塵暴");
        register(10000043, 15512, 155141500, "亙古凝視的深淵");
        register(10000044, 16212, 162141500, "花花綠綠大草原");
        register(10000045, 16412, 164141500, "仙技：開天闢地");
        register(10000046, 4112, 41141500, "終焉之須佐能乎");
        register(10000047, 4212, 42141500, "十尾妖狐的羈絆");
        register(10000048, 17212, 172141500, "森林守護者");
        register(10000049, 17512, 175141500, "絕技：破荒拳");
        register(10000050, 16112, 161141506, "登龍詩語");
        register(10000051, 18212, 182141500, "天界構想");

        buildMaps();
        rebuildSkillIdSets();
    }

    private SixthJobSkillConstants() {
    }

    private static void register(int originCoreId, int fourthJobId, int originSkillId, String name) {
        int ascentSkillId = deriveAscentSkillId(originSkillId);
        BY_ORIGIN_CORE.put(originCoreId, new OriginAscentEntry(originCoreId, fourthJobId, originSkillId, ascentSkillId, name));
    }

    private static void registerManual(int originSkillId, int ascentSkillId) {
        ORIGIN_TO_ASCENT_OVERRIDE.put(originSkillId, ascentSkillId);
    }

    private static void buildMaps() {
        for (OriginAscentEntry entry : BY_ORIGIN_CORE.values()) {
            BY_ORIGIN_SKILL.put(entry.originSkillId(), entry);
        }
    }

    private static void rebuildSkillIdSets() {
        KNOWN_ASCENT_SKILL_IDS.clear();
        KNOWN_ORIGIN_SKILL_IDS.clear();
        for (OriginAscentEntry entry : BY_ORIGIN_CORE.values()) {
            KNOWN_ORIGIN_SKILL_IDS.add(entry.originSkillId());
            if (entry.ascentSkillId() > 0) {
                KNOWN_ASCENT_SKILL_IDS.add(entry.ascentSkillId());
            }
        }
        for (Map.Entry<Integer, Integer> manual : ORIGIN_TO_ASCENT_OVERRIDE.entrySet()) {
            KNOWN_ORIGIN_SKILL_IDS.add(manual.getKey());
            if (manual.getValue() > 0) {
                KNOWN_ASCENT_SKILL_IDS.add(manual.getValue());
            }
        }
    }

    public static boolean isKnownAscentSkill(int skillId) {
        return KNOWN_ASCENT_SKILL_IDS.contains(skillId);
    }

    public static boolean isKnownOriginSkill(int skillId) {
        return KNOWN_ORIGIN_SKILL_IDS.contains(skillId);
    }

    public static boolean isRecognizedOriginSkill(int skillId, SkillInfo si) {
        return isKnownOriginSkill(skillId) || (si != null && si.isOriginSkill());
    }

    public static boolean isRecognizedAscentSkill(int skillId, SkillInfo si) {
        return isKnownAscentSkill(skillId) || (si != null && si.isAscentSkill());
    }

    public static int deriveAscentSkillId(int originSkillId) {
        Integer override = ORIGIN_TO_ASCENT_OVERRIDE.get(originSkillId);
        if (override != null) {
            return override;
        }
        String id = String.valueOf(originSkillId);
        if (id.endsWith("141506")) {
            return originSkillId - 3;
        }
        if (id.endsWith("141500")) {
            return originSkillId + 3;
        }
        if (id.endsWith("41500")) {
            return originSkillId + 3;
        }
        if (id.endsWith("1500")) {
            return originSkillId + 3;
        }
        return 0;
    }

    public static boolean isOriginCore(int coreId) {
        return coreId >= 10000000 && coreId < 20000000;
    }

    public static OriginAscentEntry getByOriginCore(int originCoreId) {
        return BY_ORIGIN_CORE.get(originCoreId);
    }

    public static OriginAscentEntry getByOriginSkill(int originSkillId) {
        OriginAscentEntry entry = BY_ORIGIN_SKILL.get(originSkillId);
        if (entry != null) {
            return entry;
        }
        int ascentSkillId = deriveAscentSkillId(originSkillId);
        if (ascentSkillId <= 0) {
            return null;
        }
        return new OriginAscentEntry(0, 0, originSkillId, ascentSkillId, "");
    }

    public static int getAscentSkillId(int originSkillId) {
        OriginAscentEntry entry = getByOriginSkill(originSkillId);
        return entry == null ? 0 : entry.ascentSkillId();
    }

    public static OriginAscentEntry getOriginEntryForJob(int jobId) {
        int job = jobId;
        while (job > 0) {
            for (OriginAscentEntry entry : BY_ORIGIN_CORE.values()) {
                if (entry.fourthJobId() == job) {
                    return entry;
                }
            }
            if (job < 100) {
                break;
            }
            job /= 10;
        }
        return null;
    }

    private static int resolveMaxLevel(int maxLevel) {
        return maxLevel > 0 ? maxLevel : 30;
    }

    private static boolean shouldNotifySkillGrant(int skillId) {
        return !SkillConstants.isSpecialInvisibleSkill(skillId)
                || isKnownAscentSkill(skillId)
                || isKnownOriginSkill(skillId);
    }

    /**
     * Grants H1/H2 for the character's sixth job line at the given level.
     */
    public static void grantSixthJobSkillsForJob(Char chr, int level, int maxLevel) {
        if (chr == null) {
            return;
        }
        OriginAscentEntry entry = getOriginEntryForJob(chr.getJob());
        if (entry == null || entry.originSkillId() <= 0) {
            return;
        }
        int resolvedMaxLevel = resolveMaxLevel(maxLevel);
        int targetLevel = Math.min(resolvedMaxLevel, Math.max(level, 1));
        List<Integer> originSkills = List.of(entry.originSkillId());
        grantConnectSkills(chr, originSkills, targetLevel, resolvedMaxLevel);
        grantAscentSkillsInternal(chr, originSkills, targetLevel, resolvedMaxLevel);
    }

    /**
     * Grants all known H2 ascent skills for the character's sixth job line.
     */
    public static void grantAscentSkillsForJob(Char chr, int level, int maxLevel) {
        if (chr == null) {
            return;
        }
        OriginAscentEntry entry = getOriginEntryForJob(chr.getJob());
        if (entry == null || entry.ascentSkillId() <= 0) {
            return;
        }
        grantAscentSkillsInternal(chr, List.of(entry.originSkillId()), level, resolveMaxLevel(maxLevel));
    }

    public static List<OriginAscentEntry> allEntries() {
        return Collections.unmodifiableList(new ArrayList<>(BY_ORIGIN_CORE.values()));
    }

    public static List<Integer> allAscentSkillIds() {
        List<Integer> ids = new ArrayList<>();
        for (OriginAscentEntry entry : BY_ORIGIN_CORE.values()) {
            if (entry.ascentSkillId() > 0) {
                ids.add(entry.ascentSkillId());
            }
        }
        for (int ascentId : ORIGIN_TO_ASCENT_OVERRIDE.values()) {
            if (ascentId > 0 && !ids.contains(ascentId)) {
                ids.add(ascentId);
            }
        }
        return ids;
    }

    public static void grantAscentSkills(Char chr, List<Integer> originSkillIds, int level, int maxLevel) {
        if (chr == null || originSkillIds == null || originSkillIds.isEmpty()) {
            return;
        }
        List<Integer> origins = new ArrayList<>();
        for (int skillId : originSkillIds) {
            if (getAscentSkillId(skillId) > 0) {
                origins.add(skillId);
            }
        }
        if (origins.isEmpty()) {
            return;
        }
        grantAscentSkillsInternal(chr, origins, level, maxLevel);
    }

    /**
     * Grants matching H2 skills whenever a hexa core connects to an origin (H1) skill.
     */
    public static void grantAscentSkillsForConnectSkills(Char chr, List<Integer> connectSkills, int level, int maxLevel) {
        grantAscentSkills(chr, connectSkills, level, maxLevel);
    }

    private static void grantAscentSkillsInternal(Char chr, List<Integer> originSkillIds, int level, int maxLevel) {
        if (chr == null || originSkillIds == null || originSkillIds.isEmpty()) {
            return;
        }
        int resolvedMaxLevel = resolveMaxLevel(maxLevel);
        List<Skill> notifySkills = new ArrayList<>();
        for (int originSkillId : originSkillIds) {
            int ascentSkillId = getAscentSkillId(originSkillId);
            if (ascentSkillId <= 0) {
                continue;
            }
            Skill skill = SkillData.getSkillDeepCopyById(ascentSkillId);
            if (skill == null) {
                skill = createFallbackSkill(ascentSkillId, resolvedMaxLevel);
            }
            if (skill == null) {
                continue;
            }
            int targetLevel = Math.min(resolvedMaxLevel, Math.max(level, 1));
            Skill existing = chr.getSkill(ascentSkillId);
            if (existing != null && existing.getCurrentLevel() >= targetLevel) {
                continue;
            }
            skill.setCharId(chr.getId());
            skill.setCurrentLevel(targetLevel);
            skill.setMasterLevel(resolvedMaxLevel);
            skill.setMaxLevel(resolvedMaxLevel);
            skill.saveToSQL();
            chr.addSkill(skill);
            if (shouldNotifySkillGrant(ascentSkillId)) {
                Skill updated = chr.getSkill(ascentSkillId);
                if (updated != null) {
                    notifySkills.add(updated);
                }
            }
        }
        if (!notifySkills.isEmpty()) {
            chr.write(WvsContext.changeSkillRecordResult(notifySkills, true, false, false));
        }
    }

    /**
     * Grants or upgrades origin (H1) connect skills for an activated hexa core.
     */
    public static void grantConnectSkills(Char chr, List<Integer> connectSkills, int level, int maxLevel) {
        if (chr == null || connectSkills == null || connectSkills.isEmpty()) {
            return;
        }
        int resolvedMaxLevel = resolveMaxLevel(maxLevel);
        List<Skill> notifySkills = new ArrayList<>();
        for (Integer skillID : connectSkills) {
            Skill skill = SkillData.getSkillDeepCopyById(skillID);
            if (skill == null) {
                skill = createFallbackSkill(skillID, resolvedMaxLevel);
            }
            if (skill == null) {
                continue;
            }
            int targetLevel = Math.min(resolvedMaxLevel, Math.max(level, 1));
            Skill existing = chr.getSkill(skillID);
            if (existing != null && existing.getCurrentLevel() >= targetLevel) {
                continue;
            }
            skill.setCharId(chr.getId());
            skill.setCurrentLevel(targetLevel);
            skill.setMasterLevel(resolvedMaxLevel);
            skill.setMaxLevel(resolvedMaxLevel);
            skill.saveToSQL();
            chr.addSkill(skill);
            if (shouldNotifySkillGrant(skillID)) {
                Skill updated = chr.getSkill(skillID);
                if (updated != null) {
                    notifySkills.add(updated);
                }
            }
        }
        if (!notifySkills.isEmpty()) {
            chr.write(WvsContext.changeSkillRecordResult(notifySkills, true, false, false));
        }
    }

    /**
     * Ensures players who already unlocked origin cores also receive matching H1/H2 skills.
     */
    public static void syncAscentSkills(Char chr) {
        syncSixthJobSkills(chr);
    }

    public static void syncSixthJobSkills(Char chr) {
        if (chr == null) {
            return;
        }
        for (HexaSkill hexaSkill : chr.getHexaSkills()) {
            int coreID = hexaSkill.getSkillId();
            HexaCore.HexaSkillCoreData coreData = HexaCore.getSkillCoreData(coreID);
            List<Integer> connectSkills = new ArrayList<>();
            if (coreData != null && !coreData.getConnectSkills().isEmpty()) {
                connectSkills.addAll(coreData.getConnectSkills());
            }
            if (isOriginCore(coreID)) {
                OriginAscentEntry entry = getByOriginCore(coreID);
                if (entry != null && !connectSkills.contains(entry.originSkillId())) {
                    connectSkills.add(entry.originSkillId());
                }
            }
            if (connectSkills.isEmpty()) {
                continue;
            }
            int level = hexaSkill.getSkillLevel();
            int maxLevel = coreData != null ? resolveMaxLevel(coreData.getMaxLevel()) : 30;
            grantConnectSkills(chr, connectSkills, level, maxLevel);
            if (isOriginCore(coreID)) {
                grantAscentSkillsForConnectSkills(chr, connectSkills, level, maxLevel);
            }
        }
        OriginAscentEntry jobEntry = getOriginEntryForJob(chr.getJob());
        if (jobEntry != null) {
            int level = chr.getHexaSkillLevel(jobEntry.originCoreId());
            if (level > 0) {
                HexaCore.HexaSkillCoreData coreData = HexaCore.getSkillCoreData(jobEntry.originCoreId());
                int maxLevel = coreData != null ? resolveMaxLevel(coreData.getMaxLevel()) : 30;
                grantSixthJobSkillsForJob(chr, level, maxLevel);
            }
        }
    }

    public static void sendAscentSkillStackUpdate(Char chr) {
        if (chr == null) {
            return;
        }
        List<Integer> ascents = new ArrayList<>();
        for (Skill skill : chr.getSkills()) {
            int skillID = skill.getSkillId();
            if (isKnownAscentSkill(skillID)) {
                ascents.add(skillID);
                continue;
            }
            SkillInfo si = SkillData.getSkillInfoById(skillID);
            if (si != null && si.isAscentSkill()) {
                ascents.add(skillID);
            }
        }
        chr.write(WvsContext.updateAscentSkillStackRequest(ascents));
    }

    /**
     * Registers missing hexa origin core definitions so newer jobs (e.g. MoXuan) can unlock sixth job skills.
     */
    public static void registerHexaCoreFallbacks() {
        for (OriginAscentEntry entry : BY_ORIGIN_CORE.values()) {
            if (HexaCore.getSkillCoreData(entry.originCoreId()) == null) {
                HexaCore.HexaSkillCoreData coreData = new HexaCore.HexaSkillCoreData(entry.originCoreId());
                coreData.setName(entry.name());
                coreData.setDesc(entry.name());
                coreData.setType(1);
                coreData.setMaxLevel(30);
                coreData.getConnectSkills().add(entry.originSkillId());
                HexaCore.hexaSkillCoreDatas.add(coreData);
            } else {
                HexaCore.HexaSkillCoreData coreData = HexaCore.getSkillCoreData(entry.originCoreId());
                if (!coreData.getConnectSkills().contains(entry.originSkillId())) {
                    coreData.getConnectSkills().add(entry.originSkillId());
                }
            }

            HexaCore.HexaSkillJobCore jobCore = findOrCreateJobCore(entry.fourthJobId());
            if (!jobCore.getActivateSkills().contains(entry.originCoreId())) {
                jobCore.getActivateSkills().add(entry.originCoreId());
            }
        }
    }

    /**
     * Registers missing hexa boost core definitions (500004xxx) for newer jobs such as MoXuan.
     */
    public static void registerHexaBoostCoreFallbacks() {
        registerBoostCoreFallback(
                MoXuan.HEXA_BOOST_OPENED_GATE_CORE,
                "真氣：爆氣強化",
                JobConstants.JobEnum.MOXUAN_4.getJobId(),
                MoXuan.HEXA_SOUL_ART_THE_OPENED_GATE
        );
    }

    private static void registerBoostCoreFallback(int coreId, String name, int jobId, int connectSkillId) {
        HexaCore.HexaSkillCoreData coreData = HexaCore.getSkillCoreData(coreId);
        if (coreData == null) {
            coreData = new HexaCore.HexaSkillCoreData(coreId);
            coreData.setName(name);
            coreData.setDesc(name);
            coreData.setType(2);
            coreData.setMaxLevel(30);
            HexaCore.hexaSkillCoreDatas.add(coreData);
        }
        if (!coreData.getConnectSkills().contains(connectSkillId)) {
            coreData.getConnectSkills().add(connectSkillId);
        }

        HexaCore.HexaSkillJobCore jobCore = findOrCreateJobCore(jobId);
        if (!jobCore.getEnforceSkills().contains(coreId)) {
            jobCore.getEnforceSkills().add(coreId);
        }
    }

    private static HexaCore.HexaSkillJobCore findOrCreateJobCore(int jobId) {
        for (HexaCore.HexaSkillJobCore jobCore : HexaCore.hexaSkillJobCores) {
            if (jobCore.getJobID() == jobId) {
                return jobCore;
            }
        }
        HexaCore.HexaSkillJobCore jobCore = new HexaCore.HexaSkillJobCore(jobId);
        HexaCore.hexaSkillJobCores.add(jobCore);
        return jobCore;
    }

    public static Skill createFallbackSkill(int skillId, int maxLevel) {
        if (skillId <= 0) {
            return null;
        }
        int resolvedMaxLevel = resolveMaxLevel(maxLevel);
        Skill skill = new Skill();
        skill.setSkillId(skillId);
        skill.setRootId(skillId / 10000);
        skill.setCurrentLevel(1);
        skill.setMasterLevel(resolvedMaxLevel);
        skill.setMaxLevel(resolvedMaxLevel);
        return skill;
    }
}
