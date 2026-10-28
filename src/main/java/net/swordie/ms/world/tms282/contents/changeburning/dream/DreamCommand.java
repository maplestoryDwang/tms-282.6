package net.swordie.ms.world.tms282.contents.changeburning.dream;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.tms282.Tms282ChatCommand;

import java.util.List;
import java.util.Locale;

/**
 * GM probe for 夢幻的裝備 (registered in {@code Tms282ChatCommands.admin}):
 * {@code @dream on|off|status}, {@code @dream give <ItemInfo 1..15>}, {@code @dream dust <n>}, {@code @dream bag}.
 * Used to check the 露希妲物品欄 in game before {@code tms282.lucid.dreamItems=true} is set for good.
 */
public record DreamCommand() implements Tms282ChatCommand {
    public String name() {
        return "dream";
    }

    public int minAccountType() {
        return 3;
    }

    public String usage() {
        return "@dream on|off|status｜@dream give <1..15>｜@dream dust <數量>｜@dream bag";
    }

    public Reply execute(Char chr, List<String> args) {
        if (args.isEmpty()) {
            throw new IllegalArgumentException("missing action");
        }
        String a = args.getFirst().toLowerCase(Locale.ROOT);
        switch (a) {
            case "on", "off" -> {
                DreamService.setEnabled(a.equals("on"));
                return Reply.text("夢幻的裝備已" + (a.equals("on") ? "開啟" : "關閉") + "（本次執行有效）。", "@dream " + a);
            }
            case "status" -> {
                return Reply.text("夢幻的裝備：" + (DreamService.enabled() ? "開啟" : "關閉"), "@dream status");
            }
            case "give" -> {
                int idx = number(args, 1);
                DreamService.Result r = DreamService.get().grant(chr, idx);
                return Reply.text("發放 ItemInfo " + idx + "：" + r, "@dream give " + idx + " -> " + r);
            }
            case "dust" -> {
                int n = number(args, 1);
                DreamService.Result r = DreamService.get().setDust(chr, n);
                return Reply.text("夢幻粉末設為 " + n + "：" + r, "@dream dust " + n + " -> " + r);
            }
            case "bag" -> {
                DreamStore.Bag b = DreamService.get().bag(chr);
                if (b == null) {
                    return Reply.text("讀不到露希妲物品欄。", "@dream bag failed");
                }
                StringBuilder sb = new StringBuilder("粉末 " + b.dust() + "；");
                for (DreamStore.Equip e : b.equips()) {
                    DreamCatalog.Info info = DreamService.get().catalog().info(e.infoIndex());
                    sb.append(e.pos()).append(':').append(info == null ? e.infoIndex() : info.name()).append('+').append(e.enhance()).append(' ');
                }
                return Reply.text(sb.toString(), "@dream bag");
            }
            default -> throw new IllegalArgumentException("unknown action " + a);
        }
    }

    private static int number(List<String> args, int i) {
        if (args.size() <= i) {
            throw new IllegalArgumentException("missing number");
        }
        try {
            return Integer.parseInt(args.get(i));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("not a number");
        }
    }
}
