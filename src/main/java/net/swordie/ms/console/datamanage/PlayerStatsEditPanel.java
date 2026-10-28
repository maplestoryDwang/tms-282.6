package net.swordie.ms.console.datamanage;

import net.swordie.ms.constants.GameConstants;

import javax.swing.*;
import java.awt.*;

/**
 * 玩家角色基礎屬性編輯（等級 + 四維 + MaxHP/MaxMP）。
 */
public final class PlayerStatsEditPanel extends JPanel {
    private final JTextField levelField = new JTextField(6);
    private final JTextField strField = new JTextField(6);
    private final JTextField dexField = new JTextField(6);
    private final JTextField intField = new JTextField(6);
    private final JTextField lukField = new JTextField(6);
    private final JTextField maxHpField = new JTextField(10);
    private final JTextField maxMpField = new JTextField(10);
    private final JLabel targetLabel = new JLabel("未選擇角色");
    private final JLabel limitHintLabel = new JLabel();

    private ConsoleCharacterService.PlayerTarget currentTarget;

    public PlayerStatsEditPanel() {
        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createTitledBorder("角色屬性修改"));
        refreshLimitHint();
        limitHintLabel.setFont(limitHintLabel.getFont().deriveFont(Font.PLAIN, 11f));
        add(limitHintLabel, BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        fields.add(new JLabel("目標:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1;
        fields.add(targetLabel, gbc);
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        addFieldPair(fields, gbc, 1, "等級", levelField, "力量", strField);
        addFieldPair(fields, gbc, 2, "敏捷", dexField, "智力", intField);
        addFieldPair(fields, gbc, 3, "運氣", lukField, "MaxHP", maxHpField);
        gbc.gridy = 4;
        gbc.gridx = 0;
        fields.add(new JLabel("MaxMP:"), gbc);
        gbc.gridx = 1;
        fields.add(maxMpField, gbc);
        add(fields, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton loadBtn = new JButton("從角色加載");
        loadBtn.addActionListener(e -> loadFromCharacter());
        actions.add(loadBtn);
        JButton saveBtn = new JButton("保存修改");
        saveBtn.addActionListener(e -> saveStats());
        actions.add(saveBtn);
        add(actions, BorderLayout.SOUTH);
    }

    public void refreshLimitHint() {
        limitHintLabel.setText("HP/MP 上限: " + GameConstants.MAX_HP_MP);
    }

    private static void addFieldPair(JPanel panel, GridBagConstraints gbc, int row, String label1,
                                     JTextField field1, String label2, JTextField field2) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel(label1 + ":"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.5;
        panel.add(field1, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        panel.add(new JLabel(label2 + ":"), gbc);
        gbc.gridx = 3;
        gbc.weightx = 0.5;
        panel.add(field2, gbc);
        gbc.weightx = 0;
    }

    public void onPlayerSelected(ConsoleCharacterService.PlayerTarget target) {
        currentTarget = target;
        if (target == null) {
            targetLabel.setText("未選擇角色");
            clearFields();
            return;
        }
        String status = target.isOnline() ? "在線" : "離線";
        targetLabel.setText(target.name + "（" + status + "）");
        loadFromCharacter();
    }

    private void clearFields() {
        levelField.setText("");
        strField.setText("");
        dexField.setText("");
        intField.setText("");
        lukField.setText("");
        maxHpField.setText("");
        maxMpField.setText("");
    }

    private void loadFromCharacter() {
        if (currentTarget == null) {
            JOptionPane.showMessageDialog(this, "請先在列表中選擇一名角色。", "加載", JOptionPane.WARNING_MESSAGE);
            return;
        }
        CharacterStatsSnapshot snap = ConsoleCharacterService.loadCharacterStats(currentTarget);
        if (snap == null) {
            JOptionPane.showMessageDialog(this, "無法讀取角色屬性。", "加載", JOptionPane.ERROR_MESSAGE);
            return;
        }
        fillFields(snap);
    }

    private void fillFields(CharacterStatsSnapshot snap) {
        levelField.setText(String.valueOf(snap.level));
        strField.setText(String.valueOf(snap.str));
        dexField.setText(String.valueOf(snap.dex));
        intField.setText(String.valueOf(snap.inte));
        lukField.setText(String.valueOf(snap.luk));
        maxHpField.setText(String.valueOf(snap.maxHp));
        maxMpField.setText(String.valueOf(snap.maxMp));
    }

    private CharacterStatsSnapshot readFieldsFromUi() throws NumberFormatException {
        return new CharacterStatsSnapshot(
                Integer.parseInt(levelField.getText().trim()),
                Integer.parseInt(strField.getText().trim()),
                Integer.parseInt(dexField.getText().trim()),
                Integer.parseInt(intField.getText().trim()),
                Integer.parseInt(lukField.getText().trim()),
                Integer.parseInt(maxHpField.getText().trim()),
                Integer.parseInt(maxMpField.getText().trim()));
    }

    private void saveStats() {
        if (currentTarget == null) {
            JOptionPane.showMessageDialog(this, "請先在列表中選擇一名角色。", "保存", JOptionPane.WARNING_MESSAGE);
            return;
        }
        CharacterStatsSnapshot requested;
        try {
            requested = readFieldsFromUi();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "請填寫有效的數字。", "保存", JOptionPane.ERROR_MESSAGE);
            return;
        }
        CharacterStatsSnapshot clamped = CharacterStatsSnapshot.clamp(requested);
        if (!snapshotsEqual(requested, clamped)) {
            int c = JOptionPane.showConfirmDialog(this,
                    "部分數值超出範圍，將自動限制爲：\n" + clamped.describe() + "\n\n是否繼續保存？",
                    "數值校正", JOptionPane.YES_NO_OPTION);
            if (c != JOptionPane.YES_OPTION) {
                return;
            }
            fillFields(clamped);
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "確定保存對「" + currentTarget.name + "」的屬性修改嗎？",
                "確認保存", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        ConsoleCharacterService.OpResult result = ConsoleCharacterService.updateCharacterStats(currentTarget, clamped);
        JOptionPane.showMessageDialog(this, result.message, "保存屬性",
                result.success ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
        if (result.success) {
            loadFromCharacter();
            firePropertyChange("statsSaved", false, true);
        }
    }

    private static boolean snapshotsEqual(CharacterStatsSnapshot a, CharacterStatsSnapshot b) {
        return a.level == b.level && a.str == b.str && a.dex == b.dex && a.inte == b.inte && a.luk == b.luk
                && a.maxHp == b.maxHp && a.maxMp == b.maxMp;
    }
}
