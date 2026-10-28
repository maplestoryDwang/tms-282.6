package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.WvsContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Admin shortcuts for the 5th and 6th job advancement ({@link Tms282ChatCommand}s, account type >= 3), the same steps
 * as the 265 admin NPC (data/scripts/npc/quick_adminNPC.py selections 21 and 22) and ScriptManager:
 * <ul>
 *     <li>{@code @jobv}: completes 1460..1465 - 1465 is "[5轉技能與神祕力量] 5轉：紀錄力量，完成覺醒" in the 282
 *     Quest.wz, the quest the client checks for the V matrix;</li>
 *     <li>{@code @jobvi}: 265 {@code ScriptManager.completeJobVIIfNeeded} (1488 and the 282 6th job chain 38601..38625,
 *     "[6轉] 轉職完成" = 38612), then the HEXA state (LP0272 skills, LP0273 stats, LP0274 costs through
 *     {@link net.swordie.ms.connection.packet.Tms282HexaMatrixTranslators}).</li>
 * </ul>
 * The quest records reach the client as MESSAGE records through the profile's legacy translators.
 * TMS shortcut completion grants neither start nor end quest rewards.
 */
public final class Tms282JobAdvanceCommands {
    /** 5th job quests completed by {@code @jobv} (1460..1465 inclusive, as the 265 admin NPC). */
    public static final int FIFTH_JOB_FIRST_QUEST = 1460;
    public static final int FIFTH_JOB_QUEST = 1465;
    public static final int MIN_LEVEL_FIFTH = 200;
    public static final int MIN_LEVEL_SIXTH = 260;

    private Tms282JobAdvanceCommands() {
    }

    public static List<Tms282ChatCommand> all() {
        return List.of(new FifthJob(Tms282JobAdvanceCommands::completeFifthJobQuests),
                new SixthJob(chr -> chr.getScriptManager().completeJobVIIfNeeded()));
    }

    static void completeFifthJobQuests(Char chr) {
        chr.getScriptManager().completeJobVIfNeeded();
    }

    /** The HEXA state packets (265 builders; translated to LP0272 / LP0273 / LP0274 for 282). */
    public static List<OutPacket> hexaState(Char chr) {
        List<OutPacket> packets = new ArrayList<>();
        packets.add(WvsContext.hexaMatrixInformation());
        packets.add(WvsContext.hexaSkillsUpdate(chr));
        packets.add(WvsContext.hexaStatsUpdate(chr));
        return packets;
    }

    static String levelNote(Char chr, int min) {
        return chr.getLevel() < min ? "（目前等級 " + chr.getLevel() + " < " + min + "，用戶端可能仍不顯示）" : "";
    }

    /** {@code @jobv}. */
    public static final class FifthJob implements Tms282ChatCommand {
        private final Consumer<Char> advance;

        FifthJob(Consumer<Char> advance) {
            this.advance = Objects.requireNonNull(advance, "advance");
        }

        @Override
        public String name() {
            return "jobv";
        }

        @Override
        public int minAccountType() {
            return 3;
        }

        @Override
        public String usage() {
            return "@jobv（五轉：任務 1460..1465）";
        }

        @Override
        public Reply execute(Char chr, List<String> args) {
            if (!args.isEmpty()) {
                throw new IllegalArgumentException("no arguments");
            }
            advance.accept(chr);
            return Reply.text("已完成五轉任務" + levelNote(chr, MIN_LEVEL_FIFTH),
                    "@jobv character=" + chr.getId() + " level=" + chr.getLevel() + " quests 1460..1465");
        }
    }

    /** {@code @jobvi}. */
    public static final class SixthJob implements Tms282ChatCommand {
        private final Consumer<Char> advance;

        SixthJob(Consumer<Char> advance) {
            this.advance = Objects.requireNonNull(advance, "advance");
        }

        @Override
        public String name() {
            return "jobvi";
        }

        @Override
        public int minAccountType() {
            return 3;
        }

        @Override
        public String usage() {
            return "@jobvi（六轉：任務 1488 與 38601..38625 系列，並送出 HEXA 狀態）";
        }

        @Override
        public Reply execute(Char chr, List<String> args) {
            if (!args.isEmpty()) {
                throw new IllegalArgumentException("no arguments");
            }
            advance.accept(chr);
            return new Reply(hexaState(chr), "已完成六轉任務，並送出 HEXA 矩陣" + levelNote(chr, MIN_LEVEL_SIXTH),
                    "@jobvi character=" + chr.getId() + " level=" + chr.getLevel() + " quests 1488, 38601..38625");
        }
    }
}
