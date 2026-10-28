package net.swordie.ms.client.character.tms282;

import java.util.List;
import net.swordie.ms.Server;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.quest.Tms282QuestService;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.constants.SixthJobSkillConstants;
import net.swordie.ms.lifecycle.OnlineCharacterSaver;
import net.swordie.ms.world.tms282.newbie.NewbieFlowService;
import net.swordie.ms.world.tms282.newbie.steps.FirstJobStep;

/** GM policy: level 300, ordinary advancements, empty V matrix and the supported job's Origin Lv1.
 * Dialogs consume the immutable result after packet locks have been released. */
public final class Tms282GmLevelAdvance {
    private Tms282GmLevelAdvance() { }
    public record Choice(int job, String label, List<Integer> quests, boolean first) { }
    public record Result(int job, int level, List<Choice> choices, String message) {
        public boolean complete() { return choices.isEmpty(); }
    }

    public static Result advance(Char chr, int expectedJob, int expectedLevel, int selectedJob) {
        if (chr == null || Protocol.active().version() != ProtocolVersion.TMS_282_3)
            throw new IllegalStateException("此轉職流程僅適用 TMS 282。");
        Result[] result = {null};
        int[] selection = {selectedJob};
        EconomyLocks.packet(chr, () -> {
            requireActor(chr);
            if (chr.getJob() != expectedJob || chr.getLevel() != expectedLevel)
                throw new IllegalStateException("對話期間職業或等級已變更，請重新開啟功能。");
            Tms282VMatrixRuntime.requireEconomicAccess(chr);
            var sm = chr.getScriptManager();
            if (chr.getJob() == 10000 && !NewbieFlowService.get().completeZeroPrologue(chr))
                throw new IllegalStateException("神之子序章尚未完成，請稍後重試。");
            for (int steps = 0; steps < 310; steps++) {
                Tms282JobAdvance.catchUp(chr);
                List<Choice> choices = choices(chr);
                if (!choices.isEmpty()) {
                    Choice chosen = null;
                    if (selection[0] != 0) {
                        for (Choice choice : choices) if (choice.job() == selection[0]) chosen = choice;
                        if (chosen == null) throw new IllegalArgumentException("無效的轉職分支。");
                    } else if (choices.size() == 1) chosen = choices.get(0);
                    if (chosen == null) {
                        save(chr);
                        result[0] = new Result(chr.getJob(), chr.getLevel(), choices, "請選擇轉職分支。");
                        return null;
                    }
                    levelTo(chr, chosen.first() ? 10 : 30);
                    if (chosen.first()) sm.jobAdvance((short) chosen.job());
                    else if (!Tms282JobAdvance.chooseSecondJob(chr, chr.getJob(), chosen.job()))
                        throw new IllegalStateException("轉職分支未套用，請重新開啟功能。");
                    for (int quest : chosen.quests()) if (!chr.hasQuestCompleted(quest)) {
                        Tms282QuestService.markStarted(chr, quest);
                        chr.completeQuest(quest, true);
                    }
                    if (chr.getJob() != chosen.job()) throw new IllegalStateException("轉職結果不符。");
                    selection[0] = 0;
                    continue;
                }
                if (selection[0] != 0) throw new IllegalArgumentException("目前職業不接受此分支選擇。");
                if (JobConstants.isBeginnerJob(chr.getJob()) && !JobConstants.isSpecialEventJob(chr.getJob()))
                    throw new IllegalStateException("目前初始職業缺少轉職設定：" + chr.getJob());
                if (chr.getLevel() >= 300) break;
                levelTo(chr, chr.getLevel() + 1);
            }
            if (chr.getLevel() < 300) throw new IllegalStateException("升級未完成，請重試。");
            sm.setLevelDirect(300); // already at 300: existing HP/MP compensation only, never replay level growth
            String message = "已升至 300 級並保存。";
            if (JobConstants.isSpecialEventJob(chr.getJob())) {
                message += "活動職業沿用專屬成長規則，沒有一般五、六轉。";
            } else {
                if (!JobConstants.isReadyForFifthJob(chr.getJob(), chr.getSubJob()))
                    throw new IllegalStateException("一般轉職尚未完成：" + chr.getJob());
                sm.completeJobVIfNeeded();
                if (!Tms282VMatrixRuntime.eligible(chr))
                    throw new IllegalStateException("目前職業缺少新版五轉矩陣資料。");
                Tms282VMatrixRuntime.onFieldEntry(chr);
                if (!Tms282VMatrixRuntime.managed(chr))
                    throw new IllegalStateException("五轉矩陣初始化失敗，請重試。");
                message += "已完成一般轉職並開通五轉。";
                var origin = SixthJobSkillConstants.getOriginEntryForJob(chr.getJob());
                if (origin != null) {
                    sm.completeJobVIIfNeeded();
                    Tms282GmOrigin.ensure(chr);
                    if (!chr.hasQuestCompleted(1488) || chr.getHexaSkillLevel(origin.originCoreId()) < 1
                            || chr.getSkillLevel(origin.originSkillId()) < 1)
                        throw new IllegalStateException("六轉起源核心或技能未完成初始化。");
                    message += "六轉已開通，起源核心至少 Lv1；既有等級與其他核心保留。";
                } else message += "此職業目前沒有可用的六轉起源資料，未標記六轉完成。";
            }
            save(chr);
            result[0] = new Result(chr.getJob(), chr.getLevel(), List.of(), message);
            return null;
        });
        return result[0];
    }

