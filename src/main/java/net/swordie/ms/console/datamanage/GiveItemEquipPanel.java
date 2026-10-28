package net.swordie.ms.console.datamanage;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** 裝備發物高級選項面板。 */
public final class GiveItemEquipPanel extends JPanel {
    private final GiveItemSpec spec;
    private final JComboBox<GiveItemSpec.EquipPreset> presetBox = new JComboBox<>(GiveItemSpec.EquipPreset.values());
    private final JPanel fieldsHost = new JPanel();
    private JScrollPane fieldsScroll;
    private final List<FieldRow> fieldRows = new ArrayList<>();
    private int boundItemId = -1;

    public GiveItemEquipPanel(GiveItemSpec spec) {
        this.spec = spec;
        setLayout(new BorderLayout(8, 8));
        buildToolbar();
        buildFields();
    }

    private void buildToolbar() {
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row1.add(new JLabel("裝備模板:"));
        presetBox.setPreferredSize(new Dimension(180, 26));
        presetBox.addActionListener(e -> onPresetChanged());
        row1.add(presetBox);
        top.add(row1);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton loadTpl = new JButton("讀取WZ模板");
        loadTpl.addActionListener(e -> loadTemplateFromWz());
        row2.add(loadTpl);
        JButton preset17 = new JButton("樣板:17星");
        preset17.addActionListener(e -> applyPreset17Star());
        row2.add(preset17);
        JButton presetPot = new JButton("樣板:史詩潛能");
        presetPot.addActionListener(e -> applyPresetEpicPot());
        row2.add(presetPot);
        top.add(row2);

        add(top, BorderLayout.NORTH);
    }

    private void buildFields() {
        fieldsHost.setLayout(new BoxLayout(fieldsHost, BoxLayout.Y_AXIS));
        addSection("基礎屬性",
                row("力量", "applyStr", "str", 0, 9999),
                row("敏捷", "applyDex", "dex", 0, 9999),
                row("智力", "applyInt", "int_", 0, 9999),
                row("運氣", "applyLuk", "luk", 0, 9999),
                row("HP", "applyHp", "hp", 0, 99999),
                row("MP", "applyMp", "mp", 0, 99999),
                row("物攻", "applyWatk", "watk", 0, 9999),
                row("魔攻", "applyMatk", "matk", 0, 9999),
                row("物防", "applyWdef", "wdef", 0, 9999),
                row("魔防", "applyMdef", "mdef", 0, 9999),
                row("命中", "applyAcc", "acc", 0, 9999),
                row("迴避", "applyAvoid", "avoid", 0, 9999),
                row("速度", "applySpeed", "speed", 0, 9999),
                row("跳躍", "applyJump", "jump", 0, 9999));
        addSection("強化 / 卷軸",
                row("星力", "applyEnhance", "enhance", 0, 30),
                row("可砸卷數", "applyUpgradeSlots", "upgradeSlots", 0, 50),
                row("砸卷等級", "applyLevel", "level", 0, 30));
        addSection("潛能",
                row("潛能1", "applyPotential1", "potential1", -99999, 999999),
                row("潛能2", "applyPotential2", "potential2", -99999, 999999),
                row("潛能3", "applyPotential3", "potential3", -99999, 999999),
                row("附加1", "applyPotential4", "potential4", -99999, 999999),
                row("附加2", "applyPotential5", "potential5", -99999, 999999),
                row("附加3", "applyPotential6", "potential6", -99999, 999999));
        addSection("附加",
                row("BOSS傷%", "applyBossDamage", "bossDamage", 0, 100),
                row("全屬性%", "applyAllStat", "allStat", 0, 100),
                row("總傷害%", "applyTotalDamage", "totalDamage", 0, 100),
                rowText("所有者", "applyOwner", "ownerText"));

        fieldsScroll = new JScrollPane(fieldsHost);
        fieldsScroll.setBorder(new TitledBorder("自定義屬性（勾選「應用」後寫入裝備）"));
        fieldsScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(fieldsScroll, BorderLayout.CENTER);
        onPresetChanged();
    }

    private void addSection(String title, FieldRow... rows) {
        JPanel section = new JPanel(new GridBagLayout());
        section.setBorder(new TitledBorder(title));
        Font bold = section.getFont().deriveFont(Font.BOLD);
        addHeaderCell(section, 0, 0, "應用", bold);
        addHeaderCell(section, 1, 0, "屬性", bold);
        addHeaderCell(section, 2, 0, "數值", bold);
        int rowIdx = 1;
        for (FieldRow fr : rows) {
            fieldRows.add(fr);
            fr.addTo(section, rowIdx++);
        }
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        fieldsHost.add(section);
        fieldsHost.add(Box.createVerticalStrut(8));
    }

