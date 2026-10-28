package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.Tms282Notice;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * {@code @notice [--kind=chat,slide,...] <text>} (notice-1005): a world announcement through {@link Tms282Notice}.
 * Without {@code --kind} it uses {@code tms282.notice.kinds} (default {@code chat}, LP04F0 type 11 - the system chat
 * line proven to display on 282). {@code --kind=} is the GM probe for the kinds not yet seen on a live client
 * ({@code notice}/{@code slide} LP00E1, {@code weather} LP010C, {@code tip} LP04F0 type 7). Text: MS950 only, no control
 * characters, at most {@code tms282.notice.maxBytes} (200) MS950 bytes; words are joined by one space. Account type
 * >= 3.
 */
public final class Tms282NoticeCommand implements Tms282ChatCommand {
    public static final String KIND_FLAG = "--kind=";

    private final BiFunction<List<Tms282Notice.Kind>, String, Integer> sender;

    public Tms282NoticeCommand() {
        this(Tms282Notice::broadcast);
    }

    /** {@code sender} gets the kinds and the checked text and returns the characters reached (checks record it). */
    public Tms282NoticeCommand(BiFunction<List<Tms282Notice.Kind>, String, Integer> sender) {
        this.sender = Objects.requireNonNull(sender, "sender");
    }

    @Override
    public String name() {
        return "notice";
    }

    @Override
    public int minAccountType() {
        return 3;
    }

    @Override
    public String usage() {
        return "@notice [--kind=chat|tip|notice|slide|weather] <公告內容>";
    }

    @Override
    public Reply execute(Char chr, List<String> args) {
        List<String> words = new ArrayList<>(args);
        List<Tms282Notice.Kind> kinds;
        if (!words.isEmpty() && words.get(0).toLowerCase(java.util.Locale.ROOT).startsWith(KIND_FLAG)) {
            List<String> unknown = new ArrayList<>();
            kinds = Tms282Notice.parseKinds(words.remove(0).substring(KIND_FLAG.length()), unknown);
            if (!unknown.isEmpty()) {
                throw new IllegalArgumentException("unknown kind " + unknown);
            }
        } else {
            kinds = Tms282Notice.defaultKinds();
        }
        Tms282Notice.Checked c = Tms282Notice.validate(String.join(" ", words));
        if (!c.ok()) {
            if ("empty".equals(c.problem())) {
                throw new IllegalArgumentException("empty text");
            }
            String why = switch (c.problem()) {
                case "tooLong" -> "公告太長（" + c.bytes() + " bytes，上限 " + Tms282Notice.maxBytes() + "）";
                case "charset" -> "公告含有繁體中文字型（MS950）無法顯示的字元";
                default -> "公告不能含控制字元";
            };
            return Reply.text(why, "@notice refused: " + c.problem());
        }
        int online = sender.apply(kinds, c.text());
        String ids = String.join(",", kinds.stream().map(Tms282Notice.Kind::id).toList());
        boolean probe = kinds.stream().anyMatch(k -> !k.proven());
        return Reply.text("公告已送出（" + ids + (probe ? "，含未實測封包" : "") + "），線上 " + online + " 人",
                "@notice kinds=" + ids + " online=" + online + " bytes=" + c.bytes() + " by " + (chr == null ? "?" : chr.getName()) + ": " + c.text());
    }
}
