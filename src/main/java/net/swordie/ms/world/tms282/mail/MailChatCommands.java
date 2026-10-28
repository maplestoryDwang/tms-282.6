package net.swordie.ms.world.tms282.mail;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.tms282.Tms282ChatCommand;
import net.swordie.ms.enums.InvType;

import java.util.List;
import java.util.Locale;

/**
 * Player chat commands for memo / Duey while the 282 memo and parcel packets are unverified (Command pattern,
 * {@link Tms282ChatCommand}; account type 0 = every player). Register with {@link #all()}.
 */
public final class MailChatCommands {
    private MailChatCommands() { }

    public static List<Tms282ChatCommand> all() {
        return List.of(new MemoCommand(MemoService.get()), new ParcelCommand(ParcelService.get()));
    }

    static int id(List<String> args, int index) {
        if (args.size() <= index) throw new IllegalArgumentException("missing id");
        try {
            return Integer.parseInt(args.get(index).replace("#", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("not a number: " + args.get(index));
        }
    }

    static long amount(String s) {
        try {
            long v = Long.parseLong(s.replace(",", ""));
            if (v < 0) throw new IllegalArgumentException("negative");
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("not a number: " + s);
        }
    }

    static Tms282ChatCommand.Reply reply(MailResult r, String log) {
        String text = r.message().replace("\r\n", " | ");
        return Tms282ChatCommand.Reply.text(text, log + (r.ok() ? " ok" : " refused: " + text));
    }

    /** @memo | @memo read id | @memo del id | @memo send name text... */
    public record MemoCommand(MemoService memos) implements Tms282ChatCommand {
        @Override
        public String name() {
            return "memo";
        }

        @Override
        public int minAccountType() {
            return 0;
        }

        @Override
        public String usage() {
            return "@memo（列表） | @memo read 編號 | @memo del 編號 | @memo send 角色名稱 內容";
        }

        @Override
        public Reply execute(Char chr, List<String> args) {
            if (args.isEmpty()) return reply(memos.listText(chr), "@memo list");
            String sub = args.getFirst().toLowerCase(Locale.ROOT);
            return switch (sub) {
                case "read" -> reply(memos.read(chr, id(args, 1)), "@memo read");
                case "del", "delete" -> reply(memos.delete(chr, id(args, 1)), "@memo del");
                case "send" -> {
                    if (args.size() < 3) throw new IllegalArgumentException("send needs name and text");
                    yield reply(memos.send(chr, args.get(1), String.join(" ", args.subList(2, args.size()))), "@memo send");
                }
                default -> throw new IllegalArgumentException("unknown " + sub);
            };
        }
    }

    /**
     * @parcel | @parcel get id | @parcel refuse id | @parcel fee meso |
     * @parcel send name meso [欄位 格子 數量] [quick]  (欄位 1=裝備 2=消耗 3=裝飾 4=其他)
     */
    public record ParcelCommand(ParcelService parcels) implements Tms282ChatCommand {
        @Override
        public String name() {
            return "parcel";
        }

        @Override
        public int minAccountType() {
            return 0;
        }

        @Override
        public String usage() {
            return "@parcel（列表） | @parcel get 編號 | @parcel refuse 編號 | @parcel fee 楓幣 | "
                    + "@parcel send 角色名稱 楓幣 [欄位(1裝備2消耗3裝飾4其他) 格子 數量] [quick]";
        }

        @Override
        public Reply execute(Char chr, List<String> args) {
            if (args.isEmpty()) return reply(parcels.listText(chr), "@parcel list");
            String sub = args.getFirst().toLowerCase(Locale.ROOT);
            switch (sub) {
                case "get":
                    return reply(parcels.receive(chr, id(args, 1)), "@parcel get");
                case "refuse":
                case "return":
                    return reply(parcels.refuse(chr, id(args, 1)), "@parcel refuse");
                case "fee": {
                    long money = args.size() > 1 ? amount(args.get(1)) : 0;
                    return Reply.text("手續費：" + String.format("%,d", ParcelFees.fee(money)) + " 楓幣。", "@parcel fee");
                }
                case "send": {
                    List<String> rest = args.subList(1, args.size());
                    boolean quick = !rest.isEmpty() && rest.getLast().equalsIgnoreCase("quick");
                    if (quick) rest = rest.subList(0, rest.size() - 1);
                    if (rest.size() != 2 && rest.size() != 5) throw new IllegalArgumentException("bad send arguments");
                    long money = amount(rest.get(1));
                    InvType inv = null;
                    int slot = 0, qty = 0;
                    if (rest.size() == 5) {
                        inv = InvType.getInvTypeByVal((int) amount(rest.get(2)));
                        if (inv == null) throw new IllegalArgumentException("bad inventory");
                        slot = (int) amount(rest.get(3));
                        qty = (int) amount(rest.get(4));
                    }
                    return reply(parcels.send(chr, rest.getFirst(), money, inv, slot, qty, "", quick), "@parcel send");
                }
                default:
                    throw new IllegalArgumentException("unknown " + sub);
            }
        }
    }
}
