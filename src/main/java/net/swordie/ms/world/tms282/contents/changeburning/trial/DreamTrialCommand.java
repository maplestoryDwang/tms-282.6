package net.swordie.ms.world.tms282.contents.changeburning.trial;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.tms282.Tms282ChatCommand;
import net.swordie.ms.world.tms282.contents.changeburning.ChangeBurning;

import java.util.List;
import java.util.Locale;

/**
 * GM probe of 夢幻的試煉 (registered in {@code Tms282ChatCommands}):
 * {@code @dreamtrial on|off|status|list|reload}, {@code @dreamtrial enter <key>}, {@code @dreamtrial clear} (ends the
 * current stage), {@code @dreamtrial leave}, {@code @dreamtrial reset <key>} (today's count), {@code @dreamtrial hp <key> [lucidLv]}.
 */
public record DreamTrialCommand() implements Tms282ChatCommand {
    public String name() {
        return "dreamtrial";
    }

    public int minAccountType() {
        return 3;
    }

    public String usage() {
        return "@dreamtrial on|off|status|list|reload｜enter <key>｜clear｜leave｜reset <key>｜hp <key> [露希妲等級]";
    }

    public Reply execute(Char chr, List<String> args) {
        if (args.isEmpty()) {
            throw new IllegalArgumentException("missing action");
        }
        String a = args.getFirst().toLowerCase(Locale.ROOT);
        DreamTrialService s = DreamTrialService.get();
        switch (a) {
            case "on", "off" -> {
                DreamTrialService.setEnabled(a.equals("on"));
                s.config(); // registers / unregisters the Lucid & Vellum field ids
                return Reply.text("夢幻的試煉已" + (a.equals("on") ? "開啟" : "關閉") + "（本次執行有效）。", "@dreamtrial " + a);
            }
            case "status" -> {
                DreamTrialConfig c = s.config();
                return Reply.text("夢幻的試煉：" + (DreamTrialService.enabled() ? "開啟" : "關閉") + "；試煉 " + c.trials().size()
                        + " 個；限時 " + c.timeLimitSeconds() + " 秒；復活 " + c.revives() + "；HP 倍率 " + c.hpMultiplier()
                        + "；露希妲變身原因 r=" + ChangeBurning.get().transformReason(chr)
                        + (s.inTrial(chr) ? "；目前在 " + s.trialOf(chr).key() : ""), "@dreamtrial status");
            }
            case "list" -> {
                StringBuilder sb = new StringBuilder();
                for (DreamTrialConfig.Trial t : s.config().trials()) {
                    sb.append(t.key()).append(t.enabled() ? "" : "(關)").append(':').append(t.name()).append(' ')
                            .append(s.used(chr, t)).append('/').append(s.config().dailyLimitOf(t)).append("；");
                }
                return Reply.text(sb.toString(), "@dreamtrial list");
            }
            case "reload" -> {
                try {
                    DreamTrialService.file().parse(java.nio.file.Files.readString(DreamTrialService.file().path()));
                } catch (Exception e) {
                    return Reply.text("dreamtrial.json 有錯誤，沒有載入：" + e.getMessage(), "@dreamtrial reload failed");
                }
                DreamTrialConfig c = s.config();
                return Reply.text("已讀取 dreamtrial.json（試煉 " + c.trials().size() + " 個；檔案變更後 2 秒內自動生效）。", "@dreamtrial reload");
            }
            case "enter" -> {
                String key = arg(args, 1);
                String r = s.enter(chr, key);
                return Reply.text(r == null ? "進入 " + key : r, "@dreamtrial enter " + key + " -> " + (r == null ? "OK" : r));
            }
            case "clear" -> {
                String r = s.gmClearStage(chr);
                return Reply.text(r, "@dreamtrial clear -> " + r);
            }
            case "leave" -> {
                boolean ok = s.leave(chr);
                return Reply.text(ok ? "已離開試煉。" : "不在試煉中。", "@dreamtrial leave");
            }
            case "reset" -> {
                String key = arg(args, 1);
                String r = s.gmReset(chr, key);
                return Reply.text(r, "@dreamtrial reset " + key);
            }
            case "hp" -> {
                String key = arg(args, 1);
                DreamTrialConfig c = s.config();
                DreamTrialConfig.Trial t = c.trial(key);
                if (t == null) return Reply.text("找不到試煉 " + key, "@dreamtrial hp " + key);
                int lv = args.size() > 2 ? number(args.get(2)) : ChangeBurning.lucidLevel(chr);
                return Reply.text(DreamTrialService.hpTable(c, t, lv), "@dreamtrial hp " + key);
            }
            default -> throw new IllegalArgumentException("unknown action " + a);
        }
    }

    private static String arg(List<String> args, int i) {
        if (args.size() <= i) throw new IllegalArgumentException("missing argument");
        return args.get(i);
    }

    private static int number(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("not a number");
        }
    }
}
