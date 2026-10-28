package net.swordie.ms.console.config;

final class SkillCheckItem {
    final int skillId;
    final String name;
    final String ownerJob;

    SkillCheckItem(int skillId, String name) {
        this(skillId, name, null);
    }

    SkillCheckItem(int skillId, String name, String ownerJob) {
        this.skillId = skillId;
        this.name = name == null ? "" : name;
        this.ownerJob = ownerJob;
    }

    @Override
    public String toString() {
        if (skillId <= 0) {
            return name;
        }
        String prefix = ownerJob == null || ownerJob.isBlank() ? "" : "[" + ownerJob + "] ";
        return prefix + skillId + " - " + name;
    }
}