    private static void addHeaderCell(JPanel p, int x, int y, String text, Font font) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = x;
        gc.gridy = y;
        gc.insets = new Insets(4, 8, 4, 8);
        gc.anchor = GridBagConstraints.WEST;
        JLabel lb = new JLabel(text);
        lb.setFont(font);
        p.add(lb, gc);
    }

    private static FieldRow row(String label, String applyKey, String valueKey, int min, int max) {
        return new FieldRow(label, applyKey, valueKey, false, min, max);
    }

    private static FieldRow rowText(String label, String applyKey, String valueKey) {
        return new FieldRow(label, applyKey, valueKey, true, 0, 0);
    }

    void onItemSelected(int itemId) {
        boundItemId = itemId;
        if (itemId > 0) {
            loadTemplateFromWz();
        }
    }

    void syncFromUi() {
        spec.equipPreset = (GiveItemSpec.EquipPreset) presetBox.getSelectedItem();
        for (FieldRow fr : fieldRows) {
            fr.syncTo(spec.equip);
        }
    }

    private void onPresetChanged() {
        boolean custom = presetBox.getSelectedItem() == GiveItemSpec.EquipPreset.CUSTOM;
        fieldsScroll.setVisible(custom);
        revalidate();
        repaint();
    }

    private void loadTemplateFromWz() {
        if (boundItemId <= 0) {
            JOptionPane.showMessageDialog(this, "請先選擇一件裝備。", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        EquipGrantHelper.loadTemplateIntoCustomize(boundItemId, spec.equip);
        refreshAllFields();
    }

    private void applyPreset17Star() {
        presetBox.setSelectedItem(GiveItemSpec.EquipPreset.CUSTOM);
        onPresetChanged();
        loadTemplateFromWz();
        setApply("applyEnhance", true);
        spec.equip.enhance = 17;
        setApply("applyPotential1", true);
        spec.equip.potential1 = -18;
        setApply("applyPotential2", true);
        spec.equip.potential2 = -18;
        setApply("applyPotential3", true);
        spec.equip.potential3 = -18;
        refreshAllFields();
    }

    private void applyPresetEpicPot() {
        presetBox.setSelectedItem(GiveItemSpec.EquipPreset.CUSTOM);
        onPresetChanged();
        loadTemplateFromWz();
        setApply("applyPotential1", true);
        spec.equip.potential1 = -18;
        setApply("applyPotential2", true);
        spec.equip.potential2 = -18;
        setApply("applyPotential3", true);
        spec.equip.potential3 = -18;
        refreshAllFields();
    }

    private void setApply(String key, boolean v) {
        for (FieldRow fr : fieldRows) {
            if (key.equals(fr.applyKey)) {
                fr.applyCheck.setSelected(v);
            }
        }
    }

    private void refreshAllFields() {
        for (FieldRow fr : fieldRows) {
            fr.refreshFrom(spec.equip);
        }
    }

    private static final class FieldRow {
        final String applyKey;
        final String valueKey;
        final boolean textField;
        final JCheckBox applyCheck = new JCheckBox();
        final JComponent valueInput;

        FieldRow(String label, String applyKey, String valueKey, boolean textField, int min, int max) {
            this.applyKey = applyKey;
            this.valueKey = valueKey;
            this.textField = textField;
            if (textField) {
                JTextField tf = new JTextField(12);
                tf.setPreferredSize(new Dimension(140, 26));
                valueInput = tf;
            } else {
                JSpinner sp = new JSpinner(new SpinnerNumberModel(0, min, max, 1));
                sp.setPreferredSize(new Dimension(100, 26));
                valueInput = sp;
            }
            applyCheck.setToolTipText("勾選後發送時寫入該屬性");
        }

        void addTo(JPanel section, int row) {
            GridBagConstraints gc = new GridBagConstraints();
            gc.gridy = row;
            gc.insets = new Insets(2, 6, 2, 6);
            gc.anchor = GridBagConstraints.WEST;
            gc.gridx = 0;
            section.add(applyCheck, gc);
            gc.gridx = 1;
            section.add(new JLabel(labelFromKey()), gc);
            gc.gridx = 2;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            section.add(valueInput, gc);
        }

        private String labelFromKey() {
            return switch (applyKey) {
                case "applyStr" -> "力量";
                case "applyDex" -> "敏捷";
                case "applyInt" -> "智力";
                case "applyLuk" -> "運氣";
                case "applyHp" -> "HP";
                case "applyMp" -> "MP";
                case "applyWatk" -> "物攻";
                case "applyMatk" -> "魔攻";
                case "applyWdef" -> "物防";
                case "applyMdef" -> "魔防";
                case "applyAcc" -> "命中";
                case "applyAvoid" -> "迴避";
                case "applySpeed" -> "速度";
                case "applyJump" -> "跳躍";
                case "applyEnhance" -> "星力";
                case "applyUpgradeSlots" -> "可砸卷數";
                case "applyLevel" -> "砸卷等級";
                case "applyPotential1" -> "潛能1";
                case "applyPotential2" -> "潛能2";
                case "applyPotential3" -> "潛能3";
                case "applyPotential4" -> "附加1";
                case "applyPotential5" -> "附加2";
                case "applyPotential6" -> "附加3";
                case "applyBossDamage" -> "BOSS傷%";
                case "applyAllStat" -> "全屬性%";
                case "applyTotalDamage" -> "總傷害%";
                case "applyOwner" -> "所有者";
                default -> applyKey;
            };
        }

        void syncTo(GiveItemSpec.EquipCustomize c) {
            boolean on = applyCheck.isSelected();
            EquipFieldBridge.setApply(c, applyKey, on);
            if (!on) {
                return;
            }
            if (textField) {
                c.owner = ((JTextField) valueInput).getText();
                return;
            }
            int val = ((Number) ((JSpinner) valueInput).getValue()).intValue();
            EquipFieldBridge.setValue(c, valueKey, val);
        }

        void refreshFrom(GiveItemSpec.EquipCustomize c) {
            applyCheck.setSelected(EquipFieldBridge.getApply(c, applyKey));
            if (textField) {
                ((JTextField) valueInput).setText(c.owner != null ? c.owner : "");
            } else {
                ((JSpinner) valueInput).setValue(EquipFieldBridge.getValue(c, valueKey));
            }
        }
    }
}
