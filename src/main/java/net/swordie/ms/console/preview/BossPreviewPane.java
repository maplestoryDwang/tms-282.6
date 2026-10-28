package net.swordie.ms.console.preview;

import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.swordie.ms.console.config.BossRuntimeConfig;
import net.swordie.ms.constants.BossHpConstants;
import net.swordie.ms.enums.BossPartyEnterCountType;
import net.swordie.ms.enums.BossPartyType;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.loaders.MobData;
import net.swordie.ms.loaders.StringData;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

/** 首領預覽: every boss and difficulty with the main mob's sprite, entry rules and server-side stats. */
public final class BossPreviewPane extends JPanel {
    private static final int SPRITE_MAX = 260;
    private static final String[] ACTIONS = {"stand", "fly", "move", "regen", "attack1"};

    private final DefaultListModel<Integer> bossModel = new DefaultListModel<>();
    private final JList<Integer> bossList = new JList<>(bossModel);
    private final JPanel detail = new JPanel();
    private final Map<Integer, List<BossPartyType>> byOrder = new LinkedHashMap<>();
    private final Map<Integer, ImageIcon> sprites = new HashMap<>();

    public BossPreviewPane() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        for (BossPartyType type : BossPartyType.values()) {
            byOrder.computeIfAbsent(type.getOrderId(), k -> new ArrayList<>()).add(type);
        }
        byOrder.keySet().forEach(bossModel::addElement);
        bossList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bossList.setCellRenderer((list, orderId, index, selected, focus) -> {
            JLabel l = new JLabel(BossRuntimeConfig.bossChineseName(orderId) + "   (" + byOrder.get(orderId).size() + " 種難度)");
            l.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            l.setOpaque(true);
            l.setBackground(selected ? list.getSelectionBackground() : list.getBackground());
            l.setForeground(selected ? list.getSelectionForeground() : list.getForeground());
            return l;
        });
        bossList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && bossList.getSelectedValue() != null) showBoss(bossList.getSelectedValue());
        });
        JScrollPane listScroll = new JScrollPane(bossList);
        listScroll.setPreferredSize(new Dimension(230, 400));
        detail.setLayout(new BoxLayout(detail, BoxLayout.Y_AXIS));
        JScrollPane detailScroll = new JScrollPane(detail);
        detailScroll.getVerticalScrollBar().setUnitIncrement(24);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, detailScroll);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
        if (!bossModel.isEmpty()) bossList.setSelectedIndex(0);
        indexStatus.setForeground(new Color(0x666666));
        add(indexStatus, BorderLayout.SOUTH);
        buildPackIndex();
    }

    private final JLabel indexStatus = new JLabel(" ");

    /** Most boss art is only reachable through the mob packs; resolve their links once in the background. */
    private void buildPackIndex() {
        Set<Integer> ids = new TreeSet<>();
        for (BossPartyType type : BossPartyType.values()) {
            int id = mainMob(type);
            if (id > 0) ids.add(id);
        }
        indexStatus.setText("正在建立首領圖片索引（第一次約需 1 分鐘，之後會使用快取）…");
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                return MobPackLinks.resolve(WzClientData.get(), ids);
            }

            @Override
            protected void done() {
                try {
                    int found = get();
                    List<String> bad = MobPackLinks.unreadablePacks();
                    indexStatus.setText((found > 0 ? "首領圖片索引完成，新增 " + found + " 筆。" : "首領圖片索引已就緒。")
                            + (bad.isEmpty() ? "" : "　無法讀取：" + String.join("、", bad) + "（該檔內的首領沒有圖片）"));
                } catch (Exception ex) {
                    Throwable c = ex.getCause() == null ? ex : ex.getCause();
                    indexStatus.setText("首領圖片索引失敗：" + c.getMessage());
                }
                if (bossList.getSelectedValue() != null) showBoss(bossList.getSelectedValue());
            }
        }.execute();
    }

    static String difficulty(BossPartyType type) {
        return switch (type.getDifficulty()) {
            case Easy -> "簡單";
            case Normal -> "普通";
            case Hard -> "困難";
            case Chaos -> "混沌";
            case Extreme -> "極限";
            case Story -> "故事";
        };
    }

    static String enterLimit(BossPartyEnterCountType t) {
        return switch (t) {
            case OnceADay -> "每日 1 次";
            case TwiceAday -> "每日 2 次";
            case ThreeTimesAday -> "每日 3 次";
            case SevenTimesADay -> "每日 7 次";
            case TenTimesADay -> "每日 10 次";
            case OnceInSevendays -> "每週 1 次";
            case TwiceInSevendays -> "每週 2 次";
            case OnceInTwodays -> "每 2 天 1 次";
            case OnceInThreedays -> "每 3 天 1 次";
            case OnceInAMonth -> "每月 1 次";
            default -> t.name();
        };
    }

    /** The main mob of a difficulty: the one with the largest configured HP. */
    static int mainMob(BossPartyType type) {
        Int2LongOpenHashMap ctx = BossHpConstants.getDefaultContextHpMap(type);
        if (ctx == null || ctx.isEmpty()) return 0;
        int best = 0;
        long hp = -1;
        for (var e : ctx.int2LongEntrySet()) {
            if (e.getLongValue() > hp) {
                hp = e.getLongValue();
                best = e.getIntKey();
            }
        }
        return best;
    }

    private void showBoss(int orderId) {
        detail.removeAll();
        List<BossPartyType> types = byOrder.get(orderId);
        JLabel title = new JLabel(BossRuntimeConfig.bossChineseName(orderId));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setAlignmentX(LEFT_ALIGNMENT);
        detail.add(title);
        detail.add(Box.createVerticalStrut(8));
        for (BossPartyType type : types) {
            JPanel card = card(type);
            card.setAlignmentX(LEFT_ALIGNMENT);
            detail.add(card);
            detail.add(Box.createVerticalStrut(10));
        }
        detail.revalidate();
        detail.repaint();
    }

    private JPanel card(BossPartyType type) {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(difficulty(type)),
                BorderFactory.createEmptyBorder(4, 8, 8, 8)));
        int mobId = mainMob(type);
        JLabel sprite = new JLabel("載入圖片中…", SwingConstants.CENTER);
        sprite.setPreferredSize(new Dimension(SPRITE_MAX, SPRITE_MAX));
        card.add(sprite, BorderLayout.WEST);
        loadSprite(mobId, sprite);

        NumberFormat nf = NumberFormat.getIntegerInstance();
        StringBuilder sb = new StringBuilder("<html><table cellpadding='2'>");
        row(sb, "狀態", BossRuntimeConfig.isDifficultyEnabled(type) ? "<font color='#2e9e44'>開放</font>" : "<font color='#c0392b'>關閉</font>");
        row(sb, "入場等級", "Lv." + type.getLevelMin());
        row(sb, "入場限制", enterLimit(type.getEnterCount()));
        if (type.getPreQuest() > 0) row(sb, "前置任務", String.valueOf(type.getPreQuest()));
        if (mobId > 0) {
            String name = StringData.getMobStringById(mobId);
            row(sb, "主要怪物", (name == null ? "" : name + " ") + "(" + mobId + ")");
            row(sb, "設定 HP", nf.format(BossRuntimeConfig.getContextHp(type, mobId)));
            Mob mob = MobData.getMobById(mobId);
            if (mob != null && mob.getForcedMobStat() != null) {
                var st = mob.getForcedMobStat();
                row(sb, "怪物等級", "Lv." + st.getLevel());
                row(sb, "原始 HP", nf.format(st.getMaxHP()));
                row(sb, "防禦率", st.getPdr() + "%");
                if (st.getExp() > 0) row(sb, "經驗值", nf.format(st.getExp()));
            }
            Int2LongOpenHashMap ctx = BossHpConstants.getDefaultContextHpMap(type);
            if (ctx != null && ctx.size() > 1) row(sb, "相關怪物", ctx.size() + " 隻（多階段或部位）");
        } else {
            row(sb, "主要怪物", "<font color='#777'>無 HP 設定資料</font>");
        }
        sb.append("</table></html>");
        card.add(new JLabel(sb.toString()), BorderLayout.CENTER);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, SPRITE_MAX + 60));
        return card;
    }

    private static void row(StringBuilder sb, String k, String v) {
        sb.append("<tr><td><font color='#666'>").append(k).append("</font></td><td>").append(v).append("</td></tr>");
    }

    private void loadSprite(int mobId, JLabel target) {
        if (mobId <= 0) {
            target.setText("無圖片");
            return;
        }
        ImageIcon cached = sprites.get(mobId);
        if (cached != null) {
            target.setText("");
            target.setIcon(cached);
            return;
        }
        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() {
                BufferedImage img = sprite(WzClientData.get(), mobId);
                return img == null ? null : new ImageIcon(fit(img, SPRITE_MAX));
            }

            @Override
            protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon == null) {
                        target.setText("客戶端無此怪物圖片");
                        return;
                    }
                    sprites.put(mobId, icon);
                    target.setText("");
                    target.setIcon(icon);
                } catch (Exception ex) {
                    Throwable c = ex.getCause() == null ? ex : ex.getCause();
                    target.setText("<html><center>圖片載入失敗<br>" + c.getMessage() + "</center></html>");
                }
            }
        }.execute();
    }

    /** First frame of the first available action; follows the mob's link (server mob data, the 282 client's main
     * Mob.wz being a stub) to the mob that owns the art. */
    static BufferedImage sprite(WzClientData wz, int mobId) {
        for (int guard = 0, id = mobId; guard < 4 && id > 0; guard++) {
            Object img = wz.img("Mob/_Canvas", String.format("%07d.img", id));
            if (img != null) {
                for (String action : ACTIONS) {
                    BufferedImage bi = wz.image(wz.child(img, action + "/0"));
                    if (bi != null && bi.getWidth() > 1) return bi;
                }
            }
            BufferedImage packed = wz.imageAt(MobPackLinks.link(id));
            if (packed != null && packed.getWidth() > 1) return packed;
            Mob template = MobData.getMobById(id);
            int link = template == null ? 0 : template.getLink();
            if (link <= 0 && img != null) link = wz.integer(wz.child(img, "info/link"), 0);
            if (link == id) break;
            id = link;
        }
        return null;
    }

    static BufferedImage fit(BufferedImage src, int max) {
        double s = Math.min(1.0, (double) max / Math.max(src.getWidth(), src.getHeight()));
        if (s >= 1.0) return src;
        int w = Math.max(1, (int) (src.getWidth() * s)), h = Math.max(1, (int) (src.getHeight() * s));
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }
}
