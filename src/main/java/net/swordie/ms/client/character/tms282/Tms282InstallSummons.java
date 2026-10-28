package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.enums.AssistType;
import net.swordie.ms.enums.MoveAbility;
import net.swordie.ms.life.Summon;
import net.swordie.ms.loaders.SkillData;

import java.util.Map;

/**
 * TMS 282.3 skills whose 282 Skill.wz has a {@code summon} node (installed on the foothold, {@code time} seconds) but
 * no 265 job logic: the use was accepted and nothing appeared (live 10-07: 凍結地帶 pressed 19 times). The server spawns
 * a stationary summon at the character; the client drives its attack (CP02E8).
 * <ul>
 * <li>21111034 / 21141013 狂狼勇士 凍結地帶 / VI (type 31, attack1)</li>
 * <li>154121041 / 154141012 卡莉 死亡綻放 / VI (attack1, activeDelay)</li>
 * <li>33121005 狂豹獵人 誘餌信標 (stand / hit / die only: no attack)</li>
 * </ul>
 * Not shown to be the official layout; live untested.
 */
public final class Tms282InstallSummons {
    /** skill -> attacks. */
    private static final Map<Integer, Boolean> SKILLS = Map.of(
            21111034, true, 21141013, true, 154121041, true, 154141012, true, 33121005, false);

    private Tms282InstallSummons() {
    }

    /**
     * Return-to-town skills (invisible, beginner book, {@code x} = map, 600 s cooldown) that no job class handles:
     * Illium 回歸魔法屏障 150001016 / 150001021, Khali 回歸領地 150031074, Ho Young 青雲回歸 160001074, Mihile 聖地回歸
     * 50001245, Kanna 返回楓葉丘陵 40021227.
     */
    private static final java.util.Set<Integer> RETURN_SKILLS = java.util.Set.of(
            150001016, 150001021, 150031074, 160001074, 50001245, 40021227);

    public static boolean returnSkill(int skillId) {
        return Protocol.active().version() == ProtocolVersion.TMS_282_3 && RETURN_SKILLS.contains(skillId);
    }

    public static boolean handles(int skillId) {
        return Protocol.active().version() == ProtocolVersion.TMS_282_3 && SKILLS.containsKey(skillId);
    }

    /** Replaces the character's previous summon of the skill. */
    public static Summon spawn(Char chr, int skillId, int slv) {
        SkillInfo si = SkillData.getSkillInfoById(skillId);
        if (chr == null || chr.getField() == null || si == null || !handles(skillId)) {
            return null;
        }
        chr.getField().removeSummon(skillId, chr.getId());
        boolean attacks = SKILLS.get(skillId);
        Summon summon = Summon.getSummonByAndSetStat(chr, skillId, slv);
        summon.setMoveAction((byte) 4);
        summon.setMoveAbility(MoveAbility.Stop);
        summon.setFlyMob(false);
        summon.setAssistType(attacks ? AssistType.Attack : AssistType.None);
        summon.setAttackActive(attacks);
        chr.getField().spawnSummon(summon);
        return summon;
    }
}
