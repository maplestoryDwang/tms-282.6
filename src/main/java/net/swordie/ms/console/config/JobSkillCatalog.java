package net.swordie.ms.console.config;

import net.swordie.ms.constants.SixthJobSkillConstants;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.loaders.containerclasses.SkillStringInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class JobSkillCatalog {
    public static final String FIFTH_JOB = "五轉技能";
    public static final String SIXTH_JOB = "六轉技能";

    public record JobCategory(String name, List<String> jobs) {
    }

    private final Map<String, Map<Integer, String>> skillsByJob = new LinkedHashMap<>();
    private final List<JobCategory> categories = new ArrayList<>();

    public JobSkillCatalog() {
        Map<Integer, Map<Integer, String>> byRoot = new TreeMap<>();
        Map<Integer, String> fifth = new TreeMap<>();
        Map<Integer, String> sixth = new TreeMap<>();
        for (SkillInfo info : SkillData.getLoadedSkillInfos()) {
            int skillId = info.getSkillId();
            if (skillId <= 0) {
                continue;
            }
            if (isSixthSkill(info)) {
                sixth.put(skillId, skillName(skillId));
                continue;
            }
            if (isFifthSkill(info)) {
                String fifthJobName = JobCatalog.fifthSkillRootName(info.getRootId());
                if (fifthJobName != null) {
                    byRoot.computeIfAbsent(info.getRootId(), ignored -> new TreeMap<>()).put(skillId, skillName(skillId));
                } else {
                    fifth.put(skillId, skillName(skillId));
                }
                continue;
            }
            byRoot.computeIfAbsent(info.getRootId(), ignored -> new TreeMap<>()).put(skillId, skillName(skillId));
        }
        List<String> jobNames = new ArrayList<>();
        for (Map.Entry<Integer, Map<Integer, String>> entry : byRoot.entrySet()) {
            String jobName = JobCatalog.fifthSkillRootName(entry.getKey());
            if (jobName == null) {
                jobName = JobCatalog.skillRootName(entry.getKey());
            }
            skillsByJob.put(jobName, entry.getValue());
            jobNames.add(jobName);
        }
        if (!fifth.isEmpty()) {
            skillsByJob.put(FIFTH_JOB, fifth);
            jobNames.add(FIFTH_JOB);
        }
        if (!sixth.isEmpty()) {
            skillsByJob.put(SIXTH_JOB, sixth);
            jobNames.add(SIXTH_JOB);
        }
        categories.add(new JobCategory("全部職業", jobNames));
    }

    private static boolean isFifthSkill(SkillInfo info) {
        int id = info.getSkillId();
        return info.getVSkill() > 0 || id / 10_000_000 == 40 || id / 10_000_000 == 80;
    }

    private static boolean isSixthSkill(SkillInfo info) {
        int id = info.getSkillId();
        return SixthJobSkillConstants.isRecognizedOriginSkill(id, info)
                || SixthJobSkillConstants.isRecognizedAscentSkill(id, info)
                || id / 10_000_000 == 45 || id / 10_000_000 == 46;
    }

    private static String skillName(int skillId) {
        try {
            SkillStringInfo stringInfo = StringData.getSkillString().get(skillId);
            if (stringInfo != null && stringInfo.getName() != null && !stringInfo.getName().isBlank()) {
                return stringInfo.getName();
            }
        } catch (Exception ignored) {
        }
        return "技能" + skillId;
    }

    public List<JobCategory> categories() {
        return categories;
    }

    public List<String> jobNames() {
        List<String> all = new ArrayList<>();
        for (JobCategory category : categories) {
            all.addAll(category.jobs());
        }
        return all;
    }

    public boolean isKnownJob(String jobName) {
        return skillsByJob.containsKey(jobName);
    }

    public Map<Integer, String> skillsFor(String jobName) {
        return skillsByJob.getOrDefault(jobName, Map.of());
    }

    public String findJobForSkill(int skillId) {
        for (String job : jobNames()) {
            if (skillsFor(job).containsKey(skillId)) {
                return job;
            }
        }
        return null;
    }
}