    private static void requireActor(Char chr) {
        Client client = chr.getClient();
        if (!chr.isGM() || !chr.isOnline() || client == null || client.getChr() != chr
                || client.getCurrentState() != Client.IN_FIELD || client.getNextState() != Client.NONE
                || client.getCh() == null || !client.getCh().isActive()
                || Server.get().getChars().get(chr.getId()) != chr)
            throw new IllegalStateException("角色必須是目前在線上且未切換頻道的管理員。");
    }

    private static void save(Char chr) {
        requireActor(chr);
        if (OnlineCharacterSaver.save(chr) != OnlineCharacterSaver.Result.SAVED)
            throw new IllegalStateException("角色保存未完成，請重新開啟功能重試；不會重複已完成的轉職。");
    }

    private static void levelTo(Char chr, int level) {
        while (chr.getLevel() < level && chr.getLevel() < 300) {
            var result = new Tms282LevelService(Tms282CharacterStore.SQL).gainLevels(chr, 1);
            result.packets().forEach(chr::write);
            if (result.toLevel() <= result.fromLevel()) throw new IllegalStateException("等級未增加。");
            Tms282JobAdvance.afterLevelUp(chr, result.fromLevel(), result.toLevel());
        }
    }

    private static Choice second(int job, String name, int... quests) {
        return new Choice(job, name, java.util.Arrays.stream(quests).boxed().toList(), false);
    }

    private static List<Choice> choices(Char chr) {
        var config = NewbieFlowService.get().status().config();
        if (config == null) throw new IllegalStateException("新手轉職設定尚未載入。");
        for (var step : config.steps()) if (step instanceof FirstJobStep first) {
            String login = chr.getSubJob() == 1 ? "DUAL_BLADE" : chr.getSubJob() == 2 ? "CANNONER" : "";
            var rule = first.ruleFor(chr.getJob(), login);
            if (rule != null) return rule.branches().stream()
                    .map(b -> new Choice(b.job(), b.label(), b.quests(), true)).toList();
        }
        return switch (chr.getJob()) {
            case 100 -> List.of(second(110,"狂戰士",1410,1411),second(120,"見習騎士",1410,1412),second(130,"槍騎兵",1410,1413));
            case 200 -> List.of(second(210,"火毒巫師",1414,1415),second(220,"冰雷巫師",1414,1416),second(230,"僧侶",1414,1417));
            case 300 -> List.of(second(310,"獵人",1418,1419),second(320,"弩弓手",1418,1420));
            case 400 -> chr.getSubJob() == 1 ? List.of() : List.of(second(410,"刺客",1421,1422),second(420,"俠盜",1421,1423));
            case 500 -> List.of(second(510,"打手",1424,1425),second(520,"槍手",1424,1426));
            case 501 -> List.of(second(530,"重砲兵",1427,1428));
            default -> List.of();
        };
    }
}
