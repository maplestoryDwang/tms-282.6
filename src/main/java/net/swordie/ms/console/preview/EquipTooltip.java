package net.swordie.ms.console.preview;

import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.EquipFlame;
import net.swordie.ms.client.character.items.ItemOption;
import net.swordie.ms.enums.EquipBaseStat;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.StringData;

import java.util.List;

/** HTML tooltip for an equip: name, stars, required level, total stats, bonus (flame) stats, potentials. */
public final class EquipTooltip {
    private EquipTooltip() { }

    private record StatRow(String label, EquipBaseStat stat, boolean percent) { }

    private static final List<StatRow> STATS = List.of(
            new StatRow("STR", EquipBaseStat.iStr, false), new StatRow("DEX", EquipBaseStat.iDex, false),
            new StatRow("INT", EquipBaseStat.iInt, false), new StatRow("LUK", EquipBaseStat.iLuk, false),
            new StatRow("最大 HP", EquipBaseStat.iMaxHP, false), new StatRow("最大 MP", EquipBaseStat.iMaxMP, false),
            new StatRow("攻擊力", EquipBaseStat.iPAD, false), new StatRow("魔法攻擊力", EquipBaseStat.iMAD, false),
            new StatRow("防禦力", EquipBaseStat.iPDD, false), new StatRow("移動速度", EquipBaseStat.iSpeed, false),
            new StatRow("跳躍力", EquipBaseStat.iJump, false), new StatRow("BOSS 攻擊時傷害", EquipBaseStat.bdr, true),
            new StatRow("無視怪物防禦率", EquipBaseStat.imdr, true), new StatRow("總傷害", EquipBaseStat.damR, true),
            new StatRow("全屬性", EquipBaseStat.statR, true));

    public static String name(int itemId) {
        String n = StringData.getItemStringById(itemId);
        return n == null || n.isBlank() ? "道具 " + itemId : n;
    }

    static String gradeName(int grade) {
        return switch (grade) {
            case 17 -> "特殊";
            case 18 -> "稀有";
            case 19 -> "罕見";
            case 20 -> "傳說";
            default -> grade > 0 && grade < 16 ? "未鑑定" : null;
        };
    }

    static String gradeColor(int grade) {
        return switch (grade) {
            case 17 -> "#3a7bd5";
            case 18 -> "#8e44ad";
            case 19 -> "#d4a017";
            case 20 -> "#2e9e44";
            default -> "#777777";
        };
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static int total(Equip e, EquipBaseStat stat) {
        try {
            return e.getTotalStat(stat);
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    public static String html(Equip e) {
        StringBuilder sb = new StringBuilder("<html><body style='width:260px;font-family:sans-serif'>");
        sb.append("<b style='font-size:1.1em'>").append(esc(name(e.getItemId()))).append("</b>");
        if (e.getChuc() > 0) sb.append("<br><span style='color:#e0a800'>").append("★".repeat(Math.min(e.getChuc(), 30))).append("</span> ").append(e.getChuc()).append(" 星");
        sb.append("<br><span style='color:#666'>ID ").append(e.getItemId()).append(" · 需求等級 ").append(e.getrLevel() + e.getiIncReq());
        if (e.getTuc() > 0 || e.getCuc() > 0) sb.append(" · 可升級次數 ").append(e.getTuc());
        if (e.getCuc() > 0) sb.append(" · 已強化 ").append(e.getCuc());
        sb.append("</span><hr>");
        EquipFlame flame = e.getFlameStat();
        for (StatRow row : STATS) {
            int v = total(e, row.stat());
            if (v == 0) continue;
            sb.append(row.label()).append(" : +").append(v).append(row.percent() ? "%" : "");
            int f = flame == null ? 0 : flameValue(flame, row.stat());
            if (f != 0) sb.append(" <span style='color:#1e9e8a'>(附加 +").append(f).append(row.percent() ? "%" : "").append(")</span>");
            sb.append("<br>");
        }
        appendPotential(sb, "潛在能力", e.getGrade(), e.getOptions(), 0, e);
        appendPotential(sb, "附加潛在能力", e.getBonusGrade(), e.getOptions(), 3, e);
        sb.append("</body></html>");
        return sb.toString();
    }

    private static int flameValue(EquipFlame f, EquipBaseStat stat) {
        return switch (stat) {
            case iStr -> f.getSTR();
            case iDex -> f.getDEX();
            case iInt -> f.getINT();
            case iLuk -> f.getLUK();
            case iMaxHP -> f.getHP();
            case iMaxMP -> f.getMP();
            case iPAD -> f.getPAD();
            case iMAD -> f.getMAD();
            case iPDD -> f.getPDD();
            case iSpeed -> f.getSpeed();
            case iJump -> f.getJump();
            case bdr -> f.getBossDamageR();
            case damR -> f.getDamage();
            case statR -> f.getAllStatR();
            default -> 0;
        };
    }

    private static void appendPotential(StringBuilder sb, String title, int grade, List<Integer> options, int from, Equip e) {
        if (options == null || options.size() < from + 3) return;
        boolean any = false;
        for (int i = from; i < from + 3; i++) any |= options.get(i) != 0;
        if (!any) return;
        String g = gradeName(grade);
        sb.append("<hr><b style='color:").append(gradeColor(grade)).append("'>").append(title);
        if (g != null) sb.append("（").append(g).append("）");
        sb.append("</b><br>");
        if (grade > 0 && grade < 16) {
            sb.append("<span style='color:#777'>尚未鑑定</span><br>");
            return;
        }
        int level = e.getrLevel() + e.getiIncReq();
        for (int i = from; i < from + 3; i++) {
            int id = options.get(i);
            if (id == 0) continue;
            ItemOption option = ItemData.getItemOptionById(id);
            String text = option == null ? null : option.getString(level);
            sb.append("· ").append(esc(text == null || text.isBlank() ? "選項 " + id : text)).append("<br>");
        }
    }
}
