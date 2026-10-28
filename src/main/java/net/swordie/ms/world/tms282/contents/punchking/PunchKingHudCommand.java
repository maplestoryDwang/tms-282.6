package net.swordie.ms.world.tms282.contents.punchking;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.tms282.Tms282ChatCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * GM probe for the punch-king HUD (Command pattern, registered in {@code Tms282ChatCommands.admin}):
 * <ul>
 *     <li>{@code @punchhud <sub 0..8> <int>...}: sends one LP0833 with exactly the client's read count for that sub,
 *     only while the GM stands in a fieldType-236 map (the HUD component does not exist elsewhere);</li>
 *     <li>{@code @punchhud on|off|status}: the {@link PunchKingHud} switch for this server run (event scripts).</li>
 * </ul>
 * Used to confirm the sub meanings in game before {@code tms282.punchking.hud=true} is set for good.
 */
public record PunchKingHudCommand() implements Tms282ChatCommand {
    public String name() {
        return "punchhud";
    }

    public int minAccountType() {
        return 3;
    }

    public String usage() {
        return "@punchhud <sub 0..8> <整數…>（0:1 1:1 2:1 3:2 4:1 5:1 6:3 7:4 8:11 個）｜@punchhud on|off|status";
    }

    public Reply execute(Char chr, List<String> args) {
        if (args.isEmpty()) {
            throw new IllegalArgumentException("missing sub");
        }
        String first = args.getFirst().toLowerCase(java.util.Locale.ROOT);
        switch (first) {
            case "on", "off" -> {
                PunchKingHud.setEnabled(first.equals("on"));
                return Reply.text("拳擊王計量條（事件腳本）已" + (first.equals("on") ? "開啟" : "關閉") + "（本次執行有效）。",
                        "@punchhud " + first);
            }
            case "status" -> {
                return Reply.text("拳擊王計量條：" + (PunchKingHud.enabled() ? "開啟" : "關閉") + "；目前地圖"
                        + (PunchKingHud.inHudField(chr) ? "是" : "不是") + " fieldType 236。", "@punchhud status");
            }
            default -> {
            }
        }
        int sub = parse(first);
        int n = PunchKingHudPackets.arity(sub);
        if (n < 0) {
            throw new IllegalArgumentException("sub " + sub);
        }
        List<String> rest = args.subList(1, args.size());
        if (rest.size() != n) {
            throw new IllegalArgumentException("sub " + sub + " needs " + n);
        }
        if (!PunchKingHud.inHudField(chr)) {
            return Reply.text("請先到拳擊王地圖（fieldType 236，例如 993222200）再測試。", "@punchhud refused: not a HUD field");
        }
        List<Integer> values = new ArrayList<>();
        for (String s : rest) {
            values.add(parse(s));
        }
        int[] ints = values.stream().mapToInt(Integer::intValue).toArray();
        return new Reply(List.of(PunchKingHudPackets.hud(sub, ints)), "已送出 LP0833 sub " + sub + " " + values,
                "@punchhud sub=" + sub + " args=" + values + " field=" + chr.getField().getId());
    }

    private static int parse(String s) {
        try {
            return s.startsWith("0x") ? Integer.parseUnsignedInt(s.substring(2), 16) : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("not a number: " + s);
        }
    }
}
