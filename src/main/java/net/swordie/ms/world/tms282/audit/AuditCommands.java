package net.swordie.ms.world.tms282.audit;

import net.swordie.ms.Server;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.commands.AdminCommand;
import net.swordie.ms.client.character.commands.Command;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.swordie.ms.enums.AccountType.Admin;
import static net.swordie.ms.enums.AccountType.GameMaster;
import static net.swordie.ms.enums.ChatType.SpeakerChannel;

/**
 * Ban / audit GM commands. Registered through the same {@code @Command} registry as AdminCommands: the command
 * dispatcher (ChatHandler.handleUserChat) scans {@code AuditCommands.class.getClasses()} as well.
 * All DB work happens on the audit thread; replies arrive asynchronously.
 */
public class AuditCommands {

    private static String join(String[] args, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(args[i]);
        }
        return sb.toString();
    }

    private static boolean looksLikeIp(String s) {
        return s.matches("[0-9.*:]+") && (s.contains(".") || s.contains(":"));
    }

    private static Char online(String name) {
        return Server.get().getWorld().getCharByName(name);
    }

    /** users.id of an online or offline character; 0 if unknown. */
    private static CompletableFuture<Integer> accountIdOf(String name) {
        Char c = online(name);
        if (c != null && c.getUser() != null) {
            return CompletableFuture.completedFuture(c.getUser().getId());
        }
        return BanService.findAccountIdByCharName(name);
    }

    private static void kick(Char target) {
        if (target != null) {
            Client client = target.getClient();
            if (client != null) {
                client.close();
            }
        }
    }

    @Command(names = {"banip", "ipban"}, requiredType = GameMaster)
    public static class BanIp extends AdminCommand {
        public static void execute(Char chr, String[] args) {
            if (args.length < 2) {
                chr.chatMessage(SpeakerChannel, "用法: !banip <IP|a.b.c.*|角色名> [原因]");
                return;
            }
            String reason = args.length > 2 ? join(args, 2) : "GM " + chr.getName();
            String ip;
            Char target = null;
            if (looksLikeIp(args[1])) {
                ip = args[1];
            } else {
                target = online(args[1]);
                if (target == null || target.getClient() == null) {
                    chr.chatMessage(SpeakerChannel, "角色 " + args[1] + " 不在線，請直接指定 IP。");
                    return;
                }
                ip = target.getClient().getIP();
            }
            String finalIp = BanService.normalizeIp(ip);
            BanService.banIp(finalIp, reason + " (by " + chr.getName() + ")")
                    .thenAccept(ok -> chr.chatMessage(SpeakerChannel, ok
                            ? "已封鎖 IP " + finalIp + "。"
                            : "IP " + finalIp + " 已加入快取，但寫入資料庫失敗。"));
            // kick every online character from that IP
            for (Char c : List.copyOf(Server.get().getWorld().getChars())) {
                if (c != chr && c.getClient() != null && BanService.isIpBanned(c.getClient().getIP())) {
                    kick(c);
                }
            }
        }
    }

    @Command(names = {"unbanip", "ipunban"}, requiredType = GameMaster)
    public static class UnbanIp extends AdminCommand {
        public static void execute(Char chr, String[] args) {
            if (args.length < 2) {
                chr.chatMessage(SpeakerChannel, "用法: !unbanip <IP|a.b.c.*>");
                return;
            }
            String ip = BanService.normalizeIp(args[1]);
            BanService.unbanIp(ip).thenAccept(ok -> chr.chatMessage(SpeakerChannel, ok
                    ? "已解除 IP 封鎖 " + ip + "。"
                    : "資料庫中找不到 IP " + ip + "（已從快取移除）。"));
        }
    }

    @Command(names = {"tempban"}, requiredType = GameMaster)
    public static class TempBan extends AdminCommand {
        public static void execute(Char chr, String[] args) {
            if (args.length < 5) {
                StringBuilder types = new StringBuilder();
                for (BanType t : BanType.values()) {
                    types.append(t.getVal()).append('=').append(t.name().toLowerCase()).append(' ');
                }
                chr.chatMessage(SpeakerChannel, "用法: !tempban <角色名> <類型> <小時(0=永久)> <原因>");
                chr.chatMessage(SpeakerChannel, "類型: " + types.toString().trim());
                return;
            }
            String name = args[1];
            BanType type = BanType.parse(args[2]);
            if (type == null) {
                chr.chatMessage(SpeakerChannel, "未知的封鎖類型 " + args[2] + "。");
                return;
            }
            long hours;
            try {
                hours = Long.parseLong(args[3]);
            } catch (NumberFormatException e) {
                chr.chatMessage(SpeakerChannel, "小時必須是數字。");
                return;
            }
            String reason = join(args, 4) + " (by " + chr.getName() + ")";
            accountIdOf(name).thenAccept(accId -> {
                if (accId == null || accId <= 0) {
                    chr.chatMessage(SpeakerChannel, "找不到角色 " + name + "。");
                    return;
                }
                BanService.tempBan(accId, type, hours, reason).thenAccept(ok -> chr.chatMessage(SpeakerChannel,
                        String.format("%s (帳號 %d) 已封鎖: %s，%s。%s", name, accId, type.getLabel(),
                                hours <= 0 ? "永久" : hours + " 小時", ok ? "" : "（資料庫寫入失敗，只在快取中生效）")));
                if (type.blocksLogin()) {
                    kick(online(name));
                }
            });
        }
    }

    @Command(names = {"untempban", "unblock"}, requiredType = GameMaster)
    public static class UnTempBan extends AdminCommand {
        public static void execute(Char chr, String[] args) {
            if (args.length < 2) {
                chr.chatMessage(SpeakerChannel, "用法: !untempban <角色名> [類型]");
                return;
            }
            String name = args[1];
            BanType type = args.length > 2 ? BanType.parse(args[2]) : null;
            if (args.length > 2 && type == null) {
                chr.chatMessage(SpeakerChannel, "未知的封鎖類型 " + args[2] + "。");
                return;
            }
            accountIdOf(name).thenAccept(accId -> {
                if (accId == null || accId <= 0) {
                    chr.chatMessage(SpeakerChannel, "找不到角色 " + name + "。");
                    return;
                }
                BanService.liftBans(accId, type).thenAccept(n -> chr.chatMessage(SpeakerChannel,
                        n < 0 ? "解除封鎖時資料庫錯誤。" : String.format("已解除 %s (帳號 %d) 的 %d 筆封鎖。", name, accId, n)));
            });
        }
    }

    @Command(names = {"baninfo"}, requiredType = GameMaster)
    public static class BanInfo extends AdminCommand {
        public static void execute(Char chr, String[] args) {
            if (args.length < 2) {
                chr.chatMessage(SpeakerChannel, "用法: !baninfo <角色名>");
                return;
            }
            String name = args[1];
            accountIdOf(name).thenAccept(accId -> {
                if (accId == null || accId <= 0) {
                    chr.chatMessage(SpeakerChannel, "找不到角色 " + name + "。");
                    return;
                }
                long now = System.currentTimeMillis();
                List<AccountBlock> list = BanService.blocksOf(accId);
                if (list.isEmpty()) {
                    chr.chatMessage(SpeakerChannel, name + " (帳號 " + accId + ") 沒有生效中的封鎖。");
                    return;
                }
                for (AccountBlock b : list) {
                    if (!b.isActive(now)) {
                        continue;
                    }
                    long mins = b.remainingMinutes(now);
                    chr.chatMessage(SpeakerChannel, String.format("[%s] %s, 原因: %s", b.banType().name(),
                            mins < 0 ? "永久" : "剩餘 " + mins + " 分鐘", b.reason()));
                }
            });
        }
    }

    @Command(names = {"auditstat"}, requiredType = Admin)
    public static class AuditStat extends AdminCommand {
        public static void execute(Char chr, String[] args) {
            if (args.length > 1 && "flush".equalsIgnoreCase(args[1])) {
                AuditWriter.flushAsync();
            }
            if (args.length > 1 && "reloadbans".equalsIgnoreCase(args[1])) {
                BanService.refreshAsync();
            }
            chr.chatMessage(SpeakerChannel, AuditLog.stats());
        }
    }
}
