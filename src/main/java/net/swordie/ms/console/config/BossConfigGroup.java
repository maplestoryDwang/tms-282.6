package net.swordie.ms.console.config;

import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.swordie.ms.constants.BossHpConstants;
import net.swordie.ms.enums.BossPartyType;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class BossConfigGroup {
    private static final NumberFormat HP_FORMAT = NumberFormat.getIntegerInstance(Locale.CHINA);

    private final DefaultListModel<String> bossListModel = new DefaultListModel<>();
    private final JList<String> bossList = new JList<>(bossListModel);
    private final JPanel detailPanel = new JPanel();
    private final Map<Integer, List<BossPartyType>> bossesByOrder = new LinkedHashMap<>();
    private final Map<BossPartyType, DifficultyEditor> editors = new LinkedHashMap<>();

    JComponent buildPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        root.add(new JLabel("維護各首領難度開放狀態與標準血量。默認全部開放，血量基於平滑曲線生成，保存後實時生效。"),
                BorderLayout.NORTH);

        buildBossIndex();
        bossList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bossList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showBoss(bossList.getSelectedValue());
            }
        });

        JScrollPane listScroll = ConfigUi.verticalScroll(bossList);
        listScroll.setPreferredSize(new Dimension(220, 0));
        listScroll.setMinimumSize(new Dimension(180, 120));

        detailPanel.setLayout(new BorderLayout());
        JScrollPane detailScroll = ConfigUi.verticalScroll(detailPanel);
        detailScroll.setMinimumSize(new Dimension(420, 120));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, detailScroll);
        split.setContinuousLayout(true);
        split.setOneTouchExpandable(true);
        split.setResizeWeight(0.22);
        split.setDividerLocation(220);
        root.add(split, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton reload = new JButton("重載配置");
        reload.addActionListener(e -> reload());
        JButton reset = new JButton("恢復當前首領默認");
        reset.addActionListener(e -> resetSelectedBoss());
        JButton save = new JButton("保存並實時生效");
        save.addActionListener(e -> save(root));
        buttons.add(reload);
        buttons.add(reset);
        buttons.add(save);
        root.add(buttons, BorderLayout.SOUTH);

        if (!bossListModel.isEmpty()) {
            bossList.setSelectedIndex(0);
        }
        return root;
    }

    private void buildBossIndex() {
        bossesByOrder.clear();
        for (BossPartyType type : BossPartyType.values()) {
            bossesByOrder.computeIfAbsent(type.getOrderId(), ignored -> new ArrayList<>()).add(type);
        }
        bossListModel.clear();
        for (Map.Entry<Integer, List<BossPartyType>> entry : bossesByOrder.entrySet()) {
            int orderId = entry.getKey();
            String english = entry.getValue().getFirst().getBossName();
            bossListModel.addElement(BossRuntimeConfig.bossChineseName(orderId) + "  [" + english + "]");
        }
    }

    private void showBoss(String selection) {
        detailPanel.removeAll();
        if (selection == null) {
            detailPanel.revalidate();
            detailPanel.repaint();
            return;
        }
        int orderId = resolveOrderId(selection);
        List<BossPartyType> types = bossesByOrder.get(orderId);
        if (types == null) {
            return;
        }
        types.forEach(type -> editors.remove(type));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        for (BossPartyType type : types) {
            content.add(buildDifficultyPanel(type));
            content.add(Box.createVerticalStrut(10));
        }

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(content, BorderLayout.NORTH);
        detailPanel.add(wrap, BorderLayout.NORTH);
        detailPanel.revalidate();
        detailPanel.repaint();
    }

    private JPanel buildDifficultyPanel(BossPartyType type) {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder(
                BossRuntimeConfig.difficultyLabel(type) + "  (Lv." + type.getLevelMin() + ")"));

        JCheckBox enabled = new JCheckBox("開放該難度", BossRuntimeConfig.isDifficultyEnabled(type));
        panel.add(enabled, BorderLayout.NORTH);

        JPanel mobPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = ConfigUi.gbc();
        c.gridwidth = 4;
        c.weightx = 1;
        JLabel header = new JLabel("怪物ID    血量配置    默認血量    狀態");
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        mobPanel.add(header, c);
        c.gridy++;
        c.gridwidth = 1;
        c.weightx = 0;

        List<MobHpEditor> mobEditors = new ArrayList<>();
        Int2LongOpenHashMap ctx = BossHpConstants.getDefaultContextHpMap(type);
        if (ctx == null || ctx.isEmpty()) {
            c.gridwidth = 4;
            mobPanel.add(new JLabel("該難度暫無標準血量映射，請檢查 BossHpConstants。"), c);
        } else {
            ctx.forEach((mobId, defaultHp) -> {
                long currentHp = BossRuntimeConfig.getContextHp(type, mobId);
                JLabel idLabel = new JLabel(String.valueOf(mobId));
                JTextField hpField = new JTextField(String.valueOf(currentHp), 16);
                JLabel defaultLabel = new JLabel(formatHp(defaultHp));
                JLabel statusLabel = new JLabel(BossRuntimeConfig.hpStatus(currentHp, mobId));
                statusLabel.setForeground(statusColor(currentHp, mobId));

                hpField.getDocument().addDocumentListener(ConfigUi.onChange(() -> {
                    long hp = parseHp(hpField.getText(), currentHp);
                    statusLabel.setText(BossRuntimeConfig.hpStatus(hp, mobId));
                    statusLabel.setForeground(statusColor(hp, mobId));
                }));

                c.gridx = 0;
                c.weightx = 0;
                mobPanel.add(idLabel, c);
                c.gridx = 1;
                c.weightx = 1;
                mobPanel.add(hpField, c);
                c.gridx = 2;
                c.weightx = 0;
                mobPanel.add(defaultLabel, c);
                c.gridx = 3;
                mobPanel.add(statusLabel, c);
                c.gridy++;

                mobEditors.add(new MobHpEditor(mobId, hpField, defaultHp));
            });
        }

        editors.put(type, new DifficultyEditor(type, enabled, mobEditors));
        panel.add(mobPanel, BorderLayout.CENTER);
        return panel;
    }

    private int resolveOrderId(String selection) {
        int index = bossList.getSelectedIndex();
        if (index < 0) {
            return -1;
        }
        int i = 0;
        for (Integer orderId : bossesByOrder.keySet()) {
            if (i == index) {
                return orderId;
            }
            i++;
        }
        return -1;
    }

    private void reload() {
        BossRuntimeConfig.reload();
        showBoss(bossList.getSelectedValue());
    }

    private void resetSelectedBoss() {
        int orderId = resolveOrderId(bossList.getSelectedValue());
        if (orderId < 0) {
            return;
        }
        for (BossPartyType type : bossesByOrder.get(orderId)) {
            DifficultyEditor editor = editors.get(type);
            if (editor == null) {
                continue;
            }
            editor.enabledBox.setSelected(true);
            for (MobHpEditor mobEditor : editor.mobEditors) {
                mobEditor.hpField.setText(String.valueOf(mobEditor.defaultHp));
            }
        }
    }

    private void save(Component parent) {
        Map<String, String> values = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        for (BossPartyType type : BossPartyType.values()) {
            DifficultyEditor editor = editors.get(type);
            boolean enabled = editor != null ? editor.enabledBox.isSelected() : BossRuntimeConfig.isDifficultyEnabled(type);
            values.put(BossRuntimeConfig.enabledKey(type), String.valueOf(enabled));

            Int2LongOpenHashMap ctx = BossHpConstants.getDefaultContextHpMap(type);
            if (ctx == null || ctx.isEmpty()) {
                continue;
            }
            ctx.forEach((mobId, defaultHp) -> {
                long hp;
                if (editor != null) {
                    MobHpEditor mobEditor = editor.mobEditors.stream()
                            .filter(item -> item.mobId == mobId)
                            .findFirst()
                            .orElse(null);
                    hp = mobEditor == null
                            ? BossRuntimeConfig.getContextHp(type, mobId)
                            : parseHp(mobEditor.hpField.getText(), defaultHp);
                } else {
                    hp = BossRuntimeConfig.getContextHp(type, mobId);
                }
                if (hp <= 0) {
                    warnings.add("INVALID:" + type.name() + ":" + mobId);
                    return;
                }
                String status = BossRuntimeConfig.hpStatus(hp, mobId);
                if (status.startsWith("危險") || status.startsWith("偏低")) {
                    warnings.add(BossRuntimeConfig.bossChineseName(type.getOrderId())
                            + " " + BossRuntimeConfig.difficultyLabel(type)
                            + " / " + mobId + ": " + status);
                }
                values.put(BossRuntimeConfig.hpKey(type, mobId), String.valueOf(hp));
            });
        }
        if (warnings.stream().anyMatch(item -> item.startsWith("INVALID:"))) {
            JOptionPane.showMessageDialog(parent, "存在無效血量，請檢查後再保存。", "首領BOSS", JOptionPane.ERROR_MESSAGE);
            return;
        }
        List<String> lowHpWarnings = warnings.stream().filter(item -> !item.startsWith("INVALID:")).toList();
        if (!lowHpWarnings.isEmpty() && JOptionPane.showConfirmDialog(parent,
                "以下條目血量偏低，仍要保存嗎？\n" + String.join("\n", lowHpWarnings),
                "首領BOSS", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            BossRuntimeConfig.save(values);
            BossRuntimeConfig.reload();
            JOptionPane.showMessageDialog(parent,
                    "首領配置已保存到 config/boss.properties，並已實時生效。\n"
                            + "· 難度開放：立即影響 Boss UI 與入場腳本。\n"
                            + "· 血量：新刷出的首領怪物立即使用新血量。",
                    "保存成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, "保存失敗: " + ex.getMessage(), "首領BOSS", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static long parseHp(String raw, long fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(raw.trim().replace(",", ""));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String formatHp(long hp) {
        return HP_FORMAT.format(hp);
    }

    private static Color statusColor(long hp, int mobId) {
        String status = BossRuntimeConfig.hpStatus(hp, mobId);
        if (status.startsWith("危險")) {
            return new Color(180, 0, 0);
        }
        if (status.startsWith("偏低") || status.startsWith("未配置")) {
            return new Color(180, 120, 0);
        }
        return new Color(0, 120, 0);
    }

    private record DifficultyEditor(BossPartyType type, JCheckBox enabledBox, List<MobHpEditor> mobEditors) {
    }

    private record MobHpEditor(int mobId, JTextField hpField, long defaultHp) {
    }
}
