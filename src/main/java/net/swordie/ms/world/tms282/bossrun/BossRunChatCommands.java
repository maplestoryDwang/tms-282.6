package net.swordie.ms.world.tms282.bossrun;

import net.swordie.ms.Server;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.tms282.Tms282ChatCommand;
import net.swordie.ms.enums.ChatType;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * "@" chat commands of the 282 in-field chat (Command pattern, {@link Tms282ChatCommand}); register with {@link #all()}
 * in {@code Tms282ChatCommands.admin}. GM: @bosslog, @bossreset (account type &gt;= 3). Players: @bossreward (claim
 * stored rewards), @bosspractice [on|off] (arm practice mode for the party you lead; 15 minutes).
 */
public final class BossRunChatCommands {
    private BossRunChatCommands() {
    }

    public static List<Tms282ChatCommand> all() {
        return all(BossRunService::get, n -> Server.get().getWorld().getCharByName(n));
    }

    public static List<Tms282ChatCommand> all(Supplier<BossRunService> service, Function<String, Char> online) {
        return List.of(new BossLog(service, online), new BossReset(service, online), new BossReward(service),
                new BossPractice(service));
    }

    static Tms282ChatCommand.Reply lines(Char chr, List<String> lines, String log) {
        for (int i = 1; i < lines.size(); i++) chr.chatMessage(ChatType.SystemNotice, lines.get(i));
        return Tms282ChatCommand.Reply.text(lines.isEmpty() ? "" : lines.getFirst(), log);
    }

    public record BossLog(Supplier<BossRunService> service, Function<String, Char> online) implements Tms282ChatCommand {
        public String name() {
            return "bosslog";
        }

        public int minAccountType() {
            return 3;
        }

        public String usage() {
            return "@bosslog 角色名稱 [筆數]";
        }

        public Reply execute(Char chr, List<String> args) {
            if (args.isEmpty()) throw new IllegalArgumentException("missing name");
            int n = 10;
            if (args.size() > 1) {
                try {
                    n = Math.max(1, Math.min(50, Integer.parseInt(args.get(1))));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("not a number: " + args.get(1));
                }
            }
            return lines(chr, new BossRunAdmin(service.get(), online).log(args.getFirst(), n), "@bosslog " + args);
        }
    }

    public record BossReset(Supplier<BossRunService> service, Function<String, Char> online) implements Tms282ChatCommand {
        public String name() {
            return "bossreset";
        }

        public int minAccountType() {
            return 3;
        }

        public String usage() {
            return "@bossreset 角色名稱 [首領：ZAKUM_EASY／Zakum／殘暴炎魔／aufheben；省略＝全部]";
        }

        public Reply execute(Char chr, List<String> args) {
            if (args.isEmpty()) throw new IllegalArgumentException("missing name");
            String boss = args.size() > 1 ? String.join(" ", args.subList(1, args.size())) : null;
            String text = new BossRunAdmin(service.get(), online).reset(args.getFirst(), boss);
            return Reply.text(text, "@bossreset " + args + ": " + text);
        }
    }

    public record BossReward(Supplier<BossRunService> service) implements Tms282ChatCommand {
        public String name() {
            return "bossreward";
        }

        public int minAccountType() {
            return 0;
        }

        public String usage() {
            return "@bossreward（領取暫存的首領獎勵）";
        }

        public Reply execute(Char chr, List<String> args) {
            String text = service.get().claimPending(chr.getId(), new BossRunService.CharDelivery(chr)).replace("\r\n", " ");
            return Reply.text(text, "@bossreward: " + text);
        }
    }

    public record BossPractice(Supplier<BossRunService> service) implements Tms282ChatCommand {
        public String name() {
            return "bosspractice";
        }

        public int minAccountType() {
            return 0;
        }

        public String usage() {
            return "@bosspractice [on|off]（隊長使用：下一次首領挑戰改為練習模式，15 分鐘內有效）";
        }

        public Reply execute(Char chr, List<String> args) {
            BossRunService s = service.get();
            if (chr.getParty() != null && !chr.getParty().isLeader(chr)) {
                return Reply.text("只有隊長可以切換練習模式。", "@bosspractice refused: not leader");
            }
            boolean on = args.isEmpty() || !"off".equalsIgnoreCase(args.getFirst());
            int leader = BossRunService.leaderIdOf(chr);
            if (on) {
                s.arm(leader, true);
                return Reply.text("已啟用練習模式（15 分鐘內進場有效）：不計次數、不記錄、不發放獎勵。", "@bosspractice on");
            }
            s.disarm(leader);
            return Reply.text("已取消練習模式，下一次挑戰為正式挑戰。", "@bosspractice off");
        }
    }
}
