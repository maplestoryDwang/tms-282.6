package net.swordie.ms.console.datamanage;

import net.swordie.ms.constants.GameConstants;

public final class CharacterStatsSnapshot {
    public final int level;
    public final int str;
    public final int dex;
    public final int inte;
    public final int luk;
    public final int maxHp;
    public final int maxMp;

    public CharacterStatsSnapshot(int level, int str, int dex, int inte, int luk, int maxHp, int maxMp) {
        this.level = level;
        this.str = str;
        this.dex = dex;
        this.inte = inte;
        this.luk = luk;
        this.maxHp = maxHp;
        this.maxMp = maxMp;
    }

    public static CharacterStatsSnapshot clamp(CharacterStatsSnapshot raw) {
        int cap = GameConstants.MAX_HP_MP;
        int lvl = Math.max(1, Math.min(300, raw.level));
        int s = clampStat(raw.str);
        int d = clampStat(raw.dex);
        int i = clampStat(raw.inte);
        int l = clampStat(raw.luk);
        int hp = Math.max(1, Math.min(cap, raw.maxHp));
        int mp = Math.max(1, Math.min(cap, raw.maxMp));
        return new CharacterStatsSnapshot(lvl, s, d, i, l, hp, mp);
    }

    private static int clampStat(int v) {
        return Math.max(0, Math.min(32767, v));
    }

    public String describe() {
        return String.format("等級=%d 力量=%d 敏捷=%d 智力=%d 運氣=%d MaxHP=%d MaxMP=%d",
                level, str, dex, inte, luk, maxHp, maxMp);
    }

    public String diffFrom(CharacterStatsSnapshot before) {
        if (before == null) {
            return describe();
        }
        StringBuilder sb = new StringBuilder();
        appendDiff(sb, "等級", before.level, level);
        appendDiff(sb, "力量", before.str, str);
        appendDiff(sb, "敏捷", before.dex, dex);
        appendDiff(sb, "智力", before.inte, inte);
        appendDiff(sb, "運氣", before.luk, luk);
        appendDiff(sb, "MaxHP", before.maxHp, maxHp);
        appendDiff(sb, "MaxMP", before.maxMp, maxMp);
        return sb.length() == 0 ? "（無變化）" : sb.toString();
    }

    private static void appendDiff(StringBuilder sb, String label, long oldV, long newV) {
        if (oldV == newV) {
            return;
        }
        if (sb.length() > 0) {
            sb.append("; ");
        }
        sb.append(label).append(' ').append(oldV).append(" → ").append(newV);
    }
}
