package net.swordie.ms.client.character.social.tms282;

import net.swordie.ms.Server;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Inventory;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.tms282.Tms282ChatCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Chat commands of the social records (register them in the {@code Tms282ChatCommands} registry, see the report):
 * <ul>
 *     <li>{@code @block <角色名稱> [備註]}, {@code @unblock <角色名稱>}, {@code @blocklist}: the block list (players);</li>
 *     <li>{@code @ringlink <角色名稱>}: GM, pairs the couple / friendship ring both characters hold (live testing
 *     until the cash-shop couple / friendship purchase is ported);</li>
 *     <li>{@code @marry <角色名稱>}: GM, records a marriage with the wedding rings both characters hold.</li>
 * </ul>
 */
public final class SocialChatCommands {
    private SocialChatCommands() {
    }

    public static List<Tms282ChatCommand> all() {
        return List.of(new Block(), new Unblock(), new BlockList(), new RingLink(), new Marry());
    }

    static String joinFrom(List<String> args, int from) {
        return args.size() > from ? String.join(" ", args.subList(from, args.size())) : null;
    }

    static void requireName(List<String> args) {
        if (args.isEmpty() || args.get(0).isBlank()) {
            throw new IllegalArgumentException("name missing");
        }
    }

    public static final class Block implements Tms282ChatCommand {
        @Override
        public String name() {
            return "block";
        }

        @Override
        public int minAccountType() {
            return 0;
        }

        @Override
        public String usage() {
            return "@block <角色名稱> [備註]";
        }

        @Override
        public Reply execute(Char chr, List<String> args) {
            requireName(args);
            String name = args.get(0);
            BlockListService.AddResult r = BlockListService.get().add(chr, name, joinFrom(args, 1));
            String msg = switch (r) {
                case ADDED -> "已將 " + name + " 加入黑名單。";
                case ALREADY -> name + " 已在黑名單中。";
                case SELF -> "無法將自己的角色加入黑名單。";
                case NOT_FOUND -> "找不到角色 " + name + "。";
                case FULL -> "黑名單已滿（最多 " + BlockListService.MAX_ENTRIES + " 人）。";
                case FAILED -> "黑名單儲存失敗，請稍後再試。";
            };
            return Reply.text(msg, "@block " + name + ": " + r);
        }
    }

    public static final class Unblock implements Tms282ChatCommand {
        @Override
        public String name() {
            return "unblock";
        }

        @Override
        public int minAccountType() {
            return 0;
        }

        @Override
        public String usage() {
            return "@unblock <角色名稱>";
        }

        @Override
        public Reply execute(Char chr, List<String> args) {
            requireName(args);
            String name = args.get(0);
            boolean removed = BlockListService.get().remove(chr, name).isPresent();
            return Reply.text(removed ? "已將 " + name + " 從黑名單移除。" : name + " 不在黑名單中。",
                    "@unblock " + name + ": " + removed);
        }
    }

    public static final class BlockList implements Tms282ChatCommand {
        @Override
        public String name() {
            return "blocklist";
        }

        @Override
        public int minAccountType() {
            return 0;
        }

        @Override
        public String usage() {
            return "@blocklist";
        }

        @Override
        public Reply execute(Char chr, List<String> args) {
            List<BlockListService.Entry> list = BlockListService.get().list(chr);
            if (list.isEmpty()) {
                return Reply.text("黑名單是空的。", "@blocklist: empty");
            }
            List<String> names = new ArrayList<>();
            for (BlockListService.Entry e : list) {
                names.add(e.memo() == null || e.memo().isBlank() ? e.name() : e.name() + "(" + e.memo() + ")");
            }
            return Reply.text("黑名單（" + list.size() + "）：" + String.join("、", names), "@blocklist: " + list.size());
        }
    }

    /** The first pair ring of {@code kind} held (worn or in the equip tab) by {@code chr}, optionally of one item id. */
    static Optional<Item> heldRing(Char chr, RingKind kind, int itemId) {
        for (Inventory inv : new Inventory[]{chr.getEquippedInventory(), chr.getEquipInventory()}) {
            if (inv == null) {
                continue;
            }
            for (Item item : inv.getItems()) {
                if (item != null && RingKind.of(item.getItemId()) == kind && (itemId == 0 || item.getItemId() == itemId)) {
                    return Optional.of(item);
                }
            }
        }
        return Optional.empty();
    }

    static Char onlinePartner(Char chr, String name) {
        Char partner = Server.get().getWorld().getCharByName(name);
        if (partner == null) {
            throw new IllegalArgumentException("partner offline");
        }
        if (partner.getId() == chr.getId()) {
            throw new IllegalArgumentException("self");
        }
        return partner;
    }

    public static final class RingLink implements Tms282ChatCommand {
        @Override
        public String name() {
            return "ringlink";
        }

        @Override
        public int minAccountType() {
            return 3;
        }

        @Override
        public String usage() {
            return "@ringlink <在線角色名稱>（雙方持有同一種戀人/友情戒指）";
        }

        @Override
        public Reply execute(Char chr, List<String> args) {
            requireName(args);
            Char partner = onlinePartner(chr, args.get(0));
            for (RingKind kind : new RingKind[]{RingKind.COUPLE, RingKind.FRIENDSHIP}) {
                Optional<Item> mine = heldRing(chr, kind, 0);
                if (mine.isEmpty()) {
                    continue;
                }
                Optional<Item> theirs = heldRing(partner, kind, mine.get().getItemId());
                if (theirs.isEmpty()) {
                    continue;
                }
                boolean ok = RingRecordService.get().createPair(chr, mine.get(), partner, theirs.get());
                return Reply.text(ok ? "已與 " + partner.getName() + " 建立" + kind.displayName() + "紀錄。" : "戒指紀錄建立失敗。",
                        "@ringlink " + partner.getId() + " " + kind + " item " + mine.get().getItemId() + ": " + ok);
            }
            return Reply.text("雙方需持有同一種戀人戒指或友情戒指。", "@ringlink " + partner.getId() + ": no common ring");
        }
    }

    public static final class Marry implements Tms282ChatCommand {
        @Override
        public String name() {
            return "marry";
        }

        @Override
        public int minAccountType() {
            return 3;
        }

        @Override
        public String usage() {
            return "@marry <在線角色名稱>（你為新郎，對方為新娘）";
        }

        @Override
        public Reply execute(Char chr, List<String> args) {
            requireName(args);
            Char partner = onlinePartner(chr, args.get(0));
            Item mine = heldRing(chr, RingKind.MARRIAGE, 0).orElse(null);
            Item theirs = heldRing(partner, RingKind.MARRIAGE, 0).orElse(null);
            boolean ok = RingRecordService.get().marry(chr, mine, partner, theirs).isPresent();
            return Reply.text(ok ? "已與 " + partner.getName() + " 登記結婚。" : "登記失敗：其中一方已訂婚或已婚。",
                    "@marry " + partner.getId() + ": " + ok);
        }
    }
}
