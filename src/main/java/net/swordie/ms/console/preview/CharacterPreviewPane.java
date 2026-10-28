package net.swordie.ms.console.preview;

import net.swordie.ms.client.character.items.Equip;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/** 角色預覽: rendered character plus the in-game style equipment window (normal / cash) with item tooltips. */
public final class CharacterPreviewPane extends JPanel {
    private static final int SLOT = 52, AVATAR_SCALE = 3;

    /** Equipment window layout: {bodyPart, column, row}. Cash parts use the same cell as their normal part. */
    private static final int[][] LAYOUT = {
            {12, 0, 0}, {1, 2, 0}, {61, 4, 0},
            {13, 0, 1}, {17, 1, 1}, {2, 2, 1}, {56, 4, 1},
            {15, 0, 2}, {65, 1, 2}, {3, 2, 2}, {4, 3, 2}, {49, 4, 2},
            {16, 0, 3}, {11, 1, 3}, {5, 2, 3}, {51, 3, 3}, {10, 4, 3},
            {52, 0, 4}, {22, 1, 4}, {6, 2, 4}, {8, 3, 4}, {9, 4, 4},
            {7, 2, 5}, {53, 3, 5}, {54, 4, 5},
    };
    private static final Map<Integer, String> SLOT_NAMES = Map.ofEntries(
            Map.entry(1, "帽子"), Map.entry(2, "臉飾"), Map.entry(3, "眼飾"), Map.entry(4, "耳環"), Map.entry(5, "上衣"),
            Map.entry(6, "褲裙"), Map.entry(7, "鞋子"), Map.entry(8, "手套"), Map.entry(9, "披風"), Map.entry(10, "輔助"),
            Map.entry(11, "武器"), Map.entry(12, "戒指"), Map.entry(13, "戒指"), Map.entry(15, "戒指"), Map.entry(16, "戒指"),
            Map.entry(17, "墜飾"), Map.entry(65, "墜飾"), Map.entry(22, "腰帶"), Map.entry(49, "勳章"), Map.entry(51, "肩飾"),
            Map.entry(52, "口袋"), Map.entry(53, "機器人"), Map.entry(54, "心臟"), Map.entry(56, "徽章"), Map.entry(61, "徽章"));

    private final JTextField nameField = new JTextField(16);
    private final JToggleButton cashToggle = new JToggleButton("現金裝備");
    private final JLabel avatarLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel infoLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel status = new JLabel(" ");
    private final Map<Integer, JLabel> cells = new HashMap<>();
    private CharacterPreviewData.Snapshot snapshot;
    private final JLabel androidLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel androidInfo = new JLabel(" ", SwingConstants.CENTER);
    private final Map<Integer, JLabel> androidCells = new HashMap<>();
    private AndroidPreview.Snapshot android;
    /** Android window: android, heart, then the android's own equipment (hat .. weapon). */
    private static final int[][] ANDROID_LAYOUT = {
            {AndroidPreview.ANDROID_SLOT, 0, 0}, {AndroidPreview.HEART_SLOT, 1, 0},
            {1200, 0, 1}, {1201, 1, 1}, {1202, 0, 2}, {1203, 1, 2}, {1204, 0, 3}, {1205, 1, 3}, {1206, 0, 4}, {1207, 1, 4}};
    private static final Map<Integer, String> ANDROID_SLOT_NAMES = Map.of(
            AndroidPreview.ANDROID_SLOT, "機器人", AndroidPreview.HEART_SLOT, "心臟", 1200, "帽子", 1201, "披風", 1202, "臉飾",
            1203, "上衣", 1204, "褲裙", 1205, "鞋子", 1206, "手套", 1207, "武器");
    private final Map<Integer, ImageIcon> iconCache = new HashMap<>();

    public CharacterPreviewPane() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        bar.add(new JLabel("角色名稱:"));
        bar.add(nameField);
        JButton load = new JButton("預覽");
        load.addActionListener(e -> show(nameField.getText()));
        nameField.addActionListener(e -> show(nameField.getText()));
        cashToggle.addActionListener(e -> fillGrid());
        bar.add(load);
        bar.add(cashToggle);
        bar.add(status);
        add(bar, BorderLayout.NORTH);

