package net.swordie.ms.world.tms282.bossrun;

import net.swordie.ms.Server;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.commands.AdminCommand;
import net.swordie.ms.client.character.commands.Command;

import static net.swordie.ms.enums.AccountType.GameMaster;
import static net.swordie.ms.enums.ChatType.SpeakerChannel;

/**
 * Boss-run GM commands for the "!" registry (@Command classes; ChatHandler scans
 * {@code BossRunCommands.class.getClasses()} once registered like AuditCommands). The 282 in-field chat routes
 * only "@" lines, so the same commands also exist as {@link BossRunChatCommands}.
 */
public class BossRunCommands {

    static BossRunAdmin admin() {
        return new BossRunAdmin(BossRunService.get(), n -> Server.get().getWorld().getCharByName(n));
    }

    static String join(String[] args, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }

    /** !bosslog &lt;角色名&gt; [筆數] */
    @Command(names = {"bosslog"}, requiredType = GameMaster)
    public static class BossLog extends AdminCommand {
        public static void execute(Char chr, String[] args) {
            if (args.length < 2) {
                chr.chatMessage(SpeakerChannel, "用法: !bosslog <角色名> [筆數]");
                return;
            }
            int n = 10;
            if (args.length > 2) {
                try {
                    n = Math.max(1, Math.min(50, Integer.parseInt(args[2])));
                } catch (NumberFormatException ignored) {
                }
            }
            for (String line : admin().log(args[1], n)) chr.chatMessage(SpeakerChannel, line);
        }
    }

    /** !bossreset &lt;角色名&gt; [首領] — 首領: ZAKUM_EASY / Zakum / 殘暴炎魔 / aufheben; 省略 = 全部。 */
    @Command(names = {"bossreset"}, requiredType = GameMaster)
    public static class BossReset extends AdminCommand {
        public static void execute(Char chr, String[] args) {
            if (args.length < 2) {
                chr.chatMessage(SpeakerChannel, "用法: !bossreset <角色名> [首領，例如 ZAKUM_EASY、Zakum、aufheben；省略=全部]");
                return;
            }
            chr.chatMessage(SpeakerChannel, admin().reset(args[1], args.length > 2 ? join(args, 2) : null));
        }
    }
}