        JPanel avatarPanel = new JPanel(new BorderLayout());
        avatarPanel.setBorder(BorderFactory.createTitledBorder("人物預覽"));
        avatarPanel.setBackground(new Color(0xF4F6FA));
        avatarLabel.setPreferredSize(new Dimension(300, 360));
        avatarPanel.add(avatarLabel, BorderLayout.CENTER);
        avatarPanel.add(infoLabel, BorderLayout.SOUTH);

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setBorder(BorderFactory.createTitledBorder("裝備"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 3, 3, 3);
        for (int[] cell : LAYOUT) {
            JLabel slot = new JLabel("", SwingConstants.CENTER);
            slot.setPreferredSize(new Dimension(SLOT, SLOT));
            slot.setOpaque(true);
            c.gridx = cell[1];
            c.gridy = cell[2];
            grid.add(slot, c);
            cells.put(cell[0], slot);
        }
        clearGrid();

        JTabbedPane right = new JTabbedPane();
        right.addTab("裝備", grid);
        right.addTab("機器人", buildAndroidPanel());
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, avatarPanel, right);
        split.setResizeWeight(0.45);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
        fillAndroidGrid();
        ToolTipManager.sharedInstance().setDismissDelay(60_000);
    }

    private JComponent buildAndroidPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel view = new JPanel(new BorderLayout());
        view.setBackground(new Color(0xF4F6FA));
        view.setBorder(BorderFactory.createTitledBorder("機器人外觀"));
        androidLabel.setPreferredSize(new Dimension(220, 280));
        view.add(androidLabel, BorderLayout.CENTER);
        view.add(androidInfo, BorderLayout.SOUTH);
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setBorder(BorderFactory.createTitledBorder("機器人裝備"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 3, 3, 3);
        for (int[] cell : ANDROID_LAYOUT) {
            JLabel slot = new JLabel("", SwingConstants.CENTER);
            slot.setPreferredSize(new Dimension(SLOT, SLOT));
            slot.setOpaque(true);
            c.gridx = cell[1];
            c.gridy = cell[2];
            grid.add(slot, c);
            androidCells.put(cell[0], slot);
        }
        panel.add(view, BorderLayout.CENTER);
        panel.add(grid, BorderLayout.EAST);
        return panel;
    }

    /** Loads and shows {@code name} in the background. */
    public void show(String name) {
        if (name == null || name.isBlank()) return;
        nameField.setText(name.trim());
        status.setText("載入中…（第一次讀取客戶端 WZ 約需 10–20 秒）");
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                return prepare(CharacterPreviewData.load(name.trim()));
            }

            @Override
            protected void done() {
                try {
                    apply(get());
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    status.setText("載入失敗：" + cause.getMessage());
                }
            }
        }.execute();
    }

    /** Background part of a load: avatar render and equipment icons from the client WZ. */
    static Object[] prepare(CharacterPreviewData.Snapshot s) {
        WzClientData wz = WzClientData.get();
        BufferedImage avatar = new AvatarRenderer(wz).render(s.look());
        Map<Integer, ImageIcon> icons = new HashMap<>();
        for (Equip e : s.normal().values()) icons.put(e.getItemId(), icon(wz, e.getItemId()));
        for (Equip e : s.cash().values()) icons.put(e.getItemId(), icon(wz, e.getItemId()));
        AndroidPreview.Snapshot android = AndroidPreview.of(s.normal());
        AvatarRenderer.Look androidLook = AndroidPreview.look(wz, android);
        BufferedImage androidImage = androidLook == null ? null : new AvatarRenderer(wz).render(androidLook);
        return new Object[]{s, avatar, icons, android, androidImage};
    }

    /** UI part of a load (event thread): shows a prepared result. */
    @SuppressWarnings("unchecked")
    void apply(Object[] r) {
        snapshot = (CharacterPreviewData.Snapshot) r[0];
        iconCache.putAll((Map<Integer, ImageIcon>) r[2]);
        avatarLabel.setIcon(new ImageIcon(scale((BufferedImage) r[1], AVATAR_SCALE)));
        android = r.length > 3 ? (AndroidPreview.Snapshot) r[3] : null;
        BufferedImage androidImage = r.length > 4 ? (BufferedImage) r[4] : null;
        androidLabel.setIcon(androidImage == null ? null : new ImageIcon(scale(androidImage, AVATAR_SCALE)));
        if (android == null || !android.present()) {
            androidLabel.setText("未裝備機器人");
            androidInfo.setText(" ");
        } else {
            androidLabel.setText(androidImage == null ? "客戶端無此機器人資料" : "");
            String grade = android.heart() == null ? "未裝備心臟（機器人不會出現）"
                    : "心臟等級 " + android.heart().getAndroidGrade() + "（機器人等級 " + android.android().getAndroidGrade() + "）";
            androidInfo.setText("<html><center><b>" + EquipTooltip.name(android.android().getItemId()) + "</b><br>" + grade
                    + (android.live() == null ? "<br><font color='#777'>外觀為此型號預設造型</font>" : "") + "</center></html>");
        }
        String job = net.swordie.ms.console.config.JobNames.of(snapshot.job());
        infoLabel.setText("<html><center><b>" + snapshot.name() + "</b>　Lv." + snapshot.level() + "　"
                + (job == null ? "職業 " + snapshot.job() : job) + (snapshot.online() ? "　<font color='#2e9e44'>在線</font>" : "")
                + "</center></html>");
        status.setText("已載入 " + snapshot.normal().size() + " 件裝備、" + snapshot.cash().size() + " 件現金裝備");
        fillGrid();
    }

    static ImageIcon icon(WzClientData wz, int itemId) {
        String dir = AvatarRenderer.equipDir(itemId);
        if (dir == null) return null;
        Object img = wz.img("Character/" + dir, AvatarRenderer.img(itemId));
        BufferedImage icon = img == null ? null : wz.image(wz.child(img, "info/icon"));
        return icon == null ? null : new ImageIcon(icon);
    }

    static BufferedImage scale(BufferedImage src, int factor) {
        if (factor <= 1) return src;
        BufferedImage out = new BufferedImage(src.getWidth() * factor, src.getHeight() * factor, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(src, 0, 0, out.getWidth(), out.getHeight(), null);
        g.dispose();
        return out;
    }

    private void clearGrid() {
        cells.forEach((part, cell) -> {
            cell.setIcon(null);
            cell.setText("<html><center><font color='#aaaaaa' size='2'>" + SLOT_NAMES.getOrDefault(part, "") + "</font></center></html>");
            cell.setToolTipText(null);
            cell.setBackground(new Color(0xEEF0F4));
            cell.setBorder(BorderFactory.createLineBorder(new Color(0xCCD0D8)));
        });
    }

    private void fillAndroidGrid() {
        androidCells.forEach((part, cell) -> {
            Equip equip = android == null ? null
                    : part == AndroidPreview.ANDROID_SLOT ? android.android()
                    : part == AndroidPreview.HEART_SLOT ? android.heart() : android.equips().get(part);
            ImageIcon icon = equip == null ? null : iconCache.get(equip.getItemId());
            cell.setIcon(icon);
            cell.setText(equip == null ? "<html><center><font color='#aaaaaa' size='2'>" + ANDROID_SLOT_NAMES.get(part) + "</font></center></html>"
                    : icon == null ? "<html><center><font size='2'>" + equip.getItemId() + "</font></center></html>" : "");
            cell.setToolTipText(equip == null ? null : EquipTooltip.html(equip));
            cell.setBackground(equip == null ? new Color(0xEEF0F4) : Color.WHITE);
            cell.setBorder(equip == null ? BorderFactory.createLineBorder(new Color(0xCCD0D8)) : border(equip));
        });
    }

    private void fillGrid() {
        fillAndroidGrid();
        clearGrid();
        if (snapshot == null) return;
        Map<Integer, Equip> shown = cashToggle.isSelected() ? snapshot.cash() : snapshot.normal();
        for (Map.Entry<Integer, Equip> e : shown.entrySet()) {
            JLabel cell = cells.get(e.getKey());
            if (cell == null) continue;
            Equip equip = e.getValue();
            ImageIcon icon = iconCache.get(equip.getItemId());
            cell.setText(icon == null ? "<html><center><font size='2'>" + equip.getItemId() + "</font></center></html>" : "");
            cell.setIcon(icon);
            cell.setBackground(Color.WHITE);
            cell.setBorder(border(equip));
            cell.setToolTipText(EquipTooltip.html(equip));
        }
    }

    private static Border border(Equip e) {
        int grade = e.getGrade();
        Color color = grade >= 17 && grade <= 20 ? Color.decode(EquipTooltip.gradeColor(grade)) : new Color(0x9AA3B2);
        return BorderFactory.createLineBorder(color, grade >= 17 ? 2 : 1);
    }
}
